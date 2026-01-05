package com.example.aibpmn.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Canonical representation of a BPMN process
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProcessModel {
    
    private String id;
    private String name;
    private String version;
    private ProcessStatus status;
    private List<ProcessNode> nodes;
    private List<ProcessEdge> edges;
    private List<RuleModel> rules;
    
    public ProcessModel() {
        this.nodes = new ArrayList<>();
        this.edges = new ArrayList<>();
        this.rules = new ArrayList<>();
        this.status = ProcessStatus.DRAFT;
    }
    
    public ProcessModel(String id, String name, String version) {
        this();
        this.id = id;
        this.name = name;
        this.version = version;
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
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
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

