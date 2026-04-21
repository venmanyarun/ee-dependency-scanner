package io.openliberty.tools.scanner.parser;

import io.openliberty.tools.scanner.api.DependencyInfo;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Filter for selecting specific dependencies during parsing.
 * Allows parsers to collect only relevant dependencies (e.g., MicroProfile, Jakarta EE)
 * instead of all project dependencies, improving performance.
 */
public class DependencyFilter {
    
    private final Set<String> groupIdPrefixes;
    private final Set<String> artifactIdPrefixes;
    private final boolean includeAll;
    
    /**
     * Creates a filter that includes all dependencies.
     */
    public static DependencyFilter includeAll() {
        return new DependencyFilter(true, new HashSet<>(), new HashSet<>());
    }
    
    /**
     * Creates a filter for MicroProfile dependencies.
     */
    public static DependencyFilter microProfileOnly() {
        return new Builder()
            .addGroupIdPrefix("org.eclipse.microprofile")
            .addGroupIdPrefix("io.smallrye")
            .build();
    }
    
    /**
     * Creates a filter for Jakarta EE dependencies.
     */
    public static DependencyFilter jakartaEEOnly() {
        return new Builder()
            .addGroupIdPrefix("jakarta.")
            .addGroupIdPrefix("javax.")
            .build();
    }
    
    /**
     * Creates a filter for both MicroProfile and Jakarta EE dependencies.
     */
    public static DependencyFilter microProfileAndJakartaEE() {
        return new Builder()
            .addGroupIdPrefix("org.eclipse.microprofile")
            .addGroupIdPrefix("io.smallrye")
            .addGroupIdPrefix("jakarta.")
            .addGroupIdPrefix("javax.")
            .build();
    }
    
    private DependencyFilter(boolean includeAll, Set<String> groupIdPrefixes, Set<String> artifactIdPrefixes) {
        this.includeAll = includeAll;
        this.groupIdPrefixes = groupIdPrefixes;
        this.artifactIdPrefixes = artifactIdPrefixes;
    }
    
    /**
     * Checks if a dependency matches this filter.
     * 
     * @param dependency the dependency to check
     * @return true if the dependency should be included
     */
    public boolean matches(DependencyInfo dependency) {
        if (includeAll) {
            return true;
        }
        
        String groupId = dependency.getGroupId();
        String artifactId = dependency.getArtifactId();
        
        // Check groupId prefixes
        if (groupId != null && !groupIdPrefixes.isEmpty()) {
            for (String prefix : groupIdPrefixes) {
                if (groupId.startsWith(prefix)) {
                    return true;
                }
            }
        }
        
        // Check artifactId prefixes
        if (artifactId != null && !artifactIdPrefixes.isEmpty()) {
            for (String prefix : artifactIdPrefixes) {
                if (artifactId.startsWith(prefix)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Checks if a dependency matches based on Maven coordinates.
     * 
     * @param groupId the dependency groupId
     * @param artifactId the dependency artifactId
     * @return true if the dependency should be included
     */
    public boolean matches(String groupId, String artifactId) {
        if (includeAll) {
            return true;
        }
        
        // Check groupId prefixes
        if (groupId != null && !groupIdPrefixes.isEmpty()) {
            for (String prefix : groupIdPrefixes) {
                if (groupId.startsWith(prefix)) {
                    return true;
                }
            }
        }
        
        // Check artifactId prefixes
        if (artifactId != null && !artifactIdPrefixes.isEmpty()) {
            for (String prefix : artifactIdPrefixes) {
                if (artifactId.startsWith(prefix)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Builder for creating custom dependency filters.
     */
    public static class Builder {
        private final Set<String> groupIdPrefixes = new HashSet<>();
        private final Set<String> artifactIdPrefixes = new HashSet<>();
        
        /**
         * Adds a groupId prefix to match.
         */
        public Builder addGroupIdPrefix(String prefix) {
            groupIdPrefixes.add(prefix);
            return this;
        }
        
        /**
         * Adds multiple groupId prefixes to match.
         */
        public Builder addGroupIdPrefixes(String... prefixes) {
            groupIdPrefixes.addAll(Arrays.asList(prefixes));
            return this;
        }
        
        /**
         * Adds an artifactId prefix to match.
         */
        public Builder addArtifactIdPrefix(String prefix) {
            artifactIdPrefixes.add(prefix);
            return this;
        }
        
        /**
         * Adds multiple artifactId prefixes to match.
         */
        public Builder addArtifactIdPrefixes(String... prefixes) {
            artifactIdPrefixes.addAll(Arrays.asList(prefixes));
            return this;
        }
        
        /**
         * Builds the dependency filter.
         */
        public DependencyFilter build() {
            return new DependencyFilter(false, groupIdPrefixes, artifactIdPrefixes);
        }
    }
}


