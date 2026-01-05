# AI-BPMN Compiler API Documentation

## Overview

REST API for the AI-BPMN Compiler application. This API allows uploading BPMN diagram images for AI-powered process generation.

**Base URL**: `http://localhost:8080/api`

---

## Endpoints

### 1. Create Process from Text

Create a BPMN process from a plain text description.

**Endpoint**: `POST /api/process/from-text`

**Content-Type**: `application/json`

**Request Body**:

```json
{
  "description": "Order approval process: receive order, validate, approve if amount < $1000, send to manager if amount >= $1000, ship when approved",
  "name": "Order Approval Process"
}
```

**Request Fields**:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `description` | String | Yes | Plain text description of the process (max 10,000 characters) |
| `name` | String | No | Optional custom name for the process (defaults to first 50 chars of description) |

**Success Response**:

**Status Code**: `201 Created`

```json
{
  "processId": "proc-b3c4d5e6",
  "name": "Order Approval Process",
  "descriptionLength": 145,
  "status": "SUCCESS",
  "message": "Process created successfully from text description"
}
```

**Response Fields**:

| Field | Type | Description |
|-------|------|-------------|
| `processId` | String | Generated unique process identifier |
| `name` | String | Process name (custom or auto-generated) |
| `descriptionLength` | Integer | Length of the description in characters |
| `status` | String | Status of the operation (`SUCCESS` or `ERROR`) |
| `message` | String | Human-readable message |

**Error Responses**:

```json
// Empty description
{
  "status": "ERROR",
  "message": "Process description cannot be empty",
  "error": "INVALID_REQUEST"
}

// Description too long
{
  "status": "ERROR",
  "message": "Description too long. Maximum 10,000 characters allowed",
  "error": "INVALID_REQUEST"
}
```

---

### 2. Get Text Description

Retrieve the stored text description for a process.

**Endpoint**: `GET /api/process/{processId}/text`

**Path Parameters**:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `processId` | String | Yes | The process identifier |

**Success Response**:

**Status Code**: `200 OK`

```json
{
  "processId": "proc-b3c4d5e6",
  "description": "Order approval process: receive order, validate, approve if amount < $1000..."
}
```

**Error Response**:

**Status Code**: `404 Not Found` - When process text description doesn't exist

---

### 3. Upload Process Image

Upload a BPMN process diagram image for processing.

**Endpoint**: `POST /api/process/from-image`

**Content-Type**: `multipart/form-data`

**Request Parameters**:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `file` | File | Yes | The image file (PNG or JPEG) |

**Constraints**:
- **File Types**: PNG, JPEG, JPG only
- **Max File Size**: 10 MB
- **Content-Type**: Must be `image/png`, `image/jpeg`, or `image/jpg`

**Success Response**:

**Status Code**: `201 Created`

```json
{
  "processId": "proc-a1b2c3d4",
  "fileName": "diagram.png",
  "filePath": "./data/uploads/proc-a1b2c3d4/original.png",
  "fileSize": 524288,
  "status": "SUCCESS",
  "message": "Image uploaded successfully"
}
```

**Response Fields**:

| Field | Type | Description |
|-------|------|-------------|
| `processId` | String | Generated unique process identifier (format: `proc-{8-char-uuid}`) |
| `fileName` | String | Original filename of the uploaded file |
| `filePath` | String | Server path where the file is stored |
| `fileSize` | Long | Size of the uploaded file in bytes |
| `status` | String | Status of the operation (`SUCCESS` or `ERROR`) |
| `message` | String | Human-readable message |

---

### Error Responses

#### 400 Bad Request - Invalid File

Returned when the uploaded file doesn't meet validation criteria.

```json
{
  "status": "ERROR",
  "message": "Invalid file type. Only PNG and JPEG images are allowed. Received: application/pdf",
  "error": "INVALID_FILE"
}
```

**Common Invalid File Errors**:
- Empty or null file
- File size exceeds 10 MB
- Invalid content type (not PNG/JPEG)
- Invalid file extension (not .png, .jpg, .jpeg)
- Missing filename

#### 500 Internal Server Error - File Storage Error

Returned when file storage operations fail.

```json
{
  "status": "ERROR",
  "message": "Failed to store file: Permission denied",
  "error": "FILE_STORAGE_ERROR"
}
```

---

## Usage Examples

### cURL

#### Create Process from Text

```bash
curl -X POST http://localhost:8080/api/process/from-text \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Customer onboarding: collect info, verify identity, create account, send welcome email",
    "name": "Customer Onboarding"
  }'
```

#### Get Text Description

```bash
curl -X GET http://localhost:8080/api/process/proc-12345678/text
```

#### Upload PNG Image

```bash
curl -X POST http://localhost:8080/api/process/from-image \
  -F "file=@/path/to/diagram.png" \
  -H "Content-Type: multipart/form-data"
```

#### Upload JPEG Image

```bash
curl -X POST http://localhost:8080/api/process/from-image \
  -F "file=@/path/to/process-diagram.jpg"
```

### JavaScript (Fetch API)

#### Create Process from Text

```javascript
async function createProcessFromText(description, name = null) {
  const requestBody = { description };
  if (name) {
    requestBody.name = name;
  }
  
  try {
    const response = await fetch('http://localhost:8080/api/process/from-text', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(requestBody)
    });
    
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message);
    }
    
    const result = await response.json();
    console.log('Process ID:', result.processId);
    console.log('Process Name:', result.name);
    
    return result;
  } catch (error) {
    console.error('Failed to create process:', error);
    throw error;
  }
}

// Usage
const description = "Order processing: receive, validate, approve, ship";
createProcessFromText(description, "Order Process");
```

#### Upload Process Image

