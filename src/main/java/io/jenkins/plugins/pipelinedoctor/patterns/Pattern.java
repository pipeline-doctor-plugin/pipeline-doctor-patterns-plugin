package io.jenkins.plugins.pipelinedoctor.patterns;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a diagnostic pattern that can match against build logs.
 */
public class Pattern {
    
    private String id;
    private String category;
    private String name;
    private String description;
    private String severity;
    private List<String> tags;
    private List<PatternMatcher> matchers;
    private List<PatternSolution> solutions;
    
    // Constructors
    public Pattern() {}
    
    public Pattern(String id, String category, String name, String description, 
                   String severity, List<String> tags, List<PatternMatcher> matchers, 
                   List<PatternSolution> solutions) {
        this.id = id;
        this.category = category;
        this.name = name;
        this.description = description;
        this.severity = severity;
        this.tags = tags;
        this.matchers = matchers;
        this.solutions = solutions;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getSeverity() {
        return severity;
    }
    
    public void setSeverity(String severity) {
        this.severity = severity;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
    public List<PatternMatcher> getMatchers() {
        return matchers;
    }
    
    public void setMatchers(List<PatternMatcher> matchers) {
        this.matchers = matchers;
    }
    
    public List<PatternSolution> getSolutions() {
        return solutions;
    }
    
    public void setSolutions(List<PatternSolution> solutions) {
        this.solutions = solutions;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pattern pattern = (Pattern) o;
        return Objects.equals(id, pattern.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Pattern{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", severity='" + severity + '\'' +
                '}';
    }
}