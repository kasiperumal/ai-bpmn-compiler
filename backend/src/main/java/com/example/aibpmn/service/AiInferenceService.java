package com.example.aibpmn.service;

import com.example.aibpmn.model.ProcessModel;
import com.example.aibpmn.repository.ProcessModelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Service for AI-powered process inference operations.
 * Uses configured AI provider (OpenAI GPT-4o or Google Gemini 2.0) to analyze images and text.
 */
@Service
public class AiInferenceService {
    
    private static final Logger logger = LoggerFactory.getLogger(AiInferenceService.class);
    
    private final AiClient aiClient;
    private final ProcessModelRepository processModelRepository;
    private final String uploadBaseDir;
    
    public AiInferenceService(
            AiClient aiClient,
            ProcessModelRepository processModelRepository,
            @Value("${app.upload.base-dir:./data/uploads}") String uploadBaseDir) {
        this.aiClient = aiClient;
        this.processModelRepository = processModelRepository;
        this.uploadBaseDir = uploadBaseDir;
        logger.info("AiInferenceService initialized with AI provider: {}", aiClient.getProviderName());
    }
    
    /**
     * Infer a process description from an uploaded image using AI.
     * 
     * This method analyzes a process diagram image and generates a structured
     * description of the workflow, focusing on business logic rather than
     * technical BPMN terminology.
     *
     * @param processId The process identifier
     * @return Structured text description of the process
     * @throws IllegalArgumentException if process not found or image not found
     * @throws RuntimeException if AI inference fails
     */
    public String inferProcessDescriptionFromImage(String processId) {
        logger.info("Starting process description inference for processId: {}", processId);
        
        // 1. Validate process exists
        ProcessModel process = processModelRepository.findById(processId)
            .orElseThrow(() -> new IllegalArgumentException("Process not found: " + processId));
        
        // 2. Locate the image file
        Path imagePath = findProcessImage(processId);
        if (imagePath == null || !Files.exists(imagePath)) {
            throw new IllegalArgumentException(
                String.format("Image file not found for process: %s", processId)
            );
        }
        
        logger.debug("Found image at: {}", imagePath);
        
        // 3. Create detailed prompt for Gemini
        String prompt = createInferencePrompt();
        
        // 4. Call Gemini to analyze the image
        try {
            String description = aiClient.generateFromImage(imagePath, prompt);
            
            logger.info("Successfully inferred process description for processId: {} (length: {} chars)",
                processId, description.length());
            
            return description;
            
        } catch (Exception e) {
            logger.error("Failed to infer process description for processId: {}", processId, e);
            throw new RuntimeException(
                "Failed to infer process description: " + e.getMessage(), e
            );
        }
    }
    
    /**
     * Create a detailed prompt for Gemini to analyze the process diagram.
     * 
     * The prompt instructs the AI to:
     * - Describe steps, decisions, and conditions in business terms
     * - Avoid BPMN technical terminology
     * - Call out ambiguities explicitly
     * - Return structured, well-formatted text
     *
     * @return The prompt string
     */
    private String createInferencePrompt() {
        return """
            Analyze this process diagram and provide a detailed description of the workflow.
            
            IMPORTANT GUIDELINES:
            
            1. USE BUSINESS LANGUAGE, NOT TECHNICAL BPMN TERMS:
               - Instead of "Start Event", say "The process begins when..."
               - Instead of "Task" or "Activity", say "Step" or "Action"
               - Instead of "Gateway", describe the decision or branching point
               - Instead of "End Event", say "The process completes when..."
               - Instead of "Sequence Flow", describe how steps connect
            
            2. DESCRIBE THE FOLLOWING IN DETAIL:
               - STEPS: What actions or activities are performed? Who performs them?
               - DECISIONS: What choices or branching points exist? What are the conditions?
               - CONDITIONS: Under what circumstances does each path execute?
               - FLOW: How do steps connect? What happens in sequence vs. parallel?
               - ROLES: If visible, what roles or actors are involved?
               - DATA: What information is needed or produced at each step?
            
            3. CALL OUT AMBIGUITIES EXPLICITLY:
               - If the diagram is unclear or incomplete, say so
               - If labels are missing or hard to read, mention it
               - If the flow logic is ambiguous, describe the uncertainty
               - If there are multiple possible interpretations, list them
               - Use phrases like "It appears that..." or "This step seems to..."
            
            4. STRUCTURE YOUR RESPONSE AS FOLLOWS:
            
            ## Overview
            [Brief 2-3 sentence summary of what this process does]
            
            ## Main Flow
            [Describe the primary/happy path through the process, step by step]
            
            ## Decision Points
            [List each decision point, the conditions, and the resulting paths]
            
            ## Alternative Paths
            [Describe any alternative or exception flows]
            
            ## Parallel Activities
            [If applicable, describe any steps that happen simultaneously]
            
            ## Process Completion
            [Describe the different ways the process can end]
            
            ## Ambiguities and Uncertainties
            [List any unclear or ambiguous aspects of the diagram]
            
            ## Additional Observations
            [Any other relevant details: roles, data, timing, etc.]
            
            REMEMBER: Write for a business audience, not technical BPMN experts.
            Be clear, specific, and honest about what you can and cannot determine from the diagram.
            """;
    }
    
    /**
     * Find the image file for a given process.
     * 
     * Looks for common image extensions in the process upload directory.
     *
     * @param processId The process identifier
     * @return Path to the image file, or null if not found
     */
    private Path findProcessImage(String processId) {
        String processDir = String.format("%s/%s", uploadBaseDir, processId);
        
        // Try common image file names and extensions
        String[] possibleFiles = {
            "original.png",
            "original.jpg",
            "original.jpeg",
            "original.gif",
            "original.webp"
        };
        
        for (String filename : possibleFiles) {
            Path imagePath = Paths.get(processDir, filename);
            if (Files.exists(imagePath)) {
                logger.debug("Found image: {}", imagePath);
                return imagePath;
            }
        }
        
        logger.warn("No image file found for processId: {} in directory: {}", processId, processDir);
        return null;
    }
    
    /**
     * Check if a process has an associated image file.
     *
     * @param processId The process identifier
     * @return true if image exists, false otherwise
     */
    public boolean hasProcessImage(String processId) {
        Path imagePath = findProcessImage(processId);
        return imagePath != null && Files.exists(imagePath);
    }
}

