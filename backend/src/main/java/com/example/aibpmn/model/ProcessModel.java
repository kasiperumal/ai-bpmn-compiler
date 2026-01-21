package com.example.aibpmn.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * ═══════════════════════════════════════════════════════════════════════
 * BPMN PROCESS MODEL - Hybrid Storage Architecture
 * ═══════════════════════════════════════════════════════════════════════
 * 
 * Primary Storage: BPMN Moddle JSON (full BPMN 2.0 specification)
 * Secondary Storage: Metadata (for efficient queries)
 * 
 * This approach allows:
 * - GenAI to output BPMN directly
 * - Frontend to work with standard BPMN
 * - Backend to query efficiently
 * - Full BPMN 2.0 feature support
 * 
 * ═══════════════════════════════════════════════════════════════════════
 */
@Entity
@Table(name = "process_models")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProcessModel {
    
    @Id
    private String id;
    
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    // ═══════════════════════════════════════════════════════════════════
    // PRIMARY STORAGE: BPMN Moddle JSON
    // ═══════════════════════════════════════════════════════════════════
    @Column(name = "bpmn_moddle_json", columnDefinition = "TEXT", nullable = false)
    private String bpmnModdleJson;
    
    // ═══════════════════════════════════════════════════════════════════
    // METADATA: For queries and UI display
    // ═══════════════════════════════════════════════════════════════════
    @Embedded
    private BpmnMetadata metadata;
    
    @Enumerated(EnumType.STRING)
    private ProcessStatus status;
    
    private Integer version;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // ═══════════════════════════════════════════════════════════════════
    // LEGACY FIELDS (for backward compatibility - will be deprecated)
    // ═══════════════════════════════════════════════════════════════════
    @Transient
    private List<ProcessNode> nodes;
    
    @Transient
    private List<ProcessEdge> edges;
    
    @Transient
    private List<RuleModel> rules;
    
    public ProcessModel() {
        this.nodes = new ArrayList<>();
        this.edges = new ArrayList<>();
        this.rules = new ArrayList<>();
        this.status = ProcessStatus.DRAFT;
        this.version = 1;
        this.metadata = new BpmnMetadata(); // Initialize metadata
    }
    
    public ProcessModel(String id, String name) {
        this();
        this.id = id;
        this.name = name;
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
    
    public Integer getVersion() {
        return version;
    }
    
    public void setVersion(Integer version) {
        this.version = version;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getBpmnModdleJson() {
        return bpmnModdleJson;
    }
    
    public void setBpmnModdleJson(String bpmnModdleJson) {
        this.bpmnModdleJson = bpmnModdleJson;
    }
    
    public BpmnMetadata getMetadata() {
        return metadata;
    }
    
    public void setMetadata(BpmnMetadata metadata) {
        this.metadata = metadata;
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
    
    public ProcessStatus getStatus() {
        return status;
    }
    
    public void setStatus(ProcessStatus status) {
        this.status = status;
    }
    
    public List<ProcessNode> getNodes() {
        return nodes;
    }
    
    public void setNodes(List<ProcessNode> nodes) {
        this.nodes = nodes;
    }
    
    public List<ProcessEdge> getEdges() {
        return edges;
    }
    
    public void setEdges(List<ProcessEdge> edges) {
        this.edges = edges;
    }
    
    public List<RuleModel> getRules() {
        return rules;
    }
    
    public void setRules(List<RuleModel> rules) {
        this.rules = rules;
    }
    
    // Utility methods
    
    public void addNode(ProcessNode node) {
        this.nodes.add(node);
    }
    
    public void addEdge(ProcessEdge edge) {
        this.edges.add(edge);
    }
    
    public void addRule(RuleModel rule) {
        this.rules.add(rule);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessModel that = (ProcessModel) o;
        return Objects.equals(id, that.id) && 
               Objects.equals(version, that.version);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, version);
    }
    
    @Override
    public String toString() {
        return "ProcessModel{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", status=" + status +
                ", nodes=" + nodes.size() +
                ", edges=" + edges.size() +
                ", rules=" + rules.size() +
                '}';
    }
}

