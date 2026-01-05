# BPMN Generator Service - Quick Summary

## Purpose
Converts canonical `ProcessModel` to BPMN 2.0 XML for Kogito execution.

## Key Features
- ✅ BPMN 2.0 XML generation
- ✅ Executable process definitions (`isExecutable="true"`)
- ✅ Validation (one start, at least one end)
- ✅ Gateway balance checking
- ✅ Auto-layout for visualization
- ✅ XML escaping
- ✅ Kogito compatible

## Supported Elements

### Events
- **Start Event**: `NodeType.EVENT` + `eventType=start`
- **End Event**: `NodeType.EVENT` + `eventType=end`
- **Intermediate Event**: `NodeType.EVENT` (other)

### Tasks
- **Script Task**: `NodeType.TASK` (generates `<bpmn2:scriptTask>`)

### Gateways
- **Exclusive**: `NodeType.GATEWAY` + `gatewayType=exclusive` (default)
- **Parallel**: `NodeType.GATEWAY` + `gatewayType=parallel`
- **Inclusive**: `NodeType.GATEWAY` + `gatewayType=inclusive`

### Edges
- **Sequence Flow**: Basic connection
- **Conditional Flow**: With `condition` expression

## Usage

```java
@Autowired
private BpmnGeneratorService bpmnGenerator;

// Generate BPMN
String bpmnXml = bpmnGenerator.generateBpmn(processModel);

// Save to file
Files.writeString(Paths.get("process.bpmn"), bpmnXml);
```

## Quick Example

```java
// Create process
ProcessModel process = new ProcessModel();
process.setId("order-process");
process.setName("Order Processing");

// Add start event
ProcessNode start = new ProcessNode("start", NodeType.EVENT, "Start");
start.addProperty("eventType", "start");
process.addNode(start);

// Add task
ProcessNode task = new ProcessNode("validate", NodeType.TASK, "Validate Order");
process.addNode(task);

// Add end event
ProcessNode end = new ProcessNode("end", NodeType.EVENT, "End");
end.addProperty("eventType", "end");
process.addNode(end);

// Connect
process.addEdge(new ProcessEdge("start", "validate"));
process.addEdge(new ProcessEdge("validate", "end"));

// Generate
String bpmnXml = bpmnGenerator.generateBpmn(process);
```

## Validation Rules

| Rule | Description |
|------|-------------|
| **One Start** | Exactly 1 start event required |
| **One+ End** | At least 1 end event required |
| **Non-Empty** | Process must have nodes |
| **Gateway Balance** | Warns about unbalanced gateways |

## Output Structure

```xml
<?xml version="1.0" encoding="UTF-8"?>
<bpmn2:definitions ...>
  <bpmn2:process id="..." name="..." isExecutable="true">
    <bpmn2:startEvent id="..." name="..."/>
    <bpmn2:scriptTask id="..." name="..." scriptFormat="java">
      <bpmn2:script>...</bpmn2:script>
    </bpmn2:scriptTask>
    <bpmn2:exclusiveGateway id="..." name="..."/>
    <bpmn2:sequenceFlow id="..." sourceRef="..." targetRef="...">
      <bpmn2:conditionExpression>...</bpmn2:conditionExpression>
    </bpmn2:sequenceFlow>
    <bpmn2:endEvent id="..." name="..."/>
  </bpmn2:process>
  <bpmndi:BPMNDiagram>
    <!-- Visualization layout -->
  </bpmndi:BPMNDiagram>
</bpmn2:definitions>
```

## Error Handling

```java
try {
    String bpmn = bpmnGenerator.generateBpmn(process);
} catch (BpmnValidationException e) {
    // Handle validation errors
    System.err.println("Validation failed: " + e.getMessage());
}
```

### Common Errors

| Error | Fix |
|-------|-----|
| `Process must have exactly one start event (found 0)` | Add start event with `eventType=start` |
| `Process must have at least one end event (found 0)` | Add end event with `eventType=end` |
| `Process must have at least one node` | Add nodes to process |

## Testing

```bash
# Run BPMN Generator tests
./gradlew test --tests BpmnGeneratorServiceTest

# All tests
./gradlew test
```

## Files

- **Service**: `BpmnGeneratorService.java`
- **Exception**: `BpmnValidationException.java`
- **Tests**: `BpmnGeneratorServiceTest.java`
- **Docs**: `BPMN_GENERATOR.md`

## Integration Points

- **Input**: `ProcessModel` (canonical Java model)
- **Output**: BPMN 2.0 XML `String`
- **Used By**: Future BPMN publishing/deployment services
- **Works With**: Kogito runtime

## Next Steps

1. **DRL Generator**: Generate Drools rules from `RuleModel`
2. **Deployment Service**: Deploy BPMN to Kogito runtime
3. **Enhanced Tasks**: Support UserTask, ServiceTask, etc.
4. **Advanced Events**: Timer, Message, Error events

## Related Services

- `ProcessReasonerService` - Infers `ProcessModel` from text
- `AiInferenceService` - Extracts process from images
- `AiOrchestratorService` - Manages AI workflow state

## Quick Reference

```java
// Helper methods for creating nodes
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

private ProcessNode createGateway(String id, String name, String type) {
    ProcessNode node = new ProcessNode(id, NodeType.GATEWAY, name);
    node.addProperty("gatewayType", type); // "exclusive", "parallel", "inclusive"
    return node;
}
```

---

For detailed documentation, see [BPMN_GENERATOR.md](./BPMN_GENERATOR.md)

