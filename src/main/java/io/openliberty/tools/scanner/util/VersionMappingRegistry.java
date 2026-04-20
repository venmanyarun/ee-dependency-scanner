package io.openliberty.tools.scanner.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Registry for version mappings between feature and platform versions.
 */
public class VersionMappingRegistry {
    
    private final Map<String, String> jakartaEEMappings;
    private final Map<String, String> microProfileMappings;
    private final boolean valid;
    
    private VersionMappingRegistry(Map<String, String> jakartaEEMappings, 
                                   Map<String, String> microProfileMappings,
                                   boolean valid) {
        this.jakartaEEMappings = Collections.unmodifiableMap(jakartaEEMappings);
        this.microProfileMappings = Collections.unmodifiableMap(microProfileMappings);
        this.valid = valid;
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
     * Loads version mappings from classpath properties files.
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
                for (String key : props.stringPropertyNames()) {
                    mappings.put(key, props.getProperty(key));
                }
            }
        } catch (IOException e) {
            System.err.println("Warning: Failed to load " + resourcePath + ": " + e.getMessage());
        }
        
        return mappings;
    }
    
    /**
     * Gets Jakarta EE platform version for feature.
     * @param artifactId artifact ID
     * @param version feature version
     * @return platform version or null
     */
    public String getJakartaEEPlatformVersion(String artifactId, String version) {
        String feature = normalizeFeatureName(artifactId);
        String majorMinor = extractMajorMinor(version);
        
        if (feature == null || majorMinor == null) return null;
        
        String key = feature + ":" + majorMinor;
        return jakartaEEMappings.get(key);
    }
    
    /**
     * Gets MicroProfile platform version for feature.
     * @param artifactId artifact ID
     * @param version feature version
     * @return platform version or null
     */
    public String getMicroProfilePlatformVersion(String artifactId, String version) {
        String feature = normalizeFeatureName(artifactId);
        String majorMinor = extractMajorMinor(version);
        
        if (feature == null || majorMinor == null) return null;
        
        String key = feature + ":" + majorMinor;
        return microProfileMappings.get(key);
    }
    
    private String normalizeFeatureName(String artifactId) {
        if (artifactId == null) return null;
        
        String name = artifactId
            .replaceAll("-api$", "")
            .replaceAll("-spec$", "")
            .replaceAll("^jakarta\\.", "")
            .replaceAll("^javax\\.", "")
            .replaceAll("^microprofile-", "")
            .replaceAll("-", "");
        
        return name.toLowerCase();
    }
    
    private String extractMajorMinor(String version) {
        if (version == null) return null;
        
        String[] parts = version.split("\\.");
        if (parts.length >= 2) {
            return parts[0] + "." + parts[1];
        } else if (parts.length == 1) {
            return parts[0] + ".0";
        }
        
        return version;
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
}
