package com.example.aibpmn.service;

import com.example.aibpmn.dto.InteractiveProcessRequest;
import com.example.aibpmn.dto.InteractiveProcessResponse;
import com.example.aibpmn.dto.ProcessTextRequest;
import com.example.aibpmn.dto.ProcessTextResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for interactive process generation with clarifying questions.
 * The AI asks questions before generating the BPMN diagram.
 */
@Service
public class InteractiveProcessService {

    private static final Logger logger = LoggerFactory.getLogger(InteractiveProcessService.class);

    private final AiClient aiClient;
    private final ProcessTextService processTextService;
    private final ObjectMapper objectMapper;

    // In-memory conversation state storage
    // In production, use Redis or database
    private final Map<String, ConversationState> conversations = new ConcurrentHashMap<>();

    public InteractiveProcessService(
            AiClient aiClient,
            ProcessTextService processTextService,
            ObjectMapper objectMapper) {
        this.aiClient = aiClient;
        this.processTextService = processTextService;
        this.objectMapper = objectMapper;
        logger.info("InteractiveProcessService initialized with AI provider: {}", aiClient.getProviderName());
    }

    /**
     * Start a new interactive process generation conversation.
     * AI will analyze the description and generate clarifying questions.
     * If no clarification is needed, returns READY phase immediately.
     */
    public InteractiveProcessResponse startConversation(String processName, String processDescription) {
        logger.info("Starting interactive conversation for process: {}", processName);

        String conversationId = "conv-" + UUID.randomUUID().toString().substring(0, 8);
        
        // Generate clarifying questions using AI
        List<String> questions = generateClarifyingQuestions(processDescription);

        // Store conversation state
        ConversationState state = new ConversationState();
        state.conversationId = conversationId;
        state.processName = processName;
        state.processDescription = processDescription;
        state.questions = questions;
        conversations.put(conversationId, state);

        // Check if clarification is needed
        if (questions.isEmpty()) {
            // Description is comprehensive - no clarification needed
            logger.info("No clarification needed for conversation: {} - description is comprehensive", conversationId);
            InteractiveProcessResponse response = new InteractiveProcessResponse(
                    conversationId,
                    InteractiveProcessResponse.Phase.READY
            );
            response.setMessage("Your description is clear and comprehensive! Ready to generate the BPMN diagram.");
            return response;
        }

        // Build response with questions
        InteractiveProcessResponse response = new InteractiveProcessResponse(
                conversationId, 
                InteractiveProcessResponse.Phase.CLARIFYING
        );
        response.setQuestions(questions);
        response.setMessage("I have a few questions to help me create the best BPMN diagram:");

        logger.info("Generated {} clarifying questions for conversation: {}", questions.size(), conversationId);
        return response;
    }

    /**
     * Submit answers to clarifying questions.
     * Returns either more questions or indicates ready to generate.
     */
    public InteractiveProcessResponse submitAnswers(InteractiveProcessRequest request) {
        logger.info("Received answers for conversation: {}", request.getConversationId());

        ConversationState state = conversations.get(request.getConversationId());
        if (state == null) {
            throw new IllegalArgumentException("Conversation not found: " + request.getConversationId());
        }

        // Store Q&A pairs
        state.questionsAndAnswers.addAll(request.getQuestionsAndAnswers());

        // Check if we have enough information to generate BPMN
        if (state.questionsAndAnswers.size() >= state.questions.size()) {
            // All questions answered - ready to generate
            InteractiveProcessResponse response = new InteractiveProcessResponse(
                    request.getConversationId(),
                    InteractiveProcessResponse.Phase.READY
            );
            response.setMessage("Thank you! I have all the information I need. Ready to generate the BPMN diagram.");
            logger.info("All questions answered for conversation: {}", request.getConversationId());
            return response;
        } else {
            // More questions to answer
            int remainingQuestions = state.questions.size() - state.questionsAndAnswers.size();
            InteractiveProcessResponse response = new InteractiveProcessResponse(
                    request.getConversationId(),
                    InteractiveProcessResponse.Phase.CLARIFYING
            );
            response.setQuestions(state.questions.subList(
                    state.questionsAndAnswers.size(),
                    state.questions.size()
            ));
            response.setMessage(String.format("Thank you! I have %d more question%s:",
                    remainingQuestions,
                    remainingQuestions == 1 ? "" : "s"));
            return response;
        }
    }

    /**
     * Generate the final BPMN diagram using all collected information.
     */
    public InteractiveProcessResponse generateBpmn(String conversationId) {
        logger.info("Generating BPMN for conversation: {}", conversationId);

        ConversationState state = conversations.get(conversationId);
        if (state == null) {
            throw new IllegalArgumentException("Conversation not found: " + conversationId);
        }

        // Build enriched process description with Q&A
        String enrichedDescription = buildEnrichedDescription(state);

        // Use ProcessTextService to generate BPMN
        ProcessTextRequest textRequest = new ProcessTextRequest();
        textRequest.setName(state.processName);
        textRequest.setDescription(enrichedDescription);

        ProcessTextResponse textResponse = processTextService.createProcessFromText(textRequest);

        // Cleanup conversation state
        conversations.remove(conversationId);

        // Build response
        InteractiveProcessResponse response = new InteractiveProcessResponse(
                conversationId,
                InteractiveProcessResponse.Phase.COMPLETED
        );
        response.setProcessId(textResponse.getProcessId());
        response.setMessage("BPMN diagram generated successfully!");

        logger.info("Successfully generated BPMN (processId: {}) for conversation: {}", 
                textResponse.getProcessId(), conversationId);
        return response;
    }

