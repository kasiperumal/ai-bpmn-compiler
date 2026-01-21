package com.example.aibpmn.service;

import com.example.aibpmn.dto.ProcessUploadResponse;
import com.example.aibpmn.exception.InvalidFileException;
import com.example.aibpmn.model.ProcessModel;
import com.example.aibpmn.model.ProcessStatus;
import com.example.aibpmn.repository.ProcessModelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProcessImageUploadServiceTest {
    
    private ProcessImageUploadService service;
    private ProcessModelRepository processModelRepository;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        processModelRepository = mock(ProcessModelRepository.class);
        service = new ProcessImageUploadService(processModelRepository);
        
        // Set upload base directory to temp directory
        ReflectionTestUtils.setField(service, "uploadBaseDir", tempDir.toString());
    }
    
    @Test
    void testUploadValidPngImage() {
        // Arrange
        byte[] content = "fake image content".getBytes();
        MultipartFile file = new MockMultipartFile(
            "file",
            "diagram.png",
            "image/png",
            content
        );
        
        when(processModelRepository.save(any(ProcessModel.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        ProcessUploadResponse response = service.uploadProcessImage(file);
        
        // Assert
        assertNotNull(response);
        assertNotNull(response.getProcessId());
        assertTrue(response.getProcessId().startsWith("proc-"));
        assertEquals("diagram.png", response.getFileName());
        assertEquals(content.length, response.getFileSize());
        assertEquals("SUCCESS", response.getStatus());
        
        // Verify file was stored
        assertTrue(Files.exists(Path.of(response.getFilePath())));
        
        // Verify process model was created
        ArgumentCaptor<ProcessModel> captor = ArgumentCaptor.forClass(ProcessModel.class);
        verify(processModelRepository).save(captor.capture());
        
        ProcessModel savedModel = captor.getValue();
        assertEquals(response.getProcessId(), savedModel.getId());
        assertEquals(ProcessStatus.DRAFT, savedModel.getStatus());
        assertEquals(1, savedModel.getVersion());
    }
    
    @Test
    void testUploadValidJpegImage() {
        // Arrange
        byte[] content = "fake jpeg content".getBytes();
        MultipartFile file = new MockMultipartFile(
            "file",
            "diagram.jpeg",
            "image/jpeg",
            content
        );
        
        when(processModelRepository.save(any(ProcessModel.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        ProcessUploadResponse response = service.uploadProcessImage(file);
        
        // Assert
        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertTrue(response.getFilePath().endsWith(".jpeg"));
    }
    
    @Test
    void testUploadValidJpgImage() {
        // Arrange
        byte[] content = "fake jpg content".getBytes();
        MultipartFile file = new MockMultipartFile(
            "file",
            "diagram.jpg",
            "image/jpg",
            content
        );
        
        when(processModelRepository.save(any(ProcessModel.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        ProcessUploadResponse response = service.uploadProcessImage(file);
        
        // Assert
        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
    }
    
    @Test
    void testUploadEmptyFile() {
        // Arrange
        MultipartFile file = new MockMultipartFile(
            "file",
            "empty.png",
            "image/png",
            new byte[0]
        );
        
        // Act & Assert
        InvalidFileException exception = assertThrows(
            InvalidFileException.class,
            () -> service.uploadProcessImage(file)
        );
        
        assertTrue(exception.getMessage().contains("empty"));
    }
    
    @Test
    void testUploadNullFile() {
        // Act & Assert
        assertThrows(InvalidFileException.class, () -> service.uploadProcessImage(null));
    }
    
    @Test
    void testUploadInvalidFileType() {
        // Arrange
        MultipartFile file = new MockMultipartFile(
            "file",
            "document.pdf",
            "application/pdf",
            "fake pdf content".getBytes()
        );
        
        // Act & Assert
        InvalidFileException exception = assertThrows(
            InvalidFileException.class,
            () -> service.uploadProcessImage(file)
        );
        
        assertTrue(exception.getMessage().toLowerCase().contains("invalid file type"));
    }
    
    @Test
    void testUploadFileTooLarge() {
        // Arrange - Create a file larger than 10MB
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11 MB
        MultipartFile file = new MockMultipartFile(
            "file",
            "large.png",
            "image/png",
            largeContent
        );
        
        // Act & Assert
        InvalidFileException exception = assertThrows(
            InvalidFileException.class,
            () -> service.uploadProcessImage(file)
        );
        
        assertTrue(exception.getMessage().toLowerCase().contains("size exceeds"));
    }
    
    @Test
    void testUploadInvalidFileExtension() {
        // Arrange
        MultipartFile file = new MockMultipartFile(
            "file",
            "diagram.txt",
            "image/png", // Content-type says PNG but extension is .txt
            "fake content".getBytes()
        );
        
        // Act & Assert
        InvalidFileException exception = assertThrows(
            InvalidFileException.class,
            () -> service.uploadProcessImage(file)
        );
        
        assertTrue(exception.getMessage().toLowerCase().contains("invalid file extension"));
    }
    
    @Test
    void testUploadFileWithoutExtension() {
        // Arrange
        MultipartFile file = new MockMultipartFile(
            "file",
            "diagram",
            "image/png",
            "fake content".getBytes()
        );
        
        // Act & Assert
        assertThrows(InvalidFileException.class, () -> service.uploadProcessImage(file));
    }
    
    @Test
    void testGenerateUniqueProcessIds() {
        // Arrange
        MultipartFile file1 = new MockMultipartFile(
            "file",
            "diagram1.png",
            "image/png",
            "content1".getBytes()
        );
        
        MultipartFile file2 = new MockMultipartFile(
            "file",
            "diagram2.png",
            "image/png",
            "content2".getBytes()
        );
        
        when(processModelRepository.save(any(ProcessModel.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        ProcessUploadResponse response1 = service.uploadProcessImage(file1);
        ProcessUploadResponse response2 = service.uploadProcessImage(file2);
        
        // Assert
        assertNotEquals(response1.getProcessId(), response2.getProcessId());
    }
}

