package io.jenkins.plugins.pipelinedoctor.patterns;

import hudson.model.Run;
import io.jenkins.plugins.pipelinedoctor.BuildContext;
import io.jenkins.plugins.pipelinedoctor.BuildMetadata;
import io.jenkins.plugins.pipelinedoctor.DiagnosticResult;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Tests for PatternBasedDiagnosticProvider functionality.
 */
public class PatternBasedDiagnosticProviderTest {

    private PatternBasedDiagnosticProvider provider;
    private MockBuildContext buildContext;

    @Before
    public void setUp() {
        provider = new PatternBasedDiagnosticProvider();
        buildContext = new MockBuildContext();
    }

    @Test
    public void testProviderIdentification() {
        assertEquals("Should have correct provider ID", "pattern-matcher", provider.getProviderId());
        assertEquals("Should have correct provider name", "Pattern Matcher", provider.getProviderName());
        assertTrue("Should be enabled by default", provider.isEnabled(buildContext));
        assertEquals("Should have high priority", 200, provider.getPriority());
    }

    @Test
    public void testSupportedCategories() {
        Set<String> categories = provider.getSupportedCategories();
        assertNotNull("Should return categories", categories);
        assertTrue("Should support kubernetes category", categories.contains("kubernetes"));
        assertTrue("Should support docker category", categories.contains("docker"));
        assertTrue("Should support build-tool category", categories.contains("build-tool"));
    }

    @Test
    public void testKubernetesNetworkPolicyDetection() {
        String buildLog = 
            "Starting kubectl command...\n" +
            "Error: connection refused by NetworkPolicy for pod/web-app-123 in namespace/production\n" +
            "Command failed with exit code 1";

        buildContext.setBuildLog(buildLog);
        List<DiagnosticResult> results = provider.analyze(buildContext);

        assertNotNull("Should return results", results);
        assertTrue("Should find at least one result", results.size() > 0);

        DiagnosticResult networkPolicyResult = results.stream()
            .filter(r -> r.getMetadata().containsValue("k8s-network-policy-denied"))
            .findFirst()
            .orElse(null);

        assertNotNull("Should detect NetworkPolicy issue", networkPolicyResult);
        assertEquals("Should have correct category", "kubernetes", networkPolicyResult.getCategory());
        assertEquals("Should have critical severity", DiagnosticResult.Severity.CRITICAL, networkPolicyResult.getSeverity());
        assertNotNull("Should have solutions", networkPolicyResult.getSolutions());
        assertFalse("Should have at least one solution", networkPolicyResult.getSolutions().isEmpty());
    }

    @Test
    public void testDockerAuthenticationDetection() {
        String buildLog = 
            "Pulling image from registry...\n" +
            "Error: unauthorized: authentication required for https://my-registry.com/my-app\n" +
            "Docker pull failed";

        buildContext.setBuildLog(buildLog);
        List<DiagnosticResult> results = provider.analyze(buildContext);

        assertNotNull("Should return results", results);
        assertTrue("Should find at least one result", results.size() > 0);

        DiagnosticResult dockerAuthResult = results.stream()
            .filter(r -> r.getMetadata().containsValue("docker-registry-auth-failed"))
            .findFirst()
            .orElse(null);

        assertNotNull("Should detect Docker auth issue", dockerAuthResult);
        assertEquals("Should have correct category", "docker", dockerAuthResult.getCategory());
        assertEquals("Should have critical severity", DiagnosticResult.Severity.CRITICAL, dockerAuthResult.getSeverity());
    }

    @Test
    public void testMavenDependencyDetection() {
        String buildLog = 
            "[INFO] Building My Project 1.0.0\n" +
            "[ERROR] Could not find artifact com.example:my-lib:jar:1.2.3 in central (https://repo1.maven.org/maven2)\n" +
            "[ERROR] BUILD FAILURE";

        buildContext.setBuildLog(buildLog);
        List<DiagnosticResult> results = provider.analyze(buildContext);

        assertNotNull("Should return results", results);
        assertTrue("Should find at least one result", results.size() > 0);

        DiagnosticResult mavenResult = results.stream()
            .filter(r -> r.getMetadata().containsValue("maven-dependency-not-found"))
            .findFirst()
            .orElse(null);

        assertNotNull("Should detect Maven dependency issue", mavenResult);
        assertEquals("Should have correct category", "build-tool", mavenResult.getCategory());
        assertEquals("Should have high severity", DiagnosticResult.Severity.HIGH, mavenResult.getSeverity());
    }

