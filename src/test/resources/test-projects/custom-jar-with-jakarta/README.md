# Custom JAR with Jakarta EE Dependencies

This test project demonstrates a complex scenario where a user has a custom JAR library that contains Jakarta EE dependencies embedded inside it.

## Scenario

A developer has created a custom library (`custom-jakarta-lib-1.0.0.jar`) that:
1. Contains Jakarta EE dependencies in its `pom.xml` (metadata)
2. Has actual Jakarta EE JAR files bundled inside a `lib/` directory within the JAR
3. Is used as a system dependency in a Maven project

This simulates real-world scenarios where:
- Third-party libraries bundle their dependencies
- Legacy libraries package Jakarta EE APIs internally
- Custom corporate libraries include Jakarta EE specifications

## Project Structure

```
custom-jar-with-jakarta/
├── pom.xml                           # Maven project using the custom JAR
├── create-custom-jar.sh              # Script to build the custom JAR
├── lib/
│   └── custom-jakarta-lib-1.0.0.jar  # Custom JAR with embedded Jakarta EE
└── README.md                         # This file
```

## Custom JAR Contents

The `custom-jakarta-lib-1.0.0.jar` contains:

```
custom-jakarta-lib-1.0.0.jar
├── META-INF/
│   ├── MANIFEST.MF
│   └── maven/
│       └── com.example.custom/
│           └── custom-jakarta-lib/
│               ├── pom.xml           # Declares Jakarta EE dependencies
│               └── pom.properties
├── lib/                              # Embedded JAR files
│   ├── jakarta.servlet-api-6.0.0.jar
│   ├── jakarta.persistence-api-3.1.0.jar
│   └── jakarta.enterprise.cdi-api-4.0.1.jar
└── com/
    └── example/
        └── custom/
            └── CustomLibrary.class
```

## Dependencies

### From Custom JAR (via pom.xml metadata)
- jakarta.servlet-api 6.0.0 (Jakarta EE 10)
- jakarta.persistence-api 3.1.0 (Jakarta EE 10)
- jakarta.enterprise.cdi-api 4.0.1 (Jakarta EE 10)

### From Custom JAR (embedded JARs in lib/)
- jakarta.servlet-api-6.0.0.jar (actual JAR file)
- jakarta.persistence-api-3.1.0.jar (actual JAR file)
- jakarta.enterprise.cdi-api-4.0.1.jar (actual JAR file)

### Direct Maven Dependencies
- microprofile-config-api 3.0 (MicroProfile 6.0)

## Expected Analysis Results

The scanner should detect:
1. **Jakarta EE 10** from the custom JAR's pom.xml metadata
2. **Jakarta EE 10** from the embedded JAR files inside the custom JAR
3. **MicroProfile 6.0** from the direct Maven dependency
4. All dependencies should be properly tracked with their sources:
   - `JAR_SCAN` for dependencies from custom JAR's pom.xml
   - `JAR_SCAN` for embedded JAR files
   - `MAVEN` for direct Maven dependencies

## Building the Custom JAR

Run the script to create the custom JAR:

```bash
cd src/test/resources/test-projects/custom-jar-with-jakarta
chmod +x create-custom-jar.sh
./create-custom-jar.sh
```

This will:
1. Create a temporary directory structure
2. Generate pom.xml with Jakarta EE dependencies
3. Download actual Jakarta EE JAR files from Maven Central
4. Bundle everything into `lib/custom-jakarta-lib-1.0.0.jar`

## Test Scenarios

1. **JAR Scanning**: Verify scanner can extract dependencies from JAR's embedded pom.xml
2. **Nested JAR Detection**: Verify scanner can detect JAR files inside another JAR
3. **Multi-level Dependencies**: Test extraction of dependencies at multiple levels
4. **Mixed Sources**: Combine JAR-based and Maven-based dependencies
5. **Version Consistency**: All Jakarta EE deps should resolve to EE 10

## Usage in Tests

```java
@Test
void testCustomJarWithEmbeddedJakartaEE() {
    Path projectPath = Paths.get("src/test/resources/test-projects/custom-jar-with-jakarta");
    ClasspathAnalysisResult result = analyzer.analyze(projectPath);
    
    // Should detect Jakarta EE 10 from custom JAR
    assertTrue(result.getDetectedVersions().contains("Jakarta EE 10"));
    
    // Should detect MicroProfile 6.0 from Maven
    assertTrue(result.getDetectedVersions().contains("MicroProfile 6.0"));
    
    // Should have dependencies from both JAR_SCAN and MAVEN sources
    assertTrue(result.getDependencies().stream()
        .anyMatch(d -> d.getSource() == DependencySource.JAR_SCAN));
    assertTrue(result.getDependencies().stream()
        .anyMatch(d -> d.getSource() == DependencySource.MAVEN));
}
```

## Notes

- The script requires `curl` or `wget` to download Jakarta EE JARs
- If download fails, placeholder files are created for testing
- The custom JAR simulates a real-world vendor library scenario
- This tests the scanner's ability to handle complex dependency structures