    /**
     * Generate clarifying questions based on the process description.
     */
    private List<String> generateClarifyingQuestions(String processDescription) {
        String prompt = String.format("""
            You are a BPMN expert. Analyze this process description and determine what information is MISSING or UNCLEAR.
            
            CRITICAL INSTRUCTIONS:
            1. First, identify what information IS ALREADY PROVIDED in the description
            2. Only ask questions about information that is MISSING or UNCLEAR
            3. DO NOT ask questions about information that is explicitly stated
            4. If the description is comprehensive (actors, flow, conditions are clear), return an EMPTY array []
            5. Maximum 3 questions - only the most important missing pieces
            
            Focus areas for missing information:
            - Decision points: If conditions are NOT mentioned, ask about them
            - Actor roles: If specific actors/roles are NOT mentioned, ask about them
            - Exception handling: If error paths are NOT described, ask about them
            - Data validation: If business rules are NOT specified, ask about them
            - Parallel vs sequential: If timing/concurrency is NOT clear, ask about it
            
            Process Description:
            %s
            
            EXAMPLES:
            
            Example 1 (Comprehensive description):
            Input: "Employee submits leave, manager approves if days <= 5, otherwise rejected. HR processes approved requests."
            Output: []
            Reason: Actors (Employee, Manager, HR), flow, and conditions (days <= 5) are all clear.
            
            Example 2 (Missing information):
            Input: "User submits a form and it gets reviewed."
            Output: ["Who reviews the form and what are the approval criteria?", "What happens if the review is rejected?"]
            Reason: Reviewer role and decision criteria are missing.
            
            Example 3 (Partial information):
            Input: "Customer places order, payment is processed, order is shipped."
            Output: ["What happens if payment fails?", "Can order preparation happen in parallel with payment processing?"]
            Reason: Error handling and parallelism are not specified.
            
            NOW ANALYZE THE GIVEN DESCRIPTION:
            Return ONLY a JSON array (no markdown, no explanation):
            ["Question 1?", "Question 2?"]
            OR
            []
            """, processDescription);

        try {
            String aiResponse = aiClient.generateFromText(prompt);
            
            // Clean up response (remove markdown code blocks if present)
            String cleanedResponse = aiResponse.trim();
            if (cleanedResponse.startsWith("```")) {
                cleanedResponse = cleanedResponse.replaceAll("```json\\s*", "").replaceAll("```\\s*", "");
            }

            // Parse JSON array
            JsonNode jsonNode = objectMapper.readTree(cleanedResponse);
            List<String> questions = new ArrayList<>();
            
            if (jsonNode.isArray()) {
                for (JsonNode questionNode : jsonNode) {
                    questions.add(questionNode.asText());
                }
            }

            if (questions.isEmpty()) {
                // Fallback to default questions
                logger.warn("AI did not return valid questions, using defaults");
                return getDefaultQuestions();
            }

            return questions;

        } catch (Exception e) {
            logger.error("Error generating clarifying questions, using defaults", e);
            return getDefaultQuestions();
        }
    }

    /**
     * Build enriched process description including Q&A pairs.
     */
    private String buildEnrichedDescription(ConversationState state) {
        StringBuilder enriched = new StringBuilder();
        enriched.append("ORIGINAL PROCESS DESCRIPTION:\n");
        enriched.append(state.processDescription);
        enriched.append("\n\nCLARIFICATIONS:\n");

        for (int i = 0; i < state.questionsAndAnswers.size(); i++) {
            InteractiveProcessRequest.QAPair qa = state.questionsAndAnswers.get(i);
            enriched.append(String.format("\nQ%d: %s\nA%d: %s\n", 
                    i + 1, qa.getQuestion(), 
                    i + 1, qa.getAnswer()));
        }

        return enriched.toString();
    }

    /**
     * Default questions if AI fails to generate them.
     */
    private List<String> getDefaultQuestions() {
        return Arrays.asList(
                "What are the main decision points in this process and their conditions?",
                "Who are the actors/roles involved and what are their responsibilities?",
                "What happens if validation fails or an error occurs?",
                "Are there any parallel activities that can happen at the same time?"
        );
    }

    /**
     * Internal conversation state.
     */
    private static class ConversationState {
        String conversationId;
        String processName;
        String processDescription;
        List<String> questions = new ArrayList<>();
        List<InteractiveProcessRequest.QAPair> questionsAndAnswers = new ArrayList<>();
    }
}