```javascript
async function uploadProcessImage(file) {
  const formData = new FormData();
  formData.append('file', file);
  
  try {
    const response = await fetch('http://localhost:8080/api/process/from-image', {
      method: 'POST',
      body: formData
    });
    
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message);
    }
    
    const result = await response.json();
    console.log('Process ID:', result.processId);
    console.log('File stored at:', result.filePath);
    
    return result;
  } catch (error) {
    console.error('Upload failed:', error);
    throw error;
  }
}

// Usage
const fileInput = document.getElementById('fileInput');
const file = fileInput.files[0];
uploadProcessImage(file);
```

### React Example

```jsx
import React, { useState } from 'react';

function ProcessImageUploader() {
  const [file, setFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  
  const handleFileChange = (e) => {
    setFile(e.target.files[0]);
    setError(null);
  };
  
  const handleUpload = async () => {
    if (!file) {
      setError('Please select a file');
      return;
    }
    
    setUploading(true);
    setError(null);
    
    const formData = new FormData();
    formData.append('file', file);
    
    try {
      const response = await fetch('http://localhost:8080/api/process/from-image', {
        method: 'POST',
        body: formData
      });
      
      const data = await response.json();
      
      if (!response.ok) {
        throw new Error(data.message || 'Upload failed');
      }
      
      setResult(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setUploading(false);
    }
  };
  
  return (
    <div>
      <input 
        type="file" 
        accept="image/png,image/jpeg" 
        onChange={handleFileChange} 
      />
      <button onClick={handleUpload} disabled={uploading}>
        {uploading ? 'Uploading...' : 'Upload Process Image'}
      </button>
      
      {error && <div className="error">{error}</div>}
      
      {result && (
        <div className="success">
          <h3>Upload Successful!</h3>
          <p>Process ID: {result.processId}</p>
          <p>File: {result.fileName}</p>
          <p>Size: {(result.fileSize / 1024).toFixed(2)} KB</p>
        </div>
      )}
    </div>
  );
}

export default ProcessImageUploader;
```

### Python (requests)

```python
import requests

def upload_process_image(file_path):
    """Upload a BPMN process image"""
    url = 'http://localhost:8080/api/process/from-image'
    
    with open(file_path, 'rb') as f:
        files = {'file': (file_path, f, 'image/png')}
        response = requests.post(url, files=files)
    
    if response.status_code == 201:
        result = response.json()
        print(f"Success! Process ID: {result['processId']}")
        return result
    else:
        error = response.json()
        raise Exception(f"Upload failed: {error['message']}")

# Usage
result = upload_process_image('/path/to/diagram.png')
print(f"File stored at: {result['filePath']}")
```

### Java (Spring RestTemplate)

```java
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;

public class ProcessImageClient {
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final String apiUrl = "http://localhost:8080/api/process/from-image";
    
    public ProcessUploadResponse uploadImage(File imageFile) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(imageFile));
        
        HttpEntity<MultiValueMap<String, Object>> requestEntity = 
            new HttpEntity<>(body, headers);
        
        ResponseEntity<ProcessUploadResponse> response = restTemplate.exchange(
            apiUrl,
            HttpMethod.POST,
            requestEntity,
            ProcessUploadResponse.class
        );
        
        return response.getBody();
    }
}
```

---

## File Storage

### Directory Structure

Uploaded files are stored in the following structure:

```
./data/uploads/
└── {processId}/
    └── original.{ext}
```

**Example**:
```
./data/uploads/
├── proc-a1b2c3d4/
│   └── original.png
├── proc-e5f6g7h8/
│   └── original.jpeg
└── proc-i9j0k1l2/
    └── original.jpg
```

### File Naming

- Original uploaded file is always saved as `original.{extension}`
- Extension matches the uploaded file's extension (png, jpg, jpeg)
- Each process has its own dedicated directory

---

## Process Model Creation

When an image is successfully uploaded:

1. **Process ID Generated**: Format `proc-{8-char-uuid}`
2. **File Stored**: In `./data/uploads/{processId}/original.{ext}`
3. **ProcessModel Created**:
   ```json
   {
     "id": "proc-a1b2c3d4",
     "name": "Process from Image - proc-a1b2c3d4",
     "version": "1.0.0",
     "status": "DRAFT",
     "nodes": [],
     "edges": [],
     "rules": []
   }
   ```

The empty `ProcessModel` with `DRAFT` status is ready for AI processing.

---

## Validation Rules

### File Type Validation

```java
✅ Allowed: image/png, image/jpeg, image/jpg
❌ Rejected: application/pdf, text/plain, image/gif, etc.
```

### File Extension Validation

```java
✅ Allowed: .png, .jpg, .jpeg
❌ Rejected: .pdf, .gif, .txt, .doc, etc.
```

### File Size Validation

```java
✅ Allowed: 0 < size ≤ 10 MB
❌ Rejected: size > 10 MB or empty files
```

### Validation Order

1. Check if file is null or empty
2. Check file size (≤ 10 MB)
3. Check content type (must be image/png or image/jpeg)
4. Check file extension (must be .png, .jpg, or .jpeg)
5. Check filename is present

---

## Testing

### Test with Valid Image

```bash
# Create a test PNG image (if you have ImageMagick)
convert -size 800x600 xc:white test-diagram.png

# Upload
curl -X POST http://localhost:8080/api/process/from-image \
  -F "file=@test-diagram.png" \
  -v
```

Expected: **201 Created** with process ID

### Test with Invalid File Type

```bash
echo "test" > test.txt

curl -X POST http://localhost:8080/api/process/from-image \
  -F "file=@test.txt" \
  -v
```

Expected: **400 Bad Request** with error message

### Test with Large File

