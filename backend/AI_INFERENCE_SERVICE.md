# AI Inference Service

## Overview

The `AiInferenceService` provides AI-powered process inference capabilities using Google Gemini. It analyzes BPMN diagram images and generates business-friendly descriptions of workflows without technical BPMN terminology.

**Key Features**:
- ✅ Image-to-text process description inference
- ✅ Business-friendly language (no BPMN jargon)
- ✅ Structured output format
- ✅ Explicit ambiguity detection
- ✅ Multiple image format support
- ✅ Comprehensive error handling

---

## API Reference

### `inferProcessDescriptionFromImage(String processId)`

Analyze a process diagram image and generate a detailed business description.

**Parameters**:
- `processId` (String, required): The process identifier

**Returns**: `String` - Structured text description of the process

**Throws**:
- `IllegalArgumentException` - If process not found or image file not found
- `RuntimeException` - If AI inference fails

**Example**:
```java
@Autowired
private AiInferenceService aiInferenceService;

public void analyzeProcess(String processId) {
    String description = aiInferenceService.inferProcessDescriptionFromImage(processId);
    System.out.println(description);
}
```

---

### `hasProcessImage(String processId)`

Check if a process has an associated image file.

**Parameters**:
- `processId` (String, required): The process identifier

**Returns**: `boolean` - true if image exists, false otherwise

**Example**:
```java
if (aiInferenceService.hasProcessImage(processId)) {
    String description = aiInferenceService.inferProcessDescriptionFromImage(processId);
}
```

---

## Output Structure

The service generates descriptions in the following structured format:

### Example Output

```markdown
## Overview
This process handles customer order fulfillment from receipt to delivery.

## Main Flow
1. The process begins when a customer order is received
2. Order details are validated for completeness
3. Inventory is checked for product availability
4. If items are in stock, the order proceeds to fulfillment
5. Items are picked from the warehouse
6. Order is packed and shipping label is created
7. Package is handed to shipping carrier
8. The process completes when delivery confirmation is received

## Decision Points
- After validation: If order is incomplete, it is rejected
- After inventory check: If items are out of stock, customer is notified
- During fulfillment: If quality issues found, item returns to inventory

## Alternative Paths
- Rejected orders: Customer receives rejection notice with reason
- Out of stock: Option to backorder or cancel
- Failed delivery: Package returns to warehouse for reprocessing

## Parallel Activities
None identified - process appears to be sequential

## Process Completion
- Success: Delivery confirmed, customer receives notification
- Cancellation: Customer notified, refund processed
- Rejection: Order not processed, customer notified

## Ambiguities and Uncertainties
- It's unclear who approves high-value orders
- The diagram doesn't show what happens if payment fails
- Timing for inventory checks is not specified

## Additional Observations
- Process involves warehouse staff, shipping coordinator, and customer service
- Key data: order number, customer info, inventory levels, tracking number
- No exception handling visible for carrier issues
```

---

## Prompt Engineering

The service uses a carefully crafted prompt that instructs Gemini to:

### 1. Use Business Language, Not BPMN Terms

**Avoid**:
- Start Event
- End Event
- Task/Activity
- Gateway
- Sequence Flow
- Pool/Lane

**Use Instead**:
- "The process begins when..."
- "The process completes when..."
- "Step" or "Action"
- "Decision point" or "Choice"
- "Then" or "Next"
- "Role" or "Actor"

### 2. Describe Key Elements

- **Steps**: What actions are performed? Who performs them?
- **Decisions**: What choices exist? What are the conditions?
- **Conditions**: Under what circumstances does each path execute?
- **Flow**: How do steps connect? Sequential vs. parallel?
- **Roles**: What actors or systems are involved?
- **Data**: What information is needed or produced?

### 3. Call Out Ambiguities Explicitly

The AI is instructed to be honest about uncertainties:
- Unclear or incomplete diagrams
- Missing or hard-to-read labels
- Ambiguous flow logic
- Multiple possible interpretations

Uses phrases like:
- "It appears that..."
- "This step seems to..."
- "It's unclear..."
- "The diagram doesn't show..."

