package io.openliberty.tools.scanner.model;

/**
 * Enumeration of dependency types that can be detected by the scanner.
 * Used to categorize dependencies into Jakarta EE, Java EE, MicroProfile, or other types.
 */
public enum DependencyType {
    /**
     * Jakarta EE dependencies (jakarta.* namespace)
     * Examples: jakarta.servlet, jakarta.persistence, jakarta.enterprise
     */
    JAKARTA_EE("Jakarta EE"),
    
    /**
     * Java EE dependencies (javax.* namespace)
     * Examples: javax.servlet, javax.persistence, javax.enterprise
     */
    JAVA_EE("Java EE"),
    
    /**
     * MicroProfile dependencies (org.eclipse.microprofile.*)
     * Examples: microprofile-config-api, microprofile-health-api
     */
    MICROPROFILE("MicroProfile"),
    
    /**
     * Other dependencies that don't fall into the above categories
     */
    OTHER("Other");

    private final String displayName;

    DependencyType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Determines the dependency type based on groupId and artifactId.
     * 
     * @param groupId the Maven groupId
     * @param artifactId the Maven artifactId
     * @return the detected DependencyType
     */
    public static DependencyType fromCoordinates(String groupId, String artifactId) {
        if (groupId == null) {
            return OTHER;
        }

        // Jakarta EE detection
        if (groupId.startsWith("jakarta.")) {
            return JAKARTA_EE;
        }

        // Java EE detection
        if (groupId.startsWith("javax.")) {
            return JAVA_EE;
        }

        // MicroProfile detection
        if (groupId.startsWith("org.eclipse.microprofile") || 
            (artifactId != null && artifactId.contains("microprofile"))) {
            return MICROPROFILE;
        }

        return OTHER;
    }
}