```bash
# Create 11 MB file
dd if=/dev/zero of=large.png bs=1M count=11

curl -X POST http://localhost:8080/api/process/from-image \
  -F "file=@large.png" \
  -v
```

Expected: **400 Bad Request** - file size exceeds limit

---

## Configuration

### application.yml

```yaml
spring:
  servlet:
    multipart:
      enabled: true
      max-file-size: 10MB      # Maximum file size
      max-request-size: 10MB   # Maximum request size

app:
  upload:
    base-dir: ./data/uploads   # Base directory for uploads
```

### Customizing File Size Limit

To change the maximum file size, update `application.yml`:

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 20MB    # Change to 20MB
      max-request-size: 20MB
```

### Customizing Upload Directory

```yaml
app:
  upload:
    base-dir: /var/app/uploads  # Absolute path
```

---

## Security Considerations

### Current Implementation

- ✅ File type validation (content-type and extension)
- ✅ File size limits (10 MB)
- ✅ Dedicated directories per process
- ✅ No arbitrary path traversal
- ⚠️ **No authentication** (as per requirements)
- ⚠️ **CORS enabled for all origins** (development mode)

### Production Recommendations

For production deployment, consider adding:

1. **Authentication & Authorization**
   ```java
   @PreAuthorize("hasRole('USER')")
   @PostMapping("/from-image")
   ```

2. **Rate Limiting**
   - Limit uploads per user/IP
   - Prevent abuse

3. **Virus Scanning**
   - Scan uploaded files for malware
   - Use ClamAV or similar

4. **CORS Restrictions**
   ```java
   @CrossOrigin(origins = "https://yourdomain.com")
   ```

5. **File Cleanup**
   - Implement retention policy
   - Clean up old/unused files

---

## Troubleshooting

### Problem: "Failed to store file: Permission denied"

**Solution**: Ensure the application has write permissions to the upload directory:

```bash
mkdir -p ./data/uploads
chmod 755 ./data/uploads
```

### Problem: "File size exceeds maximum"

**Solution**: Check `spring.servlet.multipart.max-file-size` in `application.yml`

### Problem: "Invalid file type"

**Solution**: Ensure the file is actually PNG or JPEG. Check:
- File extension matches content
- Content-Type header is correct
- File is not corrupted

---

## Next Steps

After successful upload:

1. **Process Image**: Use AI/ML service to extract BPMN diagram
2. **Generate Nodes**: Create `ProcessNode` entities from diagram
3. **Generate Edges**: Create `ProcessEdge` connections
4. **Add Explanations**: Generate `Explanation` for each node
5. **Request Approval**: Create `Approval` records
6. **Publish Process**: Change status from DRAFT to PUBLISHED

---

## See Also

- [Model Classes Documentation](MODEL_CLASSES.md)
- [Repository Documentation](REPOSITORIES.md)
- [Spring Boot Multipart File Upload](https://spring.io/guides/gs/uploading-files/)

---

# AI Orchestrator API

## Overview

The AI Orchestrator manages the workflow state of AI-driven BPMN process compilation. It tracks processes through various stages from initial input to final publication.

**Base URL**: `http://localhost:8080/api/orchestrator`

---

## AI State Workflow

```
IMAGE_RECEIVED / TEXT_RECEIVED
    ↓
PROCESS_INFERRED
    ↓
MODEL_READY (requires approval)
    ↓
BPMN_GENERATED
    ↓
DRL_GENERATED
    ↓
PUBLISHED (complete)

CLARIFICATION_REQUIRED (requires user input)
FAILED (can retry)
```

---

## Endpoints

### 1. Start AI Inference

Start AI processing for a process that has received input (image or text).

**Endpoint**: `POST /api/orchestrator/{processId}/start-inference`

**Path Parameters**:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `processId` | String | Yes | The process identifier |

**Preconditions**:
- Process must exist
- Process must be in `IMAGE_RECEIVED` or `TEXT_RECEIVED` state

**Success Response**:

**Status Code**: `200 OK`

```json
{
  "processId": "proc-a1b2c3d4",
  "currentState": "PROCESS_INFERRED",
  "nextState": "MODEL_READY",
  "description": "AI has inferred a preliminary process model from the input.",
  "requiresUserAction": false,
  "complete": false,
  "failed": false
}
```

**Error Responses**:

```json
// Process not found
{
  "error": "Process not found: proc-xyz"
}

// Invalid state
{
  "error": "Cannot start inference: process proc-a1b2c3d4 is already in state PROCESS_INFERRED"
}
```

**Example**:

```bash
curl -X POST http://localhost:8080/api/orchestrator/proc-a1b2c3d4/start-inference
```

---

### 2. Approve Step

Approve a step and advance to the next state. Used when process is in `MODEL_READY` or `CLARIFICATION_REQUIRED` state.

**Endpoint**: `POST /api/orchestrator/{processId}/approve`

**Path Parameters**:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `processId` | String | Yes | The process identifier |

**Query Parameters**:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `stepId` | String | No | Optional step identifier for tracking |

**Preconditions**:
- Process must exist
- Process must be in `MODEL_READY` or `CLARIFICATION_REQUIRED` state

**Success Response**:

**Status Code**: `200 OK`

```json
{
  "processId": "proc-a1b2c3d4",
  "currentState": "BPMN_GENERATED",
  "nextState": "DRL_GENERATED",
  "description": "BPMN XML representation of the process has been generated.",
  "requiresUserAction": false,
  "complete": false,
  "failed": false
}
```

**Error Responses**:

```json
// Not in approvable state
{
  "error": "Cannot approve: process proc-a1b2c3d4 is in state PROCESS_INFERRED which doesn't require approval"
}
```

**Example**:

