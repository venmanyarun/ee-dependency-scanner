package io.openliberty.tools.scanner.parser;

import java.util.List;

/**
 * Service Provider Interface (SPI) for contributing custom dependency parsers.
 * 
 * IDE plugins and extensions can implement this interface and register it via
 * Java's ServiceLoader mechanism to add custom parsers without modifying the core library.
 * 
 * To register a provider, create a file:
 * META-INF/services/io.openliberty.tools.scanner.parser.DependencyParserProvider
 * 
 * containing the fully qualified class name of your implementation.
 * 
 * Example implementation:
 * <pre>
 * public class EclipseJDTParserProvider implements DependencyParserProvider {
 *     {@literal @}Override
 *     public List<DependencyParser> getParsers() {
 *         return Arrays.asList(new EclipseJDTParser());
 *     }
 *     
 *     {@literal @}Override
 *     public int getPriority() {
 *         return 50; // Higher priority than default parsers
 *     }
 * }
 * </pre>
 */
public interface DependencyParserProvider {
    
    /**
     * Returns the list of parsers provided by this provider.
     * 
     * @return list of dependency parsers
     */
    List<DependencyParser> getParsers();
    
    /**
     * Gets the priority for this provider. Lower values = higher priority.
     * This affects the order in which parsers from this provider are added
     * relative to other providers.
     * 
     * @return priority value (default: 100)
     */
    default int getPriority() {
        return 100;
    }
}
