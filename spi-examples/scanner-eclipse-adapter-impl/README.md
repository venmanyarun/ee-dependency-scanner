# Eclipse Adapter Implementation with DependencyAnalysisHelper

This directory contains a complete reference implementation showing how to use `DependencyAnalysisHelper` in the Eclipse adapter.

## File

- `EclipseProjectModelParserImpl.java` - Complete implementation example

## How It Works

### 1. Initialize Helper

```java
private final DependencyAnalysisHelper helper;

public EclipseProjectModelParserImpl() {
    this.helper = new DependencyAnalysisHelper();
}
```

### 2. Collect Dependencies Using Eclipse APIs

The implementation shows three approaches in priority order:

#### A. M2E (Maven Integration for Eclipse)

```java
// Get M2E project facade
IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getProject(project);

// Get Maven project model
MavenProject mavenProject = facade.getMavenProject(null);

// Collect dependencies using helper
for (Dependency dep : mavenProject.getDependencies()) {
    DependencyInfo depInfo = helper.createDependency(
        dep.getGroupId(),
        dep.getArtifactId(),
        dep.getVersion()
    );
    dependencies.add(depInfo);
}
```

#### B. Buildship (Gradle Integration for Eclipse)

```java
// Get Buildship project
GradleBuild gradleBuild = GradleCore.getWorkspace().getBuild(project).orElse(null);

// Get Gradle project model
GradleProject gradleProject = gradleBuild.getModelProvider()
    .fetchModel(GradleProject.class);

// Collect dependencies using helper
for (GradleConfiguration config : gradleProject.getConfigurations()) {
    for (GradleDependency dep : config.getDependencies()) {
        if (dep instanceof ExternalDependency) {
            ExternalDependency extDep = (ExternalDependency) dep;
            DependencyInfo depInfo = helper.createDependency(
                extDep.getGroup(),
                extDep.getName(),
                extDep.getVersion()
            );
            dependencies.add(depInfo);
        }
    }
}
```

#### C. JDT Classpath (Fallback)

```java
// Get Java project
IJavaProject javaProject = JavaCore.create(project);

// Get resolved classpath
IClasspathEntry[] entries = javaProject.getResolvedClasspath(true);

for (IClasspathEntry entry : entries) {
    if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
        File jarFile = entry.getPath().toFile();
        
        // Extract dependencies from JAR using helper
        List<DependencyInfo> jarDeps = helper.extractDependenciesFromJar(jarFile);
        dependencies.addAll(jarDeps);
    }
}
```

## Key Benefits of Using DependencyAnalysisHelper

1. **Consistent Dependency Creation**: Use `helper.createDependency()` instead of manually building DependencyInfo
2. **JAR Extraction**: Use `helper.extractDependenciesFromJar()` to extract Maven coordinates from JARs
3. **Version Detection**: After collection, use `helper.detectVersions()` to find EE versions
4. **Filtering**: Use `helper.getJakartaEEDependencies()`, `helper.getMicroProfileDependencies()`, etc.
5. **Deduplication**: Use `helper.deduplicate()` to remove duplicate dependencies

## To Use in Real Eclipse Plugin

1. Copy `EclipseProjectModelParserImpl.java` to `scanner-eclipse-adapter/src/main/java/io/openliberty/tools/scanner/eclipse/`
2. Replace the existing `EclipseProjectModelParser.java`
3. Uncomment the Eclipse API imports
4. Add Eclipse dependencies to your plugin:
   ```xml
   <dependency>
       <groupId>org.eclipse.jdt</groupId>
       <artifactId>org.eclipse.jdt.core</artifactId>
   </dependency>
   <dependency>
       <groupId>org.eclipse.m2e</groupId>
       <artifactId>org.eclipse.m2e.core</artifactId>
   </dependency>
   <dependency>
       <groupId>org.eclipse.buildship</groupId>
       <artifactId>org.eclipse.buildship.core</artifactId>
   </dependency>
   ```
5. Build and test with a real Eclipse workspace

## Testing

```java
@Test
public void testEclipseAdapter() {
    // Create test Eclipse project
    IProject project = createTestProject();
    
    // Parse dependencies
    EclipseProjectModelParserImpl parser = new EclipseProjectModelParserImpl();
    List<DependencyInfo> deps = parser.parse(project.getLocation().toFile());
    
    // Verify
    assertFalse(deps.isEmpty());
    
    // Use helper to detect versions
    DependencyAnalysisHelper helper = new DependencyAnalysisHelper();
    Map<String, Set<String>> versions = helper.detectVersions(deps);
    
    assertNotNull(versions.get("jakartaEE"));
}
```

## See Also

- [DependencyAnalysisHelper API](../../ee-dependency-scanner-core/src/main/java/io/openliberty/tools/scanner/util/DependencyAnalysisHelper.java)
- [Implementation Notes](../IMPLEMENTATION_NOTES.md)
- [Eclipse JDT Documentation](https://www.eclipse.org/jdt/)
- [M2E Documentation](https://www.eclipse.org/m2e/)
- [Buildship Documentation](https://projects.eclipse.org/projects/tools.buildship)