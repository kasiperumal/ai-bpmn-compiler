# Process Lifecycle - Publish & Execute

## Overview

The Process Lifecycle API provides high-level endpoints for publishing and executing AI-generated processes. These endpoints orchestrate the complete workflow from process model to running instances.

## Architecture

```
Process Model (In Memory)
         ↓
    [PUBLISH]
         ↓
┌────────────────────────┐
│ 1. Generate BPMN       │
│ 2. Validate BPMN       │
│ 3. Generate DRL        │
│ 4. Deploy to Kogito    │
│ 5. Mark as PUBLISHED   │
└────────────────────────┘
         ↓
   Kogito Runtime
         ↓
    [EXECUTE]
         ↓
   Process Instance
```

## Endpoints

### 1. Publish Process

**Endpoint**: `POST /api/process/{processId}/publish`

Orchestrates the complete publish workflow:
1. Retrieves process model from repository
2. Generates BPMN 2.0 XML from process model
3. Validates BPMN structure
4. Generates DRL from business rules
5. Deploys BPMN and DRL to Kogito
6. Updates process status to PUBLISHED
7. Updates orchestrator state to AiState.PUBLISHED

**Request**:
```bash
curl -X POST http://localhost:8080/api/process/proc-123/publish
```

**Response** (200 OK):
```json
{
  "processId": "proc-123",
  "status": "PUBLISHED",
  "bpmnPath": "./data/kogito/processes/proc-123.bpmn",
  "drlPath": "./data/kogito/rules/proc-123.drl",
  "message": "Process published successfully. Execute at: POST /proc-123",
  "executeEndpoint": "/api/process/proc-123/execute",
  "kogitoEndpoint": "/proc-123"
}
```

**Error Responses**:
- `404 NOT_FOUND` - Process not found in repository
- `409 CONFLICT` - Process not ready (e.g., needs clarification, in FAILED state)
- `500 INTERNAL_SERVER_ERROR` - Generation, validation, or deployment failed

**Prerequisites**:
- Process must exist in repository
- Process must have nodes (cannot be empty)
- Process AI state must not be FAILED or CLARIFICATION_REQUIRED

**Side Effects**:
- Creates BPMN file in `./data/kogito/processes/`
- Creates DRL file in `./data/kogito/rules/`
- Creates timestamped backups
- Updates process status to PUBLISHED
- Updates AiState to PUBLISHED
- Kogito auto-generates REST endpoints for execution

---

### 2. Execute Process

**Endpoint**: `POST /api/process/{processId}/execute`

Starts a new process instance. Only PUBLISHED processes can be executed.

**Request**:
```bash
curl -X POST http://localhost:8080/api/process/proc-123/execute \
  -H "Content-Type: application/json" \
  -d '{
    "orderAmount": 5000,
    "customerId": "CUST-001",
    "priority": "HIGH"
  }'
```

**Request Body**: Process variables (key-value pairs)
```json
{
  "orderAmount": 5000,
  "customerId": "CUST-001",
  "priority": "HIGH",
  "requestDate": "2026-01-02"
}
```

**Response** (201 CREATED):
```json
{
  "processId": "proc-123",
  "instanceId": "abc-123-def-456",
  "status": "STARTED",
  "message": "Process instance created successfully",
  "queryEndpoint": "/api/process/proc-123/instance/abc-123-def-456",
  "kogitoEndpoint": "/proc-123/abc-123-def-456",
  "instanceData": {
    "id": "abc-123-def-456",
    "orderAmount": 5000,
    "customerId": "CUST-001",
    "priority": "HIGH"
  }
}
```

**Error Responses**:
- `404 NOT_FOUND` - Process not found
- `409 CONFLICT` - Process not PUBLISHED or not deployed to Kogito
- `500 INTERNAL_SERVER_ERROR` - Kogito execution failed

**Validation Rules**:
1. Process must exist in repository
2. Process status must be PUBLISHED
3. Process must be deployed to Kogito
4. Delegates to Kogito's `POST /{processId}` endpoint

