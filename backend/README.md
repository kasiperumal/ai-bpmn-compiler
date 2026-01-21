# GenAI-Powered BPMN Builder - Backend

Spring Boot application with AI-driven BPMN generation, image processing, and business rules management.

---

## 📋 Overview

**GenAI-powered backend** that converts natural language and images into executable BPMN processes with integrated business rules.

**Key Capabilities:**
- ✅ **Text-to-BPMN** via GPT-4o
- ✅ **Image-to-BPMN** via GPT-4o Vision
- ✅ **Rule Generation** (Natural language → Drools DRL)
- ✅ **BPMN Validation** (BPMN 2.0 compliance)
- ✅ **H2 In-Memory Database** (JPA/Hibernate)
- ✅ **ELK.js-ready Output** (layout-free BPMN JSON)

---

## 🛠️ Technology Stack

### **Core Framework**
- **Spring Boot**: 3.4.3
- **Java**: 17
- **Build Tool**: Gradle
- **Port**: 8080

### **AI Integration**
- **Spring AI**: 1.0.0-M6
- **OpenAI GPT-4o**: Text generation
- **OpenAI GPT-4o Vision**: Image analysis

### **Database**
- **H2 Database**: In-memory (embedded)
- **JPA/Hibernate**: ORM
- **Spring Data JPA**: Repository abstraction

### **Business Rules**
- **Drools**: 8.x
  - `drools-core` - Rule engine
  - `drools-compiler` - DRL compilation
  - `drools-mvel` - Expression language

### **Process Engine**
- **Kogito**: 10.1.0
- **jBPM**: Spring Boot integration

---

## 📁 Project Structure

```
backend/
├── build.gradle                      # Gradle build config
├── settings.gradle                   # Project settings
├── src/
│   ├── main/
│   │   ├── java/com/example/aibpmn/
│   │   │   ├── AiBpmnCompilerApplication.java
│   │   │   ├── config/
│   │   │   │   ├── AiClientConfiguration.java      # AI provider config
│   │   │   │   └── WebConfig.java                  # CORS config
│   │   │   ├── controller/
│   │   │   │   ├── ProcessTextController.java      # Text-to-BPMN
│   │   │   │   ├── ProcessImageController.java     # Image-to-BPMN
│   │   │   │   ├── ProcessLifecycleController.java # Process management
│   │   │   │   └── RuleController.java             # Rule management
│   │   │   ├── service/
│   │   │   │   ├── AiClient.java                   # AI interface
│   │   │   │   ├── OpenAiClient.java               # GPT-4o implementation
│   │   │   │   ├── GeminiClient.java               # Gemini implementation
│   │   │   │   ├── ProcessReasonerService.java     # Text → BPMN Moddle JSON
│   │   │   │   ├── ProcessTextService.java         # Text processing
│   │   │   │   ├── ProcessImageService.java        # Image processing
│   │   │   │   ├── DroolsRuleService.java          # Rule generation
│   │   │   │   ├── BpmnValidationService.java      # BPMN validator
│   │   │   │   └── BpmnGeneratorService.java       # BPMN XML generator
│   │   │   ├── model/
│   │   │   │   ├── ProcessModel.java               # Hybrid storage entity
│   │   │   │   ├── BpmnMetadata.java               # Metadata embeddable
│   │   │   │   ├── RuleSet.java                    # Rule entity
│   │   │   │   ├── RuleStatus.java                 # Rule lifecycle enum
│   │   │   │   └── ProcessStatus.java              # Process lifecycle enum
│   │   │   ├── repository/
│   │   │   │   ├── ProcessModelRepository.java     # JPA repository
│   │   │   │   └── RuleSetRepository.java          # Rule repository
│   │   │   └── dto/
│   │   │       ├── ProcessTextRequest.java
│   │   │       ├── ProcessTextResponse.java
│   │   │       ├── ReasoningResult.java
│   │   │       └── ValidationResult.java
│   │   └── resources/
│   │       ├── application.yml                      # Main config
│   │       └── application-test.yml                 # Test config
│   └── test/
│       └── java/com/example/aibpmn/
│           ├── AiBpmnCompilerApplicationTests.java
│           └── config/
│               └── TestAiConfiguration.java         # Test AI mocks
└── data/
    ├── uploads/                                      # Uploaded images
    ├── kogito/
    │   ├── processes/                               # Generated BPMN
    │   └── rules/                                   # Generated DRL
```

---

## 🔧 Configuration

### **application.yml**

