# BPMN Generator Service

## Overview

The `BpmnGeneratorService` converts the canonical `ProcessModel` into BPMN 2.0 XML format. It generates executable, Kogito-compatible BPMN processes with proper validation and structure.

## Features

### Core Capabilities

1. **BPMN 2.0 XML Generation**
   - Converts ProcessModel to standard BPMN 2.0 XML
   - Generates executable process definitions
   - Creates visualization-ready diagrams (BPMNDiagram)

2. **Comprehensive Validation**
   - Ensures exactly one start event
   - Requires at least one end event
   - Detects disconnected nodes
   - Validates gateway balance (warns about splits/joins)

3. **Node Type Support**
   - **Events**: Start, End, Intermediate
   - **Tasks**: ScriptTask (extensible to other task types)
   - **Gateways**: Exclusive, Parallel, Inclusive

4. **Edge Features**
   - Sequence flows with optional conditions
   - Conditional expressions for gateway branches
   - Labels for visual clarity

5. **Kogito Compatibility**
   - Generates Kogito-compatible BPMN
   - Executable process definitions
   - Standard BPMN 2.0 compliance

## Architecture

### Service Structure

```
BpmnGeneratorService
├── generateBpmn(ProcessModel) → String
├── validateModel(ProcessModel)
├── validateGatewayBalance(ProcessModel)
├── generateNode(StringBuilder, ProcessNode)
├── generateEvent(StringBuilder, ProcessNode)
├── generateTask(StringBuilder, ProcessNode)
├── generateGateway(StringBuilder, ProcessNode)
├── generateEdge(StringBuilder, ProcessEdge, ProcessModel)
├── generateDiagram(StringBuilder, ProcessModel)
└── escapeXml(String) → String
```

### Validation Rules

1. **Start Event**
   - Exactly one start event required
   - Identified by: `NodeType.EVENT` + `eventType=start`

2. **End Event**
   - At least one end event required
   - Identified by: `NodeType.EVENT` + `eventType=end`

3. **Gateway Balance**
   - Splits: Gateway with 1 incoming, multiple outgoing
   - Joins: Gateway with multiple incoming, 1 outgoing
   - Logs warnings for potentially unbalanced gateways

4. **Disconnected Nodes**
   - Warns about nodes not connected via edges
   - Start events allowed without incoming edges
   - End events allowed without outgoing edges

## Usage

### Basic Example

```java
@Autowired
private BpmnGeneratorService bpmnGenerator;

public String generateBpmnForProcess(ProcessModel process) {
    try {
        String bpmnXml = bpmnGenerator.generateBpmn(process);
        return bpmnXml;
    } catch (BpmnValidationException e) {
        // Handle validation errors
        logger.error("BPMN validation failed: {}", e.getMessage());
        throw e;
    }
}
```

### Simple Linear Process

```java
ProcessModel process = new ProcessModel();
process.setId("simple-process");
process.setName("Simple Order Process");

// Create nodes
ProcessNode start = new ProcessNode("start", NodeType.EVENT, "Start");
start.addProperty("eventType", "start");

ProcessNode validate = new ProcessNode("validate", NodeType.TASK, "Validate Order");
ProcessNode end = new ProcessNode("end", NodeType.EVENT, "End");
end.addProperty("eventType", "end");

// Add nodes
process.addNode(start);
process.addNode(validate);
process.addNode(end);

// Connect nodes
process.addEdge(new ProcessEdge("start", "validate"));
process.addEdge(new ProcessEdge("validate", "end"));

// Generate BPMN
String bpmnXml = bpmnGenerator.generateBpmn(process);
```

### Process with Exclusive Gateway

```java
ProcessModel process = new ProcessModel();
process.setId("conditional-process");
process.setName("Order Approval Process");

// Create nodes
ProcessNode start = createStartEvent("start", "Order Received");
ProcessNode gateway = createGateway("gateway", "Check Amount", "exclusive");
ProcessNode autoApprove = new ProcessNode("auto", NodeType.TASK, "Auto Approve");
ProcessNode manualApprove = new ProcessNode("manual", NodeType.TASK, "Manual Approve");
ProcessNode end = createEndEvent("end", "Completed");

// Add nodes
process.addNode(start);
process.addNode(gateway);
process.addNode(autoApprove);
process.addNode(manualApprove);
process.addNode(end);

// Connect with conditions
process.addEdge(new ProcessEdge("start", "gateway"));

ProcessEdge lowValue = new ProcessEdge("gateway", "auto");
lowValue.setCondition("amount <= 1000");
lowValue.setLabel("Low Value");
process.addEdge(lowValue);

ProcessEdge highValue = new ProcessEdge("gateway", "manual");
highValue.setCondition("amount > 1000");
highValue.setLabel("High Value");
process.addEdge(highValue);

process.addEdge(new ProcessEdge("auto", "end"));
process.addEdge(new ProcessEdge("manual", "end"));

// Generate BPMN
String bpmnXml = bpmnGenerator.generateBpmn(process);
```

### Process with Parallel Gateway

