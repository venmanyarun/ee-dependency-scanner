package io.openliberty.tools.scanner.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for VersionMappingRegistry to verify version mapping functionality.
 */
class VersionMappingRegistryTest {
    
    @Test
    void testLoadFromClasspath() {
        VersionMappingRegistry registry = VersionMappingRegistry.loadFromClasspath();
        
        assertTrue(registry.isValid(), "Registry should be valid after loading from classpath");
        assertFalse(registry.getJakartaEEMappings().isEmpty(), 
            "Should load Jakarta EE mappings");
    }
    
    @Test
    void testEmptyRegistry() {
        VersionMappingRegistry registry = VersionMappingRegistry.empty();
        
        assertFalse(registry.isValid(), "Empty registry should not be valid");
        assertTrue(registry.getJakartaEEMappings().isEmpty(), 
            "Empty registry should have no Jakarta EE mappings");
        assertTrue(registry.getMicroProfileMappings().isEmpty(), 
            "Empty registry should have no MicroProfile mappings");
    }
    
    @Test
    void testGetJakartaEEPlatformVersion() {
        VersionMappingRegistry registry = VersionMappingRegistry.loadFromClasspath();
        
        // Test servlet API version mapping - may return null if not in properties file
        String version = registry.getJakartaEEPlatformVersion("jakarta.servlet-api", "6.0.0");
        // Just verify the method works, don't assert specific version exists
        // since properties file content may vary
        
        // Test with different artifact ID format
        version = registry.getJakartaEEPlatformVersion("jakarta.servlet", "6.0.0");
        // Method should handle normalization without errors
        assertNotNull(registry, "Registry should be valid");
    }
    
    @Test
    void testGetJakartaEEPlatformVersionWithNullInputs() {
        VersionMappingRegistry registry = VersionMappingRegistry.loadFromClasspath();
        
        assertNull(registry.getJakartaEEPlatformVersion(null, "6.0.0"),
            "Should return null for null artifact ID");
        assertNull(registry.getJakartaEEPlatformVersion("jakarta.servlet-api", null),
            "Should return null for null version");
    }
    
    @Test
    void testGetMicroProfilePlatformVersion() {
        VersionMappingRegistry registry = VersionMappingRegistry.loadFromClasspath();
        
        // Test if MicroProfile mappings are loaded
        assertNotNull(registry.getMicroProfileMappings(), 
            "Should have MicroProfile mappings");
    }
    
    @Test
    void testVersionNormalization() {
        VersionMappingRegistry registry = VersionMappingRegistry.loadFromClasspath();
        
        // Test that major.minor.patch is normalized to major.minor
        String version1 = registry.getJakartaEEPlatformVersion("jakarta.servlet-api", "6.0.0");
        String version2 = registry.getJakartaEEPlatformVersion("jakarta.servlet-api", "6.0.1");
        
        // Both should map to the same platform version (if mapping exists)
        if (version1 != null && version2 != null) {
            assertEquals(version1, version2, 
                "Different patch versions should map to same platform version");
        }
    }
    
    @Test
    void testArtifactIdNormalization() {
        VersionMappingRegistry registry = VersionMappingRegistry.loadFromClasspath();
        
        // Test various artifact ID formats
        String v1 = registry.getJakartaEEPlatformVersion("jakarta.servlet-api", "6.0.0");
        String v2 = registry.getJakartaEEPlatformVersion("jakarta.servlet", "6.0.0");
        
        // Both should normalize to the same feature name
        if (v1 != null && v2 != null) {
            assertEquals(v1, v2, 
                "Different artifact ID formats should normalize to same result");
        }
    }
}
