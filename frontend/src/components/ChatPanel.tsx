/**
 * ═══════════════════════════════════════════════════════════════════════════
 * ChatPanel - AI-Driven Process Creation and Editing via Canonical Model
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * ARCHITECTURAL PRINCIPLES:
 * 
 * 1. PROCESS CREATION:
 *    - User describes process in natural language
 *    - Backend AI creates canonical ProcessModel
 *    - Backend generates BPMN from ProcessModel
 *    - Frontend displays generated BPMN
 * 
 * 2. PROCESS EDITING:
 *    - User selects element in BPMN diagram (read-only viewer)
 *    - User describes desired change in natural language
 *    - Backend AI interprets instruction
 *    - Backend updates canonical ProcessModel
 *    - Backend regenerates BPMN from updated model
 *    - Frontend reloads regenerated BPMN
 * 
 * 3. NEVER:
 *    - Directly modify BPMN XML
 *    - Save BPMN changes without going through canonical model
 *    - Bypass the edit-intent or process creation APIs
 * 
 * This ensures single source of truth and enables AI-driven workflows.
 * ═══════════════════════════════════════════════════════════════════════════
 */

import { useState, useRef, useEffect } from 'react'
import './ChatPanel.css'
import axios from 'axios'

interface ChatPanelProps {
  onProcessCreated?: (processId: string) => void
  selectedElement?: any
  processId?: string
}

interface Message {
  id: string
  type: 'user' | 'assistant' | 'system'
  content: string
  timestamp: Date
  streaming?: boolean
}

interface Question {
  id: string
  question: string
}

