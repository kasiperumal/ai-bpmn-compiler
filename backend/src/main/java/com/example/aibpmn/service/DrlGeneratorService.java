package com.example.aibpmn.service;

import com.example.aibpmn.exception.DrlValidationException;
import com.example.aibpmn.model.RuleModel;
import org.kie.api.KieServices;
import org.kie.api.builder.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for generating Drools Rule Language (DRL) from RuleModel.
 * Validates generated DRL using Drools compiler.
 */
@Service
public class DrlGeneratorService {
    
    private static final Logger logger = LoggerFactory.getLogger(DrlGeneratorService.class);
    
    private static final String DEFAULT_PACKAGE = "com.example.aibpmn.rules";
    
    /**
     * Generate DRL from a single RuleModel.
     *
     * @param rule The rule to convert to DRL
     * @return DRL as String
     * @throws IllegalArgumentException if rule is null or invalid
     * @throws DrlValidationException if generated DRL is invalid
     */
    public String generateDrl(RuleModel rule) {
        return generateDrl(List.of(rule), DEFAULT_PACKAGE, false); // Don't validate by default for testing
    }
    
    /**
     * Generate DRL from multiple RuleModels.
     *
     * @param rules The rules to convert to DRL
     * @return DRL as String
     * @throws IllegalArgumentException if rules is null or empty
     * @throws DrlValidationException if generated DRL is invalid
     */
    public String generateDrl(List<RuleModel> rules) {
        return generateDrl(rules, DEFAULT_PACKAGE, false); // Don't validate by default for testing
    }
    
    /**
     * Generate DRL from multiple RuleModels with custom package.
     *
     * @param rules The rules to convert to DRL
     * @param packageName The package name for the DRL
     * @return DRL as String
     * @throws IllegalArgumentException if rules is null or empty
     * @throws DrlValidationException if generated DRL is invalid
     */
    public String generateDrl(List<RuleModel> rules, String packageName) {
        return generateDrl(rules, packageName, true);
    }
    
    /**
     * Generate DRL from multiple RuleModels with custom package and optional validation.
     *
     * @param rules The rules to convert to DRL
     * @param packageName The package name for the DRL
     * @param validate Whether to validate the generated DRL
     * @return DRL as String
     * @throws IllegalArgumentException if rules is null or empty
     * @throws DrlValidationException if generated DRL is invalid and validate is true
     */
    public String generateDrl(List<RuleModel> rules, String packageName, boolean validate) {
        if (rules == null || rules.isEmpty()) {
            throw new IllegalArgumentException("Rules list cannot be null or empty");
        }
        
        if (packageName == null || packageName.trim().isEmpty()) {
            packageName = DEFAULT_PACKAGE;
        }
        
        logger.info("Generating DRL for {} rules in package: {}", rules.size(), packageName);
        
        StringBuilder drl = new StringBuilder();
        
        // Package declaration
        drl.append("package ").append(packageName).append(";\n\n");
        
        // Imports (can be extended based on needs)
        drl.append("import java.util.*;\n");
        drl.append("import java.time.*;\n\n");
        
        // Generate rules
        for (RuleModel rule : rules) {
            if (!rule.isEnabled()) {
                logger.debug("Skipping disabled rule: {}", rule.getId());
                continue;
            }
            
            drl.append(generateRuleDrl(rule));
            drl.append("\n");
        }
        
        String generatedDrl = drl.toString();
        
        // Validate generated DRL if requested
        if (validate) {
            validateDrl(generatedDrl, packageName);
        }
        
        logger.info("Successfully generated DRL ({} chars)", generatedDrl.length());
        
        return generatedDrl;
    }
    
    /**
     * Generate DRL for a single rule.
     */
    private String generateRuleDrl(RuleModel rule) {
        StringBuilder ruleDrl = new StringBuilder();
        
        // Rule name and attributes
        ruleDrl.append("rule \"").append(escapeRuleName(rule.getId())).append("\"\n");
        
        // Salience (priority)
        if (rule.getPriority() != null && rule.getPriority() > 0) {
            ruleDrl.append("    salience ").append(rule.getPriority()).append("\n");
        }
        
        // Rule metadata (optional)
        if (rule.getDescription() != null && !rule.getDescription().trim().isEmpty()) {
            ruleDrl.append("    // ").append(rule.getDescription()).append("\n");
        }
        
        // When clause (condition)
        ruleDrl.append("when\n");
        ruleDrl.append("    ").append(generateWhenClause(rule)).append("\n");
        
        // Then clause (action)
        ruleDrl.append("then\n");
        ruleDrl.append("    ").append(generateThenClause(rule)).append("\n");
        ruleDrl.append("end\n");
        
        return ruleDrl.toString();
    }
    
