package com.example.aibpmn.service;

import com.example.aibpmn.dto.ProcessTextRequest;
import com.example.aibpmn.dto.ProcessTextResponse;
import com.example.aibpmn.dto.ReasoningResult;
import com.example.aibpmn.model.BpmnMetadata;
import com.example.aibpmn.model.ProcessModel;
import com.example.aibpmn.model.ProcessStatus;
import com.example.aibpmn.repository.ProcessModelRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * ═══════════════════════════════════════════════════════════════════════
 * PROCESS TEXT SERVICE - BPMN Moddle JSON Architecture
 * ═══════════════════════════════════════════════════════════════════════
 * 
 * Flow:
 * 1. AI generates BPMN Moddle JSON
 * 2. Validate BPMN structure
 * 3. Extract metadata for DB queries
 * 4. Save to database (H2)
 * 5. Frontend applies ELK layout
 * 
 * ═══════════════════════════════════════════════════════════════════════
 */
@Service
public class ProcessTextService {
    
    private static final Logger logger = LoggerFactory.getLogger(ProcessTextService.class);
    
    private final ProcessModelRepository processModelRepository;
    private final ProcessReasonerService processReasonerService;
    private final BpmnValidationService bpmnValidationService;
    private final DrlGeneratorService drlGeneratorService;
    private final ObjectMapper objectMapper;
    
    public ProcessTextService(
            ProcessModelRepository processModelRepository,
            ProcessReasonerService processReasonerService,
            BpmnValidationService bpmnValidationService,
            DrlGeneratorService drlGeneratorService,
            ObjectMapper objectMapper) {
        this.processModelRepository = processModelRepository;
        this.processReasonerService = processReasonerService;
        this.bpmnValidationService = bpmnValidationService;
        this.drlGeneratorService = drlGeneratorService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Create a process from text description using AI reasoning.
     * 
     * NEW FLOW (BPMN Moddle JSON Architecture):
     * 1. Validate input
     * 2. Generate process ID
     * 3. Call AI to generate BPMN Moddle JSON
     * 4. Validate BPMN structure
     * 5. Extract metadata
     * 6. Save to database
     * 
     * @param request The text request containing description
     * @return Response with generated processId
     */
    public ProcessTextResponse createProcessFromText(ProcessTextRequest request) {
        // Validate input
        validateRequest(request);
        
        // Generate unique process ID
        String processId = generateProcessId();
        
        logger.info("Creating process from text, processId: {}", processId);
        
        try {
            // 1. Use AI to generate BPMN Moddle JSON
            logger.info("Calling AI to generate BPMN Moddle JSON for process: {}", processId);
            ReasoningResult reasoningResult = processReasonerService.reasonOverDescription(
                request.getDescription()
            );
            
            String bpmnModdleJson = reasoningResult.getBpmnModdleJson();
            
            if (bpmnModdleJson == null || bpmnModdleJson.isBlank()) {
                throw new IllegalStateException("AI did not generate valid BPMN Moddle JSON");
            }
            
            // 2. Validate BPMN structure
            logger.info("Validating BPMN structure for process: {}", processId);
            ValidationResult validationResult = bpmnValidationService.validate(bpmnModdleJson);
            
            if (!validationResult.isValid()) {
                String errors = String.join(", ", validationResult.getErrors());
                throw new IllegalStateException("BPMN validation failed: " + errors);
            }
            
            if (!validationResult.getWarnings().isEmpty()) {
                logger.warn("BPMN validation warnings for {}: {}", 
                    processId, String.join(", ", validationResult.getWarnings()));
            }
            
            // 3. Extract metadata from BPMN
            BpmnMetadata metadata = extractMetadata(bpmnModdleJson);
            
            // 4. Create and save process model
            ProcessModel processModel = new ProcessModel();
            processModel.setId(processId);
            
            // Set name from request, AI reasoning, or generated name
            String processName = determineProcessName(request, reasoningResult);
            processModel.setName(processName);
            processModel.setDescription(request.getDescription());
            processModel.setVersion(1);
            processModel.setStatus(ProcessStatus.DRAFT);
            processModel.setBpmnModdleJson(bpmnModdleJson);
            processModel.setMetadata(metadata);
            processModel.setCreatedAt(LocalDateTime.now());
            processModel.setUpdatedAt(LocalDateTime.now());
            
            // Add rules from reasoning result
            if (reasoningResult.getRules() != null && !reasoningResult.getRules().isEmpty()) {
                processModel.setRules(reasoningResult.getRules());
                logger.info("✅ Added {} rules to process model", reasoningResult.getRules().size());
            } else {
                logger.warn("⚠️  No rules found in reasoning result! DRL file will NOT be generated.");
                logger.warn("ReasoningResult has drlFileName: {}", reasoningResult.getDrlFileName());
            }
            
            // Save to database (H2)
            processModelRepository.save(processModel);
            
            // 5. Generate DRL file immediately (for preview in properties panel)
            if (!processModel.getRules().isEmpty()) {
                try {
                    // Use DRL filename from AI reasoning result (e.g., "LeaveValidationRules.drl")
                    String drlFileName = reasoningResult.getDrlFileName();
                    if (drlFileName == null || drlFileName.isBlank()) {
                        drlFileName = sanitizePackageName(processName) + ".drl";
                        logger.warn("No DRL filename from AI, using generated: {}", drlFileName);
                    }
                    
                    // Extract base name without .drl extension for package name
                    String baseName = drlFileName.endsWith(".drl") ? 
                            drlFileName.substring(0, drlFileName.length() - 4) : 
                            drlFileName;
                    
                    String drlContent = drlGeneratorService.generateDrl(
                        processModel.getRules(),
                        "com.example.aibpmn.rules." + sanitizePackageName(baseName),
                        false // Don't validate for now
                    );
                    
                    // Save DRL file to disk with the correct filename
                    saveDrlFile(drlFileName, drlContent);
                    logger.info("Generated DRL file: {} ({} rules)", drlFileName, processModel.getRules().size());
                } catch (Exception e) {
                    logger.warn("Failed to generate DRL file for process {}: {}", processId, e.getMessage());
                    // Don't fail process creation if DRL generation fails
                }
            }
            
            logger.info("Successfully created process from text: {} with name: {}", processId, processName);
            logger.info("BPMN elements: {} total, {} business rule tasks",
                metadata.getElementCount(), metadata.getBusinessRuleTaskCount());
            
            return new ProcessTextResponse(
                processId,
                processName,
                request.getDescription().length()
            );
            
        } catch (Exception e) {
            logger.error("Failed to create process from text: {}", processId, e);
            throw new RuntimeException("Failed to generate process from description: " + e.getMessage(), e);
        }
    }
    
    /**
     * Extract metadata from BPMN Moddle JSON for efficient querying
     */
    private BpmnMetadata extractMetadata(String bpmnModdleJson) {
        try {
            JsonNode bpmn = objectMapper.readTree(bpmnModdleJson);
            
            String processName = "Untitled Process";
            int elementCount = 0;
            int taskCount = 0;
            int gatewayCount = 0;
            int businessRuleTaskCount = 0;
            List<String> businessRuleTaskIds = new ArrayList<>();
            
            // Navigate to process elements
            if (bpmn.has("rootElements")) {
                for (JsonNode element : bpmn.get("rootElements")) {
                    if ("bpmn:Process".equals(element.get("$type").asText())) {
                        if (element.has("name")) {
                            processName = element.get("name").asText();
                        }
                        
                        if (element.has("flowElements")) {
                            JsonNode flowElements = element.get("flowElements");
                            elementCount = flowElements.size();
                            
                            for (JsonNode flowElement : flowElements) {
                                String type = flowElement.get("$type").asText();
                                
                                if (type.contains("Task")) {
                                    taskCount++;
                                    if ("bpmn:BusinessRuleTask".equals(type)) {
                                        businessRuleTaskCount++;
                                        businessRuleTaskIds.add(flowElement.get("id").asText());
                                    }
                                } else if (type.contains("Gateway")) {
                                    gatewayCount++;
                                }
                            }
                        }
                    }
                }
            }
            
            return new BpmnMetadata(
                processName,
                elementCount,
                taskCount,
                gatewayCount,
                businessRuleTaskCount,
                businessRuleTaskIds
            );
            
        } catch (Exception e) {
            logger.error("Failed to extract metadata from BPMN", e);
            return new BpmnMetadata("Untitled Process", 0, 0, 0, 0, new ArrayList<>());
        }
    }
    
    /**
     * Validate the request
     */
    private void validateRequest(ProcessTextRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            throw new IllegalArgumentException("Process description cannot be empty");
        }
        
        // Optional: Add maximum length validation
        if (request.getDescription().length() > 10000) {
            throw new IllegalArgumentException("Description too long. Maximum 10,000 characters allowed");
        }
    }
    
