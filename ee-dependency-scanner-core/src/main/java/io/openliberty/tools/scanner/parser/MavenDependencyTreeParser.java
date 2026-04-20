package io.openliberty.tools.scanner.parser;

import io.openliberty.tools.scanner.model.DependencyInfo;
import io.openliberty.tools.scanner.model.DependencySource;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for Maven dependency tree output.
 * Executes 'mvn dependency:tree' and parses the output to extract all dependencies including transitives.
 */
public class MavenDependencyTreeParser implements DependencyParser {
    
    private static final String POM_XML = "pom.xml";
    
    // Pattern to match dependency lines in Maven tree output
    // Example: [INFO] +- jakarta.servlet:jakarta.servlet-api:jar:6.0.0:compile
    private static final Pattern DEPENDENCY_PATTERN = Pattern.compile(
        "\\[INFO\\]\\s+[+\\\\|-]+\\s+([^:]+):([^:]+):([^:]+):([^:]+):([^:]+)"
    );
    
    private boolean enableTransitiveResolution = true;
    
    /**
     * Sets whether to resolve transitive dependencies by running Maven.
     * 
     * @param enable true to resolve transitives, false for direct dependencies only
     */
    public void setEnableTransitiveResolution(boolean enable) {
        this.enableTransitiveResolution = enable;
    }
    
    @Override
    public List<DependencyInfo> parse(File path) throws ParserException {
        File pomFile = findPomFile(path);
        if (pomFile == null) {
            throw new ParserException("No pom.xml found in: " + path);
        }
        
        if (!enableTransitiveResolution) {
            // Fall back to basic POM parser for direct dependencies only
            return new MavenPomParser().parse(path);
        }
        
        return parseDependencyTree(pomFile.getParentFile());
    }
    
    @Override
    public boolean canParse(File path) {
        return findPomFile(path) != null;
    }
    
    @Override
    public String getParserName() {
        return "Maven Dependency Tree";
    }
    
    /**
     * Finds pom.xml file in the given path.
     */
    private File findPomFile(File path) {
        if (path.isFile() && path.getName().equals(POM_XML)) {
            return path;
        }
        if (path.isDirectory()) {
            File pomFile = new File(path, POM_XML);
            if (pomFile.exists()) {
                return pomFile;
            }
        }
        return null;
    }
    
    /**
     * Executes Maven dependency:tree and parses the output.
     */
    private List<DependencyInfo> parseDependencyTree(File projectDir) throws ParserException {
        List<DependencyInfo> dependencies = new ArrayList<>();
        
        try {
            // Execute Maven dependency:tree command
            ProcessBuilder pb = new ProcessBuilder(
                getMavenCommand(),
                "dependency:tree",
                "-DoutputType=text",
                "-DoutputFile=target/dependency-tree.txt"
            );
            pb.directory(projectDir);
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            // Read output
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                
                String line;
                while ((line = reader.readLine()) != null) {
                    DependencyInfo depInfo = parseDependencyLine(line);
                    if (depInfo != null) {
                        dependencies.add(depInfo);
                    }
                }
            }
            
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                // Maven command failed, try reading from file if it exists
                File treeFile = new File(projectDir, "target/dependency-tree.txt");
                if (treeFile.exists()) {
                    return parseTreeFile(treeFile);
                }
                throw new ParserException("Maven dependency:tree command failed with exit code: " + exitCode);
            }
            
        } catch (Exception e) {
            throw new ParserException("Failed to execute Maven dependency:tree", e);
        }
        
        return dependencies;
    }
    
    /**
     * Parses a single dependency line from Maven tree output.
     */
    private DependencyInfo parseDependencyLine(String line) {
        Matcher matcher = DEPENDENCY_PATTERN.matcher(line);
        if (!matcher.find()) {
            return null;
        }
        
        String groupId = matcher.group(1);
        String artifactId = matcher.group(2);
        String type = matcher.group(3); // jar, war, etc.
        String version = matcher.group(4);
        String scope = matcher.group(5);
        
        // Skip test scope dependencies
        if ("test".equals(scope)) {
            return null;
        }
        
        return DependencyInfo.builder()
            .groupId(groupId)
            .artifactId(artifactId)
            .version(version)
            .source(DependencySource.MAVEN)
            .build();
    }
    
    /**
     * Parses dependency tree from file.
     */
    private List<DependencyInfo> parseTreeFile(File treeFile) throws ParserException {
        List<DependencyInfo> dependencies = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new java.io.FileReader(treeFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                DependencyInfo depInfo = parseDependencyLine(line);
                if (depInfo != null) {
                    dependencies.add(depInfo);
                }
            }
        } catch (Exception e) {
            throw new ParserException("Failed to parse dependency tree file", e);
        }
        
        return dependencies;
    }
    
    /**
     * Gets the Maven command based on the operating system.
     */
    private String getMavenCommand() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return "mvn.cmd";
        }
        return "mvn";
    }
}