```bash
# With step ID
curl -X POST "http://localhost:8080/api/orchestrator/proc-a1b2c3d4/approve?stepId=model-review"

# Without step ID
curl -X POST http://localhost:8080/api/orchestrator/proc-a1b2c3d4/approve
```

---

### 3. Retry Failed Process

Retry a process that has failed. Resets the process to initial state.

**Endpoint**: `POST /api/orchestrator/{processId}/retry`

**Path Parameters**:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `processId` | String | Yes | The process identifier |

**Preconditions**:
- Process must exist
- Process must be in `FAILED` state

**Success Response**:

**Status Code**: `200 OK`

```json
{
  "processId": "proc-a1b2c3d4",
  "currentState": "TEXT_RECEIVED",
  "nextState": "PROCESS_INFERRED",
  "description": "Text description has been provided and is awaiting AI processing.",
  "requiresUserAction": false,
  "complete": false,
  "failed": false
}
```

**Error Responses**:

```json
// Not in FAILED state
{
  "error": "Cannot retry: process proc-a1b2c3d4 is in state MODEL_READY, not FAILED"
}
```

**Example**:

```bash
curl -X POST http://localhost:8080/api/orchestrator/proc-a1b2c3d4/retry
```

---

### 4. Get Current State

Get the current AI processing state for a process.

**Endpoint**: `GET /api/orchestrator/{processId}/state`

**Path Parameters**:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `processId` | String | Yes | The process identifier |

**Success Response**:

**Status Code**: `200 OK`

```json
{
  "processId": "proc-a1b2c3d4",
  "currentState": "BPMN_GENERATED",
  "nextState": "DRL_GENERATED",
  "description": "BPMN XML representation of the process has been generated.",
  "requiresUserAction": false,
  "complete": false,
  "failed": false
}
```

**Error Responses**:

```json
// Process not tracked
{
  "error": "Process not tracked: proc-xyz"
}
```

**Example**:

```bash
curl -X GET http://localhost:8080/api/orchestrator/proc-a1b2c3d4/state
```

---

### 5. Mark as Failed

Mark a process as failed with an optional reason.

**Endpoint**: `POST /api/orchestrator/{processId}/fail`

**Path Parameters**:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `processId` | String | Yes | The process identifier |

**Query Parameters**:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `reason` | String | No | Optional failure reason |

**Success Response**:

**Status Code**: `200 OK`

```json
{
  "processId": "proc-a1b2c3d4",
  "currentState": "FAILED",
  "nextState": null,
  "description": "An error occurred during AI processing or artifact generation.",
  "requiresUserAction": false,
  "complete": false,
  "failed": true
}
```

**Example**:

```bash
# With reason
curl -X POST "http://localhost:8080/api/orchestrator/proc-a1b2c3d4/fail?reason=AI+service+timeout"

# Without reason
curl -X POST http://localhost:8080/api/orchestrator/proc-a1b2c3d4/fail
```

---

### 6. Advance State

Manually advance to the next state in the workflow.

**Endpoint**: `POST /api/orchestrator/{processId}/advance`

**Path Parameters**:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `processId` | String | Yes | The process identifier |

**Preconditions**:
- Process must be able to advance (not in terminal or clarification state)

**Success Response**:

**Status Code**: `200 OK`

```json
{
  "processId": "proc-a1b2c3d4",
  "currentState": "DRL_GENERATED",
  "nextState": "PUBLISHED",
  "description": "Drools Rule Language (DRL) rules have been generated.",
  "requiresUserAction": false,
  "complete": false,
  "failed": false
}
```

**Error Responses**:

```json
// Cannot advance
{
  "error": "Cannot advance from current state"
}

// At terminal state
{
  "error": "Cannot advance from state PUBLISHED"
}
```

**Example**:

```bash
curl -X POST http://localhost:8080/api/orchestrator/proc-a1b2c3d4/advance
```

---

### 7. Check if Can Advance

Check if a process can advance to the next state.

**Endpoint**: `GET /api/orchestrator/{processId}/can-advance`

**Path Parameters**:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `processId` | String | Yes | The process identifier |

**Success Response**:

**Status Code**: `200 OK`

```json
{
  "processId": "proc-a1b2c3d4",
  "canAdvance": true
}
```

**Example**:

```bash
curl -X GET http://localhost:8080/api/orchestrator/proc-a1b2c3d4/can-advance
```

---

### 8. Get Tracked Process Count

Get the number of processes currently being tracked by the orchestrator.

**Endpoint**: `GET /api/orchestrator/tracked-count`

**Success Response**:

**Status Code**: `200 OK`

```json
{
  "trackedProcessCount": 42
}
```

**Example**:

```bash
curl -X GET http://localhost:8080/api/orchestrator/tracked-count
```

---

## AI State Reference

### State Descriptions

| State | Description | Can Advance? | Requires User Action? | Terminal? |
|-------|-------------|--------------|----------------------|-----------|
| `IMAGE_RECEIVED` | Image uploaded, awaiting processing | Yes | No | No |
| `TEXT_RECEIVED` | Text description provided, awaiting processing | Yes | No | No |
| `PROCESS_INFERRED` | AI has inferred preliminary model | Yes | No | No |
| `CLARIFICATION_REQUIRED` | AI needs user input to proceed | No | Yes | No |
| `MODEL_READY` | Model ready for review and approval | Yes | Yes | No |
| `BPMN_GENERATED` | BPMN XML has been generated | Yes | No | No |
| `DRL_GENERATED` | Drools rules have been generated | Yes | No | No |
| `PUBLISHED` | Process is complete and published | No | No | Yes |
| `FAILED` | An error occurred during processing | No (use retry) | No | Yes |

### State Transitions

