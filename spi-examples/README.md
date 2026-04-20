# SPI Integration Examples

This directory contains reference implementations showing how to use the EE Dependency Scanner's `DependencyAnalysisHelper` utility in IDE adapters and language servers.

## Overview

The EE Dependency Scanner provides a `DependencyAnalysisHelper` utility class that simplifies integration with IDE plugins and language servers. This directory contains complete reference implementations for Eclipse, IntelliJ IDEA, LSP4Jakarta, and LSP4MP, including **provider-based integrations** that follow the architectural patterns of LSP4Jakarta and LSP4MP.

## Directory Structure

```
spi-examples/
├── README.md                           # This file
├── scanner-eclipse-adapter-impl/       # Eclipse adapter reference implementation
│   ├── EclipseProjectModelParserImpl.java
│   └── README.md
├── scanner-intellij-adapter-impl/      # IntelliJ adapter reference implementation
│   ├── IntelliJProjectModelParserImpl.java
│   └── README.md
├── lsp4jakarta-adapter-impl/           # LSP4Jakarta language server integration
│   ├── LSP4JakartaDependencyAnalyzer.java      # Direct integration
│   ├── EEVersionProjectLabelProvider.java      # Provider-based (RECOMMENDED)
│   └── README.md
└── lsp4mp-adapter-impl/                # LSP4MP language server integration
    ├── LSP4MPDependencyAnalyzer.java           # Direct integration
    ├── EEVersionProjectLabelProvider.java      # Provider-based (RECOMMENDED)
    └── README.md
```

## Quick Start

### 1. Review the DependencyAnalysisHelper

The core utility class is located at:
```
ee-dependency-scanner-core/src/main/java/io/openliberty/tools/scanner/util/DependencyAnalysisHelper.java
```

Key methods:
```java
// Create dependency from Maven coordinates
DependencyInfo createDependency(String groupId, String artifactId, String version)

// Extract dependencies from JAR files
List<DependencyInfo> extractDependenciesFromJar(File jarFile)

// Detect Jakarta EE and MicroProfile versions
Map<String, Set<String>> detectVersions(List<DependencyInfo> dependencies)

// Get primary version from a set
String getPrimaryVersion(Set<String> versions)

// Filter dependencies by type
List<DependencyInfo> getJakartaEEDependencies(List<DependencyInfo> dependencies)
List<DependencyInfo> getMicroProfileDependencies(List<DependencyInfo> dependencies)

// Deduplicate dependencies
List<DependencyInfo> deduplicate(List<DependencyInfo> dependencies)
```

### 2. Review Reference Implementations

- **Eclipse**: [scanner-eclipse-adapter-impl/](./scanner-eclipse-adapter-impl/) - Shows M2E, Buildship, and JDT integration
- **IntelliJ**: [scanner-intellij-adapter-impl/](./scanner-intellij-adapter-impl/) - Shows IntelliJ Platform API usage
- **LSP4Jakarta**: [lsp4jakarta-adapter-impl/](./lsp4jakarta-adapter-impl/) - Shows Jakarta EE language server integration (provider-based recommended)
- **LSP4MP**: [lsp4mp-adapter-impl/](./lsp4mp-adapter-impl/) - Shows MicroProfile language server integration (provider-based recommended)

## Integration Approaches

### For IDE Plugins (Eclipse, IntelliJ)

Use the adapter pattern to collect dependencies using native IDE APIs:

```java
// Eclipse: Use M2E, Buildship, or JDT
// IntelliJ: Use Module APIs
```

See: [scanner-eclipse-adapter-impl/](./scanner-eclipse-adapter-impl/) and [scanner-intellij-adapter-impl/](./scanner-intellij-adapter-impl/)

### For Language Servers (LSP4Jakarta, LSP4MP) - RECOMMENDED

Use the **provider-based integration** that implements `IProjectLabelProvider`:

#### Registration (plugin.xml)
```xml
<!-- LSP4Jakarta -->
<extension point="org.eclipse.lsp4jakarta.jdt.core.projectLabelProviders">
  <provider class="org.eclipse.lsp4jakarta.jdt.internal.core.providers.EEVersionProjectLabelProvider"/>
</extension>

<!-- LSP4MP -->
<extension point="org.eclipse.lsp4mp.jdt.core.projectLabelProviders">
  <provider class="org.eclipse.lsp4mp.jdt.internal.core.providers.EEVersionProjectLabelProvider"/>
</extension>
```

#### Usage in Diagnostics
```java
public List<Diagnostic> collectDiagnostics(JavaDiagnosticsContext context) {
    // Labels are automatically available!
    List<String> labels = context.getProjectLabels();
    
    if (labels.contains("jakartaee-9") || labels.contains("jakartaee-10")) {
        // Apply Jakarta EE 9+ diagnostics (jakarta.* namespace)
    } else if (labels.contains("javaee-8")) {
        // Apply Java EE 8 diagnostics (javax.* namespace)
    }
}
```