const ChatPanel = ({ onProcessCreated, selectedElement, processId: propProcessId }: ChatPanelProps) => {
  const [messages, setMessages] = useState<Message[]>([
    {
      id: '1',
      type: 'system',
      content: 'Welcome to the AI BPMN Compiler! Describe your business process in natural language, and I will help you create a BPMN diagram.',
      timestamp: new Date()
    }
  ])
  const [inputValue, setInputValue] = useState('')
  const [loading, setLoading] = useState(false)
  const [currentProcessId, setCurrentProcessId] = useState<string | null>(null)
  const [pendingQuestions, setPendingQuestions] = useState<Question[]>([])
  const [streamingMessageId, setStreamingMessageId] = useState<string | null>(null)
  const messagesEndRef = useRef<HTMLDivElement>(null)
  const abortControllerRef = useRef<AbortController | null>(null)

  // Update current process ID when prop changes
  useEffect(() => {
    if (propProcessId && propProcessId !== currentProcessId) {
      setCurrentProcessId(propProcessId)
    }
  }, [propProcessId])

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }

  useEffect(() => {
    scrollToBottom()
  }, [messages])

  const addMessage = (type: 'user' | 'assistant' | 'system', content: string, streaming: boolean = false) => {
    const newMessage: Message = {
      id: Date.now().toString(),
      type,
      content,
      timestamp: new Date(),
      streaming
    }
    setMessages(prev => [...prev, newMessage])
    return newMessage.id
  }

  const updateStreamingMessage = (id: string, content: string) => {
    setMessages(prev => 
      prev.map(msg => 
        msg.id === id 
          ? { ...msg, content, streaming: true }
          : msg
      )
    )
  }

  const finalizeStreamingMessage = (id: string) => {
    setMessages(prev => 
      prev.map(msg => 
        msg.id === id 
          ? { ...msg, streaming: false }
          : msg
      )
    )
    setStreamingMessageId(null)
  }

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

  // ═══════════════════════════════════════════════════════════════════════
  // EDIT INTENT HANDLER: All Edits Through Canonical Model
  // ═══════════════════════════════════════════════════════════════════════
  // This function handles natural language edit instructions from the chat.
  // 
  // ARCHITECTURAL FLOW:
  // 1. User types edit instruction (e.g., "Rename this to 'Approve Request'")
  // 2. Instruction sent to backend edit-intent API
  // 3. Backend AI (Gemini) interprets natural language
  // 4. Backend generates structured edit command (JSON)
  // 5. Backend applies edit to canonical ProcessModel
  // 6. Backend regenerates BPMN from updated ProcessModel
  // 7. Frontend reloads regenerated BPMN
  // 
  // CRITICAL: This ensures the canonical ProcessModel is always the source
  // of truth. BPMN is a generated artifact, never the primary data store.
  // 
  // WHY THIS MATTERS:
  // - Prevents model drift (BPMN vs ProcessModel inconsistencies)
  // - Enables AI-driven process generation and modification
  // - Maintains business rules, validations, and DRL generation
  // - Allows for complex transformations and optimizations
  // 
  // NEVER attempt to modify BPMN directly in the frontend!
  // ═══════════════════════════════════════════════════════════════════════
  
  const handleEditIntent = async (instruction: string) => {
    if (!currentProcessId) {
      addMessage('system', 'Please create or load a process first before editing.')
      return
    }

    try {
      console.info('[Edit Intent] Sending instruction to canonical model API:', instruction)
      
      // Send edit instruction to backend (canonical model API)
      const response = await axios.post(
        `http://localhost:8080/api/process/${currentProcessId}/edit-intent`,
        {
          instruction,
          nodeId: selectedElement?.id || null
        }
      )

      if (response.data.success) {
        console.info('[Edit Intent] Canonical model updated, BPMN regenerated')
        addMessage('assistant', `✅ Edit applied successfully! ${response.data.message}`)
        addMessage('system', 'The BPMN diagram has been regenerated from canonical model. Refreshing...')
        
        // Reload to display freshly generated BPMN from canonical model
        setTimeout(() => {
          window.location.reload()
        }, 1500)
      } else {
        console.warn('[Edit Intent] Edit failed:', response.data.message)
        addMessage('assistant', `❌ Edit failed: ${response.data.message}`)
      }
    } catch (err: any) {
      console.error('[Edit Intent] Error updating canonical model:', err)
      addMessage('system', `Error applying edit: ${err.response?.data?.message || err.message}`)
    }
  }

  const streamAIResponse = async (endpoint: string, payload: any): Promise<string> => {
    // Create abort controller for this request
    abortControllerRef.current = new AbortController()
    
    // Create streaming message
    const messageId = addMessage('assistant', '', true)
    setStreamingMessageId(messageId)
    
    let fullContent = ''

    try {
      const response = await fetch(`http://localhost:8080${endpoint}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(payload),
        signal: abortControllerRef.current.signal
      })

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }

      const reader = response.body?.getReader()
      const decoder = new TextDecoder()

      if (!reader) {
        throw new Error('Response body is not readable')
      }

      while (true) {
        const { done, value } = await reader.read()
        
        if (done) break

        const chunk = decoder.decode(value, { stream: true })
        fullContent += chunk
        
        // Update the streaming message
        updateStreamingMessage(messageId, fullContent)
      }

      finalizeStreamingMessage(messageId)
      return fullContent

    } catch (error: any) {
      if (error.name === 'AbortError') {
        finalizeStreamingMessage(messageId)
        addMessage('system', 'Response streaming was cancelled.')
      } else {
        finalizeStreamingMessage(messageId)
        throw error
      }
      return fullContent
    }
  }

  const handleSend = async () => {
    if (!inputValue.trim()) return

    const userMessage = inputValue.trim()
    setInputValue('')
    addMessage('user', userMessage)
    setLoading(true)

    try {
      // Check for edit intent if a process exists
      if (currentProcessId && detectEditIntent(userMessage)) {
        // Show context if element is selected
        if (selectedElement) {
          addMessage('system', `Editing: ${selectedElement.businessObject?.name || selectedElement.id} (${selectedElement.type})`)
        }
        
        await handleEditIntent(userMessage)
        setLoading(false)
        return
      }

      if (!currentProcessId) {
        // Start a new process
        addMessage('system', 'Creating your process...')
        
        // Try streaming if backend supports it
        try {
          await streamAIResponse('/api/process/start', {
            description: userMessage
          })
        } catch (streamErr) {
          console.warn('Streaming not available, falling back to regular request')
          
          const response = await axios.post('http://localhost:8080/api/process/start', {
            description: userMessage
          })
          
          const processId = response.data.processId
          setCurrentProcessId(processId)
          
          if (onProcessCreated) {
            onProcessCreated(processId)
          }

          addMessage('system', `Process created with ID: ${processId}`)
          addMessage('assistant', 'I\'ve started analyzing your process description. I will create a BPMN diagram based on your requirements.')
          
          // Check for clarification questions
          checkForQuestions(processId)
        }
      } else {
        // Check if we have pending questions
        if (pendingQuestions.length > 0) {
          // Answer the first pending question
          const question = pendingQuestions[0]
          await axios.post(`http://localhost:8080/api/process/${currentProcessId}/answer`, {
            questionId: question.id,
            answer: userMessage
          })
          
          setPendingQuestions(prev => prev.slice(1))
          addMessage('assistant', 'Thank you for the clarification.')
          
          // Check if there are more questions
          if (pendingQuestions.length > 1) {
            addMessage('assistant', pendingQuestions[1].question)
          } else {
            addMessage('assistant', 'Processing your answers...')
            // Resume the process
            await axios.post(`http://localhost:8080/api/process/${currentProcessId}/resume`)
            checkForQuestions(currentProcessId)
          }
        } else {
          // General conversation - provide helpful response
          if (selectedElement) {
            addMessage('assistant', 
              `I see you have selected "${selectedElement.businessObject?.name || selectedElement.id}". ` +
              `You can ask me to rename it, update its condition, or modify its description. ` +
              `For example: "Rename this to 'Approve Request'" or "Change condition to amount > 5000"`
            )
          } else {
            addMessage('assistant', 
              'I can help you refine the process or edit specific elements. ' +
              'Select an element in the diagram and tell me what you\'d like to change.'
            )
          }
        }
      }
    } catch (err: any) {
      console.error('Error sending message:', err)
      addMessage('system', `Error: ${err.response?.data?.message || err.message}`)
    } finally {
      setLoading(false)
    }
  }

  const checkForQuestions = async (processId: string) => {
    try {
      // Poll for questions
      const response = await axios.get(`http://localhost:8080/api/process/${processId}/questions`)
      
      if (response.data && response.data.length > 0) {
        setPendingQuestions(response.data)
        addMessage('assistant', response.data[0].question)
      } else {
        // No questions, process is complete or in progress
        addMessage('assistant', 'Your process has been created! You can view it in the diagram panel and publish it when ready.')
      }
    } catch (err: any) {
      console.error('Error checking for questions:', err)
      // If there's an error, assume process is complete
      addMessage('assistant', 'Your process has been created! You can view it in the diagram panel and publish it when ready.')
    }
  }

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  const handleNewProcess = () => {
    setCurrentProcessId(null)
    setPendingQuestions([])
    setMessages([
      {
        id: Date.now().toString(),
        type: 'system',
        content: 'Starting a new process. Please describe your business process.',
        timestamp: new Date()
      }
    ])
    if (onProcessCreated) {
      onProcessCreated('')
    }
  }

  const stopStreaming = () => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort()
      abortControllerRef.current = null
    }
  }

  return (
    <div className="chat-panel">
      <div className="chat-header">
        <h2>AI Assistant</h2>
        <div className="chat-header-actions">
          {currentProcessId && (
            <button onClick={handleNewProcess} className="btn-new-process">
              New Process
            </button>
          )}
        </div>
      </div>
      
      {/* Context indicator */}
      {selectedElement && (
        <div className="chat-context">
          <span className="context-icon">🎯</span>
          <span className="context-text">
            Selected: <strong>{selectedElement.businessObject?.name || selectedElement.id}</strong>
            <span className="context-type">({selectedElement.type})</span>
          </span>
        </div>
      )}
      
      <div className="chat-messages">
        {messages.map(msg => (
          <div key={msg.id} className={`message message-${msg.type} ${msg.streaming ? 'streaming' : ''}`}>
            <div className="message-avatar">
              {msg.type === 'user' ? '👤' : msg.type === 'assistant' ? '🤖' : 'ℹ️'}
            </div>
            <div className="message-content">
              <div className="message-text">
                {msg.content}
                {msg.streaming && <span className="streaming-cursor">▊</span>}
              </div>
              <div className="message-time">
                {msg.timestamp.toLocaleTimeString()}
              </div>
            </div>
          </div>
        ))}
        {loading && !streamingMessageId && (
          <div className="message message-assistant">
            <div className="message-avatar">🤖</div>
            <div className="message-content">
              <div className="message-loading">
                <span className="dot">.</span>
                <span className="dot">.</span>
                <span className="dot">.</span>
              </div>
            </div>
          </div>
        )}
        <div ref={messagesEndRef} />
      </div>
      
      <div className="chat-input-container">
        {streamingMessageId && (
          <button 
            onClick={stopStreaming}
            className="btn-stop-streaming"
          >
            ⏸ Stop
          </button>
        )}
        <textarea
          className="chat-input"
          placeholder={
            selectedElement 
              ? `Edit "${selectedElement.businessObject?.name || selectedElement.id}"...`
              : pendingQuestions.length > 0 
                ? "Answer the question..." 
                : "Describe your process or edit an element..."
          }
          value={inputValue}
          onChange={(e) => setInputValue(e.target.value)}
          onKeyPress={handleKeyPress}
          disabled={loading}
          rows={3}
        />
        <button 
          className="chat-send-button"
          onClick={handleSend}
          disabled={loading || !inputValue.trim()}
        >
          Send
        </button>
      </div>
    </div>
  )
}

export default ChatPanel
