package io.jenkins.plugins.pipelinedoctor.patterns;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a pattern matcher that can find matches in build logs.
 */
public class PatternMatcher {
    
    private String regex;
    private int confidence;
    private List<String> captures;
    private boolean multiline;
    private transient java.util.regex.Pattern compiledPattern;
    
    // Constructors
    public PatternMatcher() {}
    
    public PatternMatcher(String regex, int confidence, List<String> captures, boolean multiline) {
        this.regex = regex;
        this.confidence = confidence;
        this.captures = captures;
        this.multiline = multiline;
    }
    
    /**
     * Get the compiled regex pattern, compiling it if necessary.
     */
    public java.util.regex.Pattern getCompiledPattern() {
        if (compiledPattern == null && regex != null) {
            int flags = 0;
            if (multiline) {
                flags |= java.util.regex.Pattern.MULTILINE | java.util.regex.Pattern.DOTALL;
            }
            compiledPattern = java.util.regex.Pattern.compile(regex, flags);
        }
        return compiledPattern;
    }
    
    /**
     * Match this pattern against text and return match result.
     */
    public PatternMatchResult match(String text) {
        java.util.regex.Pattern pattern = getCompiledPattern();
        if (pattern == null) {
            return null;
        }
        
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            PatternMatchResult result = new PatternMatchResult();
            result.setMatched(true);
            result.setConfidence(confidence);
            result.setMatchText(matcher.group(0));
            result.setStartIndex(matcher.start());
            result.setEndIndex(matcher.end());
            
            // Extract named captures
            if (captures != null && !captures.isEmpty()) {
                for (int i = 0; i < captures.size() && i + 1 <= matcher.groupCount(); i++) {
                    String captureValue = matcher.group(i + 1);
                    if (captureValue != null) {
                        result.addCapture(captures.get(i), captureValue);
                    }
                }
            }
            
            return result;
        }
        
        return new PatternMatchResult(); // No match
    }
    
    // Getters and Setters
    public String getRegex() {
        return regex;
    }
    
    public void setRegex(String regex) {
        this.regex = regex;
        this.compiledPattern = null; // Reset compiled pattern
    }
    
    public int getConfidence() {
        return confidence;
    }
    
    public void setConfidence(int confidence) {
        this.confidence = confidence;
    }
    
    public List<String> getCaptures() {
        return captures;
    }
    
    public void setCaptures(List<String> captures) {
        this.captures = captures;
    }
    
    public boolean isMultiline() {
        return multiline;
    }
    
    public void setMultiline(boolean multiline) {
        this.multiline = multiline;
        this.compiledPattern = null; // Reset compiled pattern
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PatternMatcher that = (PatternMatcher) o;
        return confidence == that.confidence &&
                multiline == that.multiline &&
                Objects.equals(regex, that.regex) &&
                Objects.equals(captures, that.captures);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(regex, confidence, captures, multiline);
    }
    
    @Override
    public String toString() {
        return "PatternMatcher{" +
                "regex='" + regex + '\'' +
                ", confidence=" + confidence +
                ", multiline=" + multiline +
                '}';
    }
}