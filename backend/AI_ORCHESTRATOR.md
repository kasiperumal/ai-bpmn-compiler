# AI Orchestrator Service

## Overview

The `AiOrchestratorService` manages the AI-driven workflow for BPMN process compilation. It tracks the state of each process through various stages, from initial input (image or text) through AI inference, user approval, artifact generation, and final publication.

**Key Features**:
- ✅ In-memory state tracking using `ConcurrentHashMap`
- ✅ Thread-safe operations
- ✅ State transition validation
- ✅ Automatic retry on BPMN/DRL generation failures
- ✅ Configurable max retry limits per process
- ✅ Retry explanation tracking
- ✅ User approval workflow
- ✅ Clarification request/response handling
- ✅ No AI integration yet (ready for future implementation)

---

## Architecture

### Components

```
┌─────────────────────────────────────────────────────────────┐
│                  AiOrchestratorController                   │
│                    (REST Endpoints)                         │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ↓
┌─────────────────────────────────────────────────────────────┐
│                  AiOrchestratorService                      │
│                  (Business Logic)                           │
│  • State tracking (ConcurrentHashMap)                       │
│  • State transitions                                        │
│  • Validation                                               │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ↓
┌─────────────────────────────────────────────────────────────┐
│              ProcessModelRepository                         │
│              (Process Storage)                              │
└─────────────────────────────────────────────────────────────┘
```

### State Management

The orchestrator maintains a `ConcurrentHashMap<String, AiState>` that maps:
- **Key**: `processId` (String)
- **Value**: Current `AiState` (Enum)

This provides:
- O(1) lookup time
- Thread-safe concurrent access
- In-memory storage (no database required yet)

---

## AI State Workflow

### State Diagram

```
┌─────────────────┐       ┌─────────────────┐
│ IMAGE_RECEIVED  │       │  TEXT_RECEIVED  │
└────────┬────────┘       └────────┬────────┘
         │                         │
         └──────────┬──────────────┘
                    ↓
         ┌──────────────────┐
         │ PROCESS_INFERRED │
         └─────────┬────────┘
                   │
         ┌─────────┴─────────┐
         │                   │
         ↓                   ↓
┌────────────────────┐  ┌────────────┐
│ CLARIFICATION_     │  │ MODEL_READY│
│ REQUIRED           │  │ (approval) │
└────────────────────┘  └─────┬──────┘
         ↑                    │
         │                    ↓
         │            ┌───────────────┐
         │            │ BPMN_GENERATED│
         │            └───────┬───────┘
         │                    │
         │                    ↓
         │            ┌───────────────┐
         │            │ DRL_GENERATED │
         │            └───────┬───────┘
         │                    │
         │                    ↓
         │            ┌───────────────┐
         │            │   PUBLISHED   │
         │            └───────────────┘
         │
         │            ┌───────────────┐
         └────────────│    FAILED     │
                      └───────────────┘
                            │
                            ↓ (retry)
                      ┌─────────────┐
                      │TEXT_RECEIVED│
                      └─────────────┘
```

### State Descriptions

| State | Type | Description | Next State | User Action Required |
|-------|------|-------------|------------|---------------------|
| `IMAGE_RECEIVED` | Initial | Image uploaded | `PROCESS_INFERRED` | No |
| `TEXT_RECEIVED` | Initial | Text description provided | `PROCESS_INFERRED` | No |
| `PROCESS_INFERRED` | Processing | AI inferred preliminary model | `MODEL_READY` | No |
| `CLARIFICATION_REQUIRED` | Waiting | AI needs user input | `null` (manual) | Yes |
| `MODEL_READY` | Approval | Model ready for review | `BPMN_GENERATED` | Yes |
| `BPMN_GENERATED` | Processing | BPMN XML generated | `DRL_GENERATED` | No |
| `DRL_GENERATED` | Processing | Drools rules generated | `PUBLISHED` | No |
| `PUBLISHED` | Terminal | Process complete | `null` | No |
| `FAILED` | Terminal | Error occurred | `null` (can retry) | No |