**Normal Flow**:
```
IMAGE_RECEIVED → PROCESS_INFERRED → MODEL_READY → BPMN_GENERATED → DRL_GENERATED → PUBLISHED
```

**Alternative Flow**:
```
TEXT_RECEIVED → PROCESS_INFERRED → MODEL_READY → BPMN_GENERATED → DRL_GENERATED → PUBLISHED
```

**Clarification Flow**:
```
PROCESS_INFERRED → CLARIFICATION_REQUIRED → (user provides input) → PROCESS_INFERRED
```

**Failure Flow**:
```
Any State → FAILED → (retry) → TEXT_RECEIVED
```

---

## Usage Examples

### Complete Workflow Example

```bash
# 1. Create process from text
PROCESS_ID=$(curl -s -X POST http://localhost:8080/api/process/from-text \
  -H "Content-Type: application/json" \
  -d '{"description": "Order processing workflow"}' \
  | jq -r '.processId')

echo "Created process: $PROCESS_ID"

# 2. Start AI inference
curl -X POST http://localhost:8080/api/orchestrator/$PROCESS_ID/start-inference

# 3. Check current state
curl -X GET http://localhost:8080/api/orchestrator/$PROCESS_ID/state

# 4. Advance to MODEL_READY
curl -X POST http://localhost:8080/api/orchestrator/$PROCESS_ID/advance

# 5. Approve the model
curl -X POST http://localhost:8080/api/orchestrator/$PROCESS_ID/approve

# 6. Advance through remaining states
curl -X POST http://localhost:8080/api/orchestrator/$PROCESS_ID/advance
curl -X POST http://localhost:8080/api/orchestrator/$PROCESS_ID/advance

# 7. Verify published
curl -X GET http://localhost:8080/api/orchestrator/$PROCESS_ID/state
```

### JavaScript/TypeScript Example

```typescript
class AiOrchestratorClient {
  private baseUrl = 'http://localhost:8080/api/orchestrator';

  async startInference(processId: string) {
    const response = await fetch(`${this.baseUrl}/${processId}/start-inference`, {
      method: 'POST'
    });
    return await response.json();
  }

  async approveStep(processId: string, stepId?: string) {
    const url = stepId 
      ? `${this.baseUrl}/${processId}/approve?stepId=${stepId}`
      : `${this.baseUrl}/${processId}/approve`;
    
    const response = await fetch(url, { method: 'POST' });
    return await response.json();
  }

  async retry(processId: string) {
    const response = await fetch(`${this.baseUrl}/${processId}/retry`, {
      method: 'POST'
    });
    return await response.json();
  }

  async getState(processId: string) {
    const response = await fetch(`${this.baseUrl}/${processId}/state`);
    return await response.json();
  }

  async markAsFailed(processId: string, reason?: string) {
    const url = reason
      ? `${this.baseUrl}/${processId}/fail?reason=${encodeURIComponent(reason)}`
      : `${this.baseUrl}/${processId}/fail`;
    
    const response = await fetch(url, { method: 'POST' });
    return await response.json();
  }

  async advanceState(processId: string) {
    const response = await fetch(`${this.baseUrl}/${processId}/advance`, {
      method: 'POST'
    });
    return await response.json();
  }

  async canAdvance(processId: string) {
    const response = await fetch(`${this.baseUrl}/${processId}/can-advance`);
    return await response.json();
  }

  async getTrackedCount() {
    const response = await fetch(`${this.baseUrl}/tracked-count`);
    return await response.json();
  }
}

// Usage
const client = new AiOrchestratorClient();

async function processWorkflow(processId: string) {
  // Start inference
  let state = await client.startInference(processId);
  console.log('Started inference:', state.currentState);

  // Advance to MODEL_READY
  await client.advanceState(processId);
  
  // Approve model
  state = await client.approveStep(processId, 'model-review');
  console.log('Approved model:', state.currentState);

  // Continue advancing until published
  while (await client.canAdvance(processId)) {
    state = await client.advanceState(processId);
    console.log('Advanced to:', state.currentState);
  }

  console.log('Workflow complete!');
}
```

---

## Clarification Workflow

### 1. Get Clarification Request

Get pending clarification questions for a process.

**Endpoint**: `GET /api/orchestrator/{processId}/clarification`

**Success Response**:

```json
{
  "processId": "proc-123",
  "questions": [
    "Who is responsible for approving high-value orders?",
    "What is the threshold for 'high value'?"
  ],
  "context": "Approval workflow details are unclear"
}
```

**Example**:
```bash
curl -X GET http://localhost:8080/api/orchestrator/proc-123/clarification
```

---

### 2. Submit Clarification Response

Submit answers to clarification questions and resume inference.

**Endpoint**: `POST /api/orchestrator/{processId}/clarification`

**Request Body**:

```json
{
  "answers": {
    "Who is responsible for approving high-value orders?": "Manager or Department Head",
    "What is the threshold for 'high value'?": "Orders over $10,000"
  },
  "additionalNotes": "High-value orders also require finance review if over $50,000"
}
```

**Success Response**:

```json
{
  "processId": "proc-123",
  "currentState": "PROCESS_INFERRED",
  "nextState": "MODEL_READY",
  "description": "AI has inferred a preliminary process model from the input.",
  "requiresUserAction": false,
  "complete": false,
  "failed": false
}
```

**Example**:
```bash
curl -X POST http://localhost:8080/api/orchestrator/proc-123/clarification \
  -H "Content-Type: application/json" \
  -d '{
    "answers": {
      "Who approves?": "Manager",
      "What threshold?": "$10,000"
    }
  }'
```

---

### 3. Check Pending Clarification

Check if a process has pending clarification.

