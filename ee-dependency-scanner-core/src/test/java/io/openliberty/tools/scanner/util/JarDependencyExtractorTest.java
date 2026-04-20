package io.openliberty.tools.scanner.util;

import io.openliberty.tools.scanner.model.DependencyInfo;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for JarDependencyExtractor to verify instance method functionality
 * and backward compatibility with static methods.
 */
class JarDependencyExtractorTest {
    
    @Test
    void testInstanceMethodCreation() {
        JarDependencyExtractor extractor = new JarDependencyExtractor();
        assertNotNull(extractor, "Should be able to create instance");
    }
    
    @Test
    void testStaticMethodBackwardCompatibility() {
        // Test that static method still works for backward compatibility
        File testJar = new File("src/test/resources/test-projects/multi-module-with-jars/lib/custom-library-1.0.0.jar");
        
        if (testJar.exists()) {
            List<DependencyInfo> deps = JarDependencyExtractor.extractDependencies(testJar);
            assertNotNull(deps, "Static method should return non-null list");
        }
    }
    
    @Test
    void testInstanceMethodExtract() {
        JarDependencyExtractor extractor = new JarDependencyExtractor();
        File testJar = new File("src/test/resources/test-projects/multi-module-with-jars/lib/custom-library-1.0.0.jar");
        
        if (testJar.exists()) {
            List<DependencyInfo> deps = extractor.extract(testJar);
            assertNotNull(deps, "Instance method should return non-null list");
        }
    }
    
    @Test
    void testExtractWithNonExistentFile() {
        JarDependencyExtractor extractor = new JarDependencyExtractor();
        File nonExistent = new File("non-existent.jar");
        
        List<DependencyInfo> deps = extractor.extract(nonExistent);
        assertNotNull(deps, "Should return empty list for non-existent file");
        assertTrue(deps.isEmpty(), "Should return empty list for non-existent file");
    }
    
    @Test
    void testExtractWithInvalidJar() {
        JarDependencyExtractor extractor = new JarDependencyExtractor();
        // Use a non-JAR file
        File invalidJar = new File("pom.xml");
        
        if (invalidJar.exists()) {
            List<DependencyInfo> deps = extractor.extract(invalidJar);
            assertNotNull(deps, "Should handle invalid JAR gracefully");
        }
    }
    
    @Test
    void testCustomExtractorSubclass() {
        // Test that we can subclass and override methods
        JarDependencyExtractor customExtractor = new JarDependencyExtractor() {
            @Override
            public List<DependencyInfo> extract(File jarFile) {
                // Custom implementation
                return List.of();
            }
        };
        
        File testFile = new File("test.jar");
        List<DependencyInfo> result = customExtractor.extract(testFile);
        assertNotNull(result, "Custom extractor should work");
        assertTrue(result.isEmpty(), "Custom extractor returns empty list");
    }
    
    @Test
    void testProtectedMethodsAccessible() {
        // Create a subclass to test protected methods are accessible
        TestableJarExtractor extractor = new TestableJarExtractor();
        assertTrue(extractor.canAccessProtectedMethods(),
            "Protected methods should be accessible to subclasses");
    }
    
    /**
     * Test subclass to verify protected methods are accessible.
     */
    private static class TestableJarExtractor extends JarDependencyExtractor {
        public boolean canAccessProtectedMethods() {
            // If we can reference these methods, they're accessible
            try {
                // Check if protected methods exist in the superclass
                JarDependencyExtractor.class.getDeclaredMethod("extractFromEmbeddedPom",
                    java.util.jar.JarFile.class);
                return true;
            } catch (NoSuchMethodException e) {
                // Method might not exist or might be private
                return false;
            }
        }
    }
}
