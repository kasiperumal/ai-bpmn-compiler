import { useState } from 'react'
import './App.css'
import BpmnDiagram from './components/BpmnDiagram'
import PropertiesPanel from './components/PropertiesPanel'
import ChatPanel from './components/ChatPanel'
import axios from 'axios'

function App() {
  const [selectedElement, setSelectedElement] = useState<any>(null)
  const [processId, setProcessId] = useState<string>('')
  const [showProperties, setShowProperties] = useState(false)
  const [processStatus, setProcessStatus] = useState<string>('')
  const [loading, setLoading] = useState(false)

  const handlePublish = async () => {
    if (!processId) return
    
    setLoading(true)
    try {
      await axios.post(`http://localhost:8080/api/process/${processId}/publish`)
      setProcessStatus('PUBLISHED')
      alert('Process published successfully!')
    } catch (error: any) {
      console.error('Error publishing process:', error)
      alert(`Failed to publish: ${error.response?.data?.message || error.message}`)
    } finally {
      setLoading(false)
    }
  }

  const handleExecute = async () => {
    if (!processId) return
    
    setLoading(true)
    try {
      const response = await axios.post(`http://localhost:8080/api/process/${processId}/execute`, {})
      alert(`Process instance started: ${response.data.processInstanceId}`)
    } catch (error: any) {
      console.error('Error executing process:', error)
      alert(`Failed to execute: ${error.response?.data?.message || error.message}`)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="app-container">
      <header className="app-header">
        <div className="header-left">
          <h1>AI BPMN Compiler</h1>
          {processId && <span className="process-id">Process ID: {processId}</span>}
        </div>
        
        <div className="header-actions">
          {processId && (
            <>
              <button
                onClick={() => setShowProperties(!showProperties)}
                className="btn-icon"
                title="Toggle Properties Panel"
              >
                <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                  <path d="M10 6a2 2 0 110-4 2 2 0 010 4zm0 6a2 2 0 110-4 2 2 0 010 4zm0 6a2 2 0 110-4 2 2 0 010 4z" fill="currentColor"/>
                </svg>
              </button>
              
              <button
                onClick={handlePublish}
                disabled={loading || processStatus === 'PUBLISHED'}
                className="btn-primary"
              >
                {processStatus === 'PUBLISHED' ? '✓ Published' : 'Publish'}
              </button>
              
              <button
                onClick={handleExecute}
                disabled={loading || processStatus !== 'PUBLISHED'}
                className="btn-secondary"
              >
                Execute
              </button>
            </>
          )}
        </div>
      </header>
      
      <div className="main-layout">
        {/* Left: BPMN Diagram */}
        <div className={`diagram-section ${showProperties ? 'with-properties' : ''}`}>
          <BpmnDiagram 
            onElementSelect={setSelectedElement}
            processId={processId}
          />
        </div>
        
        {/* Middle: Properties Panel (Collapsible) */}
        {showProperties && (
          <div className="properties-section">
            <PropertiesPanel 
              selectedElement={selectedElement}
              processId={processId}
              onClose={() => setShowProperties(false)}
            />
          </div>
        )}
        
        {/* Right: Chat Panel */}
        <div className={`chat-section ${showProperties ? 'with-properties' : ''}`}>
          <ChatPanel 
            onProcessCreated={(id) => {
              setProcessId(id)
              setProcessStatus('DRAFT')
            }}
            selectedElement={selectedElement}
            processId={processId}
          />
        </div>
      </div>
    </div>
  )
}

export default App
