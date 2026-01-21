import { useEffect, useRef, useState } from 'react'
import BpmnModeler from 'bpmn-js/lib/Modeler'
import {
  BpmnPropertiesPanelModule,
  BpmnPropertiesProviderModule,
  CamundaPlatformPropertiesProviderModule
} from 'bpmn-js-properties-panel'
import CamundaBpmnModdle from 'camunda-bpmn-moddle/resources/camunda.json'
import CamundaPlatformBehaviorsModule from 'camunda-bpmn-js-behaviors/lib/camunda-platform'
import BpmnModdle from 'bpmn-moddle'
import 'bpmn-js/dist/assets/diagram-js.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn-embedded.css'
import '@bpmn-io/properties-panel/dist/assets/properties-panel.css'
import './BpmnDiagram.css'
import axios from 'axios'
import { layoutProcess } from 'bpmn-auto-layout'

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * BPMN MODELER WITH CAMUNDA PLATFORM PROPERTIES
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * This component uses BpmnModeler with ALL editing features enabled:
 * - Visual BPMN editor with palette, context pad, and properties panel
 * - Direct manipulation of elements (drag, resize, connect)
 * - Property editing via properties panel
 * - Full BPMN 2.0 modeling capabilities
 * - Comprehensive Camunda Platform 7 properties support
 * 
 * WORKFLOW:
 * 1. AI generates initial BPMN from natural language description
 * 2. User can refine the BPMN using:
 *    a) Visual editor (this component) - direct BPMN manipulation
 *    b) Chat interface - natural language refinements
 *    c) Properties panel - structured property editing
 * 3. Save button persists BPMN changes back to canonical model
 * 4. Backend can regenerate BPMN from canonical model when needed
 * 
 * FEATURES ENABLED:
 * ✓ Palette (add new elements)
 * ✓ Context Pad (element actions)
 * ✓ Direct editing (label editing)
 * ✓ Properties Panel with official Camunda Platform properties
 * ✓ Camunda Platform behaviors (maintains model integrity)
 * ✓ Drag-and-drop modeling
 * ✓ Element connections
 * ✓ Save functionality
 * ✓ Export/Import BPMN
 * 
 * CAMUNDA PROPERTIES AVAILABLE:
 * ✓ User Assignment (Assignee, Candidate Users, Candidate Groups)
 * ✓ Implementation (Class, Expression, Delegate Expression)
 * ✓ External Tasks (Topic, Priority, Retry Time Cycle)
 * ✓ Form Keys & Task Listeners
 * ✓ Async Continuations & Job Execution
 * ✓ Input/Output Mappings
 * ✓ Conditions & Scripts
 * ✓ And much more!
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 */

interface BpmnDiagramProps {
  onElementSelect?: (element: any) => void
  processId?: string
  onSave?: (bpmnXml: string) => Promise<void>
}

/**
 * Convert BPMN Moddle JSON to BPMN XML for bpmn-js import
 * Creates a standalone BpmnModdle instance with proper packages
 * 
 * @param moddleJson - BPMN Moddle JSON from backend
 */
