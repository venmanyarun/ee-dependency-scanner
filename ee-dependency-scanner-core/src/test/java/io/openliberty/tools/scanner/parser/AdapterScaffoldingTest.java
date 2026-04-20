package io.openliberty.tools.scanner.parser;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests adapter scaffolding without introducing core test dependencies on adapter modules.
 */
class AdapterScaffoldingTest {

    @Test
    void testIntelliJAdapterScaffoldingFilesExist() throws Exception {
        assertTrue(Files.exists(Path.of("../scanner-intellij-adapter/src/main/java/io/openliberty/tools/scanner/intellij/IntelliJDependencyParserProvider.java")));
        assertTrue(Files.exists(Path.of("../scanner-intellij-adapter/src/main/java/io/openliberty/tools/scanner/intellij/IntelliJProjectModelParser.java")));
        assertTrue(Files.exists(Path.of("../scanner-intellij-adapter/src/main/resources/META-INF/services/io.openliberty.tools.scanner.parser.DependencyParserProvider")));
    }

    @Test
    void testEclipseAdapterScaffoldingFilesExist() throws Exception {
        assertTrue(Files.exists(Path.of("../scanner-eclipse-adapter/src/main/java/io/openliberty/tools/scanner/eclipse/EclipseDependencyParserProvider.java")));
        assertTrue(Files.exists(Path.of("../scanner-eclipse-adapter/src/main/java/io/openliberty/tools/scanner/eclipse/EclipseProjectModelParser.java")));
        assertTrue(Files.exists(Path.of("../scanner-eclipse-adapter/src/main/resources/META-INF/services/io.openliberty.tools.scanner.parser.DependencyParserProvider")));
    }

}


