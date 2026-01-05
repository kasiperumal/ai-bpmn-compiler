package com.example.aibpmn.dto;

import com.example.aibpmn.model.RuleModel;

import java.util.Objects;

/**
 * Represents a detected business rule with metadata about its detection.
 */
public class DetectedRule {
    
    private RuleModel rule;
    private double confidence;
    private String detectionReason;
    private String sourceText;
    private RuleType ruleType;
    
    public DetectedRule() {
    }
    
    public DetectedRule(RuleModel rule, double confidence, String detectionReason, String sourceText, RuleType ruleType) {
        this.rule = rule;
        this.confidence = confidence;
        this.detectionReason = detectionReason;
        this.sourceText = sourceText;
        this.ruleType = ruleType;
    }
    
    public RuleModel getRule() {
        return rule;
    }
    
    public void setRule(RuleModel rule) {
        this.rule = rule;
    }
    
    public double getConfidence() {
        return confidence;
    }
    
    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }
    
    public String getDetectionReason() {
        return detectionReason;
    }
    
    public void setDetectionReason(String detectionReason) {
        this.detectionReason = detectionReason;
    }
    
    public String getSourceText() {
        return sourceText;
    }
    
    public void setSourceText(String sourceText) {
        this.sourceText = sourceText;
    }
    
    public RuleType getRuleType() {
        return ruleType;
    }
    
    public void setRuleType(RuleType ruleType) {
        this.ruleType = ruleType;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DetectedRule that = (DetectedRule) o;
        return Double.compare(that.confidence, confidence) == 0 && 
               Objects.equals(rule, that.rule) && 
               Objects.equals(detectionReason, that.detectionReason) && 
               Objects.equals(sourceText, that.sourceText) && 
               ruleType == that.ruleType;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(rule, confidence, detectionReason, sourceText, ruleType);
    }
    
    @Override
    public String toString() {
        return "DetectedRule{" +
               "rule=" + rule +
               ", confidence=" + confidence +
               ", detectionReason='" + detectionReason + '\'' +
               ", sourceText='" + sourceText + '\'' +
               ", ruleType=" + ruleType +
               '}';
    }
    
    /**
     * Types of detected rules.
     */
    public enum RuleType {
        CONDITIONAL,      // If-then conditions
        THRESHOLD,        // Numeric thresholds
        COMPARISON,       // Comparison operations
        VALIDATION,       // Data validation
        CALCULATION,      // Mathematical calculations
        APPROVAL,         // Approval/authorization rules
        ASSIGNMENT        // Value assignment
    }
}

