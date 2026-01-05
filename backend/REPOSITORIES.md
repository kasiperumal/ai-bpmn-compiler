# Repository Layer Documentation

## Overview

The repository layer provides data access abstraction for the AI-BPMN Compiler application. It follows the Repository pattern with interfaces for abstraction and in-memory implementations using `ConcurrentHashMap` for thread-safe operations.

---

## Architecture

```
┌─────────────────────┐
│   Service Layer     │
└──────────┬──────────┘
           │
           ├─── ProcessModelRepository (interface)
           │    └── InMemoryProcessModelRepository (impl)
           │
           ├─── ApprovalRepository (interface)
           │    └── InMemoryApprovalRepository (impl)
           │
           └─── ExplanationRepository (interface)
                └── InMemoryExplanationRepository (impl)
```

**Design Principles:**
- **Interface-based**: All repositories are defined as interfaces
- **Pluggable implementations**: Implementations can be swapped (in-memory → database)
- **Thread-safe**: Uses `ConcurrentHashMap` for concurrent access
- **Spring-managed**: Repositories are Spring beans (`@Repository`)
- **Immutable collections**: Returns defensive copies to prevent external modification

---

## Repositories

### 1. ProcessModelRepository

**Purpose**: Manages persistence of `ProcessModel` entities

**Interface**: `com.example.aibpmn.repository.ProcessModelRepository`  
**Implementation**: `com.example.aibpmn.repository.impl.InMemoryProcessModelRepository`

#### Operations

| Method | Description | Return Type |
|--------|-------------|-------------|
| `save(ProcessModel)` | Save or update a process | `ProcessModel` |
| `findById(String)` | Find process by ID | `Optional<ProcessModel>` |
| `findByIdAndVersion(String, String)` | Find by ID and version | `Optional<ProcessModel>` |
| `findAll()` | Get all processes | `List<ProcessModel>` |
| `findByStatus(ProcessStatus)` | Find by status (DRAFT/PUBLISHED) | `List<ProcessModel>` |
| `findByNameContaining(String)` | Search by name (case-insensitive) | `List<ProcessModel>` |
| `existsById(String)` | Check if process exists | `boolean` |
| `deleteById(String)` | Delete process by ID | `boolean` |
| `deleteAll()` | Delete all processes | `void` |
| `count()` | Count total processes | `long` |

#### Usage Example

```java
@Service
public class ProcessService {
    
    private final ProcessModelRepository repository;
    
    @Autowired
    public ProcessService(ProcessModelRepository repository) {
        this.repository = repository;
    }
    
    public ProcessModel createProcess(String id, String name, String version) {
        ProcessModel process = new ProcessModel(id, name, version);
        process.setStatus(ProcessStatus.DRAFT);
        return repository.save(process);
    }
    
    public List<ProcessModel> getAllDraftProcesses() {
        return repository.findByStatus(ProcessStatus.DRAFT);
    }
    
    public Optional<ProcessModel> getProcess(String id) {
        return repository.findById(id);
    }
}
```

---

### 2. ApprovalRepository

**Purpose**: Manages persistence of `Approval` entities

**Interface**: `com.example.aibpmn.repository.ApprovalRepository`  
**Implementation**: `com.example.aibpmn.repository.impl.InMemoryApprovalRepository`

#### Operations

| Method | Description | Return Type |
|--------|-------------|-------------|
| `save(Approval)` | Save or update an approval | `Approval` |
| `findByNodeId(String)` | Find approval by node ID | `Optional<Approval>` |
| `findAll()` | Get all approvals | `List<Approval>` |
| `findByAiApproved(Boolean)` | Find by AI approval status | `List<Approval>` |
| `findByUserApproved(Boolean)` | Find by user approval status | `List<Approval>` |
| `findFullyApproved()` | Find fully approved (AI + user) | `List<Approval>` |
| `findPendingApproval()` | Find pending approvals | `List<Approval>` |
| `findByApprovedBy(String)` | Find by approver | `List<Approval>` |
| `existsByNodeId(String)` | Check if approval exists | `boolean` |
| `deleteByNodeId(String)` | Delete approval by node ID | `boolean` |
| `deleteAll()` | Delete all approvals | `void` |
| `count()` | Count total approvals | `long` |

#### Usage Example

```java
@Service
public class ApprovalService {
    
    private final ApprovalRepository repository;
    
    @Autowired
    public ApprovalService(ApprovalRepository repository) {
        this.repository = repository;
    }
    
    public Approval aiApprove(String nodeId, String comment) {
        Approval approval = repository.findByNodeId(nodeId)
            .orElse(new Approval(nodeId));
        
        approval.setAiApproved(true);
        approval.setAiComment(comment);
        
        return repository.save(approval);
    }
    
    public Approval userApprove(String nodeId, String user, String comment) {
        Approval approval = repository.findByNodeId(nodeId)
            .orElseThrow(() -> new NotFoundException("Approval not found"));
        
        approval.setUserApproved(true);
        approval.setUserComment(comment);
        approval.setApprovedBy(user);
        
        return repository.save(approval);
    }
    
    public List<Approval> getPendingApprovals() {
        return repository.findPendingApproval();
    }
}
```