---

### 3. Get Process Instance

**Endpoint**: `GET /api/process/{processId}/instance/{instanceId}`

Retrieve status and data of a specific process instance.

**Request**:
```bash
curl http://localhost:8080/api/process/proc-123/instance/abc-123-def
```

**Response** (200 OK):
```json
{
  "processId": "proc-123",
  "instanceId": "abc-123-def",
  "data": {
    "id": "abc-123-def",
    "processId": "proc-123",
    "state": 1,
    "variables": {
      "orderAmount": 5000,
      "customerId": "CUST-001",
      "approvalStatus": "PENDING"
    }
  }
}
```

---

### 4. List Process Instances

**Endpoint**: `GET /api/process/{processId}/instances`

List all instances for a process.

**Request**:
```bash
curl http://localhost:8080/api/process/proc-123/instances
```

**Response** (200 OK):
```json
{
  "processId": "proc-123",
  "instances": [
    {
      "id": "abc-123",
      "state": 1
    },
    {
      "id": "def-456",
      "state": 2
    }
  ]
}
```

---

### 5. Get Process Status

**Endpoint**: `GET /api/process/{processId}/status`

Check if a process is published and ready for execution.

**Request**:
```bash
curl http://localhost:8080/api/process/proc-123/status
```

**Response** (200 OK):
```json
{
  "processId": "proc-123",
  "published": true,
  "canExecute": true,
  "executeEndpoint": "/api/process/proc-123/execute",
  "kogitoEndpoint": "/proc-123"
}
```

## Complete Workflow Examples

### Example 1: Text to Execution

```bash
# 1. Create process from text description
curl -X POST http://localhost:8080/api/process/from-text \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Order Approval Process",
    "description": "When an order comes in, check the amount. If amount is greater than $5000, require manager approval. Otherwise, auto-approve."
  }'

# Response: { "processId": "proc-order-001" }

# 2. Check AI orchestrator state
curl http://localhost:8080/api/orchestrator/proc-order-001/state

# Response: { "currentState": "TEXT_RECEIVED" }

# 3. Start AI inference
curl -X POST http://localhost:8080/api/orchestrator/proc-order-001/start-inference

# Response: { "currentState": "PROCESS_INFERRED" }

# 4. Advance to MODEL_READY
curl -X POST http://localhost:8080/api/orchestrator/proc-order-001/advance

# Response: { "currentState": "MODEL_READY" }

# 5. Publish process (generates BPMN, DRL, deploys)
curl -X POST http://localhost:8080/api/process/proc-order-001/publish

# Response:
# {
#   "processId": "proc-order-001",
#   "status": "PUBLISHED",
#   "executeEndpoint": "/api/process/proc-order-001/execute"
# }

# 6. Execute process
curl -X POST http://localhost:8080/api/process/proc-order-001/execute \
  -H "Content-Type: application/json" \
  -d '{
    "orderAmount": 7500,
    "customerId": "CUST-001",
    "orderDate": "2026-01-02"
  }'

# Response:
# {
#   "instanceId": "inst-abc-123",
#   "status": "STARTED"
# }

# 7. Query instance
curl http://localhost:8080/api/process/proc-order-001/instance/inst-abc-123
```

### Example 2: Multiple Executions

```bash
# Execute same process multiple times
for i in {1..5}; do
  curl -X POST http://localhost:8080/api/process/proc-order-001/execute \
    -H "Content-Type: application/json" \
    -d "{
      \"orderAmount\": $((RANDOM % 10000)),
      \"customerId\": \"CUST-00$i\"
    }"
done

# List all instances
curl http://localhost:8080/api/process/proc-order-001/instances
```

### Example 3: Error Handling