---

## Service Methods

### Core Operations

#### `startInference(processId)`

Start AI inference for a process.

**Preconditions**:
- Process must exist in repository
- Process must be in initial state (`IMAGE_RECEIVED` or `TEXT_RECEIVED`)

**Behavior**:
- Validates process exists
- Checks current state is initial
- Transitions to `PROCESS_INFERRED`
- Returns `AiStateInfo`

**Throws**:
- `IllegalArgumentException` - Process not found
- `IllegalStateException` - Not in initial state

**Example**:
```java
AiStateInfo info = orchestrator.startInference("proc-123");
// info.getCurrentState() == PROCESS_INFERRED
```

---

#### `approveStep(processId, stepId)`

Approve a step and advance to next state.

**Preconditions**:
- Process must exist
- Process must be in `MODEL_READY` or `CLARIFICATION_REQUIRED` state

**Behavior**:
- Validates process exists
- Checks state requires approval
- Advances to next state
- Returns `AiStateInfo`

**Parameters**:
- `processId` - The process identifier
- `stepId` - Optional step identifier for tracking

**Throws**:
- `IllegalArgumentException` - Process not found
- `IllegalStateException` - State doesn't require approval or cannot advance

**Example**:
```java
AiStateInfo info = orchestrator.approveStep("proc-123", "model-review");
// info.getCurrentState() == BPMN_GENERATED
```

---

#### `retry(processId)`

Retry a failed process.

**Preconditions**:
- Process must exist
- Process must be in `FAILED` state

**Behavior**:
- Validates process exists
- Checks state is `FAILED`
- Resets to `TEXT_RECEIVED` (initial state)
- Returns `AiStateInfo`

**Throws**:
- `IllegalArgumentException` - Process not found
- `IllegalStateException` - Not in FAILED state

**Example**:
```java
AiStateInfo info = orchestrator.retry("proc-123");
// info.getCurrentState() == TEXT_RECEIVED
```

---

### State Query Methods

#### `getStateInfo(processId)`

Get current state information.

**Returns**: `AiStateInfo` or `null` if not tracked

**Example**:
```java
AiStateInfo info = orchestrator.getStateInfo("proc-123");
if (info != null) {
    System.out.println("Current: " + info.getCurrentState());
    System.out.println("Next: " + info.getNextState());
}
```

---

#### `getCurrentState(processId)`

Get current state (raw enum).

**Returns**: `AiState` (defaults to `TEXT_RECEIVED` if not tracked)

**Example**:
```java
AiState state = orchestrator.getCurrentState("proc-123");
if (state.isTerminalState()) {
    System.out.println("Process is complete or failed");
}
```

---

#### `canAdvance(processId)`

Check if process can advance to next state.

**Returns**: `boolean`

**Example**:
```java
if (orchestrator.canAdvance("proc-123")) {
    orchestrator.advanceState("proc-123");
}
```

---

### State Management Methods

#### `setInitialState(processId, initialState)`

Set the initial state for a new process.

**Parameters**:
- `processId` - The process identifier
- `initialState` - Must be `IMAGE_RECEIVED` or `TEXT_RECEIVED`

**Throws**:
- `IllegalArgumentException` - If state is not initial

**Example**:
```java
orchestrator.setInitialState("proc-123", AiState.TEXT_RECEIVED);
```

---

#### `updateState(processId, newState)`

Update state to a specific value.

**Parameters**:
- `processId` - The process identifier
- `newState` - The new state

**Example**:
```java
orchestrator.updateState("proc-123", AiState.MODEL_READY);
```

---

#### `advanceState(processId)`

Advance to the next state in the workflow.

**Returns**: The new `AiState`

**Throws**:
- `IllegalStateException` - Cannot advance from current state

**Example**:
```java
AiState newState = orchestrator.advanceState("proc-123");
```

---

#### `markAsFailed(processId, reason)`

