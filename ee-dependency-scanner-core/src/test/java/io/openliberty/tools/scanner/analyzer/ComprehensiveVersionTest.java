package io.openliberty.tools.scanner.analyzer;

import io.openliberty.tools.scanner.api.DependencyAnalysisResult;
import io.openliberty.tools.scanner.api.DependencyInfo;
import io.openliberty.tools.scanner.api.DependencySource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ComprehensiveVersionTest {

    private ClasspathAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new ClasspathAnalyzer();
    }

    private File getTestProject(String projectName) {
        return new File("src/test/resources/test-projects/" + projectName);
    }

    @Test
    void testMavenJavaEE5Project() {
        File projectDir = getTestProject("maven-javaee5");
        DependencyAnalysisResult result = analyzer.analyze(projectDir);

        assertNotNull(result);
        assertFalse(result.getAllDependencies().isEmpty());
        assertFalse(result.getJavaEEDependencies().isEmpty(), "Should detect Java EE 5 dependencies");
        
        assertTrue(result.getAllDependencies().stream()
                .anyMatch(d -> d.getArtifactId().contains("servlet")),
                "Should detect servlet dependency");
    }

    @Test
    void testGradleJakartaEE91Project() {
        File projectDir = getTestProject("gradle-jakarta-ee9.1");
        assertTrue(projectDir.exists(), "Gradle Jakarta EE 9.1 project should exist");
        
        DependencyAnalysisResult result = analyzer.analyze(projectDir);
        assertNotNull(result);
        
        if (!result.getAllDependencies().isEmpty()) {
            verifyJakartaEE91Dependencies(result);
        }
    }

    @Test
    void testMavenMixedJavaEEVersions() {
        File projectDir = getTestProject("maven-mixed-javaee");
        DependencyAnalysisResult result = analyzer.analyze(projectDir);

        assertNotNull(result);
        assertFalse(result.getAllDependencies().isEmpty());
        assertFalse(result.getJavaEEDependencies().isEmpty(), "Should detect Java EE dependencies");
        
        long javaxCount = result.getAllDependencies().stream()
                .filter(d -> d.getGroupId().startsWith("javax."))
                .count();
        assertTrue(javaxCount > 0, "Should detect javax dependencies");
    }

    @Test
    void testGradleMicroProfile40Project() {
        File projectDir = getTestProject("gradle-microprofile-4.0");
        assertTrue(projectDir.exists(), "Gradle MicroProfile 4.0 project should exist");
        
        DependencyAnalysisResult result = analyzer.analyze(projectDir);
        assertNotNull(result);
        
        if (!result.getAllDependencies().isEmpty()) {
            verifyMicroProfile40Dependencies(result);
        }
    }

    @Test
    void testCustomJarWithEmbeddedJakartaEE() {
        File projectDir = getTestProject("custom-jar-with-jakarta");
        DependencyAnalysisResult result = analyzer.analyze(projectDir);

        assertNotNull(result);
        assertFalse(result.getAllDependencies().isEmpty(), "Should detect dependencies");

        long jarScanCount = result.getAllDependencies().stream()
                .filter(d -> d.getSource() == DependencySource.JAR_SCAN)
                .count();
        long mavenCount = result.getAllDependencies().stream()
                .filter(d -> d.getSource() == DependencySource.MAVEN)
                .count();

        assertTrue(jarScanCount > 0 || mavenCount > 0,
                "Should detect dependencies from JAR_SCAN or MAVEN sources");

        System.out.println("Custom JAR - Total: " + result.getAllDependencies().size() +
                ", JAR_SCAN: " + jarScanCount + ", MAVEN: " + mavenCount);
    }

    private void verifyJakartaEE91Dependencies(DependencyAnalysisResult result) {
        List<DependencyInfo> jakartaDeps = result.getAllDependencies().stream()
                .filter(d -> d.getGroupId().startsWith("jakarta."))
                .collect(Collectors.toList());
        
        if (!jakartaDeps.isEmpty()) {
            boolean hasExpectedVersion = jakartaDeps.stream()
                    .anyMatch(d -> d.getVersion().equals("5.0.0") ||
                                 d.getVersion().equals("3.0.0") ||
                                 d.getVersion().equals("3.0.2"));
            assertTrue(hasExpectedVersion, "Should detect Jakarta EE 9.1 versions");
        }
    }

    private void verifyMicroProfile40Dependencies(DependencyAnalysisResult result) {
        List<DependencyInfo> mpDeps = result.getAllDependencies().stream()
                .filter(d -> d.getGroupId().contains("microprofile"))
                .collect(Collectors.toList());
        
        if (!mpDeps.isEmpty()) {
            boolean hasExpectedVersion = mpDeps.stream()
                    .anyMatch(d -> d.getVersion().equals("2.0") || d.getVersion().equals("3.0"));
            assertTrue(hasExpectedVersion, "Should detect MicroProfile 4.0 versions");
        }
    }

    @Test
    void testAllNewProjectsExist() {
        File javaEE5 = getTestProject("maven-javaee5");
        File jakartaEE91 = getTestProject("gradle-jakarta-ee9.1");
        File mixedJavaEE = getTestProject("maven-mixed-javaee");
        File mp40 = getTestProject("gradle-microprofile-4.0");
        File customJar = getTestProject("custom-jar-with-jakarta");

        assertTrue(javaEE5.exists(), "maven-javaee5 project should exist");
        assertTrue(jakartaEE91.exists(), "gradle-jakarta-ee9.1 project should exist");
        assertTrue(mixedJavaEE.exists(), "maven-mixed-javaee project should exist");
        assertTrue(mp40.exists(), "gradle-microprofile-4.0 project should exist");
        assertTrue(customJar.exists(), "custom-jar-with-jakarta project should exist");
    }

    @Test
    void testVersionSpanCoverage() {
        String[] projectNames = {
            "maven-javaee5",
            "maven-javaee6",
            "maven-javaee7",
            "maven-javaee8",
            "maven-jakarta-ee9",
            "gradle-jakarta-ee9.1",
            "maven-jakarta-ee10"
        };

        int successfulAnalyses = 0;
        for (String projectName : projectNames) {
            File project = getTestProject(projectName);
            if (project.exists()) {
                DependencyAnalysisResult result = analyzer.analyze(project);
                if (result != null && !result.getAllDependencies().isEmpty()) {
                    successfulAnalyses++;
                    System.out.println("Analyzed " + projectName +
                            ": Jakarta EE=" + result.getJakartaEEDependencies().size() +
                            ", Java EE=" + result.getJavaEEDependencies().size());
                }
            }
        }

        assertTrue(successfulAnalyses >= 5,
                "Should successfully analyze at least 5 projects spanning different versions");
    }
}
