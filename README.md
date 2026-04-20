# EE Dependency Scanner

A unified dependency detection solution for Jakarta EE, Java EE, and MicroProfile across Liberty and Jakarta EE tooling projects.

## Overview

The EE Dependency Scanner is a Java library that automatically detects and analyzes Jakarta EE, Java EE, and MicroProfile dependencies in your projects. It supports multiple build systems and dependency sources, providing comprehensive insights into your project's enterprise Java dependencies.

## Features

- **Multi-Source Detection**: Automatically detects dependencies from:
  - Maven `pom.xml` files
  - Gradle `build.gradle` files
  - Eclipse `.classpath` files
  - JAR manifest files with embedded `pom.xml`
  - Recursive JAR scanning

- **Comprehensive Analysis**:
  - Identifies Jakarta EE, Java EE, and MicroProfile dependencies
  - Detects platform versions from individual dependency versions
  - Identifies version conflicts across dependencies
  - Provides detailed feature-level version information
  - Tracks dependency sources for traceability

- **Smart Version Detection**:
  - Maps individual feature versions to platform versions
  - Detects mixed version scenarios
  - Supports Jakarta EE 8, 9, 9.1, 10, and 11
  - Supports MicroProfile 3.3 through 6.1

- **Flexible Integration**:
  - Simple programmatic API
  - Customizable parsers
  - Configurable fallback scanning
  - Detailed analysis results with metadata

## Requirements

- Java 11 or higher
- Maven 3.6+ (for building)

## Installation

### Maven

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.openliberty.tools</groupId>
    <artifactId>ee-dependency-scanner</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Gradle

Add the following to your `build.gradle`:

```gradle
implementation 'io.openliberty.tools:ee-dependency-scanner:1.0.0-SNAPSHOT'
```

## Quick Start

### Basic Usage

```java
import io.openliberty.tools.scanner.analyzer.ClasspathAnalyzer;
import io.openliberty.tools.scanner.model.ClasspathAnalysisResult;

// Create analyzer
ClasspathAnalyzer analyzer = new ClasspathAnalyzer();

// Analyze a project directory
ClasspathAnalysisResult result = analyzer.analyze("/path/to/project");

// Get results
System.out.println("Total dependencies: " + result.getTotalDependenciesFound());
System.out.println("Jakarta EE dependencies: " + result.getJakartaEEDependencies().size());
System.out.println("MicroProfile dependencies: " + result.getMicroProfileDependencies().size());

// Print summary
System.out.println(result.getSummary());
```

### Detecting Project Type

```java
ClasspathAnalyzer analyzer = new ClasspathAnalyzer();
File projectDir = new File("/path/to/project");

List<String> projectTypes = analyzer.detectProjectType(projectDir);
System.out.println("Detected project types: " + projectTypes);
// Output: [Maven, Eclipse]
```

### Checking for EE Dependencies

```java
ClasspathAnalyzer analyzer = new ClasspathAnalyzer();
File projectDir = new File("/path/to/project");

boolean hasEE = analyzer.hasEEDependencies(projectDir);
if (hasEE) {
    System.out.println("Project uses Jakarta EE, Java EE, or MicroProfile");
}
```

### Version Detection

```java
ClasspathAnalysisResult result = analyzer.analyze("/path/to/project");

// Get Jakarta EE platform versions
Set<String> jakartaVersions = result.getJakartaEEPlatformVersions();
System.out.println("Jakarta EE versions: " + jakartaVersions);

// Get MicroProfile platform versions
Set<String> mpVersions = result.getMicroProfilePlatformVersions();
System.out.println("MicroProfile versions: " + mpVersions);

// Check for version conflicts
if (result.hasMultipleJakartaEEVersions()) {
    System.out.println("Warning: Multiple Jakarta EE versions detected!");
}
```

### Feature-Level Version Details

```java
ClasspathAnalysisResult result = analyzer.analyze("/path/to/project");

// Get detailed feature versions
Map<String, Set<String>> jakartaFeatures = result.getJakartaEEFeatureVersions();
for (Map.Entry<String, Set<String>> entry : jakartaFeatures.entrySet()) {
    System.out.println(entry.getKey() + ": " + entry.getValue());
}
// Output:
// servlet: [6.0.0]
// persistence: [3.1.0]
// cdi: [4.0.1]
```

### Accessing Individual Dependencies

