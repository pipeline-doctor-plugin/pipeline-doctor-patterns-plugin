package io.jenkins.plugins.pipelinedoctor.patterns;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for PatternLoader functionality.
 */
public class PatternLoaderTest {

    @Test
    public void testLoadValidPattern() throws IOException {
        String yamlContent = 
            "patterns:\n" +
            "  - id: test-pattern\n" +
            "    category: test\n" +
            "    name: Test Pattern\n" +
            "    description: A test pattern\n" +
            "    severity: HIGH\n" +
            "    tags: [test, example]\n" +
            "    matchers:\n" +
            "      - regex: 'Error: (.+)'\n" +
            "        confidence: 90\n" +
            "        captures: [error_msg]\n" +
            "    solutions:\n" +
            "      - id: fix-test\n" +
            "        title: Fix Test Issue\n" +
            "        priority: 100\n" +
            "        steps:\n" +
            "          - Check the error message\n" +
            "          - Apply the fix\n" +
            "        examples:\n" +
            "          command: 'fix-command'";

        InputStream inputStream = new ByteArrayInputStream(yamlContent.getBytes());
        PatternLoader loader = new PatternLoader();
        List<Pattern> patterns = loader.loadPatternsFromStream(inputStream);

        assertEquals("Should load one pattern", 1, patterns.size());
        
        Pattern pattern = patterns.get(0);
        assertEquals("Should have correct ID", "test-pattern", pattern.getId());
        assertEquals("Should have correct category", "test", pattern.getCategory());
        assertEquals("Should have correct name", "Test Pattern", pattern.getName());
        assertEquals("Should have correct description", "A test pattern", pattern.getDescription());
        assertEquals("Should have correct severity", "HIGH", pattern.getSeverity());
        
        assertNotNull("Should have tags", pattern.getTags());
        assertEquals("Should have 2 tags", 2, pattern.getTags().size());
        assertTrue("Should contain test tag", pattern.getTags().contains("test"));
        assertTrue("Should contain example tag", pattern.getTags().contains("example"));
        
        assertNotNull("Should have matchers", pattern.getMatchers());
        assertEquals("Should have 1 matcher", 1, pattern.getMatchers().size());
        
        PatternMatcher matcher = pattern.getMatchers().get(0);
        assertEquals("Should have correct regex", "Error: (.+)", matcher.getRegex());
        assertEquals("Should have correct confidence", 90, matcher.getConfidence());
        assertNotNull("Should have captures", matcher.getCaptures());
        assertEquals("Should have 1 capture", 1, matcher.getCaptures().size());
        assertEquals("Should have correct capture name", "error_msg", matcher.getCaptures().get(0));
        
        assertNotNull("Should have solutions", pattern.getSolutions());
        assertEquals("Should have 1 solution", 1, pattern.getSolutions().size());
        
        PatternSolution solution = pattern.getSolutions().get(0);
        assertEquals("Should have correct solution ID", "fix-test", solution.getId());
        assertEquals("Should have correct title", "Fix Test Issue", solution.getTitle());
        assertEquals("Should have correct priority", 100, solution.getPriority());
        
        assertNotNull("Should have steps", solution.getSteps());
        assertEquals("Should have 2 steps", 2, solution.getSteps().size());
        assertEquals("Should have correct first step", "Check the error message", solution.getSteps().get(0));
        
        assertNotNull("Should have examples", solution.getExamples());
        assertEquals("Should have correct example", "fix-command", solution.getExamples().get("command"));
    }

    @Test
    public void testLoadMultiplePatterns() throws IOException {
        String yamlContent = 
            "patterns:\n" +
            "  - id: pattern1\n" +
            "    category: test1\n" +
            "    name: Pattern 1\n" +
            "    description: First pattern\n" +
            "    severity: HIGH\n" +
            "    matchers:\n" +
            "      - regex: 'Error1'\n" +
            "        confidence: 80\n" +
            "    solutions: []\n" +
            "  - id: pattern2\n" +
            "    category: test2\n" +
            "    name: Pattern 2\n" +
            "    description: Second pattern\n" +
            "    severity: MEDIUM\n" +
            "    matchers:\n" +
            "      - regex: 'Error2'\n" +
            "        confidence: 85\n" +
            "    solutions: []";

        InputStream inputStream = new ByteArrayInputStream(yamlContent.getBytes());
        PatternLoader loader = new PatternLoader();
        List<Pattern> patterns = loader.loadPatternsFromStream(inputStream);

        assertEquals("Should load two patterns", 2, patterns.size());
        
        Pattern pattern1 = patterns.get(0);
        assertEquals("Should have correct first ID", "pattern1", pattern1.getId());
        assertEquals("Should have correct first category", "test1", pattern1.getCategory());
        
        Pattern pattern2 = patterns.get(1);
        assertEquals("Should have correct second ID", "pattern2", pattern2.getId());
        assertEquals("Should have correct second category", "test2", pattern2.getCategory());
    }

