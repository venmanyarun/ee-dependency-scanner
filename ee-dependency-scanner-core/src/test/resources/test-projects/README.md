# Test Projects

This directory contains sample projects for testing the EE Dependency Scanner.

## Test Project Structure

### Jakarta EE Projects

#### 1. maven-jakarta-ee10
**Purpose:** Test Maven project with Jakarta EE 10 dependencies
**Dependencies:**
- jakarta.servlet-api 6.0.0 (Jakarta EE 10)
- jakarta.persistence-api 3.1.0 (Jakarta EE 10)
- jakarta.enterprise.cdi-api 4.0.1 (Jakarta EE 10)

**Expected Result:** Detects Jakarta EE 10

#### 2. maven-jakarta-ee9
**Purpose:** Test Maven project with Jakarta EE 9 dependencies
**Dependencies:**
- jakarta.servlet-api 5.0.0 (Jakarta EE 9)
- jakarta.persistence-api 3.0.0 (Jakarta EE 9)
- jakarta.enterprise.cdi-api 3.0.0 (Jakarta EE 9)

**Expected Result:** Detects Jakarta EE 9

#### 3. maven-javaee6
**Purpose:** Test Maven project with Java EE 6 dependencies
**Dependencies:**
- javax.servlet-api 3.0.1 (Java EE 6)
- javax.persistence-api 2.0 (Java EE 6)

**Expected Result:** Detects Java EE 6

#### 4. maven-javaee7
**Purpose:** Test Maven project with Java EE 7 dependencies
**Dependencies:**
- javax.servlet-api 3.1.0 (Java EE 7)
- javax.persistence-api 2.1 (Java EE 7)
- javax.enterprise.cdi-api 1.2 (Java EE 7)

**Expected Result:** Detects Java EE 7

#### 5. maven-javaee8
**Purpose:** Test Maven project with Java EE 8 dependencies
**Dependencies:**
- javax.servlet-api 4.0.1 (Java EE 8)
- javax.persistence-api 2.2 (Java EE 8)
- javax.enterprise.cdi-api 2.0 (Java EE 8)

**Expected Result:** Detects Java EE 8

#### 5a. maven-javaee5
**Purpose:** Test Maven project with Java EE 5 dependencies
**Dependencies:**
- servlet-api 2.5 (Java EE 5)
- jsp-api 2.1 (Java EE 5)
- ejb-api 3.0 (Java EE 5)
- persistence-api 1.0 (Java EE 5)

**Expected Result:** Detects Java EE 5

#### 6. gradle-jakarta-ee10
**Purpose:** Test Gradle project with Jakarta EE 10 and MicroProfile
**Dependencies:**
- jakarta.servlet-api 6.0.0
- jakarta.persistence-api 3.1.0
- jakarta.enterprise.cdi-api 4.0.1
- microprofile-config-api 3.0

**Expected Result:** Detects Jakarta EE 10 and MicroProfile 6.0

#### 6a. gradle-jakarta-ee9.1
**Purpose:** Test Gradle project with Jakarta EE 9.1 dependencies
**Dependencies:**
- jakarta.servlet-api 5.0.0 (Jakarta EE 9.1)
- jakarta.persistence-api 3.0.0 (Jakarta EE 9.1)
- jakarta.enterprise.cdi-api 3.0.2 (Jakarta EE 9.1)
- jakarta.ws.rs-api 3.0.0 (Jakarta EE 9.1)
- jakarta.json.bind-api 2.0.0 (Jakarta EE 9.1)

**Expected Result:** Detects Jakarta EE 9.1

### MicroProfile Projects

#### 7. gradle-microprofile
**Purpose:** Test Gradle project with MicroProfile 5.0 dependencies
**Dependencies:**
- microprofile-config-api 3.0
- microprofile-rest-client-api 3.0
- microprofile-health-api 4.0

**Expected Result:** Detects MicroProfile 5.0

#### 7a. gradle-microprofile-4.0
**Purpose:** Test Gradle project with MicroProfile 4.0 dependencies
**Dependencies:**
- microprofile-config-api 2.0
- microprofile-rest-client-api 2.0
- microprofile-health-api 3.0
- microprofile-metrics-api 3.0
- microprofile-fault-tolerance-api 3.0
- microprofile-jwt-auth-api 1.2
- microprofile-openapi-api 2.0
- microprofile-opentracing-api 2.0

**Expected Result:** Detects MicroProfile 4.0

### Version Testing Projects

#### 8. maven-mixed-versions
**Purpose:** Test version conflict detection
**Dependencies:**
- jakarta.enterprise.cdi-api 4.0.1 (Jakarta EE 10)
- jakarta.persistence-api 3.0.0 (Jakarta EE 9)
- jakarta.servlet-api 6.0.0 (Jakarta EE 10)

**Expected Result:** Detects both Jakarta EE 10 and 9, flags version conflict

#### 9. maven-duplicate-deps
**Purpose:** Test deduplication with conflicting versions
**Dependencies:**
- jakarta.servlet-api 6.0.0 (declared twice)
- jakarta.servlet-api 5.0.0 (different version)
- jakarta.persistence-api 3.1.0 (declared twice)

**Expected Result:** Deduplicates to single entry per artifact, prefers higher version

#### 9a. maven-mixed-javaee
**Purpose:** Test mixed Java EE 6, 7, and 8 version detection
**Dependencies:**
- javax.servlet-api 3.0.1 (Java EE 6)
- javax.persistence-api 2.1 (Java EE 7)
- cdi-api 2.0 (Java EE 8)
- javax.ws.rs-api 2.0.1 (Java EE 7)
- javax.json.bind-api 1.0 (Java EE 8)
- javax.ejb-api 3.1 (Java EE 6)

