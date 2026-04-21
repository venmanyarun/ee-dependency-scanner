package io.openliberty.tools.scanner.api;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Interface for parsing dependencies from various project representations.
 * 
 * <p>The generic type parameter T specifies what type of project representation this parser works with:
 * <ul>
 *   <li>DependencyParser<File> - File-based parsers (Maven pom.xml, Gradle build files)</li>
 *   <li>DependencyParser<Module> - IntelliJ IDEA Module parsers</li>
 *   <li>DependencyParser<IJavaProject> - Eclipse IJavaProject parsers</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b>
 * <pre>
 * // File-based parser
 * public class MavenDependencyParser implements DependencyParser<File> {
 *     public List<DependencyInfo> parse(File pomFile) throws ParserException { ... }
 *     public boolean canParse(File file) { return file.getName().equals("pom.xml"); }
 * }
 *
 * // IntelliJ Module parser
 * public class IntelliJDependencyParser implements DependencyParser<Module> {
 *     public List<DependencyInfo> parse(Module module) throws ParserException { ... }
 *     public boolean canParse(Module module) { return module != null; }
 * }
 *
 * // Eclipse IJavaProject parser
 * public class EclipseDependencyParser implements DependencyParser<IJavaProject> {
 *     public List<DependencyInfo> parse(IJavaProject project) throws ParserException { ... }
 *     public boolean canParse(IJavaProject project) { return project.exists(); }
 * }
 * </pre>
 * 
 * @param <T> the type of project representation (File, Module, IJavaProject, etc.)
 */
public interface DependencyParser<T> {
    
    /**
     * Parses dependencies from the given project representation.
     * 
     * @param project the project to parse (File, Module, IJavaProject, etc.)
     * @return list of detected dependencies
     * @throws ParserException if parsing fails
     */
    List<DependencyInfo> parse(T project) throws ParserException;
    
    /**
     * Parses dependencies from the given project representation with filtering.
     * This allows collecting only specific dependencies (e.g., MicroProfile, Jakarta EE).
     * 
     * <p>Default implementation parses all dependencies and filters them. Parsers should
     * override this method to filter during collection for better performance.
     * 
     * @param project the project to parse (File, Module, IJavaProject, etc.)
     * @param filter filter to select specific dependencies
     * @return list of detected dependencies matching the filter
     * @throws ParserException if parsing fails
     */
    default List<DependencyInfo> parse(T project, DependencyFilter filter) throws ParserException {
        // Default implementation: parse all and filter
        List<DependencyInfo> allDeps = parse(project);
        if (filter == null || filter == DependencyFilter.includeAll()) {
            return allDeps;
        }
        return allDeps.stream()
            .filter(filter::matches)
            .collect(Collectors.toList());
    }
    
    /**
     * Checks if parser can handle the given project.
     * 
     * @param project the project to check
     * @return true if parser can handle this project
     */
    boolean canParse(T project);
    
    /**
     * Gets parser name for identification.
     * 
     * @return parser name
     */
    String getName();
}


