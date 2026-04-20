package io.openliberty.tools.scanner.parser;

import io.openliberty.tools.scanner.model.DependencyInfo;
import java.io.File;
import java.util.List;

/**
 * Interface for parsing dependencies from various sources.
 * Parsers execute in priority order (lower = higher priority).
 */
public interface DependencyParser extends Comparable<DependencyParser> {
    
    /**
     * Parses dependencies from file or directory.
     * @param path file or directory to parse
     * @return list of detected dependencies
     * @throws ParserException if parsing fails
     */
    List<DependencyInfo> parse(File path) throws ParserException;
    
    /**
     * Checks if parser can handle the given path.
     * @param path file or directory to check
     * @return true if parser can handle path
     */
    boolean canParse(File path);
    
    /**
     * Gets parser name for identification.
     * @return parser name
     */
    String getParserName();
    
    /**
     * Gets execution priority (lower = higher priority).
     * @return priority value (default: 100)
     */
    default int getPriority() {
        return 100;
    }
    
    @Override
    default int compareTo(DependencyParser other) {
        return Integer.compare(this.getPriority(), other.getPriority());
    }
}

