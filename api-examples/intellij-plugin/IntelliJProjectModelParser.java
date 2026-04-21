package io.openliberty.tools.scanner.intellij;

import io.openliberty.tools.scanner.api.DependencyInfo;
import io.openliberty.tools.scanner.api.DependencyFilter;
import io.openliberty.tools.scanner.api.DependencyParser;
import io.openliberty.tools.scanner.api.ParserException;
import io.openliberty.tools.scanner.core.JarAnalyzer;
import io.openliberty.tools.scanner.util.DependencyAnalysisHelper;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * IntelliJ IDEA Module-based parser using IntelliJ Platform APIs.
 * 
 * <p>This parser uses IntelliJ's Platform API to access ALL module dependencies:
 * <ul>
 *   <li>Uses OrderEnumerator to traverse all module dependencies</li>
 *   <li>Handles Maven dependencies (via IntelliJ's Maven integration)</li>
 *   <li>Handles Gradle dependencies (via IntelliJ's Gradle integration)</li>
 *   <li>Handles manually added library JARs</li>
 *   <li>Leverages IntelliJ's cached dependency model for performance</li>
 *   <li>Uses core utilities for JAR analysis and version detection</li>
 * </ul>
 * 
 * <p><b>Usage Example:</b>
 * <pre>{@code
 * // In your IntelliJ plugin code:
 * Module module = ...; // Get the specific module
 * 
 * IntelliJProjectModelParser parser = new IntelliJProjectModelParser();
 * DependencyFilter filter = DependencyFilter.microProfileOnly();
 * List<DependencyInfo> dependencies = parser.parse(module, filter);
 * 
 * // Use DependencyAnalysisHelper for version detection
 * DependencyAnalysisHelper helper = new DependencyAnalysisHelper();
 * Map<String, Set<String>> versions = helper.detectVersions(dependencies);
 * String mpVersion = helper.getPrimaryVersion(versions.get("microProfile"));
 * }</pre>
 * 
 * <p><b>Performance Notes:</b>
 * <ul>
 *   <li>Uses OrderEnumerator for efficient dependency traversal</li>
 *   <li>Leverages IntelliJ's cached dependency resolution</li>
 *   <li>Can use JavaLibraryUtil.hasLibraryClass() for fast class checks</li>
 *   <li>Automatically benefits from IntelliJ's dependency cache updates</li>
 *   <li>Uses core JarAnalyzer for efficient JAR scanning</li>
 * </ul>
 * 
 * <p><b>Implementation Requirements:</b>
 * To implement this parser in your IntelliJ plugin:
 * <ol>
 *   <li>Add IntelliJ Platform SDK dependencies to your plugin</li>
 *   <li>Add ee-dependency-scanner-core dependency</li>
 *   <li>Import required IntelliJ APIs (Module, OrderEnumerator, Library, etc.)</li>
 *   <li>Implement the parse method using the example below</li>
 * </ol>
 * 
 * <p><b>Example Implementation:</b>
 * <pre>{@code
 * @Override
 * public List<DependencyInfo> parse(Object project, DependencyFilter filter) throws ParserException {
 *     if (!(project instanceof Module)) {
 *         throw new ParserException("Expected Module but got: " + project.getClass());
 *     }
 *     
 *     Module module = (Module) project;
 *     List<DependencyInfo> dependencies = new ArrayList<>();
 *     JarAnalyzer jarAnalyzer = new JarAnalyzer();
 *     
 *     // Use OrderEnumerator to get ALL dependencies (Maven, Gradle, manual JARs)
 *     OrderEnumerator.orderEntries(module)
 *         .withoutSdk()
 *         .recursively()
 *         .forEachLibrary(library -> {
 *             // Get library files (JARs)
 *             VirtualFile[] files = library.getFiles(OrderRootType.CLASSES);
 *             
 *             for (VirtualFile vFile : files) {
 *                 File jarFile = VfsUtilCore.virtualToIoFile(vFile);
 *                 
 *                 if (jarFile.exists() && jarFile.getName().endsWith(".jar")) {
 *                     // Use core JarAnalyzer to extract dependency info
 *                     List<DependencyInfo> jarDeps = jarAnalyzer.analyzeJar(jarFile, filter);
 *                     dependencies.addAll(jarDeps);
 *                 }
 *             }
 *             
 *             return true; // Continue iteration
 *         });
 *     
 *     return dependencies;
 * }
 * }</pre>
 * 
 * <p><b>Alternative: Parsing Library Names for Maven/Gradle Coordinates</b>
 * <pre>{@code
 * // IntelliJ stores Maven/Gradle coordinates in library names
 * // Format: "Maven: groupId:artifactId:version" or "Gradle: groupId:artifactId:version"
 * 
 * DependencyAnalysisHelper helper = new DependencyAnalysisHelper();
 * 
 * OrderEnumerator.orderEntries(module)
 *     .withoutSdk()
 *     .forEachLibrary(library -> {
 *         String libraryName = library.getName();
 *         
 *         if (libraryName != null && (libraryName.startsWith("Maven: ") || libraryName.startsWith("Gradle: "))) {
 *             String coords = libraryName.substring(libraryName.indexOf(": ") + 2);
 *             String[] parts = coords.split(":");
 *             
 *             if (parts.length >= 3) {
 *                 String groupId = parts[0];
 *                 String artifactId = parts[1];
 *                 String version = parts[2];
 *                 
 *                 // Apply filter for performance
 *                 if (filter.matches(groupId, artifactId)) {
 *                     // Get JAR file path
 *                     VirtualFile[] files = library.getFiles(OrderRootType.CLASSES);
 *                     String jarPath = files.length > 0 ? 
 *                         VfsUtilCore.virtualToIoFile(files[0]).getAbsolutePath() : null;
 *                     
 *                     // Use helper to create dependency
 *                     DependencyInfo dep = helper.createDependency(groupId, artifactId, version, jarPath);
 *                     dependencies.add(dep);
 *                 }
 *             }
 *         }
 *         return true;
 *     });
 * }</pre>
 * 
 * <p><b>Performance Tip: Using JavaLibraryUtil for Fast Checks</b>
 * <pre>{@code
 * // For performance-critical checks (e.g., checking if MicroProfile Config is present)
 * import com.intellij.openapi.roots.libraries.JavaLibraryUtil;
 * 
 * if (JavaLibraryUtil.hasLibraryClass(module, "org.eclipse.microprofile.config.Config")) {
 *     // Module has MicroProfile Config - proceed with detailed analysis
 * }
 * }</pre>
 * 
 * @see <a href="https://github.com/redhat-developer/intellij-quarkus">IntelliJ Quarkus Plugin Example</a>
 */
public class IntelliJProjectModelParser implements DependencyParser<Object> {

    @Override
    public List<DependencyInfo> parse(Object module) throws ParserException {
        return parse(module, DependencyFilter.includeAll());
    }

    @Override
    public List<DependencyInfo> parse(Object module, DependencyFilter filter) throws ParserException {
        // This is a stub implementation
        // Real implementation should follow the example in the class javadoc above
        
        // Key points:
        // 1. Cast to Module
        // 2. Use OrderEnumerator to get ALL dependencies
        // 3. Use JarAnalyzer from core to analyze JARs
        // 4. Apply filter during analysis for performance
        // 5. Optionally parse library names for Maven/Gradle coordinates
        // 6. Use DependencyAnalysisHelper for creating dependencies and version detection
        // 7. Return filtered dependencies
        
        return Collections.emptyList();
    }

    @Override
    public boolean canParse(Object module) {
        // Real implementation should check if this is an IntelliJ Module
        // return module instanceof Module;
        return false;
    }

    @Override
    public String getName() {
        return "IntelliJ Platform Module Model";
    }
}

