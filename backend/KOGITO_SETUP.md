# Kogito Configuration Guide

## Overview

This document explains how Kogito is configured to run embedded in Spring Boot, load generated BPMN and DRL files from the filesystem, and expose REST endpoints for process execution.

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│              Spring Boot Application                     │
│  ┌──────────────────────────────────────────────────┐  │
│  │         Kogito Runtime (Embedded)                 │  │
│  │  ┌────────────────────────────────────────────┐  │  │
│  │  │      BPMN Process Engine (jBPM)            │  │  │
│  │  └────────────────────────────────────────────┘  │  │
│  │  ┌────────────────────────────────────────────┐  │  │
│  │  │      Drools Rule Engine                     │  │  │
│  │  └────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
           ↓                          ↓
    ┌──────────┐              ┌─────────────┐
    │   BPMN   │              │     DRL     │
    │  Files   │              │    Files    │
    └──────────┘              └─────────────┘
 ./data/kogito/processes  ./data/kogito/rules
```

## Directory Structure

```
ai-bpmn-compiler/
├── backend/
│   └── data/
│       └── kogito/
│           ├── processes/          # BPMN files (.bpmn)
│           │   ├── proc-123.bpmn
│           │   └── backups/        # Automatic backups
│           └── rules/              # DRL files (.drl)
│               ├── proc-123.drl
│               └── backups/        # Automatic backups
```

## Dependencies

### Gradle Dependencies (`build.gradle`)

```groovy
ext {
    kogitoVersion = '10.1.0'
}

dependencyManagement {
    imports {
        mavenBom "org.kie.kogito:kogito-spring-boot-bom:${kogitoVersion}"
    }
}

dependencies {
    // Kogito Core
    implementation 'org.kie.kogito:kogito-api'
    implementation 'org.kie.kogito:kogito-drools'
    implementation 'org.kie.kogito:kogito-processes'
    implementation 'org.kie.kogito:kogito-spring-boot-starter'
    
    // jBPM for BPMN execution
    implementation 'org.jbpm:jbpm-spring-boot-starter'
    
    // Kogito Addons
    implementation 'org.kie:kie-addons-springboot-process-management'
    implementation 'org.kie:kie-addons-springboot-rest-exception-handler'
    
    // Drools for rule execution
    implementation 'org.drools:drools-core'
    implementation 'org.drools:drools-compiler'
    implementation 'org.drools:drools-mvel'
}
```

## Configuration

### Application Configuration (`application.yml`)

```yaml
# Kogito directories
app:
  kogito:
    bpmn-dir: ./data/kogito/processes
    drl-dir: ./data/kogito/rules

# Kogito runtime configuration
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

# Logging
logging:
  level:
    org.kie.kogito: DEBUG
    org.jbpm: DEBUG
    org.drools: DEBUG
```

### Java Configuration (`KogitoConfiguration.java`)

The `KogitoConfiguration` class:
- Creates necessary directories on startup
- Initializes Kogito runtime
- Provides beans for directory paths
- Logs deployment status

```java
@Configuration
public class KogitoConfiguration {
    
    @Value("${app.kogito.bpmn-dir}")
    private String bpmnDirectory;
    
    @Value("${app.kogito.drl-dir}")
    private String drlDirectory;
    
    @PostConstruct
    public void initializeKogitoDirectories() {
        // Creates directories if they don't exist
        // Logs existing files
    }
}
```

## Services

### KogitoDeploymentService

Handles deployment of generated BPMN and DRL files to Kogito runtime.

**Key Methods**:

```java
// Deploy BPMN
Path deployBpmn(String processId, String bpmnXml)

// Deploy DRL
Path deployDrl(String processId, String drlContent)

// Deploy both
DeploymentResult deployProcess(String processId, String bpmnXml, String drlContent)

// Undeploy
boolean undeployProcess(String processId)

// Query
boolean isDeployed(String processId)
List<String> listDeployedProcesses()
DeploymentInfo getDeploymentInfo(String processId)
```

**Features**:
- Automatic backup creation
- File sanitization
- Deployment verification
- Thread-safe operations

## REST Endpoints

### Deployment Management

**Base Path**: `/api/kogito/deployments`

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/{processId}/bpmn` | Deploy BPMN file |
| POST | `/{processId}/drl` | Deploy DRL file |
| POST | `/{processId}` | Deploy both BPMN and DRL |
| DELETE | `/{processId}` | Undeploy process |
| GET | `/{processId}/status` | Check deployment status |
| GET | `/{processId}` | Get deployment info |
| GET | `/` | List all deployments |

### Kogito Process Execution

**Note**: Kogito automatically generates REST endpoints for each deployed process.

