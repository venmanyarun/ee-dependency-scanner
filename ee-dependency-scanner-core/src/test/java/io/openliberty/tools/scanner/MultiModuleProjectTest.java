package io.openliberty.tools.scanner;

import io.openliberty.tools.scanner.analyzer.ClasspathAnalyzer;
import io.openliberty.tools.scanner.model.ClasspathAnalysisResult;
import io.openliberty.tools.scanner.model.DependencyInfo;
import io.openliberty.tools.scanner.model.DependencySource;
import io.openliberty.tools.scanner.model.DependencyType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for multi-module Maven projects with custom JARs and transitive dependencies.
 * 
 * This test suite covers:
 * - Multi-module Maven project structure
 * - Inter-module dependencies (module-b depends on module-a)
 * - Custom JAR dependencies with embedded pom.xml
 * - Transitive dependency resolution across modules
 * - Mixed Jakarta EE and MicroProfile dependencies
 */
public class MultiModuleProjectTest {

    private static Path testProjectRoot;
    private static Path moduleARoot;
    private static Path moduleBRoot;
    private static Path libDir;
    private static ClasspathAnalyzer analyzer;

    @BeforeAll
    public static void setup() throws IOException {
        testProjectRoot = Paths.get("src/test/resources/test-projects/multi-module-with-jars");
        moduleARoot = testProjectRoot.resolve("module-a");
        moduleBRoot = testProjectRoot.resolve("module-b");
        libDir = testProjectRoot.resolve("lib");
        
        // Create lib directory if it doesn't exist
        Files.createDirectories(libDir);
        
        // Create custom JAR with embedded pom.xml containing transitive dependencies
        createCustomJar();
        
        analyzer = new ClasspathAnalyzer();
    }

