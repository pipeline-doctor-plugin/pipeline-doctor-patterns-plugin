package io.jenkins.plugins.pipelinedoctor.patterns;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the result of matching a pattern against text.
 */
public class PatternMatchResult {
    
    private boolean matched = false;
    private int confidence = 0;
    private String matchText;
    private int startIndex = -1;
    private int endIndex = -1;
    private Map<String, String> captures = new HashMap<>();
    
    // Constructors
    public PatternMatchResult() {}
    
    public PatternMatchResult(boolean matched, int confidence, String matchText) {
        this.matched = matched;
        this.confidence = confidence;
        this.matchText = matchText;
    }
    
    /**
     * Add a capture group result.
     */
    public void addCapture(String name, String value) {
        if (captures == null) {
            captures = new HashMap<>();
        }
        captures.put(name, value);
    }
    
    /**
     * Get a capture group value by name.
     */
    public String getCapture(String name) {
        return captures != null ? captures.get(name) : null;
    }
    
    /**
     * Check if this match has a specific capture.
     */
    public boolean hasCapture(String name) {
        return captures != null && captures.containsKey(name);
    }
    
    // Getters and Setters
    public boolean isMatched() {
        return matched;
    }
    
    public void setMatched(boolean matched) {
        this.matched = matched;
    }
    
    public int getConfidence() {
        return confidence;
    }
    
    public void setConfidence(int confidence) {
        this.confidence = confidence;
    }
    
    public String getMatchText() {
        return matchText;
    }
    
    public void setMatchText(String matchText) {
        this.matchText = matchText;
    }
    
    public int getStartIndex() {
        return startIndex;
    }
    
    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }
    
    public int getEndIndex() {
        return endIndex;
    }
    
    public void setEndIndex(int endIndex) {
        this.endIndex = endIndex;
    }
    
    public Map<String, String> getCaptures() {
        return captures;
    }
    
    public void setCaptures(Map<String, String> captures) {
        this.captures = captures;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PatternMatchResult that = (PatternMatchResult) o;
        return matched == that.matched &&
                confidence == that.confidence &&
                startIndex == that.startIndex &&
                endIndex == that.endIndex &&
                Objects.equals(matchText, that.matchText) &&
                Objects.equals(captures, that.captures);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(matched, confidence, matchText, startIndex, endIndex, captures);
    }
    
    @Override
    public String toString() {
        return "PatternMatchResult{" +
                "matched=" + matched +
                ", confidence=" + confidence +
                ", matchText='" + matchText + '\'' +
                ", captures=" + captures +
                '}';
    }
}