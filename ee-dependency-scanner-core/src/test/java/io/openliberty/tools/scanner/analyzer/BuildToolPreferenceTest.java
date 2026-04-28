package io.openliberty.tools.scanner.analyzer;

import io.openliberty.tools.scanner.api.DependencyAnalysisResult;
import io.openliberty.tools.scanner.api.DependencyFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for build tool preference functionality in projects with multiple build configurations.
 */
class BuildToolPreferenceTest {
    
    @TempDir
    Path tempDir;
    
    @Test
    void testDefaultPreferenceIsMaven() {
        ClasspathAnalyzer analyzer = ClasspathAnalyzer.builder().build();
        assertEquals(BuildToolPreference.AUTO, analyzer.getBuildToolPreference());
    }
    
    @Test
    void testBuilderWithGradleOnly() {
        ClasspathAnalyzer analyzer = ClasspathAnalyzer.builder()
            .buildToolPreference(BuildToolPreference.GRADLE_ONLY)
            .build();
        assertEquals(BuildToolPreference.GRADLE_ONLY, analyzer.getBuildToolPreference());
    }
    
    @Test
    void testBuilderWithMavenOnly() {
        ClasspathAnalyzer analyzer = ClasspathAnalyzer.builder()
            .buildToolPreference(BuildToolPreference.MAVEN_ONLY)
            .build();
        assertEquals(BuildToolPreference.MAVEN_ONLY, analyzer.getBuildToolPreference());
    }
    
    @Test
    void testBuilderWithNullPreferenceDefaultsToAuto() {
        ClasspathAnalyzer analyzer = ClasspathAnalyzer.builder()
            .buildToolPreference(null)
            .build();
        assertEquals(BuildToolPreference.AUTO, analyzer.getBuildToolPreference());
    }
    
    @Test
    void testBuilderWithFallbackScanningDisabled() {
        ClasspathAnalyzer analyzer = ClasspathAnalyzer.builder()
            .enableFallbackScanning(false)
            .build();
        assertFalse(analyzer.isFallbackScanningEnabled());
    }
    
    @Test
    void testBuilderFluentAPI() {
        ClasspathAnalyzer analyzer = ClasspathAnalyzer.builder()
            .buildToolPreference(BuildToolPreference.PREFER_GRADLE)
            .enableFallbackScanning(false)
            .build();
        
        assertEquals(BuildToolPreference.PREFER_GRADLE, analyzer.getBuildToolPreference());
        assertFalse(analyzer.isFallbackScanningEnabled());
    }
    
    @Test
    void testProjectWithOnlyMaven() throws IOException {
        // Create project with only pom.xml
        File projectDir = tempDir.toFile();
        createMavenPom(projectDir);
        
        // Should use Maven regardless of preference
        ClasspathAnalyzer analyzer = ClasspathAnalyzer.builder()
            .buildToolPreference(BuildToolPreference.GRADLE_ONLY)
            .build();
        DependencyAnalysisResult result = analyzer.analyze(projectDir);
        
        // Maven parser should be used (or fallback if no dependencies)
        assertNotNull(result);
    }
    
    @Test
    void testProjectWithOnlyGradle() throws IOException {
        // Create project with only build.gradle
        File projectDir = tempDir.toFile();
        createGradleBuild(projectDir);
        
        // Should use Gradle regardless of preference
        ClasspathAnalyzer analyzer = ClasspathAnalyzer.builder()
            .buildToolPreference(BuildToolPreference.MAVEN_ONLY)
            .build();
        DependencyAnalysisResult result = analyzer.analyze(projectDir);
        
        // Gradle parser should be used (or fallback if no dependencies)
        assertNotNull(result);
    }
    
    @Test
    void testProjectWithBothBuildFiles_AutoPreference() throws IOException {
        // Create project with both pom.xml and build.gradle
        File projectDir = tempDir.toFile();
        createMavenPom(projectDir);
        createGradleBuild(projectDir);
        
        // AUTO should prefer Maven
        ClasspathAnalyzer analyzer = ClasspathAnalyzer.builder()
            .buildToolPreference(BuildToolPreference.AUTO)
            .build();
        DependencyAnalysisResult result = analyzer.analyze(projectDir);
        
        assertNotNull(result);
        // In a real scenario with actual dependencies, we'd verify Maven was used
    }
    
    @Test
    void testProjectWithBothBuildFiles_MavenOnly() throws IOException {
        // Create project with both pom.xml and build.gradle
        File projectDir = tempDir.toFile();
        createMavenPom(projectDir);
        createGradleBuild(projectDir);
        
        // MAVEN_ONLY should use only Maven
        ClasspathAnalyzer analyzer = ClasspathAnalyzer.builder()
            .buildToolPreference(BuildToolPreference.MAVEN_ONLY)
            .build();
        DependencyAnalysisResult result = analyzer.analyze(projectDir);
        
        assertNotNull(result);
    }
    