```java
ProcessModel process = new ProcessModel();
process.setId("parallel-process");
process.setName("Parallel Tasks Process");

// Create nodes
ProcessNode start = createStartEvent("start", "Start");
ProcessNode split = createGateway("split", "Split", "parallel");
ProcessNode task1 = new ProcessNode("task1", NodeType.TASK, "Task 1");
ProcessNode task2 = new ProcessNode("task2", NodeType.TASK, "Task 2");
ProcessNode join = createGateway("join", "Join", "parallel");
ProcessNode end = createEndEvent("end", "End");

// Add nodes
process.addNode(start);
process.addNode(split);
process.addNode(task1);
process.addNode(task2);
process.addNode(join);
process.addNode(end);

// Connect for parallel execution
process.addEdge(new ProcessEdge("start", "split"));
process.addEdge(new ProcessEdge("split", "task1"));
process.addEdge(new ProcessEdge("split", "task2"));
process.addEdge(new ProcessEdge("task1", "join"));
process.addEdge(new ProcessEdge("task2", "join"));
process.addEdge(new ProcessEdge("join", "end"));

// Generate BPMN
String bpmnXml = bpmnGenerator.generateBpmn(process);
```

## BPMN Output Structure

### Generated XML Structure

```xml
<?xml version="1.0" encoding="UTF-8"?>
<bpmn2:definitions 
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:bpmn2="http://www.omg.org/spec/BPMN/20100524/MODEL"
    xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
    xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
    xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
    id="Definitions_process-id"
    targetNamespace="http://example.com/bpmn"
    exporter="AI-BPMN-Compiler"
    exporterVersion="1.0">
    
  <bpmn2:process id="process-id" name="Process Name" isExecutable="true">
    <!-- Start Event -->
    <bpmn2:startEvent id="start" name="Start"/>
    
    <!-- Tasks -->
    <bpmn2:scriptTask id="task-1" name="Task Name" scriptFormat="java">
      <bpmn2:script>System.out.println("Executing: Task Name");</bpmn2:script>
    </bpmn2:scriptTask>
    
    <!-- Gateways -->
    <bpmn2:exclusiveGateway id="gateway-1" name="Decision" />
    
    <!-- End Event -->
    <bpmn2:endEvent id="end" name="End"/>
    
    <!-- Sequence Flows -->
    <bpmn2:sequenceFlow id="flow-1" sourceRef="start" targetRef="task-1" />
    <bpmn2:sequenceFlow id="flow-2" sourceRef="task-1" targetRef="gateway-1" />
    
    <!-- Conditional Flow -->
    <bpmn2:sequenceFlow id="flow-3" sourceRef="gateway-1" targetRef="end" name="Yes">
      <bpmn2:conditionExpression xsi:type="bpmn2:tFormalExpression">
        condition == true
      </bpmn2:conditionExpression>
    </bpmn2:sequenceFlow>
  </bpmn2:process>
  
  <!-- BPMN Diagram for Visualization -->
  <bpmndi:BPMNDiagram id="BPMNDiagram_process-id">
    <bpmndi:BPMNPlane id="BPMNPlane_process-id" bpmnElement="process-id">
      <bpmndi:BPMNShape id="Shape_start" bpmnElement="start">
        <dc:Bounds x="100" y="100" width="100" height="80" />
      </bpmndi:BPMNShape>
      <!-- More shapes... -->
      <bpmndi:BPMNEdge id="Edge_flow-1" bpmnElement="flow-1" />
      <!-- More edges... -->
    </bpmndi:BPMPlane>
  </bpmndi:BPMNDiagram>
</bpmn2:definitions>
```

## Node Type Mapping

### Events

| ProcessNode Configuration | BPMN Element |
|---------------------------|--------------|
| `NodeType.EVENT` + `eventType=start` | `<bpmn2:startEvent>` |
| `NodeType.EVENT` + `eventType=end` | `<bpmn2:endEvent>` |
| `NodeType.EVENT` (other) | `<bpmn2:intermediateCatchEvent>` |

### Tasks

| ProcessNode Configuration | BPMN Element |
|---------------------------|--------------|
| `NodeType.TASK` | `<bpmn2:scriptTask>` |

*Note: Currently generates ScriptTask. Can be extended for UserTask, ServiceTask, etc.*

### Gateways

| ProcessNode Configuration | BPMN Element |
|---------------------------|--------------|
| `NodeType.GATEWAY` + `gatewayType=exclusive` | `<bpmn2:exclusiveGateway>` |
| `NodeType.GATEWAY` + `gatewayType=parallel` | `<bpmn2:parallelGateway>` |
| `NodeType.GATEWAY` + `gatewayType=inclusive` | `<bpmn2:inclusiveGateway>` |
| `NodeType.GATEWAY` (default) | `<bpmn2:exclusiveGateway>` |

## Error Handling

### BpmnValidationException

Thrown when the ProcessModel fails validation:

```java
try {
    String bpmnXml = bpmnGenerator.generateBpmn(process);
} catch (BpmnValidationException e) {
    // Validation failed
    System.err.println("Validation error: " + e.getMessage());
    // Example messages:
    // - "Process must have at least one node"
    // - "Process must have exactly one start event (found 0)"
    // - "Process must have exactly one start event (found 2)"
    // - "Process must have at least one end event (found 0)"
}
```

