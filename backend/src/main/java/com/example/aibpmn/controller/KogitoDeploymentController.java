package com.example.aibpmn.controller;

import com.example.aibpmn.service.KogitoDeploymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for managing Kog ito process deployments.
 * Provides endpoints for deploying, undeploying, and querying process deployments.
 */
@RestController
@RequestMapping("/api/kogito/deployments")
public class KogitoDeploymentController {
    
    private static final Logger logger = LoggerFactory.getLogger(KogitoDeploymentController.class);
    
    private final KogitoDeploymentService deploymentService;
    
    public KogitoDeploymentController(KogitoDeploymentService deploymentService) {
        this.deploymentService = deploymentService;
    }
    
    /**
     * Deploy BPMN for a process.
     * POST /api/kogito/deployments/{processId}/bpmn
     */
    @PostMapping("/{processId}/bpmn")
    public ResponseEntity<Map<String, Object>> deployBpmn(
            @PathVariable String processId,
            @RequestBody String bpmnXml) {
        
        logger.info("Received request to deploy BPMN for processId: {}", processId);
        
        try {
            var path = deploymentService.deployBpmn(processId, bpmnXml);
            
            Map<String, Object> response = new HashMap<>();
            response.put("processId", processId);
            response.put("type", "BPMN");
            response.put("path", path.toString());
            response.put("status", "deployed");
            
            logger.info("BPMN deployed successfully for processId: {}", processId);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid BPMN for processId {}: {}", processId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IOException e) {
            logger.error("Failed to deploy BPMN for processId {}: {}", processId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Failed to deploy BPMN: " + e.getMessage());
        }
    }
    
    /**
     * Deploy DRL for a process.
     * POST /api/kogito/deployments/{processId}/drl
     */
    @PostMapping("/{processId}/drl")
    public ResponseEntity<Map<String, Object>> deployDrl(
            @PathVariable String processId,
            @RequestBody String drlContent) {
        
        logger.info("Received request to deploy DRL for processId: {}", processId);
        
        try {
            var path = deploymentService.deployDrl(processId, drlContent);
            
            Map<String, Object> response = new HashMap<>();
            response.put("processId", processId);
            response.put("type", "DRL");
            response.put("path", path.toString());
            response.put("status", "deployed");
            
            logger.info("DRL deployed successfully for processId: {}", processId);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid DRL for processId {}: {}", processId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IOException e) {
            logger.error("Failed to deploy DRL for processId {}: {}", processId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Failed to deploy DRL: " + e.getMessage());
        }
    }
    
    /**
     * Deploy complete process (both BPMN and DRL).
     * POST /api/kogito/deployments/{processId}
     */
    @PostMapping("/{processId}")
    public ResponseEntity<Map<String, Object>> deployProcess(
            @PathVariable String processId,
            @RequestBody DeploymentRequest request) {
        
        logger.info("Received request to deploy complete process for processId: {}", processId);
        
        try {
            var result = deploymentService.deployProcess(
                processId, 
                request.getBpmnXml(), 
                request.getDrlContent()
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("processId", processId);
            response.put("bpmnPath", result.getBpmnPath().toString());
            response.put("drlPath", result.getDrlPath().toString());
            response.put("status", "deployed");
            
            logger.info("Complete process deployed successfully for processId: {}", processId);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid deployment request for processId {}: {}", processId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IOException e) {
            logger.error("Failed to deploy process {}: {}", processId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Failed to deploy process: " + e.getMessage());
        }
    }
    
    /**
     * Undeploy a process.
     * DELETE /api/kogito/deployments/{processId}
     */
    @DeleteMapping("/{processId}")
    public ResponseEntity<Map<String, Object>> undeployProcess(@PathVariable String processId) {
        logger.info("Received request to undeploy processId: {}", processId);
        
        try {
            boolean undeployed = deploymentService.undeployProcess(processId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("processId", processId);
            response.put("status", undeployed ? "undeployed" : "not found");
            
            if (undeployed) {
                logger.info("Process undeployed successfully: {}", processId);
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Process not found for undeployment: {}", processId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
        } catch (IOException e) {
            logger.error("Failed to undeploy process {}: {}", processId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Failed to undeploy process: " + e.getMessage());
        }
    }
    
    /**
     * Check if a process is deployed.
     * GET /api/kogito/deployments/{processId}/status
     */
    @GetMapping("/{processId}/status")
    public ResponseEntity<Map<String, Object>> getDeploymentStatus(@PathVariable String processId) {
        logger.debug("Checking deployment status for processId: {}", processId);
        
        boolean deployed = deploymentService.isDeployed(processId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("processId", processId);
        response.put("deployed", deployed);
        
        if (deployed) {
            var info = deploymentService.getDeploymentInfo(processId);
            if (info != null) {
                response.put("bpmnSize", info.getBpmnSize());
                response.put("bpmnLastModified", info.getBpmnLastModified());
                response.put("hasDrl", info.hasDrl());
                if (info.hasDrl()) {
                    response.put("drlSize", info.getDrlSize());
                    response.put("drlLastModified", info.getDrlLastModified());
                }
            }
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get deployment information for a process.
     * GET /api/kogito/deployments/{processId}
     */
    @GetMapping("/{processId}")
    public ResponseEntity<Map<String, Object>> getDeploymentInfo(@PathVariable String processId) {
        logger.debug("Getting deployment info for processId: {}", processId);
        
        var info = deploymentService.getDeploymentInfo(processId);
        
        if (info == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
                "Process not deployed: " + processId);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("processId", info.getProcessId());
        response.put("bpmnPath", info.getBpmnPath().toString());
        response.put("bpmnSize", info.getBpmnSize());
        response.put("bpmnLastModified", info.getBpmnLastModified());
        
        if (info.hasDrl()) {
            response.put("drlPath", info.getDrlPath().toString());
            response.put("drlSize", info.getDrlSize());
            response.put("drlLastModified", info.getDrlLastModified());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * List all deployed processes.
     * GET /api/kogito/deployments
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listDeployments() {
        logger.debug("Listing all deployments");
        
        try {
            List<String> processIds = deploymentService.listDeployedProcesses();
            
            Map<String, Object> response = new HashMap<>();
            response.put("count", processIds.size());
            response.put("processIds", processIds);
            
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            logger.error("Failed to list deployments: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Failed to list deployments: " + e.getMessage());
        }
    }
    
    /**
     * Request DTO for deploying a complete process.
     */
    public static class DeploymentRequest {
        private String bpmnXml;
        private String drlContent;
        
        public String getBpmnXml() {
            return bpmnXml;
        }
        
        public void setBpmnXml(String bpmnXml) {
            this.bpmnXml = bpmnXml;
        }
        
        public String getDrlContent() {
            return drlContent;
        }
        
        public void setDrlContent(String drlContent) {
            this.drlContent = drlContent;
        }
    }
}