### 4. Provide Structured Output

Ensures consistent format with clear sections:
- Overview
- Main Flow
- Decision Points
- Alternative Paths
- Parallel Activities
- Process Completion
- Ambiguities and Uncertainties
- Additional Observations

---

## Usage Examples

### Basic Usage

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProcessAnalysisService {
    
    @Autowired
    private AiInferenceService aiInferenceService;
    
    public String analyzeUploadedDiagram(String processId) {
        // Infer description from image
        String description = aiInferenceService
            .inferProcessDescriptionFromImage(processId);
        
        // Use description for further processing
        return description;
    }
}
```

---

### Integration with AI Orchestrator

```java
@Service
public class WorkflowOrchestrationService {
    
    @Autowired
    private AiInferenceService aiInferenceService;
    
    @Autowired
    private AiOrchestratorService orchestrator;
    
    @Autowired
    private ProcessModelRepository processRepository;
    
    public void executeImageInference(String processId) {
        try {
            // Start inference
            orchestrator.startInference(processId);
            
            // Use AI to analyze image
            String description = aiInferenceService
                .inferProcessDescriptionFromImage(processId);
            
            // Store description in process model
            ProcessModel process = processRepository.findById(processId).get();
            // Could add a description field to ProcessModel
            // process.setAiGeneratedDescription(description);
            // processRepository.save(process);
            
            // Advance state
            orchestrator.advanceState(processId);
            
        } catch (Exception e) {
            orchestrator.markAsFailed(processId, e.getMessage());
        }
    }
}
```

---

### Error Handling

```java
@Service
public class SafeInferenceService {
    
    @Autowired
    private AiInferenceService aiInferenceService;
    
    public String inferWithFallback(String processId) {
        try {
            // Check if image exists first
            if (!aiInferenceService.hasProcessImage(processId)) {
                return "No diagram image available for analysis.";
            }
            
            // Attempt inference
            return aiInferenceService.inferProcessDescriptionFromImage(processId);
            
        } catch (IllegalArgumentException e) {
            // Process or image not found
            log.error("Invalid process or missing image: {}", e.getMessage());
            return "Unable to analyze: " + e.getMessage();
            
        } catch (RuntimeException e) {
            // AI/Gemini error
            log.error("AI inference failed: {}", e.getMessage());
            return "AI analysis failed. Please try again later.";
        }
    }
}
```

---

### Retry Logic

```java
@Service
public class ResilientInferenceService {
    
    @Autowired
    private AiInferenceService aiInferenceService;
    
    public String inferWithRetry(String processId, int maxRetries) {
        int attempts = 0;
        Exception lastException = null;
        
        while (attempts < maxRetries) {
            try {
                return aiInferenceService.inferProcessDescriptionFromImage(processId);
                
            } catch (RuntimeException e) {
                lastException = e;
                attempts++;
                
                if (attempts < maxRetries) {
                    // Exponential backoff
                    long delay = (long) Math.pow(2, attempts) * 1000;
                    Thread.sleep(delay);
                    log.info("Retry attempt {} after {}ms", attempts, delay);
                }
            }
        }
        
        throw new RuntimeException(
            "Failed after " + maxRetries + " attempts",
            lastException
        );
    }
}
```

---

## Image File Discovery

The service automatically finds process images by looking for these files:

```
./data/uploads/{processId}/original.png
./data/uploads/{processId}/original.jpg
./data/uploads/{processId}/original.jpeg
./data/uploads/{processId}/original.gif
./data/uploads/{processId}/original.webp
```

**Search Order**:
1. PNG (`.png`)
2. JPEG (`.jpg`)
3. JPEG (`.jpeg`)
4. GIF (`.gif`)
5. WebP (`.webp`)

The first matching file is used.

---

## Configuration

### Required Configuration

**application.yml**:
```yaml
app:
  upload:
    base-dir: ./data/uploads
```

### Gemini Configuration

Requires GeminiClient configuration (see [GEMINI_CLIENT.md](GEMINI_CLIENT.md)):

```yaml
spring:
  ai:
    vertex:
      ai:
        gemini:
          project-id: ${VERTEX_AI_PROJECT_ID}
          location: ${VERTEX_AI_LOCATION}
          api-key: ${GEMINI_API_KEY}
