package io.openliberty.tools.scanner.examples.eclipse;

import io.openliberty.tools.scanner.analyzer.ClasspathAnalyzer;
import io.openliberty.tools.scanner.model.ClasspathAnalysisResult;
import io.openliberty.tools.scanner.model.DependencyInfo;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Integration example for Liberty Tools for Eclipse with EE Dependency Scanner.
 * 
 * Liberty Tools for Eclipse provides development tools for Liberty applications in Eclipse IDE.
 * This integration demonstrates how to use the scanner with Eclipse's project model.
 * 
 * When the scanner-eclipse-adapter module is on the classpath, the scanner will automatically
 * use Eclipse's resolved project model (M2E for Maven, Buildship for Gradle) via the SPI mechanism.
 * 
 * Integration points:
 * 1. Project import - detect EE versions when project is imported
 * 2. Workspace initialization - scan all Liberty projects on startup
 * 3. Build path changes - re-scan when dependencies change
 * 4. Server configuration - suggest Liberty features based on detected versions
 * 5. Language server integration - configure LSP4Jakarta and LSP4MP
 */
public class LibertyToolsEclipseIntegration {
    
    private static final Logger LOGGER = Logger.getLogger(LibertyToolsEclipseIntegration.class.getName());
    
    // Thread-safe cache for project analysis results
    private final Map<String, ClasspathAnalysisResult> projectCache = new ConcurrentHashMap<>();
    
    /**
     * Scans an Eclipse project using the scanner.
     * 
     * When the Eclipse adapter (scanner-eclipse-adapter) is on the classpath,
     * the scanner will automatically use Eclipse's resolved project model:
     * - M2E (Maven) resolved dependencies
     * - Buildship (Gradle) resolved dependencies
     * - Eclipse .classpath file entries
     * 
     * @param projectLocation the Eclipse project location
     * @return analysis result with detected EE versions
     */
    public ClasspathAnalysisResult scanEclipseProject(String projectLocation) {
        // Check cache first
        if (projectCache.containsKey(projectLocation)) {
            LOGGER.fine("Using cached analysis for: " + projectLocation);
            return projectCache.get(projectLocation);
        }
        
        LOGGER.info("Scanning Eclipse project: " + projectLocation);
        
        File projectRoot = new File(projectLocation);
        
        // The ClasspathAnalyzer will automatically discover and use the
        // Eclipse adapter if it's on the classpath (via ServiceLoader)
        // Priority: Eclipse adapter > Maven parser > Gradle parser
        ClasspathAnalyzer analyzer = new ClasspathAnalyzer();
        ClasspathAnalysisResult result = analyzer.analyzeProject(projectRoot);
        
        // Cache the result
        projectCache.put(projectLocation, result);
        
        logEclipseIntegrationResults(result);
        return result;
    }
    
    /**
     * Invalidates the cache for a project (call when build path changes).
     * 
     * @param projectLocation the Eclipse project location
     */
    public void invalidateCache(String projectLocation) {
        projectCache.remove(projectLocation);
        LOGGER.info("Cache invalidated for: " + projectLocation);
    }
    
    /**
     * Gets Liberty server configuration based on detected versions.
     * 
     * @param projectLocation the Eclipse project location
     * @return server configuration map
     */
    public Map<String, Object> getLibertyServerConfiguration(String projectLocation) {
        ClasspathAnalysisResult result = scanEclipseProject(projectLocation);
        Map<String, Object> config = new HashMap<>();
        
        String jakartaVersion = result.getJakartaEEVersion();
        String javaeeVersion = result.getJavaEEVersion();
        String mpVersion = result.getMicroProfileVersion();
        
        config.put("jakartaEEVersion", jakartaVersion);
        config.put("javaEEVersion", javaeeVersion);
        config.put("microProfileVersion", mpVersion);
        
        // Suggest Liberty features
        if (jakartaVersion != null) {
            if (jakartaVersion.startsWith("9.1")) {
                config.put("platformFeature", "jakartaee-9.1");
                config.put("webProfile", "webProfile-9.1");
            } else if (jakartaVersion.startsWith("10.")) {
                config.put("platformFeature", "jakartaee-10.0");
                config.put("webProfile", "webProfile-10.0");
            } else if (jakartaVersion.startsWith("11.")) {
                config.put("platformFeature", "jakartaee-11.0");
                config.put("webProfile", "webProfile-11.0");
            }
        } else if (javaeeVersion != null) {
            config.put("platformFeature", "javaee-" + javaeeVersion);
            config.put("webProfile", "webProfile-" + javaeeVersion);
            config.put("showMigrationWizard", true);
        }
        
        if (mpVersion != null) {
            config.put("microProfileFeature", "microProfile-" + mpVersion);
        }
        
        // Suggest individual features based on detected APIs
        config.put("suggestedFeatures", suggestIndividualFeatures(result));
        
        return config;
    }
    
