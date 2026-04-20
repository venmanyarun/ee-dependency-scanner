package io.openliberty.tools.scanner.parser;

import io.openliberty.tools.scanner.model.DependencyInfo;
import java.io.File;
import java.util.List;

/**
 * Interface for parsing dependencies from various sources (Maven, Gradle, Eclipse, etc.).
 * Each implementation handles a specific build system or configuration format.
 *
 * Parsers are executed in priority order (lower values = higher priority).
 */
public interface DependencyParser extends Comparable<DependencyParser> {
    
    /**
     * Parses dependencies from the given file or directory.
     *
     * @param path the file or directory to parse
     * @return list of detected dependencies
     * @throws ParserException if parsing fails
     */
    List<DependencyInfo> parse(File path) throws ParserException;
    
    /**
     * Checks if this parser can handle the given file or directory.
     *
     * @param path the file or directory to check
     * @return true if this parser can handle the path, false otherwise
     */
    boolean canParse(File path);
    
    /**
     * Gets the name of this parser (e.g., "Maven", "Gradle", "Eclipse").
     *
     * @return the parser name
     */
    String getParserName();
    
    /**
     * Gets the priority for parser execution. Lower values = higher priority.
     *
     * Recommended priority ranges:
     * <ul>
     *   <li>0-99: Build system parsers (Maven, Gradle) - most authoritative</li>
     *   <li>100-199: IDE-specific parsers (Eclipse, IntelliJ)</li>
     *   <li>200-299: Fallback parsers (JAR manifest scanning)</li>
     *   <li>300+: Custom/experimental parsers</li>
     * </ul>
     *
     * @return priority value (default: 100)
     */
    default int getPriority() {
        return 100;
    }
    
    /**
     * Compares parsers by priority for sorting.
     */
    @Override
    default int compareTo(DependencyParser other) {
        return Integer.compare(this.getPriority(), other.getPriority());
    }
}

