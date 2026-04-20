package org.eclipse.lsp4mp.jdt.internal.core.providers;

import io.openliberty.tools.scanner.model.ClasspathAnalysisResult;
import io.openliberty.tools.scanner.model.DependencyInfo;
import io.openliberty.tools.scanner.util.DependencyAnalysisHelper;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.lsp4mp.jdt.core.IProjectLabelProvider;

// Uncomment these imports when integrating into LSP4MP with M2E support
// import org.eclipse.m2e.core.MavenPlugin;
// import org.eclipse.m2e.core.project.IMavenProjectFacade;
// import org.apache.maven.project.MavenProject;
// import org.apache.maven.model.Dependency;

// Uncomment these imports when integrating into LSP4MP with Buildship support
// import org.eclipse.buildship.core.GradleCore;
// import org.eclipse.buildship.core.GradleBuild;
// import org.gradle.tooling.model.GradleProject;
// import org.gradle.tooling.model.gradle.GradleConfiguration;
// import org.gradle.tooling.model.gradle.GradleDependency;
// import org.gradle.tooling.model.gradle.ExternalDependency;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Project label provider that uses EE Dependency Scanner to detect
 * Jakarta EE and MicroProfile versions and provide version-specific labels.
 * 
 * This provider adds labels like:
 * - "jakartaee-9", "jakartaee-10", etc.
 * - "microprofile-4.0", "microprofile-5.0", etc.
 * - "jakartaee" (generic)
 * - "microprofile" (generic)
 * 
 * These labels can be used by LSP4MP to:
 * - Enable/disable version-specific diagnostics
 * - Provide appropriate code actions
 * - Handle namespace differences (javax.* vs jakarta.*)
 * 
 * Usage in LSP4MP:
 * 1. Register this provider via extension point or programmatically
 * 2. Labels will be automatically added to projects
 * 3. Use labels in diagnostics/code actions to provide version-aware features
 */
public class EEVersionProjectLabelProvider implements IProjectLabelProvider {
    
    private static final Logger LOGGER = Logger.getLogger(EEVersionProjectLabelProvider.class.getName());
    
    // Label constants
    public static final String JAKARTA_EE_LABEL = "jakartaee";
    public static final String MICROPROFILE_LABEL = "microprofile";
    
    private final DependencyAnalysisHelper helper;
    
    // Cache to avoid re-analyzing on every call
    private final Map<String, ClasspathAnalysisResult> analysisCache = new java.util.concurrent.ConcurrentHashMap<>();
    
    public EEVersionProjectLabelProvider() {
        this.helper = new DependencyAnalysisHelper();
    }
    
    @Override
    public List<String> getProjectLabels(IJavaProject project) throws JavaModelException {
        List<String> labels = new ArrayList<>();
        
        try {
            // Get or compute analysis result
            String projectName = project.getProject().getName();
            ClasspathAnalysisResult result = analysisCache.computeIfAbsent(projectName, 
                key -> analyzeDependencies(project));
            
            // Add Jakarta EE labels
            if (result.getJakartaEEVersion() != null) {
                labels.add(JAKARTA_EE_LABEL);
                labels.add(JAKARTA_EE_LABEL + "-" + result.getJakartaEEVersion());
                
                LOGGER.info("Project " + projectName + " has Jakarta EE " + result.getJakartaEEVersion());
            }
            
            // Add MicroProfile labels
            if (result.getMicroProfileVersion() != null) {
                labels.add(MICROPROFILE_LABEL);
                labels.add(MICROPROFILE_LABEL + "-" + result.getMicroProfileVersion());
                
                LOGGER.info("Project " + projectName + " has MicroProfile " + result.getMicroProfileVersion());
            }
            
        } catch (Exception e) {
            LOGGER.severe("Failed to analyze project dependencies: " + e.getMessage());
        }
        
        return labels;
    }
    
