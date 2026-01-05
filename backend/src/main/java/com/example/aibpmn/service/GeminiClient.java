package com.example.aibpmn.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Client service for interacting with Google Gemini 2.5 Pro via Spring AI.
 * Supports both text-only prompts and image + prompt interactions.
 */
@Service
public class GeminiClient {
    
    private static final Logger logger = LoggerFactory.getLogger(GeminiClient.class);
    
    private final ChatClient chatClient;
    private final ChatModel chatModel;
    
    public GeminiClient(ChatModel chatModel) {
        this.chatModel = chatModel;
        this.chatClient = ChatClient.builder(chatModel).build();
        logger.info("GeminiClient initialized with Gemini 2.5 Pro");
    }
    
    /**
     * Send a text-only prompt to Gemini and get the response.
     *
     * @param prompt The text prompt to send
     * @return Raw text response from Gemini
     * @throws IllegalArgumentException if prompt is null or empty
     */
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
