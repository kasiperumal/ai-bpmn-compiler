package com.example.aibpmn.config;

import com.example.aibpmn.service.AiClient;
import org.mockito.Mockito;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;

import java.nio.file.Path;

/**
 * Test configuration for AI services.
 * 
 * This configuration provides mock beans for testing
 * to avoid issues with multiple ChatModel beans from OpenAI and Gemini.
 */
@TestConfiguration
public class TestAiConfiguration {
    
    /**
     * Creates a mock ChatModel with default behavior.
     */
    private ChatModel createMockChatModel() {
        ChatModel mockChatModel = Mockito.mock(ChatModel.class);
        
        // Configure default mock behavior
        ChatResponse mockResponse = new ChatResponse(
            java.util.List.of(new Generation(new AssistantMessage("{\"nodes\": [], \"edges\": [], \"rules\": []}")))
        );
        
        Mockito.when(mockChatModel.call(Mockito.any(Prompt.class)))
            .thenReturn(mockResponse);
        
        return mockChatModel;
    }
    
    /**
     * Provides a mock ChatModel for OpenAI.
     * Bean name matches what OpenAI auto-configuration creates.
     */
    @Bean(name = "openAiChatModel")
    public ChatModel openAiChatModel() {
        return createMockChatModel();
    }
    
    /**
     * Provides a mock ChatModel for Gemini.
     * Bean name matches what Gemini auto-configuration creates.
     */
    @Bean(name = "vertexAiGeminiChat")
    public ChatModel vertexAiGeminiChat() {
        return createMockChatModel();
    }
    
    /**
     * Provides a mock AiClient for testing.
     * This avoids the need for real API keys and prevents
     * NoUniqueBeanDefinitionException from multiple ChatModel beans.
     * 
     * Marked as @Primary to override any other AiClient beans.
     */
    @Bean
    @Primary
    public AiClient primaryAiClient() {
        AiClient mockClient = Mockito.mock(AiClient.class);
        
        // Configure default mock behavior
        Mockito.when(mockClient.getProviderName()).thenReturn("Test AI Provider");
        Mockito.when(mockClient.generateFromText(Mockito.anyString()))
            .thenReturn("{\"nodes\": [], \"edges\": [], \"rules\": []}");
        
        // Mock image methods
        Mockito.when(mockClient.generateFromImage(Mockito.any(Path.class), Mockito.anyString()))
            .thenReturn("Generated from image");
        Mockito.when(mockClient.generateFromImage(Mockito.any(Resource.class), Mockito.anyString()))
            .thenReturn("Generated from image resource");
        
        return mockClient;
    }
}

