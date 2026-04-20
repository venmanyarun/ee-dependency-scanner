package io.openliberty.tools.scanner.util;

import io.openliberty.tools.scanner.model.DependencyInfo;
import io.openliberty.tools.scanner.model.DependencySource;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.jar.Attributes;

/**
 * Extracts transitive dependencies from JAR files by examining:
 * 1. Embedded pom.xml (META-INF/maven/groupId/artifactId/pom.xml)
 * 2. MANIFEST.MF Class-Path attribute
 * 3. OSGi Bundle metadata
 *
 * This class can be instantiated for testing with custom extractors,
 * or used via static methods for backward compatibility.
 */
public class JarDependencyExtractor {
    
    private static final JarDependencyExtractor DEFAULT_INSTANCE = new JarDependencyExtractor();
    
    /**
     * Extracts all dependencies (direct and transitive) from a JAR file.
     * Static method for backward compatibility.
     *
     * @param jarFile the JAR file to analyze
     * @return list of dependencies found in the JAR
     */
    public static List<DependencyInfo> extractDependencies(File jarFile) {
        return DEFAULT_INSTANCE.extract(jarFile);
    }
    
    /**
     * Instance method to extract all dependencies from a JAR file.
     *
     * @param jarFile the JAR file to analyze
     * @return list of dependencies found in the JAR
     */
    public List<DependencyInfo> extract(File jarFile) {
        List<DependencyInfo> dependencies = new ArrayList<>();
        
        try (JarFile jar = new JarFile(jarFile)) {
            // 1. Try to extract from embedded pom.xml
            List<DependencyInfo> pomDeps = extractFromEmbeddedPom(jar);
            dependencies.addAll(pomDeps);
            
            // 2. Try to extract from MANIFEST.MF Class-Path
            List<DependencyInfo> manifestDeps = extractFromManifestClassPath(jar);
            dependencies.addAll(manifestDeps);
            
            // 3. Try to extract from OSGi metadata
            List<DependencyInfo> osgiBundleDeps = extractFromOSGiMetadata(jar);
            dependencies.addAll(osgiBundleDeps);
            
        } catch (IOException e) {
            System.err.println("Failed to extract dependencies from JAR: " + jarFile + " - " + e.getMessage());
        }
        
        return dependencies;
    }
    
    /**
     * Extracts dependencies from embedded pom.xml in JAR.
     * Maven JARs typically include pom.xml at META-INF/maven/groupId/artifactId/pom.xml
     */
    protected List<DependencyInfo> extractFromEmbeddedPom(JarFile jar) {
        List<DependencyInfo> dependencies = new ArrayList<>();
        
        // Find pom.xml entries
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            
            // Look for pom.xml in META-INF/maven/
            if (name.startsWith("META-INF/maven/") && name.endsWith("/pom.xml")) {
                try (InputStream is = jar.getInputStream(entry)) {
                    dependencies.addAll(parsePomXml(is));
                } catch (Exception e) {
                    System.err.println("Failed to parse embedded pom.xml: " + name + " - " + e.getMessage());
                }
            }
        }
        
