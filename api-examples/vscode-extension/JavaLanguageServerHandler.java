package io.openliberty.tools.vscode.scanner;

import io.openliberty.tools.scanner.api.*;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Java Language Server Handler for EE Dependency Scanner
 * 
 * This class runs in the Java Language Server (JVM) and has access to IJavaProject.
 * It implements the LSP request handlers that the VSCode extension calls.
 * 
 * Integration with liberty-tools-vscode pattern:
 * - Runs in the same JVM as the Java language server
 * - Has access to Eclipse JDT APIs (IJavaProject, IClasspathEntry, etc.)
 * - Communicates with VSCode extension via LSP protocol
 * 
 * Usage in Language Server:
 * 1. Add this handler to your language server
 * 2. Register the request handlers
 * 3. VSCode extension can call these via LanguageClient.sendRequest()
 */
public class JavaLanguageServerHandler {

    /**
     * Parser implementation that works with IJavaProject
     * This uses the Eclipse JDT APIs to access project dependencies efficiently
     */
    private static class IJavaProjectParser implements DependencyParser<IJavaProject> {
        
        @Override
        public List<DependencyInfo> parse(IJavaProject javaProject) throws ParserException {
            return parse(javaProject, DependencyFilter.ALL);
        }

        @Override
        public List<DependencyInfo> parse(IJavaProject javaProject, DependencyFilter filter) 
                throws ParserException {
            try {
                List<DependencyInfo> dependencies = new java.util.ArrayList<>();
                
                // Get classpath entries from IJavaProject
                // This is cached by Eclipse and very fast
                IClasspathEntry[] classpathEntries = javaProject.getResolvedClasspath(true);
                
                for (IClasspathEntry entry : classpathEntries) {
                    if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
                        // Process JAR dependencies
                        IPath path = entry.getPath();
                        String jarPath = path.toOSString();
                        
                        // Extract Maven coordinates from path if possible
                        // Example: /path/to/.m2/repository/org/eclipse/microprofile/config/
                        //          microprofile-config-api/3.0/microprofile-config-api-3.0.jar
                        DependencyInfo depInfo = extractDependencyInfo(jarPath);
                        
                        if (depInfo != null && filter.test(depInfo)) {
                            dependencies.add(depInfo);
                        }
                    } else if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
                        // Handle Maven/Gradle containers
                        // These are managed by m2e or buildship plugins
                        String containerPath = entry.getPath().toString();
                        if (containerPath.contains("MAVEN") || containerPath.contains("GRADLE")) {
                            // Container dependencies are already resolved in CPE_LIBRARY entries
                            continue;
                        }
                    }
                }
                
