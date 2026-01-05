package com.example.aibpmn.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Explanation for why a node exists or was generated
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Explanation {
    
    private String nodeId;
    private String reason;
    private String source;
    private Double confidenceScore;
    private LocalDateTime timestamp;
    
    public Explanation() {
        this.timestamp = LocalDateTime.now();
    }
    
    public Explanation(String nodeId, String reason) {
        this();
        this.nodeId = nodeId;
        this.reason = reason;
    }
    
    // Getters and Setters
    
    public String getNodeId() {
        return nodeId;
    }
    
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public Double getConfidenceScore() {
        return confidenceScore;
    }
    
    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Explanation that = (Explanation) o;
        return Objects.equals(nodeId, that.nodeId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(nodeId);
    }
    
    @Override
    public String toString() {
        return "Explanation{" +
                "nodeId='" + nodeId + '\'' +
                ", reason='" + reason + '\'' +
                ", source='" + source + '\'' +
                ", confidenceScore=" + confidenceScore +
                '}';
    }
}

