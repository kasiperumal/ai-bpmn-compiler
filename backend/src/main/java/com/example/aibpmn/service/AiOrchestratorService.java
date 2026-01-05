package com.example.aibpmn.service;

import com.example.aibpmn.dto.AiStateInfo;
import com.example.aibpmn.dto.ClarificationRequest;
import com.example.aibpmn.dto.ClarificationResponse;
import com.example.aibpmn.model.AiState;
import com.example.aibpmn.model.ProcessModel;
import com.example.aibpmn.repository.ProcessModelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Orchestrates AI processing workflow and state management with automatic retry capabilities
 */
@Service
public class AiOrchestratorService {
    
    private static final Logger logger = LoggerFactory.getLogger(AiOrchestratorService.class);
    
    // Default maximum retry count
    private static final int DEFAULT_MAX_RETRIES = 3;
    
    // In-memory state tracking (key: processId, value: current AiState)
    private final ConcurrentHashMap<String, AiState> stateTracker = new ConcurrentHashMap<>();
    
    // In-memory clarification tracking (key: processId, value: clarification request)
    private final ConcurrentHashMap<String, ClarificationRequest> clarificationTracker = new ConcurrentHashMap<>();
    
    // Retry tracking (key: processId, value: retry count)
    private final ConcurrentHashMap<String, Integer> retryCountTracker = new ConcurrentHashMap<>();
    
    // Retry explanations (key: processId, value: list of retry reasons)
    private final ConcurrentHashMap<String, List<String>> retryExplanations = new ConcurrentHashMap<>();
    
    // Max retries per process (key: processId, value: max retries) - defaults to DEFAULT_MAX_RETRIES
    private final ConcurrentHashMap<String, Integer> maxRetriesPerProcess = new ConcurrentHashMap<>();
    
    private final ProcessModelRepository processModelRepository;
    
    public AiOrchestratorService(ProcessModelRepository processModelRepository) {
        this.processModelRepository = processModelRepository;
    }
    
    /**
     * Start AI inference for a process
     * 
     * @param processId The process ID
     * @return Current state information
     * @throws IllegalArgumentException if process doesn't exist
     * @throws IllegalStateException if process is not in initial state
     */
    public AiStateInfo startInference(String processId) {
        logger.info("Starting inference for processId: {}", processId);
        
        // Verify process exists
        ProcessModel process = getProcessOrThrow(processId);
        
        // Get current state
        AiState currentState = getCurrentState(processId);
        
        // Validate process is in initial state
        if (!currentState.isInitialState()) {
            throw new IllegalStateException(
                String.format("Cannot start inference: process %s is already in state %s", 
                    processId, currentState)
            );
        }
        
        // Transition to PROCESS_INFERRED
        // In a real implementation, this would trigger AI processing
        AiState newState = AiState.PROCESS_INFERRED;
        updateState(processId, newState);
        
        logger.info("Process {} transitioned from {} to {}", processId, currentState, newState);
        
        return new AiStateInfo(processId, newState);
    }
    
    /**
     * Approve a step and advance to the next state
     * 
     * @param processId The process ID
     * @param stepId Optional step identifier for tracking
     * @return Current state information
     * @throws IllegalArgumentException if process doesn't exist
     * @throws IllegalStateException if approval not possible in current state
     */
    public AiStateInfo approveStep(String processId, String stepId) {
        logger.info("Approving step for processId: {}, stepId: {}", processId, stepId);
        
        // Verify process exists
        ProcessModel process = getProcessOrThrow(processId);
        
        // Get current state
        AiState currentState = getCurrentState(processId);
        
        // Can only approve if in MODEL_READY or CLARIFICATION_REQUIRED
        if (currentState != AiState.MODEL_READY && currentState != AiState.CLARIFICATION_REQUIRED) {
            throw new IllegalStateException(
                String.format("Cannot approve: process %s is in state %s which doesn't require approval", 
                    processId, currentState)
            );
        }
        
        // Advance to next state
        AiState newState = advanceState(processId);
        
        logger.info("Process {} approved and transitioned from {} to {}", 
            processId, currentState, newState);
        
        return new AiStateInfo(processId, newState);
    }
    