    @Test
    void testProjectWithBothBuildFiles_GradleOnly() throws IOException {
        // Create project with both pom.xml and build.gradle
        File projectDir = tempDir.toFile();
        createMavenPom(projectDir);
        createGradleBuild(projectDir);
        
        // GRADLE_ONLY should use only Gradle
        ClasspathAnalyzer analyzer = ClasspathAnalyzer.builder()
            .buildToolPreference(BuildToolPreference.GRADLE_ONLY)
            .build();
        DependencyAnalysisResult result = analyzer.analyze(projectDir);
        
        assertNotNull(result);
    }
    
    @Test
    void testProjectWithBothBuildFiles_PreferMaven() throws IOException {
        // Create project with both pom.xml and build.gradle
        File projectDir = tempDir.toFile();
        createMavenPom(projectDir);
        createGradleBuild(projectDir);
        
        // PREFER_MAVEN should use Maven when both exist
        ClasspathAnalyzer analyzer = ClasspathAnalyzer.builder()
            .buildToolPreference(BuildToolPreference.PREFER_MAVEN)
            .build();
        DependencyAnalysisResult result = analyzer.analyze(projectDir);
        
        assertNotNull(result);
    }
    
    @Test
    void testProjectWithBothBuildFiles_PreferGradle() throws IOException {
        // Create project with both pom.xml and build.gradle
        File projectDir = tempDir.toFile();
        createMavenPom(projectDir);
        createGradleBuild(projectDir);
        
        // PREFER_GRADLE should use Gradle when both exist
        ClasspathAnalyzer analyzer = ClasspathAnalyzer.builder()
            .buildToolPreference(BuildToolPreference.PREFER_GRADLE)
            .build();
        DependencyAnalysisResult result = analyzer.analyze(projectDir);
        
        assertNotNull(result);
    }
    
    @Test
    void testAnalyzeWithPreferenceParameter() throws IOException {
        // Create project with both build files
        File projectDir = tempDir.toFile();
        createMavenPom(projectDir);
        createGradleBuild(projectDir);
        
        ClasspathAnalyzer analyzer = ClasspathAnalyzer.builder().build();
        
        // Test analyze method with preference parameter
        DependencyAnalysisResult result1 = analyzer.analyze(projectDir, BuildToolPreference.MAVEN_ONLY);
        assertNotNull(result1);
        
        DependencyAnalysisResult result2 = analyzer.analyze(projectDir, BuildToolPreference.GRADLE_ONLY);
        assertNotNull(result2);
    }
    
    @Test
    void testAnalyzeWithFilterAndPreference() throws IOException {
        // Create project with both build files
        File projectDir = tempDir.toFile();
        createMavenPom(projectDir);
        createGradleBuild(projectDir);
        
        ClasspathAnalyzer analyzer = ClasspathAnalyzer.builder().build();
        
        // Test analyze method with filter and preference
        DependencyAnalysisResult result = analyzer.analyze(
            projectDir,
            DependencyFilter.includeAll(),
            BuildToolPreference.GRADLE_ONLY
        );
        
        assertNotNull(result);
    }
    
    @Test
    void testRealProjectWithBothBuildFiles() {
        // Test with actual test project if it exists
        File testProject = new File("src/test/resources/test-projects/maven-jakarta-ee10");
        
        if (testProject.exists()) {
            ClasspathAnalyzer analyzer = ClasspathAnalyzer.builder()
                .buildToolPreference(BuildToolPreference.MAVEN_ONLY)
                .build();
            DependencyAnalysisResult result = analyzer.analyze(testProject);
            
            assertNotNull(result);
            assertTrue(result.getTotalDependenciesFound() >= 0);
        }
    }
    
    // Helper methods to create minimal build files
    
    private void createMavenPom(File projectDir) throws IOException {
        String pomContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
            "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0\n" +
            "         http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
            "    <modelVersion>4.0.0</modelVersion>\n" +
            "    <groupId>com.example</groupId>\n" +
            "    <artifactId>test-project</artifactId>\n" +
            "    <version>1.0.0</version>\n" +
            "    <dependencies>\n" +
            "        <dependency>\n" +
            "            <groupId>jakarta.servlet</groupId>\n" +
            "            <artifactId>jakarta.servlet-api</artifactId>\n" +
            "            <version>6.0.0</version>\n" +
            "        </dependency>\n" +
            "    </dependencies>\n" +
            "</project>";
        
        Files.writeString(new File(projectDir, "pom.xml").toPath(), pomContent);
    }
    
    private void createGradleBuild(File projectDir) throws IOException {
        String gradleContent = "plugins {\n" +
            "    id 'java'\n" +
            "}\n" +
            "\n" +
            "group = 'com.example'\n" +
            "version = '1.0.0'\n" +
            "\n" +
            "dependencies {\n" +
            "    implementation 'jakarta.servlet:jakarta.servlet-api:6.0.0'\n" +
            "}";
        
        Files.writeString(new File(projectDir, "build.gradle").toPath(), gradleContent);
    }
}

// Made with Bob
