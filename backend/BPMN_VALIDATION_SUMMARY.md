# BPMN Validation Service - Quick Summary

## Purpose
Validates BPMN 2.0 XML for structural correctness and logical consistency. Provides AI-friendly error messages for automated retry.

## Key Features
- ✅ XML structure validation
- ✅ BPMN element validation (start/end events, flows)
- ✅ Logical constraint validation (orphan nodes, unreachability)
- ✅ Descriptive error codes and messages
- ✅ Error vs. warning severity
- ✅ Element-level error context

## Usage

### Basic Validation

```java
@Autowired
private BpmnValidationService validationService;

BpmnValidationResult result = validationService.validate(bpmnXml);

if (result.isValid()) {
    // BPMN is valid
    logger.info("BPMN validated successfully");
} else {
    // Handle errors
    for (ValidationError error : result.getErrors()) {
        logger.error("[{}] {}", error.getCode(), error.getMessage());
    }
}
```

### With BPMN Generator

```java
// Generate and validate in one go
String bpmn = bpmnGenerator.generateBpmn(processModel);
BpmnValidationResult result = validationService.validate(bpmn);

if (!result.isValid()) {
    throw new RuntimeException(result.getErrorSummary());
}
```

### AI Retry Loop

```java
for (int attempt = 1; attempt <= maxRetries; attempt++) {
    String bpmn = generateFromAI(processId);
    BpmnValidationResult result = validationService.validate(bpmn);
    
    if (result.isValid()) {
        return bpmn; // Success!
    }
    
    // Feed errors back to AI
    String feedback = buildErrorFeedback(result);
    aiService.retry(processId, feedback);
}
```

## Validation Categories

### 1. XML Structure
- Well-formed XML
- Valid BPMN namespace
- Required root elements

### 2. Process Elements
- Exactly one start event
- At least one end event
- Unique element IDs
- Valid process structure

### 3. Sequence Flows
- Valid sourceRef/targetRef
- References to existing nodes
- Required attributes present

### 4. Logical Constraints
- No orphan nodes (disconnected)
- No unreachable nodes (from start)
- Start events: no incoming flows
- End events: no outgoing flows

## Error Codes (Critical)

| Code | Description | Fix |
|------|-------------|-----|
| `EMPTY_XML` | XML is null/empty | Provide valid XML |
| `XML_PARSE_ERROR` | Malformed XML | Fix syntax |
| `NO_PROCESS` | Missing process element | Add `<process>` |
| `NO_START_EVENT` | No start event | Add one start event |
| `MULTIPLE_START_EVENTS` | Multiple start events | Keep only one |
| `NO_END_EVENT` | No end event | Add end event |
| `DUPLICATE_ID` | Duplicate element IDs | Make IDs unique |
| `ORPHAN_NODE` | Disconnected node | Connect or remove |
| `INVALID_SOURCE_REF` | Bad flow source | Fix reference |
| `INVALID_TARGET_REF` | Bad flow target | Fix reference |

## Warning Codes

| Code | Description |
|------|-------------|
| `NOT_EXECUTABLE` | Process not executable |
| `UNREACHABLE_NODE` | Node not reachable from start |
| `NO_INCOMING_FLOW` | Node missing incoming flow |
| `NO_OUTGOING_FLOW` | Node missing outgoing flow |

## BpmnValidationResult

```java
class BpmnValidationResult {
    boolean isValid()
    List<ValidationError> getErrors()
    List<ValidationWarning> getWarnings()
    String getErrorSummary()
}

class ValidationError {
    String getCode()           // e.g., "NO_START_EVENT"
    String getMessage()        // Human-readable
    String getElementId()      // Element context
    ErrorSeverity getSeverity() // ERROR or CRITICAL
}

class ValidationWarning {
    String getCode()
    String getMessage()
    String getElementId()
}
```

## Example: Valid BPMN

```xml
<?xml version="1.0" encoding="UTF-8"?>
<bpmn2:definitions xmlns:bpmn2="http://www.omg.org/spec/BPMN/20100524/MODEL">
  <bpmn2:process id="process-1" isExecutable="true">
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

## Example: Invalid BPMN (Orphan Node)

```xml
<bpmn2:definitions xmlns:bpmn2="http://www.omg.org/spec/BPMN/20100524/MODEL">
  <bpmn2:process id="process-1" isExecutable="true">
    <bpmn2:startEvent id="start"/>
    <bpmn2:scriptTask id="orphan" name="Orphan"/>
    <bpmn2:endEvent id="end"/>
    <bpmn2:sequenceFlow id="flow1" sourceRef="start" targetRef="end"/>
  </bpmn2:process>
</bpmn2:definitions>
```

**Result**: ❌ `ORPHAN_NODE` - Node 'orphan' is orphaned (no incoming or outgoing flows)

## Example: Error Handling

```java
BpmnValidationResult result = validationService.validate(bpmn);

if (!result.isValid()) {
    for (ValidationError error : result.getErrors()) {
        switch (error.getCode()) {
            case "NO_START_EVENT":
                logger.error("Missing start event - add one");
                break;
            case "ORPHAN_NODE":
                logger.error("Orphan node: {}", error.getElementId());
                break;
            case "INVALID_SOURCE_REF":
                logger.error("Invalid flow reference: {}", error.getMessage());
                break;
            default:
                logger.error("Validation error: {}", error.getMessage());
        }
    }
    
    throw new BpmnValidationException(result.getErrorSummary());
}
```

## Testing

```bash
# Run validation tests
./gradlew test --tests BpmnValidationServiceTest

# Run all tests
./gradlew test
```

## Integration Workflow

```
ProcessModel
    ↓
BpmnGeneratorService.generateBpmn()
    ↓
BPMN XML (String)
    ↓
BpmnValidationService.validate()
    ↓
BpmnValidationResult
    ↓
if valid → Deploy to Kogito
if invalid → AI Retry / Fix
```

## Best Practices

1. **Always validate after generation**
   ```java
   String bpmn = generator.generateBpmn(model);
   BpmnValidationResult result = validator.validate(bpmn);
   ```

2. **Handle errors programmatically**
   ```java
   for (ValidationError error : result.getErrors()) {
       handleByCode(error.getCode(), error);
   }
   ```

3. **Log warnings for monitoring**
   ```java
   result.getWarnings().forEach(w -> 
       logger.warn("[{}] {}", w.getCode(), w.getMessage())
   );
   ```

4. **Use in AI feedback loops**
   ```java
   if (!result.isValid()) {
       aiService.retry(processId, buildFeedback(result));
   }
   ```

## Files

- **Service**: `BpmnValidationService.java`
- **Result DTO**: `BpmnValidationResult.java`
- **Tests**: `BpmnValidationServiceTest.java` (22 test cases)
- **Docs**: `BPMN_VALIDATION.md`

## Related Services

- `BpmnGeneratorService` - Generates BPMN (input for validation)
- `AiOrchestratorService` - Uses validation for retry logic
- `ProcessReasonerService` - Creates ProcessModel
- `AiInferenceService` - Infers process description

## Key Benefits

✅ **Catch errors early** - Before deployment  
✅ **AI-friendly errors** - Structured codes and messages  
✅ **Detailed context** - Element-level error reporting  
✅ **Logical validation** - Not just syntax, but semantics  
✅ **Production-ready** - Comprehensive test coverage  

---

For detailed documentation, see [BPMN_VALIDATION.md](./BPMN_VALIDATION.md)

