package com.example.aibpmn.service;

import com.example.aibpmn.dto.ReasoningResult;
import com.example.aibpmn.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for reasoning over process descriptions and extracting structured BPMN elements.
 * Converts natural language process descriptions into ProcessNodes, ProcessEdges, RuleModels, and Explanations.
 * 
 * Uses configured AI provider (OpenAI GPT-4o or Google Gemini 2.0).
 */
@Service
public class ProcessReasonerService {
    
    private static final Logger logger = LoggerFactory.getLogger(ProcessReasonerService.class);
    
    private final AiClient aiClient;
    private final ObjectMapper objectMapper;
    
    public ProcessReasonerService(AiClient aiClient, ObjectMapper objectMapper) {
        this.aiClient = aiClient;
        this.objectMapper = objectMapper;
        logger.info("ProcessReasonerService initialized with AI provider: {}", aiClient.getProviderName());
    }
    
    /**
     * Reason over a process description to extract structured BPMN elements.
     *
     * @param processDescription The natural language process description
     * @return ReasoningResult containing nodes, edges, rules, explanations, and clarification flags
     * @throws IllegalArgumentException if description is null or empty
     * @throws RuntimeException if AI reasoning fails or JSON parsing fails
     */
    public ReasoningResult reasonOverDescription(String processDescription) {
        if (processDescription == null || processDescription.trim().isEmpty()) {
            throw new IllegalArgumentException("Process description cannot be null or empty");
        }
        
        logger.info("Starting reasoning over process description (length: {} chars)",
            processDescription.length());
        
        try {
            // 1. Create prompt for AI to extract structured elements
            String prompt = createReasoningPrompt(processDescription);
            
            // 2. Call AI provider to analyze and structure the description
            logger.debug("Using AI provider: {}", aiClient.getProviderName());
            String jsonResponse = aiClient.generateFromText(prompt);
            
            logger.debug("Received JSON response from Gemini (length: {} chars)",
                jsonResponse.length());
            
            // 3. Parse JSON response into ReasoningResult
            ReasoningResult result = parseReasoningResponse(jsonResponse);
            
            logger.info("Reasoning complete: {}", result);
            
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to reason over process description", e);
            throw new RuntimeException("Failed to reason over description: " + e.getMessage(), e);
        }
    }
    
