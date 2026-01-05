import { useEffect, useRef, useState } from 'react'
import BpmnViewer from 'bpmn-js/lib/Viewer'
import 'bpmn-js/dist/assets/diagram-js.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn-embedded.css'
import './BpmnDiagram.css'
import axios from 'axios'

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * ARCHITECTURAL PRINCIPLE: READ-ONLY BPMN VIEWER
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * This component uses BpmnViewer (NOT BpmnModeler) to enforce read-only access.
 * 
 * CRITICAL RULES:
 * 1. BPMN.js NEVER saves BPMN XML to the backend
 * 2. All edits MUST go through the canonical ProcessModel on the backend
 * 3. BPMN is ALWAYS regenerated from the canonical model
 * 4. This component is STRICTLY for visualization only
 * 
 * WORKFLOW FOR EDITS:
 * 1. User selects element → Triggers onElementSelect callback
 * 2. User describes change in ChatPanel or PropertiesPanel
 * 3. Natural language instruction sent to backend API
 * 4. Backend AI interprets instruction
 * 5. Backend modifies canonical ProcessModel
 * 6. Backend regenerates BPMN from ProcessModel
 * 7. Frontend reloads BPMN (this component)
 * 
 * WHY READ-ONLY?
 * - Ensures single source of truth (canonical model)
 * - Prevents model drift between BPMN XML and ProcessModel
 * - Enables AI-driven process generation and modification
 * - Maintains referential integrity and validation rules
 * - Allows for complex business logic and rule generation
 * 
 * WHAT THIS COMPONENT DOES:
 * ✓ Display BPMN diagrams (read-only)
 * ✓ Allow element selection (for context)
 * ✓ Provide zoom/pan navigation
 * ✓ Export BPMN for external use (download only, not save)
 * 
 * WHAT THIS COMPONENT DOES NOT DO:
 * ✗ Edit BPMN elements directly
 * ✗ Save BPMN changes to backend
 * ✗ Modify process structure
 * ✗ Allow drag-and-drop editing
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 */

interface BpmnDiagramProps {
  onElementSelect?: (element: any) => void
  processId?: string
}

const EMPTY_BPMN = `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                  xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
                  id="Definitions_1"
                  targetNamespace="http://bpmn.io/schema/bpmn">
  <bpmn:process id="Process_1" isExecutable="true">
    <bpmn:startEvent id="StartEvent_1"/>
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="Process_1">
      <bpmndi:BPMNShape id="StartEvent_1_di" bpmnElement="StartEvent_1">
        <dc:Bounds x="173" y="102" width="36" height="36"/>
      </bpmndi:BPMNShape>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>`

