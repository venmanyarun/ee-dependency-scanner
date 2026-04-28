# EE Dependency Scanner

A flexible, extensible library for analyzing Java EE, Jakarta EE, and MicroProfile dependencies across Maven, Gradle, and IDE project models.

## Features

### Generic API with IDE Support

The API supports **generic project types** using Java generics:
- **File-based projects** (Maven/Gradle) - `DependencyParser<File>`
- **IntelliJ IDEA Modules** - `DependencyParser<Module>`
- **Eclipse IJavaProjects** - `DependencyParser<IJavaProject>`
- **Any custom type** - `DependencyParser<T>`

```java
// File-based parser (Maven/Gradle)
DependencyParser<File> mavenParser = new MavenPomParser();
List<DependencyInfo> deps = mavenParser.parse(new File("/path/to/pom.xml"));

// IntelliJ Module parser
DependencyParser<Module> intellijParser = new IntelliJModuleParser();
List<DependencyInfo> deps = intellijParser.parse(module);

// Eclipse IJavaProject parser
DependencyParser<IJavaProject> eclipseParser = new EclipseJavaProjectParser();
List<DependencyInfo> deps = eclipseParser.parse(javaProject);
```

### Dependency Filtering

Filtering capabilities allow you to collect only relevant dependencies:

```java
// Only MicroProfile dependencies
List<DependencyInfo> mpDeps = parser.parse(module, DependencyFilter.MICROPROFILE);

// Only Jakarta EE dependencies
List<DependencyInfo> javaEEDeps = parser.parse(module, DependencyFilter.JAKARTA_EE);

// Both MicroProfile and Jakarta EE
List<DependencyInfo> allEE = parser.parse(module, DependencyFilter.MICROPROFILE_AND_JAKARTA_EE);

// Custom filter
DependencyFilter customFilter = new DependencyFilter.Builder()
    .addGroupIdPrefix("org.eclipse.microprofile")
    .addGroupIdPrefix("io.quarkus")
    .build();
List<DependencyInfo> customDeps = parser.parse(module, customFilter);
```

### Performance Optimizations

- **Module-specific parsing**: Parse only the module you need, not all projects
- **Early filtering**: Dependencies are filtered during collection
- **IDE cache utilization**: Leverages IntelliJ's `OrderEnumerator` and Eclipse's resolved classpath
- **Fast library checks**: Uses `JavaLibraryUtil` (IntelliJ) for instant dependency detection


## Quick Start

### Maven Dependencies

```xml
<!-- Pure API (zero dependencies) -->
<dependency>
    <groupId>io.openliberty.tools</groupId>
    <artifactId>ee-dependency-scanner-api</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>

<!-- Core implementation (optional, for utilities) -->
<dependency>
    <groupId>io.openliberty.tools</groupId>
    <artifactId>ee-dependency-scanner-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <scope>runtime</scope>
</dependency>
```

### Basic Usage (File-based)

```java
import io.openliberty.tools.scanner.api.*;

// Create parser
DependencyParser<File> parser = new MavenPomParser();

// Parse with filtering
List<DependencyInfo> mpDeps = parser.parse(
    new File("/path/to/pom.xml"),
    DependencyFilter.MICROPROFILE
);

// Check results
for (DependencyInfo dep : mpDeps) {
    System.out.println(dep.getGroupId() + ":" + dep.getArtifactId() + ":" + dep.getVersion());
}
```

### IntelliJ Plugin Usage

```java
import io.openliberty.tools.scanner.api.*;
import com.intellij.openapi.module.Module;

// Create IntelliJ parser
DependencyParser<Module> parser = new IntelliJModuleParser();

// Get specific module
Module module = ModuleManager.getInstance(project).findModuleByName("my-module");

// Fast check using JavaLibraryUtil
if (parser.hasMicroProfileConfig(module)) {
    System.out.println("Module has MicroProfile Config!");
}

// Get all MicroProfile dependencies
List<DependencyInfo> mpDeps = parser.parse(module, DependencyFilter.MICROPROFILE);
System.out.println("Found " + mpDeps.size() + " MicroProfile dependencies");
```

### Eclipse Plugin Usage

```java
import io.openliberty.tools.scanner.api.*;
import org.eclipse.jdt.core.IJavaProject;

// Create Eclipse parser
DependencyParser<IJavaProject> parser = new EclipseJavaProjectParser();

// Get Java project
IJavaProject javaProject = JavaCore.create(project);

// Get Jakarta EE version
String version = parser.getJakartaEEVersion(javaProject);
if (version != null) {
    System.out.println("Jakarta EE version: " + version);
}

// Get all Jakarta EE dependencies
List<DependencyInfo> javaEEDeps = parser.parse(javaProject, DependencyFilter.JAKARTA_EE);
```

## Documentation

- **[API Examples](api-examples/)** - Complete implementation examples for IntelliJ and Eclipse
  - [IntelliJ Plugin Example](api-examples/intellij-plugin/)
  - [LSP4Jakarta Integration Example](api-examples/lsp4jakarta-integration/)
- **[Core Scanner Documentation](ee-dependency-scanner-core/)** - Core library details
- **[API Documentation](ee-dependency-scanner-api/)** - Complete API reference

## Supported Technologies

### Java EE / Jakarta EE
- Java EE 5, 6, 7, 8
- Jakarta EE 8, 9, 9.1, 10, 11

### MicroProfile
- MicroProfile 1.x - 6.x
- Individual specifications (Config, Metrics, Health, etc.)

### Build Tools
- Maven (pom.xml)
- Gradle (build.gradle, build.gradle.kts)

### IDEs
- IntelliJ IDEA (Module API)
- Eclipse (IJavaProject API)
- File-based (Maven/Gradle)

## Building

```bash
# Build API module
cd ee-dependency-scanner-api
mvn clean install

# Build core module
cd ee-dependency-scanner-core
mvn clean install

# Run tests
mvn test
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## Custom Filters

Create custom filters for specific needs:

```java
DependencyFilter customFilter = new DependencyFilter.Builder()
    .addGroupIdPrefix("com.mycompany")
    .addArtifactIdPrefix("myframework-")
    .build();
```

## Contributing

Contributions are welcome! Please follow these guidelines:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

This project is licensed under the Apache License 2.0.

## Support

For questions, issues, or feature requests:
1. Review the [API Examples](api-examples/)
2. Check the [Core Documentation](ee-dependency-scanner-core/)
3. Open an issue on GitHub
