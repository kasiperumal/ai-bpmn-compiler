# ChatPanel Enhanced Features

## Overview

The ChatPanel component has been enhanced with three powerful features that make it context-aware, enable direct editing through natural language, and provide real-time streaming responses.

## Feature 1: Context-Aware (Selected Node)

### Description
The ChatPanel is now aware of which BPMN element the user has selected in the diagram. This context is displayed prominently and influences how the assistant responds.

### Implementation

**Props:**
```typescript
interface ChatPanelProps {
  onProcessCreated?: (processId: string) => void
  selectedElement?: any        // NEW: Selected BPMN element
  processId?: string            // NEW: Current process ID
}
```

**Context Display:**
When an element is selected, a context banner appears showing:
- 🎯 Icon indicator
- Element name (e.g., "Submit Request")
- Element type (e.g., "bpmn:Task")

**Visual Example:**
```
┌─────────────────────────────────────────┐
│ 🎯 Selected: Submit Request (bpmn:Task) │
└─────────────────────────────────────────┘
```

**Behavior:**
- The input placeholder changes to context-aware text:
  - With selection: "Edit 'Submit Request'..."
  - Without selection: "Describe your process or edit an element..."
- The assistant provides context-aware suggestions
- Edit intents automatically target the selected element

### Usage

```typescript
// In App.tsx
const [selectedElement, setSelectedElement] = useState<any>(null)
const [processId, setProcessId] = useState<string>('')

<ChatPanel 
  onProcessCreated={setProcessId}
  selectedElement={selectedElement}
  processId={processId}
/>
```

## Feature 2: Send Edit Instructions to Backend

### Description
Users can edit BPMN elements directly through natural language in the chat interface. The system detects edit intent, sends it to the backend, and automatically refreshes the diagram.

### Edit Intent Detection

The system automatically detects edit intents using keyword matching:

**Trigger Keywords:**
- rename, change, update, modify, edit, set
- call it, name it, change to, update to
- condition, description

**Detection Logic:**
```typescript
const detectEditIntent = (message: string): boolean => {
  const editKeywords = [
    'rename', 'change', 'update', 'modify', 'edit', 'set',
    'call it', 'name it', 'change to', 'update to',
    'condition', 'description'
  ]
  
  const lowerMessage = message.toLowerCase()
  return editKeywords.some(keyword => lowerMessage.includes(keyword)) && 
         (selectedElement !== null || lowerMessage.includes('selected') || lowerMessage.includes('this'))
}
```

### Supported Edit Commands

**1. Rename Element:**
```
User: "Rename this to 'Approve Request'"
System: ✅ Edit applied successfully!
```

**2. Update Condition:**
```
User: "Change the condition to amount > 5000"
System: ✅ Edit applied successfully!
```

**3. Update Description:**
```
User: "Add description: This task validates user credentials"
System: ✅ Edit applied successfully!
```

### Edit Workflow

```
1. User selects element in diagram
   ↓
2. Context appears in ChatPanel
   ↓
3. User types edit instruction
   "Rename this to 'Review Application'"
   ↓
4. System detects edit intent
   ↓
5. POST /api/process/{id}/edit-intent
   {
     instruction: "Rename this to 'Review Application'",
     nodeId: "task_1"
   }
   ↓
6. Backend interprets with AI
   ↓
7. Changes applied to ProcessModel
   ↓
8. BPMN regenerated
   ↓
9. Success message shown
   ↓
10. Page refreshes with updated diagram
```

### Backend Integration

