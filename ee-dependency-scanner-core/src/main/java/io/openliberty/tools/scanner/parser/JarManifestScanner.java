package io.openliberty.tools.scanner.parser;

import io.openliberty.tools.scanner.api.DependencyInfo;
import io.openliberty.tools.scanner.api.ParserException;
import io.openliberty.tools.scanner.api.DependencySource;
import io.openliberty.tools.scanner.util.JarDependencyExtractor;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.jar.Attributes;

/**
 * Scanner for JAR files with manifest and transitive dependency extraction.
 */
public class JarManifestScanner implements CoreDependencyParser<File> {
    
    private static final String[] JAR_EXTENSIONS = {".jar", ".war", ".ear"};
    private static final String[] LIBRARY_DIRS = {"lib", "libs", "target", "build"};
    
    private boolean extractTransitiveDependencies = true;
    
    public int getPriority() {
        return 200;
    }
    
    /**
     * Enables/disables transitive dependency extraction from JARs.
     * @param extract true to extract transitive dependencies
     */
    public void setExtractTransitiveDependencies(boolean extract) {
        this.extractTransitiveDependencies = extract;
    }
    
    @Override
    public List<DependencyInfo> parse(File path) throws ParserException {
        List<DependencyInfo> dependencies = new ArrayList<>();
        
        if (path.isFile() && isJarFile(path)) {
            DependencyInfo depInfo = scanJarFile(path);
            if (depInfo != null) {
                dependencies.add(depInfo);
            }
            
            if (extractTransitiveDependencies) {
                List<DependencyInfo> transitiveDeps = JarDependencyExtractor.extractDependencies(path);
                dependencies.addAll(transitiveDeps);
            }
        } else if (path.isDirectory()) {
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
            return containsJars(path) || isLibraryDirectory(path);
        }
        return false;
    }
    
    @Override
    public String getName() {
        return "JAR Scanner";
    }

    
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
                
                if (extractTransitiveDependencies) {
                    List<DependencyInfo> transitiveDeps = JarDependencyExtractor.extractDependencies(file);
                    dependencies.addAll(transitiveDeps);
                }
            } else if (file.isDirectory() && isLibraryDirectory(file)) {
                scanDirectory(file, dependencies);
            }
        }
    }
    
    private DependencyInfo scanJarFile(File jarFile) {
        try (JarFile jar = new JarFile(jarFile)) {
            Manifest manifest = jar.getManifest();
            
            if (manifest != null) {
                Attributes mainAttributes = manifest.getMainAttributes();
                
                String groupId = getManifestValue(mainAttributes,
                    "Implementation-Vendor-Id", "Bundle-SymbolicName");
                String artifactId = getManifestValue(mainAttributes,
                    "Implementation-Title", "Bundle-Name");
                String version = getManifestValue(mainAttributes,
                    "Implementation-Version", "Bundle-Version");
                
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
            
            return extractFromFilename(jarFile);
            
        } catch (IOException e) {
            return extractFromFilename(jarFile);
        }
    }
    
    private String getManifestValue(Attributes attributes, String... names) {
        for (String name : names) {
            String value = attributes.getValue(name);
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }
    
    private DependencyInfo extractFromFilename(File jarFile) {
        String fileName = jarFile.getName();
        
        for (String ext : JAR_EXTENSIONS) {
            if (fileName.endsWith(ext)) {
                fileName = fileName.substring(0, fileName.length() - ext.length());
                break;
            }
        }
        
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
    
    private String cleanArtifactId(String artifactId) {
        if (artifactId == null) return null;
        
        return artifactId
            .replaceAll("\\s+", "-")
            .toLowerCase();
    }
    
    private boolean isJarFile(File file) {
        String name = file.getName().toLowerCase();
        for (String ext : JAR_EXTENSIONS) {
            if (name.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
    
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