```

---

## Testing

### Unit Tests

The service includes 11 comprehensive tests:

```bash
./gradlew test --tests AiInferenceServiceTest
```

**Test Coverage**:
- ✅ Successful inference
- ✅ Process not found
- ✅ Image not found
- ✅ Gemini API error
- ✅ Multiple image format support
- ✅ Prompt validation
- ✅ Response structure verification
- ✅ Business language validation
- ✅ `hasProcessImage()` with existing image
- ✅ `hasProcessImage()` without image
- ✅ `hasProcessImage()` with nonexistent directory

---

### Integration Testing

```java
@SpringBootTest
class AiInferenceIntegrationTest {
    
    @Autowired
    private AiInferenceService aiInferenceService;
    
    @Autowired
    private ProcessModelRepository processRepository;
    
    @Test
    @Disabled("Requires valid API key and test image")
    void testRealInference() throws IOException {
        // Setup: Create test process and image
        ProcessModel process = new ProcessModel();
        process.setId("test-proc");
        processRepository.save(process);
        
        // Copy test BPMN image to upload directory
        Path testImage = Paths.get("src/test/resources/test-diagram.png");
        Path uploadDir = Paths.get("./data/uploads/test-proc");
        Files.createDirectories(uploadDir);
        Files.copy(testImage, uploadDir.resolve("original.png"));
        
        // Execute
        String description = aiInferenceService
            .inferProcessDescriptionFromImage("test-proc");
        
        // Verify
        assertNotNull(description);
        assertTrue(description.contains("## Overview"));
        assertTrue(description.length() > 100);
    }
}
```

---

## Performance Considerations

### Response Times

- **Typical**: 3-10 seconds
- **Factors**:
  - Image size and complexity
  - Gemini API response time
  - Network latency
  - Current API load

### Optimization Tips

1. **Optimize images before upload**:
   ```java
   // Resize large images to max 2048x2048
   // Compress to reduce file size
   ```

2. **Cache results**:
   ```java
   @Cacheable("process-descriptions")
   public String inferProcessDescriptionFromImage(String processId) {
       // ... existing code
   }
   ```

3. **Use async processing**:
   ```java
   @Async
   public CompletableFuture<String> inferAsync(String processId) {
       String description = inferProcessDescriptionFromImage(processId);
       return CompletableFuture.completedFuture(description);
   }
   ```

4. **Batch processing**:
   ```java
   public Map<String, String> inferMultiple(List<String> processIds) {
       // Process multiple diagrams
   }
   ```

---

## Best Practices

### 1. Validate Before Calling

```java
// Check process exists
if (!processRepository.existsById(processId)) {
    throw new IllegalArgumentException("Process not found");
}

// Check image exists
if (!aiInferenceService.hasProcessImage(processId)) {
    throw new IllegalArgumentException("No image available");
}

// Then infer
String description = aiInferenceService.inferProcessDescriptionFromImage(processId);
```

### 2. Handle Large Responses

```java
String description = aiInferenceService.inferProcessDescriptionFromImage(processId);

if (description.length() > 10000) {
    log.warn("Very long description: {} chars", description.length());
    // Maybe truncate or paginate
}
```

### 3. Log for Debugging

```java
log.info("Starting inference for processId: {}", processId);
String description = aiInferenceService.inferProcessDescriptionFromImage(processId);
log.info("Inference complete: {} chars, {} sections",
    description.length(),
    countSections(description));