    /**
     * Create a detailed prompt for Gemini to extract BPMN elements from description.
     *
     * @param processDescription The process description to analyze
     * @return The prompt string
     */
    private String createReasoningPrompt(String processDescription) {
        return String.format("""
            ═══════════════════════════════════════════════════════════════════════
            TASK: Convert Process Description to BPMN Moddle JSON
            ═══════════════════════════════════════════════════════════════════════
            
            PROCESS DESCRIPTION:
            %s
            
            YOUR TASK:
            Generate a complete BPMN 2.0 Moddle JSON (bpmn-js compatible format).
            
            CRITICAL REQUIREMENTS:
            1. Output MUST be valid BPMN Moddle JSON (used by bpmn-js library)
            2. NO position/layout coordinates (x, y, waypoints) - frontend will handle layout
            3. Root element MUST be bpmn:Definitions with one bpmn:Process
            4. All elements must have unique IDs
            5. SequenceFlows must reference valid sourceRef/targetRef IDs
            6. **MANDATORY**: ALL SequenceFlows from Gateways MUST have descriptive "name" attributes
               (e.g., "Approved", "Rejected", "Yes", "No", ">$1000", "Valid")
               These names are displayed on the diagram arrows and are critical for understanding!
            
            ELEMENT TYPES TO USE:
            - bpmn:StartEvent (circle) - where process begins
            - bpmn:EndEvent (thick circle) - where process ends
            - bpmn:Task - generic work step
            - bpmn:UserTask - human-performed work
            - bpmn:ServiceTask - automated/API work
            - bpmn:BusinessRuleTask - decision/rule evaluation (use this for rule-based tasks)
            - bpmn:ExclusiveGateway (diamond with X) - one path chosen
            - bpmn:ParallelGateway (diamond with +) - all paths executed
            - bpmn:SequenceFlow (arrow) - connects elements
            
            ═══════════════════════════════════════════════════════════════════════
            CRITICAL RULE - BUSINESS LOGIC vs PROCESS FLOW CONTROL
            ═══════════════════════════════════════════════════════════════════
            
            WHEN TO USE BUSINESSRULETASK vs GATEWAY:
            
            🎯 USE BUSINESSRULETASK FOR:
            - Complex business logic with MULTIPLE related conditions
            - Validation rules, eligibility checks, risk assessments
            - Calculations and data-driven decisions
            - Any time the description mentions 3+ conditions that are related
            
            Examples of BusinessRuleTask scenarios:
            ✅ "if leave > 5 days reject, if during peak period reject, if not eligible reject"
               → ONE BusinessRuleTask: "Validate Leave Request" (contains ALL rules in DRL)
            ✅ "check credit score, verify income, assess debt ratio, validate employment"
               → ONE BusinessRuleTask: "Credit Assessment" (contains ALL validation rules)
            ✅ "if amount > $5000, if customer is VIP, if inventory available"
               → ONE BusinessRuleTask: "Order Validation" (contains ALL checks)
            
            🔀 USE GATEWAY FOR:
            - Simple process flow routing based on an OUTCOME or STATUS
            - Choosing between process paths (department selection, priority routing)
            - Synchronization points for parallel flows
            - Single routing decision after a task completes
            
            Examples of Gateway scenarios:
            ✅ "if approved, send to HR; if rejected, notify employee"
               → ONE Gateway: "Approval Result?" (routes based on approval outcome)
            ✅ "if amount > $1000, route to manager; otherwise auto-approve"
               → ONE Gateway: "Amount Check" (simple threshold routing)
            ✅ "which department handles this request?"
               → ONE Gateway: "Department Selection" (process routing)
            
            ═══════════════════════════════════════════════════════════════════
            CONSOLIDATION RULE - AVOID GATEWAY EXPLOSION
            ═══════════════════════════════════════════════════════════════════
            
            ❌ WRONG PATTERN (Multiple Gateways for Related Conditions):
            [Task] → [Gateway1: days<=5?] → [Gateway2: peak period?] → [Gateway3: eligible?]
                           ↓                      ↓                           ↓
                      [Reject]               [Reject]                    [Reject]
            
            ✅ RIGHT PATTERN (One BusinessRuleTask + One Gateway):
            [Task] → [BusinessRuleTask: "Validate Leave"] → [Gateway: result?] → [Approve/Reject]
                            ↑                                        ↓
                    (Contains ALL rules:                     (Routes based on
                     - days > 5 → reject                      validation result)
                     - peak period → reject
                     - not eligible → reject)
            
            SCORING GUIDE:
            - Description has 1 condition → Consider Gateway (if it's for routing)
            - Description has 2-3 related conditions → USE BUSINESSRULETASK
            - Description has 4+ related conditions → DEFINITELY USE BUSINESSRULETASK
            - Description mentions "rules", "validation", "eligibility", "criteria" → USE BUSINESSRULETASK
            
            ═══════════════════════════════════════════════════════════════════
            
            GATEWAY USAGE (for Process Routing):
            - ExclusiveGateway: when only ONE path is chosen (IF/ELSE routing)
            - ParallelGateway: when ALL branches happen simultaneously (AND splitting)
            - Always include a default flow from exclusive gateways
            - **CRITICAL**: Every outgoing flow from a gateway MUST have a "name" attribute
              describing the route (e.g., "Approved", "Rejected", "High Priority")
            
            SEQUENCE FLOW RULES (CRITICAL):
            - ALL flows from gateways MUST have descriptive names (e.g., "Approved", "Rejected", ">$1000", "Valid")
            - Regular flows: name them with ultra-short labels (1-2 words, max 10 chars)
            - Conditional flows: ALWAYS include both "name" AND "conditionExpression"
            - Default flows: MUST have "name" attribute + mark with 'default' property on gateway
            - Flow names should describe the condition or outcome (e.g., "Yes", "No", "Approved", "Rejected")
            
            BUSINESS RULE TASK IMPLEMENTATION:
            - BusinessRuleTask encapsulates complex business logic in DRL rules
            - The task evaluates ALL rules and returns a single result/status
            - Gateway AFTER the BusinessRuleTask routes based on the result
            - Do NOT create explicit bpmn:BusinessRule elements (those don't exist in BPMN 2.0)
            - Rules are referenced by the BusinessRuleTask via implementation property
            
            MANDATORY STRUCTURE:
            {
              "$type": "bpmn:Definitions",
              "id": "Definitions_<unique-id>",
              "targetNamespace": "http://bpmn.io/schema/bpmn",
              "rootElements": [
                {
                  "$type": "bpmn:Process",
                  "id": "Process_<unique-id>",
                  "name": "<process-name>",
                  "isExecutable": true,
                  "flowElements": [
                    {
                      "$type": "bpmn:StartEvent",
                      "id": "StartEvent_<unique-id>",
                      "name": "<event-name>",
                      "outgoing": ["Flow_<id>"]
                    },
                    {
                      "$type": "bpmn:Task",
                      "id": "Task_<unique-id>",
                      "name": "<task-name>",
                      "incoming": ["Flow_<id>"],
                      "outgoing": ["Flow_<id>"]
                    },
                    {
                      "$type": "bpmn:ExclusiveGateway",
                      "id": "Gateway_<unique-id>",
                      "name": "<decision-name>",
                      "incoming": ["Flow_<id>"],
                      "outgoing": ["Flow_<id>", "Flow_<id>"],
                      "default": "Flow_<default-id>"
                    },
                    {
                      "$type": "bpmn:SequenceFlow",
                      "id": "Flow_<unique-id>",
                      "name": "<ultra-short-label>",
                      "sourceRef": "<source-element-id>",
                      "targetRef": "<target-element-id>",
                      "conditionExpression": {
                        "$type": "bpmn:FormalExpression",
                        "body": "<juel-expression>"
                      }
                    },
                    {
                      "$type": "bpmn:EndEvent",
                      "id": "EndEvent_<unique-id>",
                      "name": "<event-name>",
                      "incoming": ["Flow_<id>"]
                    }
                  ]
                }
              ]
            }
            
            COMPLETE EXAMPLE (Leave Approval with BusinessRuleTask - PREFERRED PATTERN):
            {
              "$type": "bpmn:Definitions",
              "id": "Definitions_1",
              "targetNamespace": "http://bpmn.io/schema/bpmn",
              "rootElements": [
                {
                  "$type": "bpmn:Process",
                  "id": "Process_LeaveApproval",
                  "name": "Leave Approval Process",
                  "isExecutable": true,
                  "flowElements": [
                    {
                      "$type": "bpmn:StartEvent",
                      "id": "StartEvent_1",
                      "name": "Leave Request Submitted",
                      "outgoing": ["Flow_1"]
                    },
                    {
                      "$type": "bpmn:UserTask",
                      "id": "Task_SubmitRequest",
                      "name": "Submit Leave Request",
                      "incoming": ["Flow_1"],
                      "outgoing": ["Flow_2"]
                    },
                    {
                      "$type": "bpmn:BusinessRuleTask",
                      "id": "Task_ValidateLeave",
                      "name": "Validate Leave Request",
                      "implementation": "LeaveValidationRules.drl",
                      "incoming": ["Flow_2"],
                      "outgoing": ["Flow_3"],
                      "documentation": [
                        {
                          "$type": "bpmn:Documentation",
                          "text": "Business Rules:\\n\\n1. IF days > 5 THEN reject with reason 'Exceeds 5 day limit'\\n2. IF during peak delivery period THEN reject with reason 'Critical business period'\\n3. IF all criteria met THEN approve\\n\\nDRL File: LeaveValidationRules.drl"
                        }
                      ]
                    },
                    {
                      "$type": "bpmn:ExclusiveGateway",
                      "id": "Gateway_ValidationResult",
                      "name": "Validation Result",
                      "incoming": ["Flow_3"],
                      "outgoing": ["Flow_4", "Flow_5"],
                      "default": "Flow_5"
                    },
                    {
                      "$type": "bpmn:UserTask",
                      "id": "Task_HRProcessing",
                      "name": "HR Processing",
                      "incoming": ["Flow_4"],
                      "outgoing": ["Flow_6"]
                    },
                    {
                      "$type": "bpmn:EndEvent",
                      "id": "EndEvent_Approved",
                      "name": "Leave Approved",
                      "incoming": ["Flow_6"]
                    },
                    {
                      "$type": "bpmn:EndEvent",
                      "id": "EndEvent_Rejected",
                      "name": "Leave Rejected",
                      "incoming": ["Flow_5"]
                    },
                    {
                      "$type": "bpmn:SequenceFlow",
                      "id": "Flow_1",
                      "sourceRef": "StartEvent_1",
                      "targetRef": "Task_SubmitRequest"
                    },
                    {
                      "$type": "bpmn:SequenceFlow",
                      "id": "Flow_2",
                      "sourceRef": "Task_SubmitRequest",
                      "targetRef": "Task_ValidateLeave"
                    },
                    {
                      "$type": "bpmn:SequenceFlow",
                      "id": "Flow_3",
                      "sourceRef": "Task_ValidateLeave",
                      "targetRef": "Gateway_ValidationResult"
                    },
                    {
                      "$type": "bpmn:SequenceFlow",
                      "id": "Flow_4",
                      "name": "Approved",
                      "sourceRef": "Gateway_ValidationResult",
                      "targetRef": "Task_HRProcessing",
                      "conditionExpression": {
                        "$type": "bpmn:FormalExpression",
                        "body": "${'${'}validationStatus == 'APPROVED'}"
                      }
                    },
                    {
                      "$type": "bpmn:SequenceFlow",
                      "id": "Flow_5",
                      "name": "Rejected",
                      "sourceRef": "Gateway_ValidationResult",
                      "targetRef": "EndEvent_Rejected"
                    },
                    {
                      "$type": "bpmn:SequenceFlow",
                      "id": "Flow_6",
                      "sourceRef": "Task_HRProcessing",
                      "targetRef": "EndEvent_Approved"
                    }
                  ]
                }
              ]
            }
            
            ========================================
            MANDATORY OUTPUT FORMAT (THIS IS CRITICAL!):
            ========================================
            
            You MUST wrap your response in this EXACT structure:
            
            {
              "bpmnModdleJson": {
                "$type": "bpmn:Definitions",
                "id": "Definitions_12345",
                "targetNamespace": "http://bpmn.io/schema/bpmn",
                "rootElements": [
                  {
                    "$type": "bpmn:Process",
                    "id": "Process_...",
                    "name": "...",
                    "isExecutable": true,
                    "flowElements": [ ... all your BPMN elements here ... ]
                  }
                ]
              },
              "metadata": {
                "businessRuleTasks": [
                  {
                    "taskId": "Task_ValidateLeave",
                    "taskName": "Validate Leave Request",
                    "ruleDescription": "Validates leave request against multiple criteria: days limit (<=5), peak period check, eligibility verification",
                    "suggestedRuleName": "LeaveValidationRules",
                    "drlFileName": "LeaveValidationRules.drl",
                    "rules": [
                      {
                        "ruleName": "RejectIfMoreThan5Days",
                        "condition": "days > 5",
                        "action": "reject",
                        "reason": "Leave request exceeds 5 day limit",
                        "priority": 10
                      },
                      {
                        "ruleName": "RejectIfPeakPeriod",
                        "condition": "during peak delivery period",
                        "action": "reject",
                        "reason": "Overlaps with critical business delivery dates",
                        "priority": 10
                      },
                      {
                        "ruleName": "RejectIfNotEligible",
                        "condition": "not eligible (tenure < 3 months)",
                        "action": "reject",
                        "reason": "Employee not eligible for leave",
                        "priority": 10
                      },
                      {
                        "ruleName": "ApproveIfAllCriteriaMet",
                        "condition": "all criteria met (days <= 5, not peak period, eligible)",
                        "action": "approve",
                        "reason": "Leave request is valid",
                        "priority": 1
                      }
                    ]
                  }
                ],
                "explanation": "This process handles leave request validation using business rules, then routes to HR if approved"
              }
            }
            
            CRITICAL RULES FOR BUSINESSRULETASK:
            1. When you identify multiple related conditions, consolidate into ONE BusinessRuleTask
            2. ALWAYS add a "documentation" field to BusinessRuleTask with human-readable rules
            3. Include ALL rules in the metadata array with detailed information
            4. Each rule must have: ruleName, condition, action, reason, priority
            5. The documentation text should list all rules in a clear, readable format
            
            DOCUMENTATION FORMAT FOR BUSINESSRULETASK:
            {
              "documentation": [
                {
                  "$type": "bpmn:Documentation",
                  "text": "Business Rules:\\n\\n1. IF <condition1> THEN <action1> with reason '<reason1>'\\n2. IF <condition2> THEN <action2> with reason '<reason2>'\\n...\\n\\nDRL File: <drlFileName>"
                }
              ]
            }
            
            CRITICAL OUTPUT RULES:
            - Output ONLY the JSON (no markdown, no code blocks, no explanations)
            - Use realistic IDs (not placeholder text)
            - Ensure all sourceRef/targetRef match actual element IDs
            - Every element must have incoming/outgoing arrays (except start/end where appropriate)
            - Keep flow names SHORT (max 10 characters)
            
            ========================================
            REMINDER: YOU MUST OUTPUT THE EXACT FORMAT SHOWN ABOVE!
            ========================================
            
            Your response MUST have TWO top-level fields:
            1. "bpmnModdleJson" - containing the complete BPMN structure
            2. "metadata" - containing businessRuleTasks array with rules
            
            DO NOT output just the BPMN JSON alone!
            DO NOT forget the "metadata" field!
            
            If there are business rule tasks in the process, the "metadata.businessRuleTasks" array is MANDATORY with complete rule details!
            
            NOW: Analyze the process description and generate the REQUIRED JSON output with BOTH bpmnModdleJson AND metadata.
            """, processDescription);
    }
    