Mark a process as failed.

**Parameters**:
- `processId` - The process identifier
- `reason` - Optional failure reason

**Returns**: `AiStateInfo`

**Example**:
```java
AiStateInfo info = orchestrator.markAsFailed("proc-123", "AI service timeout");
```

---

### Utility Methods

#### `isTracked(processId)`

Check if a process is being tracked.

**Returns**: `boolean`

---

#### `getTrackedProcessCount()`

Get count of tracked processes.

**Returns**: `long`

---

#### `resetState(processId)`

Reset state tracking for a process (useful for testing).

---

#### `clearAll()`

Clear all tracked states (useful for testing).

---

## Data Transfer Objects

### AiStateInfo

Encapsulates AI state information for API responses.

**Fields**:

| Field | Type | Description |
|-------|------|-------------|
| `processId` | String | The process identifier |
| `currentState` | AiState | Current state enum |
| `nextState` | AiState | Next state (null if terminal) |
| `description` | String | Human-readable description |
| `requiresUserAction` | boolean | True if user action needed |
| `complete` | boolean | True if PUBLISHED |
| `failed` | boolean | True if FAILED |

**Example**:
```json
{
  "processId": "proc-123",
  "currentState": "MODEL_READY",
  "nextState": "BPMN_GENERATED",
  "description": "Process model has been created and is ready for user review and approval.",
  "requiresUserAction": true,
  "complete": false,
  "failed": false
}
```

---

## REST Controller

### AiOrchestratorController

Exposes orchestrator functionality via REST API.

**Base Path**: `/api/orchestrator`

**Endpoints**:

| Method | Path | Description |
|--------|------|-------------|
| POST | `/{processId}/start-inference` | Start AI inference |
| POST | `/{processId}/approve` | Approve step |
| POST | `/{processId}/retry` | Retry failed process |
| GET | `/{processId}/state` | Get current state |
| POST | `/{processId}/fail` | Mark as failed |
| POST | `/{processId}/advance` | Advance state |
| GET | `/{processId}/can-advance` | Check if can advance |
| GET | `/tracked-count` | Get tracked count |

See [API.md](API.md) for detailed endpoint documentation.

---

## Usage Examples

### Basic Workflow

```java
// 1. Create process (done by ProcessTextService or ProcessImageUploadService)
ProcessModel process = new ProcessModel();
process.setId("proc-123");
processRepository.save(process);

// 2. Set initial state
orchestrator.setInitialState("proc-123", AiState.TEXT_RECEIVED);

// 3. Start inference
AiStateInfo info = orchestrator.startInference("proc-123");
// State: PROCESS_INFERRED

// 4. Advance to MODEL_READY
orchestrator.advanceState("proc-123");
// State: MODEL_READY

// 5. User approves model
info = orchestrator.approveStep("proc-123", "model-review");
// State: BPMN_GENERATED

// 6. Continue advancing
orchestrator.advanceState("proc-123"); // DRL_GENERATED
orchestrator.advanceState("proc-123"); // PUBLISHED

// 7. Check completion
info = orchestrator.getStateInfo("proc-123");
if (info.isComplete()) {
    System.out.println("Process is published!");
}
```

---

### Error Handling

```java
try {
    orchestrator.startInference("proc-123");
} catch (IllegalArgumentException e) {
    // Process not found
    logger.error("Process not found", e);
} catch (IllegalStateException e) {
    // Invalid state transition
    logger.error("Cannot start inference in current state", e);
}
```

---

### Retry on Failure

```java
// Mark as failed
orchestrator.markAsFailed("proc-123", "AI service unavailable");

// Later, retry
if (orchestrator.getCurrentState("proc-123") == AiState.FAILED) {
    AiStateInfo info = orchestrator.retry("proc-123");
    // State reset to TEXT_RECEIVED
    
    // Restart workflow
    orchestrator.startInference("proc-123");
}
```

---

## Automatic Retry Management