    /**
     * Retry processing for a failed process
     * 
     * @param processId The process ID
     * @return Current state information
     * @throws IllegalArgumentException if process doesn't exist
     * @throws IllegalStateException if process is not in FAILED state
     */
    public AiStateInfo retry(String processId) {
        logger.info("Retrying process: {}", processId);
        
        // Verify process exists
        ProcessModel process = getProcessOrThrow(processId);
        
        // Get current state
        AiState currentState = getCurrentState(processId);
        
        // Can only retry if FAILED
        if (currentState != AiState.FAILED) {
            throw new IllegalStateException(
                String.format("Cannot retry: process %s is in state %s, not FAILED", 
                    processId, currentState)
            );
        }
        
        // Reset to initial state based on how it was created
        // For now, default to TEXT_RECEIVED (could be enhanced to remember original state)
        AiState newState = AiState.TEXT_RECEIVED;
        updateState(processId, newState);
        
        logger.info("Process {} reset from FAILED to {}", processId, newState);
        
        return new AiStateInfo(processId, newState);
    }
    
    /**
     * Get current AI state for a process
     * 
     * @param processId The process ID
     * @return Current state information, or null if not tracked
     */
    public AiStateInfo getStateInfo(String processId) {
        AiState state = stateTracker.get(processId);
        if (state == null) {
            return null;
        }
        return new AiStateInfo(processId, state);
    }
    
    /**
     * Get current AI state (raw enum)
     * 
     * @param processId The process ID
     * @return Current AiState, or TEXT_RECEIVED if not tracked
     */
    public AiState getCurrentState(String processId) {
        return stateTracker.getOrDefault(processId, AiState.TEXT_RECEIVED);
    }
    
    /**
     * Set the initial state for a process
     * 
     * @param processId The process ID
     * @param initialState The initial state (should be IMAGE_RECEIVED or TEXT_RECEIVED)
     */
    public void setInitialState(String processId, AiState initialState) {
        if (!initialState.isInitialState()) {
            throw new IllegalArgumentException(
                "Initial state must be IMAGE_RECEIVED or TEXT_RECEIVED, got: " + initialState
            );
        }
        
        logger.info("Setting initial state for process {}: {}", processId, initialState);
        stateTracker.put(processId, initialState);
    }
    
    /**
     * Update state to a specific value
     * 
     * @param processId The process ID
     * @param newState The new state
     */
    public void updateState(String processId, AiState newState) {
        AiState oldState = stateTracker.put(processId, newState);
        logger.debug("State updated for process {}: {} -> {}", processId, oldState, newState);
    }
    
    /**
     * Mark a process as failed
     * 
     * @param processId The process ID
     * @param reason Optional failure reason
     * @return Current state information
     */
    public AiStateInfo markAsFailed(String processId, String reason) {
        logger.warn("Marking process {} as FAILED. Reason: {}", processId, reason);
        updateState(processId, AiState.FAILED);
        return new AiStateInfo(processId, AiState.FAILED);
    }
    
    /**
     * Advance to the next state in the workflow
     * 
     * @param processId The process ID
     * @return The new state
     * @throws IllegalStateException if cannot advance from current state
     */
    public AiState advanceState(String processId) {
        AiState currentState = getCurrentState(processId);
        AiState nextState = currentState.getNextState();
        
        if (nextState == null) {
            throw new IllegalStateException(
                String.format("Cannot advance from state %s", currentState)
            );
        }
        
        updateState(processId, nextState);
        return nextState;
    }
    
    /**
     * Check if a process can be advanced to the next state
     * 
     * @param processId The process ID
     * @return true if can advance, false otherwise
     */
    public boolean canAdvance(String processId) {
        AiState currentState = getCurrentState(processId);
        return currentState.getNextState() != null;
    }
    
    /**
     * Reset state tracking for a process (useful for testing)
     * 
     * @param processId The process ID
     */
    public void resetState(String processId) {
        logger.info("Resetting state for process: {}", processId);
        stateTracker.remove(processId);
    }
    
