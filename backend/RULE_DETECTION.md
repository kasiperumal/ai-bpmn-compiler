# Rule Detection Service

## Overview

The `RuleDetectionService` automatically detects business rules from natural language text by identifying conditions, thresholds, comparisons, and other rule patterns. It uses regex-based pattern matching and heuristics to extract structured rule information.

## Features

### Core Capabilities

1. **Pattern-Based Detection**
   - Conditional rules (if-then-else)
   - Threshold rules (numeric limits)
   - Comparison rules (greater than, less than, equals)
   - Approval rules (authorization requirements)
   - Validation rules (data checks)
   - Calculation rules (mathematical operations)

2. **Smart Extraction**
   - Automatic operator normalization (> , <, ==, !=, >=, <=)
   - Currency symbol handling ($, €, £)
   - Numeric value cleaning (commas, formatting)
   - Confidence scoring (0.0 to 1.0)

3. **Rule Deduplication**
   - Identifies similar rules
   - Keeps highest confidence version
   - Prevents duplicate entries

4. **Rule Indicators**
   - Quick check for rule presence
   - Multiple indicator types
   - Fast pre-screening

## Rule Types

### 1. Conditional Rules (`RuleType.CONDITIONAL`)

Detects if-then-else patterns.

**Patterns**:
- `if`, `when`, `whenever`, `in case`, `assuming`, `provided that`

**Examples**:
```
"If the order amount exceeds $1000, then manager approval is required."
→ Detected: expression = "order amount exceeds $1000 > 1000"

"When customer type is premium, apply discount."
→ Detected: expression = "customer type == premium"
```

**Confidence**: 0.8 (High)

### 2. Threshold Rules (`RuleType.THRESHOLD`)

Detects numeric limits and boundaries.

**Patterns**:
- `amount|value|price|cost|total|sum|count|quantity` + comparison + number

**Examples**:
```
"The order amount is greater than $5000 for automatic approval."
→ Detected: expression = "amount > 5000"

"Discount percentage is at most 25%."
→ Detected: expression = "percentage <= 25"
```

**Confidence**: 0.9 (Very High)

### 3. Comparison Rules (`RuleType.COMPARISON`)

Detects general comparison operations.

**Patterns**:
- `greater than|less than|exceeds|equals` + numeric value

**Examples**:
```
"Check if the quantity is less than 100 items."
→ Detected: expression = "quantity < 100"

"Value must equal 500."
→ Detected: expression = "Value == 500"
```

**Confidence**: 0.75 (Good)

### 4. Approval Rules (`RuleType.APPROVAL`)

Detects authorization and approval requirements.

**Patterns**:
- `requires|needs|must have` + `approval|authorization|permission|review`

**Examples**:
```
"This transaction requires approval from the finance manager."
→ Detected: expression = "requiresApproval(\"finance manager\")"

"High-value orders need authorization."
→ Detected: expression = "requiresApproval(\"authorized person\")"
```

**Confidence**: 0.85 (High)

### 5. Validation Rules (`RuleType.VALIDATION`)

Detects data validation requirements.

**Patterns**:
- `validate|verify|check|ensure|confirm`

**Examples**:
```
"Validate that the customer email is in the correct format."
→ Detected: expression = "validate(customer email == in the correct format)"

"Ensure inventory is sufficient."
→ Detected: expression = "ensure(inventory == sufficient)"
```

**Confidence**: 0.7 (Moderate)

### 6. Calculation Rules (`RuleType.CALCULATION`)

Detects mathematical operations.

**Patterns**:
- `calculate|compute|sum|total|add|subtract|multiply|divide|average`

**Examples**:
```
"Calculate the total price by multiplying quantity and unit price."
→ Detected: expression = "calculate(total price by multiplying quantity and unit price)"

"Sum all line items."
→ Detected: expression = "sum(all line items)"
```

**Confidence**: 0.75 (Good)

## Usage

### Basic Rule Detection

```java
@Autowired
private RuleDetectionService ruleDetectionService;

public void detectRulesFromDescription(String description) {
    List<DetectedRule> rules = ruleDetectionService.detectRules(description);
    
    for (DetectedRule detected : rules) {
        RuleModel rule = detected.getRule();
        System.out.println("Rule: " + rule.getDescription());
        System.out.println("Expression: " + rule.getExpression());
        System.out.println("Type: " + detected.getRuleType());
        System.out.println("Confidence: " + detected.getConfidence());
    }
}
```

### Integration with ProcessReasonerService

```java
@Autowired
private RuleDetectionService ruleDetectionService;

@Autowired
private ProcessReasonerService processReasonerService;

public ProcessModel buildProcessWithRules(String description) {
    // Reason over the description
    ReasoningResult result = processReasonerService.reasonOverDescription(description);
    
    // Detect additional rules
    List<DetectedRule> detectedRules = ruleDetectionService.detectRules(description);
    
    // Add detected rules to the result
    for (DetectedRule detected : detectedRules) {
        result.addRule(detected.getRule());
    }
    
    // Build process model
    ProcessModel process = new ProcessModel();
    process.setNodes(result.getNodes());
    process.setEdges(result.getEdges());
    process.setRules(result.getRules());
    
    return process;
}
```

