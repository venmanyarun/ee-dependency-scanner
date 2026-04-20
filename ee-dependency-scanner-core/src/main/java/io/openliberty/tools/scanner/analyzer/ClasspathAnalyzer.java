package io.openliberty.tools.scanner.analyzer;

import io.openliberty.tools.scanner.model.ClasspathAnalysisResult;
import io.openliberty.tools.scanner.model.DependencyInfo;
import io.openliberty.tools.scanner.model.DependencySource;
import io.openliberty.tools.scanner.parser.*;

import java.io.File;
import java.util.*;

/**
 * Analyzes project dependencies from various sources (Maven, Gradle, Eclipse, JARs).
 */
public class ClasspathAnalyzer {
    
    private final List<DependencyParser> parsers;
    private boolean enableFallbackScanning = true;
    private boolean preferIdeProjectModel = true;
    
    /**
     * Creates analyzer with default parsers loaded via ServiceLoader.
     */
    public ClasspathAnalyzer() {
        this.parsers = new ArrayList<>();
        this.parsers.add(new MavenPomParser());
        this.parsers.add(new GradleBuildParser());
        this.parsers.add(new EclipseClasspathParser());
        this.parsers.add(new JarManifestScanner());
        loadExtensionParsers();
        Collections.sort(this.parsers);
    }
    
    /**
     * Creates analyzer with custom parsers.
     */
    public ClasspathAnalyzer(List<DependencyParser> parsers) {
        this.parsers = new ArrayList<>(parsers);
        Collections.sort(this.parsers);
    }
    
