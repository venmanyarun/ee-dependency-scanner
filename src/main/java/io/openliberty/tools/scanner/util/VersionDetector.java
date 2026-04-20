package io.openliberty.tools.scanner.util;

import io.openliberty.tools.scanner.model.DependencyInfo;

import java.util.*;

/**
 * Detects Jakarta EE and MicroProfile platform versions from dependencies.
 */
public class VersionDetector {
    
    private static final VersionMappingRegistry REGISTRY;
    
    static {
        REGISTRY = VersionMappingRegistry.loadFromClasspath();
        if (!REGISTRY.isValid()) {
            System.err.println("Warning: Version mapping registry is not valid. " +
                             "Ensure jakarta-ee-versions.properties and microprofile-versions.properties " +
                             "are present in classpath.");
        }
    }
    
    /**
     * Detects Jakarta EE platform versions from dependencies.
     * @param dependencies list of dependencies to analyze
     * @return set of detected platform versions
     */
    public static Set<String> detectJakartaEEVersion(List<DependencyInfo> dependencies) {
        Set<String> versions = new HashSet<>();
        
        for (DependencyInfo dep : dependencies) {
            if (!dep.isJakartaEE() && !dep.isJavaEE()) continue;
            
            String platformVersion = REGISTRY.getJakartaEEPlatformVersion(
                dep.getArtifactId(),
                dep.getVersion()
            );
            
            if (platformVersion != null) {
                versions.add(platformVersion);
            } else {
                String inferredVersion = inferJakartaEEVersion(dep);
                if (inferredVersion != null) {
                    versions.add(inferredVersion);
                }
            }
        }
        
        return versions;
    }
    
    /**
     * Detects MicroProfile platform versions from dependencies.
     * @param dependencies list of dependencies to analyze
     * @return set of detected platform versions
     */
    public static Set<String> detectMicroProfileVersion(List<DependencyInfo> dependencies) {
        Set<String> versions = new HashSet<>();
        
        for (DependencyInfo dep : dependencies) {
            if (!dep.isMicroProfile()) continue;
            
            String platformVersion = REGISTRY.getMicroProfilePlatformVersion(
                dep.getArtifactId(),
                dep.getVersion()
            );
            
            if (platformVersion != null) {
                versions.add(platformVersion);
            }
        }
        
        return versions;
    }
    
    private static String inferJakartaEEVersion(DependencyInfo dep) {
        String version = dep.getVersion();
        if (version == null) return null;
        
        if (dep.getArtifactId().contains("servlet")) {
            if (version.startsWith("6.1")) return "11";
            if (version.startsWith("6.0")) return "10";
            if (version.startsWith("5.")) return "9";
            if (version.startsWith("4.")) return "8";
            if (version.startsWith("3.1")) return "7";
            if (version.startsWith("3.0")) return "6";
        }
        
        if (dep.getArtifactId().contains("persistence")) {
            if (version.startsWith("3.2")) return "11";
            if (version.startsWith("3.1")) return "10";
            if (version.startsWith("3.0")) return "9";
            if (version.startsWith("2.2")) return "8";
            if (version.startsWith("2.1")) return "7";
            if (version.startsWith("2.0")) return "6";
        }
        
        if (dep.getArtifactId().contains("cdi")) {
            if (version.startsWith("4.1")) return "11";
            if (version.startsWith("4.0")) return "10";
            if (version.startsWith("3.")) return "9";
            if (version.startsWith("2.")) return "8";
            if (version.startsWith("1.2")) return "7";
            if (version.startsWith("1.1")) return "7";
            if (version.startsWith("1.0")) return "6";
        }
        
        if (dep.getArtifactId().contains("faces") || dep.getArtifactId().contains("jsf")) {
            if (version.startsWith("4.")) return "10";
            if (version.startsWith("3.")) return "9";
            if (version.startsWith("2.3")) return "8";
            if (version.startsWith("2.2")) return "7";
            if (version.startsWith("2.1")) return "7";
            if (version.startsWith("2.0")) return "6";
        }
        
        if (dep.getArtifactId().contains("jaxrs") || dep.getArtifactId().contains("restful")) {
            if (version.startsWith("3.")) return "9";
            if (version.startsWith("2.1")) return "8";
            if (version.startsWith("2.0")) return "7";
            if (version.startsWith("1.1")) return "6";
        }
        
        if (dep.getArtifactId().contains("ejb")) {
            if (version.startsWith("4.")) return "10";
            if (version.startsWith("3.2")) return "7";
            if (version.startsWith("3.1")) return "6";
        }
        
        if (dep.getArtifactId().contains("validation")) {
            if (version.startsWith("3.")) return "9";
            if (version.startsWith("2.")) return "8";
            if (version.startsWith("1.1")) return "7";
            if (version.startsWith("1.0")) return "6";
        }
        
        return null;
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

