package io.openliberty.tools.scanner.parser;

import io.openliberty.tools.scanner.model.DependencyInfo;
import io.openliberty.tools.scanner.model.DependencySource;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.util.*;

/**
 * Parser for Maven pom.xml files with property resolution.
 */
public class MavenPomParser implements DependencyParser {
    
    private static final String POM_XML = "pom.xml";
    
    @Override
    public int getPriority() {
        return 10;
    }
    
    @Override
    public List<DependencyInfo> parse(File path) throws ParserException {
        File pomFile = findPomFile(path);
        if (pomFile == null) {
            throw new ParserException("No pom.xml found in: " + path);
        }
        
        try {
            SAXReader reader = new SAXReader();
            Document document = reader.read(pomFile);
            Element root = document.getRootElement();
            
            // Extract properties for variable resolution
            Map<String, String> properties = extractProperties(root);
            
            // Parse dependencies
            List<DependencyInfo> dependencies = new ArrayList<>();
            Element dependenciesElement = root.element("dependencies");
            
            if (dependenciesElement != null) {
                for (Element dependency : dependenciesElement.elements("dependency")) {
                    DependencyInfo depInfo = parseDependency(dependency, properties);
                    if (depInfo != null) {
                        dependencies.add(depInfo);
                    }
                }
            }
            
            Element dependencyManagement = root.element("dependencyManagement");
            if (dependencyManagement != null) {
                Element managedDeps = dependencyManagement.element("dependencies");
                if (managedDeps != null) {
                    for (Element dependency : managedDeps.elements("dependency")) {
                        DependencyInfo depInfo = parseDependency(dependency, properties);
                        if (depInfo != null) {
                            dependencies.add(depInfo);
                        }
                    }
                }
            }
            
            return dependencies;
            
        } catch (Exception e) {
            throw new ParserException("Failed to parse pom.xml: " + pomFile, e);
        }
    }
    
    @Override
    public boolean canParse(File path) {
        return findPomFile(path) != null;
    }
    
    @Override
    public String getParserName() {
        return "Maven";
    }
    
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
    
    private Map<String, String> extractProperties(Element root) {
        Map<String, String> properties = new HashMap<>();
        
        Element propertiesElement = root.element("properties");
        if (propertiesElement != null) {
            for (Element property : propertiesElement.elements()) {
                properties.put(property.getName(), property.getTextTrim());
            }
        }
        
        Element groupId = root.element("groupId");
        if (groupId != null) {
            properties.put("project.groupId", groupId.getTextTrim());
        }
        
        Element artifactId = root.element("artifactId");
        if (artifactId != null) {
            properties.put("project.artifactId", artifactId.getTextTrim());
        }
        
        Element version = root.element("version");
        if (version != null) {
            properties.put("project.version", version.getTextTrim());
        }
        
        return properties;
    }
    
    private DependencyInfo parseDependency(Element dependency, Map<String, String> properties) {
        Element groupIdElement = dependency.element("groupId");
        Element artifactIdElement = dependency.element("artifactId");
        Element versionElement = dependency.element("version");
        
        if (groupIdElement == null || artifactIdElement == null) return null;
        
        String groupId = resolveProperty(groupIdElement.getTextTrim(), properties);
        String artifactId = resolveProperty(artifactIdElement.getTextTrim(), properties);
        String version = versionElement != null ? 
            resolveProperty(versionElement.getTextTrim(), properties) : null;
        
        return DependencyInfo.builder()
            .groupId(groupId)
            .artifactId(artifactId)
            .version(version)
            .source(DependencySource.MAVEN)
            .build();
    }
    
    private String resolveProperty(String value, Map<String, String> properties) {
        if (value == null || !value.contains("${")) return value;
        
        String resolved = value;
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            resolved = resolved.replace(placeholder, entry.getValue());
        }
        
        return resolved;
    }
}

