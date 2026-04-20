package io.openliberty.tools.scanner.util;

import io.openliberty.tools.scanner.model.DependencyInfo;
import io.openliberty.tools.scanner.model.DependencySource;
import io.openliberty.tools.scanner.model.DependencyType;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Helper utility for IDE adapters to analyze collected dependencies.
 *
 * After an IDE adapter collects classpath entries/modules, use this helper
 * to convert them to DependencyInfo objects and detect versions.
 *
 * Example usage in an IDE adapter:
 * <pre>
 * DependencyAnalysisHelper helper = new DependencyAnalysisHelper();
 * List<DependencyInfo> dependencies = new ArrayList<>();
 *
 * // For each classpath entry from IDE:
 * DependencyInfo dep = helper.createDependency("jakarta.servlet", "jakarta.servlet-api", "5.0.0");
 * dependencies.add(dep);
 *
 * // Detect versions:
 * Map<String, Set<String>> versions = helper.detectVersions(dependencies);
 * </pre>
 */
public class DependencyAnalysisHelper {
    
    private final VersionMappingRegistry versionRegistry;
    
    public DependencyAnalysisHelper() {
        this.versionRegistry = VersionMappingRegistry.loadFromClasspath();
    }
    
    /**
     * Creates a DependencyInfo from Maven coordinates.
     *
     * @param groupId Maven groupId
     * @param artifactId Maven artifactId
     * @param version version string
     * @return DependencyInfo object
     */
    public DependencyInfo createDependency(String groupId, String artifactId, String version) {
        return DependencyInfo.builder()
            .groupId(groupId)
            .artifactId(artifactId)
            .version(version)
            .source(DependencySource.IDE_RESOLVED)
            .build();
    }
    
    /**
     * Creates a DependencyInfo from Maven coordinates with JAR path.
     *
     * @param groupId Maven groupId
     * @param artifactId Maven artifactId
     * @param version version string
     * @param jarPath path to JAR file
     * @return DependencyInfo object
     */
    public DependencyInfo createDependency(String groupId, String artifactId, String version, String jarPath) {
        return DependencyInfo.builder()
            .groupId(groupId)
            .artifactId(artifactId)
            .version(version)
            .jarPath(jarPath)
            .source(DependencySource.IDE_RESOLVED)
            .build();
    }
    
    /**
     * Creates a DependencyInfo from a JAR file path.
     * Extracts Maven coordinates from JAR manifest or filename.
     *
     * @param jarFile JAR file
     * @return list of dependencies found in JAR
     */
    public List<DependencyInfo> extractDependenciesFromJar(File jarFile) {
        JarDependencyExtractor extractor = new JarDependencyExtractor();
        return extractor.extract(jarFile);
    }
    
    /**
     * Analyzes a list of dependencies and detects EE versions.
     *
     * @param dependencies list of dependencies to analyze
     * @return map with detected versions (jakartaEE, javaEE, microProfile)
     */
    public Map<String, Set<String>> detectVersions(List<DependencyInfo> dependencies) {
        Map<String, Set<String>> versions = new HashMap<>();
        
        Set<String> jakartaVersions = VersionDetector.detectJakartaEEVersion(dependencies);
        Set<String> mpVersions = VersionDetector.detectMicroProfileVersion(dependencies);
        
        if (!jakartaVersions.isEmpty()) {
            versions.put("jakartaEE", jakartaVersions);
        }
        if (!mpVersions.isEmpty()) {
            versions.put("microProfile", mpVersions);
        }
        
        return versions;
    }
    
    /**
     * Gets the primary (most common) version from a set of detected versions.
     *
     * @param versions set of versions
     * @return primary version or null if empty
     */
    public String getPrimaryVersion(Set<String> versions) {
        if (versions == null || versions.isEmpty()) {
            return null;
        }
        // Return the highest version
        return versions.stream()
            .max(this::compareVersions)
            .orElse(null);
    }
    
    /**
     * Filters dependencies by type.
     * 
     * @param dependencies list of dependencies
     * @param type dependency type to filter by
     * @return filtered list
     */
    public List<DependencyInfo> filterByType(List<DependencyInfo> dependencies, DependencyType type) {
        return dependencies.stream()
            .filter(dep -> dep.getType() == type)
            .collect(Collectors.toList());
    }
    
    /**
     * Gets all Jakarta EE dependencies.
     * 
     * @param dependencies list of dependencies
     * @return list of Jakarta EE dependencies
     */
    public List<DependencyInfo> getJakartaEEDependencies(List<DependencyInfo> dependencies) {
        return filterByType(dependencies, DependencyType.JAKARTA_EE);
    }
    
