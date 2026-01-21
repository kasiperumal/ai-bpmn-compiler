# Unified AI-Driven Chat Architecture

## 🎯 **Overview**

This document describes the refactored chat architecture that eliminates frontend keyword detection in favor of AI-driven intent detection.

### **Previous Architecture (Flawed):**
```
User Message → Frontend Keyword Matching → Route Decision
                      ↓                           ↓
              If matches keywords          If no match
                      ↓                           ↓
            /edit-intent endpoint       /interactive/start endpoint
```

**Problems:**
- ❌ Frontend did "dumb" keyword matching
- ❌ Could miss valid requests (e.g., "instead of user task...")
- ❌ Two separate code paths for AI processing
- ❌ Brittle - required maintaining keyword lists
- ❌ AI capabilities wasted

---

### **New Architecture (AI-Driven):**
```
User Message → Unified /api/chat/message → AI Intent Analysis
                                                   ↓
                                    ┌──────────────┴──────────────┐
                                    ↓                             ↓
                            Intent: EDIT                  Intent: CREATE
                                    ↓                             ↓
                      Apply edit to canonical        Start interactive
                      ProcessModel, regenerate       process creation
                                    ↓                             ↓
                               ChatResponse                  ChatResponse
```

**Benefits:**
- ✅ **Single source of truth** - AI handles all intent detection
- ✅ **Smarter detection** - AI understands context, not just keywords
- ✅ **Simpler frontend** - Just send message to one endpoint
- ✅ **Flexible** - Works with any natural language pattern
- ✅ **Handles edge cases** - AI distinguishes "create task" vs "modify task"

---

## 📋 **Components**

### **1. DTOs (Data Transfer Objects)**

#### `ChatRequest.java`
```java
{
  "message": "Instead of user task, make it a business rule task",
  "processId": "proc-123"           // Optional - if editing existing
  "selectedElementId": "Task_1"     // Optional - if element selected
  "conversationId": "conv-456"      // Optional - for continuing conversations
}
```

#### `ChatResponse.java`
```java
{
  "intent": "EDIT" | "CREATE" | "QUESTION" | "CLARIFICATION",
  "action": "EDIT_APPLIED" | "PROCESS_CREATED" | "CLARIFICATION_NEEDED" | "INFORMATION_PROVIDED" | "ERROR",
  "message": "Human-readable message",
  "processId": "proc-123",
  "requiresRefresh": true,           // Frontend should reload diagram
  "questions": ["Question 1", ...],  // If clarification needed
  "conversationId": "conv-456",
  "success": true
}
```

### **2. ChatService (Brain of the System)**

**Location:** `backend/src/main/java/com/example/aibpmn/service/ChatService.java`

**Key Methods:**

1. **`processMessage(ChatRequest)`** - Main entry point
   - Validates input
   - Calls AI to analyze intent
   - Routes to appropriate handler
   - Returns unified response

2. **`analyzeIntent(ChatRequest)`** - AI Intent Detection
   - Builds comprehensive prompt with context
   - Sends to AI client
   - Parses AI response to determine intent
   - Fallback to heuristic detection if parsing fails

3. **Intent Handlers:**
   - `handleEditIntent()` - Routes to `ProcessEditService`
   - `handleCreateIntent()` - Routes to `InteractiveProcessService`
   - `handleQuestionIntent()` - Uses AI to answer questions
   - `handleClarificationIntent()` - Asks user for more details

**AI Intent Detection Prompt:**

The AI receives:
- User message
- Process ID (if exists)
- Selected element ID (if exists)
- Process details (name, node count)

The AI returns:
```json
{
  "intent": "EDIT",
  "confidence": 0.95,
  "reasoning": "User selected element and wants to change type",
  "action": "change_type"
}
```

**Intent Definitions:**
- **EDIT**: Modify existing process/element
  - Indicators: Element selected, mentions changes to existing
  - Examples: "Change this to...", "Instead of...", "Update condition..."
  
- **CREATE**: Create new process
  - Indicators: No process exists, describes new workflow
  - Examples: "Create leave approval...", "I need workflow for..."
  