#### Labels Provided

**LSP4Jakarta:**
- `jakartaee`, `jakartaee-9`, `jakartaee-10`, `jakartaee-11`
- `javaee`, `javaee-5`, `javaee-6`, `javaee-7`, `javaee-8`

**LSP4MP:**
- `jakartaee`, `jakartaee-9`, `jakartaee-10`
- `microprofile`, `microprofile-1.0` through `microprofile-6.x`

## Integration Pattern

### Basic Pattern (All Integrations)

```java
// 1. Initialize helper
DependencyAnalysisHelper helper = new DependencyAnalysisHelper();

// 2. Collect dependencies using IDE APIs
List<DependencyInfo> dependencies = new ArrayList<>();

// Example: From Maven coordinates
DependencyInfo dep = helper.createDependency("jakarta.servlet", "jakarta.servlet-api", "5.0.0");
dependencies.add(dep);

// Example: From JAR file
List<DependencyInfo> jarDeps = helper.extractDependenciesFromJar(new File("lib/my-lib.jar"));
dependencies.addAll(jarDeps);

// 3. Deduplicate
dependencies = helper.deduplicate(dependencies);

// 4. Detect versions
Map<String, Set<String>> versions = helper.detectVersions(dependencies);
String jakartaVersion = helper.getPrimaryVersion(versions.get("jakartaEE"));
String mpVersion = helper.getPrimaryVersion(versions.get("microProfile"));

// 5. Return result
return new ClasspathAnalysisResult(dependencies, jakartaVersion, mpVersion, versions);
```

### Provider Pattern (LSP4Jakarta, LSP4MP)

```java
public class EEVersionProjectLabelProvider implements IProjectLabelProvider {
    
    @Override
    public List<String> getProjectLabels(IJavaProject project) {
        // 1. Analyze dependencies using three-tier approach:
        //    - Try M2E (Maven) first
        //    - Try Buildship (Gradle) second
        //    - Fall back to JDT classpath
        
        // 2. Return version labels
        return Arrays.asList("jakartaee", "jakartaee-10");
    }
}
```

## Eclipse Adapter

The Eclipse implementation shows how to:
- Use M2E (Maven Integration for Eclipse) APIs
- Use Buildship (Gradle Integration) APIs
- Fall back to JDT classpath parsing
- Integrate with DependencyAnalysisHelper

See: [scanner-eclipse-adapter-impl/README.md](./scanner-eclipse-adapter-impl/README.md)

## IntelliJ Adapter

The IntelliJ implementation shows how to:
- Use IntelliJ Platform Module APIs
- Parse Maven/Gradle library names
- Extract dependencies from JARs
- Integrate with DependencyAnalysisHelper

See: [scanner-intellij-adapter-impl/README.md](./scanner-intellij-adapter-impl/README.md)

## LSP4Jakarta Integration

The LSP4Jakarta integration provides **two approaches**:

### Provider-based Integration (RECOMMENDED)
- Implements `IProjectLabelProvider` interface
- Follows LSP4Jakarta's architecture
- Automatic label propagation to all diagnostics/code actions
- Three-tier dependency collection (M2E → Buildship → JDT)
- Built-in caching

### Direct Integration
- For custom analysis logic
- More control but requires more code

**Features:**
- Detect Jakarta EE and Java EE versions
- Handle namespace differences (javax.* vs jakarta.*)
- Provide version-specific diagnostics

See: [lsp4jakarta-adapter-impl/README.md](./lsp4jakarta-adapter-impl/README.md)

## LSP4MP Integration

The LSP4MP integration provides **two approaches**:

### Provider-based Integration (RECOMMENDED)
- Implements `IProjectLabelProvider` interface
- Follows LSP4MP's architecture
- Automatic label propagation to all diagnostics/code actions
- Three-tier dependency collection (M2E → Buildship → JDT)
- Built-in caching

### Direct Integration
- For custom analysis logic
- More control but requires more code

**Features:**
- Detect MicroProfile versions
- Detect Jakarta EE versions (for MP 4.0+)
- Handle MicroProfile 4.0+ namespace changes
- Provide API-specific diagnostics and code actions

See: [lsp4mp-adapter-impl/README.md](./lsp4mp-adapter-impl/README.md)

## Common Use Cases

### Detect Jakarta EE Version

```java
DependencyAnalysisHelper helper = new DependencyAnalysisHelper();
List<DependencyInfo> deps = collectDependencies();
Map<String, Set<String>> versions = helper.detectVersions(deps);
String jakartaVersion = helper.getPrimaryVersion(versions.get("jakartaEE"));
```

### Detect MicroProfile Version

