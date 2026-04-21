package io.openliberty.tools.scanner.parser;

import io.openliberty.tools.scanner.api.DependencyParser;

/**
 * Extended parser interface for core module parsers with tier and priority support.
 * This interface adds implementation-specific methods to the API's DependencyParser.
 * 
 * @param <T> the type of project representation (File, Module, IJavaProject, etc.)
 */
public interface CoreDependencyParser<T> extends DependencyParser<T>, Comparable<CoreDependencyParser<?>> {
    
    /**
     * Gets the execution tier for this parser.
     * @return parser tier
     */
    default ParserTier getTier() {
        return ParserTier.BUILD_FILE;
    }
    
    /**
     * Gets parser priority within its tier (lower = higher priority).
     * @return priority value
     */
    default int getPriority() {
        return 100;
    }
    
    /**
     * Checks if this is an IDE project model parser.
     * @return true if IDE model parser
     */
    default boolean isIdeProjectModelParser() {
        return getTier() == ParserTier.IDE_MODEL;
    }
    
    /**
     * Compares parsers by tier and priority.
     */
    @Override
    default int compareTo(CoreDependencyParser<?> other) {
        int tierCompare = getTier().compareTo(other.getTier());
        if (tierCompare != 0) {
            return tierCompare;
        }
        return Integer.compare(getPriority(), other.getPriority());
    }
}

