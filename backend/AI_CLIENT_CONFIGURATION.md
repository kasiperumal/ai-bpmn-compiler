# AI Client Configuration Guide

## Overview

The AI BPMN Compiler supports multiple AI providers through a unified `AiClient` interface. You can switch between AI providers using a simple configuration flag.

## Supported AI Providers

### 1. OpenAI GPT-4o (Default)
- **Model**: gpt-4o
- **Capabilities**: 
  - Text generation
  - Vision (image analysis)
  - Streaming responses
  - JSON mode
- **Best for**: Production deployments, consistent performance

### 2. Google Gemini 2.0 (Alternative)
- **Model**: gemini-2.0-flash-exp
- **Capabilities**:
  - Text generation
  - Multimodal (image + text)
  - Fast responses
- **Best for**: Experimentation, Google Cloud integration

---

## Configuration

### Environment Variables

#### OpenAI (Default)
```bash
export OPENAI_API_KEY="sk-your-openai-api-key-here"
```

#### Gemini (Alternative)
```bash
export GEMINI_API_KEY="your-gemini-api-key-here"
export VERTEX_AI_PROJECT_ID="your-gcp-project-id"
export VERTEX_AI_LOCATION="us-central1"
```

### Application Configuration

The AI provider is configured in `application.yml`:

```yaml
app:
  ai:
    # AI Provider Selection: "openai" (default) or "gemini"
    provider: ${AI_PROVIDER:openai}

spring:
  ai:
    # OpenAI Configuration (Default)
    openai:
      api-key: ${OPENAI_API_KEY:your-openai-api-key-here}
      chat:
        options:
          model: gpt-4o
          temperature: 0.7
          max-tokens: 4096
    
    # Vertex AI Gemini Configuration (Alternative)
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

---

## Switching Between Providers

### Method 1: Environment Variable (Recommended)

```bash
# Use OpenAI (default)
export AI_PROVIDER=openai
export OPENAI_API_KEY="sk-your-key"

# Or use Gemini
export AI_PROVIDER=gemini
export GEMINI_API_KEY="your-key"
export VERTEX_AI_PROJECT_ID="your-project"
export VERTEX_AI_LOCATION="us-central1"

# Then start the application
./gradlew bootRun
```

### Method 2: Application Properties

Edit `application.yml`:

```yaml
app:
  ai:
    provider: gemini  # Change from "openai" to "gemini"
```

### Method 3: Command Line

```bash
./gradlew bootRun --args='--app.ai.provider=gemini'
```

---

## Getting API Keys

### OpenAI API Key

1. Visit [OpenAI Platform](https://platform.openai.com/api-keys)
2. Sign in or create an account
3. Navigate to "API Keys"
4. Click "Create new secret key"
5. Copy the key (starts with `sk-`)

**Pricing**: Pay-as-you-go based on usage
- GPT-4o: ~$5-15 per million tokens
- [Current pricing](https://openai.com/pricing)

### Google Gemini API Key

#### Option A: Google AI Studio (Quickest)
1. Visit [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Click "Create API Key"
3. Copy the generated key

#### Option B: Google Cloud Console
1. Visit [Google Cloud Console](https://console.cloud.google.com)
2. Create or select a project
3. Enable "Vertex AI API" or "Generative Language API"
4. Create credentials (API Key)
5. Copy the API key and project details

**Pricing**: 
- Gemini 2.0 Flash: Free tier available
- [Current pricing](https://ai.google.dev/pricing)

---

## Architecture

### AiClient Interface

All AI providers implement the `AiClient` interface:

```java
public interface AiClient {
    String generateFromText(String prompt);
    String generateFromImage(Path imagePath, String prompt);
    String generateFromImage(Resource imageResource, String prompt);
    String getProviderName();
}
```

### Implementations

1. **OpenAiClient** (`@Service("openAiClient")`)
   - Uses Spring AI OpenAI starter
   - GPT-4o model
   - Supports vision capabilities

2. **GeminiClient** (`@Service("geminiClient")`)
   - Uses Spring AI Vertex AI Gemini starter
   - Gemini 2.0 model
   - Multimodal support

### Configuration Bean

`AiClientConfiguration` provides a `@Primary` bean that selects the appropriate client:

```java
@Bean
@Primary
public AiClient primaryAiClient(
        @Qualifier("openAiClient") AiClient openAiClient,
        @Qualifier("geminiClient") AiClient geminiClient) {
    
    if ("gemini".equalsIgnoreCase(aiProvider)) {
        return geminiClient;
    } else {
        return openAiClient; // Default
    }
}
```

### Service Integration

All services that use AI inject the `AiClient` interface:

```java
@Service
public class ProcessReasonerService {
    private final AiClient aiClient;
    
