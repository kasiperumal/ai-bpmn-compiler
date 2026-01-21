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
  type: 'user' | 'assistant' | 'system' | 'questions'
  content: string
  timestamp: Date
  questions?: string[]
  answers?: Map<number, string>
}

// Unique ID generator for messages to avoid duplicate keys
let messageIdCounter = 0
const generateMessageId = () => {
  messageIdCounter++
  return `msg-${Date.now()}-${messageIdCounter}`
}

const ChatPanel = ({ onProcessCreated, selectedElement, processId: propProcessId }: ChatPanelProps) => {
  const [messages, setMessages] = useState<Message[]>([
    {
      id: 'initial-1',
      type: 'system',
      content: 'Welcome to the AI BPMN Compiler! Describe your business process in natural language, and I will help you create a BPMN diagram.',
      timestamp: new Date()
    }
  ])
  const [inputValue, setInputValue] = useState('')
  const [loading, setLoading] = useState(false)
  const [currentProcessId, setCurrentProcessId] = useState<string | null>(null)
  const messagesEndRef = useRef<HTMLDivElement>(null)
  
  // Interactive conversation state
  const [conversationId, setConversationId] = useState<string | null>(null)
  const [conversationPhase, setConversationPhase] = useState<'CLARIFYING' | 'READY' | 'GENERATING' | 'COMPLETED' | null>(null)
  const [currentQuestions, setCurrentQuestions] = useState<string[]>([])
  const [currentAnswers, setCurrentAnswers] = useState<Map<number, string>>(new Map())

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

  const addMessage = (
    type: 'user' | 'assistant' | 'system' | 'questions', 
    content: string, 
    timestamp?: Date,
    questions?: string[]
  ) => {
    const newMessage: Message = {
      id: generateMessageId(),
      type,
      content,
      timestamp: timestamp || new Date(),
      questions: questions
    }
    setMessages(prev => [...prev, newMessage])
    return newMessage.id
  }


  // ═══════════════════════════════════════════════════════════════════════
  // UNIFIED CHAT MESSAGE HANDLER
  // ═══════════════════════════════════════════════════════════════════════
  // All messages now go through /api/chat/message endpoint.
  // AI handles intent detection - no more frontend keyword matching!
  // ═══════════════════════════════════════════════════════════════════════

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
  
  // ═══════════════════════════════════════════════════════════════════════
  // INTERACTIVE CONVERSATION HANDLERS
  // ═══════════════════════════════════════════════════════════════════════
  
  const handleAnswerUpdate = (questionIndex: number, answer: string) => {
    setCurrentAnswers(prev => {
      const updated = new Map(prev)
      updated.set(questionIndex, answer)
      return updated
    })
  }

  const handleSubmitAnswers = async () => {
    if (!conversationId) return
    
    setLoading(true)
    try {
      // Build Q&A pairs
      const questionsAndAnswers = currentQuestions.map((q, idx) => ({
        question: q,
        answer: currentAnswers.get(idx) || ''
      }))

      const response = await axios.post('http://localhost:8080/api/process/interactive/answer', {
        conversationId,
        questionsAndAnswers
      })

      const { phase, questions, message } = response.data
      
      setConversationPhase(phase)
      addMessage('assistant', message)

      if (phase === 'CLARIFYING' && questions && questions.length > 0) {
        // More questions to answer
        setCurrentQuestions(questions)
        setCurrentAnswers(new Map())
        addMessage('questions', '', new Date(), questions)
      } else if (phase === 'READY') {
        // Ready to generate BPMN
        setCurrentQuestions([])
        setCurrentAnswers(new Map())
      }
    } catch (err: any) {
      console.error('Error submitting answers:', err)
      addMessage('system', `Error submitting answers: ${err.response?.data?.message || err.message}`)
    } finally {
      setLoading(false)
    }
  }

  const handleGenerateBpmn = async () => {
    if (!conversationId) return
    
    setLoading(true)
    addMessage('system', 'Generating BPMN diagram from your answers...')
    
    try {
      const response = await axios.post(`http://localhost:8080/api/process/interactive/generate/${conversationId}`)
      
      const { processId, message } = response.data
      
      setCurrentProcessId(processId)
      setConversationPhase('COMPLETED')
      setConversationId(null)
      
      if (onProcessCreated) {
        onProcessCreated(processId)
      }

      addMessage('assistant', `✅ ${message}`)
      addMessage('system', 'You can now view your BPMN diagram in the diagram panel!')
    } catch (err: any) {
      console.error('Error generating BPMN:', err)
      addMessage('system', `Error generating BPMN: ${err.response?.data?.message || err.message}`)
    } finally {
      setLoading(false)
    }
  }

  /**
   * Unified message handler - sends all messages to /api/chat/message
   * AI determines intent and routes to appropriate action
   */
  const handleUnifiedMessage = async (message: string) => {
    try {
      console.info('[Unified Chat] Sending message to AI:', message)
      
      // Build request with all available context
      const request = {
        message: message,
        processId: currentProcessId || undefined,
        selectedElementId: selectedElement?.id || undefined,
        conversationId: conversationId || undefined
      }
      
      // Send to unified chat endpoint
      const response = await axios.post('http://localhost:8080/api/chat/message', request)
      
      const { intent, action, message: responseMessage, processId: newProcessId, requiresRefresh, questions, conversationId: newConvId, success } = response.data
      
      console.info('[Unified Chat] Response:', { intent, action, success })
      
      if (!success) {
        addMessage('assistant', `❌ ${responseMessage}`)
        return
      }
      
      // Handle response based on action type
      switch (action) {
        case 'EDIT_APPLIED':
          addMessage('assistant', `✅ ${responseMessage}`)
          if (requiresRefresh) {
            addMessage('system', 'Diagram updated! Refreshing...')
            setTimeout(() => window.location.reload(), 1500)
          }
          break
          
        case 'PROCESS_CREATED':
          addMessage('assistant', `✅ ${responseMessage}`)
          if (newProcessId) {
            setCurrentProcessId(newProcessId)
            if (onProcessCreated) {
              onProcessCreated(newProcessId)
            }
          }
          if (requiresRefresh) {
            addMessage('system', 'Loading your new BPMN diagram...')
            setTimeout(() => window.location.reload(), 1500)
          }
          break
          
        case 'CLARIFICATION_NEEDED':
          addMessage('assistant', responseMessage)
          if (questions && questions.length > 0) {
            setConversationId(newConvId)
            setConversationPhase('CLARIFYING') // Enable input boxes
            setCurrentQuestions(questions)
            setCurrentAnswers(new Map())
            addMessage('questions', '', new Date(), questions)
          }
          break
          
        case 'INFORMATION_PROVIDED':
          addMessage('assistant', responseMessage)
          break
          
        default:
          addMessage('assistant', responseMessage)
      }
      
    } catch (err: any) {
      console.error('[Unified Chat] Error:', err)
      addMessage('system', `Error: ${err.response?.data?.message || err.message}`)
    }
  }


  const handleSend = async () => {
    if (!inputValue.trim()) return

    const userMessage = inputValue.trim()
    setInputValue('')
    addMessage('user', userMessage)
    setLoading(true)

    try {
      // Show context if element is selected
      if (selectedElement) {
        const elementName = selectedElement.businessObject?.name || selectedElement.id
        addMessage('system', `Context: Selected "${elementName}" (${selectedElement.type})`)
      }
      
      // Send all messages to unified endpoint - AI determines intent!
      await handleUnifiedMessage(userMessage)
      
    } catch (err: any) {
      console.error('[Chat] Error sending message:', err)
      addMessage('system', `Error: ${err.response?.data?.message || err.message}`)
    } finally {
      setLoading(false)
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
    setConversationId(null)
    setConversationPhase(null)
    setCurrentQuestions([])
    setCurrentAnswers(new Map())
    setMessages([
      {
        id: generateMessageId(),
        type: 'system',
        content: 'Starting a new process. Please describe your business process.',
        timestamp: new Date()
      }
    ])
    if (onProcessCreated) {
      onProcessCreated('')
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
          <div key={msg.id} className={`message message-${msg.type}`}>
            <div className="message-avatar">
              {msg.type === 'user' ? '👤' : msg.type === 'assistant' ? '🤖' : msg.type === 'questions' ? '❓' : 'ℹ️'}
            </div>
            <div className="message-content">
              {msg.type === 'questions' && msg.questions ? (
                <div className="questions-container">
                  <div className="questions-title">Please answer these questions:</div>
                  {msg.questions.map((question, idx) => (
                    <div key={idx} className="question-item">
                      <label className="question-label">
                        {idx + 1}. {question}
                      </label>
                      <textarea
                        className="question-input"
                        placeholder="Your answer..."
                        value={currentAnswers.get(idx) || ''}
                        onChange={(e) => handleAnswerUpdate(idx, e.target.value)}
                        disabled={loading || conversationPhase !== 'CLARIFYING'}
                        rows={2}
                      />
                    </div>
                  ))}
                  {conversationPhase === 'CLARIFYING' && (
                    <button 
                      className="submit-answers-button"
                      onClick={handleSubmitAnswers}
                      disabled={loading || Array.from(currentAnswers.values()).some(a => !a.trim())}
                    >
                      Submit Answers
                    </button>
                  )}
                </div>
              ) : (
                <>
                  <div className="message-text">
                    {msg.content}
                  </div>
                  <div className="message-time">
                    {msg.timestamp.toLocaleTimeString()}
                  </div>
                </>
              )}
            </div>
          </div>
        ))}
        
        {/* Generate BPMN button when ready */}
        {conversationPhase === 'READY' && (
          <div className="message message-system">
            <div className="message-avatar">✅</div>
            <div className="message-content">
              <button 
                className="generate-bpmn-button"
                onClick={handleGenerateBpmn}
                disabled={loading}
              >
                🚀 Generate BPMN Diagram
              </button>
            </div>
          </div>
        )}
        
        {loading && (
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
        <textarea
          className="chat-input"
          placeholder={
            selectedElement 
              ? `Edit "${selectedElement.businessObject?.name || selectedElement.id}"...`
              : currentProcessId
                ? "Describe changes to your process or select an element to edit..."
                : "Describe your business process..."
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