**Endpoint**: `GET /api/orchestrator/{processId}/has-clarification`

**Success Response**:

```json
{
  "processId": "proc-123",
  "hasPendingClarification": true
}
```

**Example**:
```bash
curl -X GET http://localhost:8080/api/orchestrator/proc-123/has-clarification
```

---

### 4. Cancel Clarification

Cancel clarification and mark process as failed.

**Endpoint**: `DELETE /api/orchestrator/{processId}/clarification`

**Success Response**:

```json
{
  "processId": "proc-123",
  "currentState": "FAILED",
  "nextState": null,
  "description": "An error occurred during AI processing or artifact generation.",
  "requiresUserAction": false,
  "complete": false,
  "failed": true
}
```

**Example**:
```bash
curl -X DELETE http://localhost:8080/api/orchestrator/proc-123/clarification
```

---

### 5. Get Clarification Count

Get count of processes with pending clarification.

**Endpoint**: `GET /api/orchestrator/clarification-count`

**Success Response**:

```json
{
  "pendingClarificationCount": 5
}
```

**Example**:
```bash
curl -X GET http://localhost:8080/api/orchestrator/clarification-count
```

---

### Complete Clarification Flow Example

```bash
# 1. Create process
PROCESS_ID=$(curl -s -X POST http://localhost:8080/api/process/from-text \
  -H "Content-Type: application/json" \
  -d '{"description": "Approval workflow with unclear steps"}' \
  | jq -r '.processId')

# 2. Start inference (AI detects ambiguity)
curl -X POST http://localhost:8080/api/orchestrator/$PROCESS_ID/start-inference

# 3. Check state (should be CLARIFICATION_REQUIRED)
curl -X GET http://localhost:8080/api/orchestrator/$PROCESS_ID/state

# 4. Get clarification questions
curl -X GET http://localhost:8080/api/orchestrator/$PROCESS_ID/clarification

# 5. Submit answers
curl -X POST http://localhost:8080/api/orchestrator/$PROCESS_ID/clarification \
  -H "Content-Type: application/json" \
  -d '{
    "answers": {
      "Who approves high-value orders?": "Department Manager",
      "What is the threshold?": "$5,000"
    },
    "additionalNotes": "Orders over $50k need VP approval"
  }'

# 6. Continue workflow (AI resumes with clarification)
curl -X POST http://localhost:8080/api/orchestrator/$PROCESS_ID/advance
```

---

### JavaScript Clarification Example

```typescript
async function handleClarificationWorkflow(processId: string) {
  // Check if clarification is needed
  const hasClarf = await fetch(`/api/orchestrator/${processId}/has-clarification`);
  const {hasPendingClarification} = await hasClarf.json();
  
  if (hasPendingClarification) {
    // Get clarification questions
    const clarResp = await fetch(`/api/orchestrator/${processId}/clarification`);
    const clarification = await clarResp.json();
    
    console.log('Clarification needed:');
    clarification.questions.forEach((q: string) => console.log(`  - ${q}`));
    
    // Collect user answers (in real app, show UI)
    const answers = {
      [clarification.questions[0]]: "Manager",
      [clarification.questions[1]]: "$10,000"
    };
    
    // Submit clarification
    const submitResp = await fetch(`/api/orchestrator/${processId}/clarification`, {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({
        answers,
        additionalNotes: "Additional context from user"
      })
    });
    
    const result = await submitResp.json();
    console.log('Clarification submitted, resuming:', result.currentState);
  }
}
```

---

## Error Handling

All endpoints return appropriate HTTP status codes:

- **200 OK**: Successful operation
- **404 Not Found**: Process not found or not tracked
- **409 Conflict**: Invalid state transition
- **500 Internal Server Error**: Server error

Error response format:

```json
{
  "error": "Descriptive error message"
}
```

---

## Integration with Process Creation

When creating a process, the orchestrator automatically sets the initial state:

```bash
# Create from text → sets TEXT_RECEIVED
curl -X POST http://localhost:8080/api/process/from-text \
  -H "Content-Type: application/json" \
  -d '{"description": "My process"}'

# Create from image → sets IMAGE_RECEIVED
curl -X POST http://localhost:8080/api/process/from-image \
  -F "file=@diagram.png"
```

---

## Process Lifecycle Endpoints

### Base Path: `/api/process`

High-level endpoints for publishing and executing processes.

### 1. Publish Process

Publish a process to Kogito runtime (generates BPMN, DRL, validates, and deploys).

**Endpoint**: `POST /api/process/{processId}/publish`

**Path Parameters**:
| Parameter   | Type   | Required | Description                     |
|-------------|--------|----------|---------------------------------|
| `processId` | String | Yes      | The process identifier          |

**Success Response**:
**Status Code**: `200 OK`
```json
{
  "processId": "proc-123",
  "status": "PUBLISHED",
  "bpmnPath": "./data/kogito/processes/proc-123.bpmn",
  "drlPath": "./data/kogito/rules/proc-123.drl",
  "message": "Process published successfully. Execute at: POST /proc-123",
  "executeEndpoint": "/api/process/proc-123/execute",
  "kogitoEndpoint": "/proc-123"
}
```

**Error Responses**:
- `404 NOT_FOUND` - Process not found
- `409 CONFLICT` - Process not ready for publish (e.g., needs clarification)
- `500 INTERNAL_SERVER_ERROR` - Publishing failed (BPMN/DRL generation or validation error)

**Workflow**:
1. Retrieves process model from repository
2. Generates BPMN 2.0 XML
3. Validates BPMN structure
4. Generates DRL from rules
5. Deploys to Kogito
6. Marks process as PUBLISHED
7. Returns deployment details

### 2. Execute Process

Start a new process instance (only PUBLISHED processes can execute).