    /**
     * Get count of tracked processes
     * 
     * @return Number of processes being tracked
     */
    public long getTrackedProcessCount() {
        return stateTracker.size();
    }
    
    /**
     * Check if a process is being tracked
     * 
     * @param processId The process ID
     * @return true if tracked, false otherwise
     */
    public boolean isTracked(String processId) {
        return stateTracker.containsKey(processId);
    }
    
    /**
     * Clear all tracked states (useful for testing)
     */
    public void clearAll() {
        logger.info("Clearing all tracked states");
        stateTracker.clear();
        clarificationTracker.clear();
        retryCountTracker.clear();
        retryExplanations.clear();
        maxRetriesPerProcess.clear();
    }
    
    // ========== Retry Management Methods ==========
    
    /**
     * Set maximum retry count for a process.
     *
     * @param processId The process ID
     * @param maxRetries Maximum number of retries (must be >= 0)
     */
    public void setMaxRetries(String processId, int maxRetries) {
        if (maxRetries < 0) {
            throw new IllegalArgumentException("Max retries cannot be negative");
        }
        logger.info("Setting max retries for process {} to {}", processId, maxRetries);
        maxRetriesPerProcess.put(processId, maxRetries);
    }
    
    /**
     * Get maximum retry count for a process.
     *
     * @param processId The process ID
     * @return Maximum retry count (defaults to DEFAULT_MAX_RETRIES)
     */
    public int getMaxRetries(String processId) {
        return maxRetriesPerProcess.getOrDefault(processId, DEFAULT_MAX_RETRIES);
    }
    
    /**
     * Get current retry count for a process.
     *
     * @param processId The process ID
     * @return Current retry count
     */
    public int getRetryCount(String processId) {
        return retryCountTracker.getOrDefault(processId, 0);
    }
    
    /**
     * Get retry explanations for a process.
     *
     * @param processId The process ID
     * @return List of retry reasons (empty if none)
     */
    public List<String> getRetryExplanations(String processId) {
        return retryExplanations.getOrDefault(processId, Collections.emptyList());
    }
    
    /**
     * Check if a process has reached max retries.
     *
     * @param processId The process ID
     * @return true if max retries exceeded, false otherwise
     */
    public boolean hasReachedMaxRetries(String processId) {
        int current = getRetryCount(processId);
        int max = getMaxRetries(processId);
        return current > max;
    }
    
    /**
     * Record a BPMN generation failure and attempt auto-retry.
     *
     * @param processId The process ID
     * @param errorMessage The error message from BPMN generation
     * @return AiStateInfo with current state
     */
    public AiStateInfo recordBpmnGenerationFailure(String processId, String errorMessage) {
        logger.warn("BPMN generation failed for process {}: {}", processId, errorMessage);
        
        // Verify process exists
        getProcessOrThrow(processId);
        
        // Record retry explanation
        String explanation = "BPMN Generation Failed: " + errorMessage;
        recordRetryExplanation(processId, explanation);
        
        // Increment retry count first
        int retryCount = retryCountTracker.merge(processId, 1, Integer::sum);
        
        // Check if exceeded max retries
        if (hasReachedMaxRetries(processId)) {
            logger.error("Process {} has reached max retries ({}/{}). Marking as FAILED.", 
                processId, retryCount, getMaxRetries(processId));
            return markAsFailed(processId, "Max retries reached after BPMN generation failures");
        }
        
        logger.info("Auto-retrying process {} (attempt {}/{})", 
            processId, retryCount, getMaxRetries(processId));
        
        // Reset to MODEL_READY for regeneration
        updateState(processId, AiState.MODEL_READY);
        
        return new AiStateInfo(processId, AiState.MODEL_READY);
    }
    
