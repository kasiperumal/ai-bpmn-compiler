/**
 * ═══════════════════════════════════════════════════════════════════════════
 * PropertiesPanel - Edit through Canonical Model
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * ARCHITECTURAL PRINCIPLE:
 * All edits in this component go through the backend's canonical ProcessModel.
 * 
 * EDIT WORKFLOW:
 * 1. User clicks "Edit Element" button
 * 2. User enters natural language instruction
 * 3. POST /api/process/{processId}/edit-intent
 *    - Sends instruction + selected nodeId
 * 4. Backend AI interprets instruction
 * 5. Backend updates canonical ProcessModel
 * 6. Backend regenerates BPMN from model
 * 7. Frontend reloads updated BPMN
 * 
 * NEVER:
 * - Directly modify BPMN XML
 * - Save changes to BPMN without going through canonical model
 * - Bypass the edit-intent API
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 */

import { useState, useEffect } from 'react'
import './PropertiesPanel.css'
import axios from 'axios'

interface PropertiesPanelProps {
  selectedElement?: any
  processId?: string
  onClose?: () => void
}

interface ProcessInfo {
  id: string
  name: string
  description: string
  status: string
  aiState: string
  createdAt: string
  updatedAt: string
}

interface Explanation {
  nodeId: string
  reason: string
  source: string
  confidenceScore: number
  timestamp: string
}

