package com.example.aibpmn.repository.impl;

import com.example.aibpmn.model.ProcessModel;
import com.example.aibpmn.model.ProcessStatus;
import com.example.aibpmn.repository.ProcessModelRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of ProcessModelRepository using ConcurrentHashMap
 */
@Repository
public class InMemoryProcessModelRepository implements ProcessModelRepository {
    
    private final ConcurrentHashMap<String, ProcessModel> storage = new ConcurrentHashMap<>();
    
    @Override
    public ProcessModel save(ProcessModel process) {
        if (process == null || process.getId() == null) {
            throw new IllegalArgumentException("Process and process ID cannot be null");
        }
        storage.put(process.getId(), process);
        return process;
    }
    
    @Override
    public Optional<ProcessModel> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(storage.get(id));
    }
    
    @Override
    public Optional<ProcessModel> findByIdAndVersion(String id, String version) {
        if (id == null || version == null) {
            return Optional.empty();
        }
        return storage.values().stream()
                .filter(p -> id.equals(p.getId()) && version.equals(p.getVersion()))
                .findFirst();
    }
    
    @Override
    public List<ProcessModel> findAll() {
        return List.copyOf(storage.values());
    }
    
    @Override
    public List<ProcessModel> findByStatus(ProcessStatus status) {
        if (status == null) {
            return List.of();
        }
        return storage.values().stream()
                .filter(p -> status.equals(p.getStatus()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ProcessModel> findByNameContaining(String name) {
        if (name == null) {
            return List.of();
        }
        String lowerCaseName = name.toLowerCase();
        return storage.values().stream()
                .filter(p -> p.getName() != null && 
                            p.getName().toLowerCase().contains(lowerCaseName))
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean existsById(String id) {
        return id != null && storage.containsKey(id);
    }
    
    @Override
    public boolean deleteById(String id) {
        if (id == null) {
            return false;
        }
        return storage.remove(id) != null;
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

