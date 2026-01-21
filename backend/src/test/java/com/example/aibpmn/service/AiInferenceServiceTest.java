package com.example.aibpmn.service;

import com.example.aibpmn.model.ProcessModel;
import com.example.aibpmn.model.ProcessStatus;
import com.example.aibpmn.repository.ProcessModelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AiInferenceServiceTest {
    
    private GeminiClient geminiClient;
    private ProcessModelRepository processModelRepository;
    private AiInferenceService aiInferenceService;
    
    @TempDir
    Path tempDir;
    
    private String uploadBaseDir;
    
    @BeforeEach
    void setUp() {
        geminiClient = mock(GeminiClient.class);
        processModelRepository = mock(ProcessModelRepository.class);
        
        uploadBaseDir = tempDir.toString();
        aiInferenceService = new AiInferenceService(
            geminiClient,
            processModelRepository,
            uploadBaseDir
        );
    }
    
    @Test
    void testInferProcessDescriptionFromImage_Success() throws Exception {
        // Arrange
        String processId = "proc-123";
        ProcessModel process = createProcess(processId);
        
        // Create image file
        Path processDir = tempDir.resolve(processId);
        Files.createDirectories(processDir);
        Path imagePath = processDir.resolve("original.png");
        Files.write(imagePath, createTestImageData());
        
        // Mock repository
        when(processModelRepository.findById(processId)).thenReturn(Optional.of(process));
        
        // Mock Gemini response
        String expectedDescription = """
            ## Overview
            This is an order approval workflow.
            
            ## Main Flow
            1. The process begins when an order is received
            2. The order is validated
            3. A decision is made based on order amount
            """;
        
        when(geminiClient.generateFromImage(any(Path.class), anyString()))
            .thenReturn(expectedDescription);
        
        // Act
        String description = aiInferenceService.inferProcessDescriptionFromImage(processId);
        
        // Assert
        assertNotNull(description);
        assertTrue(description.contains("## Overview"));
        assertTrue(description.contains("order approval"));
        
        verify(processModelRepository, times(1)).findById(processId);
        verify(geminiClient, times(1)).generateFromImage(any(Path.class), anyString());
    }
    
    @Test
    void testInferProcessDescriptionFromImage_ProcessNotFound() {
        // Arrange
        String processId = "nonexistent";
        when(processModelRepository.findById(processId)).thenReturn(Optional.empty());
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> aiInferenceService.inferProcessDescriptionFromImage(processId)
        );
        
        assertTrue(exception.getMessage().contains("Process not found"));
        verify(geminiClient, never()).generateFromImage(any(Path.class), anyString());
    }
    
    @Test
    void testInferProcessDescriptionFromImage_ImageNotFound() {
        // Arrange
        String processId = "proc-456";
        ProcessModel process = createProcess(processId);
        
        when(processModelRepository.findById(processId)).thenReturn(Optional.of(process));
        
        // No image file created
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> aiInferenceService.inferProcessDescriptionFromImage(processId)
        );
        
        assertTrue(exception.getMessage().contains("Image file not found"));
        verify(geminiClient, never()).generateFromImage(any(Path.class), anyString());
    }
    
    @Test
    void testInferProcessDescriptionFromImage_GeminiError() throws Exception {
        // Arrange
        String processId = "proc-789";
        ProcessModel process = createProcess(processId);
        
        // Create image file
        Path processDir = tempDir.resolve(processId);
        Files.createDirectories(processDir);
        Path imagePath = processDir.resolve("original.png");
        Files.write(imagePath, createTestImageData());
        
        when(processModelRepository.findById(processId)).thenReturn(Optional.of(process));
        
        // Mock Gemini to throw exception
        when(geminiClient.generateFromImage(any(Path.class), anyString()))
            .thenThrow(new RuntimeException("API rate limit exceeded"));
        
        // Act & Assert
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> aiInferenceService.inferProcessDescriptionFromImage(processId)
        );
        
        assertTrue(exception.getMessage().contains("Failed to infer process description"));
        verify(geminiClient, times(1)).generateFromImage(any(Path.class), anyString());
    }
    
    @Test
    void testInferProcessDescriptionFromImage_FindsDifferentImageFormats() throws Exception {
        // Test that the service finds different image format extensions
        String[] formats = {"png", "jpg", "jpeg", "gif", "webp"};
        
        for (String format : formats) {
            // Arrange
            String processId = "proc-" + format;
            ProcessModel process = createProcess(processId);
            
            Path processDir = tempDir.resolve(processId);
            Files.createDirectories(processDir);
            Path imagePath = processDir.resolve("original." + format);
            Files.write(imagePath, createTestImageData());
            
            when(processModelRepository.findById(processId)).thenReturn(Optional.of(process));
            
            String mockDescription = "Process description for " + format;
            when(geminiClient.generateFromImage(any(Path.class), anyString()))
                .thenReturn(mockDescription);
            
            // Act
            String description = aiInferenceService.inferProcessDescriptionFromImage(processId);
            
            // Assert
            assertNotNull(description);
            assertEquals(mockDescription, description);
        }
    }
    
    @Test
    void testInferProcessDescriptionFromImage_PromptContainsRequiredElements() throws Exception {
        // Arrange
        String processId = "proc-abc";
        ProcessModel process = createProcess(processId);
        
        Path processDir = tempDir.resolve(processId);
        Files.createDirectories(processDir);
        Path imagePath = processDir.resolve("original.png");
        Files.write(imagePath, createTestImageData());
        
        when(processModelRepository.findById(processId)).thenReturn(Optional.of(process));
        when(geminiClient.generateFromImage(any(Path.class), anyString()))
            .thenReturn("Mock description");
        
        // Act
        aiInferenceService.inferProcessDescriptionFromImage(processId);
        
        // Assert - verify prompt was passed to Gemini
        verify(geminiClient, times(1)).generateFromImage(
            any(Path.class),
            argThat(prompt -> {
                // Check that prompt contains key requirements
                return prompt.contains("BUSINESS LANGUAGE") &&
                       prompt.contains("DESCRIBE THE FOLLOWING") &&
                       prompt.contains("CALL OUT AMBIGUITIES") &&
                       prompt.contains("## Overview") &&
                       prompt.contains("## Main Flow") &&
                       prompt.contains("## Decision Points") &&
                       prompt.contains("## Ambiguities");
            })
        );
    }
    
    @Test
    void testInferProcessDescriptionFromImage_ResponseStructure() throws Exception {
        // Arrange
        String processId = "proc-structured";
        ProcessModel process = createProcess(processId);
        
        Path processDir = tempDir.resolve(processId);
        Files.createDirectories(processDir);
        Path imagePath = processDir.resolve("original.png");
        Files.write(imagePath, createTestImageData());
        
        when(processModelRepository.findById(processId)).thenReturn(Optional.of(process));
        
        // Mock a well-structured response
        String structuredResponse = """
            ## Overview
            This process handles customer order fulfillment from receipt to delivery.
            
            ## Main Flow
            1. The process begins when a customer order is received
            2. Order details are validated for completeness
            3. Inventory is checked for product availability
            4. If items are in stock, the order proceeds to fulfillment
            5. Items are picked from the warehouse
            6. Order is packed and shipping label is created
            7. Package is handed to shipping carrier
            8. The process completes when delivery confirmation is received
            
            ## Decision Points
            - After validation: If order is incomplete, it is rejected
            - After inventory check: If items are out of stock, customer is notified
            - During fulfillment: If quality issues found, item returns to inventory
            
            ## Alternative Paths
            - Rejected orders: Customer receives rejection notice with reason
            - Out of stock: Option to backorder or cancel
            - Failed delivery: Package returns to warehouse for reprocessing
            
            ## Parallel Activities
            None identified - process appears to be sequential
            
            ## Process Completion
            - Success: Delivery confirmed, customer receives notification
            - Cancellation: Customer notified, refund processed
            - Rejection: Order not processed, customer notified
            
            ## Ambiguities and Uncertainties
            - It's unclear who approves high-value orders
            - The diagram doesn't show what happens if payment fails
            - Timing for inventory checks is not specified
            
            ## Additional Observations
            - Process involves warehouse staff, shipping coordinator, and customer service
            - Key data: order number, customer info, inventory levels, tracking number
            - No exception handling visible for carrier issues
            """;
        
        when(geminiClient.generateFromImage(any(Path.class), anyString()))
            .thenReturn(structuredResponse);
        
        // Act
        String description = aiInferenceService.inferProcessDescriptionFromImage(processId);
        
        // Assert
        assertNotNull(description);
        assertTrue(description.contains("## Overview"));
        assertTrue(description.contains("## Main Flow"));
        assertTrue(description.contains("## Decision Points"));
        assertTrue(description.contains("## Alternative Paths"));
        assertTrue(description.contains("## Parallel Activities"));
        assertTrue(description.contains("## Process Completion"));
        assertTrue(description.contains("## Ambiguities and Uncertainties"));
        assertTrue(description.contains("## Additional Observations"));
        
        // Verify business language (not BPMN terms)
        assertFalse(description.contains("Start Event"));
        assertFalse(description.contains("End Event"));
        assertFalse(description.contains("Gateway"));
        assertFalse(description.contains("Sequence Flow"));
    }
    
    @Test
    void testHasProcessImage_ImageExists() throws Exception {
        // Arrange
        String processId = "proc-with-image";
        Path processDir = tempDir.resolve(processId);
        Files.createDirectories(processDir);
        Path imagePath = processDir.resolve("original.png");
        Files.write(imagePath, createTestImageData());
        
        // Act
        boolean hasImage = aiInferenceService.hasProcessImage(processId);
        
        // Assert
        assertTrue(hasImage);
    }
    
    @Test
    void testHasProcessImage_ImageDoesNotExist() {
        // Arrange
        String processId = "proc-without-image";
        
        // Act
        boolean hasImage = aiInferenceService.hasProcessImage(processId);
        
        // Assert
        assertFalse(hasImage);
    }
    
    @Test
    void testHasProcessImage_DirectoryDoesNotExist() {
        // Arrange
        String processId = "nonexistent-process";
        
        // Act
        boolean hasImage = aiInferenceService.hasProcessImage(processId);
        
        // Assert
        assertFalse(hasImage);
    }
    
    // Helper methods
    
    private ProcessModel createProcess(String processId) {
        ProcessModel process = new ProcessModel();
        process.setId(processId);
        process.setName("Test Process " + processId);
        process.setVersion(1);
        process.setStatus(ProcessStatus.DRAFT);
        return process;
    }
    
    private byte[] createTestImageData() {
        // Create a minimal PNG header
        return new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D,
            0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x10,
            0x00, 0x00, 0x00, 0x10,
            0x08, 0x02, 0x00, 0x00, 0x00
        };
    }
}

