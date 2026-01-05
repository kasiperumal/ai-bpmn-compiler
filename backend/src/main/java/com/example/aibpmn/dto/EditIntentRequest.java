package com.example.aibpmn.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO for receiving edit intent requests from the frontend.
 * Contains natural language instructions for editing a process.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EditIntentRequest {
    
    private String instruction;
    private String nodeId;
    
    public EditIntentRequest() {
    }
    
    public EditIntentRequest(String instruction, String nodeId) {
        this.instruction = instruction;
        this.nodeId = nodeId;
    }
    
    /**
     * Natural language instruction describing the desired edit.
     * Examples:
     * - "Rename this task to 'Review Application'"
     * - "Change the approval condition to amount > 10000"
     * - "Add a new task after this one called 'Send Notification'"
     */
    public String getInstruction() {
        return instruction;
    }
    
    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }
    
    /**
     * Optional: The ID of the node being edited.
     * If not provided, the instruction should be process-level.
     */
    public String getNodeId() {
        return nodeId;
    }
    
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }
    
    @Override
    public String toString() {
        return "EditIntentRequest{" +
                "instruction='" + instruction + '\'' +
                ", nodeId='" + nodeId + '\'' +
                '}';
    }
}

