package com.example.aibpmn.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

/**
 * Request DTO for interactive process generation with clarifying questions.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class InteractiveProcessRequest {

    private String conversationId;
    private String processName;
    private String processDescription;
    private List<QAPair> questionsAndAnswers = new ArrayList<>();

    public InteractiveProcessRequest() {
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public String getProcessDescription() {
        return processDescription;
    }

    public void setProcessDescription(String processDescription) {
        this.processDescription = processDescription;
    }

    public List<QAPair> getQuestionsAndAnswers() {
        return questionsAndAnswers;
    }

    public void setQuestionsAndAnswers(List<QAPair> questionsAndAnswers) {
        this.questionsAndAnswers = questionsAndAnswers;
    }

    /**
     * Question-Answer pair for conversation history.
     */
    public static class QAPair {
        private String question;
        private String answer;

        public QAPair() {
        }

        public QAPair(String question, String answer) {
            this.question = question;
            this.answer = answer;
        }

        public String getQuestion() {
            return question;
        }

        public void setQuestion(String question) {
            this.question = question;
        }

        public String getAnswer() {
            return answer;
        }

        public void setAnswer(String answer) {
            this.answer = answer;
        }
    }
}
