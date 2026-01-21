package com.example.aibpmn.service;

import java.util.Collections;
import java.util.List;

/**
 * Result of BPMN validation
 */
public class ValidationResult {
    
    private final boolean valid;
    private final List<String> errors;
    private final List<String> warnings;
    
    private ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
        this.valid = valid;
        this.errors = errors != null ? errors : Collections.emptyList();
        this.warnings = warnings != null ? warnings : Collections.emptyList();
    }
    
    public static ValidationResult valid(List<String> warnings) {
        return new ValidationResult(true, Collections.emptyList(), warnings);
    }
    
    public static ValidationResult invalid(List<String> errors, List<String> warnings) {
        return new ValidationResult(false, errors, warnings);
    }
    
    public boolean isValid() {
        return valid;
    }
    
    public List<String> getErrors() {
        return errors;
    }
    
    public List<String> getWarnings() {
        return warnings;
    }
}
