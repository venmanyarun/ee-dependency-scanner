package io.openliberty.tools.scanner.parser;

import io.openliberty.tools.scanner.model.DependencyInfo;
import io.openliberty.tools.scanner.model.DependencySource;
import io.openliberty.tools.scanner.util.JarDependencyExtractor;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.jar.Attributes;

/**
 * Scanner for JAR files and their MANIFEST.MF files.
 * Extracts dependency information from JAR manifests, file names, and embedded metadata.
 * Supports transitive dependency extraction from:
 * - Embedded pom.xml files
 * - MANIFEST.MF Class-Path attribute
 * - OSGi Bundle metadata
 */
public class JarManifestScanner implements DependencyParser {
    
    private static final String[] JAR_EXTENSIONS = {".jar", ".war", ".ear"};
    private static final String[] LIBRARY_DIRS = {"lib", "libs", "target", "build"};
    
    private boolean extractTransitiveDependencies = true;
    
    @Override
    public int getPriority() {
        return 200; // Low priority - fallback parser
    }
    
    /**
     * Sets whether to extract transitive dependencies from JARs.
     * When enabled, scans embedded pom.xml, MANIFEST Class-Path, and OSGi metadata.
     *
     * @param extract true to extract transitive dependencies, false otherwise
     */
    public void setExtractTransitiveDependencies(boolean extract) {
        this.extractTransitiveDependencies = extract;
    }
    
    @Override
    public List<DependencyInfo> parse(File path) throws ParserException {
        List<DependencyInfo> dependencies = new ArrayList<>();
        
        if (path.isFile() && isJarFile(path)) {
            // Single JAR file
            DependencyInfo depInfo = scanJarFile(path);
            if (depInfo != null) {
                dependencies.add(depInfo);
            }
            
            // Extract transitive dependencies if enabled
            if (extractTransitiveDependencies) {
                List<DependencyInfo> transitiveDeps = JarDependencyExtractor.extractDependencies(path);
                dependencies.addAll(transitiveDeps);
            }
        } else if (path.isDirectory()) {
            // Scan directory for JARs
            scanDirectory(path, dependencies);
        }
        
        return dependencies;
    }
    
    @Override
    public boolean canParse(File path) {
        if (path.isFile()) {
            return isJarFile(path);
        }
        if (path.isDirectory()) {
            // Check if directory contains JARs or is a library directory
            return containsJars(path) || isLibraryDirectory(path);
        }
        return false;
    }
    
    @Override
    public String getParserName() {
        return "JAR Scanner";
    }
    
    /**
     * Scans a directory recursively for JAR files.
     */
    private void scanDirectory(File directory, List<DependencyInfo> dependencies) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        
        for (File file : files) {
            if (file.isFile() && isJarFile(file)) {
                DependencyInfo depInfo = scanJarFile(file);
                if (depInfo != null) {
                    dependencies.add(depInfo);
                }
                
                // Extract transitive dependencies if enabled
                if (extractTransitiveDependencies) {
                    List<DependencyInfo> transitiveDeps = JarDependencyExtractor.extractDependencies(file);
                    dependencies.addAll(transitiveDeps);
                }
            } else if (file.isDirectory() && isLibraryDirectory(file)) {
                // Recursively scan library directories
                scanDirectory(file, dependencies);
            }
        }
    }
    
    /**
     * Scans a single JAR file for dependency information.
     */
    private DependencyInfo scanJarFile(File jarFile) {
        try (JarFile jar = new JarFile(jarFile)) {
            Manifest manifest = jar.getManifest();
            
            if (manifest != null) {
                Attributes mainAttributes = manifest.getMainAttributes();
                
                // Try to extract Maven coordinates from manifest
                String groupId = getManifestValue(mainAttributes, 
                    "Implementation-Vendor-Id", "Bundle-SymbolicName");
                String artifactId = getManifestValue(mainAttributes,
                    "Implementation-Title", "Bundle-Name");
                String version = getManifestValue(mainAttributes,
                    "Implementation-Version", "Bundle-Version");
                
                // If we found coordinates in manifest, use them
                if (artifactId != null) {
                    return DependencyInfo.builder()
                        .groupId(groupId)
                        .artifactId(cleanArtifactId(artifactId))
                        .version(version)
                        .source(DependencySource.MANIFEST)
                        .jarPath(jarFile.getAbsolutePath())
                        .build();
                }
            }
            
            // Fallback: extract from filename
            return extractFromFilename(jarFile);
            
        } catch (IOException e) {
            // If we can't read the JAR, try to extract from filename
            return extractFromFilename(jarFile);
        }
    }
    
    /**
     * Gets a manifest attribute value, trying multiple possible attribute names.
     */
    private String getManifestValue(Attributes attributes, String... names) {
        for (String name : names) {
            String value = attributes.getValue(name);
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }
    
    /**
     * Extracts dependency information from JAR filename.
     * Handles patterns like: artifactId-version.jar
     */
    private DependencyInfo extractFromFilename(File jarFile) {
        String fileName = jarFile.getName();
        
        // Remove extension
        for (String ext : JAR_EXTENSIONS) {
            if (fileName.endsWith(ext)) {
                fileName = fileName.substring(0, fileName.length() - ext.length());
                break;
            }
        }
        
        // Try to split into artifactId and version
        // Common patterns: name-1.0.0, name-1.0, name_1.0.0
        String[] parts = fileName.split("[-_](?=\\d)");
        
        String artifactId = parts[0];
        String version = parts.length > 1 ? parts[1] : null;
        
        return DependencyInfo.builder()
            .artifactId(artifactId)
            .version(version)
            .source(DependencySource.JAR_SCAN)
            .jarPath(jarFile.getAbsolutePath())
            .build();
    }
    
    /**
     * Cleans artifact ID by removing common suffixes and prefixes.
     */
    private String cleanArtifactId(String artifactId) {
        if (artifactId == null) {
            return null;
        }
        
        // Remove common patterns
        return artifactId
            .replaceAll("\\s+", "-")
            .toLowerCase();
    }
    
    /**
     * Checks if a file is a JAR file.
     */
    private boolean isJarFile(File file) {
        String name = file.getName().toLowerCase();
        for (String ext : JAR_EXTENSIONS) {
            if (name.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Checks if a directory contains JAR files.
     */
    private boolean containsJars(File directory) {
        File[] files = directory.listFiles();
        if (files == null) {
            return false;
        }
        
        for (File file : files) {
            if (file.isFile() && isJarFile(file)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Checks if a directory is a known library directory.
     */
    private boolean isLibraryDirectory(File directory) {
        String name = directory.getName().toLowerCase();
        for (String libDir : LIBRARY_DIRS) {
            if (name.equals(libDir)) {
                return true;
            }
        }
        return false;
    }
}

