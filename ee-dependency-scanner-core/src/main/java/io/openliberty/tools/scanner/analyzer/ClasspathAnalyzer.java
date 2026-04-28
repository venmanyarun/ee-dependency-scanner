package io.openliberty.tools.scanner.analyzer;

import io.openliberty.tools.scanner.api.DependencyAnalysisResult;
import io.openliberty.tools.scanner.api.DependencyInfo;
import io.openliberty.tools.scanner.api.DependencyParser;
import io.openliberty.tools.scanner.api.DependencySource;
import io.openliberty.tools.scanner.api.DependencyFilter;
import io.openliberty.tools.scanner.api.ParserException;
import io.openliberty.tools.scanner.parser.*;

import java.io.File;
import java.util.*;

/**
 * Analyzes project dependencies from various sources (Maven, Gradle, Eclipse, JARs).
 * <p>
 * Supports build tool preference for projects with multiple build configurations.
 * By default, Maven has higher priority than Gradle when both are present.
 * </p>
 */
public class ClasspathAnalyzer {
    
    private final List<CoreDependencyParser<?>> parsers;
    private final boolean enableFallbackScanning;
    private final BuildToolPreference buildToolPreference;
    
    /**
     * Private constructor used by builder.
     */
    private ClasspathAnalyzer(Builder builder) {
        this.parsers = builder.parsers;
        this.enableFallbackScanning = builder.enableFallbackScanning;
        this.buildToolPreference = builder.buildToolPreference;
        Collections.sort(this.parsers);
    }
    
    /**
     * Creates a new builder for ClasspathAnalyzer.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Gets the current build tool preference.
     *
     * @return the build tool preference
     */
    public BuildToolPreference getBuildToolPreference() {
        return buildToolPreference;
    }
    
    /**
     * Checks if fallback scanning is enabled.
     *
     * @return true if fallback scanning is enabled
     */
    public boolean isFallbackScanningEnabled() {
        return enableFallbackScanning;
    }
    
    
    /**
     * Analyzes project for dependencies.
     * @param projectPath project directory or build file
     * @return analysis result with detected dependencies
     */
    public DependencyAnalysisResult analyze(String projectPath) {
        return analyze(new File(projectPath));
    }
    
    /**
     * Analyzes project for dependencies.
     * @param projectPath project directory or build file
     * @return analysis result with detected dependencies
     */
    public DependencyAnalysisResult analyze(File projectPath) {
        return analyze(projectPath, DependencyFilter.includeAll(), buildToolPreference);
    }
    
    /**
     * Analyzes project for dependencies with build tool preference.
     *
     * @param projectPath project directory or build file
     * @param preference build tool preference for projects with multiple build files
     * @return analysis result with detected dependencies
     */
    public DependencyAnalysisResult analyze(File projectPath, BuildToolPreference preference) {
        return analyze(projectPath, DependencyFilter.includeAll(), preference);
    }
    
    /**
     * Analyzes project for dependencies with filtering.
     *
     * @param projectPath project directory or build file
     * @param filter filter to select specific dependencies
     * @return analysis result with detected dependencies
     */
    public DependencyAnalysisResult analyze(File projectPath, DependencyFilter filter) {
        return analyze(projectPath, filter, buildToolPreference);
    }
    
