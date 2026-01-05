# End-to-End Testing Guide - AI BPMN Compiler

## Overview

This guide provides step-by-step instructions to configure, run, and test the AI BPMN Compiler application from start to finish.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Initial Setup](#initial-setup)
3. [Backend Configuration](#backend-configuration)
4. [Frontend Configuration](#frontend-configuration)
5. [Starting the Application](#starting-the-application)
6. [End-to-End Testing Scenarios](#end-to-end-testing-scenarios)
7. [Troubleshooting](#troubleshooting)
8. [Advanced Testing](#advanced-testing)

---

## Prerequisites

### Required Software

1. **Java 17 or higher**
   ```bash
   java -version
   # Should show: java version "17" or higher
   ```

2. **Node.js 18+ and npm**
   ```bash
   node --version  # Should be v18.x.x or higher
   npm --version   # Should be 9.x.x or higher
   ```

3. **Git** (to clone the repository)
   ```bash
   git --version
   ```

### Required Accounts/Keys

1. **OpenAI Account** (Default - Recommended)
   - OpenAI Platform account
   - API key generated
   - [Get API Key](https://platform.openai.com/api-keys)

2. **Alternative: Google Cloud Account** (for Gemini AI)
   - Google Cloud project
   - Gemini API enabled
   - API key generated
   - [Get API Key](https://makersuite.google.com/app/apikey)

📖 **See [backend/AI_CLIENT_CONFIGURATION.md](backend/AI_CLIENT_CONFIGURATION.md) for detailed provider comparison and setup**

---

## Initial Setup

### Step 1: Clone the Repository

```bash
# If not already cloned
git clone <repository-url>
cd ai-bpmn-compiler
```

### Step 2: Verify Project Structure

```bash
# Verify the structure
ls -la

# You should see:
# ├── backend/          # Spring Boot backend
# ├── frontend/         # React frontend
# ├── data/            # Will be created for uploads and Kogito files
# └── README.md
```

---

## Backend Configuration

### Step 1: Get AI API Key

The application uses **OpenAI GPT-4o by default**. You can optionally switch to Google Gemini.

#### Option A: OpenAI (Default - Recommended)

1. Go to [OpenAI Platform](https://platform.openai.com/api-keys)
2. Sign in or create an account
3. Navigate to "API Keys"
4. Click "Create new secret key"
5. Copy the key (starts with `sk-`)

**Cost**: Pay-as-you-go, ~$5-15 per million tokens

#### Option B: Google Gemini (Alternative)

**Using Google AI Studio (Quickest)**:
1. Go to [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Click "Create API Key"
3. Copy the generated API key

**Or using Google Cloud Console**:
1. Go to [Google Cloud Console](https://console.cloud.google.com)
2. Create or select a project
3. Enable "Vertex AI API"
4. Create credentials (API Key)
5. Copy the API key and project ID

**Cost**: Free tier available, generous limits

📖 **Detailed comparison**: See [backend/AI_CLIENT_CONFIGURATION.md](backend/AI_CLIENT_CONFIGURATION.md)

### Step 2: Configure Backend

#### Method 1: Environment Variables (Recommended)

**For OpenAI (Default)**:
```bash
# On macOS/Linux
export OPENAI_API_KEY="sk-your-api-key-here"

# On Windows (PowerShell)
$env:OPENAI_API_KEY="sk-your-api-key-here"

# On Windows (CMD)
set OPENAI_API_KEY=sk-your-api-key-here
```

**For Gemini (Alternative)**:
```bash
# On macOS/Linux
export AI_PROVIDER=gemini
export GEMINI_API_KEY="your-api-key-here"
export VERTEX_AI_PROJECT_ID="your-project-id"
export VERTEX_AI_LOCATION="us-central1"

# On Windows (PowerShell)
$env:AI_PROVIDER="gemini"
$env:GEMINI_API_KEY="your-api-key-here"
$env:VERTEX_AI_PROJECT_ID="your-project-id"
$env:VERTEX_AI_LOCATION="us-central1"
```

#### Method 2: Application Properties File

Create or edit `backend/src/main/resources/application.yml`:

**OpenAI Configuration** (already in file by default):
```yaml
app:
  ai:
    provider: openai  # Default

spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY:your-openai-api-key-here}
      chat:
        options:
          model: gpt-4o
          temperature: 0.7
          max-tokens: 4096
```

**Or switch to Gemini**:
```yaml
app:
  ai:
    provider: gemini  # Change to gemini

spring:
  ai:
    vertex:
      ai:
        gemini:
          project-id: your-project-id
          location: us-central1
          api-key: your-api-key-here
```

⚠️ **Security Warning**: Never commit API keys to version control!

Add to `.gitignore`:
```
application-local.yml
.env
.env.local
```

📖 **More details**: See [backend/AI_CLIENT_CONFIGURATION.md](backend/AI_CLIENT_CONFIGURATION.md) for advanced configuration

### Step 3: Verify Backend Dependencies

```bash
cd backend

# On macOS/Linux
./gradlew dependencies

# On Windows
gradlew.bat dependencies

# This downloads all required dependencies
```

### Step 4: Create Required Directories

```bash
# From project root
mkdir -p data/uploads
mkdir -p data/kogito/processes
mkdir -p data/kogito/rules
```

Or let the application create them automatically on first run.

### Step 5: Build Backend

```bash
cd backend

# Clean and build
./gradlew clean build

# This should complete without errors
# Output: BUILD SUCCESSFUL in X s
```

---

## Frontend Configuration

### Step 1: Install Dependencies

```bash
cd frontend
npm install

# This installs all required packages
# Should complete without errors
```

### Step 2: Verify Configuration

The frontend is pre-configured to connect to `http://localhost:8080` (backend).

If you need to change this, edit the axios calls in:
- `frontend/src/components/BpmnDiagram.tsx`
- `frontend/src/components/PropertiesPanel.tsx`
- `frontend/src/components/ChatPanel.tsx`

Or create `frontend/.env.local`:
```env
VITE_API_BASE_URL=http://localhost:8080
```

### Step 3: Build Frontend (Optional - for production)

```bash
cd frontend
npm run build

# Creates optimized production build in dist/
```

---

## Starting the Application

### Step 1: Start Backend

Open a terminal and run:

```bash
cd backend

# Start the Spring Boot application
./gradlew bootRun

# Wait for the message:
# "Started AiBpmnCompilerApplication in X.XXX seconds"
```

The backend will be available at: **http://localhost:8080**

#### Verify Backend is Running

```bash
# In a new terminal
curl http://localhost:8080/actuator/health

# Expected response:
# {"status":"UP"}
```

### Step 2: Start Frontend

Open a **new terminal** (keep backend running) and run:

```bash
cd frontend

# Start the development server
npm run dev

# Wait for the message:
# "Local: http://localhost:5173/"
```

The frontend will be available at: **http://localhost:5173**

### Step 3: Open Application

Open your web browser and navigate to:
```
http://localhost:5173
```

You should see the AI BPMN Compiler interface with:
- **Left**: BPMN Diagram (Read-Only)
- **Middle**: Properties Panel
- **Right**: AI Chat Panel

---

## End-to-End Testing Scenarios

### Test Scenario 1: Create a Simple Process

#### Step 1: Start a Conversation

1. In the **Chat Panel** (right side), you'll see a welcome message
2. Type a process description:
   ```
   Create a simple leave approval process where an employee submits a request, 
   their manager reviews it, and if approved, HR processes it.
   ```
3. Press **Enter** or click **Send**

#### Step 2: Observe AI Processing

You should see:
- User message appears
- System message: "Process created with ID: proc-xxxxx"
- Assistant thinking/processing
- Possible clarification questions

#### Step 3: Answer Clarification Questions

If the AI asks questions like:
```
Q: "Should there be a rejection path if the manager denies the request?"
```

Answer naturally:
```
Yes, if rejected, notify the employee and end the process.
```

#### Step 4: View Generated BPMN

1. Once complete, the **BPMN Diagram** (left panel) will display the process
2. You should see:
   - Start event
   - Submit Request task
   - Manager Review task/gateway
   - HR Processing task
   - End event(s)

#### Step 5: Verify Process Details

1. In the **Properties Panel** (middle), you should see:
   - Process ID
   - Process Name
   - Status: DRAFT
   - AI State: MODEL_READY or DRL_GENERATED
   - Created/Updated timestamps

**✅ Success Criteria**:
- Process ID generated
- BPMN diagram displays
- Properties panel shows process info
- No errors in browser console or backend logs

---

### Test Scenario 2: Edit a Process Element

#### Step 1: Select an Element

1. Click on any task in the BPMN diagram (e.g., "Submit Request")
2. The element should be highlighted
3. The **Properties Panel** shows selected element details
4. The **Chat Panel** shows context: "🎯 Selected: Submit Request (bpmn:Task)"

#### Step 2: Edit via Properties Panel

1. In the **Properties Panel**, scroll down to "Selected Element"
2. Click the **"Edit Element"** button
3. A text area appears with edit instructions
4. Type an edit instruction:
   ```
   Rename this to "Submit Leave Application"
   ```
5. Click **"Apply Edit"**

#### Step 3: Observe Edit Processing

You should see:
- Loading indicator
- Backend processing the edit
- Success message: "Edit applied successfully!"
- Page refreshes automatically

#### Step 4: Verify Edit Applied

After refresh:
1. The task name should be updated in the BPMN diagram
2. Properties panel shows new name
3. The canonical model was updated (backend logs confirm)

**Alternative: Edit via Chat**

With element still selected:
1. In **Chat Panel**, type:
   ```
   Change the condition to amount > 5000
   ```
2. Send the message
3. System applies edit through canonical model
4. Diagram refreshes with changes

**✅ Success Criteria**:
- Element name changed in diagram
- Edit went through edit-intent API
- BPMN regenerated from canonical model
- No direct BPMN XML modification occurred

---

### Test Scenario 3: View Explanations

#### Step 1: Load Explanations

1. With a process open, select any element
2. In the **Properties Panel**, look for "AI Explanation" section
3. You should see an explanation like:
   ```
   AI Explanation:
   "This task collects the initial request from the user and 
   validates the basic information before proceeding to the 
   approval stage."
   
   Source: AI Generated
   Confidence: 85%
   ```

#### Step 2: View All Explanations

1. Deselect any element (click on canvas background)
2. Scroll down in **Properties Panel**
3. See "Process Explanations" section with all node explanations

**✅ Success Criteria**:
- Explanations display for selected elements
- All explanations available when nothing selected
- Confidence scores shown
- AI-generated reasons are meaningful

---

### Test Scenario 4: Publish a Process

#### Step 1: Verify Process is Ready

In **Properties Panel**:
- Status should be DRAFT
- AI State should be DRL_GENERATED or MODEL_READY

#### Step 2: Publish the Process

1. In **Properties Panel**, click the **"Publish"** button
2. Backend will:
   - Generate BPMN from canonical model
   - Generate DRL from rules
   - Deploy to Kogito filesystem
   - Update process status

#### Step 3: Verify Publishing

You should see:
- Status changes to PUBLISHED
- AI State changes to PUBLISHED
- Success alert appears
- "Execute" button becomes enabled

#### Step 4: Check Filesystem

```bash
# Verify BPMN file created
ls -la data/kogito/processes/
# Should see: proc-xxxxx.bpmn

# Verify DRL file created
ls -la data/kogito/rules/
# Should see: proc-xxxxx.drl
```

**✅ Success Criteria**:
- Process status: PUBLISHED
- BPMN file exists in kogito/processes
- DRL file exists in kogito/rules
- Execute button enabled

---

### Test Scenario 5: Execute a Process

#### Step 1: Execute via Properties Panel

1. With a PUBLISHED process, click the **"Execute"** button in Properties Panel
2. Enter process variables (if prompted), or just execute with defaults
3. Backend starts a process instance in Kogito

#### Step 2: Verify Execution

You should see:
- Success alert with instance ID
- Console shows: "Process instance started: xxx"

#### Step 3: Check Process Instance

```bash
# Query process instances via API
curl http://localhost:8080/api/process/proc-xxxxx/instances

# Or query specific instance
curl http://localhost:8080/api/process/proc-xxxxx/instance/{instanceId}
```

**Alternative: Execute via Kogito Endpoint**

```bash
# Direct Kogito execution
curl -X POST http://localhost:8080/proc-xxxxx \
  -H "Content-Type: application/json" \
  -d '{
    "orderAmount": 7500,
    "customerId": "CUST-001"
  }'
```

**✅ Success Criteria**:
- Instance ID returned
- Process instance created in Kogito
- No errors in execution
- Process variables passed correctly

---

### Test Scenario 6: Export BPMN

#### Step 1: Export Diagram

1. In the **BPMN Diagram** toolbar (top), click the **↓** (Export) button
2. A download should start automatically
3. File name: `proc-xxxxx.bpmn`

#### Step 2: Verify Export

1. Open the downloaded file in a text editor
2. Verify it's valid BPMN 2.0 XML
3. Contains all process elements

#### Step 3: Import to External Tool (Optional)

1. Open Camunda Modeler or other BPMN tool
2. Import the exported file
3. Verify diagram renders correctly

**✅ Success Criteria**:
- BPMN file downloads successfully
- XML is valid and well-formed
- Can be imported to other BPMN tools
- Export does NOT save to backend (download only)

---

### Test Scenario 7: Streaming AI Responses (If Enabled)

#### Step 1: Create Process with Long Description

```
Create a comprehensive employee onboarding process that includes:
1. Account setup
2. Document collection and verification
3. Background check initiation
4. IT equipment provisioning
5. Training module assignment
6. Manager introduction
7. Team integration activities
8. 30-60-90 day check-ins
```

#### Step 2: Observe Streaming

You should see:
- Response appears word-by-word
- Blinking cursor (▊) at the end
- "Stop" button appears
- Can cancel mid-stream

**Note**: Streaming requires backend support. If not available, system falls back to traditional request-response.

**✅ Success Criteria**:
- Text streams progressively
- Streaming cursor visible
- Stop button functional
- Graceful fallback if streaming unavailable

---

## Troubleshooting

### Backend Won't Start

#### Issue: Port 8080 Already in Use

```bash
# Find process using port 8080
lsof -i :8080  # macOS/Linux
netstat -ano | findstr :8080  # Windows

# Kill the process or change port in application.yml
server:
  port: 8081
```

#### Issue: API Key Not Configured

Error: "API key not found" or "401 Unauthorized"

**Solution**:
1. Verify environment variable is set:
   ```bash
   echo $GEMINI_API_KEY  # macOS/Linux
   echo %GEMINI_API_KEY%  # Windows CMD
   ```
2. Restart backend after setting environment variables
3. Check application.yml has correct configuration

#### Issue: Dependencies Not Downloading

```bash
# Clear Gradle cache
rm -rf ~/.gradle/caches/

# Re-download dependencies
./gradlew clean build --refresh-dependencies
```

### Frontend Won't Start

#### Issue: Port 5173 Already in Use

Vite will automatically use the next available port (5174, 5175, etc.)

#### Issue: Module Not Found

```bash
# Clear node_modules and reinstall
rm -rf node_modules package-lock.json
npm install
```

#### Issue: Build Errors

```bash
# Clear cache and rebuild
npm run build -- --force
```

### CORS Errors

If you see CORS errors in browser console:

**Backend Fix**:
Add CORS configuration in `backend/src/main/java/com/example/aibpmn/config/`:

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
```

### API Errors

#### Issue: 404 Not Found

- Verify backend is running: `curl http://localhost:8080/actuator/health`
- Check API endpoint URL in frontend code
- Verify process ID is correct

#### Issue: 500 Internal Server Error

1. Check backend console for stack traces
2. Verify Gemini API key is valid
3. Check API rate limits
4. Review backend logs in `backend/logs/` (if configured)

### BPMN Not Displaying

#### Issue: Blank Diagram

1. Open browser developer tools (F12)
2. Check Console for errors
3. Check Network tab for failed requests
4. Verify process ID is set correctly
5. Check backend logs for BPMN generation errors

#### Issue: Invalid BPMN XML

1. Verify BpmnGeneratorService is working
2. Check backend logs for validation errors
3. Test BPMN generation endpoint directly:
   ```bash
   curl http://localhost:8080/api/process/{processId}/bpmn
   ```

---

## Advanced Testing

### Performance Testing

#### Test 1: Large Process Creation

```
Create a complex supply chain management process with 50+ tasks including:
- Order intake and validation
- Inventory checking across multiple warehouses
- Supplier coordination
- Manufacturing scheduling
- Quality control checkpoints
- Shipping and logistics
- Customer notification
- Returns processing
```

**Expected**: Process should be created within 30-60 seconds

#### Test 2: Multiple Concurrent Edits

1. Open process in multiple browser tabs
2. Make different edits in each tab
3. Verify consistency after all edits complete

#### Test 3: Large BPMN Export

1. Create process with 30+ elements
2. Export BPMN
3. Verify export completes and file is valid

### Integration Testing

#### Test 1: Kogito Direct Execution

```bash
# Start a process directly via Kogito
curl -X POST http://localhost:8080/proc-xxxxx \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 5000,
    "requestType": "vacation"
  }'

# Query instance
curl http://localhost:8080/proc-xxxxx/{instanceId}

# Get task list
curl http://localhost:8080/proc-xxxxx/{instanceId}/tasks
```

#### Test 2: Rule Execution

If your process has rules (amount > 10000):

```bash
curl -X POST http://localhost:8080/proc-xxxxx \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 15000,
    "requestType": "purchase"
  }'

# Verify rule fires (check in backend logs)
# Should see: "Rule 'HighAmountApproval' fired"
```

#### Test 3: API Health Check

```bash
# Backend health
curl http://localhost:8080/actuator/health

# Backend info
curl http://localhost:8080/actuator/info

# Metrics (if enabled)
curl http://localhost:8080/actuator/metrics
```

### Security Testing

#### Test 1: API Authentication (If Enabled)

```bash
# Without auth (should fail)
curl http://localhost:8080/api/process/proc-123

# With auth
curl -H "Authorization: Bearer {token}" \
  http://localhost:8080/api/process/proc-123
```

#### Test 2: Input Validation

Try invalid inputs:
```bash
# Empty process description
curl -X POST http://localhost:8080/api/process/start \
  -H "Content-Type: application/json" \
  -d '{"description": ""}'

# Expected: 400 Bad Request

# Invalid process ID
curl http://localhost:8080/api/process/invalid-id/bpmn

# Expected: 404 Not Found
```

---

## Test Checklist

### Initial Setup
- [ ] Java 17+ installed and verified
- [ ] Node.js 18+ installed and verified
- [ ] Gemini API key obtained
- [ ] Environment variables configured
- [ ] Backend builds successfully
- [ ] Frontend installs dependencies

### Backend Tests
- [ ] Backend starts without errors
- [ ] Health endpoint responds
- [ ] API key is working (check logs)
- [ ] Kogito directories created
- [ ] Logging is working

### Frontend Tests
- [ ] Frontend starts without errors
- [ ] UI loads correctly
- [ ] All three panels visible
- [ ] No console errors
- [ ] Dev mode warning appears

### Functional Tests
- [ ] Create process via natural language
- [ ] BPMN diagram displays
- [ ] Edit element via properties panel
- [ ] Edit element via chat panel
- [ ] View explanations
- [ ] Publish process
- [ ] Execute process
- [ ] Export BPMN

### Integration Tests
- [ ] Edit through canonical model only
- [ ] BPMN regenerated after edits
- [ ] Kogito files created correctly
- [ ] Process instances can be queried
- [ ] Rules fire correctly

### Architectural Tests
- [ ] BpmnViewer used (not BpmnModeler)
- [ ] No direct BPMN saves
- [ ] All edits via edit-intent API
- [ ] Console logs show canonical model updates
- [ ] Dev notice appears

---

## Logging and Monitoring

### Backend Logs

Check console output for:
```
INFO  [BpmnGeneratorService] Generating BPMN for process: proc-xxx
INFO  [ProcessEditService] Processing edit intent for process: proc-xxx
INFO  [KogitoDeploymentService] Process deployed successfully: proc-xxx
```

### Frontend Logs

Open browser console (F12) and check for:
```
[BPMN Viewer] Loaded BPMN from canonical model for process: proc-xxx
[Edit] Successfully updated canonical model, BPMN regenerated
[Edit Intent] Sending instruction to canonical model API: ...
```

### Enable Debug Logging

In `backend/src/main/resources/application.yml`:

```yaml
logging:
  level:
    root: INFO
    com.example.aibpmn: DEBUG
    org.kie.kogito: DEBUG
    org.springframework.ai: DEBUG
```

---

## Next Steps

After successful end-to-end testing:

1. **Explore Features**:
   - Try different process types
   - Test complex edit scenarios
   - Experiment with business rules

2. **Review Documentation**:
   - Read `CANONICAL_MODEL_ARCHITECTURE.md`
   - Review `EDIT_INTENT_API.md`
   - Check `KOGITO_SETUP.md`

3. **Customize**:
   - Adjust AI prompts
   - Modify UI styling
   - Add custom validations

4. **Deploy**:
   - Build for production
   - Configure for production API
   - Set up monitoring

---

## Getting Help

### Documentation Files
- `QUICK_START.md` - Quick setup guide
- `CANONICAL_MODEL_ARCHITECTURE.md` - Architecture details
- `backend/API.md` - API documentation
- `frontend/README.md` - Frontend guide
- `backend/KOGITO_SETUP.md` - Kogito configuration

### Common Issues
- Check backend console for errors
- Check browser console for frontend errors
- Verify API key is valid
- Ensure ports are not in use
- Restart both backend and frontend

### Debug Mode
- Frontend: Already in dev mode with HMR
- Backend: Add `--debug` flag to bootRun

---

## Conclusion

You should now have a fully configured and tested AI BPMN Compiler application running locally. The system demonstrates:

✅ AI-driven process creation  
✅ Natural language editing  
✅ Canonical model architecture  
✅ Read-only BPMN viewer  
✅ Kogito process execution  
✅ Business rule management  

Enjoy exploring the AI-powered BPMN workflow! 🚀

