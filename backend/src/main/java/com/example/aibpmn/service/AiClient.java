package com.example.aibpmn.service;

import org.springframework.core.io.Resource;

import java.nio.file.Path;

/**
 * Interface for AI client services.
 * Supports both text-only prompts and image + prompt interactions.
 * 
 * Implementations:
 * - OpenAiClient: Uses OpenAI GPT-4o (default)
 * - GeminiClient: Uses Google Gemini 2.0
 */
public interface AiClient {
    
    /**
     * Send a text-only prompt to the AI and get the response.
     *
     * @param prompt The text prompt to send
     * @return Raw text response from AI
     * @throws IllegalArgumentException if prompt is null or empty
     */
    String generateFromText(String prompt);
    
    /**
     * Send an image with a text prompt to the AI and get the response.
     *
     * @param imagePath Path to the image file
     * @param prompt The text prompt to send along with the image
     * @return Raw text response from AI
     * @throws IllegalArgumentException if imagePath or prompt is null/empty
     * @throws RuntimeException if image cannot be read or API call fails
     */
    String generateFromImage(Path imagePath, String prompt);
    
    /**
     * Send an image (as Resource) with a text prompt to the AI.
     *
     * @param imageResource Spring Resource containing the image
     * @param prompt The text prompt to send along with the image
     * @return Raw text response from AI
     * @throws IllegalArgumentException if imageResource or prompt is null/empty
     * @throws RuntimeException if image cannot be read or API call fails
     */
    String generateFromImage(Resource imageResource, String prompt);
    
    /**
     * Get the name of the AI provider.
     *
     * @return Provider name (e.g., "OpenAI", "Gemini")
     */
    String getProviderName();
}

