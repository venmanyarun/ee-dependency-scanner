# LSP4Jakarta Integration - Complete Replacement Guide

This guide shows how to completely replace LSP4Jakarta's `JakartaVersionFinder.java` with the ee-dependency-scanner's `EclipseJDTParser`.

## What Gets Replaced

**Remove these files from LSP4Jakarta:**
- `JakartaVersionFinder.java` - Regex-based JAR filename parsing
- `JarFilenameVersionDetector.java` - Filename parsing logic
- `JarManifestVersionDetector.java` - Basic manifest inspection

**Replace with:**
- `EclipseJDTParser.java` - Complete Eclipse JDT-based parser from ee-dependency-scanner

## Why Replace?

The ee-dependency-scanner provides:

✅ **Bundle-SymbolicName extraction** - Proper OSGi bundle identification  
✅ **Multiple version attributes** - Specification-Version, Implementation-Version, Bundle-Version, Jakarta-Version  
✅ **Transitive dependencies** - Extracts from embedded pom.xml files  
✅ **Centralized version registry** - Properties files instead of hardcoded mappings  
✅ **Filename fallback** - Automatic if manifest inspection fails  
✅ **Works with all project types** - Maven, Gradle, manual JARs  

## Integration Steps

### Step 1: Add Dependency

Add to LSP4Jakarta's `pom.xml`:

```xml
<dependency>
    <groupId>io.openliberty.tools</groupId>
    <artifactId>ee-dependency-scanner-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Step 2: Copy EclipseJDTParser

Copy `EclipseJDTParser.java` to your LSP4Jakarta project:

```
org.eclipse.lsp4jakarta.jdt.core/src/main/java/org/eclipse/lsp4jakarta/version/EclipseJDTParser.java
```

Uncomment the implementation code (remove `/*` and `*/` comments).

### Step 3: Update JakartaVersionManager

Replace the version detection logic:

```java
package org.eclipse.lsp4jakarta.version;

