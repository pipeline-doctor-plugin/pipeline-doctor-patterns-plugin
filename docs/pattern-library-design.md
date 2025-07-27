# Pattern Library Design

## Overview

The pattern library is the core of the Pipeline Doctor plugin, providing instant detection of common build failures. Each pattern includes:
- Regex patterns for log matching
- Categorization and severity levels
- Associated solutions with variables
- Confidence scoring

## Pattern Structure

### Pattern Model

```java
public class DiagnosticPattern {
    private final String id;
    private final String category;
    private final String name;
    private final String description;
    private final Severity severity;
    private final List<PatternMatcher> matchers;
    private final List<SolutionTemplate> solutions;
    private final Map<String, String> metadata;
    
    public enum Severity {
        CRITICAL,   // Build fails, no workaround
        HIGH,       // Build fails, manual workaround exists
        MEDIUM,     // Build succeeds but with issues
        LOW         // Performance or optimization suggestions
    }
}

public class PatternMatcher {
    private final Pattern regex;
    private final int confidence;  // 0-100
    private final List<String> captureGroups;
    private final boolean multiline;
    
    /**
     * Extract variables from matched groups
     */
    public Map<String, String> extractVariables(Matcher matcher) {
        Map<String, String> vars = new HashMap<>();
        for (int i = 0; i < captureGroups.size(); i++) {
            vars.put(captureGroups.get(i), matcher.group(i + 1));
        }
        return vars;
    }
}

public class SolutionTemplate {
    private final String id;
    private final String title;
    private final String description;
    private final List<String> steps;
    private final Map<String, String> examples;
    private final int priority;  // Higher = show first
    
    /**
     * Render solution with extracted variables
     */
    public RenderedSolution render(Map<String, String> variables) {
        // Replace ${var} placeholders with actual values
    }
}
```

## Pattern Categories

### 1. Network & Connectivity Issues

#### NetworkPolicy Kubernetes Issues
```yaml
id: k8s-network-policy-denied
category: kubernetes
name: NetworkPolicy Connection Denied
severity: CRITICAL
patterns:
  - regex: 'connection refused.*NetworkPolicy.*pod/(\S+).*namespace/(\S+)'
    confidence: 95
    captures: [pod_name, namespace]
  - regex: 'NetworkPolicies\.networking\.k8s\.io.*denied.*from (\S+) to (\S+)'
    confidence: 90
    captures: [source, destination]
solutions:
  - title: Add NetworkPolicy Rule
    priority: 100
    steps:
      - Check existing NetworkPolicy for namespace ${namespace}
      - Add ingress rule for pod ${pod_name}
      - "Example: kubectl get networkpolicy -n ${namespace}"
    examples:
      rule: |
        - from:
          - podSelector:
              matchLabels:
                app: ${source}
```

#### Docker Registry Authentication
```yaml
id: docker-registry-auth-failed
category: docker
name: Docker Registry Authentication Failed
severity: CRITICAL
patterns:
  - regex: 'unauthorized: authentication required.*registry[:/]([^/\s]+)'
    confidence: 95
    captures: [registry_url]
  - regex: 'pull access denied.*repository[:/]([^,\s]+)'
    confidence: 90
    captures: [image_name]
solutions:
  - title: Configure Docker Credentials
    priority: 100
    steps:
      - Create Docker registry credentials in Jenkins
      - Add credentials to pipeline
      - "docker login ${registry_url}"
```

### 2. Package Repository Issues

#### Debian Archive Moved
```yaml
id: debian-archive-moved
category: package-manager
name: Debian Package Archive Moved
severity: HIGH
patterns:
  - regex: 'Failed to fetch.*debian.*404.*Not Found'
    confidence: 85
  - regex: 'The following signatures were invalid.*EXPKEYSIG'
    confidence: 90
solutions:
  - title: Update to Archive Repository
    priority: 100
    steps:
      - Replace main repository with archive
      - "sed -i 's|deb.debian.org|archive.debian.org|g' /etc/apt/sources.list"
      - Update package lists
```

#### NPM Registry Timeout
```yaml
id: npm-registry-timeout
category: package-manager
name: NPM Registry Timeout
severity: HIGH
patterns:
  - regex: 'npm ERR!.*ETIMEDOUT.*registry\.npmjs\.org'
    confidence: 95
  - regex: 'npm ERR!.*network.*timeout.*(\d+)ms'
    confidence: 85
    captures: [timeout_ms]
solutions:
  - title: Increase NPM Timeout
    priority: 90
    steps:
      - "npm config set fetch-timeout ${timeout_ms * 2}"
      - Consider using local npm proxy
  - title: Use Alternative Registry
    priority: 80
    steps:
      - "npm config set registry https://registry.npmmirror.com"
```

