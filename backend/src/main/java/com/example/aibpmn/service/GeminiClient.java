package com.example.aibpmn.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.Media;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

/**
 * Client service for interacting with Google Gemini 2.0 via Spring AI.
 * Supports both text-only prompts and image + prompt interactions.
 * 
 * This is an alternative AI provider (default is OpenAI GPT-4o).
 * To use this, set: app.ai.provider=gemini in application.yml
 */
@Service("geminiClient")
public class GeminiClient implements AiClient {
    
    private static final Logger logger = LoggerFactory.getLogger(GeminiClient.class);
    
    private final ChatClient chatClient;
    private final ChatModel chatModel;
    
    public GeminiClient(@Qualifier("vertexAiGeminiChat") ChatModel chatModel) {
        this.chatModel = chatModel;
        this.chatClient = ChatClient.builder(chatModel).build();
        logger.info("GeminiClient initialized with Gemini 2.0");
    }
    
    @Override
    public String getProviderName() {
        return "Google Gemini 2.0";
    }
    
    /**
     * Send a text-only prompt to Gemini and get the response.
     *
     * @param prompt The text prompt to send
     * @return Raw text response from Gemini
     * @throws IllegalArgumentException if prompt is null or empty
     */
    @Override
    public String generateFromText(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException("Prompt cannot be null or empty");
        }
        
        logger.debug("Sending text-only prompt to Gemini (length: {} chars)", prompt.length());
        
