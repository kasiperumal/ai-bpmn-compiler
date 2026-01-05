package com.example.aibpmn.service;

import com.example.aibpmn.dto.ProcessUploadResponse;
import com.example.aibpmn.exception.FileStorageException;
import com.example.aibpmn.exception.InvalidFileException;
import com.example.aibpmn.model.ProcessModel;
import com.example.aibpmn.model.ProcessStatus;
import com.example.aibpmn.repository.ProcessModelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * Service for handling process image uploads
 */
@Service
public class ProcessImageUploadService {
    
    private static final Logger logger = LoggerFactory.getLogger(ProcessImageUploadService.class);
    
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "image/png",
        "image/jpeg",
        "image/jpg"
    );
    
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
    
    @Value("${app.upload.base-dir:./data/uploads}")
    private String uploadBaseDir;
    
    private final ProcessModelRepository processModelRepository;
    
    public ProcessImageUploadService(ProcessModelRepository processModelRepository) {
        this.processModelRepository = processModelRepository;
    }
    
    /**
     * Upload and process a BPMN diagram image
     * 
     * @param file The uploaded image file
     * @return Upload response with process ID
     */
    public ProcessUploadResponse uploadProcessImage(MultipartFile file) {
        // Validate file
        validateFile(file);
        
        // Generate unique process ID
        String processId = generateProcessId();
        
        logger.info("Processing image upload for processId: {}", processId);
        
        // Store file
        String filePath = storeFile(file, processId);
        
        // Create empty process model
        createEmptyProcessModel(processId);
        
        logger.info("Successfully uploaded image for processId: {} at path: {}", processId, filePath);
        
        return new ProcessUploadResponse(
            processId,
            file.getOriginalFilename(),
            filePath,
            file.getSize()
        );
    }
    
    /**
     * Validate the uploaded file
     */
    private void validateFile(MultipartFile file) {
        // Check if file is empty
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("File is empty or null");
        }
        
        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidFileException(
                String.format("File size exceeds maximum allowed size of %d MB", MAX_FILE_SIZE / (1024 * 1024))
            );
        }
        
        // Check content type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new InvalidFileException(
                "Invalid file type. Only PNG and JPEG images are allowed. Received: " + contentType
            );
        }
        
        // Check file extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new InvalidFileException("Filename is missing");
        }
        
        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!extension.matches("png|jpe?g")) {
            throw new InvalidFileException(
                "Invalid file extension. Only .png, .jpg, and .jpeg are allowed. Received: " + extension
            );
        }
    }
    
    /**
     * Store the uploaded file
     */
    private String storeFile(MultipartFile file, String processId) {
        try {
            // Create directory structure: ./data/uploads/{processId}/
            Path uploadDir = Paths.get(uploadBaseDir, processId);
            Files.createDirectories(uploadDir);
            
            // Determine file extension
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            
            // Store as original.{ext}
            String targetFilename = "original." + extension;
            Path targetPath = uploadDir.resolve(targetFilename);
            
            // Copy file
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            return targetPath.toString();
            
        } catch (IOException e) {
            throw new FileStorageException("Failed to store file: " + e.getMessage(), e);
        }
    }
    
    /**
     * Create an empty process model with DRAFT status
     */
    private void createEmptyProcessModel(String processId) {
        ProcessModel processModel = new ProcessModel();
        processModel.setId(processId);
        processModel.setName("Process from Image - " + processId);
        processModel.setVersion("1.0.0");
        processModel.setStatus(ProcessStatus.DRAFT);
        
        processModelRepository.save(processModel);
        
        logger.info("Created empty ProcessModel with ID: {} and status: DRAFT", processId);
    }
    
    /**
     * Generate a unique process ID
     */
    private String generateProcessId() {
        return "proc-" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}