### 3. Build Tool Issues

#### Maven Dependency Resolution
```yaml
id: maven-dependency-not-found
category: build-tool
name: Maven Dependency Not Found
severity: HIGH
patterns:
  - regex: 'Could not find artifact ([^:]+):([^:]+):([^:]+):([^\s]+)'
    confidence: 95
    captures: [group_id, artifact_id, packaging, version]
  - regex: 'Failed to collect dependencies.*([^:]+:[^:]+:[^:]+)'
    confidence: 85
    captures: [dependency]
solutions:
  - title: Check Repository Configuration
    priority: 100
    steps:
      - Verify repository contains ${group_id}:${artifact_id}:${version}
      - Check settings.xml for repository configuration
      - Try with -U flag to update snapshots
```

#### Gradle Build Cache Issues
```yaml
id: gradle-cache-corrupted
category: build-tool
name: Gradle Cache Corrupted
severity: MEDIUM
patterns:
  - regex: 'Timeout waiting to lock.*cache.*\.gradle/caches/(\S+)'
    confidence: 90
    captures: [cache_version]
  - regex: 'Could not resolve all files.*configuration.*cache'
    confidence: 80
solutions:
  - title: Clear Gradle Cache
    priority: 100
    steps:
      - "./gradlew clean --no-daemon"
      - "rm -rf ~/.gradle/caches/${cache_version}"
      - Rebuild with fresh cache
```

### 4. Kubernetes & Container Issues

#### RBAC Permission Denied
```yaml
id: k8s-rbac-forbidden
category: kubernetes
name: Kubernetes RBAC Permission Denied
severity: CRITICAL
patterns:
  - regex: 'Error from server \(Forbidden\).*User "([^"]+)".*cannot (\w+) resource "([^"]+)"'
    confidence: 95
    captures: [user, verb, resource]
  - regex: 'forbidden:.*serviceaccount:([^:]+):([^\s]+).*cannot (\w+)'
    confidence: 90
    captures: [namespace, sa_name, verb]
solutions:
  - title: Grant RBAC Permission
    priority: 100
    steps:
      - Create Role/ClusterRole for ${verb} on ${resource}
      - Bind to ServiceAccount ${sa_name}
    examples:
      role: |
        apiVersion: rbac.authorization.k8s.io/v1
        kind: Role
        metadata:
          name: ${resource}-${verb}
          namespace: ${namespace}
        rules:
        - apiGroups: [""]
          resources: ["${resource}"]
          verbs: ["${verb}"]
```

#### Container Image Pull Error
```yaml
id: container-image-pull-error
category: container
name: Container Image Pull Failed
severity: CRITICAL
patterns:
  - regex: 'Failed to pull image "([^"]+)".*([^:]+): (.+)'
    confidence: 90
    captures: [image, error_type, error_message]
  - regex: 'ImagePullBackOff.*image\s+([^\s]+)'
    confidence: 85
    captures: [image]
solutions:
  - title: Verify Image Exists
    priority: 100
    steps:
      - Check if image ${image} exists in registry
      - Verify image tag is correct
      - Check registry connectivity
```

### 5. Version Control Issues

#### Git Authentication Failed
```yaml
id: git-auth-failed
category: vcs
name: Git Authentication Failed
severity: CRITICAL
patterns:
  - regex: 'fatal: Authentication failed for.*[:/]([^/]+)/([^/]+)/([^/\s]+)'
    confidence: 95
    captures: [host, org, repo]
  - regex: 'Permission denied \(publickey\).*git@([^:]+)'
    confidence: 90
    captures: [host]
solutions:
  - title: Configure Git Credentials
    priority: 100
    steps:
      - Add SSH key or token to Jenkins credentials
      - Configure credential in pipeline
      - Test with: git ls-remote ${host}/${org}/${repo}
```

## Pattern Matching Engine

### Implementation