    /**
     * Suggests individual Liberty features based on detected dependencies.
     */
    private Map<String, String> suggestIndividualFeatures(ClasspathAnalysisResult result) {
        Map<String, String> features = new HashMap<>();
        
        for (DependencyInfo dep : result.getAllDependencies()) {
            String groupId = dep.getGroupId();
            String artifactId = dep.getArtifactId();
            
            // Jakarta EE APIs
            if ("jakarta.servlet".equals(groupId) && "jakarta.servlet-api".equals(artifactId)) {
                features.put("servlet", getServletFeature(dep.getVersion()));
            } else if ("jakarta.persistence".equals(groupId) && "jakarta.persistence-api".equals(artifactId)) {
                features.put("persistence", getPersistenceFeature(dep.getVersion()));
            } else if ("jakarta.enterprise".equals(groupId) && "jakarta.enterprise.cdi-api".equals(artifactId)) {
                features.put("cdi", getCDIFeature(dep.getVersion()));
            } else if ("jakarta.ws.rs".equals(groupId) && "jakarta.ws.rs-api".equals(artifactId)) {
                features.put("restfulWS", getRestfulWSFeature(dep.getVersion()));
            } else if ("jakarta.faces".equals(groupId) && "jakarta.faces-api".equals(artifactId)) {
                features.put("faces", getFacesFeature(dep.getVersion()));
            }
            
            // MicroProfile APIs
            else if (groupId.startsWith("org.eclipse.microprofile")) {
                if (artifactId.contains("config")) {
                    features.put("mpConfig", "mpConfig-" + getMPFeatureVersion(dep.getVersion()));
                } else if (artifactId.contains("health")) {
                    features.put("mpHealth", "mpHealth-" + getMPFeatureVersion(dep.getVersion()));
                } else if (artifactId.contains("metrics")) {
                    features.put("mpMetrics", "mpMetrics-" + getMPFeatureVersion(dep.getVersion()));
                } else if (artifactId.contains("openapi")) {
                    features.put("mpOpenAPI", "mpOpenAPI-" + getMPFeatureVersion(dep.getVersion()));
                }
            }
        }
        
        return features;
    }
    
    private String getServletFeature(String version) {
        if (version.startsWith("5.")) return "servlet-5.0";
        if (version.startsWith("6.")) return "servlet-6.0";
        if (version.startsWith("6.1")) return "servlet-6.1";
        return "servlet-" + version;
    }
    
    private String getPersistenceFeature(String version) {
        if (version.startsWith("3.0")) return "persistence-3.0";
        if (version.startsWith("3.1")) return "persistence-3.1";
        if (version.startsWith("3.2")) return "persistence-3.2";
        return "persistence-" + version;
    }
    
    private String getCDIFeature(String version) {
        if (version.startsWith("3.")) return "cdi-3.0";
        if (version.startsWith("4.")) return "cdi-4.0";
        if (version.startsWith("4.1")) return "cdi-4.1";
        return "cdi-" + version;
    }
    
    private String getRestfulWSFeature(String version) {
        if (version.startsWith("3.0")) return "restfulWS-3.0";
        if (version.startsWith("3.1")) return "restfulWS-3.1";
        if (version.startsWith("4.")) return "restfulWS-4.0";
        return "restfulWS-" + version;
    }
    
