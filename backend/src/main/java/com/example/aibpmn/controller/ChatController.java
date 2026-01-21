package com.example.aibpmn.controller;

import com.example.aibpmn.dto.ChatRequest;
import com.example.aibpmn.dto.ChatResponse;
import com.example.aibpmn.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Unified chat controller for all AI interactions.
 * 
 * This replaces the previous approach of having separate endpoints for:
 * - Process creation (/api/process/from-text, /api/process/interactive/*)
 * - Process editing (/api/process/{id}/edit-intent)
 * - Questions/help
 * 
 * Now all user messages go through a single endpoint where AI determines
 * the intent and routes to the appropriate action.
 * 
 * This is a much cleaner, more maintainable architecture.
 */
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "false") // Allow frontend access
public class ChatController {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    
    private final ChatService chatService;
    
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }
    
    /**
     * Unified endpoint for all chat interactions.
     * POST /api/chat/message
     * 
     * The AI analyzes the message and context to determine what the user wants:
     * - EDIT: Modify existing process/element
     * - CREATE: Create new process
     * - QUESTION: Answer user questions
     * - CLARIFICATION: Ask user for more details
     * 
     * Request Body:
     * {
     *   "message": "Instead of user task, make it a business rule task",
     *   "processId": "proc-123" (optional),
     *   "selectedElementId": "Task_ManagerReview" (optional),
     *   "conversationId": "conv-456" (optional)
     * }
     * 
     * Response:
     * {
     *   "intent": "EDIT",
     *   "action": "EDIT_APPLIED",
     *   "message": "Successfully changed to BusinessRuleTask",
     *   "processId": "proc-123",
     *   "requiresRefresh": true,
     *   "success": true
     * }
     */
    @PostMapping("/message")
    public ResponseEntity<ChatResponse> processMessage(@RequestBody ChatRequest request) {
        logger.info("Received chat message: '{}' (processId: {}, selectedElement: {})", 
            request.getMessage(), 
            request.getProcessId(), 
            request.getSelectedElementId());
        
        try {
            ChatResponse response = chatService.processMessage(request);
            
            logger.info("Chat response: intent={}, action={}, success={}", 
                response.getIntent(), 
                response.getAction(), 
                response.isSuccess());
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            logger.error("Error processing chat message: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(ChatResponse.error("Internal server error: " + e.getMessage()));
        }
    }
    
    /**
     * Health check endpoint.
     * GET /api/chat/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Chat service is healthy");
    }
}
