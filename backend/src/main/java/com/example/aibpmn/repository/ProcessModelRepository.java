package com.example.aibpmn.repository;

import com.example.aibpmn.model.ProcessModel;
import com.example.aibpmn.model.ProcessStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ProcessModel persistence using JPA
 */
@Repository
public interface ProcessModelRepository extends JpaRepository<ProcessModel, String> {
    
    /**
     * Find a process by ID and version
     * @param id The process ID
     * @param version The process version
     * @return Optional containing the process if found
     */
    Optional<ProcessModel> findByIdAndVersion(String id, Integer version);
    
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
    List<ProcessModel> findByNameContainingIgnoreCase(String name);
}

