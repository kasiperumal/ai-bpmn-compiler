package com.example.aibpmn.dto;

/**
 * Response DTO for process creation from text
 */
public class ProcessTextResponse {
    
    private String processId;
    private String name;
    private int descriptionLength;
    private String status;
    private String message;
    
    public ProcessTextResponse() {
    }
    
    public ProcessTextResponse(String processId, String name, int descriptionLength) {
        this.processId = processId;
        this.name = name;
        this.descriptionLength = descriptionLength;
        this.status = "SUCCESS";
        this.message = "Process created successfully from text description";
    }
    
    // Getters and Setters
    
    public String getProcessId() {
        return processId;
    }
    
    public void setProcessId(String processId) {
        this.processId = processId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public int getDescriptionLength() {
        return descriptionLength;
    }
    
    public void setDescriptionLength(int descriptionLength) {
        this.descriptionLength = descriptionLength;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}

