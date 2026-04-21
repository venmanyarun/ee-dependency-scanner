package org.eclipse.lsp4jakarta.ls.integration;

import io.openliberty.tools.scanner.api.DependencyInfo;
import io.openliberty.tools.scanner.util.DependencyAnalysisHelper;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Complete replacement for LSP4Jakarta's JakartaVersionManager.
 * 
 * <p>This class provides Jakarta EE and MicroProfile version detection
 * using ee-dependency-scanner's EclipseJDTParser.
 * 
 * <p><b>Usage:</b>
 * <pre>
 * LSP4JakartaIntegrationExample integration = new LSP4JakartaIntegrationExample();
 * 
 * // Get Jakarta EE version
 * String jakartaVersion = integration.getJakartaEEVersion(javaProject);
 * 
 * // Get MicroProfile version
 * String mpVersion = integration.getMicroProfileVersion(javaProject);
 * 
 * // Check for version conflicts
 * boolean hasConflicts = integration.hasVersionConflicts(javaProject);
 * </pre>
 * 
 * <p><b>Features:</b>
 * <ul>
 *   <li>Bundle-SymbolicName extraction from MANIFEST.MF</li>
 *   <li>Multiple version attribute support (Specification-Version, Implementation-Version, Bundle-Version)</li>
 *   <li>Transitive dependency extraction from embedded pom.xml</li>
 *   <li>Filename parsing fallback</li>
 *   <li>Centralized version registry (no hardcoded mappings)</li>
 *   <li>Works with Maven, Gradle, and manual JAR dependencies</li>
 * </ul>
 */
public class LSP4JakartaIntegrationExample {
    
    private final DependencyAnalysisHelper helper;
    private final EclipseJDTParser eclipseParser;
    
    public LSP4JakartaIntegrationExample() {
        this.helper = new DependencyAnalysisHelper();
        this.eclipseParser = new EclipseJDTParser();
    }
    