```bash
# Try to execute unpublished process
curl -X POST http://localhost:8080/api/process/proc-new/execute \
  -H "Content-Type: application/json" \
  -d '{"data": "value"}'

# Response (409 CONFLICT):
# {
#   "error": "Process proc-new is not published (status: DRAFT). Publish the process first."
# }

# Publish first
curl -X POST http://localhost:8080/api/process/proc-new/publish

# Now execute
curl -X POST http://localhost:8080/api/process/proc-new/execute \
  -H "Content-Type: application/json" \
  -d '{"data": "value"}'

# Success (201 CREATED)
```

## Service Architecture

### ProcessPublishingService

Orchestrates the complete publish workflow:

```java
@Service
public class ProcessPublishingService {
    
    @Autowired
    private ProcessModelRepository repository;
    
    @Autowired
    private BpmnGeneratorService bpmnGenerator;
    
    @Autowired
    private DrlGeneratorService drlGenerator;
    
    @Autowired
    private BpmnValidationService bpmnValidator;
    
    @Autowired
    private KogitoDeploymentService kogitoDeployment;
    
    @Autowired
    private AiOrchestratorService orchestrator;
    
    public PublishResult publishProcess(String processId) {
        // 1. Get process model
        ProcessModel model = repository.findById(processId)
            .orElseThrow(() -> new IllegalArgumentException("Process not found"));
        
        // 2. Validate ready for publish
        validateProcessReadyForPublish(model);
        
        // 3. Generate BPMN
        String bpmnXml = bpmnGenerator.generateBpmn(model);
        
        // 4. Validate BPMN
        bpmnValidator.validate(bpmnXml);
        
        // 5. Generate DRL
        String drl = drlGenerator.generateDrl(model.getRules());
        
        // 6. Deploy to Kogito
        DeploymentResult result = kogitoDeployment.deployProcess(
            processId, bpmnXml, drl
        );
        
        // 7. Update status
        model.setStatus(ProcessStatus.PUBLISHED);
        repository.save(model);
        
        // 8. Update orchestrator
        orchestrator.updateState(processId, AiState.PUBLISHED);
        
        return new PublishResult(processId, result);
    }
}
```

### ProcessExecutionService

Validates and executes published processes:

```java
@Service
public class ProcessExecutionService {
    
    @Autowired
    private ProcessModelRepository repository;
    
    @Autowired
    private KogitoDeploymentService kogitoDeployment;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${kogito.service.url}")
    private String kogitoUrl;
    
    public ExecutionResult executeProcess(
        String processId, 
        Map<String, Object> variables
    ) {
        // 1. Validate process exists
        ProcessModel model = repository.findById(processId)
            .orElseThrow(() -> new IllegalArgumentException("Process not found"));
        
        // 2. Validate PUBLISHED
        if (model.getStatus() != ProcessStatus.PUBLISHED) {
            throw new IllegalStateException("Process not published");
        }
        
        // 3. Validate deployed
        if (!kogitoDeployment.isDeployed(processId)) {
            throw new IllegalStateException("Process not deployed");
        }
        
        // 4. Call Kogito: POST /{processId}
        String endpoint = kogitoUrl + "/" + processId;
        ResponseEntity<String> response = restTemplate.postForEntity(
            endpoint, 
            variables, 
            String.class
        );
        
        // 5. Extract instance ID
        String instanceId = extractInstanceId(response.getBody());
        
        return new ExecutionResult(processId, instanceId, response.getBody());
    }
}
```

## Integration Points

### With AI Orchestrator

```java
// After all AI workflow steps complete
if (orchestrator.getCurrentState(processId) == AiState.MODEL_READY) {
    // Approve the model
    orchestrator.approveStep(processId, "final-approval");
    
    // Publish to Kogito
    PublishResult result = publishingService.publishProcess(processId);
    
    // Now at AiState.PUBLISHED
}
```

### With BPMN/DRL Generators

```java
// Publishing automatically invokes generators
PublishResult result = publishingService.publishProcess(processId);

// Internally:
// - BpmnGeneratorService.generateBpmn(model)
// - BpmnValidationService.validate(bpmn)
// - DrlGeneratorService.generateDrl(rules)
// - KogitoDeploymentService.deployProcess(processId, bpmn, drl)
```

