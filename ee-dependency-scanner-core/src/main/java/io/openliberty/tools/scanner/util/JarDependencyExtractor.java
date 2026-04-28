package io.openliberty.tools.scanner.util;

import io.openliberty.tools.scanner.api.DependencyInfo;
import io.openliberty.tools.scanner.api.DependencySource;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.jar.Attributes;

/**
 * Extractor for transitive dependencies from JAR files.
 *
 * Extracts dependencies via:
 * - Embedded pom.xml
 * - MANIFEST.MF Class-Path
 * - OSGi metadata
 */
public class JarDependencyExtractor {
    
    private static final JarDependencyExtractor DEFAULT_INSTANCE = new JarDependencyExtractor();
    
    // Thread-local SAXReader for XML parsing (reusable, not thread-safe)
    private static final ThreadLocal<SAXReader> SAX_READER = ThreadLocal.withInitial(() -> {
        SAXReader reader = new SAXReader();
        try {
            // Disable external entity resolution for security
            reader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            reader.setFeature("http://xml.org/sax/features/external-general-entities", false);
            reader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        } catch (Exception e) {
            // Ignore if features not supported
        }
        return reader;
    });
    
    // Cache for parsed JAR files
    private final Map<String, List<DependencyInfo>> cache;
    
    public JarDependencyExtractor() {
        this(true);
    }
    
    public JarDependencyExtractor(boolean enableCaching) {
        this.cache = enableCaching ? new ConcurrentHashMap<>() : null;
    }
    
    /**
     * Extracts all dependencies from JAR file (static convenience method).
     * @param jarFile JAR file to analyze
     * @return list of dependencies found
     */
    public static List<DependencyInfo> extractDependencies(File jarFile) {
        return DEFAULT_INSTANCE.extract(jarFile);
    }
    
    /**
     * Extracts dependencies from multiple JAR files in parallel.
     * @param jarFiles list of JAR files to analyze
     * @return map of file path to dependencies
     */
    public static Map<String, List<DependencyInfo>> extractDependenciesBatch(List<File> jarFiles) {
        return DEFAULT_INSTANCE.extractBatch(jarFiles);
    }
    
    /**
     * Instance method to extract dependencies from JAR (cached).
     * @param jarFile JAR file to analyze
     * @return list of dependencies found
     */
    public List<DependencyInfo> extract(File jarFile) {
        if (cache != null) {
            String cacheKey = jarFile.getAbsolutePath() + ":" + jarFile.lastModified();
            return cache.computeIfAbsent(cacheKey, k -> extractInternal(jarFile));
        }
        return extractInternal(jarFile);
    }
    
    /**
     * Extracts dependencies from multiple JAR files in parallel.
     * @param jarFiles list of JAR files
     * @return map of file path to dependencies
     */
    public Map<String, List<DependencyInfo>> extractBatch(List<File> jarFiles) {
        Map<String, List<DependencyInfo>> results = new ConcurrentHashMap<>();
        
        jarFiles.parallelStream().forEach(jarFile -> {
            List<DependencyInfo> deps = extract(jarFile);
            results.put(jarFile.getAbsolutePath(), deps);
        });
        
        return results;
    }
    
    /**
     * Clears the cache.
     */
    public void clearCache() {
        if (cache != null) {
            cache.clear();
        }
    }
    
    private List<DependencyInfo> extractInternal(File jarFile) {
        List<DependencyInfo> dependencies = new ArrayList<>();
        
        try (JarFile jar = new JarFile(jarFile)) {
            // Extract from embedded POM (most reliable)
            List<DependencyInfo> pomDeps = extractFromEmbeddedPom(jar);
            dependencies.addAll(pomDeps);
            
            // Extract from MANIFEST Class-Path
            List<DependencyInfo> manifestDeps = extractFromManifestClassPath(jar);
            dependencies.addAll(manifestDeps);
            
            // Extract from OSGi metadata
            List<DependencyInfo> osgiBundleDeps = extractFromOSGiMetadata(jar);
            dependencies.addAll(osgiBundleDeps);
            
        } catch (IOException e) {
            System.err.println("Failed to extract dependencies from JAR: " + jarFile + " - " + e.getMessage());
        }
        
        return dependencies;
    }
    
