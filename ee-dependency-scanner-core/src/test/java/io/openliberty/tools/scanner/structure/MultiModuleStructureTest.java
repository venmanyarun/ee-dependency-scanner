package io.openliberty.tools.scanner.structure;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiModuleStructureTest {

    @Test
    void testParentAndModulePomsExist() {
        assertTrue(new File("../pom.xml").exists(), "Parent pom.xml should exist");
        assertTrue(new File("../ee-dependency-scanner-core/pom.xml").exists(), "Core module pom.xml should exist");
        assertTrue(new File("../scanner-intellij-adapter/pom.xml").exists(), "IntelliJ adapter pom.xml should exist");
        assertTrue(new File("../scanner-eclipse-adapter/pom.xml").exists(), "Eclipse adapter pom.xml should exist");
    }

    @Test
    void testAdapterServiceRegistrationFilesExist() {
        assertTrue(new File("../scanner-intellij-adapter/src/main/resources/META-INF/services/io.openliberty.tools.scanner.parser.DependencyParserProvider").exists(),
            "IntelliJ adapter service registration should exist");
        assertTrue(new File("../scanner-eclipse-adapter/src/main/resources/META-INF/services/io.openliberty.tools.scanner.parser.DependencyParserProvider").exists(),
            "Eclipse adapter service registration should exist");
    }
}