- **QUESTION**: User asking for help
  - Indicators: Question words, asking for explanation
  - Examples: "How do I...", "What is...", "Can you explain..."
  
- **CLARIFICATION**: Ambiguous request
  - Indicators: Not enough context
  - Examples: Vague references, unclear requests

### **3. ChatController**

**Location:** `backend/src/main/java/com/example/aibpmn/controller/ChatController.java`

**Endpoint:**
```
POST /api/chat/message
Content-Type: application/json

Request: ChatRequest
Response: ChatResponse
```

**Features:**
- ✅ Single unified endpoint for ALL chat interactions
- ✅ Replaces previous separate endpoints:
  - `/api/process/from-text` ❌ (removed)
  - `/api/process/interactive/start` ❌ (removed) 
  - `/api/process/{id}/edit-intent` ❌ (removed)
- ✅ Comprehensive error handling
- ✅ Health check endpoint: `GET /api/chat/health`

### **4. Frontend ChatPanel.tsx**

**Changes:**
1. **Removed `detectEditIntent()` function** ❌
   - No more frontend keyword matching!
   
2. **Removed `handleEditIntent()` function** ❌
   - No direct calls to edit endpoint
   
3. **Added `handleUnifiedMessage()` function** ✅
   - Single message handler
   - Sends to `/api/chat/message`
   - Handles all response types
   
4. **Simplified `handleSend()` function** ✅
   - Just calls `handleUnifiedMessage()`
   - No routing logic needed!

**Message Flow:**
```typescript
User types message
    ↓
handleSend() adds user message to chat
    ↓
handleUnifiedMessage(message) builds request:
    {
      message: "Change to business rule task",
      processId: currentProcessId,
      selectedElementId: selectedElement?.id
    }
    ↓
POST /api/chat/message
    ↓
Receive ChatResponse
    ↓
Switch on response.action:
    - EDIT_APPLIED → Show success, reload diagram
    - PROCESS_CREATED → Show success, set processId, reload
    - CLARIFICATION_NEEDED → Show questions
    - INFORMATION_PROVIDED → Show answer
```

---

## 🔄 **Request/Response Flow Examples**

### **Example 1: Edit Intent**

**User Message:** "Instead of user task, make it a business rule task. Add rule: if days > 5 then reject."

**Request:**
```json
{
  "message": "Instead of user task, make it a business rule task. Add rule: if days > 5 then reject.",
  "processId": "proc-123",
  "selectedElementId": "Task_ManagerReview"
}
```

**AI Analysis:**
```json
{
  "intent": "EDIT",
  "confidence": 0.98,
  "reasoning": "User selected element and wants to change type + add rule",
  "action": "change_type_and_add_rule"
}
```

**Backend Action:**
1. Routes to `ProcessEditService.processEditIntent()`
2. AI generates edit commands: `change_type`, `add_rule`
3. Updates canonical `ProcessModel`
4. Regenerates BPMN XML
5. Returns success

**Response:**
```json
{
  "intent": "EDIT",
  "action": "EDIT_APPLIED",
  "message": "Successfully changed to BusinessRuleTask and added rejection rule",
  "processId": "proc-123",
  "requiresRefresh": true,
  "success": true
}
```

**Frontend Action:**
- Shows success message
- Reloads page to display updated BPMN

---

### **Example 2: Create Intent**

**User Message:** "Create a leave approval workflow with manager and HR review"

**Request:**
```json
{
  "message": "Create a leave approval workflow with manager and HR review",
  "processId": null,
  "selectedElementId": null
}
```

**AI Analysis:**
```json
{
  "intent": "CREATE",
  "confidence": 0.95,
  "reasoning": "No active process and user describes new workflow",
  "action": "start_interactive_creation"
}
```

**Backend Action:**
1. Routes to `InteractiveProcessService.startConversation()`
2. AI analyzes requirements
3. Generates clarifying questions

