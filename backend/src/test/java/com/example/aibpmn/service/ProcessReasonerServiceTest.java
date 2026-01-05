package com.example.aibpmn.service;

import com.example.aibpmn.dto.ReasoningResult;
import com.example.aibpmn.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ProcessReasonerServiceTest {
    
    private GeminiClient geminiClient;
    private ProcessReasonerService reasonerService;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        geminiClient = mock(GeminiClient.class);
        objectMapper = new ObjectMapper();
        reasonerService = new ProcessReasonerService(geminiClient, objectMapper);
    }
    
    @Test
    void testReasonOverDescription_Success() {
        // Arrange
        String description = """
            ## Overview
            This is an order approval process.
            
            ## Main Flow
            1. Process begins when order is received
            2. Order is validated
            3. Check if amount is over $1000
            4. If yes, manager approves
            5. If no, auto-approve
            6. Process completes
            """;
        
        String mockJsonResponse = """
            {
              "nodes": [
                {
                  "id": "start-1",
                  "type": "EVENT",
                  "name": "Order Received",
                  "description": "Process starts",
                  "properties": {"eventType": "start"}
                },
                {
                  "id": "task-validate",
                  "type": "TASK",
                  "name": "Validate Order",
                  "description": "Validation step",
                  "properties": {}
                },
                {
                  "id": "gateway-amount-check",
                  "type": "GATEWAY",
                  "name": "Check Amount",
                  "description": "Check if over $1000",
                  "properties": {"gatewayType": "exclusive"}
                },
                {
                  "id": "end-1",
                  "type": "EVENT",
                  "name": "Completed",
                  "description": "Process ends",
                  "properties": {"eventType": "end"}
                }
              ],
              "edges": [
                {
                  "id": "edge-1",
                  "fromNodeId": "start-1",
                  "toNodeId": "task-validate",
                  "condition": null,
                  "description": "Start to validate"
                },
                {
                  "id": "edge-2",
                  "fromNodeId": "task-validate",
                  "toNodeId": "gateway-amount-check",
                  "condition": null,
                  "description": "After validation"
                }
              ],
              "rules": [
                {
                  "id": "rule-1",
                  "name": "High Value Check",
                  "expression": "amount > 1000",
                  "description": "Check for high value orders",
                  "priority": 10,
                  "enabled": true
                }
              ],
              "explanations": [
                {
                  "nodeId": "gateway-amount-check",
                  "reason": "Decision point for amount threshold",
                  "confidenceScore": 0.95,
                  "source": "AI_REASONING"
                }
              ],
              "clarificationRequired": false,
              "clarificationReasons": []
            }
            """;
        
        when(geminiClient.generateFromText(anyString())).thenReturn(mockJsonResponse);
        
        // Act
        ReasoningResult result = reasonerService.reasonOverDescription(description);
        
        // Assert
        assertNotNull(result);
        assertEquals(4, result.getNodes().size());
        assertEquals(2, result.getEdges().size());
        assertEquals(1, result.getRules().size());
        assertEquals(1, result.getExplanations().size());
        assertFalse(result.isClarificationRequired());
        
        // Verify nodes
        ProcessNode startNode = result.getNodes().get(0);
        assertEquals("start-1", startNode.getId());
        assertEquals(NodeType.EVENT, startNode.getType());
        assertEquals("Order Received", startNode.getName());
        
        ProcessNode taskNode = result.getNodes().get(1);
        assertEquals("task-validate", taskNode.getId());
        assertEquals(NodeType.TASK, taskNode.getType());
        
        // Verify edges
        ProcessEdge edge = result.getEdges().get(0);
        assertEquals("edge-1", edge.getId());
        assertEquals("start-1", edge.getFromNodeId());
        assertEquals("task-validate", edge.getToNodeId());
        
        // Verify rules
        RuleModel rule = result.getRules().get(0);
        assertEquals("rule-1", rule.getId());
        assertEquals("amount > 1000", rule.getExpression());
        assertTrue(rule.isEnabled());
        
        // Verify explanations
        Explanation explanation = result.getExplanations().get(0);
        assertEquals("gateway-amount-check", explanation.getNodeId());
        assertEquals(0.95, explanation.getConfidenceScore());
        
        verify(geminiClient, times(1)).generateFromText(anyString());
    }
    
    @Test
    void testReasonOverDescription_WithClarificationRequired() {
        // Arrange
        String description = "A process with some unclear steps.";
        
        String mockJsonResponse = """
            {
              "nodes": [],
              "edges": [],
              "rules": [],
              "explanations": [],
              "clarificationRequired": true,
              "clarificationReasons": [
                "Process steps are not clearly described",
                "Missing information about decision criteria"
              ]
            }
            """;
        
        when(geminiClient.generateFromText(anyString())).thenReturn(mockJsonResponse);
        
        // Act
        ReasoningResult result = reasonerService.reasonOverDescription(description);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.isClarificationRequired());
        assertEquals(2, result.getClarificationReasons().size());
        assertEquals("Process steps are not clearly described",
            result.getClarificationReasons().get(0));
    }
    
    @Test
    void testReasonOverDescription_NullDescription() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> reasonerService.reasonOverDescription(null));
    }
    
    @Test
    void testReasonOverDescription_EmptyDescription() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> reasonerService.reasonOverDescription(""));
        
        assertThrows(IllegalArgumentException.class,
            () -> reasonerService.reasonOverDescription("   "));
    }
    
    @Test
    void testReasonOverDescription_GeminiError() {
        // Arrange
        String description = "Test process";
        when(geminiClient.generateFromText(anyString()))
            .thenThrow(new RuntimeException("API error"));
        
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> reasonerService.reasonOverDescription(description));
        
        assertTrue(exception.getMessage().contains("Failed to reason over description"));
    }
    
    @Test
    void testReasonOverDescription_InvalidJson() {
        // Arrange
        String description = "Test process";
        when(geminiClient.generateFromText(anyString())).thenReturn("Not valid JSON");
        
        // Act & Assert
        assertThrows(RuntimeException.class,
            () -> reasonerService.reasonOverDescription(description));
    }
    
    @Test
    void testReasonOverDescription_JsonWithMarkdown() {
        // Arrange
        String description = "Test process";
        
        String mockJsonResponse = """
            ```json
            {
              "nodes": [
                {
                  "id": "node-1",
                  "type": "TASK",
                  "name": "Test Task",
                  "description": "Test",
                  "properties": {}
                }
              ],
              "edges": [],
              "rules": [],
              "explanations": [],
              "clarificationRequired": false,
              "clarificationReasons": []
            }
            ```
            """;
        
        when(geminiClient.generateFromText(anyString())).thenReturn(mockJsonResponse);
        
        // Act
        ReasoningResult result = reasonerService.reasonOverDescription(description);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.getNodes().size());
        assertEquals("node-1", result.getNodes().get(0).getId());
    }
    
    @Test
    void testReasonOverDescription_ComplexWorkflow() {
        // Arrange
        String description = """
            Complex order processing workflow with multiple decision points,
            parallel activities, and exception handling.
            """;
        
        String mockJsonResponse = """
            {
              "nodes": [
                {
                  "id": "start",
                  "type": "EVENT",
                  "name": "Start",
                  "description": "Begin",
                  "properties": {"eventType": "start"}
                },
                {
                  "id": "task-1",
                  "type": "TASK",
                  "name": "Task 1",
                  "description": "First task",
                  "properties": {}
                },
                {
                  "id": "task-2",
                  "type": "TASK",
                  "name": "Task 2",
                  "description": "Second task",
                  "properties": {}
                },
                {
                  "id": "gateway-1",
                  "type": "GATEWAY",
                  "name": "Gateway 1",
                  "description": "Decision",
                  "properties": {"gatewayType": "exclusive"}
                },
                {
                  "id": "gateway-2",
                  "type": "GATEWAY",
                  "name": "Gateway 2",
                  "description": "Parallel split",
                  "properties": {"gatewayType": "parallel"}
                },
                {
                  "id": "end",
                  "type": "EVENT",
                  "name": "End",
                  "description": "Complete",
                  "properties": {"eventType": "end"}
                }
              ],
              "edges": [
                {"id": "e1", "fromNodeId": "start", "toNodeId": "task-1", "condition": null, "description": ""},
                {"id": "e2", "fromNodeId": "task-1", "toNodeId": "gateway-1", "condition": null, "description": ""},
                {"id": "e3", "fromNodeId": "gateway-1", "toNodeId": "task-2", "condition": "approved", "description": ""},
                {"id": "e4", "fromNodeId": "task-2", "toNodeId": "end", "condition": null, "description": ""}
              ],
              "rules": [
                {"id": "r1", "name": "Rule 1", "expression": "x > 10", "description": "Test", "priority": 5, "enabled": true},
                {"id": "r2", "name": "Rule 2", "expression": "y < 5", "description": "Test 2", "priority": 3, "enabled": true}
              ],
              "explanations": [
                {"nodeId": "gateway-1", "reason": "Decision point", "confidenceScore": 0.9, "source": "AI_REASONING"},
                {"nodeId": "gateway-2", "reason": "Parallel gateway", "confidenceScore": 0.85, "source": "AI_REASONING"},
                {"nodeId": "task-1", "reason": "Clear task", "confidenceScore": 0.98, "source": "AI_REASONING"}
              ],
              "clarificationRequired": false,
              "clarificationReasons": []
            }
            """;
        
        when(geminiClient.generateFromText(anyString())).thenReturn(mockJsonResponse);
        
        // Act
        ReasoningResult result = reasonerService.reasonOverDescription(description);
        
        // Assert
        assertNotNull(result);
        assertEquals(6, result.getNodes().size());
        assertEquals(4, result.getEdges().size());
        assertEquals(2, result.getRules().size());
        assertEquals(3, result.getExplanations().size());
        assertFalse(result.isClarificationRequired());
        
        // Verify total elements
        assertEquals(12, result.getTotalElements());
    }
    
    @Test
    void testReasonOverDescription_PromptContainsDescription() {
        // Arrange
        String description = "Order approval workflow";
        when(geminiClient.generateFromText(anyString())).thenReturn(
            "{\"nodes\":[], \"edges\":[], \"rules\":[], \"explanations\":[], \"clarificationRequired\":false, \"clarificationReasons\":[]}"
        );
        
        // Act
        reasonerService.reasonOverDescription(description);
        
        // Assert - verify the prompt contains the description
        verify(geminiClient, times(1)).generateFromText(
            argThat(prompt -> prompt.contains(description) &&
                              prompt.contains("PROCESS DESCRIPTION") &&
                              prompt.contains("Identify all PROCESS NODES") &&
                              prompt.contains("Identify CONNECTIONS") &&
                              prompt.contains("Identify BUSINESS RULES") &&
                              prompt.contains("Provide EXPLANATIONS") &&
                              prompt.contains("Detect UNCERTAINTIES"))
        );
    }
    
    @Test
    void testReasoningResult_HelperMethods() {
        // Arrange
        ReasoningResult result = new ReasoningResult();
        
        ProcessNode node = new ProcessNode();
        node.setId("node-1");
        node.setType(NodeType.TASK);
        
        ProcessEdge edge = new ProcessEdge();
        edge.setId("edge-1");
        
        RuleModel rule = new RuleModel();
        rule.setId("rule-1");
        
        Explanation explanation = new Explanation();
        explanation.setNodeId("node-1");
        
        // Act
        result.addNode(node);
        result.addEdge(edge);
        result.addRule(rule);
        result.addExplanation(explanation);
        result.addClarificationReason("Test reason");
        
        // Assert
        assertEquals(1, result.getNodes().size());
        assertEquals(1, result.getEdges().size());
        assertEquals(1, result.getRules().size());
        assertEquals(1, result.getExplanations().size());
        assertEquals(1, result.getClarificationReasons().size());
        assertTrue(result.isClarificationRequired());
        assertEquals("Test reason", result.getClarificationReasons().get(0));
        
        // Test toString
        String resultString = result.toString();
        assertTrue(resultString.contains("nodes=1"));
        assertTrue(resultString.contains("edges=1"));
        assertTrue(resultString.contains("rules=1"));
    }
}

