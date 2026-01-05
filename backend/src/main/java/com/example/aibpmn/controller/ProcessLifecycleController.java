package com.example.aibpmn.controller;

import com.example.aibpmn.dto.EditIntentRequest;
import com.example.aibpmn.dto.EditIntentResponse;
import com.example.aibpmn.model.Explanation;
import com.example.aibpmn.service.ProcessEditService;
import com.example.aibpmn.service.ProcessExecutionService;
import com.example.aibpmn.service.ProcessPublishingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for process lifecycle operations: publish and execute.
 */
@RestController
@RequestMapping("/api/process")
public class ProcessLifecycleController {
    
    private static final Logger logger = LoggerFactory.getLogger(ProcessLifecycleController.class);
    
    private final ProcessPublishingService publishingService;
    private final ProcessExecutionService executionService;
    private final ProcessEditService processEditService;
    private final ObjectMapper objectMapper;
    
    public ProcessLifecycleController(
            ProcessPublishingService publishingService,
            ProcessExecutionService executionService,
            ProcessEditService processEditService,
            ObjectMapper objectMapper) {
        this.publishingService = publishingService;
        this.executionService = executionService;
        this.processEditService = processEditService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Publish a process to Kogito runtime.
     * POST /api/process/{processId}/publish
     * 
     * This endpoint:
     * 1. Generates BPMN from process model
     * 2. Validates BPMN structure
     * 3. Generates DRL from rules
     * 4. Deploys to Kogito
     * 5. Marks process as PUBLISHED
     * 6. Returns deployment details and execution endpoint
     */
    @PostMapping("/{processId}/publish")
    public ResponseEntity<Map<String, Object>> publishProcess(@PathVariable String processId) {
        logger.info("Received request to publish processId: {}", processId);
        
        try {
            ProcessPublishingService.PublishResult result = publishingService.publishProcess(processId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("processId", result.getProcessId());
            response.put("status", "PUBLISHED");
            response.put("bpmnPath", result.getBpmnPath());
            response.put("drlPath", result.getDrlPath());
            response.put("message", result.getMessage());
            response.put("executeEndpoint", "/api/process/" + processId + "/execute");
            response.put("kogitoEndpoint", "/" + processId);  // Direct Kogito endpoint
            
            logger.info("Process published successfully: {}", processId);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.error("Process not found: {}", processId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
            
        } catch (IllegalStateException e) {
            logger.error("Process cannot be published: {} - {}", processId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
            
        } catch (ProcessPublishingService.PublishException e) {
            logger.error("Publishing failed for {}: {}", processId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Publishing failed: " + e.getMessage());
        }
    }
    
    /**
     * Execute a published process by starting a new process instance.
     * POST /api/process/{processId}/execute
     * 
     * This endpoint:
     * 1. Validates process is PUBLISHED
     * 2. Validates process is deployed to Kogito
     * 3. Delegates to Kogito's auto-generated POST /{processId} endpoint
     * 4. Returns instance ID and response data
     * 
     * Request Body: Process variables (key-value pairs)
     * Example:
     * {
     *   "orderAmount": 5000,
     *   "customerId": "CUST-001",
     *   "priority": "HIGH"
     * }
     */
    @PostMapping("/{processId}/execute")
    public ResponseEntity<Map<String, Object>> executeProcess(
            @PathVariable String processId,
            @RequestBody(required = false) Map<String, Object> variables) {
        
        logger.info("Received request to execute processId: {}", processId);
        logger.debug("Process variables: {}", variables);
        
        // Default to empty map if no variables provided
        if (variables == null) {
            variables = new HashMap<>();
        }
        
        try {
            ProcessExecutionService.ExecutionResult result = 
                executionService.executeProcess(processId, variables);
            
            Map<String, Object> response = new HashMap<>();
            response.put("processId", result.getProcessId());
            response.put("instanceId", result.getInstanceId());
            response.put("status", "STARTED");
            response.put("message", result.getMessage());
            response.put("queryEndpoint", "/api/process/" + processId + "/instance/" + result.getInstanceId());
            response.put("kogitoEndpoint", "/" + processId + "/" + result.getInstanceId());
            
            // Include full Kogito response data
            try {
                response.put("instanceData", objectMapper.readValue(
                    result.getResponseData(), 
                    Map.class
                ));
            } catch (Exception e) {
                response.put("instanceData", result.getResponseData());
            }
            
            logger.info("Process instance created successfully. ProcessId: {}, InstanceId: {}", 
                processId, result.getInstanceId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            logger.error("Process not found: {}", processId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
            
        } catch (IllegalStateException e) {
            logger.error("Process cannot be executed: {} - {}", processId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
            
        } catch (ProcessExecutionService.ExecutionException e) {
            logger.error("Execution failed for {}: {}", processId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Execution failed: " + e.getMessage());
        }
    }
    
    /**
     * Get process instance status.
     * GET /api/process/{processId}/instance/{instanceId}
     */
    @GetMapping("/{processId}/instance/{instanceId}")
    public ResponseEntity<Map<String, Object>> getProcessInstance(
            @PathVariable String processId,
            @PathVariable String instanceId) {
        
        logger.debug("Getting process instance. ProcessId: {}, InstanceId: {}", processId, instanceId);
        
        try {
            String instanceData = executionService.getProcessInstance(processId, instanceId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("processId", processId);
            response.put("instanceId", instanceId);
            
            // Parse and include instance data
            try {
                response.put("data", objectMapper.readValue(
                    instanceData, 
                    Map.class
                ));
            } catch (Exception e) {
                response.put("data", instanceData);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (ProcessExecutionService.ExecutionException e) {
            logger.error("Failed to get instance {}/{}: {}", processId, instanceId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Failed to get process instance: " + e.getMessage());
        }
    }
    
    /**
     * List all process instances for a process.
     * GET /api/process/{processId}/instances
     */
    @GetMapping("/{processId}/instances")
    public ResponseEntity<Map<String, Object>> listProcessInstances(@PathVariable String processId) {
        logger.debug("Listing process instances for processId: {}", processId);
        
        try {
            String instancesData = executionService.listProcessInstances(processId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("processId", processId);
            
            // Parse and include instances data
            try {
                response.put("instances", objectMapper.readValue(
                    instancesData, 
                    Object.class
                ));
            } catch (Exception e) {
                response.put("instances", instancesData);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (ProcessExecutionService.ExecutionException e) {
            logger.error("Failed to list instances for {}: {}", processId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Failed to list process instances: " + e.getMessage());
        }
    }
    
    /**
     * Check if a process is published and ready for execution.
     * GET /api/process/{processId}/status
     */
    @GetMapping("/{processId}/status")
    public ResponseEntity<Map<String, Object>> getProcessStatus(@PathVariable String processId) {
        logger.debug("Checking process status for processId: {}", processId);
        
        boolean isPublished = publishingService.isPublished(processId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("processId", processId);
        response.put("published", isPublished);
        response.put("canExecute", isPublished);
        
        if (isPublished) {
            response.put("executeEndpoint", "/api/process/" + processId + "/execute");
            response.put("kogitoEndpoint", "/" + processId);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Process an edit intent for a process.
     * POST /api/process/{processId}/edit-intent
     * 
     * This endpoint accepts natural language instructions to edit a process,
     * interprets them using AI, applies the changes to the canonical model,
     * and regenerates the BPMN.
     * 
     * Request Body:
     * {
     *   "instruction": "Rename this task to 'Review Application'",
     *   "nodeId": "task_1" (optional)
     * }
     * 
     * Supported edits:
     * - Rename nodes
     * - Update conditions
     * - Update descriptions
     */
    @PostMapping("/{processId}/edit-intent")
    public ResponseEntity<EditIntentResponse> processEditIntent(
            @PathVariable String processId,
            @RequestBody EditIntentRequest request) {
        
        logger.info("Received edit intent for processId: {} - {}", processId, request.getInstruction());
        
        try {
            EditIntentResponse response = processEditService.processEditIntent(processId, request);
            
            if (response.isSuccess()) {
                logger.info("Edit intent processed successfully for processId: {}", processId);
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Edit intent failed for processId: {} - {}", processId, response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid edit intent request for processId: {}", processId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
            
        } catch (Exception e) {
            logger.error("Error processing edit intent for {}: {}", processId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Failed to process edit intent: " + e.getMessage());
        }
    }
    
    /**
     * Get explanations for process nodes.
     * GET /api/process/{processId}/explanations
     * 
     * Returns AI-generated explanations for why nodes exist in the process.
     */
    @GetMapping("/{processId}/explanations")
    public ResponseEntity<Map<String, Object>> getExplanations(@PathVariable String processId) {
        logger.debug("Getting explanations for processId: {}", processId);
        
        try {
            List<Explanation> explanations = processEditService.getExplanations(processId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("processId", processId);
            response.put("explanations", explanations);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.error("Process not found: {}", processId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
            
        } catch (Exception e) {
            logger.error("Error getting explanations for {}: {}", processId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Failed to get explanations: " + e.getMessage());
        }
    }
}