    /**
     * Analyzes project for dependencies with filtering and build tool preference.
     * This is the most flexible method for targeted dependency analysis.
     *
     * @param project the project to analyze (File, Module, IJavaProject, etc.)
     * @param filter filter to select specific dependencies
     * @param preference build tool preference for projects with multiple build files
     * @return analysis result with detected dependencies
     */
    public <T> DependencyAnalysisResult analyze(T project, DependencyFilter filter, BuildToolPreference preference) {
        long startTime = System.currentTimeMillis();
        
        // For File-based projects, check existence
        File projectPath = null;
        if (project instanceof File) {
            projectPath = (File) project;
            if (!projectPath.exists()) {
                throw new IllegalArgumentException("Path does not exist: " + projectPath);
            }
        }
        
        List<DependencyInfo> allDependencies = new ArrayList<>();
        Set<String> detectionMethods = new LinkedHashSet<>();
        int jarsScanned = 0;
        
        for (ParserTier tier : ParserTier.values()) {
            List<DependencyInfo> tierDependencies = new ArrayList<>();
            Set<String> tierDetectionMethods = new LinkedHashSet<>();
            int tierJarsScanned = 0;
            
            for (CoreDependencyParser<?> parser : getParsersForTier(tier, project)) {
                // Apply build tool preference filter
                if (!shouldUseParser(parser, project, preference)) {
                    continue;
                }
                
                if (canParseProject(parser, project)) {
                    try {
                        List<DependencyInfo> dependencies = parseProject(parser, project, filter);
                        if (!dependencies.isEmpty()) {
                            tierDependencies.addAll(dependencies);
                            tierDetectionMethods.add(parser.getName());
                            if (parser instanceof JarManifestScanner) {
                                tierJarsScanned = dependencies.size();
                            }
                        }
                    } catch (ParserException e) {
                        System.err.println("Warning: " + parser.getName() + " failed: " + e.getMessage());
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
        
        // Fallback scanning only for File-based projects
        if (allDependencies.isEmpty() && enableFallbackScanning && projectPath != null && projectPath.isDirectory()) {
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
        
        return DependencyAnalysisResult.builder()
            .addDependencies(allDependencies)
            .analysisTimeMs(endTime - startTime)
            .detectionMethod(String.join(", ", detectionMethods))
            .build();
    }
    
    /**
     * Type-safe helper to check if a parser can parse a project.
     */
    @SuppressWarnings("unchecked")
    private <T> boolean canParseProject(CoreDependencyParser<?> parser, T project) {
        try {
            DependencyParser<T> typedParser = (DependencyParser<T>) parser;
            return typedParser.canParse(project);
        } catch (ClassCastException e) {
            return false;
        }
    }
    
    /**
     * Type-safe helper to parse a project with a parser.
     */
    @SuppressWarnings("unchecked")
    private <T> List<DependencyInfo> parseProject(CoreDependencyParser<?> parser, T project, DependencyFilter filter) throws ParserException {
        DependencyParser<T> typedParser = (DependencyParser<T>) parser;
        return typedParser.parse(project, filter);
    }
    
    /**
     * Determines if a parser should be used based on build tool preference.
     */
    private <T> boolean shouldUseParser(CoreDependencyParser<?> parser, T project, BuildToolPreference preference) {
        // Only apply preference to File-based Maven/Gradle parsers
        if (!(project instanceof File)) {
            return true; // IDE parsers are not affected by build tool preference
        }
        
        File projectPath = (File) project;
        String parserName = parser.getName();
        boolean isMavenParser = parserName.contains("Maven");
        boolean isGradleParser = parserName.contains("Gradle");
        
        // Non-build-tool parsers are always allowed
        if (!isMavenParser && !isGradleParser) {
            return true;
        }
        
        // Check if both build files exist
        boolean hasMaven = hasBuildFile(projectPath, "pom.xml");
        boolean hasGradle = hasBuildFile(projectPath, "build.gradle") ||
                           hasBuildFile(projectPath, "build.gradle.kts");
        
        // If only one build tool exists, use it regardless of preference
        if (hasMaven && !hasGradle) {
            return isMavenParser;
        }
        if (hasGradle && !hasMaven) {
            return isGradleParser;
        }
        
        // If neither exists, allow both (for fallback scenarios)
        if (!hasMaven && !hasGradle) {
            return true;
        }
        
        // Both exist - apply preference
        switch (preference) {
            case MAVEN_ONLY:
                return isMavenParser;
            
            case GRADLE_ONLY:
                return isGradleParser;
            
            case PREFER_GRADLE:
                return isGradleParser; // Only use Gradle when both exist
            
            case AUTO:
            case PREFER_MAVEN:
            default:
                return isMavenParser; // Default: Maven has priority
        }
    }
    
    /**
     * Checks if a build file exists in the project directory.
     */
    private boolean hasBuildFile(File projectPath, String fileName) {
        if (projectPath.isDirectory()) {
            return new File(projectPath, fileName).exists();
        } else if (projectPath.isFile()) {
            // If projectPath is a file, check its parent directory
            File parent = projectPath.getParentFile();
            return parent != null && new File(parent, fileName).exists();
        }
        return false;
    }
    
    private <T> List<CoreDependencyParser<?>> getParsersForTier(ParserTier tier, T project) {
        List<CoreDependencyParser<?>> tierParsers = new ArrayList<>();
        
        for (CoreDependencyParser<?> parser : parsers) {
            if (parser.getTier() == tier) {
                tierParsers.add(parser);
            }
        }
        
        tierParsers.sort(CoreDependencyParser::compareTo);
        return tierParsers;
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
        DependencyAnalysisResult result = analyze(projectPath);
        return !result.getJakartaEEDependencies().isEmpty() ||
               !result.getJavaEEDependencies().isEmpty() ||
               !result.getMicroProfileDependencies().isEmpty();
    }
    
    /**
     * Builder for ClasspathAnalyzer with fluent API.
     */
    public static class Builder {
        private List<CoreDependencyParser<?>> parsers;
        private boolean enableFallbackScanning = true;
        private BuildToolPreference buildToolPreference = BuildToolPreference.AUTO;
        
        /**
         * Creates a new builder with default settings.
         */
        public Builder() {
            this.parsers = new ArrayList<>();
            this.parsers.add(new MavenPomParser());
            this.parsers.add(new GradleBuildParser());
            this.parsers.add(new EclipseClasspathParser());
            this.parsers.add(new JarManifestScanner());
            loadExtensionParsers();
        }
        
        /**
         * Sets custom parsers.
         *
         * @param parsers list of custom parsers
         * @return this builder
         */
        public Builder parsers(List<? extends DependencyParser<?>> parsers) {
            this.parsers = new ArrayList<>();
            for (DependencyParser<?> parser : parsers) {
                if (parser instanceof CoreDependencyParser) {
                    this.parsers.add((CoreDependencyParser<?>) parser);
                }
            }
            return this;
        }
        
        /**
         * Enables or disables fallback JAR scanning when no build files found.
         *
         * @param enable true to enable fallback scanning, false to disable
         * @return this builder
         */
        public Builder enableFallbackScanning(boolean enable) {
            this.enableFallbackScanning = enable;
            return this;
        }
        
        /**
         * Sets build tool preference for projects with multiple build configurations.
         * <p>
         * Use this when a project has both Maven (pom.xml) and Gradle (build.gradle) files
         * to explicitly specify which build tool's dependencies should be analyzed.
         * </p>
         *
         * @param preference the build tool preference (default: AUTO with Maven priority)
         * @return this builder
         */
        public Builder buildToolPreference(BuildToolPreference preference) {
            this.buildToolPreference = preference != null ? preference : BuildToolPreference.AUTO;
            return this;
        }
        
        /**
         * Builds the ClasspathAnalyzer instance.
         *
         * @return a new ClasspathAnalyzer instance
         */
        public ClasspathAnalyzer build() {
            return new ClasspathAnalyzer(this);
        }
        
        private void loadExtensionParsers() {
            try {
                ServiceLoader<DependencyParserProvider> loader = ServiceLoader.load(DependencyParserProvider.class);
                List<DependencyParserProvider> providers = new ArrayList<>();
                loader.forEach(providers::add);
                providers.sort(Comparator.comparingInt(DependencyParserProvider::getPriority));
                
                for (DependencyParserProvider provider : providers) {
                    List<CoreDependencyParser<?>> extensionParsers = provider.getParsers();
                    if (extensionParsers != null) {
                        this.parsers.addAll(extensionParsers);
                    }
                }
            } catch (Exception e) {
                System.err.println("Warning: Failed to load extension parsers: " + e.getMessage());
            }
        }
    }
}


