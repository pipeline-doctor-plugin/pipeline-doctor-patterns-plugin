# Pattern Testing Framework

## Overview

The pattern testing framework ensures pattern accuracy and helps maintain the quality of diagnostic patterns. It provides tools for:
- Unit testing individual patterns
- Integration testing with real build logs
- Performance benchmarking
- Accuracy measurement and reporting

## Test Structure

### Pattern Test Case Model

```java
public class PatternTestCase {
    private final String testId;
    private final String patternId;
    private final String description;
    private final String inputLog;
    private final List<ExpectedMatch> expectedMatches;
    private final List<String> shouldNotMatch;
    
    public static class ExpectedMatch {
        private final int confidence;
        private final Map<String, String> expectedVariables;
        private final String expectedSolutionId;
    }
}
```

### YAML Test Format

```yaml
test_cases:
  - id: test-k8s-network-policy-basic
    pattern_id: k8s-network-policy-denied
    description: Basic NetworkPolicy denial detection
    input_log: |
      2024-01-15 10:23:45 ERROR: connection refused due to NetworkPolicy pod/frontend-abc123 namespace/production
      2024-01-15 10:23:46 ERROR: Unable to connect to backend service
    expected_matches:
      - confidence: 95
        variables:
          pod_name: frontend-abc123
          namespace: production
        solution_id: add-network-policy-rule
    should_not_match:
      - "connection refused without NetworkPolicy"
      - "network timeout unrelated to policy"

  - id: test-docker-auth-variants
    pattern_id: docker-registry-auth-failed
    description: Various Docker authentication failure formats
    input_logs:
      - log: "unauthorized: authentication required for registry.example.com/myapp:latest"
        expected:
          registry_url: registry.example.com
          image_name: myapp:latest
      - log: "pull access denied for myapp, repository does not exist or may require 'docker login'"
        expected:
          full_image: myapp
```

## Test Implementation

### Pattern Test Runner

```java
@Component
public class PatternTestRunner {
    private final PatternMatchingEngine engine;
    private final PatternLoader patternLoader;
    private final TestCaseLoader testLoader;
    
    public TestReport runTests(String patternId) {
        DiagnosticPattern pattern = patternLoader.load(patternId);
        List<PatternTestCase> testCases = testLoader.loadTestsForPattern(patternId);
        
        TestReport report = new TestReport(patternId);
        
        for (PatternTestCase testCase : testCases) {
            TestResult result = runTestCase(pattern, testCase);
            report.addResult(result);
        }
        
        return report;
    }
    
    private TestResult runTestCase(DiagnosticPattern pattern, PatternTestCase testCase) {
        List<PatternMatch> matches = engine.matchPattern(pattern, testCase.getInputLog());
        
        TestResult result = new TestResult(testCase.getTestId());
        
        // Verify expected matches
        for (ExpectedMatch expected : testCase.getExpectedMatches()) {
            boolean found = matches.stream()
                .anyMatch(m -> matchesExpectation(m, expected));
            
            if (!found) {
                result.addFailure("Expected match not found: " + expected);
            }
        }
        
        // Verify no false positives
        for (String shouldNotMatch : testCase.getShouldNotMatch()) {
            List<PatternMatch> falsePositives = engine.matchPattern(pattern, shouldNotMatch);
            if (!falsePositives.isEmpty()) {
                result.addFailure("False positive: pattern matched '" + shouldNotMatch + "'");
            }
        }
        
        return result;
    }
}
```

### Performance Testing

```java
@Component
public class PatternPerformanceTester {
    private static final int WARMUP_ITERATIONS = 100;
    private static final int TEST_ITERATIONS = 1000;
    
    public PerformanceReport testPerformance(String patternId, String sampleLog) {
        DiagnosticPattern pattern = patternLoader.load(patternId);
        
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            engine.matchPattern(pattern, sampleLog);
        }
        
        // Test
        List<Long> timings = new ArrayList<>();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            long start = System.nanoTime();
            engine.matchPattern(pattern, sampleLog);
            long duration = System.nanoTime() - start;
            timings.add(duration);
        }
        
        return PerformanceReport.builder()
            .patternId(patternId)
            .averageTimeMs(calculateAverage(timings) / 1_000_000.0)
            .p95TimeMs(calculatePercentile(timings, 0.95) / 1_000_000.0)
            .p99TimeMs(calculatePercentile(timings, 0.99) / 1_000_000.0)
            .build();
    }
}
```

## Accuracy Measurement

### Feedback Collection

```java
@RestController
@Path("/pipeline-doctor/feedback")
public class PatternFeedbackController {
    private final PatternAccuracyService accuracyService;
    
    @POST
    @Path("/pattern/{patternId}/helpful")
    public void markHelpful(@PathParam("patternId") String patternId,
                           @RequestBody FeedbackRequest request) {
        accuracyService.recordFeedback(patternId, request.getMatchId(), true);
    }
    
    @POST
    @Path("/pattern/{patternId}/not-helpful")
    public void markNotHelpful(@PathParam("patternId") String patternId,
                              @RequestBody FeedbackRequest request) {
        accuracyService.recordFeedback(patternId, request.getMatchId(), false);
    }
}
```

### Accuracy Tracking