**Endpoint**: `POST /api/process/{processId}/execute`

**Path Parameters**:
| Parameter   | Type   | Required | Description                     |
|-------------|--------|----------|---------------------------------|
| `processId` | String | Yes      | The process identifier          |

**Content-Type**: `application/json`

**Request Body**: Process variables (optional)
```json
{
  "orderAmount": 5000,
  "customerId": "CUST-001",
  "priority": "HIGH"
}
```

**Success Response**:
**Status Code**: `201 CREATED`
```json
{
  "processId": "proc-123",
  "instanceId": "abc-123-def-456",
  "status": "STARTED",
  "message": "Process instance created successfully",
  "queryEndpoint": "/api/process/proc-123/instance/abc-123-def-456",
  "kogitoEndpoint": "/proc-123/abc-123-def-456",
  "instanceData": {
    "id": "abc-123-def-456",
    "orderAmount": 5000,
    "customerId": "CUST-001",
    "priority": "HIGH"
  }
}
```

**Error Responses**:
- `404 NOT_FOUND` - Process not found
- `409 CONFLICT` - Process not published or not deployed
- `500 INTERNAL_SERVER_ERROR` - Execution failed (Kogito error)

**Validation Rules**:
1. Process must exist in repository
2. Process status must be PUBLISHED
3. Process must be deployed to Kogito

### 3. Get Process Instance

Get the status and data of a specific process instance.

**Endpoint**: `GET /api/process/{processId}/instance/{instanceId}`

**Path Parameters**:
| Parameter   | Type   | Required | Description                     |
|-------------|--------|----------|---------------------------------|
| `processId` | String | Yes      | The process identifier          |
| `instanceId`| String | Yes      | The process instance identifier |

**Success Response**:
**Status Code**: `200 OK`
```json
{
  "processId": "proc-123",
  "instanceId": "abc-123-def-456",
  "data": {
    "id": "abc-123-def-456",
    "state": 1,
    "variables": {
      "orderAmount": 5000,
      "customerId": "CUST-001"
    }
  }
}
```

### 4. List Process Instances

List all instances for a specific process.

**Endpoint**: `GET /api/process/{processId}/instances`

**Path Parameters**:
| Parameter   | Type   | Required | Description                     |
|-------------|--------|----------|---------------------------------|
| `processId` | String | Yes      | The process identifier          |

**Success Response**:
**Status Code**: `200 OK`
```json
{
  "processId": "proc-123",
  "instances": [
    {
      "id": "abc-123",
      "state": 1
    },
    {
      "id": "def-456",
      "state": 2
    }
  ]
}
```

### 5. Get Process Status

Check if a process is published and ready for execution.

**Endpoint**: `GET /api/process/{processId}/status`

**Path Parameters**:
| Parameter   | Type   | Required | Description                     |
|-------------|--------|----------|---------------------------------|
| `processId` | String | Yes      | The process identifier          |

**Success Response**:
**Status Code**: `200 OK`
```json
{
  "processId": "proc-123",
  "published": true,
  "canExecute": true,
  "executeEndpoint": "/api/process/proc-123/execute",
  "kogitoEndpoint": "/proc-123"
}
```

## Kogito Deployment Endpoints

### Base Path: `/api/kogito/deployments`

Low-level endpoints for managing BPMN and DRL deployments to Kogito runtime.

### 14. Deploy Complete Process

Deploy both BPMN and DRL for a process to Kogito runtime.

**Endpoint**: `POST /api/kogito/deployments/{processId}`

**Path Parameters**:
| Parameter   | Type   | Required | Description                     |
|-------------|--------|----------|---------------------------------|
| `processId` | String | Yes      | The process identifier          |

**Content-Type**: `application/json`

**Request Body**:
```json
{
  "bpmnXml": "<?xml version=\"1.0\"...",
  "drlContent": "package com.example..."
}
```

**Success Response**:
**Status Code**: `200 OK`
```json
{
  "processId": "proc-123",
  "bpmnPath": "./data/kogito/processes/proc-123.bpmn",
  "drlPath": "./data/kogito/rules/proc-123.drl",
  "status": "deployed"
}
```

**After deployment, Kogito auto-generates REST endpoints**:
- `POST /{processId}` - Start process instance
- `GET /{processId}` - List instances
- `GET /{processId}/{instanceId}` - Get instance details

### 15. Deploy BPMN Only

Deploy only BPMN file to Kogito runtime.

**Endpoint**: `POST /api/kogito/deployments/{processId}/bpmn`

**Content-Type**: `application/xml`

**Request Body**: BPMN 2.0 XML content (raw)

**Success Response**:
**Status Code**: `200 OK`
```json
{
  "processId": "proc-123",
  "type": "BPMN",
  "path": "./data/kogito/processes/proc-123.bpmn",
  "status": "deployed"
}
```

### 16. Deploy DRL Only

Deploy only DRL file to Kogito runtime.

**Endpoint**: `POST /api/kogito/deployments/{processId}/drl`

**Content-Type**: `text/plain`

**Request Body**: DRL content (raw)

**Success Response**:
**Status Code**: `200 OK`
```json
{
  "processId": "proc-123",
  "type": "DRL",
  "path": "./data/kogito/rules/proc-123.drl",
  "status": "deployed"
}
```

### 17. Get Deployment Status

Check if a process is deployed and get deployment details.

**Endpoint**: `GET /api/kogito/deployments/{processId}/status`

