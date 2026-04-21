package io.openliberty.tools.scanner.api;

/**
 * Type of dependency (Jakarta EE, Java EE, MicroProfile, or other).
 */
public enum DependencyType {
    JAKARTA_EE("Jakarta EE"),
    JAVA_EE("Java EE"),
    MICROPROFILE("MicroProfile"),
    OTHER("Other");

    private final String displayName;

    DependencyType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Determines dependency type from Maven coordinates.
     * @param groupId Maven groupId
     * @param artifactId Maven artifactId
     * @return detected dependency type
     */
    public static DependencyType fromCoordinates(String groupId, String artifactId) {
        if (groupId == null) return OTHER;
        if (groupId.startsWith("jakarta.")) return JAKARTA_EE;
        if (groupId.startsWith("javax.")) return JAVA_EE;
        if (groupId.startsWith("org.eclipse.microprofile") ||
            (artifactId != null && artifactId.contains("microprofile"))) return MICROPROFILE;
        return OTHER;
    }
}


