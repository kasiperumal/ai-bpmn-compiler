package com.example.aibpmn.service;

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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * ═══════════════════════════════════════════════════════════════════════
 * PROCESS IMAGE SERVICE - Image-to-BPMN Conversion
 * ═══════════════════════════════════════════════════════════════════════
 * 
 * Flow:
 * 1. User uploads process diagram image (hand-drawn, screenshot, etc.)
 * 2. Image is encoded to base64
 * 3. AI Vision (GPT-4o) analyzes the image
 * 4. AI generates BPMN Moddle JSON from the visual process flow
 * 5. Validate and save to database
 * 
 * ═══════════════════════════════════════════════════════════════════════
 */
@Service
public class ProcessImageService {
    
    private static final Logger logger = LoggerFactory.getLogger(ProcessImageService.class);
    
    private final ProcessModelRepository processModelRepository;
    private final AiClient aiClient;
    private final BpmnValidationService bpmnValidationService;
    private final ObjectMapper objectMapper;
    
    private final Path uploadDirectory;
    
    public ProcessImageService(
            ProcessModelRepository processModelRepository,
            AiClient aiClient,
            BpmnValidationService bpmnValidationService,
            ObjectMapper objectMapper) {
        this.processModelRepository = processModelRepository;
        this.aiClient = aiClient;
        this.bpmnValidationService = bpmnValidationService;
        this.objectMapper = objectMapper;
        
        // Create upload directory
        this.uploadDirectory = Paths.get("./data/uploads");
        try {
            Files.createDirectories(uploadDirectory);
        } catch (IOException e) {
            logger.error("Failed to create upload directory", e);
        }
    }
    
    /**
     * Create a process from an image of a process diagram
     * 
     * @param image The uploaded image file
     * @param name Optional process name
     * @return Response with generated processId
     */
    public ProcessTextResponse createProcessFromImage(MultipartFile image, String name) {
        validateImage(image);
        
        String processId = generateProcessId();
        
        logger.info("Creating process from image, processId: {}, filename: {}", 
            processId, image.getOriginalFilename());
        
        try {
            // 1. Save image to disk
            String savedImagePath = saveImage(image, processId);
            
            // 2. Convert image to base64 for AI processing
            byte[] imageBytes = image.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String mimeType = image.getContentType();
            
            // 3. Create AI prompt for image analysis
            String prompt = createImageAnalysisPrompt();
            
            // 4. Call AI Vision to analyze image and generate BPMN
            logger.info("Calling AI Vision to analyze process diagram image");
            String aiResponse = aiClient.generateFromImageAndText(base64Image, mimeType, prompt);
            
            // 5. Parse AI response to extract BPMN Moddle JSON
            ReasoningResult reasoningResult = parseImageAnalysisResponse(aiResponse);
            
            String bpmnModdleJson = reasoningResult.getBpmnModdleJson();
            
            if (bpmnModdleJson == null || bpmnModdleJson.isBlank()) {
                throw new IllegalStateException("AI did not generate valid BPMN Moddle JSON from image");
            }
            
            // 6. Validate BPMN structure
            logger.info("Validating BPMN structure for process: {}", processId);
            ValidationResult validationResult = bpmnValidationService.validate(bpmnModdleJson);
            
            if (!validationResult.isValid()) {
                String errors = String.join(", ", validationResult.getErrors());
                throw new IllegalStateException("BPMN validation failed: " + errors);
            }
            
            // 7. Extract metadata
            BpmnMetadata metadata = extractMetadata(bpmnModdleJson);
            
            // 8. Create and save process model
            ProcessModel processModel = new ProcessModel();
            processModel.setId(processId);
            
            String processName = determineProcessName(name, reasoningResult);
            processModel.setName(processName);
            processModel.setDescription("Generated from image: " + image.getOriginalFilename());
            processModel.setVersion(1);
            processModel.setStatus(ProcessStatus.DRAFT);
            processModel.setBpmnModdleJson(bpmnModdleJson);
            processModel.setMetadata(metadata);
            processModel.setCreatedAt(LocalDateTime.now());
            processModel.setUpdatedAt(LocalDateTime.now());
            
            processModelRepository.save(processModel);
            
            logger.info("Successfully created process from image: {} with name: {}", processId, processName);
            logger.info("Image saved at: {}", savedImagePath);
            
            return new ProcessTextResponse(
                processId,
                processName,
                0 // No text description length for image-based
            );
            
        } catch (Exception e) {
            logger.error("Failed to create process from image: {}", processId, e);
            throw new RuntimeException("Failed to generate process from image: " + e.getMessage(), e);
        }
    }
    
