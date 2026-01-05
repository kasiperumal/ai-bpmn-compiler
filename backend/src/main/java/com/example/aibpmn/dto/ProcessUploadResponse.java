package com.example.aibpmn.dto;

/**
 * Response DTO for process image upload
 */
public class ProcessUploadResponse {
    
    private String processId;
    private String fileName;
    private String filePath;
    private long fileSize;
    private String status;
    private String message;
    
    public ProcessUploadResponse() {
    }
    
    public ProcessUploadResponse(String processId, String fileName, String filePath, long fileSize) {
        this.processId = processId;
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.status = "SUCCESS";
        this.message = "Image uploaded successfully";
    }
    
    // Getters and Setters
    
    public String getProcessId() {
        return processId;
    }
    
    public void setProcessId(String processId) {
        this.processId = processId;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}

