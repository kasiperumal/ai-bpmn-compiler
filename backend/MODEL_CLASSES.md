# Canonical BPMN Model Classes

This document describes the canonical Java model classes for representing normalized BPMN processes.

## Overview

These plain Java classes provide a technology-agnostic representation of BPMN processes, enabling:
- AI-generated process modeling
- Human approval workflows
- JSON serialization/deserialization
- Process validation and analysis
- Rule-based decision making

---

## Model Classes

### 1. ProcessModel

**Purpose**: Root entity representing a complete BPMN process

**Properties**:
- `id` (String) - Unique identifier
- `name` (String) - Human-readable name
- `version` (String) - Version number (e.g., "1.0.0")
- `status` (ProcessStatus) - DRAFT or PUBLISHED
- `nodes` (List<ProcessNode>) - All nodes in the process
- `edges` (List<ProcessEdge>) - All connections between nodes
- `rules` (List<RuleModel>) - Business rules associated with the process

**Example**:
```java
ProcessModel process = new ProcessModel("order-001", "Order Processing", "1.0.0");
process.setStatus(ProcessStatus.DRAFT);
process.addNode(new ProcessNode("start-1", NodeType.EVENT, "Order Received"));
```

---

### 2. ProcessNode

**Purpose**: Represents a single node/step in the process

**Properties**:
- `id` (String) - Unique node identifier
- `type` (NodeType) - TASK, GATEWAY, or EVENT
- `name` (String) - Human-readable name
- `properties` (Map<String, Object>) - Flexible key-value properties
- `explanation` (Explanation) - AI-generated explanation
- `approval` (Approval) - Approval status

**Node Types**:
- **TASK**: Work to be performed (e.g., validate order, send email)
- **GATEWAY**: Decision points or parallel splits/joins
- **EVENT**: Start, end, or intermediate events

**Example**:
```java
ProcessNode task = new ProcessNode("task-001", NodeType.TASK, "Validate Order");
task.addProperty("taskType", "SERVICE");
task.addProperty("implementation", "OrderValidationService");
task.addProperty("timeout", "PT5M");

Explanation explanation = new Explanation("task-001", 
    "Validates order details including inventory and payment");
explanation.setConfidenceScore(0.92);
task.setExplanation(explanation);
```

---

### 3. ProcessEdge

**Purpose**: Represents a connection/flow between two nodes

**Properties**:
- `id` (String) - Unique edge identifier
- `fromNodeId` (String) - Source node ID
- `toNodeId` (String) - Target node ID
- `condition` (String) - Conditional expression (for gateway branches)
- `label` (String) - Optional label for the connection

**Example**:
```java
ProcessEdge edge = new ProcessEdge("start-001", "task-001");
edge.setId("edge-001");
edge.setLabel("Begin Processing");

// Conditional edge from gateway
ProcessEdge conditionalEdge = new ProcessEdge("gateway-001", "task-002", 
    "orderValid == true");
conditionalEdge.setLabel("Valid Order");
```

---

### 4. RuleModel

**Purpose**: Represents a business rule

**Properties**:
- `id` (String) - Unique rule identifier
- `expression` (String) - Rule condition/expression
- `description` (String) - Human-readable description
- `ruleType` (String) - Type of rule (e.g., VALIDATION, ROUTING)
- `priority` (Integer) - Execution priority (lower = higher priority)
- `enabled` (boolean) - Whether the rule is active

**Example**:
```java
RuleModel rule = new RuleModel("rule-001",
    "order.totalAmount > 1000",
    "Orders over $1000 require additional approval");
rule.setRuleType("VALIDATION");
rule.setPriority(1);
rule.setEnabled(true);
```

---

### 5. Explanation

**Purpose**: AI-generated explanation for why a node exists

**Properties**:
- `nodeId` (String) - Associated node ID
- `reason` (String) - Explanation text
- `source` (String) - Source of explanation (e.g., "AI-Generated", "User-Defined")
- `confidenceScore` (Double) - AI confidence (0.0 to 1.0)
- `timestamp` (LocalDateTime) - When the explanation was created

**Example**:
```java
Explanation explanation = new Explanation("task-001",
    "Validates order details including inventory availability");
explanation.setSource("AI-Generated");
explanation.setConfidenceScore(0.95);
```

---

### 6. Approval

**Purpose**: Tracks AI and human approval status for a node

**Properties**:
- `nodeId` (String) - Associated node ID
- `aiApproved` (Boolean) - AI approval status
- `userApproved` (Boolean) - Human approval status
- `aiComment` (String) - AI approval comment
- `userComment` (String) - Human approval comment
- `approvedBy` (String) - User who approved (email/username)
- `aiApprovedAt` (LocalDateTime) - When AI approved
- `userApprovedAt` (LocalDateTime) - When user approved

**Utility Methods**:
- `isFullyApproved()` - Returns true if both AI and user approved
- `isPendingApproval()` - Returns true if either approval is missing

**Example**:
```java
Approval approval = new Approval("task-001");
approval.setAiApproved(true);
approval.setAiComment("Standard validation step");

// Later, user approves
approval.setUserApproved(true);
approval.setUserComment("Looks good");
approval.setApprovedBy("john.doe@example.com");

if (approval.isFullyApproved()) {
    // Proceed with process
}
```

