package io.openliberty.tools.scanner.analyzer;

import io.openliberty.tools.scanner.model.ClasspathAnalysisResult;
import io.openliberty.tools.scanner.model.DependencyInfo;
import io.openliberty.tools.scanner.model.DependencySource;
import io.openliberty.tools.scanner.parser.*;

import java.io.File;
import java.util.*;

/**
 * Main analyzer that orchestrates dependency detection across multiple sources.
 * Automatically detects project type and applies appropriate parsers.
 */
public class ClasspathAnalyzer {
    
    private final List<DependencyParser> parsers;
    private boolean enableFallbackScanning = true;
    
    /**
     * Creates a new ClasspathAnalyzer with default parsers.
     * Automatically loads additional parsers via ServiceLoader.
     */
    public ClasspathAnalyzer() {
        this.parsers = new ArrayList<>();
        
        // Add core parsers with explicit priorities
        this.parsers.add(new MavenPomParser());
        this.parsers.add(new GradleBuildParser());
        this.parsers.add(new EclipseClasspathParser());
        this.parsers.add(new JarManifestScanner());
        
        // Load extension parsers via ServiceLoader
        loadExtensionParsers();
        
        // Sort parsers by priority (lower = higher priority)
        Collections.sort(this.parsers);
    }
    
    /**
     * Creates a new ClasspathAnalyzer with custom parsers.
     */
    public ClasspathAnalyzer(List<DependencyParser> parsers) {
        this.parsers = new ArrayList<>(parsers);
        Collections.sort(this.parsers);
    }
    
    /**
     * Loads extension parsers via ServiceLoader.
     */
    private void loadExtensionParsers() {
        try {
            ServiceLoader<DependencyParserProvider> loader =
                ServiceLoader.load(DependencyParserProvider.class);
            
            List<DependencyParserProvider> providers = new ArrayList<>();
            for (DependencyParserProvider provider : loader) {
                providers.add(provider);
            }
            
            // Sort providers by priority
            providers.sort(Comparator.comparingInt(DependencyParserProvider::getPriority));
            
            // Add parsers from each provider
            for (DependencyParserProvider provider : providers) {
                List<DependencyParser> extensionParsers = provider.getParsers();
                if (extensionParsers != null) {
                    this.parsers.addAll(extensionParsers);
                }
            }
        } catch (Exception e) {
            // Log but don't fail if ServiceLoader has issues
            System.err.println("Warning: Failed to load extension parsers: " + e.getMessage());
        }
    }
    
    /**
     * Enables or disables fallback JAR scanning when no build files are found.
     */
    public void setEnableFallbackScanning(boolean enable) {
        this.enableFallbackScanning = enable;
    }
    
    /**
     * Analyzes the given project directory or file for dependencies.
     * 
     * @param projectPath the project directory or build file to analyze
     * @return comprehensive analysis result with all detected dependencies
     */
    public ClasspathAnalysisResult analyze(String projectPath) {
        return analyze(new File(projectPath));
    }
    
    /**
     * Analyzes the given project directory or file for dependencies.
     * 
     * @param projectPath the project directory or build file to analyze
     * @return comprehensive analysis result with all detected dependencies
     */
    public ClasspathAnalysisResult analyze(File projectPath) {
        long startTime = System.currentTimeMillis();
        
        if (!projectPath.exists()) {
            throw new IllegalArgumentException("Path does not exist: " + projectPath);
        }
        
        List<DependencyInfo> allDependencies = new ArrayList<>();
        Set<String> detectionMethods = new LinkedHashSet<>();
        int jarsScanned = 0;
        
        // Try each parser in order
        for (DependencyParser parser : parsers) {
            if (parser.canParse(projectPath)) {
                try {
                    List<DependencyInfo> dependencies = parser.parse(projectPath);
                    if (!dependencies.isEmpty()) {
                        allDependencies.addAll(dependencies);
                        detectionMethods.add(parser.getParserName());
                        
                        // Count JARs if this is the JAR scanner
                        if (parser instanceof JarManifestScanner) {
                            jarsScanned = dependencies.size();
                        }
                    }
                } catch (ParserException e) {
                    // Log error but continue with other parsers
                    System.err.println("Warning: " + parser.getParserName() + 
                                     " failed: " + e.getMessage());
                }
            }
        }
        
        // Fallback: recursive JAR scanning if no dependencies found
        if (allDependencies.isEmpty() && enableFallbackScanning && projectPath.isDirectory()) {
            try {
                JarManifestScanner fallbackScanner = new JarManifestScanner();
                List<DependencyInfo> jarDeps = scanRecursively(projectPath, fallbackScanner);
                if (!jarDeps.isEmpty()) {
                    allDependencies.addAll(jarDeps);
                    detectionMethods.add("Recursive JAR Scan");
                    jarsScanned = jarDeps.size();
                }
            } catch (Exception e) {
                System.err.println("Warning: Fallback scanning failed: " + e.getMessage());
            }
        }
        
        // Remove duplicates based on coordinate
        allDependencies = removeDuplicates(allDependencies);
        
        long endTime = System.currentTimeMillis();
        
        return ClasspathAnalysisResult.builder()
            .addDependencies(allDependencies)
            .totalJarsScanned(jarsScanned)
            .analysisTimeMs(endTime - startTime)
            .detectionMethod(String.join(", ", detectionMethods))
            .build();
    }
    