```java
ClasspathAnalysisResult result = analyzer.analyze("/path/to/project");

// Iterate through all dependencies
for (DependencyInfo dep : result.getAllDependencies()) {
    System.out.println(dep.getCoordinate() + " from " + dep.getSource());
}

// Filter by type
for (DependencyInfo dep : result.getJakartaEEDependencies()) {
    System.out.println("Jakarta EE: " + dep.getArtifactId() + " v" + dep.getVersion());
}
```

## Advanced Usage

### Custom Parsers

```java
import io.openliberty.tools.scanner.parser.*;

// Create custom parser list
List<DependencyParser> customParsers = Arrays.asList(
    new MavenPomParser(),
    new EclipseClasspathParser()
    // Add your custom parser here
);

// Create analyzer with custom parsers
ClasspathAnalyzer analyzer = new ClasspathAnalyzer(customParsers);
```

### Disable Fallback Scanning

```java
ClasspathAnalyzer analyzer = new ClasspathAnalyzer();

// Disable recursive JAR scanning fallback
analyzer.setEnableFallbackScanning(false);

ClasspathAnalysisResult result = analyzer.analyze("/path/to/project");
```

### Analyzing Specific Files

```java
ClasspathAnalyzer analyzer = new ClasspathAnalyzer();

// Analyze a specific pom.xml
File pomFile = new File("/path/to/pom.xml");
ClasspathAnalysisResult result = analyzer.analyze(pomFile);

// Analyze a specific JAR
File jarFile = new File("/path/to/library.jar");
ClasspathAnalysisResult result = analyzer.analyze(jarFile);
```

## Supported Dependencies

### Jakarta EE

The scanner recognizes Jakarta EE dependencies including:
- Servlet API
- Persistence API (JPA)
- CDI (Contexts and Dependency Injection)
- JAX-RS (RESTful Web Services)
- JSF (Faces)
- JSON-B and JSON-P
- Bean Validation
- Jakarta Mail
- Jakarta Messaging

### Java EE

Legacy Java EE dependencies (javax.* namespace) are also detected and categorized separately.

### MicroProfile

The scanner recognizes MicroProfile specifications including:
- Config
- Health
- Metrics
- OpenAPI
- REST Client
- Fault Tolerance
- JWT Authentication

## Architecture

### Core Components

- **ClasspathAnalyzer**: Main entry point for dependency analysis with SPI support
- **DependencyParser**: Interface for implementing custom parsers with priority system
- **DependencyParserProvider**: SPI interface for contributing parsers via ServiceLoader
- **DependencyInfo**: Model representing a single dependency
- **ClasspathAnalysisResult**: Comprehensive analysis results with metadata
- **VersionMappingRegistry**: Centralized version mapping with fail-fast validation

### Built-in Parsers

Parsers are executed in priority order (lower number = higher priority):

1. **MavenPomParser** (Priority: 10): Parses Maven `pom.xml` files
2. **GradleBuildParser** (Priority: 20): Parses Gradle build files
3. **EclipseClasspathParser** (Priority: 100): Parses Eclipse `.classpath` files
4. **JarManifestScanner** (Priority: 200): Scans JAR files and extracts embedded dependencies
5. **MavenDependencyTreeParser**: Parses Maven dependency tree output
6. **GradleDependencyTreeParser**: Parses Gradle dependency tree output

### Parser Priority System

The parser priority system ensures that more authoritative sources are preferred:

- **0-99**: Build system parsers (Maven, Gradle) - most authoritative
- **100-199**: IDE-specific parsers (Eclipse, IntelliJ) - secondary sources
- **200+**: Fallback parsers (JAR scanning) - least authoritative

Custom parsers can implement `getPriority()` to control execution order.

### Extensibility via SPI

The scanner supports plugin contributions through Java's ServiceLoader mechanism:

```java
// In your plugin, implement DependencyParserProvider
public class MyParserProvider implements DependencyParserProvider {
    @Override
    public List<DependencyParser> getParsers() {
        return Arrays.asList(new MyCustomParser());
    }
}

// Register in META-INF/services/io.openliberty.tools.scanner.parser.DependencyParserProvider
io.myplugin.MyParserProvider
```

This allows IDE plugins (Eclipse, IntelliJ, VS Code) to contribute parsers that read from live workspace models without modifying the core library.

### Deduplication Strategy

The analyzer uses intelligent deduplication:

1. **Key**: Dependencies are deduplicated by `groupId:artifactId` (not version)
2. **Selection**: When duplicates exist, the analyzer selects the entry with:
   - Most complete information (version, source, type)
   - Higher priority source (Maven > Gradle > Eclipse > JAR)
3. **Version Conflicts**: Multiple versions of the same artifact are preserved in feature version maps

### Version Detection

Version detection uses a centralized registry approach:

