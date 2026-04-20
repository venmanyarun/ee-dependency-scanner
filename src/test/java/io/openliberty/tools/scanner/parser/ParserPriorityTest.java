package io.openliberty.tools.scanner.parser;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Tests for parser priority system to ensure correct ordering.
 */
class ParserPriorityTest {
    
    @Test
    void testParserPriorityValues() {
        MavenPomParser mavenParser = new MavenPomParser();
        GradleBuildParser gradleParser = new GradleBuildParser();
        EclipseClasspathParser eclipseParser = new EclipseClasspathParser();
        JarManifestScanner jarScanner = new JarManifestScanner();
        
        // Verify priority values match documented ranges
        assertEquals(10, mavenParser.getPriority(), 
            "Maven parser should have priority 10");
        assertEquals(20, gradleParser.getPriority(), 
            "Gradle parser should have priority 20");
        assertEquals(100, eclipseParser.getPriority(), 
            "Eclipse parser should have priority 100");
        assertEquals(200, jarScanner.getPriority(), 
            "JAR scanner should have priority 200");
    }
    
    @Test
    void testParserComparison() {
        MavenPomParser mavenParser = new MavenPomParser();
        GradleBuildParser gradleParser = new GradleBuildParser();
        EclipseClasspathParser eclipseParser = new EclipseClasspathParser();
        JarManifestScanner jarScanner = new JarManifestScanner();
        
        // Maven should come before Gradle
        assertTrue(mavenParser.compareTo(gradleParser) < 0,
            "Maven should have higher priority than Gradle");
        
        // Gradle should come before Eclipse
        assertTrue(gradleParser.compareTo(eclipseParser) < 0,
            "Gradle should have higher priority than Eclipse");
        
        // Eclipse should come before JAR scanner
        assertTrue(eclipseParser.compareTo(jarScanner) < 0,
            "Eclipse should have higher priority than JAR scanner");
    }
    
    @Test
    void testParserSorting() {
        // Create parsers in reverse priority order
        List<DependencyParser> parsers = Arrays.asList(
            new JarManifestScanner(),      // priority 200
            new EclipseClasspathParser(),  // priority 100
            new GradleBuildParser(),       // priority 20
            new MavenPomParser()           // priority 10
        );
        
        // Sort by priority
        parsers.sort(DependencyParser::compareTo);
        
        // Verify correct order
        assertTrue(parsers.get(0) instanceof MavenPomParser,
            "Maven should be first after sorting");
        assertTrue(parsers.get(1) instanceof GradleBuildParser,
            "Gradle should be second after sorting");
        assertTrue(parsers.get(2) instanceof EclipseClasspathParser,
            "Eclipse should be third after sorting");
        assertTrue(parsers.get(3) instanceof JarManifestScanner,
            "JAR scanner should be last after sorting");
    }
    
    @Test
    void testParserNames() {
        MavenPomParser mavenParser = new MavenPomParser();
        GradleBuildParser gradleParser = new GradleBuildParser();
        EclipseClasspathParser eclipseParser = new EclipseClasspathParser();
        JarManifestScanner jarScanner = new JarManifestScanner();
        
        assertEquals("Maven", mavenParser.getParserName());
        assertEquals("Gradle", gradleParser.getParserName());
        assertEquals("Eclipse", eclipseParser.getParserName());
        assertEquals("JAR Scanner", jarScanner.getParserName());
    }
}
