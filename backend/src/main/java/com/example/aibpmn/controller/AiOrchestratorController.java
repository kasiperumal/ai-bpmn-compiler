package com.example.aibpmn.controller;

import com.example.aibpmn.dto.AiStateInfo;
import com.example.aibpmn.dto.ClarificationRequest;
import com.example.aibpmn.dto.ClarificationResponse;
import com.example.aibpmn.service.AiOrchestratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for AI orchestration operations
 */
@RestController
@RequestMapping("/api/orchestrator")
public class AiOrchestratorController {
    
    private static final Logger logger = LoggerFactory.getLogger(AiOrchestratorController.class);
    
    private final AiOrchestratorService orchestratorService;
    
    public AiOrchestratorController(AiOrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }
    
    /**
     * Start AI inference for a process
     * POST /api/orchestrator/{processId}/start-inference
     */
    @PostMapping("/{processId}/start-inference")
    public ResponseEntity<?> startInference(@PathVariable String processId) {
        try {
            logger.info("Starting inference for processId: {}", processId);
            AiStateInfo stateInfo = orchestratorService.startInference(processId);
            return ResponseEntity.ok(stateInfo);
        } catch (IllegalArgumentException e) {
            logger.error("Process not found: {}", processId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            logger.error("Invalid state transition: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error starting inference for processId: {}", processId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to start inference: " + e.getMessage()));
        }
    }
    
    /**
     * Approve a step and advance to the next state
     * POST /api/orchestrator/{processId}/approve
     */
    @PostMapping("/{processId}/approve")
    public ResponseEntity<?> approveStep(
            @PathVariable String processId,
            @RequestParam(required = false) String stepId) {
        try {
            logger.info("Approving step for processId: {}, stepId: {}", processId, stepId);
            AiStateInfo stateInfo = orchestratorService.approveStep(processId, stepId);
            return ResponseEntity.ok(stateInfo);
        } catch (IllegalArgumentException e) {
            logger.error("Process not found: {}", processId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            logger.error("Cannot approve in current state: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error approving step for processId: {}", processId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to approve step: " + e.getMessage()));
        }
    }
    
    /**
     * Retry a failed process
     * POST /api/orchestrator/{processId}/retry
     */
    @PostMapping("/{processId}/retry")
    public ResponseEntity<?> retry(@PathVariable String processId) {
        try {
            logger.info("Retrying processId: {}", processId);
            AiStateInfo stateInfo = orchestratorService.retry(processId);
            return ResponseEntity.ok(stateInfo);
        } catch (IllegalArgumentException e) {
            logger.error("Process not found: {}", processId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            logger.error("Cannot retry in current state: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error retrying processId: {}", processId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to retry process: " + e.getMessage()));
        }
    }
    
    /**
     * Get current AI state for a process
     * GET /api/orchestrator/{processId}/state
     */
    @GetMapping("/{processId}/state")
    public ResponseEntity<?> getState(@PathVariable String processId) {
        try {
            logger.debug("Getting state for processId: {}", processId);
            AiStateInfo stateInfo = orchestratorService.getStateInfo(processId);
            
            if (stateInfo == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Process not tracked: " + processId));
            }
            
            return ResponseEntity.ok(stateInfo);
        } catch (Exception e) {
            logger.error("Error getting state for processId: {}", processId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get state: " + e.getMessage()));
        }
    }
    
    /**
     * Mark a process as failed
     * POST /api/orchestrator/{processId}/fail
     */
    @PostMapping("/{processId}/fail")
    public ResponseEntity<?> markAsFailed(
            @PathVariable String processId,
            @RequestParam(required = false) String reason) {
        try {
            logger.warn("Marking processId {} as failed. Reason: {}", processId, reason);
            AiStateInfo stateInfo = orchestratorService.markAsFailed(processId, reason);
            return ResponseEntity.ok(stateInfo);
        } catch (Exception e) {
            logger.error("Error marking process as failed: {}", processId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to mark as failed: " + e.getMessage()));
        }
    }
    
    /**
     * Advance to the next state
     * POST /api/orchestrator/{processId}/advance
     */
    @PostMapping("/{processId}/advance")
    public ResponseEntity<?> advanceState(@PathVariable String processId) {
        try {
            logger.info("Advancing state for processId: {}", processId);
            
            if (!orchestratorService.canAdvance(processId)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Cannot advance from current state"));
            }
            
            orchestratorService.advanceState(processId);
            AiStateInfo stateInfo = orchestratorService.getStateInfo(processId);
            
            return ResponseEntity.ok(stateInfo);
        } catch (IllegalStateException e) {
            logger.error("Cannot advance state: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error advancing state for processId: {}", processId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to advance state: " + e.getMessage()));
        }
    }
    
    /**
     * Check if a process can advance to the next state
     * GET /api/orchestrator/{processId}/can-advance
     */
    @GetMapping("/{processId}/can-advance")
    public ResponseEntity<?> canAdvance(@PathVariable String processId) {
        try {
            boolean canAdvance = orchestratorService.canAdvance(processId);
            return ResponseEntity.ok(Map.of(
                "processId", processId,
                "canAdvance", canAdvance
            ));
        } catch (Exception e) {
            logger.error("Error checking if can advance: {}", processId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to check advance capability: " + e.getMessage()));
        }
    }
    
    /**
     * Get count of tracked processes
     * GET /api/orchestrator/tracked-count
     */
    @GetMapping("/tracked-count")
    public ResponseEntity<?> getTrackedCount() {
        try {
            long count = orchestratorService.getTrackedProcessCount();
            return ResponseEntity.ok(Map.of("trackedProcessCount", count));
        } catch (Exception e) {
            logger.error("Error getting tracked count", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get tracked count: " + e.getMessage()));
        }
    }
    
    // ========== Clarification Endpoints ==========
    
    /**
     * Get clarification request for a process
     * GET /api/orchestrator/{processId}/clarification
     */
    @GetMapping("/{processId}/clarification")
    public ResponseEntity<?> getClarification(@PathVariable String processId) {
        try {
            logger.info("Getting clarification for processId: {}", processId);
            
            ClarificationRequest request = orchestratorService.getClarificationRequest(processId);
            
            if (request == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No pending clarification for process: " + processId));
            }
            
            return ResponseEntity.ok(request);
            
        } catch (Exception e) {
            logger.error("Error getting clarification for processId: {}", processId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get clarification: " + e.getMessage()));
        }
    }
    
    /**
     * Submit clarification response
     * POST /api/orchestrator/{processId}/clarification
     */
    @PostMapping("/{processId}/clarification")
    public ResponseEntity<?> submitClarification(
            @PathVariable String processId,
            @RequestBody ClarificationResponse response) {
        try {
            logger.info("Submitting clarification for processId: {}", processId);
            
            // Set processId from path if not in body
            if (response.getProcessId() == null) {
                response.setProcessId(processId);
            }
            
            AiStateInfo stateInfo = orchestratorService.submitClarification(processId, response);
            
            return ResponseEntity.ok(stateInfo);
            
        } catch (IllegalArgumentException e) {
            logger.error("No clarification pending: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
                
        } catch (IllegalStateException e) {
            logger.error("Invalid state for clarification: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", e.getMessage()));
                
        } catch (Exception e) {
            logger.error("Error submitting clarification for processId: {}", processId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to submit clarification: " + e.getMessage()));
        }
    }
    
    /**
     * Check if process has pending clarification
     * GET /api/orchestrator/{processId}/has-clarification
     */
    @GetMapping("/{processId}/has-clarification")
    public ResponseEntity<?> hasPendingClarification(@PathVariable String processId) {
        try {
            boolean hasClarification = orchestratorService.hasPendingClarification(processId);
            
            return ResponseEntity.ok(Map.of(
                "processId", processId,
                "hasPendingClarification", hasClarification
            ));
            
        } catch (Exception e) {
            logger.error("Error checking clarification for processId: {}", processId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to check clarification: " + e.getMessage()));
        }
    }
    
    /**
     * Cancel clarification and mark process as failed
     * DELETE /api/orchestrator/{processId}/clarification
     */
    @DeleteMapping("/{processId}/clarification")
    public ResponseEntity<?> cancelClarification(@PathVariable String processId) {
        try {
            logger.info("Cancelling clarification for processId: {}", processId);
            
            AiStateInfo stateInfo = orchestratorService.cancelClarification(processId);
            
            return ResponseEntity.ok(stateInfo);
            
        } catch (Exception e) {
            logger.error("Error cancelling clarification for processId: {}", processId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to cancel clarification: " + e.getMessage()));
        }
    }
    
    /**
     * Get count of processes with pending clarification
     * GET /api/orchestrator/clarification-count
     */
    @GetMapping("/clarification-count")
    public ResponseEntity<?> getClarificationCount() {
        try {
            long count = orchestratorService.getPendingClarificationCount();
            return ResponseEntity.ok(Map.of("pendingClarificationCount", count));
        } catch (Exception e) {
            logger.error("Error getting clarification count", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get clarification count: " + e.getMessage()));
        }
    }
}

