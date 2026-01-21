# Dynamic Java POJO Generation for DRL

## Overview
The system now **dynamically generates Java POJO files at runtime** based on rule expressions. This provides the best of both worlds:

✅ **Dynamic field detection** - Adapts to any process type  
✅ **Java POJOs** - Type-safe, reusable, IDE-friendly  
✅ **Zero manual coding** - POJOs are auto-generated  

## How It Works

### **Flow:**

```
1. Analyze Rule Expressions
   ↓
2. Extract Required Fields (days, amount, isPeakPeriod, etc.)
   ↓
3. Generate Java POJO Source Code
   ↓
4. Write to backend/src/main/java/com/example/aibpmn/model/
   ↓
5. Import POJOs in DRL
```

---

## Example: Leave Approval Process

### **Input: Rule Expressions**
```
- "Reject if days > 5"
- "Reject if during peak delivery period"
- "Approve if all criteria met"
```

### **Step 1: Field Detection**
System detects:
- `days:Integer` (from "days > 5")
- `isPeakPeriod:Boolean` (from "peak delivery period")
- `data:Map` (always included)

### **Step 2: Generated ProcessData.java**

```java
package com.example.aibpmn.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Date;

/**
 * Dynamically generated Drools fact model for process data.
 * This class is auto-generated based on rule expressions.
 * DO NOT EDIT MANUALLY - will be regenerated on next DRL generation.
 */
public class ProcessData {

    private Integer days;
    private Boolean isPeakPeriod;
    private Map<String, Object> data;

    public ProcessData() {
        this.data = new HashMap<>();
    }

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    public Boolean getIsPeakPeriod() {
        return isPeakPeriod;
    }

    public void setIsPeakPeriod(Boolean isPeakPeriod) {
        this.isPeakPeriod = isPeakPeriod;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "ProcessData{" +
                "days=" + days +
                ", isPeakPeriod=" + isPeakPeriod +
                ", data=" + data +
                '}';
    }
}
```

### **Step 3: Generated RuleValidationResult.java**

```java
package com.example.aibpmn.model;

/**
 * Drools fact model for rule validation results.
 * This class captures the outcome of business rule execution.
 * DO NOT EDIT MANUALLY - will be regenerated on next DRL generation.
 */
public class RuleValidationResult {

    private String status;
    private String reason;

    public RuleValidationResult() {
    }

    public RuleValidationResult(String status, String reason) {
        this.status = status;
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public String toString() {
        return "RuleValidationResult{" +
                "status='" + status + '\'' +
                ", reason='" + reason + '\'' +
                '}';
    }
}
```

### **Step 4: Generated DRL**

```drools
package com.example.aibpmn.rules.leaveapproval;

import java.util.*;
import java.time.*;

// Input fact model (dynamically generated Java POJO)
import com.example.aibpmn.model.ProcessData;

// Output fact model (dynamically generated Java POJO)
import com.example.aibpmn.model.RuleValidationResult;

rule "RejectIfMoreThan5Days"
    salience 10
when
    $data: ProcessData(days > 5)
then
    System.out.println("❌ Rule fired: RejectIfMoreThan5Days - REJECTED");
    RuleValidationResult result = new RuleValidationResult();
    result.setStatus("REJECTED");
    result.setReason("Leave request exceeds 5 day limit");
    insert(result);
end
```

---

## Example 2: Expense Approval Process

### **Input: Rule Expressions**
```
- "Reject if amount > 1000"
- "Reject if no receipt provided"
```

### **Generated ProcessData.java**
```java
public class ProcessData {
    private Double amount;        // ← Different fields!
    private Boolean hasReceipt;   // ← Auto-detected!
    private Map<String, Object> data;
    
    // ... getters, setters, toString
}
```

**Notice:** Completely different fields based on the process type!

---

## Benefits

### ✅ **1. Type Safety**
- Compile-time checking for field names and types
- IDE autocomplete and refactoring support
- Catch errors before runtime

