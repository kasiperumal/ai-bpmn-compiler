package com.example.aibpmn.dto;

/**
 * Request DTO for creating a process from text description
 */
public class ProcessTextRequest {
    
    private String description;
    private String name;
    
    public ProcessTextRequest() {
    }
    
    public ProcessTextRequest(String description) {
        this.description = description;
    }
    
    public ProcessTextRequest(String description, String name) {
        this.description = description;
        this.name = name;
    }
    
    // Getters and Setters
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
}

