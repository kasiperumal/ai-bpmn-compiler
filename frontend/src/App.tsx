import { useState } from 'react'
import './App.css'
import BpmnDiagram from './components/BpmnDiagram'
import PropertiesPanel from './components/PropertiesPanel'
import ChatPanel from './components/ChatPanel'

function App() {
  const [selectedElement, setSelectedElement] = useState<any>(null)
  const [processId, setProcessId] = useState<string>('')

  return (
    <div className="app-container">
      <header className="app-header">
        <h1>AI BPMN Compiler</h1>
        <div className="header-info">
          {processId && <span>Process ID: {processId}</span>}
        </div>
      </header>
      
      <div className="main-layout">
        {/* Left: BPMN Diagram */}
        <div className="diagram-section">
          <BpmnDiagram 
            onElementSelect={setSelectedElement}
            processId={processId}
          />
        </div>
        
        {/* Middle: Properties Panel */}
        <div className="properties-section">
          <PropertiesPanel 
            selectedElement={selectedElement}
            processId={processId}
          />
        </div>
        
        {/* Right: Chat Panel */}
        <div className="chat-section">
          <ChatPanel 
            onProcessCreated={setProcessId}
            selectedElement={selectedElement}
            processId={processId}
          />
        </div>
      </div>
    </div>
  )
}

export default App