```typescript
const handleEditIntent = async (instruction: string) => {
  if (!currentProcessId) {
    addMessage('system', 'Please create or load a process first.')
    return
  }

  try {
    const response = await axios.post(
      `http://localhost:8080/api/process/${currentProcessId}/edit-intent`,
      {
        instruction,
        nodeId: selectedElement?.id || null
      }
    )

    if (response.data.success) {
      addMessage('assistant', `✅ Edit applied successfully!`)
      addMessage('system', 'The BPMN diagram has been regenerated. Refreshing...')
      setTimeout(() => window.location.reload(), 1500)
    } else {
      addMessage('assistant', `❌ Edit failed: ${response.data.message}`)
    }
  } catch (err: any) {
    addMessage('system', `Error: ${err.message}`)
  }
}
```

### User Feedback

**Success:**
```
🤖 ✅ Edit applied successfully! Edit applied successfully.
ℹ️ The BPMN diagram has been regenerated. Refreshing...
```

**Failure:**
```
🤖 ❌ Edit failed: Node not found
```

**Context Shown:**
```
ℹ️ Editing: Submit Request (bpmn:Task)
```

## Feature 3: Stream AI Responses

### Description
AI responses are now streamed in real-time, showing text as it's generated rather than waiting for the complete response. This provides better user experience and perceived performance.

### Implementation

**Streaming State:**
```typescript
const [streamingMessageId, setStreamingMessageId] = useState<string | null>(null)
const abortControllerRef = useRef<AbortController | null>(null)
```

**Streaming Function:**
```typescript
const streamAIResponse = async (endpoint: string, payload: any): Promise<string> => {
  // Create abort controller for cancellation
  abortControllerRef.current = new AbortController()
  
  // Create streaming message with cursor
  const messageId = addMessage('assistant', '', true)
  setStreamingMessageId(messageId)
  
  let fullContent = ''

  try {
    const response = await fetch(`http://localhost:8080${endpoint}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
      signal: abortControllerRef.current.signal
    })

    const reader = response.body?.getReader()
    const decoder = new TextDecoder()

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      const chunk = decoder.decode(value, { stream: true })
      fullContent += chunk
      
      // Update message in real-time
      updateStreamingMessage(messageId, fullContent)
    }

    finalizeStreamingMessage(messageId)
    return fullContent

  } catch (error: any) {
    if (error.name === 'AbortError') {
      addMessage('system', 'Response streaming was cancelled.')
    }
    finalizeStreamingMessage(messageId)
    throw error
  }
}
```

### Visual Indicators

**Streaming Cursor:**
A blinking cursor (▊) appears at the end of the streaming text to indicate active generation.

```css
.streaming-cursor {
  display: inline-block;
  margin-left: 2px;
  animation: blink 1s infinite;
  color: #3b82f6;
  font-weight: bold;
}

@keyframes blink {
  0%, 49% { opacity: 1; }
  50%, 100% { opacity: 0; }
}
```

**Stop Button:**
While streaming, a red "⏸ Stop" button appears allowing users to cancel the generation:

```typescript
{streamingMessageId && (
  <button 
    onClick={stopStreaming}
    className="btn-stop-streaming"
  >
    ⏸ Stop
  </button>
)}
```

### Message States

**Message Interface:**
```typescript
interface Message {
  id: string
  type: 'user' | 'assistant' | 'system'
  content: string
  timestamp: Date
  streaming?: boolean    // NEW: Indicates streaming state
}
```

**State Management:**
```typescript
// Add streaming message
const messageId = addMessage('assistant', '', true)

// Update during streaming
updateStreamingMessage(messageId, 'Partial content...')

// Finalize when complete
finalizeStreamingMessage(messageId)
```

### Fallback Behavior

If streaming is not supported by the backend (or fails), the system automatically falls back to regular request-response:

```typescript
try {
  await streamAIResponse('/api/process/start', { description: userMessage })
} catch (streamErr) {
  console.warn('Streaming not available, falling back to regular request')
  
  // Use traditional axios POST
  const response = await axios.post('http://localhost:8080/api/process/start', {
    description: userMessage
  })
  // Handle response...
}
```

### User Experience

**Before (No Streaming):**
```
User: "Create a leave approval process"
      ... 5 seconds wait ...
🤖: "I've created your process with 5 tasks..."
```

**After (With Streaming):**
```
User: "Create a leave approval process"
🤖: "I've creat▊"
🤖: "I've created your pr▊"
🤖: "I've created your process with 5▊"
🤖: "I've created your process with 5 tasks..."
```

**With Stop Button:**
```
┌───────────────────────────────┐
│ ⏸ Stop                        │
└───────────────────────────────┘
```

## Combined Feature Demonstration

### Scenario: Context-Aware Edit with Streaming

1. **User selects "Submit Request" task in diagram**
   ```
   ┌─────────────────────────────────────────┐
   │ 🎯 Selected: Submit Request (bpmn:Task) │
   └─────────────────────────────────────────┘
   ```

2. **User types in chat:**
   ```
   👤: "Rename this to 'Submit Leave Request'"
   ```

3. **System detects edit intent and shows context:**
   ```
   ℹ️: Editing: Submit Request (bpmn:Task)
   ```

4. **System processes edit (with streaming response if available):**
   ```
   🤖: "I'm ren▊"
   🤖: "I'm renaming the task to▊"
   🤖: "I'm renaming the task to 'Submit Leave Request'..."
   ```

5. **Success confirmation:**
   ```
   🤖: ✅ Edit applied successfully!
   ℹ️: The BPMN diagram has been regenerated. Refreshing...
   ```

6. **Diagram refreshes automatically**

## Integration Points

### App.tsx Integration

```typescript
function App() {
  const [selectedElement, setSelectedElement] = useState<any>(null)
  const [processId, setProcessId] = useState<string>('')

  return (
    <div className="app-container">
      {/* ... */}
      <BpmnDiagram 
        onElementSelect={setSelectedElement}  // Provides selection
        processId={processId}
      />
      
      <PropertiesPanel 
        selectedElement={selectedElement}
        processId={processId}
      />
      
      <ChatPanel 
        onProcessCreated={setProcessId}
        selectedElement={selectedElement}      // NEW
        processId={processId}                  // NEW
      />
    </div>
  )
}
```

### API Endpoints Used

1. **Edit Intent:**
   - `POST /api/process/{processId}/edit-intent`
   - Payload: `{ instruction, nodeId }`
   - Response: `{ success, message, modifiedNodeId, bpmnRegenerated }`

2. **Process Creation (with potential streaming):**
   - `POST /api/process/start`
   - Payload: `{ description }`
   - Response: Streamed or JSON

3. **Questions:**
   - `GET /api/process/{processId}/questions`

4. **Resume:**
   - `POST /api/process/{processId}/resume`

## Benefits

### 1. Context-Aware
- ✅ Users can see what they're editing
- ✅ No ambiguity about target element
- ✅ Context-sensitive suggestions
- ✅ Improved user experience

### 2. Edit Instructions
- ✅ Natural language editing (no technical knowledge required)
- ✅ Direct manipulation through chat
- ✅ Immediate feedback
- ✅ Automatic diagram updates
- ✅ Reduces need for technical BPMN editors

### 3. Streaming Responses
- ✅ Perceived better performance
- ✅ Real-time feedback
- ✅ User can start reading before completion
- ✅ Ability to stop long responses
- ✅ Modern, responsive UI
- ✅ Graceful fallback to non-streaming

## Configuration

### Streaming Timeout
To configure streaming behavior:

```typescript
// In ChatPanel.tsx
const STREAMING_TIMEOUT = 30000 // 30 seconds

const response = await fetch(url, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(payload),
  signal: abortControllerRef.current.signal,
  // Add timeout if needed
})
```

### Edit Intent Keywords
To customize edit detection:

```typescript
const editKeywords = [
  'rename', 'change', 'update', 'modify', 'edit', 'set',
  'call it', 'name it', 'change to', 'update to',
  'condition', 'description',
  // Add custom keywords here
]
```

## Testing

### Manual Testing Checklist

**Context-Aware:**
- [ ] Select element in diagram
- [ ] Verify context banner appears
- [ ] Verify placeholder text changes
- [ ] Verify assistant provides context-aware responses

**Edit Instructions:**
- [ ] Select element
- [ ] Send rename instruction
- [ ] Verify edit is applied
- [ ] Verify diagram refreshes
- [ ] Test condition update
- [ ] Test description update
- [ ] Test error handling (invalid instruction)

**Streaming:**
- [ ] Send long instruction
- [ ] Verify streaming cursor appears
- [ ] Verify text streams in real-time
- [ ] Test stop button during streaming
- [ ] Verify fallback to non-streaming works

### Example Test Cases

**Test 1: Context-Aware Rename**
```
1. Select "Approve Request" task
2. Type: "Rename to 'Manager Approval'"
3. Expected: Edit applied, diagram updated
```

**Test 2: Condition Update**
```
1. Select approval gateway
2. Type: "Change condition to amount > 10000"
3. Expected: Condition updated on edges
```

**Test 3: Streaming Stop**
```
1. Send process creation request
2. Click "Stop" during streaming
3. Expected: Streaming cancelled, partial message shown
```

## Troubleshooting

### Issue 1: Context Not Showing
**Symptom:** Selected element not displayed in ChatPanel
**Solution:** 
- Ensure `selectedElement` prop is passed from App.tsx
- Check that BpmnDiagram's `onElementSelect` callback is working
- Verify element selection in browser console

### Issue 2: Edit Not Working
**Symptom:** Edit instruction sent but no changes
**Solution:**
- Check browser console for errors
- Verify processId is set
- Ensure element is selected
- Check backend logs for edit-intent endpoint
- Verify instruction contains edit keywords

### Issue 3: Streaming Not Working
**Symptom:** No streaming, or immediate full response
**Solution:**
- Check if backend supports streaming (look for chunked transfer)
- Verify fetch API is available (all modern browsers)
- Check for CORS issues
- System automatically falls back to non-streaming if unavailable

### Issue 4: Page Refresh After Edit
**Symptom:** Page refreshes but diagram not updated
**Solution:**
- Verify BPMN was actually regenerated (check backend logs)
- Check that processId matches
- Ensure BpmnDiagram component reloads on processId change
- Consider implementing a better refresh mechanism (event-based)

## Future Enhancements

1. **Better Refresh Mechanism:**
   - Replace `window.location.reload()` with event-based refresh
   - Only reload the diagram, not the entire page
   - Preserve chat history across refreshes

2. **Streaming Backend Support:**
   - Implement Server-Sent Events (SSE) on backend
   - Add chunked response support for AI responses
   - Provide progress indicators for long operations

3. **Multi-Element Editing:**
   - Support editing multiple selected elements
   - Batch edit operations
   - Undo/redo functionality

4. **Smart Suggestions:**
   - Context-aware auto-complete for edit instructions
   - Show edit examples based on element type
   - Suggest common edits for selected element

5. **Voice Input:**
   - Add speech-to-text for edit instructions
   - Hands-free editing capability

## Conclusion

The enhanced ChatPanel provides a powerful, intuitive interface for interacting with BPMN processes. The combination of context awareness, natural language editing, and streaming responses creates a modern, responsive user experience that lowers the barrier to entry for BPMN process creation and editing.

Users can now:
- See what they're editing (context-aware)
- Edit through natural conversation (no technical knowledge needed)
- Get real-time feedback (streaming responses)
- Stop long operations (cancellable streaming)

These features work together seamlessly to create a truly AI-powered BPMN editing experience.