### ✅ **2. Reusability**
- POJOs can be used in Java code, tests, and DRL
- Single source of truth for fact model structure
- Easy to serialize/deserialize (JSON, XML)

### ✅ **3. IDE Support**
- Full IntelliJ/Eclipse integration
- Code navigation (Go to Definition)
- Javadoc generation

### ✅ **4. Flexibility**
- Still adaptive - POJOs regenerated for each process
- No manual coding required
- AI can freely generate any process type

### ✅ **5. Debugging**
- Easy to inspect objects in debugger
- Clear stack traces with class names
- toString() methods for logging

---

## Field Detection Patterns

The system auto-detects 15+ field types:

| Expression Pattern | Generated Field | Java Type |
|-------------------|----------------|-----------|
| "days > 5", "3 days" | `days` | `Integer` |
| "amount > 1000", "$500" | `amount` | `Double` |
| "peak period", "critical" | `isPeakPeriod` | `Boolean` |
| "eligible" | `isEligible` | `Boolean` |
| "receipt", "document" | `hasReceipt` | `Boolean` |
| "credit score" | `creditScore` | `Integer` |
| "income", "salary" | `income` | `Double` |
| "user", "employee" | `userId` | `String` |
| "department" | `department` | `String` |
| "category", "type" | `category` | `String` |
| "priority" | `priority` | `Integer` |
| "date", "period" | `startDate`, `endDate` | `Date` |
| "count", "quantity" | `count` | `Integer` |
| "hours" | `hours` | `Double` |
| "status" | `status` | `String` |
| *(always)* | `data` | `Map<String, Object>` |

---

## Implementation Details

### **Key Methods:**

#### 1. `extractRequiredFields(List<RuleModel> rules)`
- Scans all rule expressions
- Returns `Set<String>` of "fieldName:FieldType" pairs
- Uses regex and keyword matching

#### 2. `generateOrUpdateProcessDataPojo(Set<String> fields)`
- Generates complete Java source code
- Includes: package, imports, fields, constructors, getters/setters, toString
- Writes to `backend/src/main/java/com/example/aibpmn/model/ProcessData.java`

#### 3. `generateOrUpdateValidationResultPojo()`
- Generates standard result POJO
- Always the same structure (status, reason)
- Writes to `backend/src/main/java/com/example/aibpmn/model/RuleValidationResult.java`

#### 4. `mapDrlTypeToJava(String drlType)`
- Maps DRL types to Java types
- `Integer` → `Integer`
- `Double` → `Double`
- `Boolean` → `Boolean`
- `String` → `String`
- `Date` → `Date`
- `Map` → `Map<String, Object>`

#### 5. `writeJavaFile(String packageName, String className, String content)`
- Writes Java source file to disk
- Creates directories if needed
- Logs file path for debugging

---

## File Locations

### **Generated POJOs:**
```
backend/src/main/java/com/example/aibpmn/model/
├── ProcessData.java              ← Auto-generated (DO NOT EDIT)
└── RuleValidationResult.java     ← Auto-generated (DO NOT EDIT)
```

### **Generated DRL:**
```
backend/data/kogito/rules/
└── LeaveValidationRules.drl      ← Imports the POJOs
```

---

## Regeneration Trigger

POJOs are **regenerated** when:
1. A new process is created via AI Assistant
2. DRL generation is invoked (`DrlGeneratorService.generateDrl()`)
3. Rule expressions change (different fields detected)

**Warning:** POJOs are **overwritten** on each generation. Do not manually edit them!

---

## Testing

### **Test 1: Leave Approval**
1. Create process: "Reject if days > 5 or during peak period"
2. Check `backend/src/main/java/com/example/aibpmn/model/ProcessData.java`
3. Should have: `days`, `isPeakPeriod`, `data`

### **Test 2: Expense Approval**
1. Create process: "Reject if amount > 1000 or no receipt"
2. Check `ProcessData.java` again
3. Should now have: `amount`, `hasReceipt`, `data` (different fields!)

