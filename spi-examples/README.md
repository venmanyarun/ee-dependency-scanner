# SPI Integration Examples

This directory contains reference implementations showing how to use the EE Dependency Scanner's `DependencyAnalysisHelper` utility in IDE adapters and language servers.

## Overview

The EE Dependency Scanner provides a `DependencyAnalysisHelper` utility class that simplifies integration with IDE plugins and language servers. This directory contains complete reference implementations for Eclipse, IntelliJ IDEA, LSP4Jakarta, and LSP4MP.

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
│   ├── LSP4JakartaDependencyAnalyzer.java
│   └── README.md
└── lsp4mp-adapter-impl/                # LSP4MP language server integration
    ├── LSP4MPDependencyAnalyzer.java
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
- **LSP4Jakarta**: [lsp4jakarta-adapter-impl/](./lsp4jakarta-adapter-impl/) - Shows Jakarta EE language server integration using JDT
- **LSP4MP**: [lsp4mp-adapter-impl/](./lsp4mp-adapter-impl/) - Shows MicroProfile language server integration using JDT

## Integration Pattern

The typical integration pattern:

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

The LSP4Jakarta integration shows how to:
- Use Eclipse JDT APIs in a language server
- Detect Jakarta EE versions
- Provide version-specific diagnostics
- Handle namespace differences (javax.* vs jakarta.*)

See: [lsp4jakarta-adapter-impl/README.md](./lsp4jakarta-adapter-impl/README.md)

## LSP4MP Integration

The LSP4MP integration shows how to:
- Use Eclipse JDT APIs in a language server
- Detect MicroProfile versions
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

### Check API Availability (Language Servers)

```java
// LSP4Jakarta
boolean hasServlet = analyzer.isApiAvailable(result, 
    "jakarta.servlet", "jakarta.servlet-api");

// LSP4MP
boolean hasConfig = analyzer.isApiAvailable(result,
    "org.eclipse.microprofile.config", "microprofile-config-api");
```

## Best Practices

1. **Use DependencyAnalysisHelper**: Don't manually create DependencyInfo objects
2. **Prefer IDE APIs**: Use native IDE APIs for dependency collection when available
3. **Implement Fallbacks**: Have fallback strategies (e.g., JAR extraction) when IDE APIs fail
4. **Deduplicate**: Always deduplicate dependencies before analysis
5. **Handle Errors**: Gracefully handle missing dependencies or parsing errors
6. **Cache Results**: Cache analysis results to improve performance (especially in language servers)

## Integration Approaches

### IDE Plugin Approach (Eclipse, IntelliJ)

IDE plugins have direct access to project models and can collect dependencies using native APIs:

```java
// Eclipse: Use M2E or Buildship APIs
IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getProject(project);
MavenProject mavenProject = facade.getMavenProject(null);

// IntelliJ: Use Module APIs
Module module = ModuleManager.getInstance(project).getModules()[0];
ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
```

### Language Server Approach (LSP4Jakarta, LSP4MP)

Language servers that run in Eclipse use JDT APIs to collect classpath information:

```java
// Get Eclipse project
IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);

// Get Java project
IJavaProject javaProject = JavaCore.create(project);

// Get resolved classpath
IClasspathEntry[] entries = javaProject.getResolvedClasspath(true);
```

## Version Detection Features

### Jakarta EE Versions

- Java EE 5, 6, 7, 8
- Jakarta EE 8, 9, 9.1, 10, 11

### MicroProfile Versions

- MicroProfile 1.0 - 6.x
- Automatic detection of namespace changes (MP 4.0+ uses jakarta.*)

## Resources

- [DependencyAnalysisHelper API](../ee-dependency-scanner-core/src/main/java/io/openliberty/tools/scanner/util/DependencyAnalysisHelper.java)
- [Eclipse JDT Documentation](https://www.eclipse.org/jdt/)
- [IntelliJ Platform SDK](https://plugins.jetbrains.com/docs/intellij/welcome.html)
- [Language Server Protocol](https://microsoft.github.io/language-server-protocol/)
- [Jakarta EE](https://jakarta.ee/)
- [MicroProfile](https://microprofile.io/)