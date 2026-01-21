package com.example.aibpmn.service;

import com.example.aibpmn.exception.BpmnValidationException;
import com.example.aibpmn.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating BPMN 2.0 XML from ProcessModel.
 * Creates Kogito-compatible, executable BPMN processes.
 * 
 * LAYOUT STRATEGY: Minimal bounds provided by backend, BPMN.js calculates
 * all positions and routing automatically to prevent overlaps.
 */
@Service
public class BpmnGeneratorService {
    
    private static final Logger logger = LoggerFactory.getLogger(BpmnGeneratorService.class);
    
    private static final String BPMN_NAMESPACE = "http://www.omg.org/spec/BPMN/20100524/MODEL";
    private static final String BPMNDI_NAMESPACE = "http://www.omg.org/spec/BPMN/20100524/DI";
    private static final String DC_NAMESPACE = "http://www.omg.org/spec/DD/20100524/DC";
    private static final String DI_NAMESPACE = "http://www.omg.org/spec/DD/20100524/DI";
    
    /**
     * Generate BPMN 2.0 XML from ProcessModel.
     *
     * @param processModel The process model to convert
     * @return BPMN 2.0 XML as String
     * @throws IllegalArgumentException if processModel is null or invalid
     * @throws BpmnValidationException if model validation fails
     */
    public String generateBpmn(ProcessModel processModel) {
        if (processModel == null) {
            throw new IllegalArgumentException("ProcessModel cannot be null");
        }
        
        logger.info("Generating BPMN for process: {} (nodes: {}, edges: {})",
            processModel.getId(), processModel.getNodes().size(), processModel.getEdges().size());
        
        // Store reference for gateway generation
        this.currentProcessModel = processModel;
        
        // Validate model
        validateModel(processModel);
        
        // Generate XML
        StringBuilder xml = new StringBuilder();
        
        // XML declaration and definitions
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<bpmn2:definitions ");
        xml.append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ");
        xml.append("xmlns:bpmn2=\"").append(BPMN_NAMESPACE).append("\" ");
        xml.append("xmlns:bpmndi=\"").append(BPMNDI_NAMESPACE).append("\" ");
        xml.append("xmlns:dc=\"").append(DC_NAMESPACE).append("\" ");
        xml.append("xmlns:di=\"").append(DI_NAMESPACE).append("\" ");
        xml.append("id=\"Definitions_").append(processModel.getId()).append("\" ");
        xml.append("targetNamespace=\"http://example.com/bpmn\" ");
        xml.append("exporter=\"AI-BPMN-Compiler\" ");
        xml.append("exporterVersion=\"1.0\">\n");
        
        // Process element
        xml.append("  <bpmn2:process ");
        xml.append("id=\"").append(escapeXml(processModel.getId())).append("\" ");
        xml.append("name=\"").append(escapeXml(processModel.getName())).append("\" ");
        xml.append("isExecutable=\"true\">\n");
        
        // Generate nodes
        for (ProcessNode node : processModel.getNodes()) {
            generateNode(xml, node);
        }
        
        // Generate edges (sequence flows)
        for (ProcessEdge edge : processModel.getEdges()) {
            generateEdge(xml, edge, processModel);
        }
        
        xml.append("  </bpmn2:process>\n");
        
        // BPMNDiagram (optional, for visualization)
        generateDiagram(xml, processModel);
        
        xml.append("</bpmn2:definitions>\n");
        
        String bpmnXml = xml.toString();
        logger.info("Generated BPMN XML ({} chars) for process: {}", bpmnXml.length(), processModel.getId());
        
        return bpmnXml;
    }
    
    /**
     * Validate ProcessModel before generating BPMN.
     */
    private void validateModel(ProcessModel processModel) {
        List<String> errors = new ArrayList<>();
        
        // Check for nodes
        if (processModel.getNodes().isEmpty()) {
            errors.add("Process must have at least one node");
        }
        
        // Count start and end events
        long startEvents = processModel.getNodes().stream()
            .filter(n -> n.getType() == NodeType.EVENT && "start".equals(n.getProperties().get("eventType")))
            .count();
        
        long endEvents = processModel.getNodes().stream()
            .filter(n -> n.getType() == NodeType.EVENT && "end".equals(n.getProperties().get("eventType")))
            .count();
        
        if (startEvents == 0) {
            errors.add("Process must have exactly one start event (found 0)");
        } else if (startEvents > 1) {
            errors.add("Process must have exactly one start event (found " + startEvents + ")");
        }
        
        if (endEvents == 0) {
            errors.add("Process must have at least one end event (found 0)");
        }
        
        // Check for disconnected nodes (basic check)
        Set<String> nodesInEdges = new HashSet<>();
        for (ProcessEdge edge : processModel.getEdges()) {
            nodesInEdges.add(edge.getFromNodeId());
            nodesInEdges.add(edge.getToNodeId());
        }
        
        for (ProcessNode node : processModel.getNodes()) {
            // Start events don't need incoming edges
            boolean isStartEvent = node.getType() == NodeType.EVENT && 
                "start".equals(node.getProperties().get("eventType"));
            // End events don't need outgoing edges
            boolean isEndEvent = node.getType() == NodeType.EVENT && 
                "end".equals(node.getProperties().get("eventType"));
            
            if (!isStartEvent && !isEndEvent && !nodesInEdges.contains(node.getId())) {
                logger.warn("Node {} appears to be disconnected", node.getId());
            }
        }
        
        // Validate gateway balance (simplified - just warn, don't fail)
        validateGatewayBalance(processModel);
        
        if (!errors.isEmpty()) {
            throw new BpmnValidationException(
                "BPMN validation failed: " + String.join(", ", errors)
            );
        }
    }
    
