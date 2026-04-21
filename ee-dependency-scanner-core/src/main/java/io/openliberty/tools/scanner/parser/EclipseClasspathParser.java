package io.openliberty.tools.scanner.parser;

import io.openliberty.tools.scanner.api.DependencyInfo;
import io.openliberty.tools.scanner.api.ParserException;
import io.openliberty.tools.scanner.api.DependencySource;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for Eclipse .classpath files with M2_REPO resolution.
 */
public class EclipseClasspathParser implements CoreDependencyParser<File> {
    
    private static final String CLASSPATH_FILE = ".classpath";
    private static final String M2_REPO = "M2_REPO";
    
    public int getPriority() {
        return 100;
    }
    
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
            
            for (Element entry : root.elements("classpathentry")) {
                String kind = entry.attributeValue("kind");
                String entryPath = entry.attributeValue("path");
                
                if (entryPath == null) continue;
                
                if ("var".equals(kind) && entryPath.startsWith(M2_REPO)) {
                    DependencyInfo depInfo = parseMavenPath(entryPath);
                    if (depInfo != null) {
                        dependencies.add(depInfo);
                    }
                }
                
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
    public String getName() {
        return "Eclipse";
    }

    
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
    
    private DependencyInfo parseMavenPath(String path) {
        String relativePath = path.substring(M2_REPO.length() + 1);
        String[] parts = relativePath.split("/");
        if (parts.length < 4) return null;
        
        String version = parts[parts.length - 2];
        String artifactId = parts[parts.length - 3];
        
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
    
    private DependencyInfo parseLibraryPath(String path) {
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