---

### 3. ExplanationRepository

**Purpose**: Manages persistence of `Explanation` entities

**Interface**: `com.example.aibpmn.repository.ExplanationRepository`  
**Implementation**: `com.example.aibpmn.repository.impl.InMemoryExplanationRepository`

#### Operations

| Method | Description | Return Type |
|--------|-------------|-------------|
| `save(Explanation)` | Save or update an explanation | `Explanation` |
| `findByNodeId(String)` | Find explanation by node ID | `Optional<Explanation>` |
| `findAll()` | Get all explanations | `List<Explanation>` |
| `findBySource(String)` | Find by source (e.g., "AI-Generated") | `List<Explanation>` |
| `findByConfidenceScoreGreaterThan(Double)` | Find by min confidence | `List<Explanation>` |
| `findByReasonContaining(String)` | Search in reason text | `List<Explanation>` |
| `existsByNodeId(String)` | Check if explanation exists | `boolean` |
| `deleteByNodeId(String)` | Delete explanation by node ID | `boolean` |
| `deleteAll()` | Delete all explanations | `void` |
| `count()` | Count total explanations | `long` |

#### Usage Example

```java
@Service
public class ExplanationService {
    
    private final ExplanationRepository repository;
    
    @Autowired
    public ExplanationService(ExplanationRepository repository) {
        this.repository = repository;
    }
    
    public Explanation addExplanation(String nodeId, String reason, Double confidence) {
        Explanation explanation = new Explanation(nodeId, reason);
        explanation.setSource("AI-Generated");
        explanation.setConfidenceScore(confidence);
        
        return repository.save(explanation);
    }
    
    public List<Explanation> getHighConfidenceExplanations() {
        return repository.findByConfidenceScoreGreaterThan(0.90);
    }
    
    public List<Explanation> getAiGeneratedExplanations() {
        return repository.findBySource("AI-Generated");
    }
}
```

---

## Thread Safety

All in-memory implementations use `ConcurrentHashMap` which provides:

- **Thread-safe operations**: Multiple threads can read/write concurrently
- **No external synchronization needed**: Internal locking mechanisms
- **High performance**: Lock striping for better concurrency
- **Fail-safe iterators**: Modifications don't cause `ConcurrentModificationException`

### Example: Concurrent Access

```java
// Safe to use from multiple threads
ProcessModelRepository repository = ...; // Spring-injected

// Thread 1
CompletableFuture.runAsync(() -> {
    ProcessModel p1 = new ProcessModel("proc-001", "Process 1", "1.0");
    repository.save(p1);
});

// Thread 2
CompletableFuture.runAsync(() -> {
    ProcessModel p2 = new ProcessModel("proc-002", "Process 2", "1.0");
    repository.save(p2);
});

// Thread 3
CompletableFuture.runAsync(() -> {
    List<ProcessModel> all = repository.findAll();
    // Safe iteration
});
```

---

## Defensive Copying

All `findAll()` and finder methods return **defensive copies** using `List.copyOf()`:

```java
public List<ProcessModel> findAll() {
    return List.copyOf(storage.values()); // Immutable copy
}
```

**Benefits:**
- External code cannot modify internal storage
- Prevents accidental data corruption
- Maintains encapsulation

---

## Implementation Details

### Storage Key Strategy

| Repository | Key | Value |
|------------|-----|-------|
| ProcessModelRepository | Process ID | ProcessModel |
| ApprovalRepository | Node ID | Approval |
| ExplanationRepository | Node ID | Explanation |

### Update Strategy

All repositories use **upsert semantics**:
- `save()` will **insert** if key doesn't exist
- `save()` will **update** if key exists

```java
// First save - INSERT
ProcessModel process = new ProcessModel("proc-001", "Original", "1.0");
repository.save(process); // Creates new entry

// Second save - UPDATE
process.setName("Updated");
repository.save(process); // Updates existing entry
```

---

## Migration to Database

To migrate from in-memory to database storage:

### Option 1: JPA/Hibernate

```java
@Repository
public class JpaProcessModelRepository implements ProcessModelRepository {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Override
    public ProcessModel save(ProcessModel process) {
        return entityManager.merge(process);
    }
    
    @Override
    public Optional<ProcessModel> findById(String id) {
        return Optional.ofNullable(entityManager.find(ProcessModel.class, id));
    }
    
    // ... implement other methods
}
```

