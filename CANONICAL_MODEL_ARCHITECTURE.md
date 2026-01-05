# Canonical Model Architecture

## Overview

The AI BPMN Compiler follows a **Canonical Model Architecture** where the `ProcessModel` serves as the single source of truth for all process definitions. BPMN XML is **always generated** from this canonical model and **never saved directly**.

## Core Principle

```
┌─────────────────────────────────────────────────────────────┐
│                   CANONICAL MODEL                            │
│              (ProcessModel - Source of Truth)                │
│                                                              │
│  ┌────────┐  ┌────────┐  ┌────────┐  ┌───────────┐         │
│  │ Nodes  │  │ Edges  │  │ Rules  │  │Properties │         │
│  └────────┘  └────────┘  └────────┘  └───────────┘         │
└─────────────────────────────────────────────────────────────┘
                          │
                          │ ALWAYS GENERATES
                          ▼
         ┌────────────────────────────────┐
         │        BPMN XML                │
         │    (Generated Artifact)        │
         │                                │
         │  - Never saved directly        │
         │  - Always regenerated          │
         │  - Read-only in frontend       │
         └────────────────────────────────┘
```

## Why Canonical Model?

### 1. **Single Source of Truth**
- ProcessModel is the authoritative representation
- No ambiguity about current process state
- Eliminates synchronization issues

### 2. **AI-Driven Workflows**
- Natural language process creation
- AI interprets user intent and updates model
- Enables intelligent process generation

### 3. **Business Rule Integration**
- Rules (DRL) generated from same model
- Consistent rule application
- Validation and constraints enforced

### 4. **Prevents Model Drift**
- BPMN XML always matches ProcessModel
- No manual BPMN edits that diverge from model
- Referential integrity maintained

### 5. **Complex Transformations**
- Process optimizations
- Layout calculations
- Semantic validation

## Architecture Components

### Backend: Canonical Model (Java)

```java
ProcessModel (Canonical)
    ├── List<ProcessNode> nodes
    ├── List<ProcessEdge> edges
    ├── List<RuleModel> rules
    └── ProcessStatus status

    ↓ (Generates)

BpmnGeneratorService
    └── generateBpmn(ProcessModel) → String (BPMN XML)

DrlGeneratorService
    └── generateDrl(List<RuleModel>) → String (DRL)
```

### Frontend: Read-Only BPMN (React/TypeScript)

```typescript
BpmnViewer (NOT BpmnModeler)
    ├── Display BPMN (read-only)
    ├── Element selection (for context)
    ├── Zoom/pan navigation
    └── Export (download only)

    ✗ NEVER saves BPMN to backend
    ✗ NEVER allows direct editing
    ✗ NEVER modifies process structure
```

## Edit Workflow

### User Initiates Edit

```
1. User Action
   │
   ├─→ Selects element in BPMN diagram
   │   (Read-only viewer, selection for context)
   │
   ├─→ Types natural language instruction
   │   "Rename this to 'Approve Request'"
   │
   └─→ Submits via ChatPanel or PropertiesPanel
```

### Backend Processing

```
2. Edit Intent API
   │
   POST /api/process/{processId}/edit-intent
   {
     instruction: "Rename this to 'Approve Request'",
     nodeId: "task_1"
   }
   │
   ▼
3. ProcessEditService
   │
   ├─→ AI interprets instruction (Gemini)
   │   Returns: { action: "rename", nodeId: "task_1", newValue: "Approve Request" }
   │
   ├─→ Apply to canonical ProcessModel
   │   ProcessNode.setName("Approve Request")
   │
   ├─→ Save updated ProcessModel
   │   processModelRepository.save(processModel)
   │
   └─→ Regenerate BPMN from updated model
       BpmnGeneratorService.generateBpmn(processModel)
```

### Frontend Refresh

```
4. Frontend Reload
   │
   ├─→ Receives success response
   │   { success: true, bpmnRegenerated: true }
   │
   ├─→ Page refreshes (or diagram reloads)
   │
   └─→ BpmnViewer fetches regenerated BPMN
       GET /api/process/{processId}/bpmn
       (Returns BPMN generated from canonical model)
```

## Code Enforcement

### Backend Guards

