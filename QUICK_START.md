# AI BPMN Compiler - Quick Start Guide

Get the AI BPMN Compiler up and running in minutes!

## Prerequisites

- **Java 17+** (for backend)
- **Node.js 18+** and npm (for frontend)
- **Git** (to clone the repository)

## Quick Start

### 1. Start the Backend

```bash
# Navigate to backend directory
cd backend

# Build the project
./gradlew clean build

# Run the application
./gradlew bootRun
```

The backend will start on **http://localhost:8080**

### 2. Start the Frontend

Open a new terminal:

```bash
# Navigate to frontend directory
cd frontend

# Install dependencies (first time only)
npm install

# Start the development server
npm run dev
```

The frontend will start on **http://localhost:5173**

### 3. Open the Application

Open your browser and navigate to:
```
http://localhost:5173
```

## Your First Process

1. **Describe Your Process**: In the Chat Panel (right side), type:
   ```
   Create a leave approval process where an employee submits a request,
   their manager approves it, and HR processes it.
   ```

2. **Answer Questions**: The AI may ask clarifying questions. Answer them to refine your process.

3. **View the Diagram**: The BPMN diagram will appear in the left panel.

4. **Publish**: Click the "Publish" button in the Properties Panel (middle).

5. **Execute**: Once published, click "Execute" to start a process instance.

## Directory Structure

```
ai-bpmn-compiler/
├── backend/              # Spring Boot backend
│   ├── src/
│   ├── build.gradle
│   └── gradlew
├── frontend/             # React frontend
│   ├── src/
│   ├── package.json
│   └── vite.config.ts
└── README.md
```

## Configuration

### Backend Configuration

Edit `backend/src/main/resources/application.yml`:

```yaml
server:
  port: 8080  # Change if port 8080 is busy

spring:
  ai:
    vertex:
      ai:
        gemini:
          project-id: ${VERTEX_AI_PROJECT_ID}
          location: ${VERTEX_AI_LOCATION}
          api-key: ${GEMINI_API_KEY}  # Set your API key
```

### Frontend Configuration

The frontend connects to `http://localhost:8080` by default. No additional configuration is needed for local development.

## Environment Variables

Set these environment variables for the backend:

```bash
# For Gemini AI (required for AI features)
export GEMINI_API_KEY=your-api-key-here
export VERTEX_AI_PROJECT_ID=your-project-id
export VERTEX_AI_LOCATION=us-central1
```

Or create a `.env` file in the backend directory:

```env
GEMINI_API_KEY=your-api-key-here
VERTEX_AI_PROJECT_ID=your-project-id
VERTEX_AI_LOCATION=us-central1
```

## Troubleshooting

### Backend Won't Start

**Issue**: Port 8080 already in use
**Solution**: Change the port in `application.yml`:
```yaml
server:
  port: 8081  # Use a different port
```

**Issue**: Gemini API key not set
**Solution**: Set the `GEMINI_API_KEY` environment variable

### Frontend Won't Start

**Issue**: Port 5173 already in use
**Solution**: Vite will automatically use the next available port

**Issue**: CORS errors in browser console
**Solution**: Ensure the backend is running and accessible

### Can't Connect to Backend

**Issue**: Network error when creating a process
**Solution**: 
1. Verify backend is running: `curl http://localhost:8080/actuator/health`
2. Check the backend console for errors
3. Ensure CORS is properly configured

## API Health Check

To verify the backend is running:

```bash
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{
  "status": "UP"
}
```

## Next Steps

- **Read the Documentation**:
  - Backend: `backend/README.md`
  - Frontend: `frontend/README.md`
  - Architecture: `frontend/FRONTEND_ARCHITECTURE.md`

- **Explore the Code**:
  - Backend services: `backend/src/main/java/com/example/aibpmn/service/`
  - Frontend components: `frontend/src/components/`

- **Run Tests**:
  ```bash
  # Backend
  cd backend && ./gradlew test

  # Frontend (when tests are added)
  cd frontend && npm test
  ```

## Production Deployment

### Backend

```bash
cd backend
./gradlew bootJar
java -jar build/libs/ai-bpmn-compiler-0.0.1-SNAPSHOT.jar
```

### Frontend

```bash
cd frontend
npm run build
# Deploy the 'dist' folder to your hosting service
```

## Support

For issues or questions:
1. Check the documentation in each directory
2. Review the API documentation: `backend/API.md`
3. Check the console logs for error messages

## Features Overview

✅ **Natural Language Process Creation**
- Describe your business process in plain English
- AI generates BPMN diagrams automatically

✅ **BPMN Visualization**
- Interactive diagram viewer
- Zoom and navigation controls
- Export to BPMN XML

✅ **Process Publishing**
- Generate BPMN and DRL files
- Deploy to Kogito runtime
- Validate before execution

✅ **Process Execution**
- Execute published processes
- Track process instances
- View execution results

✅ **Clarification Workflow**
- AI asks questions when needed
- Refine processes through conversation
- Iterative improvement

## Example Processes

Try creating these processes to explore the features:

### Simple Approval
```
Create a simple approval process where a user submits a request and a manager approves or rejects it.
```

### Invoice Processing
```
Create an invoice processing workflow where invoices are received, validated, 
approved by management, and then paid. If validation fails, the invoice is rejected.
```

### Customer Onboarding
```
Create a customer onboarding process that includes account creation, 
document verification, and welcome email sending.
```

## Development Mode

Both backend and frontend support hot-reloading:
- **Backend**: Uses Spring Boot DevTools (if enabled)
- **Frontend**: Uses Vite's Hot Module Replacement (HMR)

Save your changes and see them reflected immediately!

## Happy Building! 🚀

You're now ready to create AI-powered BPMN processes. Start with the chat interface and explore the features!

