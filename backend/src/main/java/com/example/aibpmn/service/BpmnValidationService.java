package com.example.aibpmn.service;

import com.example.aibpmn.dto.BpmnValidationResult;
import com.example.aibpmn.dto.BpmnValidationResult.ValidationError;
import com.example.aibpmn.dto.BpmnValidationResult.ValidationWarning;
import com.example.aibpmn.dto.BpmnValidationResult.ErrorSeverity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;

/**
 * Service for validating BPMN 2.0 XML.
 * Performs XML structure validation and logical constraint validation.
 */
@Service
public class BpmnValidationService {
    
    private static final Logger logger = LoggerFactory.getLogger(BpmnValidationService.class);
    
    private static final String BPMN_NAMESPACE = "http://www.omg.org/spec/BPMN/20100524/MODEL";
    
    /**
     * Validate a BPMN XML string.
     *
     * @param bpmnXml The BPMN XML to validate
     * @return BpmnValidationResult with errors and warnings
     */
    public BpmnValidationResult validate(String bpmnXml) {
        if (bpmnXml == null || bpmnXml.trim().isEmpty()) {
            return BpmnValidationResult.failure(
                List.of(new ValidationError("EMPTY_XML", "BPMN XML is empty or null"))
            );
        }
        
        logger.info("Starting BPMN validation (XML length: {})", bpmnXml.length());
        
        List<ValidationError> errors = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();
        
        // 1. Parse XML
        Document document;
        try {
            document = parseXml(bpmnXml);
        } catch (Exception e) {
            logger.error("XML parsing failed: {}", e.getMessage());
            errors.add(new ValidationError(
                "XML_PARSE_ERROR",
                "Failed to parse XML: " + e.getMessage(),
                null,
                ErrorSeverity.CRITICAL
            ));
            return BpmnValidationResult.failure(errors);
        }
        
        // 2. Validate BPMN structure
        validateBpmnStructure(document, errors, warnings);
        
        // 3. Validate process elements
        validateProcessElements(document, errors, warnings);
        
        // 4. Validate sequence flows
        validateSequenceFlows(document, errors, warnings);
        
        // 5. Validate logical constraints
        validateLogicalConstraints(document, errors, warnings);
        
        boolean valid = errors.isEmpty();
        logger.info("BPMN validation completed: valid={}, errors={}, warnings={}", 
            valid, errors.size(), warnings.size());
        
        return new BpmnValidationResult(valid, errors, warnings);
    }
    
