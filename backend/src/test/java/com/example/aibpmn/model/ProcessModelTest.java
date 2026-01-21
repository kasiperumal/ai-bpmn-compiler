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
        ProcessModel process = new ProcessModel("test-001", "Test Process");
        
        assertEquals("test-001", process.getId());
        assertEquals("Test Process", process.getName());
        assertEquals(1, process.getVersion());
        assertEquals(ProcessStatus.DRAFT, process.getStatus());
        assertNotNull(process.getRules());
        assertNotNull(process.getMetadata());
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
        ProcessModel process = new ProcessModel("order-001", "Order Processing");
        process.setStatus(ProcessStatus.DRAFT);
        
        // Note: Process structure (nodes, edges) now stored in bpmnModdleJson
        // This test validates basic process model properties
        
        String sampleBpmnJson = "{\"$type\":\"bpmn:Process\",\"id\":\"Process_001\",\"flowElements\":[]}";
        process.setBpmnModdleJson(sampleBpmnJson);
        
        // Verify
        assertEquals("order-001", process.getId());
        assertEquals("Order Processing", process.getName());
        assertEquals(ProcessStatus.DRAFT, process.getStatus());
        assertNotNull(process.getBpmnModdleJson());
        assertNotNull(process.getMetadata());
    }
    
    @Test
    void testJsonSerialization() throws Exception {
        // Create a complete process
        ProcessModel process = new ProcessModel("json-test", "JSON Test Process");
        process.setStatus(ProcessStatus.PUBLISHED);
        
        // Note: ProcessNode, Explanation, Approval are now stored in bpmnModdleJson
        // This test now validates basic process model serialization only
        
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
        assertNotNull(deserialized.getMetadata());
    }
}