    /**
     * Parse AI's JSON response containing BPMN Moddle JSON and metadata.
     *
     * @param jsonResponse The JSON string from AI
     * @return Parsed ReasoningResult with BPMN JSON
     * @throws Exception if JSON parsing fails
     */
    private ReasoningResult parseReasoningResponse(String jsonResponse) throws Exception {
        // Clean up response (remove markdown code blocks if present)
        String cleanJson = jsonResponse.trim();
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
        
        ReasoningResult result = new ReasoningResult();
        JsonNode root = objectMapper.readTree(cleanJson);
        
        // Log what the AI actually returned
        logger.debug("AI response has {} top-level keys", root.size());
        root.fieldNames().forEachRemaining(fieldName -> logger.debug("  - {}", fieldName));
        
        // Extract BPMN Moddle JSON
        if (root.has("bpmnModdleJson")) {
            JsonNode bpmnNode = root.get("bpmnModdleJson");
            String bpmnJson = objectMapper.writeValueAsString(bpmnNode);
            result.setBpmnModdleJson(bpmnJson);
            
            // Extract process name from BPMN
            extractProcessName(bpmnNode, result);
        } else if (root.has("$type") && "bpmn:Definitions".equals(root.get("$type").asText())) {
            // AI returned BPMN directly without wrapper
            String bpmnJson = objectMapper.writeValueAsString(root);
            result.setBpmnModdleJson(bpmnJson);
            extractProcessName(root, result);
        } else {
            throw new IllegalArgumentException("Response does not contain valid BPMN Moddle JSON");
        }
        
        // Extract metadata for business rule tasks
        if (root.has("metadata")) {
            JsonNode metadata = root.get("metadata");
            logger.debug("Found metadata in AI response");
            
            if (metadata.has("businessRuleTasks")) {
                logger.info("Found businessRuleTasks in metadata, count: {}", metadata.get("businessRuleTasks").size());
                
                // Process first BusinessRuleTask for DRL filename (assuming one rule file per process)
                JsonNode firstRuleTask = metadata.get("businessRuleTasks").get(0);
                String drlFileName = firstRuleTask.has("drlFileName") ? 
                        firstRuleTask.get("drlFileName").asText() : 
                        firstRuleTask.get("suggestedRuleName").asText() + ".drl";
                result.setDrlFileName(drlFileName);
                logger.info("Set DRL filename: {}", drlFileName);
                
                for (JsonNode ruleTask : metadata.get("businessRuleTasks")) {
                    String taskId = ruleTask.get("taskId").asText();
                    String taskName = ruleTask.get("taskName").asText();
                    String taskDrlFileName = ruleTask.has("drlFileName") ? 
                            ruleTask.get("drlFileName").asText() : 
                            ruleTask.get("suggestedRuleName").asText() + ".drl";
                    
                    // Create individual RuleModel for each rule in the rules array
                    if (ruleTask.has("rules") && ruleTask.get("rules").isArray()) {
                        logger.info("Found rules array in ruleTask, count: {}", ruleTask.get("rules").size());
                        for (JsonNode ruleNode : ruleTask.get("rules")) {
                            RuleModel rule = new RuleModel();
                            rule.setId(ruleNode.has("ruleName") ? 
                                    ruleNode.get("ruleName").asText() : 
                                    "rule-" + UUID.randomUUID().toString().substring(0, 8));
                            rule.setExpression(ruleNode.get("condition").asText());
                            rule.setDescription(ruleNode.get("reason").asText());
                            rule.setRuleType(ruleNode.get("action").asText()); // "approve" or "reject"
                            rule.setPriority(ruleNode.has("priority") ? ruleNode.get("priority").asInt() : 1);
                            rule.setEnabled(true);
                            result.addRule(rule);
                            
                            logger.info("Created RuleModel: id={}, expression={}, action={}", 
                                    rule.getId(), rule.getExpression(), rule.getRuleType());
                        }
                    } else {
                        logger.warn("RuleTask '{}' does not have 'rules' array or it's not an array", taskId);
                    }
                    
                    // Add explanation for the rule task
                    Explanation explanation = new Explanation();
                    explanation.setNodeId(taskId);
                    explanation.setReason("Business rule task: " + taskName + " (DRL: " + taskDrlFileName + ")");
                    explanation.setConfidenceScore(0.95);
                    explanation.setSource("AI_REASONING");
                    result.addExplanation(explanation);
                    
                    // Add documentation to the BusinessRuleTask element in BPMN
                    addDocumentationToBusinessRuleTask(root, taskId, ruleTask);
                }
            }
            
            if (metadata.has("explanation")) {
                result.setOverallExplanation(metadata.get("explanation").asText());
            }
        } else {
            logger.warn("AI response does not contain 'metadata' field!");
            logger.warn("Response keys: {}", root.fieldNames());
        }
        
        // Default: no clarification required (can be enhanced later)
        result.setClarificationRequired(false);
        
        return result;
    }
    