    /**
     * Analyze project dependencies using JDT APIs with M2E and Buildship support.
     * This provides better Maven and Gradle integration.
     */
    private ClasspathAnalysisResult analyzeDependencies(IJavaProject javaProject) {
        try {
            IProject project = javaProject.getProject();
            List<DependencyInfo> dependencies = new ArrayList<>();
            
            // Try M2E (Maven) first
            boolean foundMaven = collectMavenDependencies(project, dependencies);
            
            // Try Buildship (Gradle) if not Maven
            if (!foundMaven) {
                boolean foundGradle = collectGradleDependencies(project, dependencies);
                
                // Fall back to JDT classpath if neither Maven nor Gradle
                if (!foundGradle) {
                    collectJDTClasspathDependencies(javaProject, dependencies);
                }
            }
            
            // Deduplicate dependencies
            dependencies = helper.deduplicate(dependencies);
            
            // Detect versions
            Map<String, Set<String>> versions = helper.detectVersions(dependencies);
            String jakartaVersion = helper.getPrimaryVersion(versions.get("jakartaEE"));
            String mpVersion = helper.getPrimaryVersion(versions.get("microProfile"));
            
            return new ClasspathAnalysisResult(dependencies, jakartaVersion, mpVersion, versions);
            
        } catch (Exception e) {
            LOGGER.severe("Failed to analyze dependencies: " + e.getMessage());
            return createEmptyResult();
        }
    }
    
    /**
     * Collect dependencies using M2E (Maven Integration for Eclipse).
     *
     * @return true if Maven dependencies were found
     */
    private boolean collectMavenDependencies(IProject project, List<DependencyInfo> dependencies) {
        try {
            // Uncomment when integrating into LSP4MP:
            // IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getProject(project);
            // if (facade != null) {
            //     MavenProject mavenProject = facade.getMavenProject(null);
            //
            //     // Collect dependencies
            //     for (Dependency dep : mavenProject.getDependencies()) {
            //         DependencyInfo depInfo = helper.createDependency(
            //             dep.getGroupId(),
            //             dep.getArtifactId(),
            //             dep.getVersion()
            //         );
            //         dependencies.add(depInfo);
            //     }
            //
            //     LOGGER.info("Collected " + dependencies.size() + " Maven dependencies");
            //     return true;
            // }
            
            return false;
        } catch (Exception e) {
            LOGGER.warning("Failed to collect Maven dependencies: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Collect dependencies using Buildship (Gradle Integration for Eclipse).
     *
     * @return true if Gradle dependencies were found
     */
    private boolean collectGradleDependencies(IProject project, List<DependencyInfo> dependencies) {
        try {
            // Uncomment when integrating into LSP4MP:
            // GradleBuild gradleBuild = GradleCore.getWorkspace().getBuild(project).orElse(null);
            // if (gradleBuild != null) {
            //     GradleProject gradleProject = gradleBuild.getModelProvider()
            //         .fetchModel(GradleProject.class);
            //
            //     // Collect dependencies from all configurations
            //     for (GradleConfiguration config : gradleProject.getConfigurations()) {
            //         for (GradleDependency dep : config.getDependencies()) {
            //             if (dep instanceof ExternalDependency) {
            //                 ExternalDependency extDep = (ExternalDependency) dep;
            //                 DependencyInfo depInfo = helper.createDependency(
            //                     extDep.getGroup(),
            //                     extDep.getName(),
            //                     extDep.getVersion()
            //                 );
            //                 dependencies.add(depInfo);
            //             }
            //         }
            //     }
            //
            //     LOGGER.info("Collected " + dependencies.size() + " Gradle dependencies");
            //     return true;
            // }
            
            return false;
        } catch (Exception e) {
            LOGGER.warning("Failed to collect Gradle dependencies: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Collect dependencies from JDT classpath (fallback method).
     * This extracts dependencies from JAR files in the classpath.
     */
    private void collectJDTClasspathDependencies(IJavaProject javaProject, List<DependencyInfo> dependencies) {
        try {
            // Get resolved classpath entries
            IClasspathEntry[] entries = javaProject.getResolvedClasspath(true);
            
            for (IClasspathEntry entry : entries) {
                // Process library entries (JAR files)
                if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
                    File jarFile = entry.getPath().toFile();
                    
                    if (jarFile.exists() && jarFile.getName().endsWith(".jar")) {
                        // Extract dependencies from JAR using helper
                        List<DependencyInfo> jarDeps = helper.extractDependenciesFromJar(jarFile);
                        dependencies.addAll(jarDeps);
                    }
                }
            }
            
            LOGGER.info("Collected " + dependencies.size() + " dependencies from JDT classpath");
            
        } catch (Exception e) {
            LOGGER.warning("Failed to collect JDT classpath dependencies: " + e.getMessage());
        }
    }
    
    /**
     * Clear the analysis cache for a specific project.
     * Call this when project dependencies change.
     */
    public void clearCache(String projectName) {
        analysisCache.remove(projectName);
    }
    
    /**
     * Clear the entire analysis cache.
     */
    public void clearAllCache() {
        analysisCache.clear();
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