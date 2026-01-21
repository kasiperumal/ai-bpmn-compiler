package com.example.aibpmn.dto;

/**
 * Unified chat request for all user interactions.
 * AI will analyze the message and context to determine intent.
 */
public class ChatRequest {
    
    private String message;
    private String processId;           // Optional - if editing/viewing existing process
    private String selectedElementId;   // Optional - if user selected a specific element
    private String conversationId;      // Optional - for continuing interactive conversations
    
    // Constructors
    
    public ChatRequest() {
    }
    
    public ChatRequest(String message) {
        this.message = message;
    }
    
    // Getters and Setters
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getProcessId() {
        return processId;
    }
    
    public void setProcessId(String processId) {
        this.processId = processId;
    }
    
    public String getSelectedElementId() {
        return selectedElementId;
    }
    
    public void setSelectedElementId(String selectedElementId) {
        this.selectedElementId = selectedElementId;
    }
    
    public String getConversationId() {
        return conversationId;
    }
    
    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
}
