package com.example.aibpmn.controller;

import com.example.aibpmn.model.AiState;
import com.example.aibpmn.model.ProcessModel;
import com.example.aibpmn.model.ProcessStatus;
import com.example.aibpmn.repository.ProcessModelRepository;
import com.example.aibpmn.service.AiOrchestratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AiOrchestratorControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ProcessModelRepository processModelRepository;
    
    @Autowired
    private AiOrchestratorService orchestratorService;
    
    @BeforeEach
    void setUp() {
        // Clean up
        processModelRepository.deleteAll();
        orchestratorService.clearAll();
    }
    
    @Test
    void testStartInference() throws Exception {
        // Arrange
        ProcessModel process = createAndSaveProcess("proc-001");
        orchestratorService.setInitialState(process.getId(), AiState.TEXT_RECEIVED);
        
        // Act & Assert
        mockMvc.perform(post("/api/orchestrator/{processId}/start-inference", process.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.processId").value(process.getId()))
            .andExpect(jsonPath("$.currentState").value("PROCESS_INFERRED"))
            .andExpect(jsonPath("$.nextState").value("MODEL_READY"))
            .andExpect(jsonPath("$.complete").value(false))
            .andExpect(jsonPath("$.failed").value(false));
    }
    
    @Test
    void testStartInferenceProcessNotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/orchestrator/{processId}/start-inference", "nonexistent"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").exists());
    }
    
    @Test
    void testStartInferenceInvalidState() throws Exception {
        // Arrange
        ProcessModel process = createAndSaveProcess("proc-002");
        orchestratorService.updateState(process.getId(), AiState.PROCESS_INFERRED);
        
        // Act & Assert
        mockMvc.perform(post("/api/orchestrator/{processId}/start-inference", process.getId()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").exists());
    }
    
    @Test
    void testApproveStep() throws Exception {
        // Arrange
        ProcessModel process = createAndSaveProcess("proc-003");
        orchestratorService.updateState(process.getId(), AiState.MODEL_READY);
        
        // Act & Assert
        mockMvc.perform(post("/api/orchestrator/{processId}/approve", process.getId())
                .param("stepId", "step-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.processId").value(process.getId()))
            .andExpect(jsonPath("$.currentState").value("BPMN_GENERATED"));
    }
    
    @Test
    void testApproveStepWithoutStepId() throws Exception {
        // Arrange
        ProcessModel process = createAndSaveProcess("proc-004");
        orchestratorService.updateState(process.getId(), AiState.MODEL_READY);
        
        // Act & Assert
        mockMvc.perform(post("/api/orchestrator/{processId}/approve", process.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentState").value("BPMN_GENERATED"));
    }
    
    @Test
    void testApproveStepInvalidState() throws Exception {
        // Arrange
        ProcessModel process = createAndSaveProcess("proc-005");
        orchestratorService.updateState(process.getId(), AiState.PROCESS_INFERRED);
        
        // Act & Assert
        mockMvc.perform(post("/api/orchestrator/{processId}/approve", process.getId()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").exists());
    }
    
    @Test
    void testRetry() throws Exception {
        // Arrange
        ProcessModel process = createAndSaveProcess("proc-006");
        orchestratorService.updateState(process.getId(), AiState.FAILED);
        
        // Act & Assert
        mockMvc.perform(post("/api/orchestrator/{processId}/retry", process.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.processId").value(process.getId()))
            .andExpect(jsonPath("$.currentState").value("TEXT_RECEIVED"));
    }
    
    @Test
    void testRetryNotFailed() throws Exception {
        // Arrange
        ProcessModel process = createAndSaveProcess("proc-007");
        orchestratorService.updateState(process.getId(), AiState.MODEL_READY);
        
        // Act & Assert
        mockMvc.perform(post("/api/orchestrator/{processId}/retry", process.getId()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").exists());
    }
    
    @Test
    void testGetState() throws Exception {
        // Arrange
        ProcessModel process = createAndSaveProcess("proc-008");
        orchestratorService.updateState(process.getId(), AiState.BPMN_GENERATED);
        
        // Act & Assert
        mockMvc.perform(get("/api/orchestrator/{processId}/state", process.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.processId").value(process.getId()))
            .andExpect(jsonPath("$.currentState").value("BPMN_GENERATED"))
            .andExpect(jsonPath("$.nextState").value("DRL_GENERATED"))
            .andExpect(jsonPath("$.description").exists());
    }
    
    @Test
    void testGetStateNotTracked() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/orchestrator/{processId}/state", "unknown"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").exists());
    }
    
    @Test
    void testMarkAsFailed() throws Exception {
        // Arrange
        ProcessModel process = createAndSaveProcess("proc-009");
        orchestratorService.updateState(process.getId(), AiState.PROCESS_INFERRED);
        
        // Act & Assert
        mockMvc.perform(post("/api/orchestrator/{processId}/fail", process.getId())
                .param("reason", "AI service timeout"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.processId").value(process.getId()))
            .andExpect(jsonPath("$.currentState").value("FAILED"))
            .andExpect(jsonPath("$.failed").value(true));
    }
    
    @Test
    void testMarkAsFailedWithoutReason() throws Exception {
        // Arrange
        ProcessModel process = createAndSaveProcess("proc-010");
        orchestratorService.updateState(process.getId(), AiState.PROCESS_INFERRED);
        
        // Act & Assert
        mockMvc.perform(post("/api/orchestrator/{processId}/fail", process.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentState").value("FAILED"));
    }
    
    @Test
    void testAdvanceState() throws Exception {
        // Arrange
        ProcessModel process = createAndSaveProcess("proc-011");
        orchestratorService.updateState(process.getId(), AiState.BPMN_GENERATED);
        
        // Act & Assert
        mockMvc.perform(post("/api/orchestrator/{processId}/advance", process.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.processId").value(process.getId()))
            .andExpect(jsonPath("$.currentState").value("DRL_GENERATED"));
    }
    
    @Test
    void testAdvanceStateAtTerminal() throws Exception {
        // Arrange
        ProcessModel process = createAndSaveProcess("proc-012");
        orchestratorService.updateState(process.getId(), AiState.PUBLISHED);
        
        // Act & Assert
        mockMvc.perform(post("/api/orchestrator/{processId}/advance", process.getId()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").exists());
    }
    
    @Test
    void testCanAdvance() throws Exception {
        // Arrange - can advance
        ProcessModel process1 = createAndSaveProcess("proc-013");
        orchestratorService.updateState(process1.getId(), AiState.PROCESS_INFERRED);
        
        // Act & Assert
        mockMvc.perform(get("/api/orchestrator/{processId}/can-advance", process1.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.processId").value(process1.getId()))
            .andExpect(jsonPath("$.canAdvance").value(true));
        
        // Arrange - cannot advance
        ProcessModel process2 = createAndSaveProcess("proc-014");
        orchestratorService.updateState(process2.getId(), AiState.PUBLISHED);
        
        // Act & Assert
        mockMvc.perform(get("/api/orchestrator/{processId}/can-advance", process2.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.processId").value(process2.getId()))
            .andExpect(jsonPath("$.canAdvance").value(false));
    }
    
    @Test
    void testGetTrackedCount() throws Exception {
        // Arrange
        ProcessModel process1 = createAndSaveProcess("proc-015");
        ProcessModel process2 = createAndSaveProcess("proc-016");
        
        orchestratorService.updateState(process1.getId(), AiState.TEXT_RECEIVED);
        orchestratorService.updateState(process2.getId(), AiState.IMAGE_RECEIVED);
        
        // Act & Assert
        mockMvc.perform(get("/api/orchestrator/tracked-count"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.trackedProcessCount").value(2));
    }
    
    @Test
    void testCompleteWorkflow() throws Exception {
        // Arrange
        ProcessModel process = createAndSaveProcess("proc-017");
        orchestratorService.setInitialState(process.getId(), AiState.TEXT_RECEIVED);
        
        // Start inference
        mockMvc.perform(post("/api/orchestrator/{processId}/start-inference", process.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentState").value("PROCESS_INFERRED"));
        
        // Advance to MODEL_READY
        mockMvc.perform(post("/api/orchestrator/{processId}/advance", process.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentState").value("MODEL_READY"));
        
        // Approve model
        mockMvc.perform(post("/api/orchestrator/{processId}/approve", process.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentState").value("BPMN_GENERATED"));
        
        // Advance to DRL_GENERATED
        mockMvc.perform(post("/api/orchestrator/{processId}/advance", process.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentState").value("DRL_GENERATED"));
        
        // Advance to PUBLISHED
        mockMvc.perform(post("/api/orchestrator/{processId}/advance", process.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentState").value("PUBLISHED"))
            .andExpect(jsonPath("$.complete").value(true));
        
        // Verify cannot advance further
        mockMvc.perform(post("/api/orchestrator/{processId}/advance", process.getId()))
            .andExpect(status().isConflict());
    }
    
    @Test
    void testGetClarification() throws Exception {
        // Arrange
        ProcessModel process = createAndSaveProcess("proc-get-clarify");
        List<String> questions = Arrays.asList("Question 1", "Question 2");
        orchestratorService.requestClarification(process.getId(), questions);
        
        // Act & Assert
        mockMvc.perform(get("/api/orchestrator/{processId}/clarification", process.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.processId").value(process.getId()))
            .andExpect(jsonPath("$.questions").isArray())
            .andExpect(jsonPath("$.questions[0]").value("Question 1"))
            .andExpect(jsonPath("$.questions[1]").value("Question 2"));
    }
    
    @Test
    void testGetClarificationNotFound() throws Exception {
        // Arrange
        ProcessModel process = createAndSaveProcess("proc-no-clarify");
        
        // Act & Assert - no clarification requested
        mockMvc.perform(get("/api/orchestrator/{processId}/clarification", process.getId()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").exists());
    }
    
    @Test
    void testSubmitClarification() throws Exception {
        // Arrange
        ProcessModel process = createAndSaveProcess("proc-submit-clarify");
        List<String> questions = Arrays.asList("Question 1");
        orchestratorService.requestClarification(process.getId(), questions);
        
        String requestBody = """
            {
              "answers": {
                "Question 1": "Answer 1"
              },
              "additionalNotes": "Some additional context"
            }
            """;
        
        // Act & Assert
        mockMvc.perform(post("/api/orchestrator/{processId}/clarification", process.getId())
                .contentType("application/json")
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.processId").value(process.getId()))
            .andExpect(jsonPath("$.currentState").value("PROCESS_INFERRED"));
    }
    
    @Test
    void testSubmitClarificationNotInClarificationState() throws Exception {
        // Arrange
        ProcessModel process = createAndSaveProcess("proc-wrong-state-clarify");
        orchestratorService.updateState(process.getId(), AiState.MODEL_READY);
        
        String requestBody = """
            {
              "answers": {}
            }
            """;
        
        // Act & Assert
        mockMvc.perform(post("/api/orchestrator/{processId}/clarification", process.getId())
                .contentType("application/json")
                .content(requestBody))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").exists());
    }
    
    @Test
    void testHasPendingClarification() throws Exception {
        // Arrange - with clarification
        ProcessModel process1 = createAndSaveProcess("proc-has-clarify");
        orchestratorService.requestClarification(process1.getId(), Arrays.asList("Q"));
        
        // Act & Assert
        mockMvc.perform(get("/api/orchestrator/{processId}/has-clarification", process1.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.processId").value(process1.getId()))
            .andExpect(jsonPath("$.hasPendingClarification").value(true));
        
        // Arrange - without clarification
        ProcessModel process2 = createAndSaveProcess("proc-no-clarify-2");
        
        // Act & Assert
        mockMvc.perform(get("/api/orchestrator/{processId}/has-clarification", process2.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.processId").value(process2.getId()))
            .andExpect(jsonPath("$.hasPendingClarification").value(false));
    }
    
    @Test
    void testCancelClarification() throws Exception {
        // Arrange
        ProcessModel process = createAndSaveProcess("proc-cancel-clarify");
        orchestratorService.requestClarification(process.getId(), Arrays.asList("Q"));
        
        // Act & Assert
        mockMvc.perform(delete("/api/orchestrator/{processId}/clarification", process.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.processId").value(process.getId()))
            .andExpect(jsonPath("$.currentState").value("FAILED"));
    }
    
    @Test
    void testGetClarificationCount() throws Exception {
        // Arrange
        ProcessModel process1 = createAndSaveProcess("proc-count-1");
        ProcessModel process2 = createAndSaveProcess("proc-count-2");
        
        orchestratorService.requestClarification(process1.getId(), Arrays.asList("Q1"));
        orchestratorService.requestClarification(process2.getId(), Arrays.asList("Q2"));
        
        // Act & Assert
        mockMvc.perform(get("/api/orchestrator/clarification-count"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pendingClarificationCount").value(2));
    }
    
    private ProcessModel createAndSaveProcess(String processId) {
        ProcessModel process = new ProcessModel();
        process.setId(processId);
        process.setName("Test Process " + processId);
        process.setVersion("1.0.0");
        process.setStatus(ProcessStatus.DRAFT);
        return processModelRepository.save(process);
    }
}

