package com.example.aibpmn.service;

import com.example.aibpmn.dto.ProcessTextRequest;
import com.example.aibpmn.dto.ProcessTextResponse;
import com.example.aibpmn.model.ProcessModel;
import com.example.aibpmn.model.ProcessStatus;
import com.example.aibpmn.repository.ProcessModelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for creating processes from text descriptions
 */
@Service
public class ProcessTextService {
    
    private static final Logger logger = LoggerFactory.getLogger(ProcessTextService.class);
    
    // In-memory storage for text descriptions (key: processId, value: description)
    private final ConcurrentHashMap<String, String> textStorage = new ConcurrentHashMap<>();
    
    private final ProcessModelRepository processModelRepository;
    
    public ProcessTextService(ProcessModelRepository processModelRepository) {
        this.processModelRepository = processModelRepository;
    }
    
    /**
     * Create a process from text description
     * 
     * @param request The text request containing description
     * @return Response with generated processId
     */
    public ProcessTextResponse createProcessFromText(ProcessTextRequest request) {
        // Validate input
        validateRequest(request);
        
        // Generate unique process ID
        String processId = generateProcessId();
        
        logger.info("Creating process from text, processId: {}", processId);
        
        // Store text description in memory
        textStorage.put(processId, request.getDescription());
        
        // Create process model
        String processName = determineProcessName(request);
        createProcessModel(processId, processName);
        
        logger.info("Successfully created process from text: {} with name: {}", processId, processName);
        
        return new ProcessTextResponse(
            processId,
            processName,
            request.getDescription().length()
        );
    }
    
    /**
     * Retrieve the stored text description for a process
     * 
     * @param processId The process ID
     * @return The text description, or null if not found
     */
    public String getTextDescription(String processId) {
        return textStorage.get(processId);
    }
    
    /**
     * Check if a text description exists for a process
     * 
     * @param processId The process ID
     * @return true if exists, false otherwise
     */
    public boolean hasTextDescription(String processId) {
        return textStorage.containsKey(processId);
    }
    
    /**
     * Delete the text description for a process
     * 
     * @param processId The process ID
     * @return true if deleted, false if not found
     */
    public boolean deleteTextDescription(String processId) {
        return textStorage.remove(processId) != null;
    }
    
    /**
     * Get count of stored text descriptions
     * 
     * @return The number of stored descriptions
     */
    public long getTextDescriptionCount() {
        return textStorage.size();
    }
    
    /**
     * Validate the request
     */
    private void validateRequest(ProcessTextRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            throw new IllegalArgumentException("Process description cannot be empty");
        }
        
        // Optional: Add maximum length validation
        if (request.getDescription().length() > 10000) {
            throw new IllegalArgumentException("Description too long. Maximum 10,000 characters allowed");
        }
    }
    
    /**
     * Create a process model with DRAFT status
     */
    private void createProcessModel(String processId, String processName) {
        ProcessModel processModel = new ProcessModel();
        processModel.setId(processId);
        processModel.setName(processName);
        processModel.setVersion("1.0.0");
        processModel.setStatus(ProcessStatus.DRAFT);
        
        processModelRepository.save(processModel);
        
        logger.info("Created ProcessModel with ID: {} and status: DRAFT", processId);
    }
    
    /**
     * Determine the process name from request or generate default
     */
    private String determineProcessName(ProcessTextRequest request) {
        if (request.getName() != null && !request.getName().isBlank()) {
            return request.getName();
        }
        
        // Generate name from description (first 50 chars)
        String description = request.getDescription();
        if (description.length() <= 50) {
            return description;
        }
        
        return description.substring(0, 47) + "...";
    }
    
    /**
     * Generate a unique process ID
     */
    private String generateProcessId() {
        return "proc-" + UUID.randomUUID().toString().substring(0, 8);
    }
}