const convertModdleJsonToXml = async (moddleJson: any): Promise<string> => {
  // Create a standalone BpmnModdle instance with BPMN packages + Camunda extension
  // Note: CamundaBpmnModdle is already a JSON descriptor from camunda-bpmn-moddle/resources/camunda.json
  const moddle = new BpmnModdle({
    camunda: CamundaBpmnModdle
  })
  
  // Element map for resolving references
  const elementMap = new Map<string, any>()
  
  // FIRST PASS: Create all elements and populate the map (skip reference fields)
  const createElementFirstPass = (json: any): any => {
    if (!json || typeof json !== 'object') return json
    
    const type = json.$type
    if (!type) return json
    
    const props: any = {}
    const referenceFields = ['sourceRef', 'targetRef', 'default']
    
    for (const key in json) {
      if (key === '$type') continue
      
      const value = json[key]
      if (value === undefined || value === null) continue
      
      // Skip reference fields in first pass
      if (referenceFields.includes(key)) {
        continue
      }
      
      if (key === 'incoming' || key === 'outgoing') {
        // Skip incoming/outgoing in first pass (they reference flows by ID)
        continue
      }
      
      if (Array.isArray(value)) {
        const filteredArray = value
          .filter((item: any) => item !== undefined && item !== null)
          .map((item: any) => {
            if (item && typeof item === 'object' && item.$type) {
              return createElementFirstPass(item)
            }
            return item
          })
        if (filteredArray.length > 0) {
          props[key] = filteredArray
        }
      } else if (value && typeof value === 'object' && value.$type) {
        props[key] = createElementFirstPass(value)
      } else {
        props[key] = value
      }
    }
    
    const element = moddle.create(type, props)
    
    // Store in map if it has an ID
    if (element.id) {
      elementMap.set(element.id, element)
    }
    
    return element
  }
  
  // DEBUG: Check what's in the backend JSON
  const process = moddleJson.rootElements?.find((el: any) => el.$type === 'bpmn:Process')
  if (process && process.flowElements) {
    const sampleFlow = process.flowElements.find((el: any) => el.$type === 'bpmn:SequenceFlow')
    const sampleTask = process.flowElements.find((el: any) => el.$type.includes('Task'))
    console.log('[DEBUG] Sample SequenceFlow from backend JSON:', JSON.stringify(sampleFlow, null, 2))
    console.log('[DEBUG] Sample Task from backend JSON:', JSON.stringify(sampleTask, null, 2))
  }
  
  // FIRST PASS: Create all elements (skip references)
  const definitions = createElementFirstPass(moddleJson)
  console.log('[DEBUG] First pass complete - elementMap size:', elementMap.size)
  
  // SECOND PASS: Resolve sourceRef/targetRef AND add incoming/outgoing
  const processElement = definitions.rootElements?.find((el: any) => el.$type === 'bpmn:Process')
  if (processElement && processElement.flowElements) {
    for (const element of processElement.flowElements) {
      const originalElement = process.flowElements.find((el: any) => el.id === element.id)
      
      if (element.$type === 'bpmn:SequenceFlow') {
        // Resolve sourceRef/targetRef from string ID to element object
        if (originalElement) {
          if (originalElement.sourceRef) {
            element.sourceRef = elementMap.get(originalElement.sourceRef)
            console.log('[DEBUG] Resolved', element.id, 'sourceRef:', originalElement.sourceRef, '→', element.sourceRef ? 'OK' : 'MISSING')
          }
          if (originalElement.targetRef) {
            element.targetRef = elementMap.get(originalElement.targetRef)
            console.log('[DEBUG] Resolved', element.id, 'targetRef:', originalElement.targetRef, '→', element.targetRef ? 'OK' : 'MISSING')
          }
        }
      } else {
        // For flow nodes (tasks, events, gateways), resolve incoming/outgoing to SequenceFlow objects
        if (originalElement) {
          if (originalElement.incoming && Array.isArray(originalElement.incoming) && originalElement.incoming.length > 0) {
            // Resolve string IDs to SequenceFlow element objects
            element.incoming = originalElement.incoming
              .filter((id: any) => id !== null && id !== undefined)
              .map((id: any) => elementMap.get(id))
              .filter((el: any) => el !== undefined)
            console.log('[DEBUG] Set', element.id, 'incoming: resolved', originalElement.incoming.length, 'flows')
          }
          if (originalElement.outgoing && Array.isArray(originalElement.outgoing) && originalElement.outgoing.length > 0) {
            // Resolve string IDs to SequenceFlow element objects
            element.outgoing = originalElement.outgoing
              .filter((id: any) => id !== null && id !== undefined)
              .map((id: any) => elementMap.get(id))
              .filter((el: any) => el !== undefined)
            console.log('[DEBUG] Set', element.id, 'outgoing: resolved', originalElement.outgoing.length, 'flows')
          }
          if (originalElement.default) {
            // Resolve default flow reference to SequenceFlow object
            element.default = elementMap.get(originalElement.default)
          }
        }
      }
    }
  }
  
  console.log('[DEBUG] Second pass complete - references and incoming/outgoing added')
  
  // ═══════════════════════════════════════════════════════════════════════
  // IMPORTANT: DO NOT GENERATE DI (Diagram Interchange) HERE
  // ═══════════════════════════════════════════════════════════════════════
  // We intentionally skip creating diagrams, shapes, edges, and bounds.
  // The bpmn-auto-layout library will generate all DI information for us.
  // If we provide DI here, bpmn-auto-layout won't process it correctly.
  // ═══════════════════════════════════════════════════════════════════════
  
  // Leave definitions.diagrams as undefined/empty - this is correct!
  
  // Convert to XML
  const { xml } = await moddle.toXML(definitions)
  return xml
}