```java
DependencyAnalysisHelper helper = new DependencyAnalysisHelper();
List<DependencyInfo> deps = collectDependencies();
Map<String, Set<String>> versions = helper.detectVersions(deps);
String mpVersion = helper.getPrimaryVersion(versions.get("microProfile"));
```

### Filter Jakarta EE Dependencies

```java
DependencyAnalysisHelper helper = new DependencyAnalysisHelper();
List<DependencyInfo> allDeps = collectDependencies();
List<DependencyInfo> jakartaDeps = helper.getJakartaEEDependencies(allDeps);
```

### Filter MicroProfile Dependencies

```java
DependencyAnalysisHelper helper = new DependencyAnalysisHelper();
List<DependencyInfo> allDeps = collectDependencies();
List<DependencyInfo> mpDeps = helper.getMicroProfileDependencies(allDeps);
```

### Extract Dependencies from JAR

```java
DependencyAnalysisHelper helper = new DependencyAnalysisHelper();
File jarFile = new File("lib/my-library.jar");
List<DependencyInfo> deps = helper.extractDependenciesFromJar(jarFile);
```

### Use Labels in Language Servers (Provider-based)

```java
// LSP4Jakarta
public List<Diagnostic> collectDiagnostics(JavaDiagnosticsContext context) {
    List<String> labels = context.getProjectLabels();
    
    if (labels.contains("jakartaee-9") || labels.contains("jakartaee-10")) {
        // Jakarta EE 9+ - use jakarta.* namespace
        return checkJakartaNamespace(context);
    } else if (labels.contains("javaee-8")) {
        // Java EE 8 - use javax.* namespace
        return checkJavaxNamespace(context);
    }
}

// LSP4MP
public List<Diagnostic> collectDiagnostics(JavaDiagnosticsContext context) {
    List<String> labels = context.getProjectLabels();
    
    if (labels.stream().anyMatch(l -> l.startsWith("microprofile-4") || 
                                       l.startsWith("microprofile-5"))) {
        // MicroProfile 4.0+ uses jakarta.* namespace
        return checkMicroProfileJakartaNamespace(context);
    }
}
```

## Best Practices

1. **Use DependencyAnalysisHelper**: Don't manually create DependencyInfo objects
2. **Prefer Provider-based Integration**: For LSP4Jakarta and LSP4MP, use the provider pattern
3. **Prefer IDE APIs**: Use native IDE APIs (M2E, Buildship) for dependency collection when available
4. **Implement Fallbacks**: Have fallback strategies (e.g., JAR extraction) when IDE APIs fail
5. **Deduplicate**: Always deduplicate dependencies before analysis
6. **Handle Errors**: Gracefully handle missing dependencies or parsing errors
7. **Cache Results**: Cache analysis results to improve performance (especially in language servers)

## Three-Tier Dependency Collection

The provider-based integrations use a smart three-tier approach:

### 1. M2E (Maven) - Priority 1
- Uses Maven Integration for Eclipse APIs
- Best accuracy for Maven projects
- Direct access to Maven project model

### 2. Buildship (Gradle) - Priority 2
- Uses Gradle Integration for Eclipse APIs
- Best accuracy for Gradle projects
- Direct access to Gradle project model

### 3. JDT Classpath (Fallback) - Priority 3
- Extracts dependencies from JAR files
- Works for any project type
- Universal fallback

## Version Detection Features

### Jakarta EE Versions

- Java EE 5, 6, 7, 8
- Jakarta EE 8, 9, 9.1, 10, 11

### MicroProfile Versions

- MicroProfile 1.0 - 6.x
- Automatic detection of namespace changes (MP 4.0+ uses jakarta.*)

## Comparison: Provider vs Direct Integration

| Feature | Provider-based | Direct Integration |
|---------|---------------|-------------------|
| Integration Effort | Low | Medium |
| Code Changes | Minimal | More extensive |
| Architecture Fit | Perfect | Custom |
| Label Propagation | Automatic | Manual |
| Caching | Built-in | Custom |
| M2E/Buildship Support | ✅ Complete | Requires implementation |
| **Recommendation** | **✅ Use for LSP4Jakarta/LSP4MP** | Use for special cases |

## Resources

- [DependencyAnalysisHelper API](../ee-dependency-scanner-core/src/main/java/io/openliberty/tools/scanner/util/DependencyAnalysisHelper.java)
- [Eclipse JDT Documentation](https://www.eclipse.org/jdt/)
- [IntelliJ Platform SDK](https://plugins.jetbrains.com/docs/intellij/welcome.html)
- [Language Server Protocol](https://microsoft.github.io/language-server-protocol/)
- [Jakarta EE](https://jakarta.ee/)
- [MicroProfile](https://microprofile.io/)
- [LSP4Jakarta GitHub](https://github.com/eclipse/lsp4jakarta)
- [LSP4MP GitHub](https://github.com/eclipse/lsp4mp)