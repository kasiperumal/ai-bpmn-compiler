package com.example.aibpmn.model;

/**
 * Represents the current state of AI processing for a BPMN process
 */
public enum AiState {
    
    /**
     * Initial state: Image has been uploaded and stored
     */
    IMAGE_RECEIVED,
    
    /**
     * Initial state: Text description has been received and stored
     */
    TEXT_RECEIVED,
    
    /**
     * AI has successfully inferred the process structure from input
     */
    PROCESS_INFERRED,
    
    /**
     * AI needs clarification from user before proceeding
     */
    CLARIFICATION_REQUIRED,
    
    /**
     * Process model has been created and is ready for review
     */
    MODEL_READY,
    
    /**
     * BPMN XML has been generated from the model
     */
    BPMN_GENERATED,
    
    /**
     * DRL (Drools Rule Language) has been generated for business rules
     */
    DRL_GENERATED,
    
    /**
     * Process has been published and is ready for execution
     */
    PUBLISHED,
    
    /**
     * AI processing has failed
     */
    FAILED;
    
    /**
     * Check if this is an initial state
     * 
     * @return true if IMAGE_RECEIVED or TEXT_RECEIVED
     */
    public boolean isInitialState() {
        return this == IMAGE_RECEIVED || this == TEXT_RECEIVED;
    }
    
    /**
     * Check if this is a terminal state
     * 
     * @return true if PUBLISHED or FAILED
     */
    public boolean isTerminalState() {
        return this == PUBLISHED || this == FAILED;
    }
    
    /**
     * Check if this state requires user action
     * 
     * @return true if CLARIFICATION_REQUIRED or MODEL_READY
     */
    public boolean requiresUserAction() {
        return this == CLARIFICATION_REQUIRED || this == MODEL_READY;
    }
    
    /**
     * Check if processing has completed successfully
     * 
     * @return true if state is PUBLISHED
     */
    public boolean isCompleted() {
        return this == PUBLISHED;
    }
    
    /**
     * Check if processing has failed
     * 
     * @return true if state is FAILED
     */
    public boolean isFailed() {
        return this == FAILED;
    }
    
    /**
     * Get the next expected state in the normal workflow
     * 
     * @return The next state, or null if at terminal state or clarification needed
     */
    public AiState getNextState() {
        switch (this) {
            case IMAGE_RECEIVED:
            case TEXT_RECEIVED:
                return PROCESS_INFERRED;
            case PROCESS_INFERRED:
                return MODEL_READY;
            case MODEL_READY:
                return BPMN_GENERATED;
            case BPMN_GENERATED:
                return DRL_GENERATED;
            case DRL_GENERATED:
                return PUBLISHED;
            case CLARIFICATION_REQUIRED:
            case PUBLISHED:
            case FAILED:
            default:
                return null;
        }
    }
    
    /**
     * Get a human-readable description of this state
     * 
     * @return Description string
     */
    public String getDescription() {
        switch (this) {
            case IMAGE_RECEIVED:
                return "Image has been uploaded and is waiting for AI processing";
            case TEXT_RECEIVED:
                return "Text description has been received and is waiting for AI processing";
            case PROCESS_INFERRED:
                return "AI has successfully analyzed the input and inferred the process structure";
            case CLARIFICATION_REQUIRED:
                return "AI needs additional information or clarification from user";
            case MODEL_READY:
                return "Process model has been created and is ready for user review and approval";
            case BPMN_GENERATED:
                return "BPMN XML has been generated and is ready for deployment";
            case DRL_GENERATED:
                return "Business rules (DRL) have been generated";
            case PUBLISHED:
                return "Process has been published and is ready for execution";
            case FAILED:
                return "AI processing has failed";
            default:
                return "Unknown state";
        }
    }
}

