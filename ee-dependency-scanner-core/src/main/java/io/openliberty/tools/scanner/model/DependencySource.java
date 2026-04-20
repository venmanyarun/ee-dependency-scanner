package io.openliberty.tools.scanner.model;

/**
 * Source from which a dependency was detected.
 */
public enum DependencySource {
    MAVEN("Maven pom.xml"),
    GRADLE("Gradle build.gradle"),
    GRADLE_KOTLIN("Gradle build.gradle.kts"),
    ECLIPSE("Eclipse .classpath"),
    INTELLIJ("IntelliJ .iml"),
    VSCODE("VS Code workspace"),
    IDE_RESOLVED("IDE resolved dependencies"),
    MANIFEST("JAR MANIFEST.MF"),
    JAR_SCAN("JAR file scan"),
    LIBRARY_DIR("Library directory"),
    BUILD_OUTPUT("Build output"),
    UNKNOWN("Unknown");

    private final String displayName;

    DependencySource(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

