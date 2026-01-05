package com.example.aibpmn.repository;

import com.example.aibpmn.model.ProcessModel;
import com.example.aibpmn.model.ProcessStatus;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ProcessModel persistence
 */
public interface ProcessModelRepository {
    
    /**
     * Save a process model
     * @param process The process to save
     * @return The saved process
     */
    ProcessModel save(ProcessModel process);
    
    /**
     * Find a process by ID
     * @param id The process ID
     * @return Optional containing the process if found
     */
    Optional<ProcessModel> findById(String id);
    
    /**
     * Find a process by ID and version
     * @param id The process ID
     * @param version The process version
     * @return Optional containing the process if found
     */
    Optional<ProcessModel> findByIdAndVersion(String id, String version);
    
    /**
     * Find all processes
     * @return List of all processes
     */
    List<ProcessModel> findAll();
    
    /**
     * Find processes by status
     * @param status The process status
     * @return List of processes with the given status
     */
    List<ProcessModel> findByStatus(ProcessStatus status);
    
    /**
     * Find processes by name (case-insensitive partial match)
     * @param name The process name to search for
     * @return List of matching processes
     */
    List<ProcessModel> findByNameContaining(String name);
    
    /**
     * Check if a process exists
     * @param id The process ID
     * @return true if exists, false otherwise
     */
    boolean existsById(String id);
    
    /**
     * Delete a process by ID
     * @param id The process ID
     * @return true if deleted, false if not found
     */
    boolean deleteById(String id);
    
    /**
     * Delete all processes
     */
    void deleteAll();
    
    /**
     * Count all processes
     * @return The total number of processes
     */
    long count();
}

