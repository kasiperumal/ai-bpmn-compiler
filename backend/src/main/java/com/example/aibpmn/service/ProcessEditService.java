package com.example.aibpmn.service;

import com.example.aibpmn.dto.EditIntentRequest;
import com.example.aibpmn.dto.EditIntentResponse;
import com.example.aibpmn.model.Explanation;
import com.example.aibpmn.model.ProcessEdge;
import com.example.aibpmn.model.ProcessModel;
import com.example.aibpmn.model.ProcessNode;
import com.example.aibpmn.repository.ProcessModelRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for handling process edit intents.
 * Interprets natural language instructions and applies them to the process model.
 * 
 * Uses configured AI provider (OpenAI GPT-4o or Google Gemini 2.0).
 */
@Service
public class ProcessEditService {

    private static final Logger logger = LoggerFactory.getLogger(ProcessEditService.class);

    private final ProcessModelRepository processModelRepository;
    private final AiClient aiClient;
    private final BpmnGeneratorService bpmnGeneratorService;
    private final ObjectMapper objectMapper;

    public ProcessEditService(ProcessModelRepository processModelRepository,
                              AiClient aiClient,
                              BpmnGeneratorService bpmnGeneratorService,
                              ObjectMapper objectMapper) {
        this.processModelRepository = processModelRepository;
        this.aiClient = aiClient;
        this.bpmnGeneratorService = bpmnGeneratorService;
        this.objectMapper = objectMapper;
        logger.info("ProcessEditService initialized with AI provider: {}", aiClient.getProviderName());
    }

    /**
     * Processes an edit intent and applies it to the process model.
     *
     * @param processId The ID of the process to edit.
     * @param request The edit intent request containing the instruction and optional node ID.
     * @return EditIntentResponse indicating success or failure.
     */
    public EditIntentResponse processEditIntent(String processId, EditIntentRequest request) {
        logger.info("Processing edit intent for process {}: {}", processId, request.getInstruction());

        // Validate input
        if (!StringUtils.hasText(request.getInstruction())) {
            return new EditIntentResponse(false, "Edit instruction cannot be empty.");
        }

        // Fetch the process model
        Optional<ProcessModel> processModelOpt = processModelRepository.findById(processId);
        if (processModelOpt.isEmpty()) {
            return new EditIntentResponse(false, "Process not found with ID: " + processId);
        }

        ProcessModel processModel = processModelOpt.get();

        try {
            // Use AI to interpret the instruction and generate edit commands
            String editCommands = interpretEditIntent(processModel, request);

            // Apply the edit commands to the process model
            boolean modified = applyEditCommands(processModel, editCommands, request.getNodeId());

            if (modified) {
                // Save the updated model
                processModelRepository.save(processModel);

                // Regenerate BPMN
                try {
                    String bpmn = bpmnGeneratorService.generateBpmn(processModel);
                    logger.info("BPMN regenerated for process {}", processId);

                    EditIntentResponse response = new EditIntentResponse(true, "Edit applied successfully.");
                    response.setModifiedNodeId(request.getNodeId());
                    response.setBpmnRegenerated(true);
                    return response;
                } catch (Exception e) {
                    logger.error("Failed to regenerate BPMN after edit: {}", e.getMessage(), e);
                    return new EditIntentResponse(false, "Edit applied but BPMN regeneration failed: " + e.getMessage());
                }
            } else {
                return new EditIntentResponse(false, "No changes were made based on the instruction.");
            }

        } catch (Exception e) {
            logger.error("Error processing edit intent: {}", e.getMessage(), e);
            return new EditIntentResponse(false, "Failed to process edit: " + e.getMessage());
        }
    }