```java
/**
 * ProcessEditService
 * 
 * RULE: All edits must update the canonical ProcessModel first,
 * then regenerate BPMN. Never accept BPMN XML as input for edits.
 */
public EditIntentResponse processEditIntent(String processId, EditIntentRequest request) {
    // 1. Fetch canonical model
    ProcessModel model = processModelRepository.findById(processId)
                            .orElseThrow(...);
    
    // 2. AI interprets natural language instruction
    String editCommands = interpretEditIntent(model, request);
    
    // 3. Apply changes to canonical model
    boolean modified = applyEditCommands(model, editCommands, ...);
    
    // 4. Save canonical model
    processModelRepository.save(model);
    
    // 5. Regenerate BPMN from canonical model
    String bpmn = bpmnGeneratorService.generateBpmn(model);
    
    return new EditIntentResponse(true, "Edit applied, BPMN regenerated");
}
```

### Frontend Guards

```typescript
/**
 * BpmnDiagram Component
 * 
 * ARCHITECTURAL GUARD: Uses BpmnViewer (read-only), NOT BpmnModeler
 * This prevents accidental direct BPMN editing.
 */
import BpmnViewer from 'bpmn-js/lib/Viewer'  // ✓ Correct (read-only)
// import BpmnModeler from 'bpmn-js/lib/Modeler'  // ✗ NEVER use this

const viewer = new BpmnViewer({
  container: containerRef.current,
  // Read-only: no editing capabilities
})

/**
 * Export Function
 * 
 * CRITICAL: This downloads BPMN for user, but NEVER saves to backend.
 * It's a snapshot of the generated BPMN from canonical model.
 */
const handleExport = async () => {
  const { xml } = await viewerRef.current.saveXML({ format: true })
  
  // LOCAL download only (not backend save)
  const blob = new Blob([xml], { type: 'application/xml' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${processId}.bpmn`
  a.click()
  
  // ✗ NEVER: axios.post('/api/process/save-bpmn', { xml })
}
```

## API Contracts

### Endpoints That Follow Architecture

✓ **Process Creation**
```
POST /api/process/start
{
  description: "Create a leave approval process..."
}
Response: { processId: "proc-123" }

Backend:
1. AI creates canonical ProcessModel
2. Generates BPMN from model
3. Returns processId
```

✓ **Edit Intent**
```
POST /api/process/{processId}/edit-intent
{
  instruction: "Rename to 'Review Application'",
  nodeId: "task_1"
}
Response: { success: true, bpmnRegenerated: true }

Backend:
1. AI interprets instruction
2. Updates canonical ProcessModel
3. Regenerates BPMN from model
```

✓ **Get BPMN**
```
GET /api/process/{processId}/bpmn
Response: <bpmn XML>

Backend:
1. Fetches canonical ProcessModel
2. Generates BPMN on-the-fly (or returns cached)
3. Returns generated BPMN XML
```

### Endpoints That Violate Architecture

✗ **NEVER Implement These**

```
// ✗ WRONG: Accepting BPMN XML as input
POST /api/process/{processId}/save-bpmn
{
  bpmnXml: "<bpmn:definitions>...</bpmn:definitions>"
}

// ✗ WRONG: Updating process without canonical model
PUT /api/process/{processId}/bpmn
{
  bpmnXml: "..."
}

