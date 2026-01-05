# Gemini Client Service

## Overview

The `GeminiClient` service provides integration with Google's Gemini 2.5 Pro AI model via Spring AI. It supports both text-only prompts and multimodal interactions (image + text).

**Key Features**:
- ✅ Text-only prompt support
- ✅ Image + text prompt support (multimodal)
- ✅ Multiple image format support (PNG, JPEG, GIF, WebP)
- ✅ Spring AI integration
- ✅ Configurable via `application.yml`
- ✅ Comprehensive error handling
- ✅ Full test coverage

---

## Configuration

### Dependencies

**build.gradle**:
```groovy
dependencies {
    implementation 'org.springframework.ai:spring-ai-vertex-ai-gemini-spring-boot-starter:1.0.0-M6'
}

repositories {
    mavenCentral()
    maven { url 'https://repo.spring.io/milestone' }
}
```

### Application Configuration

**application.yml**:
```yaml
spring:
  ai:
    vertex:
      ai:
        gemini:
          project-id: ${VERTEX_AI_PROJECT_ID:your-project-id}
          location: ${VERTEX_AI_LOCATION:us-central1}
          api-key: ${GEMINI_API_KEY:your-api-key-here}
          chat:
            options:
              model: gemini-2.0-flash-exp
              temperature: 0.7
              maxOutputTokens: 4096
```

### Environment Variables

Set these environment variables for production:

```bash
export VERTEX_AI_PROJECT_ID="your-gcp-project-id"
export VERTEX_AI_LOCATION="us-central1"
export GEMINI_API_KEY="your-gemini-api-key"
```

Or use a `.env` file:
```properties
VERTEX_AI_PROJECT_ID=your-gcp-project-id
VERTEX_AI_LOCATION=us-central1
GEMINI_API_KEY=your-gemini-api-key
```

---

## API Reference

### Text-Only Generation

#### `generateFromText(String prompt)`

Send a text-only prompt to Gemini and receive a response.

**Parameters**:
- `prompt` (String, required): The text prompt to send to Gemini

**Returns**: `String` - Raw text response from Gemini

**Throws**:
- `IllegalArgumentException` - If prompt is null or empty
- `RuntimeException` - If API call fails

**Example**:
```java
@Autowired
private GeminiClient geminiClient;

public void analyzeProcess() {
    String prompt = "Explain the key components of a BPMN approval workflow.";
    String response = geminiClient.generateFromText(prompt);
    
    System.out.println("Gemini response: " + response);
}
```

---

### Image + Text Generation

#### `generateFromImage(Path imagePath, String prompt)`

Send an image file along with a text prompt to Gemini.

**Parameters**:
- `imagePath` (Path, required): Path to the image file
- `prompt` (String, required): The text prompt to send along with the image

**Returns**: `String` - Raw text response from Gemini

**Throws**:
- `IllegalArgumentException` - If imagePath is null, file doesn't exist, or prompt is null/empty
- `RuntimeException` - If image cannot be read or API call fails

**Supported Image Formats**:
- PNG (`.png`)
- JPEG (`.jpg`, `.jpeg`)
- GIF (`.gif`)
- WebP (`.webp`)

**Example**:
```java
@Autowired
private GeminiClient geminiClient;

public void analyzeDiagram() {
    Path imagePath = Paths.get("./data/uploads/proc-123/original.png");
    String prompt = "Analyze this BPMN diagram and describe the workflow.";
    
    String response = geminiClient.generateFromImage(imagePath, prompt);
    
    System.out.println("Analysis: " + response);
}
```

---

#### `generateFromImage(Resource imageResource, String prompt)`

Send an image (as Spring Resource) along with a text prompt to Gemini.

**Parameters**:
- `imageResource` (Resource, required): Spring Resource containing the image
- `prompt` (String, required): The text prompt to send along with the image

**Returns**: `String` - Raw text response from Gemini

**Throws**:
- `IllegalArgumentException` - If imageResource is null, doesn't exist, or prompt is null/empty
- `RuntimeException` - If image cannot be read or API call fails

**Example**:
```java
@Autowired
private GeminiClient geminiClient;

public void analyzeUploadedFile(MultipartFile file) throws IOException {
    Resource resource = file.getResource();
    String prompt = "What BPMN elements are shown in this diagram?";
    
    String response = geminiClient.generateFromImage(resource, prompt);
    
    return response;
}
```

---

## Usage Examples

### Basic Text Generation