    /**
     * Record a DRL generation failure and attempt auto-retry.
     *
     * @param processId The process ID
     * @param errorMessage The error message from DRL generation
     * @return AiStateInfo with current state
     */
    public AiStateInfo recordDrlGenerationFailure(String processId, String errorMessage) {
        logger.warn("DRL generation failed for process {}: {}", processId, errorMessage);
        
        // Verify process exists
        getProcessOrThrow(processId);
        
        // Record retry explanation
        String explanation = "DRL Generation Failed: " + errorMessage;
        recordRetryExplanation(processId, explanation);
        
        // Increment retry count first
        int retryCount = retryCountTracker.merge(processId, 1, Integer::sum);
        
        // Check if exceeded max retries
        if (hasReachedMaxRetries(processId)) {
            logger.error("Process {} has reached max retries ({}/{}). Marking as FAILED.", 
                processId, retryCount, getMaxRetries(processId));
            return markAsFailed(processId, "Max retries reached after DRL generation failures");
        }
        
        logger.info("Auto-retrying process {} (attempt {}/{})", 
            processId, retryCount, getMaxRetries(processId));
        
        // Reset to MODEL_READY for regeneration
        updateState(processId, AiState.MODEL_READY);
        
        return new AiStateInfo(processId, AiState.MODEL_READY);
    }
    
    /**
     * Record a general generation failure and attempt auto-retry.
     *
     * @param processId The process ID
     * @param failureType Type of failure (e.g., "BPMN", "DRL", "Validation")
     * @param errorMessage The error message
     * @return AiStateInfo with current state
     */
    public AiStateInfo recordGenerationFailure(String processId, String failureType, String errorMessage) {
        logger.warn("{} generation failed for process {}: {}", failureType, processId, errorMessage);
        
        // Verify process exists
        getProcessOrThrow(processId);
        
        // Record retry explanation
        String explanation = failureType + " Generation Failed: " + errorMessage;
        recordRetryExplanation(processId, explanation);
        
        // Increment retry count first
        int retryCount = retryCountTracker.merge(processId, 1, Integer::sum);
        
        // Check if exceeded max retries
        if (hasReachedMaxRetries(processId)) {
            logger.error("Process {} has reached max retries ({}/{}). Marking as FAILED.", 
                processId, retryCount, getMaxRetries(processId));
            return markAsFailed(processId, 
                "Max retries reached after " + failureType + " generation failures");
        }
        
        logger.info("Auto-retrying process {} (attempt {}/{})", 
            processId, retryCount, getMaxRetries(processId));
        
        // Reset to MODEL_READY for regeneration
        updateState(processId, AiState.MODEL_READY);
        
        return new AiStateInfo(processId, AiState.MODEL_READY);
    }
    
    /**
     * Reset retry tracking for a process (useful when starting fresh).
     *
     * @param processId The process ID
     */
    public void resetRetryTracking(String processId) {
        logger.info("Resetting retry tracking for process: {}", processId);
        retryCountTracker.remove(processId);
        retryExplanations.remove(processId);
        maxRetriesPerProcess.remove(processId);
    }
    
    /**
     * Record a retry explanation.
     *
     * @param processId The process ID
     * @param explanation The retry reason
     */
    private void recordRetryExplanation(String processId, String explanation) {
        retryExplanations.computeIfAbsent(processId, k -> new CopyOnWriteArrayList<>())
            .add(explanation);
        logger.debug("Recorded retry explanation for process {}: {}", processId, explanation);
    }
    
    // ========== Clarification Methods ==========
    
    /**
     * Request clarification by pausing inference and storing questions.
     *
     * @param processId The process ID
     * @param questions List of clarification questions
     * @return Current state information
     * @throws IllegalArgumentException if process doesn't exist or questions are empty
     */
    public AiStateInfo requestClarification(String processId, List<String> questions) {
        return requestClarification(processId, questions, null);
    }
    
