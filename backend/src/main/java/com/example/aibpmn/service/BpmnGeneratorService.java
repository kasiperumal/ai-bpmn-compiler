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
     * Generate BPMN gateway element.
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
        xml.append("/>\n");
    }
    
    /**
     * Generate BPMN sequence flow element.
     */
    private void generateEdge(StringBuilder xml, ProcessEdge edge, ProcessModel processModel) {
        xml.append("    <bpmn2:sequenceFlow ");
        xml.append("id=\"").append(escapeXml(edge.getId())).append("\" ");
        xml.append("sourceRef=\"").append(escapeXml(edge.getFromNodeId())).append("\" ");
        xml.append("targetRef=\"").append(escapeXml(edge.getToNodeId())).append("\"");
        
        // Add name/label if present
        if (edge.getLabel() != null && !edge.getLabel().trim().isEmpty()) {
            xml.append(" name=\"").append(escapeXml(edge.getLabel())).append("\"");
        }
        
        // Add condition expression for conditional flows
        if (edge.getCondition() != null && !edge.getCondition().trim().isEmpty()) {
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
     * Generate BPMN diagram element (for visualization).
     */
    private void generateDiagram(StringBuilder xml, ProcessModel processModel) {
        xml.append("  <bpmndi:BPMNDiagram id=\"BPMNDiagram_").append(processModel.getId()).append("\">\n");
        xml.append("    <bpmndi:BPMNPlane id=\"BPMNPlane_").append(processModel.getId()).append("\" ");
        xml.append("bpmnElement=\"").append(escapeXml(processModel.getId())).append("\">\n");
        
        // Generate shapes for nodes (with auto-layout)
        int x = 100;
        int y = 100;
        int xSpacing = 200;
        int ySpacing = 100;
        int column = 0;
        
        for (ProcessNode node : processModel.getNodes()) {
            xml.append("      <bpmndi:BPMNShape id=\"Shape_").append(node.getId()).append("\" ");
            xml.append("bpmnElement=\"").append(escapeXml(node.getId())).append("\">\n");
            xml.append("        <dc:Bounds x=\"").append(x + (column * xSpacing)).append("\" ");
            xml.append("y=\"").append(y).append("\" ");
            xml.append("width=\"100\" height=\"80\" />\n");
            xml.append("      </bpmndi:BPMNShape>\n");
            
            column++;
            if (column > 3) {
                column = 0;
                y += ySpacing;
            }
        }
        
        // Generate edges for sequence flows
        for (ProcessEdge edge : processModel.getEdges()) {
            xml.append("      <bpmndi:BPMNEdge id=\"Edge_").append(edge.getId()).append("\" ");
            xml.append("bpmnElement=\"").append(escapeXml(edge.getId())).append("\" />\n");
        }
        
        xml.append("    </bpmndi:BPMNPlane>\n");
        xml.append("  </bpmndi:BPMNDiagram>\n");
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