const PropertiesPanel = ({ selectedElement, processId, onClose }: PropertiesPanelProps) => {
  const [processInfo, setProcessInfo] = useState<ProcessInfo | null>(null)
  const [explanations, setExplanations] = useState<Explanation[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [editMode, setEditMode] = useState(false)
  const [editInstruction, setEditInstruction] = useState('')
  const [editLoading, setEditLoading] = useState(false)

  useEffect(() => {
    if (!processId) {
      setProcessInfo(null)
      setExplanations([])
      return
    }

    fetchProcessInfo()
    fetchExplanations()
  }, [processId])

  const fetchProcessInfo = async () => {
    if (!processId) return
    
    setLoading(true)
    setError(null)
    try {
      const response = await axios.get(`http://localhost:8080/api/process/${processId}`)
      setProcessInfo(response.data)
    } catch (err: any) {
      console.error('Error fetching process info:', err)
      setError(err.response?.data?.message || 'Failed to load process information')
    } finally {
      setLoading(false)
    }
  }

  const fetchExplanations = async () => {
    if (!processId) return
    
    try {
      const response = await axios.get(`http://localhost:8080/api/process/${processId}/explanations`)
      setExplanations(response.data.explanations || [])
    } catch (err: any) {
      console.error('Error fetching explanations:', err)
      // Don't set error state for explanations, as they're optional
    }
  }


  // ═══════════════════════════════════════════════════════════════════════
  // EDIT SUBMISSION: Through Canonical Model Only
  // ═══════════════════════════════════════════════════════════════════════
  // This function sends edit instructions to the backend edit-intent API.
  // 
  // CRITICAL FLOW:
  // 1. Natural language instruction → Backend API
  // 2. Backend AI interprets instruction
  // 3. Backend modifies ProcessModel (canonical source of truth)
  // 4. Backend regenerates BPMN from ProcessModel
  // 5. Frontend reloads regenerated BPMN
  // 
  // This ensures:
  // - Single source of truth (ProcessModel)
  // - No model drift between BPMN and canonical model
  // - AI-driven modifications with validation
  // - Consistent business rule application
  // 
  // NEVER bypass this flow by modifying BPMN directly!
  // ═══════════════════════════════════════════════════════════════════════
  
  const handleEditSubmit = async () => {
    if (!processId || !editInstruction.trim()) return
    
    setEditLoading(true)
    setError(null)
    try {
      // Send edit instruction to canonical model API
      const response = await axios.post(`http://localhost:8080/api/process/${processId}/edit-intent`, {
        instruction: editInstruction,
        nodeId: selectedElement?.id || null
      })
      
      if (response.data.success) {
        console.info('[Edit] Successfully updated canonical model, BPMN regenerated')
        alert('Edit applied successfully! The BPMN has been regenerated.')
        setEditInstruction('')
        setEditMode(false)
        
        // Refresh process info and explanations
        await fetchProcessInfo()
        await fetchExplanations()
        
        // Reload page to display regenerated BPMN from canonical model
        // This ensures we always display the authoritative BPMN
        window.location.reload()
      } else {
        console.warn('[Edit] Canonical model update failed:', response.data.message)
        alert('Edit failed: ' + response.data.message)
      }
    } catch (err: any) {
      console.error('[Edit] Error updating canonical model:', err)
      setError(err.response?.data?.message || 'Failed to apply edit')
      alert('Failed to apply edit: ' + (err.response?.data?.message || err.message))
    } finally {
      setEditLoading(false)
    }
  }

  const getExplanationForElement = (elementId: string): Explanation | undefined => {
    return explanations.find(exp => exp.nodeId === elementId)
  }

  const renderEditInterface = () => {
    if (!editMode) {
      return (
        <button 
          onClick={() => setEditMode(true)}
          className="btn-edit"
          disabled={loading || !selectedElement}
        >
          Edit Element
        </button>
      )
    }

    return (
      <div className="edit-interface">
        <h4>Edit Instruction</h4>
        <p className="edit-hint">
          Examples: "Rename to 'Approve Request'", "Change condition to amount &gt; 5000"
        </p>
        <textarea
          value={editInstruction}
          onChange={(e) => setEditInstruction(e.target.value)}
          placeholder={`What would you like to change about ${selectedElement?.businessObject?.name || 'this element'}?`}
          rows={3}
          className="edit-textarea"
          disabled={editLoading}
        />
        <div className="edit-actions">
          <button 
            onClick={handleEditSubmit}
            disabled={editLoading || !editInstruction.trim()}
            className="btn-primary"
          >
            {editLoading ? 'Applying...' : 'Apply Edit'}
          </button>
          <button 
            onClick={() => {
              setEditMode(false)
              setEditInstruction('')
            }}
            disabled={editLoading}
            className="btn-cancel"
          >
            Cancel
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="properties-panel">
      <div className="properties-header">
        <h2>Properties</h2>
        {onClose && (
          <button onClick={onClose} className="btn-close" title="Close Properties Panel">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
              <path d="M15 5L5 15M5 5l10 10" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
            </svg>
          </button>
        )}
      </div>
      
      <div className="properties-content">
        {loading && <div className="properties-loading">Loading...</div>}
        {error && <div className="properties-error">{error}</div>}
        
        {/* Process Information */}
        {processInfo && (
          <div className="properties-section">
            <h3>Process Information</h3>
            <div className="property-item">
              <label>ID:</label>
              <span>{processInfo.id}</span>
            </div>
            <div className="property-item">
              <label>Name:</label>
              <span>{processInfo.name || 'N/A'}</span>
            </div>
            <div className="property-item">
              <label>Description:</label>
              <span>{processInfo.description || 'N/A'}</span>
            </div>
            <div className="property-item">
              <label>Status:</label>
              <span className={`status-badge status-${processInfo.status?.toLowerCase()}`}>
                {processInfo.status}
              </span>
            </div>
            <div className="property-item">
              <label>AI State:</label>
              <span className="ai-state-badge">{processInfo.aiState}</span>
            </div>
            <div className="property-item">
              <label>Created:</label>
              <span>{new Date(processInfo.createdAt).toLocaleString()}</span>
            </div>
            <div className="property-item">
              <label>Updated:</label>
              <span>{new Date(processInfo.updatedAt).toLocaleString()}</span>
            </div>
          </div>
        )}
        
        {/* Selected Element Information */}
        {selectedElement && (
          <>
            <div className="properties-section">
              <h3>Selected Element</h3>
              <div className="property-item">
                <label>Type:</label>
                <span>{selectedElement.type}</span>
              </div>
              <div className="property-item">
                <label>ID:</label>
                <span>{selectedElement.id}</span>
              </div>
              {selectedElement.businessObject?.name && (
                <div className="property-item">
                  <label>Name:</label>
                  <span>{selectedElement.businessObject.name}</span>
                </div>
              )}
              
              {/* Display explanation for selected element */}
              {(() => {
                const explanation = getExplanationForElement(selectedElement.id)
                if (explanation) {
                  return (
                    <div className="explanation-box">
                      <label>AI Explanation:</label>
                      <p className="explanation-text">{explanation.reason}</p>
                      <div className="explanation-meta">
                        <span className="explanation-source">{explanation.source}</span>
                        <span className="explanation-confidence">
                          Confidence: {(explanation.confidenceScore * 100).toFixed(0)}%
                        </span>
                      </div>
                    </div>
                  )
                }
                return null
              })()}
            </div>
            
            {/* Edit Interface */}
            {processId && (
              <div className="properties-section">
                <h3>Edit Element</h3>
                {renderEditInterface()}
              </div>
            )}
          </>
        )}
        
        {/* All Explanations (expandable) */}
        {explanations.length > 0 && !selectedElement && (
          <div className="properties-section">
            <h3>Process Explanations</h3>
            <div className="explanations-list">
              {explanations.map((exp) => (
                <div key={exp.nodeId} className="explanation-item">
                  <div className="explanation-header">
                    <strong>Node: {exp.nodeId}</strong>
                    <span className="explanation-confidence-small">
                      {(exp.confidenceScore * 100).toFixed(0)}%
                    </span>
                  </div>
                  <p className="explanation-text-small">{exp.reason}</p>
                </div>
              ))}
            </div>
          </div>
        )}
        
        {!processId && !selectedElement && (
          <div className="properties-empty">
            <p>No process loaded. Start a conversation in the chat panel to create a process.</p>
          </div>
        )}
      </div>
    </div>
  )
}

export default PropertiesPanel
