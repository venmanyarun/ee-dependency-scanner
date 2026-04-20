package io.openliberty.tools.scanner.examples.intellij;

import io.openliberty.tools.scanner.analyzer.ClasspathAnalyzer;
import io.openliberty.tools.scanner.model.ClasspathAnalysisResult;
import io.openliberty.tools.scanner.model.DependencyInfo;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Integration example for Liberty Tools for IntelliJ IDEA with EE Dependency Scanner.
 * 
 * Liberty Tools for IntelliJ provides development tools for Liberty applications in IntelliJ IDEA.
 * This integration demonstrates how to use the scanner with IntelliJ's project model.
 * 
 * When the scanner-intellij-adapter module is on the classpath, the scanner will automatically
 * use IntelliJ's resolved project model (Maven/Gradle dependencies) via the SPI mechanism.
 * 
 * Integration points:
 * 1. Project initialization - detect EE versions when project opens
 * 2. Language server configuration - configure LSP4Jakarta and LSP4MP
 * 3. Run configuration - adjust Liberty dev mode based on detected versions
 * 4. Diagnostics - enable version-specific diagnostics
 */
public class LibertyToolsIntelliJIntegration {
    
    private static final Logger LOGGER = Logger.getLogger(LibertyToolsIntelliJIntegration.class.getName());
    
    // Thread-safe cache for project analysis results
    private final Map<String, ClasspathAnalysisResult> projectCache = new ConcurrentHashMap<>();
    
    /**
     * Scans an IntelliJ project using the scanner.
     * 
     * When the IntelliJ adapter (scanner-intellij-adapter) is on the classpath,
     * the scanner will automatically use IntelliJ's resolved project model.
     * 
     * @param projectBasePath the IntelliJ project base path
     * @return analysis result with detected EE versions
     */
    public ClasspathAnalysisResult scanIntelliJProject(String projectBasePath) {
        // Check cache first
        if (projectCache.containsKey(projectBasePath)) {
            LOGGER.fine("Using cached analysis for: " + projectBasePath);
            return projectCache.get(projectBasePath);
        }
        
        LOGGER.info("Scanning IntelliJ project: " + projectBasePath);
        
        File projectRoot = new File(projectBasePath);
        
        // The ClasspathAnalyzer will automatically discover and use the
        // IntelliJ adapter if it's on the classpath (via ServiceLoader)
        ClasspathAnalyzer analyzer = new ClasspathAnalyzer();
        ClasspathAnalysisResult result = analyzer.analyzeProject(projectRoot);
        
        // Cache the result
        projectCache.put(projectBasePath, result);
        
        logIntelliJIntegrationResults(result);
        return result;
    }
    
    /**
     * Invalidates the cache for a project (call when project structure changes).
     * 
     * @param projectBasePath the IntelliJ project base path
     */
    public void invalidateCache(String projectBasePath) {
        projectCache.remove(projectBasePath);
        LOGGER.info("Cache invalidated for: " + projectBasePath);
    }
    
    /**
     * Gets configuration for Liberty Tools based on detected versions.
     * 
     * @param projectBasePath the IntelliJ project base path
     * @return configuration map
     */
    public Map<String, Object> getLibertyToolsConfiguration(String projectBasePath) {
        ClasspathAnalysisResult result = scanIntelliJProject(projectBasePath);
        Map<String, Object> config = new HashMap<>();
        
        String jakartaVersion = result.getJakartaEEVersion();
        String javaeeVersion = result.getJavaEEVersion();
        String mpVersion = result.getMicroProfileVersion();
        
        config.put("jakartaEEVersion", jakartaVersion);
        config.put("javaEEVersion", javaeeVersion);
        config.put("microProfileVersion", mpVersion);
        config.put("isJakartaEE", jakartaVersion != null);
        config.put("isLegacyJavaEE", javaeeVersion != null);
        
        // Determine Liberty features to suggest
        if (jakartaVersion != null) {
            if (jakartaVersion.startsWith("9.1")) {
                config.put("suggestedFeatures", "jakartaee-9.1");
            } else if (jakartaVersion.startsWith("10.")) {
                config.put("suggestedFeatures", "jakartaee-10.0");
            } else if (jakartaVersion.startsWith("11.")) {
                config.put("suggestedFeatures", "jakartaee-11.0");
            }
        } else if (javaeeVersion != null) {
            config.put("suggestedFeatures", "javaee-" + javaeeVersion);
            config.put("showMigrationNotification", true);
        }
        
        if (mpVersion != null) {
            config.put("microProfileFeature", "microProfile-" + mpVersion);
        }
        
        return config;
    }
    
