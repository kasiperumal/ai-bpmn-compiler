# Automatic Retry Enhancement - Quick Reference

## Overview

The `AiOrchestratorService` now includes automatic retry capabilities for handling BPMN and DRL generation failures.

## Key Features

✅ **Auto-retry on failures** - Automatically retries when generation fails  
✅ **Configurable limits** - Per-process maximum retry count (default: 3)  
✅ **Retry tracking** - Tracks count and explanations for each failure  
✅ **Automatic failure** - Marks FAILED when max retries exceeded  
✅ **Thread-safe** - All operations use `ConcurrentHashMap`

## Quick Start

### Basic Usage

```java
// Set custom retry limit (optional)
orchestrator.setMaxRetries(processId, 5);

// Record BPMN failure - auto-retry
try {
    String bpmn = bpmnGenerator.generateBpmn(model);
} catch (BpmnValidationException e) {
    AiStateInfo info = orchestrator.recordBpmnGenerationFailure(
        processId, 
        e.getMessage()
    );
    
    if (info.isFailed()) {
        // Max retries reached
        handleMaxRetriesReached(processId);
    } else {
        // Auto-retry triggered - state reset to MODEL_READY
        retryGeneration(processId);
    }
}

// Record DRL failure - auto-retry
try {
    String drl = drlGenerator.generateDrl(rules);
} catch (DrlValidationException e) {
    AiStateInfo info = orchestrator.recordDrlGenerationFailure(
        processId, 
        e.getMessage()
    );
}
```

## API Reference

### Configuration

```java
// Set max retries (default: 3)
setMaxRetries(processId, maxRetries)

// Get max retries
int max = getMaxRetries(processId)
```

### Status Checking

```java
// Get current retry count
int count = getRetryCount(processId)

// Get all retry explanations
List<String> reasons = getRetryExplanations(processId)

// Check if max exceeded
boolean exceeded = hasReachedMaxRetries(processId)
```

### Recording Failures

```java
// Record BPMN failure
AiStateInfo info = recordBpmnGenerationFailure(processId, errorMessage)

// Record DRL failure
AiStateInfo info = recordDrlGenerationFailure(processId, errorMessage)

// Record generic failure
AiStateInfo info = recordGenerationFailure(processId, failureType, errorMessage)
```

### Reset

```java
// Reset retry tracking
resetRetryTracking(processId)

// Clear all (including retry data)
clearAll()
```

## Retry Workflow

```
Generation Attempt
      ↓
   Success? ─Yes─→ Continue Workflow
      │
     No
      ↓
Record Explanation
      ↓
Increment Count
      ↓
Count > Max?
      │
  ┌───┴───┐
Yes│       │No
  ↓       ↓
FAILED  MODEL_READY
        (retry)
```

## Examples

### Example 1: BPMN with Custom Retry Limit

```java
public void processWithCustomRetries(String processId) {
    // High-priority process - allow more retries
    orchestrator.setMaxRetries(processId, 10);
    
    try {
        generateBpmn(processId);
    } catch (BpmnValidationException e) {
        AiStateInfo info = orchestrator.recordBpmnGenerationFailure(
            processId, 
            e.getMessage()
        );
        
        if (info.isFailed()) {
            logger.error("Failed after {} attempts", 
                orchestrator.getRetryCount(processId));
            
            // Get all failure reasons
            List<String> reasons = orchestrator.getRetryExplanations(processId);
            notifyAdmin(processId, reasons);
        }
    }
}
```

### Example 2: Zero Retries (Fail Fast)

```java
// Critical process - no retries
orchestrator.setMaxRetries(processId, 0);

AiStateInfo info = orchestrator.recordBpmnGenerationFailure(
    processId, 
    "Invalid structure"
);

// Immediately fails - no retry
assert info.isFailed();
assert orchestrator.getRetryCount(processId) == 1;
```

### Example 3: Monitoring Retry Status

