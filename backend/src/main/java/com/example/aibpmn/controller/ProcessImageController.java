package com.example.aibpmn.controller;

import com.example.aibpmn.dto.ProcessUploadResponse;
import com.example.aibpmn.exception.FileStorageException;
import com.example.aibpmn.exception.InvalidFileException;
import com.example.aibpmn.service.ProcessImageUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for process image operations.
 * 
 * CORS is configured globally in WebConfig.java - do not add @CrossOrigin here
 * to avoid conflicts with the global configuration.
 */
@RestController
@RequestMapping("/api/process")
public class ProcessImageController {
    
    private static final Logger logger = LoggerFactory.getLogger(ProcessImageController.class);
    
    private final ProcessImageUploadService uploadService;
    
    public ProcessImageController(ProcessImageUploadService uploadService) {
        this.uploadService = uploadService;
    }
    
    /**
     * Upload a BPMN process diagram image
     * 
     * @param file The image file (PNG or JPEG)
     * @return Process upload response with generated processId
     */
    @PostMapping(value = "/from-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProcessUploadResponse> uploadProcessImage(
            @RequestParam("file") MultipartFile file) {
        
        logger.info("Received image upload request: filename={}, size={}, contentType={}",
            file.getOriginalFilename(), file.getSize(), file.getContentType());
        
        try {
            ProcessUploadResponse response = uploadService.uploadProcessImage(file);
            
            logger.info("Successfully processed image upload for processId: {}", response.getProcessId());
            
            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
                
        } catch (InvalidFileException e) {
            logger.warn("Invalid file upload attempt: {}", e.getMessage());
            throw e;
        } catch (FileStorageException e) {
            logger.error("File storage error: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during image upload: {}", e.getMessage(), e);
            throw new FileStorageException("Failed to upload image: " + e.getMessage(), e);
        }
    }
    
    /**
     * Exception handler for invalid file uploads
     */
    @ExceptionHandler(InvalidFileException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidFileException(InvalidFileException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("status", "ERROR");
        error.put("message", ex.getMessage());
        error.put("error", "INVALID_FILE");
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(error);
    }
    
    /**
     * Exception handler for file storage errors
     */
    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<Map<String, Object>> handleFileStorageException(FileStorageException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("status", "ERROR");
        error.put("message", ex.getMessage());
        error.put("error", "FILE_STORAGE_ERROR");
        
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
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