    /**
     * Configures LSP4Jakarta language server based on detected version.
     * 
     * @param projectBasePath the IntelliJ project base path
     * @return LSP4Jakarta configuration
     */
    public Map<String, Object> configureLSP4Jakarta(String projectBasePath) {
        ClasspathAnalysisResult result = scanIntelliJProject(projectBasePath);
        Map<String, Object> config = new HashMap<>();
        
        String jakartaVersion = result.getJakartaEEVersion();
        
        if (jakartaVersion != null) {
            config.put("enabled", true);
            config.put("version", jakartaVersion);
            
            // Enable version-specific diagnostics
            if (jakartaVersion.startsWith("9.")) {
                config.put("servlet.version", "5.0");
                config.put("cdi.version", "3.0");
                config.put("jpa.version", "3.0");
            } else if (jakartaVersion.startsWith("10.")) {
                config.put("servlet.version", "6.0");
                config.put("cdi.version", "4.0");
                config.put("jpa.version", "3.1");
            }
            
            LOGGER.info("LSP4Jakarta configured for Jakarta EE " + jakartaVersion);
        } else {
            config.put("enabled", false);
            LOGGER.info("LSP4Jakarta disabled (no Jakarta EE detected)");
        }
        
        return config;
    }
    
    /**
     * Configures LSP4MP (MicroProfile) language server based on detected version.
     * 
     * @param projectBasePath the IntelliJ project base path
     * @return LSP4MP configuration
     */
    public Map<String, Object> configureLSP4MP(String projectBasePath) {
        ClasspathAnalysisResult result = scanIntelliJProject(projectBasePath);
        Map<String, Object> config = new HashMap<>();
        
        String mpVersion = result.getMicroProfileVersion();
        
        if (mpVersion != null) {
            config.put("enabled", true);
            config.put("version", mpVersion);
            
            // Enable version-specific features
            if (mpVersion.startsWith("4.")) {
                config.put("config.version", "2.0");
                config.put("health.version", "3.0");
                config.put("metrics.version", "3.0");
            } else if (mpVersion.startsWith("5.")) {
                config.put("config.version", "3.0");
                config.put("health.version", "4.0");
                config.put("metrics.version", "4.0");
            }
            
            LOGGER.info("LSP4MP configured for MicroProfile " + mpVersion);
        } else {
            config.put("enabled", false);
            LOGGER.info("LSP4MP disabled (no MicroProfile detected)");
        }
        
        return config;
    }
    
    /**
     * Gets Liberty dev mode parameters based on detected versions.
     * 
     * @param projectBasePath the IntelliJ project base path
     * @return suggested dev mode parameters
     */
    public String getLibertyDevModeParameters(String projectBasePath) {
        ClasspathAnalysisResult result = scanIntelliJProject(projectBasePath);
        StringBuilder params = new StringBuilder();
        
        // Add container mode if Jakarta EE 9+ is detected
        String jakartaVersion = result.getJakartaEEVersion();
        if (jakartaVersion != null && !jakartaVersion.startsWith("8")) {
            params.append("--container ");
        }
        
        // Add hot tests if test dependencies are present
        if (hasTestDependencies(result)) {
            params.append("--hotTests ");
        }
        
        return params.toString().trim();
    }
    
