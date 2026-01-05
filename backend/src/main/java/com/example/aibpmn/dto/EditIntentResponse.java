package com.example.aibpmn.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO for edit intent response.
 * Contains information about the edit operation result.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EditIntentResponse {
    
    private boolean success;
    private String message;
    private String modifiedNodeId;
    private boolean bpmnRegenerated;
    
    public EditIntentResponse() {
    }
    
    public EditIntentResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getModifiedNodeId() {
        return modifiedNodeId;
    }
    
    public void setModifiedNodeId(String modifiedNodeId) {
        this.modifiedNodeId = modifiedNodeId;
    }
    
    public boolean isBpmnRegenerated() {
        return bpmnRegenerated;
    }
    
    public void setBpmnRegenerated(boolean bpmnRegenerated) {
        this.bpmnRegenerated = bpmnRegenerated;
    }
    
    @Override
    public String toString() {
        return "EditIntentResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", modifiedNodeId='" + modifiedNodeId + '\'' +
                ", bpmnRegenerated=" + bpmnRegenerated +
                '}';
    }
}