    /**
     * Extract process name from BPMN Definitions
     */
    private void extractProcessName(JsonNode bpmnNode, ReasoningResult result) {
        if (bpmnNode.has("rootElements")) {
            for (JsonNode element : bpmnNode.get("rootElements")) {
                if ("bpmn:Process".equals(element.get("$type").asText()) && element.has("name")) {
                    result.setProcessName(element.get("name").asText());
                    return;
                }
            }
        }
        result.setProcessName("Untitled Process");
    }
    
    /**
     * Parse a ProcessNode from JSON.
     */
    private ProcessNode parseNode(JsonNode json) {
        ProcessNode node = new ProcessNode();
        
        node.setId(json.get("id").asText());
        node.setType(NodeType.valueOf(json.get("type").asText()));
        node.setName(json.get("name").asText());
        
        // Store description as a property if present
        if (json.has("description") && !json.get("description").isNull()) {
            node.addProperty("description", json.get("description").asText());
        }
        
        // Parse properties
        if (json.has("properties")) {
            JsonNode propsJson = json.get("properties");
            propsJson.fields().forEachRemaining(entry -> {
                node.addProperty(entry.getKey(), entry.getValue().asText());
            });
        }
        
        return node;
    }
    
    /**
     * Parse a ProcessEdge from JSON.
     */
    private ProcessEdge parseEdge(JsonNode json) {
        ProcessEdge edge = new ProcessEdge();
        
        edge.setId(json.get("id").asText());
        edge.setFromNodeId(json.get("fromNodeId").asText());
        edge.setToNodeId(json.get("toNodeId").asText());
        
        if (json.has("condition") && !json.get("condition").isNull()) {
            edge.setCondition(json.get("condition").asText());
        }
        
        // Use label field for description
        if (json.has("description") && !json.get("description").isNull()) {
            edge.setLabel(json.get("description").asText());
        }
        
        return edge;
    }
    
