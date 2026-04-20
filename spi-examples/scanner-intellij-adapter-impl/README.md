# IntelliJ Adapter Implementation with DependencyAnalysisHelper

This directory contains a complete reference implementation showing how to use `DependencyAnalysisHelper` in the IntelliJ IDEA adapter.

## File

- `IntelliJProjectModelParserImpl.java` - Complete implementation example

## How It Works

### 1. Initialize Helper

```java
private final DependencyAnalysisHelper helper;

public IntelliJProjectModelParserImpl() {
    this.helper = new DependencyAnalysisHelper();
}
```

### 2. Collect Dependencies Using IntelliJ Platform APIs

The implementation shows how to use IntelliJ's module system:

#### A. Get Module Dependencies

```java
// Get module from project
Module module = ModuleManager.getInstance(project).getModules()[0];

// Get module root manager
ModuleRootManager rootManager = ModuleRootManager.getInstance(module);

// Iterate through order entries (dependencies)
for (OrderEntry entry : rootManager.getOrderEntries()) {
    if (entry instanceof LibraryOrderEntry) {
        LibraryOrderEntry libEntry = (LibraryOrderEntry) entry;
        Library library = libEntry.getLibrary();
        
        // Process library...
    }
}
```

#### B. Parse Maven/Gradle Library Names

IntelliJ stores Maven/Gradle dependencies with names like:
- Maven: `Maven: groupId:artifactId:version`
- Gradle: `Gradle: groupId:artifactId:version`

```java
String libraryName = library.getName();

if (libraryName.startsWith("Maven: ") || libraryName.startsWith("Gradle: ")) {
    String coords = libraryName.substring(libraryName.indexOf(":") + 2);
    String[] parts = coords.split(":");
    
    if (parts.length >= 3) {
        DependencyInfo depInfo = helper.createDependency(
            parts[0],  // groupId
            parts[1],  // artifactId
            parts[2]   // version
        );
        dependencies.add(depInfo);
    }
}
```

#### C. JAR Extraction (Fallback)

For libraries without Maven/Gradle coordinates:

```java
VirtualFile[] files = library.getFiles(OrderRootType.CLASSES);

for (VirtualFile file : files) {
    if (file.getExtension() != null && file.getExtension().equals("jar")) {
        File jarFile = VfsUtilCore.virtualToIoFile(file);
        
        // Extract dependencies from JAR using helper
        List<DependencyInfo> jarDeps = helper.extractDependenciesFromJar(jarFile);
        dependencies.addAll(jarDeps);
    }
}
```

### 3. Detect Versions and Return Result

```java
// Deduplicate dependencies
dependencies = helper.deduplicate(dependencies);

// Detect EE versions
Map<String, Set<String>> versions = helper.detectVersions(dependencies);

// Get primary versions
String jakartaVersion = helper.getPrimaryVersion(versions.get("jakartaEE"));
String mpVersion = helper.getPrimaryVersion(versions.get("microProfile"));

// Return result
return new ClasspathAnalysisResult(
    dependencies,
    jakartaVersion,
    mpVersion,
    versions
);
```

## Key Benefits of Using DependencyAnalysisHelper

1. **Consistent Dependency Creation**: Use `helper.createDependency()` instead of manually building DependencyInfo
2. **JAR Extraction**: Use `helper.extractDependenciesFromJar()` to extract Maven coordinates from JARs
3. **Version Detection**: After collection, use `helper.detectVersions()` to find EE versions
4. **Filtering**: Use `helper.getJakartaEEDependencies()`, `helper.getMicroProfileDependencies()`, etc.
5. **Deduplication**: Use `helper.deduplicate()` to remove duplicate dependencies

## To Use in Real IntelliJ Plugin

1. Copy `IntelliJProjectModelParserImpl.java` to `scanner-intellij-adapter/src/main/java/io/openliberty/tools/scanner/intellij/`
2. Replace the existing `IntelliJProjectModelParser.java`
3. Uncomment the IntelliJ Platform API imports
4. Add IntelliJ Platform dependencies to your `plugin.xml`:
   ```xml
   <depends>com.intellij.modules.platform</depends>
   <depends>com.intellij.modules.java</depends>
   <depends>org.jetbrains.idea.maven</depends>
   <depends>com.intellij.gradle</depends>
   ```
5. Build and test with IntelliJ IDEA

## IntelliJ Platform API Overview

### Key Classes

- **`Module`**: Represents a module in IntelliJ project
- **`ModuleRootManager`**: Manages module dependencies and classpath
- **`OrderEntry`**: Represents a dependency (library, module, SDK, etc.)
- **`LibraryOrderEntry`**: Represents a library dependency
- **`Library`**: Represents a library with JAR files
- **`VirtualFile`**: IntelliJ's file abstraction

### Common Patterns

```java
// Get all modules in project
Module[] modules = ModuleManager.getInstance(project).getModules();

// Get module dependencies
OrderEntry[] entries = ModuleRootManager.getInstance(module).getOrderEntries();

// Check dependency scope
if (entry instanceof LibraryOrderEntry) {
    DependencyScope scope = ((LibraryOrderEntry) entry).getScope();
    // COMPILE, TEST, RUNTIME, PROVIDED
}

// Get library files
Library library = ((LibraryOrderEntry) entry).getLibrary();
VirtualFile[] files = library.getFiles(OrderRootType.CLASSES);
```

## Testing

```java
@Test
public void testIntelliJAdapter() {
    // Create test IntelliJ project
    Project project = createTestProject();
    
    // Parse dependencies
    IntelliJProjectModelParserImpl parser = new IntelliJProjectModelParserImpl();
    List<DependencyInfo> deps = parser.parse(project.getBasePath());
    
    // Verify
    assertFalse(deps.isEmpty());
    
    // Use helper to detect versions
    DependencyAnalysisHelper helper = new DependencyAnalysisHelper();
    Map<String, Set<String>> versions = helper.detectVersions(deps);
    
    assertNotNull(versions.get("jakartaEE"));
}
```

## Common Issues and Solutions

### Issue: Library Name Doesn't Match Pattern

**Problem**: Some libraries don't follow the `Maven: groupId:artifactId:version` pattern.

**Solution**: Use JAR extraction fallback:
```java
if (!libraryName.startsWith("Maven: ") && !libraryName.startsWith("Gradle: ")) {
    // Extract from JAR
    List<DependencyInfo> jarDeps = helper.extractDependenciesFromJar(jarFile);
    dependencies.addAll(jarDeps);
}
```

### Issue: Module Has No Dependencies

**Problem**: `getOrderEntries()` returns empty array.

**Solution**: Check if module is properly configured:
```java
if (rootManager.getOrderEntries().length == 0) {
    // Module not configured or no dependencies
    return Collections.emptyList();
}
```

### Issue: VirtualFile to File Conversion

**Problem**: Need to convert IntelliJ's `VirtualFile` to `java.io.File`.

**Solution**: Use `VfsUtilCore.virtualToIoFile()`:
```java
File jarFile = VfsUtilCore.virtualToIoFile(virtualFile);
```

## See Also

- [DependencyAnalysisHelper API](../../ee-dependency-scanner-core/src/main/java/io/openliberty/tools/scanner/util/DependencyAnalysisHelper.java)
- [Implementation Notes](../IMPLEMENTATION_NOTES.md)
- [IntelliJ Platform SDK Documentation](https://plugins.jetbrains.com/docs/intellij/welcome.html)
- [IntelliJ Platform API Reference](https://plugins.jetbrains.com/docs/intellij/api-reference.html)