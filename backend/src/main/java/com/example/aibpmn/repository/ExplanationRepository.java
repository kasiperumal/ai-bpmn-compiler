package com.example.aibpmn.repository;

import com.example.aibpmn.model.Explanation;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Explanation persistence
 */
public interface ExplanationRepository {
    
    /**
     * Save an explanation
     * @param explanation The explanation to save
     * @return The saved explanation
     */
    Explanation save(Explanation explanation);
    
    /**
     * Find an explanation by node ID
     * @param nodeId The node ID
     * @return Optional containing the explanation if found
     */
    Optional<Explanation> findByNodeId(String nodeId);
    
    /**
     * Find all explanations
     * @return List of all explanations
     */
    List<Explanation> findAll();
    
    /**
     * Find explanations by source
     * @param source The explanation source (e.g., "AI-Generated", "User-Defined")
     * @return List of explanations from the given source
     */
    List<Explanation> findBySource(String source);
    
    /**
     * Find explanations with confidence score above threshold
     * @param minConfidence Minimum confidence score
     * @return List of explanations meeting the threshold
     */
    List<Explanation> findByConfidenceScoreGreaterThan(Double minConfidence);
    
    /**
     * Find explanations by reason containing text (case-insensitive)
     * @param text Text to search for in reason
     * @return List of matching explanations
     */
    List<Explanation> findByReasonContaining(String text);
    
    /**
     * Check if an explanation exists for a node
     * @param nodeId The node ID
     * @return true if exists, false otherwise
     */
    boolean existsByNodeId(String nodeId);
    
    /**
     * Delete an explanation by node ID
     * @param nodeId The node ID
     * @return true if deleted, false if not found
     */
    boolean deleteByNodeId(String nodeId);
    
    /**
     * Delete all explanations
     */
    void deleteAll();
    
    /**
     * Count all explanations
     * @return The total number of explanations
     */
    long count();
}

