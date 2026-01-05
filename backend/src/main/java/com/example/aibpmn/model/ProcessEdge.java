package com.example.aibpmn.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Objects;

/**
 * Represents a connection between two process nodes
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProcessEdge {
    
    private String id;
    private String fromNodeId;
    private String toNodeId;
    private String condition;
    private String label;
    
    public ProcessEdge() {
    }
    
    public ProcessEdge(String fromNodeId, String toNodeId) {
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
    }
    
    public ProcessEdge(String fromNodeId, String toNodeId, String condition) {
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
        this.condition = condition;
    }
    
    // Getters and Setters
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getFromNodeId() {
        return fromNodeId;
    }
    
    public void setFromNodeId(String fromNodeId) {
        this.fromNodeId = fromNodeId;
    }
    
    public String getToNodeId() {
        return toNodeId;
    }
    
    public void setToNodeId(String toNodeId) {
        this.toNodeId = toNodeId;
    }
    
    public String getCondition() {
        return condition;
    }
    
    public void setCondition(String condition) {
        this.condition = condition;
    }
    
    public String getLabel() {
        return label;
    }
    
    public void setLabel(String label) {
        this.label = label;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessEdge that = (ProcessEdge) o;
        return Objects.equals(fromNodeId, that.fromNodeId) && 
               Objects.equals(toNodeId, that.toNodeId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(fromNodeId, toNodeId);
    }
    
    @Override
    public String toString() {
        return "ProcessEdge{" +
                "id='" + id + '\'' +
                ", from='" + fromNodeId + '\'' +
                ", to='" + toNodeId + '\'' +
                ", condition='" + condition + '\'' +
                '}';
    }
}

