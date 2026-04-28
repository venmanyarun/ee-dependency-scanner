package io.openliberty.tools.scanner.analyzer;

/**
 * Specifies build tool preference for projects with multiple build configurations.
 * <p>
 * In projects with both Maven and Gradle build files, this preference determines
 * which build tool's dependencies should be analyzed.
 * </p>
 */
public enum BuildToolPreference {
    /**
     * Automatically detect build tool. Maven has higher priority if both exist.
     * This is the default behavior.
     */
    AUTO,
    
    /**
     * Use Maven (pom.xml) exclusively. Gradle files will be ignored.
     */
    MAVEN_ONLY,
    
    /**
     * Use Gradle (build.gradle/build.gradle.kts) exclusively. Maven files will be ignored.
     */
    GRADLE_ONLY,
    
    /**
     * Prefer Maven if both exist, otherwise use Gradle.
     * Same as AUTO but more explicit.
     */
    PREFER_MAVEN,
    
    /**
     * Prefer Gradle if both exist, otherwise use Maven.
     */
    PREFER_GRADLE
}
