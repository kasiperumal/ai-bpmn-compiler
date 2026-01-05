# AI Provider Support

## Quick Overview

The AI BPMN Compiler now supports **multiple AI providers** with easy switching via configuration.

### Supported Providers

| Provider | Model | Status | Vision | Cost |
|----------|-------|--------|--------|------|
| **OpenAI** | GPT-4o | ✅ Default | ✅ Yes | 💰 Pay-per-use |
| **Google Gemini** | Gemini 2.0 Flash | ✅ Alternative | ✅ Yes | 💰 Free tier |

---

## Quick Start

### Using OpenAI (Default)

```bash
# 1. Get API key from: https://platform.openai.com/api-keys
export OPENAI_API_KEY="sk-your-key-here"

# 2. Start application
cd backend && ./gradlew bootRun

# That's it! OpenAI is the default provider
```

### Using Gemini (Alternative)

```bash
# 1. Get API key from: https://makersuite.google.com/app/apikey
export AI_PROVIDER=gemini
export GEMINI_API_KEY="your-key-here"
export VERTEX_AI_PROJECT_ID="your-project-id"
export VERTEX_AI_LOCATION="us-central1"

# 2. Start application
cd backend && ./gradlew bootRun

# Application will use Gemini
```

---

## Switching Providers

### At Runtime (Environment Variable)

```bash
# Use OpenAI
export AI_PROVIDER=openai
./gradlew bootRun

# Use Gemini
export AI_PROVIDER=gemini
./gradlew bootRun
```

### In Configuration (application.yml)

```yaml
app:
  ai:
    provider: openai  # or "gemini"
```

---

## Verification

When the application starts, check logs:

```
╔═══════════════════════════════════════════════════════╗
║   AI Provider: OpenAI GPT-4o (Default)                ║
║   To switch: app.ai.provider=gemini                    ║
╚═══════════════════════════════════════════════════════╝

ProcessReasonerService initialized with AI provider: OpenAI GPT-4o
AiInferenceService initialized with AI provider: OpenAI GPT-4o
ProcessEditService initialized with AI provider: OpenAI GPT-4o
```

---

## Which Provider Should I Use?

### Use OpenAI if:
- ✅ You want production-ready stability
- ✅ You need consistent JSON output
- ✅ You prefer established enterprise support
- ✅ Budget allows pay-per-use pricing

### Use Gemini if:
- ✅ You want free tier for testing
- ✅ You need longer context windows (1M tokens)
- ✅ You're already in Google Cloud ecosystem
- ✅ You want faster experimental features

### Our Recommendation:
- **Development/Testing**: Gemini (free tier)
- **Production**: OpenAI GPT-4o (stability)

---

## Features Supported

All features work with **both providers**:

- ✅ Natural language to BPMN conversion
- ✅ Process diagram image analysis
- ✅ Edit intent interpretation
- ✅ Node explanations
- ✅ Rule detection
- ✅ Clarification questions
- ✅ DRL generation

No code changes needed when switching!

---

## Cost Comparison

### OpenAI GPT-4o
- **Input**: $2.50 per 1M tokens
- **Output**: $10.00 per 1M tokens
- **Typical process**: ~5,000 tokens = $0.06
- **100 processes/day**: ~$180/month

### Google Gemini 2.0 Flash
- **Free tier**: 15 requests/minute, 1M requests/day
- **Paid**: $0.075 per 1M tokens (input)
- **Typical process**: ~5,000 tokens = $0.001
- **100 processes/day**: ~$3/month or FREE

💡 **Tip**: Use Gemini for development, OpenAI for production.

---

## Documentation

For detailed configuration, see:

📖 **[backend/AI_CLIENT_CONFIGURATION.md](backend/AI_CLIENT_CONFIGURATION.md)** - Complete guide
- Getting API keys
- Advanced configuration
- Troubleshooting
- Best practices

📖 **[END_TO_END_TESTING_GUIDE.md](END_TO_END_TESTING_GUIDE.md)** - Testing guide

📖 **[QUICK_START.md](QUICK_START.md)** - Quick setup

---

## Architecture

### Unified Interface

```java
public interface AiClient {
    String generateFromText(String prompt);
    String generateFromImage(Path imagePath, String prompt);
    String generateFromImage(Resource imageResource, String prompt);
    String getProviderName();
}
```

### Implementations

- `OpenAiClient` - Spring AI OpenAI integration
- `GeminiClient` - Spring AI Vertex AI Gemini integration

### Configuration

- `AiClientConfiguration` - Provider selection based on `app.ai.provider`

---

## Troubleshooting

### OpenAI: 401 Unauthorized
```bash
# Check API key
echo $OPENAI_API_KEY  # Should start with sk-

# Verify it's valid at: https://platform.openai.com/api-keys
```

### Gemini: 403 Forbidden
```bash
# Check Vertex AI is enabled
# Visit: https://console.cloud.google.com/apis/library/aiplatform.googleapis.com

# Verify project ID
echo $VERTEX_AI_PROJECT_ID
```

### Provider Not Switching
```bash
# Explicitly set provider
export AI_PROVIDER=gemini

# Restart application (environment variables only load on startup)
./gradlew bootRun
```

---

## Version History

- **v2.0** (Current) - Added OpenAI GPT-4o support, made it default
- **v1.0** - Gemini-only implementation

---

## FAQ

**Q: Can I use both providers at once?**  
A: One is active at a time, but you can inject both using `@Qualifier` if needed.

**Q: Will my existing code break?**  
A: No! All services use the `AiClient` interface. Switching is seamless.

**Q: How do I check which provider is active?**  
A: Check startup logs or call `aiClient.getProviderName()` in your code.

**Q: Can I use GPT-3.5 instead of GPT-4o?**  
A: Yes, change `model: gpt-3.5-turbo` in `application.yml`. Note: No vision support.

---

## Support

For issues or questions:
1. Check [backend/AI_CLIENT_CONFIGURATION.md](backend/AI_CLIENT_CONFIGURATION.md)
2. Review startup logs
3. Test with `curl` to verify API keys
4. Check rate limits and quotas

---

**Default**: OpenAI GPT-4o 🚀  
**Alternative**: Google Gemini 2.0 ⚡  
**Switch**: `export AI_PROVIDER=gemini`