**Success Response**:
**Status Code**: `200 OK`
```json
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

### 18. Get Deployment Info

Get detailed deployment information for a process.

**Endpoint**: `GET /api/kogito/deployments/{processId}`

**Success Response**:
**Status Code**: `200 OK`
```json
{
  "processId": "proc-123",
  "bpmnPath": "./data/kogito/processes/proc-123.bpmn",
  "bpmnSize": 5432,
  "bpmnLastModified": "2026-01-02T10:30:00",
  "drlPath": "./data/kogito/rules/proc-123.drl",
  "drlSize": 1234,
  "drlLastModified": "2026-01-02T10:30:01"
}
```

### 19. List All Deployments

List all deployed processes.

**Endpoint**: `GET /api/kogito/deployments`

**Success Response**:
**Status Code**: `200 OK`
```json
{
  "count": 3,
  "processIds": ["proc-123", "proc-456", "proc-789"]
}
```

### 20. Undeploy Process

Remove a deployed process from Kogito runtime.

**Endpoint**: `DELETE /api/kogito/deployments/{processId}`

**Success Response**:
**Status Code**: `200 OK`
```json
{
  "processId": "proc-123",
  "status": "undeployed"
}
```

## Kogito Process Execution (Auto-Generated)

**Note**: These endpoints are automatically created by Kogito for each deployed process.

### Start Process Instance

**Endpoint**: `POST /{processId}`

**Content-Type**: `application/json`

**Request Body**: Process variables
```json
{
  "orderAmount": 5000,
  "customerId": "CUST-001"
}
```

**Success Response**:
**Status Code**: `200 OK` (or `201 Created`)
```json
{
  "id": "inst-12345-67890",
  "orderAmount": 5000,
  "customerId": "CUST-001"
}
```

### Get Process Instance

**Endpoint**: `GET /{processId}/{instanceId}`

**Success Response**:
```json
{
  "id": "inst-12345-67890",
  "processId": "proc-123",
  "state": 1,
  "variables": {
    "orderAmount": 5000,
    "customerId": "CUST-001"
  }
}
```

### List Process Instances

**Endpoint**: `GET /{processId}`

**Success Response**:
```json
[
  {
    "id": "inst-12345",
    "state": 1
  },
  {
    "id": "inst-67890",
    "state": 2
  }
]
```

## Complete Workflow Example

### From Text to Execution

```bash
# 1. Create process from text
curl -X POST http://localhost:8080/api/process/from-text \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Order Approval",
    "description": "When order amount > 5000, require manager approval"
  }'

# Response: { "processId": "proc-123" }

# 2. Publish process (generates BPMN + DRL, deploys to Kogito)
curl -X POST http://localhost:8080/api/process/proc-123/publish

# Response: 
# {
#   "processId": "proc-123",
#   "status": "PUBLISHED",
#   "executeEndpoint": "/api/process/proc-123/execute"
# }

# 3. Execute process (start instance)
curl -X POST http://localhost:8080/api/process/proc-123/execute \
  -H "Content-Type: application/json" \
  -d '{
    "orderAmount": 7500,
    "customerId": "CUST-001"
  }'

# Response:
# {
#   "instanceId": "abc-123-def",
#   "status": "STARTED"
# }

# 4. Query instance
curl http://localhost:8080/api/process/proc-123/instance/abc-123-def
```

## Edit Intent Endpoints

### Process Edit Intent

Submit natural language instructions to edit process elements.

**Endpoint**: `POST /api/process/{processId}/edit-intent`

**Request Body**:
```json
{
  "instruction": "Rename this task to 'Review Application'",
  "nodeId": "task_1"
}
```

**Response**:
```json
{
  "success": true,
  "message": "Edit applied successfully.",
  "modifiedNodeId": "task_1",
  "bpmnRegenerated": true
}
```

**Supported Edits**: Rename, Update Condition, Update Description

**Status Codes**: `200 OK`, `400 Bad Request`, `404 Not Found`, `500 Internal Server Error`

### Get Process Explanations

Get AI-generated explanations for all process nodes.

**Endpoint**: `GET /api/process/{processId}/explanations`

**Response**:
```json
{
  "processId": "proc_123",
  "explanations": [
    {
      "nodeId": "task_1",
      "reason": "This task collects the initial request...",
      "source": "AI Generated",
      "confidenceScore": 0.85,
      "timestamp": "2026-01-03T12:00:00"
    }
  ]
}
```

**Status Codes**: `200 OK`, `404 Not Found`, `500 Internal Server Error`

**See Also**: [EDIT_INTENT_API.md](./EDIT_INTENT_API.md) for comprehensive documentation.

---

## Related Services

The API integrates with several backend services:

- **`ProcessTextService`** - Creates processes from text descriptions
- **`ProcessImageUploadService`** - Creates processes from uploaded images
- **`ProcessReasonerService`** - Infers `ProcessModel` from natural language descriptions
- **`AiInferenceService`** - Extracts process descriptions from uploaded images
- **`AiOrchestratorService`** - Manages AI workflow state and transitions
- **`BpmnGeneratorService`** - Converts `ProcessModel` to executable BPMN 2.0 XML
- **`BpmnValidationService`** - Validates BPMN 2.0 XML for correctness and logical consistency
- **`RuleDetectionService`** - Automatically detects business rules from text (conditions, thresholds, comparisons)
- **`DrlGeneratorService`** - Converts `RuleModel` to Drools Rule Language (DRL) format
- **`KogitoDeploymentService`** - Deploys BPMN and DRL to Kogito runtime
- **`ProcessPublishingService`** - Orchestrates complete publish workflow (generate, validate, deploy)
- **`ProcessExecutionService`** - Executes published processes (validates and delegates to Kogito)
- **`ProcessEditService`** - Handles natural language edit intents and node explanations
- **`GeminiClient`** - Interfaces with Google Gemini AI for inference

For detailed documentation on each service, see their respective markdown files in the backend directory.

---