    /**
     * Determine the process name from request, AI reasoning, or description
     */
    private String determineProcessName(ProcessTextRequest request, ReasoningResult reasoningResult) {
        // Priority 1: Explicit name in request
        if (request.getName() != null && !request.getName().isBlank()) {
            return request.getName();
        }
        
        // Priority 2: Name extracted from AI-generated BPMN
        if (reasoningResult.getProcessName() != null && !reasoningResult.getProcessName().isBlank()) {
            return reasoningResult.getProcessName();
        }
        
        // Priority 3: Generate from description (first 50 chars)
        String description = request.getDescription();
        if (description.length() <= 50) {
            return description;
        }
        
        return description.substring(0, 47) + "...";
    }
    
    /**
     * Generate a unique process ID
     */
    private String generateProcessId() {
        return "proc-" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * Sanitize process ID for use in package name
     */
    private String sanitizePackageName(String processId) {
        return processId.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
    }
    
    /**
     * Save DRL file to disk
     */
    private void saveDrlFile(String drlFileName, String drlContent) {
        try {
            java.nio.file.Path drlDir = java.nio.file.Paths.get("data/kogito/rules");
            java.nio.file.Files.createDirectories(drlDir);
            
            java.nio.file.Path drlFile = drlDir.resolve(drlFileName);
            
            java.nio.file.Files.writeString(drlFile, drlContent);
            logger.info("Saved DRL file: {}", drlFile.toAbsolutePath());
        } catch (Exception e) {
            logger.error("Failed to save DRL file {}: {}", drlFileName, e.getMessage(), e);
            throw new RuntimeException("Failed to save DRL file: " + e.getMessage(), e);
        }
    }
}