    /**
     * Create AI prompt for analyzing process diagram images
     */
    private String createImageAnalysisPrompt() {
        return """
            ═══════════════════════════════════════════════════════════════════════
            TASK: Analyze Process Diagram Image and Generate BPMN Moddle JSON
            ═══════════════════════════════════════════════════════════════════════
            
            You are analyzing an image of a business process diagram. The image may be:
            - A hand-drawn process flowchart
            - A screenshot of an existing BPMN diagram
            - A whiteboard photo of process steps
            - A flowchart from any notation (will be converted to BPMN)
            
            YOUR TASK:
            1. Identify all visual elements (shapes, text, arrows, connections)
            2. Map them to BPMN 2.0 element types
            3. Generate valid BPMN Moddle JSON (bpmn-js compatible)
            
            ELEMENT MAPPING RULES:
            - Circles/Ovals at START → bpmn:StartEvent
            - Circles/Ovals at END → bpmn:EndEvent
            - Rectangles/Boxes → bpmn:Task or bpmn:UserTask
            - Diamonds/Decision points → bpmn:ExclusiveGateway
            - Parallel splits → bpmn:ParallelGateway
            - Arrows/Lines → bpmn:SequenceFlow
            
            BUSINESS RULE DETECTION:
            - If you see decision boxes with rule text (e.g., "amount > $1000") → bpmn:BusinessRuleTask
            - If text mentions "check rule", "validate", "decision table" → bpmn:BusinessRuleTask
            
            TEXT EXTRACTION:
            - Extract all visible text labels for element names
            - Extract conditions on arrows/branches
            - If text is unclear, use generic names (e.g., "Task 1", "Decision Point")
            
            CRITICAL REQUIREMENTS:
            1. Output MUST be valid BPMN Moddle JSON (used by bpmn-js)
            2. NO position/layout coordinates - frontend handles layout
            3. All elements must have unique IDs
            4. SequenceFlows must reference valid sourceRef/targetRef
            
            OUTPUT FORMAT (JSON only, no explanation):
            {
              "bpmnModdleJson": {
                "$type": "bpmn:Definitions",
                "id": "Definitions_Image_Analysis",
                "targetNamespace": "http://bpmn.io/schema/bpmn",
                "rootElements": [
                  {
                    "$type": "bpmn:Process",
                    "id": "Process_FromImage",
                    "name": "<extracted-process-name>",
                    "isExecutable": true,
                    "flowElements": [
                      ... (start events, tasks, gateways, end events, flows)
                    ]
                  }
                ]
              },
              "metadata": {
                "imageAnalysisNotes": "Brief description of what you identified",
                "businessRuleTasks": [
                  {
                    "taskId": "<task-id>",
                    "taskName": "<task-name>",
                    "ruleDescription": "<rule-logic-from-image>",
                    "suggestedRuleName": "<rule-name>"
                  }
                ],
                "confidenceScore": 0.0-1.0
              }
            }
            
            IMPORTANT NOTES:
            - If the image quality is poor, still extract what you can
            - If the process flow is unclear, make reasonable assumptions
            - Prioritize correctness of BPMN structure over perfect text extraction
            - If you detect any rule-based tasks, flag them in metadata
            
            Now analyze the provided image and generate the BPMN Moddle JSON output.
            """;
    }
    
    /**
     * Parse AI Vision response containing BPMN from image analysis
     */
    private ReasoningResult parseImageAnalysisResponse(String aiResponse) throws Exception {
        // Clean up response (remove markdown if present)
        String cleanJson = aiResponse.trim();
        if (cleanJson.startsWith("```json")) {
            cleanJson = cleanJson.substring(7);
        }
        if (cleanJson.startsWith("```")) {
            cleanJson = cleanJson.substring(3);
        }
        if (cleanJson.endsWith("```")) {
            cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
        }
        cleanJson = cleanJson.trim();
        
        ReasoningResult result = new ReasoningResult();
        JsonNode root = objectMapper.readTree(cleanJson);
        
        // Extract BPMN Moddle JSON
        if (root.has("bpmnModdleJson")) {
            JsonNode bpmnNode = root.get("bpmnModdleJson");
            String bpmnJson = objectMapper.writeValueAsString(bpmnNode);
            result.setBpmnModdleJson(bpmnJson);
            extractProcessName(bpmnNode, result);
        } else {
            throw new IllegalArgumentException("Response does not contain valid BPMN Moddle JSON");
        }
        
        // Extract metadata
        if (root.has("metadata")) {
            JsonNode metadata = root.get("metadata");
            if (metadata.has("imageAnalysisNotes")) {
                result.setOverallExplanation(metadata.get("imageAnalysisNotes").asText());
            }
        }
        
        return result;
    }
    
    /**
     * Extract process name from BPMN
     */
    private void extractProcessName(JsonNode bpmnNode, ReasoningResult result) {
        if (bpmnNode.has("rootElements")) {
            for (JsonNode element : bpmnNode.get("rootElements")) {
                if ("bpmn:Process".equals(element.get("$type").asText()) && element.has("name")) {
                    result.setProcessName(element.get("name").asText());
                    return;
                }
            }
        }
        result.setProcessName("Process from Image");
    }
    
    /**
     * Extract metadata from BPMN Moddle JSON
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
     * Determine process name
     */
    private String determineProcessName(String explicitName, ReasoningResult reasoningResult) {
        if (explicitName != null && !explicitName.isBlank()) {
            return explicitName;
        }
        
        if (reasoningResult.getProcessName() != null && !reasoningResult.getProcessName().isBlank()) {
            return reasoningResult.getProcessName();
        }
        
        return "Process from Image";
    }
    
    /**
     * Save uploaded image to disk
     */
    private String saveImage(MultipartFile image, String processId) throws IOException {
        String originalFilename = image.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".") 
            ? originalFilename.substring(originalFilename.lastIndexOf("."))
            : ".png";
        
        String filename = processId + extension;
        Path filePath = uploadDirectory.resolve(filename);
        
        Files.write(filePath, image.getBytes());
        
        return filePath.toString();
    }
    
    /**
     * Validate uploaded image
     */
    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Image file cannot be null or empty");
        }
        
        // Check file size (max 10MB)
        if (image.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("Image file too large. Maximum 10MB allowed");
        }
        
        // Check content type
        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File must be an image (JPEG, PNG, etc.)");
        }
    }
    
    /**
     * Generate unique process ID
     */
    private String generateProcessId() {
        return "proc-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
