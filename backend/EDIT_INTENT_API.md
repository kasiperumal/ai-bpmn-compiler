# Edit Intent API Documentation

## Overview

The Edit Intent API allows users to modify BPMN processes using natural language instructions. The system interprets these instructions using AI, applies changes to the canonical process model, and automatically regenerates the BPMN diagram.

## Architecture

### Components

1. **EditIntentRequest** - DTO for receiving edit requests
2. **EditIntentResponse** - DTO for edit operation results
3. **ProcessEditService** - Core service handling edit logic and AI interpretation
4. **ProcessLifecycleController** - REST endpoint exposing edit functionality

### Flow

```
User Input (Natural Language)
    ↓
POST /api/process/{processId}/edit-intent
    ↓
ProcessEditService
    ↓
AI Interpretation (Gemini)
    ↓
Parse Edit Commands (JSON)
    ↓
Apply to ProcessModel
    ↓
Save to Repository
    ↓
Regenerate BPMN (BpmnGeneratorService)
    ↓
Return Success/Failure Response
```

## API Endpoints

### 1. Process Edit Intent

**Endpoint:** `POST /api/process/{processId}/edit-intent`

**Description:** Submit a natural language instruction to edit a process element.

**Request Body:**
```json
{
  "instruction": "Rename this task to 'Review Application'",
  "nodeId": "task_1"
}
```

**Request Fields:**
- `instruction` (required): Natural language description of the desired change
- `nodeId` (optional): ID of the specific node to edit. If not provided, the instruction should be process-level.

**Response:**
```json
{
  "success": true,
  "message": "Edit applied successfully.",
  "modifiedNodeId": "task_1",
  "bpmnRegenerated": true
}
```

**Response Fields:**
- `success`: Boolean indicating if the edit was applied
- `message`: Human-readable description of the result
- `modifiedNodeId`: ID of the node that was modified
- `bpmnRegenerated`: Boolean indicating if BPMN was regenerated

**Status Codes:**
- `200 OK` - Edit applied successfully
- `400 Bad Request` - Invalid request or instruction
- `404 Not Found` - Process not found
- `500 Internal Server Error` - Server error during edit processing

### 2. Get Explanations

**Endpoint:** `GET /api/process/{processId}/explanations`

**Description:** Get AI-generated explanations for all nodes in a process.

**Response:**
```json
{
  "processId": "proc_123",
  "explanations": [
    {
      "nodeId": "task_1",
      "reason": "This task collects the initial request from the user and validates the basic information before proceeding to the approval stage.",
      "source": "AI Generated",
      "confidenceScore": 0.85,
      "timestamp": "2026-01-03T12:00:00"
    }
  ]
}
```

**Status Codes:**
- `200 OK` - Explanations retrieved successfully
- `404 Not Found` - Process not found
- `500 Internal Server Error` - Error generating explanations

## Supported Edit Operations

### 1. Rename Node

**Instruction Examples:**
- "Rename this task to 'Review Application'"
- "Change the name to 'Approve Request'"
- "Call this 'Send Notification'"

**Effect:** Updates the `name` property of the specified node.

**Edit Command (AI Generated):**
```json
{
  "action": "rename",
  "nodeId": "task_1",
  "newValue": "Review Application",
  "field": "name"
}
```

### 2. Update Condition

**Instruction Examples:**
- "Change the approval condition to amount > 10000"
- "Update condition to priority == 'HIGH'"
- "Set the gateway condition to status == 'APPROVED'"

**Effect:** Updates the `condition` property on outgoing edges from the specified node (typically gateways).

**Edit Command (AI Generated):**
```json
{
  "action": "update_condition",
  "nodeId": "gateway_1",
  "newValue": "amount > 10000",
  "field": "condition"
}
```

### 3. Update Description

**Instruction Examples:**
- "Add description: 'This task validates user credentials'"
- "Update description to 'Manager reviews and approves the request'"
- "Change description to 'Send email notification to user'"

**Effect:** Updates the `description` property in the node's properties map.

**Edit Command (AI Generated):**
```json
{
  "action": "update_description",
  "nodeId": "task_2",
  "newValue": "This task validates user credentials",
  "field": "description"
}
```

## AI Prompt Structure

### Edit Intent Prompt

The system sends a structured prompt to the AI model:

```
You are a BPMN process editor. Your task is to interpret natural language edit instructions
and generate structured edit commands in JSON format.

Current Process Model (JSON):
{...process model...}

Target Node ID: task_1

Edit Instruction: Rename this task to 'Review Application'

Generate a JSON object with the following structure:
{
  "action": "rename" | "update_condition" | "update_description" | "add_node" | "delete_node",
  "nodeId": "<node-id-to-modify>",
  "newValue": "<new-value>",
  "field": "name" | "condition" | "description"
}

Rules:
1. Only support safe edits: rename, condition updates, description updates
2. Do NOT change node types or process structure without explicit instruction
3. If the instruction is ambiguous, choose the most conservative interpretation
4. Return ONLY the JSON object, no additional text

JSON Edit Command:
```

### Explanation Generation Prompt

For generating node explanations:

```
You are a BPMN process analyst. Explain why this specific node exists in the process.

Process Model (JSON):
{...process model...}

Node to Explain:
- ID: task_1
- Name: Submit Request
- Type: TASK
- Description: User submits a leave request

Provide a brief, clear explanation (2-3 sentences) of:
1. What this node does in the process
2. Why it's necessary
3. Its relationship to other nodes

Keep the explanation concise, user-friendly, and avoid technical jargon.

Explanation:
```

## Implementation Details

### ProcessEditService

**Key Methods:**

1. **processEditIntent(String processId, EditIntentRequest request)**
   - Main entry point for edit operations
   - Validates input and fetches process model
   - Delegates to AI for interpretation
   - Applies changes and regenerates BPMN

2. **interpretEditIntent(ProcessModel processModel, EditIntentRequest request)**
   - Uses ChatClient to send prompt to AI
   - Returns structured edit commands as JSON

3. **applyEditCommands(ProcessModel processModel, String editCommandsJson, String targetNodeId)**
   - Parses AI response as EditCommand
   - Applies changes to process model
   - Returns true if modifications were made

4. **getExplanations(String processId)**
   - Generates explanations for all nodes in a process
   - Uses AI to create human-friendly descriptions

5. **generateNodeExplanation(ProcessModel processModel, ProcessNode node)**
   - Generates explanation for a single node
   - Returns concise description of node's purpose

### Error Handling

The service handles various error scenarios:

- **Invalid Instruction**: Returns failure response with message
- **Process Not Found**: Throws IllegalArgumentException (404)
- **Node Not Found**: Logs warning and returns false
- **JSON Parsing Error**: Logs error and returns false
- **BPMN Regeneration Error**: Returns partial success with error message
- **AI Service Error**: Catches exception and returns failure response

### Safety Considerations

The edit system prioritizes safety:

1. **Conservative Interpretation**: AI is instructed to choose conservative interpretations for ambiguous instructions
2. **Limited Operations**: Only rename, condition, and description updates are supported
3. **No Structural Changes**: Node types and process structure cannot be changed without explicit implementation
4. **Validation**: All changes are validated before being applied
5. **Regeneration**: BPMN is regenerated after each edit to ensure consistency

## Frontend Integration

### PropertiesPanel Component

The PropertiesPanel component provides the UI for edit operations:

**Features:**
- Display explanations for selected elements
- Edit interface with natural language input
- Real-time feedback on edit success/failure
- Automatic diagram refresh after edits

**Edit Workflow:**
1. User selects a BPMN element
2. Clicks "Edit Element" button
3. Enters natural language instruction
4. Clicks "Apply Edit"
5. System processes edit and reloads diagram
6. Success/failure message displayed

**Example Usage:**

