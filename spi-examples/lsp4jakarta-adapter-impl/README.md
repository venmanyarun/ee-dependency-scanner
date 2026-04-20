# LSP4Jakarta Integration with DependencyAnalysisHelper (Eclipse JDT)

This directory contains integration examples showing how to use the EE Dependency Scanner in the LSP4Jakarta language server using Eclipse JDT APIs.

## Files

- `LSP4JakartaDependencyAnalyzer.java` - Direct integration class for LSP4Jakarta using JDT
- `EEVersionProjectLabelProvider.java` - **Provider-based integration** using LSP4Jakarta's `IProjectLabelProvider` interface (RECOMMENDED)

## Overview

LSP4Jakarta is a Language Server Protocol (LSP) implementation for Jakarta EE that runs in Eclipse and has access to Eclipse JDT APIs. This directory provides two integration approaches:

1. **Direct Integration** (`LSP4JakartaDependencyAnalyzer`) - For custom analysis logic
2. **Provider-based Integration** (`EEVersionProjectLabelProvider`) - **Recommended** - Follows LSP4Jakarta's architecture

## Recommended Approach: Provider-based Integration

The provider-based approach is recommended because it:
- Follows LSP4Jakarta's existing architecture (same as LSP4MP)
- Integrates seamlessly with LSP4Jakarta's label system
- Automatically provides version information to all diagnostics and code actions
- Requires minimal code changes

### How It Works

The `EEVersionProjectLabelProvider` implements LSP4Jakarta's `IProjectLabelProvider` interface and adds version-specific labels to projects:

```java
public class EEVersionProjectLabelProvider implements IProjectLabelProvider {
    
    @Override
    public List<String> getProjectLabels(IJavaProject project) throws JavaModelException {
        // Analyze dependencies and return labels like:
        // - "jakartaee", "jakartaee-9", "jakartaee-10"
        // - "javaee", "javaee-6", "javaee-7", "javaee-8"
    }
}
```

### Registration

**Option 1: Via Extension Point (plugin.xml)**

```xml
<extension point="org.eclipse.lsp4jakarta.jdt.core.projectLabelProviders">
  <provider class="org.eclipse.lsp4jakarta.jdt.internal.core.providers.EEVersionProjectLabelProvider"/>
</extension>
```

**Option 2: Programmatic Registration**

```java
// In your plugin activator or initialization code
ProjectLabelManager.getInstance().registerProvider(new EEVersionProjectLabelProvider());
```

### Usage in Diagnostics

Once registered, the labels are automatically available in all diagnostics participants:

```java
public class MyDiagnosticsParticipant implements IJavaDiagnosticsParticipant {
    
    @Override
    public List<Diagnostic> collectDiagnostics(JavaDiagnosticsContext context) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        
        // Get project labels (automatically includes EE version labels)
        List<String> labels = context.getProjectLabels();
        
        // Check for Jakarta EE 9+ (jakarta.* namespace)
        if (labels.contains("jakartaee-9") || labels.contains("jakartaee-10")) {
            // Apply Jakarta EE 9+ diagnostics
            diagnostics.add(checkJakartaNamespace(context));
        } 
        // Check for Java EE or Jakarta EE 8 (javax.* namespace)
        else if (labels.stream().anyMatch(l -> l.startsWith("javaee-") || l.equals("jakartaee-8"))) {
            // Apply Java EE / Jakarta EE 8 diagnostics
            diagnostics.add(checkJavaxNamespace(context));
        }
        
        return diagnostics;
    }
}
```

### Usage in Code Actions

```java
public class MyCodeActionParticipant implements IJavaCodeActionParticipant {
    
    @Override
    public List<CodeAction> getCodeActions(JavaCodeActionContext context) {
        List<CodeAction> actions = new ArrayList<>();
        
        // Get project labels
        List<String> labels = context.getProjectLabels();
        
        // Determine correct namespace
        String namespace = labels.stream()
            .anyMatch(l -> l.startsWith("jakartaee-9") || 
                          l.startsWith("jakartaee-10") ||
                          l.startsWith("jakartaee-11"))
            ? "jakarta" : "javax";
        
        // Provide namespace-aware code actions
        actions.add(createInjectCodeAction(namespace));
        actions.add(createServletCodeAction(namespace));
        
        return actions;
    }
}
```

## Labels Provided

The provider adds the following labels to projects:

### Jakarta EE Labels (jakarta.* namespace)
- `jakartaee` - Generic label for Jakarta EE 9+ projects
- `jakartaee-9` - Jakarta EE 9
- `jakartaee-9.1` - Jakarta EE 9.1
- `jakartaee-10` - Jakarta EE 10
- `jakartaee-11` - Jakarta EE 11

### Java EE Labels (javax.* namespace)
- `javaee` - Generic label for Java EE projects
- `javaee-5` - Java EE 5
- `javaee-6` - Java EE 6
- `javaee-7` - Java EE 7
- `javaee-8` - Java EE 8 / Jakarta EE 8

## Three-Tier Dependency Collection

The provider uses a smart three-tier approach:

### 1. M2E (Maven) - Priority 1
```java
private boolean collectMavenDependencies(IProject project, List<DependencyInfo> dependencies) {
    // Uses M2E APIs to get Maven project model
    // Best for Maven projects
}
```

