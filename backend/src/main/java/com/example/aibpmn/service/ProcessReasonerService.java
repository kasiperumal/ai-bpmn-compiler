package com.example.aibpmn.service;

import com.example.aibpmn.dto.ReasoningResult;
import com.example.aibpmn.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for reasoning over process descriptions and extracting structured BPMN elements.
 * Converts natural language process descriptions into ProcessNodes, ProcessEdges, RuleModels, and Explanations.
 */
@Service
public class ProcessReasonerService {
    
    private static final Logger logger = LoggerFactory.getLogger(ProcessReasonerService.class);
    
    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;
    
    public ProcessReasonerService(GeminiClient geminiClient, ObjectMapper objectMapper) {
        this.geminiClient = geminiClient;
        this.objectMapper = objectMapper;
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
            // 1. Create prompt for Gemini to extract structured elements
            String prompt = createReasoningPrompt(processDescription);
            
            // 2. Call Gemini to analyze and structure the description
            String jsonResponse = geminiClient.generateFromText(prompt);
            
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
            Analyze the following process description and extract structured BPMN elements.
            
            PROCESS DESCRIPTION:
            %s
            
            INSTRUCTIONS:
            
            1. Identify all PROCESS NODES:
               - Start events (where the process begins)
               - End events (where the process completes)
               - Tasks (actions or steps to be performed)
               - Gateways (decision points, branching, or merging)
            
            2. Identify CONNECTIONS (edges):
               - How nodes connect to each other
               - Conditions for conditional branches
            
            3. Identify BUSINESS RULES (if any):
               - Explicit rules mentioned (e.g., "if amount > $1000")
               - Validation rules
               - Decision criteria
            
            4. Provide EXPLANATIONS:
               - Why you identified each element
               - Your confidence level (0.0 to 1.0)
               - Any assumptions made
            
            5. Detect UNCERTAINTIES:
               - Missing information
               - Ambiguous descriptions
               - Unclear connections
               - If clarification is needed, list specific questions
            
            IMPORTANT GUIDELINES:
            - Use descriptive IDs (e.g., "start-order-received", "task-validate-order", "gateway-check-amount")
            - For gateways, clearly specify the type (exclusive, parallel, inclusive)
            - Include all mentioned decision conditions
            - Be explicit about what you can and cannot determine
            - If information is missing or unclear, flag clarificationRequired = true
            
            OUTPUT FORMAT (JSON only, no additional text):
            {
              "nodes": [
                {
                  "id": "start-1",
                  "type": "EVENT",
                  "name": "Order Received",
                  "description": "Process begins when customer order is received",
                  "properties": {
                    "eventType": "start"
                  }
                },
                {
                  "id": "task-validate",
                  "type": "TASK",
                  "name": "Validate Order",
                  "description": "Check order completeness and correctness",
                  "properties": {}
                },
                {
                  "id": "gateway-check-stock",
                  "type": "GATEWAY",
                  "name": "Check Stock Availability",
                  "description": "Decision based on inventory levels",
                  "properties": {
                    "gatewayType": "exclusive"
                  }
                },
                {
                  "id": "end-success",
                  "type": "EVENT",
                  "name": "Order Completed",
                  "description": "Process ends successfully",
                  "properties": {
                    "eventType": "end"
                  }
                }
              ],
              "edges": [
                {
                  "id": "edge-1",
                  "fromNodeId": "start-1",
                  "toNodeId": "task-validate",
                  "condition": null,
                  "description": "Flow to validation"
                },
                {
                  "id": "edge-2",
                  "fromNodeId": "task-validate",
                  "toNodeId": "gateway-check-stock",
                  "condition": null,
                  "description": "After validation"
                },
                {
                  "id": "edge-3",
                  "fromNodeId": "gateway-check-stock",
                  "toNodeId": "task-fulfill",
                  "condition": "stock available",
                  "description": "When items are in stock"
                }
              ],
              "rules": [
                {
                  "id": "rule-1",
                  "name": "High Value Order Check",
                  "expression": "orderAmount > 1000",
                  "description": "Orders over $1000 require manager approval",
                  "priority": 10,
                  "enabled": true
                }
              ],
              "explanations": [
                {
                  "nodeId": "gateway-check-stock",
                  "reason": "Identified as gateway because description mentions checking inventory and branching based on availability",
                  "confidenceScore": 0.95,
                  "source": "AI_REASONING"
                },
                {
                  "nodeId": "task-validate",
                  "reason": "Clearly described as validation step, high confidence",
                  "confidenceScore": 0.98,
                  "source": "AI_REASONING"
                }
              ],
              "clarificationRequired": false,
              "clarificationReasons": []
            }
            
            If information is ambiguous or missing, set clarificationRequired = true and list specific questions in clarificationReasons.
            
            Example of when clarification is needed:
            {
              "clarificationRequired": true,
              "clarificationReasons": [
                "Who is responsible for approving high-value orders?",
                "What happens if payment processing fails?",
                "Are there any parallel activities in the fulfillment step?"
              ]
            }
            
            Now analyze the process description and return ONLY the JSON output.
            """, processDescription);
    }
    
    /**
     * Parse Gemini's JSON response into a ReasoningResult object.
     *
     * @param jsonResponse The JSON string from Gemini
     * @return Parsed ReasoningResult
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
        
        // Parse nodes
        if (root.has("nodes")) {
            for (JsonNode nodeJson : root.get("nodes")) {
                ProcessNode node = parseNode(nodeJson);
                result.addNode(node);
            }
        }
        
        // Parse edges
        if (root.has("edges")) {
            for (JsonNode edgeJson : root.get("edges")) {
                ProcessEdge edge = parseEdge(edgeJson);
                result.addEdge(edge);
            }
        }
        
        // Parse rules
        if (root.has("rules")) {
            for (JsonNode ruleJson : root.get("rules")) {
                RuleModel rule = parseRule(ruleJson);
                result.addRule(rule);
            }
        }
        
        // Parse explanations
        if (root.has("explanations")) {
            for (JsonNode explJson : root.get("explanations")) {
                Explanation explanation = parseExplanation(explJson);
                result.addExplanation(explanation);
            }
        }
        
        // Parse clarification flags
        if (root.has("clarificationRequired")) {
            result.setClarificationRequired(root.get("clarificationRequired").asBoolean());
        }
        
        if (root.has("clarificationReasons")) {
            for (JsonNode reasonJson : root.get("clarificationReasons")) {
                result.addClarificationReason(reasonJson.asText());
            }
        }
        
        return result;
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
}

