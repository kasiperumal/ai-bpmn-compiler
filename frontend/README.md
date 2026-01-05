# AI BPMN Compiler - Frontend

A React-based frontend application for the AI BPMN Compiler, built with Vite, TypeScript, and BPMN.js.

## Features

- 📊 **BPMN Diagram Viewer**: Visualize and interact with BPMN process diagrams using BPMN.js
- 🔧 **Properties Panel**: View and manage process properties and selected elements
- 💬 **AI Chat Interface**: Natural language interface for creating and managing BPMN processes
- 🚀 **Process Publishing**: Publish processes to the Kogito runtime
- ▶️ **Process Execution**: Execute published processes directly from the UI

## Tech Stack

- **React 18** - UI framework
- **TypeScript** - Type safety
- **Vite** - Build tool and dev server
- **BPMN.js** - BPMN 2.0 diagram rendering and editing
- **Axios** - HTTP client for API communication
- **CSS3** - Styling with modern CSS features

## Project Structure

```
frontend/
├── src/
│   ├── components/
│   │   ├── BpmnDiagram.tsx      # BPMN diagram viewer component
│   │   ├── BpmnDiagram.css
│   │   ├── PropertiesPanel.tsx   # Properties and process info panel
│   │   ├── PropertiesPanel.css
│   │   ├── ChatPanel.tsx         # AI chat interface
│   │   └── ChatPanel.css
│   ├── App.tsx                   # Main application component
│   ├── App.css                   # Main application styles
│   ├── main.tsx                  # Application entry point
│   └── index.css                 # Global styles
├── public/                       # Static assets
├── package.json                  # Dependencies and scripts
├── tsconfig.json                 # TypeScript configuration
└── vite.config.ts               # Vite configuration
```

## Getting Started

### Prerequisites

- Node.js 18+ and npm
- Backend server running on `http://localhost:8080`

### Installation

1. Install dependencies:

```bash
npm install
```

2. Start the development server:

```bash
npm run dev
```

The application will be available at `http://localhost:5173` (or the next available port).

### Building for Production

```bash
npm run build
```

The built files will be in the `dist/` directory.

### Preview Production Build

```bash
npm run preview
```

## Usage

### Creating a Process

1. **Start a Conversation**: In the Chat Panel on the right, describe your business process in natural language.
   
   Example: "Create a leave approval process where an employee submits a request, their manager approves it, and HR processes it."

2. **Answer Clarifications**: The AI may ask clarifying questions. Answer them to refine your process.

3. **View the Diagram**: Once the process is created, the BPMN diagram will appear in the Diagram panel on the left.

### Publishing a Process

1. In the Properties Panel (middle), click the **"Publish"** button.
2. This will generate the BPMN and DRL files and deploy them to the Kogito runtime.
3. The AI State will change to "PUBLISHED".

### Executing a Process

1. Once a process is published, the **"Execute"** button in the Properties Panel will be enabled.
2. Click **"Execute"** to start a new instance of the process.
3. You'll receive a process instance ID upon successful execution.

### Diagram Interactions

- **Zoom In/Out**: Use the `+` and `-` buttons in the toolbar
- **Fit to Screen**: Click the `⊡` button to fit the diagram to the viewport
- **Export**: Click the `↓` button to download the BPMN XML file
- **Select Elements**: Click on any element in the diagram to view its properties

## API Integration

The frontend communicates with the backend API at `http://localhost:8080`. The main endpoints used are:

- `POST /api/process/start` - Start a new process creation
- `GET /api/process/{processId}` - Get process details
- `GET /api/process/{processId}/bpmn` - Get BPMN XML
- `POST /api/process/{processId}/publish` - Publish a process
- `POST /api/process/{processId}/execute` - Execute a process
- `GET /api/process/{processId}/questions` - Get clarification questions
- `POST /api/process/{processId}/answer` - Answer clarification questions
- `POST /api/process/{processId}/resume` - Resume process after answering questions

## Configuration

To configure the backend API URL, you can modify the Axios base URL in the component files or create an environment variable:

Create a `.env.local` file in the root directory:

```env
VITE_API_BASE_URL=http://localhost:8080
```

Then update the Axios calls to use `import.meta.env.VITE_API_BASE_URL`.

## Development

### Available Scripts

- `npm run dev` - Start development server with hot reload
- `npm run build` - Build for production
- `npm run preview` - Preview production build
- `npm run lint` - Run ESLint

### Code Style

This project uses ESLint for code quality. Run `npm run lint` to check for issues.

## Layout

The application uses a three-column layout:

```
+------------------------------------------------------------------+
|                          Header                                   |
+----------------------+-------------------+------------------------+
|                      |                   |                        |
|   BPMN Diagram       |  Properties       |      Chat Panel        |
|   (50%)              |  Panel (25%)      |      (25%)            |
|                      |                   |                        |
|   - Diagram viewer   |  - Process info   |  - AI conversation     |
|   - Zoom controls    |  - Element props  |  - Process creation    |
|   - Export           |  - Publish/Exec   |  - Clarifications      |
|                      |                   |                        |
+----------------------+-------------------+------------------------+
```

On smaller screens (< 1200px), the layout switches to a vertical stack.

## Troubleshooting

### CORS Errors

If you encounter CORS errors, ensure the backend is configured to allow requests from `http://localhost:5173`.

### BPMN Not Loading

1. Check that the backend is running and accessible
2. Verify the process ID is correct
3. Check the browser console for detailed error messages

### Diagram Not Rendering

1. Ensure BPMN.js CSS files are properly loaded
2. Check that the container has a defined height
3. Verify the BPMN XML is valid

## Contributing

1. Follow the existing code style
2. Add comments for complex logic
3. Test your changes thoroughly
4. Ensure no ESLint errors

## License

This project is part of the AI BPMN Compiler system.
