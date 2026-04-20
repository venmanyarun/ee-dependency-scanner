# LSP4MP Integration with DependencyAnalysisHelper (Eclipse JDT)

This directory contains integration examples showing how to use the EE Dependency Scanner in the LSP4MP (MicroProfile Language Server) using Eclipse JDT APIs.

## Files

- `LSP4MPDependencyAnalyzer.java` - Direct integration class for LSP4MP using JDT
- `EEVersionProjectLabelProvider.java` - **Provider-based integration** using LSP4MP's `IProjectLabelProvider` interface

## Overview

LSP4MP is a Language Server Protocol (LSP) implementation for Eclipse MicroProfile that runs in Eclipse and has access to Eclipse JDT APIs. This directory provides two integration approaches:

1. **Direct Integration** (`LSP4MPDependencyAnalyzer`) - For custom analysis logic
2. **Provider-based Integration** (`EEVersionProjectLabelProvider`) - **Recommended** - Follows LSP4MP's architecture

## Recommended Approach: Provider-based Integration

The provider-based approach is recommended because it:
- Follows LSP4MP's existing architecture
- Integrates seamlessly with LSP4MP's label system
- Automatically provides version information to all diagnostics and code actions
- Requires minimal code changes

### How It Works

The `EEVersionProjectLabelProvider` implements LSP4MP's `IProjectLabelProvider` interface and adds version-specific labels to projects:

```java
public class EEVersionProjectLabelProvider implements IProjectLabelProvider {
    
    @Override
    public List<String> getProjectLabels(IJavaProject project) throws JavaModelException {
        // Analyze dependencies and return labels like:
        // - "jakartaee", "jakartaee-9", "jakartaee-10"
        // - "microprofile", "microprofile-4.0", "microprofile-5.0"
    }
}
```

### Registration

**Option 1: Via Extension Point (plugin.xml)**

```xml
<extension point="org.eclipse.lsp4mp.jdt.core.projectLabelProviders">
  <provider class="org.eclipse.lsp4mp.jdt.internal.core.providers.EEVersionProjectLabelProvider"/>
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
        
        // Check for Jakarta EE 9+
        if (labels.contains("jakartaee-9") || labels.contains("jakartaee-10")) {
            // Apply Jakarta EE 9+ diagnostics (jakarta.* namespace)
            diagnostics.add(checkJakartaNamespace(context));
        } else if (labels.contains("jakartaee")) {
            // Apply Jakarta EE 8 or Java EE diagnostics (javax.* namespace)
            diagnostics.add(checkJavaxNamespace(context));
        }
        
        // Check for MicroProfile 4.0+
        if (labels.stream().anyMatch(l -> l.startsWith("microprofile-4") || 
                                           l.startsWith("microprofile-5") ||
                                           l.startsWith("microprofile-6"))) {
            // MicroProfile 4.0+ uses Jakarta EE 9+ (jakarta.* namespace)
            diagnostics.add(checkMicroProfileJakartaNamespace(context));
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
                          l.startsWith("microprofile-4") ||
                          l.startsWith("microprofile-5"))
            ? "jakarta" : "javax";
        
        // Provide namespace-aware code actions
        actions.add(createInjectCodeAction(namespace));
        
        return actions;
    }
}
```

## Labels Provided

The provider adds the following labels to projects:

### Jakarta EE Labels
- `jakartaee` - Generic label for any Jakarta EE project
- `jakartaee-8` - Jakarta EE 8 (javax.* namespace)
- `jakartaee-9` - Jakarta EE 9 (jakarta.* namespace)
- `jakartaee-9.1` - Jakarta EE 9.1
- `jakartaee-10` - Jakarta EE 10
- `jakartaee-11` - Jakarta EE 11

### MicroProfile Labels
- `microprofile` - Generic label for any MicroProfile project
- `microprofile-1.0` through `microprofile-3.x` - Uses javax.* namespace
- `microprofile-4.0` through `microprofile-6.x` - Uses jakarta.* namespace

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

If you need more control or custom analysis logic, use the direct integration approach with `LSP4MPDependencyAnalyzer`:

```java
private LSP4MPDependencyAnalyzer dependencyAnalyzer;
private Map<String, ClasspathAnalysisResult> projectAnalysisCache;

public void initialize(InitializeParams params) {
    dependencyAnalyzer = new LSP4MPDependencyAnalyzer();
    projectAnalysisCache = new ConcurrentHashMap<>();
}
```

See the full `LSP4MPDependencyAnalyzer.java` file for complete implementation details.

## Key Features

### Provider-based Integration
✅ **Seamless Integration**: Works with existing LSP4MP architecture  
✅ **Automatic Label Propagation**: Labels available in all diagnostics/code actions  
✅ **Minimal Code Changes**: Just register the provider  
✅ **Caching**: Built-in caching for performance  
✅ **Version Detection**: Automatic Jakarta EE and MicroProfile version detection  

### Direct Integration
✅ **Full Control**: Complete control over analysis logic  
✅ **Custom Caching**: Implement your own caching strategy  
✅ **Flexible Usage**: Use anywhere in your code  

## MicroProfile Version and Namespace

### MicroProfile Version History

- **MicroProfile 1.x - 3.x**: Uses Java EE (javax.* namespace)
- **MicroProfile 4.0+**: Uses Jakarta EE 9+ (jakarta.* namespace)

### Handling Namespace Differences

```java
// Using labels
List<String> labels = context.getProjectLabels();

if (labels.stream().anyMatch(l -> l.startsWith("microprofile-4") || 
                                   l.startsWith("microprofile-5") ||
                                   l.startsWith("microprofile-6"))) {
    // MicroProfile 4.0+ - use jakarta.* namespace
    String injectAnnotation = "jakarta.inject.Inject";
} else if (labels.contains("microprofile")) {
    // MicroProfile 1.x-3.x - use javax.* namespace
    String injectAnnotation = "javax.inject.Inject";
}
```

## Common MicroProfile APIs

### Config API

```java
if (labels.contains("microprofile")) {
    // Provide Config-specific diagnostics and code actions
    // @ConfigProperty, ConfigSource, etc.
}
```

### REST Client API

```java
if (labels.contains("microprofile")) {
    // Provide REST Client-specific diagnostics and code actions
    // @RegisterRestClient, RestClientBuilder, etc.
}
```

### Health API

```java
if (labels.contains("microprofile")) {
    // Provide Health-specific diagnostics and code actions
    // @Health, @Liveness, @Readiness, etc.
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
    assertTrue(labels.contains("microprofile"));
    assertTrue(labels.contains("microprofile-5.0"));
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
- [LSP4Jakarta Integration](../lsp4jakarta-adapter-impl/) - Similar approach for Jakarta EE
- [Eclipse JDT Documentation](https://www.eclipse.org/jdt/)
- [MicroProfile Documentation](https://microprofile.io/)
- [LSP4MP GitHub](https://github.com/eclipse/lsp4mp)