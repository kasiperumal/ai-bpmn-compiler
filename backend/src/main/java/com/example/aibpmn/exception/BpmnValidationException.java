package com.example.aibpmn.exception;

/**
 * Exception thrown when BPMN model validation fails.
 */
public class BpmnValidationException extends RuntimeException {
    
    public BpmnValidationException(String message) {
        super(message);
    }
    
    public BpmnValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}

