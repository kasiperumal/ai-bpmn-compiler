package com.example.aibpmn.repository;

import com.example.aibpmn.model.Approval;
import com.example.aibpmn.repository.impl.InMemoryApprovalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ApprovalRepositoryTest {
    
    private ApprovalRepository repository;
    
    @BeforeEach
    void setUp() {
        repository = new InMemoryApprovalRepository();
    }
    
    @Test
    void testSaveAndFindByNodeId() {
        Approval approval = new Approval("node-001", true, false);
        
        Approval saved = repository.save(approval);
        
        assertNotNull(saved);
        assertEquals("node-001", saved.getNodeId());
        
        Optional<Approval> found = repository.findByNodeId("node-001");
        assertTrue(found.isPresent());
        assertTrue(found.get().getAiApproved());
        assertFalse(found.get().getUserApproved());
    }
    
    @Test
    void testSaveNullApproval() {
        assertThrows(IllegalArgumentException.class, () -> repository.save(null));
    }
    
    @Test
    void testFindAll() {
        repository.save(new Approval("node-001", true, false));
        repository.save(new Approval("node-002", false, true));
        repository.save(new Approval("node-003", true, true));
        
        List<Approval> all = repository.findAll();
        assertEquals(3, all.size());
    }
    
    @Test
    void testFindByAiApproved() {
        repository.save(new Approval("node-001", true, false));
        repository.save(new Approval("node-002", true, true));
        repository.save(new Approval("node-003", false, false));
        
        List<Approval> aiApproved = repository.findByAiApproved(true);
        assertEquals(2, aiApproved.size());
        
        List<Approval> aiNotApproved = repository.findByAiApproved(false);
        assertEquals(1, aiNotApproved.size());
    }
    
    @Test
    void testFindByUserApproved() {
        repository.save(new Approval("node-001", true, false));
        repository.save(new Approval("node-002", true, true));
        repository.save(new Approval("node-003", false, true));
        
        List<Approval> userApproved = repository.findByUserApproved(true);
        assertEquals(2, userApproved.size());
    }
    
    @Test
    void testFindFullyApproved() {
        repository.save(new Approval("node-001", true, false));
        repository.save(new Approval("node-002", true, true));
        repository.save(new Approval("node-003", false, true));
        repository.save(new Approval("node-004", true, true));
        
        List<Approval> fullyApproved = repository.findFullyApproved();
        assertEquals(2, fullyApproved.size());
    }
    
    @Test
    void testFindPendingApproval() {
        repository.save(new Approval("node-001", true, false));
        repository.save(new Approval("node-002", true, true));
        repository.save(new Approval("node-003", false, false));
        
        List<Approval> pending = repository.findPendingApproval();
        assertEquals(2, pending.size());
    }
    
    @Test
    void testFindByApprovedBy() {
        Approval approval1 = new Approval("node-001", true, true);
        approval1.setApprovedBy("john.doe@example.com");
        
        Approval approval2 = new Approval("node-002", true, true);
        approval2.setApprovedBy("john.doe@example.com");
        
        Approval approval3 = new Approval("node-003", true, true);
        approval3.setApprovedBy("jane.smith@example.com");
        
        repository.save(approval1);
        repository.save(approval2);
        repository.save(approval3);
        
        List<Approval> johnApprovals = repository.findByApprovedBy("john.doe@example.com");
        assertEquals(2, johnApprovals.size());
        
        List<Approval> janeApprovals = repository.findByApprovedBy("jane.smith@example.com");
        assertEquals(1, janeApprovals.size());
    }
    
    @Test
    void testExistsByNodeId() {
        repository.save(new Approval("node-001", true, false));
        
        assertTrue(repository.existsByNodeId("node-001"));
        assertFalse(repository.existsByNodeId("node-999"));
    }
    
    @Test
    void testDeleteByNodeId() {
        repository.save(new Approval("node-001", true, false));
        
        assertTrue(repository.existsByNodeId("node-001"));
        
        boolean deleted = repository.deleteByNodeId("node-001");
        assertTrue(deleted);
        assertFalse(repository.existsByNodeId("node-001"));
        
        boolean deletedAgain = repository.deleteByNodeId("node-001");
        assertFalse(deletedAgain);
    }
    
    @Test
    void testDeleteAll() {
        repository.save(new Approval("node-001", true, false));
        repository.save(new Approval("node-002", false, true));
        
        assertEquals(2, repository.count());
        
        repository.deleteAll();
        
        assertEquals(0, repository.count());
        assertTrue(repository.findAll().isEmpty());
    }
    
    @Test
    void testCount() {
        assertEquals(0, repository.count());
        
        repository.save(new Approval("node-001", true, false));
        assertEquals(1, repository.count());
        
        repository.save(new Approval("node-002", false, true));
        assertEquals(2, repository.count());
        
        repository.deleteByNodeId("node-001");
        assertEquals(1, repository.count());
    }
}