The orchestrator includes built-in automatic retry capabilities for handling BPMN and DRL generation failures gracefully.

### Key Features

- **Auto-retry on failures**: Automatically retries BPMN/DRL generation when errors occur
- **Configurable max retries**: Set per-process maximum retry limits (default: 3)
- **Retry tracking**: Tracks retry count and explanations for each failure
- **Automatic failure**: Marks process as FAILED after max retries exceeded
- **Thread-safe**: All retry operations are thread-safe using ConcurrentHashMap

### Retry Management Methods

#### `setMaxRetries(processId, maxRetries)`

Configure maximum retry count for a process.

**Parameters**:
- `processId` - The process identifier
- `maxRetries` - Maximum retry count (must be >= 0)

**Default**: 3 retries

**Example**:
```java
// Allow up to 5 retry attempts
orchestrator.setMaxRetries("proc-123", 5);

// Disable retries (fail immediately)
orchestrator.setMaxRetries("proc-123", 0);
```

---

#### `getMaxRetries(processId)`

Get configured maximum retry count.

**Returns**: `int` (defaults to 3)

**Example**:
```java
int max = orchestrator.getMaxRetries("proc-123");
System.out.println("Max retries: " + max);
```

---

#### `getRetryCount(processId)`

Get current retry count for a process.

**Returns**: `int`

**Example**:
```java
int count = orchestrator.getRetryCount("proc-123");
System.out.println("Current attempts: " + count);
```

---

#### `getRetryExplanations(processId)`

Get list of all retry reasons.

**Returns**: `List<String>` (empty if no retries)

**Example**:
```java
List<String> explanations = orchestrator.getRetryExplanations("proc-123");
for (String explanation : explanations) {
    System.out.println("Retry reason: " + explanation);
}
```

---

#### `hasReachedMaxRetries(processId)`

Check if process has exceeded max retries.

**Returns**: `boolean`

**Example**:
```java
if (orchestrator.hasReachedMaxRetries("proc-123")) {
    System.out.println("Cannot retry anymore - max reached");
}
```

---

#### `recordBpmnGenerationFailure(processId, errorMessage)`

Record a BPMN generation failure and auto-retry.

**Behavior**:
1. Records the error message as a retry explanation
2. Increments retry count
3. If max retries exceeded → marks as FAILED
4. Otherwise → resets to MODEL_READY for regeneration

**Returns**: `AiStateInfo` with updated state

**Example**:
```java
try {
    String bpmn = bpmnGenerator.generateBpmn(processModel);
} catch (BpmnValidationException e) {
    // Auto-retry with orchestrator
    AiStateInfo info = orchestrator.recordBpmnGenerationFailure(
        processId, 
        e.getMessage()
    );
    
    if (info.isFailed()) {
        logger.error("Max retries reached for BPMN generation");
    } else {
        logger.info("Retrying BPMN generation...");
    }
}
```

---

#### `recordDrlGenerationFailure(processId, errorMessage)`

Record a DRL generation failure and auto-retry.

**Behavior**: Same as `recordBpmnGenerationFailure` but for DRL

**Example**:
```java
try {
    String drl = drlGenerator.generateDrl(rules);
} catch (DrlValidationException e) {
    AiStateInfo info = orchestrator.recordDrlGenerationFailure(
        processId, 
        e.getMessage()
    );
}
```

---

#### `recordGenerationFailure(processId, failureType, errorMessage)`

Record a generic generation failure and auto-retry.

**Parameters**:
- `processId` - The process identifier
- `failureType` - Type of failure (e.g., "BPMN", "DRL", "Validation")
- `errorMessage` - The error message

**Example**:
```java
try {
    validateModel(processModel);
} catch (ValidationException e) {
    orchestrator.recordGenerationFailure(
        processId,
        "Validation",
        e.getMessage()
    );
}
```

---

#### `resetRetryTracking(processId)`

Reset all retry tracking for a process.

**Use Case**: When starting a fresh attempt after user intervention

