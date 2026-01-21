# Properties Panel Toggle Feature

## 🎯 **Overview**

Implemented collapsible properties panel to maximize diagram workspace when properties are not needed.

---

## ✨ **Features**

### **1. Toggle Button**
- Located in toolbar: `[←] Properties` or `[→] Properties`
- Shows current state with arrow direction
- Button highlights when panel is open (black background)

### **2. Auto-Show on Selection**
- Properties panel automatically opens when you click any BPMN element
- Makes it intuitive - no need to manually open when editing

### **3. Smooth Animation**
- Slides in/out with 0.3s smooth transition
- Uses cubic-bezier easing for professional feel
- Opacity fade adds polish

### **4. Remembers Preference**
- Saves state to localStorage
- Opens in same state as last time
- Per-browser persistence

### **5. Space Optimization**
- **Closed**: Diagram uses full width
- **Open**: Diagram resizes to ~70%, Properties takes ~30% (300px)
- No overlapping - clean layout

---

## 🎨 **UI States**

### **Default (Collapsed)**
```
┌────────────────────────────────────────────────────────┐
│ BPMN Modeler  [💾Save] [+][-][⊡][↓] [[←] Properties] │
├────────────────────────────────────────────────────────┤
│                                                        │
│              BPMN Diagram (Full Width)                 │
│                                                        │
└────────────────────────────────────────────────────────┘
```

### **Expanded (After Clicking Element or Toggle)**
```
┌──────────────────────────────────┬─────────────────────┐
│ BPMN Modeler  [[→] Properties ✓] │                     │
├──────────────────────────────────┤   Properties        │
│                                  │   Panel             │
│     BPMN Diagram                 │   (300px)           │
│     (~70%)                       │                     │
│                                  │   Name: Task_1      │
│                                  │   Type: UserTask    │
│                                  │   Assignee: ...     │
└──────────────────────────────────┴─────────────────────┘
```

---

## 🔧 **Implementation Details**

### **Files Modified**

1. **`frontend/src/components/BpmnDiagram.tsx`**
   - Added `showPropertiesPanel` state with localStorage persistence
   - Added `togglePropertiesPanel()` function
   - Auto-show on element click
   - Added toggle button in toolbar
   - Added conditional classes to properties panel

2. **`frontend/src/components/BpmnDiagram.css`**
   - Added `.collapsed` and `.expanded` classes
   - Smooth CSS transitions (0.3s cubic-bezier)
   - Active button styling
   - Opacity animation

---

## 📊 **Technical Details**

### **State Management**
```typescript
const [showPropertiesPanel, setShowPropertiesPanel] = useState(() => {
  const saved = localStorage.getItem('bpmn-properties-panel-visible')
  return saved === 'true' // Default: false (hidden)
})
```

### **Auto-Show Logic**
```typescript
eventBus.on('element.click', (event: any) => {
  if (onElementSelect) {
    onElementSelect(event.element)
  }
  
  // Auto-show when element selected (except labels)
  if (event.element && event.element.type !== 'label') {
    setShowPropertiesPanel(true)
  }
})
```

### **Toggle Function**
```typescript
const togglePropertiesPanel = () => {
  const newValue = !showPropertiesPanel
  setShowPropertiesPanel(newValue)
  localStorage.setItem('bpmn-properties-panel-visible', String(newValue))
}
```

### **CSS Transitions**
```css
.bpmn-properties-panel {
  transition: all 0.3s cubic-bezier(0.4, 0.0, 0.2, 1);
}

.bpmn-properties-panel.collapsed {
  width: 0;
  opacity: 0;
  border-left: none;
  overflow: hidden;
}

.bpmn-properties-panel.expanded {
  width: 300px;
  opacity: 1;
}
```

---

## 🎯 **User Experience Flow**

### **Scenario 1: First Time User**
1. Opens application
2. Properties panel is **closed** (maximizes diagram space)
3. Clicks any element → Properties **auto-opens** ✨
4. User can now edit properties
5. User can manually toggle with button

