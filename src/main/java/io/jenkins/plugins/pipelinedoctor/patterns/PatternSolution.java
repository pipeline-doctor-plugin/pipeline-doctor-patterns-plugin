package io.jenkins.plugins.pipelinedoctor.patterns;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a solution definition within a pattern.
 */
public class PatternSolution {
    
    private String id;
    private String title;
    private int priority;
    private List<String> steps;
    private Map<String, String> examples;
    
    // Constructors
    public PatternSolution() {}
    
    public PatternSolution(String id, String title, int priority, List<String> steps, Map<String, String> examples) {
        this.id = id;
        this.title = title;
        this.priority = priority;
        this.steps = steps;
        this.examples = examples;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public void setPriority(int priority) {
        this.priority = priority;
    }
    
    public List<String> getSteps() {
        return steps;
    }
    
    public void setSteps(List<String> steps) {
        this.steps = steps;
    }
    
    public Map<String, String> getExamples() {
        return examples;
    }
    
    public void setExamples(Map<String, String> examples) {
        this.examples = examples;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PatternSolution that = (PatternSolution) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "PatternSolution{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", priority=" + priority +
                '}';
    }
}