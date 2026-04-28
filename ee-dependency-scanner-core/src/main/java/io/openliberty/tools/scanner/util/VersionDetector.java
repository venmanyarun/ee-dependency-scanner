package io.openliberty.tools.scanner.util;

import io.openliberty.tools.scanner.api.DependencyInfo;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detector for Jakarta EE and MicroProfile platform versions from dependencies.
 * Uses version mappings from properties files to determine platform versions.
 */
public class VersionDetector {
    
    private static volatile VersionMappingRegistry REGISTRY;
    
    // Cache for version detection results
    private static final Map<Integer, Set<String>> JAKARTA_VERSION_CACHE = new ConcurrentHashMap<>();
    private static final Map<Integer, Set<String>> MP_VERSION_CACHE = new ConcurrentHashMap<>();
    
    /**
     * Gets or initializes the version registry.
     */
    private static VersionMappingRegistry getRegistry() {
        if (REGISTRY == null) {
            synchronized (VersionDetector.class) {
                if (REGISTRY == null) {
                    REGISTRY = VersionMappingRegistry.loadFromClasspath();
                    if (!REGISTRY.isValid()) {
                        System.err.println("Warning: Version mapping registry is not valid. " +
                                         "Ensure jakarta-ee-versions.properties and microprofile-versions.properties " +
                                         "are present in classpath.");
                    }
                }
            }
        }
        return REGISTRY;
    }
    
    /**
     * Clears all caches. Useful for testing.
     */
    public static void clearCaches() {
        JAKARTA_VERSION_CACHE.clear();
        MP_VERSION_CACHE.clear();
    }
    
    /**
     * Detects Jakarta EE platform versions from dependencies (cached).
     * @param dependencies list of dependencies to analyze
     * @return set of detected platform versions
     */
    public static Set<String> detectJakartaEEVersion(List<DependencyInfo> dependencies) {
        int cacheKey = computeDependencyHash(dependencies);
        
        return JAKARTA_VERSION_CACHE.computeIfAbsent(cacheKey, k -> 
            detectJakartaEEVersionInternal(dependencies)
        );
    }
    
    /**
     * Detects MicroProfile platform versions from dependencies (cached).
     * @param dependencies list of dependencies to analyze
     * @return set of detected platform versions
     */
    public static Set<String> detectMicroProfileVersion(List<DependencyInfo> dependencies) {
        int cacheKey = computeDependencyHash(dependencies);
        
        return MP_VERSION_CACHE.computeIfAbsent(cacheKey, k -> 
            detectMicroProfileVersionInternal(dependencies)
        );
    }
    
    private static int computeDependencyHash(List<DependencyInfo> dependencies) {
        int hash = 1;
        for (DependencyInfo dep : dependencies) {
            if (dep.isJakartaEE() || dep.isJavaEE() || dep.isMicroProfile()) {
                hash = 31 * hash + (dep.getArtifactId() != null ? dep.getArtifactId().hashCode() : 0);
                hash = 31 * hash + (dep.getVersion() != null ? dep.getVersion().hashCode() : 0);
            }
        }
        return hash;
    }
    
    private static Set<String> detectJakartaEEVersionInternal(List<DependencyInfo> dependencies) {
        Set<String> versions = new HashSet<>();
        VersionMappingRegistry registry = getRegistry();
        
        // Use parallel stream for large lists
        if (dependencies.size() > 50) {
            dependencies.parallelStream()
                .filter(dep -> dep.isJakartaEE() || dep.isJavaEE())
                .forEach(dep -> {
                    String platformVersion = detectSingleJakartaVersion(dep, registry);
                    if (platformVersion != null) {
                        synchronized (versions) {
                            versions.add(platformVersion);
                        }
                    }
                });
        } else {
            for (DependencyInfo dep : dependencies) {
                if (!dep.isJakartaEE() && !dep.isJavaEE()) continue;
                
                String platformVersion = detectSingleJakartaVersion(dep, registry);
                if (platformVersion != null) {
                    versions.add(platformVersion);
                }
            }
        }
        
        return versions;
    }
    
