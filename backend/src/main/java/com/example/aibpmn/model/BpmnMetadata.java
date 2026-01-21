package com.example.aibpmn.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import java.util.ArrayList;
import java.util.List;

/**
 * Embeddable metadata extracted from BPMN Moddle JSON
 * for efficient database queries without parsing full JSON.
 */
@Embeddable
public class BpmnMetadata {
    
    @Column(name = "process_name")
    private String processName;
    
    @Column(name = "element_count")
    private Integer elementCount;
    
    @Column(name = "task_count")
    private Integer taskCount;
    
    @Column(name = "gateway_count")
    private Integer gatewayCount;
    
    @Column(name = "business_rule_task_count")
    private Integer businessRuleTaskCount;
    
    @ElementCollection
    @CollectionTable(
        name = "process_business_rule_tasks",
        joinColumns = @JoinColumn(name = "process_id")
    )
    @Column(name = "task_id")
    private List<String> businessRuleTaskIds = new ArrayList<>();
    
    @Column(name = "has_business_rules")
    private Boolean hasBusinessRules;
    
    // Constructors
    public BpmnMetadata() {
    }
    
    public BpmnMetadata(
        String processName,
        Integer elementCount,
        Integer taskCount,
        Integer gatewayCount,
        Integer businessRuleTaskCount,
        List<String> businessRuleTaskIds
    ) {
        this.processName = processName;
        this.elementCount = elementCount;
        this.taskCount = taskCount;
        this.gatewayCount = gatewayCount;
        this.businessRuleTaskCount = businessRuleTaskCount;
        this.businessRuleTaskIds = businessRuleTaskIds != null ? businessRuleTaskIds : new ArrayList<>();
        this.hasBusinessRules = businessRuleTaskCount != null && businessRuleTaskCount > 0;
    }
    
    // Getters and Setters
    public String getProcessName() {
        return processName;
    }
    
    public void setProcessName(String processName) {
        this.processName = processName;
    }
    
    public Integer getElementCount() {
        return elementCount;
    }
    
    public void setElementCount(Integer elementCount) {
        this.elementCount = elementCount;
    }
    
    public Integer getTaskCount() {
        return taskCount;
    }
    
    public void setTaskCount(Integer taskCount) {
        this.taskCount = taskCount;
    }
    
    public Integer getGatewayCount() {
        return gatewayCount;
    }
    
    public void setGatewayCount(Integer gatewayCount) {
        this.gatewayCount = gatewayCount;
    }
    
    public Integer getBusinessRuleTaskCount() {
        return businessRuleTaskCount;
    }
    
    public void setBusinessRuleTaskCount(Integer businessRuleTaskCount) {
        this.businessRuleTaskCount = businessRuleTaskCount;
        this.hasBusinessRules = businessRuleTaskCount != null && businessRuleTaskCount > 0;
    }
    
    public List<String> getBusinessRuleTaskIds() {
        return businessRuleTaskIds;
    }
    
    public void setBusinessRuleTaskIds(List<String> businessRuleTaskIds) {
        this.businessRuleTaskIds = businessRuleTaskIds;
    }
    
    public Boolean getHasBusinessRules() {
        return hasBusinessRules;
    }
    
    public void setHasBusinessRules(Boolean hasBusinessRules) {
        this.hasBusinessRules = hasBusinessRules;
    }
}
