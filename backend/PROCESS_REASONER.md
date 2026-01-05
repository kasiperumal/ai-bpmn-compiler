# Process Reasoner Service

## Overview

The `ProcessReasonerService` converts natural language process descriptions into structured BPMN elements (nodes, edges, rules, explanations). It uses AI reasoning to extract logical flow, detect uncertainties, and flag when clarification is needed.

**Key Features**:
- ✅ Extracts ProcessNodes (tasks, gateways, events)
- ✅ Extracts ProcessEdges (connections with conditions)
- ✅ Extracts RuleModels (business rules)
- ✅ Generates Explanations (AI reasoning)
- ✅ Detects uncertainties and ambiguities
- ✅ Flags clarificationRequired when needed
- ✅ JSON-based structured output
- ✅ Comprehensive error handling

---

## API Reference

### `reasonOverDescription(String processDescription)`

Analyze a process description and extract structured BPMN elements.

**Parameters**:
- `processDescription` (String, required): Natural language process description

**Returns**: `ReasoningResult` containing:
- `List<ProcessNode>` - Extracted nodes
- `List<ProcessEdge>` - Extracted edges/connections
- `List<RuleModel>` - Extracted business rules
- `List<Explanation>` - AI reasoning explanations
- `boolean clarificationRequired` - True if more info needed
- `List<String> clarificationReasons` - Specific questions/issues

**Throws**:
- `IllegalArgumentException` - If description is null or empty
- `RuntimeException` - If AI reasoning or JSON parsing fails

**Example**:
```java
@Autowired
private ProcessReasonerService reasonerService;

public void extractElements(String description) {
    ReasoningResult result = reasonerService.reasonOverDescription(description);
    
    System.out.println("Nodes: " + result.getNodes().size());
    System.out.println("Edges: " + result.getEdges().size());
    System.out.println("Rules: " + result.getRules().size());
    
    if (result.isClarificationRequired()) {
        System.out.println("Clarification needed:");
        result.getClarificationReasons().forEach(System.out::println);
    }
}
```

---

## ReasoningResult

DTO containing the results of AI reasoning.

### Fields

| Field | Type | Description |
|-------|------|-------------|
| `nodes` | `List<ProcessNode>` | Extracted process nodes |
| `edges` | `List<ProcessEdge>` | Connections between nodes |
| `rules` | `List<RuleModel>` | Business rules |
| `explanations` | `List<Explanation>` | AI reasoning for elements |
| `clarificationRequired` | `boolean` | True if more info needed |
| `clarificationReasons` | `List<String>` | Specific questions |

### Methods

```java
// Helper methods
void addNode(ProcessNode node)
void addEdge(ProcessEdge edge)
void addRule(RuleModel rule)
void addExplanation(Explanation explanation)
void addClarificationReason(String reason)

// Query methods
int getTotalElements()  // Sum of nodes + edges + rules
String toString()       // Summary string
```

---

## Prompt Engineering

The service creates a detailed prompt that instructs Gemini to:

### 1. Identify Process Nodes

- **Start events**: Where the process begins
- **End events**: Where the process completes
- **Tasks**: Actions or steps to be performed
- **Gateways**: Decision points, branching, or merging

### 2. Identify Connections (Edges)

- How nodes connect to each other
- Conditions for conditional branches
- Flow sequence

### 3. Identify Business Rules

- Explicit rules mentioned (e.g., "if amount > $1000")
- Validation rules
- Decision criteria

### 4. Provide Explanations

- Why each element was identified
- Confidence level (0.0 to 1.0)
- Assumptions made

### 5. Detect Uncertainties

- Missing information
- Ambiguous descriptions
- Unclear connections
- Flag `clarificationRequired = true` when needed

---

## Example Output

### Input Description

```markdown
## Overview
Order approval process

## Main Flow
1. Process begins when order is received
2. Validate order details
3. Check if amount is over $1000
4. If yes, require manager approval
5. If no, auto-approve
6. Process completes
```

### Output (JSON Structure)