For a process with ID `processId`, Kogito exposes:

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/{processId}` | Start new process instance |
| GET | `/{processId}` | List process instances |
| GET | `/{processId}/{instanceId}` | Get process instance |
| DELETE | `/{processId}/{instanceId}` | Abort process instance |
| GET | `/{processId}/{instanceId}/tasks` | Get user tasks |
| POST | `/{processId}/{instanceId}/tasks/{taskId}` | Complete user task |

**Example Process Start**:
```bash
curl -X POST http://localhost:8080/proc-123 \
  -H "Content-Type: application/json" \
  -d '{
    "processData": "value"
  }'
```

## Usage Examples

### Example 1: Deploy Complete Process

```java
@Autowired
private KogitoDeploymentService deploymentService;

@Autowired
private BpmnGeneratorService bpmnGenerator;

@Autowired
private DrlGeneratorService drlGenerator;

public void generateAndDeploy(String processId) {
    // 1. Get process model
    ProcessModel model = getProcessModel(processId);
    
    // 2. Generate BPMN
    String bpmn = bpmnGenerator.generateBpmn(model);
    
    // 3. Generate DRL
    String drl = drlGenerator.generateDrl(model.getRules());
    
    // 4. Deploy to Kogito
    try {
        DeploymentResult result = deploymentService.deployProcess(
            processId, 
            bpmn, 
            drl
        );
        
        logger.info("Process deployed: {}", result);
        
        // 5. Now Kogito will expose REST endpoints for this process
        // POST /proc-123 to start instances
        
    } catch (IOException e) {
        logger.error("Deployment failed", e);
    }
}
```

### Example 2: Deploy via REST API

```bash
# Deploy BPMN
curl -X POST http://localhost:8080/api/kogito/deployments/proc-123/bpmn \
  -H "Content-Type: application/xml" \
  -d @process.bpmn

# Deploy DRL
curl -X POST http://localhost:8080/api/kogito/deployments/proc-123/drl \
  -H "Content-Type: text/plain" \
  -d @rules.drl

# Deploy both
curl -X POST http://localhost:8080/api/kogito/deployments/proc-123 \
  -H "Content-Type: application/json" \
  -d '{
    "bpmnXml": "<?xml version=\"1.0\"...",
    "drlContent": "package com.example..."
  }'
```

### Example 3: Check Deployment Status

```bash
# Check if deployed
curl http://localhost:8080/api/kogito/deployments/proc-123/status

# Response:
{
  "processId": "proc-123",
  "deployed": true,
  "bpmnSize": 5432,
  "bpmnLastModified": "2026-01-02T10:30:00",
  "hasDrl": true,
  "drlSize": 1234,
  "drlLastModified": "2026-01-02T10:30:01"
}
```

### Example 4: List Deployed Processes

```bash
curl http://localhost:8080/api/kogito/deployments

# Response:
{
  "count": 3,
  "processIds": ["proc-123", "proc-456", "proc-789"]
}
```

### Example 5: Start Process Instance

```bash
# Start a process instance (Kogito auto-generated endpoint)
curl -X POST http://localhost:8080/proc-123 \
  -H "Content-Type: application/json" \
  -d '{
    "orderAmount": 5000,
    "customerId": "CUST-001"
  }'

# Response:
{
  "id": "12345-67890-abcde",
  "orderAmount": 5000,
  "customerId": "CUST-001"
}
```

### Example 6: Query Process Instance

```bash
# Get process instance status
curl http://localhost:8080/proc-123/12345-67890-abcde

# Response:
{
  "id": "12345-67890-abcde",
  "processId": "proc-123",
  "state": 1,
  "variables": {
    "orderAmount": 5000,
    "customerId": "CUST-001"
  }
}
```

## Integration with AI Workflow

```java
@Service
public class ProcessPublishingService {
    
    @Autowired
    private BpmnGeneratorService bpmnGenerator;
    
    @Autowired
    private DrlGeneratorService drlGenerator;
    
    @Autowired
    private KogitoDeploymentService deploymentService;
    
    @Autowired
    private AiOrchestratorService orchestrator;
    
    public void publishProcess(String processId) {
        try {
            // 1. Get process model
            ProcessModel model = getProcessModel(processId);
            
            // 2. Generate BPMN
            String bpmn = bpmnGenerator.generateBpmn(model);
            
            // 3. Generate DRL
            String drl = drlGenerator.generateDrl(model.getRules());
            
            // 4. Deploy to Kogito
            DeploymentResult result = deploymentService.deployProcess(
                processId, 
                bpmn, 
                drl
            );
            
            // 5. Update orchestrator state
            orchestrator.updateState(processId, AiState.PUBLISHED);
            
            logger.info("Process published successfully: {}", processId);
            logger.info("Process can now be started at: POST /{}", processId);
            
        } catch (Exception e) {
            logger.error("Publishing failed for {}: {}", processId, e.getMessage());
            orchestrator.markAsFailed(processId, "Publishing failed: " + e.getMessage());
        }
    }
}
```

## Automatic Features

### 1. Hot Reload

Kogito automatically detects and loads BPMN and DRL files from the configured directories.

- **BPMN Files**: Placed in `./data/kogito/processes/`
- **DRL Files**: Placed in `./data/kogito/rules/`
- **Auto-detection**: Kogito scans these directories on startup
- **REST Endpoints**: Automatically generated for each process

### 2. Automatic Backups

Every deployment creates a timestamped backup:

```
data/kogito/processes/backups/
  └── proc-123.bpmn.20260102_103045.backup