    public ProcessReasonerService(AiClient aiClient) {
        this.aiClient = aiClient;
        logger.info("Using AI provider: {}", aiClient.getProviderName());
    }
}
```

Services using `AiClient`:
- `ProcessReasonerService` - Process reasoning and structuring
- `AiInferenceService` - Image analysis and inference
- `ProcessEditService` - Natural language edit intent interpretation

---

## Verification

### Check Active Provider on Startup

When the application starts, look for log messages:

```
╔═══════════════════════════════════════════════════════╗
║   AI Provider: OpenAI GPT-4o (Default)                ║
║   To switch: app.ai.provider=gemini                    ║
╚═══════════════════════════════════════════════════════╝

Selected AI Provider: OpenAI GPT-4o

ProcessReasonerService initialized with AI provider: OpenAI GPT-4o
AiInferenceService initialized with AI provider: OpenAI GPT-4o
ProcessEditService initialized with AI provider: OpenAI GPT-4o
```

Or if using Gemini:

```
╔═══════════════════════════════════════════════════════╗
║   AI Provider: Google Gemini 2.0                       ║
║   Configure via: app.ai.provider=gemini                ║
╚═══════════════════════════════════════════════════════╝

Selected AI Provider: Google Gemini 2.0
```

### Test API Endpoints

```bash
# Start a process (will use configured AI provider)
curl -X POST http://localhost:8080/api/process/start \
  -H "Content-Type: application/json" \
  -d '{"description": "Create a simple approval process"}'

# Check logs for AI provider being used
```

---

## Comparison

| Feature | OpenAI GPT-4o | Google Gemini 2.0 |
|---------|---------------|-------------------|
| **Default** | ✅ Yes | ❌ No |
| **Text Generation** | ✅ Excellent | ✅ Excellent |
| **Vision/Image** | ✅ Yes | ✅ Yes |
| **Streaming** | ✅ Yes | ✅ Yes |
| **JSON Mode** | ✅ Yes | ⚠️ Limited |
| **Stability** | ✅ Production-ready | ⚠️ Experimental |
| **Pricing** | 💰 Pay-per-use | 💰 Free tier available |
| **Latency** | ⚡ ~1-3s | ⚡ ~1-2s |
| **Context Window** | 128K tokens | 1M tokens |
| **Rate Limits** | Standard | Generous |

---

## Troubleshooting

### OpenAI Issues

#### Error: 401 Unauthorized
```
Failed to generate response from OpenAI: 401 Unauthorized
```

**Solution**: 
- Verify API key is correct
- Check key starts with `sk-`
- Ensure key has sufficient credits

#### Error: 429 Rate Limit
```
Failed to generate response from OpenAI: 429 Too Many Requests
```

**Solution**:
- Implement exponential backoff
- Upgrade OpenAI plan
- Switch to Gemini temporarily

### Gemini Issues

#### Error: 403 Forbidden
```
Failed to generate response from Gemini: 403 Forbidden
```

**Solution**:
- Verify Vertex AI API is enabled in GCP
- Check project ID is correct
- Ensure API key has permissions

#### Error: Project Not Found
```
Failed to generate response from Gemini: Project not found
```

**Solution**:
- Verify `VERTEX_AI_PROJECT_ID` is correct
- Check you have access to the project
- Ensure project is active in GCP

---

## Best Practices

### 1. Use Environment Variables

Never hardcode API keys in `application.yml`:

```yaml
# ❌ BAD
spring:
  ai:
    openai:
      api-key: sk-actual-api-key-here

# ✅ GOOD
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
```

### 2. Add to .gitignore

```gitignore
application-local.yml
.env
.env.local
```

### 3. Use Different Providers for Dev/Prod

```bash
# Development (use Gemini free tier)
export AI_PROVIDER=gemini

