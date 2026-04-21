package io.openliberty.tools.scanner.analyzer;

import io.openliberty.tools.scanner.api.DependencyAnalysisResult;
import io.openliberty.tools.scanner.api.DependencyInfo;
import io.openliberty.tools.scanner.api.DependencySource;
import io.openliberty.tools.scanner.api.ParserException;
import io.openliberty.tools.scanner.parser.CoreDependencyParser;
import io.openliberty.tools.scanner.parser.ParserTier;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests tiered parser execution and short-circuit behavior.
 */
class ParserTierExecutionTest {

    @Test
    void testIdeTierShortCircuitsBuildFileTier() {
        CoreDependencyParser<File> ideParser = new StubTierParser(
            "IntelliJ Project Model",
            ParserTier.IDE_MODEL,
            5,
            true,
            Collections.singletonList(dependency("jakarta.platform", "jakarta.jakartaee-api", "10.0.0", DependencySource.INTELLIJ))
        );

        CoreDependencyParser<File> mavenParser = new StubTierParser(
            "Maven",
            ParserTier.BUILD_FILE,
            10,
            true,
            Collections.singletonList(dependency("jakarta.platform", "jakarta.jakartaee-api", "9.1.0", DependencySource.MAVEN))
        );

        ClasspathAnalyzer analyzer = new ClasspathAnalyzer(Arrays.asList(mavenParser, ideParser));
        DependencyAnalysisResult result = analyzer.analyze(new File("."));

        assertEquals(1, result.getAllDependencies().size());
        assertEquals(DependencySource.INTELLIJ, result.getAllDependencies().get(0).getSource());
        assertEquals("IntelliJ Project Model", result.getDetectionMethod());
    }

    @Test
    void testBuildToolResolvedTierShortCircuitsBuildFileTier() {
        CoreDependencyParser<File> resolvedParser = new StubTierParser(
            "Maven Dependency Tree",
            ParserTier.BUILD_TOOL_RESOLVED,
            5,
            true,
            Collections.singletonList(dependency("jakarta.platform", "jakarta.jakartaee-api", "10.0.0", DependencySource.MAVEN))
        );

        CoreDependencyParser<File> gradleParser = new StubTierParser(
            "Gradle",
            ParserTier.BUILD_FILE,
            20,
            true,
            Collections.singletonList(dependency("jakarta.platform", "jakarta.jakartaee-api", "9.1.0", DependencySource.GRADLE))
        );

        ClasspathAnalyzer analyzer = new ClasspathAnalyzer(Arrays.asList(gradleParser, resolvedParser));
        DependencyAnalysisResult result = analyzer.analyze(new File("."));

        assertEquals(1, result.getAllDependencies().size());
        assertEquals(DependencySource.MAVEN, result.getAllDependencies().get(0).getSource());
        assertEquals("Maven Dependency Tree", result.getDetectionMethod());
    }

    @Test
    void testFallsBackToBuildFileTierWhenHigherTiersEmpty() {
        CoreDependencyParser<File> ideParser = new StubTierParser(
            "IntelliJ Project Model",
            ParserTier.IDE_MODEL,
            5,
            true,
            Collections.emptyList()
        );

        CoreDependencyParser<File> mavenParser = new StubTierParser(
            "Maven",
            ParserTier.BUILD_FILE,
            10,
            true,
            Collections.singletonList(dependency("jakarta.platform", "jakarta.jakartaee-api", "9.1.0", DependencySource.MAVEN))
        );

        ClasspathAnalyzer analyzer = new ClasspathAnalyzer(Arrays.asList(mavenParser, ideParser));
        DependencyAnalysisResult result = analyzer.analyze(new File("."));

        assertEquals(1, result.getAllDependencies().size());
        assertEquals(DependencySource.MAVEN, result.getAllDependencies().get(0).getSource());
        assertEquals("Maven", result.getDetectionMethod());
    }

    private static DependencyInfo dependency(String groupId, String artifactId, String version, DependencySource source) {
        return DependencyInfo.builder()
            .groupId(groupId)
            .artifactId(artifactId)
            .version(version)
            .source(source)
            .build();
    }

    private static final class StubTierParser implements CoreDependencyParser<File> {
        private final String name;
        private final ParserTier tier;
        private final int priority;
        private final boolean canParse;
        private final List<DependencyInfo> dependencies;

        private StubTierParser(String name, ParserTier tier, int priority, boolean canParse,
                               List<DependencyInfo> dependencies) {
            this.name = name;
            this.tier = tier;
            this.priority = priority;
            this.canParse = canParse;
            this.dependencies = dependencies;
        }

        @Override
        public List<DependencyInfo> parse(File path) throws ParserException {
            return dependencies;
        }

        @Override
        public boolean canParse(File path) {
            return canParse;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override

        public ParserTier getTier() {
            return tier;
        }

        @Override
        public boolean isIdeProjectModelParser() {
            return tier == ParserTier.IDE_MODEL;
        }

        @Override
        public int getPriority() {
            return priority;
        }
    }
}


