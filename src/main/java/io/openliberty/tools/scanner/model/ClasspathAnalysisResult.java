package io.openliberty.tools.scanner.model;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents the complete result of a classpath analysis.
 * Contains all detected dependencies, categorized by type, along with metadata about the analysis.
 */
public class ClasspathAnalysisResult {
    private final List<DependencyInfo> allDependencies;
    private final List<DependencyInfo> jakartaEEDependencies;
    private final List<DependencyInfo> javaEEDependencies;
    private final List<DependencyInfo> microProfileDependencies;
    private final int totalDependenciesFound;
    private final int totalJarsScanned;
    private final long analysisTimeMs;
    private final String detectionMethod;
    private final Map<String, Set<String>> jakartaEEVersions;
    private final Map<String, Set<String>> microProfileVersions;

    private ClasspathAnalysisResult(Builder builder) {
        this.allDependencies = Collections.unmodifiableList(new ArrayList<>(builder.allDependencies));
        this.jakartaEEDependencies = Collections.unmodifiableList(
            allDependencies.stream()
                .filter(DependencyInfo::isJakartaEE)
                .collect(Collectors.toList())
        );
        this.javaEEDependencies = Collections.unmodifiableList(
            allDependencies.stream()
                .filter(DependencyInfo::isJavaEE)
                .collect(Collectors.toList())
        );
        this.microProfileDependencies = Collections.unmodifiableList(
            allDependencies.stream()
                .filter(DependencyInfo::isMicroProfile)
                .collect(Collectors.toList())
        );
        this.totalDependenciesFound = allDependencies.size();
        this.totalJarsScanned = builder.totalJarsScanned;
        this.analysisTimeMs = builder.analysisTimeMs;
        this.detectionMethod = builder.detectionMethod;
        this.jakartaEEVersions = detectJakartaEEVersions();
        this.microProfileVersions = detectMicroProfileVersions();
    }

    /**
     * Detects Jakarta EE platform versions based on dependency versions.
     * Also includes Java EE dependencies for version detection.
     * Returns a map of feature names to their detected versions.
     */
    private Map<String, Set<String>> detectJakartaEEVersions() {
        Map<String, Set<String>> versions = new HashMap<>();
        
        // Include both Jakarta EE and Java EE dependencies
        for (DependencyInfo dep : jakartaEEDependencies) {
            String feature = extractFeatureName(dep.getArtifactId());
            String version = dep.getVersion();
            
            if (feature != null && version != null) {
                versions.computeIfAbsent(feature, k -> new HashSet<>()).add(version);
            }
        }
        
        for (DependencyInfo dep : javaEEDependencies) {
            String feature = extractFeatureName(dep.getArtifactId());
            String version = dep.getVersion();
            
            if (feature != null && version != null) {
                versions.computeIfAbsent(feature, k -> new HashSet<>()).add(version);
            }
        }
        
        return Collections.unmodifiableMap(versions);
    }

    /**
     * Detects MicroProfile platform versions based on dependency versions.
     * Returns a map of feature names to their detected versions.
     */
    private Map<String, Set<String>> detectMicroProfileVersions() {
        Map<String, Set<String>> versions = new HashMap<>();
        
        for (DependencyInfo dep : microProfileDependencies) {
            String feature = extractFeatureName(dep.getArtifactId());
            String version = dep.getVersion();
            
            if (feature != null && version != null) {
                versions.computeIfAbsent(feature, k -> new HashSet<>()).add(version);
            }
        }
        
        return Collections.unmodifiableMap(versions);
    }

    /**
     * Extracts feature name from artifact ID.
     * Example: "jakarta.servlet-api" -> "servlet"
     */
    private String extractFeatureName(String artifactId) {
        if (artifactId == null) return null;
        
        // Remove common suffixes
        String name = artifactId
            .replaceAll("-api$", "")
            .replaceAll("-spec$", "")
            .replaceAll("^jakarta\\.", "")
            .replaceAll("^javax\\.", "")
            .replaceAll("^microprofile-", "");
        
        return name;
    }

    /**
     * Gets the detected Jakarta EE platform version(s).
     * Returns a set of versions if multiple are detected (mixed versions scenario).
     */
    public Set<String> getJakartaEEPlatformVersions() {
        return inferPlatformVersion(jakartaEEVersions);
    }

    /**
     * Gets the detected MicroProfile platform version(s).
     * Returns a set of versions if multiple are detected (mixed versions scenario).
     */
    public Set<String> getMicroProfilePlatformVersions() {
        return inferPlatformVersion(microProfileVersions);
    }