**Expected Result:** Detects Java EE 6, 7, and 8, flags version conflicts

### Multi-Source Testing Projects

#### 10. maven-with-classpath
**Purpose:** Test mixed dependency sources (Maven + Eclipse classpath)
**Maven Dependencies:**
- jakarta.servlet-api 6.0.0
- microprofile-config-api 3.0

**Classpath Dependencies:**
- jakarta.persistence-api 3.1.0
- jakarta.enterprise.cdi-api 4.0.1

**Expected Result:** Detects dependencies from both Maven and Eclipse sources

#### 11. single-feature-cdi
**Purpose:** Test single feature version detection
**Dependencies:**
- jakarta.enterprise.cdi-api 4.0.1 (only CDI)

**Expected Result:** Detects Jakarta EE 10 from single CDI dependency

#### 12. custom-jar-with-jakarta
**Purpose:** Test custom JAR with embedded Jakarta EE dependencies inside lib/
**Custom JAR Contents:**
- Embedded pom.xml with Jakarta EE 10 dependencies
- lib/ directory containing actual Jakarta EE JAR files:
  - jakarta.servlet-api-6.0.0.jar
  - jakarta.persistence-api-3.1.0.jar
  - jakarta.enterprise.cdi-api-4.0.1.jar

**Maven Dependencies:**
- custom-jakarta-lib 1.0.0 (system dependency)
- microprofile-config-api 3.0

**Expected Result:**
- Detects Jakarta EE 10 from custom JAR's pom.xml
- Detects Jakarta EE 10 from embedded JAR files
- Detects MicroProfile 6.0 from Maven dependency
- Properly tracks JAR_SCAN and MAVEN sources

## Test Scenarios Covered

1. ✅ Single build tool (Maven only)
2. ✅ Single build tool (Gradle only)
3. ✅ Mixed sources (Maven + Eclipse classpath)
4. ✅ Version conflict detection (multiple Jakarta EE versions)
5. ✅ Single feature detection (CDI only)
6. ✅ Jakarta EE + MicroProfile combination
7. ✅ Multiple dependency sources in same project

## Adding New Test Projects

To add a new test project:

1. Create a new directory under `test-projects/`
2. Add appropriate build files (pom.xml, build.gradle, .classpath, etc.)
3. Document the project in this README
4. Add corresponding test case in `ClasspathAnalyzerTest.java`

## Test Scenarios Covered

### Build Systems
1. ✅ Maven projects (Java EE 5, 6, 7, 8, Jakarta EE 9, 9.1, 10)
2. ✅ Gradle projects (Jakarta EE 9.1, 10 and MicroProfile 4.0, 5.0)
3. ✅ Mixed sources (Maven + Eclipse classpath)

### Version Detection
4. ✅ Jakarta EE 9, 9.1, 10 detection
5. ✅ Java EE 5, 6, 7, 8 detection
6. ✅ MicroProfile 4.0, 5.0 detection
7. ✅ Version conflict detection (multiple Jakarta EE versions)
8. ✅ Version conflict detection (mixed Java EE 6-8 versions)
9. ✅ Single feature detection (CDI only)

### Deduplication & Priority
10. ✅ Duplicate dependency handling
11. ✅ Version conflict resolution
12. ✅ Parser priority (Maven > Gradle > Eclipse > JAR)
13. ✅ Completeness scoring for duplicate selection

### Multi-Source Testing
14. ✅ Multiple dependency sources in same project
15. ✅ Jakarta EE + MicroProfile combination
16. ✅ Multi-module projects with custom JARs
17. ✅ Transitive dependency extraction

### Advanced JAR Scenarios
18. ✅ Custom JAR with embedded pom.xml
19. ✅ Custom JAR with nested JAR files in lib/
20. ✅ Multi-level dependency extraction from JARs
21. ✅ Mixed JAR_SCAN and MAVEN sources

## Test Coverage

The test suite includes:
- **ClasspathAnalyzerTest**: Core analyzer functionality (13 tests)
- **DeduplicationTest**: Deduplication logic (7 tests)
- **ParserPriorityTest**: Parser ordering (4 tests)
- **ParserProviderSPITest**: SPI mechanism (4 tests)
- **VersionMappingRegistryTest**: Version detection (7 tests)
- **JarDependencyExtractorTest**: JAR extraction (7 tests)
- **ExtendedProjectTest**: New test projects (6 tests)
- **TransitiveDependencyTest**: Transitive dependencies (6 tests)
- **MultiModuleProjectTest**: Multi-module projects (8 tests)
- **ComprehensiveVersionTest**: New comprehensive version tests (5+ tests)

**Total: 67+ tests**

## Architectural Improvements Tested

1. **SPI Extension Point**: ParserProviderSPITest validates ServiceLoader mechanism
2. **Parser Priority System**: ParserPriorityTest ensures correct ordering
3. **Improved Deduplication**: DeduplicationTest validates groupId:artifactId keying and completeness scoring
4. **Consolidated Version Detection**: VersionMappingRegistryTest validates centralized version mapping
5. **Testable Architecture**: JarDependencyExtractorTest validates instance-based design

## Adding New Test Projects

To add a new test project:

1. Create a new directory under `test-projects/`
2. Add appropriate build files (pom.xml, build.gradle, .classpath, etc.)
3. Document the project in this README with:
   - Purpose
   - Dependencies
   - Expected results
4. Add corresponding test case in appropriate test class:
   - `ClasspathAnalyzerTest.java` for general functionality
   - `ExtendedProjectTest.java` for new scenarios
   - `DeduplicationTest.java` for deduplication testing
5. Run tests to verify: `mvn test`