import io.openliberty.tools.scanner.api.DependencyInfo;
import io.openliberty.tools.scanner.util.DependencyAnalysisHelper;
import org.eclipse.jdt.core.IJavaProject;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class JakartaVersionManager {
    
    private final EclipseJDTParser parser;
    private final DependencyAnalysisHelper helper;
    
    public JakartaVersionManager() {
        this.parser = new EclipseJDTParser();
        this.helper = new DependencyAnalysisHelper();
    }
    
    /**
     * Gets Jakarta EE version for a project.
     * 
     * @param javaProject Eclipse IJavaProject
     * @return Jakarta EE version (e.g., "10", "9.1", "9") or null
     */
    public String getJakartaEEVersion(IJavaProject javaProject) {
        try {
            // Parse Jakarta EE dependencies using Eclipse JDT
            List<DependencyInfo> dependencies = parser.parseJakartaEEOnly(javaProject);
            
            // Detect version using centralized registry
            Map<String, Set<String>> versions = helper.detectVersions(dependencies);
            Set<String> jakartaVersions = versions.get("jakartaEE");
            
            if (jakartaVersions != null && !jakartaVersions.isEmpty()) {
                return helper.getPrimaryVersion(jakartaVersions);
            }
            
            // Check for Java EE (legacy)
            Set<String> javaEEVersions = versions.get("javaEE");
            if (javaEEVersions != null && !javaEEVersions.isEmpty()) {
                return "JavaEE-" + helper.getPrimaryVersion(javaEEVersions);
            }
            
            return null; // Unknown - don't fallback to EE 9
            
        } catch (Exception e) {
            System.err.println("Failed to detect Jakarta EE version: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Gets MicroProfile version for a project.
     * 
     * @param javaProject Eclipse IJavaProject
     * @return MicroProfile version (e.g., "6.0", "5.0") or null
     */
    public String getMicroProfileVersion(IJavaProject javaProject) {
        try {
            List<DependencyInfo> dependencies = parser.parseMicroProfileOnly(javaProject);
            
            Map<String, Set<String>> versions = helper.detectVersions(dependencies);
            Set<String> mpVersions = versions.get("microProfile");
            
            return mpVersions != null ? helper.getPrimaryVersion(mpVersions) : null;
            
        } catch (Exception e) {
            System.err.println("Failed to detect MicroProfile version: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Checks if project has version conflicts.
     * 
     * @param javaProject Eclipse IJavaProject
     * @return true if multiple versions of same feature detected
     */
    public boolean hasVersionConflicts(IJavaProject javaProject) {
        try {
            List<DependencyInfo> dependencies = parser.parseJakartaEEOnly(javaProject);
            Map<String, Set<String>> featureVersions = getFeatureVersions(dependencies);
            
            return featureVersions.values().stream()
                .anyMatch(versions -> versions.size() > 1);
                
        } catch (Exception e) {
            return false;
        }
    }
    
    private Map<String, Set<String>> getFeatureVersions(List<DependencyInfo> dependencies) {
        Map<String, Set<String>> featureVersions = new java.util.HashMap<>();
        
        for (DependencyInfo dep : dependencies) {
            String feature = extractFeatureName(dep.getArtifactId());
            String version = dep.getVersion();
            
            if (feature != null && version != null) {
                featureVersions.computeIfAbsent(feature, k -> new java.util.HashSet<>()).add(version);
            }
        }
        
        return featureVersions;
    }
    
    private String extractFeatureName(String artifactId) {
        if (artifactId == null) return null;
        
        return artifactId
            .replaceAll("-api$", "")
            .replaceAll("-spec$", "")
            .replaceAll("^jakarta\\.", "")
            .replaceAll("^javax\\.", "")
            .replaceAll("-", "")
            .toLowerCase();
    }
}
```

### Step 4: Update Callers

Update any code that calls the old `JakartaVersionFinder`:

**Before:**
```java
IClasspathEntry[] entries = javaProject.getResolvedClasspath(true);
JakartaVersion version = JakartaVersionFinder.analyzeClasspath(entries);
```

**After:**
```java
JakartaVersionManager manager = new JakartaVersionManager();
String version = manager.getJakartaEEVersion(javaProject);
```

### Step 5: Remove Old Files

Delete these files from LSP4Jakarta:
- `JakartaVersionFinder.java`
- `JarFilenameVersionDetector.java`
- `JarManifestVersionDetector.java`

## What EclipseJDTParser Does

The parser handles everything automatically:

1. **Gets resolved classpath** from `IJavaProject.getResolvedClasspath(true)`
2. **Processes each JAR** using `JarManifestScanner`:
   - Reads `Bundle-SymbolicName` from MANIFEST.MF
   - Extracts version from multiple attributes
   - Parses embedded `META-INF/maven/*/pom.xml` for transitive dependencies
   - Falls back to filename parsing if needed
3. **Applies filtering** to return only relevant dependencies
4. **Returns complete list** including transitive dependencies

## API Reference

### EclipseJDTParser

```java
EclipseJDTParser parser = new EclipseJDTParser();

// Parse Jakarta EE dependencies only
List<DependencyInfo> jakartaDeps = parser.parseJakartaEEOnly(javaProject);

// Parse MicroProfile dependencies only
List<DependencyInfo> mpDeps = parser.parseMicroProfileOnly(javaProject);

// Parse with custom filter
List<DependencyInfo> allDeps = parser.parse(javaProject, DependencyFilter.includeAll());
```

### DependencyAnalysisHelper

```java
DependencyAnalysisHelper helper = new DependencyAnalysisHelper();

// Detect all versions
Map<String, Set<String>> versions = helper.detectVersions(dependencies);
Set<String> jakartaVersions = versions.get("jakartaEE");
Set<String> mpVersions = versions.get("microProfile");

// Get primary (highest) version
String primaryVersion = helper.getPrimaryVersion(jakartaVersions);

// Filter dependencies
List<DependencyInfo> jakartaDeps = helper.getJakartaEEDependencies(dependencies);
```

## Example Output

```java
// Jakarta EE 10 project
String version = manager.getJakartaEEVersion(javaProject);
// Returns: "10"

// Project with conflicts
boolean hasConflicts = manager.hasVersionConflicts(javaProject);
// Returns: true if multiple versions detected
```

## Benefits

The new approach provides:

- **JAR Analysis**: Manifest inspection with filename fallback (vs. filename regex only)
- **Version Mapping**: Properties files (vs. hardcoded in code)
- **Transitive Dependencies**: Fully supported (vs. not supported)
- **Manual JARs**: Full support (vs. limited support)
- **Maintainability**: Config updates only (vs. code updates needed)
- **OSGi Bundles**: Full Bundle-SymbolicName support (vs. basic support)

## Testing

Test with different project types:

1. **Maven project** - Dependencies from pom.xml
2. **Gradle project** - Dependencies from build.gradle
3. **Manual JARs** - JARs in lib/ folder
4. **Mixed project** - Combination of all above

All should work seamlessly with the new parser.

## See Also

- [IntelliJ Plugin Example](../intellij-plugin/README.md) - IntelliJ IDEA integration
- [Core Scanner Documentation](../../ee-dependency-scanner-core/README.md) - Core library details
- [API Documentation](../../ee-dependency-scanner-api/) - Complete API reference