# Production (use OpenAI for stability)
export AI_PROVIDER=openai
```

### 4. Monitor Costs

- Set up billing alerts in OpenAI/GCP
- Track token usage in logs
- Use appropriate model temperature (0.7 is balanced)

### 5. Implement Fallbacks

Consider implementing a fallback mechanism:

```java
try {
    return primaryAiClient.generateFromText(prompt);
} catch (Exception e) {
    logger.warn("Primary AI provider failed, trying fallback...");
    return fallbackAiClient.generateFromText(prompt);
}
```

---

## Advanced Configuration

### Custom Model Parameters

#### OpenAI

```yaml
spring:
  ai:
    openai:
      chat:
        options:
          model: gpt-4o
          temperature: 0.7        # 0.0-2.0 (higher = more creative)
          max-tokens: 4096        # Max response length
          top-p: 1.0              # Nucleus sampling
          frequency-penalty: 0.0  # Reduce repetition
          presence-penalty: 0.0   # Encourage topic diversity
```

#### Gemini

```yaml
spring:
  ai:
    vertex:
      ai:
        gemini:
          chat:
            options:
              model: gemini-2.0-flash-exp
              temperature: 0.7
              maxOutputTokens: 4096
              topK: 40
              topP: 0.95
```

### Using Both Providers Simultaneously

You can inject both clients directly if needed:

```java
@Service
public class MyService {
    private final OpenAiClient openAiClient;
    private final GeminiClient geminiClient;
    
    public MyService(
            @Qualifier("openAiClient") AiClient openAiClient,
            @Qualifier("geminiClient") AiClient geminiClient) {
        this.openAiClient = openAiClient;
        this.geminiClient = geminiClient;
    }
    
    public String generateWithConsensus(String prompt) {
        String response1 = openAiClient.generateFromText(prompt);
        String response2 = geminiClient.generateFromText(prompt);
        return combineResponses(response1, response2);
    }
}
```

---

## Migration Guide

### From Gemini-Only to Configurable

If you're upgrading from an older version that only supported Gemini:

1. **Update dependencies** (already done in `build.gradle`)
2. **Add OpenAI configuration** to `application.yml`
3. **Set environment variable**: `export OPENAI_API_KEY="your-key"`
4. **Restart application**
5. **Verify** logs show OpenAI as active provider

No code changes needed! All services automatically use the configured provider.

---

## Support Matrix

| Spring AI Version | OpenAI Client | Gemini Client |
|-------------------|---------------|---------------|
| 1.0.0-M6 | ✅ gpt-4o | ✅ gemini-2.0-flash-exp |
| 1.0.0-M5 | ✅ gpt-4-turbo | ✅ gemini-1.5-pro |
| 1.0.0-M4 | ⚠️ Limited | ✅ gemini-1.0-pro |

---

## FAQ

### Q: Can I use GPT-3.5 instead of GPT-4o?

Yes, change the model in `application.yml`:

```yaml
spring:
  ai:
    openai:
      chat:
        options:
          model: gpt-3.5-turbo
```

Note: GPT-3.5 does not support vision, so image analysis will fail.

### Q: How much will this cost?

Costs depend on usage:
- **Light usage** (10-20 processes/day): ~$5-10/month
- **Medium usage** (100-200 processes/day): ~$50-100/month
- **Heavy usage** (1000+ processes/day): $500+/month

Use Gemini's free tier for development/testing.

### Q: Can I use both providers in the same application?

Yes, but only one is active as the primary provider. You can inject both using qualifiers if needed for specialized use cases.

### Q: Which provider is better for BPMN generation?

Both work well. OpenAI GPT-4o is more stable and consistent, while Gemini 2.0 is faster and has a longer context window. We recommend OpenAI for production.

---

## Related Documentation

- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [OpenAI API Docs](https://platform.openai.com/docs)
- [Google Gemini API Docs](https://ai.google.dev/docs)
- [END_TO_END_TESTING_GUIDE.md](../END_TO_END_TESTING_GUIDE.md)

---

## Version History

- **v2.0** - Added OpenAI support with GPT-4o as default (current)
- **v1.0** - Gemini-only implementation

