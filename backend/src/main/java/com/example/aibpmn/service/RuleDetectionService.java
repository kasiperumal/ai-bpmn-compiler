package com.example.aibpmn.service;

import com.example.aibpmn.dto.DetectedRule;
import com.example.aibpmn.dto.DetectedRule.RuleType;
import com.example.aibpmn.model.RuleModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for detecting business rules from text.
 * Identifies conditions, thresholds, comparisons, and other rule patterns.
 */
@Service
public class RuleDetectionService {
    
    private static final Logger logger = LoggerFactory.getLogger(RuleDetectionService.class);
    
    // Comparison operators
    private static final Pattern COMPARISON_PATTERN = Pattern.compile(
        "(greater than|more than|exceeds?|over|above|less than|below|under|fewer than|" +
        "equals?|equal to|is|are|matches?|not equal|different from|" +
        "at least|minimum of?|at most|maximum of?|between|within)" +
        "\\s+([\\$€£]?\\d+[,\\d]*\\.?\\d*[%]?|\\d+)",
        Pattern.CASE_INSENSITIVE
    );
    
    // Threshold patterns
    private static final Pattern THRESHOLD_PATTERN = Pattern.compile(
        "(amount|value|price|cost|total|sum|count|quantity|number|size|length|duration|time)" +
        "\\s+(is|are|of|:)?\\s*" +
        "(greater than|more than|exceeds?|over|above|less than|below|under|fewer than|" +
        "equals?|equal to|at least|minimum of?|at most|maximum of?|between)" +
        "\\s+([\\$€£]?\\d+[,\\d]*\\.?\\d*[%]?)",
        Pattern.CASE_INSENSITIVE
    );
    
    // Conditional patterns (if-then-else)
    private static final Pattern CONDITIONAL_PATTERN = Pattern.compile(
        "(if|when|whenever|in case|assuming|provided that)\\s+(.+?)(?:then|,|\\.|$)",
        Pattern.CASE_INSENSITIVE
    );
    
    // Approval patterns
    private static final Pattern APPROVAL_PATTERN = Pattern.compile(
        "(requires?|needs?|must have|must be|should be)\\s+" +
        "(approval|authorization|permission|review|validation|sign-?off|clearance)" +
        "(?:\\s+(?:from|by)\\s+(.+?))?",
        Pattern.CASE_INSENSITIVE
    );
    
    // Validation patterns
    private static final Pattern VALIDATION_PATTERN = Pattern.compile(
        "(validate|verify|check|ensure|confirm)\\s+(?:that\\s+)?(.+?)(?:\\.|,|$)",
        Pattern.CASE_INSENSITIVE
    );
    
    // Mathematical operators
    private static final Pattern CALCULATION_PATTERN = Pattern.compile(
        "(calculate|compute|sum|total|add|subtract|multiply|divide|average)" +
        "\\s+(.+?)(?:\\.|,|$)",
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * Detect rules from text description.
     *
     * @param text The text to analyze
     * @return List of detected rules
     */
    public List<DetectedRule> detectRules(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        logger.info("Detecting rules from text (length: {})", text.length());
        
        List<DetectedRule> detectedRules = new ArrayList<>();
        
        // Split into sentences for better analysis
        String[] sentences = text.split("[.!?]+");
        
        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            
            // Detect different types of rules
            detectedRules.addAll(detectConditionalRules(trimmed));
            detectedRules.addAll(detectThresholdRules(trimmed));
            detectedRules.addAll(detectComparisonRules(trimmed));
            detectedRules.addAll(detectApprovalRules(trimmed));
            detectedRules.addAll(detectValidationRules(trimmed));
            detectedRules.addAll(detectCalculationRules(trimmed));
        }
        
        // Deduplicate rules with similar expressions
        List<DetectedRule> uniqueRules = deduplicateRules(detectedRules);
        
        logger.info("Detected {} unique rules from text", uniqueRules.size());
        
        return uniqueRules;
    }
    
    /**
     * Detect conditional rules (if-then).
     */
    private List<DetectedRule> detectConditionalRules(String text) {
        List<DetectedRule> rules = new ArrayList<>();
        Matcher matcher = CONDITIONAL_PATTERN.matcher(text);
        
        while (matcher.find()) {
            String condition = matcher.group(2).trim();
            
            if (condition.length() < 5) {
                continue; // Too short to be meaningful
            }
            
            RuleModel rule = new RuleModel();
            rule.setId(UUID.randomUUID().toString());
            rule.setExpression(normalizeCondition(condition));
            rule.setDescription("Conditional rule: " + condition);
            rule.setRuleType("CONDITIONAL");
            rule.setPriority(50);
            rule.setEnabled(true);
            
            DetectedRule detected = new DetectedRule(
                rule,
                0.8, // High confidence for explicit conditionals
                "Detected conditional pattern: " + matcher.group(1),
                text,
                RuleType.CONDITIONAL
            );
            
            rules.add(detected);
        }
        
        return rules;
    }
    
