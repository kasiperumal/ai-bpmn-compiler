package com.example.aibpmn.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration class for Kogito BPMN and Drools integration.
 * Sets up directories for dynamic process and rule loading.
 */
@Configuration
public class KogitoConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(KogitoConfiguration.class);
    
    @Value("${app.kogito.bpmn-dir:./data/kogito/processes}")
    private String bpmnDirectory;
    
    @Value("${app.kogito.drl-dir:./data/kogito/rules}")
    private String drlDirectory;
    
    /**
     * Initialize Kogito directories on startup.
     * Creates the necessary directory structure for BPMN and DRL files.
     */
    @PostConstruct
    public void initializeKogitoDirectories() {
        logger.info("Initializing Kogito directories...");
        
        try {
            // Create BPMN directory
            Path bpmnPath = Paths.get(bpmnDirectory);
            if (!Files.exists(bpmnPath)) {
                Files.createDirectories(bpmnPath);
                logger.info("Created BPMN directory: {}", bpmnPath.toAbsolutePath());
            } else {
                logger.info("BPMN directory already exists: {}", bpmnPath.toAbsolutePath());
            }
            
            // Create DRL directory
            Path drlPath = Paths.get(drlDirectory);
            if (!Files.exists(drlPath)) {
                Files.createDirectories(drlPath);
                logger.info("Created DRL directory: {}", drlPath.toAbsolutePath());
            } else {
                logger.info("DRL directory already exists: {}", drlPath.toAbsolutePath());
            }
            
            // Log existing files
            logExistingFiles(bpmnPath, "BPMN");
            logExistingFiles(drlPath, "DRL");
            
            logger.info("Kogito directories initialized successfully");
            
        } catch (Exception e) {
            logger.error("Failed to initialize Kogito directories", e);
            throw new RuntimeException("Failed to initialize Kogito directories", e);
        }
    }
    
    /**
     * Log existing files in a directory.
     */
    private void logExistingFiles(Path directory, String type) {
        try {
            File dir = directory.toFile();
            File[] files = dir.listFiles();
            
            if (files != null && files.length > 0) {
                logger.info("Found {} {} files in {}", files.length, type, directory);
                for (File file : files) {
                    logger.debug("  - {}", file.getName());
                }
            } else {
                logger.info("No {} files found in {}", type, directory);
            }
        } catch (Exception e) {
            logger.warn("Error listing {} files: {}", type, e.getMessage());
        }
    }
    
    /**
     * Get the BPMN directory path.
     */
    @Bean
    public String bpmnDirectoryPath() {
        return bpmnDirectory;
    }
    
    /**
     * Get the DRL directory path.
     */
    @Bean
    public String drlDirectoryPath() {
        return drlDirectory;
    }
}

