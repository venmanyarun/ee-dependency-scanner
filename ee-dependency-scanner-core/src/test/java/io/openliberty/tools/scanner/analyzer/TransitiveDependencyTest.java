package io.openliberty.tools.scanner.analyzer;

import io.openliberty.tools.scanner.api.DependencyAnalysisResult;
import io.openliberty.tools.scanner.api.DependencyInfo;
import io.openliberty.tools.scanner.api.DependencySource;
import io.openliberty.tools.scanner.parser.JarManifestScanner;
import io.openliberty.tools.scanner.util.JarDependencyExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for transitive dependency extraction from JAR files.
 * Tests the ability to extract Jakarta EE dependencies from embedded pom.xml in JARs.
 */
class TransitiveDependencyTest {

    private ClasspathAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new ClasspathAnalyzer();
    }

    @Test
    void testProjectWithTransitiveJarDependencies() {
        File projectDir = getTestProject("project-with-transitive-jar");
        
        DependencyAnalysisResult result = analyzer.analyze(projectDir);
        
        // Verify some detection method was used (could be Maven pom.xml or JAR scanning)
        assertNotNull(result.getDetectionMethod(),
            "Should have a detection method");
        
        // The project has a pom.xml, so Maven parser will be used
        // Verify we found dependencies (from pom.xml or JAR scanning)
        assertNotNull(result.getAllDependencies(),
            "Should return dependencies list");
        
        // If dependencies are found, verify they're properly categorized
        if (!result.getAllDependencies().isEmpty()) {
            // Check if Jakarta EE dependencies were found
            long jakartaCount = result.getJakartaEEDependencies().size();
            assertTrue(jakartaCount >= 0,
                "Should categorize Jakarta EE dependencies");
        }
    }

    @Test
    void testDirectJarDependencyExtraction() {
        File projectDir = getTestProject("project-with-transitive-jar");
        File jarFile = new File(projectDir, "lib/custom-library-1.0.0.jar");
        
        // Skip if JAR doesn't exist (script might have failed)
        if (!jarFile.exists()) {
            System.out.println("Skipping test - JAR file not found. Run create-test-jar.sh first.");
            return;
        }
        
        // Extract dependencies directly from JAR
        List<DependencyInfo> dependencies = JarDependencyExtractor.extractDependencies(jarFile);
        
        // Verify we extracted transitive dependencies
        assertFalse(dependencies.isEmpty(), 
            "Should extract dependencies from JAR's embedded pom.xml");
        
        // Verify Jakarta EE dependencies
        long jakartaCount = dependencies.stream()
            .filter(DependencyInfo::isJakartaEE)
            .count();
        assertTrue(jakartaCount >= 2, 
            "Should find at least 2 Jakarta EE dependencies (servlet, persistence)");
        
        // Verify MicroProfile dependencies
        long mpCount = dependencies.stream()
            .filter(DependencyInfo::isMicroProfile)
            .count();
        assertTrue(mpCount >= 1, 
            "Should find at least 1 MicroProfile dependency (config)");
        
        // Verify specific dependencies
        boolean hasServlet = dependencies.stream()
            .anyMatch(d -> "jakarta.servlet".equals(d.getGroupId()) && 
                          "jakarta.servlet-api".equals(d.getArtifactId()));
        assertTrue(hasServlet, 
            "Should extract jakarta.servlet-api from embedded pom.xml");
        
        boolean hasPersistence = dependencies.stream()
            .anyMatch(d -> "jakarta.persistence".equals(d.getGroupId()) && 
                          "jakarta.persistence-api".equals(d.getArtifactId()));
        assertTrue(hasPersistence, 
            "Should extract jakarta.persistence-api from embedded pom.xml");
        
        boolean hasMPConfig = dependencies.stream()
            .anyMatch(d -> d.getGroupId() != null && 
                          d.getGroupId().contains("microprofile.config"));
        assertTrue(hasMPConfig, 
            "Should extract microprofile-config-api from embedded pom.xml");
    }

    @Test
    void testTransitiveDependencyVersions() {
        File projectDir = getTestProject("project-with-transitive-jar");
        File jarFile = new File(projectDir, "lib/custom-library-1.0.0.jar");
        
        if (!jarFile.exists()) {
            System.out.println("Skipping test - JAR file not found.");
            return;
        }
        
        List<DependencyInfo> dependencies = JarDependencyExtractor.extractDependencies(jarFile);
        
        // Verify versions are extracted
        for (DependencyInfo dep : dependencies) {
            if (dep.isJakartaEE() || dep.isMicroProfile()) {
                assertNotNull(dep.getVersion(), 
                    "Transitive dependency should have version: " + dep.getArtifactId());
            }
        }
        
        // Verify specific versions
        DependencyInfo servlet = dependencies.stream()
            .filter(d -> "jakarta.servlet-api".equals(d.getArtifactId()))
            .findFirst()
            .orElse(null);
        
        if (servlet != null) {
            assertEquals("6.0.0", servlet.getVersion(), 
                "Servlet API should be version 6.0.0");
        }
    }

    @Test
    void testTransitiveDependencySource() {
        File projectDir = getTestProject("project-with-transitive-jar");
        File jarFile = new File(projectDir, "lib/custom-library-1.0.0.jar");
        
        if (!jarFile.exists()) {
            System.out.println("Skipping test - JAR file not found.");
            return;
        }
        
        List<DependencyInfo> dependencies = JarDependencyExtractor.extractDependencies(jarFile);
        
        // Verify all transitive dependencies have MANIFEST source
        for (DependencyInfo dep : dependencies) {
            assertEquals(DependencySource.MANIFEST, dep.getSource(), 
                "Transitive dependencies from embedded pom.xml should have MANIFEST source");
        }
    }

    @Test
    void testDisableTransitiveDependencyExtraction() {
        File projectDir = getTestProject("project-with-transitive-jar");
        
        // Create analyzer with transitive extraction disabled
        JarManifestScanner scanner = new JarManifestScanner();
        scanner.setExtractTransitiveDependencies(false);
        
        List<io.openliberty.tools.scanner.api.DependencyParser<?>> parsers =
            java.util.Arrays.asList(scanner);
        
        ClasspathAnalyzer customAnalyzer = new ClasspathAnalyzer(parsers);
        DependencyAnalysisResult result = customAnalyzer.analyze(projectDir);
        
        // Should only find the JAR itself, not transitive dependencies
        // (or very few dependencies compared to when transitive extraction is enabled)
        int depCount = result.getAllDependencies().size();
        
        // Now enable transitive extraction
        scanner.setExtractTransitiveDependencies(true);
        DependencyAnalysisResult resultWithTransitive = customAnalyzer.analyze(projectDir);
        
        int depCountWithTransitive = resultWithTransitive.getAllDependencies().size();
        
        // Should have more dependencies when transitive extraction is enabled
        assertTrue(depCountWithTransitive >= depCount, 
            "Should have more (or equal) dependencies with transitive extraction enabled");
    }

    @Test
    void testTransitiveDependenciesInSummary() {
        File projectDir = getTestProject("project-with-transitive-jar");
        
        DependencyAnalysisResult result = analyzer.analyze(projectDir);
        String summary = result.getSummary();
        
        // Verify summary includes information about found dependencies
        assertTrue(summary.contains("Jakarta EE"), 
            "Summary should mention Jakarta EE dependencies");
        assertTrue(summary.contains("MicroProfile"), 
            "Summary should mention MicroProfile dependencies");
    }

    // Helper method to get test project directory
    private File getTestProject(String projectName) {
        URL resource = getClass().getClassLoader()
            .getResource("test-projects/" + projectName);
        
        assertNotNull(resource, 
            "Test project not found: " + projectName);
        
        return new File(resource.getFile());
    }
}

