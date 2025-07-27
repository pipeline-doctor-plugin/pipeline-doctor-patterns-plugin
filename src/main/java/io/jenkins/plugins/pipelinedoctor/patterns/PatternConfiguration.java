package io.jenkins.plugins.pipelinedoctor.patterns;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Global configuration for Pattern-based Diagnostic Provider.
 */
@Extension
public class PatternConfiguration extends GlobalConfiguration {

    private boolean enableBuiltinPatterns = true;
    private String customPatternsPath = "";
    private boolean enableDebugLogging = false;
    private int maxMatchesPerPattern = 5;
    private Set<String> disabledCategories = new HashSet<>();
    private int minimumConfidenceThreshold = 70;

    public PatternConfiguration() {
        load();
    }

    public static PatternConfiguration get() {
        return GlobalConfiguration.all().get(PatternConfiguration.class);
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws Descriptor.FormException {
        req.bindJSON(this, json);
        save();
        return super.configure(req, json);
    }

    @Override
    public String getDisplayName() {
        return "Pattern-based Diagnostics";
    }

    // Getters and Setters

    public boolean isEnableBuiltinPatterns() {
        return enableBuiltinPatterns;
    }

    public void setEnableBuiltinPatterns(boolean enableBuiltinPatterns) {
        this.enableBuiltinPatterns = enableBuiltinPatterns;
    }

    public String getCustomPatternsPath() {
        return customPatternsPath;
    }

    public void setCustomPatternsPath(String customPatternsPath) {
        this.customPatternsPath = customPatternsPath;
    }

    public boolean isEnableDebugLogging() {
        return enableDebugLogging;
    }

    public void setEnableDebugLogging(boolean enableDebugLogging) {
        this.enableDebugLogging = enableDebugLogging;
    }

    public int getMaxMatchesPerPattern() {
        return maxMatchesPerPattern;
    }

    public void setMaxMatchesPerPattern(int maxMatchesPerPattern) {
        this.maxMatchesPerPattern = maxMatchesPerPattern;
    }

    public Set<String> getDisabledCategories() {
        return disabledCategories;
    }

    public void setDisabledCategories(Set<String> disabledCategories) {
        this.disabledCategories = disabledCategories;
    }

    public int getMinimumConfidenceThreshold() {
        return minimumConfidenceThreshold;
    }

    public void setMinimumConfidenceThreshold(int minimumConfidenceThreshold) {
        this.minimumConfidenceThreshold = minimumConfidenceThreshold;
    }

    /**
     * Get disabled categories as comma-separated string for form binding.
     */
    public String getDisabledCategoriesString() {
        if (disabledCategories == null || disabledCategories.isEmpty()) {
            return "";
        }
        return String.join(", ", disabledCategories);
    }

    /**
     * Set disabled categories from comma-separated string.
     */
    public void setDisabledCategoriesString(String categories) {
        if (categories == null || categories.trim().isEmpty()) {
            this.disabledCategories = new HashSet<>();
        } else {
            this.disabledCategories = new HashSet<>(
                Arrays.asList(categories.split(","))
            );
            // Trim whitespace and remove empty strings
            Set<String> trimmed = new HashSet<>();
            for (String category : this.disabledCategories) {
                String trimmedCategory = category.trim();
                if (!trimmedCategory.isEmpty()) {
                    trimmed.add(trimmedCategory);
                }
            }
            this.disabledCategories = trimmed;
        }
    }

    /**
     * Check if a category is enabled.
     */
    public boolean isCategoryEnabled(String category) {
        return disabledCategories == null || !disabledCategories.contains(category);
    }

    // Form validation methods

    public FormValidation doCheckMaxMatchesPerPattern(@QueryParameter int value) {
        if (value < 1) {
            return FormValidation.error("Maximum matches must be at least 1");
        }
        if (value > 100) {
            return FormValidation.warning("Very high values may impact performance");
        }
        return FormValidation.ok();
    }

    public FormValidation doCheckMinimumConfidenceThreshold(@QueryParameter int value) {
        if (value < 0 || value > 100) {
            return FormValidation.error("Confidence threshold must be between 0 and 100");
        }
        if (value < 50) {
            return FormValidation.warning("Low confidence threshold may produce false positives");
        }
        return FormValidation.ok();
    }

    public FormValidation doCheckCustomPatternsPath(@QueryParameter String value) {
        if (value == null || value.trim().isEmpty()) {
            return FormValidation.ok("Custom patterns are optional");
        }
        
        // Basic validation - check if path looks reasonable
        if (!value.endsWith(".yaml") && !value.endsWith(".yml")) {
            return FormValidation.warning("Pattern files should have .yaml or .yml extension");
        }
        
        return FormValidation.ok();
    }

    public FormValidation doCheckDisabledCategoriesString(@QueryParameter String value) {
        if (value == null || value.trim().isEmpty()) {
            return FormValidation.ok();
        }

        String[] categories = value.split(",");
        Set<String> validCategories = new HashSet<>(Arrays.asList(
            "kubernetes", "docker", "package-manager", "build-tool", "vcs", 
            "test", "performance", "network", "security"
        ));

        for (String category : categories) {
            String trimmed = category.trim();
            if (!trimmed.isEmpty() && !validCategories.contains(trimmed)) {
                return FormValidation.warning("Unknown category: " + trimmed + 
                    ". Valid categories: " + String.join(", ", validCategories));
            }
        }

        return FormValidation.ok();
    }
}