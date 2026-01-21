# GenAI-Powered BPMN Builder - Frontend

React + TypeScript application with BPMN.js visualization and ELK.js layout.

---

## 📋 Overview

**Modern React frontend** for visualizing and interacting with AI-generated BPMN processes.

**Key Features:**
- ✅ **BPMN.js Modeler** - Professional BPMN rendering
- ✅ **ELK.js Layout** - Zero-overlap hierarchical layout
- ✅ **Image Upload** - Drag-and-drop diagram upload
- ✅ **Rule Inspector** - View and manage Drools rules
- ✅ **Chat Interface** - Natural language process editing
- ✅ **Properties Panel** - View/edit element metadata

---

## 🛠️ Technology Stack

### **Core**
- **React**: 18.x
- **TypeScript**: 5.x
- **Vite**: 5.x (build tool)
- **Node.js**: 18+

### **BPMN**
- **bpmn-js**: BPMN 2.0 modeler/viewer
- **bpmn-moddle**: BPMN JSON ↔ XML conversion
- **ELK.js**: Graph layout algorithm

### **HTTP & Routing**
- **Axios**: HTTP client
- **React Router DOM**: Client-side routing

### **Styling**
- **CSS Modules**: Component-scoped styles
- **bpmn-font**: BPMN icon font

---

## 📁 Project Structure

```
frontend/
├── package.json                    # Dependencies
├── vite.config.ts                  # Vite configuration
├── tsconfig.json                   # TypeScript config
├── index.html                      # HTML entry
├── public/                         # Static assets
└── src/
    ├── main.tsx                    # React entry point
    ├── App.tsx                     # Main application layout
    ├── App.css                     # Global styles
    ├── components/
    │   ├── BpmnDiagram.tsx         # BPMN viewer with ELK layout
    │   ├── BpmnDiagram.css
    │   ├── PropertiesPanel.tsx     # Element properties editor
    │   ├── PropertiesPanel.css
    │   ├── ChatPanel.tsx           # Natural language interface
    │   ├── ChatPanel.css
    │   ├── ImageUpload.tsx         # Image upload component
    │   ├── ImageUpload.css
    │   ├── RuleInspector.tsx       # Drools rule viewer
    │   └── RuleInspector.css
    └── utils/
        └── elkLayout.ts            # ELK.js layout service
```

---

## 🚀 Getting Started

### **Install Dependencies**

```bash
npm install
```

### **Development Server**

```bash
npm run dev
```

App runs on **http://localhost:5173**

### **Build for Production**

```bash
npm run build
```

Output in `dist/` directory

### **Preview Production Build**

```bash
npm run preview
```

---

## 🔧 Configuration

### **vite.config.ts**

```typescript
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
```

### **Environment Variables**

Create `.env.local`:
```env
VITE_API_URL=http://localhost:8080
```

---

## 📦 Dependencies

### **Core Dependencies**
```json
{
  "react": "^18.2.0",
  "react-dom": "^18.2.0",
  "react-router-dom": "^6.20.1",
  "typescript": "^5.3.3"
}
```

### **BPMN & Layout**
```json
{
  "bpmn-js": "^17.0.0",
  "bpmn-moddle": "^8.1.0",
  "elkjs": "^0.9.2"
}
```

### **HTTP & Utils**
```json
{
  "axios": "^1.6.2"
}
```

### **Build Tools**
```json
{
  "vite": "^5.0.8",
  "@vitejs/plugin-react": "^4.2.1"
}
```

---

## 🏗️ Component Architecture

### **App.tsx - Main Layout**

```
┌─────────────────────────────────────────────────────┐
│                 Header                               │
│  [Logo]  [Upload Image]  [Publish]  [Execute]       │
├──────────────┬──────────────────┬───────────────────┤
│   BPMN       │   Properties     │     Chat          │
│  Diagram     │     Panel        │    Panel          │
│   (50%)      │     (25%)        │    (25%)          │
├──────────────┼──────────────────┼───────────────────┤
│ BPMN.js      │ Element Details  │ AI Assistant      │
│ + ELK.js     │ Rule Inspector   │ Natural Language  │
│ Layout       │ Edit Intent      │ Commands          │
│ Zoom/Pan     │ Explanations     │ Image Upload      │
└──────────────┴──────────────────┴───────────────────┘
```