    /**
     * Parse a RuleModel from JSON.
     */
    private RuleModel parseRule(JsonNode json) {
        RuleModel rule = new RuleModel();
        
        rule.setId(json.get("id").asText());
        rule.setExpression(json.get("expression").asText());
        
        // Use name as description if present
        if (json.has("name") && !json.get("name").isNull()) {
            String name = json.get("name").asText();
            rule.setDescription(name);
        }
        
        // Override with description if present
        if (json.has("description") && !json.get("description").isNull()) {
            rule.setDescription(json.get("description").asText());
        }
        
        if (json.has("priority")) {
            rule.setPriority(json.get("priority").asInt());
        }
        
        if (json.has("enabled")) {
            rule.setEnabled(json.get("enabled").asBoolean());
        } else {
            rule.setEnabled(true); // Default to enabled
        }
        
        return rule;
    }
    
    /**
     * Parse an Explanation from JSON.
     */
    private Explanation parseExplanation(JsonNode json) {
        Explanation explanation = new Explanation();
        
        explanation.setNodeId(json.get("nodeId").asText());
        explanation.setReason(json.get("reason").asText());
        
        if (json.has("confidenceScore")) {
            explanation.setConfidenceScore(json.get("confidenceScore").asDouble());
        }
        
        if (json.has("source")) {
            explanation.setSource(json.get("source").asText());
        } else {
            explanation.setSource("AI_REASONING");
        }
        
        explanation.setTimestamp(LocalDateTime.now());
        
        return explanation;
    }
    