### **Scenario 2: Working with Properties**
1. User selects element → Properties opens
2. User edits properties (assignee, form key, etc.)
3. User selects another element → Properties stays open
4. User clicks `[→] Properties` → Panel closes, more diagram space

### **Scenario 3: Preference Persistence**
1. User closes properties panel
2. Refreshes browser
3. Properties panel stays closed ✅
4. Preference remembered across sessions

---

## 🚀 **Benefits**

### **Before**
- ❌ Properties panel always visible (300px wasted)
- ❌ Less space for diagram
- ❌ No control over layout
- ❌ Cluttered interface

### **After**
- ✅ Properties panel only when needed
- ✅ Full width for diagram by default
- ✅ User has control
- ✅ Clean, professional interface
- ✅ Auto-show on element selection (smart UX)
- ✅ Smooth animations (polished feel)
- ✅ Remembers preference

---

## 📱 **Responsive Behavior**

The properties panel works well on different screen sizes:

- **Large screens (>1920px)**: Properties takes ~15% width
- **Normal screens (1280-1920px)**: Properties takes ~23% width (300px)
- **Small screens (<1280px)**: User can collapse for more space

---

## 🎨 **Visual Polish**

### **Button States**
- **Inactive (panel closed)**: `[←] Properties` - white background
- **Active (panel open)**: `[→] Properties` - black background, white text
- **Hover**: Black background, white text
- **Icon changes**: `←` (show) / `→` (hide) indicates action

### **Animation**
- **Transition**: 0.3s cubic-bezier easing
- **Properties**: width, opacity, border
- **Feel**: Smooth, professional, not jarring

---

## 🧪 **How to Test**

1. **Refresh browser** to load changes

2. **Test Default State:**
   - Properties should be **closed** by default
   - Diagram uses full width

3. **Test Auto-Show:**
   - Click any element (task, gateway, event)
   - Properties panel should **slide in from right**

4. **Test Manual Toggle:**
   - Click `[←] Properties` button
   - Panel should open smoothly
   - Click `[→] Properties` button
   - Panel should close smoothly

5. **Test Persistence:**
   - Open properties panel
   - Refresh page
   - Properties should stay open ✅

6. **Test Button State:**
   - When open: Button has black background
   - When closed: Button has white background

---

## 💡 **Future Enhancements**

Potential improvements (not implemented yet):

1. **Keyboard Shortcut**: `Ctrl+P` or `Cmd+P` to toggle
2. **Resize Handle**: Drag to adjust panel width
3. **Multiple Sizes**: Small (200px), Medium (300px), Large (400px)
4. **Bottom Panel Mode**: For very wide screens
5. **Floating Mode**: Detached panel that can be dragged

---

## 📝 **Configuration**

### **Change Default State**

Edit `BpmnDiagram.tsx`:
```typescript
// Change to true for open by default
return saved === 'true' || false // ← Change false to true
```

### **Change Panel Width**

Edit `BpmnDiagram.css`:
```css
.bpmn-properties-panel.expanded {
  width: 300px; /* ← Change to desired width */
}
```

### **Change Animation Speed**

Edit `BpmnDiagram.css`:
```css
transition: all 0.3s cubic-bezier(...); /* ← Change 0.3s */
```

### **Disable Auto-Show**

Edit `BpmnDiagram.tsx`:
```typescript
// Comment out this line:
// setShowPropertiesPanel(true)
```

---

## ✅ **Status**

- [x] Toggle button implemented
- [x] Smooth animations
- [x] Auto-show on element selection
- [x] localStorage persistence
- [x] Active button styling
- [x] No TypeScript errors
- [ ] User acceptance testing

---

**Date:** 2026-01-16  
**Feature:** Collapsible Properties Panel  
**Status:** ✅ **Implemented - Ready for Testing**  
**Impact:** High - Significantly improves workspace utilization
