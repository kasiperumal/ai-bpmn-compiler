package com.example.aibpmn.exception;

import java.util.List;

/**
 * Exception thrown when DRL validation fails.
 */
public class DrlValidationException extends RuntimeException {
    
    private final List<String> errors;
    
    public DrlValidationException(String message, List<String> errors) {
        super(message);
        this.errors = errors;
    }
    
    public DrlValidationException(String message) {
        super(message);
        this.errors = List.of(message);
    }
    
    public List<String> getErrors() {
        return errors;
    }
    
    public String getErrorSummary() {
        if (errors.isEmpty()) {
            return getMessage();
        }
        StringBuilder sb = new StringBuilder(getMessage()).append(":\n");
        for (String error : errors) {
            sb.append("  - ").append(error).append("\n");
        }
        return sb.toString();
    }
}