data/kogito/rules/backups/
  └── proc-123.drl.20260102_103046.backup
```

### 3. REST API Generation

For each deployed process, Kogito automatically creates:
- Process instance management endpoints
- Task management endpoints
- Process definition queries
- Swagger/OpenAPI documentation

### 4. Process Management UI

Kogito provides built-in management endpoints:
- `/management/processes` - List processes
- `/management/processes/{processId}` - Process details
- `/management/processes/{processId}/instances` - Active instances

## Troubleshooting

### Issue: Process not found after deployment

**Cause**: File not in correct directory or incorrect filename

**Solution**:
```bash
# Check if file exists
ls -la ./data/kogito/processes/proc-123.bpmn

# Check logs
tail -f logs/application.log | grep Kogito
```

### Issue: REST endpoints not exposed

**Cause**: Kogito REST not enabled or process ID invalid

**Solution**:
```yaml
# Ensure in application.yml
kogito:
  rest:
    enabled: true
```

### Issue: Rules not executing

**Cause**: DRL not deployed or has syntax errors

**Solution**:
```bash
# Check DRL deployment
curl http://localhost:8080/api/kogito/deployments/proc-123/status

# Review DRL file
cat ./data/kogito/rules/proc-123.drl
```

### Issue: Cannot start process instances

**Cause**: Process not deployed or BPMN invalid

**Solution**:
```bash
# Verify deployment
curl http://localhost:8080/api/kogito/deployments

# Check BPMN validation
# Re-deploy with validation
curl -X POST http://localhost:8080/api/kogito/deployments/proc-123/bpmn \
  -H "Content-Type: application/xml" \
  -d @validated-process.bpmn
```

## Best Practices

### 1. Version Control

```java
// Include version in process ID
String processId = "order-process-v1.0.0";
deploymentService.deployProcess(processId, bpmn, drl);
```

### 2. Deployment Verification

```java
// Always verify after deployment
if (deploymentService.isDeployed(processId)) {
    logger.info("Deployment successful: {}", processId);
} else {
    logger.error("Deployment verification failed: {}", processId);
}
```

### 3. Clean Undeployment

```java
// Undeploy old versions before deploying new
if (deploymentService.isDeployed(oldProcessId)) {
    deploymentService.undeployProcess(oldProcessId);
}
deploymentService.deployProcess(newProcessId, bpmn, drl);
```

### 4. Error Handling

```java
try {
    deploymentService.deployProcess(processId, bpmn, drl);
} catch (IOException e) {
    logger.error("Deployment failed", e);
    orchestrator.recordGenerationFailure(
        processId, 
        "Deployment", 
        e.getMessage()
    );
}
```

### 5. Monitoring

```java
// Regular deployment health check
List<String> deployed = deploymentService.listDeployedProcesses();
logger.info("Currently deployed processes: {}", deployed.size());
```

## Security Considerations

1. **File System Access**: Ensure proper file permissions on Kogito directories
2. **REST Endpoints**: Add authentication/authorization to Kogito endpoints
3. **Input Validation**: Always validate BPMN and DRL before deployment
4. **Process Variables**: Sanitize all process input data

## Performance Tips

1. **Batch Deployments**: Deploy multiple processes in sequence to minimize I/O
2. **Caching**: Kogito caches process definitions automatically
3. **Resource Limits**: Monitor memory usage with many deployed processes
4. **Cleanup**: Regularly undeploy unused processes

## Related Documentation

- [Kogito Official Docs](https://docs.jboss.org/kogito/release/latest/html_single/)
- [BPMN_GENERATOR.md](./BPMN_GENERATOR.md) - BPMN generation
- [DRL_GENERATOR.md](./DRL_GENERATOR.md) - DRL generation
- [API.md](./API.md) - API documentation

## Files Created

- `KogitoConfiguration.java` - Configuration class
- `KogitoDeploymentService.java` - Deployment service
- `KogitoDeploymentController.java` - REST controller
- `application.yml` - Updated configuration
- `build.gradle` - Updated dependencies

---

**Setup Date**: January 2, 2026  
**Kogito Version**: 10.1.0  
**Spring Boot**: 3.4.3  
**Java**: 17