```yaml
spring:
  application:
    name: ai-bpmn-compiler
  
  # H2 In-Memory Database
  datasource:
    url: jdbc:h2:mem:aibpmn;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:
  
  # H2 Console
  h2:
    console:
      enabled: true
      path: /h2-console
  
  # JPA Configuration
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: create-drop
    show-sql: false
  
  # AI Configuration
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4o
          temperature: 0.7
          max-tokens: 4096

server:
  port: 8080

# Application Config
app:
  upload:
    base-dir: ./data/uploads
  kogito:
    bpmn-dir: ./data/kogito/processes
    drl-dir: ./data/kogito/rules
  ai:
    provider: openai

logging:
  level:
    root: INFO
    com.example.aibpmn: DEBUG
    org.drools: DEBUG
```

### **Environment Variables**

```bash
# Required
export OPENAI_API_KEY="sk-your-openai-api-key"

# Optional (for Gemini)
export AI_PROVIDER="gemini"
export GEMINI_API_KEY="your-gemini-key"
```

---

## 🚀 Building & Running

### **Development**

```bash
# Clean build
./gradlew clean build

# Run application
./gradlew bootRun

# Run with auto-restart (DevTools)
./gradlew bootRun --continuous

# Access H2 Console
open http://localhost:8080/h2-console
```

### **Production**

```bash
# Build JAR
./gradlew build

# Run JAR
java -jar build/libs/ai-bpmn-compiler-0.0.1-SNAPSHOT.jar

# With custom config
java -jar build/libs/ai-bpmn-compiler-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  --server.port=9090
```

### **Testing**

```bash
# Run all tests
./gradlew test

# Run specific test
./gradlew test --tests ProcessReasonerServiceTest

# With coverage
./gradlew test jacocoTestReport
```

---

## 📡 API Endpoints

### **Process Management**

#### **Create from Text**
```http
POST /api/process/from-text
Content-Type: application/json

{
  "description": "Order approval: receive order, validate, if amount > $1000 then manager approval",
  "name": "Order Approval Process"
}
```

#### **Create from Image**
```http
POST /api/process/from-image
Content-Type: multipart/form-data

image: <file>
name: "My Process"
```

#### **Get Process**
```http
GET /api/process/{processId}
```

#### **Get BPMN Moddle JSON**
```http
GET /api/process/{processId}/bpmn-json
```

#### **Publish Process**
```http
POST /api/process/{processId}/publish
```

#### **Execute Process**
```http
POST /api/process/{processId}/execute
Content-Type: application/json

{
  "amount": 1500,
  "customerId": "CUST-001"
}
```

### **Rule Management**

#### **Generate Rule**
```http
POST /api/rules/generate
Content-Type: application/json

{
  "ruleName": "High Value Order Check",
  "ruleDescription": "Orders over $1000 require manager approval",
  "processId": "proc-abc123",
  "taskId": "Task_ApprovalRule"
}
```

#### **Get Rules for Process**
```http
GET /api/rules/process/{processId}
```

#### **Activate Rule**
```http
POST /api/rules/{ruleId}/activate
```

### **Database Console**
```http
GET /h2-console
```

---

## 🏗️ Architecture

### **Hybrid Storage Model**

```
┌─────────────────────────────────────────┐
│           ProcessModel Entity            │
├─────────────────────────────────────────┤
│  Primary: bpmnModdleJson (TEXT)         │
│  {                                       │
│    "$type": "bpmn:Definitions",         │
│    "rootElements": [...]                │
│  }                                       │
├─────────────────────────────────────────┤
│  Secondary: BpmnMetadata (Embedded)     │
│  {                                       │
│    processName, elementCount,           │
│    taskCount, gatewayCount,             │
│    businessRuleTaskCount, ...           │
│  }                                       │
└─────────────────────────────────────────┘
```

**Benefits:**
- Full BPMN 2.0 support (Moddle JSON)
- Fast queries (metadata)
- No parsing overhead
- Frontend compatibility

### **AI Service Flow**

```
User Input (Text/Image)
    ↓
ProcessReasonerService / ProcessImageService
    ↓
AI generates BPMN Moddle JSON
    ↓
BpmnValidationService validates
    ↓
Extract metadata
    ↓
Save to H2 database
    ↓
Return processId to frontend
```

### **Rule Generation Flow**

```
Natural Language Rule Description
    ↓
DroolsRuleService
    ↓
AI generates DRL + Java fact classes
    ↓
Validate DRL syntax (Drools compiler)
    ↓
Save RuleSet entity
    ↓
Attach to BusinessRuleTask
```

---

## 🗄️ Database Schema

