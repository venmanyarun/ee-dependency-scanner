package io.openliberty.tools.scanner.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for version mappings between feature and platform versions.
 * Loads mappings from properties files and provides efficient lookup methods.
 */
public class VersionMappingRegistry {
    
    private final Map<String, String> jakartaEEMappings;
    private final Map<String, String> microProfileMappings;
    
    // Pre-computed normalized artifact names for faster lookups
    private final Map<String, String> normalizedArtifactCache;
    
    // Reverse index for fuzzy matching: feature -> list of (version, platformVersion)
    private final Map<String, List<VersionMapping>> jakartaEEFuzzyIndex;
    private final Map<String, List<VersionMapping>> microProfileFuzzyIndex;
    
    private final boolean valid;
    
    private static class VersionMapping {
        final String featureVersion;
        final String platformVersion;
        
        VersionMapping(String featureVersion, String platformVersion) {
            this.featureVersion = featureVersion;
            this.platformVersion = platformVersion;
        }
    }
    
    private VersionMappingRegistry(Map<String, String> jakartaEEMappings, 
                                   Map<String, String> microProfileMappings,
                                   boolean valid) {
        this.jakartaEEMappings = Collections.unmodifiableMap(jakartaEEMappings);
        this.microProfileMappings = Collections.unmodifiableMap(microProfileMappings);
        this.normalizedArtifactCache = new ConcurrentHashMap<>();
        this.valid = valid;
        
        // Build fuzzy matching indexes
        this.jakartaEEFuzzyIndex = buildFuzzyIndex(jakartaEEMappings);
        this.microProfileFuzzyIndex = buildFuzzyIndex(microProfileMappings);
    }
    
    /**
     * Builds a fuzzy matching index from mappings.
     * Groups by feature name for efficient prefix matching.
     */
    private Map<String, List<VersionMapping>> buildFuzzyIndex(Map<String, String> mappings) {
        Map<String, List<VersionMapping>> index = new HashMap<>();
        
        for (Map.Entry<String, String> entry : mappings.entrySet()) {
            String key = entry.getKey();
            String platformVersion = entry.getValue();
            
            // Parse key: feature.version (e.g., "servlet.6.0")
            int lastDotIndex = key.lastIndexOf('.');
            if (lastDotIndex > 0) {
                String feature = key.substring(0, lastDotIndex);
                String version = key.substring(lastDotIndex + 1);
                
                index.computeIfAbsent(feature, k -> new ArrayList<>())
                     .add(new VersionMapping(version, platformVersion));
            }
        }
        
        // Sort by version (descending) for each feature
        for (List<VersionMapping> versionList : index.values()) {
            versionList.sort((a, b) -> compareVersions(b.featureVersion, a.featureVersion));
        }
        
        return Collections.unmodifiableMap(index);
    }
    
    /**
     * Creates empty registry.
     * @return empty registry instance
     */
    public static VersionMappingRegistry empty() {
        return new VersionMappingRegistry(
            Collections.emptyMap(), 
            Collections.emptyMap(), 
            false
        );
    }
    
    /**
     * Loads version mappings from classpath properties files (optimized).
     * @return registry with loaded mappings
     */
    public static VersionMappingRegistry loadFromClasspath() {
        Map<String, String> jakartaEE = loadProperties("/jakarta-ee-versions.properties");
        Map<String, String> microProfile = loadProperties("/microprofile-versions.properties");
        
        boolean valid = !jakartaEE.isEmpty() || !microProfile.isEmpty();
        
        return new VersionMappingRegistry(jakartaEE, microProfile, valid);
    }
    