    private String getFacesFeature(String version) {
        if (version.startsWith("3.")) return "faces-3.0";
        if (version.startsWith("4.")) return "faces-4.0";
        if (version.startsWith("4.1")) return "faces-4.1";
        return "faces-" + version;
    }
    
    private String getMPFeatureVersion(String apiVersion) {
        // Map API version to feature version
        if (apiVersion.startsWith("2.")) return "3.0";
        if (apiVersion.startsWith("3.")) return "4.0";
        if (apiVersion.startsWith("4.")) return "5.0";
        return apiVersion;
    }
    
    /**
     * Configures LSP4Jakarta for Eclipse JDT.
     * 
     * @param projectLocation the Eclipse project location
     * @return LSP4Jakarta configuration
     */
    public Map<String, Object> configureLSP4Jakarta(String projectLocation) {
        ClasspathAnalysisResult result = scanEclipseProject(projectLocation);
        Map<String, Object> config = new HashMap<>();
        
        String jakartaVersion = result.getJakartaEEVersion();
        
        if (jakartaVersion != null) {
            config.put("enabled", true);
            config.put("version", jakartaVersion);
            config.put("diagnosticsEnabled", true);
            config.put("quickFixesEnabled", true);
            config.put("codeActionsEnabled", true);
            
            LOGGER.info("LSP4Jakarta configured for Jakarta EE " + jakartaVersion);
        } else {
            config.put("enabled", false);
            LOGGER.info("LSP4Jakarta disabled (no Jakarta EE detected)");
        }
        
        return config;
    }
    
    /**
     * Configures LSP4MP for Eclipse JDT.
     * 
     * @param projectLocation the Eclipse project location
     * @return LSP4MP configuration
     */
    public Map<String, Object> configureLSP4MP(String projectLocation) {
        ClasspathAnalysisResult result = scanEclipseProject(projectLocation);
        Map<String, Object> config = new HashMap<>();
        
        String mpVersion = result.getMicroProfileVersion();
        
        if (mpVersion != null) {
            config.put("enabled", true);
            config.put("version", mpVersion);
            config.put("diagnosticsEnabled", true);
            config.put("quickFixesEnabled", true);
            
            LOGGER.info("LSP4MP configured for MicroProfile " + mpVersion);
        } else {
            config.put("enabled", false);
            LOGGER.info("LSP4MP disabled (no MicroProfile detected)");
        }
        
        return config;
    }
    
    /**
     * Checks if a specific Jakarta EE API is available.
     * 
     * @param projectLocation the Eclipse project location
     * @param groupId the Maven groupId
     * @param artifactId the Maven artifactId
     * @return true if the API is available
     */
    public boolean hasJakartaAPI(String projectLocation, String groupId, String artifactId) {
        ClasspathAnalysisResult result = scanEclipseProject(projectLocation);
        return result.hasDependency(groupId, artifactId);
    }
    
    /**
     * Gets the version of a specific API.
     * 
     * @param projectLocation the Eclipse project location
     * @param groupId the Maven groupId
     * @param artifactId the Maven artifactId
     * @return the version string or null if not found
     */
    public String getAPIVersion(String projectLocation, String groupId, String artifactId) {
        ClasspathAnalysisResult result = scanEclipseProject(projectLocation);
        for (DependencyInfo dep : result.getAllDependencies()) {
            if (groupId.equals(dep.getGroupId()) && artifactId.equals(dep.getArtifactId())) {
                return dep.getVersion();
            }
        }
        return null;
    }
    
    /**
     * Generates server.xml feature suggestions.
     * 
     * @param projectLocation the Eclipse project location
     * @return XML snippet with suggested features
     */
    public String generateServerXmlFeatures(String projectLocation) {
        Map<String, Object> config = getLibertyServerConfiguration(projectLocation);
        StringBuilder xml = new StringBuilder();
        
        xml.append("<!-- Suggested Liberty features based on project dependencies -->\n");
        xml.append("<featureManager>\n");
        
        // Add platform feature if available
        if (config.containsKey("platformFeature")) {
            xml.append("    <feature>").append(config.get("platformFeature")).append("</feature>\n");
        } else if (config.containsKey("webProfile")) {
            xml.append("    <feature>").append(config.get("webProfile")).append("</feature>\n");
        }
        
        // Add MicroProfile feature if available
        if (config.containsKey("microProfileFeature")) {
            xml.append("    <feature>").append(config.get("microProfileFeature")).append("</feature>\n");
        }
        
        // Add individual features
        @SuppressWarnings("unchecked")
        Map<String, String> features = (Map<String, String>) config.get("suggestedFeatures");
        if (features != null && !features.isEmpty()) {
            xml.append("\n    <!-- Or use individual features: -->\n");
            for (String feature : features.values()) {
                xml.append("    <!-- <feature>").append(feature).append("</feature> -->\n");
            }
        }
        
        xml.append("</featureManager>");
        
        return xml.toString();
    }
    
