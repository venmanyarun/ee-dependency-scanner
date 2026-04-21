package io.openliberty.tools.scanner.parser;

import io.openliberty.tools.scanner.api.DependencyInfo;
import io.openliberty.tools.scanner.api.ParserException;
import io.openliberty.tools.scanner.api.DependencySource;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for Gradle dependency tree output.
 * Executes 'gradle dependencies' and parses the output to extract all dependencies including transitives.
 */
public class GradleDependencyTreeParser implements CoreDependencyParser<File> {
    
    private static final String BUILD_GRADLE = "build.gradle";
    private static final String BUILD_GRADLE_KTS = "build.gradle.kts";
    
    // Pattern to match dependency lines in Gradle output
    // Example: +--- jakarta.servlet:jakarta.servlet-api:6.0.0
    // Example: |    \--- jakarta.transaction:jakarta.transaction-api:2.0.1
    private static final Pattern DEPENDENCY_PATTERN = Pattern.compile(
        "[+\\\\|\\s-]+\\s*([^:]+):([^:]+):([^\\s]+)"
    );
    
    private boolean enableTransitiveResolution = true;
    private String configuration = "compileClasspath";
    
    /**
     * Sets whether to resolve transitive dependencies by running Gradle.
     * 
     * @param enable true to resolve transitives, false for direct dependencies only
     */
    public void setEnableTransitiveResolution(boolean enable) {
        this.enableTransitiveResolution = enable;
    }
    
    /**
     * Sets the Gradle configuration to analyze.
     * Default is "compileClasspath".
     * 
     * @param configuration the configuration name (e.g., "compileClasspath", "runtimeClasspath")
     */
    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }
    
    @Override
    public List<DependencyInfo> parse(File path) throws ParserException {
        File buildFile = findBuildFile(path);
        if (buildFile == null) {
            throw new ParserException("No build.gradle or build.gradle.kts found in: " + path);
        }
        
        if (!enableTransitiveResolution) {
            // Fall back to basic Gradle parser for direct dependencies only
            return new GradleBuildParser().parse(path);
        }
        
        return parseDependencyTree(buildFile.getParentFile());
    }
    
    @Override
    public boolean canParse(File path) {
        return findBuildFile(path) != null;
    }
    
    @Override
    public String getName() {
        return "Gradle Dependency Tree";
    }

    
    /**
     * Finds build.gradle or build.gradle.kts file in the given path.
     */
    private File findBuildFile(File path) {
        if (path.isFile()) {
            String name = path.getName();
            if (name.equals(BUILD_GRADLE) || name.equals(BUILD_GRADLE_KTS)) {
                return path;
            }
        }
        
        if (path.isDirectory()) {
            File buildGradle = new File(path, BUILD_GRADLE);
            if (buildGradle.exists()) {
                return buildGradle;
            }
            
            File buildGradleKts = new File(path, BUILD_GRADLE_KTS);
            if (buildGradleKts.exists()) {
                return buildGradleKts;
            }
        }
        
        return null;
    }
    
    /**
     * Executes Gradle dependencies and parses the output.
     */
    private List<DependencyInfo> parseDependencyTree(File projectDir) throws ParserException {
        List<DependencyInfo> dependencies = new ArrayList<>();
        
        try {
            // Execute Gradle dependencies command
            ProcessBuilder pb = new ProcessBuilder(
                getGradleCommand(),
                "dependencies",
                "--configuration", configuration
            );
            pb.directory(projectDir);
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            // Read output
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                
                String line;
                boolean inDependencySection = false;
                
                while ((line = reader.readLine()) != null) {
                    // Start parsing after we see the configuration header
                    if (line.contains(configuration)) {
                        inDependencySection = true;
                        continue;
                    }
                    
                    // Stop at next configuration or empty line after dependencies
                    if (inDependencySection && line.trim().isEmpty()) {
                        break;
                    }
                    
                    if (inDependencySection) {
                        DependencyInfo depInfo = parseDependencyLine(line);
                        if (depInfo != null) {
                            dependencies.add(depInfo);
                        }
                    }
                }
            }
            
            int exitCode = process.waitFor();
            if (exitCode != 0 && dependencies.isEmpty()) {
                throw new ParserException("Gradle dependencies command failed with exit code: " + exitCode);
            }
            
        } catch (Exception e) {
            throw new ParserException("Failed to execute Gradle dependencies", e);
        }
        
        return dependencies;
    }
    
    /**
     * Parses a single dependency line from Gradle output.
     */
    private DependencyInfo parseDependencyLine(String line) {
        // Skip lines that don't contain dependencies
        if (!line.contains(":") || line.contains("(*)")  || line.contains("FAILED")) {
            return null;
        }
        
        Matcher matcher = DEPENDENCY_PATTERN.matcher(line);
        if (!matcher.find()) {
            return null;
        }
        
        String groupId = matcher.group(1).trim();
        String artifactId = matcher.group(2).trim();
        String version = matcher.group(3).trim();
        
        // Clean up version (remove -> and other markers)
        version = version.split("->")[0].trim();
        version = version.replaceAll("[()]", "").trim();
        
        return DependencyInfo.builder()
            .groupId(groupId)
            .artifactId(artifactId)
            .version(version)
            .source(DependencySource.GRADLE)
            .build();
    }
    
    /**
     * Gets the Gradle command based on the operating system and project.
     */
    private String getGradleCommand() {
        String os = System.getProperty("os.name").toLowerCase();
        
        // Check for Gradle wrapper first
        File gradlew = new File(os.contains("win") ? "gradlew.bat" : "gradlew");
        if (gradlew.exists()) {
            return gradlew.getAbsolutePath();
        }
        
        // Fall back to system Gradle
        if (os.contains("win")) {
            return "gradle.bat";
        }
        return "gradle";
    }
}

