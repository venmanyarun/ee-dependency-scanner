#!/bin/bash
# Script to create a custom JAR with embedded Jakarta EE dependencies inside lib/

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_NAME="custom-jakarta-lib-1.0.0.jar"
GROUP_ID="com.example.custom"
ARTIFACT_ID="custom-jakarta-lib"
VERSION="1.0.0"

echo "Creating custom JAR with embedded Jakarta EE dependencies..."

# Create temporary directory structure
TEMP_DIR=$(mktemp -d)
trap "rm -rf $TEMP_DIR" EXIT

# Create META-INF/maven structure for the main JAR
mkdir -p "$TEMP_DIR/META-INF/maven/$GROUP_ID/$ARTIFACT_ID"

# Create pom.xml with Jakarta EE dependencies
cat > "$TEMP_DIR/META-INF/maven/$GROUP_ID/$ARTIFACT_ID/pom.xml" << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example.custom</groupId>
    <artifactId>custom-jakarta-lib</artifactId>
    <version>1.0.0</version>

    <dependencies>
        <!-- Jakarta EE 10 - Servlet -->
        <dependency>
            <groupId>jakarta.servlet</groupId>
            <artifactId>jakarta.servlet-api</artifactId>
            <version>6.0.0</version>
        </dependency>

        <!-- Jakarta EE 10 - Persistence -->
        <dependency>
            <groupId>jakarta.persistence</groupId>
            <artifactId>jakarta.persistence-api</artifactId>
            <version>3.1.0</version>
        </dependency>

        <!-- Jakarta EE 10 - CDI -->
        <dependency>
            <groupId>jakarta.enterprise</groupId>
            <artifactId>jakarta.enterprise.cdi-api</artifactId>
            <version>4.0.1</version>
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
cat > "$TEMP_DIR/META-INF/MANIFEST.MF" << EOF
Manifest-Version: 1.0
Implementation-Title: Custom Jakarta Library
Implementation-Version: 1.0.0
Implementation-Vendor-Id: com.example.custom
Built-By: test-script
Build-Jdk: 11.0.0
Created-By: Maven
EOF

# Create lib directory inside the JAR to hold actual Jakarta EE JARs
mkdir -p "$TEMP_DIR/lib"

# Download actual Jakarta EE JARs to embed inside
echo "Downloading Jakarta EE dependencies to embed..."

# Download Jakarta Servlet API 6.0.0
if command -v curl &> /dev/null; then
    curl -s -L -o "$TEMP_DIR/lib/jakarta.servlet-api-6.0.0.jar" \
        "https://repo1.maven.org/maven2/jakarta/servlet/jakarta.servlet-api/6.0.0/jakarta.servlet-api-6.0.0.jar" || \
        echo "Warning: Could not download jakarta.servlet-api-6.0.0.jar"
    
    curl -s -L -o "$TEMP_DIR/lib/jakarta.persistence-api-3.1.0.jar" \
        "https://repo1.maven.org/maven2/jakarta/persistence/jakarta.persistence-api/3.1.0/jakarta.persistence-api-3.1.0.jar" || \
        echo "Warning: Could not download jakarta.persistence-api-3.1.0.jar"
    
    curl -s -L -o "$TEMP_DIR/lib/jakarta.enterprise.cdi-api-4.0.1.jar" \
        "https://repo1.maven.org/maven2/jakarta/enterprise/jakarta.enterprise.cdi-api/4.0.1/jakarta.enterprise.cdi-api-4.0.1.jar" || \
        echo "Warning: Could not download jakarta.enterprise.cdi-api-4.0.1.jar"
elif command -v wget &> /dev/null; then
    wget -q -O "$TEMP_DIR/lib/jakarta.servlet-api-6.0.0.jar" \
        "https://repo1.maven.org/maven2/jakarta/servlet/jakarta.servlet-api/6.0.0/jakarta.servlet-api-6.0.0.jar" || \
        echo "Warning: Could not download jakarta.servlet-api-6.0.0.jar"
    
    wget -q -O "$TEMP_DIR/lib/jakarta.persistence-api-3.1.0.jar" \
        "https://repo1.maven.org/maven2/jakarta/persistence/jakarta.persistence-api/3.1.0/jakarta.persistence-api-3.1.0.jar" || \
        echo "Warning: Could not download jakarta.persistence-api-3.1.0.jar"
    
    wget -q -O "$TEMP_DIR/lib/jakarta.enterprise.cdi-api-4.0.1.jar" \
        "https://repo1.maven.org/maven2/jakarta/enterprise/jakarta.enterprise.cdi-api/4.0.1/jakarta.enterprise.cdi-api-4.0.1.jar" || \
        echo "Warning: Could not download jakarta.enterprise.cdi-api-4.0.1.jar"
else
    echo "Warning: Neither curl nor wget found. Creating placeholder JARs..."
    # Create placeholder JARs if download tools not available
    echo "Placeholder" > "$TEMP_DIR/lib/jakarta.servlet-api-6.0.0.jar"
    echo "Placeholder" > "$TEMP_DIR/lib/jakarta.persistence-api-3.1.0.jar"
    echo "Placeholder" > "$TEMP_DIR/lib/jakarta.enterprise.cdi-api-4.0.1.jar"
fi

# Create a dummy class file
mkdir -p "$TEMP_DIR/com/example/custom"
cat > "$TEMP_DIR/com/example/custom/CustomLibrary.java" << 'EOF'
package com.example.custom;

public class CustomLibrary {
    public String getVersion() {
        return "1.0.0";
    }
}
EOF

# Compile the dummy class if javac is available
if command -v javac &> /dev/null; then
    javac -d "$TEMP_DIR" "$TEMP_DIR/com/example/custom/CustomLibrary.java" 2>/dev/null || true
    rm -f "$TEMP_DIR/com/example/custom/CustomLibrary.java"
fi

# Create the JAR
mkdir -p "$SCRIPT_DIR/lib"
cd "$TEMP_DIR"
jar cfm "$SCRIPT_DIR/lib/$JAR_NAME" META-INF/MANIFEST.MF .

echo ""
echo "✓ Created $JAR_NAME in lib/ directory"
echo "✓ JAR contains:"
echo "  - Embedded pom.xml with Jakarta EE 10 dependencies"
echo "  - lib/ directory with actual Jakarta EE JAR files:"
echo "    - jakarta.servlet-api-6.0.0.jar"
echo "    - jakarta.persistence-api-3.1.0.jar"
echo "    - jakarta.enterprise.cdi-api-4.0.1.jar"
echo ""
echo "This simulates a custom library that bundles Jakarta EE dependencies inside it."

# Made with Bob