    /**
     * Gets Jakarta EE version for a project.
     * 
     * <p>This method replaces JakartaVersionFinder.analyzeClasspath().
     * 
     * @param javaProject Eclipse IJavaProject
     * @return Jakarta EE version (e.g., "10", "9.1", "9") or null if not detected
     */
    public String getJakartaEEVersion(Object javaProject) {
        try {
            // Use EclipseJDTParser to parse dependencies
            // This handles:
            // - Bundle-SymbolicName extraction from MANIFEST.MF
            // - Transitive dependencies from embedded pom.xml
            // - Filename parsing fallback
            List<DependencyInfo> dependencies = eclipseParser.parseJakartaEEOnly(javaProject);
            
            // Detect versions using the helper
            Map<String, Set<String>> versions = helper.detectVersions(dependencies);
            Set<String> jakartaVersions = versions.get("jakartaEE");
            
            if (jakartaVersions == null || jakartaVersions.isEmpty()) {
                // Check for Java EE (legacy)
                Set<String> javaEEVersions = versions.get("javaEE");
                if (javaEEVersions != null && !javaEEVersions.isEmpty()) {
                    return "JavaEE-" + helper.getPrimaryVersion(javaEEVersions);
                }
                return null;
            }
            
            // Return the primary (highest) version
            return helper.getPrimaryVersion(jakartaVersions);
            
        } catch (Exception e) {
            System.err.println("Failed to detect Jakarta EE version: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Gets detailed Jakarta EE feature versions.
     * Useful for diagnostics and detailed version reporting.
     * 
     * <p><b>Example usage in LSP4Jakarta diagnostics:</b>
     * <pre>
     * Map<String, Set<String>> features = getJakartaEEFeatureVersions(javaProject);
     * for (Map.Entry<String, Set<String>> entry : features.entrySet()) {
     *     if (entry.getValue().size() > 1) {
     *         // Multiple versions detected - show warning
     *         addDiagnostic("Multiple versions of " + entry.getKey() + 
     *                      " detected: " + entry.getValue());
     *     }
     * }
     * </pre>
     * 
     * @param javaProject Eclipse Java project
     * @return map of feature names to versions (e.g., "servlet" -> ["6.0", "5.0"])
     */
    public Map<String, Set<String>> getJakartaEEFeatureVersions(Object javaProject) {
        try {
            // Use EclipseJDTParser to get Jakarta EE dependencies
            List<DependencyInfo> jakartaDeps = eclipseParser.parseJakartaEEOnly(javaProject);
            
            // Extract feature versions
            Map<String, Set<String>> featureVersions = new java.util.HashMap<>();
            for (DependencyInfo dep : jakartaDeps) {
                String feature = extractFeatureName(dep.getArtifactId());
                String version = dep.getVersion();
                if (feature != null && version != null) {
                    featureVersions.computeIfAbsent(feature, k -> new java.util.HashSet<>()).add(version);
                }
            }
            
            return featureVersions;
            
        } catch (Exception e) {
            System.err.println("Failed to get feature versions: " + e.getMessage());
            return java.util.Collections.emptyMap();
        }
    }
    
    /**
     * Checks if project has version conflicts.
     * Useful for showing warnings in LSP diagnostics.
     * 
     * <p><b>Example usage in LSP4Jakarta:</b>
     * <pre>
     * if (hasVersionConflicts(javaProject)) {
     *     // Show warning in Problems view
     *     publishDiagnostics(uri, createConflictDiagnostics(javaProject));
     * }
     * </pre>
     * 
     * @param javaProject Eclipse Java project
     * @return true if multiple versions of same feature detected
     */
    public boolean hasVersionConflicts(Object javaProject) {
        Map<String, Set<String>> featureVersions = getJakartaEEFeatureVersions(javaProject);
        return featureVersions.values().stream()
            .anyMatch(versions -> versions.size() > 1);
    }
    
    /**
     * Gets MicroProfile version for a project.
     * 
     * @param javaProject Eclipse Java project
     * @return MicroProfile version (e.g., "6.0", "5.0") or null if not detected
     */
    public String getMicroProfileVersion(Object javaProject) {
        try {
            // Use EclipseJDTParser to get MicroProfile dependencies
            List<DependencyInfo> dependencies = eclipseParser.parseMicroProfileOnly(javaProject);
            
            Map<String, Set<String>> versions = helper.detectVersions(dependencies);
            Set<String> mpVersions = versions.get("microProfile");
            
            return mpVersions != null ? helper.getPrimaryVersion(mpVersions) : null;
            
        } catch (Exception e) {
            System.err.println("Failed to detect MicroProfile version: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Gets project labels for Jakarta EE and MicroProfile versions.
     * 
     * <p><b>Usage in LSP4Jakarta:</b>
     * <pre>
     * // In your language server initialization or project change handler:
     * LSP4JakartaIntegrationExample integration = new LSP4JakartaIntegrationExample();
     * String[] labels = integration.getProjectLabels(javaProject);
     * // Use labels for project identification, filtering, etc.
     * </pre>
     * 
     * @param javaProject Eclipse Java project
     * @return array of project labels (e.g., ["jakarta-10", "microprofile-6.0"])
     */
    public String[] getProjectLabels(Object javaProject) {
        String jakartaVersion = getJakartaEEVersion(javaProject);
        String mpVersion = getMicroProfileVersion(javaProject);
        
        java.util.List<String> labels = new java.util.ArrayList<>();
        
        if (jakartaVersion != null) {
            labels.add("jakarta-" + jakartaVersion);
        }
        if (mpVersion != null) {
            labels.add("microprofile-" + mpVersion);
        }
        
        return labels.toArray(new String[0]);
    }
    
    /**
     * Extracts feature name from artifact ID.
     * 
     * @param artifactId artifact ID (e.g., "jakarta.servlet-api")
     * @return feature name (e.g., "servlet")
     */
    private String extractFeatureName(String artifactId) {
        if (artifactId == null) return null;
        
        return artifactId
            .replaceAll("-api$", "")
            .replaceAll("-spec$", "")
            .replaceAll("^jakarta\\.", "")
            .replaceAll("^javax\\.", "")
            .replaceAll("-", "")
            .toLowerCase();
    }
}