### **Test 3: DRL Compilation**
1. Check `backend/data/kogito/rules/*.drl`
2. Should contain: `import com.example.aibpmn.model.ProcessData;`
3. Should **NOT** contain: `declare ProcessData` (using Java import now)

---

## Comparison: Dynamic POJOs vs Static POJOs vs DRL Declarations

| Aspect | Dynamic POJOs ✅ | Static POJOs | DRL Declarations |
|--------|-----------------|--------------|------------------|
| **Flexibility** | ✅ Adapts to any process | ❌ One per type | ✅ Adapts |
| **Type Safety** | ✅ Compile-time | ✅ Compile-time | ⚠️ Runtime |
| **IDE Support** | ✅ Full | ✅ Full | ⚠️ Limited |
| **Reusability** | ✅ Java + DRL | ✅ Java + DRL | ❌ DRL only |
| **Code Changes** | ✅ None | ❌ Manual | ✅ None |
| **AI-Friendly** | ✅ Yes | ❌ Limited | ✅ Yes |
| **Debugging** | ✅ Easy | ✅ Easy | ⚠️ Harder |
| **Best For** | **AI-driven systems** | Fixed domains | Simple rules |

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    AI Assistant                             │
│  "Create leave approval process with days and peak period"  │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│              ProcessReasonerService                         │
│  Generates: RuleModel objects with expressions              │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│              DrlGeneratorService                            │
│  1. extractRequiredFields(rules)                            │
│     → ["days:Integer", "isPeakPeriod:Boolean", "data:Map"]  │
│                                                             │
│  2. generateOrUpdateProcessDataPojo(fields)                 │
│     → Writes ProcessData.java                               │
│                                                             │
│  3. generateOrUpdateValidationResultPojo()                  │
│     → Writes RuleValidationResult.java                      │
│                                                             │
│  4. Generate DRL with imports                               │
│     → import com.example.aibpmn.model.ProcessData;          │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│              File System                                    │
│  backend/src/main/java/com/example/aibpmn/model/           │
│  ├── ProcessData.java              ← Generated             │
│  └── RuleValidationResult.java     ← Generated             │
│                                                             │
│  backend/data/kogito/rules/                                 │
│  └── LeaveValidationRules.drl      ← Imports POJOs         │
└─────────────────────────────────────────────────────────────┘
```

---

## Advantages Over Pure DRL Declarations

### **Why Java POJOs are Better:**

1. **Compile-Time Safety**
   - Errors caught during compilation, not at runtime
   - Prevents typos in field names

2. **IDE Integration**
   - Autocomplete in both Java and DRL editors
   - Refactoring tools work seamlessly

3. **Reusability**
   - Use POJOs in Java services, controllers, tests
   - Serialize to JSON for APIs

4. **Debugging**
   - Inspect objects in debugger with full type info
   - Clear stack traces

5. **Documentation**
   - Javadoc generation
   - Self-documenting code

6. **Testing**
   - Easy to create mock objects
   - JUnit tests can use POJOs directly

---

## Future Enhancements

### **Possible Improvements:**

1. **Field Merging**
   - Merge fields from multiple processes
   - Create a "universal" ProcessData with all fields

2. **Versioning**
   - Keep history of generated POJOs
   - Support schema evolution

3. **Custom Annotations**
   - Add validation annotations (@NotNull, @Min, @Max)
   - Add JPA annotations for persistence

4. **Builder Pattern**
   - Generate builder classes for fluent API
   - `ProcessData.builder().days(5).isPeakPeriod(false).build()`

5. **Lombok Integration**
   - Use @Data, @Builder annotations
   - Reduce boilerplate code

---

## Conclusion

Dynamic Java POJO generation provides the **perfect balance** for an AI-driven BPMN compiler:

✅ **Adaptive** - Works with any process type  
✅ **Type-Safe** - Compile-time checking  
✅ **Reusable** - Java + DRL integration  
✅ **IDE-Friendly** - Full tooling support  
✅ **Zero Manual Work** - Fully automated  

**This is the ideal solution for production AI systems!** 🚀