**Example**:
```java
// User fixed issues manually
orchestrator.resetRetryTracking("proc-123");

// Now start fresh with zero retry count
orchestrator.startInference("proc-123");
```

---

### Retry Workflow

```
BPMN/DRL Generation Attempt
         ↓
    ┌─────────┐
    │ Success │────────→ Continue Workflow
    └─────────┘
         ↓
    ┌─────────┐
    │ Failure │
    └────┬────┘
         │
         ↓
   Record Explanation
         │
         ↓
   Increment Retry Count
         │
         ↓
    ┌─────────────────┐
    │ Check Retry     │
    │ Count > Max?    │
    └────┬────┬───────┘
         │    │
      Yes│    │No
         │    │
         ↓    ↓
    ┌────────┐  ┌─────────────┐
    │ FAILED │  │ MODEL_READY │
    └────────┘  └──────┬──────┘
                       │
                       ↓
                Auto-Retry Generation
```

### Usage Examples

#### Example 1: BPMN Generation with Auto-Retry

```java
@Service
public class BpmnWorkflowService {
    
    @Autowired
    private AiOrchestratorService orchestrator;
    
    @Autowired
    private BpmnGeneratorService bpmnGenerator;
    
    public void generateBpmn(String processId) {
        ProcessModel model = getProcessModel(processId);
        
        try {
            String bpmn = bpmnGenerator.generateBpmn(model);
            saveGenerated Bpmn(processId, bpmn);
            
            // Success - advance to next state
            orchestrator.advanceState(processId);
            
        } catch (BpmnValidationException e) {
            logger.error("BPMN generation failed: {}", e.getMessage());
            
            // Auto-retry with orchestrator
            AiStateInfo info = orchestrator.recordBpmnGenerationFailure(
                processId, 
                e.getErrorSummary()
            );
            
            if (info.isFailed()) {
                logger.error("Process failed after {} retries", 
                    orchestrator.getRetryCount(processId));
                notifyUser(processId, "Max retries reached");
            } else {
                logger.info("Retrying BPMN generation (attempt {}/{})", 
                    orchestrator.getRetryCount(processId),
                    orchestrator.getMaxRetries(processId));
                
                // Retry generation
                generateBpmn(processId);
            }
        }
    }
}
```

#### Example 2: DRL Generation with Auto-Retry

```java
public void generateDrl(String processId) {
    List<RuleModel> rules = getRulesForProcess(processId);
    
    try {
        String drl = drlGenerator.generateDrl(rules, "com.example.rules", true);
        saveDrl(processId, drl);
        
        // Success
        orchestrator.advanceState(processId);
        
    } catch (DrlValidationException e) {
        // Auto-retry
        AiStateInfo info = orchestrator.recordDrlGenerationFailure(
            processId, 
            e.getErrorSummary()
        );
        
        if (!info.isFailed()) {
            // Retry
            generateDrl(processId);
        }
    }
}
```

#### Example 3: Custom Retry Limits

```java
// High-priority process - allow more retries
orchestrator.setMaxRetries("important-proc", 10);

// Simple process - fewer retries
orchestrator.setMaxRetries("simple-proc", 1);

// Critical process - no auto-retry (fail fast)
orchestrator.setMaxRetries("critical-proc", 0);
```

#### Example 4: Monitoring Retry Status

```java
public RetryStatus getRetryStatus(String processId) {
    int current = orchestrator.getRetryCount(processId);
    int max = orchestrator.getMaxRetries(processId);
    List<String> explanations = orchestrator.getRetryExplanations(processId);
    
    return new RetryStatus(current, max, explanations);
}
```

#### Example 5: Retry with User Intervention

```java
// After max retries reached
if (orchestrator.hasReachedMaxRetries(processId)) {
    // Get all retry explanations
    List<String> errors = orchestrator.getRetryExplanations(processId);
    
    // Show to user for manual fixes
    showErrorsToUser(processId, errors);
    
    // After user fixes issues
    orchestrator.resetRetryTracking(processId);
    
    // Start fresh
    orchestrator.updateState(processId, AiState.MODEL_READY);
    generateBpmn(processId);
}
```