```java
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class ProcessAnalysisService {
    
    @Autowired
    private GeminiClient geminiClient;
    
    public String analyzeProcessDescription(String description) {
        String prompt = String.format(
            "Analyze this process description and identify the main steps:\n\n%s", 
            description
        );
        
        return geminiClient.generateFromText(prompt);
    }
}
```

---

### Image Analysis

```java
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class DiagramAnalysisService {
    
    @Autowired
    private GeminiClient geminiClient;
    
    public String extractBpmnElements(String processId) {
        Path imagePath = Paths.get("./data/uploads/" + processId + "/original.png");
        
        String prompt = """
            Analyze this BPMN diagram and extract:
            1. All tasks (activities)
            2. All gateways (decision points)
            3. Start and end events
            4. Sequence flows between elements
            
            Format the response as JSON.
            """;
        
        return geminiClient.generateFromImage(imagePath, prompt);
    }
}
```

---

### Processing Uploaded Images

```java
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageProcessingService {
    
    @Autowired
    private GeminiClient geminiClient;
    
    public String processUploadedDiagram(MultipartFile file) {
        Resource resource = file.getResource();
        
        String prompt = """
            This is a BPMN process diagram. Please:
            1. Describe the overall workflow
            2. Identify any potential issues or improvements
            3. Suggest optimizations
            """;
        
        return geminiClient.generateFromImage(resource, prompt);
    }
}
```

---

### Structured Output Example

```java
@Service
public class BpmnExtractionService {
    
    @Autowired
    private GeminiClient geminiClient;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    public ProcessModel extractProcessModel(Path diagramPath) {
        String prompt = """
            Analyze this BPMN diagram and return a JSON object with this structure:
            {
              "name": "process name",
              "nodes": [
                {"id": "node1", "type": "TASK", "name": "Task Name"},
                {"id": "node2", "type": "GATEWAY", "name": "Gateway Name"}
              ],
              "edges": [
                {"fromNodeId": "node1", "toNodeId": "node2", "condition": ""}
              ]
            }
            
            Only return valid JSON, no additional text.
            """;
        
        String jsonResponse = geminiClient.generateFromImage(diagramPath, prompt);
        
        // Parse JSON response into ProcessModel
        return objectMapper.readValue(jsonResponse, ProcessModel.class);
    }
}
```

---

## Error Handling

### Common Exceptions

```java
try {
    String response = geminiClient.generateFromText(prompt);
    // Process response
    
} catch (IllegalArgumentException e) {
    // Invalid input (null/empty prompt)
    log.error("Invalid prompt: {}", e.getMessage());
    
} catch (RuntimeException e) {
    // API error (rate limit, authentication, etc.)
    log.error("Gemini API error: {}", e.getMessage());
    
    // Could retry with exponential backoff
    // or return cached/default response
}
```

### Best Practices

1. **Validate inputs before calling**:
   ```java
   if (prompt == null || prompt.trim().isEmpty()) {
       throw new IllegalArgumentException("Prompt is required");
   }
   
   if (imagePath == null || !Files.exists(imagePath)) {
       throw new IllegalArgumentException("Image file not found");
   }
   ```

2. **Implement retry logic**:
   ```java
   public String generateWithRetry(String prompt, int maxRetries) {
       for (int i = 0; i < maxRetries; i++) {
           try {
               return geminiClient.generateFromText(prompt);
           } catch (RuntimeException e) {
               if (i == maxRetries - 1) throw e;
               
               // Exponential backoff
               Thread.sleep((long) Math.pow(2, i) * 1000);
           }
       }
       throw new RuntimeException("Max retries exceeded");
   }
   ```

3. **Handle large responses**:
   ```java
   String response = geminiClient.generateFromText(prompt);
   
   if (response.length() > MAX_RESPONSE_LENGTH) {
       log.warn("Response truncated (original length: {})", response.length());
       response = response.substring(0, MAX_RESPONSE_LENGTH);
   }
   ```

---

## Testing

### Unit Tests

The service includes comprehensive unit tests:

```java
@Test
void testGenerateFromText_Success() {
    // Mock ChatModel
    ChatModel chatModel = mock(ChatModel.class);
    GeminiClient client = new GeminiClient(chatModel);
    
    // Setup mock response
    ChatResponse mockResponse = createMockChatResponse("Test response");
    when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);
    
    // Test
    String response = client.generateFromText("Test prompt");
    
    // Verify
    assertEquals("Test response", response);
}
```

**Run tests**:
```bash
./gradlew test --tests GeminiClientTest
```

---

### Integration Testing

For integration tests with actual Gemini API:

