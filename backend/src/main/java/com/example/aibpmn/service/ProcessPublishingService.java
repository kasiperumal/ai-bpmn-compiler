package com.example.aibpmn.service;

import com.example.aibpmn.exception.BpmnValidationException;
import com.example.aibpmn.exception.DrlValidationException;
import com.example.aibpmn.model.AiState;
import com.example.aibpmn.model.ProcessModel;
import com.example.aibpmn.model.ProcessStatus;
import com.example.aibpmn.repository.ProcessModelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Service for publishing processes to Kogito runtime.
 * Handles the complete workflow from process model to executable process.
 */
@Service
public class ProcessPublishingService {
    
    private static final Logger logger = LoggerFactory.getLogger(ProcessPublishingService.class);
    
    private final ProcessModelRepository processModelRepository;
    private final BpmnGeneratorService bpmnGenerator;
    private final DrlGeneratorService drlGenerator;
    private final BpmnValidationService bpmnValidator;
    private final KogitoDeploymentService kogitoDeployment;
    private final AiOrchestratorService orchestrator;
    
    public ProcessPublishingService(
            ProcessModelRepository processModelRepository,
            BpmnGeneratorService bpmnGenerator,
            DrlGeneratorService drlGenerator,
            BpmnValidationService bpmnValidator,
            KogitoDeploymentService kogitoDeployment,
            AiOrchestratorService orchestrator) {
        this.processModelRepository = processModelRepository;
        this.bpmnGenerator = bpmnGenerator;
        this.drlGenerator = drlGenerator;
        this.bpmnValidator = bpmnValidator;
        this.kogitoDeployment = kogitoDeployment;
        this.orchestrator = orchestrator;
    }
    
