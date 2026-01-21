package com.example.aibpmn.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Drools Rule Set - DRL storage and Java model tracking
 */
@Entity
@Table(name = "rule_sets")
public class RuleSet {
    
    @Id
    private String id;
    
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    // DRL (Drools Rule Language)
    @Column(name = "drl", columnDefinition = "TEXT")
    private String drl;
    
    // Java models used in this rule
    @ElementCollection
    @CollectionTable(
        name = "rule_java_models_used",
        joinColumns = @JoinColumn(name = "rule_set_id")
    )
    @Column(name = "model_class")
    private List<String> javaModelsUsed = new ArrayList<>();
    
    // Java models created for this rule
    @ElementCollection
    @CollectionTable(
        name = "rule_java_models_created",
        joinColumns = @JoinColumn(name = "rule_set_id")
    )
    @Column(name = "model_class")
    private List<String> javaModelsCreated = new ArrayList<>();
    
    // Explanation of the rule logic
    @Column(columnDefinition = "TEXT")
    private String explanation;
    
    // Link to process and task
    @Column(name = "process_id")
    private String processId;
    
    @Column(name = "task_id")
    private String taskId; // BusinessRuleTask ID
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Enumerated(EnumType.STRING)
    private RuleStatus status;
    
    public RuleSet() {
        this.status = RuleStatus.DRAFT;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getDrl() {
        return drl;
    }
    
    public void setDrl(String drl) {
        this.drl = drl;
    }
    
    public List<String> getJavaModelsUsed() {
        return javaModelsUsed;
    }
    
    public void setJavaModelsUsed(List<String> javaModelsUsed) {
        this.javaModelsUsed = javaModelsUsed;
    }
    
    public List<String> getJavaModelsCreated() {
        return javaModelsCreated;
    }
    
    public void setJavaModelsCreated(List<String> javaModelsCreated) {
        this.javaModelsCreated = javaModelsCreated;
    }
    
    public String getExplanation() {
        return explanation;
    }
    
    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
    
    public String getProcessId() {
        return processId;
    }
    
    public void setProcessId(String processId) {
        this.processId = processId;
    }
    
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public RuleStatus getStatus() {
        return status;
    }
    
    public void setStatus(RuleStatus status) {
        this.status = status;
    }
}
