package com.example.aibpmn.repository;

import com.example.aibpmn.model.Explanation;
import com.example.aibpmn.repository.impl.InMemoryExplanationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ExplanationRepositoryTest {
    
    private ExplanationRepository repository;
    
    @BeforeEach
    void setUp() {
        repository = new InMemoryExplanationRepository();
    }
    
    @Test
    void testSaveAndFindByNodeId() {
        Explanation explanation = new Explanation("node-001", "This node validates the input");
        explanation.setSource("AI-Generated");
        explanation.setConfidenceScore(0.95);
        
        Explanation saved = repository.save(explanation);
        
        assertNotNull(saved);
        assertEquals("node-001", saved.getNodeId());
        
        Optional<Explanation> found = repository.findByNodeId("node-001");
        assertTrue(found.isPresent());
        assertEquals("This node validates the input", found.get().getReason());
        assertEquals(0.95, found.get().getConfidenceScore());
    }
    
    @Test
    void testSaveNullExplanation() {
        assertThrows(IllegalArgumentException.class, () -> repository.save(null));
    }
    
    @Test
    void testFindAll() {
        repository.save(new Explanation("node-001", "Reason 1"));
        repository.save(new Explanation("node-002", "Reason 2"));
        repository.save(new Explanation("node-003", "Reason 3"));
        
        List<Explanation> all = repository.findAll();
        assertEquals(3, all.size());
    }
    
    @Test
    void testFindBySource() {
        Explanation ai1 = new Explanation("node-001", "AI generated reason 1");
        ai1.setSource("AI-Generated");
        
        Explanation ai2 = new Explanation("node-002", "AI generated reason 2");
        ai2.setSource("AI-Generated");
        
        Explanation user = new Explanation("node-003", "User defined reason");
        user.setSource("User-Defined");
        
        repository.save(ai1);
        repository.save(ai2);
        repository.save(user);
        
        List<Explanation> aiGenerated = repository.findBySource("AI-Generated");
        assertEquals(2, aiGenerated.size());
        
        List<Explanation> userDefined = repository.findBySource("User-Defined");
        assertEquals(1, userDefined.size());
    }
    
    @Test
    void testFindByConfidenceScoreGreaterThan() {
        Explanation high1 = new Explanation("node-001", "High confidence");
        high1.setConfidenceScore(0.95);
        
        Explanation high2 = new Explanation("node-002", "Also high");
        high2.setConfidenceScore(0.92);
        
        Explanation low = new Explanation("node-003", "Low confidence");
        low.setConfidenceScore(0.65);
        
        repository.save(high1);
        repository.save(high2);
        repository.save(low);
        
        List<Explanation> highConfidence = repository.findByConfidenceScoreGreaterThan(0.90);
        assertEquals(2, highConfidence.size());
        
        List<Explanation> allAboveThreshold = repository.findByConfidenceScoreGreaterThan(0.60);
        assertEquals(3, allAboveThreshold.size());
    }
    
    @Test
    void testFindByReasonContaining() {
        repository.save(new Explanation("node-001", "This validates the order"));
        repository.save(new Explanation("node-002", "This processes the payment"));
        repository.save(new Explanation("node-003", "Validation step for input"));
        
        List<Explanation> withValidate = repository.findByReasonContaining("validat");
        assertEquals(2, withValidate.size());
        
        List<Explanation> withPayment = repository.findByReasonContaining("payment");
        assertEquals(1, withPayment.size());
    }
    
    @Test
    void testExistsByNodeId() {
        repository.save(new Explanation("node-001", "Test reason"));
        
        assertTrue(repository.existsByNodeId("node-001"));
        assertFalse(repository.existsByNodeId("node-999"));
    }
    
    @Test
    void testDeleteByNodeId() {
        repository.save(new Explanation("node-001", "Test reason"));
        
        assertTrue(repository.existsByNodeId("node-001"));
        
        boolean deleted = repository.deleteByNodeId("node-001");
        assertTrue(deleted);
        assertFalse(repository.existsByNodeId("node-001"));
        
        boolean deletedAgain = repository.deleteByNodeId("node-001");
        assertFalse(deletedAgain);
    }
    
    @Test
    void testDeleteAll() {
        repository.save(new Explanation("node-001", "Reason 1"));
        repository.save(new Explanation("node-002", "Reason 2"));
        
        assertEquals(2, repository.count());
        
        repository.deleteAll();
        
        assertEquals(0, repository.count());
        assertTrue(repository.findAll().isEmpty());
    }
    
    @Test
    void testCount() {
        assertEquals(0, repository.count());
        
        repository.save(new Explanation("node-001", "Reason 1"));
        assertEquals(1, repository.count());
        
        repository.save(new Explanation("node-002", "Reason 2"));
        assertEquals(2, repository.count());
        
        repository.deleteByNodeId("node-001");
        assertEquals(1, repository.count());
    }
}

