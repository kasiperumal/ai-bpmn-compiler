package com.example.aibpmn.repository.impl;

import com.example.aibpmn.model.Explanation;
import com.example.aibpmn.repository.ExplanationRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of ExplanationRepository using ConcurrentHashMap
 */
@Repository
public class InMemoryExplanationRepository implements ExplanationRepository {
    
    private final ConcurrentHashMap<String, Explanation> storage = new ConcurrentHashMap<>();
    
    @Override
    public Explanation save(Explanation explanation) {
        if (explanation == null || explanation.getNodeId() == null) {
            throw new IllegalArgumentException("Explanation and node ID cannot be null");
        }
        storage.put(explanation.getNodeId(), explanation);
        return explanation;
    }
    
    @Override
    public Optional<Explanation> findByNodeId(String nodeId) {
        if (nodeId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(storage.get(nodeId));
    }
    
    @Override
    public List<Explanation> findAll() {
        return List.copyOf(storage.values());
    }
    
    @Override
    public List<Explanation> findBySource(String source) {
        if (source == null) {
            return List.of();
        }
        return storage.values().stream()
                .filter(e -> source.equals(e.getSource()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Explanation> findByConfidenceScoreGreaterThan(Double minConfidence) {
        if (minConfidence == null) {
            return List.of();
        }
        return storage.values().stream()
                .filter(e -> e.getConfidenceScore() != null && 
                            e.getConfidenceScore() > minConfidence)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Explanation> findByReasonContaining(String text) {
        if (text == null) {
            return List.of();
        }
        String lowerCaseText = text.toLowerCase();
        return storage.values().stream()
                .filter(e -> e.getReason() != null && 
                            e.getReason().toLowerCase().contains(lowerCaseText))
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean existsByNodeId(String nodeId) {
        return nodeId != null && storage.containsKey(nodeId);
    }
    
    @Override
    public boolean deleteByNodeId(String nodeId) {
        if (nodeId == null) {
            return false;
        }
        return storage.remove(nodeId) != null;
    }
    
    @Override
    public void deleteAll() {
        storage.clear();
    }
    
    @Override
    public long count() {
        return storage.size();
    }
}

