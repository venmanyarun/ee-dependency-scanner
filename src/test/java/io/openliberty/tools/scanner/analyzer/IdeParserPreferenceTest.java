package io.openliberty.tools.scanner.analyzer;

import io.openliberty.tools.scanner.model.ClasspathAnalysisResult;
import io.openliberty.tools.scanner.model.DependencyInfo;
import io.openliberty.tools.scanner.model.DependencySource;
import io.openliberty.tools.scanner.parser.DependencyParser;
import io.openliberty.tools.scanner.parser.ParserException;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests IDE-backed parser preference over build-file parsing.
 */
class IdeParserPreferenceTest {

    @Test
    void testIdeProjectModelParserPreferredOverBuildFileParser() {
        DependencyParser ideParser = new StubParser(
            "IntelliJ Project Model",
            5,
            true,
            true,
            Collections.singletonList(dependency("jakarta.platform", "jakarta.jakartaee-api", "10.0.0", DependencySource.INTELLIJ))
        );

        DependencyParser mavenParser = new StubParser(
            "Maven",
            10,
            false,
            true,
            Collections.singletonList(dependency("jakarta.platform", "jakarta.jakartaee-api", "9.1.0", DependencySource.MAVEN))
        );

        ClasspathAnalyzer analyzer = new ClasspathAnalyzer(Arrays.asList(mavenParser, ideParser));
        ClasspathAnalysisResult result = analyzer.analyze(new File("."));

        assertEquals(1, result.getAllDependencies().size());
        assertEquals(DependencySource.MAVEN, result.getAllDependencies().get(0).getSource());
        assertEquals("IntelliJ Project Model, Maven", result.getDetectionMethod());
    }

    @Test
    void testBuildFileParserUsedWhenIdePreferenceDisabled() {
        DependencyParser ideParser = new StubParser(
            "IntelliJ Project Model",
            5,
            true,
            true,
            Collections.singletonList(dependency("jakarta.platform", "jakarta.jakartaee-api", "10.0.0", DependencySource.INTELLIJ))
        );

        DependencyParser gradleParser = new StubParser(
            "Gradle",
            20,
            false,
            true,
            Collections.singletonList(dependency("jakarta.platform", "jakarta.jakartaee-api", "9.1.0", DependencySource.GRADLE))
        );

        ClasspathAnalyzer analyzer = new ClasspathAnalyzer(Arrays.asList(gradleParser, ideParser));
        analyzer.setPreferIdeProjectModel(false);

        ClasspathAnalysisResult result = analyzer.analyze(new File("."));

        assertEquals(1, result.getAllDependencies().size());
        assertEquals(DependencySource.GRADLE, result.getAllDependencies().get(0).getSource());
        assertEquals("IntelliJ Project Model, Gradle", result.getDetectionMethod());
    }

    private static DependencyInfo dependency(String groupId, String artifactId, String version, DependencySource source) {
        return DependencyInfo.builder()
            .groupId(groupId)
            .artifactId(artifactId)
            .version(version)
            .source(source)
            .build();
    }

    private static final class StubParser implements DependencyParser {
        private final String name;
        private final int priority;
        private final boolean ideProjectModelParser;
        private final boolean canParse;
        private final List<DependencyInfo> dependencies;

        private StubParser(String name, int priority, boolean ideProjectModelParser, boolean canParse,
                           List<DependencyInfo> dependencies) {
            this.name = name;
            this.priority = priority;
            this.ideProjectModelParser = ideProjectModelParser;
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
        public String getParserName() {
            return name;
        }

        @Override
        public boolean isIdeProjectModelParser() {
            return ideProjectModelParser;
        }

        @Override
        public int getPriority() {
            return priority;
        }
    }
}