### Quick Check for Rule Presence

```java
public boolean hasRules(String text) {
    return ruleDetectionService.containsRuleIndicators(text);
}

// Usage
if (hasRules("If amount > 1000 then approve")) {
    // Process has rules
    List<DetectedRule> rules = ruleDetectionService.detectRules(text);
}
```

### Filter by Rule Type

```java
public List<DetectedRule> getThresholdRules(String description) {
    List<DetectedRule> allRules = ruleDetectionService.detectRules(description);
    
    return allRules.stream()
        .filter(r -> r.getRuleType() == RuleType.THRESHOLD)
        .collect(Collectors.toList());
}
```

### Filter by Confidence

```java
public List<DetectedRule> getHighConfidenceRules(String description) {
    List<DetectedRule> allRules = ruleDetectionService.detectRules(description);
    
    return allRules.stream()
        .filter(r -> r.getConfidence() >= 0.8)
        .collect(Collectors.toList());
}
```

## DetectedRule Structure

```java
public class DetectedRule {
    private RuleModel rule;           // The extracted rule
    private double confidence;        // 0.0 to 1.0
    private String detectionReason;   // Why it was detected
    private String sourceText;        // Original text
    private RuleType ruleType;        // Rule classification
}
```

### RuleModel Fields

```java
public class RuleModel {
    private String id;              // UUID
    private String expression;      // Code-friendly expression
    private String description;     // Human-readable description
    private String ruleType;        // "CONDITIONAL", "THRESHOLD", etc.
    private Integer priority;       // 40-70 (varies by type)
    private boolean enabled;        // true by default
}
```

## Operator Normalization

The service normalizes natural language operators to code-friendly symbols:

| Natural Language | Normalized |
|------------------|------------|
| greater than, more than, exceeds, over, above | `>` |
| less than, below, under, fewer than | `<` |
| equals, equal to, is, are, matches | `==` |
| not equal, different from | `!=` |
| at least, minimum of | `>=` |
| at most, maximum of | `<=` |

**Example**:
```
"amount greater than 1000" → "amount > 1000"
"value is equal to 500" → "value == 500"
"quantity at least 10" → "quantity >= 10"
```

## Value Cleaning

### Currency Symbols

Automatically removes currency symbols from expressions:

```
"$1,000" → "1000"
"€8,500" → "8500"
"£2,500.50" → "2500.50"
```

### Percentages

Preserves percentage symbols:

```
"25%" → "25%"
"10.5%" → "10.5%"
```

### Commas

Removes thousand separators:

```
"1,000,000" → "1000000"
"5,500.75" → "5500.75"
```

## Priority Assignment

Rules are assigned priorities based on their type:

| Rule Type | Priority | Rationale |
|-----------|----------|-----------|
| CALCULATION | 40 | Execute calculations first |
| CONDITIONAL | 50 | Evaluate conditions second |
| COMPARISON | 55 | Similar to conditionals |
| THRESHOLD | 60 | Enforce limits |
| VALIDATION | 65 | Validate data |
| APPROVAL | 70 | Final approval checks |

## Examples

### Example 1: Order Approval Process

**Input**:
```
If the order amount exceeds $1000, manager approval is required.
The discount percentage must be at most 25%.
Validate that customer information is complete.
```

**Detected Rules**:

1. **Conditional Rule**
   - Expression: `order amount exceeds $1000 > 1000`
   - Type: CONDITIONAL
   - Confidence: 0.8
   - Priority: 50

2. **Threshold Rule**
   - Expression: `percentage <= 25`
   - Type: THRESHOLD
   - Confidence: 0.9
   - Priority: 60

3. **Validation Rule**
   - Expression: `validate(customer information == complete)`
   - Type: VALIDATION
   - Confidence: 0.7
   - Priority: 65

### Example 2: Purchase Order Process

**Input**:
```
When order value is greater than $5000, it requires authorization from the CFO.
Calculate the total by summing all line items.
Ensure inventory levels are sufficient before approving.
```

**Detected Rules**:

1. **Conditional Rule**
   - Expression: `order value > 5000`
   - Type: CONDITIONAL
   - Confidence: 0.8

2. **Calculation Rule**
   - Expression: `calculate(total by summing all line items)`
   - Type: CALCULATION
   - Confidence: 0.75

3. **Validation Rule**
   - Expression: `ensure(inventory levels are sufficient before approving)`
   - Type: VALIDATION
   - Confidence: 0.7

### Example 3: Employee Onboarding

**Input**:
```
If employee type is contractor, limit access duration to 6 months.
Verify that all required documents are submitted.
The security clearance level must be at least 2.
```

**Detected Rules**:

1. **Conditional Rule**
   - Expression: `employee type == contractor`
   - Type: CONDITIONAL
   - Confidence: 0.8

