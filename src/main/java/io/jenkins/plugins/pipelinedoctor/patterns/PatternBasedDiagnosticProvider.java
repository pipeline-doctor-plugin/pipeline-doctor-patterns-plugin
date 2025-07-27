package io.jenkins.plugins.pipelinedoctor.patterns;

import hudson.Extension;
import io.jenkins.plugins.pipelinedoctor.*;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Diagnostic provider that uses predefined patterns to analyze build logs.
 * This is a Jenkins extension that automatically registers with the core plugin.
 */
@Extension
public class PatternBasedDiagnosticProvider implements DiagnosticProvider {
    
    private static final Logger LOGGER = Logger.getLogger(PatternBasedDiagnosticProvider.class.getName());
    private static final String BUILTIN_PATTERNS_RESOURCE = "patterns/builtin-patterns.yaml";
    
    private List<Pattern> patterns;
    private boolean initialized = false;
    
    /**
     * Initialize the provider by loading patterns.
     */
    private synchronized void initialize() {
        if (initialized) {
            return;
        }
        
        try {
            PatternLoader loader = new PatternLoader();
            patterns = loader.loadPatternsFromResource(BUILTIN_PATTERNS_RESOURCE);
            LOGGER.info("Initialized PatternBasedDiagnosticProvider with " + patterns.size() + " patterns");
            initialized = true;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load patterns", e);
            patterns = Collections.emptyList();
            initialized = true; // Still mark as initialized to avoid repeated failures
        }
    }
    