1. **VersionMappingRegistry**: Loads and validates version mappings from properties files
2. **Property-based mapping**: Maps feature versions to platform versions
3. **Fail-fast validation**: Ensures properties files are correctly packaged

Configuration files:
- `jakarta-ee-versions.properties`: Jakarta EE version mappings
- `microprofile-versions.properties`: MicroProfile version mappings

## Building from Source

```bash
# Clone the repository
git clone https://github.com/OpenLiberty/ee-dependency-scanner.git
cd ee-dependency-scanner

# Build with Maven
mvn clean install

# Run tests
mvn test
```

## Testing

The project includes comprehensive test coverage:

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ClasspathAnalyzerTest

# Run with verbose output
mvn test -X
```

### Test Projects

The `src/test/resources/test-projects` directory contains sample projects for testing:

**Jakarta EE Projects:**
- `maven-jakarta-ee10`: Maven project with Jakarta EE 10 dependencies
- `maven-jakarta-ee9`: Maven project with Jakarta EE 9 dependencies
- `maven-javaee6`: Maven project with Java EE 6 dependencies
- `maven-javaee7`: Maven project with Java EE 7 dependencies
- `maven-javaee8`: Maven project with Java EE 8 dependencies
- `gradle-jakarta-ee10`: Gradle project with Jakarta EE 10

**MicroProfile Projects:**
- `gradle-microprofile`: Gradle project with MicroProfile 5.0 dependencies

**Version Testing:**
- `maven-mixed-versions`: Maven project with mixed Jakarta EE versions
- `maven-duplicate-deps`: Maven project testing deduplication with conflicting versions

**Multi-Source Testing:**
- `maven-with-classpath`: Maven project with Eclipse classpath
- `single-feature-cdi`: Project with single CDI dependency
- `multi-module-with-jars`: Multi-module Maven project with custom JARs
- `project-with-transitive-jar`: Project testing transitive dependency extraction

## Examples

### Example 1: Analyzing a Maven Project

```java
ClasspathAnalyzer analyzer = new ClasspathAnalyzer();
ClasspathAnalysisResult result = analyzer.analyze("./my-maven-project");

System.out.println("=== Analysis Results ===");
System.out.println("Detection Method: " + result.getDetectionMethod());
System.out.println("Total Dependencies: " + result.getTotalDependenciesFound());
System.out.println("Analysis Time: " + result.getAnalysisTimeMs() + "ms");

if (!result.getJakartaEEDependencies().isEmpty()) {
    System.out.println("\nJakarta EE Dependencies:");
    for (DependencyInfo dep : result.getJakartaEEDependencies()) {
        System.out.println("  - " + dep.getCoordinate());
    }
    System.out.println("Detected Versions: " + result.getJakartaEEPlatformVersions());
}
```

### Example 2: Version Conflict Detection

```java
ClasspathAnalyzer analyzer = new ClasspathAnalyzer();
ClasspathAnalysisResult result = analyzer.analyze("./my-project");

if (result.hasMultipleJakartaEEVersions()) {
    System.out.println("WARNING: Version conflicts detected!");
    
    Map<String, Set<String>> features = result.getJakartaEEFeatureVersions();
    for (Map.Entry<String, Set<String>> entry : features.entrySet()) {
        if (entry.getValue().size() > 1) {
            System.out.println("  " + entry.getKey() + " has multiple versions: " + 
                             entry.getValue());
        }
    }
}
```

### Example 3: Multi-Module Project Analysis

```java
ClasspathAnalyzer analyzer = new ClasspathAnalyzer();

// Analyze parent project
File parentDir = new File("./multi-module-project");
ClasspathAnalysisResult parentResult = analyzer.analyze(parentDir);

