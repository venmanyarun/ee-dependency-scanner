package io.openliberty.tools.scanner.model;

/**
 * Enumeration of sources from which dependencies can be detected.
 * Indicates the origin of dependency information (build files, IDE configs, runtime classpath, etc.).
 */
public enum DependencySource {
    /**
     * Dependency detected from Maven pom.xml file
     */
    MAVEN("Maven pom.xml"),
    
    /**
     * Dependency detected from Gradle build.gradle file
     */
    GRADLE("Gradle build.gradle"),
    
    /**
     * Dependency detected from Gradle Kotlin DSL build.gradle.kts file
     */
    GRADLE_KOTLIN("Gradle build.gradle.kts"),
    
    /**
     * Dependency detected from Eclipse .classpath file
     */
    ECLIPSE("Eclipse .classpath"),
    
    /**
     * Dependency detected from IntelliJ .iml file
     */
    INTELLIJ("IntelliJ .iml"),
    
    /**
     * Dependency detected from VS Code workspace configuration
     */
    VSCODE("VS Code workspace"),
    
    /**
     * Dependency detected from JAR MANIFEST.MF file
     */
    MANIFEST("JAR MANIFEST.MF"),
    
    /**
     * Dependency detected from JAR file scanning
     */
    JAR_SCAN("JAR file scan"),
    
    /**
     * Dependency detected from library directory (lib/, libs/)
     */
    LIBRARY_DIR("Library directory"),
    
    /**
     * Dependency detected from build output directory (target/, build/)
     */
    BUILD_OUTPUT("Build output"),
    
    /**
     * Dependency source could not be determined
     */
    UNKNOWN("Unknown");

    private final String displayName;

    DependencySource(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

