package io.jenkins.plugins.pipelinedoctor.patterns;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Tests for PatternMatcher functionality.
 */
public class PatternMatcherTest {

    @Test
    public void testBasicRegexMatch() {
        PatternMatcher matcher = new PatternMatcher();
        matcher.setRegex("Error: (.+)");
        matcher.setConfidence(85);
        matcher.setCaptures(Arrays.asList("error_message"));

        String testLog = "Build failed with error\nError: Connection timeout\nRetrying...";
        PatternMatchResult result = matcher.match(testLog);

        assertNotNull("Should find a match", result);
        assertTrue("Should be matched", result.isMatched());
        assertEquals("Should have correct confidence", 85, result.getConfidence());
        assertEquals("Should capture error message", "Connection timeout", result.getCapture("error_message"));
        assertEquals("Should have correct match text", "Error: Connection timeout", result.getMatchText());
    }

    @Test
    public void testNoMatch() {
        PatternMatcher matcher = new PatternMatcher();
        matcher.setRegex("Warning: (.+)");
        matcher.setConfidence(75);

        String testLog = "Build completed successfully\nNo issues found";
        PatternMatchResult result = matcher.match(testLog);

        assertNotNull("Should return result object", result);
        assertFalse("Should not be matched", result.isMatched());
    }

    @Test
    public void testMultilineMatch() {
        PatternMatcher matcher = new PatternMatcher();
        matcher.setRegex("Error.*\\n.*details: ([^\\n]+)");
        matcher.setConfidence(90);
        matcher.setMultiline(true);
        matcher.setCaptures(Arrays.asList("details"));

        String testLog = "Error occurred during build\nAdditional details: Docker daemon not running\nPlease check configuration";
        PatternMatchResult result = matcher.match(testLog);

        assertNotNull("Should find a match", result);
        assertTrue("Should be matched", result.isMatched());
        assertEquals("Should capture details", "Docker daemon not running", result.getCapture("details"));
    }

    @Test
    public void testMultipleCaptureGroups() {
        PatternMatcher matcher = new PatternMatcher();
        matcher.setRegex("Failed to pull image ([^:]+):([^\\s]+)");
        matcher.setConfidence(95);
        matcher.setCaptures(Arrays.asList("registry", "image"));

        String testLog = "Failed to pull image docker.io/library:nginx latest";
        PatternMatchResult result = matcher.match(testLog);

        assertNotNull("Should find a match", result);
        assertTrue("Should be matched", result.isMatched());
        assertEquals("Should capture registry", "docker.io/library", result.getCapture("registry"));
        assertEquals("Should capture image", "nginx", result.getCapture("image"));
    }

    @Test
    public void testKubernetesNetworkPolicyPattern() {
        PatternMatcher matcher = new PatternMatcher();
        matcher.setRegex("connection refused.*NetworkPolicy.*pod/(\\S+).*namespace/(\\S+)");
        matcher.setConfidence(95);
        matcher.setCaptures(Arrays.asList("pod_name", "namespace"));

        String testLog = "kubectl exec failed: connection refused by NetworkPolicy for pod/web-app-123 in namespace/production";
        PatternMatchResult result = matcher.match(testLog);

        assertNotNull("Should find a match", result);
        assertTrue("Should be matched", result.isMatched());
        assertEquals("Should capture pod name", "web-app-123", result.getCapture("pod_name"));
        assertEquals("Should capture namespace", "production", result.getCapture("namespace"));
    }

    @Test
    public void testDockerAuthPattern() {
        PatternMatcher matcher = new PatternMatcher();
        matcher.setRegex("unauthorized: authentication required.*?(?:for\\s+)?(?:https?://)?([^/\\s]+)/([^\\s]+)");
        matcher.setConfidence(95);
        matcher.setCaptures(Arrays.asList("registry_url", "image_name"));

        String testLog = "Error: unauthorized: authentication required for https://my-registry.com/my-app";
        PatternMatchResult result = matcher.match(testLog);

        assertNotNull("Should find a match", result);
        assertTrue("Should be matched", result.isMatched());
        assertEquals("Should capture registry URL", "my-registry.com", result.getCapture("registry_url"));
        assertEquals("Should capture image name", "my-app", result.getCapture("image_name"));
    }

    @Test
    public void testMavenDependencyPattern() {
        PatternMatcher matcher = new PatternMatcher();
        matcher.setRegex("Could not find artifact ([^:]+):([^:]+):(?:jar|pom):([^\\s]+)\\s+in\\s+(\\S+)");
        matcher.setConfidence(95);
        matcher.setCaptures(Arrays.asList("group_id", "artifact_id", "version", "repository"));

        String testLog = "Could not find artifact com.example:my-lib:jar:1.2.3 in central (https://repo1.maven.org/maven2)";
        PatternMatchResult result = matcher.match(testLog);

        assertNotNull("Should find a match", result);
        assertTrue("Should be matched", result.isMatched());
        assertEquals("Should capture group ID", "com.example", result.getCapture("group_id"));
        assertEquals("Should capture artifact ID", "my-lib", result.getCapture("artifact_id"));
        assertEquals("Should capture version", "1.2.3", result.getCapture("version"));
        assertEquals("Should capture repository", "central", result.getCapture("repository"));
    }

    @Test
    public void testPatternWithoutCaptures() {
        PatternMatcher matcher = new PatternMatcher();
        matcher.setRegex("java\\.lang\\.OutOfMemoryError: Java heap space");
        matcher.setConfidence(100);

        String testLog = "Exception in thread main java.lang.OutOfMemoryError: Java heap space at MyClass.main";
        PatternMatchResult result = matcher.match(testLog);

        assertNotNull("Should find a match", result);
        assertTrue("Should be matched", result.isMatched());
        assertEquals("Should have correct confidence", 100, result.getConfidence());
        assertNull("Should not have captures", result.getCapture("anything"));
    }

    @Test
    public void testInvalidRegex() {
        PatternMatcher matcher = new PatternMatcher();
        matcher.setRegex("[invalid regex pattern");
        matcher.setConfidence(50);

        String testLog = "Some log content";
        
        // Should handle invalid regex gracefully
        try {
            PatternMatchResult result = matcher.match(testLog);
            // If no exception, result should indicate no match
            if (result != null) {
                assertFalse("Should not match with invalid regex", result.isMatched());
            }
        } catch (Exception e) {
            // It's also acceptable to throw an exception for invalid regex
            assertTrue("Exception should be related to regex", 
                e.getMessage().contains("regex") || e instanceof java.util.regex.PatternSyntaxException);
        }
    }
}