```java
public RetryStatusResponse getRetryStatus(String processId) {
    int current = orchestrator.getRetryCount(processId);
    int max = orchestrator.getMaxRetries(processId);
    List<String> explanations = orchestrator.getRetryExplanations(processId);
    
    return new RetryStatusResponse(
        current,
        max,
        current > 0 ? current + "/" + max : "No retries",
        explanations
    );
}
```

### Example 4: Mixed Failures (BPMN + DRL)

```java
// First BPMN fails
orchestrator.recordBpmnGenerationFailure(processId, "BPMN Error 1");
// count = 1, explanations = ["BPMN Generation Failed: BPMN Error 1"]

// Then DRL fails
orchestrator.recordDrlGenerationFailure(processId, "DRL Error 1");
// count = 2, explanations = [... , "DRL Generation Failed: DRL Error 1"]

// BPMN fails again
orchestrator.recordBpmnGenerationFailure(processId, "BPMN Error 2");
// count = 3, explanations = [... , "BPMN Generation Failed: BPMN Error 2"]

// Max reached (3) - next failure marks as FAILED
AiStateInfo info = orchestrator.recordBpmnGenerationFailure(processId, "Final error");
assert info.isFailed();
assert orchestrator.getRetryCount(processId) == 4;
```

### Example 5: Reset and Retry After User Intervention

```java
// Process failed after max retries
if (orchestrator.hasReachedMaxRetries(processId)) {
    List<String> errors = orchestrator.getRetryExplanations(processId);
    
    // Show errors to user
    UserFeedback feedback = showErrorsToUser(processId, errors);
    
    // User made manual fixes
    applyUserFixes(processId, feedback);
    
    // Reset retry tracking
    orchestrator.resetRetryTracking(processId);
    
    // Start fresh
    orchestrator.updateState(processId, AiState.MODEL_READY);
    generateBpmn(processId); // Will start with retry count = 0
}
```

## Integration with Services

### BpmnGeneratorService

```java
@Service
public class BpmnWorkflowService {
    
    @Autowired
    private AiOrchestratorService orchestrator;
    
    @Autowired
    private BpmnGeneratorService bpmnGenerator;
    
    public void generateAndAdvance(String processId) {
        ProcessModel model = getModel(processId);
        
        try {
            String bpmn = bpmnGenerator.generateBpmn(model);
            saveBpmn(processId, bpmn);
            
            // Success - advance to next state
            orchestrator.advanceState(processId);
            
        } catch (BpmnValidationException e) {
            // Auto-retry
            AiStateInfo info = orchestrator.recordBpmnGenerationFailure(
                processId, 
                e.getErrorSummary()
            );
            
            if (!info.isFailed()) {
                // Retry
                generateAndAdvance(processId);
            }
        }
    }
}
```

### DrlGeneratorService

```java
@Service
public class DrlWorkflowService {
    
    @Autowired
    private AiOrchestratorService orchestrator;
    
    @Autowired
    private DrlGeneratorService drlGenerator;
    
    public void generateAndAdvance(String processId) {
        List<RuleModel> rules = getRules(processId);
        
        try {
            String drl = drlGenerator.generateDrl(rules);
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
                generateAndAdvance(processId);
            }
        }
    }
}
```

## Best Practices

### 1. Set Appropriate Limits

```java
// AI-generated content (may need multiple tries)
orchestrator.setMaxRetries(processId, 5);

// Validation (should succeed quickly)
orchestrator.setMaxRetries(processId, 1);

// Critical (fail fast)
orchestrator.setMaxRetries(processId, 0);
```

### 2. Log Retry Attempts

```java
int attempt = orchestrator.getRetryCount(processId);
int max = orchestrator.getMaxRetries(processId);

logger.warn("Retry attempt {}/{}: {}", attempt, max, errorMessage);
```

### 3. Monitor Patterns