// ✗ WRONG: Direct BPMN modifications
PATCH /api/process/{processId}/element/{elementId}
{
  name: "New Name"  // Should go through canonical model
}
```

## Development Guidelines

### For Backend Developers

1. **Always Update Canonical Model First**
   ```java
   // ✓ Correct
   node.setName("New Name");
   processModelRepository.save(model);
   String bpmn = bpmnGeneratorService.generateBpmn(model);
   
   // ✗ Wrong
   String bpmn = loadBpmnXml();
   bpmn = bpmn.replace("OldName", "NewName");
   saveBpmnXml(bpmn);
   ```

2. **Never Accept BPMN XML as Edit Input**
   ```java
   // ✗ NEVER do this
   public void updateProcess(String processId, String bpmnXml) {
       // Violates canonical model principle
   }
   ```

3. **Always Regenerate BPMN After Model Changes**
   ```java
   // After any model modification
   String bpmn = bpmnGeneratorService.generateBpmn(processModel);
   ```

### For Frontend Developers

1. **Always Use BpmnViewer, Never BpmnModeler**
   ```typescript
   // ✓ Correct
   import BpmnViewer from 'bpmn-js/lib/Viewer'
   const viewer = new BpmnViewer({ ... })
   
   // ✗ Wrong
   import BpmnModeler from 'bpmn-js/lib/Modeler'
   const modeler = new BpmnModeler({ ... })
   ```

2. **Never Save BPMN to Backend**
   ```typescript
   // ✗ NEVER do this
   const { xml } = await modeler.saveXML()
   await axios.post('/api/process/save-bpmn', { xml })
   ```

3. **All Edits Through Edit Intent API**
   ```typescript
   // ✓ Correct
   await axios.post(`/api/process/${processId}/edit-intent`, {
     instruction: "Rename to 'New Name'",
     nodeId: selectedElement.id
   })
   
   // ✗ Wrong
   selectedElement.businessObject.name = "New Name"
   await modeler.saveXML()
   ```

4. **Export is Download Only**
   ```typescript
   // ✓ Correct: Download for user
   const { xml } = await viewer.saveXML()
   downloadFile(xml, 'process.bpmn')
   
   // ✗ Wrong: Saving to backend
   await axios.post('/api/save', { xml })
   ```

## Benefits of This Architecture

### 1. Data Integrity
- Single source of truth eliminates conflicts
- No risk of BPMN/model inconsistencies
- Changes are atomic and validated

### 2. AI Integration
- Natural language instructions can modify model
- Complex transformations without manual BPMN editing
- Intelligent process optimization

### 3. Business Logic Enforcement
- Rules and validations applied consistently
- DRL generation from same model
- Semantic constraints enforced

### 4. Maintainability
- Clear separation of concerns
- Model changes don't break BPMN
- Easy to add new features (just update model)

### 5. Auditability
- All changes tracked in canonical model
- Complete change history
- Easy rollback and versioning

## Migration from Direct BPMN Editing

If you have existing code that edits BPMN directly:

### Before (Wrong)
```typescript
// Direct BPMN manipulation
const modeling = modeler.get('modeling')
modeling.updateProperties(element, {
  name: 'New Name'
})
const { xml } = await modeler.saveXML()
await axios.post('/api/save-bpmn', { xml })
```

### After (Correct)
```typescript
// Edit through canonical model
await axios.post(`/api/process/${processId}/edit-intent`, {
  instruction: "Rename this to 'New Name'",
  nodeId: element.id
})
// Backend updates model and regenerates BPMN
// Frontend reloads regenerated BPMN
```

## Testing Checklist

### Architectural Compliance Tests

- [ ] BpmnViewer is used (not BpmnModeler)
- [ ] No direct BPMN save endpoints exist
- [ ] All edits go through edit-intent API
- [ ] BPMN is regenerated after every model change
- [ ] Export function downloads only (doesn't save)
- [ ] Frontend shows "Read-Only" indicator
- [ ] Dev mode shows architectural reminder

### Integration Tests

- [ ] Create process → Canonical model created
- [ ] Edit element → Model updated, BPMN regenerated
- [ ] Reload diagram → Shows regenerated BPMN
- [ ] Export BPMN → Downloads current snapshot
- [ ] No way to upload BPMN for editing

## Troubleshooting

### Issue: "I want to enable direct BPMN editing"

**Answer**: This violates the canonical model architecture. Instead:
1. Identify what needs to be edited
2. Add support to canonical ProcessModel
3. Implement in edit-intent API
4. BPMN will be regenerated automatically

### Issue: "Export doesn't reflect my changes"

**Answer**: Ensure you're editing through canonical model:
1. Use edit-intent API for changes
2. Wait for BPMN regeneration
3. Reload diagram
4. Then export

### Issue: "Can I import BPMN from external source?"

**Answer**: Yes, but it must be converted to canonical model first:
1. Parse BPMN XML
2. Extract process structure
3. Create ProcessModel
4. Let backend regenerate BPMN from model

## Conclusion

The Canonical Model Architecture ensures:
- **Data Integrity**: Single source of truth
- **AI Integration**: Natural language workflows
- **Maintainability**: Clear separation of concerns
- **Scalability**: Easy to add features
- **Reliability**: No model drift or inconsistencies

By following these principles, we create a robust, AI-powered BPMN compilation system that's both powerful and maintainable.

---

**Remember**: BPMN is a **generated artifact**, not a data store. The canonical ProcessModel is the source of truth. All edits flow through the model, and BPMN is always regenerated.

