package com.example.aibpmn.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProcessModelTest {
    
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    
    @Test
    void testProcessModelCreation() {
        ProcessModel process = new ProcessModel("test-001", "Test Process", "1.0.0");
        
        assertEquals("test-001", process.getId());
        assertEquals("Test Process", process.getName());
        assertEquals("1.0.0", process.getVersion());
        assertEquals(ProcessStatus.DRAFT, process.getStatus());
        assertNotNull(process.getNodes());
        assertNotNull(process.getEdges());
        assertNotNull(process.getRules());
    }
    
    @Test
    void testProcessNodeCreation() {
        ProcessNode node = new ProcessNode("node-001", NodeType.TASK, "Validate Order");
        node.addProperty("taskType", "SERVICE");
        node.addProperty("timeout", "PT5M");
        
        assertEquals("node-001", node.getId());
        assertEquals(NodeType.TASK, node.getType());
        assertEquals("Validate Order", node.getName());
        assertEquals("SERVICE", node.getProperty("taskType"));
        assertEquals(2, node.getProperties().size());
    }
    
    @Test
    void testProcessEdgeCreation() {
        ProcessEdge edge = new ProcessEdge("node-1", "node-2", "amount > 100");
        edge.setLabel("High Value");
        
        assertEquals("node-1", edge.getFromNodeId());
        assertEquals("node-2", edge.getToNodeId());
        assertEquals("amount > 100", edge.getCondition());
        assertEquals("High Value", edge.getLabel());
    }
    
    @Test
    void testRuleModelCreation() {
        RuleModel rule = new RuleModel("rule-001", "order.amount > 1000", 
            "Large orders require approval");
        rule.setRuleType("VALIDATION");
        rule.setPriority(1);
        
        assertEquals("rule-001", rule.getId());
        assertEquals("order.amount > 1000", rule.getExpression());
        assertTrue(rule.isEnabled());
        assertEquals(1, rule.getPriority());
    }
    
    @Test
    void testExplanationCreation() {
        Explanation explanation = new Explanation("node-001", 
            "This step validates the order");
        explanation.setSource("AI-Generated");
        explanation.setConfidenceScore(0.95);
        
        assertEquals("node-001", explanation.getNodeId());
        assertEquals("This step validates the order", explanation.getReason());
        assertEquals(0.95, explanation.getConfidenceScore());
        assertNotNull(explanation.getTimestamp());
    }
    
    @Test
    void testApprovalCreation() {
        Approval approval = new Approval("node-001");
        approval.setAiApproved(true);
        approval.setAiComment("Looks good");
        
        assertEquals("node-001", approval.getNodeId());
        assertTrue(approval.getAiApproved());
        assertFalse(approval.isFullyApproved()); // User hasn't approved yet
        assertTrue(approval.isPendingApproval());
        
        approval.setUserApproved(true);
        approval.setApprovedBy("john.doe@example.com");
        
        assertTrue(approval.isFullyApproved());
        assertFalse(approval.isPendingApproval());
    }
    
    @Test
    void testCompleteProcessStructure() {
        // Create process
        ProcessModel process = new ProcessModel("order-001", "Order Processing", "1.0.0");
        
        // Add nodes
        ProcessNode start = new ProcessNode("start-1", NodeType.EVENT, "Order Received");
        start.addProperty("eventType", "START");
        process.addNode(start);
        
        ProcessNode task = new ProcessNode("task-1", NodeType.TASK, "Validate Order");
        task.addProperty("taskType", "SERVICE");
        process.addNode(task);
        
        ProcessNode gateway = new ProcessNode("gateway-1", NodeType.GATEWAY, "Valid?");
        gateway.addProperty("gatewayType", "EXCLUSIVE");
        process.addNode(gateway);
        
        // Add edges
        process.addEdge(new ProcessEdge("start-1", "task-1"));
        process.addEdge(new ProcessEdge("task-1", "gateway-1"));
        
        // Add rule
        RuleModel rule = new RuleModel("rule-1", "order.amount > 1000", 
            "High value orders");
        process.addRule(rule);
        
        // Verify
        assertEquals(3, process.getNodes().size());
        assertEquals(2, process.getEdges().size());
        assertEquals(1, process.getRules().size());
    }
    
    @Test
    void testJsonSerialization() throws Exception {
        // Create a complete process
        ProcessModel process = new ProcessModel("json-test", "JSON Test Process", "1.0.0");
        process.setStatus(ProcessStatus.PUBLISHED);
        
        ProcessNode node = new ProcessNode("node-1", NodeType.TASK, "Test Task");
        node.addProperty("key1", "value1");
        
        Explanation explanation = new Explanation("node-1", "Test explanation");
        explanation.setConfidenceScore(0.9);
        node.setExplanation(explanation);
        
        Approval approval = new Approval("node-1", true, true);
        approval.setApprovedBy("tester");
        node.setApproval(approval);
        
        process.addNode(node);
        process.addEdge(new ProcessEdge("node-1", "node-2", "condition"));
        process.addRule(new RuleModel("rule-1", "expr", "desc"));
        
        // Serialize to JSON
        String json = objectMapper.writeValueAsString(process);
        assertNotNull(json);
        assertTrue(json.contains("json-test"));
        assertTrue(json.contains("PUBLISHED"));
        
        // Deserialize from JSON
        ProcessModel deserialized = objectMapper.readValue(json, ProcessModel.class);
        assertNotNull(deserialized);
        assertEquals(process.getId(), deserialized.getId());
        assertEquals(process.getName(), deserialized.getName());
        assertEquals(process.getStatus(), deserialized.getStatus());
        assertEquals(1, deserialized.getNodes().size());
        assertEquals(1, deserialized.getEdges().size());
        assertEquals(1, deserialized.getRules().size());
    }
}

