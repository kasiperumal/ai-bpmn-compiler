package com.example.aibpmn.model;

/**
 * Status of a Drools rule set
 */
public enum RuleStatus {
    /**
     * Rule is being drafted
     */
    DRAFT,
    
    /**
     * Rule syntax has been validated
     */
    VALIDATED,
    
    /**
     * Rule is active and can be executed
     */
    ACTIVE,
    
    /**
     * Rule is deprecated and should not be used
     */
    DEPRECATED
}