    @Test
    public void testJavaOutOfMemoryDetection() {
        String buildLog = 
            "Running tests...\n" +
            "java.lang.OutOfMemoryError: Java heap space\n" +
            "    at com.example.MyClass.main(MyClass.java:42)\n" +
            "Test execution failed";

        buildContext.setBuildLog(buildLog);
        List<DiagnosticResult> results = provider.analyze(buildContext);

        assertNotNull("Should return results", results);
        assertTrue("Should find at least one result", results.size() > 0);

        DiagnosticResult oomResult = results.stream()
            .filter(r -> r.getMetadata().containsValue("out-of-memory-java"))
            .findFirst()
            .orElse(null);

        assertNotNull("Should detect Java OOM issue", oomResult);
        assertEquals("Should have correct category", "performance", oomResult.getCategory());
        assertEquals("Should have critical severity", DiagnosticResult.Severity.CRITICAL, oomResult.getSeverity());
    }

    @Test
    public void testEmptyBuildLog() {
        buildContext.setBuildLog("");
        List<DiagnosticResult> results = provider.analyze(buildContext);

        assertNotNull("Should return results", results);
        assertEquals("Should return empty results for empty log", 0, results.size());
    }

    @Test
    public void testNullBuildLog() {
        buildContext.setBuildLog(null);
        List<DiagnosticResult> results = provider.analyze(buildContext);

        assertNotNull("Should return results", results);
        assertEquals("Should return empty results for null log", 0, results.size());
    }

    @Test
    public void testNoMatchingPatterns() {
        String buildLog = 
            "Build started\n" +
            "All tests passed\n" +
            "Build completed successfully";

        buildContext.setBuildLog(buildLog);
        List<DiagnosticResult> results = provider.analyze(buildContext);

        assertNotNull("Should return results", results);
        assertEquals("Should return empty results for successful build", 0, results.size());
    }

    @Test
    public void testResultSorting() {
        String buildLog = 
            "Multiple issues in this build:\n" +
            "java.lang.OutOfMemoryError: Java heap space\n" +  // CRITICAL
            "npm ERR! ETIMEDOUT registry.npmjs.org\n" +          // HIGH
            "Tests run: 5, Failures: 1, Errors: 0, Skipped: 0\n"; // MEDIUM

        buildContext.setBuildLog(buildLog);
        List<DiagnosticResult> results = provider.analyze(buildContext);

        assertNotNull("Should return results", results);
        assertTrue("Should find multiple results", results.size() > 1);

        // Results should be sorted by severity (CRITICAL first)
        DiagnosticResult.Severity firstSeverity = results.get(0).getSeverity();
        for (int i = 1; i < results.size(); i++) {
            DiagnosticResult.Severity currentSeverity = results.get(i).getSeverity();
            assertTrue("Results should be sorted by severity", 
                getSeverityValue(firstSeverity) <= getSeverityValue(currentSeverity));
        }
    }

    @Test
    public void testMetadataCapture() {
        String buildLog = 
            "Error connecting to Kubernetes cluster\n" +
            "Error from server (Forbidden): User \"system:serviceaccount:default:jenkins\" cannot get resource \"pods\" in API group \"\" in the namespace \"production\"";

        buildContext.setBuildLog(buildLog);
        List<DiagnosticResult> results = provider.analyze(buildContext);

        assertNotNull("Should return results", results);
        assertTrue("Should find at least one result", results.size() > 0);

        DiagnosticResult rbacResult = results.stream()
            .filter(r -> r.getMetadata().containsValue("k8s-rbac-forbidden"))
            .findFirst()
            .orElse(null);

        if (rbacResult != null) {
            assertNotNull("Should have metadata", rbacResult.getMetadata());
            assertTrue("Should capture user info", 
                rbacResult.getMetadata().containsKey("capture_user") ||
                rbacResult.getMetadata().containsKey("capture_namespace"));
        }
    }

    private int getSeverityValue(DiagnosticResult.Severity severity) {
        switch (severity) {
            case CRITICAL: return 0;
            case HIGH: return 1;
            case MEDIUM: return 2;
            case LOW: return 3;
            default: return 2;
        }
    }

    /**
     * Mock BuildContext for testing.
     */
    private static class MockBuildContext implements BuildContext {
        private String buildLog;
        private Map<String, String> environment = new HashMap<>();

        public void setBuildLog(String buildLog) {
            this.buildLog = buildLog;
        }

        @Override
        public String getBuildLog() {
            return buildLog;
        }

        @Override
        public Map<String, String> getEnvironment() {
            return environment;
        }

        @Override
        public BuildMetadata getMetadata() {
            return new BuildMetadata(
                "test-job",
                1,
                "http://localhost:8080/job/test-job/1/",
                "master",
                System.currentTimeMillis(),
                "abc123",
                "main"
            );
        }

        @Override
        public boolean isPipelineBuild() {
            return true;
        }

        @Override
        public String getBuildResult() {
            return "FAILURE";
        }

        @Override
        public long getBuildDuration() {
            return 60000; // 1 minute
        }

        @Override
        public Run<?, ?> getRun() {
            return null; // Not needed for pattern-based tests
        }
    }
}