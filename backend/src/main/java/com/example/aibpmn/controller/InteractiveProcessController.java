package com.example.aibpmn.controller;

import com.example.aibpmn.dto.InteractiveProcessRequest;
import com.example.aibpmn.dto.InteractiveProcessResponse;
import com.example.aibpmn.service.InteractiveProcessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for interactive process generation.
 * Supports a conversational flow where the AI asks clarifying questions
 * before generating the BPMN diagram.
 */
@RestController
@RequestMapping("/api/process/interactive")
public class InteractiveProcessController {

    private static final Logger logger = LoggerFactory.getLogger(InteractiveProcessController.class);

    private final InteractiveProcessService interactiveService;

    public InteractiveProcessController(InteractiveProcessService interactiveService) {
        this.interactiveService = interactiveService;
    }

    /**
     * Start a new interactive conversation.
     * POST /api/process/interactive/start
     * 
     * Request body:
     * {
     *   "processName": "Leave Approval",
     *   "processDescription": "Employee submits leave request..."
     * }
     * 
     * Response:
     * {
     *   "conversationId": "conv-abc123",
     *   "phase": "CLARIFYING",
     *   "questions": ["Question 1?", "Question 2?"],
     *   "message": "I have a few questions..."
     * }
     */
    @PostMapping("/start")
    public ResponseEntity<InteractiveProcessResponse> startConversation(
            @RequestBody Map<String, String> request) {
        
        String processName = request.get("processName");
        String processDescription = request.get("processDescription");

        logger.info("POST /api/process/interactive/start - processName: {}", processName);

        if (processName == null || processName.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (processDescription == null || processDescription.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        InteractiveProcessResponse response = interactiveService.startConversation(
                processName, 
                processDescription
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Submit answers to clarifying questions.
     * POST /api/process/interactive/answer
     * 
     * Request body:
     * {
     *   "conversationId": "conv-abc123",
     *   "questionsAndAnswers": [
     *     {"question": "Q1?", "answer": "A1"},
     *     {"question": "Q2?", "answer": "A2"}
     *   ]
     * }
     * 
     * Response:
     * {
     *   "conversationId": "conv-abc123",
     *   "phase": "READY" | "CLARIFYING",
     *   "questions": [...],  // if more questions
     *   "message": "..."
     * }
     */
    @PostMapping("/answer")
    public ResponseEntity<InteractiveProcessResponse> submitAnswers(
            @RequestBody InteractiveProcessRequest request) {

        logger.info("POST /api/process/interactive/answer - conversationId: {}, answers: {}", 
                request.getConversationId(), 
                request.getQuestionsAndAnswers().size());

        if (request.getConversationId() == null || request.getConversationId().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            InteractiveProcessResponse response = interactiveService.submitAnswers(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid conversation ID: {}", request.getConversationId());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Generate the final BPMN diagram.
     * POST /api/process/interactive/generate/{conversationId}
     * 
     * Response:
     * {
     *   "conversationId": "conv-abc123",
     *   "phase": "COMPLETED",
     *   "processId": "proc-xyz789",
     *   "message": "BPMN diagram generated successfully!"
     * }
     */
    @PostMapping("/generate/{conversationId}")
    public ResponseEntity<InteractiveProcessResponse> generateBpmn(
            @PathVariable String conversationId) {

        logger.info("POST /api/process/interactive/generate/{}", conversationId);

        try {
            InteractiveProcessResponse response = interactiveService.generateBpmn(conversationId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid conversation ID: {}", conversationId);
            return ResponseEntity.notFound().build();
        }
    }
}