```json
{
  "nodes": [
    {
      "id": "start-order-received",
      "type": "EVENT",
      "name": "Order Received",
      "description": "Process begins when order is received",
      "properties": {"eventType": "start"}
    },
    {
      "id": "task-validate",
      "type": "TASK",
      "name": "Validate Order",
      "description": "Check order completeness",
      "properties": {}
    },
    {
      "id": "gateway-check-amount",
      "type": "GATEWAY",
      "name": "Check Order Amount",
      "description": "Decision based on amount threshold",
      "properties": {"gatewayType": "exclusive"}
    },
    {
      "id": "task-manager-approval",
      "type": "TASK",
      "name": "Manager Approval",
      "description": "Require manager approval for high value",
      "properties": {}
    },
    {
      "id": "task-auto-approve",
      "type": "TASK",
      "name": "Auto Approve",
      "description": "Automatically approve low value orders",
      "properties": {}
    },
    {
      "id": "end-completed",
      "type": "EVENT",
      "name": "Process Completed",
      "description": "Process ends",
      "properties": {"eventType": "end"}
    }
  ],
  "edges": [
    {
      "id": "edge-1",
      "fromNodeId": "start-order-received",
      "toNodeId": "task-validate",
      "condition": null,
      "description": "Flow to validation"
    },
    {
      "id": "edge-2",
      "fromNodeId": "task-validate",
      "toNodeId": "gateway-check-amount",
      "condition": null,
      "description": "After validation"
    },
    {
      "id": "edge-3",
      "fromNodeId": "gateway-check-amount",
      "toNodeId": "task-manager-approval",
      "condition": "amount > 1000",
      "description": "High value path"
    },
    {
      "id": "edge-4",
      "fromNodeId": "gateway-check-amount",
      "toNodeId": "task-auto-approve",
      "condition": "amount <= 1000",
      "description": "Low value path"
    },
    {
      "id": "edge-5",
      "fromNodeId": "task-manager-approval",
      "toNodeId": "end-completed",
      "condition": null,
      "description": "After approval"
    },
    {
      "id": "edge-6",
      "fromNodeId": "task-auto-approve",
      "toNodeId": "end-completed",
      "condition": null,
      "description": "After auto-approval"
    }
  ],
  "rules": [
    {
      "id": "rule-high-value",
      "name": "High Value Order Check",
      "expression": "orderAmount > 1000",
      "description": "Orders over $1000 require manager approval",
      "priority": 10,
      "enabled": true
    }
  ],
  "explanations": [
    {
      "nodeId": "gateway-check-amount",
      "reason": "Identified as exclusive gateway because description mentions checking amount and branching based on threshold",
      "confidenceScore": 0.95,
      "source": "AI_REASONING"
    },
    {
      "nodeId": "task-validate",
      "reason": "Clear validation step mentioned in main flow",
      "confidenceScore": 0.98,
      "source": "AI_REASONING"
    }
  ],
  "clarificationRequired": false,
  "clarificationReasons": []
}
```

---

## Usage Examples

### Basic Usage

```java
@Service
public class ProcessModelBuilder {
    
    @Autowired
    private ProcessReasonerService reasonerService;
    
    @Autowired
    private ProcessModelRepository processRepository;
    
    public ProcessModel buildFromDescription(String processId, String description) {
        // Reason over description
        ReasoningResult result = reasonerService.reasonOverDescription(description);
        
        // Get or create process model
        ProcessModel process = processRepository.findById(processId).orElseThrow();
        
        // Add nodes
        result.getNodes().forEach(node -> process.addNode(node));
        
        // Add edges
        result.getEdges().forEach(edge -> process.addEdge(edge));
        
        // Add rules
        result.getRules().forEach(rule -> process.addRule(rule));
        
        // Save
        return processRepository.save(process);
    }
}
```

---

### With Clarification Handling

```java
@Service
public class InteractiveProcessBuilder {
    
    @Autowired
    private ProcessReasonerService reasonerService;
    
    @Autowired
    private AiOrchestratorService orchestrator;
    
    public ProcessModel buildWithClarification(String processId, String description) {
        // Reason over description
        ReasoningResult result = reasonerService.reasonOverDescription(description);
        
        // Check if clarification needed
        if (result.isClarificationRequired()) {
            // Set orchestrator state to CLARIFICATION_REQUIRED
            orchestrator.updateState(processId, AiState.CLARIFICATION_REQUIRED);
            
            // Log clarification reasons
            logger.warn("Clarification needed for process {}", processId);
            result.getClarificationReasons().forEach(reason -> {
                logger.warn("  - {}", reason);
            });
            
            // Could notify user or return for user input
            throw new ClarificationRequiredException(
                result.getClarificationReasons()
            );
        }
        
        // Build process model
        return buildProcessModel(processId, result);
    }
}
```

