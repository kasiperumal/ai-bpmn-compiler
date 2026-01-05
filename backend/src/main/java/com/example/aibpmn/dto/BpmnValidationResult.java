package com.example.aibpmn.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Result of BPMN validation containing errors and warnings.
 */
public class BpmnValidationResult {
    
    private final boolean valid;
    private final List<ValidationError> errors;
    private final List<ValidationWarning> warnings;
    
    public BpmnValidationResult(boolean valid, List<ValidationError> errors, List<ValidationWarning> warnings) {
        this.valid = valid;
        this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
        this.warnings = warnings != null ? new ArrayList<>(warnings) : new ArrayList<>();
    }
    
    public static BpmnValidationResult success() {
        return new BpmnValidationResult(true, Collections.emptyList(), Collections.emptyList());
    }
    
    public static BpmnValidationResult failure(List<ValidationError> errors) {
        return new BpmnValidationResult(false, errors, Collections.emptyList());
    }
    
    public boolean isValid() {
        return valid;
    }
    
    public List<ValidationError> getErrors() {
        return Collections.unmodifiableList(errors);
    }
    
    public List<ValidationWarning> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }
    
    public String getErrorSummary() {
        if (valid) {
            return "Valid";
        }
        StringBuilder sb = new StringBuilder("Validation failed with " + errors.size() + " error(s):\n");
        for (ValidationError error : errors) {
            sb.append("  - ").append(error.getMessage()).append("\n");
        }
        return sb.toString();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BpmnValidationResult that = (BpmnValidationResult) o;
        return valid == that.valid && 
               Objects.equals(errors, that.errors) && 
               Objects.equals(warnings, that.warnings);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(valid, errors, warnings);
    }
    
    @Override
    public String toString() {
        return "BpmnValidationResult{" +
               "valid=" + valid +
               ", errors=" + errors.size() +
               ", warnings=" + warnings.size() +
               '}';
    }
    
    /**
     * Represents a validation error.
     */
    public static class ValidationError {
        private final String code;
        private final String message;
        private final String elementId;
        private final ErrorSeverity severity;
        
        public ValidationError(String code, String message, String elementId, ErrorSeverity severity) {
            this.code = code;
            this.message = message;
            this.elementId = elementId;
            this.severity = severity;
        }
        
        public ValidationError(String code, String message) {
            this(code, message, null, ErrorSeverity.ERROR);
        }
        
        public String getCode() {
            return code;
        }
        
        public String getMessage() {
            return message;
        }
        
        public String getElementId() {
            return elementId;
        }
        
        public ErrorSeverity getSeverity() {
            return severity;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ValidationError that = (ValidationError) o;
            return Objects.equals(code, that.code) && 
                   Objects.equals(message, that.message) && 
                   Objects.equals(elementId, that.elementId) && 
                   severity == that.severity;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(code, message, elementId, severity);
        }
        
        @Override
        public String toString() {
            return "ValidationError{" +
                   "code='" + code + '\'' +
                   ", message='" + message + '\'' +
                   ", elementId='" + elementId + '\'' +
                   ", severity=" + severity +
                   '}';
        }
    }
    
    /**
     * Represents a validation warning.
     */
    public static class ValidationWarning {
        private final String code;
        private final String message;
        private final String elementId;
        
        public ValidationWarning(String code, String message, String elementId) {
            this.code = code;
            this.message = message;
            this.elementId = elementId;
        }
        
        public ValidationWarning(String code, String message) {
            this(code, message, null);
        }
        
        public String getCode() {
            return code;
        }
        
        public String getMessage() {
            return message;
        }
        
        public String getElementId() {
            return elementId;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ValidationWarning that = (ValidationWarning) o;
            return Objects.equals(code, that.code) && 
                   Objects.equals(message, that.message) && 
                   Objects.equals(elementId, that.elementId);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(code, message, elementId);
        }
        
        @Override
        public String toString() {
            return "ValidationWarning{" +
                   "code='" + code + '\'' +
                   ", message='" + message + '\'' +
                   ", elementId='" + elementId + '\'' +
                   '}';
        }
    }
    
    /**
     * Error severity levels.
     */
    public enum ErrorSeverity {
        ERROR,
        CRITICAL
    }
}

