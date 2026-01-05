# Rule Detection Service - Quick Summary

## Purpose
Automatically detects business rules from natural language text by identifying conditions, thresholds, comparisons, and rule patterns.

## Key Features
- ✅ Pattern-based rule detection
- ✅ 6 rule types supported
- ✅ Automatic operator normalization
- ✅ Currency/number formatting
- ✅ Confidence scoring
- ✅ Rule deduplication
- ✅ Quick indicator checking

## Usage

### Basic Detection

```java
@Autowired
private RuleDetectionService ruleDetectionService;

List<DetectedRule> rules = ruleDetectionService.detectRules(description);

for (DetectedRule detected : rules) {
    System.out.println("Rule: " + detected.getRule().getExpression());
    System.out.println("Type: " + detected.getRuleType());
    System.out.println("Confidence: " + detected.getConfidence());
}
```

### Quick Check

```java
if (ruleDetectionService.containsRuleIndicators(text)) {
    // Text contains rules
    List<DetectedRule> rules = ruleDetectionService.detectRules(text);
}
```

## Rule Types

| Type | Patterns | Confidence | Example |
|------|----------|------------|---------|
| **CONDITIONAL** | if, when, whenever | 0.8 | "If amount > 1000..." |
| **THRESHOLD** | amount/value + comparison | 0.9 | "Amount greater than $5000" |
| **COMPARISON** | greater/less than + number | 0.75 | "Quantity less than 100" |
| **APPROVAL** | requires approval | 0.85 | "Needs manager approval" |
| **VALIDATION** | validate, verify, check | 0.7 | "Validate email format" |
| **CALCULATION** | calculate, sum, total | 0.75 | "Calculate total price" |

## Operator Normalization

| Natural Language | Code |
|------------------|------|
| greater than, more than, exceeds | `>` |
| less than, below, under | `<` |
| equals, is, are | `==` |
| not equal | `!=` |
| at least | `>=` |
| at most | `<=` |

## Examples

### Example 1: Simple Threshold

**Input**:
```
"The order amount is greater than $5000 for automatic approval."
```

**Output**:
```
Rule: amount > 5000
Type: THRESHOLD
Confidence: 0.9
Priority: 60
```

### Example 2: Conditional with Approval

**Input**:
```
"If the order amount exceeds $1000, then manager approval is required."
```

**Output**:
```
Rule 1: order amount exceeds $1000 > 1000
Type: CONDITIONAL
Confidence: 0.8

Rule 2: requiresApproval("authorized person")
Type: APPROVAL
Confidence: 0.85
```

### Example 3: Multiple Rules

**Input**:
```
If order value > $5000, requires CFO approval.
Validate customer information is complete.
Calculate total by summing line items.
```

**Output**:
```
Rule 1: CONDITIONAL - order value > 5000
Rule 2: APPROVAL - requiresApproval(...)
Rule 3: VALIDATION - validate(customer information...)
Rule 4: CALCULATION - calculate(total...)
```

## Value Cleaning

Automatically handles:

```
"$1,000" → "1000"
"€8,500.50" → "8500.50"
"25%" → "25%" (preserved)
```

## DetectedRule Structure

```java
DetectedRule {
    RuleModel rule;         // The extracted rule
    double confidence;      // 0.0 to 1.0
    String detectionReason; // Why detected
    String sourceText;      // Original text
    RuleType ruleType;      // Classification
}
```

## Priority Assignment

Rules get automatic priorities:

| Type | Priority | Rationale |
|------|----------|-----------|
| CALCULATION | 40 | Execute first |
| CONDITIONAL | 50 | Evaluate second |
| COMPARISON | 55 | Similar to conditionals |
| THRESHOLD | 60 | Enforce limits |
| VALIDATION | 65 | Validate data |
| APPROVAL | 70 | Final checks |

## Integration

### With ProcessReasonerService

```java
ReasoningResult result = processReasonerService.reasonOverDescription(description);
List<DetectedRule> rules = ruleDetectionService.detectRules(description);

// Add detected rules
rules.forEach(r -> result.addRule(r.getRule()));
```

### With AI Inference

```java
// After AI extracts description
String description = aiInferenceService.inferProcessDescriptionFromImage(processId);
List<DetectedRule> rules = ruleDetectionService.detectRules(description);
```

## Filtering

### By Confidence

```java
List<DetectedRule> highConfidence = rules.stream()
    .filter(r -> r.getConfidence() >= 0.8)
    .collect(Collectors.toList());
```

### By Type

```java
List<DetectedRule> thresholds = rules.stream()
    .filter(r -> r.getRuleType() == RuleType.THRESHOLD)
    .collect(Collectors.toList());
```

### By Priority

```java
rules.sort(Comparator.comparing(r -> r.getRule().getPriority()));
```

## Testing

```bash
# Run tests
./gradlew test --tests RuleDetectionServiceTest

# All 24 tests should pass
✅ Conditional detection
✅ Threshold detection
✅ Comparison detection
✅ Approval detection
✅ Validation detection
✅ Calculation detection
✅ Multiple rules
✅ Currency handling
✅ Percentage handling
✅ Deduplication
✅ Priority assignment
✅ Indicator checking
```

## Common Patterns

### 1. Pre-screen

```java
if (ruleDetectionService.containsRuleIndicators(text)) {
    List<DetectedRule> rules = ruleDetectionService.detectRules(text);
    processRules(rules);
}
```

### 2. Filter and Process

```java
List<DetectedRule> rules = ruleDetectionService.detectRules(text);

// Process high-confidence rules
rules.stream()
    .filter(r -> r.getConfidence() >= 0.8)
    .forEach(this::applyRule);

// Log low-confidence rules for review
rules.stream()
    .filter(r -> r.getConfidence() < 0.7)
    .forEach(r -> logger.warn("Review: {}", r.getDetectionReason()));
```

### 3. Group by Type

```java
Map<RuleType, List<DetectedRule>> grouped = rules.stream()
    .collect(Collectors.grouping(DetectedRule::getRuleType));

// Process thresholds first
processThresholds(grouped.get(RuleType.THRESHOLD));

// Then conditionals
processConditionals(grouped.get(RuleType.CONDITIONAL));
```

## Limitations

- **Pattern-based**: Only detects known patterns
- **English only**: Currently single-language
- **Simple logic**: Complex boolean expressions not supported
- **Heuristic**: Confidence is an estimate
- **Context-free**: No business domain understanding

## Best Practices

1. ✅ Always check confidence scores
2. ✅ Review low-confidence rules manually
3. ✅ Use in combination with AI for best results
4. ✅ Filter by rule type for targeted processing
5. ✅ Log detection reasons for debugging

## Files

- **Service**: `RuleDetectionService.java`
- **DTO**: `DetectedRule.java`
- **Tests**: `RuleDetectionServiceTest.java`
- **Docs**: `RULE_DETECTION.md`

## Related Services

- `ProcessReasonerService` - Uses for process inference
- `AiInferenceService` - Provides input descriptions
- `BpmnGeneratorService` - Consumes detected rules
- `GeminiClient` - Can enhance detection

---

For detailed documentation, see [RULE_DETECTION.md](./RULE_DETECTION.md)

