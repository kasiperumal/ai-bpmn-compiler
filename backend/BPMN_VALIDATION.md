# BPMN Validation Service

## Overview

The `BpmnValidationService` validates BPMN 2.0 XML for structural correctness and logical consistency. It provides detailed error reports with AI-friendly error messages for automated retry and correction.

## Features

### Core Capabilities

1. **XML Structure Validation**
   - Well-formed XML parsing
   - BPMN namespace validation
   - Required element presence
   - Attribute validation

2. **BPMN Element Validation**
   - Process structure
   - Start/End events
   - Unique element IDs
   - Sequence flow references

3. **Logical Constraint Validation**
   - Orphan node detection
   - Unreachable node detection
   - Flow connectivity
   - Graph reachability analysis

4. **Descriptive Error Reporting**
   - Error codes for programmatic handling
   - Human-readable messages
   - Element-level error context
   - Warning vs. error severity

## Validation Rules

### Critical Errors (Block Execution)

| Code | Description | Fix |
|------|-------------|-----|
| `EMPTY_XML` | XML is null or empty | Provide valid BPMN XML |
| `XML_PARSE_ERROR` | XML is malformed | Fix XML syntax errors |
| `MISSING_DEFINITIONS` | Root element not `<definitions>` | Add proper BPMN root |
| `NO_PROCESS` | No process element found | Add `<process>` element |
| `NO_START_EVENT` | Process has no start event | Add exactly one start event |
| `MULTIPLE_START_EVENTS` | Process has multiple start events | Remove extra start events |
| `NO_END_EVENT` | Process has no end event | Add at least one end event |
| `DUPLICATE_ID` | Element IDs are not unique | Ensure all IDs are unique |
| `MISSING_SOURCE_REF` | Sequence flow missing sourceRef | Add sourceRef attribute |
| `MISSING_TARGET_REF` | Sequence flow missing targetRef | Add targetRef attribute |
| `INVALID_SOURCE_REF` | sourceRef references non-existent node | Fix reference to existing node |
| `INVALID_TARGET_REF` | targetRef references non-existent node | Fix reference to existing node |
| `ORPHAN_NODE` | Node has no incoming/outgoing flows | Connect node or remove it |
| `START_EVENT_HAS_INCOMING` | Start event should not have incoming | Remove incoming flows |
| `END_EVENT_HAS_OUTGOING` | End event should not have outgoing | Remove outgoing flows |

### Warnings (Execution Possible, May Be Issues)

| Code | Description | Recommendation |
|------|-------------|----------------|
| `NOT_EXECUTABLE` | Process not marked as executable | Set `isExecutable="true"` |
| `INVALID_NAMESPACE` | Non-standard BPMN namespace | Use standard namespace |
| `MULTIPLE_PROCESSES` | Multiple process definitions | Consider splitting |
| `UNREACHABLE_NODE` | Node not reachable from start | Review flow logic |
| `NO_INCOMING_FLOW` | Node (non-start) has no incoming | May be unreachable |
| `NO_OUTGOING_FLOW` | Node (non-end) has no outgoing | Process may end abruptly |

## Architecture

### Service Structure

```
BpmnValidationService
├── validate(String bpmnXml) → BpmnValidationResult
├── parseXml(String xml) → Document
├── validateBpmnStructure(Document, errors, warnings)
├── validateProcessElements(Document, errors, warnings)
├── validateStartEvents(Element, processId, errors, warnings)
├── validateEndEvents(Element, processId, errors, warnings)
├── validateUniqueIds(Element, processId, errors)
├── validateSequenceFlows(Document, errors, warnings)
├── validateLogicalConstraints(Document, errors, warnings)
├── validateReachability(flowNodes, outgoing, errors, warnings, processId)
├── collectElements(Element) → Map<String, Element>
├── collectFlowNodes(Element) → Map<String, Element>
└── collectFlows(Element) → Map<String, FlowConnection>
```

### Validation Flow