    /**
     * Checks if a specific Jakarta EE API is available.
     * 
     * @param projectBasePath the IntelliJ project base path
     * @param groupId the Maven groupId
     * @param artifactId the Maven artifactId
     * @return true if the API is available
     */
    public boolean hasJakartaAPI(String projectBasePath, String groupId, String artifactId) {
        ClasspathAnalysisResult result = scanIntelliJProject(projectBasePath);
        return result.hasDependency(groupId, artifactId);
    }
    
    /**
     * Gets the version of a specific API.
     * 
     * @param projectBasePath the IntelliJ project base path
     * @param groupId the Maven groupId
     * @param artifactId the Maven artifactId
     * @return the version string or null if not found
     */
    public String getAPIVersion(String projectBasePath, String groupId, String artifactId) {
        ClasspathAnalysisResult result = scanIntelliJProject(projectBasePath);
        for (DependencyInfo dep : result.getAllDependencies()) {
            if (groupId.equals(dep.getGroupId()) && artifactId.equals(dep.getArtifactId())) {
                return dep.getVersion();
            }
        }
        return null;
    }
    
    /**
     * Checks if the project has test dependencies.
     */
    private boolean hasTestDependencies(ClasspathAnalysisResult result) {
        for (DependencyInfo dep : result.getAllDependencies()) {
            String artifactId = dep.getArtifactId().toLowerCase();
            if (artifactId.contains("junit") || artifactId.contains("testng") || 
                artifactId.contains("arquillian")) {
                return true;
            }
        }
        return false;
    }
    
    private void logIntelliJIntegrationResults(ClasspathAnalysisResult result) {
        LOGGER.info("=== Liberty Tools IntelliJ Integration Results ===");
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
     * Example showing how Liberty Tools IntelliJ would use this integration.
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java LibertyToolsIntelliJIntegration <intellij-project-path>");
            System.exit(1);
        }
        
        String projectBasePath = args[0];
        LibertyToolsIntelliJIntegration integration = new LibertyToolsIntelliJIntegration();
        
        System.out.println("\n=== Liberty Tools IntelliJ Integration Example ===\n");
        
        // Scan the project
        ClasspathAnalysisResult result = integration.scanIntelliJProject(projectBasePath);
        
        // Get Liberty Tools configuration
        Map<String, Object> libertyConfig = integration.getLibertyToolsConfiguration(projectBasePath);
        System.out.println("Liberty Tools Configuration:");
        libertyConfig.forEach((key, value) -> 
            System.out.println("  " + key + ": " + value)
        );
        
        // Configure language servers
        System.out.println("\nLanguage Server Configuration:");
        Map<String, Object> lsp4jakartaConfig = integration.configureLSP4Jakarta(projectBasePath);
        System.out.println("  LSP4Jakarta: " + lsp4jakartaConfig);
        
        Map<String, Object> lsp4mpConfig = integration.configureLSP4MP(projectBasePath);
        System.out.println("  LSP4MP: " + lsp4mpConfig);
        
        // Get dev mode parameters
        String devModeParams = integration.getLibertyDevModeParameters(projectBasePath);
        System.out.println("\nSuggested Liberty Dev Mode Parameters: " + devModeParams);
        
        // Check for specific APIs
        System.out.println("\nAPI Availability:");
        if (integration.hasJakartaAPI(projectBasePath, "jakarta.servlet", "jakarta.servlet-api")) {
            String version = integration.getAPIVersion(projectBasePath, "jakarta.servlet", "jakarta.servlet-api");
            System.out.println("  ✓ Jakarta Servlet API: " + version);
        }
        
        if (integration.hasJakartaAPI(projectBasePath, "jakarta.persistence", "jakarta.persistence-api")) {
            String version = integration.getAPIVersion(projectBasePath, "jakarta.persistence", "jakarta.persistence-api");
            System.out.println("  ✓ Jakarta Persistence API: " + version);
        }
        
        if (integration.hasJakartaAPI(projectBasePath, "org.eclipse.microprofile.config", "microprofile-config-api")) {
            String version = integration.getAPIVersion(projectBasePath, "org.eclipse.microprofile.config", "microprofile-config-api");
            System.out.println("  ✓ MicroProfile Config API: " + version);
        }
    }
}