```java
public class PatternMatchingEngine {
    private final List<DiagnosticPattern> patterns;
    private final PatternCache cache;
    
    public List<PatternMatch> analyze(String buildLog) {
        List<PatternMatch> matches = new ArrayList<>();
        
        // Split log into segments for efficient matching
        LogSegmenter segmenter = new LogSegmenter(buildLog);
        
        for (LogSegment segment : segmenter.getSegments()) {
            for (DiagnosticPattern pattern : patterns) {
                matches.addAll(matchPattern(pattern, segment));
            }
        }
        
        // Sort by confidence and severity
        return matches.stream()
            .sorted(Comparator
                .comparing(PatternMatch::getSeverity)
                .thenComparing(PatternMatch::getConfidence).reversed())
            .collect(Collectors.toList());
    }
    
    private List<PatternMatch> matchPattern(DiagnosticPattern pattern, LogSegment segment) {
        List<PatternMatch> matches = new ArrayList<>();
        
        for (PatternMatcher matcher : pattern.getMatchers()) {
            Matcher regexMatcher = matcher.getRegex().matcher(segment.getText());
            
            while (regexMatcher.find()) {
                Map<String, String> variables = matcher.extractVariables(regexMatcher);
                
                PatternMatch match = PatternMatch.builder()
                    .pattern(pattern)
                    .confidence(matcher.getConfidence())
                    .variables(variables)
                    .logLineNumber(segment.getStartLine() + regexMatcher.start())
                    .matchedText(regexMatcher.group())
                    .build();
                    
                matches.add(match);
            }
        }
        
        return matches;
    }
}
```

### Performance Optimization

```java
public class PatternCache {
    private final Cache<String, CompiledPattern> cache;
    
    public PatternCache() {
        this.cache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();
    }
    
    public CompiledPattern compile(String regex) {
        return cache.get(regex, k -> {
            int flags = Pattern.CASE_INSENSITIVE | Pattern.MULTILINE;
            return new CompiledPattern(Pattern.compile(k, flags));
        });
    }
}

public class LogSegmenter {
    private static final int SEGMENT_SIZE = 10000; // characters
    private static final int OVERLAP = 500; // for multiline patterns
    
    public List<LogSegment> getSegments() {
        // Intelligent segmentation based on log structure
        // Preserves stack traces and multi-line errors
    }
}
```

## Pattern Configuration

### YAML Format

```yaml
patterns:
  - id: unique-pattern-id
    category: kubernetes|docker|build-tool|package-manager|vcs
    name: Human Readable Name
    description: Detailed description of the issue
    severity: CRITICAL|HIGH|MEDIUM|LOW
    tags: [tag1, tag2]
    matchers:
      - regex: 'pattern with (capture groups)'
        confidence: 95
        captures: [var_name]
        multiline: false
    solutions:
      - id: solution-1
        title: Primary Solution
        priority: 100
        steps:
          - Step with ${var_name} variable
          - Another step
        examples:
          config: |
            Example configuration
```

### Loading Patterns

```java
@Component
public class PatternLoader {
    private static final String BUILTIN_PATTERNS = "/extensions/patterns/builtin/";
    private static final String USER_PATTERNS = "${JENKINS_HOME}/pipeline-doctor/patterns/";
    
    @PostConstruct
    public void loadPatterns() {
        List<DiagnosticPattern> patterns = new ArrayList<>();
        
        // Load built-in patterns
        patterns.addAll(loadFromClasspath(BUILTIN_PATTERNS));
        
        // Load user-defined patterns
        patterns.addAll(loadFromDirectory(USER_PATTERNS));
        
        // Validate and register
        validatePatterns(patterns);
        patternRegistry.registerAll(patterns);
    }
}
```

## Testing Patterns

### Pattern Test Framework

```java
public class PatternTest {
    @Test
    public void testNetworkPolicyPattern() {
        DiagnosticPattern pattern = patternLoader.load("k8s-network-policy-denied");
        String testLog = loadTestLog("network-policy-error.log");
        
        List<PatternMatch> matches = engine.matchPattern(pattern, testLog);
        
        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).getVariables())
            .containsEntry("pod_name", "frontend-abc123")
            .containsEntry("namespace", "production");
    }
}
```

## Pattern Accuracy Metrics

```java
public class PatternMetrics {
    private final Map<String, PatternStats> stats = new ConcurrentHashMap<>();
    
    public void recordMatch(String patternId, boolean wasHelpful) {
        stats.compute(patternId, (k, v) -> {
            if (v == null) v = new PatternStats();
            v.incrementMatches();
            if (wasHelpful) v.incrementHelpful();
            return v;
        });
    }
    
    public double getAccuracy(String patternId) {
        PatternStats stat = stats.get(patternId);
        return stat != null ? stat.getAccuracyRate() : 0.0;
    }
}
```