package com.example.aibpmn.dto;

import com.example.aibpmn.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of AI reasoning over a process description.
 * Contains extracted nodes, edges, rules, explanations, and clarification flags.
 */
public class ReasoningResult {
    
    // NEW: BPMN Moddle JSON (primary output)
    private String bpmnModdleJson;
    private String processName;
    private String overallExplanation;
    private String drlFileName; // Suggested DRL filename (e.g., "LeaveValidationRules.drl")
    
    // LEGACY: For backward compatibility (deprecated)
    private List<ProcessNode> nodes;
    private List<ProcessEdge> edges;
    private List<RuleModel> rules;
    private List<Explanation> explanations;
    private boolean clarificationRequired;
    private List<String> clarificationReasons;
    
    public ReasoningResult() {
        this.nodes = new ArrayList<>();
        this.edges = new ArrayList<>();
        this.rules = new ArrayList<>();
        this.explanations = new ArrayList<>();
        this.clarificationReasons = new ArrayList<>();
        this.clarificationRequired = false;
    }
    
    // Getters and Setters
    
    public String getBpmnModdleJson() {
        return bpmnModdleJson;
    }
    
    public void setBpmnModdleJson(String bpmnModdleJson) {
        this.bpmnModdleJson = bpmnModdleJson;
    }
    
    public String getProcessName() {
        return processName;
    }
    
    public void setProcessName(String processName) {
        this.processName = processName;
    }
    
    public String getOverallExplanation() {
        return overallExplanation;
    }
    
    public void setOverallExplanation(String overallExplanation) {
        this.overallExplanation = overallExplanation;
    }
    
    public String getDrlFileName() {
        return drlFileName;
    }
    
    public void setDrlFileName(String drlFileName) {
        this.drlFileName = drlFileName;
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
    
    public List<Explanation> getExplanations() {
        return explanations;
    }
    
    public void setExplanations(List<Explanation> explanations) {
        this.explanations = explanations;
    }
    
    public boolean isClarificationRequired() {
        return clarificationRequired;
    }
    
    public void setClarificationRequired(boolean clarificationRequired) {
        this.clarificationRequired = clarificationRequired;
    }
    
    public List<String> getClarificationReasons() {
        return clarificationReasons;
    }
    
    public void setClarificationReasons(List<String> clarificationReasons) {
        this.clarificationReasons = clarificationReasons;
    }
    
    // Helper methods
    
    public void addNode(ProcessNode node) {
        this.nodes.add(node);
    }
    
    public void addEdge(ProcessEdge edge) {
        this.edges.add(edge);
    }
    
    public void addRule(RuleModel rule) {
        this.rules.add(rule);
    }
    
    public void addExplanation(Explanation explanation) {
        this.explanations.add(explanation);
    }
    
    public void addClarificationReason(String reason) {
        this.clarificationReasons.add(reason);
        this.clarificationRequired = true;
    }
    
    public int getTotalElements() {
        return nodes.size() + edges.size() + rules.size();
    }
    
    @Override
    public String toString() {
        return String.format(
            "ReasoningResult[nodes=%d, edges=%d, rules=%d, explanations=%d, clarificationRequired=%s]",
            nodes.size(), edges.size(), rules.size(), explanations.size(), clarificationRequired
        );
    }
}

