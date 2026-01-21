package com.example.aibpmn.service;

import com.example.aibpmn.dto.*;
import com.example.aibpmn.model.ProcessModel;
import com.example.aibpmn.repository.ProcessModelRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * Unified chat service that uses AI to detect intent and route to appropriate actions.
 * 
 * This replaces the frontend keyword detection with intelligent AI-based intent analysis.
 * The AI understands context and natural language to determine what the user wants.
 */
@Service
public class ChatService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    
    private final AiClient aiClient;
    private final ProcessEditService processEditService;
    private final InteractiveProcessService interactiveProcessService;
    private final ProcessModelRepository processModelRepository;
    private final ObjectMapper objectMapper;
    
    public ChatService(AiClient aiClient,
                       ProcessEditService processEditService,
                       InteractiveProcessService interactiveProcessService,
                       ProcessModelRepository processModelRepository,
                       ObjectMapper objectMapper) {
        this.aiClient = aiClient;
        this.processEditService = processEditService;
        this.interactiveProcessService = interactiveProcessService;
        this.processModelRepository = processModelRepository;
        this.objectMapper = objectMapper;
        logger.info("ChatService initialized with AI provider: {}", aiClient.getProviderName());
    }
    
    /**
     * Main entry point for all chat interactions.
     * AI analyzes the message and context to determine intent and take appropriate action.
     */
    public ChatResponse processMessage(ChatRequest request) {
        logger.info("Processing chat message: {}", request.getMessage());
        
        // Validate input
        if (!StringUtils.hasText(request.getMessage())) {
            return ChatResponse.error("Message cannot be empty.");
        }
        
        try {
            // Use AI to detect intent
            IntentAnalysis analysis = analyzeIntent(request);
            
            logger.info("Detected intent: {} for message: {}", analysis.intent, request.getMessage());
            
            // Route to appropriate handler based on detected intent
            switch (analysis.intent) {
                case EDIT:
                    return handleEditIntent(request, analysis);
                    
                case CREATE:
                    return handleCreateIntent(request, analysis);
                    
                case QUESTION:
                    return handleQuestionIntent(request, analysis);
                    
                case CLARIFICATION:
                    return handleClarificationIntent(request, analysis);
                    
                default:
                    return ChatResponse.error("Unable to understand your request. Please try rephrasing.");
            }
            
        } catch (Exception e) {
            logger.error("Error processing chat message: {}", e.getMessage(), e);
            return ChatResponse.error("An error occurred: " + e.getMessage());
        }
    }
    
    /**
     * Use AI to analyze the user's message and determine their intent.
     */
    private IntentAnalysis analyzeIntent(ChatRequest request) {
        String prompt = buildIntentAnalysisPrompt(request);
        
        logger.debug("Sending intent analysis prompt to AI...");
        String aiResponse = aiClient.generateFromText(prompt);
        
        logger.debug("AI intent analysis response: {}", aiResponse);
        
        // Parse AI response - strip markdown code blocks if present
        try {
            String cleanJson = stripMarkdownCodeBlocks(aiResponse);
            JsonNode jsonResponse = objectMapper.readTree(cleanJson);
            
            IntentAnalysis analysis = new IntentAnalysis();
            analysis.intent = ChatResponse.Intent.valueOf(jsonResponse.get("intent").asText().toUpperCase());
            analysis.confidence = jsonResponse.has("confidence") ? jsonResponse.get("confidence").asDouble() : 0.9;
            analysis.reasoning = jsonResponse.has("reasoning") ? jsonResponse.get("reasoning").asText() : "";
            analysis.suggestedAction = jsonResponse.has("action") ? jsonResponse.get("action").asText() : "";
            
            return analysis;
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse AI intent response: {}", e.getMessage());
            // Fallback: try to determine intent based on context
            return fallbackIntentDetection(request);
        }
    }
    
    /**
     * Build prompt for AI to analyze user intent.
     */
    private String buildIntentAnalysisPrompt(ChatRequest request) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are an intelligent BPMN assistant. Analyze this user message and determine their intent.\n\n");
        
        prompt.append("**Context:**\n");
        prompt.append("- Process ID: ").append(request.getProcessId() != null ? request.getProcessId() : "NONE (no active process)").append("\n");
        prompt.append("- Selected Element: ").append(request.getSelectedElementId() != null ? request.getSelectedElementId() : "NONE").append("\n");
        
        if (request.getProcessId() != null) {
            // Add process details if available
            Optional<ProcessModel> processOpt = processModelRepository.findById(request.getProcessId());
            if (processOpt.isPresent()) {
                ProcessModel process = processOpt.get();
                prompt.append("- Process Name: ").append(process.getName()).append("\n");
                prompt.append("- Number of Nodes: ").append(process.getNodes().size()).append("\n");
            }
        }
        
        prompt.append("\n**User Message:**\n");
        prompt.append(request.getMessage()).append("\n\n");
        
        prompt.append("**Task:**\n");
        prompt.append("Determine the user's intent and respond with ONLY a JSON object:\n\n");
        
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"intent\": \"EDIT\" | \"CREATE\" | \"QUESTION\" | \"CLARIFICATION\",\n");
        prompt.append("  \"confidence\": 0.0 to 1.0,\n");
        prompt.append("  \"reasoning\": \"Brief explanation of why this intent was chosen\",\n");
        prompt.append("  \"action\": \"Suggested action to take\"\n");
        prompt.append("}\n");
        prompt.append("```\n\n");
        
        prompt.append("**Intent Definitions:**\n");
        prompt.append("- **EDIT**: User wants to modify an existing process or element\n");
        prompt.append("  Examples: \"Change this to a business rule task\", \"Add a condition\", \"Instead of user task...\"\n");
        prompt.append("  Indicators: Element selected OR mentions modifying existing process\n\n");
        
        prompt.append("- **CREATE**: User wants to create a new BPMN process\n");
        prompt.append("  Examples: \"Create a leave approval process\", \"I need a workflow for...\"\n");
        prompt.append("  Indicators: Describes a new process, no process ID provided\n\n");
        
        prompt.append("- **QUESTION**: User asking for help or information\n");
        prompt.append("  Examples: \"How do I...\", \"What is...\", \"Can you explain...\"\n");
        prompt.append("  Indicators: Question words, asking for explanation\n\n");
        
        prompt.append("- **CLARIFICATION**: Message is ambiguous, need more information\n");
        prompt.append("  Examples: Vague requests, unclear references\n");
        prompt.append("  Indicators: Not enough context to determine intent\n\n");
        
        prompt.append("**Important:**\n");
        prompt.append("- If element is selected AND message describes a change → EDIT\n");
        prompt.append("- If no process exists AND message describes a workflow → CREATE\n");
        prompt.append("- Natural language like \"instead of\", \"make it\", \"convert\" → EDIT (if context exists)\n");
        prompt.append("- When in doubt, use higher confidence intent\n\n");
        
        prompt.append("Return ONLY the JSON object, no additional text:");
        
        return prompt.toString();
    }
    
    /**
     * Fallback intent detection if AI response parsing fails.
     */
    private IntentAnalysis fallbackIntentDetection(ChatRequest request) {
        IntentAnalysis analysis = new IntentAnalysis();
        analysis.confidence = 0.5;
        
        String lowerMessage = request.getMessage().toLowerCase();
        
        // If element selected and message mentions changes → EDIT
        if (request.getSelectedElementId() != null &&
            (lowerMessage.contains("change") || lowerMessage.contains("modify") || 
             lowerMessage.contains("instead") || lowerMessage.contains("update") ||
             lowerMessage.contains("add") || lowerMessage.contains("convert"))) {
            analysis.intent = ChatResponse.Intent.EDIT;
            analysis.reasoning = "Element selected and message indicates modification";
            return analysis;
        }
        
        // If no process and describes workflow → CREATE
        if (request.getProcessId() == null &&
            (lowerMessage.contains("process") || lowerMessage.contains("workflow") ||
             lowerMessage.contains("approval") || lowerMessage.contains("create"))) {
            analysis.intent = ChatResponse.Intent.CREATE;
            analysis.reasoning = "No active process and message describes new workflow";
            return analysis;
        }
        
        // If question words → QUESTION
        if (lowerMessage.startsWith("how") || lowerMessage.startsWith("what") ||
            lowerMessage.startsWith("why") || lowerMessage.startsWith("can you") ||
            lowerMessage.contains("?")) {
            analysis.intent = ChatResponse.Intent.QUESTION;
            analysis.reasoning = "Message appears to be a question";
            return analysis;
        }
        
        // Default: CLARIFICATION
        analysis.intent = ChatResponse.Intent.CLARIFICATION;
        analysis.reasoning = "Unable to determine intent, need more information";
        return analysis;
    }
    
    /**
     * Handle EDIT intent - modify existing process/element.
     */
    private ChatResponse handleEditIntent(ChatRequest request, IntentAnalysis analysis) {
        if (request.getProcessId() == null) {
            return ChatResponse.error("No active process to edit. Please create or load a process first.");
        }
        
        logger.info("Handling EDIT intent for process: {}", request.getProcessId());
        
        // Use existing ProcessEditService
        EditIntentRequest editRequest = new EditIntentRequest();
        editRequest.setInstruction(request.getMessage());
        editRequest.setNodeId(request.getSelectedElementId());
        
        EditIntentResponse editResponse = processEditService.processEditIntent(request.getProcessId(), editRequest);
        
        if (editResponse.isSuccess()) {
            ChatResponse response = ChatResponse.success(
                ChatResponse.Intent.EDIT,
                ChatResponse.ActionType.EDIT_APPLIED,
                editResponse.getMessage()
            );
            response.setProcessId(request.getProcessId());
            response.setRequiresRefresh(editResponse.isBpmnRegenerated());
            return response;
        } else {
            return ChatResponse.error("Edit failed: " + editResponse.getMessage());
        }
    }
    
    /**
     * Handle CREATE intent - start new process creation.
     */
    private ChatResponse handleCreateIntent(ChatRequest request, IntentAnalysis analysis) {
        logger.info("Handling CREATE intent for new process");
        
        // Use existing InteractiveProcessService
        try {
            InteractiveProcessResponse interactiveResponse = 
                interactiveProcessService.startConversation("Generated Process", request.getMessage());
            
            ChatResponse response = new ChatResponse();
            response.setIntent(ChatResponse.Intent.CREATE);
            response.setSuccess(true);
            
            if (interactiveResponse.getPhase() == InteractiveProcessResponse.Phase.CLARIFYING) {
                // Description needs clarification
                response.setAction(ChatResponse.ActionType.CLARIFICATION_NEEDED);
                response.setQuestions(interactiveResponse.getQuestions());
                response.setConversationId(interactiveResponse.getConversationId());
                response.setMessage(interactiveResponse.getMessage());
            } else if (interactiveResponse.getPhase() == InteractiveProcessResponse.Phase.READY) {
                // Description is comprehensive - generate BPMN immediately
                logger.info("Description is comprehensive, generating BPMN immediately");
                InteractiveProcessResponse bpmnResponse = 
                    interactiveProcessService.generateBpmn(interactiveResponse.getConversationId());
                
                response.setAction(ChatResponse.ActionType.PROCESS_CREATED);
                response.setProcessId(bpmnResponse.getProcessId());
                response.setRequiresRefresh(true);
                response.setMessage("✅ " + bpmnResponse.getMessage());
            } else if (interactiveResponse.getPhase() == InteractiveProcessResponse.Phase.COMPLETED) {
                // Already completed (shouldn't happen in startConversation, but handle it)
                response.setAction(ChatResponse.ActionType.PROCESS_CREATED);
                response.setProcessId(interactiveResponse.getProcessId());
                response.setRequiresRefresh(true);
                response.setMessage(interactiveResponse.getMessage());
            }
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error creating process: {}", e.getMessage(), e);
            return ChatResponse.error("Failed to start process creation: " + e.getMessage());
        }
    }
    
    /**
     * Handle QUESTION intent - provide information to user.
     */
    private ChatResponse handleQuestionIntent(ChatRequest request, IntentAnalysis analysis) {
        logger.info("Handling QUESTION intent");
        
        // Generate helpful response using AI
        String helpPrompt = buildHelpPrompt(request);
        String aiResponse = aiClient.generateFromText(helpPrompt);
        
        ChatResponse response = ChatResponse.success(
            ChatResponse.Intent.QUESTION,
            ChatResponse.ActionType.INFORMATION_PROVIDED,
            aiResponse
        );
        response.setRequiresRefresh(false);
        return response;
    }
    
    /**
     * Handle CLARIFICATION intent - ask user for more information.
     */
    private ChatResponse handleClarificationIntent(ChatRequest request, IntentAnalysis analysis) {
        logger.info("Handling CLARIFICATION intent");
        
        String clarificationMessage = "I'm not sure I understand. ";
        
        if (request.getProcessId() == null && request.getSelectedElementId() == null) {
            clarificationMessage += "Would you like to:\n" +
                "1. Create a new BPMN process (describe your workflow)\n" +
                "2. Edit an existing process (load a process first)\n" +
                "3. Ask a question about BPMN";
        } else if (request.getSelectedElementId() != null) {
            clarificationMessage += "You have an element selected. Would you like to modify it? " +
                "Please describe what changes you'd like to make.";
        } else {
            clarificationMessage += "Could you please rephrase your request or provide more details?";
        }
        
        ChatResponse response = ChatResponse.success(
            ChatResponse.Intent.CLARIFICATION,
            ChatResponse.ActionType.INFORMATION_PROVIDED,
            clarificationMessage
        );
        response.setRequiresRefresh(false);
        return response;
    }
    
    /**
     * Build prompt for answering user questions.
     */
    private String buildHelpPrompt(ChatRequest request) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are a helpful BPMN assistant. Answer this user question clearly and concisely:\n\n");
        prompt.append("User Question: ").append(request.getMessage()).append("\n\n");
        
        if (request.getProcessId() != null) {
            prompt.append("Context: User has an active process loaded.\n");
        }
        if (request.getSelectedElementId() != null) {
            prompt.append("Context: User has selected element: ").append(request.getSelectedElementId()).append("\n");
        }
        
        prompt.append("\nProvide a helpful, friendly response. Keep it concise (2-3 sentences) unless more detail is needed.\n\n");
        prompt.append("Answer:");
        
        return prompt.toString();
    }
    
    /**
     * Strip markdown code blocks from AI response.
     * AI sometimes returns JSON wrapped in ```json ... ``` which breaks parsing.
     */
    private String stripMarkdownCodeBlocks(String response) {
        if (response == null) {
            return response;
        }
        
        // Remove ```json ... ``` or ``` ... ``` blocks
        String cleaned = response.trim();
        
        // Check if wrapped in code blocks
        if (cleaned.startsWith("```")) {
            // Find the first newline after opening ```
            int firstNewline = cleaned.indexOf('\n');
            if (firstNewline > 0) {
                cleaned = cleaned.substring(firstNewline + 1);
            } else {
                cleaned = cleaned.substring(3); // Just remove ```
            }
            
            // Remove closing ```
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3);
            }
            
            cleaned = cleaned.trim();
        }
        
        return cleaned;
    }
    
    /**
     * Internal class for intent analysis results.
     */
    private static class IntentAnalysis {
        ChatResponse.Intent intent;
        double confidence;
        String reasoning;
        String suggestedAction;
    }
}
