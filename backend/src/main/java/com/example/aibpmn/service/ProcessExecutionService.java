package com.example.aibpmn.service;

import com.example.aibpmn.model.ProcessModel;
import com.example.aibpmn.model.ProcessStatus;
import com.example.aibpmn.repository.ProcessModelRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

/**
 * Service for executing published processes.
 * Delegates to Kogito's auto-generated REST endpoints after validating process is published.
 */
@Service
public class ProcessExecutionService {
    
    private static final Logger logger = LoggerFactory.getLogger(ProcessExecutionService.class);
    
    private final ProcessModelRepository processModelRepository;
    private final KogitoDeploymentService kogitoDeployment;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${kogito.service.url:http://localhost:8080}")
    private String kogitoServiceUrl;
    
    public ProcessExecutionService(
            ProcessModelRepository processModelRepository,
            KogitoDeploymentService kogitoDeployment,
            ObjectMapper objectMapper) {
        this.processModelRepository = processModelRepository;
        this.kogitoDeployment = kogitoDeployment;
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }
    
    /**
     * Execute a published process by starting a new process instance.
     * This validates the process is published and deployed, then delegates to Kogito's 
     * auto-generated POST /{processId} endpoint.
     *
     * @param processId The process identifier
     * @param variables Process variables (input data)
     * @return ExecutionResult with instance ID and response data
     * @throws IllegalArgumentException if process not found
     * @throws IllegalStateException if process not published
     * @throws ExecutionException if execution fails
     */
    public ExecutionResult executeProcess(String processId, Map<String, Object> variables) {
        logger.info("Starting process execution for processId: {}", processId);
        
        // 1. Validate process exists
        ProcessModel processModel = processModelRepository.findById(processId)
                .orElseThrow(() -> new IllegalArgumentException("Process not found: " + processId));
        
        // 2. Validate process is published
        if (processModel.getStatus() != ProcessStatus.PUBLISHED) {
            throw new IllegalStateException(
                String.format("Process %s is not published (status: %s). Publish the process first.", 
                    processId, processModel.getStatus())
            );
        }
        
        // 3. Validate process is deployed to Kogito
        if (!kogitoDeployment.isDeployed(processId)) {
            throw new IllegalStateException(
                String.format("Process %s is published but not deployed to Kogito. Re-publish the process.", 
                    processId)
            );
        }
        
        try {
            // 4. Call Kogito's auto-generated endpoint: POST /{processId}
            String kogitoUrl = kogitoServiceUrl + "/" + processId;
            
            logger.info("Calling Kogito endpoint: POST {}", kogitoUrl);
            logger.debug("Process variables: {}", variables);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(variables, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                kogitoUrl,
                HttpMethod.POST,
                request,
                String.class
            );
            
            // 5. Parse response to extract instance ID
            String responseBody = response.getBody();
            String instanceId = extractInstanceId(responseBody);
            
            logger.info("Process instance created successfully. ProcessId: {}, InstanceId: {}", 
                processId, instanceId);
            
            return new ExecutionResult(
                processId,
                instanceId,
                responseBody,
                true,
                "Process instance created successfully"
            );
            
        } catch (HttpClientErrorException e) {
            logger.error("Kogito returned error for processId {}: {} - {}", 
                processId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new ExecutionException(
                String.format("Kogito execution failed: %s", e.getResponseBodyAsString()), 
                e
            );
            
        } catch (Exception e) {
            logger.error("Failed to execute process {}: {}", processId, e.getMessage(), e);
            throw new ExecutionException("Process execution failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get process instance status from Kogito.
     *
     * @param processId The process identifier
     * @param instanceId The process instance identifier
     * @return Instance data from Kogito
     * @throws ExecutionException if retrieval fails
     */
    public String getProcessInstance(String processId, String instanceId) {
        logger.info("Getting process instance. ProcessId: {}, InstanceId: {}", processId, instanceId);
        
        try {
            String kogitoUrl = kogitoServiceUrl + "/" + processId + "/" + instanceId;
            
            ResponseEntity<String> response = restTemplate.getForEntity(kogitoUrl, String.class);
            
            logger.debug("Retrieved process instance: {}", response.getBody());
            
            return response.getBody();
            
        } catch (HttpClientErrorException e) {
            logger.error("Failed to get process instance {}/{}: {} - {}", 
                processId, instanceId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new ExecutionException(
                String.format("Failed to get process instance: %s", e.getResponseBodyAsString()), 
                e
            );
            
        } catch (Exception e) {
            logger.error("Failed to get process instance {}/{}: {}", 
                processId, instanceId, e.getMessage(), e);
            throw new ExecutionException("Failed to get process instance: " + e.getMessage(), e);
        }
    }
    
    /**
     * List all process instances for a process from Kogito.
     *
     * @param processId The process identifier
     * @return List of instances from Kogito
     * @throws ExecutionException if retrieval fails
     */
    public String listProcessInstances(String processId) {
        logger.info("Listing process instances for processId: {}", processId);
        
        try {
            String kogitoUrl = kogitoServiceUrl + "/" + processId;
            
            ResponseEntity<String> response = restTemplate.getForEntity(kogitoUrl, String.class);
            
            logger.debug("Retrieved process instances: {}", response.getBody());
            
            return response.getBody();
            
        } catch (HttpClientErrorException e) {
            logger.error("Failed to list process instances for {}: {} - {}", 
                processId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new ExecutionException(
                String.format("Failed to list process instances: %s", e.getResponseBodyAsString()), 
                e
            );
            
        } catch (Exception e) {
            logger.error("Failed to list process instances for {}: {}", processId, e.getMessage(), e);
            throw new ExecutionException("Failed to list process instances: " + e.getMessage(), e);
        }
    }
    
    /**
     * Extract instance ID from Kogito's response.
     * Kogito typically returns the instance with an "id" field.
     */
    private String extractInstanceId(String responseBody) {
        try {
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            
            // Try to get "id" field
            if (jsonNode.has("id")) {
                return jsonNode.get("id").asText();
            }
            
            // Fallback: generate UUID if Kogito doesn't return ID
            logger.warn("Could not extract instance ID from Kogito response, generating UUID");
            return UUID.randomUUID().toString();
            
        } catch (Exception e) {
            logger.warn("Failed to parse Kogito response, generating UUID: {}", e.getMessage());
            return UUID.randomUUID().toString();
        }
    }
    
    /**
     * Result of a process execution.
     */
    public static class ExecutionResult {
        private final String processId;
        private final String instanceId;
        private final String responseData;
        private final boolean success;
        private final String message;
        
        public ExecutionResult(String processId, String instanceId, String responseData,
                             boolean success, String message) {
            this.processId = processId;
            this.instanceId = instanceId;
            this.responseData = responseData;
            this.success = success;
            this.message = message;
        }
        
        public String getProcessId() {
            return processId;
        }
        
        public String getInstanceId() {
            return instanceId;
        }
        
        public String getResponseData() {
            return responseData;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
        
        @Override
        public String toString() {
            return "ExecutionResult{" +
                    "processId='" + processId + '\'' +
                    ", instanceId='" + instanceId + '\'' +
                    ", success=" + success +
                    ", message='" + message + '\'' +
                    '}';
        }
    }
    
    /**
     * Exception thrown when execution fails.
     */
    public static class ExecutionException extends RuntimeException {
        public ExecutionException(String message) {
            super(message);
        }
        
        public ExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

