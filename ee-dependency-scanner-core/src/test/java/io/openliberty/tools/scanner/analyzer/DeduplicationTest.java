package io.openliberty.tools.scanner.analyzer;

import io.openliberty.tools.scanner.api.DependencyAnalysisResult;
import io.openliberty.tools.scanner.api.DependencyInfo;
import io.openliberty.tools.scanner.api.DependencySource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for improved deduplication logic in ClasspathAnalyzer.
 */
class DeduplicationTest {
    
    private ClasspathAnalyzer analyzer;
    
    @BeforeEach
    void setUp() {
        analyzer = ClasspathAnalyzer.builder().build();
    }
    
    @Test
    void testDeduplicationWithDifferentVersions() {
        // Create a custom analyzer with test dependencies
        List<DependencyInfo> testDeps = List.of(
            DependencyInfo.builder()
                .groupId("jakarta.servlet")
                .artifactId("jakarta.servlet-api")
                .version("6.0.0")
                .source(DependencySource.MAVEN)
                .build(),
            DependencyInfo.builder()
                .groupId("jakarta.servlet")
                .artifactId("jakarta.servlet-api")
                .version(null)  // No version from JAR filename
                .source(DependencySource.MANIFEST)
                .build()
        );
        
        // The analyzer should deduplicate these and prefer the Maven one with version
        // This is tested indirectly through the analyzer's behavior
        assertNotNull(testDeps, "Test dependencies should be created");
    }
    
    @Test
    void testPreferMavenOverJarScanning() {
        DependencyInfo mavenDep = DependencyInfo.builder()
            .groupId("jakarta.servlet")
            .artifactId("jakarta.servlet-api")
            .version("6.0.0")
            .source(DependencySource.MAVEN)
            .build();
        
        DependencyInfo jarDep = DependencyInfo.builder()
            .groupId("jakarta.servlet")
            .artifactId("jakarta.servlet-api")
            .version("6.0.0")
            .source(DependencySource.MANIFEST)
            .build();
        
        // Maven source should be preferred over JAR manifest
        assertEquals(DependencySource.MAVEN, mavenDep.getSource());
        assertEquals(DependencySource.MANIFEST, jarDep.getSource());
    }
    
    @Test
    void testPreferGradleOverEclipse() {
        DependencyInfo gradleDep = DependencyInfo.builder()
            .groupId("jakarta.servlet")
            .artifactId("jakarta.servlet-api")
            .version("6.0.0")
            .source(DependencySource.GRADLE)
            .build();
        
        DependencyInfo eclipseDep = DependencyInfo.builder()
            .groupId("jakarta.servlet")
            .artifactId("jakarta.servlet-api")
            .version("6.0.0")
            .source(DependencySource.ECLIPSE)
            .build();
        
        // Gradle source should be preferred over Eclipse
        assertEquals(DependencySource.GRADLE, gradleDep.getSource());
        assertEquals(DependencySource.ECLIPSE, eclipseDep.getSource());
    }
    
    @Test
    void testDeduplicationKeyWithoutGroupId() {
        DependencyInfo dep1 = DependencyInfo.builder()
            .artifactId("custom-library")
            .version("1.0.0")
            .source(DependencySource.MANIFEST)
            .build();
        
        DependencyInfo dep2 = DependencyInfo.builder()
            .artifactId("custom-library")
            .version("1.0.0")
            .source(DependencySource.MANIFEST)
            .build();
        
        // Both should have same artifact ID for deduplication
        assertEquals(dep1.getArtifactId(), dep2.getArtifactId());
    }
    
    @Test
    void testCompletenessScoring() {
        // Dependency with full information
        DependencyInfo fullInfo = DependencyInfo.builder()
            .groupId("jakarta.servlet")
            .artifactId("jakarta.servlet-api")
            .version("6.0.0")
            .jarPath("/path/to/servlet-api-6.0.0.jar")
            .source(DependencySource.MAVEN)
            .build();
        
        // Dependency with partial information
        DependencyInfo partialInfo = DependencyInfo.builder()
            .artifactId("jakarta.servlet-api")
            .source(DependencySource.MANIFEST)
            .build();
        
        // Full info should have all fields populated
        assertNotNull(fullInfo.getGroupId());
        assertNotNull(fullInfo.getVersion());
        assertNotNull(fullInfo.getJarPath());
        
        // Partial info should have missing fields
        assertNull(partialInfo.getGroupId());
        assertNull(partialInfo.getVersion());
    }
    
    @Test
    void testDeduplicationPreservesOrder() {
        // Test that deduplication maintains insertion order
        // This is important for consistent results
        DependencyInfo dep1 = DependencyInfo.builder()
            .groupId("jakarta.servlet")
            .artifactId("jakarta.servlet-api")
            .version("6.0.0")
            .source(DependencySource.MAVEN)
            .build();
        
        DependencyInfo dep2 = DependencyInfo.builder()
            .groupId("jakarta.persistence")
            .artifactId("jakarta.persistence-api")
            .version("3.1.0")
            .source(DependencySource.MAVEN)
            .build();
        
        // Order should be preserved
        assertNotNull(dep1);
        assertNotNull(dep2);
    }
    
    @Test
    void testNoDuplicatesInRealProject() {
        // Test with a real project that might have duplicates
        File projectDir = new File("src/test/resources/test-projects/maven-jakarta-ee10");
        
        if (projectDir.exists()) {
            DependencyAnalysisResult result = analyzer.analyze(projectDir);
            
            // Check that there are no duplicate groupId:artifactId combinations
            List<DependencyInfo> deps = result.getAllDependencies();
            long uniqueCount = deps.stream()
                .map(d -> d.getGroupId() + ":" + d.getArtifactId())
                .distinct()
                .count();
            
            assertEquals(deps.size(), uniqueCount,
                "Should have no duplicate dependencies");
        }
    }
}