---

## Enumerations

### ProcessStatus
```java
public enum ProcessStatus {
    DRAFT,      // Process is being designed/modified
    PUBLISHED   // Process is finalized and ready for execution
}
```

### NodeType
```java
public enum NodeType {
    TASK,       // Work activity
    GATEWAY,    // Decision point or parallel split/join
    EVENT       // Start, end, or intermediate event
}
```

---

## JSON Serialization

All classes are designed to be JSON-serializable using Jackson (included in Spring Boot).

### Serialization Example:
```java
ObjectMapper mapper = new ObjectMapper();
mapper.registerModule(new JavaTimeModule());

ProcessModel process = createSampleProcess();
String json = mapper.writeValueAsString(process);
```

### Deserialization Example:
```java
ObjectMapper mapper = new ObjectMapper();
mapper.registerModule(new JavaTimeModule());

String json = loadJsonFromFile();
ProcessModel process = mapper.readValue(json, ProcessModel.class);
```

---

## Usage Examples

### Building a Simple Process

```java
// Create process
ProcessModel process = new ProcessModel("simple-approval", 
    "Document Approval Process", "1.0.0");

// Add start event
ProcessNode start = new ProcessNode("start-1", NodeType.EVENT, "Document Submitted");
start.addProperty("eventType", "START");
process.addNode(start);

// Add task
ProcessNode review = new ProcessNode("task-1", NodeType.TASK, "Review Document");
review.addProperty("assignee", "manager");
review.addProperty("dueDate", "PT24H");

// Add explanation
Explanation exp = new Explanation("task-1", 
    "Manager reviews the submitted document for compliance");
exp.setConfidenceScore(0.88);
review.setExplanation(exp);

process.addNode(review);

// Add gateway
ProcessNode gateway = new ProcessNode("gateway-1", NodeType.GATEWAY, "Approved?");
gateway.addProperty("gatewayType", "EXCLUSIVE");
process.addNode(gateway);

// Add edges
process.addEdge(new ProcessEdge("start-1", "task-1"));
process.addEdge(new ProcessEdge("task-1", "gateway-1"));

ProcessEdge approvedEdge = new ProcessEdge("gateway-1", "task-2", "approved == true");
approvedEdge.setLabel("Approved");
process.addEdge(approvedEdge);

// Add rules
RuleModel rule = new RuleModel("rule-1",
    "document.type == 'CONTRACT'",
    "Contracts require legal department review");
rule.setRuleType("ROUTING");
process.addRule(rule);

// Set status
process.setStatus(ProcessStatus.DRAFT);
```

### Checking Approval Status

```java
public boolean isProcessFullyApproved(ProcessModel process) {
    return process.getNodes().stream()
        .map(ProcessNode::getApproval)
        .filter(Objects::nonNull)
        .allMatch(Approval::isFullyApproved);
}

public List<ProcessNode> getPendingApprovals(ProcessModel process) {
    return process.getNodes().stream()
        .filter(node -> node.getApproval() != null)
        .filter(node -> node.getApproval().isPendingApproval())
        .collect(Collectors.toList());
}
```

### Validating Process Structure

```java
public List<String> validateProcess(ProcessModel process) {
    List<String> errors = new ArrayList<>();
    
    // Check for start events
    long startEvents = process.getNodes().stream()
        .filter(n -> n.getType() == NodeType.EVENT)
        .filter(n -> "START".equals(n.getProperty("eventType")))
        .count();
    
    if (startEvents == 0) {
        errors.add("Process must have at least one start event");
    }
    
    // Check for orphaned nodes
    Set<String> nodeIds = process.getNodes().stream()
        .map(ProcessNode::getId)
        .collect(Collectors.toSet());
    
    for (ProcessEdge edge : process.getEdges()) {
        if (!nodeIds.contains(edge.getFromNodeId())) {
            errors.add("Edge references non-existent node: " + edge.getFromNodeId());
        }
        if (!nodeIds.contains(edge.getToNodeId())) {
            errors.add("Edge references non-existent node: " + edge.getToNodeId());
        }
    }
    
    return errors;
}
```

---

## Best Practices

1. **IDs**: Use meaningful, unique IDs (e.g., "task-validate-order" instead of "t1")
2. **Versioning**: Use semantic versioning for process versions (major.minor.patch)
3. **Properties**: Use consistent property keys across similar node types
4. **Explanations**: Always provide confidence scores for AI-generated content
5. **Approvals**: Require both AI and user approval for critical processes
6. **Rules**: Keep expressions simple and testable
7. **Validation**: Always validate process structure before publishing

---

## Integration Points

### With Drools
Rules from `RuleModel` can be converted to DRL format for execution

### With jBPM
`ProcessModel` can be transformed into jBPM process definitions

### With REST APIs
All classes serialize cleanly to/from JSON for API communication

### With AI Services
`Explanation` and `Approval` classes support AI-in-the-loop workflows

---

## See Also

- Sample JSON: `src/main/resources/examples/sample-process.json`
- Spring Boot Application: `AiBpmnCompilerApplication.java`
- API Documentation: Coming soon

---

## License

Copyright © 2026