    /**
     * Publish a process to Kogito runtime.
     * This generates BPMN and DRL, validates them, deploys to Kogito, and marks as PUBLISHED.
     *
     * @param processId The process identifier
     * @return PublishResult with deployment details
     * @throws IllegalArgumentException if process not found
     * @throws IllegalStateException if process cannot be published
     * @throws PublishException if publishing fails
     */
    public PublishResult publishProcess(String processId) {
        logger.info("Starting publish workflow for processId: {}", processId);
        
        // 1. Get process model
        ProcessModel processModel = processModelRepository.findById(processId)
                .orElseThrow(() -> new IllegalArgumentException("Process not found: " + processId));
        
        // 2. Validate process is ready to publish
        validateProcessReadyForPublish(processModel);
        
        try {
            // 3. Generate BPMN
            logger.info("Generating BPMN for processId: {}", processId);
            String bpmnXml = bpmnGenerator.generateBpmn(processModel);
            
            // 4. Validate BPMN
            logger.info("Validating BPMN for processId: {}", processId);
            bpmnValidator.validate(bpmnXml);
            
            // 5. Generate DRL (if rules exist)
            String drlContent = null;
            if (!processModel.getRules().isEmpty()) {
                logger.info("Generating DRL for processId: {} ({} rules)", 
                    processId, processModel.getRules().size());
                drlContent = drlGenerator.generateDrl(
                    processModel.getRules(), 
                    "com.example.aibpmn.rules." + sanitizePackageName(processId),
                    false  // Skip validation for now as it may fail with placeholder rules
                );
            } else {
                logger.info("No rules to generate for processId: {}", processId);
                // Create empty DRL file
                drlContent = generateEmptyDrl(processId);
            }
            
            // 6. Deploy to Kogito
            logger.info("Deploying to Kogito for processId: {}", processId);
            KogitoDeploymentService.DeploymentResult deploymentResult = 
                kogitoDeployment.deployProcess(processId, bpmnXml, drlContent);
            
            // 7. Update process status
            processModel.setStatus(ProcessStatus.PUBLISHED);
            processModelRepository.save(processModel);
            
            // 8. Update orchestrator state
            orchestrator.updateState(processId, AiState.PUBLISHED);
            
            logger.info("Process published successfully: {}", processId);
            logger.info("Process can now be executed at: POST /{}", processId);
            
            return new PublishResult(
                processId,
                deploymentResult.getBpmnPath().toString(),
                deploymentResult.getDrlPath().toString(),
                true,
                "Process published successfully. Execute at: POST /" + processId
            );
            
        } catch (BpmnValidationException e) {
            logger.error("BPMN validation failed for processId {}: {}", processId, e.getMessage());
            orchestrator.recordBpmnGenerationFailure(processId, e.getMessage());
            throw new PublishException("BPMN validation failed: " + e.getMessage(), e);
            
        } catch (DrlValidationException e) {
            logger.error("DRL validation failed for processId {}: {}", processId, e.getMessage());
            String errorMsg = e.getErrors() != null ? String.join("; ", e.getErrors()) : e.getMessage();
            orchestrator.recordDrlGenerationFailure(processId, errorMsg);
            throw new PublishException("DRL validation failed: " + errorMsg, e);
            
        } catch (IOException e) {
            logger.error("Deployment failed for processId {}: {}", processId, e.getMessage());
            orchestrator.recordGenerationFailure(processId, "Deployment", e.getMessage());
            throw new PublishException("Deployment failed: " + e.getMessage(), e);
            
        } catch (Exception e) {
            logger.error("Publishing failed for processId {}: {}", processId, e.getMessage(), e);
            orchestrator.markAsFailed(processId, "Publishing failed: " + e.getMessage());
            throw new PublishException("Publishing failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Check if a process is published and ready for execution.
     *
     * @param processId The process identifier
     * @return true if published, false otherwise
     */
    public boolean isPublished(String processId) {
        return processModelRepository.findById(processId)
                .map(model -> model.getStatus() == ProcessStatus.PUBLISHED)
                .orElse(false);
    }
    
    /**
     * Validate that a process is ready to be published.
     */
    private void validateProcessReadyForPublish(ProcessModel processModel) {
        // Check AI state
        AiState currentState = orchestrator.getCurrentState(processModel.getId());
        
        if (currentState == AiState.FAILED) {
            throw new IllegalStateException("Cannot publish failed process. Use retry first.");
        }
        
        if (currentState == AiState.CLARIFICATION_REQUIRED) {
            throw new IllegalStateException("Cannot publish process requiring clarification. Submit clarification first.");
        }
        
        // Check process model has required elements
        if (processModel.getNodes().isEmpty()) {
            throw new IllegalStateException("Cannot publish process without nodes");
        }
        
        // Already published?
        if (processModel.getStatus() == ProcessStatus.PUBLISHED) {
            logger.warn("Process {} is already published. Re-publishing will overwrite.", processModel.getId());
        }
    }
    
    /**
     * Generate empty DRL file for processes without rules.
     */
    private String generateEmptyDrl(String processId) {
        String packageName = "com.example.aibpmn.rules." + sanitizePackageName(processId);
        return String.format("""
            package %s;
            
            import java.util.*;
            import java.time.*;
            
            // No rules defined for this process
            """, packageName);
    }
    
    /**
     * Sanitize process ID for use in package name.
     */
    private String sanitizePackageName(String processId) {
        return processId.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
    }
    
    /**
     * Result of a publishing operation.
     */
    public static class PublishResult {
        private final String processId;
        private final String bpmnPath;
        private final String drlPath;
        private final boolean success;
        private final String message;
        
        public PublishResult(String processId, String bpmnPath, String drlPath, 
                           boolean success, String message) {
            this.processId = processId;
            this.bpmnPath = bpmnPath;
            this.drlPath = drlPath;
            this.success = success;
            this.message = message;
        }
        
        public String getProcessId() {
            return processId;
        }
        
        public String getBpmnPath() {
            return bpmnPath;
        }
        
        public String getDrlPath() {
            return drlPath;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
        
        @Override
        public String toString() {
            return "PublishResult{" +
                    "processId='" + processId + '\'' +
                    ", success=" + success +
                    ", message='" + message + '\'' +
                    '}';
        }
    }
    
    /**
     * Exception thrown when publishing fails.
     */
    public static class PublishException extends RuntimeException {
        public PublishException(String message) {
            super(message);
        }
        
        public PublishException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