```
validate(bpmnXml)
    ↓
1. Parse XML
    ↓ (if parse fails → CRITICAL ERROR)
2. Validate BPMN Structure
    - Root element
    - Process presence
    - Namespace
    ↓
3. Validate Process Elements
    - Process ID
    - Start events
    - End events
    - Unique IDs
    ↓
4. Validate Sequence Flows
    - Required attributes
    - Valid references
    ↓
5. Validate Logical Constraints
    - Orphan nodes
    - Unreachable nodes
    - Flow connectivity
    ↓
Return BpmnValidationResult
```

## Usage

### Basic Validation

```java
@Autowired
private BpmnValidationService validationService;

public void validateAndHandle(String bpmnXml) {
    BpmnValidationResult result = validationService.validate(bpmnXml);
    
    if (result.isValid()) {
        System.out.println("BPMN is valid!");
    } else {
        System.err.println("Validation failed:");
        for (ValidationError error : result.getErrors()) {
            System.err.println("  [" + error.getCode() + "] " + error.getMessage());
        }
    }
    
    // Check warnings
    for (ValidationWarning warning : result.getWarnings()) {
        System.out.println("  [WARNING] " + warning.getMessage());
    }
}
```

### Integration with BPMN Generator

```java
@Autowired
private BpmnGeneratorService bpmnGenerator;

@Autowired
private BpmnValidationService validationService;

public String generateAndValidate(ProcessModel processModel) {
    // Generate BPMN
    String bpmnXml = bpmnGenerator.generateBpmn(processModel);
    
    // Validate generated BPMN
    BpmnValidationResult result = validationService.validate(bpmnXml);
    
    if (!result.isValid()) {
        throw new RuntimeException(
            "Generated BPMN is invalid: " + result.getErrorSummary()
        );
    }
    
    return bpmnXml;
}
```

### AI Retry Loop

```java
@Autowired
private BpmnValidationService validationService;

@Autowired
private AiOrchestratorService orchestrator;

public String generateWithRetry(String processId, int maxRetries) {
    for (int attempt = 1; attempt <= maxRetries; attempt++) {
        // Generate BPMN (from AI inference)
        String bpmnXml = generateBpmnFromProcess(processId);
        
        // Validate
        BpmnValidationResult result = validationService.validate(bpmnXml);
        
        if (result.isValid()) {
            logger.info("BPMN valid on attempt {}", attempt);
            return bpmnXml;
        }
        
        // Log errors for AI to learn from
        logger.warn("Validation failed on attempt {}:", attempt);
        for (ValidationError error : result.getErrors()) {
            logger.warn("  [{}] {} (element: {})", 
                error.getCode(), 
                error.getMessage(), 
                error.getElementId()
            );
        }
        
        if (attempt < maxRetries) {
            // Feed errors back to AI for correction
            String errorFeedback = buildErrorFeedback(result);
            orchestrator.retry(processId, errorFeedback);
        }
    }
    
    throw new RuntimeException("Failed to generate valid BPMN after " + maxRetries + " attempts");
}

private String buildErrorFeedback(BpmnValidationResult result) {
    StringBuilder feedback = new StringBuilder("The generated BPMN has the following errors:\n");
    for (ValidationError error : result.getErrors()) {
        feedback.append("- ").append(error.getMessage()).append("\n");
    }
    feedback.append("\nPlease correct these issues and regenerate the BPMN.");
    return feedback.toString();
}
```

### Error-Specific Handling

```java
public void handleValidationErrors(BpmnValidationResult result) {
    for (ValidationError error : result.getErrors()) {
        switch (error.getCode()) {
            case "NO_START_EVENT":
                logger.error("Missing start event - add one start event to the process");
                // Auto-fix: inject a start event
                break;
                
            case "ORPHAN_NODE":
                logger.error("Orphan node detected: {}", error.getElementId());
                // Auto-fix: remove orphan node or connect it
                break;
                
            case "INVALID_SOURCE_REF":
            case "INVALID_TARGET_REF":
                logger.error("Invalid reference in flow: {}", error.getMessage());
                // Auto-fix: update reference or remove flow
                break;
                
            case "DUPLICATE_ID":
                logger.error("Duplicate ID found: {}", error.getElementId());
                // Auto-fix: rename duplicate IDs
                break;
                
            default:
                logger.error("Validation error: {}", error.getMessage());
        }
    }
}
```