```

### 4. Provide User Feedback

```java
// Async with progress updates
CompletableFuture.supplyAsync(() -> {
    notifyUser(processId, "Analyzing diagram...");
    String desc = aiInferenceService.inferProcessDescriptionFromImage(processId);
    notifyUser(processId, "Analysis complete!");
    return desc;
});
```

---

## Troubleshooting

### Problem: "Image file not found for process"

**Cause**: No image uploaded or file in wrong location

**Solution**:
```java
// Verify image exists
Path expectedPath = Paths.get("./data/uploads/" + processId + "/original.png");
if (!Files.exists(expectedPath)) {
    log.error("Image not found at: {}", expectedPath);
    // Check if image was uploaded
    // Check file permissions
    // Check upload service logs
}
```

---

### Problem: Empty or unexpected description

**Cause**: Poor image quality or Gemini misinterpretation

**Solution**:
1. Improve image quality (higher resolution, clearer labels)
2. Re-upload image
3. Check Gemini logs for specific errors
4. Try with a simpler diagram first

---

### Problem: "Failed to infer process description"

**Cause**: Gemini API error (rate limit, authentication, network)

**Solution**:
```java
try {
    return aiInferenceService.inferProcessDescriptionFromImage(processId);
} catch (RuntimeException e) {
    if (e.getMessage().contains("rate limit")) {
        // Wait and retry
        Thread.sleep(5000);
        return aiInferenceService.inferProcessDescriptionFromImage(processId);
    }
    throw e;
}
```

---

## Output Quality

### What Makes a Good Description

✅ **Clear business language**
- No BPMN jargon
- Easy for non-technical users to understand

✅ **Structured and organized**
- Follows the template sections
- Logical flow

✅ **Specific and detailed**
- Names actual steps and decisions
- Provides conditions and criteria

✅ **Honest about limitations**
- Calls out ambiguities
- Acknowledges missing information

✅ **Actionable**
- Provides enough detail for business users to validate
- Identifies areas needing clarification

### Example Quality Indicators

```java
// Good description
String description = aiInferenceService.inferProcessDescriptionFromImage(processId);

// Quality checks
boolean hasOverview = description.contains("## Overview");
boolean hasMainFlow = description.contains("## Main Flow");
boolean hasAmbiguities = description.contains("## Ambiguities");
boolean avoidsBpmnTerms = !description.contains("Start Event") &&
                          !description.contains("Gateway") &&
                          !description.contains("Sequence Flow");

if (hasOverview && hasMainFlow && hasAmbiguities && avoidsBpmnTerms) {
    log.info("High quality description generated");
}
```

---

## Dependencies

- **GeminiClient**: For AI inference
- **ProcessModelRepository**: For process lookup
- **application.yml**: For upload directory configuration

---

## Future Enhancements

### 1. Support for Text-Based Inference

```java
public String inferProcessDescriptionFromText(String processId) {
    ProcessModel process = processRepository.findById(processId).get();
    String textDescription = process.getDescription();
    
    // Use GeminiClient to analyze text description
    String prompt = createTextInferencePrompt(textDescription);
    return geminiClient.generateFromText(prompt);
}
```

### 2. Multi-Language Support

```java
public String inferProcessDescriptionFromImage(
        String processId, 
        Locale locale) {
    // Generate description in user's language
}
```

### 3. Customizable Output Format

```java
public String inferProcessDescriptionFromImage(
        String processId,
        OutputFormat format) {
    // JSON, XML, or Markdown
}
```

### 4. Confidence Scoring

```java
public InferenceResult inferWithConfidence(String processId) {
    String description = inferProcessDescriptionFromImage(processId);
    double confidence = calculateConfidence(description);
    return new InferenceResult(description, confidence);
}
```

---

## Summary

The `AiInferenceService` provides:

✅ **AI-powered diagram analysis** using Gemini  
✅ **Business-friendly output** without BPMN jargon  
✅ **Structured descriptions** with clear sections  
✅ **Explicit ambiguity detection**  
✅ **Multiple image format support**  
✅ **Comprehensive error handling**  
✅ **Full test coverage** (11 tests)  
✅ **Production-ready** implementation  

**Status**: ✅ **READY FOR INTEGRATION**

---

## See Also

- [Gemini Client Documentation](GEMINI_CLIENT.md)
- [AI Orchestrator Documentation](AI_ORCHESTRATOR.md)
- [Process Model Documentation](MODEL_CLASSES.md)
- [API Documentation](API.md)

---

**Implementation Date**: January 2, 2026  
**Dependencies**: GeminiClient, Spring AI 1.0.0-M6  
**Java Version**: 17