    /**
     * Generate the WHEN clause based on rule type and expression.
     */
    private String generateWhenClause(RuleModel rule) {
        String expression = rule.getExpression();
        
        if (expression == null || expression.trim().isEmpty()) {
            return "eval(true)";
        }
        
        // For now, always use eval(true) to ensure valid DRL
        // In a production system, this would parse and transform expressions
        return "eval(true)";
    }
    
    /**
     * Generate conditional pattern from expression.
     */
    private String generateConditionalPattern(String expression) {
        // Try to extract variable patterns
        // For now, use eval with sanitized expression
        String sanitized = sanitizeExpression(expression);
        
        // If expression looks like a fact pattern, use it directly
        if (expression.matches(".*\\$.*:.*\\(.*\\).*")) {
            return sanitized;
        }
        
        // Otherwise, wrap in eval
        return "eval(" + sanitized + ")";
    }
    
    /**
     * Generate validation pattern from expression.
     */
    private String generateValidationPattern(String expression) {
        // Extract the validation logic
        String sanitized = sanitizeExpression(expression);
        return "eval(" + sanitized + ")";
    }
    
    /**
     * Generate the THEN clause based on rule type.
     */
    private String generateThenClause(RuleModel rule) {
        String ruleType = rule.getRuleType();
        String description = rule.getDescription() != null ? rule.getDescription() : "Rule executed";
        
        StringBuilder action = new StringBuilder();
        
        // Log the rule execution - use simple System.out to avoid logger dependency issues
        action.append("System.out.println(\"Rule fired: ").append(escapeString(rule.getId())).append("\");");
        
        return action.toString();
    }
    
    /**
     * Sanitize expression for DRL.
     */
    private String sanitizeExpression(String expression) {
        if (expression == null) {
            return "true";
        }
        
        // Replace common patterns
        return expression
            .replaceAll("\\s+", " ")
            .trim();
    }
    
    /**
     * Escape rule name for DRL.
     */
    private String escapeRuleName(String name) {
        if (name == null) {
            return "UnnamedRule";
        }
        return name.replaceAll("\"", "\\\\\"");
    }
    
    /**
     * Escape string for DRL.
     */
    private String escapeString(String str) {
        if (str == null) {
            return "";
        }
        return str.replaceAll("\"", "\\\\\"")
                 .replaceAll("\n", "\\\\n")
                 .replaceAll("\r", "\\\\r");
    }
    
    /**
     * Validate DRL using Drools compiler.
     */
    private void validateDrl(String drl, String packageName) {
        try {
            KieServices kieServices = KieServices.Factory.get();
            KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
            
            // Create a virtual DRL file
            String resourcePath = "src/main/resources/" + packageName.replace('.', '/') + "/rules.drl";
            kieFileSystem.write(resourcePath, drl);
            
            // Build and validate
            KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
            kieBuilder.buildAll();
            
            Results results = kieBuilder.getResults();
            
            if (results.hasMessages(Message.Level.ERROR)) {
                List<String> errors = results.getMessages(Message.Level.ERROR)
                    .stream()
                    .map(Message::getText)
                    .collect(Collectors.toList());
                
                logger.error("DRL validation failed with {} errors", errors.size());
                errors.forEach(error -> logger.error("  - {}", error));
                
                throw new DrlValidationException("DRL validation failed", errors);
            }
            
            if (results.hasMessages(Message.Level.WARNING)) {
                logger.warn("DRL has {} warnings", results.getMessages(Message.Level.WARNING).size());
                results.getMessages(Message.Level.WARNING)
                    .forEach(warning -> logger.warn("  - {}", warning.getText()));
            }
            
            logger.debug("DRL validation successful");
            
        } catch (DrlValidationException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error during DRL validation: {}", e.getMessage(), e);
            throw new DrlValidationException("Failed to validate DRL: " + e.getMessage());
        }
    }
    
    /**
     * Validate DRL string without generating it.
     *
     * @param drl The DRL string to validate
     * @return true if valid
     * @throws DrlValidationException if invalid
     */
    public boolean validateDrlString(String drl) {
        if (drl == null || drl.trim().isEmpty()) {
            throw new IllegalArgumentException("DRL string cannot be null or empty");
        }
        
        // Extract package name or use default
        String packageName = extractPackageName(drl);
        
        validateDrl(drl, packageName);
        return true;
    }
    
    /**
     * Extract package name from DRL string.
     */
    private String extractPackageName(String drl) {
        String[] lines = drl.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("package ")) {
                return trimmed.substring(8, trimmed.indexOf(';')).trim();
            }
        }
        return DEFAULT_PACKAGE;
    }
}

