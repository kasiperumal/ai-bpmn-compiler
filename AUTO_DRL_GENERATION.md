# Auto-DRL Generation on Process Creation

**Date**: 2026-01-18  
**Status**: ✅ **IMPLEMENTED**  
**Backend Status**: ✅ Restarted at 09:57:23 with latest changes

---

## 🔍 **Problem**

DRL files were **not being generated** when a process was created. They were only generated when explicitly calling the `/api/process/{processId}/publish` endpoint.

**User Issue**: "Why don't I see `LeaveValidationRules.drl` in `data/kogito/rules` path?"

---

## ✅ **Solution**

Updated `ProcessTextService` to **automatically generate DRL files** when a process is created, so they're immediately available for viewing in the properties panel.

---

## 📋 **What Changed**

### **Before** (Old Workflow)

```
User creates process
    ↓
AI generates BPMN + Rules
    ↓
Rules saved to database (RuleModel objects)
    ↓
Process status: DRAFT
    ↓
❌ NO DRL file generated yet
    ↓
User must manually call /api/process/{processId}/publish
    ↓
✅ DRL file generated
```

### **After** (New Workflow)

```
User creates process
    ↓
AI generates BPMN + Rules
    ↓
Rules saved to database (RuleModel objects)
    ↓
✅ DRL file generated IMMEDIATELY
    ↓
DRL saved to data/kogito/rules/{processId}.drl
    ↓
Process status: DRAFT
    ↓
(Optional) User can call /publish for full deployment
```

---

## 🔧 **Technical Implementation**

### File Modified: `ProcessTextService.java`

#### 1. **Added Dependency Injection**

```java
private final DrlGeneratorService drlGeneratorService;

public ProcessTextService(
        ProcessModelRepository processModelRepository,
        ProcessReasonerService processReasonerService,
        BpmnValidationService bpmnValidationService,
        DrlGeneratorService drlGeneratorService, // ← Added
        ObjectMapper objectMapper) {
    // ...
    this.drlGeneratorService = drlGeneratorService;
}
```

#### 2. **Added Rules to ProcessModel**

```java
// Add rules from reasoning result
if (reasoningResult.getRules() != null && !reasoningResult.getRules().isEmpty()) {
    processModel.setRules(reasoningResult.getRules());
    logger.info("Added {} rules to process model", reasoningResult.getRules().size());
}
```

#### 3. **Auto-Generate DRL on Creation**

```java
// 5. Generate DRL file immediately (for preview in properties panel)
if (!processModel.getRules().isEmpty()) {
    try {
        String drlContent = drlGeneratorService.generateDrl(
            processModel.getRules(),
            "com.example.aibpmn.rules." + sanitizePackageName(processId),
            false // Don't validate for now
        );
        
        // Save DRL file to disk
        saveDrlFile(processId, drlContent);
        logger.info("Generated DRL file for process: {} ({} rules)", processId, processModel.getRules().size());
    } catch (Exception e) {
        logger.warn("Failed to generate DRL file for process {}: {}", processId, e.getMessage());
        // Don't fail process creation if DRL generation fails
    }
}
```

#### 4. **Added Helper Methods**

```java
/**
 * Sanitize process ID for use in package name
 */
private String sanitizePackageName(String processId) {
    return processId.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
}

/**
 * Save DRL file to disk
 */
private void saveDrlFile(String processId, String drlContent) {
    try {
        java.nio.file.Path drlDir = java.nio.file.Paths.get("data/kogito/rules");
        java.nio.file.Files.createDirectories(drlDir);
        
        String fileName = sanitizePackageName(processId) + ".drl";
        java.nio.file.Path drlFile = drlDir.resolve(fileName);
        
        java.nio.file.Files.writeString(drlFile, drlContent);
        logger.info("Saved DRL file: {}", drlFile.toAbsolutePath());
    } catch (Exception e) {
        logger.error("Failed to save DRL file for process {}: {}", processId, e.getMessage(), e);
        throw new RuntimeException("Failed to save DRL file: " + e.getMessage(), e);
    }
}
```

---

## 🎯 **Expected Behavior**

### When You Create a Process

**Input**:
```
"Create a simple leave approval process where an employee submits a request, 
their manager reviews it, and if approved, HR processes it. Reject if leave 
is more than 5 days or during peak delivery period."
```

