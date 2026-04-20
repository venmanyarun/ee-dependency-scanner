package org.eclipse.lsp4jakarta.ls.dependency;

import io.openliberty.tools.scanner.model.ClasspathAnalysisResult;
import io.openliberty.tools.scanner.model.DependencyInfo;
import io.openliberty.tools.scanner.util.DependencyAnalysisHelper;

// Uncomment these imports when integrating into LSP4Jakarta
// import org.eclipse.jdt.core.IClasspathEntry;
// import org.eclipse.jdt.core.IJavaProject;
// import org.eclipse.jdt.core.JavaCore;
// import org.eclipse.core.resources.IProject;
// import org.eclipse.core.resources.ResourcesPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * LSP4Jakarta integration with EE Dependency Scanner using Eclipse JDT.
 * 
 * This class provides dependency analysis capabilities for Jakarta EE projects
 * in the LSP4Jakarta language server using Eclipse JDT APIs. It can be used to:
 * - Detect Jakarta EE version from project dependencies
 * - Provide version-specific diagnostics and code actions
 * - Filter diagnostics based on available Jakarta EE APIs
 * 
 * Since LSP4Jakarta runs in Eclipse and has access to JDT, this implementation
 * uses JDT APIs to collect classpath information, similar to the Eclipse adapter.
 */
public class LSP4JakartaDependencyAnalyzer {
    
    private static final Logger LOGGER = Logger.getLogger(LSP4JakartaDependencyAnalyzer.class.getName());
    
    private final DependencyAnalysisHelper helper;
    
    public LSP4JakartaDependencyAnalyzer() {
        this.helper = new DependencyAnalysisHelper();
    }
    
    /**
     * Analyze dependencies for a Java project using Eclipse JDT.
     * 
     * This method uses JDT APIs to get the resolved classpath and extract
     * dependency information from JAR files.
     * 
     * @param projectName The Eclipse project name
     * @return Analysis result with detected versions and dependencies
     */
    public ClasspathAnalysisResult analyzeDependencies(String projectName) {
        try {
            LOGGER.info("Analyzing dependencies for project: " + projectName);
            
            // STEP 1: Get Eclipse project from workspace
            // IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
            // if (!project.exists() || !project.isOpen()) {
            //     LOGGER.warning("Project not found or not open: " + projectName);
            //     return createEmptyResult();
            // }
            
            // STEP 2: Get Java project
            // IJavaProject javaProject = JavaCore.create(project);
            // if (javaProject == null || !javaProject.exists()) {
            //     LOGGER.warning("Not a Java project: " + projectName);
            //     return createEmptyResult();
            // }
            
            // STEP 3: Get resolved classpath entries
            List<DependencyInfo> dependencies = new ArrayList<>();
            
            // IClasspathEntry[] entries = javaProject.getResolvedClasspath(true);
            // 
            // for (IClasspathEntry entry : entries) {
            //     // Process library entries (JAR files)
            //     if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
            //         File jarFile = entry.getPath().toFile();
            //         
            //         if (jarFile.exists() && jarFile.getName().endsWith(".jar")) {
            //             // Extract dependencies from JAR using helper
            //             List<DependencyInfo> jarDeps = helper.extractDependenciesFromJar(jarFile);
            //             dependencies.addAll(jarDeps);
            //         }
            //     }
            //     
            //     // Process Maven/Gradle container entries
            //     if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
            //         // M2E Maven container: org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER
            //         // Buildship Gradle container: org.eclipse.buildship.core.gradleclasspathcontainer
            //         String containerPath = entry.getPath().toString();
            //         
            //         if (containerPath.contains("MAVEN2_CLASSPATH_CONTAINER")) {
            //             // Use M2E APIs to get Maven dependencies (see Eclipse adapter example)
            //             dependencies.addAll(collectMavenDependencies(project));
            //         } else if (containerPath.contains("gradleclasspathcontainer")) {
            //             // Use Buildship APIs to get Gradle dependencies (see Eclipse adapter example)
            //             dependencies.addAll(collectGradleDependencies(project));
            //         }
            //     }
            // }
            
            // STEP 4: Deduplicate dependencies
            dependencies = helper.deduplicate(dependencies);
            
            // STEP 5: Detect versions
            Map<String, Set<String>> versions = helper.detectVersions(dependencies);
            String jakartaVersion = helper.getPrimaryVersion(versions.get("jakartaEE"));
            String mpVersion = helper.getPrimaryVersion(versions.get("microProfile"));
            
            LOGGER.info("Detected Jakarta EE version: " + jakartaVersion);
            LOGGER.info("Detected MicroProfile version: " + mpVersion);
            
            return new ClasspathAnalysisResult(dependencies, jakartaVersion, mpVersion, versions);
            
        } catch (Exception e) {
            LOGGER.severe("Failed to analyze dependencies: " + e.getMessage());
            return createEmptyResult();
        }
    }
    
