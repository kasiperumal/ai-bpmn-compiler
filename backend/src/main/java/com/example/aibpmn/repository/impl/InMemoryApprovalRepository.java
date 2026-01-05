package com.example.aibpmn.repository.impl;

import com.example.aibpmn.model.Approval;
import com.example.aibpmn.repository.ApprovalRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of ApprovalRepository using ConcurrentHashMap
 */
@Repository
public class InMemoryApprovalRepository implements ApprovalRepository {
    
    private final ConcurrentHashMap<String, Approval> storage = new ConcurrentHashMap<>();
    
    @Override
    public Approval save(Approval approval) {
        if (approval == null || approval.getNodeId() == null) {
            throw new IllegalArgumentException("Approval and node ID cannot be null");
        }
        storage.put(approval.getNodeId(), approval);
        return approval;
    }
    
    @Override
    public Optional<Approval> findByNodeId(String nodeId) {
        if (nodeId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(storage.get(nodeId));
    }
    
    @Override
    public List<Approval> findAll() {
        return List.copyOf(storage.values());
    }
    
    @Override
    public List<Approval> findByAiApproved(Boolean aiApproved) {
        if (aiApproved == null) {
            return List.of();
        }
        return storage.values().stream()
                .filter(a -> aiApproved.equals(a.getAiApproved()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Approval> findByUserApproved(Boolean userApproved) {
        if (userApproved == null) {
            return List.of();
        }
        return storage.values().stream()
                .filter(a -> userApproved.equals(a.getUserApproved()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Approval> findFullyApproved() {
        return storage.values().stream()
                .filter(Approval::isFullyApproved)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Approval> findPendingApproval() {
        return storage.values().stream()
                .filter(Approval::isPendingApproval)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Approval> findByApprovedBy(String approvedBy) {
        if (approvedBy == null) {
            return List.of();
        }
        return storage.values().stream()
                .filter(a -> approvedBy.equals(a.getApprovedBy()))
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