    /**
     * Add documentation field to BusinessRuleTask in BPMN JSON.
     * This makes the rules visible in the properties panel.
     * Also adds Camunda-specific attributes for rule execution.
     */
    private void addDocumentationToBusinessRuleTask(JsonNode root, String taskId, JsonNode ruleTaskMetadata) {
        try {
            // Navigate to the process's flowElements
            JsonNode bpmnJson = root.has("bpmnModdleJson") ? root.get("bpmnModdleJson") : root;
            
            if (bpmnJson.has("rootElements")) {
                for (JsonNode rootElement : bpmnJson.get("rootElements")) {
                    if ("bpmn:Process".equals(rootElement.get("$type").asText()) && rootElement.has("flowElements")) {
                        for (JsonNode flowElement : rootElement.get("flowElements")) {
                            if (taskId.equals(flowElement.get("id").asText()) && 
                                "bpmn:BusinessRuleTask".equals(flowElement.get("$type").asText())) {
                                
                                // Build documentation text from rules
                                StringBuilder docText = new StringBuilder("Business Rules:\\n\\n");
                                int ruleNum = 1;
                                
                                if (ruleTaskMetadata.has("rules") && ruleTaskMetadata.get("rules").isArray()) {
                                    for (JsonNode rule : ruleTaskMetadata.get("rules")) {
                                        docText.append(ruleNum++).append(". IF ")
                                                .append(rule.get("condition").asText())
                                                .append(" THEN ")
                                                .append(rule.get("action").asText())
                                                .append(" with reason '")
                                                .append(rule.get("reason").asText())
                                                .append("'\\n");
                                    }
                                }
                                
                                // Add DRL file reference
                                String drlFileName = ruleTaskMetadata.has("drlFileName") ? 
                                        ruleTaskMetadata.get("drlFileName").asText() : 
                                        ruleTaskMetadata.get("suggestedRuleName").asText() + ".drl";
                                docText.append("\\nDRL File: ").append(drlFileName);
                                
                                // Create documentation array node
                                ObjectNode documentationNode = objectMapper.createObjectNode();
                                documentationNode.put("$type", "bpmn:Documentation");
                                documentationNode.put("text", docText.toString());
                                
                                ArrayNode documentationArray = objectMapper.createArrayNode();
                                documentationArray.add(documentationNode);
                                
                                // Add to flowElement (modify the node in place)
                                ObjectNode taskNode = (ObjectNode) flowElement;
                                taskNode.set("documentation", documentationArray);
                                
                                // Add Camunda-specific attributes for rule execution
                                // These will be visible in the properties panel
                                taskNode.put("camunda:resource", drlFileName);
                                taskNode.put("camunda:decisionRef", ruleTaskMetadata.get("suggestedRuleName").asText());
                                taskNode.put("camunda:resultVariable", "validationResult");
                                taskNode.put("camunda:mapDecisionResult", "singleResult");
                                
                                logger.debug("Added documentation and Camunda attributes to BusinessRuleTask: {}", taskId);
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to add documentation to BusinessRuleTask {}: {}", taskId, e.getMessage());
            // Don't fail the entire process if documentation addition fails
        }
    }
}