const BpmnDiagram = ({ onElementSelect, processId }: BpmnDiagramProps) => {
  const containerRef = useRef<HTMLDivElement>(null)
  const viewerRef = useRef<any>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!containerRef.current) return

    // ═══════════════════════════════════════════════════════════════════════
    // ARCHITECTURAL GUARD: Use BpmnViewer (read-only) NOT BpmnModeler
    // ═══════════════════════════════════════════════════════════════════════
    // BpmnViewer provides read-only display without editing capabilities.
    // This enforces our architectural principle of canonical model as source.
    // ═══════════════════════════════════════════════════════════════════════
    
    const viewer = new BpmnViewer({
      container: containerRef.current,
      keyboard: {
        bindTo: document
      }
    })

    viewerRef.current = viewer

    // Load empty diagram initially
    viewer.importXML(EMPTY_BPMN).catch((err: any) => {
      console.error('Error loading BPMN diagram:', err)
      setError('Failed to initialize BPMN diagram')
    })

    // ═══════════════════════════════════════════════════════════════════════
    // ELEMENT SELECTION: Read-only interaction for context
    // ═══════════════════════════════════════════════════════════════════════
    // Selection is allowed to provide context for edits, but selection alone
    // does not enable direct BPMN modification. The selected element is passed
    // to parent components (ChatPanel, PropertiesPanel) which send edit
    // instructions to the backend API for canonical model updates.
    // ═══════════════════════════════════════════════════════════════════════
    
    const eventBus = viewer.get('eventBus') as any
    eventBus.on('element.click', (event: any) => {
      if (onElementSelect) {
        onElementSelect(event.element)
      }
    })

    // Cleanup
    return () => {
      viewer.destroy()
    }
  }, [onElementSelect])

  // ═══════════════════════════════════════════════════════════════════════
  // BPMN LOADING: Always from backend canonical model
  // ═══════════════════════════════════════════════════════════════════════
  // BPMN is fetched from the backend, where it was generated from the
  // canonical ProcessModel. This ensures we always display the authoritative
  // representation of the process.
  // 
  // The backend endpoint /api/process/{processId}/bpmn returns BPMN XML
  // that was generated by BpmnGeneratorService from the ProcessModel.
  // ═══════════════════════════════════════════════════════════════════════
  
  useEffect(() => {
    if (!processId || !viewerRef.current) return

    const loadBpmn = async () => {
      setLoading(true)
      setError(null)
      try {
        // Fetch BPMN from backend (generated from canonical model)
        const response = await axios.get(`http://localhost:8080/api/process/${processId}/bpmn`, {
          responseType: 'text'
        })
        
        // Import XML into viewer (read-only display)
        await viewerRef.current.importXML(response.data)
        
        console.info('[BPMN Viewer] Loaded BPMN from canonical model for process:', processId)
      } catch (err: any) {
        console.error('[BPMN Viewer] Error loading BPMN:', err)
        setError(err.response?.data?.message || 'Failed to load BPMN diagram')
      } finally {
        setLoading(false)
      }
    }

    loadBpmn()
  }, [processId])

  // ═══════════════════════════════════════════════════════════════════════
  // EXPORT FUNCTION: Download only, NEVER saves to backend
  // ═══════════════════════════════════════════════════════════════════════
  // This function exports the current BPMN XML to a local file for the user.
  // 
  // IMPORTANT: This is for USER download only. It does NOT save changes back
  // to the backend. Any modifications must go through the canonical model
  // via the edit-intent API.
  // 
  // The exported BPMN is a snapshot of what was generated from the canonical
  // model. Users can download it for external use, documentation, or import
  // into other BPMN tools, but it is NOT a mechanism for editing.
  // ═══════════════════════════════════════════════════════════════════════
  
  const handleExport = async () => {
    if (!viewerRef.current) return

    try {
      // Extract current BPMN XML from viewer
      const { xml } = await viewerRef.current.saveXML({ format: true })
      
      // Create download (LOCAL file, NOT backend save)
      const blob = new Blob([xml], { type: 'application/xml' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `${processId || 'process'}.bpmn`
      a.click()
      URL.revokeObjectURL(url)
      
      console.info('[BPMN Viewer] Exported BPMN to local file (read-only snapshot)')
    } catch (err) {
      console.error('[BPMN Viewer] Error exporting BPMN:', err)
      setError('Failed to export BPMN diagram')
    }
  }

  // ═══════════════════════════════════════════════════════════════════════
  // NAVIGATION CONTROLS: Read-only zoom and pan
  // ═══════════════════════════════════════════════════════════════════════
  // These controls allow users to navigate the diagram but do not enable
  // editing capabilities. They are pure visualization aids.
  // ═══════════════════════════════════════════════════════════════════════

  const handleZoomIn = () => {
    if (!viewerRef.current) return
    const zoomScroll = viewerRef.current.get('zoomScroll')
    zoomScroll.stepZoom(1)
  }

  const handleZoomOut = () => {
    if (!viewerRef.current) return
    const zoomScroll = viewerRef.current.get('zoomScroll')
    zoomScroll.stepZoom(-1)
  }

  const handleZoomReset = () => {
    if (!viewerRef.current) return
    const canvas = viewerRef.current.get('canvas')
    canvas.zoom('fit-viewport')
  }

  return (
    <div className="bpmn-diagram-container">
      <div className="bpmn-toolbar">
        <h2>BPMN Diagram (Read-Only)</h2>
        <div className="toolbar-actions">
          <button onClick={handleZoomIn} title="Zoom In">
            +
          </button>
          <button onClick={handleZoomOut} title="Zoom Out">
            -
          </button>
          <button onClick={handleZoomReset} title="Fit to Screen">
            ⊡
          </button>
          <button onClick={handleExport} title="Export BPMN (Download Only)">
            ↓
          </button>
        </div>
      </div>
      {loading && <div className="bpmn-loading">Loading diagram...</div>}
      {error && <div className="bpmn-error">{error}</div>}
      <div ref={containerRef} className="bpmn-canvas" />
      
      {/* Architectural reminder for developers */}
      {import.meta.env.DEV && (
        <div className="bpmn-dev-notice">
          ⚠️ Read-Only Viewer: All edits via canonical model
        </div>
      )}
    </div>
  )
}

export default BpmnDiagram
