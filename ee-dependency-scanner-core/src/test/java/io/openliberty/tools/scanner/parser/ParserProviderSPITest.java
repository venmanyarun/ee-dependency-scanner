package io.openliberty.tools.scanner.parser;

import io.openliberty.tools.scanner.analyzer.ClasspathAnalyzer;
import io.openliberty.tools.scanner.api.DependencyInfo;
import io.openliberty.tools.scanner.api.ParserException;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DependencyParserProvider SPI mechanism.
 */
class ParserProviderSPITest {
    
    @Test
    void testDependencyParserProviderInterface() {
        // Create a test provider
        DependencyParserProvider testProvider = new DependencyParserProvider() {
            @Override
            public List<CoreDependencyParser<?>> getParsers() {
                return Arrays.asList(new TestCustomParser());
            }
            
            @Override
            public int getPriority() {
                return 50; // Higher priority than default parsers
            }
        };
        
        assertNotNull(testProvider.getParsers(), "Provider should return parsers");
        assertEquals(50, testProvider.getPriority(), "Provider should have priority 50");
        assertFalse(testProvider.getParsers().isEmpty(), "Provider should have at least one parser");
    }
    
    @Test
    void testCustomParserIntegration() {
        // Create analyzer with custom parser
        List<CoreDependencyParser<?>> customParsers = Arrays.asList(
            new MavenPomParser(),
            new TestCustomParser(),
            new JarManifestScanner()
        );
        
        ClasspathAnalyzer analyzer = ClasspathAnalyzer.builder().parsers(customParsers).build();
        assertNotNull(analyzer, "Analyzer should be created with custom parsers");
    }
    
    @Test
    void testParserProviderDefaultPriority() {
        DependencyParserProvider provider = new DependencyParserProvider() {
            @Override
            public List<CoreDependencyParser<?>> getParsers() {
                return Arrays.asList(new TestCustomParser());
            }
        };
        
        assertEquals(100, provider.getPriority(),
            "Default priority should be 100");
    }
    
    @Test
    void testMultipleProvidersWithDifferentPriorities() {
        DependencyParserProvider highPriority = new DependencyParserProvider() {
            @Override
            public List<CoreDependencyParser<?>> getParsers() {
                return Arrays.asList(new TestCustomParser());
            }
            
            @Override
            public int getPriority() {
                return 10;
            }
        };
        
        DependencyParserProvider lowPriority = new DependencyParserProvider() {
            @Override
            public List<CoreDependencyParser<?>> getParsers() {
                return Arrays.asList(new TestCustomParser());
            }
            
            @Override
            public int getPriority() {
                return 200;
            }
        };
        
        assertTrue(highPriority.getPriority() < lowPriority.getPriority(),
            "High priority provider should have lower numeric value");
    }
    
    /**
     * Test custom parser implementation for testing purposes.
     */
    private static class TestCustomParser implements CoreDependencyParser<File> {
        
        @Override
        public List<DependencyInfo> parse(File path) throws ParserException {
            return List.of();
        }
        
        @Override
        public String getName() {
            return "TestCustomParser";
        }
        
        @Override
        public boolean canParse(File path) {
            return false; // Never matches in tests
        }
        
        @Override
        public int getPriority() {
            return 150; // Custom priority
        }
    }
}
