package com.example.aibpmn.config;

import com.example.aibpmn.service.AiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration for AI Client selection.
 * 
 * Configures which AI provider to use based on application.yml setting:
 * - app.ai.provider=openai (default) → Uses OpenAI GPT-4o
 * - app.ai.provider=gemini → Uses Google Gemini 2.0
 * 
 * The selected client is available for injection as the primary AiClient bean.
 */
@Configuration
public class AiClientConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(AiClientConfiguration.class);
    
    @Value("${app.ai.provider:openai}")
    private String aiProvider;
    
    /**
     * Provides the primary AI client bean based on configuration.
     * 
     * @param openAiClient The OpenAI client implementation
     * @param geminiClient The Gemini client implementation
     * @return The selected AI client
     */
    @Bean
    @Primary
    public AiClient primaryAiClient(
            @Qualifier("openAiClient") AiClient openAiClient,
            @Qualifier("geminiClient") AiClient geminiClient) {
        
        AiClient selectedClient;
        
        if ("gemini".equalsIgnoreCase(aiProvider)) {
            selectedClient = geminiClient;
            logger.info("╔═══════════════════════════════════════════════════════╗");
            logger.info("║   AI Provider: Google Gemini 2.0                       ║");
            logger.info("║   Configure via: app.ai.provider=gemini                ║");
            logger.info("╚═══════════════════════════════════════════════════════╝");
        } else {
            selectedClient = openAiClient;
            logger.info("╔═══════════════════════════════════════════════════════╗");
            logger.info("║   AI Provider: OpenAI GPT-4o (Default)                ║");
            logger.info("║   To switch: app.ai.provider=gemini                    ║");
            logger.info("╚═══════════════════════════════════════════════════════╝");
        }
        
        logger.info("Selected AI Provider: {}", selectedClient.getProviderName());
        
        return selectedClient;
    }
}

