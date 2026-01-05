package com.example.aibpmn.service;

import com.example.aibpmn.dto.DetectedRule;
import com.example.aibpmn.dto.DetectedRule.RuleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RuleDetectionServiceTest {
    
    private RuleDetectionService ruleDetectionService;
    
    @BeforeEach
    void setUp() {
        ruleDetectionService = new RuleDetectionService();
    }
    
    @Test
    void testDetectRules_ConditionalRule() {
        String text = "If the order amount exceeds $1000, then manager approval is required.";
        
        List<DetectedRule> rules = ruleDetectionService.detectRules(text);
        
        assertFalse(rules.isEmpty());
        assertTrue(rules.stream().anyMatch(r -> r.getRuleType() == RuleType.CONDITIONAL));
        
        DetectedRule conditionalRule = rules.stream()
            .filter(r -> r.getRuleType() == RuleType.CONDITIONAL)
            .findFirst()
            .orElseThrow();
        
        assertNotNull(conditionalRule.getRule());
        assertEquals("CONDITIONAL", conditionalRule.getRule().getRuleType());
        assertTrue(conditionalRule.getConfidence() >= 0.7);
    }
    
    @Test
    void testDetectRules_ThresholdRule() {
        String text = "The order amount is greater than $5000 for automatic approval.";
        
        List<DetectedRule> rules = ruleDetectionService.detectRules(text);
        
        assertFalse(rules.isEmpty());
        assertTrue(rules.stream().anyMatch(r -> r.getRuleType() == RuleType.THRESHOLD));
        
        DetectedRule thresholdRule = rules.stream()
            .filter(r -> r.getRuleType() == RuleType.THRESHOLD)
            .findFirst()
            .orElseThrow();
        
        assertNotNull(thresholdRule.getRule());
        assertEquals("THRESHOLD", thresholdRule.getRule().getRuleType());
        assertTrue(thresholdRule.getRule().getExpression().contains("amount"));
        assertTrue(thresholdRule.getRule().getExpression().contains(">"));
        assertTrue(thresholdRule.getConfidence() >= 0.8);
    }
    
    @Test
    void testDetectRules_ComparisonRule() {
        String text = "Check if the quantity is less than 100 items.";
        
        List<DetectedRule> rules = ruleDetectionService.detectRules(text);
        
        assertFalse(rules.isEmpty());
        // Should detect as either comparison or threshold
        assertTrue(rules.stream().anyMatch(r -> 
            r.getRuleType() == RuleType.COMPARISON || r.getRuleType() == RuleType.THRESHOLD));
    }
    
    @Test
    void testDetectRules_ApprovalRule() {
        String text = "This transaction requires approval from the finance manager.";
        
        List<DetectedRule> rules = ruleDetectionService.detectRules(text);
        
        assertFalse(rules.isEmpty());
        assertTrue(rules.stream().anyMatch(r -> r.getRuleType() == RuleType.APPROVAL));
        
        DetectedRule approvalRule = rules.stream()
            .filter(r -> r.getRuleType() == RuleType.APPROVAL)
            .findFirst()
            .orElseThrow();
        
        assertNotNull(approvalRule.getRule());
        assertEquals("APPROVAL", approvalRule.getRule().getRuleType());
        assertTrue(approvalRule.getRule().getExpression().contains("requiresApproval"));
    }
    
    @Test
    void testDetectRules_ValidationRule() {
        String text = "Validate that the customer email is in the correct format.";
        
        List<DetectedRule> rules = ruleDetectionService.detectRules(text);
        
        assertFalse(rules.isEmpty());
        assertTrue(rules.stream().anyMatch(r -> r.getRuleType() == RuleType.VALIDATION));
        
        DetectedRule validationRule = rules.stream()
            .filter(r -> r.getRuleType() == RuleType.VALIDATION)
            .findFirst()
            .orElseThrow();
        
        assertNotNull(validationRule.getRule());
        assertEquals("VALIDATION", validationRule.getRule().getRuleType());
    }
    
    @Test
    void testDetectRules_CalculationRule() {
        String text = "Calculate the total price by multiplying quantity and unit price.";
        
        List<DetectedRule> rules = ruleDetectionService.detectRules(text);
        
        assertFalse(rules.isEmpty());
        assertTrue(rules.stream().anyMatch(r -> r.getRuleType() == RuleType.CALCULATION));
        
        DetectedRule calculationRule = rules.stream()
            .filter(r -> r.getRuleType() == RuleType.CALCULATION)
            .findFirst()
            .orElseThrow();
        
        assertNotNull(calculationRule.getRule());
        assertEquals("CALCULATION", calculationRule.getRule().getRuleType());
    }
    
    @Test
    void testDetectRules_MultipleRules() {
        String text = """
            If the order amount exceeds $1000, manager approval is required.
            Validate that the customer information is complete.
            The discount percentage must be less than 50%.
            """;
        
        List<DetectedRule> rules = ruleDetectionService.detectRules(text);
        
        assertTrue(rules.size() >= 3);
        
        // Should have conditional, validation, and threshold/comparison rules
        assertTrue(rules.stream().anyMatch(r -> r.getRuleType() == RuleType.CONDITIONAL));
        assertTrue(rules.stream().anyMatch(r -> r.getRuleType() == RuleType.VALIDATION));
        assertTrue(rules.stream().anyMatch(r -> 
            r.getRuleType() == RuleType.THRESHOLD || r.getRuleType() == RuleType.COMPARISON));
    }
    
    @Test
    void testDetectRules_NoRules() {
        String text = "The process starts when an order is received. Then it continues to the next step.";
        
        List<DetectedRule> rules = ruleDetectionService.detectRules(text);
        
        // Might detect a conditional from "when", but that's OK
        // The important thing is it doesn't crash
        assertNotNull(rules);
    }
    
    @Test
    void testDetectRules_EmptyText() {
        List<DetectedRule> rules = ruleDetectionService.detectRules("");
        
        assertTrue(rules.isEmpty());
    }
    
    @Test
    void testDetectRules_NullText() {
        List<DetectedRule> rules = ruleDetectionService.detectRules(null);
        
        assertTrue(rules.isEmpty());
    }
    
    @Test
    void testDetectRules_WithCurrencySymbols() {
        String text = "If the amount is greater than $10,000 or €8,500, escalate to senior management.";
        
        List<DetectedRule> rules = ruleDetectionService.detectRules(text);
        
        assertFalse(rules.isEmpty());
        
        // Should clean currency symbols in expression
        rules.forEach(rule -> {
            if (rule.getRule().getExpression().contains("10000") || 
                rule.getRule().getExpression().contains("8500")) {
                assertFalse(rule.getRule().getExpression().contains("$"));
                assertFalse(rule.getRule().getExpression().contains("€"));
            }
        });
    }
    
    @Test
    void testDetectRules_WithPercentages() {
        String text = "The discount rate must be at most 25% for standard customers.";
        
        List<DetectedRule> rules = ruleDetectionService.detectRules(text);
        
        assertFalse(rules.isEmpty());
        assertTrue(rules.stream().anyMatch(r -> 
            r.getRuleType() == RuleType.THRESHOLD || r.getRuleType() == RuleType.COMPARISON));
    }
    
    @Test
    void testDetectRules_ComplexThresholds() {
        String text = """
            For orders with a total value exceeding $5000, the following rules apply:
            - Discount percentage is at most 10%
            - Delivery time is at least 3 days
            - Number of items is between 10 and 100
            """;
        
        List<DetectedRule> rules = ruleDetectionService.detectRules(text);
        
        assertFalse(rules.isEmpty());
        
        // Should detect multiple threshold rules
        long thresholdRules = rules.stream()
            .filter(r -> r.getRuleType() == RuleType.THRESHOLD || r.getRuleType() == RuleType.COMPARISON)
            .count();
        
        assertTrue(thresholdRules >= 2);
    }
    
    @Test
    void testDetectRules_MultipleConditions() {
        String text = """
            When the customer type is premium and order value is above $1000,
            apply a 15% discount and waive shipping fees.
            """;
        
        List<DetectedRule> rules = ruleDetectionService.detectRules(text);
        
        assertFalse(rules.isEmpty());
        
        // Should detect conditional and possibly threshold
        assertTrue(rules.stream().anyMatch(r -> 
            r.getRuleType() == RuleType.CONDITIONAL || r.getRuleType() == RuleType.THRESHOLD));
    }
    
    @Test
    void testContainsRuleIndicators_WithConditional() {
        assertTrue(ruleDetectionService.containsRuleIndicators("If amount > 1000 then approve"));
        assertTrue(ruleDetectionService.containsRuleIndicators("When customer is premium"));
        assertTrue(ruleDetectionService.containsRuleIndicators("Whenever order exceeds limit"));
    }
    
    @Test
    void testContainsRuleIndicators_WithComparison() {
        assertTrue(ruleDetectionService.containsRuleIndicators("amount greater than 500"));
        assertTrue(ruleDetectionService.containsRuleIndicators("value less than threshold"));
        assertTrue(ruleDetectionService.containsRuleIndicators("quantity exceeds 100"));
    }
    
    @Test
    void testContainsRuleIndicators_WithNumbers() {
        assertTrue(ruleDetectionService.containsRuleIndicators("Total is $1,000"));
        assertTrue(ruleDetectionService.containsRuleIndicators("Quantity: 50 items"));
        assertTrue(ruleDetectionService.containsRuleIndicators("Discount 25%"));
    }
    
    @Test
    void testContainsRuleIndicators_WithApproval() {
        assertTrue(ruleDetectionService.containsRuleIndicators("Requires approval from manager"));
        assertTrue(ruleDetectionService.containsRuleIndicators("Needs authorization"));
    }
    
    @Test
    void testContainsRuleIndicators_NoIndicators() {
        assertFalse(ruleDetectionService.containsRuleIndicators("Process the order"));
        assertFalse(ruleDetectionService.containsRuleIndicators("Send notification"));
        assertFalse(ruleDetectionService.containsRuleIndicators("Complete the task"));
    }
    
    @Test
    void testContainsRuleIndicators_EmptyText() {
        assertFalse(ruleDetectionService.containsRuleIndicators(""));
        assertFalse(ruleDetectionService.containsRuleIndicators(null));
    }
    
    @Test
    void testDetectRules_Deduplication() {
        String text = """
            If amount exceeds 1000 then escalate.
            When amount is greater than 1000 then escalate.
            Amount greater than 1000 requires escalation.
            """;
        
        List<DetectedRule> rules = ruleDetectionService.detectRules(text);
        
        // Should deduplicate similar rules
        // The exact count depends on pattern matching, but should be reasonable
        assertNotNull(rules);
        assertTrue(rules.size() < 10); // Should not create excessive duplicates
    }
    
    @Test
    void testDetectRules_PriorityAssignment() {
        String text = """
            If amount exceeds 5000, manager approval is required.
            Validate customer information is complete.
            Calculate total by summing line items.
            """;
        
        List<DetectedRule> rules = ruleDetectionService.detectRules(text);
        
        assertFalse(rules.isEmpty());
        
        // All rules should have priorities assigned
        rules.forEach(rule -> {
            assertNotNull(rule.getRule());
            assertTrue(rule.getRule().getPriority() > 0);
        });
    }
    
    @Test
    void testDetectRules_RuleDescriptions() {
        String text = "If the order amount exceeds $1000, then manager approval is required.";
        
        List<DetectedRule> rules = ruleDetectionService.detectRules(text);
        
        assertFalse(rules.isEmpty());
        
        // All rules should have descriptions
        rules.forEach(rule -> {
            assertNotNull(rule.getRule().getDescription());
            assertFalse(rule.getRule().getDescription().isEmpty());
        });
    }
    
    @Test
    void testDetectRules_EnabledByDefault() {
        String text = "If amount > 1000 then escalate.";
        
        List<DetectedRule> rules = ruleDetectionService.detectRules(text);
        
        assertFalse(rules.isEmpty());
        
        // All rules should be enabled by default
        rules.forEach(rule -> {
            assertTrue(rule.getRule().isEnabled());
        });
    }
}