        return dependencies;
    }
    
    /**
     * Parses pom.xml InputStream and extracts dependencies.
     */
    protected List<DependencyInfo> parsePomXml(InputStream pomStream) throws Exception {
        List<DependencyInfo> dependencies = new ArrayList<>();
        
        SAXReader reader = new SAXReader();
        Document document = reader.read(pomStream);
        Element root = document.getRootElement();
        
        // Extract properties for variable resolution
        Map<String, String> properties = extractProperties(root);
        
        // Parse dependencies
        Element dependenciesElement = root.element("dependencies");
        if (dependenciesElement != null) {
            for (Element dependency : dependenciesElement.elements("dependency")) {
                DependencyInfo depInfo = parseDependency(dependency, properties);
                if (depInfo != null) {
                    dependencies.add(depInfo);
                }
            }
        }
        
        return dependencies;
    }
    
    /**
     * Extracts properties from POM for variable resolution.
     */
    protected Map<String, String> extractProperties(Element root) {
        Map<String, String> properties = new HashMap<>();
        
        Element propertiesElement = root.element("properties");
        if (propertiesElement != null) {
            for (Element property : propertiesElement.elements()) {
                properties.put(property.getName(), property.getTextTrim());
            }
        }
        
        // Add standard Maven properties
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
    
    /**
     * Parses a single dependency element.
     */
    protected DependencyInfo parseDependency(Element dependency, Map<String, String> properties) {
        Element groupIdElement = dependency.element("groupId");
        Element artifactIdElement = dependency.element("artifactId");
        Element versionElement = dependency.element("version");
        Element scopeElement = dependency.element("scope");
        
        if (groupIdElement == null || artifactIdElement == null) {
            return null;
        }
        
        String scope = scopeElement != null ? scopeElement.getTextTrim() : "compile";
        
        // Skip test and provided dependencies for transitive resolution
        if ("test".equals(scope) || "provided".equals(scope)) {
            return null;
        }
        
        String groupId = resolveProperty(groupIdElement.getTextTrim(), properties);
        String artifactId = resolveProperty(artifactIdElement.getTextTrim(), properties);
        String version = versionElement != null ? 
            resolveProperty(versionElement.getTextTrim(), properties) : null;
        
        return DependencyInfo.builder()
            .groupId(groupId)
            .artifactId(artifactId)
            .version(version)
            .source(DependencySource.MANIFEST)
            .build();
    }
    
    /**
     * Resolves Maven property placeholders.
     */
    protected String resolveProperty(String value, Map<String, String> properties) {
        if (value == null || !value.contains("${")) {
            return value;
        }
        
        String resolved = value;
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            resolved = resolved.replace(placeholder, entry.getValue());
        }
        
        return resolved;
    }
    
    /**
     * Extracts dependencies from MANIFEST.MF Class-Path attribute.
     * The Class-Path attribute lists JAR files that this JAR depends on.
     */
    protected List<DependencyInfo> extractFromManifestClassPath(JarFile jar) {
        List<DependencyInfo> dependencies = new ArrayList<>();
        
        try {
            Manifest manifest = jar.getManifest();
            if (manifest == null) {
                return dependencies;
            }
            
            Attributes mainAttributes = manifest.getMainAttributes();
            String classPath = mainAttributes.getValue("Class-Path");
            
            if (classPath != null && !classPath.trim().isEmpty()) {
                // Class-Path contains space-separated JAR file names or paths
                String[] jarPaths = classPath.trim().split("\\s+");
                
                for (String jarPath : jarPaths) {
                    if (jarPath.endsWith(".jar")) {
                        // Extract artifact info from JAR filename
                        DependencyInfo depInfo = extractFromJarFilename(jarPath);
                        if (depInfo != null) {
                            dependencies.add(depInfo);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to read MANIFEST.MF: " + e.getMessage());
        }
        
        return dependencies;
    }
    
    /**
     * Extracts dependency info from JAR filename.
     * Handles patterns like: artifactId-version.jar, groupId-artifactId-version.jar
     */
    protected DependencyInfo extractFromJarFilename(String jarPath) {
        // Get just the filename
        String filename = new File(jarPath).getName();
        
        // Remove .jar extension
        if (filename.endsWith(".jar")) {
            filename = filename.substring(0, filename.length() - 4);
        }
        
        // Try to split into artifactId and version
        // Common patterns: name-1.0.0.jar, name-1.0.jar
        String[] parts = filename.split("-(?=\\d)");
        
        if (parts.length >= 2) {
            String artifactId = parts[0];
            String version = parts[1];
            
            return DependencyInfo.builder()
                .artifactId(artifactId)
                .version(version)
                .source(DependencySource.MANIFEST)
                .jarPath(jarPath)
                .build();
        } else {
            // No version found, just use filename as artifactId
            return DependencyInfo.builder()
                .artifactId(filename)
                .source(DependencySource.MANIFEST)
                .jarPath(jarPath)
                .build();
        }
    }
    
    /**
     * Extracts dependencies from OSGi Bundle metadata.
     * OSGi bundles declare dependencies via Require-Bundle and Import-Package.
     */
    protected List<DependencyInfo> extractFromOSGiMetadata(JarFile jar) {
        List<DependencyInfo> dependencies = new ArrayList<>();
        
        try {
            Manifest manifest = jar.getManifest();
            if (manifest == null) {
                return dependencies;
            }
            
            Attributes mainAttributes = manifest.getMainAttributes();
            
            // Check if this is an OSGi bundle
            String bundleSymbolicName = mainAttributes.getValue("Bundle-SymbolicName");
            if (bundleSymbolicName == null) {
                return dependencies; // Not an OSGi bundle
            }
            
            // Extract from Require-Bundle
            String requireBundle = mainAttributes.getValue("Require-Bundle");
            if (requireBundle != null) {
                dependencies.addAll(parseOSGiRequireBundle(requireBundle));
            }
            
            // Note: Import-Package is more complex and typically doesn't map directly
            // to Maven dependencies, so we skip it for now
            
        } catch (IOException e) {
            System.err.println("Failed to read OSGi metadata: " + e.getMessage());
        }
        
        return dependencies;
    }
    
    /**
     * Parses OSGi Require-Bundle header.
     * Format: bundle1;bundle-version="[1.0.0,2.0.0)",bundle2;bundle-version="1.5.0"
     */
    protected List<DependencyInfo> parseOSGiRequireBundle(String requireBundle) {
        List<DependencyInfo> dependencies = new ArrayList<>();
        
        // Split by comma (but not within quotes or brackets)
        String[] bundles = requireBundle.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        
        for (String bundle : bundles) {
            String[] parts = bundle.split(";");
            String bundleName = parts[0].trim();
            
            String version = null;
            for (int i = 1; i < parts.length; i++) {
                String part = parts[i].trim();
                if (part.startsWith("bundle-version=")) {
                    version = part.substring("bundle-version=".length())
                        .replaceAll("[\"\\[\\]\\(\\)]", "")
                        .split(",")[0]; // Take first version if range
                }
            }
            
            dependencies.add(DependencyInfo.builder()
                .artifactId(bundleName)
                .version(version)
                .source(DependencySource.MANIFEST)
                .build());
        }
        
        return dependencies;
    }
}