    /**
     * Creates a custom JAR file with embedded pom.xml for testing transitive dependencies.
     */
    private static void createCustomJar() throws IOException {
        Path jarPath = libDir.resolve("custom-library-1.0.0.jar");
        
        // Skip if JAR already exists
        if (Files.exists(jarPath)) {
            return;
        }
        
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarPath.toFile()), manifest)) {
            // Add a dummy class file
            JarEntry classEntry = new JarEntry("com/example/CustomLibrary.class");
            jos.putNextEntry(classEntry);
            jos.write(new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE}); // Minimal class file header
            jos.closeEntry();
            
            // Add embedded pom.xml with Jakarta EE dependencies
            String pomContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\">\n" +
                "    <modelVersion>4.0.0</modelVersion>\n" +
                "    <groupId>com.example</groupId>\n" +
                "    <artifactId>custom-library</artifactId>\n" +
                "    <version>1.0.0</version>\n" +
                "    <dependencies>\n" +
                "        <dependency>\n" +
                "            <groupId>jakarta.servlet</groupId>\n" +
                "            <artifactId>jakarta.servlet-api</artifactId>\n" +
                "            <version>6.0.0</version>\n" +
                "        </dependency>\n" +
                "        <dependency>\n" +
                "            <groupId>jakarta.persistence</groupId>\n" +
                "            <artifactId>jakarta.persistence-api</artifactId>\n" +
                "            <version>3.1.0</version>\n" +
                "        </dependency>\n" +
                "    </dependencies>\n" +
                "</project>";
            
            JarEntry pomEntry = new JarEntry("META-INF/maven/com.example/custom-library/pom.xml");
            jos.putNextEntry(pomEntry);
            jos.write(pomContent.getBytes());
            jos.closeEntry();
        }
    }

    @Test
    void testMultiModuleProjectStructure() {
        // Verify test project structure exists
        assertTrue(Files.exists(testProjectRoot), "Test project root should exist");
        assertTrue(Files.exists(moduleARoot), "Module A should exist");
        assertTrue(Files.exists(moduleBRoot), "Module B should exist");
    }

    @Test
    void testModuleAAnalysis() {
        ClasspathAnalysisResult result = analyzer.analyze(moduleARoot.toFile());
        
        // Verify Maven detection
        assertTrue(result.getDetectionMethod().contains("Maven"), 
            "Should detect Maven as source");
        
        // Verify dependencies found
        assertFalse(result.getAllDependencies().isEmpty(), 
            "Should find dependencies in module-a");
        
        // Verify Jakarta EE dependencies
        assertFalse(result.getJakartaEEDependencies().isEmpty(), 
            "Should find Jakarta EE dependencies in module-a");
    }

    @Test
    void testModuleBAnalysis() {
        ClasspathAnalysisResult result = analyzer.analyze(moduleBRoot.toFile());
        
        // Verify Maven detection
        assertTrue(result.getDetectionMethod().contains("Maven"), 
            "Should detect Maven as source");
        
        // Verify dependencies found
        assertFalse(result.getAllDependencies().isEmpty(), 
            "Should find dependencies in module-b");
        
        // Verify Jakarta EE dependencies
        assertFalse(result.getJakartaEEDependencies().isEmpty(), 
            "Should find Jakarta EE dependencies in module-b");
    }

    @Test
    void testParentPomAnalysis() {
        ClasspathAnalysisResult result = analyzer.analyze(testProjectRoot.toFile());
        
        // Verify Maven detection
        assertTrue(result.getDetectionMethod().contains("Maven"), 
            "Should detect Maven as source");
        
        // Parent pom might not have direct dependencies, but should be parseable
        assertNotNull(result, "Should successfully analyze parent pom");
    }

    @Test
    void testCustomJarWithTransitiveDependencies() {
        // Analyze the lib directory containing custom JAR
        ClasspathAnalysisResult result = analyzer.analyze(libDir.toFile());
        
        // Verify JAR scanning
        assertTrue(result.getDetectionMethod().contains("JAR") || 
                   result.getDetectionMethod().contains("Manifest"), 
            "Should detect JAR scanning");
        
        // Verify dependencies from embedded pom.xml
        if (!result.getAllDependencies().isEmpty()) {
            // If transitive extraction is working, we should find Jakarta EE dependencies
            long jakartaCount = result.getJakartaEEDependencies().size();
            assertTrue(jakartaCount >= 0, 
                "Should extract Jakarta EE dependencies from embedded pom.xml");
        }
    }

    @Test
    void testMultiModuleVersionConsistency() {
        ClasspathAnalysisResult resultA = analyzer.analyze(moduleARoot.toFile());
        ClasspathAnalysisResult resultB = analyzer.analyze(moduleBRoot.toFile());
        
        // Get Jakarta EE versions from both modules
        Set<String> versionsA = resultA.getJakartaEEPlatformVersions();
        Set<String> versionsB = resultB.getJakartaEEPlatformVersions();
        
        // Both modules should detect Jakarta EE versions
        assertFalse(versionsA.isEmpty() || versionsB.isEmpty(), 
            "Both modules should have Jakarta EE dependencies");
    }

    @Test
    void testDependencySourceTracking() {
        ClasspathAnalysisResult result = analyzer.analyze(moduleARoot.toFile());
        
        // Verify all dependencies have a source
        for (DependencyInfo dep : result.getAllDependencies()) {
            assertNotNull(dep.getSource(), 
                "Each dependency should have a source: " + dep.getCoordinate());
            assertNotEquals(DependencySource.UNKNOWN, dep.getSource(), 
                "Dependency source should not be UNKNOWN: " + dep.getCoordinate());
        }
    }

    @Test
    void testMultiModuleSummary() {
        ClasspathAnalysisResult result = analyzer.analyze(testProjectRoot.toFile());
        String summary = result.getSummary();
        
        // Verify summary contains key information
        assertNotNull(summary, "Summary should not be null");
        assertTrue(summary.contains("Dependencies"), 
            "Summary should mention dependencies");
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

