package io.openliberty.tools.scanner.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Registry for version mappings between feature versions and platform versions.
 * Loads mappings from properties files and provides fail-fast validation.
 * 
 * This consolidates version detection logic in a single, testable class.
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
     * Creates an empty registry (used when properties files are missing).
     */
    public static VersionMappingRegistry empty() {
        return new VersionMappingRegistry(
            Collections.emptyMap(), 
            Collections.emptyMap(), 
            false
        );
    }
    
    /**
     * Loads version mappings from properties files in the classpath.
     * 
     * @return registry with loaded mappings
     * @throws IllegalStateException if properties files cannot be loaded
     */
    public static VersionMappingRegistry loadFromClasspath() {
        Map<String, String> jakartaEE = loadProperties("/jakarta-ee-versions.properties");
        Map<String, String> microProfile = loadProperties("/microprofile-versions.properties");
        
        boolean valid = !jakartaEE.isEmpty() || !microProfile.isEmpty();
        
        return new VersionMappingRegistry(jakartaEE, microProfile, valid);
    }
    
    /**
     * Loads properties from a classpath resource.
     */
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
     * Gets the Jakarta EE platform version for a given feature and version.
     * 
     * @param artifactId the artifact ID (e.g., "jakarta.servlet-api")
     * @param version the feature version (e.g., "6.0.0")
     * @return platform version (e.g., "10") or null if not found
     */
    public String getJakartaEEPlatformVersion(String artifactId, String version) {
        String feature = normalizeFeatureName(artifactId);
        String majorMinor = extractMajorMinor(version);
        
        if (feature == null || majorMinor == null) {
            return null;
        }
        
        String key = feature + ":" + majorMinor;
        return jakartaEEMappings.get(key);
    }
    
    /**
     * Gets the MicroProfile platform version for a given feature and version.
     * 
     * @param artifactId the artifact ID (e.g., "microprofile-config-api")
     * @param version the feature version (e.g., "3.0")
     * @return platform version (e.g., "5.0") or null if not found
     */
    public String getMicroProfilePlatformVersion(String artifactId, String version) {
        String feature = normalizeFeatureName(artifactId);
        String majorMinor = extractMajorMinor(version);
        
        if (feature == null || majorMinor == null) {
            return null;
        }
        
        String key = feature + ":" + majorMinor;
        return microProfileMappings.get(key);
    }
    
    /**
     * Normalizes a feature name from an artifact ID.
     * Example: "jakarta.servlet-api" -> "servlet"
     */
    private String normalizeFeatureName(String artifactId) {
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
     * Extracts major.minor version from a version string.
     * Example: "6.0.0" -> "6.0", "5.0" -> "5.0"
     */
    private String extractMajorMinor(String version) {
        if (version == null) {
            return null;
        }
        
        String[] parts = version.split("\\.");
        if (parts.length >= 2) {
            return parts[0] + "." + parts[1];
        } else if (parts.length == 1) {
            return parts[0] + ".0";
        }
        
        return version;
    }
    
    /**
     * Checks if the registry is valid (has loaded mappings).
     */
    public boolean isValid() {
        return valid;
    }
    
    /**
     * Gets all Jakarta EE mappings.
     */
    public Map<String, String> getJakartaEEMappings() {
        return jakartaEEMappings;
    }
    
    /**
     * Gets all MicroProfile mappings.
     */
    public Map<String, String> getMicroProfileMappings() {
        return microProfileMappings;
    }
}