    private static String detectSingleJakartaVersion(DependencyInfo dep, VersionMappingRegistry registry) {
        // Try exact lookup from properties file
        String platformVersion = registry.getJakartaEEPlatformVersion(
            dep.getArtifactId(),
            dep.getVersion()
        );
        
        if (platformVersion != null) {
            return platformVersion;
        }
        
        // Try fuzzy matching with properties file data
        return registry.findBestJakartaEEMatch(dep.getArtifactId(), dep.getVersion());
    }
    
    private static Set<String> detectMicroProfileVersionInternal(List<DependencyInfo> dependencies) {
        Set<String> versions = new HashSet<>();
        VersionMappingRegistry registry = getRegistry();
        
        // Use parallel stream for large lists
        if (dependencies.size() > 50) {
            dependencies.parallelStream()
                .filter(DependencyInfo::isMicroProfile)
                .forEach(dep -> {
                    String platformVersion = detectSingleMicroProfileVersion(dep, registry);
                    
                    if (platformVersion != null) {
                        synchronized (versions) {
                            versions.add(platformVersion);
                        }
                    }
                });
        } else {
            for (DependencyInfo dep : dependencies) {
                if (!dep.isMicroProfile()) continue;
                
                String platformVersion = detectSingleMicroProfileVersion(dep, registry);
                
                if (platformVersion != null) {
                    versions.add(platformVersion);
                }
            }
        }
        
        return versions;
    }
    
    private static String detectSingleMicroProfileVersion(DependencyInfo dep, VersionMappingRegistry registry) {
        // Try exact lookup from properties file
        String platformVersion = registry.getMicroProfilePlatformVersion(
            dep.getArtifactId(),
            dep.getVersion()
        );
        
        if (platformVersion != null) {
            return platformVersion;
        }
        
        // Try fuzzy matching with properties file data
        return registry.findBestMicroProfileMatch(dep.getArtifactId(), dep.getVersion());
    }
    
    private static String extractFeatureName(String artifactId) {
        if (artifactId == null) {
            return null;
        }
        
        String name = artifactId
            .replaceAll("-api$", "")
            .replaceAll("-spec$", "")
            .replaceAll("^jakarta\\.", "")
            .replaceAll("^javax\\.", "")
            .replaceAll("^microprofile-", "")
            .replaceAll("-", "");
        
        return name.toLowerCase();
    }
    
    /**
     * Gets detailed Jakarta EE feature version report.
     * @param dependencies list of dependencies to analyze
     * @return map of feature names to versions
     */
    public static Map<String, Set<String>> getJakartaEEFeatureVersions(List<DependencyInfo> dependencies) {
        Map<String, Set<String>> featureVersions = new HashMap<>();
        
        for (DependencyInfo dep : dependencies) {
            if (!dep.isJakartaEE() && !dep.isJavaEE()) {
                continue;
            }
            
            String feature = extractFeatureName(dep.getArtifactId());
            String version = dep.getVersion();
            
            if (feature != null && version != null) {
                featureVersions.computeIfAbsent(feature, k -> new HashSet<>()).add(version);
            }
        }
        
        return featureVersions;
    }
    
    /**
     * Gets detailed MicroProfile feature version report.
     * @param dependencies list of dependencies to analyze
     * @return map of feature names to versions
     */
    public static Map<String, Set<String>> getMicroProfileFeatureVersions(List<DependencyInfo> dependencies) {
        Map<String, Set<String>> featureVersions = new HashMap<>();
        
        for (DependencyInfo dep : dependencies) {
            if (!dep.isMicroProfile()) {
                continue;
            }
            
            String feature = extractFeatureName(dep.getArtifactId());
            String version = dep.getVersion();
            
            if (feature != null && version != null) {
                featureVersions.computeIfAbsent(feature, k -> new HashSet<>()).add(version);
            }
        }
        
        return featureVersions;
    }
    
    /**
     * Checks if multiple versions of same feature detected.
     * @param featureVersions map of features to versions
     * @return true if conflicts exist
     */
    public static boolean hasVersionConflicts(Map<String, Set<String>> featureVersions) {
        return featureVersions.values().stream()
            .anyMatch(versions -> versions.size() > 1);
    }
}

// Made with Bob