    /**
     * Request clarification with context.
     *
     * @param processId The process ID
     * @param questions List of clarification questions
     * @param context Optional context about why clarification is needed
     * @return Current state information
     * @throws IllegalArgumentException if process doesn't exist or questions are empty
     */
    public AiStateInfo requestClarification(String processId, List<String> questions, String context) {
        logger.info("Requesting clarification for processId: {} ({} questions)", 
            processId, questions.size());
        
        // Verify process exists
        getProcessOrThrow(processId);
        
        // Validate questions
        if (questions == null || questions.isEmpty()) {
            throw new IllegalArgumentException("Clarification questions cannot be null or empty");
        }
        
        // Create clarification request
        ClarificationRequest request = new ClarificationRequest(processId, questions);
        request.setContext(context);
        
        // Store clarification request
        clarificationTracker.put(processId, request);
        
        // Update state to CLARIFICATION_REQUIRED
        updateState(processId, AiState.CLARIFICATION_REQUIRED);
        
        logger.info("Clarification requested for processId: {}, questions: {}", 
            processId, questions);
        
        return new AiStateInfo(processId, AiState.CLARIFICATION_REQUIRED);
    }
    
    /**
     * Get clarification request for a process.
     *
     * @param processId The process ID
     * @return ClarificationRequest or null if no clarification pending
     */
    public ClarificationRequest getClarificationRequest(String processId) {
        return clarificationTracker.get(processId);
    }
    
    /**
     * Check if process has pending clarification.
     *
     * @param processId The process ID
     * @return true if clarification is pending, false otherwise
     */
    public boolean hasPendingClarification(String processId) {
        return clarificationTracker.containsKey(processId);
    }
    
    /**
     * Submit clarification response and resume inference.
     *
     * @param processId The process ID
     * @param response User's clarification responses
     * @return Current state information
     * @throws IllegalArgumentException if process doesn't exist or no clarification pending
     * @throws IllegalStateException if process is not in CLARIFICATION_REQUIRED state
     */
    public AiStateInfo submitClarification(String processId, ClarificationResponse response) {
        logger.info("Submitting clarification for processId: {} ({} answers)",
            processId, response.getAnswerCount());
        
        // Verify process exists
        getProcessOrThrow(processId);
        
        // Verify in clarification state
        AiState currentState = getCurrentState(processId);
        if (currentState != AiState.CLARIFICATION_REQUIRED) {
            throw new IllegalStateException(
                String.format("Cannot submit clarification: process %s is in state %s, not CLARIFICATION_REQUIRED",
                    processId, currentState)
            );
        }
        
        // Verify clarification request exists
        ClarificationRequest request = clarificationTracker.get(processId);
        if (request == null) {
            throw new IllegalArgumentException("No pending clarification request for process: " + processId);
        }
        
        // Log clarification responses
        logger.info("Clarification responses for processId {}:", processId);
        response.getAnswers().forEach((question, answer) -> {
            logger.info("  Q: {} -> A: {}", question, answer);
        });
        
        if (response.getAdditionalNotes() != null) {
            logger.info("  Additional notes: {}", response.getAdditionalNotes());
        }
        
        // Remove clarification request (it's been answered)
        clarificationTracker.remove(processId);
        
        // Resume inference by transitioning back to PROCESS_INFERRED
        // This allows the AI workflow to continue with the new information
        updateState(processId, AiState.PROCESS_INFERRED);
        
        logger.info("Clarification submitted, resuming inference for processId: {}", processId);
        
        return new AiStateInfo(processId, AiState.PROCESS_INFERRED);
    }
    
    /**
     * Cancel clarification and mark process as failed.
     *
     * @param processId The process ID
     * @return Current state information
     */
    public AiStateInfo cancelClarification(String processId) {
        logger.info("Cancelling clarification for processId: {}", processId);
        
        // Remove clarification request
        clarificationTracker.remove(processId);
        
        // Mark as failed
        return markAsFailed(processId, "Clarification cancelled by user");
    }
    
    /**
     * Get count of processes with pending clarification.
     *
     * @return Number of processes waiting for clarification
     */
    public long getPendingClarificationCount() {
        return clarificationTracker.size();
    }
    
    /**
     * Get the process model or throw exception
     */
    private ProcessModel getProcessOrThrow(String processId) {
        Optional<ProcessModel> processOpt = processModelRepository.findById(processId);
        if (!processOpt.isPresent()) {
            throw new IllegalArgumentException("Process not found: " + processId);
        }
        return processOpt.get();
    }
}