### **Component Responsibilities**

#### **1. BpmnDiagram.tsx**
**Purpose:** Render BPMN with professional layout

**Features:**
- Fetches BPMN Moddle JSON from backend
- Converts JSON → XML using bpmn-moddle
- Applies ELK.js hierarchical layout
- Renders with BPMN.js Modeler (read-only mode)
- Supports zoom, pan, element selection
- Exports BPMN for download

**Key Code:**
```typescript
// Fetch BPMN Moddle JSON
const response = await axios.get(`/api/process/${processId}/bpmn-json`)

// Apply ELK layout
const layoutResult = await applyElkLayout(response.data)

// Render with BPMN.js
await modeler.importXML(bpmnXml)

// Apply layout positions
modeling.moveShape(element, { x, y })
```

#### **2. ImageUpload.tsx**
**Purpose:** Upload process diagram images

**Features:**
- Drag-and-drop support
- Image preview
- File validation (size, type)
- Process name input
- Upload progress indicator

**Supported Formats:**
- JPEG, PNG, GIF, WebP
- Max size: 10MB

#### **3. RuleInspector.tsx**
**Purpose:** View and manage Drools rules

**Features:**
- List all rules for process
- Filter by selected task
- Create new rules via natural language
- View DRL code
- Activate/deactivate rules
- Display rule status (DRAFT, VALIDATED, ACTIVE)

#### **4. ChatPanel.tsx**
**Purpose:** Natural language process editing

**Features:**
- Context-aware (selected element)
- Send edit instructions to backend
- Display AI responses
- Process history

#### **5. PropertiesPanel.tsx**
**Purpose:** View/edit element properties

**Features:**
- Display element metadata
- Show AI-generated explanations
- Edit intent interface
- Rule attachment for BusinessRuleTasks

---

## 🎨 Styling Architecture

### **Global Styles (App.css)**
```css
/* Layout */
.app-container {
  display: grid;
  grid-template-rows: auto 1fr;
  height: 100vh;
}

.main-content {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr;
  gap: 16px;
}
```

### **Component Styles (Scoped CSS)**

Each component has its own CSS file:
- `BpmnDiagram.css` - BPMN viewer styles
- `ChatPanel.css` - Chat interface styles
- `PropertiesPanel.css` - Properties panel styles
- `ImageUpload.css` - Upload component styles
- `RuleInspector.css` - Rule viewer styles

### **BPMN Styling**

```css
/* BPMN element colors */
.djs-element.start-event {
  fill: #d4f4dd;
  stroke: #52b788;
}

.djs-element.end-event {
  fill: #ffebee;
  stroke: #ef5350;
}

.djs-element.task {
  fill: #e3f2fd;
  stroke: #1976d2;
}
```

---

## 🔍 Key Utilities

### **elkLayout.ts - ELK.js Layout Service**

**Purpose:** Calculate optimal BPMN layout positions

**Features:**
- Hierarchical (Sugiyama) algorithm
- Orthogonal edge routing
- Minimized edge crossings
- Configurable spacing

**Usage:**
```typescript
import { applyElkLayout } from './utils/elkLayout'

// Calculate layout
const layoutResult = await applyElkLayout(bpmnModdleJson)

// Apply to BPMN.js
for (const [id, layout] of layoutResult.elements) {
  modeling.moveShape(element, { x: layout.x, y: layout.y })
}
```

**Configuration:**
```typescript
layoutOptions: {
  'elk.algorithm': 'layered',
  'elk.direction': 'RIGHT',
  'elk.spacing.nodeNode': '80',
  'elk.layered.spacing.nodeNodeBetweenLayers': '100',
  'elk.edgeRouting': 'ORTHOGONAL',
  'elk.padding': '[top=50,left=50,bottom=50,right=50]'
}
```

