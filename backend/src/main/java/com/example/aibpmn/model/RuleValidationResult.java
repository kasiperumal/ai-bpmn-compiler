package com.example.aibpmn.model;

/**
 * Drools fact model for rule validation results.
 * This class captures the outcome of business rule execution.
 * DO NOT EDIT MANUALLY - will be regenerated on next DRL generation.
 */
public class RuleValidationResult {

    private String status;
    private String reason;

    public RuleValidationResult() {
    }

    public RuleValidationResult(String status, String reason) {
        this.status = status;
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public String toString() {
        return "RuleValidationResult{" +
                "status='" + status + '\'' +
                ", reason='" + reason + '\'' +
                '}';
    }
}
