package io.openliberty.tools.scanner.api;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Complete result of dependency analysis with categorized dependencies and metadata.
 */
public class DependencyAnalysisResult {
    private final List<DependencyInfo> allDependencies;
    private final List<DependencyInfo> jakartaEEDependencies;
    private final List<DependencyInfo> javaEEDependencies;
    private final List<DependencyInfo> microProfileDependencies;
    private final int totalDependenciesFound;
    private final long analysisTimeMs;
    private final String detectionMethod;
    private final Map<String, Set<String>> jakartaEEVersions;
    private final Map<String, Set<String>> microProfileVersions;

    private DependencyAnalysisResult(Builder builder) {
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
        this.analysisTimeMs = builder.analysisTimeMs;
        this.detectionMethod = builder.detectionMethod;
        this.jakartaEEVersions = detectJakartaEEVersions();
        this.microProfileVersions = detectMicroProfileVersions();
    }

    private Map<String, Set<String>> detectJakartaEEVersions() {
        Map<String, Set<String>> versions = new HashMap<>();
        
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

    private String extractFeatureName(String artifactId) {
        if (artifactId == null) return null;
        
        return artifactId
            .replaceAll("-api$", "")
            .replaceAll("-spec$", "")
            .replaceAll("^jakarta\\.", "")
            .replaceAll("^javax\\.", "")
            .replaceAll("^microprofile-", "");
    }

    /**
     * Gets detected Jakarta EE platform versions.
     * @return set of platform versions (may contain multiple if mixed)
     */
    public Set<String> getJakartaEEPlatformVersions() {
        return inferPlatformVersion(jakartaEEVersions);
    }

    /**
     * Gets detected MicroProfile platform versions.
     * @return set of platform versions (may contain multiple if mixed)
     */
    public Set<String> getMicroProfilePlatformVersions() {
        return inferPlatformVersion(microProfileVersions);
    }

    private Set<String> inferPlatformVersion(Map<String, Set<String>> featureVersions) {
        Set<String> platformVersions = new HashSet<>();
        
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

    private String extractMajorVersion(String version) {
        if (version == null) return null;
        
        int dotIndex = version.indexOf('.');
        if (dotIndex > 0) {
            return version.substring(0, dotIndex);
        }
        return version;
    }

    /**
     * Gets detailed Jakarta EE feature versions.
     * @return map of feature names to their versions
     */
    public Map<String, Set<String>> getJakartaEEFeatureVersions() {
        return jakartaEEVersions;
    }

    /**
     * Gets detailed MicroProfile feature versions.
     * @return map of feature names to their versions
     */
    public Map<String, Set<String>> getMicroProfileFeatureVersions() {
        return microProfileVersions;
    }

    public boolean hasMultipleJakartaEEVersions() {
        return jakartaEEVersions.values().stream()
            .anyMatch(versions -> versions.size() > 1);
    }

    public boolean hasMultipleMicroProfileVersions() {
        return microProfileVersions.values().stream()
            .anyMatch(versions -> versions.size() > 1);
    }

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

    public long getAnalysisTimeMs() {
        return analysisTimeMs;
    }

    public String getDetectionMethod() {
        return detectionMethod;
    }

    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Dependency Analysis Summary:\n");
        sb.append("  Total Dependencies: ").append(totalDependenciesFound).append("\n");
        sb.append("  Jakarta EE: ").append(jakartaEEDependencies.size()).append("\n");
        sb.append("  Java EE: ").append(javaEEDependencies.size()).append("\n");
        sb.append("  MicroProfile: ").append(microProfileDependencies.size()).append("\n");
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<DependencyInfo> allDependencies = new ArrayList<>();
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

        public Builder analysisTimeMs(long analysisTimeMs) {
            this.analysisTimeMs = analysisTimeMs;
            return this;
        }

        public Builder detectionMethod(String detectionMethod) {
            this.detectionMethod = detectionMethod;
            return this;
        }

        public DependencyAnalysisResult build() {
            return new DependencyAnalysisResult(this);
        }
    }
}


