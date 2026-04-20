package io.openliberty.tools.scanner.parser;

import io.openliberty.tools.scanner.model.DependencyInfo;
import io.openliberty.tools.scanner.model.DependencySource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for Gradle build files (build.gradle and build.gradle.kts).
 * Performs single-pass parsing with variable resolution.
 */
public class GradleBuildParser implements DependencyParser {
    
    private static final String BUILD_GRADLE = "build.gradle";
    private static final String BUILD_GRADLE_KTS = "build.gradle.kts";
    
    @Override
    public int getPriority() {
        return 20; // High priority - Gradle is authoritative
    }
    
    // Patterns for dependency declarations
    private static final Pattern DEPENDENCY_PATTERN = Pattern.compile(
        "(?:implementation|api|compile|compileOnly|runtimeOnly|testImplementation|testCompile)\\s*[\\(\\s]+" +
        "['\"]([^:]+):([^:]+):([^'\"]+)['\"]"
    );
    
    // Pattern for variable declarations
    private static final Pattern VARIABLE_PATTERN = Pattern.compile(
        "(?:def|val|var)\\s+(\\w+)\\s*=\\s*['\"]([^'\"]+)['\"]"
    );
    
    // Pattern for property references
    private static final Pattern PROPERTY_PATTERN = Pattern.compile(
        "\\$\\{?([\\w.]+)\\}?"
    );
    
    @Override
    public List<DependencyInfo> parse(File path) throws ParserException {
        File buildFile = findBuildFile(path);
        if (buildFile == null) {
            throw new ParserException("No build.gradle or build.gradle.kts found in: " + path);
        }
        
        DependencySource source = buildFile.getName().endsWith(".kts") ? 
            DependencySource.GRADLE_KOTLIN : DependencySource.GRADLE;
        
        try {
            Map<String, String> variables = new HashMap<>();
            List<DependencyInfo> dependencies = new ArrayList<>();
            
            try (BufferedReader reader = new BufferedReader(new FileReader(buildFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    
                    // Skip comments and empty lines
                    if (line.isEmpty() || line.startsWith("//") || line.startsWith("/*")) {
                        continue;
                    }
                    
                    // Extract variables
                    Matcher varMatcher = VARIABLE_PATTERN.matcher(line);
                    if (varMatcher.find()) {
                        variables.put(varMatcher.group(1), varMatcher.group(2));
                    }
                    
                    // Extract dependencies
                    Matcher depMatcher = DEPENDENCY_PATTERN.matcher(line);
                    if (depMatcher.find()) {
                        String groupId = resolveVariables(depMatcher.group(1), variables);
                        String artifactId = resolveVariables(depMatcher.group(2), variables);
                        String version = resolveVariables(depMatcher.group(3), variables);
                        
                        DependencyInfo depInfo = DependencyInfo.builder()
                            .groupId(groupId)
                            .artifactId(artifactId)
                            .version(version)
                            .source(source)
                            .build();
                        
                        dependencies.add(depInfo);
                    }
                }
            }
            
            return dependencies;
            
        } catch (IOException e) {
            throw new ParserException("Failed to parse Gradle build file: " + buildFile, e);
        }
    }
    
    @Override
    public boolean canParse(File path) {
        return findBuildFile(path) != null;
    }
    
    @Override
    public String getParserName() {
        return "Gradle";
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
     * Resolves variable references in a string.
     */
    private String resolveVariables(String value, Map<String, String> variables) {
        if (value == null || (!value.contains("$") && !value.contains("{"))) {
            return value;
        }
        
        String resolved = value;
        Matcher matcher = PROPERTY_PATTERN.matcher(value);
        
        while (matcher.find()) {
            String varName = matcher.group(1);
            String varValue = variables.get(varName);
            
            if (varValue != null) {
                String placeholder = matcher.group(0);
                resolved = resolved.replace(placeholder, varValue);
            }
        }
        
        return resolved;
    }
}