    @Test
    public void testLoadPatternWithMultilineMatch() throws IOException {
        String yamlContent = 
            "patterns:\n" +
            "  - id: multiline-pattern\n" +
            "    category: test\n" +
            "    name: Multiline Pattern\n" +
            "    description: Pattern with multiline matching\n" +
            "    severity: CRITICAL\n" +
            "    matchers:\n" +
            "      - regex: 'Error.*\\n.*details: (.+)'\n" +
            "        confidence: 95\n" +
            "        multiline: true\n" +
            "        captures: [details]\n" +
            "    solutions: []";

        InputStream inputStream = new ByteArrayInputStream(yamlContent.getBytes());
        PatternLoader loader = new PatternLoader();
        List<Pattern> patterns = loader.loadPatternsFromStream(inputStream);

        assertEquals("Should load one pattern", 1, patterns.size());
        
        Pattern pattern = patterns.get(0);
        PatternMatcher matcher = pattern.getMatchers().get(0);
        assertTrue("Should be multiline", matcher.isMultiline());
        assertEquals("Should have correct regex", "Error.*\\n.*details: (.+)", matcher.getRegex());
    }

    @Test
    public void testLoadInvalidYaml() throws IOException {
        String yamlContent = "invalid: yaml: content: [unclosed";

        InputStream inputStream = new ByteArrayInputStream(yamlContent.getBytes());
        PatternLoader loader = new PatternLoader();

        try {
            List<Pattern> patterns = loader.loadPatternsFromStream(inputStream);
            fail("Should throw IOException for invalid YAML");
        } catch (IOException e) {
            assertTrue("Should have meaningful error message", 
                e.getMessage().contains("Failed to load patterns from YAML"));
        }
    }

    @Test
    public void testLoadEmptyPatterns() throws IOException {
        String yamlContent = "patterns: []";

        InputStream inputStream = new ByteArrayInputStream(yamlContent.getBytes());
        PatternLoader loader = new PatternLoader();
        List<Pattern> patterns = loader.loadPatternsFromStream(inputStream);

        assertNotNull("Should return non-null list", patterns);
        assertEquals("Should return empty list", 0, patterns.size());
    }

    @Test
    public void testLoadPatternMissingRequiredFields() throws IOException {
        String yamlContent = 
            "patterns:\n" +
            "  - category: test\n" +
            "    description: Missing ID and name\n" +
            "    severity: HIGH\n" +
            "    matchers: []\n" +
            "    solutions: []";

        InputStream inputStream = new ByteArrayInputStream(yamlContent.getBytes());
        PatternLoader loader = new PatternLoader();
        List<Pattern> patterns = loader.loadPatternsFromStream(inputStream);

        assertEquals("Should skip pattern with missing required fields", 0, patterns.size());
    }

    @Test
    public void testLoadBuiltinPatternsResource() throws IOException {
        PatternLoader loader = new PatternLoader();
        List<Pattern> patterns = loader.loadPatternsFromResource("patterns/builtin-patterns.yaml");

        assertNotNull("Should load patterns", patterns);
        assertTrue("Should load multiple patterns", patterns.size() > 0);
        
        // Check that we have some expected pattern categories
        boolean hasKubernetes = patterns.stream().anyMatch(p -> "kubernetes".equals(p.getCategory()));
        boolean hasDocker = patterns.stream().anyMatch(p -> "docker".equals(p.getCategory()));
        boolean hasBuildTool = patterns.stream().anyMatch(p -> "build-tool".equals(p.getCategory()));
        
        assertTrue("Should have kubernetes patterns", hasKubernetes);
        assertTrue("Should have docker patterns", hasDocker);
        assertTrue("Should have build-tool patterns", hasBuildTool);
        
        // Check that all patterns have required fields
        for (Pattern pattern : patterns) {
            assertNotNull("Pattern should have ID", pattern.getId());
            assertNotNull("Pattern should have name", pattern.getName());
            assertNotNull("Pattern should have category", pattern.getCategory());
            assertNotNull("Pattern should have matchers", pattern.getMatchers());
            assertFalse("Pattern should have at least one matcher", pattern.getMatchers().isEmpty());
        }
    }

    @Test
    public void testLoadNonExistentResource() {
        PatternLoader loader = new PatternLoader();
        
        try {
            loader.loadPatternsFromResource("non-existent-file.yaml");
            fail("Should throw IOException for non-existent resource");
        } catch (IOException e) {
            assertTrue("Should have meaningful error message", 
                e.getMessage().contains("Pattern resource not found"));
        }
    }
}