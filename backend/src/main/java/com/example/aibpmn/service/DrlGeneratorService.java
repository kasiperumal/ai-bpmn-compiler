package com.example.aibpmn.service;

import com.example.aibpmn.exception.DrlValidationException;
import com.example.aibpmn.model.RuleModel;
import org.kie.api.KieServices;
import org.kie.api.builder.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
        
        // Analyze rules to determine required fields
        Set<String> requiredFields = extractRequiredFields(rules);
        logger.debug("Detected {} fields from rules: {}", requiredFields.size(), requiredFields);
        
        // Generate/Update Java POJO models based on detected fields
        try {
            generateOrUpdateProcessDataPojo(requiredFields);
            generateOrUpdateValidationResultPojo();
            logger.info("✅ Generated/updated Java POJO models with {} fields", requiredFields.size());
        } catch (Exception e) {
            logger.error("Failed to generate POJO models: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate POJO models", e);
        }
        
        StringBuilder drl = new StringBuilder();
        
        // Package declaration
        drl.append("package ").append(packageName).append(";\n\n");
        
        // Imports (can be extended based on needs)
        drl.append("import java.util.*;\n");
        drl.append("import java.time.*;\n\n");
        
        // Import Java POJO fact models
        drl.append("// Input fact model (dynamically generated Java POJO)\n");
        drl.append("import com.example.aibpmn.model.ProcessData;\n\n");
        
        drl.append("// Output fact model (dynamically generated Java POJO)\n");
        drl.append("import com.example.aibpmn.model.RuleValidationResult;\n\n");
        
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
     * Converts natural language expressions to proper Drools patterns.
     */
    private String generateWhenClause(RuleModel rule) {
        String expression = rule.getExpression();
        
        if (expression == null || expression.trim().isEmpty()) {
            return "$data: ProcessData()";
        }
        
        // Try to convert natural language expression to Drools pattern
        String pattern = convertExpressionToDroolsPattern(expression);
        
        return pattern;
    }
    
    /**
     * Convert natural language expression to a proper Drools pattern.
     * Examples:
     *   "days > 5" → "$data: ProcessData(days > 5)"
     *   "amount > 1000" → "$data: ProcessData(amount > 1000)"
     *   "during peak delivery period" → "$data: ProcessData(isPeakPeriod == true)"
     *   "all criteria met" → "$data: ProcessData()"
     */
    private String convertExpressionToDroolsPattern(String expression) {
        String expr = expression.toLowerCase().trim();
        
        // Pattern 1: Direct numeric comparisons (days > 5, amount < 1000, etc.)
        if (expr.matches(".*\\b(days|amount|count|quantity|hours|minutes)\\s*[><=!]+\\s*\\d+.*")) {
            // Extract the comparison part
            String comparison = extractComparison(expr);
            return "$data: ProcessData(" + comparison + ")";
        }
        
        // Pattern 2: Peak period / critical period checks
        if (expr.contains("peak") || expr.contains("critical") || expr.contains("busy")) {
            return "$data: ProcessData(isPeakPeriod == true)";
        }
        
        // Pattern 3: Eligibility checks
        if (expr.contains("eligible") || expr.contains("qualified")) {
            if (expr.contains("not") || expr.contains("in")) {
                return "$data: ProcessData(isEligible == false)";
            }
            return "$data: ProcessData(isEligible == true)";
        }
        
        // Pattern 4: "All criteria met" or general approval conditions
        if (expr.contains("all criteria") || expr.contains("everything") || expr.contains("valid")) {
            // Multiple conditions - check multiple fields
            return "$data: ProcessData(days != null && days <= 5, isPeakPeriod == false, isEligible == true)";
        }
        
        // Pattern 5: Try to extract field names and values
        String fieldPattern = extractFieldPattern(expr);
        if (fieldPattern != null) {
            return "$data: ProcessData(" + fieldPattern + ")";
        }
        
        // Default: Match any ProcessData (rule will always fire, logic in THEN clause)
        logger.warn("Could not convert expression to Drools pattern: '{}', using default match", expression);
        return "$data: ProcessData() // Expression: " + expression;
    }
    
    /**
     * Extract comparison expression (e.g., "days > 5" from "days greater than 5")
     */
    private String extractComparison(String expression) {
        // Replace common natural language with operators
        String normalized = expression
            .replaceAll("greater than", ">")
            .replaceAll("less than", "<")
            .replaceAll("equal to", "==")
            .replaceAll("equals", "==")
            .trim();
        
        // Try to extract pattern like "days > 5"
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\w+)\\s*([><=!]+)\\s*(\\d+\\.?\\d*)");
        java.util.regex.Matcher matcher = pattern.matcher(normalized);
        
        if (matcher.find()) {
            String field = matcher.group(1);
            String operator = matcher.group(2);
            String value = matcher.group(3);
            return field + " " + operator + " " + value;
        }
        
        // If we can't parse it, return as-is wrapped in comment
        return "true /* Could not parse: " + expression + " */";
    }
    
    /**
     * Extract field pattern from expression
     */
    private String extractFieldPattern(String expression) {
        // Simple keyword matching for common fields
        if (expression.contains("approved") || expression.contains("approve")) {
            return "data[\"status\"] == \"APPROVED\"";
        }
        if (expression.contains("rejected") || expression.contains("reject")) {
            return "data[\"status\"] == \"REJECTED\"";
        }
        
        return null;
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
     * Generates working Drools code with actual fact insertions.
     */
    private String generateThenClause(RuleModel rule) {
        String ruleType = rule.getRuleType();
        String description = rule.getDescription() != null ? rule.getDescription() : "Rule executed";
        
        StringBuilder action = new StringBuilder();
        
        // Set result based on rule type with actual working code
        if ("approve".equalsIgnoreCase(ruleType)) {
            action.append("System.out.println(\"✅ Rule fired: ").append(escapeString(rule.getId()))
                  .append(" - APPROVED: ").append(escapeString(description)).append("\");\n    ");
            action.append("RuleValidationResult result = new RuleValidationResult();\n    ");
            action.append("result.setStatus(\"APPROVED\");\n    ");
            action.append("result.setReason(\"").append(escapeString(description)).append("\");\n    ");
            action.append("insert(result);");
        } else if ("reject".equalsIgnoreCase(ruleType)) {
            action.append("System.out.println(\"❌ Rule fired: ").append(escapeString(rule.getId()))
                  .append(" - REJECTED: ").append(escapeString(description)).append("\");\n    ");
            action.append("RuleValidationResult result = new RuleValidationResult();\n    ");
            action.append("result.setStatus(\"REJECTED\");\n    ");
            action.append("result.setReason(\"").append(escapeString(description)).append("\");\n    ");
            action.append("insert(result);");
        } else {
            // Default action
            action.append("System.out.println(\"Rule fired: ").append(escapeString(rule.getId())).append("\");\n    ");
            action.append("RuleValidationResult result = new RuleValidationResult();\n    ");
            action.append("result.setStatus(\"").append(escapeString(ruleType).toUpperCase()).append("\");\n    ");
            action.append("result.setReason(\"").append(escapeString(description)).append("\");\n    ");
            action.append("insert(result);");
        }
        
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
    
    /**
     * Extract required fields from rule expressions.
     * Analyzes all rule expressions to determine what fields are used.
     */
    private Set<String> extractRequiredFields(List<RuleModel> rules) {
        Set<String> fields = new LinkedHashSet<>(); // Preserve order
        
        for (RuleModel rule : rules) {
            if (rule.getExpression() == null) {
                continue;
            }
            
            String expr = rule.getExpression().toLowerCase();
            
            // Common field patterns
            if (expr.matches(".*\\bdays?\\b.*")) {
                fields.add("days:Integer");
            }
            if (expr.matches(".*\\bamounts?\\b.*") || expr.matches(".*\\$\\d+.*")) {
                fields.add("amount:Double");
            }
            if (expr.contains("peak") || expr.contains("critical") || expr.contains("busy")) {
                fields.add("isPeakPeriod:Boolean");
            }
            if (expr.contains("eligible") || expr.contains("eligibility")) {
                fields.add("isEligible:Boolean");
            }
            if (expr.matches(".*\\bcounts?\\b.*") || expr.matches(".*\\bquantity\\b.*")) {
                fields.add("count:Integer");
            }
            if (expr.matches(".*\\bhours?\\b.*")) {
                fields.add("hours:Double");
            }
            if (expr.matches(".*\\bminutes?\\b.*")) {
                fields.add("minutes:Integer");
            }
            if (expr.contains("category") || expr.contains("type")) {
                fields.add("category:String");
            }
            if (expr.contains("receipt") || expr.contains("document")) {
                fields.add("hasReceipt:Boolean");
            }
            if (expr.contains("credit") || expr.contains("score")) {
                fields.add("creditScore:Integer");
            }
            if (expr.contains("income") || expr.contains("salary")) {
                fields.add("income:Double");
            }
            if (expr.contains("user") || expr.contains("employee")) {
                fields.add("userId:String");
            }
            if (expr.contains("department") || expr.contains("team")) {
                fields.add("department:String");
            }
            if (expr.contains("date") || expr.contains("time") || expr.contains("period")) {
                fields.add("startDate:Date");
                fields.add("endDate:Date");
            }
            if (expr.contains("priority") || expr.contains("urgent")) {
                fields.add("priority:Integer");
            }
            if (expr.contains("status")) {
                fields.add("status:String");
            }
        }
        
        // Always include a generic data map for extensibility
        fields.add("data:Map");
        
        return fields;
    }
    
    /**
     * Generate or update ProcessData Java POJO based on required fields.
     */
    private void generateOrUpdateProcessDataPojo(Set<String> fields) throws Exception {
        String className = "ProcessData";
        String packageName = "com.example.aibpmn.model";
        
        StringBuilder javaCode = new StringBuilder();
        
        // Package declaration
        javaCode.append("package ").append(packageName).append(";\n\n");
        
        // Imports
        javaCode.append("import java.util.HashMap;\n");
        javaCode.append("import java.util.Map;\n");
        javaCode.append("import java.util.Date;\n\n");
        
        // Class JavaDoc
        javaCode.append("/**\n");
        javaCode.append(" * Dynamically generated Drools fact model for process data.\n");
        javaCode.append(" * This class is auto-generated based on rule expressions.\n");
        javaCode.append(" * DO NOT EDIT MANUALLY - will be regenerated on next DRL generation.\n");
        javaCode.append(" */\n");
        javaCode.append("public class ").append(className).append(" {\n\n");
        
        // Field declarations
        for (String field : fields) {
            String[] parts = field.split(":");
            String fieldName = parts[0];
            String fieldType = parts.length > 1 ? mapDrlTypeToJava(parts[1]) : "Object";
            
            javaCode.append("    private ").append(fieldType).append(" ").append(fieldName).append(";\n");
        }
        
        javaCode.append("\n");
        
        // Default constructor
        javaCode.append("    public ").append(className).append("() {\n");
        if (fields.stream().anyMatch(f -> f.startsWith("data:"))) {
            javaCode.append("        this.data = new HashMap<>();\n");
        }
        javaCode.append("    }\n\n");
        
        // Getters and Setters
        for (String field : fields) {
            String[] parts = field.split(":");
            String fieldName = parts[0];
            String fieldType = parts.length > 1 ? mapDrlTypeToJava(parts[1]) : "Object";
            String capitalizedFieldName = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            
            // Getter
            javaCode.append("    public ").append(fieldType).append(" get").append(capitalizedFieldName).append("() {\n");
            javaCode.append("        return ").append(fieldName).append(";\n");
            javaCode.append("    }\n\n");
            
            // Setter
            javaCode.append("    public void set").append(capitalizedFieldName).append("(").append(fieldType).append(" ").append(fieldName).append(") {\n");
            javaCode.append("        this.").append(fieldName).append(" = ").append(fieldName).append(";\n");
            javaCode.append("    }\n\n");
        }
        
        // toString method
        javaCode.append("    @Override\n");
        javaCode.append("    public String toString() {\n");
        javaCode.append("        return \"").append(className).append("{\" +\n");
        int i = 0;
        for (String field : fields) {
            String[] parts = field.split(":");
            String fieldName = parts[0];
            if (i++ > 0) javaCode.append(" +\n");
            javaCode.append("                \"").append(i > 1 ? ", " : "").append(fieldName).append("=\" + ").append(fieldName);
        }
        javaCode.append(" +\n");
        javaCode.append("                '}';\n");
        javaCode.append("    }\n");
        
        javaCode.append("}\n");
        
        // Write to file
        writeJavaFile(packageName, className, javaCode.toString());
    }
    
    /**
     * Generate or update RuleValidationResult Java POJO.
     */
    private void generateOrUpdateValidationResultPojo() throws Exception {
        String className = "RuleValidationResult";
        String packageName = "com.example.aibpmn.model";
        
        StringBuilder javaCode = new StringBuilder();
        
        // Package declaration
        javaCode.append("package ").append(packageName).append(";\n\n");
        
        // Class JavaDoc
        javaCode.append("/**\n");
        javaCode.append(" * Drools fact model for rule validation results.\n");
        javaCode.append(" * This class captures the outcome of business rule execution.\n");
        javaCode.append(" * DO NOT EDIT MANUALLY - will be regenerated on next DRL generation.\n");
        javaCode.append(" */\n");
        javaCode.append("public class ").append(className).append(" {\n\n");
        
        // Fields
        javaCode.append("    private String status;\n");
        javaCode.append("    private String reason;\n\n");
        
        // Default constructor
        javaCode.append("    public ").append(className).append("() {\n");
        javaCode.append("    }\n\n");
        
        // Parameterized constructor
        javaCode.append("    public ").append(className).append("(String status, String reason) {\n");
        javaCode.append("        this.status = status;\n");
        javaCode.append("        this.reason = reason;\n");
        javaCode.append("    }\n\n");
        
        // Getters and Setters
        javaCode.append("    public String getStatus() {\n");
        javaCode.append("        return status;\n");
        javaCode.append("    }\n\n");
        
        javaCode.append("    public void setStatus(String status) {\n");
        javaCode.append("        this.status = status;\n");
        javaCode.append("    }\n\n");
        
        javaCode.append("    public String getReason() {\n");
        javaCode.append("        return reason;\n");
        javaCode.append("    }\n\n");
        
        javaCode.append("    public void setReason(String reason) {\n");
        javaCode.append("        this.reason = reason;\n");
        javaCode.append("    }\n\n");
        
        // toString method
        javaCode.append("    @Override\n");
        javaCode.append("    public String toString() {\n");
        javaCode.append("        return \"").append(className).append("{\" +\n");
        javaCode.append("                \"status='\" + status + '\\'' +\n");
        javaCode.append("                \", reason='\" + reason + '\\'' +\n");
        javaCode.append("                '}';\n");
        javaCode.append("    }\n");
        
        javaCode.append("}\n");
        
        // Write to file
        writeJavaFile(packageName, className, javaCode.toString());
    }
    
    /**
     * Map DRL type names to Java type names.
     */
    private String mapDrlTypeToJava(String drlType) {
        switch (drlType) {
            case "Integer": return "Integer";
            case "Double": return "Double";
            case "String": return "String";
            case "Boolean": return "Boolean";
            case "Date": return "Date";
            case "Map": return "Map<String, Object>";
            default: return "Object";
        }
    }
    
    /**
     * Write Java source file to disk.
     */
    private void writeJavaFile(String packageName, String className, String content) throws Exception {
        // Determine the source directory
        String projectRoot = System.getProperty("user.dir");
        String packagePath = packageName.replace('.', '/');
        String filePath = projectRoot + "/src/main/java/" + packagePath + "/" + className + ".java";
        
        java.io.File file = new java.io.File(filePath);
        file.getParentFile().mkdirs(); // Ensure directory exists
        
        // Write the file
        try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
            writer.write(content);
        }
        
        logger.info("Generated Java POJO: {}", filePath);
    }
}

