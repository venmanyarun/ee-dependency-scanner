package io.openliberty.tools.scanner.parser;

import java.util.List;

/**
 * SPI for contributing custom dependency parsers via ServiceLoader.
 */
public interface DependencyParserProvider {
    
    /**
     * Returns parsers provided by this provider.
     * @return list of dependency parsers
     */
    List<CoreDependencyParser<?>> getParsers();
    
    /**
     * Gets provider priority (lower = higher priority).
     * @return priority value (default: 100)
     */
    default int getPriority() {
        return 100;
    }
}