    /**
     * Validate that gateways are balanced (splits have corresponding joins).
     */
    private void validateGatewayBalance(ProcessModel processModel) {
        Map<String, Integer> gatewayOutgoing = new HashMap<>();
        Map<String, Integer> gatewayIncoming = new HashMap<>();
        
        // Count outgoing and incoming edges for gateways
        for (ProcessEdge edge : processModel.getEdges()) {
            ProcessNode fromNode = findNodeById(processModel, edge.getFromNodeId());
            ProcessNode toNode = findNodeById(processModel, edge.getToNodeId());
            
            if (fromNode != null && fromNode.getType() == NodeType.GATEWAY) {
                gatewayOutgoing.merge(fromNode.getId(), 1, Integer::sum);
            }
            
            if (toNode != null && toNode.getType() == NodeType.GATEWAY) {
                gatewayIncoming.merge(toNode.getId(), 1, Integer::sum);
            }
        }
        
        // Log warnings for potentially unbalanced gateways
        for (ProcessNode node : processModel.getNodes()) {
            if (node.getType() == NodeType.GATEWAY) {
                int outgoing = gatewayOutgoing.getOrDefault(node.getId(), 0);
                int incoming = gatewayIncoming.getOrDefault(node.getId(), 0);
                
                if (outgoing > 1 && incoming == 1) {
                    logger.info("Gateway {} is a split (diverging gateway)", node.getId());
                } else if (incoming > 1 && outgoing == 1) {
                    logger.info("Gateway {} is a join (converging gateway)", node.getId());
                } else if (outgoing > 1 && incoming > 1) {
                    logger.warn("Gateway {} has multiple incoming and outgoing flows", node.getId());
                }
            }
        }
    }
    