### Best Practices

1. **Set Appropriate Limits**
   ```java
   // For AI-generated content (may need multiple tries)
   orchestrator.setMaxRetries(processId, 5);
   
   // For validation (should succeed quickly)
   orchestrator.setMaxRetries(processId, 1);
   ```

2. **Log Retry Attempts**
   ```java
   int attempt = orchestrator.getRetryCount(processId);
   logger.warn("Retry attempt {}/{}: {}", 
       attempt, 
       orchestrator.getMaxRetries(processId),
       errorMessage
   );
   ```

3. **Monitor Retry Patterns**
   ```java
   // Track which errors occur most often
   List<String> explanations = orchestrator.getRetryExplanations(processId);
   analyzeCommonFailures(explanations);
   ```

4. **Reset After Manual Fixes**
   ```java
   // After user intervention
   orchestrator.resetRetryTracking(processId);
   ```

5. **Provide Feedback**
   ```java
   if (info.isFailed()) {
       List<String> errors = orchestrator.getRetryExplanations(processId);
       return new ErrorResponse(
           "Generation failed after " + errors.size() + " attempts",
           errors
       );
   }
   ```

### Data Structures

The orchestrator maintains three tracking maps:

1. **`retryCountTracker`**: `ConcurrentHashMap<String, Integer>`
   - Maps processId → current retry count

2. **`retryExplanations`**: `ConcurrentHashMap<String, List<String>>`
   - Maps processId → list of retry reasons

3. **`maxRetriesPerProcess`**: `ConcurrentHashMap<String, Integer>`
   - Maps processId → max retry limit
   - Defaults to `DEFAULT_MAX_RETRIES` (3) if not set

All maps are thread-safe and cleared by `clearAll()`.

---

## Testing

### Unit Tests

Comprehensive unit tests in `AiOrchestratorServiceTest`:

**Core Functionality** (25 tests):
- ✅ Start inference from different initial states
- ✅ Approve steps
- ✅ Manual retry of failed processes
- ✅ State queries
- ✅ State management
- ✅ Error conditions
- ✅ Complete workflow

**Clarification Workflow** (9 tests):
- ✅ Request clarification
- ✅ Submit clarification response
- ✅ Cancel clarification
- ✅ Pending clarification tracking

**Auto-Retry Management** (13 tests):
- ✅ Set/get max retries
- ✅ Record BPMN generation failures
- ✅ Record DRL generation failures
- ✅ Auto-retry until max reached
- ✅ Mark as FAILED after max retries
- ✅ Retry explanation tracking
- ✅ Mixed failure types
- ✅ Zero retries (immediate failure)
- ✅ Reset retry tracking
- ✅ Clear all clears retry data

**Total**: 47 tests, all passing ✅

**Run tests**:
```bash
./gradlew test --tests AiOrchestratorServiceTest
```

---

### Integration Tests

Full integration tests in `AiOrchestratorControllerTest`:

- ✅ All REST endpoints
- ✅ Error responses
- ✅ Complete workflow via HTTP
- ✅ State persistence

**Run tests**:
```bash
./gradlew test --tests AiOrchestratorControllerTest
```

---

## Thread Safety

The orchestrator is thread-safe:

- Uses `ConcurrentHashMap` for state storage
- All operations are atomic at the map level
- No synchronized blocks needed for basic operations

**Example - Concurrent Access**:
```java
// Multiple threads can safely access the orchestrator
ExecutorService executor = Executors.newFixedThreadPool(10);

for (int i = 0; i < 100; i++) {
    final String processId = "proc-" + i;
    executor.submit(() -> {
        orchestrator.setInitialState(processId, AiState.TEXT_RECEIVED);
        orchestrator.startInference(processId);
    });
}
```

