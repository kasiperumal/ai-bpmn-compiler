package com.example.aibpmn.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * User's responses to clarification questions.
 */
public class ClarificationResponse {
    
    private String processId;
    private Map<String, String> answers; // question -> answer
    private String additionalNotes; // Optional additional context from user
    
    public ClarificationResponse() {
        this.answers = new HashMap<>();
    }
    
    public ClarificationResponse(String processId, Map<String, String> answers) {
        this.processId = processId;
        this.answers = answers;
    }
    
    // Getters and Setters
    
    public String getProcessId() {
        return processId;
    }
    
    public void setProcessId(String processId) {
        this.processId = processId;
    }
    
    public Map<String, String> getAnswers() {
        return answers;
    }
    
    public void setAnswers(Map<String, String> answers) {
        this.answers = answers;
    }
    
    public String getAdditionalNotes() {
        return additionalNotes;
    }
    
    public void setAdditionalNotes(String additionalNotes) {
        this.additionalNotes = additionalNotes;
    }
    
    public void addAnswer(String question, String answer) {
        this.answers.put(question, answer);
    }
    
    public String getAnswer(String question) {
        return answers.get(question);
    }
    
    public int getAnswerCount() {
        return answers.size();
    }
}