### Option 2: Spring Data JPA

```java
public interface ProcessModelRepository extends JpaRepository<ProcessModel, String> {
    
    // Spring Data auto-implements basic CRUD
    
    // Custom queries
    List<ProcessModel> findByStatus(ProcessStatus status);
    
    List<ProcessModel> findByNameContainingIgnoreCase(String name);
    
    @Query("SELECT p FROM ProcessModel p WHERE p.id = :id AND p.version = :version")
    Optional<ProcessModel> findByIdAndVersion(
        @Param("id") String id, 
        @Param("version") String version
    );
}
```

**No service layer changes needed** - thanks to interface abstraction!

---

## Testing

### Unit Tests

All repositories have comprehensive unit tests:

- **ProcessModelRepositoryTest**: 14 tests
- **ApprovalRepositoryTest**: 12 tests  
- **ExplanationRepositoryTest**: 11 tests

**Total**: 37 tests, all passing ✅

### Test Coverage

```
✅ Save operations
✅ Find operations (by ID, by attributes)
✅ Update operations
✅ Delete operations
✅ Count operations
✅ Null handling
✅ Edge cases
✅ Search operations (case-insensitive, partial match)
✅ Complex filters (confidence score, approval status)
```

### Running Tests

```bash
# All repository tests
./gradlew test --tests "*Repository*"

# Specific repository
./gradlew test --tests "ProcessModelRepositoryTest"
```

---

## Performance Characteristics

### Time Complexity

| Operation | Complexity | Notes |
|-----------|-----------|-------|
| `save()` | O(1) | HashMap put |
| `findById()` | O(1) | HashMap get |
| `findAll()` | O(n) | Iterate all values |
| `findBy*()` | O(n) | Stream filter |
| `delete()` | O(1) | HashMap remove |
| `count()` | O(1) | HashMap size |

### Space Complexity

- **Memory**: O(n) where n = number of entities
- **In-memory storage**: Not suitable for large datasets (>100K entities)
- **For production**: Consider database-backed implementation

---

## Best Practices

### 1. Use Constructor Injection

```java
@Service
public class MyService {
    private final ProcessModelRepository repository;
    
    @Autowired // Optional in Spring 4.3+
    public MyService(ProcessModelRepository repository) {
        this.repository = repository;
    }
}
```

### 2. Handle Optional Results

```java
// Good
Optional<ProcessModel> result = repository.findById(id);
result.ifPresent(process -> {
    // Process found
});

// Or
ProcessModel process = repository.findById(id)
    .orElseThrow(() -> new NotFoundException("Process not found"));
```

### 3. Use Appropriate Finder Methods

```java
// Don't do this
List<ProcessModel> all = repository.findAll();
List<ProcessModel> drafts = all.stream()
    .filter(p -> p.getStatus() == ProcessStatus.DRAFT)
    .collect(Collectors.toList());

// Do this
List<ProcessModel> drafts = repository.findByStatus(ProcessStatus.DRAFT);
```

### 4. Validate Inputs

```java
public ProcessModel getProcess(String id) {
    if (id == null || id.isBlank()) {
        throw new IllegalArgumentException("Process ID cannot be null or empty");
    }
    return repository.findById(id)
        .orElseThrow(() -> new NotFoundException("Process not found: " + id));
}
```

---

## Future Enhancements

### Planned Features

1. **Pagination Support**
   ```java
   Page<ProcessModel> findAll(Pageable pageable);
   ```

2. **Sorting**
   ```java
   List<ProcessModel> findAll(Sort sort);
   ```

3. **Batch Operations**
   ```java
   List<ProcessModel> saveAll(List<ProcessModel> processes);
   void deleteAll(List<String> ids);
   ```

4. **Audit Trail**
   ```java
   @CreatedDate
   private LocalDateTime createdAt;
   
   @LastModifiedDate
   private LocalDateTime updatedAt;
   ```

5. **Query DSL Support**
   ```java
   List<ProcessModel> findAll(Specification<ProcessModel> spec);
   ```

---

## Summary

✅ **3 Repository Interfaces** - Clean abstractions  
✅ **3 In-Memory Implementations** - ConcurrentHashMap-based  
✅ **37 Unit Tests** - All passing  
✅ **Thread-Safe** - Concurrent access supported  
✅ **Defensive Copies** - Encapsulation maintained  
✅ **Spring-Managed** - @Repository beans  
✅ **Migration-Ready** - Easy switch to database  

The repository layer provides a solid foundation for data access with flexibility to evolve as requirements grow.

---

## See Also

- [Model Classes Documentation](MODEL_CLASSES.md)
- [Spring Data JPA Reference](https://spring.io/projects/spring-data-jpa)
- [ConcurrentHashMap JavaDoc](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/ConcurrentHashMap.html)