    @Override
    public List<DiagnosticResult> analyze(BuildContext context) {
        initialize();
        
        if (patterns.isEmpty()) {
            LOGGER.warning("No patterns available for analysis");
            return Collections.emptyList();
        }
        
        String buildLog = context.getBuildLog();
        if (buildLog == null || buildLog.trim().isEmpty()) {
            LOGGER.fine("Build log is empty, skipping pattern analysis");
            return Collections.emptyList();
        }
        
        List<DiagnosticResult> results = new ArrayList<>();
        
        for (Pattern pattern : patterns) {
            try {
                DiagnosticResult result = analyzeWithPattern(pattern, buildLog, context);
                if (result != null) {
                    results.add(result);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error analyzing with pattern: " + pattern.getId(), e);
            }
        }
        
        // Sort by severity and confidence
        results.sort((r1, r2) -> {
            int severityCompare = getSeverityValue(r1.getSeverity()).compareTo(getSeverityValue(r2.getSeverity()));
            if (severityCompare != 0) {
                return severityCompare;
            }
            return Integer.compare(r2.getConfidence(), r1.getConfidence()); // Higher confidence first
        });
        
        LOGGER.info("Pattern analysis found " + results.size() + " diagnostic results");
        return results;
    }
    
    /**
     * Analyze build log with a specific pattern.
     */
    private DiagnosticResult analyzeWithPattern(Pattern pattern, String buildLog, BuildContext context) {
        if (pattern.getMatchers() == null || pattern.getMatchers().isEmpty()) {
            return null;
        }
        
        // Try each matcher until we find a match
        PatternMatchResult bestMatch = null;
        PatternMatcher bestMatcher = null;
        
        for (PatternMatcher matcher : pattern.getMatchers()) {
            PatternMatchResult matchResult = matcher.match(buildLog);
            if (matchResult != null && matchResult.isMatched()) {
                if (bestMatch == null || matchResult.getConfidence() > bestMatch.getConfidence()) {
                    bestMatch = matchResult;
                    bestMatcher = matcher;
                }
            }
        }
        
        if (bestMatch == null || !bestMatch.isMatched()) {
            return null; // No match found
        }
        
        // Create diagnostic result
        DiagnosticResult.Builder resultBuilder = new DiagnosticResult.Builder()
                .id(generateResultId(pattern, bestMatch))
                .category(pattern.getCategory())
                .severity(parseSeverity(pattern.getSeverity()))
                .summary(pattern.getName())
                .description(pattern.getDescription())
                .providerId(getProviderId())
                .confidence(bestMatch.getConfidence());
        
        // Build metadata map
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("pattern_id", pattern.getId());
        metadata.put("match_text", bestMatch.getMatchText());
        if (bestMatch.getStartIndex() >= 0) {
            metadata.put("match_position", bestMatch.getStartIndex() + "-" + bestMatch.getEndIndex());
        }
        
        // Add captures as metadata
        if (bestMatch.getCaptures() != null) {
            for (Map.Entry<String, String> capture : bestMatch.getCaptures().entrySet()) {
                metadata.put("capture_" + capture.getKey(), capture.getValue());
            }
        }
        
        resultBuilder.metadata(metadata);
        
        // Convert pattern solutions to diagnostic solutions
        if (pattern.getSolutions() != null) {
            final Map<String, String> captures = bestMatch.getCaptures();
            List<Solution> solutions = pattern.getSolutions().stream()
                    .map(patternSolution -> convertToSolution(patternSolution, captures))
                    .collect(Collectors.toList());
            resultBuilder.solutions(solutions);
        }
        
        return resultBuilder.build();
    }
    
    /**
     * Convert a pattern solution to a diagnostic solution, substituting captured variables.
     */
    private Solution convertToSolution(PatternSolution patternSolution, Map<String, String> captures) {
        Solution.Builder solutionBuilder = new Solution.Builder()
                .id(patternSolution.getId())
                .title(substituteVariables(patternSolution.getTitle(), captures))
                .priority(patternSolution.getPriority());
        
        // Convert steps to action steps
        if (patternSolution.getSteps() != null) {
            List<ActionStep> actionSteps = patternSolution.getSteps().stream()
                    .map(step -> {
                        String substitutedStep = substituteVariables(step, captures);
                        // Check if step looks like a command (contains typical command patterns)
                        boolean isCommand = substitutedStep.matches(".*(?:kubectl|docker|mvn|gradle|npm|pip|git|sed|apt-get|yum)\\s+.*");
                        if (isCommand) {
                            return new ActionStep(substitutedStep, substitutedStep, false);
                        } else {
                            return new ActionStep(substitutedStep);
                        }
                    })
                    .collect(Collectors.toList());
            solutionBuilder.steps(actionSteps);
        }
        
        // Add examples
        if (patternSolution.getExamples() != null) {
            Map<String, String> substitutedExamples = new HashMap<>();
            for (Map.Entry<String, String> example : patternSolution.getExamples().entrySet()) {
                substitutedExamples.put(
                        example.getKey(),
                        substituteVariables(example.getValue(), captures)
                );
            }
            solutionBuilder.examples(substitutedExamples);
        }
        
        return solutionBuilder.build();
    }
    
    /**
     * Substitute variables in text using captured values.
     */
    private String substituteVariables(String text, Map<String, String> captures) {
        if (text == null || captures == null) {
            return text;
        }
        
        String result = text;
        for (Map.Entry<String, String> capture : captures.entrySet()) {
            String placeholder = "${" + capture.getKey() + "}";
            if (capture.getValue() != null) {
                result = result.replace(placeholder, capture.getValue());
            }
        }
        return result;
    }
    
    /**
     * Generate a unique result ID for a pattern match.
     */
    private String generateResultId(Pattern pattern, PatternMatchResult match) {
        // Use unsigned int to avoid negative values
        return pattern.getId() + "-" + Integer.toUnsignedString(match.getMatchText().hashCode());
    }
    
    /**
     * Parse severity string to enum.
     */
    private DiagnosticResult.Severity parseSeverity(String severityStr) {
        if (severityStr == null) {
            return DiagnosticResult.Severity.MEDIUM;
        }
        
        try {
            return DiagnosticResult.Severity.valueOf(severityStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Unknown severity: " + severityStr + ", defaulting to MEDIUM");
            return DiagnosticResult.Severity.MEDIUM;
        }
    }
    
    /**
     * Get numeric value for severity for sorting.
     */
    private Integer getSeverityValue(DiagnosticResult.Severity severity) {
        switch (severity) {
            case CRITICAL: return 0;
            case HIGH: return 1;
            case MEDIUM: return 2;
            case LOW: return 3;
            default: return 2;
        }
    }
    
    @Override
    public String getProviderId() {
        return "pattern-matcher";
    }
    
    @Override
    public String getProviderName() {
        return "Pattern Matcher";
    }
    
    @Override
    public Set<String> getSupportedCategories() {
        initialize();
        return patterns.stream()
                .map(Pattern::getCategory)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
    
    @Override
    public boolean isEnabled(BuildContext context) {
        // Pattern matching is fast and should run for all builds
        return true;
    }
    
    @Override
    public int getPriority() {
        // High priority since pattern matching is fast and should run first
        return 200;
    }
    
    /**
     * Get loaded patterns (for testing).
     */
    public List<Pattern> getPatterns() {
        initialize();
        return Collections.unmodifiableList(patterns);
    }
}