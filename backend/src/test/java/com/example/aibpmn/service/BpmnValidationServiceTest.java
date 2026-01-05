package com.example.aibpmn.service;

import com.example.aibpmn.dto.BpmnValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BpmnValidationServiceTest {
    
    private BpmnValidationService validationService;
    
    @BeforeEach
    void setUp() {
        validationService = new BpmnValidationService();
    }
    
    @Test
    void testValidate_ValidBpmn() {
        String bpmnXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <bpmn2:definitions xmlns:bpmn2="http://www.omg.org/spec/BPMN/20100524/MODEL"
                               id="Definitions_1"
                               targetNamespace="http://example.com/bpmn">
              <bpmn2:process id="process-1" name="Test Process" isExecutable="true">
                <bpmn2:startEvent id="start" name="Start"/>
                <bpmn2:scriptTask id="task" name="Task" scriptFormat="java">
                  <bpmn2:script>System.out.println("test");</bpmn2:script>
                </bpmn2:scriptTask>
                <bpmn2:endEvent id="end" name="End"/>
                <bpmn2:sequenceFlow id="flow1" sourceRef="start" targetRef="task"/>
                <bpmn2:sequenceFlow id="flow2" sourceRef="task" targetRef="end"/>
              </bpmn2:process>
            </bpmn2:definitions>
            """;
        
        BpmnValidationResult result = validationService.validate(bpmnXml);
        
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }
    
    @Test
    void testValidate_NullXml() {
        BpmnValidationResult result = validationService.validate(null);
        
        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals("EMPTY_XML", result.getErrors().get(0).getCode());
    }
    
    @Test
    void testValidate_EmptyXml() {
        BpmnValidationResult result = validationService.validate("");
        
        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals("EMPTY_XML", result.getErrors().get(0).getCode());
    }
    
    @Test
    void testValidate_InvalidXml() {
        String invalidXml = "<bpmn2:definitions><invalid>";
        
        BpmnValidationResult result = validationService.validate(invalidXml);
        
        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals("XML_PARSE_ERROR", result.getErrors().get(0).getCode());
    }
    
    @Test
    void testValidate_MissingDefinitions() {
        String bpmnXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <root>
              <invalid/>
            </root>
            """;
        
        BpmnValidationResult result = validationService.validate(bpmnXml);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.getCode().equals("MISSING_DEFINITIONS")));
    }
    
    @Test
    void testValidate_NoProcess() {
        String bpmnXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <bpmn2:definitions xmlns:bpmn2="http://www.omg.org/spec/BPMN/20100524/MODEL"
                               id="Definitions_1">
            </bpmn2:definitions>
            """;
        
        BpmnValidationResult result = validationService.validate(bpmnXml);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.getCode().equals("NO_PROCESS")));
    }
    
    @Test
    void testValidate_NoStartEvent() {
        String bpmnXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <bpmn2:definitions xmlns:bpmn2="http://www.omg.org/spec/BPMN/20100524/MODEL"
                               id="Definitions_1">
              <bpmn2:process id="process-1" isExecutable="true">
                <bpmn2:scriptTask id="task" name="Task" scriptFormat="java">
                  <bpmn2:script>test</bpmn2:script>
                </bpmn2:scriptTask>
                <bpmn2:endEvent id="end" name="End"/>
              </bpmn2:process>
            </bpmn2:definitions>
            """;
        
        BpmnValidationResult result = validationService.validate(bpmnXml);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.getCode().equals("NO_START_EVENT")));
    }
    
    @Test
    void testValidate_MultipleStartEvents() {
        String bpmnXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <bpmn2:definitions xmlns:bpmn2="http://www.omg.org/spec/BPMN/20100524/MODEL"
                               id="Definitions_1">
              <bpmn2:process id="process-1" isExecutable="true">
                <bpmn2:startEvent id="start1" name="Start 1"/>
                <bpmn2:startEvent id="start2" name="Start 2"/>
                <bpmn2:endEvent id="end" name="End"/>
              </bpmn2:process>
            </bpmn2:definitions>
            """;
        
        BpmnValidationResult result = validationService.validate(bpmnXml);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.getCode().equals("MULTIPLE_START_EVENTS")));
    }
    
    @Test
    void testValidate_NoEndEvent() {
        String bpmnXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <bpmn2:definitions xmlns:bpmn2="http://www.omg.org/spec/BPMN/20100524/MODEL"
                               id="Definitions_1">
              <bpmn2:process id="process-1" isExecutable="true">
                <bpmn2:startEvent id="start" name="Start"/>
                <bpmn2:scriptTask id="task" name="Task" scriptFormat="java">
                  <bpmn2:script>test</bpmn2:script>
                </bpmn2:scriptTask>
              </bpmn2:process>
            </bpmn2:definitions>
            """;
        
        BpmnValidationResult result = validationService.validate(bpmnXml);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.getCode().equals("NO_END_EVENT")));
    }
    
    @Test
    void testValidate_DuplicateIds() {
        String bpmnXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <bpmn2:definitions xmlns:bpmn2="http://www.omg.org/spec/BPMN/20100524/MODEL"
                               id="Definitions_1">
              <bpmn2:process id="process-1" isExecutable="true">
                <bpmn2:startEvent id="node1" name="Start"/>
                <bpmn2:scriptTask id="node1" name="Task" scriptFormat="java">
                  <bpmn2:script>test</bpmn2:script>
                </bpmn2:scriptTask>
                <bpmn2:endEvent id="end" name="End"/>
              </bpmn2:process>
            </bpmn2:definitions>
            """;
        
        BpmnValidationResult result = validationService.validate(bpmnXml);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.getCode().equals("DUPLICATE_ID")));
    }
    
    @Test
    void testValidate_MissingSourceRef() {
        String bpmnXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <bpmn2:definitions xmlns:bpmn2="http://www.omg.org/spec/BPMN/20100524/MODEL"
                               id="Definitions_1">
              <bpmn2:process id="process-1" isExecutable="true">
                <bpmn2:startEvent id="start" name="Start"/>
                <bpmn2:endEvent id="end" name="End"/>
                <bpmn2:sequenceFlow id="flow1" targetRef="end"/>
              </bpmn2:process>
            </bpmn2:definitions>
            """;
        
        BpmnValidationResult result = validationService.validate(bpmnXml);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.getCode().equals("MISSING_SOURCE_REF")));
    }
    
    @Test
    void testValidate_MissingTargetRef() {
        String bpmnXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <bpmn2:definitions xmlns:bpmn2="http://www.omg.org/spec/BPMN/20100524/MODEL"
                               id="Definitions_1">
              <bpmn2:process id="process-1" isExecutable="true">
                <bpmn2:startEvent id="start" name="Start"/>
                <bpmn2:endEvent id="end" name="End"/>
                <bpmn2:sequenceFlow id="flow1" sourceRef="start"/>
              </bpmn2:process>
            </bpmn2:definitions>
            """;
        
        BpmnValidationResult result = validationService.validate(bpmnXml);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.getCode().equals("MISSING_TARGET_REF")));
    }
    
    @Test
    void testValidate_InvalidSourceRef() {
        String bpmnXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <bpmn2:definitions xmlns:bpmn2="http://www.omg.org/spec/BPMN/20100524/MODEL"
                               id="Definitions_1">
              <bpmn2:process id="process-1" isExecutable="true">
                <bpmn2:startEvent id="start" name="Start"/>
                <bpmn2:endEvent id="end" name="End"/>
                <bpmn2:sequenceFlow id="flow1" sourceRef="nonexistent" targetRef="end"/>
              </bpmn2:process>
            </bpmn2:definitions>
            """;
        
        BpmnValidationResult result = validationService.validate(bpmnXml);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.getCode().equals("INVALID_SOURCE_REF")));
    }
    
    @Test
    void testValidate_InvalidTargetRef() {
        String bpmnXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <bpmn2:definitions xmlns:bpmn2="http://www.omg.org/spec/BPMN/20100524/MODEL"
                               id="Definitions_1">
              <bpmn2:process id="process-1" isExecutable="true">
                <bpmn2:startEvent id="start" name="Start"/>
                <bpmn2:endEvent id="end" name="End"/>
                <bpmn2:sequenceFlow id="flow1" sourceRef="start" targetRef="nonexistent"/>
              </bpmn2:process>
            </bpmn2:definitions>
            """;
        
        BpmnValidationResult result = validationService.validate(bpmnXml);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.getCode().equals("INVALID_TARGET_REF")));
    }
    
    @Test
    void testValidate_OrphanNode() {
        String bpmnXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <bpmn2:definitions xmlns:bpmn2="http://www.omg.org/spec/BPMN/20100524/MODEL"
                               id="Definitions_1">
              <bpmn2:process id="process-1" isExecutable="true">
                <bpmn2:startEvent id="start" name="Start"/>
                <bpmn2:scriptTask id="orphan" name="Orphan Task" scriptFormat="java">
                  <bpmn2:script>test</bpmn2:script>
                </bpmn2:scriptTask>
                <bpmn2:endEvent id="end" name="End"/>
                <bpmn2:sequenceFlow id="flow1" sourceRef="start" targetRef="end"/>
              </bpmn2:process>
            </bpmn2:definitions>
            """;
        
        BpmnValidationResult result = validationService.validate(bpmnXml);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.getCode().equals("ORPHAN_NODE") && 
                          e.getMessage().contains("orphan")));
    }
    
    @Test
    void testValidate_StartEventWithIncoming() {
        String bpmnXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <bpmn2:definitions xmlns:bpmn2="http://www.omg.org/spec/BPMN/20100524/MODEL"
                               id="Definitions_1">
              <bpmn2:process id="process-1" isExecutable="true">
                <bpmn2:startEvent id="start" name="Start">
                  <bpmn2:incoming>flow1</bpmn2:incoming>
                </bpmn2:startEvent>
                <bpmn2:scriptTask id="task" name="Task" scriptFormat="java">
                  <bpmn2:script>test</bpmn2:script>
                </bpmn2:scriptTask>
                <bpmn2:endEvent id="end" name="End"/>
                <bpmn2:sequenceFlow id="flow1" sourceRef="task" targetRef="start"/>
              </bpmn2:process>
            </bpmn2:definitions>
            """;
        
        BpmnValidationResult result = validationService.validate(bpmnXml);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.getCode().equals("START_EVENT_HAS_INCOMING")));
    }
    
    @Test
    void testValidate_EndEventWithOutgoing() {
        String bpmnXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <bpmn2:definitions xmlns:bpmn2="http://www.omg.org/spec/BPMN/20100524/MODEL"
                               id="Definitions_1">
              <bpmn2:process id="process-1" isExecutable="true">
                <bpmn2:startEvent id="start" name="Start"/>
                <bpmn2:endEvent id="end" name="End">
                  <bpmn2:outgoing>flow2</bpmn2:outgoing>
                </bpmn2:endEvent>
                <bpmn2:scriptTask id="task" name="Task" scriptFormat="java">
                  <bpmn2:script>test</bpmn2:script>
                </bpmn2:scriptTask>
                <bpmn2:sequenceFlow id="flow2" sourceRef="end" targetRef="task"/>
              </bpmn2:process>
            </bpmn2:definitions>
            """;
        
        BpmnValidationResult result = validationService.validate(bpmnXml);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.getCode().equals("END_EVENT_HAS_OUTGOING")));
    }
    
    @Test
    void testValidate_WithGateways() {
        String bpmnXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <bpmn2:definitions xmlns:bpmn2="http://www.omg.org/spec/BPMN/20100524/MODEL"
                               id="Definitions_1">
              <bpmn2:process id="process-1" isExecutable="true">
                <bpmn2:startEvent id="start" name="Start"/>
                <bpmn2:exclusiveGateway id="gateway" name="Gateway"/>
                <bpmn2:scriptTask id="task1" name="Task 1" scriptFormat="java">
                  <bpmn2:script>test1</bpmn2:script>
                </bpmn2:scriptTask>
                <bpmn2:scriptTask id="task2" name="Task 2" scriptFormat="java">
                  <bpmn2:script>test2</bpmn2:script>
                </bpmn2:scriptTask>
                <bpmn2:endEvent id="end" name="End"/>
                <bpmn2:sequenceFlow id="flow1" sourceRef="start" targetRef="gateway"/>
                <bpmn2:sequenceFlow id="flow2" sourceRef="gateway" targetRef="task1"/>
                <bpmn2:sequenceFlow id="flow3" sourceRef="gateway" targetRef="task2"/>
                <bpmn2:sequenceFlow id="flow4" sourceRef="task1" targetRef="end"/>
                <bpmn2:sequenceFlow id="flow5" sourceRef="task2" targetRef="end"/>
              </bpmn2:process>
            </bpmn2:definitions>
            """;
        
        BpmnValidationResult result = validationService.validate(bpmnXml);
        
        assertTrue(result.isValid());
    }
    
    @Test
    void testValidate_NotExecutable_Warning() {
        String bpmnXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <bpmn2:definitions xmlns:bpmn2="http://www.omg.org/spec/BPMN/20100524/MODEL"
                               id="Definitions_1">
              <bpmn2:process id="process-1" isExecutable="false">
                <bpmn2:startEvent id="start" name="Start"/>
                <bpmn2:endEvent id="end" name="End"/>
                <bpmn2:sequenceFlow id="flow1" sourceRef="start" targetRef="end"/>
              </bpmn2:process>
            </bpmn2:definitions>
            """;
        
        BpmnValidationResult result = validationService.validate(bpmnXml);
        
        assertTrue(result.isValid());
        assertTrue(result.getWarnings().stream()
            .anyMatch(w -> w.getCode().equals("NOT_EXECUTABLE")));
    }
    
    @Test
    void testValidate_UnreachableNode_Warning() {
        String bpmnXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <bpmn2:definitions xmlns:bpmn2="http://www.omg.org/spec/BPMN/20100524/MODEL"
                               id="Definitions_1">
              <bpmn2:process id="process-1" isExecutable="true">
                <bpmn2:startEvent id="start" name="Start"/>
                <bpmn2:scriptTask id="task1" name="Task 1" scriptFormat="java">
                  <bpmn2:script>test1</bpmn2:script>
                </bpmn2:scriptTask>
                <bpmn2:scriptTask id="task2" name="Task 2" scriptFormat="java">
                  <bpmn2:script>test2</bpmn2:script>
                </bpmn2:scriptTask>
                <bpmn2:endEvent id="end" name="End"/>
                <bpmn2:sequenceFlow id="flow1" sourceRef="start" targetRef="task1"/>
                <bpmn2:sequenceFlow id="flow2" sourceRef="task1" targetRef="end"/>
                <bpmn2:sequenceFlow id="flow3" sourceRef="task2" targetRef="end"/>
              </bpmn2:process>
            </bpmn2:definitions>
            """;
        
        BpmnValidationResult result = validationService.validate(bpmnXml);
        
        assertTrue(result.isValid());
        assertTrue(result.getWarnings().stream()
            .anyMatch(w -> w.getCode().equals("UNREACHABLE_NODE") || 
                          w.getCode().equals("NO_INCOMING_FLOW")));
    }
    
    @Test
    void testValidationResult_GetErrorSummary() {
        String bpmnXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <bpmn2:definitions xmlns:bpmn2="http://www.omg.org/spec/BPMN/20100524/MODEL"
                               id="Definitions_1">
              <bpmn2:process id="process-1" isExecutable="true">
                <bpmn2:scriptTask id="task" name="Task" scriptFormat="java">
                  <bpmn2:script>test</bpmn2:script>
                </bpmn2:scriptTask>
              </bpmn2:process>
            </bpmn2:definitions>
            """;
        
        BpmnValidationResult result = validationService.validate(bpmnXml);
        
        assertFalse(result.isValid());
        String summary = result.getErrorSummary();
        assertNotNull(summary);
        assertTrue(summary.contains("Validation failed"));
        assertTrue(summary.contains("error"));
    }
}

