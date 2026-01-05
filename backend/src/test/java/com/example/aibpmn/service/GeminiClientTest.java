package com.example.aibpmn.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GeminiClientTest {
    
    private ChatModel chatModel;
    private GeminiClient geminiClient;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        chatModel = mock(ChatModel.class);
        geminiClient = new GeminiClient(chatModel);
    }
    
    @Test
    void testGenerateFromText_Success() {
        // Arrange
        String prompt = "Explain BPMN process modeling";
        String expectedResponse = "BPMN is a graphical notation for modeling business processes...";
        
        ChatResponse chatResponse = createMockChatResponse(expectedResponse);
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
        
        // Act
        String response = geminiClient.generateFromText(prompt);
        
        // Assert
        assertNotNull(response);
        assertEquals(expectedResponse, response);
        verify(chatModel, times(1)).call(any(Prompt.class));
    }
    
    @Test
    void testGenerateFromText_NullPrompt() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> geminiClient.generateFromText(null));
    }
    
    @Test
    void testGenerateFromText_EmptyPrompt() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> geminiClient.generateFromText(""));
        
        assertThrows(IllegalArgumentException.class,
            () -> geminiClient.generateFromText("   "));
    }
    
    @Test
    void testGenerateFromText_ApiError() {
        // Arrange
        when(chatModel.call(any(Prompt.class)))
            .thenThrow(new RuntimeException("API rate limit exceeded"));
        
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> geminiClient.generateFromText("Test prompt"));
        
        assertTrue(exception.getMessage().contains("Failed to generate response from Gemini"));
    }
    
    @Test
    void testGenerateFromImage_WithPath_Success() throws Exception {
        // Arrange
        Path imagePath = tempDir.resolve("test-diagram.png");
        byte[] imageData = createTestImageData();
        Files.write(imagePath, imageData);
        
        String prompt = "Analyze this BPMN diagram";
        String expectedResponse = "This diagram shows a simple approval workflow...";
        
        ChatResponse chatResponse = createMockChatResponse(expectedResponse);
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
        
        // Act
        String response = geminiClient.generateFromImage(imagePath, prompt);
        
        // Assert
        assertNotNull(response);
        assertEquals(expectedResponse, response);
        verify(chatModel, times(1)).call(any(Prompt.class));
    }
    
    @Test
    void testGenerateFromImage_WithPath_NullPath() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> geminiClient.generateFromImage((Path) null, "Test prompt"));
    }
    
    @Test
    void testGenerateFromImage_WithPath_NonExistentFile() {
        // Arrange
        Path nonExistentPath = tempDir.resolve("nonexistent.png");
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> geminiClient.generateFromImage(nonExistentPath, "Test prompt"));
    }
    
    @Test
    void testGenerateFromImage_WithPath_NullPrompt() throws Exception {
        // Arrange
        Path imagePath = tempDir.resolve("test.png");
        Files.write(imagePath, createTestImageData());
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> geminiClient.generateFromImage(imagePath, null));
    }
    
    @Test
    void testGenerateFromImage_WithPath_EmptyPrompt() throws Exception {
        // Arrange
        Path imagePath = tempDir.resolve("test.png");
        Files.write(imagePath, createTestImageData());
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> geminiClient.generateFromImage(imagePath, ""));
    }
    
    @Test
    void testGenerateFromImage_WithResource_Success() throws Exception {
        // Arrange
        byte[] imageData = createTestImageData();
        Resource imageResource = new TestResource("test-diagram.png", imageData);
        
        String prompt = "What processes are shown in this image?";
        String expectedResponse = "The image shows three main processes...";
        
        ChatResponse chatResponse = createMockChatResponse(expectedResponse);
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
        
        // Act
        String response = geminiClient.generateFromImage(imageResource, prompt);
        
        // Assert
        assertNotNull(response);
        assertEquals(expectedResponse, response);
        verify(chatModel, times(1)).call(any(Prompt.class));
    }
    
    @Test
    void testGenerateFromImage_WithResource_NullResource() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> geminiClient.generateFromImage((Resource) null, "Test prompt"));
    }
    
    @Test
    void testGenerateFromImage_WithResource_NonExistentResource() {
        // Arrange
        Resource nonExistentResource = mock(Resource.class);
        when(nonExistentResource.exists()).thenReturn(false);
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> geminiClient.generateFromImage(nonExistentResource, "Test prompt"));
    }
    
    @Test
    void testGenerateFromImage_SupportsDifferentImageTypes() throws Exception {
        // Arrange
        String expectedResponse = "Image analyzed successfully";
        ChatResponse chatResponse = createMockChatResponse(expectedResponse);
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
        
        String[] imageTypes = {"test.png", "test.jpg", "test.jpeg", "test.gif", "test.webp"};
        
        for (String filename : imageTypes) {
            // Arrange
            Path imagePath = tempDir.resolve(filename);
            Files.write(imagePath, createTestImageData());
            
            // Act
            String response = geminiClient.generateFromImage(imagePath, "Analyze this");
            
            // Assert
            assertNotNull(response, "Response should not be null for " + filename);
            assertEquals(expectedResponse, response);
        }
        
        // Verify called once for each image type
        verify(chatModel, times(imageTypes.length)).call(any(Prompt.class));
    }
    
    @Test
    void testGenerateFromImage_UnknownImageType() throws Exception {
        // Arrange
        String expectedResponse = "Image analyzed";
        ChatResponse chatResponse = createMockChatResponse(expectedResponse);
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
        
        Path imagePath = tempDir.resolve("test.bmp"); // BMP not explicitly supported
        Files.write(imagePath, createTestImageData());
        
        // Act - should default to JPEG MIME type
        String response = geminiClient.generateFromImage(imagePath, "Analyze");
        
        // Assert
        assertNotNull(response);
        assertEquals(expectedResponse, response);
    }
    
    // Helper methods
    
    private ChatResponse createMockChatResponse(String content) {
        ChatResponse response = mock(ChatResponse.class);
        Generation generation = mock(Generation.class);
        AssistantMessage message = mock(AssistantMessage.class);
        
        when(message.getText()).thenReturn(content);
        when(generation.getOutput()).thenReturn(message);
        when(response.getResult()).thenReturn(generation);
        
        return response;
    }
    
    private byte[] createTestImageData() {
        // Create a minimal PNG header (not a valid image, but sufficient for testing)
        return new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, // PNG signature
            0x00, 0x00, 0x00, 0x0D, // IHDR chunk length
            0x49, 0x48, 0x44, 0x52, // IHDR
            0x00, 0x00, 0x00, 0x10, // Width: 16
            0x00, 0x00, 0x00, 0x10, // Height: 16
            0x08, 0x02, 0x00, 0x00, 0x00 // Bit depth, color type, etc.
        };
    }
    
    /**
     * Test implementation of Resource for testing purposes
     */
    private static class TestResource extends ByteArrayResource {
        private final String filename;
        
        public TestResource(String filename, byte[] byteArray) {
            super(byteArray);
            this.filename = filename;
        }
        
        @Override
        public String getFilename() {
            return filename;
        }
        
        @Override
        public boolean exists() {
            return true;
        }
    }
}