### Common Validation Errors

1. **Missing Start Event**
   ```
   BPMN validation failed: Process must have exactly one start event (found 0)
   ```
   **Fix**: Add a start event node with `eventType=start` property.

2. **Multiple Start Events**
   ```
   BPMN validation failed: Process must have exactly one start event (found 2)
   ```
   **Fix**: Remove duplicate start events, keep only one.

3. **Missing End Event**
   ```
   BPMN validation failed: Process must have at least one end event (found 0)
   ```
   **Fix**: Add at least one end event node with `eventType=end` property.

4. **Empty Process**
   ```
   BPMN validation failed: Process must have at least one node
   ```
   **Fix**: Add nodes to the process model.

## XML Escaping

The service automatically escapes XML special characters:

| Character | Escaped To |
|-----------|------------|
| `&` | `&amp;` |
| `<` | `&lt;` |
| `>` | `&gt;` |
| `"` | `&quot;` |
| `'` | `&apos;` |

Example:
```java
ProcessNode task = new ProcessNode("task", NodeType.TASK, "Check if amount > 1000");
// Generated XML: name="Check if amount &gt; 1000"
```

## Auto-Layout

The service generates a basic auto-layout for the BPMN diagram:

- **Initial Position**: (100, 100)
- **Horizontal Spacing**: 200px between columns
- **Vertical Spacing**: 100px between rows
- **Layout**: 4 nodes per row, then moves to next row

*Note: This is a simple layout. For complex processes, use a BPMN modeler to adjust the layout.*

## Integration with Kogito

The generated BPMN is Kogito-compatible:

1. **Executable Flag**: `isExecutable="true"`
2. **BPMN 2.0 Standard**: Compliant with OMG BPMN 2.0
3. **Namespace**: Standard BPMN namespaces
4. **Process ID**: Unique process identifier

### Example Kogito Deployment

```java
// 1. Generate BPMN
String bpmnXml = bpmnGenerator.generateBpmn(processModel);

// 2. Save to resources (for Kogito to pick up)
Path bpmnFile = Paths.get("src/main/resources/processes/" + processModel.getId() + ".bpmn");
Files.writeString(bpmnFile, bpmnXml, StandardCharsets.UTF_8);

// 3. Kogito will auto-generate REST endpoints for this process
// GET  /process/{processId}
// POST /process/{processId}
// etc.
```

## Testing

### Test Coverage

The `BpmnGeneratorServiceTest` provides comprehensive test coverage:

1. **Basic Functionality**
   - Simple process generation
   - Null/empty process validation

2. **Validation Tests**
   - No start event
   - Multiple start events
   - No end event

3. **Node Type Tests**
   - Events (start, end, intermediate)
   - Tasks
   - Gateways (exclusive, parallel, inclusive)

4. **Edge Tests**
   - Simple sequence flows
   - Conditional flows

5. **Complex Workflows**
   - Multi-gateway processes
   - Parallel execution
   - Conditional branching

6. **XML Generation**
   - XML escaping
   - BPMN diagram generation
   - Proper structure

### Running Tests

```bash
./gradlew test --tests BpmnGeneratorServiceTest
```

## Future Enhancements

1. **Task Types**
   - UserTask
   - ServiceTask
   - ManualTask
   - BusinessRuleTask

2. **Event Types**
   - Timer Events
   - Message Events
   - Error Events
   - Signal Events

3. **Advanced Gateways**
   - Event-based Gateway
   - Complex Gateway

4. **Sub-processes**
   - Embedded Sub-process
   - Call Activity

5. **Data Objects**
   - Data Input/Output
   - Data Stores

6. **Advanced Layout**
   - Intelligent auto-layout algorithm
   - Hierarchical layout
   - Force-directed layout

7. **Validation Enhancements**
   - Cycle detection
   - Unreachable node detection
   - Dead-end detection

8. **DRL Integration**
   - Link business rules to Business Rule Tasks
   - Generate rule-based decision nodes

## Best Practices

1. **Node IDs**: Use meaningful, unique IDs for nodes
2. **Node Names**: Use clear, descriptive names
3. **Gateway Balance**: Ensure every split has a corresponding join
4. **Conditions**: Write clear, evaluable condition expressions
5. **Start/End Events**: Always have exactly one start and at least one end
6. **Testing**: Validate generated BPMN in a BPMN modeler before deployment

## Related Services

- **ProcessReasonerService**: Infers ProcessModel from text descriptions
- **AiInferenceService**: Extracts process from images
- **DrlGeneratorService**: (Future) Generates Drools rules from RuleModels
- **AiOrchestratorService**: Manages the AI workflow state

## References

- [BPMN 2.0 Specification](https://www.omg.org/spec/BPMN/2.0/)
- [Kogito Documentation](https://docs.jboss.org/kogito/release/latest/html_single/)
- [jBPM Documentation](https://www.jbpm.org/documentation.html)

