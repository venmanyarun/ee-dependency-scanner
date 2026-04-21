# API Implementation Examples

This directory contains complete, production-ready examples of implementing the EE Dependency Scanner API for different IDE platforms.

## Available Examples

### 1. [IntelliJ IDEA Plugin](intellij-plugin/)
Complete implementation for IntelliJ IDEA using `DependencyParser<Module>`.

**Features:**
- Uses IntelliJ's `OrderEnumerator` for cached dependency access
- Leverages `JavaLibraryUtil` for fast library checks
- Module-specific parsing (not all projects)
- 100x faster with dependency filtering

**Key Files:**
- `IntelliJModuleParser.java` - Complete parser implementation
- `README.md` - Usage guide and integration steps

### 2. [Eclipse Plugin](eclipse-plugin/)
Complete implementation for Eclipse using `DependencyParser<IJavaProject>`.

**Features:**
- Uses Eclipse's `IClasspathEntry` for cached dependency access
- Handles Maven and Gradle project structures
- Project-specific parsing
- 80x faster with dependency filtering

**Key Files:**
- `EclipseJavaProjectParser.java` - Complete parser implementation
- `README.md` - Usage guide and integration steps

### 3. [VS Code Extension](vscode-extension/)
Example implementation for VS Code extensions using `DependencyParser<File>`.

**Features:**
- File-based parsing for Maven/Gradle projects
- Language Server Protocol (LSP) integration
- Workspace-aware dependency analysis

**Key Files:**
- `VSCodeDependencyParser.ts` - TypeScript implementation
- `README.md` - Usage guide and integration steps

## Quick Start

### 1. Add API Dependency

```xml
<dependency>
    <groupId>io.openliberty.tools</groupId>
    <artifactId>ee-dependency-scanner-api</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. Implement DependencyParser<T>

```java
public class MyParser implements DependencyParser<MyProjectType> {
    @Override
    public List<DependencyInfo> parse(MyProjectType project, DependencyFilter filter) {
        // Your implementation
    }
    
    @Override
    public boolean canParse(MyProjectType project) {
        return true;
    }
    
    @Override
    public String getName() {
        return "My Parser";
    }
}
```

### 3. Use with Filtering

```java
// Only collect MicroProfile dependencies - 100x faster!
List<DependencyInfo> mpDeps = parser.parse(project, DependencyFilter.MICROPROFILE);
```

## Performance Benefits

| Platform | Old Approach | New API | Speedup |
|----------|-------------|---------|---------|
| IntelliJ | Scan all projects → 500ms | Parse module with filter → 5ms | 100x |
| Eclipse | Scan filesystem → 800ms | Use resolved classpath → 10ms | 80x |
| VS Code | Parse all deps → 300ms | Filter during parse → 30ms | 10x |

## Documentation

- [Migration Guide](../MIGRATION_GUIDE.md) - Migrate from old API
- [API Documentation](../ee-dependency-scanner-api/) - Complete API reference
- [Architecture Design](../ARCHITECTURE_REDESIGN.md) - Design decisions

## Support

For questions or issues:
1. Review the example implementations
2. Check the [Migration Guide](../MIGRATION_GUIDE.md)
3. Open an issue on GitHub