    /**
     * Uses AI to interpret the natural language instruction and generate structured edit commands.
     */
    private String interpretEditIntent(ProcessModel processModel, EditIntentRequest request) {
        String processJson;
        try {
            processJson = objectMapper.writeValueAsString(processModel);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize process model", e);
            processJson = "{}";
        }

        String prompt = buildEditIntentPrompt(processJson, request);

        logger.debug("Sending edit intent prompt to AI provider: {}", aiClient.getProviderName());
        String response = aiClient.generateFromText(prompt);

        logger.debug("AI response for edit intent: {}", response);
        return response;
    }

    /**
     * Builds the prompt for interpreting the edit intent.
     */
    private String buildEditIntentPrompt(String processJson, EditIntentRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a BPMN process editor. Your task is to interpret natural language edit instructions ");
        prompt.append("and generate structured edit commands in JSON format.\n\n");

        prompt.append("Current Process Model (JSON):\n");
        prompt.append(processJson);
        prompt.append("\n\n");

        if (StringUtils.hasText(request.getNodeId())) {
            prompt.append("Target Node ID: ").append(request.getNodeId()).append("\n");
        }

        prompt.append("Edit Instruction: ").append(request.getInstruction()).append("\n\n");

        prompt.append("Generate a JSON object with the following structure:\n");
        prompt.append("{\n");
        prompt.append("  \"action\": \"rename\" | \"update_condition\" | \"update_description\" | \"add_node\" | \"delete_node\",\n");
        prompt.append("  \"nodeId\": \"<node-id-to-modify>\",\n");
        prompt.append("  \"newValue\": \"<new-value>\",\n");
        prompt.append("  \"field\": \"name\" | \"condition\" | \"description\"\n");
        prompt.append("}\n\n");

        prompt.append("Rules:\n");
        prompt.append("1. Only support safe edits: rename, condition updates, description updates\n");
        prompt.append("2. Do NOT change node types or process structure without explicit instruction\n");
        prompt.append("3. If the instruction is ambiguous, choose the most conservative interpretation\n");
        prompt.append("4. Return ONLY the JSON object, no additional text\n\n");

        prompt.append("JSON Edit Command:");

        return prompt.toString();
    }

    /**
     * Applies the edit commands to the process model.
     *
     * @return true if changes were made, false otherwise.
     */
    private boolean applyEditCommands(ProcessModel processModel, String editCommandsJson, String targetNodeId) {
        try {
            // Parse the AI response as a JSON edit command
            EditCommand command = objectMapper.readValue(editCommandsJson, EditCommand.class);

            String nodeId = StringUtils.hasText(command.nodeId) ? command.nodeId : targetNodeId;

            if (!StringUtils.hasText(nodeId)) {
                logger.warn("No node ID specified for edit command");
                return false;
            }

            // Find the node to edit
            Optional<ProcessNode> nodeOpt = processModel.getNodes().stream()
                    .filter(n -> nodeId.equals(n.getId()))
                    .findFirst();

            if (nodeOpt.isEmpty()) {
                logger.warn("Node not found with ID: {}", nodeId);
                return false;
            }

            ProcessNode node = nodeOpt.get();

            // Apply the edit based on action and field
            switch (command.action) {
                case "rename":
                    node.setName(command.newValue);
                    logger.info("Renamed node {} to {}", nodeId, command.newValue);
                    return true;

                case "update_condition":
                    if ("condition".equals(command.field)) {
                        // Update condition on outgoing edges (for gateways)
                        boolean conditionUpdated = false;
                        for (ProcessEdge edge : processModel.getEdges()) {
                            if (nodeId.equals(edge.getFromNodeId())) {
                                edge.setCondition(command.newValue);
                                conditionUpdated = true;
                                logger.info("Updated condition on edge from {} to {}", edge.getFromNodeId(), edge.getToNodeId());
                            }
                        }
                        return conditionUpdated;
                    }
                    return false;

                case "update_description":
                    if ("description".equals(command.field)) {
                        node.addProperty("description", command.newValue);
                        logger.info("Updated description for node {}", nodeId);
                        return true;
                    }
                    return false;

                default:
                    logger.warn("Unsupported edit action: {}", command.action);
                    return false;
            }

        } catch (JsonProcessingException e) {
            logger.error("Failed to parse edit commands JSON: {}", e.getMessage());
            logger.debug("Edit commands JSON: {}", editCommandsJson);
            return false;
        }
    }