        try {
            String response = chatClient
                .prompt()
                .user(prompt)
                .call()
                .content();
            
            logger.debug("Received response from Gemini (length: {} chars)", 
                response != null ? response.length() : 0);
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error calling Gemini API with text prompt", e);
            throw new RuntimeException("Failed to generate response from Gemini: " + e.getMessage(), e);
        }
    }
    
    /**
     * Send an image with a text prompt to Gemini and get the response.
     *
     * @param imagePath Path to the image file
     * @param prompt The text prompt to send along with the image
     * @return Raw text response from Gemini
     * @throws IllegalArgumentException if imagePath or prompt is null/empty
     * @throws RuntimeException if image cannot be read or API call fails
     */
    @Override
    public String generateFromImage(Path imagePath, String prompt) {
        if (imagePath == null || !Files.exists(imagePath)) {
            throw new IllegalArgumentException("Image path is null or file does not exist");
        }
        
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException("Prompt cannot be null or empty");
        }
        
        logger.debug("Sending image + prompt to Gemini (image: {}, prompt length: {} chars)", 
            imagePath.getFileName(), prompt.length());
        
        try {
            // Read image bytes
            byte[] imageData = Files.readAllBytes(imagePath);
            
            // Determine MIME type from file extension
            MimeType mimeType = getMimeTypeObject(imagePath);
            
            // Create media object with image data as resource
            Resource imageResource = new ByteArrayResource(imageData);
            Media media = new Media(mimeType, imageResource);
            
            // Create user message with text and image
            UserMessage userMessage = new UserMessage(prompt, List.of(media));
            
            // Create prompt and call Gemini
            Prompt geminiPrompt = new Prompt(List.of(userMessage));
            String response = chatModel.call(geminiPrompt).getResult().getOutput().getText();
            
            logger.debug("Received response from Gemini (length: {} chars)", 
                response != null ? response.length() : 0);
            
            return response;
            
        } catch (IOException e) {
            logger.error("Error reading image file: {}", imagePath, e);
            throw new RuntimeException("Failed to read image file: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error calling Gemini API with image", e);
            throw new RuntimeException("Failed to generate response from Gemini: " + e.getMessage(), e);
        }
    }
    
    /**
     * Send an image (as Resource) with a text prompt to Gemini.
     *
     * @param imageResource Spring Resource containing the image
     * @param prompt The text prompt to send along with the image
     * @return Raw text response from Gemini
     * @throws IllegalArgumentException if imageResource or prompt is null/empty
     * @throws RuntimeException if image cannot be read or API call fails
     */
    @Override
    public String generateFromImage(Resource imageResource, String prompt) {
        if (imageResource == null || !imageResource.exists()) {
            throw new IllegalArgumentException("Image resource is null or does not exist");
        }
        
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException("Prompt cannot be null or empty");
        }
        
        logger.debug("Sending image resource + prompt to Gemini (prompt length: {} chars)", 
            prompt.length());
        
        try {
            // Determine MIME type from filename or default to JPEG
            String filename = imageResource.getFilename();
            MimeType mimeType = filename != null ? 
                getMimeTypeObjectFromFilename(filename) : MimeTypeUtils.IMAGE_JPEG;
            
            // Create media object with image resource
            Media media = new Media(mimeType, imageResource);
            
            // Create user message with text and image
            UserMessage userMessage = new UserMessage(prompt, List.of(media));
            
            // Create prompt and call Gemini
            Prompt geminiPrompt = new Prompt(List.of(userMessage));
            String response = chatModel.call(geminiPrompt).getResult().getOutput().getText();
            
            logger.debug("Received response from Gemini (length: {} chars)", 
                response != null ? response.length() : 0);
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error calling Gemini API with image resource", e);
            throw new RuntimeException("Failed to generate response from Gemini: " + e.getMessage(), e);
        }
    }
    
    /**
     * Send a base64-encoded image with a text prompt to Google Gemini 2.0.
     *
     * @param base64Image Base64-encoded image data
     * @param mimeType Image MIME type (e.g., "image/jpeg", "image/png")
     * @param prompt The text prompt to send along with the image
     * @return Raw text response from Gemini
     * @throws IllegalArgumentException if parameters are null/empty
     * @throws RuntimeException if API call fails
     */
    @Override
    public String generateFromImageAndText(String base64Image, String mimeType, String prompt) {
        if (base64Image == null || base64Image.trim().isEmpty()) {
            throw new IllegalArgumentException("Base64 image cannot be null or empty");
        }
        if (mimeType == null || mimeType.trim().isEmpty()) {
            throw new IllegalArgumentException("MIME type cannot be null or empty");
        }
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException("Prompt cannot be null or empty");
        }
        
        logger.debug("Sending base64 image + prompt to Gemini 2.0 (mimeType: {}, prompt length: {} chars)", 
            mimeType, prompt.length());
        
        try {
            // Decode base64 image
            byte[] imageData = Base64.getDecoder().decode(base64Image);
            
            // Create media object with image data as resource
            Resource imageResource = new ByteArrayResource(imageData);
            MimeType mimeTypeObj = MimeType.valueOf(mimeType);
            Media media = new Media(mimeTypeObj, imageResource);
            
            // Create user message with text and image
            UserMessage userMessage = new UserMessage(prompt, List.of(media));
            
            // Create prompt and call Gemini
            Prompt geminiPrompt = new Prompt(List.of(userMessage));
            String response = chatModel.call(geminiPrompt).getResult().getOutput().getText();
            
            logger.debug("Received response from Gemini (length: {} chars)", 
                response != null ? response.length() : 0);
            
            return response;
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid base64 image data", e);
            throw new RuntimeException("Failed to decode base64 image: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error calling Gemini API with base64 image", e);
            throw new RuntimeException("Failed to generate response from Gemini: " + e.getMessage(), e);
        }
    }
    
    /**
     * Determine MIME type from file path.
     *
     * @param path The file path
     * @return MimeType object
     */
    private MimeType getMimeTypeObject(Path path) {
        String filename = path.getFileName().toString().toLowerCase();
        return getMimeTypeObjectFromFilename(filename);
    }
    
    /**
     * Determine MIME type from filename.
     *
     * @param filename The filename
     * @return MimeType object
     */
    private MimeType getMimeTypeObjectFromFilename(String filename) {
        if (filename.endsWith(".png")) {
            return MimeTypeUtils.IMAGE_PNG;
        } else if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
            return MimeTypeUtils.IMAGE_JPEG;
        } else if (filename.endsWith(".gif")) {
            return MimeTypeUtils.IMAGE_GIF;
        } else if (filename.endsWith(".webp")) {
            return MimeType.valueOf("image/webp");
        } else {
            // Default to JPEG
            logger.warn("Unknown image type for file: {}, defaulting to JPEG", filename);
            return MimeTypeUtils.IMAGE_JPEG;
        }
    }
}
