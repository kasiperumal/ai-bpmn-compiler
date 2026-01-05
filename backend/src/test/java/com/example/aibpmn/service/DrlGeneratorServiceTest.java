package com.example.aibpmn.service;

import com.example.aibpmn.exception.DrlValidationException;
import com.example.aibpmn.model.RuleModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DrlGeneratorServiceTest {
    
    private DrlGeneratorService drlGenerator;
    
    @BeforeEach
    void setUp() {
        drlGenerator = new DrlGeneratorService();
    }
    
    @Test
    void testGenerateDrl_SingleRule() {
        RuleModel rule = new RuleModel();
        rule.setId("test-rule-1");
        rule.setExpression("true");
        rule.setDescription("Test rule");
        rule.setRuleType("CONDITIONAL");
        rule.setPriority(50);
        rule.setEnabled(true);
        
        String drl = drlGenerator.generateDrl(rule);
        
        assertNotNull(drl);
        assertTrue(drl.contains("package com.example.aibpmn.rules"));
        assertTrue(drl.contains("rule \"test-rule-1\""));
        assertTrue(drl.contains("salience 50"));
        assertTrue(drl.contains("when"));
        assertTrue(drl.contains("then"));
        assertTrue(drl.contains("end"));
    }
    
    @Test
    void testGenerateDrl_MultipleRules() {
        List<RuleModel> rules = new ArrayList<>();
        
        RuleModel rule1 = new RuleModel();
        rule1.setId("rule-1");
        rule1.setExpression("true");
        rule1.setDescription("First rule");
        rule1.setRuleType("CONDITIONAL");
        rule1.setPriority(50);
        rule1.setEnabled(true);
        rules.add(rule1);
        
        RuleModel rule2 = new RuleModel();
        rule2.setId("rule-2");
        rule2.setExpression("true");
        rule2.setDescription("Second rule");
        rule2.setRuleType("THRESHOLD");
        rule2.setPriority(60);
        rule2.setEnabled(true);
        rules.add(rule2);
        
        String drl = drlGenerator.generateDrl(rules);
        
        assertNotNull(drl);
        assertTrue(drl.contains("rule \"rule-1\""));
        assertTrue(drl.contains("rule \"rule-2\""));
        assertTrue(drl.contains("salience 50"));
        assertTrue(drl.contains("salience 60"));
    }
    
    @Test
    void testGenerateDrl_ConditionalRule() {
        RuleModel rule = new RuleModel();
        rule.setId("conditional-rule");
        rule.setExpression("amount > 1000");
        rule.setDescription("High value conditional rule");
        rule.setRuleType("CONDITIONAL");
        rule.setPriority(50);
        rule.setEnabled(true);
        
        String drl = drlGenerator.generateDrl(rule);
        
        assertNotNull(drl);
        assertTrue(drl.contains("rule \"conditional-rule\""));
        assertTrue(drl.contains("when"));
        assertTrue(drl.contains("then"));
    }
    
    @Test
    void testGenerateDrl_ThresholdRule() {
        RuleModel rule = new RuleModel();
        rule.setId("threshold-rule");
        rule.setExpression("value >= 5000");
        rule.setDescription("Threshold check");
        rule.setRuleType("THRESHOLD");
        rule.setPriority(60);
        rule.setEnabled(true);
        
        String drl = drlGenerator.generateDrl(rule);
        
        assertNotNull(drl);
        assertTrue(drl.contains("rule \"threshold-rule\""));
        assertTrue(drl.contains("salience 60"));
    }
    
    @Test
    void testGenerateDrl_ApprovalRule() {
        RuleModel rule = new RuleModel();
        rule.setId("approval-rule");
        rule.setExpression("requiresApproval(\"manager\")");
        rule.setDescription("Manager approval required");
        rule.setRuleType("APPROVAL");
        rule.setPriority(70);
        rule.setEnabled(true);
        
        String drl = drlGenerator.generateDrl(rule);
        
        assertNotNull(drl);
        assertTrue(drl.contains("rule \"approval-rule\""));
    }
    
    @Test
    void testGenerateDrl_ValidationRule() {
        RuleModel rule = new RuleModel();
        rule.setId("validation-rule");
        rule.setExpression("validate(email)");
        rule.setDescription("Email validation");
        rule.setRuleType("VALIDATION");
        rule.setPriority(65);
        rule.setEnabled(true);
        
        String drl = drlGenerator.generateDrl(rule);
        
        assertNotNull(drl);
        assertTrue(drl.contains("rule \"validation-rule\""));
    }
    
    @Test
    void testGenerateDrl_CalculationRule() {
        RuleModel rule = new RuleModel();
        rule.setId("calculation-rule");
        rule.setExpression("calculate(total)");
        rule.setDescription("Calculate total");
        rule.setRuleType("CALCULATION");
        rule.setPriority(40);
        rule.setEnabled(true);
        
        String drl = drlGenerator.generateDrl(rule);
        
        assertNotNull(drl);
        assertTrue(drl.contains("rule \"calculation-rule\""));
    }
    
    @Test
    void testGenerateDrl_DisabledRule() {
        List<RuleModel> rules = new ArrayList<>();
        
        RuleModel enabledRule = new RuleModel();
        enabledRule.setId("enabled-rule");
        enabledRule.setExpression("true");
        enabledRule.setDescription("Enabled");
        enabledRule.setRuleType("CONDITIONAL");
        enabledRule.setPriority(50);
        enabledRule.setEnabled(true);
        rules.add(enabledRule);
        
        RuleModel disabledRule = new RuleModel();
        disabledRule.setId("disabled-rule");
        disabledRule.setExpression("false");
        disabledRule.setDescription("Disabled");
        disabledRule.setRuleType("CONDITIONAL");
        disabledRule.setPriority(50);
        disabledRule.setEnabled(false);
        rules.add(disabledRule);
        
        String drl = drlGenerator.generateDrl(rules);
        
        assertNotNull(drl);
        assertTrue(drl.contains("rule \"enabled-rule\""));
        assertFalse(drl.contains("rule \"disabled-rule\""));
    }
    
    @Test
    void testGenerateDrl_CustomPackage() {
        RuleModel rule = new RuleModel();
        rule.setId("custom-package-rule");
        rule.setExpression("true");
        rule.setDescription("Custom package test");
        rule.setRuleType("CONDITIONAL");
        rule.setPriority(50);
        rule.setEnabled(true);
        
        String drl = drlGenerator.generateDrl(List.of(rule), "com.custom.package", false);
        
        assertNotNull(drl);
        assertTrue(drl.contains("package com.custom.package"));
    }
    
    @Test
    void testGenerateDrl_NullRules() {
        assertThrows(IllegalArgumentException.class, () -> {
            drlGenerator.generateDrl((List<RuleModel>) null);
        });
    }
    
    @Test
    void testGenerateDrl_EmptyRules() {
        assertThrows(IllegalArgumentException.class, () -> {
            drlGenerator.generateDrl(new ArrayList<>());
        });
    }
    
    @Test
    void testGenerateDrl_NullExpression() {
        RuleModel rule = new RuleModel();
        rule.setId("null-expression-rule");
        rule.setExpression(null);
        rule.setDescription("Rule with null expression");
        rule.setRuleType("CONDITIONAL");
        rule.setPriority(50);
        rule.setEnabled(true);
        
        String drl = drlGenerator.generateDrl(rule);
        
        assertNotNull(drl);
        assertTrue(drl.contains("rule \"null-expression-rule\""));
        // Should have a default condition
        assertTrue(drl.contains("when"));
    }
    
    @Test
    void testGenerateDrl_EmptyExpression() {
        RuleModel rule = new RuleModel();
        rule.setId("empty-expression-rule");
        rule.setExpression("");
        rule.setDescription("Rule with empty expression");
        rule.setRuleType("CONDITIONAL");
        rule.setPriority(50);
        rule.setEnabled(true);
        
        String drl = drlGenerator.generateDrl(rule);
        
        assertNotNull(drl);
        assertTrue(drl.contains("rule \"empty-expression-rule\""));
    }
    
    @Test
    void testGenerateDrl_SpecialCharactersInName() {
        RuleModel rule = new RuleModel();
        rule.setId("rule-with-\"quotes\"");
        rule.setExpression("true");
        rule.setDescription("Test special chars");
        rule.setRuleType("CONDITIONAL");
        rule.setPriority(50);
        rule.setEnabled(true);
        
        String drl = drlGenerator.generateDrl(rule);
        
        assertNotNull(drl);
        // Should escape quotes
        assertTrue(drl.contains("rule \"rule-with-\\\"quotes\\\"\""));
    }
    
    @Test
    void testGenerateDrl_SpecialCharactersInDescription() {
        RuleModel rule = new RuleModel();
        rule.setId("special-desc-rule");
        rule.setExpression("true");
        rule.setDescription("Description with \"quotes\" and\nnewlines");
        rule.setRuleType("CONDITIONAL");
        rule.setPriority(50);
        rule.setEnabled(true);
        
        String drl = drlGenerator.generateDrl(rule);
        
        assertNotNull(drl);
        assertTrue(drl.contains("rule \"special-desc-rule\""));
    }
    
    @Test
    void testGenerateDrl_NoPriority() {
        RuleModel rule = new RuleModel();
        rule.setId("no-priority-rule");
        rule.setExpression("true");
        rule.setDescription("Rule without priority");
        rule.setRuleType("CONDITIONAL");
        rule.setPriority(null);
        rule.setEnabled(true);
        
        String drl = drlGenerator.generateDrl(rule);
        
        assertNotNull(drl);
        assertTrue(drl.contains("rule \"no-priority-rule\""));
        // Should not have salience if priority is null
        assertFalse(drl.contains("salience null"));
    }
    
    @Test
    void testGenerateDrl_ZeroPriority() {
        RuleModel rule = new RuleModel();
        rule.setId("zero-priority-rule");
        rule.setExpression("true");
        rule.setDescription("Rule with zero priority");
        rule.setRuleType("CONDITIONAL");
        rule.setPriority(0);
        rule.setEnabled(true);
        
        String drl = drlGenerator.generateDrl(rule);
        
        assertNotNull(drl);
        assertTrue(drl.contains("rule \"zero-priority-rule\""));
    }
    
    @Test
    void testValidateDrlString_NullDrl() {
        assertThrows(IllegalArgumentException.class, () -> {
            drlGenerator.validateDrlString(null);
        });
    }
    
    @Test
    void testValidateDrlString_EmptyDrl() {
        assertThrows(IllegalArgumentException.class, () -> {
            drlGenerator.validateDrlString("");
        });
    }
    
    @Test
    void testGenerateDrl_ComplexRules() {
        List<RuleModel> rules = new ArrayList<>();
        
        // Rule 1: High value threshold
        RuleModel rule1 = new RuleModel();
        rule1.setId("high-value-threshold");
        rule1.setExpression("amount > 10000");
        rule1.setDescription("Threshold for high value orders");
        rule1.setRuleType("THRESHOLD");
        rule1.setPriority(60);
        rule1.setEnabled(true);
        rules.add(rule1);
        
        // Rule 2: Approval required
        RuleModel rule2 = new RuleModel();
        rule2.setId("manager-approval");
        rule2.setExpression("requiresApproval(\"manager\")");
        rule2.setDescription("Manager approval required for high value");
        rule2.setRuleType("APPROVAL");
        rule2.setPriority(70);
        rule2.setEnabled(true);
        rules.add(rule2);
        
        // Rule 3: Validation
        RuleModel rule3 = new RuleModel();
        rule3.setId("customer-validation");
        rule3.setExpression("validate(customer)");
        rule3.setDescription("Validate customer information");
        rule3.setRuleType("VALIDATION");
        rule3.setPriority(65);
        rule3.setEnabled(true);
        rules.add(rule3);
        
        String drl = drlGenerator.generateDrl(rules);
        
        assertNotNull(drl);
        assertTrue(drl.contains("rule \"high-value-threshold\""));
        assertTrue(drl.contains("rule \"manager-approval\""));
        assertTrue(drl.contains("rule \"customer-validation\""));
        assertTrue(drl.contains("salience 60"));
        assertTrue(drl.contains("salience 70"));
        assertTrue(drl.contains("salience 65"));
    }
    
    @Test
    void testGenerateDrl_IncludesImports() {
        RuleModel rule = new RuleModel();
        rule.setId("import-test");
        rule.setExpression("true");
        rule.setDescription("Test imports");
        rule.setRuleType("CONDITIONAL");
        rule.setPriority(50);
        rule.setEnabled(true);
        
        String drl = drlGenerator.generateDrl(rule);
        
        assertNotNull(drl);
        assertTrue(drl.contains("import java.util.*"));
        assertTrue(drl.contains("import java.time.*"));
    }
    
    @Test
    void testGenerateDrl_IncludesPrintln() {
        RuleModel rule = new RuleModel();
        rule.setId("println-test");
        rule.setExpression("true");
        rule.setDescription("Test println");
        rule.setRuleType("CONDITIONAL");
        rule.setPriority(50);
        rule.setEnabled(true);
        
        String drl = drlGenerator.generateDrl(rule);
        
        assertNotNull(drl);
        assertTrue(drl.contains("System.out.println"));
    }
}

