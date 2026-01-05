package com.example.aibpmn.controller;

import com.example.aibpmn.dto.ProcessTextRequest;
import com.example.aibpmn.dto.ProcessTextResponse;
import com.example.aibpmn.service.ProcessTextService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for creating processes from text descriptions
 */
@RestController
@RequestMapping("/api/process")
@CrossOrigin(origins = "*")
public class ProcessTextController {
    
    private static final Logger logger = LoggerFactory.getLogger(ProcessTextController.class);
    
    private final ProcessTextService textService;
    
    public ProcessTextController(ProcessTextService textService) {
        this.textService = textService;
    }
    
    /**
     * Create a process from text description
     * 
     * @param request The text request containing process description
     * @return Process creation response with generated processId
     */
    @PostMapping("/from-text")
    public ResponseEntity<ProcessTextResponse> createProcessFromText(
            @RequestBody ProcessTextRequest request) {
        
        logger.info("Received text-based process creation request, description length: {}",
            request.getDescription() != null ? request.getDescription().length() : 0);
        
        try {
            ProcessTextResponse response = textService.createProcessFromText(request);
            
            logger.info("Successfully created process from text: {}", response.getProcessId());
            
            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
                
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid text request: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during text-based process creation: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create process from text: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get the stored text description for a process
     * 
     * @param processId The process ID
     * @return The text description
     */
    @GetMapping("/{processId}/text")
    public ResponseEntity<Map<String, String>> getTextDescription(@PathVariable String processId) {
        logger.info("Retrieving text description for processId: {}", processId);
        
        String description = textService.getTextDescription(processId);
        
        if (description == null) {
            logger.warn("Text description not found for processId: {}", processId);
            return ResponseEntity.notFound().build();
        }
        
        Map<String, String> response = new HashMap<>();
        response.put("processId", processId);
        response.put("description", description);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Exception handler for illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("status", "ERROR");
        error.put("message", ex.getMessage());
        error.put("error", "INVALID_REQUEST");
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(error);
    }
    
    /**
     * General exception handler
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        logger.error("Unhandled exception: ", ex);
        
        Map<String, Object> error = new HashMap<>();
        error.put("status", "ERROR");
        error.put("message", "An unexpected error occurred");
        error.put("error", "INTERNAL_SERVER_ERROR");
        
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(error);
    }
}

