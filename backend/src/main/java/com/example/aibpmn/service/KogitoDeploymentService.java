package com.example.aibpmn.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for deploying generated BPMN and DRL files to Kogito runtime.
 * Handles writing files to the Kogito directories where they will be picked up and executed.
 */
@Service
public class KogitoDeploymentService {
    
    private static final Logger logger = LoggerFactory.getLogger(KogitoDeploymentService.class);
    
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    @Value("${app.kogito.bpmn-dir:./data/kogito/processes}")
    private String bpmnDirectory;
    
    @Value("${app.kogito.drl-dir:./data/kogito/rules}")
    private String drlDirectory;
    
    /**
     * Deploy a BPMN file to Kogito runtime.
     *
     * @param processId The process identifier
     * @param bpmnXml The BPMN 2.0 XML content
     * @return Path to the deployed file
     * @throws IOException if deployment fails
     */
    public Path deployBpmn(String processId, String bpmnXml) throws IOException {
        logger.info("Deploying BPMN for processId: {}", processId);
        
        if (bpmnXml == null || bpmnXml.trim().isEmpty()) {
            throw new IllegalArgumentException("BPMN XML cannot be null or empty");
        }
        
        // Create filename: processId.bpmn
        String filename = sanitizeFilename(processId) + ".bpmn";
        Path targetPath = Paths.get(bpmnDirectory, filename);
        
        // Write BPMN file
        Files.writeString(targetPath, bpmnXml, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        
        logger.info("BPMN deployed successfully to: {}", targetPath.toAbsolutePath());
        
        // Create backup
        createBackup(targetPath, "bpmn");
        
        return targetPath;
    }
    
    /**
     * Deploy a DRL file to Kogito runtime.
     *
     * @param processId The process identifier
     * @param drlContent The DRL content
     * @return Path to the deployed file
     * @throws IOException if deployment fails
     */
    public Path deployDrl(String processId, String drlContent) throws IOException {
        logger.info("Deploying DRL for processId: {}", processId);
        
        if (drlContent == null || drlContent.trim().isEmpty()) {
            throw new IllegalArgumentException("DRL content cannot be null or empty");
        }
        
        // Create filename: processId.drl
        String filename = sanitizeFilename(processId) + ".drl";
        Path targetPath = Paths.get(drlDirectory, filename);
        
        // Write DRL file
        Files.writeString(targetPath, drlContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        
        logger.info("DRL deployed successfully to: {}", targetPath.toAbsolutePath());
        
        // Create backup
        createBackup(targetPath, "drl");
        
        return targetPath;
    }
    
    /**
     * Deploy both BPMN and DRL for a process.
     *
     * @param processId The process identifier
     * @param bpmnXml The BPMN 2.0 XML content
     * @param drlContent The DRL content
     * @return DeploymentResult with paths to both files
     * @throws IOException if deployment fails
     */
    public DeploymentResult deployProcess(String processId, String bpmnXml, String drlContent) throws IOException {
        logger.info("Deploying complete process for processId: {}", processId);
        
        Path bpmnPath = deployBpmn(processId, bpmnXml);
        Path drlPath = deployDrl(processId, drlContent);
        
        logger.info("Process deployment complete for processId: {}", processId);
        
        return new DeploymentResult(processId, bpmnPath, drlPath);
    }
    
    /**
     * Undeploy a process by removing its BPMN and DRL files.
     *
     * @param processId The process identifier
     * @return true if files were deleted, false if they didn't exist
     * @throws IOException if deletion fails
     */
    public boolean undeployProcess(String processId) throws IOException {
        logger.info("Undeploying process: {}", processId);
        
        String bpmnFilename = sanitizeFilename(processId) + ".bpmn";
        String drlFilename = sanitizeFilename(processId) + ".drl";
        
        Path bpmnPath = Paths.get(bpmnDirectory, bpmnFilename);
        Path drlPath = Paths.get(drlDirectory, drlFilename);
        
        boolean bpmnDeleted = false;
        boolean drlDeleted = false;
        
        if (Files.exists(bpmnPath)) {
            Files.delete(bpmnPath);
            bpmnDeleted = true;
            logger.info("Deleted BPMN file: {}", bpmnPath);
        }
        
        if (Files.exists(drlPath)) {
            Files.delete(drlPath);
            drlDeleted = true;
            logger.info("Deleted DRL file: {}", drlPath);
        }
        
        boolean undeployed = bpmnDeleted || drlDeleted;
        
        if (undeployed) {
            logger.info("Process undeployed: {}", processId);
        } else {
            logger.warn("No files found to undeploy for processId: {}", processId);
        }
        
        return undeployed;
    }
    
    /**
     * Check if a process is deployed.
     *
     * @param processId The process identifier
     * @return true if BPMN file exists, false otherwise
     */
    public boolean isDeployed(String processId) {
        String bpmnFilename = sanitizeFilename(processId) + ".bpmn";
        Path bpmnPath = Paths.get(bpmnDirectory, bpmnFilename);
        return Files.exists(bpmnPath);
    }
    
    /**
     * List all deployed processes.
     *
     * @return List of process IDs
     * @throws IOException if listing fails
     */
    public List<String> listDeployedProcesses() throws IOException {
        List<String> processIds = new ArrayList<>();
        
        Path bpmnDir = Paths.get(bpmnDirectory);
        if (Files.exists(bpmnDir)) {
            Files.list(bpmnDir)
                .filter(path -> path.toString().endsWith(".bpmn"))
                .forEach(path -> {
                    String filename = path.getFileName().toString();
                    String processId = filename.substring(0, filename.lastIndexOf(".bpmn"));
                    processIds.add(processId);
                });
        }
        
        logger.debug("Found {} deployed processes", processIds.size());
        
        return processIds;
    }
    
    /**
     * Get deployment information for a process.
     *
     * @param processId The process identifier
     * @return DeploymentInfo or null if not deployed
     */
    public DeploymentInfo getDeploymentInfo(String processId) {
        String bpmnFilename = sanitizeFilename(processId) + ".bpmn";
        String drlFilename = sanitizeFilename(processId) + ".drl";
        
        Path bpmnPath = Paths.get(bpmnDirectory, bpmnFilename);
        Path drlPath = Paths.get(drlDirectory, drlFilename);
        
        if (!Files.exists(bpmnPath)) {
            return null;
        }
        
        try {
            long bpmnSize = Files.size(bpmnPath);
            LocalDateTime bpmnModified = LocalDateTime.ofInstant(
                Files.getLastModifiedTime(bpmnPath).toInstant(),
                java.time.ZoneId.systemDefault()
            );
            
            Long drlSize = null;
            LocalDateTime drlModified = null;
            
            if (Files.exists(drlPath)) {
                drlSize = Files.size(drlPath);
                drlModified = LocalDateTime.ofInstant(
                    Files.getLastModifiedTime(drlPath).toInstant(),
                    java.time.ZoneId.systemDefault()
                );
            }
            
            return new DeploymentInfo(
                processId,
                bpmnPath,
                drlPath,
                bpmnSize,
                drlSize,
                bpmnModified,
                drlModified
            );
            
        } catch (IOException e) {
            logger.error("Error getting deployment info for processId {}: {}", processId, e.getMessage());
            return null;
        }
    }
    
    /**
     * Sanitize filename by removing invalid characters.
     */
    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
    
    /**
     * Create a backup of a deployed file.
     */
    private void createBackup(Path originalFile, String type) {
        try {
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
            String backupFilename = originalFile.getFileName().toString() + "." + timestamp + ".backup";
            Path backupDir = originalFile.getParent().resolve("backups");
            
            if (!Files.exists(backupDir)) {
                Files.createDirectories(backupDir);
            }
            
            Path backupPath = backupDir.resolve(backupFilename);
            Files.copy(originalFile, backupPath, StandardCopyOption.REPLACE_EXISTING);
            
            logger.debug("Created {} backup: {}", type, backupPath);
            
        } catch (IOException e) {
            logger.warn("Failed to create backup for {}: {}", originalFile, e.getMessage());
        }
    }
    
    /**
     * Result of a deployment operation.
     */
    public static class DeploymentResult {
        private final String processId;
        private final Path bpmnPath;
        private final Path drlPath;
        
        public DeploymentResult(String processId, Path bpmnPath, Path drlPath) {
            this.processId = processId;
            this.bpmnPath = bpmnPath;
            this.drlPath = drlPath;
        }
        
        public String getProcessId() {
            return processId;
        }
        
        public Path getBpmnPath() {
            return bpmnPath;
        }
        
        public Path getDrlPath() {
            return drlPath;
        }
        
        @Override
        public String toString() {
            return "DeploymentResult{" +
                    "processId='" + processId + '\'' +
                    ", bpmnPath=" + bpmnPath +
                    ", drlPath=" + drlPath +
                    '}';
        }
    }
    
    /**
     * Information about a deployed process.
     */
    public static class DeploymentInfo {
        private final String processId;
        private final Path bpmnPath;
        private final Path drlPath;
        private final long bpmnSize;
        private final Long drlSize;
        private final LocalDateTime bpmnLastModified;
        private final LocalDateTime drlLastModified;
        
        public DeploymentInfo(String processId, Path bpmnPath, Path drlPath,
                            long bpmnSize, Long drlSize,
                            LocalDateTime bpmnLastModified, LocalDateTime drlLastModified) {
            this.processId = processId;
            this.bpmnPath = bpmnPath;
            this.drlPath = drlPath;
            this.bpmnSize = bpmnSize;
            this.drlSize = drlSize;
            this.bpmnLastModified = bpmnLastModified;
            this.drlLastModified = drlLastModified;
        }
        
        public String getProcessId() {
            return processId;
        }
        
        public Path getBpmnPath() {
            return bpmnPath;
        }
        
        public Path getDrlPath() {
            return drlPath;
        }
        
        public long getBpmnSize() {
            return bpmnSize;
        }
        
        public Long getDrlSize() {
            return drlSize;
        }
        
        public LocalDateTime getBpmnLastModified() {
            return bpmnLastModified;
        }
        
        public LocalDateTime getDrlLastModified() {
            return drlLastModified;
        }
        
        public boolean hasDrl() {
            return drlSize != null;
        }
        
        @Override
        public String toString() {
            return "DeploymentInfo{" +
                    "processId='" + processId + '\'' +
                    ", bpmnSize=" + bpmnSize +
                    ", drlSize=" + drlSize +
                    ", bpmnLastModified=" + bpmnLastModified +
                    ", drlLastModified=" + drlLastModified +
                    '}';
        }
    }
}

