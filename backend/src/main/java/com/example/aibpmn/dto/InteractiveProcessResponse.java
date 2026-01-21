package com.example.aibpmn.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for interactive process generation.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class InteractiveProcessResponse {

    private String conversationId;
    private Phase phase;
    private List<String> questions = new ArrayList<>();
    private String processId;
    private String message;

    public enum Phase {
        CLARIFYING,    // AI is asking questions
        READY,         // Ready to generate BPMN
        GENERATING,    // Generating BPMN
        COMPLETED      // BPMN generated successfully
    }

    public InteractiveProcessResponse() {
    }

    public InteractiveProcessResponse(String conversationId, Phase phase) {
        this.conversationId = conversationId;
        this.phase = phase;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public Phase getPhase() {
        return phase;
    }

    public void setPhase(Phase phase) {
        this.phase = phase;
    }

    public List<String> getQuestions() {
        return questions;
    }

    public void setQuestions(List<String> questions) {
        this.questions = questions;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
