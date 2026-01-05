# Kogito Quick Start Guide

## Overview

Kogito is configured to run embedded in Spring Boot, load BPMN/DRL files from the filesystem, and auto-generate REST endpoints for process execution.

## Quick Setup

### 1. Directory Structure (Auto-Created on Startup)

```
data/
└── kogito/
    ├── processes/          # BPMN files (.bpmn)
    │   └── backups/        # Automatic backups
    └── rules/              # DRL files (.drl)
        └── backups/        # Automatic backups
```

### 2. Configuration (`application.yml`)

```yaml
app:
  kogito:
    bpmn-dir: ./data/kogito/processes
    drl-dir: ./data/kogito/rules

kogito:
  service:
    url: http://localhost:8080
  processes:
    management:
      enabled: true
  rest:
    enabled: true
  rules:
    enabled: true
```

## Quick Usage

### Deploy Process

```bash
# Deploy complete process (BPMN + DRL)
curl -X POST http://localhost:8080/api/kogito/deployments/proc-123 \
  -H "Content-Type: application/json" \
  -d '{
    "bpmnXml": "<?xml version=\"1.0\"...",
    "drlContent": "package com.example..."
  }'
```

### Start Process Instance

**After deployment, Kogito auto-generates**:
```bash
# Start process (Kogito auto-generated endpoint)
curl -X POST http://localhost:8080/proc-123 \
  -H "Content-Type: application/json" \
  -d '{
    "var1": "value1",
    "var2": "value2"
  }'

# Response includes instance ID
{
  "id": "abc-123-def",
  "var1": "value1",
  "var2": "value2"
}
```

### Query Process Instance

```bash
# Get instance status
curl http://localhost:8080/proc-123/abc-123-def

# List all instances
curl http://localhost:8080/proc-123
```

## Java API

### Deploy from Code

```java
@Autowired
private KogitoDeploymentService deploymentService;

// Deploy BPMN and DRL
DeploymentResult result = deploymentService.deployProcess(
    processId, 
    bpmnXml, 
    drlContent
);

// Now Kogito exposes: POST /{processId}
```

### Check Deployment

```java
// Check if deployed
boolean deployed = deploymentService.isDeployed(processId);

// Get deployment info
DeploymentInfo info = deploymentService.getDeploymentInfo(processId);

// List all deployments
List<String> processes = deploymentService.listDeployedProcesses();
```

### Undeploy

```java
// Undeploy process
boolean removed = deploymentService.undeployProcess(processId);
```

## REST API Reference

### Deployment Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/kogito/deployments/{id}` | Deploy process |
| POST | `/api/kogito/deployments/{id}/bpmn` | Deploy BPMN only |
| POST | `/api/kogito/deployments/{id}/drl` | Deploy DRL only |
| DELETE | `/api/kogito/deployments/{id}` | Undeploy |
| GET | `/api/kogito/deployments/{id}` | Get deployment info |
| GET | `/api/kogito/deployments` | List deployments |

### Kogito Auto-Generated Endpoints

For each deployed process `{processId}`, Kogito exposes:

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/{processId}` | Start process instance |
| GET | `/{processId}` | List instances |
| GET | `/{processId}/{instanceId}` | Get instance |
| DELETE | `/{processId}/{instanceId}` | Abort instance |
| GET | `/{processId}/{instanceId}/tasks` | Get tasks |
| POST | `/{processId}/{instanceId}/tasks/{taskId}` | Complete task |

## Example Workflow

```bash
# 1. Deploy process
curl -X POST http://localhost:8080/api/kogito/deployments/order-process \
  -H "Content-Type: application/json" \
  -d @deployment.json

# 2. Start process instance (Kogito endpoint)
curl -X POST http://localhost:8080/order-process \
  -H "Content-Type: application/json" \
  -d '{
    "orderAmount": 5000,
    "customerId": "CUST-001"
  }'

# Response:
{
  "id": "inst-12345",
  "orderAmount": 5000,
  "customerId": "CUST-001",
  "status": "ACTIVE"
}

# 3. Query instance
curl http://localhost:8080/order-process/inst-12345

# 4. List all instances
curl http://localhost:8080/order-process

# 5. Get tasks
curl http://localhost:8080/order-process/inst-12345/tasks
```

## Integration with AI Workflow

```java
@Service
public class ProcessPublisher {
    
    @Autowired
    private BpmnGeneratorService bpmnGenerator;
    
    @Autowired
    private DrlGeneratorService drlGenerator;
    
    @Autowired
    private KogitoDeploymentService kogito;
    
    @Autowired
    private AiOrchestratorService orchestrator;
    
    public void publish(String processId) {
        // 1. Generate BPMN
        String bpmn = bpmnGenerator.generateBpmn(model);
        
        // 2. Generate DRL
        String drl = drlGenerator.generateDrl(rules);
        
        // 3. Deploy to Kogito
        DeploymentResult result = kogito.deployProcess(
            processId, bpmn, drl
        );
        
        // 4. Update state
        orchestrator.updateState(processId, AiState.PUBLISHED);
        
        // 5. Now process is executable!
        logger.info("Process ready at: POST /{}", processId);
    }
}
```

## Features

✅ **Hot Reload** - Kogito auto-detects files in directories  
✅ **Auto REST** - Endpoints generated for each process  
✅ **Automatic Backups** - Timestamped backups on each deploy  
✅ **Process Management** - Built-in management endpoints  
✅ **Rule Execution** - Drools rules execute automatically  
✅ **Thread-Safe** - Concurrent process execution

## Monitoring

```bash
# Check deployment status
curl http://localhost:8080/api/kogito/deployments/proc-123/status

# List all deployed processes
curl http://localhost:8080/api/kogito/deployments

# Kogito management endpoints
curl http://localhost:8080/management/processes
```

## Troubleshooting

### Process not found
```bash
# Verify deployment
curl http://localhost:8080/api/kogito/deployments

# Check files
ls -la data/kogito/processes/
```

### Rules not executing
```bash
# Verify DRL deployment
curl http://localhost:8080/api/kogito/deployments/proc-123

# Check DRL file
cat data/kogito/rules/proc-123.drl
```

### Cannot start instance
```bash
# Check BPMN validation
curl http://localhost:8080/api/kogito/deployments/proc-123

# Re-deploy with valid BPMN
curl -X POST http://localhost:8080/api/kogito/deployments/proc-123/bpmn \
  -H "Content-Type: application/xml" \
  -d @validated.bpmn
```

## Files

- **Configuration**: `KogitoConfiguration.java`
- **Service**: `KogitoDeploymentService.java`
- **Controller**: `KogitoDeploymentController.java`
- **Config**: `application.yml`

## Documentation

- [KOGITO_SETUP.md](./KOGITO_SETUP.md) - Complete setup guide
- [BPMN_GENERATOR.md](./BPMN_GENERATOR.md) - BPMN generation
- [DRL_GENERATOR.md](./DRL_GENERATOR.md) - DRL generation
- [API.md](./API.md) - Full API reference

---

**Version**: Kogito 10.1.0  
**Spring Boot**: 3.4.3  
**Setup Date**: January 2, 2026

