package com.example.aibpmn.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains clarification questions that need user response.
 */
public class ClarificationRequest {
    
    private String processId;
    private List<String> questions;
    private String context; // Optional context about why clarification is needed
    
    public ClarificationRequest() {
        this.questions = new ArrayList<>();
    }
    
    public ClarificationRequest(String processId, List<String> questions) {
        this.processId = processId;
        this.questions = questions;
    }
    
    // Getters and Setters
    
    public String getProcessId() {
        return processId;
    }
    
    public void setProcessId(String processId) {
        this.processId = processId;
    }
    
    public List<String> getQuestions() {
        return questions;
    }
    
    public void setQuestions(List<String> questions) {
        this.questions = questions;
    }
    
    public String getContext() {
        return context;
    }
    
    public void setContext(String context) {
        this.context = context;
    }
    
    public void addQuestion(String question) {
        this.questions.add(question);
    }
    
    public int getQuestionCount() {
        return questions.size();
    }
}

