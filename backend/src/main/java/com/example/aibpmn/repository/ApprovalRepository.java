package com.example.aibpmn.repository;

import com.example.aibpmn.model.Approval;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Approval persistence
 */
public interface ApprovalRepository {
    
    /**
     * Save an approval
     * @param approval The approval to save
     * @return The saved approval
     */
    Approval save(Approval approval);
    
    /**
     * Find an approval by node ID
     * @param nodeId The node ID
     * @return Optional containing the approval if found
     */
    Optional<Approval> findByNodeId(String nodeId);
    
    /**
     * Find all approvals
     * @return List of all approvals
     */
    List<Approval> findAll();
    
    /**
     * Find approvals where AI has approved
     * @param aiApproved The AI approval status
     * @return List of matching approvals
     */
    List<Approval> findByAiApproved(Boolean aiApproved);
    
    /**
     * Find approvals where user has approved
     * @param userApproved The user approval status
     * @return List of matching approvals
     */
    List<Approval> findByUserApproved(Boolean userApproved);
    
    /**
     * Find approvals that are fully approved (both AI and user)
     * @return List of fully approved approvals
     */
    List<Approval> findFullyApproved();
    
    /**
     * Find approvals pending approval
     * @return List of approvals pending approval
     */
    List<Approval> findPendingApproval();
    
    /**
     * Find approvals by approver
     * @param approvedBy The approver identifier
     * @return List of approvals by the given approver
     */
    List<Approval> findByApprovedBy(String approvedBy);
    
    /**
     * Check if an approval exists for a node
     * @param nodeId The node ID
     * @return true if exists, false otherwise
     */
    boolean existsByNodeId(String nodeId);
    
    /**
     * Delete an approval by node ID
     * @param nodeId The node ID
     * @return true if deleted, false if not found
     */
    boolean deleteByNodeId(String nodeId);
    
    /**
     * Delete all approvals
     */
    void deleteAll();
    
    /**
     * Count all approvals
     * @return The total number of approvals
     */
    long count();
}

