# Multi-Module Maven Project with Custom JARs

This test project demonstrates complex dependency scenarios with:
- Multi-module Maven structure
- Inter-module dependencies
- Custom JAR dependencies with transitive dependencies
- Mixed Jakarta EE and MicroProfile dependencies

## Project Structure

```
multi-module-with-jars/
├── pom.xml                    # Parent POM
├── module-a/
│   └── pom.xml               # Jakarta EE Servlet + CDI
├── module-b/
│   └── pom.xml               # Persistence + MicroProfile + depends on module-a
└── lib/
    └── custom-library-1.0.0.jar  # Custom JAR with transitive deps
```

## Module Dependencies

### Module A
- **Jakarta EE Servlet API 6.0.0** (Jakarta EE 10)
- **Jakarta EE CDI API 4.0.1** (Jakarta EE 10)

### Module B
- **Jakarta EE Persistence API 3.1.0** (Jakarta EE 10)
- **MicroProfile Config API 3.0** (MicroProfile 6.0)
- **Module A** (transitive: Servlet + CDI)
- **Custom Library 1.0.0** (transitive: Commons Lang + Guava)

## Transitive Dependency Flow

```
module-b
├── Direct: jakarta.persistence-api:3.1.0
├── Direct: microprofile-config-api:3.0
├── Direct: custom-library:1.0.0
│   ├── Transitive: commons-lang3:3.12.0
│   └── Transitive: guava:31.1-jre
└── Direct: module-a
    ├── Transitive: jakarta.servlet-api:6.0.0
    └── Transitive: jakarta.cdi-api:4.0.1
```

## Expected Analysis Results

### Analyzing Parent Module
Should detect all dependencies from both sub-modules:
- Jakarta EE 10 (from Servlet 6.0.0, CDI 4.0.1, Persistence 3.1.0)
- MicroProfile 6.0 (from Config 3.0)
- Custom library transitives (Commons Lang, Guava)

### Analyzing Module A
Should detect:
- Jakarta EE 10 (from Servlet 6.0.0, CDI 4.0.1)

### Analyzing Module B
Should detect:
- Jakarta EE 10 (from Persistence 3.1.0)
- MicroProfile 6.0 (from Config 3.0)
- Transitive Jakarta EE 10 from module-a (Servlet, CDI)
- Transitive dependencies from custom JAR (Commons Lang, Guava)

## Test Scenarios

1. **Multi-module aggregation**: Analyze parent to get all sub-module dependencies
2. **Inter-module transitives**: Module B inherits dependencies from Module A
3. **Custom JAR transitives**: Module B gets transitive deps from custom-library JAR
4. **Version consistency**: All Jakarta EE dependencies should resolve to EE 10
5. **Mixed platforms**: Both Jakarta EE and MicroProfile detected correctly
6. **Source tracking**: Dependencies tracked from MAVEN, JAR_SCAN, and TRANSITIVE sources

## Usage in Tests

```java
// Analyze parent module
ClasspathAnalysisResult parentResult = analyzer.analyze(
    Paths.get("test-projects/multi-module-with-jars")
);

// Analyze individual module
ClasspathAnalysisResult moduleAResult = analyzer.analyze(
    Paths.get("test-projects/multi-module-with-jars/module-a")
);

// Analyze module with transitives
ClasspathAnalysisResult moduleBResult = analyzer.analyze(
    Paths.get("test-projects/multi-module-with-jars/module-b")
);