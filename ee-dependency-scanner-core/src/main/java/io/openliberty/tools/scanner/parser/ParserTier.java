package io.openliberty.tools.scanner.parser;

/**
 * Execution tiers for dependency parsers.
 */
public enum ParserTier {
    /**
     * IDE-resolved project model such as IntelliJ project/module/library model
     * or Eclipse JDT/Buildship/M2E resolved workspace model.
     */
    IDE_MODEL,

    /**
     * Build-tool resolved model such as Maven dependency tree or Gradle resolved dependencies.
     */
    BUILD_TOOL_RESOLVED,

    /**
     * Raw project/build file parsing such as pom.xml, build.gradle, or .classpath.
     */
    BUILD_FILE,

    /**
     * Binary or filesystem fallback such as jar manifest scanning.
     */
    BINARY_FALLBACK
}


