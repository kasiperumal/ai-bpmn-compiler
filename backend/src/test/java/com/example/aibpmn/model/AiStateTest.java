package com.example.aibpmn.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AiStateTest {
    
    @Test
    void testAllStatesExist() {
        // Verify all expected states are defined
        AiState[] states = AiState.values();
        assertEquals(9, states.length);
        
        // Verify each state exists
        assertNotNull(AiState.IMAGE_RECEIVED);
        assertNotNull(AiState.TEXT_RECEIVED);
        assertNotNull(AiState.PROCESS_INFERRED);
        assertNotNull(AiState.CLARIFICATION_REQUIRED);
        assertNotNull(AiState.MODEL_READY);
        assertNotNull(AiState.BPMN_GENERATED);
        assertNotNull(AiState.DRL_GENERATED);
        assertNotNull(AiState.PUBLISHED);
        assertNotNull(AiState.FAILED);
    }
    
    @Test
    void testIsInitialState() {
        // Initial states
        assertTrue(AiState.IMAGE_RECEIVED.isInitialState());
        assertTrue(AiState.TEXT_RECEIVED.isInitialState());
        
        // Non-initial states
        assertFalse(AiState.PROCESS_INFERRED.isInitialState());
        assertFalse(AiState.CLARIFICATION_REQUIRED.isInitialState());
        assertFalse(AiState.MODEL_READY.isInitialState());
        assertFalse(AiState.BPMN_GENERATED.isInitialState());
        assertFalse(AiState.DRL_GENERATED.isInitialState());
        assertFalse(AiState.PUBLISHED.isInitialState());
        assertFalse(AiState.FAILED.isInitialState());
    }
    
    @Test
    void testIsTerminalState() {
        // Terminal states
        assertTrue(AiState.PUBLISHED.isTerminalState());
        assertTrue(AiState.FAILED.isTerminalState());
        
        // Non-terminal states
        assertFalse(AiState.IMAGE_RECEIVED.isTerminalState());
        assertFalse(AiState.TEXT_RECEIVED.isTerminalState());
        assertFalse(AiState.PROCESS_INFERRED.isTerminalState());
        assertFalse(AiState.CLARIFICATION_REQUIRED.isTerminalState());
        assertFalse(AiState.MODEL_READY.isTerminalState());
        assertFalse(AiState.BPMN_GENERATED.isTerminalState());
        assertFalse(AiState.DRL_GENERATED.isTerminalState());
    }
    
    @Test
    void testRequiresUserAction() {
        // States requiring user action
        assertTrue(AiState.CLARIFICATION_REQUIRED.requiresUserAction());
        assertTrue(AiState.MODEL_READY.requiresUserAction());
        
        // States not requiring user action
        assertFalse(AiState.IMAGE_RECEIVED.requiresUserAction());
        assertFalse(AiState.TEXT_RECEIVED.requiresUserAction());
        assertFalse(AiState.PROCESS_INFERRED.requiresUserAction());
        assertFalse(AiState.BPMN_GENERATED.requiresUserAction());
        assertFalse(AiState.DRL_GENERATED.requiresUserAction());
        assertFalse(AiState.PUBLISHED.requiresUserAction());
        assertFalse(AiState.FAILED.requiresUserAction());
    }
    
    @Test
    void testIsCompleted() {
        // Only PUBLISHED is completed
        assertTrue(AiState.PUBLISHED.isCompleted());
        
        // All others are not completed
        assertFalse(AiState.IMAGE_RECEIVED.isCompleted());
        assertFalse(AiState.TEXT_RECEIVED.isCompleted());
        assertFalse(AiState.PROCESS_INFERRED.isCompleted());
        assertFalse(AiState.CLARIFICATION_REQUIRED.isCompleted());
        assertFalse(AiState.MODEL_READY.isCompleted());
        assertFalse(AiState.BPMN_GENERATED.isCompleted());
        assertFalse(AiState.DRL_GENERATED.isCompleted());
        assertFalse(AiState.FAILED.isCompleted());
    }
    
    @Test
    void testIsFailed() {
        // Only FAILED is failed
        assertTrue(AiState.FAILED.isFailed());
        
        // All others are not failed
        assertFalse(AiState.IMAGE_RECEIVED.isFailed());
        assertFalse(AiState.TEXT_RECEIVED.isFailed());
        assertFalse(AiState.PROCESS_INFERRED.isFailed());
        assertFalse(AiState.CLARIFICATION_REQUIRED.isFailed());
        assertFalse(AiState.MODEL_READY.isFailed());
        assertFalse(AiState.BPMN_GENERATED.isFailed());
        assertFalse(AiState.DRL_GENERATED.isFailed());
        assertFalse(AiState.PUBLISHED.isFailed());
    }
    
    @Test
    void testGetNextStateFromImageReceived() {
        assertEquals(AiState.PROCESS_INFERRED, AiState.IMAGE_RECEIVED.getNextState());
    }
    
    @Test
    void testGetNextStateFromTextReceived() {
        assertEquals(AiState.PROCESS_INFERRED, AiState.TEXT_RECEIVED.getNextState());
    }
    
    @Test
    void testGetNextStateFromProcessInferred() {
        assertEquals(AiState.MODEL_READY, AiState.PROCESS_INFERRED.getNextState());
    }
    
    @Test
    void testGetNextStateFromModelReady() {
        assertEquals(AiState.BPMN_GENERATED, AiState.MODEL_READY.getNextState());
    }
    
    @Test
    void testGetNextStateFromBpmnGenerated() {
        assertEquals(AiState.DRL_GENERATED, AiState.BPMN_GENERATED.getNextState());
    }
    
    @Test
    void testGetNextStateFromDrlGenerated() {
        assertEquals(AiState.PUBLISHED, AiState.DRL_GENERATED.getNextState());
    }
    
    @Test
    void testGetNextStateFromTerminalStates() {
        // Terminal states should return null
        assertNull(AiState.PUBLISHED.getNextState());
        assertNull(AiState.FAILED.getNextState());
    }
    
    @Test
    void testGetNextStateFromClarificationRequired() {
        // Clarification required should return null (needs user input)
        assertNull(AiState.CLARIFICATION_REQUIRED.getNextState());
    }
    
    @Test
    void testGetDescription() {
        // Verify all states have descriptions
        for (AiState state : AiState.values()) {
            assertNotNull(state.getDescription());
            assertFalse(state.getDescription().isEmpty());
        }
    }
    
    @Test
    void testSpecificDescriptions() {
        assertTrue(AiState.IMAGE_RECEIVED.getDescription().contains("uploaded"));
        assertTrue(AiState.TEXT_RECEIVED.getDescription().contains("received"));
        assertTrue(AiState.PROCESS_INFERRED.getDescription().contains("inferred"));
        assertTrue(AiState.CLARIFICATION_REQUIRED.getDescription().contains("clarification"));
        assertTrue(AiState.MODEL_READY.getDescription().contains("ready"));
        assertTrue(AiState.BPMN_GENERATED.getDescription().contains("BPMN"));
        assertTrue(AiState.DRL_GENERATED.getDescription().contains("DRL") || 
                   AiState.DRL_GENERATED.getDescription().contains("rules"));
        assertTrue(AiState.PUBLISHED.getDescription().contains("published"));
        assertTrue(AiState.FAILED.getDescription().contains("failed"));
    }
    
    @Test
    void testWorkflowProgression() {
        // Test typical workflow from image
        AiState current = AiState.IMAGE_RECEIVED;
        
        current = current.getNextState();
        assertEquals(AiState.PROCESS_INFERRED, current);
        
        current = current.getNextState();
        assertEquals(AiState.MODEL_READY, current);
        
        current = current.getNextState();
        assertEquals(AiState.BPMN_GENERATED, current);
        
        current = current.getNextState();
        assertEquals(AiState.DRL_GENERATED, current);
        
        current = current.getNextState();
        assertEquals(AiState.PUBLISHED, current);
        
        // Should be at terminal state
        assertTrue(current.isTerminalState());
        assertTrue(current.isCompleted());
        assertNull(current.getNextState());
    }
    
    @Test
    void testWorkflowProgressionFromText() {
        // Test typical workflow from text
        AiState current = AiState.TEXT_RECEIVED;
        
        current = current.getNextState();
        assertEquals(AiState.PROCESS_INFERRED, current);
        
        // Rest of workflow is same as image
        current = current.getNextState();
        assertEquals(AiState.MODEL_READY, current);
    }
    
    @Test
    void testEnumValueOf() {
        // Test valueOf for all states
        assertEquals(AiState.IMAGE_RECEIVED, AiState.valueOf("IMAGE_RECEIVED"));
        assertEquals(AiState.TEXT_RECEIVED, AiState.valueOf("TEXT_RECEIVED"));
        assertEquals(AiState.PROCESS_INFERRED, AiState.valueOf("PROCESS_INFERRED"));
        assertEquals(AiState.CLARIFICATION_REQUIRED, AiState.valueOf("CLARIFICATION_REQUIRED"));
        assertEquals(AiState.MODEL_READY, AiState.valueOf("MODEL_READY"));
        assertEquals(AiState.BPMN_GENERATED, AiState.valueOf("BPMN_GENERATED"));
        assertEquals(AiState.DRL_GENERATED, AiState.valueOf("DRL_GENERATED"));
        assertEquals(AiState.PUBLISHED, AiState.valueOf("PUBLISHED"));
        assertEquals(AiState.FAILED, AiState.valueOf("FAILED"));
    }
    
    @Test
    void testEnumToString() {
        // Test toString for all states
        assertEquals("IMAGE_RECEIVED", AiState.IMAGE_RECEIVED.toString());
        assertEquals("TEXT_RECEIVED", AiState.TEXT_RECEIVED.toString());
        assertEquals("PROCESS_INFERRED", AiState.PROCESS_INFERRED.toString());
        assertEquals("CLARIFICATION_REQUIRED", AiState.CLARIFICATION_REQUIRED.toString());
        assertEquals("MODEL_READY", AiState.MODEL_READY.toString());
        assertEquals("BPMN_GENERATED", AiState.BPMN_GENERATED.toString());
        assertEquals("DRL_GENERATED", AiState.DRL_GENERATED.toString());
        assertEquals("PUBLISHED", AiState.PUBLISHED.toString());
        assertEquals("FAILED", AiState.FAILED.toString());
    }
}