    /**
     * Internal class for parsing edit commands from AI response.
     */
    private static class EditCommand {
        public String action;
        public String nodeId;
        public String newValue;
        public String field;
    }
    
    /**
     * Gets explanations for all nodes in a process.
     * Generates AI explanations for why each node exists in the process.
     *
     * @param processId The ID of the process.
     * @return List of Explanation objects.
     */
    public List<Explanation> getExplanations(String processId) {
        logger.info("Getting explanations for process {}", processId);

        // Fetch the process model
        Optional<ProcessModel> processModelOpt = processModelRepository.findById(processId);
        if (processModelOpt.isEmpty()) {
            throw new IllegalArgumentException("Process not found with ID: " + processId);
        }

        ProcessModel processModel = processModelOpt.get();
        List<Explanation> explanations = new ArrayList<>();

        // Generate explanations for each node using AI
        for (ProcessNode node : processModel.getNodes()) {
            try {
                String explanation = generateNodeExplanation(processModel, node);
                
                Explanation exp = new Explanation();
                exp.setNodeId(node.getId());
                exp.setReason(explanation);
                exp.setSource("AI Generated");
                exp.setConfidenceScore(0.85); // Default confidence
                
                explanations.add(exp);
            } catch (Exception e) {
                logger.warn("Failed to generate explanation for node {}: {}", node.getId(), e.getMessage());
                // Add a fallback explanation
                Explanation exp = new Explanation();
                exp.setNodeId(node.getId());
                exp.setReason("This is a " + node.getType() + " node named '" + node.getName() + "'");
                exp.setSource("System");
                exp.setConfidenceScore(0.5);
                explanations.add(exp);
            }
        }

        return explanations;
    }

    /**
     * Generates an AI explanation for why a specific node exists in the process.
     */
    private String generateNodeExplanation(ProcessModel processModel, ProcessNode node) {
        String processJson;
        try {
            processJson = objectMapper.writeValueAsString(processModel);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize process model", e);
            processJson = "{}";
        }

        String prompt = buildNodeExplanationPrompt(processJson, node);

        logger.debug("Generating explanation for node {} using {}...", node.getId(), aiClient.getProviderName());
        String response = aiClient.generateFromText(prompt);

        logger.debug("AI explanation for node {}: {}", node.getId(), response);
        return response.trim();
    }

    /**
     * Builds the prompt for generating node explanations.
     */
    private String buildNodeExplanationPrompt(String processJson, ProcessNode node) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a BPMN process analyst. Explain why this specific node exists in the process.\n\n");

        prompt.append("Process Model (JSON):\n");
        prompt.append(processJson);
        prompt.append("\n\n");

        prompt.append("Node to Explain:\n");
        prompt.append("- ID: ").append(node.getId()).append("\n");
        prompt.append("- Name: ").append(node.getName()).append("\n");
        prompt.append("- Type: ").append(node.getType()).append("\n");
        
        // Get description from properties if available
        Object description = node.getProperty("description");
        if (description != null && StringUtils.hasText(description.toString())) {
            prompt.append("- Description: ").append(description.toString()).append("\n");
        }
        prompt.append("\n");

        prompt.append("Provide a brief, clear explanation (2-3 sentences) of:\n");
        prompt.append("1. What this node does in the process\n");
        prompt.append("2. Why it's necessary\n");
        prompt.append("3. Its relationship to other nodes\n\n");

        prompt.append("Keep the explanation concise, user-friendly, and avoid technical jargon.\n\n");
        prompt.append("Explanation:");

        return prompt.toString();
    }
}