## BpmnValidationResult

### Structure

```java
public class BpmnValidationResult {
    private boolean valid;
    private List<ValidationError> errors;
    private List<ValidationWarning> warnings;
    
    // Methods
    public boolean isValid();
    public List<ValidationError> getErrors();
    public List<ValidationWarning> getWarnings();
    public String getErrorSummary();
}
```

### ValidationError

```java
public class ValidationError {
    private String code;           // e.g., "NO_START_EVENT"
    private String message;        // Human-readable message
    private String elementId;      // Element that caused error (optional)
    private ErrorSeverity severity; // ERROR or CRITICAL
}
```

### ValidationWarning

```java
public class ValidationWarning {
    private String code;       // e.g., "NOT_EXECUTABLE"
    private String message;    // Human-readable message
    private String elementId;  // Element that caused warning (optional)
}
```

## Examples

### Valid BPMN

```xml
<?xml version="1.0" encoding="UTF-8"?>
<bpmn2:definitions xmlns:bpmn2="http://www.omg.org/spec/BPMN/20100524/MODEL"
                   id="Definitions_1"
                   targetNamespace="http://example.com/bpmn">
  <bpmn2:process id="process-1" name="Valid Process" isExecutable="true">
    <bpmn2:startEvent id="start" name="Start"/>
    <bpmn2:scriptTask id="task" name="Task" scriptFormat="java">
      <bpmn2:script>System.out.println("Hello");</bpmn2:script>
    </bpmn2:scriptTask>
    <bpmn2:endEvent id="end" name="End"/>
    <bpmn2:sequenceFlow id="flow1" sourceRef="start" targetRef="task"/>
    <bpmn2:sequenceFlow id="flow2" sourceRef="task" targetRef="end"/>
  </bpmn2:process>
</bpmn2:definitions>
```

**Result**: ✅ Valid

### Invalid BPMN - No Start Event

```xml
<bpmn2:definitions xmlns:bpmn2="http://www.omg.org/spec/BPMN/20100524/MODEL">
  <bpmn2:process id="process-1" isExecutable="true">
    <bpmn2:scriptTask id="task" name="Task" scriptFormat="java">
      <bpmn2:script>test</bpmn2:script>
    </bpmn2:scriptTask>
    <bpmn2:endEvent id="end" name="End"/>
  </bpmn2:process>
</bpmn2:definitions>
```

**Result**: ❌ `NO_START_EVENT` - Process 'process-1' must have at least one start event

### Invalid BPMN - Orphan Node

```xml
<bpmn2:definitions xmlns:bpmn2="http://www.omg.org/spec/BPMN/20100524/MODEL">
  <bpmn2:process id="process-1" isExecutable="true">
    <bpmn2:startEvent id="start" name="Start"/>
    <bpmn2:scriptTask id="orphan" name="Orphan" scriptFormat="java">
      <bpmn2:script>test</bpmn2:script>
    </bpmn2:scriptTask>
    <bpmn2:endEvent id="end" name="End"/>
    <bpmn2:sequenceFlow id="flow1" sourceRef="start" targetRef="end"/>
  </bpmn2:process>
</bpmn2:definitions>
```

**Result**: ❌ `ORPHAN_NODE` - Node 'orphan' is orphaned (no incoming or outgoing flows)

### Invalid BPMN - Invalid Reference

```xml
<bpmn2:definitions xmlns:bpmn2="http://www.omg.org/spec/BPMN/20100524/MODEL">
  <bpmn2:process id="process-1" isExecutable="true">
    <bpmn2:startEvent id="start" name="Start"/>
    <bpmn2:endEvent id="end" name="End"/>
    <bpmn2:sequenceFlow id="flow1" sourceRef="start" targetRef="nonexistent"/>
  </bpmn2:process>
</bpmn2:definitions>
```

**Result**: ❌ `INVALID_TARGET_REF` - Sequence flow 'flow1' references non-existent target 'nonexistent'

## Testing

### Test Coverage

The `BpmnValidationServiceTest` provides comprehensive coverage:

1. **Valid BPMN** - Ensures valid BPMN passes
2. **XML Parsing** - Null, empty, malformed XML
3. **Structure** - Missing definitions, missing process
4. **Events** - No start, multiple starts, no end
5. **IDs** - Duplicate IDs
6. **Flows** - Missing refs, invalid refs
7. **Logic** - Orphan nodes, unreachable nodes
8. **Events** - Start with incoming, end with outgoing
9. **Warnings** - Not executable, unreachable warnings

### Running Tests

```bash
# Run validation service tests
./gradlew test --tests BpmnValidationServiceTest

# Run all tests
./gradlew test

# Run with verbose output
./gradlew test --tests BpmnValidationServiceTest --info
```

## Best Practices

1. **Always Validate After Generation**
   ```java
   String bpmn = generator.generateBpmn(model);
   BpmnValidationResult result = validator.validate(bpmn);
   if (!result.isValid()) {
       throw new BpmnValidationException(result.getErrorSummary());
   }
   ```

2. **Handle Errors Programmatically**
   ```java
   for (ValidationError error : result.getErrors()) {
       switch (error.getCode()) {
           case "NO_START_EVENT": 
               // Handle specifically
               break;
           // ... other cases
       }
   }
   ```

3. **Log Warnings for Monitoring**
   ```java
   if (!result.getWarnings().isEmpty()) {
       logger.warn("BPMN validation warnings for process {}:", processId);
       result.getWarnings().forEach(w -> 
           logger.warn("  [{}] {}", w.getCode(), w.getMessage())
       );
   }
   ```

4. **Provide Context in Errors**
   ```java
   if (!result.isValid()) {
       throw new BpmnValidationException(
           "Process " + processId + " validation failed: " + 
           result.getErrorSummary()
       );
   }
   ```

5. **Use in AI Feedback Loops**
   ```java
   // Attempt generation
   String bpmn = aiGenerate(processId);
   BpmnValidationResult result = validator.validate(bpmn);
   
   // If invalid, feed errors back to AI
   if (!result.isValid()) {
       String feedback = buildAiFeedback(result);
       aiRetry(processId, feedback);
   }
   ```

## Integration Points

### With BpmnGeneratorService

```java
String bpmn = bpmnGenerator.generateBpmn(processModel);
BpmnValidationResult result = validationService.validate(bpmn);
```

### With AiOrchestratorService

```java
if (!result.isValid()) {
    orchestrator.markAsFailed(processId, result.getErrorSummary());
}
```

### With Kogito Deployment

```java
// Before deploying to Kogito
BpmnValidationResult result = validationService.validate(bpmnXml);
if (result.isValid()) {
    kogito.deploy(bpmnXml);
} else {
    throw new DeploymentException(result.getErrorSummary());
}
```

## Performance Considerations

1. **XML Parsing** - DOM parsing for moderate-sized BPMN (< 10MB)
2. **Graph Algorithms** - BFS for reachability (O(V + E))
3. **Validation Complexity** - Linear in number of elements
4. **Memory** - Entire XML loaded into memory

For very large BPMN files (> 10MB), consider:
- Streaming validation
- Parallel validation of independent processes
- Caching parsed documents

## Future Enhancements

1. **XSD Schema Validation** - Full BPMN 2.0 XSD validation
2. **Custom Rules** - Pluggable validation rules
3. **Auto-Correction** - Suggest fixes for common errors
4. **Semantic Validation** - Business logic validation
5. **Performance Metrics** - Track validation time
6. **Batch Validation** - Validate multiple BPMN files
7. **Streaming Validation** - For very large files

## Related Services

- **`BpmnGeneratorService`** - Generates BPMN that this service validates
- **`AiOrchestratorService`** - Uses validation results for AI retry logic
- **`ProcessReasonerService`** - Creates ProcessModel that becomes BPMN
- **`AiInferenceService`** - Infers process that leads to BPMN generation

## References

- [BPMN 2.0 Specification](https://www.omg.org/spec/BPMN/2.0/)
- [Kogito BPMN Documentation](https://docs.jboss.org/kogito/release/latest/html_single/)
- [W3C XML Specification](https://www.w3.org/TR/xml/)

