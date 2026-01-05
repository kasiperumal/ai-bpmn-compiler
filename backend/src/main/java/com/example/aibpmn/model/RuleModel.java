package com.example.aibpmn.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Objects;

/**
 * Represents a business rule
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RuleModel {
    
    private String id;
    private String expression;
    private String description;
    private String ruleType;
    private Integer priority;
    private boolean enabled;
    
    public RuleModel() {
        this.enabled = true;
        this.priority = 0;
    }
    
    public RuleModel(String id, String expression, String description) {
        this();
        this.id = id;
        this.expression = expression;
        this.description = description;
    }
    
    // Getters and Setters
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getExpression() {
        return expression;
    }
    
    public void setExpression(String expression) {
        this.expression = expression;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getRuleType() {
        return ruleType;
    }
    
    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }
    
    public Integer getPriority() {
        return priority;
    }
    
    public void setPriority(Integer priority) {
        this.priority = priority;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RuleModel ruleModel = (RuleModel) o;
        return Objects.equals(id, ruleModel.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "RuleModel{" +
                "id='" + id + '\'' +
                ", expression='" + expression + '\'' +
                ", description='" + description + '\'' +
                ", enabled=" + enabled +
                '}';
    }
}