    /**
     * Recursively scans a directory for JAR files.
     */
    private List<DependencyInfo> scanRecursively(File directory, JarManifestScanner scanner) {
        List<DependencyInfo> dependencies = new ArrayList<>();
        
        File[] files = directory.listFiles();
        if (files == null) {
            return dependencies;
        }
        
        for (File file : files) {
            if (file.isDirectory()) {
                // Skip common non-library directories
                String name = file.getName();
                if (!name.startsWith(".") && !name.equals("node_modules") && 
                    !name.equals("test") && !name.equals("tests")) {
                    dependencies.addAll(scanRecursively(file, scanner));
                }
            } else if (file.getName().endsWith(".jar")) {
                try {
                    List<DependencyInfo> jarDeps = scanner.parse(file);
                    dependencies.addAll(jarDeps);
                } catch (ParserException e) {
                    // Skip problematic JARs
                }
            }
        }
        
        return dependencies;
    }
    
    /**
     * Removes duplicate dependencies based on groupId:artifactId.
     * Prefers entries with more complete information and authoritative sources.
     */
    private List<DependencyInfo> removeDuplicates(List<DependencyInfo> dependencies) {
        Map<String, DependencyInfo> uniqueDeps = new LinkedHashMap<>();
        
        for (DependencyInfo dep : dependencies) {
            String key = getDeduplicationKey(dep);
            
            DependencyInfo existing = uniqueDeps.get(key);
            if (existing == null) {
                uniqueDeps.put(key, dep);
            } else {
                // Keep the one with more information
                uniqueDeps.put(key, selectBetterDependency(existing, dep));
            }
        }
        
        return new ArrayList<>(uniqueDeps.values());
    }
    
    /**
     * Gets the deduplication key for a dependency (groupId:artifactId).
     * Falls back to artifactId only if groupId is missing.
     */
    private String getDeduplicationKey(DependencyInfo dep) {
        String groupId = dep.getGroupId();
        String artifactId = dep.getArtifactId();
        
        if (groupId != null && !groupId.isEmpty()) {
            return groupId + ":" + artifactId;
        }
        return artifactId != null ? artifactId : "";
    }
    
    /**
     * Selects the better dependency between two candidates.
     * Prefers entries with more complete information and authoritative sources.
     */
    private DependencyInfo selectBetterDependency(DependencyInfo a, DependencyInfo b) {
        int scoreA = calculateCompletenessScore(a);
        int scoreB = calculateCompletenessScore(b);
        return scoreA >= scoreB ? a : b;
    }
    
    /**
     * Calculates a completeness score for a dependency.
     * Higher scores indicate more complete and authoritative information.
     */
    private int calculateCompletenessScore(DependencyInfo dep) {
        int score = 0;
        
        // Prefer entries with groupId
        if (dep.getGroupId() != null && !dep.getGroupId().isEmpty()) {
            score += 3;
        }
        
        // Prefer entries with version
        if (dep.getVersion() != null && !dep.getVersion().isEmpty()) {
            score += 2;
        }
        
        // Prefer entries with JAR path
        if (dep.getJarPath() != null && !dep.getJarPath().isEmpty()) {
            score += 1;
        }
        
        // Prefer authoritative sources (Maven/Gradle) over JAR scanning
        DependencySource source = dep.getSource();
        if (source == DependencySource.MAVEN || source == DependencySource.GRADLE) {
            score += 5;
        } else if (source == DependencySource.ECLIPSE) {
            score += 3;
        }
        
        return score;
    }
    
    /**
     * Detects the project type based on files present in the directory.
     * 
     * @param projectPath the project directory
     * @return detected project type(s)
     */
    public List<String> detectProjectType(File projectPath) {
        List<String> types = new ArrayList<>();
        
        if (!projectPath.isDirectory()) {
            return types;
        }
        
        // Check for Maven
        if (new File(projectPath, "pom.xml").exists()) {
            types.add("Maven");
        }
        
        // Check for Gradle
        if (new File(projectPath, "build.gradle").exists() || 
            new File(projectPath, "build.gradle.kts").exists()) {
            types.add("Gradle");
        }
        
        // Check for Eclipse
        if (new File(projectPath, ".classpath").exists()) {
            types.add("Eclipse");
        }
        
        // Check for IntelliJ
        if (new File(projectPath, ".idea").exists()) {
            types.add("IntelliJ");
        }
        
        // Check for VS Code
        if (new File(projectPath, ".vscode").exists()) {
            types.add("VS Code");
        }
        
        return types;
    }
    
    /**
     * Quick check to see if a project has any EE dependencies.
     * 
     * @param projectPath the project directory
     * @return true if Jakarta EE, Java EE, or MicroProfile dependencies are found
     */
    public boolean hasEEDependencies(File projectPath) {
        ClasspathAnalysisResult result = analyze(projectPath);
        return !result.getJakartaEEDependencies().isEmpty() ||
               !result.getJavaEEDependencies().isEmpty() ||
               !result.getMicroProfileDependencies().isEmpty();
    }
}

