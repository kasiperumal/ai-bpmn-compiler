package com.example.aibpmn.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Approval status for a node (AI and/or user approval)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Approval {
    
    private String nodeId;
    private Boolean aiApproved;
    private Boolean userApproved;
    private String aiComment;
    private String userComment;
    private String approvedBy;
    private LocalDateTime aiApprovedAt;
    private LocalDateTime userApprovedAt;
    
    public Approval() {
    }
    
    public Approval(String nodeId) {
        this.nodeId = nodeId;
        this.aiApproved = false;
        this.userApproved = false;
    }
    
    public Approval(String nodeId, Boolean aiApproved, Boolean userApproved) {
        this.nodeId = nodeId;
        this.aiApproved = aiApproved;
        this.userApproved = userApproved;
    }
    
    // Getters and Setters
    
    public String getNodeId() {
        return nodeId;
    }
    
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }
    
    public Boolean getAiApproved() {
        return aiApproved;
    }
    
    public void setAiApproved(Boolean aiApproved) {
        this.aiApproved = aiApproved;
        if (aiApproved != null && aiApproved) {
            this.aiApprovedAt = LocalDateTime.now();
        }
    }
    
    public Boolean getUserApproved() {
        return userApproved;
    }
    
    public void setUserApproved(Boolean userApproved) {
        this.userApproved = userApproved;
        if (userApproved != null && userApproved) {
            this.userApprovedAt = LocalDateTime.now();
        }
    }
    
    public String getAiComment() {
        return aiComment;
    }
    
    public void setAiComment(String aiComment) {
        this.aiComment = aiComment;
    }
    
    public String getUserComment() {
        return userComment;
    }
    
    public void setUserComment(String userComment) {
        this.userComment = userComment;
    }
    
    public String getApprovedBy() {
        return approvedBy;
    }
    
    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }
    
    public LocalDateTime getAiApprovedAt() {
        return aiApprovedAt;
    }
    
    public void setAiApprovedAt(LocalDateTime aiApprovedAt) {
        this.aiApprovedAt = aiApprovedAt;
    }
    
    public LocalDateTime getUserApprovedAt() {
        return userApprovedAt;
    }
    
    public void setUserApprovedAt(LocalDateTime userApprovedAt) {
        this.userApprovedAt = userApprovedAt;
    }
    
    // Utility methods
    
    public boolean isFullyApproved() {
        return Boolean.TRUE.equals(aiApproved) && Boolean.TRUE.equals(userApproved);
    }
    
    public boolean isPendingApproval() {
        return !Boolean.TRUE.equals(aiApproved) || !Boolean.TRUE.equals(userApproved);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Approval approval = (Approval) o;
        return Objects.equals(nodeId, approval.nodeId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(nodeId);
    }
    
    @Override
    public String toString() {
        return "Approval{" +
                "nodeId='" + nodeId + '\'' +
                ", aiApproved=" + aiApproved +
                ", userApproved=" + userApproved +
                ", approvedBy='" + approvedBy + '\'' +
                '}';
    }
}