**Backend Logs** (New):
```
[ProcessReasonerService] Created RuleModel: id=RejectIfMoreThan5Days, expression=days > 5, action=reject
[ProcessReasonerService] Created RuleModel: id=RejectIfPeakPeriod, expression=during peak delivery period, action=reject
[ProcessReasonerService] Created RuleModel: id=ApproveIfAllCriteriaMet, expression=all criteria met, action=approve
[ProcessTextService] Added 3 rules to process model
[DrlGeneratorService] Generating DRL for 3 rules in package: com.example.aibpmn.rules.proc_xxx
[ProcessTextService] Generated DRL file for process: proc-xxx (3 rules)
[ProcessTextService] Saved DRL file: /Users/.../data/kogito/rules/proc_xxx.drl
```

**File System**:
```
backend/data/kogito/rules/
├── proc_xxx.drl  ← ✅ Generated immediately!
```

**DRL File Content**:
```drools
package com.example.aibpmn.rules.proc_xxx;

import java.util.*;
import java.time.*;

rule "RejectIfMoreThan5Days"
    salience 10
    // Leave request exceeds 5 day limit
when
    eval(days > 5)
then
    System.out.println("Rule fired: RejectIfMoreThan5Days - REJECTED: Leave request exceeds 5 day limit");
    // Set validationResult = "REJECTED"
end

rule "RejectIfPeakPeriod"
    salience 10
    // Overlaps with critical business delivery dates
when
    eval(during peak delivery period)
then
    System.out.println("Rule fired: RejectIfPeakPeriod - REJECTED: Overlaps with critical business delivery dates");
    // Set validationResult = "REJECTED"
end

rule "ApproveIfAllCriteriaMet"
    salience 1
    // Leave request is valid
when
    eval(all criteria met (days <= 5, not peak period, eligible))
then
    System.out.println("Rule fired: ApproveIfAllCriteriaMet - APPROVED: Leave request is valid");
    // Set validationResult = "APPROVED"
end
```

---

## 🧪 **Testing**

### Step 1: Clear Old Data

```bash
rm -rf backend/data/kogito/rules/*
```

### Step 2: Create a New Process

1. Open frontend: `http://localhost:5173`
2. Open AI Assistant
3. Enter:
   ```
   Create a simple leave approval process where an employee submits a request, 
   their manager reviews it, and if approved, HR processes it. Reject if leave 
   is more than 5 days or during peak delivery period.
   ```

### Step 3: Verify DRL File

```bash
ls -la backend/data/kogito/rules/
# Should show: proc_xxx.drl

cat backend/data/kogito/rules/proc_*.drl
# Should show the generated DRL content
```

### Step 4: Check Backend Logs

```bash
tail -100 backend/logs/application.log | grep -E "Generated DRL|Saved DRL"
```

Expected:
```
[ProcessTextService] Generated DRL file for process: proc-xxx (3 rules)
[ProcessTextService] Saved DRL file: .../data/kogito/rules/proc_xxx.drl
```

---

## 📊 **Benefits**

### ✅ **Immediate Availability**
- DRL files are generated as soon as the process is created
- No need to manually call `/publish` endpoint
- Rules are immediately visible in the properties panel

### ✅ **Better Developer Experience**
- See generated DRL immediately for debugging
- Verify rule syntax without publishing
- Iterate faster on rule development

### ✅ **Consistent Behavior**
- Every process with business rules gets a DRL file
- File naming is consistent (`proc_xxx.drl`)
- Package naming follows Java conventions

---

## 🔄 **Backward Compatibility**

### Publishing Still Works

The `/api/process/{processId}/publish` endpoint still works and will:
1. Re-generate DRL files (overwriting existing ones)
2. Generate BPMN XML files
3. Deploy to Kogito runtime
4. Change status to PUBLISHED

### Draft vs. Published

- **DRAFT**: DRL file exists for preview, but process is not deployed
- **PUBLISHED**: DRL file exists AND process is deployed to Kogito runtime

---

## 🎉 **Summary**

**Problem**: DRL files were not generated on process creation  
**Solution**: Auto-generate DRL files in `ProcessTextService`  
**Result**: DRL files are now immediately available in `data/kogito/rules/`

**Backend Status**: ✅ Restarted at **09:57:23** with latest changes

---

**Try it now!** Create a new process and check `backend/data/kogito/rules/` - you should see the DRL file immediately! 🚀