    /**
     * Detect threshold rules (numeric limits).
     */
    private List<DetectedRule> detectThresholdRules(String text) {
        List<DetectedRule> rules = new ArrayList<>();
        Matcher matcher = THRESHOLD_PATTERN.matcher(text);
        
        while (matcher.find()) {
            String attribute = matcher.group(1).trim();
            String operator = matcher.group(3).trim();
            String value = matcher.group(4).trim();
            
            // Normalize operator to code-friendly format
            String normalizedOp = normalizeOperator(operator);
            String expression = attribute + " " + normalizedOp + " " + cleanNumericValue(value);
            
            RuleModel rule = new RuleModel();
            rule.setId(UUID.randomUUID().toString());
            rule.setExpression(expression);
            rule.setDescription("Threshold rule: " + attribute + " " + operator + " " + value);
            rule.setRuleType("THRESHOLD");
            rule.setPriority(60);
            rule.setEnabled(true);
            
            DetectedRule detected = new DetectedRule(
                rule,
                0.9, // Very high confidence for explicit thresholds
                "Detected threshold with numeric value",
                text,
                RuleType.THRESHOLD
            );
            
            rules.add(detected);
        }
        
        return rules;
    }
    
    /**
     * Detect comparison rules.
     */
    private List<DetectedRule> detectComparisonRules(String text) {
        List<DetectedRule> rules = new ArrayList<>();
        Matcher matcher = COMPARISON_PATTERN.matcher(text);
        
        while (matcher.find()) {
            String operator = matcher.group(1).trim();
            String value = matcher.group(2).trim();
            
            // Try to find what's being compared (look before the match)
            int startPos = matcher.start();
            String before = text.substring(Math.max(0, startPos - 30), startPos).trim();
            String[] words = before.split("\\s+");
            String attribute = words.length > 0 ? words[words.length - 1] : "value";
            
            String normalizedOp = normalizeOperator(operator);
            String expression = attribute + " " + normalizedOp + " " + cleanNumericValue(value);
            
            RuleModel rule = new RuleModel();
            rule.setId(UUID.randomUUID().toString());
            rule.setExpression(expression);
            rule.setDescription("Comparison rule: " + attribute + " " + operator + " " + value);
            rule.setRuleType("COMPARISON");
            rule.setPriority(55);
            rule.setEnabled(true);
            
            DetectedRule detected = new DetectedRule(
                rule,
                0.75, // Good confidence for comparisons
                "Detected comparison operator with value",
                text,
                RuleType.COMPARISON
            );
            
            rules.add(detected);
        }
        
        return rules;
    }
    
    /**
     * Detect approval rules.
     */
    private List<DetectedRule> detectApprovalRules(String text) {
        List<DetectedRule> rules = new ArrayList<>();
        Matcher matcher = APPROVAL_PATTERN.matcher(text);
        
        while (matcher.find()) {
            String action = matcher.group(1).trim();
            String approvalType = matcher.group(2).trim();
            String approver = matcher.group(3) != null ? matcher.group(3).trim() : "authorized person";
            
            String expression = "requiresApproval(\"" + approver + "\")";
            
            RuleModel rule = new RuleModel();
            rule.setId(UUID.randomUUID().toString());
            rule.setExpression(expression);
            rule.setDescription(action + " " + approvalType + " from " + approver);
            rule.setRuleType("APPROVAL");
            rule.setPriority(70);
            rule.setEnabled(true);
            
            DetectedRule detected = new DetectedRule(
                rule,
                0.85, // High confidence for approval patterns
                "Detected approval requirement",
                text,
                RuleType.APPROVAL
            );
            
            rules.add(detected);
        }
        
        return rules;
    }
    
    /**
     * Detect validation rules.
     */
    private List<DetectedRule> detectValidationRules(String text) {
        List<DetectedRule> rules = new ArrayList<>();
        Matcher matcher = VALIDATION_PATTERN.matcher(text);
        
        while (matcher.find()) {
            String validationType = matcher.group(1).trim();
            String condition = matcher.group(2).trim();
            
            if (condition.length() < 5) {
                continue;
            }
            
            String expression = validationType + "(" + normalizeCondition(condition) + ")";
            
            RuleModel rule = new RuleModel();
            rule.setId(UUID.randomUUID().toString());
            rule.setExpression(expression);
            rule.setDescription(validationType + " that " + condition);
            rule.setRuleType("VALIDATION");
            rule.setPriority(65);
            rule.setEnabled(true);
            
            DetectedRule detected = new DetectedRule(
                rule,
                0.7, // Moderate confidence for validation patterns
                "Detected validation requirement",
                text,
                RuleType.VALIDATION
            );
            
            rules.add(detected);
        }
        
        return rules;
    }
    