---

## 🧪 Testing

### **Run Tests**
```bash
npm test
```

### **Test Coverage**
```bash
npm run test:coverage
```

### **E2E Tests (Future)**
```bash
npm run test:e2e
```

---

## 🎯 Usage Examples

### **1. View Process Diagram**

```typescript
// In App.tsx
const [processId, setProcessId] = useState('proc-abc123')

<BpmnDiagram 
  processId={processId}
  onElementSelect={(element) => console.log(element)}
/>
```

### **2. Upload Process Image**

```typescript
<ImageUpload 
  onProcessCreated={(processId) => setProcessId(processId)}
/>
```

### **3. View Rules for Process**

```typescript
<RuleInspector 
  processId={processId}
  selectedTaskId={selectedElement?.id}
/>
```

### **4. Edit via Chat**

```typescript
<ChatPanel 
  processId={processId}
  selectedElement={selectedElement}
/>
```

---

## 🐛 Troubleshooting

### **Build Errors**

```bash
# Clean install
rm -rf node_modules package-lock.json
npm install

# Clear Vite cache
rm -rf node_modules/.vite
npm run dev
```

### **BPMN Not Rendering**

```bash
# Check browser console
# Common issues:
# 1. BPMN JSON is invalid
# 2. ELK.js not installed
# 3. CORS issues with backend

# Verify ELK.js
npm list elkjs

# Check backend URL
console.log('API URL:', import.meta.env.VITE_API_URL)
```

### **Layout Issues**

```bash
# ELK.js not applying layout
# Check console for errors:
# - "Cannot read properties of undefined (reading 'root-0')"
#   → Wait for canvas initialization
# - "layoutProcess is not a function"
#   → Check ELK.js import

# Fix: Update elkLayout.ts imports
```

### **API Connection Issues**

```bash
# Check CORS configuration
# Backend should allow: http://localhost:5173

# Verify proxy in vite.config.ts:
proxy: {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true
  }
}
```

---

## 🚀 Deployment

### **Build for Production**

```bash
# Build
npm run build

# Preview
npm run preview

# Deploy to static hosting
# Output in dist/ directory
```

### **Nginx Configuration**

```nginx
server {
    listen 80;
    server_name yourdomain.com;
    root /var/www/frontend/dist;
    
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    location /api {
        proxy_pass http://backend:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
    }
}
```

### **Docker (Future)**

```dockerfile
FROM node:18-alpine as build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

---

## 📚 Documentation

- **[../IMPLEMENTATION_COMPLETE.md](../IMPLEMENTATION_COMPLETE.md)** - Implementation summary
- **[../END_TO_END_TESTING_GUIDE.md](../END_TO_END_TESTING_GUIDE.md)** - Testing guide
- **[../README.md](../README.md)** - Main project documentation

---

## 🔗 External Resources

- **[BPMN.js Documentation](https://bpmn.io/toolkit/bpmn-js/)** - BPMN rendering
- **[ELK.js Documentation](https://eclipse.dev/elk/)** - Graph layout
- **[React Documentation](https://react.dev/)** - React framework
- **[Vite Documentation](https://vitejs.dev/)** - Build tool

---

## 📈 Performance Optimization

### **Code Splitting**
```typescript
// Lazy load components
const ImageUpload = lazy(() => import('./components/ImageUpload'))
const RuleInspector = lazy(() => import('./components/RuleInspector'))
```

### **Bundle Size**
```bash
# Analyze bundle
npm run build
npm run preview

# Check size
ls -lh dist/assets/*.js
```

### **Optimization Tips**
1. Use React.memo() for expensive components
2. Implement virtualization for long lists
3. Lazy load BPMN.js only when needed
4. Cache API responses
5. Use production builds for deployment

---

**Built with ❤️ using React, BPMN.js, and ELK.js**

**Version**: 3.0  
**Framework**: React 18 + Vite + TypeScript  
**Layout**: ELK.js  
**Status**: ✅ Production Ready