2. **Validation Rule**
   - Expression: `verify(all required documents are submitted)`
   - Type: VALIDATION
   - Confidence: 0.7

3. **Threshold Rule**
   - Expression: `level >= 2`
   - Type: THRESHOLD
   - Confidence: 0.9

## Testing

### Test Coverage

The `RuleDetectionServiceTest` provides comprehensive coverage:

1. **Rule Type Detection** (6 tests)
   - Conditional, threshold, comparison
   - Approval, validation, calculation

2. **Multiple Rules** (1 test)
   - Detecting multiple rules in one text

3. **Edge Cases** (3 tests)
   - Empty/null text
   - No rules present
   - Complex descriptions

4. **Value Handling** (2 tests)
   - Currency symbols
   - Percentages

5. **Complex Scenarios** (2 tests)
   - Multiple thresholds
   - Multiple conditions

6. **Indicators** (6 tests)
   - Rule presence checking
   - Various indicator types

7. **Features** (4 tests)
   - Deduplication
   - Priority assignment
   - Descriptions
   - Enabled by default

### Running Tests

```bash
# Run rule detection tests
./gradlew test --tests RuleDetectionServiceTest

# Run all tests
./gradlew test

# Run with verbose output
./gradlew test --tests RuleDetectionServiceTest --info
```

## Best Practices

1. **Pre-screen with Indicators**
   ```java
   if (ruleDetectionService.containsRuleIndicators(text)) {
       List<DetectedRule> rules = ruleDetectionService.detectRules(text);
   }
   ```

2. **Filter by Confidence**
   ```java
   List<DetectedRule> highConfidenceRules = rules.stream()
       .filter(r -> r.getConfidence() >= 0.8)
       .collect(Collectors.toList());
   ```

3. **Group by Type**
   ```java
   Map<RuleType, List<DetectedRule>> rulesByType = rules.stream()
       .collect(Collectors.grouping(DetectedRule::getRuleType));
   ```

4. **Review Low Confidence Rules**
   ```java
   rules.stream()
       .filter(r -> r.getConfidence() < 0.7)
       .forEach(r -> logger.warn("Low confidence rule: {}", r.getDetectionReason()));
   ```

5. **Combine with AI**
   ```java
   // Use AI for complex rules
   String aiProcessed = geminiClient.generateFromText(
       "Extract business rules from: " + description
   );
   
   // Use pattern matching for simple rules
   List<DetectedRule> rules = ruleDetectionService.detectRules(description);
   ```

## Limitations

1. **Pattern-Based** - Only detects rules matching known patterns
2. **English Only** - Currently supports English language text
3. **Simple Logic** - Complex boolean expressions may not be detected
4. **Heuristic** - Confidence scores are estimates, not guarantees
5. **Context-Free** - Doesn't understand business domain context

## Future Enhancements

1. **AI-Enhanced Detection** - Use Gemini for complex rule extraction
2. **Multi-Language Support** - Detect rules in other languages
3. **Custom Patterns** - Allow users to define custom rule patterns
4. **Boolean Logic** - Handle AND/OR/NOT combinations
5. **Context Awareness** - Consider business domain for better detection
6. **Rule Refinement** - Interactive refinement of detected rules
7. **Semantic Understanding** - Use NLP for deeper understanding

## Integration Points

### With ProcessReasonerService

```java
// ProcessReasonerService can use RuleDetectionService
public ReasoningResult reasonOverDescription(String description) {
    // ... other reasoning ...
    
    // Detect rules
    List<DetectedRule> rules = ruleDetectionService.detectRules(description);
    for (DetectedRule detected : rules) {
        result.addRule(detected.getRule());
    }
    
    return result;
}
```

### With AiInferenceService

```java
// Use after AI extracts process description
String description = aiInferenceService.inferProcessDescriptionFromImage(processId);
List<DetectedRule> rules = ruleDetectionService.detectRules(description);
```

### With BpmnGeneratorService

```java
// Add rules to process before generating BPMN
ProcessModel process = new ProcessModel();
// ... add nodes and edges ...

List<DetectedRule> rules = ruleDetectionService.detectRules(description);
rules.forEach(r -> process.addRule(r.getRule()));

String bpmn = bpmnGenerator.generateBpmn(process);
```

## Related Services

- **ProcessReasonerService** - Uses rule detection for process inference
- **AiInferenceService** - Provides text descriptions for rule detection
- **BpmnGeneratorService** - Consumes detected rules for BPMN generation
- **GeminiClient** - Can enhance rule detection with AI

## Files

- **Service**: `RuleDetectionService.java`
- **DTO**: `DetectedRule.java`
- **Tests**: `RuleDetectionServiceTest.java` (24 test cases)
- **Docs**: `RULE_DETECTION.md`

## References

- [Business Rules Management](https://en.wikipedia.org/wiki/Business_rules_engine)
- [Drools Documentation](https://docs.jboss.org/drools/release/latest-final/drools-docs/html_single/)
- [Pattern Matching in Java](https://docs.oracle.com/javase/tutorial/essential/regex/)

