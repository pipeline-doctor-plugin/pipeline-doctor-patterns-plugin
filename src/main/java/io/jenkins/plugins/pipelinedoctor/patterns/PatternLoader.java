package io.jenkins.plugins.pipelinedoctor.patterns;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads patterns from YAML files.
 */
public class PatternLoader {
    
    private static final Logger LOGGER = Logger.getLogger(PatternLoader.class.getName());
    
    /**
     * Load patterns from a YAML input stream.
     */
    public List<Pattern> loadPatternsFromStream(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            LOGGER.warning("Pattern input stream is null");
            return Collections.emptyList();
        }
        
        try {
            Yaml yaml = new Yaml();
            Object data = yaml.load(inputStream);
            
            if (!(data instanceof Map)) {
                LOGGER.warning("Pattern YAML root is not a map");
                return Collections.emptyList();
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> rootMap = (Map<String, Object>) data;
            Object patternsObj = rootMap.get("patterns");
            
            if (!(patternsObj instanceof List)) {
                LOGGER.warning("Patterns section is not a list");
                return Collections.emptyList();
            }
            
            @SuppressWarnings("unchecked")
            List<Object> patternsList = (List<Object>) patternsObj;
            
            List<Pattern> patterns = new ArrayList<>();
            for (Object patternObj : patternsList) {
                try {
                    Pattern pattern = parsePattern(patternObj);
                    if (pattern != null) {
                        patterns.add(pattern);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to parse pattern: " + patternObj, e);
                }
            }
            
            LOGGER.info("Loaded " + patterns.size() + " patterns");
            return patterns;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load patterns from YAML", e);
            throw new IOException("Failed to load patterns from YAML", e);
        }
    }
    
    /**
     * Load patterns from a resource file in the classpath.
     */
    public List<Pattern> loadPatternsFromResource(String resourcePath) throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (inputStream == null) {
            throw new IOException("Pattern resource not found: " + resourcePath);
        }
        
        try {
            return loadPatternsFromStream(inputStream);
        } finally {
            inputStream.close();
        }
    }
    
    /**
     * Parse a single pattern from YAML object.
     */
    @SuppressWarnings("unchecked")
    private Pattern parsePattern(Object patternObj) {
        if (!(patternObj instanceof Map)) {
            LOGGER.warning("Pattern is not a map: " + patternObj);
            return null;
        }
        
        Map<String, Object> patternMap = (Map<String, Object>) patternObj;
        Pattern pattern = new Pattern();
        
        // Basic fields
        pattern.setId(getString(patternMap, "id"));
        pattern.setCategory(getString(patternMap, "category"));
        pattern.setName(getString(patternMap, "name"));
        pattern.setDescription(getString(patternMap, "description"));
        pattern.setSeverity(getString(patternMap, "severity"));
        
        // Tags
        Object tagsObj = patternMap.get("tags");
        if (tagsObj instanceof List) {
            List<String> tags = new ArrayList<>();
            for (Object tag : (List<Object>) tagsObj) {
                if (tag != null) {
                    tags.add(tag.toString());
                }
            }
            pattern.setTags(tags);
        }
        
        // Matchers
        Object matchersObj = patternMap.get("matchers");
        if (matchersObj instanceof List) {
            pattern.setMatchers(parseMatchers((List<Object>) matchersObj));
        }
        
        // Solutions
        Object solutionsObj = patternMap.get("solutions");
        if (solutionsObj instanceof List) {
            pattern.setSolutions(parseSolutions((List<Object>) solutionsObj));
        }
        
        // Validate required fields
        if (pattern.getId() == null || pattern.getName() == null) {
            LOGGER.warning("Pattern missing required fields (id, name): " + patternMap);
            return null;
        }
        
        return pattern;
    }
    
    /**
     * Parse matchers from YAML objects.
     */
    @SuppressWarnings("unchecked")
    private List<PatternMatcher> parseMatchers(List<Object> matchersObj) {
        List<PatternMatcher> matchers = new ArrayList<>();
        
        for (Object matcherObj : matchersObj) {
            if (!(matcherObj instanceof Map)) {
                continue;
            }
            
            Map<String, Object> matcherMap = (Map<String, Object>) matcherObj;
            PatternMatcher matcher = new PatternMatcher();
            
            matcher.setRegex(getString(matcherMap, "regex"));
            matcher.setConfidence(getInt(matcherMap, "confidence", 80));
            matcher.setMultiline(getBoolean(matcherMap, "multiline", false));
            
            // Captures
            Object capturesObj = matcherMap.get("captures");
            if (capturesObj instanceof List) {
                List<String> captures = new ArrayList<>();
                for (Object capture : (List<Object>) capturesObj) {
                    if (capture != null) {
                        captures.add(capture.toString());
                    }
                }
                matcher.setCaptures(captures);
            }
            
            if (matcher.getRegex() != null) {
                matchers.add(matcher);
            }
        }
        
        return matchers;
    }
    
    /**
     * Parse solutions from YAML objects.
     */
    @SuppressWarnings("unchecked")
    private List<PatternSolution> parseSolutions(List<Object> solutionsObj) {
        List<PatternSolution> solutions = new ArrayList<>();
        
        for (Object solutionObj : solutionsObj) {
            if (!(solutionObj instanceof Map)) {
                continue;
            }
            
            Map<String, Object> solutionMap = (Map<String, Object>) solutionObj;
            PatternSolution solution = new PatternSolution();
            
            solution.setId(getString(solutionMap, "id"));
            solution.setTitle(getString(solutionMap, "title"));
            solution.setPriority(getInt(solutionMap, "priority", 100));
            
            // Steps
            Object stepsObj = solutionMap.get("steps");
            if (stepsObj instanceof List) {
                List<String> steps = new ArrayList<>();
                for (Object step : (List<Object>) stepsObj) {
                    if (step != null) {
                        steps.add(step.toString());
                    }
                }
                solution.setSteps(steps);
            }
            
            // Examples
            Object examplesObj = solutionMap.get("examples");
            if (examplesObj instanceof Map) {
                Map<String, String> examples = new java.util.HashMap<>();
                Map<String, Object> examplesMap = (Map<String, Object>) examplesObj;
                for (Map.Entry<String, Object> entry : examplesMap.entrySet()) {
                    if (entry.getValue() != null) {
                        examples.put(entry.getKey(), entry.getValue().toString());
                    }
                }
                solution.setExamples(examples);
            }
            
            if (solution.getId() != null && solution.getTitle() != null) {
                solutions.add(solution);
            }
        }
        
        return solutions;
    }
    
    // Helper methods for safe type conversion
    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
    
    private int getInt(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                LOGGER.warning("Failed to parse int from: " + value);
            }
        }
        return defaultValue;
    }
    
    private boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }
}