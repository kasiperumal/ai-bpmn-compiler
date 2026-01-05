package com.example.aibpmn.service;

import com.example.aibpmn.dto.ProcessTextRequest;
import com.example.aibpmn.dto.ProcessTextResponse;
import com.example.aibpmn.model.ProcessModel;
import com.example.aibpmn.model.ProcessStatus;
import com.example.aibpmn.repository.ProcessModelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProcessTextServiceTest {
    
    private ProcessTextService service;
    private ProcessModelRepository processModelRepository;
    
    @BeforeEach
    void setUp() {
        processModelRepository = mock(ProcessModelRepository.class);
        service = new ProcessTextService(processModelRepository);
    }
    
    @Test
    void testCreateProcessFromTextWithDescription() {
        // Arrange
        String description = "Order approval process: receive order, validate, approve, ship";
        ProcessTextRequest request = new ProcessTextRequest(description);
        
        when(processModelRepository.save(any(ProcessModel.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        ProcessTextResponse response = service.createProcessFromText(request);
        
        // Assert
        assertNotNull(response);
        assertNotNull(response.getProcessId());
        assertTrue(response.getProcessId().startsWith("proc-"));
        assertEquals(description.length(), response.getDescriptionLength());
        assertEquals("SUCCESS", response.getStatus());
        
        // Verify text was stored
        assertTrue(service.hasTextDescription(response.getProcessId()));
        assertEquals(description, service.getTextDescription(response.getProcessId()));
        
        // Verify process model was created
        ArgumentCaptor<ProcessModel> captor = ArgumentCaptor.forClass(ProcessModel.class);
        verify(processModelRepository).save(captor.capture());
        
        ProcessModel savedModel = captor.getValue();
        assertEquals(response.getProcessId(), savedModel.getId());
        assertEquals(ProcessStatus.DRAFT, savedModel.getStatus());
        assertEquals("1.0.0", savedModel.getVersion());
    }
    
    @Test
    void testCreateProcessWithCustomName() {
        // Arrange
        String description = "Customer onboarding workflow";
        String customName = "Customer Onboarding Process";
        ProcessTextRequest request = new ProcessTextRequest(description, customName);
        
        when(processModelRepository.save(any(ProcessModel.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        ProcessTextResponse response = service.createProcessFromText(request);
        
        // Assert
        assertEquals(customName, response.getName());
        
        ArgumentCaptor<ProcessModel> captor = ArgumentCaptor.forClass(ProcessModel.class);
        verify(processModelRepository).save(captor.capture());
        assertEquals(customName, captor.getValue().getName());
    }
    
    @Test
    void testCreateProcessWithLongDescription() {
        // Arrange
        String longDescription = "This is a very long process description that exceeds fifty characters";
        ProcessTextRequest request = new ProcessTextRequest(longDescription);
        
        when(processModelRepository.save(any(ProcessModel.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        ProcessTextResponse response = service.createProcessFromText(request);
        
        // Assert
        assertNotNull(response.getName());
        // Name should be truncated to 50 chars with "..."
        assertTrue(response.getName().endsWith("..."));
        assertEquals(50, response.getName().length());
    }
    
    @Test
    void testCreateProcessWithShortDescription() {
        // Arrange
        String shortDescription = "Simple process";
        ProcessTextRequest request = new ProcessTextRequest(shortDescription);
        
        when(processModelRepository.save(any(ProcessModel.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        ProcessTextResponse response = service.createProcessFromText(request);
        
        // Assert
        assertEquals(shortDescription, response.getName());
    }
    
    @Test
    void testCreateProcessWithNullRequest() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> service.createProcessFromText(null));
    }
    
    @Test
    void testCreateProcessWithEmptyDescription() {
        // Arrange
        ProcessTextRequest request = new ProcessTextRequest("");
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.createProcessFromText(request)
        );
        
        assertTrue(exception.getMessage().contains("cannot be empty"));
    }
    
    @Test
    void testCreateProcessWithBlankDescription() {
        // Arrange
        ProcessTextRequest request = new ProcessTextRequest("   ");
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> service.createProcessFromText(request));
    }
    
    @Test
    void testCreateProcessWithTooLongDescription() {
        // Arrange
        String tooLong = "x".repeat(10001); // 10,001 characters
        ProcessTextRequest request = new ProcessTextRequest(tooLong);
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.createProcessFromText(request)
        );
        
        assertTrue(exception.getMessage().toLowerCase().contains("too long"));
    }
    
    @Test
    void testGetTextDescription() {
        // Arrange
        String description = "Test process description";
        ProcessTextRequest request = new ProcessTextRequest(description);
        
        when(processModelRepository.save(any(ProcessModel.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        ProcessTextResponse response = service.createProcessFromText(request);
        
        // Act
        String retrieved = service.getTextDescription(response.getProcessId());
        
        // Assert
        assertEquals(description, retrieved);
    }
    
    @Test
    void testGetNonExistentTextDescription() {
        // Act
        String retrieved = service.getTextDescription("nonexistent-id");
        
        // Assert
        assertNull(retrieved);
    }
    
    @Test
    void testHasTextDescription() {
        // Arrange
        ProcessTextRequest request = new ProcessTextRequest("Test description");
        
        when(processModelRepository.save(any(ProcessModel.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        ProcessTextResponse response = service.createProcessFromText(request);
        
        // Act & Assert
        assertTrue(service.hasTextDescription(response.getProcessId()));
        assertFalse(service.hasTextDescription("nonexistent-id"));
    }
    
    @Test
    void testDeleteTextDescription() {
        // Arrange
        ProcessTextRequest request = new ProcessTextRequest("Test description");
        
        when(processModelRepository.save(any(ProcessModel.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        ProcessTextResponse response = service.createProcessFromText(request);
        String processId = response.getProcessId();
        
        // Act
        boolean deleted = service.deleteTextDescription(processId);
        
        // Assert
        assertTrue(deleted);
        assertFalse(service.hasTextDescription(processId));
        assertNull(service.getTextDescription(processId));
        
        // Try deleting again
        boolean deletedAgain = service.deleteTextDescription(processId);
        assertFalse(deletedAgain);
    }
    
    @Test
    void testGetTextDescriptionCount() {
        // Arrange
        when(processModelRepository.save(any(ProcessModel.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        assertEquals(0, service.getTextDescriptionCount());
        
        // Add descriptions
        service.createProcessFromText(new ProcessTextRequest("Description 1"));
        assertEquals(1, service.getTextDescriptionCount());
        
        service.createProcessFromText(new ProcessTextRequest("Description 2"));
        assertEquals(2, service.getTextDescriptionCount());
        
        service.createProcessFromText(new ProcessTextRequest("Description 3"));
        assertEquals(3, service.getTextDescriptionCount());
    }
    
    @Test
    void testGenerateUniqueProcessIds() {
        // Arrange
        when(processModelRepository.save(any(ProcessModel.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        ProcessTextResponse response1 = service.createProcessFromText(
            new ProcessTextRequest("Process 1"));
        ProcessTextResponse response2 = service.createProcessFromText(
            new ProcessTextRequest("Process 2"));
        
        // Assert
        assertNotEquals(response1.getProcessId(), response2.getProcessId());
    }
}

