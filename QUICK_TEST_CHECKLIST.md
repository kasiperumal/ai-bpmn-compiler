# Quick Test Checklist - AI BPMN Compiler

## 5-Minute Quick Start

### 1. Configure API Key

**Option A: OpenAI (Default, Easier)**
```bash
export OPENAI_API_KEY="sk-your-openai-api-key-here"
# Get key: https://platform.openai.com/api-keys
```

**Option B: Gemini (Alternative)**
```bash
export AI_PROVIDER=gemini
export GEMINI_API_KEY="your-gemini-api-key-here"
export VERTEX_AI_PROJECT_ID="your-project-id"
export VERTEX_AI_LOCATION="us-central1"
# Get key: https://makersuite.google.com/app/apikey
```

### 2. Start Backend

```bash
cd backend
./gradlew bootRun

# Wait for: "Started AiBpmnCompilerApplication"
# Backend: http://localhost:8080
```

### 3. Start Frontend (New Terminal)

```bash
cd frontend
npm install  # First time only
npm run dev

# Frontend: http://localhost:5173
```

### 4. Open Browser

Navigate to: **http://localhost:5173**

---

## Quick Test Scenarios

### ✅ Test 1: Create Process (2 minutes)

1. **Chat Panel** (right): Type process description
   ```
   Create a leave approval process with employee request, manager approval, and HR processing
   ```
2. Press **Enter**
3. **Expected**: BPMN diagram appears on left
4. **Expected**: Process ID shows in properties panel

### ✅ Test 2: Edit Element (1 minute)

1. **Click** any task in diagram
2. **Properties Panel** (middle): Click "Edit Element"
3. Type: `Rename to "Submit Leave Request"`
4. Click "Apply Edit"
5. **Expected**: Diagram refreshes with new name

### ✅ Test 3: Publish & Execute (2 minutes)

1. **Properties Panel**: Click "Publish"
2. Wait for success message
3. Click "Execute"
4. **Expected**: Instance ID returned

---

## Verification Checklist

### Backend ✓
- [ ] Port 8080 accessible
- [ ] Health check: `curl http://localhost:8080/actuator/health`
- [ ] No errors in console

### Frontend ✓
- [ ] Port 5173 accessible
- [ ] UI shows 3 panels: Diagram | Properties | Chat
- [ ] Dev warning visible: "⚠️ Read-Only Viewer"
- [ ] No console errors (F12)

### Functionality ✓
- [ ] Can create process from text
- [ ] BPMN diagram displays
- [ ] Can select elements
- [ ] Can edit via natural language
- [ ] Can publish process
- [ ] Can execute process
- [ ] Can export BPMN

### Architecture ✓
- [ ] BpmnViewer used (not Modeler)
- [ ] Edits go through edit-intent API
- [ ] Console shows canonical model updates
- [ ] BPMN regenerated after edits

---

## Common Issues & Quick Fixes

### Issue: Backend Won't Start
```bash
# Check port availability
lsof -i :8080

# Or change port in application.yml
server:
  port: 8081
```

### Issue: API Key Error
```bash
# Verify environment variable
echo $GEMINI_API_KEY

# Should show your API key
# If empty, set it again
```

### Issue: Frontend CORS Error
- Restart both backend and frontend
- Check backend allows origin: http://localhost:5173

### Issue: BPMN Not Loading
1. Check browser console (F12)
2. Verify backend is running
3. Check process ID is valid

---

## File Locations

### Configuration
```
backend/src/main/resources/application.yml
```

### Data Directories
```
data/uploads/                  # Uploaded files
data/kogito/processes/        # BPMN files
data/kogito/rules/            # DRL files
```

### Logs
```
Backend console output
Browser console (F12)
```

---

## Test Data Examples

### Simple Process
```
Create a simple approval process with a request, review, and approval step.
```

### Complex Process
```
Create an order fulfillment process including:
- Order validation
- Inventory check
- Payment processing
- Shipping
- Notification
```

### Edit Instructions
```
Rename this to "Review Application"
Change condition to amount > 10000
Update description: This task validates credentials
```

---

## API Quick Reference

### Create Process
```bash
curl -X POST http://localhost:8080/api/process/start \
  -H "Content-Type: application/json" \
  -d '{"description": "Create a leave approval process"}'
```

### Edit Process
```bash
curl -X POST http://localhost:8080/api/process/{id}/edit-intent \
  -H "Content-Type: application/json" \
  -d '{
    "instruction": "Rename to New Name",
    "nodeId": "task_1"
  }'
```

### Publish Process
```bash
curl -X POST http://localhost:8080/api/process/{id}/publish
```

### Execute Process
```bash
curl -X POST http://localhost:8080/api/process/{id}/execute \
  -H "Content-Type: application/json" \
  -d '{}'
```

### Get BPMN
```bash
curl http://localhost:8080/api/process/{id}/bpmn
```

---

## Success Indicators

### ✓ Everything Working
- Backend starts in < 30 seconds
- Frontend loads immediately
- Process creation completes in < 10 seconds
- BPMN diagram renders smoothly
- Edits apply in < 5 seconds
- Publishing succeeds
- Execution returns instance ID

### ✗ Something Wrong
- Startup takes > 1 minute
- 404 or 500 errors
- Blank screens
- Console errors
- API timeouts

---

## Next Steps

For detailed instructions, see:
- **END_TO_END_TESTING_GUIDE.md** - Complete testing guide
- **CANONICAL_MODEL_ARCHITECTURE.md** - Architecture details
- **QUICK_START.md** - Basic setup

---

## Emergency Reset

If everything breaks:

```bash
# Stop all
# Ctrl+C in both terminals

# Clean backend
cd backend
./gradlew clean
rm -rf data/

# Clean frontend
cd frontend
rm -rf node_modules dist
npm install

# Restart
cd backend && ./gradlew bootRun
# New terminal
cd frontend && npm run dev
```

---

**Total Test Time**: ~10 minutes for full verification  
**Minimum Test**: ~5 minutes for basic functionality

🚀 Happy Testing!