---

## Future Enhancements

### AI Integration (Not Yet Implemented)

When integrating actual AI services:

1. **In `startInference()`**:
   ```java
   // Current: Immediately transitions to PROCESS_INFERRED
   // Future: Call AI service asynchronously
   CompletableFuture<ProcessModel> aiResult = aiService.inferProcess(processId);
   aiResult.thenAccept(model -> {
       // Update process model
       // Transition to PROCESS_INFERRED or CLARIFICATION_REQUIRED
   });
   ```

2. **In `approveStep()` for MODEL_READY**:
   ```java
   // Future: Generate BPMN XML
   String bpmnXml = bpmnGenerator.generate(processModel);
   // Store BPMN
   // Transition to BPMN_GENERATED
   ```

3. **In `advanceState()` for BPMN_GENERATED**:
   ```java
   // Future: Generate DRL rules
   String drlRules = droolsGenerator.generate(processModel);
   // Store DRL
   // Transition to DRL_GENERATED
   ```

---

### Persistence

Currently in-memory. For production:

1. **Add State Repository**:
   ```java
   public interface AiStateRepository {
       void save(String processId, AiState state);
       Optional<AiState> find(String processId);
       void delete(String processId);
   }
   ```

2. **Implement with Database**:
   ```java
   @Entity
   public class ProcessState {
       @Id
       private String processId;
       
       @Enumerated(EnumType.STRING)
       private AiState state;
       
       private LocalDateTime lastUpdated;
   }
   ```

---

### Event Publishing

Add event publishing for state changes:

```java
public interface AiStateEventPublisher {
    void publishStateChange(String processId, AiState oldState, AiState newState);
}

// In updateState()
AiState oldState = stateTracker.put(processId, newState);
eventPublisher.publishStateChange(processId, oldState, newState);
```

---

### Async Processing

Make state transitions asynchronous:

```java
@Async
public CompletableFuture<AiStateInfo> startInferenceAsync(String processId) {
    // Long-running AI processing
    return CompletableFuture.completedFuture(startInference(processId));
}
```

---

## Configuration

No configuration required currently. All state is in-memory.

For future database persistence, add to `application.yml`:

```yaml
app:
  orchestrator:
    persistence:
      enabled: true
      type: database  # or redis, etc.
    async:
      enabled: true
      thread-pool-size: 10
```

---

## Monitoring

### Metrics to Track

1. **Process Count by State**:
   ```java
   Map<AiState, Long> countByState = stateTracker.values().stream()
       .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
   ```

2. **Average Time in Each State**:
   - Track state entry timestamps
   - Calculate duration when transitioning

3. **Failure Rate**:
   - Count processes in FAILED state
   - Track failure reasons

---

## Troubleshooting

### Problem: State not persisting across restarts

**Cause**: In-memory storage

**Solution**: Implement persistence (see Future Enhancements)

---

### Problem: Concurrent modification issues

**Cause**: External code modifying state tracker

**Solution**: State tracker is private; only use service methods

---

### Problem: Process stuck in CLARIFICATION_REQUIRED

**Cause**: No automatic transition from this state

**Solution**: User must provide clarification, then manually update state or restart inference

---

## Summary

The `AiOrchestratorService` provides:

✅ **State Management**: Track AI workflow states  
✅ **Validation**: Ensure valid state transitions  
✅ **Thread Safety**: Concurrent access support  
✅ **REST API**: Full HTTP interface  
✅ **Testing**: Comprehensive unit and integration tests  
✅ **Extensibility**: Ready for AI integration  

**Status**: ✅ Complete (without AI integration)

**Next Steps**:
1. Integrate actual AI services
2. Add persistence layer
3. Implement event publishing
4. Add monitoring/metrics

---

## See Also

- [API Documentation](API.md) - REST endpoint details
- [Model Classes](MODEL_CLASSES.md) - AiState enum documentation
- [Repositories](REPOSITORIES.md) - Process storage

