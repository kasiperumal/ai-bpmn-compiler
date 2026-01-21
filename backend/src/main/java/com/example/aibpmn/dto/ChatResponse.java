package com.example.aibpmn.dto;

import java.util.List;

/**
 * Unified chat response for all AI interactions.
 * Contains detected intent, action taken, and any data needed by frontend.
 */
public class ChatResponse {
    
    public enum Intent {
        EDIT,           // User wants to modify existing process/element
        CREATE,         // User wants to create new process
        QUESTION,       // User asking for help/information
        CLARIFICATION   // AI needs more information
    }
    
    public enum ActionType {
        EDIT_APPLIED,           // Edit was successfully applied
        PROCESS_CREATED,        // New process was created
        CLARIFICATION_NEEDED,   // AI needs user to answer questions
        INFORMATION_PROVIDED,   // Answered user's question
        ERROR                   // Something went wrong
    }
    
    private Intent intent;
    private ActionType action;
    private String message;                 // Human-readable message to display
    private String processId;               // Process ID (if applicable)
    private boolean requiresRefresh;        // Frontend should reload diagram
    private List<String> questions;         // Clarifying questions (if any)
    private String conversationId;          // For tracking interactive conversations
    private boolean success;
    
    // Constructors
    
    public ChatResponse() {
    }
    
    public ChatResponse(Intent intent, ActionType action, String message) {
        this.intent = intent;
        this.action = action;
        this.message = message;
        this.success = true;
    }
    
    public static ChatResponse success(Intent intent, ActionType action, String message) {
        ChatResponse response = new ChatResponse(intent, action, message);
        response.setSuccess(true);
        return response;
    }
    
    public static ChatResponse error(String message) {
        ChatResponse response = new ChatResponse();
        response.setIntent(Intent.QUESTION);
        response.setAction(ActionType.ERROR);
        response.setMessage(message);
        response.setSuccess(false);
        return response;
    }
    
    // Getters and Setters
    
    public Intent getIntent() {
        return intent;
    }
    
    public void setIntent(Intent intent) {
        this.intent = intent;
    }
    
    public ActionType getAction() {
        return action;
    }
    
    public void setAction(ActionType action) {
        this.action = action;
    }
    
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
    
    public boolean isRequiresRefresh() {
        return requiresRefresh;
    }
    
    public void setRequiresRefresh(boolean requiresRefresh) {
        this.requiresRefresh = requiresRefresh;
    }
    
    public List<String> getQuestions() {
        return questions;
    }
    
    public void setQuestions(List<String> questions) {
        this.questions = questions;
    }
    
    public String getConversationId() {
        return conversationId;
    }
    
    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
}
