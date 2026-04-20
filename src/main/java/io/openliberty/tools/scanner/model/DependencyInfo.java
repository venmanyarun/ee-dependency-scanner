package io.openliberty.tools.scanner.model;

import java.util.Objects;

/**
 * Represents a single dependency detected by the scanner.
 * Contains Maven coordinates, classification, and metadata about the dependency.
 */
public class DependencyInfo {
    private final String groupId;
    private final String artifactId;
    private final String version;
    private final DependencyType type;
    private final DependencySource source;
    private final String jarPath;
    private final String coordinate;

    private DependencyInfo(Builder builder) {
        this.groupId = builder.groupId;
        this.artifactId = builder.artifactId;
        this.version = builder.version;
        this.type = builder.type != null ? builder.type : DependencyType.fromCoordinates(groupId, artifactId);
        this.source = builder.source != null ? builder.source : DependencySource.UNKNOWN;
        this.jarPath = builder.jarPath;
        this.coordinate = buildCoordinate(groupId, artifactId, version);
    }

    private static String buildCoordinate(String groupId, String artifactId, String version) {
        StringBuilder sb = new StringBuilder();
        if (groupId != null) {
            sb.append(groupId);
        }
        if (artifactId != null) {
            if (sb.length() > 0) sb.append(":");
            sb.append(artifactId);
        }
        if (version != null) {
            if (sb.length() > 0) sb.append(":");
            sb.append(version);
        }
        return sb.toString();
    }

    // Getters
    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public DependencyType getType() {
        return type;
    }

    public DependencySource getSource() {
        return source;
    }

    public String getJarPath() {
        return jarPath;
    }

    public String getCoordinate() {
        return coordinate;
    }

    /**
     * Checks if this dependency is a Jakarta EE dependency.
     */
    public boolean isJakartaEE() {
        return type == DependencyType.JAKARTA_EE;
    }

    /**
     * Checks if this dependency is a Java EE dependency.
     */
    public boolean isJavaEE() {
        return type == DependencyType.JAVA_EE;
    }

    /**
     * Checks if this dependency is a MicroProfile dependency.
     */
    public boolean isMicroProfile() {
        return type == DependencyType.MICROPROFILE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DependencyInfo that = (DependencyInfo) o;
        return Objects.equals(groupId, that.groupId) &&
               Objects.equals(artifactId, that.artifactId) &&
               Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, version);
    }

    @Override
    public String toString() {
        return "DependencyInfo{" +
               "coordinate='" + coordinate + '\'' +
               ", type=" + type +
               ", source=" + source +
               (jarPath != null ? ", jarPath='" + jarPath + '\'' : "") +
               '}';
    }

    /**
     * Creates a new Builder for constructing DependencyInfo instances.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for DependencyInfo.
     */
    public static class Builder {
        private String groupId;
        private String artifactId;
        private String version;
        private DependencyType type;
        private DependencySource source;
        private String jarPath;

        public Builder groupId(String groupId) {
            this.groupId = groupId;
            return this;
        }

        public Builder artifactId(String artifactId) {
            this.artifactId = artifactId;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder type(DependencyType type) {
            this.type = type;
            return this;
        }

        public Builder source(DependencySource source) {
            this.source = source;
            return this;
        }

        public Builder jarPath(String jarPath) {
            this.jarPath = jarPath;
            return this;
        }

        /**
         * Builds the DependencyInfo instance.
         * At minimum, artifactId should be provided.
         */
        public DependencyInfo build() {
            if (artifactId == null || artifactId.trim().isEmpty()) {
                throw new IllegalArgumentException("artifactId is required");
            }
            return new DependencyInfo(this);
        }
    }
}