```typescript
const handleEditSubmit = async () => {
  const response = await axios.post(
    `http://localhost:8080/api/process/${processId}/edit-intent`,
    {
      instruction: editInstruction,
      nodeId: selectedElement?.id || null
    }
  )
  
  if (response.data.success) {
    alert('Edit applied successfully!')
    // Reload diagram
  }
}
```

## Example Scenarios

### Scenario 1: Rename a Task

**User Action:** Selects "Submit Request" task and enters "Rename to 'Submit Leave Request'"

**API Request:**
```json
POST /api/process/proc_123/edit-intent
{
  "instruction": "Rename to 'Submit Leave Request'",
  "nodeId": "task_1"
}
```

**AI Interpretation:**
```json
{
  "action": "rename",
  "nodeId": "task_1",
  "newValue": "Submit Leave Request",
  "field": "name"
}
```

**Result:** Task renamed, BPMN regenerated

### Scenario 2: Update Gateway Condition

**User Action:** Selects approval gateway and enters "Change condition to amount > 10000"

**API Request:**
```json
POST /api/process/proc_123/edit-intent
{
  "instruction": "Change condition to amount > 10000",
  "nodeId": "gateway_1"
}
```

**AI Interpretation:**
```json
{
  "action": "update_condition",
  "nodeId": "gateway_1",
  "newValue": "amount > 10000",
  "field": "condition"
}
```

**Result:** Condition updated on all outgoing edges from gateway_1

### Scenario 3: Add Description

**User Action:** Selects task and enters "Add description: This task validates the user's credentials before proceeding"

**API Request:**
```json
POST /api/process/proc_123/edit-intent
{
  "instruction": "Add description: This task validates the user's credentials before proceeding",
  "nodeId": "task_2"
}
```

**AI Interpretation:**
```json
{
  "action": "update_description",
  "nodeId": "task_2",
  "newValue": "This task validates the user's credentials before proceeding",
  "field": "description"
}
```

**Result:** Description added to node properties

## Configuration

### AI Model Configuration

The edit intent system uses the same ChatClient configuration as the rest of the application:

```yaml
spring:
  ai:
    vertex:
      ai:
        gemini:
          project-id: ${VERTEX_AI_PROJECT_ID}
          location: ${VERTEX_AI_LOCATION}
          api-key: ${GEMINI_API_KEY}
          chat:
            options:
              model: gemini-2.0-flash-exp
              temperature: 0.7
              maxOutputTokens: 4096
```

### Logging

Enable debug logging for edit operations:

```yaml
logging:
  level:
    com.example.aibpmn.service.ProcessEditService: DEBUG
```

## Testing

### Manual Testing

1. Start the backend server
2. Create a process using the AI chat interface
3. In the frontend, select a BPMN element
4. Click "Edit Element"
5. Enter a natural language instruction
6. Verify the edit is applied correctly
7. Check that the BPMN diagram updates

### API Testing with cURL

**Edit Request:**
```bash
curl -X POST http://localhost:8080/api/process/proc_123/edit-intent \
  -H "Content-Type: application/json" \
  -d '{
    "instruction": "Rename this task to Review Application",
    "nodeId": "task_1"
  }'
```

**Get Explanations:**
```bash
curl http://localhost:8080/api/process/proc_123/explanations
```

## Limitations

Current limitations of the edit intent system:

1. **Structural Changes**: Cannot add or remove nodes (coming in future versions)
2. **AI Accuracy**: Depends on AI interpretation; ambiguous instructions may not work as expected
3. **Synchronous Operation**: Edit operations are synchronous and may take time for complex processes
4. **No Undo**: Changes are immediately persisted; no built-in undo mechanism
5. **Single Element**: Can only edit one element at a time

## Future Enhancements

Planned improvements:

1. **Bulk Edits**: Support editing multiple elements in one request
2. **Add/Remove Nodes**: Allow structural changes to the process
3. **Undo/Redo**: Implement version control for process edits
4. **Edit History**: Track and display edit history
5. **Batch Operations**: Support complex multi-step edits
6. **Validation Feedback**: Provide more detailed validation feedback before applying edits
7. **Edit Templates**: Pre-defined edit patterns for common operations

## Troubleshooting

### Common Issues

**Issue 1: Edit not applied**
- **Cause**: Invalid instruction or node ID
- **Solution**: Check logs for errors, ensure node ID is correct, try simpler instruction

**Issue 2: BPMN not regenerated**
- **Cause**: BPMN generation error after edit
- **Solution**: Check logs, manually trigger BPMN generation, verify model is valid

**Issue 3: AI returns unexpected command**
- **Cause**: Ambiguous instruction
- **Solution**: Use clearer, more specific language in instruction

**Issue 4: Diagram not refreshing**
- **Cause**: Frontend not reloading after edit
- **Solution**: Manually refresh page, check for JavaScript errors

## Security Considerations

1. **Input Validation**: All instructions are validated before processing
2. **Authorization**: Ensure proper authentication/authorization before allowing edits (to be implemented)
3. **Rate Limiting**: Consider rate limiting edit requests to prevent abuse
4. **Audit Trail**: Log all edit operations for security and compliance

## Performance

- **Edit Operation**: ~2-5 seconds (includes AI interpretation and BPMN regeneration)
- **Explanation Generation**: ~1-3 seconds per node
- **Optimization**: Consider caching explanations and using background jobs for complex edits

## Conclusion

The Edit Intent API provides a user-friendly way to modify BPMN processes using natural language, powered by AI. It maintains process integrity while allowing flexible, intuitive edits without requiring deep BPMN knowledge.