```java
@SpringBootTest
@TestPropertySource(properties = {
    "spring.ai.vertex.ai.gemini.api-key=${GEMINI_API_KEY}"
})
class GeminiClientIntegrationTest {
    
    @Autowired
    private GeminiClient geminiClient;
    
    @Test
    @Disabled("Requires valid API key")
    void testRealApiCall() {
        String response = geminiClient.generateFromText(
            "What is BPMN?"
        );
        
        assertNotNull(response);
        assertTrue(response.length() > 0);
    }
}
```

---

## Performance Considerations

### Response Times

- **Text-only**: ~1-3 seconds
- **Image + text**: ~3-10 seconds (depending on image size)

### Rate Limits

Check Google Vertex AI quotas:
- Free tier: Limited requests per minute
- Paid tier: Higher limits

### Optimization Tips

1. **Batch requests** when possible
2. **Cache responses** for identical prompts
3. **Optimize images** before sending (resize, compress)
4. **Use async processing** for long-running operations

---

## Supported Image Formats

| Format | Extension | MIME Type | Notes |
|--------|-----------|-----------|-------|
| PNG | `.png` | `image/png` | ✅ Recommended |
| JPEG | `.jpg`, `.jpeg` | `image/jpeg` | ✅ Recommended |
| GIF | `.gif` | `image/gif` | ✅ Supported |
| WebP | `.webp` | `image/webp` | ✅ Supported |
| Others | - | - | Defaults to JPEG MIME type |

**Maximum Image Size**: Check Vertex AI limits (typically 10-20MB)

---

## Troubleshooting

### Problem: "Failed to generate response from Gemini"

**Possible Causes**:
- Invalid API key
- Network connectivity issues
- Rate limit exceeded
- Invalid request format

**Solution**:
1. Verify API key is set correctly
2. Check network connection
3. Implement retry logic with backoff
4. Review request parameters

---

### Problem: "Image path is null or file does not exist"

**Solution**:
```java
Path imagePath = Paths.get("./data/uploads/proc-123/original.png");

if (!Files.exists(imagePath)) {
    log.error("Image file not found: {}", imagePath);
    throw new FileNotFoundException("Image not found: " + imagePath);
}
```

---

### Problem: Empty or unexpected responses

**Solution**:
1. **Check prompt clarity**:
   ```java
   // Bad prompt
   "analyze"
   
   // Good prompt
   "Analyze this BPMN diagram and list all task names"
   ```

2. **Validate response**:
   ```java
   String response = geminiClient.generateFromText(prompt);
   
   if (response == null || response.trim().isEmpty()) {
       throw new RuntimeException("Empty response from Gemini");
   }
   ```

---

## Advanced Usage

### Custom Temperature

Adjust in `application.yml`:
```yaml
spring:
  ai:
    vertex:
      ai:
        gemini:
          chat:
            options:
              temperature: 0.2  # More deterministic
              # or
              temperature: 0.9  # More creative
```

### Token Limits

```yaml
spring:
  ai:
    vertex:
      ai:
        gemini:
          chat:
            options:
              maxOutputTokens: 8192  # Longer responses
```

### Model Selection

```yaml
spring:
  ai:
    vertex:
      ai:
        gemini:
          chat:
            options:
              model: gemini-2.0-flash-exp  # Fast, efficient
              # or
              model: gemini-2.5-pro          # More capable
```

---

## Security Considerations

### API Key Management

1. **Never commit API keys** to version control
2. **Use environment variables** or secret management
3. **Rotate keys regularly**
4. **Use different keys** for dev/prod

### Input Validation

```java
public String generateSafely(String userInput) {
    // Sanitize input
    if (userInput.length() > MAX_PROMPT_LENGTH) {
        throw new IllegalArgumentException("Prompt too long");
    }
    
    // Remove potential injection attempts
    String sanitized = sanitizeInput(userInput);
    
    return geminiClient.generateFromText(sanitized);
}
```

---

## Summary

The `GeminiClient` provides:

✅ **Simple API** for text and image prompts  
✅ **Spring AI integration** with auto-configuration  
✅ **Multiple image formats** support  
✅ **Comprehensive error handling**  
✅ **Full test coverage**  
✅ **Production-ready** configuration  

**Status**: ✅ **READY FOR USE**

---

## See Also

- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [Google Vertex AI Gemini API](https://cloud.google.com/vertex-ai/docs/generative-ai/model-reference/gemini)
- [AI Orchestrator Documentation](AI_ORCHESTRATOR.md)
- [API Documentation](API.md)

---

**Implementation Date**: January 2, 2026  
**Spring AI Version**: 1.0.0-M6  
**Gemini Model**: 2.0-flash-exp (configurable)  
**Java Version**: 17