    /**
     * Collect Maven dependencies using M2E APIs.
     * 
     * This is a placeholder showing the pattern. See the Eclipse adapter
     * implementation for the complete code.
     */
    // private List<DependencyInfo> collectMavenDependencies(IProject project) {
    //     List<DependencyInfo> dependencies = new ArrayList<>();
    //     
    //     try {
    //         // Get M2E project facade
    //         IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getProject(project);
    //         if (facade != null) {
    //             MavenProject mavenProject = facade.getMavenProject(null);
    //             
    //             // Collect dependencies
    //             for (Dependency dep : mavenProject.getDependencies()) {
    //                 DependencyInfo depInfo = helper.createDependency(
    //                     dep.getGroupId(),
    //                     dep.getArtifactId(),
    //                     dep.getVersion()
    //                 );
    //                 dependencies.add(depInfo);
    //             }
    //         }
    //     } catch (Exception e) {
    //         LOGGER.warning("Failed to collect Maven dependencies: " + e.getMessage());
    //     }
    //     
    //     return dependencies;
    // }
    
    /**
     * Collect Gradle dependencies using Buildship APIs.
     * 
     * This is a placeholder showing the pattern. See the Eclipse adapter
     * implementation for the complete code.
     */
    // private List<DependencyInfo> collectGradleDependencies(IProject project) {
    //     List<DependencyInfo> dependencies = new ArrayList<>();
    //     
    //     try {
    //         // Get Buildship project
    //         GradleBuild gradleBuild = GradleCore.getWorkspace().getBuild(project).orElse(null);
    //         if (gradleBuild != null) {
    //             GradleProject gradleProject = gradleBuild.getModelProvider()
    //                 .fetchModel(GradleProject.class);
    //             
    //             // Collect dependencies
    //             for (GradleConfiguration config : gradleProject.getConfigurations()) {
    //                 for (GradleDependency dep : config.getDependencies()) {
    //                     if (dep instanceof ExternalDependency) {
    //                         ExternalDependency extDep = (ExternalDependency) dep;
    //                         DependencyInfo depInfo = helper.createDependency(
    //                             extDep.getGroup(),
    //                             extDep.getName(),
    //                             extDep.getVersion()
    //                         );
    //                         dependencies.add(depInfo);
    //                     }
    //                 }
    //             }
    //         }
    //     } catch (Exception e) {
    //         LOGGER.warning("Failed to collect Gradle dependencies: " + e.getMessage());
    //     }
    //     
    //     return dependencies;
    // }
    
    /**
     * Check if a specific Jakarta EE API is available in the project.
     * 
     * This can be used to conditionally enable/disable diagnostics and code actions.
     * 
     * @param result The analysis result
     * @param groupId The Maven groupId (e.g., "jakarta.servlet")
     * @param artifactId The Maven artifactId (e.g., "jakarta.servlet-api")
     * @return true if the API is available
     */
    public boolean isApiAvailable(ClasspathAnalysisResult result, String groupId, String artifactId) {
        if (result == null || result.getDependencies() == null) {
            return false;
        }
        
        return result.getDependencies().stream()
            .anyMatch(dep -> groupId.equals(dep.getGroupId()) && artifactId.equals(dep.getArtifactId()));
    }
    
    /**
     * Get all Jakarta EE dependencies from the analysis result.
     * 
     * @param result The analysis result
     * @return List of Jakarta EE dependencies
     */
    public List<DependencyInfo> getJakartaEEDependencies(ClasspathAnalysisResult result) {
        if (result == null || result.getDependencies() == null) {
            return new ArrayList<>();
        }
        
        return helper.getJakartaEEDependencies(result.getDependencies());
    }
    
    /**
     * Get all MicroProfile dependencies from the analysis result.
     * 
     * @param result The analysis result
     * @return List of MicroProfile dependencies
     */
    public List<DependencyInfo> getMicroProfileDependencies(ClasspathAnalysisResult result) {
        if (result == null || result.getDependencies() == null) {
            return new ArrayList<>();
        }
        
        return helper.getMicroProfileDependencies(result.getDependencies());
    }
    
    /**
     * Check if the project uses Jakarta EE 9+ (namespace jakarta.*).
     * 
     * This is useful for determining which diagnostics to apply, as Jakarta EE 9+
     * uses the jakarta.* namespace instead of javax.*.
     * 
     * @param result The analysis result
     * @return true if Jakarta EE 9 or higher is detected
     */
    public boolean isJakartaEE9Plus(ClasspathAnalysisResult result) {
        if (result == null || result.getJakartaEEVersion() == null) {
            return false;
        }
        
        String version = result.getJakartaEEVersion();
        
        // Parse version number
        try {
            String[] parts = version.split("\\.");
            int major = Integer.parseInt(parts[0]);
            return major >= 9;
        } catch (Exception e) {
            // If we can't parse, check if it contains "9" or higher
            return version.contains("9") || version.contains("10") || version.contains("11");
        }
    }
    
    /**
     * Create an empty result for error cases.
     */
    private ClasspathAnalysisResult createEmptyResult() {
        return new ClasspathAnalysisResult(
            new ArrayList<>(),
            null,
            null,
            Map.of()
        );
    }
}