### 2. Buildship (Gradle) - Priority 2
```java
private boolean collectGradleDependencies(IProject project, List<DependencyInfo> dependencies) {
    // Uses Buildship APIs to get Gradle project model
    // Best for Gradle projects
}
```

### 3. JDT Classpath (Fallback) - Priority 3
```java
private void collectJDTClasspathDependencies(IJavaProject javaProject, List<DependencyInfo> dependencies) {
    // Extracts dependencies from JAR files in classpath
    // Works for any project type
}
```

## Cache Management

The provider caches analysis results for performance. Clear the cache when dependencies change:

```java
public class DependencyChangeListener implements IResourceChangeListener {
    
    private EEVersionProjectLabelProvider provider;
    
    @Override
    public void resourceChanged(IResourceChangeEvent event) {
        IResourceDelta delta = event.getDelta();
        
        // Check if pom.xml or build.gradle changed
        if (isPomOrGradleChange(delta)) {
            String projectName = getProjectName(delta);
            provider.clearCache(projectName);
            
            // Trigger re-validation
            triggerRevalidation(projectName);
        }
    }
}
```

## Alternative: Direct Integration

If you need more control or custom analysis logic, use the direct integration approach with `LSP4JakartaDependencyAnalyzer`:

```java
private LSP4JakartaDependencyAnalyzer dependencyAnalyzer;
private Map<String, ClasspathAnalysisResult> projectAnalysisCache;

public void initialize(InitializeParams params) {
    dependencyAnalyzer = new LSP4JakartaDependencyAnalyzer();
    projectAnalysisCache = new ConcurrentHashMap<>();
}
```

See the full `LSP4JakartaDependencyAnalyzer.java` file for complete implementation details.

## Key Features

### Provider-based Integration
✅ **Seamless Integration**: Works with existing LSP4Jakarta architecture  
✅ **Automatic Label Propagation**: Labels available in all diagnostics/code actions  
✅ **Minimal Code Changes**: Just register the provider  
✅ **Caching**: Built-in caching for performance  
✅ **Version Detection**: Automatic Jakarta EE and Java EE version detection  
✅ **Namespace Awareness**: Handles javax.* vs jakarta.* transitions  

### Direct Integration
✅ **Full Control**: Complete control over analysis logic  
✅ **Custom Caching**: Implement your own caching strategy  
✅ **Flexible Usage**: Use anywhere in your code  

## Namespace Handling

### Jakarta EE 9+ (jakarta.* namespace)
- Jakarta EE 9, 9.1, 10, 11
- Uses `jakarta.*` package namespace
- Example: `jakarta.servlet.http.HttpServlet`

### Java EE / Jakarta EE 8 (javax.* namespace)
- Java EE 5, 6, 7, 8
- Jakarta EE 8
- Uses `javax.*` package namespace
- Example: `javax.servlet.http.HttpServlet`

### Handling in Code

```java
// Using labels
List<String> labels = context.getProjectLabels();

if (labels.stream().anyMatch(l -> l.startsWith("jakartaee-9") || 
                                   l.startsWith("jakartaee-10") ||
                                   l.startsWith("jakartaee-11"))) {
    // Jakarta EE 9+ - use jakarta.* namespace
    String servletClass = "jakarta.servlet.http.HttpServlet";
} else if (labels.stream().anyMatch(l -> l.startsWith("javaee-") || 
                                         l.equals("jakartaee-8"))) {
    // Java EE / Jakarta EE 8 - use javax.* namespace
    String servletClass = "javax.servlet.http.HttpServlet";
}
```

## Testing

```java
@Test
public void testEEVersionProjectLabelProvider() {
    // Create provider
    EEVersionProjectLabelProvider provider = new EEVersionProjectLabelProvider();
    
    // Create test project
    IJavaProject project = createTestProject();
    
    // Get labels
    List<String> labels = provider.getProjectLabels(project);
    
    // Verify
    assertTrue(labels.contains("jakartaee"));
    assertTrue(labels.contains("jakartaee-10"));
}
```

## Dependencies

Add to your plugin's `pom.xml` or `MANIFEST.MF`:

```xml
<dependency>
    <groupId>io.openliberty.tools</groupId>
    <artifactId>ee-dependency-scanner-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Comparison: Provider vs Direct Integration

| Feature | Provider-based | Direct Integration |
|---------|---------------|-------------------|
| Integration Effort | Low | Medium |
| Code Changes | Minimal | More extensive |
| Architecture Fit | Perfect | Custom |
| Label Propagation | Automatic | Manual |
| Caching | Built-in | Custom |
| Flexibility | Standard | High |
| **Recommendation** | **✅ Use this** | Use for special cases |

## See Also

- [DependencyAnalysisHelper API](../../ee-dependency-scanner-core/src/main/java/io/openliberty/tools/scanner/util/DependencyAnalysisHelper.java)
- [Eclipse Adapter Implementation](../scanner-eclipse-adapter-impl/) - Similar pattern used here
- [LSP4MP Integration](../lsp4mp-adapter-impl/) - Similar approach for MicroProfile
- [Eclipse JDT Documentation](https://www.eclipse.org/jdt/)
- [Jakarta EE](https://jakarta.ee/)
- [LSP4Jakarta GitHub](https://github.com/eclipse/lsp4jakarta)