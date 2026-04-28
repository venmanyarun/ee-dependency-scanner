# API Implementation Examples

This directory contains complete, production-ready examples of implementing the EE Dependency Scanner API for different IDE platforms.

## Available Examples

### 1. [IntelliJ IDEA Plugin](intellij-plugin/)
Complete implementation for IntelliJ IDEA using `DependencyParser<Module>`.

**Features:**
- Uses IntelliJ's `OrderEnumerator` for cached dependency access
- Leverages `JavaLibraryUtil` for fast library checks
- Module-specific parsing (not all projects)
- Efficient dependency filtering

**Key Files:**
- `IntelliJModuleParser.java` - Complete parser implementation
- `README.md` - Usage guide and integration steps

### 2. [LSP4Jakarta Integration](lsp4jakarta-integration/)
Complete replacement for LSP4Jakarta's JakartaVersionFinder using `EclipseJDTParser`.

**Features:**
- Replaces regex-based JAR filename parsing
- Bundle-SymbolicName extraction from MANIFEST.MF
- Transitive dependency extraction from embedded pom.xml
- Centralized version registry (no hardcoded mappings)
- Handles Maven, Gradle, and manual JAR dependencies

**Key Files:**
- `EclipseJDTParser.java` - Complete Eclipse JDT parser
- `LSP4JakartaIntegrationExample.java` - Integration helper
- `README.md` - Complete replacement guide

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
// Only collect MicroProfile dependencies
List<DependencyInfo> mpDeps = parser.parse(project, DependencyFilter.MICROPROFILE);
```

## Documentation

- [API Documentation](../ee-dependency-scanner-api/) - Complete API reference
- [Core Scanner Documentation](../ee-dependency-scanner-core/) - Core library details

## Support

For questions or issues:
1. Review the example implementations
2. Check the [Core Documentation](../ee-dependency-scanner-core/)
3. Open an issue on GitHub