                return dependencies;
            } catch (Exception e) {
                throw new ParserException("Failed to parse IJavaProject dependencies", e);
            }
        }

        @Override
        public boolean canParse(IJavaProject javaProject) {
            try {
                return javaProject != null && javaProject.exists() && javaProject.getProject().isOpen();
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        public String getName() {
            return "IJavaProjectParser";
        }

        /**
         * Extract dependency info from JAR path
         * Handles Maven repository structure: groupId/artifactId/version/artifactId-version.jar
         */
        private DependencyInfo extractDependencyInfo(String jarPath) {
            // Implementation would parse the path to extract Maven coordinates
            // For brevity, showing the structure
            
            // Example path: .m2/repository/jakarta/platform/jakarta.jakartaee-api/10.0.0/jakarta.jakartaee-api-10.0.0.jar
            String[] parts = jarPath.split("/");
            if (parts.length < 4) return null;
            
            String version = parts[parts.length - 2];
            String artifactId = parts[parts.length - 3];
            
            // Build groupId from remaining parts
            StringBuilder groupId = new StringBuilder();
            for (int i = 0; i < parts.length - 3; i++) {
                if (parts[i].equals(".m2") || parts[i].equals("repository")) continue;
                if (groupId.length() > 0) groupId.append(".");
                groupId.append(parts[i]);
            }
            
            DependencyType type = detectDependencyType(groupId.toString(), artifactId);
            
            return new DependencyInfo(
                groupId.toString(),
                artifactId,
                version,
                type,
                DependencySource.ECLIPSE_CLASSPATH
            );
        }

        private DependencyType detectDependencyType(String groupId, String artifactId) {
            if (groupId.startsWith("jakarta.")) {
                return DependencyType.JAKARTA_EE;
            } else if (groupId.startsWith("javax.")) {
                return DependencyType.JAVA_EE;
            } else if (groupId.startsWith("org.eclipse.microprofile")) {
                return DependencyType.MICROPROFILE;
            }
            return DependencyType.OTHER;
        }
    }

    private final IJavaProjectParser parser = new IJavaProjectParser();

    /**
     * LSP Request Handler: Analyze project dependencies
     * Called from VSCode via: client.sendRequest('ee-scanner/analyzeDependencies', { projectUri })
     */
    @JsonRequest("ee-scanner/analyzeDependencies")
    public CompletableFuture<DependencyAnalysisResult> analyzeDependencies(AnalyzeDependenciesParams params) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                IJavaProject javaProject = getJavaProject(params.projectUri);
                if (javaProject == null) {
                    throw new IllegalArgumentException("Project not found: " + params.projectUri);
                }

                // Parse all dependencies
                List<DependencyInfo> dependencies = parser.parse(javaProject);

                // Detect versions
                String jakartaEEVersion = detectJakartaEEVersion(dependencies);
                String javaEEVersion = detectJavaEEVersion(dependencies);
                String microProfileVersion = detectMicroProfileVersion(dependencies);

                return new DependencyAnalysisResult(
                    dependencies,
                    jakartaEEVersion,
                    javaEEVersion,
                    microProfileVersion
                );
            } catch (Exception e) {
                throw new RuntimeException("Failed to analyze dependencies", e);
            }
        });
    }

    /**
     * LSP Request Handler: Check if project has MicroProfile
     * Called from VSCode via: client.sendRequest('ee-scanner/hasMicroProfile', { projectUri })
     */
    @JsonRequest("ee-scanner/hasMicroProfile")
    public CompletableFuture<Boolean> hasMicroProfile(ProjectUriParams params) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                IJavaProject javaProject = getJavaProject(params.projectUri);
                if (javaProject == null) return false;

                // Use filter for efficient check - only scans MicroProfile dependencies
                List<DependencyInfo> mpDeps = parser.parse(javaProject, DependencyFilter.MICROPROFILE);
                return !mpDeps.isEmpty();
            } catch (Exception e) {
                return false;
            }
        });
    }

    /**
     * LSP Request Handler: Get specific dependency version
     * Called from VSCode via: client.sendRequest('ee-scanner/getDependencyVersion', { projectUri, groupId, artifactId })
     */
    @JsonRequest("ee-scanner/getDependencyVersion")
    public CompletableFuture<String> getDependencyVersion(GetDependencyVersionParams params) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                IJavaProject javaProject = getJavaProject(params.projectUri);
                if (javaProject == null) return null;

                List<DependencyInfo> dependencies = parser.parse(javaProject);
                
                return dependencies.stream()
                    .filter(dep -> dep.getGroupId().equals(params.groupId) 
                                && dep.getArtifactId().equals(params.artifactId))
                    .map(DependencyInfo::getVersion)
                    .findFirst()
                    .orElse(null);
            } catch (Exception e) {
                return null;
            }
        });
    }

    /**
     * Convert project URI to IJavaProject
     * This uses Eclipse workspace APIs to find the project
     */
    private IJavaProject getJavaProject(String projectUri) {
        try {
            URI uri = new URI(projectUri);
            IPath projectPath = new Path(uri.getPath());
            
            // Find project in workspace
            IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
            for (IProject project : projects) {
                if (project.getLocation().equals(projectPath)) {
                    if (project.hasNature(JavaCore.NATURE_ID)) {
                        return JavaCore.create(project);
                    }
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    // Helper methods for version detection
    private String detectJakartaEEVersion(List<DependencyInfo> dependencies) {
        return dependencies.stream()
            .filter(dep -> dep.getType() == DependencyType.JAKARTA_EE)
            .filter(dep -> dep.getArtifactId().contains("jakartaee-api"))
            .map(DependencyInfo::getVersion)
            .findFirst()
            .orElse(null);
    }

    private String detectJavaEEVersion(List<DependencyInfo> dependencies) {
        return dependencies.stream()
            .filter(dep -> dep.getType() == DependencyType.JAVA_EE)
            .filter(dep -> dep.getArtifactId().contains("javaee-api"))
            .map(DependencyInfo::getVersion)
            .findFirst()
            .orElse(null);
    }

    private String detectMicroProfileVersion(List<DependencyInfo> dependencies) {
        return dependencies.stream()
            .filter(dep -> dep.getType() == DependencyType.MICROPROFILE)
            .filter(dep -> dep.getArtifactId().equals("microprofile"))
            .map(DependencyInfo::getVersion)
            .findFirst()
            .orElse(null);
    }

    // Parameter classes for LSP requests
    public static class AnalyzeDependenciesParams {
        public String projectUri;
    }

    public static class ProjectUriParams {
        public String projectUri;
    }

    public static class GetDependencyVersionParams {
        public String projectUri;
        public String groupId;
        public String artifactId;
    }
}


