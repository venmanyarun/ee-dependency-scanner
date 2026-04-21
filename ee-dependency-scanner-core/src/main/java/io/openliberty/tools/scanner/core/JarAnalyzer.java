package io.openliberty.tools.scanner.core;

import io.openliberty.tools.scanner.api.DependencyInfo;
import io.openliberty.tools.scanner.api.DependencyFilter;
import io.openliberty.tools.scanner.util.JarDependencyExtractor;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Reusable utility for analyzing JAR files.
 * This is a standalone utility that can be used by any parser implementation.
 */
public class JarAnalyzer {
    
    private final JarDependencyExtractor extractor;
    
    /**
     * Creates a new JarAnalyzer with default extractor.
     */
    public JarAnalyzer() {
        this.extractor = new JarDependencyExtractor();
    }
    
    /**
     * Creates a new JarAnalyzer with a custom extractor.
     * @param extractor custom JAR dependency extractor
     */
    public JarAnalyzer(JarDependencyExtractor extractor) {
        this.extractor = extractor;
    }
    
    /**
     * Extract dependency info from JAR file.
     * @param jarFile JAR file to analyze
     * @return dependency information extracted from JAR
     */
    public List<DependencyInfo> analyzeJar(File jarFile) {
        return extractor.extract(jarFile);
    }
    
    /**
     * Extract dependencies from JAR with filtering.
     * @param jarFile JAR file to analyze
     * @param filter filter to select specific dependencies
     * @return filtered list of dependencies
     */
    public List<DependencyInfo> analyzeJar(File jarFile, DependencyFilter filter) {
        List<DependencyInfo> allDeps = extractor.extract(jarFile);
        
        if (filter == null || filter == DependencyFilter.includeAll()) {
            return allDeps;
        }
        
        return allDeps.stream()
            .filter(filter::matches)
            .collect(Collectors.toList());
    }
}


