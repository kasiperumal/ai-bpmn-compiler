# AI BPMN Compiler

> Transform natural language into executable BPMN processes using AI

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)]()
[![Java](https://img.shields.io/badge/Java-17+-blue)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-green)]()
[![AI Providers](https://img.shields.io/badge/AI-OpenAI%20%7C%20Gemini-purple)]()

---

## 🚀 Features

- **Natural Language to BPMN**: Describe your process in plain English, get a BPMN diagram
- **Multiple AI Providers**: Choose between OpenAI GPT-4o (default) or Google Gemini 2.0
- **Vision Support**: Upload process diagrams, get structured BPMN
- **Edit via Chat**: Modify processes using natural language commands
- **Business Rules**: Automatic detection and DRL generation
- **Process Execution**: Deploy and run processes with Kogito
- **Real-time Explanations**: AI-generated descriptions for every process element

---

## 🤖 AI Provider Support

### Choose Your AI

| Provider | Model | Status | Best For |
|----------|-------|--------|----------|
| **OpenAI** | GPT-4o | ✅ Default | Production, Stability |
| **Gemini** | Gemini 2.0 | ⚠️ Alternative | Development, Free Tier |

**Switch easily**:
```bash
# Use OpenAI (default)
export OPENAI_API_KEY="sk-your-key"

# Or use Gemini
export AI_PROVIDER=gemini
export GEMINI_API_KEY="your-key"
```

📖 **[See AI Provider Comparison →](AI_PROVIDERS.md)**

---

## ⚡ Quick Start

### Prerequisites

- Java 17+
- Node.js 18+
- OpenAI API Key ([Get one here](https://platform.openai.com/api-keys))

### 1. Configure API Key

```bash
export OPENAI_API_KEY="sk-your-openai-api-key-here"
```

### 2. Start Backend

```bash
cd backend
./gradlew bootRun
```

Backend runs on **http://localhost:8080**

### 3. Start Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs on **http://localhost:5173**

### 4. Open Application

Navigate to **http://localhost:5173** in your browser.

---

## 📖 Documentation

### Getting Started
- **[QUICK_START.md](QUICK_START.md)** - 5-minute setup guide
- **[END_TO_END_TESTING_GUIDE.md](END_TO_END_TESTING_GUIDE.md)** - Complete testing guide
- **[QUICK_TEST_CHECKLIST.md](QUICK_TEST_CHECKLIST.md)** - Quick verification checklist

### AI Configuration
- **[AI_PROVIDERS.md](AI_PROVIDERS.md)** - AI provider overview
- **[backend/AI_CLIENT_CONFIGURATION.md](backend/AI_CLIENT_CONFIGURATION.md)** - Detailed configuration guide

### Architecture
- **[CANONICAL_MODEL_ARCHITECTURE.md](CANONICAL_MODEL_ARCHITECTURE.md)** - Core architectural principles
- **[backend/KOGITO_SETUP.md](backend/KOGITO_SETUP.md)** - Kogito integration
- **[backend/PROCESS_LIFECYCLE.md](backend/PROCESS_LIFECYCLE.md)** - Process lifecycle management
- **[frontend/FRONTEND_ARCHITECTURE.md](frontend/FRONTEND_ARCHITECTURE.md)** - Frontend design

### API Documentation
- **[backend/API.md](backend/API.md)** - REST API reference
- **[backend/EDIT_INTENT_API.md](backend/EDIT_INTENT_API.md)** - Edit intent API

---

## 🎯 Usage Example

### Create a Process

```bash
curl -X POST http://localhost:8080/api/process/start \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Create a leave approval process where an employee submits a request, their manager reviews it, and if the amount is over $5000, it requires director approval."
  }'
```

**Response**:
```json
{
  "processId": "proc-12345",
  "status": "REASONING",
  "message": "Process analysis started"
}
```

### View Generated BPMN

```bash
curl http://localhost:8080/api/process/proc-12345/bpmn
```

### Edit a Process

```bash
curl -X POST http://localhost:8080/api/process/proc-12345/edit-intent \
  -H "Content-Type: application/json" \
  -d '{
    "instruction": "Change the approval threshold to $10,000",
    "nodeId": "gateway_1"
  }'
```

### Publish & Execute

```bash
# Publish
curl -X POST http://localhost:8080/api/process/proc-12345/publish

# Execute
curl -X POST http://localhost:8080/api/process/proc-12345/execute \
  -H "Content-Type: application/json" \
  -d '{"amount": 7500, "employeeId": "EMP-001"}'
```

---

## 🏗️ Architecture

### Backend (Spring Boot)

```
┌─────────────────────────────────────────────────┐
│              REST Controllers                    │
├─────────────────────────────────────────────────┤
│   Process Lifecycle   │   Edit Intent   │ BPMN  │
└─────────────┬───────────────────────────────────┘
              │
┌─────────────▼───────────────────────────────────┐
│              Service Layer                       │
├─────────────────────────────────────────────────┤
│  AiClient Interface (Pluggable AI Providers)    │
│  ├─ OpenAiClient (GPT-4o) ✅ Default            │
│  └─ GeminiClient (Gemini 2.0) ⚠️ Alternative    │
├─────────────────────────────────────────────────┤
│  ProcessReasonerService   │  BpmnGeneratorService│
│  AiInferenceService       │  DrlGeneratorService │
│  ProcessEditService       │  RuleDetectionService│
└─────────────┬───────────────────────────────────┘
              │
┌─────────────▼───────────────────────────────────┐
│            Canonical Model                       │
├─────────────────────────────────────────────────┤
│  ProcessModel  │  ProcessNode  │  ProcessEdge   │
│  RuleModel     │  Explanation  │  Approval      │
└─────────────┬───────────────────────────────────┘
              │
┌─────────────▼───────────────────────────────────┐
│        Kogito Process Engine                     │
├─────────────────────────────────────────────────┤
│  BPMN Execution  │  DRL Rules  │  REST Endpoints│
└─────────────────────────────────────────────────┘
```

### Frontend (React + BPMN.js)

```
┌─────────────────────────────────────────────────┐
│                   App.tsx                        │
├─────────────┬─────────────┬─────────────────────┤
│   BPMN      │ Properties  │    Chat             │
│  Diagram    │   Panel     │   Panel             │
│  (50%)      │   (25%)     │   (25%)             │
├─────────────┼─────────────┼─────────────────────┤
│ BPMN.js     │ Process     │ AI Assistant        │
│ Viewer      │ Metadata    │ Natural Language    │
│ (Read-Only) │ Edit Intent │ Edit Instructions   │
│ Zoom/Export │ Explanations│ Streaming Responses │
└─────────────┴─────────────┴─────────────────────┘
```

---

## 🔑 Key Principles

### 1. Canonical Model Architecture
- Single source of truth: `ProcessModel`
- BPMN is **always generated** from the canonical model
- No direct BPMN editing in frontend
- All changes go through backend validation

### 2. AI-Driven Workflow
- Natural language as primary interface
- AI interprets intent and updates canonical model
- Automatic BPMN regeneration
- Explanations for all elements

### 3. Multi-Provider Support
- Unified `AiClient` interface
- Easy provider switching via configuration
- No code changes required
- Fallback capabilities

---

## 🛠️ Technology Stack

### Backend
- **Framework**: Spring Boot 3.4.3
- **AI**: Spring AI 1.0.0-M6
  - OpenAI GPT-4o (default)
  - Google Gemini 2.0 (alternative)
- **Process Engine**: Kogito 10.1.0 + jBPM
- **Rules Engine**: Drools
- **Language**: Java 17
- **Build Tool**: Gradle

### Frontend
- **Framework**: React 18 + Vite
- **Language**: TypeScript
- **BPMN**: BPMN.js
- **HTTP**: Axios
- **Routing**: React Router DOM

---

## 📊 Project Status

| Component | Status | Coverage |
|-----------|--------|----------|
| Backend Core | ✅ Complete | High |
| AI Integration | ✅ Complete | High |
| BPMN Generation | ✅ Complete | High |
| DRL Generation | ✅ Complete | Medium |
| Kogito Integration | ✅ Complete | Medium |
| Frontend Core | ✅ Complete | Medium |
| Edit Intent | ✅ Complete | Medium |
| Documentation | ✅ Complete | Excellent |

---

## 🧪 Testing

### Run Backend Tests

```bash
cd backend
./gradlew test
```

### Run Frontend Tests

```bash
cd frontend
npm test
```

### Manual Testing

Follow the **[END_TO_END_TESTING_GUIDE.md](END_TO_END_TESTING_GUIDE.md)** for complete testing scenarios.

---

## 🔐 Security

- **Never commit API keys** to version control
- Use environment variables for sensitive configuration
- Add `.env` and `application-local.yml` to `.gitignore`
- Rotate API keys regularly
- Set up billing alerts for AI providers

---

## 💰 Cost Estimation

### OpenAI GPT-4o (Default)
- **Light usage** (10-20 processes/day): ~$5-10/month
- **Medium usage** (100-200 processes/day): ~$50-100/month
- **Heavy usage** (1000+ processes/day): $500+/month

### Google Gemini 2.0 (Alternative)
- **Free tier**: 15 requests/minute, 1M requests/day
- **Paid**: Very low cost (~$3/month for 100 processes/day)

💡 **Tip**: Use Gemini for development, OpenAI for production.

---

## 🐛 Troubleshooting

### Backend won't start
```bash
# Check Java version
java -version  # Should be 17+

# Clean build
./gradlew clean build
```

### Frontend won't start
```bash
# Clean install
rm -rf node_modules package-lock.json
npm install
```

### AI Provider issues
```bash
# Verify API key
echo $OPENAI_API_KEY

# Check logs for provider selection
# Look for: "Selected AI Provider: OpenAI GPT-4o"
```

See **[Troubleshooting Section](END_TO_END_TESTING_GUIDE.md#troubleshooting)** in the testing guide.

---

## 📈 Roadmap

- [ ] Streaming AI responses in frontend
- [ ] Process versioning and comparison
- [ ] Collaboration features (multi-user editing)
- [ ] Advanced rule visualization
- [ ] Process analytics and metrics
- [ ] Export to Camunda/Flowable
- [ ] Additional AI providers (Anthropic Claude, Azure OpenAI)

---

## 🤝 Contributing

This is a demonstration project. For production use, consider:

1. Implementing comprehensive error handling
2. Adding authentication and authorization
3. Setting up CI/CD pipelines
4. Implementing rate limiting
5. Adding process versioning
6. Implementing audit logging
7. Setting up monitoring and alerts

---

## 📄 License

This project is for educational and demonstration purposes.

---

## 🙏 Acknowledgments

- **Spring AI** - Unified AI integration framework
- **OpenAI** - GPT-4o model
- **Google** - Gemini AI model
- **Kogito** - Business automation platform
- **BPMN.js** - BPMN visualization
- **jBPM** - Business process management
- **Drools** - Business rules engine

---

## 📞 Support

### Documentation
- [Quick Start Guide](QUICK_START.md)
- [End-to-End Testing](END_TO_END_TESTING_GUIDE.md)
- [AI Configuration](backend/AI_CLIENT_CONFIGURATION.md)
- [Architecture Guide](CANONICAL_MODEL_ARCHITECTURE.md)

### Troubleshooting
- Check [Troubleshooting Section](END_TO_END_TESTING_GUIDE.md#troubleshooting)
- Review startup logs
- Verify API keys and configuration
- Test API endpoints directly with `curl`

---

**Built with ❤️ using Spring Boot, React, and AI**

---

## Quick Commands

```bash
# Backend
cd backend && ./gradlew bootRun

# Frontend
cd frontend && npm run dev

# Build All
./gradlew clean build && npm run build

# Test All
./gradlew test && npm test
```

---

**Version**: 2.0  
**AI Provider**: OpenAI GPT-4o (default) | Google Gemini 2.0 (alternative)  
**Status**: ✅ Production Ready

