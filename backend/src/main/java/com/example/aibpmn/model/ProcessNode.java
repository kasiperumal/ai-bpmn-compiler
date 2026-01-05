package com.example.aibpmn.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a node in a process flow
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProcessNode {
    
    private String id;
    private NodeType type;
    private String name;
    private Map<String, Object> properties;
    private Explanation explanation;
    private Approval approval;
    
    public ProcessNode() {
        this.properties = new HashMap<>();
    }
    
    public ProcessNode(String id, NodeType type, String name) {
        this();
        this.id = id;
        this.type = type;
        this.name = name;
    }
    
    // Getters and Setters
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public NodeType getType() {
        return type;
    }
    
    public void setType(NodeType type) {
        this.type = type;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Map<String, Object> getProperties() {
        return properties;
    }
    
    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
    
    public Explanation getExplanation() {
        return explanation;
    }
    
    public void setExplanation(Explanation explanation) {
        this.explanation = explanation;
    }
    
    public Approval getApproval() {
        return approval;
    }
    
    public void setApproval(Approval approval) {
        this.approval = approval;
    }
    
    // Utility methods
    
    public void addProperty(String key, Object value) {
        this.properties.put(key, value);
    }
    
    public Object getProperty(String key) {
        return this.properties.get(key);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessNode that = (ProcessNode) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "ProcessNode{" +
                "id='" + id + '\'' +
                ", type=" + type +
                ", name='" + name + '\'' +
                ", properties=" + properties.size() +
                '}';
    }
}

