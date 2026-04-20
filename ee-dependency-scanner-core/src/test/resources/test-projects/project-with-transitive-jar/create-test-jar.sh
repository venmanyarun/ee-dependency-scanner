#!/bin/bash
# Script to create a test JAR with embedded pom.xml containing Jakarta EE dependencies

# Create temporary directory structure
TEMP_DIR=$(mktemp -d)
JAR_NAME="custom-library-1.0.0.jar"
GROUP_ID="com.example"
ARTIFACT_ID="custom-library"
VERSION="1.0.0"

echo "Creating test JAR with embedded pom.xml..."

# Create META-INF/maven structure
mkdir -p "$TEMP_DIR/META-INF/maven/$GROUP_ID/$ARTIFACT_ID"

# Create pom.xml with Jakarta EE transitive dependencies
cat > "$TEMP_DIR/META-INF/maven/$GROUP_ID/$ARTIFACT_ID/pom.xml" << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>custom-library</artifactId>
    <version>1.0.0</version>

    <dependencies>
        <!-- Jakarta EE 10 - Servlet (transitive dependency) -->
        <dependency>
            <groupId>jakarta.servlet</groupId>
            <artifactId>jakarta.servlet-api</artifactId>
            <version>6.0.0</version>
        </dependency>

        <!-- Jakarta EE 10 - Persistence (transitive dependency) -->
        <dependency>
            <groupId>jakarta.persistence</groupId>
            <artifactId>jakarta.persistence-api</artifactId>
            <version>3.1.0</version>
        </dependency>

        <!-- MicroProfile Config (transitive dependency) -->
        <dependency>
            <groupId>org.eclipse.microprofile.config</groupId>
            <artifactId>microprofile-config-api</artifactId>
            <version>3.0</version>
        </dependency>
    </dependencies>
</project>
EOF

# Create pom.properties
cat > "$TEMP_DIR/META-INF/maven/$GROUP_ID/$ARTIFACT_ID/pom.properties" << EOF
groupId=$GROUP_ID
artifactId=$ARTIFACT_ID
version=$VERSION
EOF

# Create MANIFEST.MF
mkdir -p "$TEMP_DIR/META-INF"
cat > "$TEMP_DIR/META-INF/MANIFEST.MF" << EOF
Manifest-Version: 1.0
Implementation-Title: Custom Library
Implementation-Version: 1.0.0
Implementation-Vendor-Id: com.example
Built-By: test
Build-Jdk: 11.0.0
Created-By: Maven
EOF

# Create a dummy class file
mkdir -p "$TEMP_DIR/com/example"
cat > "$TEMP_DIR/com/example/CustomLibrary.class" << EOF
// Dummy class file
EOF

# Create the JAR
cd "$TEMP_DIR"
jar cfm "../lib/$JAR_NAME" META-INF/MANIFEST.MF .

echo "Created $JAR_NAME in lib/ directory"
echo "JAR contains embedded pom.xml with Jakarta EE 10 transitive dependencies"

# Cleanup
cd ..
rm -rf "$TEMP_DIR"
EOF