    protected List<DependencyInfo> extractFromEmbeddedPom(JarFile jar) {
        List<DependencyInfo> dependencies = new ArrayList<>();
        
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            
            if (name.startsWith("META-INF/maven/") && name.endsWith("/pom.xml")) {
                try (InputStream is = jar.getInputStream(entry)) {
                    dependencies.addAll(parsePomXml(is));
                    // Only parse the first pom.xml found
                    break;
                } catch (Exception e) {
                    System.err.println("Failed to parse embedded pom.xml: " + name + " - " + e.getMessage());
                }
            }
        }
        
        return dependencies;
    }
    
    protected List<DependencyInfo> parsePomXml(InputStream pomStream) throws Exception {
        List<DependencyInfo> dependencies = new ArrayList<>();
        
        SAXReader reader = SAX_READER.get();
        Document document = reader.read(pomStream);
        Element root = document.getRootElement();
        
        Map<String, String> properties = extractProperties(root);
        
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
    
    protected Map<String, String> extractProperties(Element root) {
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
    
    protected DependencyInfo parseDependency(Element dependency, Map<String, String> properties) {
        Element groupIdElement = dependency.element("groupId");
        Element artifactIdElement = dependency.element("artifactId");
        Element versionElement = dependency.element("version");
        Element scopeElement = dependency.element("scope");
        
        if (groupIdElement == null || artifactIdElement == null) return null;
        
        String scope = scopeElement != null ? scopeElement.getTextTrim() : "compile";
        
        // Skip test and provided dependencies
        if ("test".equals(scope) || "provided".equals(scope)) return null;
        
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
    
    protected String resolveProperty(String value, Map<String, String> properties) {
        if (value == null || !value.contains("${")) return value;
        
        String resolved = value;
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            resolved = resolved.replace(placeholder, entry.getValue());
        }
        
        return resolved;
    }
    
    protected List<DependencyInfo> extractFromManifestClassPath(JarFile jar) {
        List<DependencyInfo> dependencies = new ArrayList<>();
        
        try {
            Manifest manifest = jar.getManifest();
            if (manifest == null) return dependencies;
            
            Attributes mainAttributes = manifest.getMainAttributes();
            String classPath = mainAttributes.getValue("Class-Path");
            
            if (classPath != null && !classPath.trim().isEmpty()) {
                String[] jarPaths = classPath.trim().split("\\s+");
                
                for (String jarPath : jarPaths) {
                    if (jarPath.endsWith(".jar")) {
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
    
    protected DependencyInfo extractFromJarFilename(String jarPath) {
        String filename = new File(jarPath).getName();
        
        if (filename.endsWith(".jar")) {
            filename = filename.substring(0, filename.length() - 4);
        }
        
        // Try to parse Maven-style filename: artifactId-version.jar
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
            return DependencyInfo.builder()
                .artifactId(filename)
                .source(DependencySource.MANIFEST)
                .jarPath(jarPath)
                .build();
        }
    }
    
    protected List<DependencyInfo> extractFromOSGiMetadata(JarFile jar) {
        List<DependencyInfo> dependencies = new ArrayList<>();
        
        try {
            Manifest manifest = jar.getManifest();
            if (manifest == null) return dependencies;
            
            Attributes mainAttributes = manifest.getMainAttributes();
            
            String bundleSymbolicName = mainAttributes.getValue("Bundle-SymbolicName");
            if (bundleSymbolicName == null) return dependencies;
            
            String requireBundle = mainAttributes.getValue("Require-Bundle");
            if (requireBundle != null) {
                dependencies.addAll(parseOSGiRequireBundle(requireBundle));
            }
            
        } catch (IOException e) {
            System.err.println("Failed to read OSGi metadata: " + e.getMessage());
        }
        
        return dependencies;
    }
    
    protected List<DependencyInfo> parseOSGiRequireBundle(String requireBundle) {
        List<DependencyInfo> dependencies = new ArrayList<>();
        
        // Split by comma, but respect quoted strings
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

// Made with Bob
