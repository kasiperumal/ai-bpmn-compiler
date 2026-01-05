package com.example.aibpmn.controller;

import com.example.aibpmn.dto.ProcessUploadResponse;
import com.example.aibpmn.exception.FileStorageException;
import com.example.aibpmn.exception.InvalidFileException;
import com.example.aibpmn.service.ProcessImageUploadService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProcessImageController.class)
class ProcessImageControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockitoBean
    private ProcessImageUploadService uploadService;
    
    @Test
    void testUploadImageSuccess() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "diagram.png",
            "image/png",
            "test content".getBytes()
        );
        
        ProcessUploadResponse response = new ProcessUploadResponse(
            "proc-12345678",
            "diagram.png",
            "./data/uploads/proc-12345678/original.png",
            file.getSize()
        );
        
        when(uploadService.uploadProcessImage(any())).thenReturn(response);
        
        // Act & Assert
        mockMvc.perform(multipart("/api/process/from-image")
                .file(file))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.processId").value("proc-12345678"))
            .andExpect(jsonPath("$.fileName").value("diagram.png"))
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.filePath").exists())
            .andExpect(jsonPath("$.fileSize").exists());
    }
    
    @Test
    void testUploadImageInvalidFile() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "document.pdf",
            "application/pdf",
            "fake pdf".getBytes()
        );
        
        when(uploadService.uploadProcessImage(any()))
            .thenThrow(new InvalidFileException("Invalid file type"));
        
        // Act & Assert
        mockMvc.perform(multipart("/api/process/from-image")
                .file(file))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("ERROR"))
            .andExpect(jsonPath("$.error").value("INVALID_FILE"))
            .andExpect(jsonPath("$.message").exists());
    }
    
    @Test
    void testUploadImageStorageError() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "diagram.png",
            "image/png",
            "test content".getBytes()
        );
        
        when(uploadService.uploadProcessImage(any()))
            .thenThrow(new FileStorageException("Failed to store file"));
        
        // Act & Assert
        mockMvc.perform(multipart("/api/process/from-image")
                .file(file))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.status").value("ERROR"))
            .andExpect(jsonPath("$.error").value("FILE_STORAGE_ERROR"))
            .andExpect(jsonPath("$.message").exists());
    }
}

