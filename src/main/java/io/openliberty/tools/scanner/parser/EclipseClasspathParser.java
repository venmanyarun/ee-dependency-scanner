package io.openliberty.tools.scanner.parser;

import io.openliberty.tools.scanner.model.DependencyInfo;
import io.openliberty.tools.scanner.model.DependencySource;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for Eclipse .classpath files.
 * Extracts dependencies with M2_REPO variable resolution.
 */
public class EclipseClasspathParser implements DependencyParser {
    
    private static final String CLASSPATH_FILE = ".classpath";
    private static final String M2_REPO = "M2_REPO";
    
    @Override
    public int getPriority() {
        return 100; // Medium priority - IDE-specific
    }
    
    // Pattern to extract Maven coordinates from path
    // Example: M2_REPO/jakarta/servlet/jakarta.servlet-api/6.0.0/jakarta.servlet-api-6.0.0.jar
    private static final Pattern MAVEN_PATH_PATTERN = Pattern.compile(
        "([^/]+)/([^/]+)/([^/]+)/([^/]+\\.jar)$"
    );
    
    @Override
    public List<DependencyInfo> parse(File path) throws ParserException {
        File classpathFile = findClasspathFile(path);
        if (classpathFile == null) {
            throw new ParserException("No .classpath found in: " + path);
        }
        
        try {
            SAXReader reader = new SAXReader();
            Document document = reader.read(classpathFile);
            Element root = document.getRootElement();
            
            List<DependencyInfo> dependencies = new ArrayList<>();
            
            // Parse classpath entries
            for (Element entry : root.elements("classpathentry")) {
                String kind = entry.attributeValue("kind");
                String entryPath = entry.attributeValue("path");
                
                if (entryPath == null) {
                    continue;
                }
                
                // Handle variable entries (M2_REPO)
                if ("var".equals(kind) && entryPath.startsWith(M2_REPO)) {
                    DependencyInfo depInfo = parseMavenPath(entryPath);
                    if (depInfo != null) {
                        dependencies.add(depInfo);
                    }
                }
                
                // Handle library entries
                if ("lib".equals(kind)) {
                    DependencyInfo depInfo = parseLibraryPath(entryPath);
                    if (depInfo != null) {
                        dependencies.add(depInfo);
                    }
                }
            }
            
            return dependencies;
            
        } catch (Exception e) {
            throw new ParserException("Failed to parse .classpath: " + classpathFile, e);
        }
    }
    
    @Override
    public boolean canParse(File path) {
        return findClasspathFile(path) != null;
    }
    
    @Override
    public String getParserName() {
        return "Eclipse";
    }
    
    /**
     * Finds .classpath file in the given path.
     */
    private File findClasspathFile(File path) {
        if (path.isFile() && path.getName().equals(CLASSPATH_FILE)) {
            return path;
        }
        if (path.isDirectory()) {
            File classpathFile = new File(path, CLASSPATH_FILE);
            if (classpathFile.exists()) {
                return classpathFile;
            }
        }
        return null;
    }
    
    /**
     * Parses Maven coordinates from M2_REPO path.
     * Example: M2_REPO/jakarta/servlet/jakarta.servlet-api/6.0.0/jakarta.servlet-api-6.0.0.jar
     */
    private DependencyInfo parseMavenPath(String path) {
        // Remove M2_REPO prefix
        String relativePath = path.substring(M2_REPO.length() + 1);
        
        // Split path into components
        String[] parts = relativePath.split("/");
        if (parts.length < 4) {
            return null;
        }
        
        // Extract version (second to last component)
        String version = parts[parts.length - 2];
        
        // Extract artifactId (third to last component)
        String artifactId = parts[parts.length - 3];
        
        // Extract groupId (everything before artifactId)
        StringBuilder groupIdBuilder = new StringBuilder();
        for (int i = 0; i < parts.length - 3; i++) {
            if (i > 0) groupIdBuilder.append(".");
            groupIdBuilder.append(parts[i]);
        }
        String groupId = groupIdBuilder.toString();
        
        return DependencyInfo.builder()
            .groupId(groupId)
            .artifactId(artifactId)
            .version(version)
            .source(DependencySource.ECLIPSE)
            .jarPath(path)
            .build();
    }
    
    /**
     * Parses library path to extract dependency information.
     */
    private DependencyInfo parseLibraryPath(String path) {
        // Try to extract Maven coordinates from path
        Matcher matcher = MAVEN_PATH_PATTERN.matcher(path);
        if (matcher.find()) {
            String groupId = matcher.group(1).replace('/', '.');
            String artifactId = matcher.group(2);
            String version = matcher.group(3);
            
            return DependencyInfo.builder()
                .groupId(groupId)
                .artifactId(artifactId)
                .version(version)
                .source(DependencySource.ECLIPSE)
                .jarPath(path)
                .build();
        }
        
        // If we can't extract Maven coordinates, create a basic entry
        String fileName = new File(path).getName();
        if (fileName.endsWith(".jar")) {
            String artifactId = fileName.substring(0, fileName.length() - 4);
            
            return DependencyInfo.builder()
                .artifactId(artifactId)
                .source(DependencySource.ECLIPSE)
                .jarPath(path)
                .build();
        }
        
        return null;
    }
}