### With Kogito Runtime

```java
// Execute delegates to Kogito's auto-generated endpoint
ExecutionResult result = executionService.executeProcess(
    "proc-123", 
    Map.of("amount", 5000)
);

// Internally calls: POST http://localhost:8080/proc-123
// Kogito handles the actual process execution
```

## Error Handling

### Publish Errors

```java
try {
    publishingService.publishProcess(processId);
} catch (IllegalArgumentException e) {
    // Process not found
} catch (IllegalStateException e) {
    // Process not ready (needs clarification, failed, etc.)
} catch (PublishException e) {
    // BPMN validation failed
    // DRL validation failed
    // Deployment failed
}
```

### Execute Errors

```java
try {
    executionService.executeProcess(processId, variables);
} catch (IllegalArgumentException e) {
    // Process not found
} catch (IllegalStateException e) {
    // Process not published
    // Process not deployed
} catch (ExecutionException e) {
    // Kogito execution failed
}
```

## Auto-Retry Integration

Publishing automatically integrates with the retry system:

```java
try {
    String bpmn = bpmnGenerator.generateBpmn(model);
    bpmnValidator.validate(bpmn);
} catch (BpmnValidationException e) {
    // Automatically triggers retry
    orchestrator.recordBpmnGenerationFailure(processId, e.getMessage());
    throw new PublishException(...);
}

try {
    String drl = drlGenerator.generateDrl(rules);
} catch (DrlValidationException e) {
    // Automatically triggers retry
    orchestrator.recordDrlGenerationFailure(processId, e.getMessage());
    throw new PublishException(...);
}
```

## Best Practices

### 1. Always Check Status Before Execute

```java
// Check if published
boolean published = publishingService.isPublished(processId);

if (!published) {
    publishingService.publishProcess(processId);
}

// Now execute
executionService.executeProcess(processId, variables);
```

### 2. Handle Publish Failures Gracefully

```java
try {
    publishingService.publishProcess(processId);
} catch (PublishException e) {
    // Check retry count
    int retries = orchestrator.getRetryCount(processId);
    
    if (retries >= orchestrator.getMaxRetries(processId)) {
        // Max retries reached - notify user
        notifyUser(processId, "Publishing failed after " + retries + " attempts");
    } else {
        // Auto-retry will handle it
        logger.info("Publishing will be retried automatically");
    }
}
```

### 3. Provide Meaningful Variables

```java
Map<String, Object> variables = new HashMap<>();
variables.put("orderAmount", 5000);
variables.put("customerId", "CUST-001");
variables.put("requestDate", LocalDate.now().toString());
variables.put("priority", "HIGH");

executionService.executeProcess(processId, variables);
```

### 4. Monitor Instance Status

```java
// Start instance
ExecutionResult result = executionService.executeProcess(processId, variables);
String instanceId = result.getInstanceId();

// Poll for completion
while (true) {
    String instanceData = executionService.getProcessInstance(processId, instanceId);
    JsonNode data = objectMapper.readTree(instanceData);
    
    int state = data.get("state").asInt();
    if (state == 2) { // Completed
        break;
    }
    
    Thread.sleep(1000);
}
```

## Files

- **Service**: `ProcessPublishingService.java`
- **Service**: `ProcessExecutionService.java`
- **Controller**: `ProcessLifecycleController.java`
- **Documentation**: `PROCESS_LIFECYCLE.md`

## Related Documentation

- [KOGITO_SETUP.md](./KOGITO_SETUP.md) - Kogito configuration
- [BPMN_GENERATOR.md](./BPMN_GENERATOR.md) - BPMN generation
- [DRL_GENERATOR.md](./DRL_GENERATOR.md) - DRL generation
- [AI_ORCHESTRATOR.md](./AI_ORCHESTRATOR.md) - AI workflow orchestration
- [API.md](./API.md) - Complete API reference

---

**Created**: January 2, 2026  
**Spring Boot**: 3.4.3  
**Kogito**: 10.1.0  
**Java**: 17

