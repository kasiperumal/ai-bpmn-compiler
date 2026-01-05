package com.example.aibpmn.service;

import com.example.aibpmn.exception.BpmnValidationException;
import com.example.aibpmn.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BpmnGeneratorServiceTest {
    
    private BpmnGeneratorService bpmnGenerator;
    
    @BeforeEach
    void setUp() {
        bpmnGenerator = new BpmnGeneratorService();
    }
    
    @Test
    void testGenerateBpmn_SimpleProcess() {
        // Arrange
        ProcessModel process = createSimpleProcess();
        
        // Act
        String bpmnXml = bpmnGenerator.generateBpmn(process);
        
        // Assert
        assertNotNull(bpmnXml);
        assertTrue(bpmnXml.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        assertTrue(bpmnXml.contains("<bpmn2:definitions"));
        assertTrue(bpmnXml.contains("<bpmn2:process"));
        assertTrue(bpmnXml.contains("id=\"test-process\""));
        assertTrue(bpmnXml.contains("isExecutable=\"true\""));
        assertTrue(bpmnXml.contains("<bpmn2:startEvent"));
        assertTrue(bpmnXml.contains("<bpmn2:endEvent"));
        assertTrue(bpmnXml.contains("<bpmn2:scriptTask"));
        assertTrue(bpmnXml.contains("<bpmn2:sequenceFlow"));
        assertTrue(bpmnXml.contains("</bpmn2:definitions>"));
    }
    
    @Test
    void testGenerateBpmn_NullProcess() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> bpmnGenerator.generateBpmn(null));
    }
    
    @Test
    void testGenerateBpmn_EmptyProcess() {
        // Arrange
        ProcessModel process = new ProcessModel();
        process.setId("empty-process");
        process.setName("Empty Process");
        
        // Act & Assert
        BpmnValidationException exception = assertThrows(
            BpmnValidationException.class,
            () -> bpmnGenerator.generateBpmn(process)
        );
        
        assertTrue(exception.getMessage().contains("must have at least one node"));
    }
    
    @Test
    void testGenerateBpmn_NoStartEvent() {
        // Arrange
        ProcessModel process = new ProcessModel();
        process.setId("no-start");
        process.setName("No Start Event");
        
        ProcessNode task = new ProcessNode("task-1", NodeType.TASK, "Task");
        ProcessNode end = createEndEvent("end-1", "End");
        
        process.addNode(task);
        process.addNode(end);
        process.addEdge(new ProcessEdge("task-1", "end-1"));
        
        // Act & Assert
        BpmnValidationException exception = assertThrows(
            BpmnValidationException.class,
            () -> bpmnGenerator.generateBpmn(process)
        );
        
        assertTrue(exception.getMessage().contains("must have exactly one start event"));
    }
    
    @Test
    void testGenerateBpmn_MultipleStartEvents() {
        // Arrange
        ProcessModel process = new ProcessModel();
        process.setId("multi-start");
        process.setName("Multiple Starts");
        
        ProcessNode start1 = createStartEvent("start-1", "Start 1");
        ProcessNode start2 = createStartEvent("start-2", "Start 2");
        ProcessNode end = createEndEvent("end-1", "End");
        
        process.addNode(start1);
        process.addNode(start2);
        process.addNode(end);
        
        // Act & Assert
        BpmnValidationException exception = assertThrows(
            BpmnValidationException.class,
            () -> bpmnGenerator.generateBpmn(process)
        );
        
        assertTrue(exception.getMessage().contains("must have exactly one start event"));
        assertTrue(exception.getMessage().contains("found 2"));
    }
    
    @Test
    void testGenerateBpmn_NoEndEvent() {
        // Arrange
        ProcessModel process = new ProcessModel();
        process.setId("no-end");
        process.setName("No End Event");
        
        ProcessNode start = createStartEvent("start-1", "Start");
        ProcessNode task = new ProcessNode("task-1", NodeType.TASK, "Task");
        
        process.addNode(start);
        process.addNode(task);
        process.addEdge(new ProcessEdge("start-1", "task-1"));
        
        // Act & Assert
        BpmnValidationException exception = assertThrows(
            BpmnValidationException.class,
            () -> bpmnGenerator.generateBpmn(process)
        );
        
        assertTrue(exception.getMessage().contains("must have at least one end event"));
    }
    
    @Test
    void testGenerateBpmn_WithGateway() {
        // Arrange
        ProcessModel process = new ProcessModel();
        process.setId("gateway-process");
        process.setName("Gateway Process");
        
        ProcessNode start = createStartEvent("start", "Start");
        ProcessNode gateway = createGateway("gateway-1", "Check Amount", "exclusive");
        ProcessNode task1 = new ProcessNode("task-1", NodeType.TASK, "High Value");
        ProcessNode task2 = new ProcessNode("task-2", NodeType.TASK, "Low Value");
        ProcessNode end = createEndEvent("end", "End");
        
        process.addNode(start);
        process.addNode(gateway);
        process.addNode(task1);
        process.addNode(task2);
        process.addNode(end);
        
        process.addEdge(new ProcessEdge("start", "gateway-1"));
        
        ProcessEdge edge1 = new ProcessEdge("gateway-1", "task-1");
        edge1.setCondition("amount > 1000");
        process.addEdge(edge1);
        
        ProcessEdge edge2 = new ProcessEdge("gateway-1", "task-2");
        edge2.setCondition("amount <= 1000");
        process.addEdge(edge2);
        
        process.addEdge(new ProcessEdge("task-1", "end"));
        process.addEdge(new ProcessEdge("task-2", "end"));
        
        // Act
        String bpmnXml = bpmnGenerator.generateBpmn(process);
        
        // Assert
        assertNotNull(bpmnXml);
        assertTrue(bpmnXml.contains("<bpmn2:exclusiveGateway"));
        assertTrue(bpmnXml.contains("id=\"gateway-1\""));
        assertTrue(bpmnXml.contains("<bpmn2:conditionExpression"));
        assertTrue(bpmnXml.contains("amount &gt; 1000")); // XML escaped
        assertTrue(bpmnXml.contains("amount &lt;= 1000")); // XML escaped
    }
    
    @Test
    void testGenerateBpmn_ParallelGateway() {
        // Arrange
        ProcessModel process = new ProcessModel();
        process.setId("parallel-process");
        process.setName("Parallel Process");
        
        ProcessNode start = createStartEvent("start", "Start");
        ProcessNode parallelSplit = createGateway("split", "Split", "parallel");
        ProcessNode task1 = new ProcessNode("task-1", NodeType.TASK, "Task 1");
        ProcessNode task2 = new ProcessNode("task-2", NodeType.TASK, "Task 2");
        ProcessNode parallelJoin = createGateway("join", "Join", "parallel");
        ProcessNode end = createEndEvent("end", "End");
        
        process.addNode(start);
        process.addNode(parallelSplit);
        process.addNode(task1);
        process.addNode(task2);
        process.addNode(parallelJoin);
        process.addNode(end);
        
        process.addEdge(new ProcessEdge("start", "split"));
        process.addEdge(new ProcessEdge("split", "task-1"));
        process.addEdge(new ProcessEdge("split", "task-2"));
        process.addEdge(new ProcessEdge("task-1", "join"));
        process.addEdge(new ProcessEdge("task-2", "join"));
        process.addEdge(new ProcessEdge("join", "end"));
        
        // Act
        String bpmnXml = bpmnGenerator.generateBpmn(process);
        
        // Assert
        assertNotNull(bpmnXml);
        assertTrue(bpmnXml.contains("<bpmn2:parallelGateway"));
        assertTrue(bpmnXml.contains("id=\"split\""));
        assertTrue(bpmnXml.contains("id=\"join\""));
    }
    
    @Test
    void testGenerateBpmn_InclusiveGateway() {
        // Arrange
        ProcessModel process = new ProcessModel();
        process.setId("inclusive-process");
        process.setName("Inclusive Process");
        
        ProcessNode start = createStartEvent("start", "Start");
        ProcessNode gateway = createGateway("gateway", "Inclusive", "inclusive");
        ProcessNode end = createEndEvent("end", "End");
        
        process.addNode(start);
        process.addNode(gateway);
        process.addNode(end);
        
        process.addEdge(new ProcessEdge("start", "gateway"));
        process.addEdge(new ProcessEdge("gateway", "end"));
        
        // Act
        String bpmnXml = bpmnGenerator.generateBpmn(process);
        
        // Assert
        assertNotNull(bpmnXml);
        assertTrue(bpmnXml.contains("<bpmn2:inclusiveGateway"));
    }
    
    @Test
    void testGenerateBpmn_XmlEscaping() {
        // Arrange
        ProcessModel process = new ProcessModel();
        process.setId("escape-test");
        process.setName("Test <>&\"' Characters");
        
        ProcessNode start = createStartEvent("start", "Start <test>");
        ProcessNode task = new ProcessNode("task", NodeType.TASK, "Task & \"Special\"");
        ProcessNode end = createEndEvent("end", "End");
        
        process.addNode(start);
        process.addNode(task);
        process.addNode(end);
        
        ProcessEdge edge = new ProcessEdge("start", "task");
        edge.setLabel("Flow with <special> chars");
        process.addEdge(edge);
        
        process.addEdge(new ProcessEdge("task", "end"));
        
        // Act
        String bpmnXml = bpmnGenerator.generateBpmn(process);
        
        // Assert
        assertNotNull(bpmnXml);
        assertTrue(bpmnXml.contains("Test &lt;&gt;&amp;&quot;&apos; Characters"));
        assertTrue(bpmnXml.contains("Start &lt;test&gt;"));
        assertTrue(bpmnXml.contains("Task &amp; &quot;Special&quot;"));
        assertTrue(bpmnXml.contains("Flow with &lt;special&gt; chars"));
        
        // Verify no unescaped characters
        assertFalse(bpmnXml.contains("name=\"Test <>&\"' Characters\""));
    }
    
    @Test
    void testGenerateBpmn_HasBpmnDiagram() {
        // Arrange
        ProcessModel process = createSimpleProcess();
        
        // Act
        String bpmnXml = bpmnGenerator.generateBpmn(process);
        
        // Assert
        assertTrue(bpmnXml.contains("<bpmndi:BPMNDiagram"));
        assertTrue(bpmnXml.contains("<bpmndi:BPMNPlane"));
        assertTrue(bpmnXml.contains("<bpmndi:BPMNShape"));
        assertTrue(bpmnXml.contains("<bpmndi:BPMNEdge"));
        assertTrue(bpmnXml.contains("<dc:Bounds"));
    }
    
    @Test
    void testGenerateBpmn_ComplexWorkflow() {
        // Arrange
        ProcessModel process = new ProcessModel();
        process.setId("complex-workflow");
        process.setName("Order Processing Workflow");
        
        ProcessNode start = createStartEvent("start", "Order Received");
        ProcessNode validate = new ProcessNode("validate", NodeType.TASK, "Validate Order");
        ProcessNode gateway1 = createGateway("gateway1", "Check Amount", "exclusive");
        ProcessNode autoApprove = new ProcessNode("auto-approve", NodeType.TASK, "Auto Approve");
        ProcessNode manualApprove = new ProcessNode("manual-approve", NodeType.TASK, "Manual Approve");
        ProcessNode gateway2 = createGateway("gateway2", "Merge", "exclusive");
        ProcessNode fulfill = new ProcessNode("fulfill", NodeType.TASK, "Fulfill Order");
        ProcessNode end = createEndEvent("end", "Order Completed");
        
        process.addNode(start);
        process.addNode(validate);
        process.addNode(gateway1);
        process.addNode(autoApprove);
        process.addNode(manualApprove);
        process.addNode(gateway2);
        process.addNode(fulfill);
        process.addNode(end);
        
        process.addEdge(new ProcessEdge("start", "validate"));
        process.addEdge(new ProcessEdge("validate", "gateway1"));
        
        ProcessEdge lowValue = new ProcessEdge("gateway1", "auto-approve");
        lowValue.setCondition("amount <= 1000");
        process.addEdge(lowValue);
        
        ProcessEdge highValue = new ProcessEdge("gateway1", "manual-approve");
        highValue.setCondition("amount > 1000");
        process.addEdge(highValue);
        
        process.addEdge(new ProcessEdge("auto-approve", "gateway2"));
        process.addEdge(new ProcessEdge("manual-approve", "gateway2"));
        process.addEdge(new ProcessEdge("gateway2", "fulfill"));
        process.addEdge(new ProcessEdge("fulfill", "end"));
        
        // Act
        String bpmnXml = bpmnGenerator.generateBpmn(process);
        
        // Assert
        assertNotNull(bpmnXml);
        assertTrue(bpmnXml.length() > 1000); // Should be substantial XML
        
        // Verify all nodes present
        assertTrue(bpmnXml.contains("id=\"start\""));
        assertTrue(bpmnXml.contains("id=\"validate\""));
        assertTrue(bpmnXml.contains("id=\"gateway1\""));
        assertTrue(bpmnXml.contains("id=\"auto-approve\""));
        assertTrue(bpmnXml.contains("id=\"manual-approve\""));
        assertTrue(bpmnXml.contains("id=\"gateway2\""));
        assertTrue(bpmnXml.contains("id=\"fulfill\""));
        assertTrue(bpmnXml.contains("id=\"end\""));
        
        // Verify structure
        assertTrue(bpmnXml.contains("<bpmn2:startEvent"));
        assertTrue(bpmnXml.contains("<bpmn2:endEvent"));
        assertTrue(bpmnXml.contains("<bpmn2:scriptTask"));
        assertTrue(bpmnXml.contains("<bpmn2:exclusiveGateway"));
        assertTrue(bpmnXml.contains("<bpmn2:sequenceFlow"));
        assertTrue(bpmnXml.contains("<bpmn2:conditionExpression"));
    }
    
    // Helper methods
    
    private ProcessModel createSimpleProcess() {
        ProcessModel process = new ProcessModel();
        process.setId("test-process");
        process.setName("Test Process");
        process.setVersion("1.0.0");
        process.setStatus(ProcessStatus.DRAFT);
        
        ProcessNode start = createStartEvent("start", "Start");
        ProcessNode task = new ProcessNode("task", NodeType.TASK, "Do Something");
        ProcessNode end = createEndEvent("end", "End");
        
        process.addNode(start);
        process.addNode(task);
        process.addNode(end);
        
        process.addEdge(new ProcessEdge("start", "task"));
        process.addEdge(new ProcessEdge("task", "end"));
        
        return process;
    }
    
    private ProcessNode createStartEvent(String id, String name) {
        ProcessNode node = new ProcessNode(id, NodeType.EVENT, name);
        node.addProperty("eventType", "start");
        return node;
    }
    
    private ProcessNode createEndEvent(String id, String name) {
        ProcessNode node = new ProcessNode(id, NodeType.EVENT, name);
        node.addProperty("eventType", "end");
        return node;
    }
    
    private ProcessNode createGateway(String id, String name, String gatewayType) {
        ProcessNode node = new ProcessNode(id, NodeType.GATEWAY, name);
        node.addProperty("gatewayType", gatewayType);
        return node;
    }
}