    /**
     * Infers platform version from feature versions.
     * This is a simplified implementation - real version detection would use
     * a mapping table of feature versions to platform versions.
     */
    private Set<String> inferPlatformVersion(Map<String, Set<String>> featureVersions) {
        Set<String> platformVersions = new HashSet<>();
        
        // Collect all major versions
        for (Set<String> versions : featureVersions.values()) {
            for (String version : versions) {
                String majorVersion = extractMajorVersion(version);
                if (majorVersion != null) {
                    platformVersions.add(majorVersion);
                }
            }
        }
        
        return platformVersions;
    }

    /**
     * Extracts major version from a version string.
     * Example: "6.0.0" -> "6", "5.0" -> "5"
     */
    private String extractMajorVersion(String version) {
        if (version == null) return null;
        
        int dotIndex = version.indexOf('.');
        if (dotIndex > 0) {
            return version.substring(0, dotIndex);
        }
        return version;
    }

    /**
     * Gets detailed version information for Jakarta EE features.
     * Returns a map of feature names to their detected versions.
     */
    public Map<String, Set<String>> getJakartaEEFeatureVersions() {
        return jakartaEEVersions;
    }

    /**
     * Gets detailed version information for MicroProfile features.
     * Returns a map of feature names to their detected versions.
     */
    public Map<String, Set<String>> getMicroProfileFeatureVersions() {
        return microProfileVersions;
    }

    /**
     * Checks if multiple versions of Jakarta EE features are detected.
     */
    public boolean hasMultipleJakartaEEVersions() {
        return jakartaEEVersions.values().stream()
            .anyMatch(versions -> versions.size() > 1);
    }

    /**
     * Checks if multiple versions of MicroProfile features are detected.
     */
    public boolean hasMultipleMicroProfileVersions() {
        return microProfileVersions.values().stream()
            .anyMatch(versions -> versions.size() > 1);
    }

    // Getters
    public List<DependencyInfo> getAllDependencies() {
        return allDependencies;
    }

    public List<DependencyInfo> getJakartaEEDependencies() {
        return jakartaEEDependencies;
    }

    public List<DependencyInfo> getJavaEEDependencies() {
        return javaEEDependencies;
    }

    public List<DependencyInfo> getMicroProfileDependencies() {
        return microProfileDependencies;
    }

    public int getTotalDependenciesFound() {
        return totalDependenciesFound;
    }

    public int getTotalJarsScanned() {
        return totalJarsScanned;
    }

    public long getAnalysisTimeMs() {
        return analysisTimeMs;
    }

    public String getDetectionMethod() {
        return detectionMethod;
    }

    /**
     * Returns a summary of the analysis result.
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Classpath Analysis Summary:\n");
        sb.append("  Total Dependencies: ").append(totalDependenciesFound).append("\n");
        sb.append("  Jakarta EE: ").append(jakartaEEDependencies.size()).append("\n");
        sb.append("  Java EE: ").append(javaEEDependencies.size()).append("\n");
        sb.append("  MicroProfile: ").append(microProfileDependencies.size()).append("\n");
        sb.append("  JARs Scanned: ").append(totalJarsScanned).append("\n");
        sb.append("  Analysis Time: ").append(analysisTimeMs).append("ms\n");
        sb.append("  Detection Method: ").append(detectionMethod).append("\n");
        
        Set<String> jakartaVersions = getJakartaEEPlatformVersions();
        if (!jakartaVersions.isEmpty()) {
            sb.append("  Jakarta EE Versions: ").append(jakartaVersions).append("\n");
        }
        
        Set<String> mpVersions = getMicroProfilePlatformVersions();
        if (!mpVersions.isEmpty()) {
            sb.append("  MicroProfile Versions: ").append(mpVersions).append("\n");
        }
        
        return sb.toString();
    }

    @Override
    public String toString() {
        return getSummary();
    }

    /**
     * Creates a new Builder for constructing ClasspathAnalysisResult instances.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for ClasspathAnalysisResult.
     */
    public static class Builder {
        private List<DependencyInfo> allDependencies = new ArrayList<>();
        private int totalJarsScanned = 0;
        private long analysisTimeMs = 0;
        private String detectionMethod = "Unknown";

        public Builder addDependency(DependencyInfo dependency) {
            if (dependency != null) {
                this.allDependencies.add(dependency);
            }
            return this;
        }

        public Builder addDependencies(Collection<DependencyInfo> dependencies) {
            if (dependencies != null) {
                this.allDependencies.addAll(dependencies);
            }
            return this;
        }

        public Builder totalJarsScanned(int totalJarsScanned) {
            this.totalJarsScanned = totalJarsScanned;
            return this;
        }

        public Builder analysisTimeMs(long analysisTimeMs) {
            this.analysisTimeMs = analysisTimeMs;
            return this;
        }

        public Builder detectionMethod(String detectionMethod) {
            this.detectionMethod = detectionMethod;
            return this;
        }

        public ClasspathAnalysisResult build() {
            return new ClasspathAnalysisResult(this);
        }
    }
}

