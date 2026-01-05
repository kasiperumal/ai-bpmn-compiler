package com.example.aibpmn.dto;

import com.example.aibpmn.model.AiState;

/**
 * Information about the current AI processing state
 */
public class AiStateInfo {
    
    private String processId;
    private AiState currentState;
    private AiState nextState;
    private String description;
    private boolean requiresUserAction;
    private boolean isComplete;
    private boolean isFailed;
    
    public AiStateInfo() {
    }
    
    public AiStateInfo(String processId, AiState currentState) {
        this.processId = processId;
        this.currentState = currentState;
        this.nextState = currentState.getNextState();
        this.description = currentState.getDescription();
        this.requiresUserAction = currentState.requiresUserAction();
        this.isComplete = currentState.isCompleted();
        this.isFailed = currentState.isFailed();
    }
    
    // Getters and Setters
    
    public String getProcessId() {
        return processId;
    }
    
    public void setProcessId(String processId) {
        this.processId = processId;
    }
    
    public AiState getCurrentState() {
        return currentState;
    }
    
    public void setCurrentState(AiState currentState) {
        this.currentState = currentState;
    }
    
    public AiState getNextState() {
        return nextState;
    }
    
    public void setNextState(AiState nextState) {
        this.nextState = nextState;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public boolean isRequiresUserAction() {
        return requiresUserAction;
    }
    
    public void setRequiresUserAction(boolean requiresUserAction) {
        this.requiresUserAction = requiresUserAction;
    }
    
    public boolean isComplete() {
        return isComplete;
    }
    
    public void setComplete(boolean complete) {
        isComplete = complete;
    }
    
    public boolean isFailed() {
        return isFailed;
    }
    
    public void setFailed(boolean failed) {
        isFailed = failed;
    }
}