```java
// Analyze which errors occur most often
List<String> explanations = orchestrator.getRetryExplanations(processId);
Map<String, Integer> errorTypes = analyzeErrorPatterns(explanations);
```

### 4. Provide User Feedback

```java
if (info.isFailed()) {
    int attempts = orchestrator.getRetryCount(processId);
    List<String> errors = orchestrator.getRetryExplanations(processId);
    
    return ErrorResponse.builder()
        .message("Generation failed after " + attempts + " attempts")
        .errors(errors)
        .processId(processId)
        .build();
}
```

### 5. Reset After Manual Fixes

```java
// After user intervention
orchestrator.resetRetryTracking(processId);

// Now start fresh
orchestrator.updateState(processId, AiState.MODEL_READY);
```

## Data Structures

### Tracking Maps

All thread-safe using `ConcurrentHashMap`:

```java
// Retry count per process
ConcurrentHashMap<String, Integer> retryCountTracker

// Retry reasons per process
ConcurrentHashMap<String, List<String>> retryExplanations

// Max retries per process (defaults to 3)
ConcurrentHashMap<String, Integer> maxRetriesPerProcess
```

## Testing

### Test Coverage

**13 new tests** in `AiOrchestratorServiceTest`:

```
✅ testSetMaxRetries()
✅ testSetMaxRetriesNegative()
✅ testGetMaxRetriesDefaultValue()
✅ testGetRetryCount()
✅ testRecordBpmnGenerationFailure()
✅ testRecordDrlGenerationFailure()
✅ testAutoRetryUntilMaxReached()
✅ testHasReachedMaxRetries()
✅ testRecordGeneralGenerationFailure()
✅ testResetRetryTracking()
✅ testClearAllClearsRetryTracking()
✅ testMixedBpmnAndDrlFailures()
✅ testZeroMaxRetriesImmediateFailure()
```

**Total**: 47 tests (all passing ✅)

### Running Tests

```bash
# Run all orchestrator tests
./gradlew test --tests AiOrchestratorServiceTest

# Run specific retry test
./gradlew test --tests AiOrchestratorServiceTest.testAutoRetryUntilMaxReached

# Full build
./gradlew clean build
```

## Performance

- **Thread-safe**: All operations use `ConcurrentHashMap`
- **O(1)** lookup for retry counts and max retries
- **Minimal overhead**: Only tracks active processes
- **Memory efficient**: Cleared with `clearAll()`

## Troubleshooting

### Issue: Process fails immediately

```java
// Check max retries
int max = orchestrator.getMaxRetries(processId);
if (max == 0) {
    orchestrator.setMaxRetries(processId, 3);
}
```

### Issue: Infinite retry loop

```java
// This CANNOT happen - max retries enforced
// After max retries, state → FAILED
```

### Issue: Lost retry tracking

```java
// Ensure not calling clearAll() or resetRetryTracking() unexpectedly
// Check logs for these operations
```

### Issue: Wrong retry count

```java
// Remember: retry count increments BEFORE checking max
// maxRetries=3 means:
//   - 1st failure: count=1, retry
//   - 2nd failure: count=2, retry
//   - 3rd failure: count=3, retry
//   - 4th failure: count=4, FAILED
```

## Related Documentation

- [AI_ORCHESTRATOR.md](./AI_ORCHESTRATOR.md) - Complete orchestrator documentation
- [ORCHESTRATOR_SUMMARY.md](./ORCHESTRATOR_SUMMARY.md) - Implementation summary
- [API.md](./API.md) - REST API documentation

## Implementation Details

**Files Modified**:
- `AiOrchestratorService.java` (+150 lines)
- `AiOrchestratorServiceTest.java` (+250 lines)
- `AI_ORCHESTRATOR.md` (+350 lines)

**Date**: January 2, 2026  
**Spring Boot**: 3.4.3  
**Java**: 17

---

For detailed documentation, see [AI_ORCHESTRATOR.md](./AI_ORCHESTRATOR.md)

