package io.openliberty.tools.scanner.eclipse;

import io.openliberty.tools.scanner.api.DependencyInfo;
import io.openliberty.tools.scanner.api.DependencyFilter;
import io.openliberty.tools.scanner.api.DependencyParser;
import io.openliberty.tools.scanner.api.ParserException;
import io.openliberty.tools.scanner.core.JarAnalyzer;
import io.openliberty.tools.scanner.util.DependencyAnalysisHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Eclipse IJavaProject-based parser using Eclipse JDT APIs.
 * 
 * <p>This parser uses Eclipse's JDT API to access ALL project dependencies:
 * <ul>
 *   <li>Uses IJavaProject.getResolvedClasspath() to get all classpath entries</li>
 *   <li>Handles Maven dependencies (via M2E if available)</li>
 *   <li>Handles Gradle dependencies (via Buildship if available)</li>
 *   <li>Handles manually added JAR libraries</li>
 *   <li>Uses core utilities for JAR analysis and version detection</li>
 * </ul>
 * 
 * <p><b>Usage Example:</b>
 * <pre>{@code
 * // In your Eclipse plugin code:
 * IJavaProject javaProject = ...; // Get the specific Java project
 * 
 * EclipseProjectModelParser parser = new EclipseProjectModelParser();
 * DependencyFilter filter = DependencyFilter.microProfileOnly();
 * List<DependencyInfo> dependencies = parser.parse(javaProject, filter);
 * 
 * // Use DependencyAnalysisHelper for version detection
 * DependencyAnalysisHelper helper = new DependencyAnalysisHelper();
 * Map<String, Set<String>> versions = helper.detectVersions(dependencies);
 * String mpVersion = helper.getPrimaryVersion(versions.get("microProfile"));
 * }</pre>
 * 
 * <p><b>Performance Notes:</b>
 * <ul>
 *   <li>Uses IJavaProject.getResolvedClasspath(true) for efficient access</li>
 *   <li>Leverages Eclipse's cached classpath resolution</li>
 *   <li>Automatically benefits from Eclipse's workspace model updates</li>
 *   <li>Uses core JarAnalyzer for efficient JAR scanning</li>
 * </ul>
 * 
 * <p><b>Implementation Requirements:</b>
 * To implement this parser in your Eclipse plugin:
 * <ol>
 *   <li>Add Eclipse JDT dependency to your plugin</li>
 *   <li>Add ee-dependency-scanner-core dependency</li>
 *   <li>Import required Eclipse APIs (IJavaProject, IClasspathEntry, etc.)</li>
 *   <li>Implement the parse method using the example below</li>
 * </ol>
 * 
 * <p><b>Example Implementation:</b>
 * <pre>{@code
 * @Override
 * public List<DependencyInfo> parse(Object project, DependencyFilter filter) throws ParserException {
 *     if (!(project instanceof IJavaProject)) {
 *         throw new ParserException("Expected IJavaProject but got: " + project.getClass());
 *     }
 *     
 *     IJavaProject javaProject = (IJavaProject) project;
 *     List<DependencyInfo> dependencies = new ArrayList<>();
 *     JarAnalyzer jarAnalyzer = new JarAnalyzer();
 *     
 *     try {
 *         // Get ALL resolved classpath entries (includes Maven, Gradle, and manual JARs)
 *         IClasspathEntry[] entries = javaProject.getResolvedClasspath(true);
 *         
 *         for (IClasspathEntry entry : entries) {
 *             // Process library entries (JARs)
 *             if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
 *                 IPath path = entry.getPath();
 *                 File jarFile = path.toFile();
 *                 
 *                 if (jarFile.exists() && jarFile.getName().endsWith(".jar")) {
 *                     // Use core JarAnalyzer to extract dependency info
 *                     List<DependencyInfo> jarDeps = jarAnalyzer.analyzeJar(jarFile, filter);
 *                     dependencies.addAll(jarDeps);
 *                 }
 *             }
 *             
 *             // Process project dependencies (other workspace projects)
 *             else if (entry.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
 *                 IPath projectPath = entry.getPath();
 *                 IProject referencedProject = ResourcesPlugin.getWorkspace().getRoot()
 *                     .getProject(projectPath.lastSegment());
 *                 
 *                 if (referencedProject.exists() && referencedProject.hasNature(JavaCore.NATURE_ID)) {
 *                     IJavaProject referencedJavaProject = JavaCore.create(referencedProject);
 *                     // Recursively analyze referenced project
 *                     List<DependencyInfo> projectDeps = parse(referencedJavaProject, filter);
 *                     dependencies.addAll(projectDeps);
 *                 }
 *             }
 *         }
 *         
 *         return dependencies;
 *         
 *     } catch (JavaModelException e) {
 *         throw new ParserException("Failed to analyze Eclipse project: " + e.getMessage(), e);
 *     }
 * }
 * }</pre>
 * 
 * <p><b>Alternative: Using M2E for Maven Projects</b>
 * <pre>{@code
 * // Optional: Check if project is Maven-managed via M2E
 * IMavenProjectRegistry registry = MavenPlugin.getMavenProjectRegistry();
 * IMavenProjectFacade facade = registry.getProject(javaProject.getProject());
 * 
 * if (facade != null) {
 *     // Maven project - M2E provides better metadata
 *     MavenProject mavenProject = facade.getMavenProject(null);
 *     DependencyAnalysisHelper helper = new DependencyAnalysisHelper();
 *     
 *     for (Artifact artifact : mavenProject.getArtifacts()) {
 *         if (filter.matches(artifact.getGroupId(), artifact.getArtifactId())) {
 *             DependencyInfo dep = helper.createDependency(
 *                 artifact.getGroupId(),
 *                 artifact.getArtifactId(),
 *                 artifact.getVersion(),
 *                 artifact.getFile().getAbsolutePath()
 *             );
 *             dependencies.add(dep);
 *         }
 *     }
 * }
 * }</pre>
 */
public class EclipseProjectModelParser implements DependencyParser<Object> {

    @Override
    public List<DependencyInfo> parse(Object javaProject) throws ParserException {
        return parse(javaProject, DependencyFilter.includeAll());
    }

    @Override
    public List<DependencyInfo> parse(Object javaProject, DependencyFilter filter) throws ParserException {
        // This is a stub implementation
        // Real implementation should follow the example in the class javadoc above
        
        // Key points:
        // 1. Cast to IJavaProject
        // 2. Use getResolvedClasspath(true) to get ALL dependencies
        // 3. Use JarAnalyzer from core to analyze JARs
        // 4. Apply filter during analysis for performance
        // 5. Use DependencyAnalysisHelper for version detection
        // 6. Return filtered dependencies
        
        return Collections.emptyList();
    }

    @Override
    public boolean canParse(Object javaProject) {
        // Real implementation should check if this is an Eclipse IJavaProject
        // return javaProject instanceof IJavaProject;
        return false;
    }

    @Override
    public String getName() {
        return "Eclipse JDT Project Model";
    }
}