    /**
     * Detect calculation rules.
     */
    private List<DetectedRule> detectCalculationRules(String text) {
        List<DetectedRule> rules = new ArrayList<>();
        Matcher matcher = CALCULATION_PATTERN.matcher(text);
        
        while (matcher.find()) {
            String operation = matcher.group(1).trim();
            String formula = matcher.group(2).trim();
            
            if (formula.length() < 3) {
                continue;
            }
            
            String expression = operation + "(" + normalizeCondition(formula) + ")";
            
            RuleModel rule = new RuleModel();
            rule.setId(UUID.randomUUID().toString());
            rule.setExpression(expression);
            rule.setDescription(operation + " " + formula);
            rule.setRuleType("CALCULATION");
            rule.setPriority(40);
            rule.setEnabled(true);
            
            DetectedRule detected = new DetectedRule(
                rule,
                0.75, // Good confidence for calculation patterns
                "Detected calculation operation",
                text,
                RuleType.CALCULATION
            );
            
            rules.add(detected);
        }
        
        return rules;
    }
    
    /**
     * Normalize condition text to code-friendly format.
     */
    private String normalizeCondition(String condition) {
        return condition
            .replaceAll("\\s+", " ")
            .replaceAll("greater than", ">")
            .replaceAll("more than", ">")
            .replaceAll("exceeds?", ">")
            .replaceAll("less than", "<")
            .replaceAll("fewer than", "<")
            .replaceAll("equals?", "==")
            .replaceAll("equal to", "==")
            .replaceAll("not equal", "!=")
            .replaceAll("at least", ">=")
            .replaceAll("at most", "<=")
            .replaceAll("\\s+is\\s+", " == ")
            .replaceAll("\\s+are\\s+", " == ")
            .trim();
    }
    
    /**
     * Normalize operator to code-friendly format.
     */
    private String normalizeOperator(String operator) {
        String lower = operator.toLowerCase().trim();
        
        if (lower.matches("greater than|more than|exceeds?|over|above")) {
            return ">";
        } else if (lower.matches("less than|below|under|fewer than")) {
            return "<";
        } else if (lower.matches("equals?|equal to|is|are|matches?")) {
            return "==";
        } else if (lower.matches("not equal|different from")) {
            return "!=";
        } else if (lower.matches("at least|minimum of?")) {
            return ">=";
        } else if (lower.matches("at most|maximum of?")) {
            return "<=";
        }
        
        return operator;
    }
    
    /**
     * Clean numeric value (remove currency symbols, commas).
     */
    private String cleanNumericValue(String value) {
        return value.replaceAll("[\\$€£,]", "").trim();
    }
    
    /**
     * Deduplicate rules with similar expressions.
     */
    private List<DetectedRule> deduplicateRules(List<DetectedRule> rules) {
        Map<String, DetectedRule> uniqueRules = new LinkedHashMap<>();
        
        for (DetectedRule rule : rules) {
            String key = rule.getRule().getExpression().toLowerCase().replaceAll("\\s+", "");
            
            // Keep rule with highest confidence
            if (!uniqueRules.containsKey(key) || 
                uniqueRules.get(key).getConfidence() < rule.getConfidence()) {
                uniqueRules.put(key, rule);
            }
        }
        
        return new ArrayList<>(uniqueRules.values());
    }
    
    /**
     * Check if text contains rule indicators.
     */
    public boolean containsRuleIndicators(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        
        String lower = text.toLowerCase();
        
        // Check for conditional indicators
        if (lower.matches(".*\\b(if|when|whenever|provided that|in case)\\b.*")) {
            return true;
        }
        
        // Check for comparison operators
        if (lower.matches(".*(greater than|less than|exceeds?|above|below|equals?).*")) {
            return true;
        }
        
        // Check for threshold patterns
        if (lower.matches(".*(amount|value|total|sum)\\s*(>|<|>=|<=|==).*")) {
            return true;
        }
        
        // Check for numeric values
        if (text.matches(".*\\d+[,\\d]*\\.?\\d*.*")) {
            return true;
        }
        
        // Check for approval patterns
        if (lower.matches(".*(requires?|needs?)\\s*(approval|authorization|permission).*")) {
            return true;
        }
        
        return false;
    }
}

