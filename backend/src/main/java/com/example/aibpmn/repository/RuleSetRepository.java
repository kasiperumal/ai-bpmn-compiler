package com.example.aibpmn.repository;

import com.example.aibpmn.model.RuleSet;
import com.example.aibpmn.model.RuleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Drools Rule Sets
 */
@Repository
public interface RuleSetRepository extends JpaRepository<RuleSet, String> {
    
    /**
     * Find all rules for a given process
     */
    List<RuleSet> findByProcessId(String processId);
    
    /**
     * Find rule attached to a specific BusinessRuleTask
     */
    Optional<RuleSet> findByTaskId(String taskId);
    
    /**
     * Find rules by status
     */
    List<RuleSet> findByStatus(RuleStatus status);
}