**Response:**
```json
{
  "intent": "CREATE",
  "action": "CLARIFICATION_NEEDED",
  "message": "I can help you create a leave approval workflow! Let me ask a few questions:",
  "questions": [
    "What is the maximum number of leave days allowed?",
    "Should HR review all requests or only requests above certain days?",
    "What happens if request is rejected at manager level?"
  ],
  "conversationId": "conv-789",
  "success": true
}
```

**Frontend Action:**
- Shows questions UI
- User answers questions
- Continues conversation until BPMN generated

---

### **Example 3: Question Intent**

**User Message:** "How do I add a gateway?"

**Request:**
```json
{
  "message": "How do I add a gateway?",
  "processId": "proc-123"
}
```

**AI Analysis:**
```json
{
  "intent": "QUESTION",
  "confidence": 0.92,
  "reasoning": "User asking for information/help",
  "action": "provide_information"
}
```

**Backend Action:**
1. Routes to `handleQuestionIntent()`
2. AI generates helpful answer

**Response:**
```json
{
  "intent": "QUESTION",
  "action": "INFORMATION_PROVIDED",
  "message": "To add a gateway, you can tell me where you want it in the process flow. For example: 'Add an exclusive gateway after the manager review to check if amount is greater than $5000'. I'll handle adding it to your BPMN model automatically!",
  "requiresRefresh": false,
  "success": true
}
```

---

## 🧪 **Testing**

### **Backend Health Check:**
```bash
curl http://localhost:8080/api/chat/health
# Response: "Chat service is healthy"
```

### **Test Edit Intent:**
```bash
curl -X POST http://localhost:8080/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Change this to a business rule task",
    "processId": "proc-123",
    "selectedElementId": "Task_1"
  }'
```

### **Test Create Intent:**
```bash
curl -X POST http://localhost:8080/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Create a leave approval process"
  }'
```

---

## 📊 **Metrics & Monitoring**

All requests log:
- Intent detected
- Confidence level
- Action taken
- Success/failure

**Log Format:**
```
[ChatService] Processing chat message: Change this to business rule task
[ChatService] Detected intent: EDIT for message: Change this to business rule task
[ChatService] Response: intent=EDIT, action=EDIT_APPLIED, success=true
```

---

## 🚀 **Deployment**

### **Running Backend:**
```bash
cd backend
./dev-with-autoreload.sh  # For development with hot-reload
```

### **Running Frontend:**
```bash
cd frontend
npm run dev
```

### **Access:**
- Frontend: http://localhost:5173
- Backend: http://localhost:8080
- Chat API: http://localhost:8080/api/chat/message

---

## 🎯 **Key Benefits of This Architecture**

1. **Intelligence Centralized in AI**
   - AI is best at understanding natural language
   - No need for frontend to "guess" user intent
   - Handles edge cases naturally

2. **Single Code Path**
   - Easier to maintain
   - Easier to debug
   - Consistent behavior

3. **Flexible & Extensible**
   - Add new intents easily (e.g., DELETE, EXPORT)
   - Change intent detection logic in one place
   - No frontend changes needed for new features

4. **Better User Experience**
   - Works with ANY phrasing
   - Understands context
   - Provides better error messages

---

## 🔮 **Future Enhancements**

1. **Conversation Context**
   - Remember previous messages in session
   - Support multi-turn edits
   - "Also, change the condition too"

2. **Batch Operations**
   - "Change all user tasks to service tasks"
   - "Add condition to all gateways"

3. **Intent Confidence Threshold**
   - If confidence < 0.7, ask for clarification
   - Learn from user corrections

4. **Analytics**
   - Track most common intents
   - Identify ambiguous patterns
   - Improve AI prompts based on data

---

## 📚 **Related Documentation**

- `END_TO_END_TESTING_GUIDE.md` - Testing procedures
- `INTERACTIVE_CONVERSATION_FEATURE.md` - Process creation flow
- `IMPLEMENTATION_COMPLETE.md` - Overall system architecture

---

**Date:** 2026-01-16  
**Author:** AI Assistant  
**Status:** ✅ Implemented and Tested