    /**
     * Gets all Java EE dependencies.
     * 
     * @param dependencies list of dependencies
     * @return list of Java EE dependencies
     */
    public List<DependencyInfo> getJavaEEDependencies(List<DependencyInfo> dependencies) {
        return filterByType(dependencies, DependencyType.JAVA_EE);
    }
    
    /**
     * Gets all MicroProfile dependencies.
     * 
     * @param dependencies list of dependencies
     * @return list of MicroProfile dependencies
     */
    public List<DependencyInfo> getMicroProfileDependencies(List<DependencyInfo> dependencies) {
        return filterByType(dependencies, DependencyType.MICROPROFILE);
    }
    
    /**
     * Checks if a dependency matches given coordinates.
     * 
     * @param dependency dependency to check
     * @param groupId groupId to match
     * @param artifactId artifactId to match
     * @return true if matches
     */
    public boolean matches(DependencyInfo dependency, String groupId, String artifactId) {
        return groupId.equals(dependency.getGroupId()) && 
               artifactId.equals(dependency.getArtifactId());
    }
    
    /**
     * Finds a dependency by coordinates.
     * 
     * @param dependencies list of dependencies
     * @param groupId groupId to find
     * @param artifactId artifactId to find
     * @return matching dependency or null
     */
    public DependencyInfo findDependency(List<DependencyInfo> dependencies, String groupId, String artifactId) {
        return dependencies.stream()
            .filter(dep -> matches(dep, groupId, artifactId))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Deduplicates dependencies, keeping the highest version.
     * 
     * @param dependencies list of dependencies (may contain duplicates)
     * @return deduplicated list
     */
    public List<DependencyInfo> deduplicate(List<DependencyInfo> dependencies) {
        Map<String, DependencyInfo> uniqueDeps = new HashMap<>();
        
        for (DependencyInfo dep : dependencies) {
            String key = dep.getGroupId() + ":" + dep.getArtifactId();
            DependencyInfo existing = uniqueDeps.get(key);
            
            if (existing == null) {
                uniqueDeps.put(key, dep);
            } else {
                // Keep the higher version
                if (compareVersions(dep.getVersion(), existing.getVersion()) > 0) {
                    uniqueDeps.put(key, dep);
                }
            }
        }
        
        return new ArrayList<>(uniqueDeps.values());
    }
    
    /**
     * Compares two version strings.
     * 
     * @param v1 first version
     * @param v2 second version
     * @return negative if v1 < v2, 0 if equal, positive if v1 > v2
     */
    private int compareVersions(String v1, String v2) {
        if (v1 == null && v2 == null) return 0;
        if (v1 == null) return -1;
        if (v2 == null) return 1;
        
        String[] parts1 = v1.split("[.-]");
        String[] parts2 = v2.split("[.-]");
        
        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int p1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
            int p2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;
            
            if (p1 != p2) {
                return Integer.compare(p1, p2);
            }
        }
        
        return 0;
    }
    
    private int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /**
     * Determines the dependency type based on groupId and artifactId.
     */
    private DependencyType determineDependencyType(String groupId, String artifactId) {
        if (groupId.startsWith("jakarta.")) {
            return DependencyType.JAKARTA_EE;
        } else if (groupId.startsWith("javax.")) {
            return DependencyType.JAVA_EE;
        } else if (groupId.contains("microprofile")) {
            return DependencyType.MICROPROFILE;
        } else {
            return DependencyType.OTHER;
        }
    }
    
    /**
     * Gets the expected Jakarta EE version for a specific API version.
     *
     * @param artifactId API artifactId (e.g., "jakarta.servlet-api")
     * @param apiVersion API version (e.g., "5.0.0")
     * @return Jakarta EE platform version or null
     */
    public String getJakartaEEPlatformVersion(String artifactId, String apiVersion) {
        return versionRegistry.getJakartaEEPlatformVersion(artifactId, apiVersion);
    }
    
    /**
     * Gets the expected MicroProfile version for a specific API version.
     *
     * @param artifactId API artifactId (e.g., "microprofile-config-api")
     * @param apiVersion API version (e.g., "2.0")
     * @return MicroProfile platform version or null
     */
    public String getMicroProfilePlatformVersion(String artifactId, String apiVersion) {
        return versionRegistry.getMicroProfilePlatformVersion(artifactId, apiVersion);
    }
}