    /**
     * Find a node by ID in the process model.
     */
    private ProcessNode findNodeById(ProcessModel processModel, String nodeId) {
        return processModel.getNodes().stream()
            .filter(n -> n.getId().equals(nodeId))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Generate BPMN XML for a single node.
     */
    private void generateNode(StringBuilder xml, ProcessNode node) {
        switch (node.getType()) {
            case EVENT:
                generateEvent(xml, node);
                break;
            case TASK:
                generateTask(xml, node);
                break;
            case GATEWAY:
                generateGateway(xml, node);
                break;
            default:
                logger.warn("Unknown node type: {}", node.getType());
        }
    }
    
    /**
     * Generate BPMN event element.
     */
    private void generateEvent(StringBuilder xml, ProcessNode node) {
        String eventType = (String) node.getProperties().get("eventType");
        
        if ("start".equals(eventType)) {
            xml.append("    <bpmn2:startEvent ");
            xml.append("id=\"").append(escapeXml(node.getId())).append("\" ");
            xml.append("name=\"").append(escapeXml(node.getName())).append("\">\n");
            xml.append("    </bpmn2:startEvent>\n");
        } else if ("end".equals(eventType)) {
            xml.append("    <bpmn2:endEvent ");
            xml.append("id=\"").append(escapeXml(node.getId())).append("\" ");
            xml.append("name=\"").append(escapeXml(node.getName())).append("\">\n");
            xml.append("    </bpmn2:endEvent>\n");
        } else {
            // Default to intermediate event
            xml.append("    <bpmn2:intermediateCatchEvent ");
            xml.append("id=\"").append(escapeXml(node.getId())).append("\" ");
            xml.append("name=\"").append(escapeXml(node.getName())).append("\">\n");
            xml.append("    </bpmn2:intermediateCatchEvent>\n");
        }
    }
    
    /**
     * Generate BPMN task element.
     */
    private void generateTask(StringBuilder xml, ProcessNode node) {
        // Use scriptTask for now (can be extended to support different task types)
        xml.append("    <bpmn2:scriptTask ");
        xml.append("id=\"").append(escapeXml(node.getId())).append("\" ");
        xml.append("name=\"").append(escapeXml(node.getName())).append("\" ");
        xml.append("scriptFormat=\"java\">\n");
        
        // Add script (placeholder - could be enhanced with actual logic)
        xml.append("      <bpmn2:script>");
        xml.append("System.out.println(\"Executing: ").append(escapeXml(node.getName())).append("\");");
        xml.append("</bpmn2:script>\n");
        
        xml.append("    </bpmn2:scriptTask>\n");
    }
    
    /**
     * Generate BPMN gateway element with proper flow references and direction.
     * Includes incoming/outgoing flow references for BPMN 2.0 compliance.
     */
    private void generateGateway(StringBuilder xml, ProcessNode node) {
        String gatewayType = (String) node.getProperties().get("gatewayType");
        
        if ("parallel".equals(gatewayType)) {
            xml.append("    <bpmn2:parallelGateway ");
        } else if ("inclusive".equals(gatewayType)) {
            xml.append("    <bpmn2:inclusiveGateway ");
        } else {
            // Default to exclusive gateway
            xml.append("    <bpmn2:exclusiveGateway ");
        }
        
        xml.append("id=\"").append(escapeXml(node.getId())).append("\" ");
        xml.append("name=\"").append(escapeXml(node.getName())).append("\" ");
        
        // CRITICAL: isMarkerVisible must be true for gateway symbols (X, +, O) to appear
        xml.append("isMarkerVisible=\"true\" ");
        
        // Determine gateway direction based on flow counts
        List<String> incomingFlows = new ArrayList<>();
        List<String> outgoingFlows = new ArrayList<>();
        
        // Find all flows connected to this gateway
        for (ProcessEdge edge : getCurrentProcessModel().getEdges()) {
            if (edge.getToNodeId().equals(node.getId())) {
                incomingFlows.add(edge.getId());
            }
            if (edge.getFromNodeId().equals(node.getId())) {
                outgoingFlows.add(edge.getId());
            }
        }
        
        // Set gateway direction
        if (outgoingFlows.size() > 1 && incomingFlows.size() == 1) {
            xml.append("gatewayDirection=\"Diverging\" ");
        } else if (incomingFlows.size() > 1 && outgoingFlows.size() == 1) {
            xml.append("gatewayDirection=\"Converging\" ");
        } else if (incomingFlows.size() > 1 && outgoingFlows.size() > 1) {
            xml.append("gatewayDirection=\"Mixed\" ");
        } else {
            xml.append("gatewayDirection=\"Diverging\" "); // Default
        }
        
        // Close opening tag and add flow references
        xml.append(">\n");
        
        // Add incoming flow references
        for (String flowId : incomingFlows) {
            xml.append("      <bpmn2:incoming>").append(escapeXml(flowId)).append("</bpmn2:incoming>\n");
        }
        
        // Add outgoing flow references
        for (String flowId : outgoingFlows) {
            xml.append("      <bpmn2:outgoing>").append(escapeXml(flowId)).append("</bpmn2:outgoing>\n");
        }
        
        // Close gateway element
        if ("parallel".equals(gatewayType)) {
            xml.append("    </bpmn2:parallelGateway>\n");
        } else if ("inclusive".equals(gatewayType)) {
            xml.append("    </bpmn2:inclusiveGateway>\n");
        } else {
            xml.append("    </bpmn2:exclusiveGateway>\n");
        }
    }
    
    /**
     * Helper to get current ProcessModel being processed.
     * Used by generateGateway to access edges.
     */
    private ProcessModel currentProcessModel;
    
    private ProcessModel getCurrentProcessModel() {
        return currentProcessModel;
    }
    
    /**
     * Generate BPMN sequence flow element.
     * 
     * INDUSTRY STANDARD LABELING:
     * - Only conditional flows (gateway branches) have visible labels
     * - Sequential flows (simple connections) have no labels
     * - This prevents label clutter and matches professional BPMN tools (Camunda, Signavio)
     */
    private void generateEdge(StringBuilder xml, ProcessEdge edge, ProcessModel processModel) {
        xml.append("    <bpmn2:sequenceFlow ");
        xml.append("id=\"").append(escapeXml(edge.getId())).append("\" ");
        xml.append("sourceRef=\"").append(escapeXml(edge.getFromNodeId())).append("\" ");
        xml.append("targetRef=\"").append(escapeXml(edge.getToNodeId())).append("\"");
        
        // INDUSTRY STANDARD: Only add labels to conditional flows (gateway branches)
        // Sequential flows should be label-free for clean diagrams
        boolean hasCondition = edge.getCondition() != null && !edge.getCondition().trim().isEmpty();
        boolean hasLabel = edge.getLabel() != null && !edge.getLabel().trim().isEmpty();
        
        if (hasCondition && hasLabel) {
            // Conditional flow with label - add short label for branch identification
            xml.append(" name=\"").append(escapeXml(edge.getLabel())).append("\"");
        }
        // Note: Sequential flows (no condition) get NO label, even if label text exists
        
        // Add condition expression for conditional flows
        if (hasCondition) {
            xml.append(">\n");
            xml.append("      <bpmn2:conditionExpression xsi:type=\"bpmn2:tFormalExpression\">");
            xml.append(escapeXml(edge.getCondition()));
            xml.append("</bpmn2:conditionExpression>\n");
            xml.append("    </bpmn2:sequenceFlow>\n");
        } else {
            xml.append(" />\n");
        }
    }
    
    /**
     * Generate BPMN diagram element with minimal bounds.
     * BPMN.js will automatically calculate optimal layout and connection routing.
     * 
     * STRATEGY: Backend provides only element types and dimensions, frontend (BPMN.js) 
     * handles all positioning and routing to avoid overlap issues.
     */
    private void generateDiagram(StringBuilder xml, ProcessModel processModel) {
        xml.append("  <bpmndi:BPMNDiagram id=\"BPMNDiagram_").append(processModel.getId()).append("\">\n");
        xml.append("    <bpmndi:BPMNPlane id=\"BPMNPlane_").append(processModel.getId()).append("\" ");
        xml.append("bpmnElement=\"").append(escapeXml(processModel.getId())).append("\">\n");
        
        // Generate shapes for nodes with standard dimensions
        // BPMN.js will calculate actual positions during rendering
        for (ProcessNode node : processModel.getNodes()) {
            xml.append("      <bpmndi:BPMNShape id=\"Shape_").append(node.getId()).append("\" ");
            xml.append("bpmnElement=\"").append(escapeXml(node.getId())).append("\">\n");
            
            // Provide standard dimensions based on element type
            // Position (x, y) will be auto-calculated by BPMN.js
            int width = getNodeWidth(node);
            int height = getNodeHeight(node);
            
            xml.append("        <dc:Bounds x=\"0\" y=\"0\" ");
            xml.append("width=\"").append(width).append("\" ");
            xml.append("height=\"").append(height).append("\" />\n");
            
            // Add label if present
            if (node.getName() != null && !node.getName().isEmpty()) {
                xml.append("        <bpmndi:BPMNLabel />\n");
            }
            
            xml.append("      </bpmndi:BPMNShape>\n");
        }
        
        // Generate edges without waypoints
        // BPMN.js will calculate optimal routing to avoid overlaps
        for (ProcessEdge edge : processModel.getEdges()) {
            xml.append("      <bpmndi:BPMNEdge id=\"Edge_").append(edge.getId()).append("\" ");
            xml.append("bpmnElement=\"").append(escapeXml(edge.getId())).append("\">\n");
            
            // NO WAYPOINTS - Let BPMN.js calculate optimal routing
            // This ensures connections never overlap with elements
            
            // Add empty label element ONLY for conditional flows (with labels)
            // BPMN.js auto-positions labels to prevent overlap
            // Sequential flows have no labels (industry standard)
            boolean hasCondition = edge.getCondition() != null && !edge.getCondition().trim().isEmpty();
            boolean hasLabel = edge.getLabel() != null && !edge.getLabel().trim().isEmpty();
            if (hasCondition && hasLabel) {
                xml.append("        <bpmndi:BPMNLabel />\n");
            }
            
            xml.append("      </bpmndi:BPMNEdge>\n");
        }
        
        xml.append("    </bpmndi:BPMNPlane>\n");
        xml.append("  </bpmndi:BPMNDiagram>\n");
    }
    
    /**
     * Get standard width for a node based on type.
     */
    private int getNodeWidth(ProcessNode node) {
        if (node.getType() == null) {
            return 120; // Default task width
        }
        
        switch (node.getType()) {
            case EVENT:
                return 36;
            case GATEWAY:
                return 50;
            case TASK:
            default:
                return 120;
        }
    }
    
    /**
     * Get standard height for a node based on type.
     */
    private int getNodeHeight(ProcessNode node) {
        if (node.getType() == null) {
            return 80; // Default task height
        }
        
        switch (node.getType()) {
            case EVENT:
                return 36;
            case GATEWAY:
                return 50;
            case TASK:
            default:
                return 80;
        }
    }
    
    /**
     * Escape XML special characters.
     */
    private String escapeXml(String text) {
        if (text == null) {
            return "";
        }
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }
}