---

### Integration with AI Inference

```java
@Service
public class EndToEndProcessor {
    
    @Autowired
    private AiInferenceService inferenceService;
    
    @Autowired
    private ProcessReasonerService reasonerService;
    
    @Autowired
    private ProcessModelRepository processRepository;
    
    public ProcessModel processFromImage(String processId) {
        // Step 1: Infer description from image
        String description = inferenceService
            .inferProcessDescriptionFromImage(processId);
        
        logger.info("Inferred description: {} chars", description.length());
        
        // Step 2: Reason over description to extract elements
        ReasoningResult result = reasonerService
            .reasonOverDescription(description);
        
        logger.info("Extracted: {} nodes, {} edges, {} rules",
            result.getNodes().size(),
            result.getEdges().size(),
            result.getRules().size());
        
        // Step 3: Build process model
        ProcessModel process = processRepository.findById(processId).orElseThrow();
        
        result.getNodes().forEach(process::addNode);
        result.getEdges().forEach(process::addEdge);
        result.getRules().forEach(process::addRule);
        
        // Step 4: Store explanations (could add to nodes)
        result.getExplanations().forEach(explanation -> {
            ProcessNode node = process.getNode(explanation.getNodeId());
            if (node != null) {
                node.setExplanation(explanation);
            }
        });
        
        return processRepository.save(process);
    }
}
```

---

## Node Type Detection

The service identifies different node types based on keywords and context:

### Event Nodes (START/END)

**Keywords**:
- Start: "begins", "starts", "received", "triggered", "initiated"
- End: "completes", "ends", "finished", "terminated", "closed"

**Properties**:
- `eventType`: "start" or "end"

---

### Task Nodes

**Keywords**:
- "validate", "check", "process", "send", "create", "update", "notify"
- Action verbs indicating work to be done

**Properties**:
- Can include additional metadata

---

### Gateway Nodes

**Keywords**:
- "if", "decision", "check whether", "branching", "split", "merge"
- Conditional logic indicators

**Properties**:
- `gatewayType`: "exclusive", "parallel", "inclusive"

**Gateway Types**:
- **Exclusive**: XOR decision (one path taken)
- **Parallel**: AND split/join (all paths)
- **Inclusive**: OR decision (one or more paths)

---

## Business Rules Extraction

The service extracts rules when it detects:

### Explicit Rules

```
"If order amount > $1000, require manager approval"
```

Extracted as:
```json
{
  "id": "rule-high-value",
  "expression": "orderAmount > 1000",
  "description": "Orders over $1000 require manager approval",
  "priority": 10,
  "enabled": true
}
```

### Validation Rules

```
"Order must have customer name and valid email"
```

Extracted as:
```json
{
  "id": "rule-order-validation",
  "expression": "customerName != null && emailValid(email)",
  "description": "Order validation requirements",
  "priority": 5,
  "enabled": true
}
```

---

## Clarification Detection

The service flags `clarificationRequired = true` when:

### 1. Missing Information

```
"Process involves approval" (By whom? What criteria?)
```

### 2. Ambiguous Descriptions

```
"Check if eligible" (Eligible for what? What are the criteria?)
```

### 3. Unclear Connections

```
"After validation, proceed to next step" (What is the next step?)
```

### 4. Multiple Interpretations

```
"Process order" (Create new order? Process existing order?)
```

### Example Clarification Output

```json
{
  "clarificationRequired": true,
  "clarificationReasons": [
    "Who is responsible for approving high-value orders?",
    "What happens if payment processing fails?",
    "Are there any parallel activities in the fulfillment step?",
    "What is the threshold for 'high value' orders?"
  ]
}
```

---

## Confidence Scoring

Each explanation includes a confidence score (0.0 to 1.0):