    /**
     * Parse XML string into DOM Document.
     */
    private Document parseXml(String xml) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(false); // We'll do manual validation
        
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xml)));
    }
    
    /**
     * Validate basic BPMN structure.
     */
    private void validateBpmnStructure(Document document, List<ValidationError> errors, List<ValidationWarning> warnings) {
        // Check for definitions element
        Element root = document.getDocumentElement();
        if (root == null || !root.getLocalName().equals("definitions")) {
            errors.add(new ValidationError(
                "MISSING_DEFINITIONS",
                "Root element must be 'definitions'",
                null,
                ErrorSeverity.CRITICAL
            ));
            return;
        }
        
        // Check for process element
        NodeList processes = root.getElementsByTagNameNS(BPMN_NAMESPACE, "process");
        if (processes.getLength() == 0) {
            errors.add(new ValidationError(
                "NO_PROCESS",
                "BPMN must contain at least one process element",
                null,
                ErrorSeverity.CRITICAL
            ));
        } else if (processes.getLength() > 1) {
            warnings.add(new ValidationWarning(
                "MULTIPLE_PROCESSES",
                "BPMN contains multiple processes (found " + processes.getLength() + ")"
            ));
        }
        
        // Check namespace
        String namespace = root.getNamespaceURI();
        if (namespace == null || !namespace.equals(BPMN_NAMESPACE)) {
            warnings.add(new ValidationWarning(
                "INVALID_NAMESPACE",
                "Expected BPMN namespace: " + BPMN_NAMESPACE + ", found: " + namespace
            ));
        }
    }
    
    /**
     * Validate process elements.
     */
    private void validateProcessElements(Document document, List<ValidationError> errors, List<ValidationWarning> warnings) {
        NodeList processes = document.getElementsByTagNameNS(BPMN_NAMESPACE, "process");
        
        for (int i = 0; i < processes.getLength(); i++) {
            Element process = (Element) processes.item(i);
            String processId = process.getAttribute("id");
            
            if (processId == null || processId.trim().isEmpty()) {
                errors.add(new ValidationError(
                    "MISSING_PROCESS_ID",
                    "Process element must have an 'id' attribute"
                ));
                continue;
            }
            
            // Check isExecutable attribute
            String isExecutable = process.getAttribute("isExecutable");
            if (!"true".equals(isExecutable)) {
                warnings.add(new ValidationWarning(
                    "NOT_EXECUTABLE",
                    "Process '" + processId + "' is not marked as executable",
                    processId
                ));
            }
            
            // Validate start events
            validateStartEvents(process, processId, errors, warnings);
            
            // Validate end events
            validateEndEvents(process, processId, errors, warnings);
            
            // Validate element IDs are unique
            validateUniqueIds(process, processId, errors);
        }
    }
    
    /**
     * Validate start events.
     */
    private void validateStartEvents(Element process, String processId, 
                                     List<ValidationError> errors, List<ValidationWarning> warnings) {
        NodeList startEvents = process.getElementsByTagNameNS(BPMN_NAMESPACE, "startEvent");
        
        if (startEvents.getLength() == 0) {
            errors.add(new ValidationError(
                "NO_START_EVENT",
                "Process '" + processId + "' must have at least one start event",
                processId,
                ErrorSeverity.ERROR
            ));
        } else if (startEvents.getLength() > 1) {
            errors.add(new ValidationError(
                "MULTIPLE_START_EVENTS",
                "Process '" + processId + "' has multiple start events (found " + startEvents.getLength() + ")",
                processId,
                ErrorSeverity.ERROR
            ));
        }
        
        // Check start event has no incoming flows
        for (int i = 0; i < startEvents.getLength(); i++) {
            Element startEvent = (Element) startEvents.item(i);
            String startId = startEvent.getAttribute("id");
            
            NodeList incoming = startEvent.getElementsByTagNameNS(BPMN_NAMESPACE, "incoming");
            if (incoming.getLength() > 0) {
                errors.add(new ValidationError(
                    "START_EVENT_HAS_INCOMING",
                    "Start event '" + startId + "' should not have incoming sequence flows",
                    startId,
                    ErrorSeverity.ERROR
                ));
            }
        }
    }
    
    /**
     * Validate end events.
     */
    private void validateEndEvents(Element process, String processId, 
                                   List<ValidationError> errors, List<ValidationWarning> warnings) {
        NodeList endEvents = process.getElementsByTagNameNS(BPMN_NAMESPACE, "endEvent");
        
        if (endEvents.getLength() == 0) {
            errors.add(new ValidationError(
                "NO_END_EVENT",
                "Process '" + processId + "' must have at least one end event",
                processId,
                ErrorSeverity.ERROR
            ));
        }
        
        // Check end event has no outgoing flows
        for (int i = 0; i < endEvents.getLength(); i++) {
            Element endEvent = (Element) endEvents.item(i);
            String endId = endEvent.getAttribute("id");
            
            NodeList outgoing = endEvent.getElementsByTagNameNS(BPMN_NAMESPACE, "outgoing");
            if (outgoing.getLength() > 0) {
                errors.add(new ValidationError(
                    "END_EVENT_HAS_OUTGOING",
                    "End event '" + endId + "' should not have outgoing sequence flows",
                    endId,
                    ErrorSeverity.ERROR
                ));
            }
        }
    }
    
    /**
     * Validate unique element IDs within a process.
     */
    private void validateUniqueIds(Element process, String processId, List<ValidationError> errors) {
        Set<String> ids = new HashSet<>();
        Set<String> duplicates = new HashSet<>();
        
        NodeList allElements = process.getElementsByTagName("*");
        for (int i = 0; i < allElements.getLength(); i++) {
            Element element = (Element) allElements.item(i);
            String id = element.getAttribute("id");
            
            if (id != null && !id.isEmpty()) {
                if (!ids.add(id)) {
                    duplicates.add(id);
                }
            }
        }
        
        for (String duplicateId : duplicates) {
            errors.add(new ValidationError(
                "DUPLICATE_ID",
                "Duplicate element ID '" + duplicateId + "' in process '" + processId + "'",
                duplicateId,
                ErrorSeverity.ERROR
            ));
        }
    }
    
    /**
     * Validate sequence flows.
     */
    private void validateSequenceFlows(Document document, List<ValidationError> errors, List<ValidationWarning> warnings) {
        NodeList processes = document.getElementsByTagNameNS(BPMN_NAMESPACE, "process");
        
        for (int i = 0; i < processes.getLength(); i++) {
            Element process = (Element) processes.item(i);
            String processId = process.getAttribute("id");
            
            // Get all elements in the process
            Map<String, Element> elements = collectElements(process);
            
            // Validate each sequence flow
            NodeList flows = process.getElementsByTagNameNS(BPMN_NAMESPACE, "sequenceFlow");
            for (int j = 0; j < flows.getLength(); j++) {
                Element flow = (Element) flows.item(j);
                String flowId = flow.getAttribute("id");
                String sourceRef = flow.getAttribute("sourceRef");
                String targetRef = flow.getAttribute("targetRef");
                
                // Check required attributes
                if (sourceRef == null || sourceRef.isEmpty()) {
                    errors.add(new ValidationError(
                        "MISSING_SOURCE_REF",
                        "Sequence flow '" + flowId + "' missing sourceRef attribute",
                        flowId,
                        ErrorSeverity.ERROR
                    ));
                }
                
                if (targetRef == null || targetRef.isEmpty()) {
                    errors.add(new ValidationError(
                        "MISSING_TARGET_REF",
                        "Sequence flow '" + flowId + "' missing targetRef attribute",
                        flowId,
                        ErrorSeverity.ERROR
                    ));
                }
                
                // Check source exists
                if (!elements.containsKey(sourceRef)) {
                    errors.add(new ValidationError(
                        "INVALID_SOURCE_REF",
                        "Sequence flow '" + flowId + "' references non-existent source '" + sourceRef + "'",
                        flowId,
                        ErrorSeverity.ERROR
                    ));
                }
                
                // Check target exists
                if (!elements.containsKey(targetRef)) {
                    errors.add(new ValidationError(
                        "INVALID_TARGET_REF",
                        "Sequence flow '" + flowId + "' references non-existent target '" + targetRef + "'",
                        flowId,
                        ErrorSeverity.ERROR
                    ));
                }
            }
        }
    }
    
    /**
     * Validate logical constraints (orphan nodes, unreachable nodes, etc.)
     */
    private void validateLogicalConstraints(Document document, List<ValidationError> errors, List<ValidationWarning> warnings) {
        NodeList processes = document.getElementsByTagNameNS(BPMN_NAMESPACE, "process");
        
        for (int i = 0; i < processes.getLength(); i++) {
            Element process = (Element) processes.item(i);
            String processId = process.getAttribute("id");
            
            // Collect all flow nodes (tasks, events, gateways)
            Map<String, Element> flowNodes = collectFlowNodes(process);
            
            // Collect all sequence flows
            Map<String, FlowConnection> flows = collectFlows(process);
            
            // Build graph
            Map<String, Set<String>> outgoing = new HashMap<>();
            Map<String, Set<String>> incoming = new HashMap<>();
            
            for (String nodeId : flowNodes.keySet()) {
                outgoing.put(nodeId, new HashSet<>());
                incoming.put(nodeId, new HashSet<>());
            }
            
            for (FlowConnection flow : flows.values()) {
                outgoing.computeIfAbsent(flow.sourceRef, k -> new HashSet<>()).add(flow.targetRef);
                incoming.computeIfAbsent(flow.targetRef, k -> new HashSet<>()).add(flow.sourceRef);
            }
            
            // Find orphan nodes (no incoming and no outgoing)
            for (Map.Entry<String, Element> entry : flowNodes.entrySet()) {
                String nodeId = entry.getKey();
                Element node = entry.getValue();
                String nodeName = node.getLocalName();
                
                boolean isStart = "startEvent".equals(nodeName);
                boolean isEnd = "endEvent".equals(nodeName);
                
                int inCount = incoming.getOrDefault(nodeId, Collections.emptySet()).size();
                int outCount = outgoing.getOrDefault(nodeId, Collections.emptySet()).size();
                
                // Check for orphan nodes
                if (!isStart && !isEnd && inCount == 0 && outCount == 0) {
                    errors.add(new ValidationError(
                        "ORPHAN_NODE",
                        "Node '" + nodeId + "' is orphaned (no incoming or outgoing flows)",
                        nodeId,
                        ErrorSeverity.ERROR
                    ));
                }
                
                // Check for disconnected nodes
                if (!isStart && inCount == 0 && outCount > 0) {
                    warnings.add(new ValidationWarning(
                        "NO_INCOMING_FLOW",
                        "Node '" + nodeId + "' has no incoming flows (might be unreachable)",
                        nodeId
                    ));
                }
                
                if (!isEnd && outCount == 0 && inCount > 0) {
                    warnings.add(new ValidationWarning(
                        "NO_OUTGOING_FLOW",
                        "Node '" + nodeId + "' has no outgoing flows (process might end abruptly)",
                        nodeId
                    ));
                }
            }
            
            // Check for unreachable nodes from start
            validateReachability(flowNodes, outgoing, errors, warnings, processId);
        }
    }
    
    /**
     * Validate that all nodes are reachable from the start event.
     */
    private void validateReachability(Map<String, Element> flowNodes, 
                                     Map<String, Set<String>> outgoing,
                                     List<ValidationError> errors,
                                     List<ValidationWarning> warnings,
                                     String processId) {
        // Find start event
        String startEventId = null;
        for (Map.Entry<String, Element> entry : flowNodes.entrySet()) {
            if ("startEvent".equals(entry.getValue().getLocalName())) {
                startEventId = entry.getKey();
                break;
            }
        }
        
        if (startEventId == null) {
            return; // Already reported as error
        }
        
        // BFS to find all reachable nodes
        Set<String> reachable = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(startEventId);
        reachable.add(startEventId);
        
        while (!queue.isEmpty()) {
            String current = queue.poll();
            Set<String> neighbors = outgoing.getOrDefault(current, Collections.emptySet());
            
            for (String neighbor : neighbors) {
                if (!reachable.contains(neighbor)) {
                    reachable.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        
        // Check for unreachable nodes
        for (String nodeId : flowNodes.keySet()) {
            if (!reachable.contains(nodeId)) {
                warnings.add(new ValidationWarning(
                    "UNREACHABLE_NODE",
                    "Node '" + nodeId + "' is not reachable from the start event",
                    nodeId
                ));
            }
        }
    }
    
    /**
     * Collect all elements in a process.
     */
    private Map<String, Element> collectElements(Element process) {
        Map<String, Element> elements = new HashMap<>();
        NodeList allElements = process.getElementsByTagName("*");
        
        for (int i = 0; i < allElements.getLength(); i++) {
            Element element = (Element) allElements.item(i);
            String id = element.getAttribute("id");
            
            if (id != null && !id.isEmpty()) {
                elements.put(id, element);
            }
        }
        
        return elements;
    }
    
    /**
     * Collect all flow nodes (activities, events, gateways) in a process.
     */
    private Map<String, Element> collectFlowNodes(Element process) {
        Map<String, Element> flowNodes = new HashMap<>();
        
        String[] flowNodeTypes = {
            "startEvent", "endEvent", "intermediateCatchEvent", "intermediateThrowEvent",
            "task", "scriptTask", "userTask", "serviceTask", "manualTask", "businessRuleTask",
            "exclusiveGateway", "parallelGateway", "inclusiveGateway", "eventBasedGateway"
        };
        
        for (String nodeType : flowNodeTypes) {
            NodeList nodes = process.getElementsByTagNameNS(BPMN_NAMESPACE, nodeType);
            for (int i = 0; i < nodes.getLength(); i++) {
                Element node = (Element) nodes.item(i);
                String id = node.getAttribute("id");
                if (id != null && !id.isEmpty()) {
                    flowNodes.put(id, node);
                }
            }
        }
        
        return flowNodes;
    }
    
    /**
     * Collect all sequence flows in a process.
     */
    private Map<String, FlowConnection> collectFlows(Element process) {
        Map<String, FlowConnection> flows = new HashMap<>();
        NodeList flowNodes = process.getElementsByTagNameNS(BPMN_NAMESPACE, "sequenceFlow");
        
        for (int i = 0; i < flowNodes.getLength(); i++) {
            Element flow = (Element) flowNodes.item(i);
            String id = flow.getAttribute("id");
            String sourceRef = flow.getAttribute("sourceRef");
            String targetRef = flow.getAttribute("targetRef");
            
            if (id != null && !id.isEmpty() && sourceRef != null && targetRef != null) {
                flows.put(id, new FlowConnection(id, sourceRef, targetRef));
            }
        }
        
        return flows;
    }
    
    /**
     * Helper class to represent a flow connection.
     */
    private static class FlowConnection {
        final String id;
        final String sourceRef;
        final String targetRef;
        
        FlowConnection(String id, String sourceRef, String targetRef) {
            this.id = id;
            this.sourceRef = sourceRef;
            this.targetRef = targetRef;
        }
    }
}

