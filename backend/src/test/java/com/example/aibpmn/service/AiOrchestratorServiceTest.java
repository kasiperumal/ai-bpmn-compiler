package com.example.aibpmn.service;

import com.example.aibpmn.dto.AiStateInfo;
import com.example.aibpmn.dto.ClarificationRequest;
import com.example.aibpmn.dto.ClarificationResponse;
import com.example.aibpmn.model.AiState;
import com.example.aibpmn.model.ProcessModel;
import com.example.aibpmn.model.ProcessStatus;
import com.example.aibpmn.repository.ProcessModelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiOrchestratorServiceTest {
    
    private AiOrchestratorService orchestrator;
    private ProcessModelRepository processModelRepository;
    
    @BeforeEach
    void setUp() {
        processModelRepository = mock(ProcessModelRepository.class);
        orchestrator = new AiOrchestratorService(processModelRepository);
    }
    
    @Test
    void testStartInferenceFromTextReceived() {
        // Arrange
        String processId = "proc-001";
        ProcessModel process = createProcess(processId);
        
        when(processModelRepository.findById(processId)).thenReturn(Optional.of(process));
        orchestrator.setInitialState(processId, AiState.TEXT_RECEIVED);
        
        // Act
        AiStateInfo result = orchestrator.startInference(processId);
        
        // Assert
        assertNotNull(result);
        assertEquals(processId, result.getProcessId());
        assertEquals(AiState.PROCESS_INFERRED, result.getCurrentState());
        assertEquals(AiState.MODEL_READY, result.getNextState());
        assertFalse(result.isComplete());
        assertFalse(result.isFailed());
    }
    
    @Test
    void testStartInferenceFromImageReceived() {
        // Arrange
        String processId = "proc-002";
        ProcessModel process = createProcess(processId);
        
        when(processModelRepository.findById(processId)).thenReturn(Optional.of(process));
        orchestrator.setInitialState(processId, AiState.IMAGE_RECEIVED);
        
        // Act
        AiStateInfo result = orchestrator.startInference(processId);
        
        // Assert
        assertEquals(AiState.PROCESS_INFERRED, result.getCurrentState());
    }
    
    @Test
    void testStartInferenceProcessNotFound() {
        // Arrange
        String processId = "nonexistent";
        orchestrator.setInitialState(processId, AiState.TEXT_RECEIVED);
        
        when(processModelRepository.findById(processId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> orchestrator.startInference(processId));
    }
    
    @Test
    void testStartInferenceNotInInitialState() {
        // Arrange
        String processId = "proc-003";
        ProcessModel process = createProcess(processId);
        
        when(processModelRepository.findById(processId)).thenReturn(Optional.of(process));
        orchestrator.updateState(processId, AiState.PROCESS_INFERRED);
        
        // Act & Assert
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> orchestrator.startInference(processId)
        );
        
        assertTrue(exception.getMessage().contains("already in state"));
    }
    
    @Test
    void testApproveStepFromModelReady() {
        // Arrange
        String processId = "proc-004";
        ProcessModel process = createProcess(processId);
        
        when(processModelRepository.findById(processId)).thenReturn(Optional.of(process));
        orchestrator.updateState(processId, AiState.MODEL_READY);
        
        // Act
        AiStateInfo result = orchestrator.approveStep(processId, "step-001");
        
        // Assert
        assertEquals(AiState.BPMN_GENERATED, result.getCurrentState());
        assertEquals(AiState.DRL_GENERATED, result.getNextState());
    }
    
    @Test
    void testApproveStepFromClarificationRequired() {
        // Arrange
        String processId = "proc-005";
        ProcessModel process = createProcess(processId);
        
        when(processModelRepository.findById(processId)).thenReturn(Optional.of(process));
        orchestrator.updateState(processId, AiState.CLARIFICATION_REQUIRED);
        
        // Act & Assert
        // CLARIFICATION_REQUIRED cannot advance automatically (getNextState() returns null)
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> orchestrator.approveStep(processId, "step-002")
        );
        
        assertTrue(exception.getMessage().contains("Cannot advance from state"));
    }
    
    @Test
    void testApproveStepInvalidState() {
        // Arrange
        String processId = "proc-006";
        ProcessModel process = createProcess(processId);
        
        when(processModelRepository.findById(processId)).thenReturn(Optional.of(process));
        orchestrator.updateState(processId, AiState.PROCESS_INFERRED);
        
        // Act & Assert
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> orchestrator.approveStep(processId, "step-003")
        );
        
        assertTrue(exception.getMessage().contains("doesn't require approval"));
    }
    
    @Test
    void testRetryFromFailed() {
        // Arrange
        String processId = "proc-007";
        ProcessModel process = createProcess(processId);
        
        when(processModelRepository.findById(processId)).thenReturn(Optional.of(process));
        orchestrator.updateState(processId, AiState.FAILED);
        
        // Act
        AiStateInfo result = orchestrator.retry(processId);
        
        // Assert
        assertEquals(AiState.TEXT_RECEIVED, result.getCurrentState());
        assertTrue(result.getCurrentState().isInitialState());
    }
    
    @Test
    void testRetryNotFailed() {
        // Arrange
        String processId = "proc-008";
        ProcessModel process = createProcess(processId);
        
        when(processModelRepository.findById(processId)).thenReturn(Optional.of(process));
        orchestrator.updateState(processId, AiState.MODEL_READY);
        
        // Act & Assert
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> orchestrator.retry(processId)
        );
        
        assertTrue(exception.getMessage().contains("not FAILED"));
    }
    
    @Test
    void testGetStateInfo() {
        // Arrange
        String processId = "proc-009";
        orchestrator.updateState(processId, AiState.BPMN_GENERATED);
        
        // Act
        AiStateInfo result = orchestrator.getStateInfo(processId);
        
        // Assert
        assertNotNull(result);
        assertEquals(processId, result.getProcessId());
        assertEquals(AiState.BPMN_GENERATED, result.getCurrentState());
        assertEquals(AiState.DRL_GENERATED, result.getNextState());
        assertNotNull(result.getDescription());
    }
    
    @Test
    void testGetStateInfoNotTracked() {
        // Act
        AiStateInfo result = orchestrator.getStateInfo("unknown");
        
        // Assert
        assertNull(result);
    }
    
    @Test
    void testGetCurrentState() {
        // Arrange
        String processId = "proc-010";
        orchestrator.updateState(processId, AiState.PUBLISHED);
        
        // Act
        AiState state = orchestrator.getCurrentState(processId);
        
        // Assert
        assertEquals(AiState.PUBLISHED, state);
    }
    
    @Test
    void testGetCurrentStateNotTracked() {
        // Act - should return default TEXT_RECEIVED
        AiState state = orchestrator.getCurrentState("unknown");
        
        // Assert
        assertEquals(AiState.TEXT_RECEIVED, state);
    }
    
    @Test
    void testSetInitialStateValid() {
        // Act
        orchestrator.setInitialState("proc-011", AiState.IMAGE_RECEIVED);
        
        // Assert
        assertEquals(AiState.IMAGE_RECEIVED, orchestrator.getCurrentState("proc-011"));
    }
    
    @Test
    void testSetInitialStateInvalid() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> orchestrator.setInitialState("proc-012", AiState.PROCESS_INFERRED));
    }
    
    @Test
    void testUpdateState() {
        // Arrange
        String processId = "proc-013";
        
        // Act
        orchestrator.updateState(processId, AiState.MODEL_READY);
        
        // Assert
        assertEquals(AiState.MODEL_READY, orchestrator.getCurrentState(processId));
    }
    
    @Test
    void testMarkAsFailed() {
        // Arrange
        String processId = "proc-014";
        ProcessModel process = createProcess(processId);
        
        when(processModelRepository.findById(processId)).thenReturn(Optional.of(process));
        orchestrator.updateState(processId, AiState.PROCESS_INFERRED);
        
        // Act
        AiStateInfo result = orchestrator.markAsFailed(processId, "AI service timeout");
        
        // Assert
        assertEquals(AiState.FAILED, result.getCurrentState());
        assertTrue(result.isFailed());
        assertEquals(AiState.FAILED, orchestrator.getCurrentState(processId));
    }
    
    @Test
    void testAdvanceState() {
        // Arrange
        String processId = "proc-015";
        orchestrator.updateState(processId, AiState.BPMN_GENERATED);
        
        // Act
        AiState newState = orchestrator.advanceState(processId);
        
        // Assert
        assertEquals(AiState.DRL_GENERATED, newState);
        assertEquals(AiState.DRL_GENERATED, orchestrator.getCurrentState(processId));
    }
    
    @Test
    void testAdvanceStateAtTerminal() {
        // Arrange
        String processId = "proc-016";
        orchestrator.updateState(processId, AiState.PUBLISHED);
        
        // Act & Assert
        assertThrows(IllegalStateException.class, 
            () -> orchestrator.advanceState(processId));
    }
    
    @Test
    void testCanAdvance() {
        // Can advance
        orchestrator.updateState("proc-017", AiState.PROCESS_INFERRED);
        assertTrue(orchestrator.canAdvance("proc-017"));
        
        // Cannot advance (terminal state)
        orchestrator.updateState("proc-018", AiState.PUBLISHED);
        assertFalse(orchestrator.canAdvance("proc-018"));
        
        // Cannot advance (requires user action)
        orchestrator.updateState("proc-019", AiState.CLARIFICATION_REQUIRED);
        assertFalse(orchestrator.canAdvance("proc-019"));
    }
    
    @Test
    void testResetState() {
        // Arrange
        String processId = "proc-020";
        orchestrator.updateState(processId, AiState.MODEL_READY);
        assertTrue(orchestrator.isTracked(processId));
        
        // Act
        orchestrator.resetState(processId);
        
        // Assert
        assertFalse(orchestrator.isTracked(processId));
    }
    
    @Test
    void testGetTrackedProcessCount() {
        // Arrange
        assertEquals(0, orchestrator.getTrackedProcessCount());
        
        orchestrator.updateState("proc-021", AiState.TEXT_RECEIVED);
        assertEquals(1, orchestrator.getTrackedProcessCount());
        
        orchestrator.updateState("proc-022", AiState.IMAGE_RECEIVED);
        assertEquals(2, orchestrator.getTrackedProcessCount());
        
        orchestrator.resetState("proc-021");
        assertEquals(1, orchestrator.getTrackedProcessCount());
    }
    
    @Test
    void testIsTracked() {
        // Arrange
        String processId = "proc-023";
        
        assertFalse(orchestrator.isTracked(processId));
        
        orchestrator.updateState(processId, AiState.TEXT_RECEIVED);
        assertTrue(orchestrator.isTracked(processId));
    }
    
    @Test
    void testCompleteWorkflow() {
        // Arrange
        String processId = "proc-024";
        ProcessModel process = createProcess(processId);
        
        when(processModelRepository.findById(processId)).thenReturn(Optional.of(process));
        
        // Start from TEXT_RECEIVED
        orchestrator.setInitialState(processId, AiState.TEXT_RECEIVED);
        assertEquals(AiState.TEXT_RECEIVED, orchestrator.getCurrentState(processId));
        
        // Start inference
        AiStateInfo info = orchestrator.startInference(processId);
        assertEquals(AiState.PROCESS_INFERRED, info.getCurrentState());
        
        // Advance to MODEL_READY
        orchestrator.advanceState(processId);
        assertEquals(AiState.MODEL_READY, orchestrator.getCurrentState(processId));
        
        // Approve model
        info = orchestrator.approveStep(processId, "approve-model");
        assertEquals(AiState.BPMN_GENERATED, info.getCurrentState());
        
        // Advance to DRL_GENERATED
        orchestrator.advanceState(processId);
        assertEquals(AiState.DRL_GENERATED, orchestrator.getCurrentState(processId));
        
        // Advance to PUBLISHED
        orchestrator.advanceState(processId);
        AiState finalState = orchestrator.getCurrentState(processId);
        assertEquals(AiState.PUBLISHED, finalState);
        assertTrue(finalState.isCompleted());
        assertTrue(finalState.isTerminalState());
    }
    
    @Test
    void testRequestClarification() {
        // Arrange
        String processId = "proc-clarify";
        ProcessModel process = createProcess(processId);
        when(processModelRepository.findById(processId)).thenReturn(Optional.of(process));
        
        List<String> questions = Arrays.asList(
            "Who approves high-value orders?",
            "What is the threshold for high-value?"
        );
        
        // Act
        AiStateInfo result = orchestrator.requestClarification(processId, questions);
        
        // Assert
        assertEquals(AiState.CLARIFICATION_REQUIRED, result.getCurrentState());
        assertTrue(orchestrator.hasPendingClarification(processId));
        
        ClarificationRequest request = orchestrator.getClarificationRequest(processId);
        assertNotNull(request);
        assertEquals(processId, request.getProcessId());
        assertEquals(2, request.getQuestions().size());
        assertEquals(questions.get(0), request.getQuestions().get(0));
    }
    
    @Test
    void testRequestClarificationWithContext() {
        // Arrange
        String processId = "proc-context";
        ProcessModel process = createProcess(processId);
        when(processModelRepository.findById(processId)).thenReturn(Optional.of(process));
        
        List<String> questions = Arrays.asList("What happens on failure?");
        String context = "Error handling is unclear in the diagram";
        
        // Act
        AiStateInfo result = orchestrator.requestClarification(processId, questions, context);
        
        // Assert
        assertEquals(AiState.CLARIFICATION_REQUIRED, result.getCurrentState());
        
        ClarificationRequest request = orchestrator.getClarificationRequest(processId);
        assertEquals(context, request.getContext());
    }
    
    @Test
    void testRequestClarificationEmptyQuestions() {
        // Arrange
        String processId = "proc-empty";
        ProcessModel process = createProcess(processId);
        when(processModelRepository.findById(processId)).thenReturn(Optional.of(process));
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> orchestrator.requestClarification(processId, List.of()));
    }
    
    @Test
    void testSubmitClarification() {
        // Arrange
        String processId = "proc-submit";
        ProcessModel process = createProcess(processId);
        when(processModelRepository.findById(processId)).thenReturn(Optional.of(process));
        
        // Request clarification
        List<String> questions = Arrays.asList("Question 1", "Question 2");
        orchestrator.requestClarification(processId, questions);
        
        // Create response
        ClarificationResponse response = new ClarificationResponse();
        response.setProcessId(processId);
        response.addAnswer("Question 1", "Answer 1");
        response.addAnswer("Question 2", "Answer 2");
        response.setAdditionalNotes("Additional context from user");
        
        // Act
        AiStateInfo result = orchestrator.submitClarification(processId, response);
        
        // Assert
        assertEquals(AiState.PROCESS_INFERRED, result.getCurrentState());
        assertFalse(orchestrator.hasPendingClarification(processId));
        assertNull(orchestrator.getClarificationRequest(processId));
    }
    
    @Test
    void testSubmitClarificationNotInClarificationState() {
        // Arrange
        String processId = "proc-wrong-state";
        ProcessModel process = createProcess(processId);
        when(processModelRepository.findById(processId)).thenReturn(Optional.of(process));
        
        orchestrator.updateState(processId, AiState.MODEL_READY);
        
        ClarificationResponse response = new ClarificationResponse();
        response.setProcessId(processId);
        
        // Act & Assert
        assertThrows(IllegalStateException.class,
            () -> orchestrator.submitClarification(processId, response));
    }
    
    @Test
    void testSubmitClarificationNoPendingRequest() {
        // Arrange
        String processId = "proc-no-request";
        ProcessModel process = createProcess(processId);
        when(processModelRepository.findById(processId)).thenReturn(Optional.of(process));
        
        orchestrator.updateState(processId, AiState.CLARIFICATION_REQUIRED);
        
        ClarificationResponse response = new ClarificationResponse();
        response.setProcessId(processId);
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> orchestrator.submitClarification(processId, response));
    }
    
    @Test
    void testCancelClarification() {
        // Arrange
        String processId = "proc-cancel";
        ProcessModel process = createProcess(processId);
        when(processModelRepository.findById(processId)).thenReturn(Optional.of(process));
        
        List<String> questions = Arrays.asList("Question");
        orchestrator.requestClarification(processId, questions);
        
        // Act
        AiStateInfo result = orchestrator.cancelClarification(processId);
        
        // Assert
        assertEquals(AiState.FAILED, result.getCurrentState());
        assertFalse(orchestrator.hasPendingClarification(processId));
    }
    
    @Test
    void testGetPendingClarificationCount() {
        // Arrange
        ProcessModel process1 = createProcess("proc-1");
        ProcessModel process2 = createProcess("proc-2");
        when(processModelRepository.findById("proc-1")).thenReturn(Optional.of(process1));
        when(processModelRepository.findById("proc-2")).thenReturn(Optional.of(process2));
        
        // Act
        orchestrator.requestClarification("proc-1", Arrays.asList("Q1"));
        orchestrator.requestClarification("proc-2", Arrays.asList("Q2"));
        
        // Assert
        assertEquals(2, orchestrator.getPendingClarificationCount());
        
        // Submit one clarification
        ClarificationResponse response = new ClarificationResponse();
        response.addAnswer("Q1", "A1");
        orchestrator.submitClarification("proc-1", response);
        
        assertEquals(1, orchestrator.getPendingClarificationCount());
    }
    
    @Test
    void testClearAllClearsClarifications() {
        // Arrange
        ProcessModel process = createProcess("proc-clear");
        when(processModelRepository.findById("proc-clear")).thenReturn(Optional.of(process));
        
        orchestrator.requestClarification("proc-clear", Arrays.asList("Q"));
        assertTrue(orchestrator.hasPendingClarification("proc-clear"));
        
        // Act
        orchestrator.clearAll();
        
        // Assert
        assertFalse(orchestrator.hasPendingClarification("proc-clear"));
        assertEquals(0, orchestrator.getPendingClarificationCount());
    }
    
    // ========== Retry Management Tests ==========
    
    @Test
    void testSetMaxRetries() {
        String processId = "proc-retry-1";
        
        orchestrator.setMaxRetries(processId, 5);
        
        assertEquals(5, orchestrator.getMaxRetries(processId));
    }
    
    @Test
    void testSetMaxRetriesNegative() {
        assertThrows(IllegalArgumentException.class, 
            () -> orchestrator.setMaxRetries("proc", -1));
    }
    
    @Test
    void testGetMaxRetriesDefaultValue() {
        String processId = "proc-default";
        
        // Should return default value (3)
        assertEquals(3, orchestrator.getMaxRetries(processId));
    }
    
    @Test
    void testGetRetryCount() {
        String processId = "proc-retry-2";
        
        // Initially zero
        assertEquals(0, orchestrator.getRetryCount(processId));
    }
    
    @Test
    void testRecordBpmnGenerationFailure() {
        // Arrange
        String processId = "proc-bpmn-fail";
        ProcessModel process = createProcess(processId);
        when(processModelRepository.findById(processId)).thenReturn(Optional.of(process));
        
        orchestrator.updateState(processId, AiState.BPMN_GENERATED);
        
        // Act
        AiStateInfo result = orchestrator.recordBpmnGenerationFailure(processId, "Invalid BPMN structure");
        
        // Assert
        assertNotNull(result);
        assertEquals(AiState.MODEL_READY, result.getCurrentState());
        assertEquals(1, orchestrator.getRetryCount(processId));
        
        List<String> explanations = orchestrator.getRetryExplanations(processId);
        assertEquals(1, explanations.size());
        assertTrue(explanations.get(0).contains("BPMN Generation Failed"));
    }
    
    @Test
    void testRecordDrlGenerationFailure() {
        // Arrange
        String processId = "proc-drl-fail";
        ProcessModel process = createProcess(processId);
        when(processModelRepository.findById(processId)).thenReturn(Optional.of(process));
        
        orchestrator.updateState(processId, AiState.DRL_GENERATED);
        
        // Act
        AiStateInfo result = orchestrator.recordDrlGenerationFailure(processId, "DRL syntax error");
        
        // Assert
        assertEquals(AiState.MODEL_READY, result.getCurrentState());
        assertEquals(1, orchestrator.getRetryCount(processId));
        
        List<String> explanations = orchestrator.getRetryExplanations(processId);
        assertEquals(1, explanations.size());
        assertTrue(explanations.get(0).contains("DRL Generation Failed"));
    }
    
    @Test
    void testAutoRetryUntilMaxReached() {
        // Arrange
        String processId = "proc-max-retry";
        ProcessModel process = createProcess(processId);
        when(processModelRepository.findById(processId)).thenReturn(Optional.of(process));
        
        orchestrator.setMaxRetries(processId, 2);
        
        // Act - First failure
        AiStateInfo result1 = orchestrator.recordBpmnGenerationFailure(processId, "Error 1");
        assertEquals(AiState.MODEL_READY, result1.getCurrentState());
        assertEquals(1, orchestrator.getRetryCount(processId));
        
        // Act - Second failure
        AiStateInfo result2 = orchestrator.recordBpmnGenerationFailure(processId, "Error 2");
        assertEquals(AiState.MODEL_READY, result2.getCurrentState());
        assertEquals(2, orchestrator.getRetryCount(processId));
        
        // Act - Third failure should mark as FAILED
        AiStateInfo result3 = orchestrator.recordBpmnGenerationFailure(processId, "Error 3");
        assertEquals(AiState.FAILED, result3.getCurrentState());
        assertEquals(3, orchestrator.getRetryCount(processId));
        
        // Verify explanations
        List<String> explanations = orchestrator.getRetryExplanations(processId);
        assertEquals(3, explanations.size());
    }
    
    @Test
    void testHasReachedMaxRetries() {
        // Arrange
        String processId = "proc-check-max";
        ProcessModel process = createProcess(processId);
        when(processModelRepository.findById(processId)).thenReturn(Optional.of(process));
        
        orchestrator.setMaxRetries(processId, 2);
        
        // Initially false
        assertFalse(orchestrator.hasReachedMaxRetries(processId));
        
        // After 1 retry
        orchestrator.recordBpmnGenerationFailure(processId, "Error 1");
        assertFalse(orchestrator.hasReachedMaxRetries(processId));
        
        // After 2 retries
        orchestrator.recordBpmnGenerationFailure(processId, "Error 2");
        assertFalse(orchestrator.hasReachedMaxRetries(processId));
        
        // After 3 retries (exceeds max)
        orchestrator.recordBpmnGenerationFailure(processId, "Error 3");
        assertTrue(orchestrator.hasReachedMaxRetries(processId));
    }
    
    @Test
    void testRecordGeneralGenerationFailure() {
        // Arrange
        String processId = "proc-general-fail";
        ProcessModel process = createProcess(processId);
        when(processModelRepository.findById(processId)).thenReturn(Optional.of(process));
        
        // Act
        AiStateInfo result = orchestrator.recordGenerationFailure(
            processId, 
            "Validation", 
            "Process validation failed"
        );
        
        // Assert
        assertEquals(AiState.MODEL_READY, result.getCurrentState());
        assertEquals(1, orchestrator.getRetryCount(processId));
        
        List<String> explanations = orchestrator.getRetryExplanations(processId);
        assertTrue(explanations.get(0).contains("Validation Generation Failed"));
    }
    
    @Test
    void testResetRetryTracking() {
        // Arrange
        String processId = "proc-reset-retry";
        ProcessModel process = createProcess(processId);
        when(processModelRepository.findById(processId)).thenReturn(Optional.of(process));
        
        orchestrator.setMaxRetries(processId, 5);
        orchestrator.recordBpmnGenerationFailure(processId, "Error");
        
        assertEquals(1, orchestrator.getRetryCount(processId));
        assertEquals(5, orchestrator.getMaxRetries(processId));
        
        // Act
        orchestrator.resetRetryTracking(processId);
        
        // Assert
        assertEquals(0, orchestrator.getRetryCount(processId));
        assertEquals(3, orchestrator.getMaxRetries(processId)); // Back to default
        assertTrue(orchestrator.getRetryExplanations(processId).isEmpty());
    }
    
    @Test
    void testClearAllClearsRetryTracking() {
        // Arrange
        String processId = "proc-clear-retry";
        ProcessModel process = createProcess(processId);
        when(processModelRepository.findById(processId)).thenReturn(Optional.of(process));
        
        orchestrator.setMaxRetries(processId, 5);
        orchestrator.recordBpmnGenerationFailure(processId, "Error");
        
        assertEquals(1, orchestrator.getRetryCount(processId));
        
        // Act
        orchestrator.clearAll();
        
        // Assert
        assertEquals(0, orchestrator.getRetryCount(processId));
        assertEquals(3, orchestrator.getMaxRetries(processId)); // Back to default
    }
    
    @Test
    void testMixedBpmnAndDrlFailures() {
        // Arrange
        String processId = "proc-mixed-fail";
        ProcessModel process = createProcess(processId);
        when(processModelRepository.findById(processId)).thenReturn(Optional.of(process));
        
        orchestrator.setMaxRetries(processId, 3);
        
        // Act
        orchestrator.recordBpmnGenerationFailure(processId, "BPMN Error 1");
        orchestrator.recordDrlGenerationFailure(processId, "DRL Error 1");
        orchestrator.recordBpmnGenerationFailure(processId, "BPMN Error 2");
        
        // Assert
        assertEquals(3, orchestrator.getRetryCount(processId));
        
        List<String> explanations = orchestrator.getRetryExplanations(processId);
        assertEquals(3, explanations.size());
        assertTrue(explanations.get(0).contains("BPMN"));
        assertTrue(explanations.get(1).contains("DRL"));
        assertTrue(explanations.get(2).contains("BPMN"));
    }
    
    @Test
    void testGetRetryExplanationsEmptyWhenNoFailures() {
        String processId = "proc-no-fails";
        
        List<String> explanations = orchestrator.getRetryExplanations(processId);
        
        assertNotNull(explanations);
        assertTrue(explanations.isEmpty());
    }
    
    @Test
    void testZeroMaxRetriesImmediateFailure() {
        // Arrange
        String processId = "proc-zero-retry";
        ProcessModel process = createProcess(processId);
        when(processModelRepository.findById(processId)).thenReturn(Optional.of(process));
        
        orchestrator.setMaxRetries(processId, 0);
        
        // Act
        AiStateInfo result = orchestrator.recordBpmnGenerationFailure(processId, "Error");
        
        // Assert
        assertEquals(AiState.FAILED, result.getCurrentState());
        assertEquals(1, orchestrator.getRetryCount(processId));
    }
    
    private ProcessModel createProcess(String processId) {
        ProcessModel process = new ProcessModel();
        process.setId(processId);
        process.setName("Test Process");
        process.setVersion("1.0.0");
        process.setStatus(ProcessStatus.DRAFT);
        return process;
    }
}