| Score | Meaning | Example |
|-------|---------|---------|
| 0.95-1.0 | Very High | Clear, explicit step mentioned |
| 0.85-0.94 | High | Strong indicators, minor ambiguity |
| 0.70-0.84 | Medium | Some ambiguity, reasonable inference |
| 0.50-0.69 | Low | Significant ambiguity, multiple interpretations |
| < 0.50 | Very Low | Highly uncertain, needs clarification |

**Example**:
```json
{
  "nodeId": "task-validate",
  "reason": "Clearly described as validation step in main flow",
  "confidenceScore": 0.98,
  "source": "AI_REASONING"
}
```

---

## Error Handling

### Common Errors

1. **Null/Empty Description**
   ```java
   IllegalArgumentException: "Process description cannot be null or empty"
   ```

2. **AI Reasoning Failure**
   ```java
   RuntimeException: "Failed to reason over description: API error"
   ```

3. **JSON Parsing Error**
   ```java
   RuntimeException: "Failed to reason over description: JsonParseException"
   ```

### Best Practices

```java
try {
    ReasoningResult result = reasonerService.reasonOverDescription(description);
    
    // Check for clarification
    if (result.isClarificationRequired()) {
        // Handle clarification flow
    } else {
        // Proceed with model building
    }
    
} catch (IllegalArgumentException e) {
    log.error("Invalid description: {}", e.getMessage());
    
} catch (RuntimeException e) {
    log.error("Reasoning failed: {}", e.getMessage());
    // Could retry or use fallback
}
```

---

## Testing

### Unit Tests

11 comprehensive tests covering:
- ✅ Successful reasoning
- ✅ Clarification required
- ✅ Null/empty description
- ✅ Gemini API errors
- ✅ Invalid JSON
- ✅ JSON with markdown blocks
- ✅ Complex workflows
- ✅ Prompt validation
- ✅ ReasoningResult helper methods

**Run tests**:
```bash
./gradlew test --tests ProcessReasonerServiceTest
```

---

## Performance Considerations

### Response Times

- **Typical**: 5-15 seconds
- **Factors**:
  - Description length and complexity
  - Number of elements to extract
  - Gemini API response time

### Optimization Tips

1. **Cache results** for identical descriptions
2. **Batch processing** for multiple descriptions
3. **Async processing** for long-running operations
4. **Limit description length** (recommend < 5000 chars)

---

## Dependencies

- **GeminiClient**: For AI reasoning
- **ObjectMapper (Jackson)**: For JSON parsing
- **Model classes**: ProcessNode, ProcessEdge, RuleModel, Explanation

---

## Future Enhancements

### 1. Iterative Refinement

```java
public ReasoningResult refineWithFeedback(
        ReasoningResult initial,
        Map<String, String> userFeedback) {
    // Use user feedback to refine extraction
}
```

### 2. Multi-Level Reasoning

```java
public ReasoningResult reasonWithDetail(
        String description,
        DetailLevel level) {
    // BASIC, DETAILED, COMPREHENSIVE
}
```

### 3. Validation

```java
public ValidationResult validate(ReasoningResult result) {
    // Check for disconnected nodes
    // Check for missing start/end events
    // Check for invalid edge connections
}
```

### 4. Auto-Correction

```java
public ReasoningResult reasonAndCorrect(String description) {
    // Automatically fix common issues
}
```

---

## Summary

The `ProcessReasonerService` provides:

✅ **AI-powered element extraction** from descriptions  
✅ **Structured BPMN output** (nodes, edges, rules)  
✅ **AI reasoning explanations** with confidence scores  
✅ **Clarification detection** when info is missing  
✅ **Comprehensive error handling**  
✅ **Full test coverage** (11 tests)  
✅ **Production-ready** implementation  

**Status**: ✅ **READY FOR INTEGRATION**

---

## See Also

- [AI Inference Service Documentation](AI_INFERENCE_SERVICE.md)
- [Gemini Client Documentation](GEMINI_CLIENT.md)
- [Model Classes Documentation](MODEL_CLASSES.md)
- [AI Orchestrator Documentation](AI_ORCHESTRATOR.md)

---

**Implementation Date**: January 2, 2026  
**Dependencies**: GeminiClient, Spring AI 1.0.0-M6  
**Java Version**: 17

