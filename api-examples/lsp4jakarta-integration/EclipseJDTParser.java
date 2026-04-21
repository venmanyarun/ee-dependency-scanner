package org.eclipse.lsp4jakarta.ls.integration;

import io.openliberty.tools.scanner.api.DependencyFilter;
import io.openliberty.tools.scanner.api.DependencyInfo;
import io.openliberty.tools.scanner.api.DependencyParser;
import io.openliberty.tools.scanner.api.ParserException;
import io.openliberty.tools.scanner.parser.JarManifestScanner;

// Eclipse JDT imports (available in LSP4Jakarta runtime)
// import org.eclipse.jdt.core.IJavaProject;
// import org.eclipse.jdt.core.IClasspathEntry;
// import org.eclipse.jdt.core.JavaModelException;
// import org.eclipse.core.runtime.IPath;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Eclipse JDT-based parser for LSP4Jakarta integration.
 * 
 * <p>This parser uses Eclipse's IJavaProject API to analyze project dependencies
 * and leverages ee-dependency-scanner's JarManifestScanner for JAR analysis.
 * 
 * <p><b>Key Features:</b>
 * <ul>
 *   <li>Uses IJavaProject.getResolvedClasspath(true) for ALL dependencies</li>
 *   <li>Inspects JAR manifests with Bundle-SymbolicName support</li>
 *   <li>Extracts transitive dependencies from embedded pom.xml</li>
 *   <li>Falls back to filename parsing if manifest inspection fails</li>
 *   <li>Handles Maven, Gradle, and manual JAR dependencies</li>
 * </ul>
 * 
 * <p><b>Usage in LSP4Jakarta:</b>
 * <pre>
 * // In JakartaVersionManager or similar
 * EclipseJDTParser parser = new EclipseJDTParser();
 * List<DependencyInfo> dependencies = parser.parse(javaProject);
 * 
 * // Use DependencyAnalysisHelper for version detection
 * DependencyAnalysisHelper helper = new DependencyAnalysisHelper();
 * Map<String, Set<String>> versions = helper.detectVersions(dependencies);
 * String jakartaVersion = helper.getPrimaryVersion(versions.get("jakartaEE"));
 * </pre>
 */
public class EclipseJDTParser implements DependencyParser<Object> {
    
    private final JarManifestScanner jarScanner;
    
    public EclipseJDTParser() {
        this.jarScanner = new JarManifestScanner();
        // Enable transitive dependency extraction (default is true)
        this.jarScanner.setExtractTransitiveDependencies(true);
    }
    
    /**
     * Parses dependencies from an Eclipse IJavaProject.
     * 
     * @param project Eclipse IJavaProject instance
     * @return list of all dependencies (direct + transitive)
     * @throws ParserException if parsing fails
     */
    @Override
    public List<DependencyInfo> parse(Object project) throws ParserException {
        return parse(project, DependencyFilter.includeAll());
    }
    
    /**
     * Parses dependencies from an Eclipse IJavaProject with filtering.
     * 
     * <p><b>Implementation:</b>
     * <ol>
     *   <li>Gets resolved classpath from IJavaProject</li>
     *   <li>Processes each CPE_LIBRARY entry</li>
     *   <li>Uses JarManifestScanner to analyze each JAR:
     *     <ul>
     *       <li>Reads Bundle-SymbolicName from MANIFEST.MF</li>
     *       <li>Extracts version from Specification-Version, Implementation-Version, Bundle-Version</li>
     *       <li>Extracts transitive dependencies from embedded pom.xml</li>
     *       <li>Falls back to filename parsing if needed</li>
     *     </ul>
     *   </li>
     *   <li>Applies filter to results</li>
     * </ol>
     * 
     * @param project Eclipse IJavaProject instance
     * @param filter dependency filter (e.g., DependencyFilter.jakartaEEOnly())
     * @return filtered list of dependencies
     * @throws ParserException if parsing fails
     */
    @Override
    public List<DependencyInfo> parse(Object project, DependencyFilter filter) throws ParserException {
        List<DependencyInfo> allDependencies = new ArrayList<>();
        Set<String> processedPaths = new HashSet<>();
        
        // REAL IMPLEMENTATION (uncomment in LSP4Jakarta):
        /*
        if (!(project instanceof IJavaProject)) {
            throw new ParserException("Expected IJavaProject but got: " + 
                (project != null ? project.getClass().getName() : "null"));
        }
        
        IJavaProject javaProject = (IJavaProject) project;
        
        try {
            // Get ALL resolved classpath entries
            // This includes:
            // - Maven dependencies (via M2E)
            // - Gradle dependencies (via Buildship)
            // - Manual JAR libraries
            // - Project dependencies
            IClasspathEntry[] entries = javaProject.getResolvedClasspath(true);
            
            for (IClasspathEntry entry : entries) {
                // Only process library entries (JAR files)
                if (entry.getEntryKind() != IClasspathEntry.CPE_LIBRARY) {
                    continue;
                }
                
                IPath path = entry.getPath();
                String pathStr = path.toOSString();
                
                // Avoid processing the same JAR multiple times
                if (processedPaths.contains(pathStr)) {
                    continue;
                }
                processedPaths.add(pathStr);
                
                // Only process JAR files
                if (!pathStr.endsWith(".jar")) {
                    continue;
                }
                
                File jarFile = path.toFile();
                if (!jarFile.exists()) {
                    continue;
                }
                
                try {
                    // JarManifestScanner does:
                    // 1. Reads Bundle-SymbolicName from MANIFEST.MF (OSGi bundles)
                    // 2. Extracts version from multiple attributes
                    // 3. Extracts transitive dependencies from embedded pom.xml
                    // 4. Falls back to filename parsing if manifest fails
                    List<DependencyInfo> jarDeps = jarScanner.parse(jarFile);
                    
                    // Apply filter
                    for (DependencyInfo dep : jarDeps) {
                        if (filter.matches(dep)) {
                            allDependencies.add(dep);
                        }
                    }
                    
                } catch (Exception e) {
                    // Log but continue processing other JARs
                    System.err.println("Failed to parse JAR: " + pathStr + " - " + e.getMessage());
                }
            }
            
        } catch (JavaModelException e) {
            throw new ParserException("Failed to get classpath from IJavaProject: " + e.getMessage(), e);
        }
        */
        
        return allDependencies;
    }
    
    /**
     * Checks if this parser can handle the given project.
     * 
     * @param project project to check
     * @return true if project is an IJavaProject
     */
    @Override
    public boolean canParse(Object project) {
        // In real implementation:
        // return project instanceof IJavaProject;
        return false;
    }
    
    /**
     * Gets the parser name.
     * 
     * @return parser name
     */
    @Override
    public String getName() {
        return "Eclipse JDT Parser for LSP4Jakarta";
    }
    
    /**
     * Parses only Jakarta EE dependencies from a project.
     * Convenience method for LSP4Jakarta.
     * 
     * @param project Eclipse IJavaProject
     * @return list of Jakarta EE dependencies
     * @throws ParserException if parsing fails
     */
    public List<DependencyInfo> parseJakartaEEOnly(Object project) throws ParserException {
        return parse(project, DependencyFilter.jakartaEEOnly());
    }
    
    /**
     * Parses only MicroProfile dependencies from a project.
     * Convenience method for LSP4Jakarta.
     * 
     * @param project Eclipse IJavaProject
     * @return list of MicroProfile dependencies
     * @throws ParserException if parsing fails
     */
    public List<DependencyInfo> parseMicroProfileOnly(Object project) throws ParserException {
        return parse(project, DependencyFilter.microProfileOnly());
    }
}