```java
@Component
public class PatternAccuracyService {
    private final PatternMetricsRepository repository;
    
    public void recordFeedback(String patternId, String matchId, boolean helpful) {
        PatternMetrics metrics = repository.findByPatternId(patternId)
            .orElse(new PatternMetrics(patternId));
        
        metrics.incrementMatches();
        if (helpful) {
            metrics.incrementHelpful();
        }
        
        repository.save(metrics);
    }
    
    public AccuracyReport generateReport() {
        List<PatternMetrics> allMetrics = repository.findAll();
        
        return AccuracyReport.builder()
            .overallAccuracy(calculateOverallAccuracy(allMetrics))
            .patternAccuracies(allMetrics.stream()
                .collect(Collectors.toMap(
                    PatternMetrics::getPatternId,
                    PatternMetrics::getAccuracy
                )))
            .lowPerformers(findLowPerformers(allMetrics))
            .build();
    }
    
    private List<String> findLowPerformers(List<PatternMetrics> metrics) {
        return metrics.stream()
            .filter(m -> m.getMatchCount() > 10 && m.getAccuracy() < 0.85)
            .map(PatternMetrics::getPatternId)
            .collect(Collectors.toList());
    }
}
```

## Continuous Improvement

### A/B Testing Framework

```java
@Component
public class PatternABTestManager {
    private final Map<String, ABTest> activeTests = new ConcurrentHashMap<>();
    
    public void createABTest(String patternId, DiagnosticPattern variantA, DiagnosticPattern variantB) {
        ABTest test = ABTest.builder()
            .patternId(patternId)
            .variantA(variantA)
            .variantB(variantB)
            .startTime(Instant.now())
            .build();
        
        activeTests.put(patternId, test);
    }
    
    public DiagnosticPattern selectVariant(String patternId) {
        ABTest test = activeTests.get(patternId);
        if (test == null) {
            return patternLoader.load(patternId);
        }
        
        // Use deterministic selection based on build ID for consistency
        boolean useVariantA = currentBuildId.hashCode() % 2 == 0;
        return useVariantA ? test.getVariantA() : test.getVariantB();
    }
    
    public ABTestReport concludeTest(String patternId) {
        ABTest test = activeTests.remove(patternId);
        
        double accuracyA = accuracyService.getAccuracy(patternId + "-A");
        double accuracyB = accuracyService.getAccuracy(patternId + "-B");
        
        return ABTestReport.builder()
            .winner(accuracyA > accuracyB ? "A" : "B")
            .accuracyA(accuracyA)
            .accuracyB(accuracyB)
            .statisticalSignificance(calculateSignificance(test))
            .build();
    }
}
```

## Test Automation

### CI Integration

```groovy
pipeline {
    agent any
    
    stages {
        stage('Pattern Tests') {
            steps {
                script {
                    // Run all pattern tests
                    sh 'mvn test -Dtest=Pattern*Test'
                    
                    // Run performance benchmarks
                    sh 'mvn test -Dtest=PatternPerformance*'
                    
                    // Generate accuracy report
                    def accuracy = sh(
                        script: 'mvn exec:java -Dexec.mainClass="PatternAccuracyReporter"',
                        returnStdout: true
                    ).trim()
                    
                    // Fail if accuracy drops below threshold
                    def overallAccuracy = readJSON(text: accuracy).overallAccuracy
                    if (overallAccuracy < 0.85) {
                        error "Pattern accuracy ${overallAccuracy} is below threshold 0.85"
                    }
                }
            }
        }
    }
    
    post {
        always {
            // Archive test reports
            archiveArtifacts artifacts: 'target/pattern-test-report.html'
            publishHTML([
                reportDir: 'target/pattern-reports',
                reportFiles: 'index.html',
                reportName: 'Pattern Test Report'
            ])
        }
    }
}
```

### Test Data Management

```java
@Component
public class TestDataManager {
    private final String TEST_DATA_DIR = "src/test/resources/pattern-tests/";
    
    public void addRealWorldExample(String patternId, String buildLog, boolean matched) {
        // Anonymize sensitive data
        String anonymized = anonymizer.process(buildLog);
        
        // Create test case from real example
        PatternTestCase testCase = PatternTestCase.builder()
            .testId(generateTestId())
            .patternId(patternId)
            .description("Real-world example from production")
            .inputLog(anonymized)
            .expectedMatches(matched ? extractMatches(patternId, buildLog) : List.of())
            .build();
        
        // Save to test data
        testCaseRepository.save(testCase);
    }
    
    private String anonymizer.process(String log) {
        // Replace sensitive data with placeholders
        return log
            .replaceAll("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b", "X.X.X.X") // IPs
            .replaceAll("([a-zA-Z0-9+._-]+)@([a-zA-Z0-9.-]+)", "user@example.com") // Emails
            .replaceAll("(password|token|key)\\s*[:=]\\s*\\S+", "$1=REDACTED"); // Secrets
    }
}
```

## Reporting

### Test Report Format

```html
<!DOCTYPE html>
<html>
<head>
    <title>Pattern Test Report</title>
</head>
<body>
    <h1>Pattern Test Report</h1>
    
    <h2>Summary</h2>
    <table>
        <tr>
            <td>Total Patterns:</td>
            <td>${totalPatterns}</td>
        </tr>
        <tr>
            <td>Tests Passed:</td>
            <td>${passedTests}</td>
        </tr>
        <tr>
            <td>Overall Accuracy:</td>
            <td>${overallAccuracy}%</td>
        </tr>
        <tr>
            <td>Average Match Time:</td>
            <td>${avgMatchTime}ms</td>
        </tr>
    </table>
    
    <h2>Pattern Details</h2>
    <table>
        <thead>
            <tr>
                <th>Pattern ID</th>
                <th>Test Coverage</th>
                <th>Accuracy</th>
                <th>Avg Time</th>
                <th>Status</th>
            </tr>
        </thead>
        <tbody>
            <!-- Pattern rows -->
        </tbody>
    </table>
    
    <h2>Failed Tests</h2>
    <!-- Details of failed tests -->
    
    <h2>Performance Warnings</h2>
    <!-- Patterns exceeding 10ms threshold -->
</body>
</html>
```