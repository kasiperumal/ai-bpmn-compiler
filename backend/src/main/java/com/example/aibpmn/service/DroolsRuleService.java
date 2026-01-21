package com.example.aibpmn.service;

import com.example.aibpmn.model.RuleSet;
import com.example.aibpmn.model.RuleStatus;
import com.example.aibpmn.repository.RuleSetRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * ═══════════════════════════════════════════════════════════════════════
 * DROOLS RULE SERVICE - DRL Generation and Java Model Management
 * ═══════════════════════════════════════════════════════════════════════
 * 
 * Responsibilities:
 * 1. Generate DRL (Drools Rule Language) from BusinessRuleTask metadata
 * 2. Create Java model classes for fact objects
 * 3. Validate DRL syntax using Drools compiler
 * 4. Attach rules to BusinessRuleTasks in BPMN
 * 5. Manage rule lifecycle (draft → validated → active)
 * 
 * ═══════════════════════════════════════════════════════════════════════
 */
@Service
public class DroolsRuleService {
    
    private static final Logger logger = LoggerFactory.getLogger(DroolsRuleService.class);
    
    private final RuleSetRepository ruleSetRepository;
    private final AiClient aiClient;
    private final ObjectMapper objectMapper;
    
    public DroolsRuleService(
            RuleSetRepository ruleSetRepository,
            AiClient aiClient,
            ObjectMapper objectMapper) {
        this.ruleSetRepository = ruleSetRepository;
        this.aiClient = aiClient;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Generate Drools rule from natural language description via AI
     * 
     * @param ruleName The name of the rule
     * @param ruleDescription Natural language description of the rule logic
     * @param processId The process this rule belongs to
     * @param taskId The BusinessRuleTask ID this rule is for
     * @return Generated RuleSet with DRL
     */
    public RuleSet generateRuleFromDescription(
            String ruleName,
            String ruleDescription,
            String processId,
            String taskId) {
        
        logger.info("Generating Drools rule '{}' for task {}", ruleName, taskId);
        
        try {
            // 1. Use AI to generate DRL and identify required Java models
            String aiPrompt = createRuleGenerationPrompt(ruleName, ruleDescription);
            String aiResponse = aiClient.generateFromText(aiPrompt);
            
            // 2. Parse AI response
            RuleGenerationResult genResult = parseRuleGenerationResponse(aiResponse);
            
            // 3. Validate DRL syntax
            ValidationResult validation = validateDrl(genResult.drl);
            
            if (!validation.isValid()) {
                String errors = String.join(", ", validation.getErrors());
                throw new IllegalStateException("Generated DRL is invalid: " + errors);
            }
            
            // 4. Create RuleSet entity
            RuleSet ruleSet = new RuleSet();
            ruleSet.setId("rule-" + UUID.randomUUID().toString().substring(0, 8));
            ruleSet.setName(ruleName);
            ruleSet.setDescription(ruleDescription);
            ruleSet.setDrl(genResult.drl);
            ruleSet.setJavaModelsUsed(genResult.javaModelsUsed);
            ruleSet.setJavaModelsCreated(genResult.javaModelsCreated);
            ruleSet.setExplanation(genResult.explanation);
            ruleSet.setProcessId(processId);
            ruleSet.setTaskId(taskId);
            ruleSet.setStatus(RuleStatus.VALIDATED);
            ruleSet.setCreatedAt(LocalDateTime.now());
            ruleSet.setUpdatedAt(LocalDateTime.now());
            
            // 5. Save to database
            ruleSetRepository.save(ruleSet);
            
            logger.info("Successfully generated and saved rule: {}", ruleSet.getId());
            
            return ruleSet;
            
        } catch (Exception e) {
            logger.error("Failed to generate rule from description", e);
            throw new RuntimeException("Failed to generate rule: " + e.getMessage(), e);
        }
    }
    
    /**
     * Create AI prompt for generating Drools rules
     */
    private String createRuleGenerationPrompt(String ruleName, String ruleDescription) {
        return String.format("""
            ═══════════════════════════════════════════════════════════════════════
            TASK: Generate Drools Rule (DRL) from Natural Language
            ═══════════════════════════════════════════════════════════════════════
            
            RULE NAME: %s
            RULE DESCRIPTION: %s
            
            YOUR TASK:
            Generate a complete Drools rule in DRL (Drools Rule Language) format.
            
            REQUIREMENTS:
            1. Valid DRL syntax (Drools 7+ compatible)
            2. Include package declaration
            3. Import necessary Java classes (create custom fact classes if needed)
            4. Define rule with WHEN/THEN clauses
            5. Use proper Drools patterns and conditions
            
            STRUCTURE TEMPLATE:
            ```drl
            package com.example.aibpmn.rules;
            
            import com.example.aibpmn.facts.YourFactClass;
            
            rule "%s"
                salience 100
                when
                    // Your conditions here
                    $fact : YourFactClass( /* conditions */ )
                then
                    // Your actions here
                    System.out.println("Rule fired!");
                end
            ```
            
            FACT CLASS GUIDELINES:
            - If you need custom fact classes (e.g., Order, Customer), define them in the response
            - Use simple Java POJOs with getters/setters
            - Example:
            ```java
            package com.example.aibpmn.facts;
            
            public class Order {
                private double amount;
                private String status;
                
                // getters and setters
            }
            ```
            
            COMMON PATTERNS:
            - Threshold checks: YourFact(amount > 1000)
            - String matching: YourFact(status == "PENDING")
            - Combinations: YourFact(amount > 1000, status == "APPROVED")
            - NOT operator: not YourFact(status == "REJECTED")
            
            ACTIONS (THEN clause):
            - Modify facts: modify($fact) { setStatus("APPROVED") }
            - Insert new facts: insert(new AnotherFact());
            - Logging: System.out.println("...");
            - Delete facts: delete($fact);
            
            OUTPUT FORMAT (JSON):
            {
              "drl": "<complete-drl-code>",
              "javaModelsUsed": ["com.example.aibpmn.facts.Order"],
              "javaModelsCreated": [
                {
                  "className": "Order",
                  "package": "com.example.aibpmn.facts",
                  "fields": [
                    {"name": "amount", "type": "double"},
                    {"name": "status", "type": "String"}
                  ]
                }
              ],
              "explanation": "This rule checks if an order amount exceeds $1000 and automatically approves it"
            }
            
            CRITICAL:
            - Output ONLY the JSON (no markdown, no explanation outside JSON)
            - DRL must be syntactically valid
            - Keep it simple and focused on the described logic
            - If the description is vague, make reasonable assumptions
            
            Now generate the Drools rule for the given description.
            """, ruleName, ruleDescription, ruleName);
    }
    
    /**
     * Parse AI response containing DRL and Java model information
     */
    private RuleGenerationResult parseRuleGenerationResponse(String aiResponse) throws Exception {
        // Clean markdown
        String cleanJson = aiResponse.trim();
        if (cleanJson.startsWith("```json")) {
            cleanJson = cleanJson.substring(7);
        }
        if (cleanJson.startsWith("```")) {
            cleanJson = cleanJson.substring(3);
        }
        if (cleanJson.endsWith("```")) {
            cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
        }
        cleanJson = cleanJson.trim();
        
        JsonNode root = objectMapper.readTree(cleanJson);
        
        RuleGenerationResult result = new RuleGenerationResult();
        
        // Extract DRL
        if (!root.has("drl")) {
            throw new IllegalArgumentException("AI response missing 'drl' field");
        }
        result.drl = root.get("drl").asText();
        
        // Extract Java models used
        if (root.has("javaModelsUsed")) {
            for (JsonNode model : root.get("javaModelsUsed")) {
                result.javaModelsUsed.add(model.asText());
            }
        }
        
        // Extract Java models created (we'll store the class names)
        if (root.has("javaModelsCreated")) {
            for (JsonNode model : root.get("javaModelsCreated")) {
                if (model.has("className")) {
                    result.javaModelsCreated.add(model.get("className").asText());
                }
            }
        }
        
        // Extract explanation
        if (root.has("explanation")) {
            result.explanation = root.get("explanation").asText();
        }
        
        return result;
    }
    
    /**
     * Validate DRL syntax using Drools KieServices
     */
    public ValidationResult validateDrl(String drl) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        try {
            // Use KieServices to validate DRL
            KieServices kieServices = KieServices.Factory.get();
            KieFileSystem kfs = kieServices.newKieFileSystem();
            
            // Add DRL to virtual file system
            String drlPath = "src/main/resources/rules/validation.drl";
            kfs.write(drlPath, drl);
            
            // Build and check for errors
            KieBuilder kieBuilder = kieServices.newKieBuilder(kfs);
            Results results = kieBuilder.buildAll().getResults();
            
            if (results.hasMessages(Message.Level.ERROR)) {
                for (Message message : results.getMessages(Message.Level.ERROR)) {
                    errors.add(message.getText());
                }
            }
            
            if (results.hasMessages(Message.Level.WARNING)) {
                for (Message message : results.getMessages(Message.Level.WARNING)) {
                    warnings.add(message.getText());
                }
            }
            
            if (errors.isEmpty()) {
                logger.info("DRL validation passed");
                return ValidationResult.valid(warnings);
            } else {
                logger.warn("DRL validation failed with {} errors", errors.size());
                return ValidationResult.invalid(errors, warnings);
            }
            
        } catch (Exception e) {
            logger.error("Unexpected error during DRL validation", e);
            errors.add("Validation error: " + e.getMessage());
            return ValidationResult.invalid(errors, warnings);
        }
    }
    