const BpmnDiagram = ({ onElementSelect, processId, onSave }: BpmnDiagramProps) => {
  const containerRef = useRef<HTMLDivElement>(null)
  const propertiesPanelRef = useRef<HTMLDivElement>(null)
  const viewerRef = useRef<any>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false)
  
  // Properties panel visibility state - starts hidden to maximize diagram space
  const [showPropertiesPanel, setShowPropertiesPanel] = useState(() => {
    // Remember user preference from localStorage
    const saved = localStorage.getItem('bpmn-properties-panel-visible')
    return saved === 'true' // Default to false (hidden)
  })

  useEffect(() => {
    if (!containerRef.current || !propertiesPanelRef.current) return

    const modeler = new BpmnModeler({
      container: containerRef.current,
      propertiesPanel: {
        parent: propertiesPanelRef.current
      },
      additionalModules: [
        BpmnPropertiesPanelModule,
        BpmnPropertiesProviderModule,
        CamundaPlatformPropertiesProviderModule,
        CamundaPlatformBehaviorsModule
      ],
      moddleExtensions: {
        camunda: CamundaBpmnModdle
      }
    })

    viewerRef.current = modeler
    console.info('[BPMN Modeler] Initialized with full editing capabilities')

    // Track changes for save indicator
    const eventBus = modeler.get('eventBus') as any
    
    eventBus.on('element.click', (event: any) => {
      if (onElementSelect) {
        onElementSelect(event.element)
      }
      
      // Auto-show properties panel when element is selected (helpful UX)
      if (event.element && event.element.type !== 'label') {
        setShowPropertiesPanel(true)
      }
    })

    // Track changes for unsaved indicator
    eventBus.on('commandStack.changed', () => {
      setHasUnsavedChanges(true)
    })

    return () => {
      modeler.destroy()
    }
  }, [onElementSelect])

  // ═══════════════════════════════════════════════════════════════════════
  // BPMN LOADING: CORRECT ARCHITECTURE - Use bpmn-auto-layout
  // ═══════════════════════════════════════════════════════════════════════
  // Flow:
  // 1. Fetch BPMN Moddle JSON from backend (no DI information)
  // 2. Convert Moddle JSON → XML (WITHOUT DI - no shapes, edges, bounds)
  // 3. Pass XML through bpmn-auto-layout (generates DI automatically)
  // 4. Import the layouted XML into BPMN.js
  // 5. Fit to viewport
  // 
  // KEY: bpmn-auto-layout requires XML WITHOUT DI to generate layout.
  // If DI already exists, it won't process correctly.
  // ═══════════════════════════════════════════════════════════════════════
  
  useEffect(() => {
    if (!processId || !viewerRef.current) return

    const loadBpmn = async () => {
      setLoading(true)
      setError(null)
      try {
        // 1. Fetch BPMN Moddle JSON from backend (no DI)
        const response = await axios.get(`http://localhost:8080/api/process/${processId}/bpmn-json`)
        const bpmnModdleJson = response.data
        
        console.info('[BPMN Viewer] Received BPMN Moddle JSON from backend')
        
        // 2. Convert to XML WITHOUT DI (no shapes, edges, bounds)
        console.info('[BPMN Viewer] Converting to XML (no DI)...')
        const bpmnXmlNoDI = await convertModdleJsonToXml(bpmnModdleJson)
        
        // DEBUG: Log the generated XML to see if sourceRef/targetRef are present
        console.log('[DEBUG] Generated XML (first 2000 chars):', bpmnXmlNoDI.substring(0, 2000))
        console.log('[DEBUG] Checking for sourceRef in XML:', bpmnXmlNoDI.includes('sourceRef='))
        console.log('[DEBUG] Checking for sequenceFlow in XML:', bpmnXmlNoDI.includes('sequenceFlow'))
        
        // 3. Apply bpmn-auto-layout to generate DI
        console.info('[BPMN Viewer] Applying bpmn-auto-layout...')
        const layoutedXml = await layoutProcess(bpmnXmlNoDI)
        
        // DEBUG: Log the layouted XML to see if DI was added
        console.log('[DEBUG] Layouted XML includes BPMNEdge:', layoutedXml.includes('BPMNEdge'))
        console.log('[DEBUG] Layouted XML includes waypoint:', layoutedXml.includes('waypoint'))
        
        // 4. Import the layouted XML into BPMN.js
        console.info('[BPMN Viewer] Importing layouted BPMN...')
        await viewerRef.current.importXML(layoutedXml)
        
        // 5. Force sequence flow labels to be visible
        console.info('[BPMN Viewer] Ensuring sequence flow labels are visible...')
        const elementRegistry = viewerRef.current.get('elementRegistry')
        const modeling = viewerRef.current.get('modeling')
        
        elementRegistry.forEach((element: any) => {
          if (element.type === 'bpmn:SequenceFlow' && element.businessObject.name) {
            // Ensure label is visible by updating its properties
            try {
              modeling.updateLabel(element, element.businessObject.name)
              console.debug(`[DEBUG] Label set for ${element.id}: "${element.businessObject.name}"`)
            } catch (err) {
              console.debug(`[DEBUG] Could not update label for ${element.id}:`, err)
            }
          }
        })
        
        // 6. Fit viewport to show the entire diagram
        const canvas = viewerRef.current.get('canvas')
        canvas.zoom('fit-viewport', 'auto')
        
        console.info('[BPMN Viewer] ✓ Successfully loaded and laid out BPMN diagram')
        console.info('[BPMN Viewer] Loaded BPMN from canonical model for process:', processId)
      } catch (err: any) {
        console.error('[BPMN Viewer] Error loading BPMN:', err)
        setError(err.response?.data?.message || err.message || 'Failed to load BPMN diagram')
      } finally {
        setLoading(false)
      }
    }

    loadBpmn()
  }, [processId])

  // ═══════════════════════════════════════════════════════════════════════
  // SAVE FUNCTION: Persist BPMN changes to backend
  // ═══════════════════════════════════════════════════════════════════════
  
  const handleSave = async () => {
    if (!viewerRef.current || !processId) return

    try {
      setLoading(true)
      const { xml } = await viewerRef.current.saveXML({ format: true })
      
      // Save to backend
      if (onSave) {
        await onSave(xml)
      } else {
        // Default save implementation
        await axios.put(`http://localhost:8080/api/process/${processId}/bpmn`, xml, {
          headers: { 'Content-Type': 'application/xml' }
        })
      }
      
      setHasUnsavedChanges(false)
      console.info('[BPMN Modeler] Saved BPMN successfully')
    } catch (err: any) {
      console.error('[BPMN Modeler] Error saving BPMN:', err)
      setError(err.response?.data?.message || 'Failed to save BPMN diagram')
    } finally {
      setLoading(false)
    }
  }

  // ═══════════════════════════════════════════════════════════════════════
  // EXPORT FUNCTION: Download BPMN to local file
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
  
  const togglePropertiesPanel = () => {
    const newValue = !showPropertiesPanel
    setShowPropertiesPanel(newValue)
    // Remember user preference
    localStorage.setItem('bpmn-properties-panel-visible', String(newValue))
  }

  return (
    <div className="bpmn-modeler-container">
      <div className="bpmn-toolbar">
        <h2>BPMN Modeler {hasUnsavedChanges && <span className="unsaved-indicator">●</span>}</h2>
        <div className="toolbar-actions">
          {hasUnsavedChanges && (
            <button onClick={handleSave} title="Save Changes" className="btn-save">
              💾 Save
            </button>
          )}
          <button onClick={handleZoomIn} title="Zoom In">
            +
          </button>
          <button onClick={handleZoomOut} title="Zoom Out">
            -
          </button>
          <button onClick={handleZoomReset} title="Fit to Screen">
            ⊡
          </button>
          <button onClick={handleExport} title="Export BPMN">
            ↓
          </button>
          <button 
            onClick={togglePropertiesPanel} 
            title={showPropertiesPanel ? "Hide Properties Panel" : "Show Properties Panel"}
            className={showPropertiesPanel ? 'btn-active' : ''}
          >
            {showPropertiesPanel ? '→' : '←'} Properties
          </button>
        </div>
      </div>
      {loading && <div className="bpmn-loading">Saving changes...</div>}
      {error && <div className="bpmn-error">{error}</div>}
      
      <div className="bpmn-content">
        <div ref={containerRef} className="bpmn-canvas" />
        <div 
          ref={propertiesPanelRef} 
          className={`bpmn-properties-panel ${showPropertiesPanel ? 'expanded' : 'collapsed'}`}
        />
      </div>
    </div>
  )
}

export default BpmnDiagram
