package com.example.aibpmn.controller;

import com.example.aibpmn.dto.ProcessTextRequest;
import com.example.aibpmn.dto.ProcessTextResponse;
import com.example.aibpmn.service.ProcessTextService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProcessTextController.class)
class ProcessTextControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockitoBean
    private ProcessTextService textService;
    
    @Test
    void testCreateProcessFromTextSuccess() throws Exception {
        // Arrange
        ProcessTextRequest request = new ProcessTextRequest(
            "Order processing workflow: receive order, validate, approve, ship",
            "Order Processing"
        );
        
        ProcessTextResponse response = new ProcessTextResponse(
            "proc-12345678",
            "Order Processing",
            request.getDescription().length()
        );
        
        when(textService.createProcessFromText(any())).thenReturn(response);
        
        // Act & Assert
        mockMvc.perform(post("/api/process/from-text")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.processId").value("proc-12345678"))
            .andExpect(jsonPath("$.name").value("Order Processing"))
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.descriptionLength").exists())
            .andExpect(jsonPath("$.message").exists());
    }
    
    @Test
    void testCreateProcessFromTextWithoutName() throws Exception {
        // Arrange
        ProcessTextRequest request = new ProcessTextRequest(
            "Simple process description"
        );
        
        ProcessTextResponse response = new ProcessTextResponse(
            "proc-87654321",
            "Simple process description",
            request.getDescription().length()
        );
        
        when(textService.createProcessFromText(any())).thenReturn(response);
        
        // Act & Assert
        mockMvc.perform(post("/api/process/from-text")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.processId").value("proc-87654321"))
            .andExpect(jsonPath("$.name").value("Simple process description"));
    }
    
    @Test
    void testCreateProcessFromTextEmptyDescription() throws Exception {
        // Arrange
        ProcessTextRequest request = new ProcessTextRequest("");
        
        when(textService.createProcessFromText(any()))
            .thenThrow(new IllegalArgumentException("Process description cannot be empty"));
        
        // Act & Assert
        mockMvc.perform(post("/api/process/from-text")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("ERROR"))
            .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
            .andExpect(jsonPath("$.message").exists());
    }
    
    @Test
    void testGetTextDescriptionSuccess() throws Exception {
        // Arrange
        String processId = "proc-12345678";
        String description = "Test process description";
        
        when(textService.getTextDescription(processId)).thenReturn(description);
        
        // Act & Assert
        mockMvc.perform(get("/api/process/{processId}/text", processId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.processId").value(processId))
            .andExpect(jsonPath("$.description").value(description));
    }
    
    @Test
    void testGetTextDescriptionNotFound() throws Exception {
        // Arrange
        String processId = "nonexistent-id";
        
        when(textService.getTextDescription(processId)).thenReturn(null);
        
        // Act & Assert
        mockMvc.perform(get("/api/process/{processId}/text", processId))
            .andExpect(status().isNotFound());
    }
}