### **ProcessModel Table**
```sql
CREATE TABLE process_models (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255),
    description TEXT,
    bpmn_moddle_json TEXT NOT NULL,
    status VARCHAR(50),
    version INT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    -- Embedded BpmnMetadata fields
    process_name VARCHAR(255),
    element_count INT,
    task_count INT,
    gateway_count INT,
    business_rule_task_count INT,
    has_business_rules BOOLEAN
);
```

### **RuleSet Table**
```sql
CREATE TABLE rule_sets (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255),
    description TEXT,
    drl TEXT,
    explanation TEXT,
    process_id VARCHAR(255),
    task_id VARCHAR(255),
    status VARCHAR(50),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

---

## 🔍 Key Services

### **1. ProcessReasonerService**
**Purpose:** Convert text descriptions to BPMN Moddle JSON

**AI Prompt:** Instructs GPT-4o to output BPMN 2.0 Moddle JSON directly

**Output:**
```json
{
  "bpmnModdleJson": {
    "$type": "bpmn:Definitions",
    "rootElements": [...]
  },
  "metadata": {
    "businessRuleTasks": [...],
    "explanation": "..."
  }
}
```

### **2. ProcessImageService**
**Purpose:** Convert process diagram images to BPMN

**AI Model:** GPT-4o Vision

**Supported Images:**
- Hand-drawn flowcharts
- Whiteboard photos
- BPMN screenshots
- Any visual process representation

### **3. DroolsRuleService**
**Purpose:** Generate Drools rules from natural language

**Output:**
- DRL (Drools Rule Language) code
- Java fact class definitions
- Validation results

**Example:**
```
Input: "High value orders need approval"
Output: DRL rule + Order.java fact class
```

### **4. BpmnValidationService**
**Purpose:** Validate AI-generated BPMN

**Checks:**
- BPMN 2.0 compliance
- Element ID uniqueness
- Flow connectivity
- Start/End event presence
- Orphan node detection

---

## 🧪 Testing

### **Unit Tests**
```bash
# All services
./gradlew test --tests com.example.aibpmn.service.*

# Specific service
./gradlew test --tests ProcessReasonerServiceTest
```

### **Integration Tests**
```bash
# Controllers
./gradlew test --tests com.example.aibpmn.controller.*
```

### **Test Configuration**

**TestAiConfiguration.java** provides mock AI clients for tests:
```java
@TestConfiguration
public class TestAiConfiguration {
    @Bean
    @Qualifier("openAiChatModel")
    public ChatModel openAiChatModel() {
        return mock(ChatModel.class);
    }
    
    @Bean
    @Qualifier("vertexAiGeminiChat")
    public ChatModel vertexAiGeminiChat() {
        return mock(ChatModel.class);
    }
}
```

---

## 🐛 Troubleshooting

### **Build Fails**
```bash
# Clean build
./gradlew clean build --refresh-dependencies

# Check Java version
java -version  # Should be 17+
```

### **H2 Database Issues**
```bash
# Access console
open http://localhost:8080/h2-console

# JDBC URL: jdbc:h2:mem:aibpmn
# Username: sa
# Password: (empty)

# Check tables
SELECT * FROM PROCESS_MODELS;
SELECT * FROM RULE_SETS;
```

### **AI Client Errors**
```bash
# Verify API key
echo $OPENAI_API_KEY

# Check logs
tail -f logs/spring.log | grep "AI Provider"

# Expected: "Selected AI Provider: OpenAI GPT-4o"
```

### **Port Already in Use**
```bash
# Kill process on port 8080
lsof -ti:8080 | xargs kill -9

# Or use different port
./gradlew bootRun --args='--server.port=9090'
```

---

## 📚 Documentation

- **[AI_CLIENT_CONFIGURATION.md](AI_CLIENT_CONFIGURATION.md)** - AI provider setup
- **[../IMPLEMENTATION_COMPLETE.md](../IMPLEMENTATION_COMPLETE.md)** - Implementation summary
- **[../END_TO_END_TESTING_GUIDE.md](../END_TO_END_TESTING_GUIDE.md)** - Testing guide

---

## 🚀 Deployment

### **Docker (Future)**
```dockerfile
FROM openjdk:17-jdk-slim
COPY build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### **Production Recommendations**
1. Use PostgreSQL instead of H2
2. Implement authentication/authorization
3. Add rate limiting
4. Set up monitoring (Prometheus, Grafana)
5. Configure logging (ELK stack)
6. Implement caching (Redis)
7. Add health checks
8. Set up CI/CD pipeline

---

**Built with ❤️ using Spring Boot, OpenAI GPT-4o, Drools, and H2**

**Version**: 3.0  
**AI Provider**: OpenAI GPT-4o + Vision  
**Database**: H2 In-Memory  
**Status**: ✅ Production Ready