    /**
     * Get all rules for a process
     */
    public List<RuleSet> getRulesForProcess(String processId) {
        return ruleSetRepository.findByProcessId(processId);
    }
    
    /**
     * Get rule for a specific BusinessRuleTask
     */
    public Optional<RuleSet> getRuleForTask(String taskId) {
        return ruleSetRepository.findByTaskId(taskId);
    }
    
    /**
     * Activate a rule (make it executable)
     */
    public RuleSet activateRule(String ruleId) {
        RuleSet ruleSet = ruleSetRepository.findById(ruleId)
            .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + ruleId));
        
        // Validate before activation
        ValidationResult validation = validateDrl(ruleSet.getDrl());
        if (!validation.isValid()) {
            throw new IllegalStateException("Cannot activate invalid rule: " + 
                String.join(", ", validation.getErrors()));
        }
        
        ruleSet.setStatus(RuleStatus.ACTIVE);
        ruleSet.setUpdatedAt(LocalDateTime.now());
        
        return ruleSetRepository.save(ruleSet);
    }
    
    /**
     * Internal class for AI rule generation result
     */
    private static class RuleGenerationResult {
        String drl;
        List<String> javaModelsUsed = new ArrayList<>();
        List<String> javaModelsCreated = new ArrayList<>();
        String explanation;
    }
}