    private void loadExtensionParsers() {
        try {
            ServiceLoader<DependencyParserProvider> loader = ServiceLoader.load(DependencyParserProvider.class);
            List<DependencyParserProvider> providers = new ArrayList<>();
            loader.forEach(providers::add);
            providers.sort(Comparator.comparingInt(DependencyParserProvider::getPriority));
            
            for (DependencyParserProvider provider : providers) {
                List<DependencyParser> extensionParsers = provider.getParsers();
                if (extensionParsers != null) {
                    this.parsers.addAll(extensionParsers);
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Failed to load extension parsers: " + e.getMessage());
        }
    }
    
    /**
     * Enables/disables fallback JAR scanning when no build files found.
     */
    public void setEnableFallbackScanning(boolean enable) {
        this.enableFallbackScanning = enable;
    }
    
    /**
     * Enables/disables preference for IDE-backed parsers when available.
     */
    public void setPreferIdeProjectModel(boolean preferIdeProjectModel) {
        this.preferIdeProjectModel = preferIdeProjectModel;
    }
    
    /**
     * Analyzes project for dependencies.
     * @param projectPath project directory or build file
     * @return analysis result with detected dependencies
     */
    public ClasspathAnalysisResult analyze(String projectPath) {
        return analyze(new File(projectPath));
    }
    
    /**
     * Analyzes project for dependencies.
     * @param projectPath project directory or build file
     * @return analysis result with detected dependencies
     */
    public ClasspathAnalysisResult analyze(File projectPath) {
        long startTime = System.currentTimeMillis();
        
        if (!projectPath.exists()) {
            throw new IllegalArgumentException("Path does not exist: " + projectPath);
        }
        
        List<DependencyInfo> allDependencies = new ArrayList<>();
        Set<String> detectionMethods = new LinkedHashSet<>();
        int jarsScanned = 0;
        
        for (ParserTier tier : ParserTier.values()) {
            List<DependencyInfo> tierDependencies = new ArrayList<>();
            Set<String> tierDetectionMethods = new LinkedHashSet<>();
            int tierJarsScanned = 0;
            
            for (DependencyParser parser : getParsersForTier(tier, projectPath)) {
                if (parser.canParse(projectPath)) {
                    try {
                        List<DependencyInfo> dependencies = parser.parse(projectPath);
                        if (!dependencies.isEmpty()) {
                            tierDependencies.addAll(dependencies);
                            tierDetectionMethods.add(parser.getParserName());
                            if (parser instanceof JarManifestScanner) {
                                tierJarsScanned = dependencies.size();
                            }
                        }
                    } catch (ParserException e) {
                        System.err.println("Warning: " + parser.getParserName() + " failed: " + e.getMessage());
                    }
                }
            }
            
            if (!tierDependencies.isEmpty()) {
                allDependencies.addAll(tierDependencies);
                detectionMethods.addAll(tierDetectionMethods);
                jarsScanned = Math.max(jarsScanned, tierJarsScanned);
                
                if (tier != ParserTier.BINARY_FALLBACK) {
                    break;
                }
            }
        }
        
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
        
        allDependencies = removeDuplicates(allDependencies);
        long endTime = System.currentTimeMillis();
        
        return ClasspathAnalysisResult.builder()
            .addDependencies(allDependencies)
            .totalJarsScanned(jarsScanned)
            .analysisTimeMs(endTime - startTime)
            .detectionMethod(String.join(", ", detectionMethods))
            .build();
    }
    
    private List<DependencyParser> getParsersForTier(ParserTier tier, File projectPath) {
        List<DependencyParser> tierParsers = new ArrayList<>();
        boolean ideProjectModelAvailable = preferIdeProjectModel && hasParserForTier(ParserTier.IDE_MODEL, projectPath);
        
        for (DependencyParser parser : parsers) {
            if (parser.getTier() != tier) {
                continue;
            }
            
            if (shouldSkipParserForTier(parser, tier, ideProjectModelAvailable)) {
                continue;
            }
            
            tierParsers.add(parser);
        }
        
        tierParsers.sort(DependencyParser::compareTo);
        return tierParsers;
    }
    
    private boolean hasParserForTier(ParserTier tier, File projectPath) {
        for (DependencyParser parser : parsers) {
            if (parser.getTier() == tier && parser.canParse(projectPath)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean shouldSkipParserForTier(DependencyParser parser, ParserTier tier, boolean ideProjectModelAvailable) {
        if (!ideProjectModelAvailable) {
            return false;
        }
        
        return tier == ParserTier.BUILD_FILE &&
               (parser instanceof MavenPomParser ||
                parser instanceof GradleBuildParser ||
                parser instanceof EclipseClasspathParser);
    }
    
    private List<DependencyInfo> scanRecursively(File directory, JarManifestScanner scanner) {
        List<DependencyInfo> dependencies = new ArrayList<>();
        
        File[] files = directory.listFiles();
        if (files == null) {
            return dependencies;
        }
        
        for (File file : files) {
            if (file.isDirectory()) {
                String name = file.getName();
                if (!name.startsWith(".") && !name.equals("node_modules") &&
                    !name.equals("test") && !name.equals("tests")) {
                    dependencies.addAll(scanRecursively(file, scanner));
                }
            } else if (file.getName().endsWith(".jar")) {
                try {
                    dependencies.addAll(scanner.parse(file));
                } catch (ParserException ignored) {
                }
            }
        }
        
        return dependencies;
    }
    
    private List<DependencyInfo> removeDuplicates(List<DependencyInfo> dependencies) {
        Map<String, DependencyInfo> uniqueDeps = new LinkedHashMap<>();
        
        for (DependencyInfo dep : dependencies) {
            String key = getDeduplicationKey(dep);
            
            uniqueDeps.merge(key, dep, this::selectBetterDependency);
        }
        
        return new ArrayList<>(uniqueDeps.values());
    }
    
    private String getDeduplicationKey(DependencyInfo dep) {
        String groupId = dep.getGroupId();
        String artifactId = dep.getArtifactId();
        
        if (groupId != null && !groupId.isEmpty()) {
            return groupId + ":" + artifactId;
        }
        return artifactId != null ? artifactId : "";
    }
    
    private DependencyInfo selectBetterDependency(DependencyInfo a, DependencyInfo b) {
        int scoreA = calculateCompletenessScore(a);
        int scoreB = calculateCompletenessScore(b);
        return scoreA >= scoreB ? a : b;
    }
    
    private int calculateCompletenessScore(DependencyInfo dep) {
        int score = 0;
        if (dep.getGroupId() != null && !dep.getGroupId().isEmpty()) score += 3;
        if (dep.getVersion() != null && !dep.getVersion().isEmpty()) score += 2;
        if (dep.getJarPath() != null && !dep.getJarPath().isEmpty()) score += 1;
        
        DependencySource source = dep.getSource();
        if (source == DependencySource.MAVEN || source == DependencySource.GRADLE) score += 5;
        else if (source == DependencySource.ECLIPSE) score += 3;
        
        return score;
    }
    
    /**
     * Detects project type based on files present.
     * @param projectPath project directory
     * @return list of detected project types
     */
    public List<String> detectProjectType(File projectPath) {
        List<String> types = new ArrayList<>();
        
        if (!projectPath.isDirectory()) {
            return types;
        }
        
        if (new File(projectPath, "pom.xml").exists()) types.add("Maven");
        if (new File(projectPath, "build.gradle").exists() ||
            new File(projectPath, "build.gradle.kts").exists()) types.add("Gradle");
        if (new File(projectPath, ".classpath").exists()) types.add("Eclipse");
        if (new File(projectPath, ".idea").exists()) types.add("IntelliJ");
        if (new File(projectPath, ".vscode").exists()) types.add("VS Code");
        
        return types;
    }
    
    /**
     * Checks if project has Jakarta EE, Java EE, or MicroProfile dependencies.
     * @param projectPath project directory
     * @return true if EE dependencies found
     */
    public boolean hasEEDependencies(File projectPath) {
        ClasspathAnalysisResult result = analyze(projectPath);
        return !result.getJakartaEEDependencies().isEmpty() ||
               !result.getJavaEEDependencies().isEmpty() ||
               !result.getMicroProfileDependencies().isEmpty();
    }
}