// Analyze each module
File[] modules = parentDir.listFiles(File::isDirectory);
for (File module : modules) {
    if (new File(module, "pom.xml").exists()) {
        ClasspathAnalysisResult moduleResult = analyzer.analyze(module);
        System.out.println("\nModule: " + module.getName());
        System.out.println(moduleResult.getSummary());
    }
}
```

## SPI Integration Examples

The `spi-examples/` directory contains complete integration examples for Liberty Tools projects:

### Available Integrations

1. **LSP4Jakarta Integration** (`spi-examples/lsp4jakarta-adapter-impl/`)
   - Provider-based integration using `IProjectLabelProvider` (recommended)
   - Direct integration using `LSP4JakartaDependencyAnalyzer`
   - Provides Jakarta EE and Java EE version labels for diagnostics
   - Three-tier dependency collection (M2E → Buildship → JDT)

2. **LSP4MP Integration** (`spi-examples/lsp4mp-adapter-impl/`)
   - Provider-based integration using `IProjectLabelProvider` (recommended)
   - Direct integration using `LSP4MPDependencyAnalyzer`
   - Provides Jakarta EE and MicroProfile version labels
   - Three-tier dependency collection (M2E → Buildship → JDT)

3. **Eclipse Adapter** (`spi-examples/scanner-eclipse-adapter-impl/`)
   - Complete reference implementation for Eclipse IDE
   - Uses M2E, Buildship, and JDT APIs
   - Demonstrates three-tier dependency collection approach

4. **IntelliJ Adapter** (`spi-examples/scanner-intellij-adapter-impl/`)
   - Complete reference implementation for IntelliJ IDEA
   - Uses IntelliJ Platform APIs (ModuleRootManager, OrderEntry, Library)
   - Shows how to integrate with IntelliJ's project model

### Quick Start

For LSP4Jakarta and LSP4MP, the **provider-based approach is recommended** as it:
- Follows existing architectural patterns
- Requires minimal code changes
- Automatically propagates labels to all diagnostics and code actions
- Provides comprehensive M2E and Buildship support

Example for LSP4Jakarta:
```java
// 1. Copy EEVersionProjectLabelProvider.java to your project
// 2. Register in plugin.xml:
<extension point="org.eclipse.lsp4jakarta.jdt.core.projectLabelProviders">
  <provider class="org.eclipse.lsp4jakarta.jdt.core.ee.EEVersionProjectLabelProvider"/>
</extension>

// 3. Labels are automatically available in diagnostics:
// - jakartaee, jakartaee-9, jakartaee-10
// - javaee, javaee-6, javaee-7, javaee-8
```

For detailed documentation, see:
- [SPI Examples README](spi-examples/README.md) - Overview and comparison of all approaches
- [LSP4Jakarta Integration](spi-examples/lsp4jakarta-adapter-impl/README.md)
- [LSP4MP Integration](spi-examples/lsp4mp-adapter-impl/README.md)
- [Eclipse Adapter](spi-examples/scanner-eclipse-adapter-impl/README.md)
- [IntelliJ Adapter](spi-examples/scanner-intellij-adapter-impl/README.md)

### DependencyAnalysisHelper Utility

All integrations use the `DependencyAnalysisHelper` utility class which provides:
- Dependency creation with Builder pattern
- JAR dependency extraction
- Version detection (Jakarta EE 5-11, MicroProfile 1.0-6.x)
- Filtering by type (Jakarta EE, Java EE, MicroProfile)
- Deduplication with intelligent selection
- Primary version detection

```java
import io.openliberty.tools.scanner.util.DependencyAnalysisHelper;

// Create helper
DependencyAnalysisHelper helper = new DependencyAnalysisHelper();

// Create dependency
DependencyInfo dep = helper.createDependency(
    "jakarta.servlet", "jakarta.servlet-api", "6.0.0",
    DependencySource.IDE_RESOLVED, DependencyType.JAKARTA_EE
);

// Detect versions
ClasspathAnalysisResult result = helper.detectVersions(dependencies);
String primaryVersion = helper.getPrimaryVersion(result.getJakartaEEPlatformVersions());
```

## Contributing

Contributions are welcome! Please follow these guidelines:

1. Fork the repository
2. Create a feature branch
3. Write tests for new functionality
4. Ensure all tests pass
5. Submit a pull request

## License

## Support


## Changelog

### Version 1.0.0-SNAPSHOT

**Initial Release:**
- Support for Maven, Gradle, and Eclipse projects
- Jakarta EE, Java EE, and MicroProfile detection
- Version detection and conflict identification
- Comprehensive test coverage

**Architectural Improvements:**
- **SPI Extension Point**: Added `DependencyParserProvider` interface with ServiceLoader support for plugin contributions
- **Parser Priority System**: Implemented priority-based parser ordering (0-99: build systems, 100-199: IDEs, 200+: fallback)
- **Improved Deduplication**: Fixed deduplication to use `groupId:artifactId` as key with intelligent completeness scoring
- **Consolidated Version Detection**: Created `VersionMappingRegistry` class with centralized, fail-fast version mapping
- **Testable Architecture**: Refactored `JarDependencyExtractor` from static to instance methods for better testability

**Test Coverage:**
- 62 comprehensive tests covering all functionality
- New test projects for Jakarta EE 9, MicroProfile, and deduplication scenarios
- Tests for parser priority, SPI, version mapping, and deduplication logic

---