    private static Map<String, String> loadProperties(String resourcePath) {
        Map<String, String> mappings = new HashMap<>();
        
        try (InputStream is = VersionMappingRegistry.class.getResourceAsStream(resourcePath)) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                
                // Pre-size the map for efficiency
                mappings = new HashMap<>(props.size());
                
                for (String key : props.stringPropertyNames()) {
                    // Intern strings to save memory for duplicate values
                    String value = props.getProperty(key).intern();
                    mappings.put(key, value);
                }
            }
        } catch (IOException e) {
            System.err.println("Warning: Failed to load " + resourcePath + ": " + e.getMessage());
        }
        
        return mappings;
    }
    
    /**
     * Gets Jakarta EE platform version for feature (cached normalization).
     * @param artifactId artifact ID
     * @param version feature version
     * @return platform version or null
     */
    public String getJakartaEEPlatformVersion(String artifactId, String version) {
        String feature = normalizeFeatureName(artifactId);
        String majorMinor = extractMajorMinor(version);
        
        if (feature == null || majorMinor == null) return null;
        
        String key = feature + "." + majorMinor;
        return jakartaEEMappings.get(key);
    }
    
    /**
     * Gets MicroProfile platform version for feature (cached normalization).
     * @param artifactId artifact ID
     * @param version feature version
     * @return platform version or null
     */
    public String getMicroProfilePlatformVersion(String artifactId, String version) {
        String feature = normalizeFeatureName(artifactId);
        String majorMinor = extractMajorMinor(version);
        
        if (feature == null || majorMinor == null) return null;
        
        String key = feature + "." + majorMinor;
        return microProfileMappings.get(key);
    }
    
    /**
     * Finds best Jakarta EE match using fuzzy matching on properties file data.
     * Tries to match version prefixes when exact match fails.
     * 
     * @param artifactId artifact ID
     * @param version feature version
     * @return platform version or null
     */
    public String findBestJakartaEEMatch(String artifactId, String version) {
        if (artifactId == null || version == null) return null;
        
        String feature = normalizeFeatureName(artifactId);
        if (feature == null) return null;
        
        List<VersionMapping> mappings = jakartaEEFuzzyIndex.get(feature);
        if (mappings == null || mappings.isEmpty()) return null;
        
        // Try to find best match by version prefix
        for (VersionMapping mapping : mappings) {
            if (version.startsWith(mapping.featureVersion)) {
                return mapping.platformVersion;
            }
        }
        
        return null;
    }
    
    /**
     * Finds best MicroProfile match using fuzzy matching on properties file data.
     * Tries to match version prefixes when exact match fails.
     * 
     * @param artifactId artifact ID
     * @param version feature version
     * @return platform version or null
     */
    public String findBestMicroProfileMatch(String artifactId, String version) {
        if (artifactId == null || version == null) return null;
        
        String feature = normalizeFeatureName(artifactId);
        if (feature == null) return null;
        
        List<VersionMapping> mappings = microProfileFuzzyIndex.get(feature);
        if (mappings == null || mappings.isEmpty()) return null;
        
        // Try to find best match by version prefix
        for (VersionMapping mapping : mappings) {
            if (version.startsWith(mapping.featureVersion)) {
                return mapping.platformVersion;
            }
        }
        
        return null;
    }
    
    /**
     * Normalizes feature name with caching for performance.
     */
    private String normalizeFeatureName(String artifactId) {
        if (artifactId == null) return null;
        
        // Check cache first
        return normalizedArtifactCache.computeIfAbsent(artifactId, this::computeNormalizedName);
    }
    
    private String computeNormalizedName(String artifactId) {
        String name = artifactId
            .replaceAll("-api$", "")
            .replaceAll("-spec$", "")
            .replaceAll("^jakarta\\.", "")
            .replaceAll("^javax\\.", "")
            .replaceAll("^microprofile-", "")
            .replaceAll("-", "");
        
        return name.toLowerCase().intern(); // Intern to save memory
    }
    
    /**
     * Extracts major.minor version efficiently.
     */
    private String extractMajorMinor(String version) {
        if (version == null) return null;
        
        int firstDot = version.indexOf('.');
        if (firstDot == -1) {
            return version + ".0";
        }
        
        int secondDot = version.indexOf('.', firstDot + 1);
        if (secondDot == -1) {
            return version;
        }
        
        return version.substring(0, secondDot);
    }
    
    /**
     * Compares two version strings.
     */
    private static int compareVersions(String v1, String v2) {
        if (v1 == null && v2 == null) return 0;
        if (v1 == null) return -1;
        if (v2 == null) return 1;
        
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        
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
    
    private static int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    public boolean isValid() {
        return valid;
    }
    
    public Map<String, String> getJakartaEEMappings() {
        return jakartaEEMappings;
    }
    
    public Map<String, String> getMicroProfileMappings() {
        return microProfileMappings;
    }
    
    /**
     * Gets the size of the normalized artifact cache.
     * Useful for monitoring memory usage.
     */
    public int getCacheSize() {
        return normalizedArtifactCache.size();
    }
    
    /**
     * Clears the normalization cache.
     * Useful for testing or memory management.
     */
    public void clearCache() {
        normalizedArtifactCache.clear();
    }
}

// Made with Bob