    private void logEclipseIntegrationResults(ClasspathAnalysisResult result) {
        LOGGER.info("=== Liberty Tools Eclipse Integration Results ===");
        LOGGER.info("Jakarta EE Version: " + result.getJakartaEEVersion());
        LOGGER.info("Java EE Version: " + result.getJavaEEVersion());
        LOGGER.info("MicroProfile Version: " + result.getMicroProfileVersion());
        LOGGER.info("Parser Used: " + result.getPrimaryParserUsed());
        LOGGER.info("Total Dependencies: " + result.getAllDependencies().size());
        
        // Log key dependencies
        LOGGER.info("Key EE Dependencies:");
        for (DependencyInfo dep : result.getAllDependencies()) {
            if (dep.getGroupId().startsWith("jakarta.") || 
                dep.getGroupId().contains("microprofile")) {
                LOGGER.info("  - " + dep.getGroupId() + ":" + dep.getArtifactId() + ":" + dep.getVersion());
            }
        }
    }
    
    /**
     * Example showing how Liberty Tools Eclipse would use this integration.
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java LibertyToolsEclipseIntegration <eclipse-project-path>");
            System.exit(1);
        }
        
        String projectLocation = args[0];
        LibertyToolsEclipseIntegration integration = new LibertyToolsEclipseIntegration();
        
        System.out.println("\n=== Liberty Tools Eclipse Integration Example ===\n");
        
        // Scan the project
        ClasspathAnalysisResult result = integration.scanEclipseProject(projectLocation);
        
        // Get Liberty server configuration
        Map<String, Object> serverConfig = integration.getLibertyServerConfiguration(projectLocation);
        System.out.println("Liberty Server Configuration:");
        serverConfig.forEach((key, value) -> 
            System.out.println("  " + key + ": " + value)
        );
        
        // Generate server.xml features
        System.out.println("\nSuggested server.xml features:");
        System.out.println(integration.generateServerXmlFeatures(projectLocation));
        
        // Configure language servers
        System.out.println("\nLanguage Server Configuration:");
        Map<String, Object> lsp4jakartaConfig = integration.configureLSP4Jakarta(projectLocation);
        System.out.println("  LSP4Jakarta: " + lsp4jakartaConfig);
        
        Map<String, Object> lsp4mpConfig = integration.configureLSP4MP(projectLocation);
        System.out.println("  LSP4MP: " + lsp4mpConfig);
        
        // Check for specific APIs
        System.out.println("\nAPI Availability:");
        if (integration.hasJakartaAPI(projectLocation, "jakarta.servlet", "jakarta.servlet-api")) {
            String version = integration.getAPIVersion(projectLocation, "jakarta.servlet", "jakarta.servlet-api");
            System.out.println("  ✓ Jakarta Servlet API: " + version);
        }
        
        if (integration.hasJakartaAPI(projectLocation, "jakarta.persistence", "jakarta.persistence-api")) {
            String version = integration.getAPIVersion(projectLocation, "jakarta.persistence", "jakarta.persistence-api");
            System.out.println("  ✓ Jakarta Persistence API: " + version);
        }
        
        if (integration.hasJakartaAPI(projectLocation, "org.eclipse.microprofile.config", "microprofile-config-api")) {
            String version = integration.getAPIVersion(projectLocation, "org.eclipse.microprofile.config", "microprofile-config-api");
            System.out.println("  ✓ MicroProfile Config API: " + version);
        }
    }
}


