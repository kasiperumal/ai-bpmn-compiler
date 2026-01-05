package com.example.aibpmn.repository;

import com.example.aibpmn.model.ProcessModel;
import com.example.aibpmn.model.ProcessStatus;
import com.example.aibpmn.repository.impl.InMemoryProcessModelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ProcessModelRepositoryTest {
    
    private ProcessModelRepository repository;
    
    @BeforeEach
    void setUp() {
        repository = new InMemoryProcessModelRepository();
    }
    
    @Test
    void testSaveAndFindById() {
        ProcessModel process = new ProcessModel("proc-001", "Test Process", "1.0.0");
        
        ProcessModel saved = repository.save(process);
        
        assertNotNull(saved);
        assertEquals("proc-001", saved.getId());
        
        Optional<ProcessModel> found = repository.findById("proc-001");
        assertTrue(found.isPresent());
        assertEquals("Test Process", found.get().getName());
    }
    
    @Test
    void testSaveNullProcess() {
        assertThrows(IllegalArgumentException.class, () -> repository.save(null));
    }
    
    @Test
    void testFindByIdAndVersion() {
        ProcessModel process = new ProcessModel("proc-001", "Test", "1.0.0");
        repository.save(process);
        
        Optional<ProcessModel> found = repository.findByIdAndVersion("proc-001", "1.0.0");
        assertTrue(found.isPresent());
        
        Optional<ProcessModel> notFound = repository.findByIdAndVersion("proc-001", "2.0.0");
        assertFalse(notFound.isPresent());
    }
    
    @Test
    void testFindAll() {
        repository.save(new ProcessModel("proc-001", "Process 1", "1.0.0"));
        repository.save(new ProcessModel("proc-002", "Process 2", "1.0.0"));
        repository.save(new ProcessModel("proc-003", "Process 3", "1.0.0"));
        
        List<ProcessModel> all = repository.findAll();
        assertEquals(3, all.size());
    }
    
    @Test
    void testFindByStatus() {
        ProcessModel draft1 = new ProcessModel("proc-001", "Draft 1", "1.0.0");
        draft1.setStatus(ProcessStatus.DRAFT);
        
        ProcessModel draft2 = new ProcessModel("proc-002", "Draft 2", "1.0.0");
        draft2.setStatus(ProcessStatus.DRAFT);
        
        ProcessModel published = new ProcessModel("proc-003", "Published", "1.0.0");
        published.setStatus(ProcessStatus.PUBLISHED);
        
        repository.save(draft1);
        repository.save(draft2);
        repository.save(published);
        
        List<ProcessModel> drafts = repository.findByStatus(ProcessStatus.DRAFT);
        assertEquals(2, drafts.size());
        
        List<ProcessModel> publishedList = repository.findByStatus(ProcessStatus.PUBLISHED);
        assertEquals(1, publishedList.size());
    }
    
    @Test
    void testFindByNameContaining() {
        repository.save(new ProcessModel("proc-001", "Order Processing", "1.0.0"));
        repository.save(new ProcessModel("proc-002", "Invoice Processing", "1.0.0"));
        repository.save(new ProcessModel("proc-003", "Shipping Workflow", "1.0.0"));
        
        List<ProcessModel> results = repository.findByNameContaining("Processing");
        assertEquals(2, results.size());
        
        List<ProcessModel> caseInsensitive = repository.findByNameContaining("processing");
        assertEquals(2, caseInsensitive.size());
    }
    
    @Test
    void testExistsById() {
        ProcessModel process = new ProcessModel("proc-001", "Test", "1.0.0");
        repository.save(process);
        
        assertTrue(repository.existsById("proc-001"));
        assertFalse(repository.existsById("proc-999"));
    }
    
    @Test
    void testDeleteById() {
        ProcessModel process = new ProcessModel("proc-001", "Test", "1.0.0");
        repository.save(process);
        
        assertTrue(repository.existsById("proc-001"));
        
        boolean deleted = repository.deleteById("proc-001");
        assertTrue(deleted);
        assertFalse(repository.existsById("proc-001"));
        
        boolean deletedAgain = repository.deleteById("proc-001");
        assertFalse(deletedAgain);
    }
    
    @Test
    void testDeleteAll() {
        repository.save(new ProcessModel("proc-001", "Process 1", "1.0.0"));
        repository.save(new ProcessModel("proc-002", "Process 2", "1.0.0"));
        
        assertEquals(2, repository.count());
        
        repository.deleteAll();
        
        assertEquals(0, repository.count());
        assertTrue(repository.findAll().isEmpty());
    }
    
    @Test
    void testCount() {
        assertEquals(0, repository.count());
        
        repository.save(new ProcessModel("proc-001", "Process 1", "1.0.0"));
        assertEquals(1, repository.count());
        
        repository.save(new ProcessModel("proc-002", "Process 2", "1.0.0"));
        assertEquals(2, repository.count());
        
        repository.deleteById("proc-001");
        assertEquals(1, repository.count());
    }
    
    @Test
    void testUpdateExistingProcess() {
        ProcessModel process = new ProcessModel("proc-001", "Original Name", "1.0.0");
        repository.save(process);
        
        ProcessModel updated = new ProcessModel("proc-001", "Updated Name", "1.0.0");
        updated.setStatus(ProcessStatus.PUBLISHED);
        repository.save(updated);
        
        Optional<ProcessModel> found = repository.findById("proc-001");
        assertTrue(found.isPresent());
        assertEquals("Updated Name", found.get().getName());
        assertEquals(ProcessStatus.PUBLISHED, found.get().getStatus());
    }
}

