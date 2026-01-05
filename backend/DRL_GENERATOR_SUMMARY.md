# DRL Generator Service - Quick Summary

## Purpose
Converts `RuleModel` objects to Drools Rule Language (DRL) format for Kogito/Drools execution.

## Key Features
- ✅ Single/multiple rule generation
- ✅ 6 rule types supported
- ✅ Custom package names
- ✅ Priority (salience) configuration
- ✅ Enabled/disabled rule handling
- ✅ Optional Drools validation
- ✅ Special character escaping

## Usage

### Basic Generation

```java
@Autowired
private DrlGeneratorService drlGenerator;

// Single rule
String drl = drlGenerator.generateDrl(rule);

// Multiple rules
String drl = drlGenerator.generateDrl(rules);

// Custom package
String drl = drlGenerator.generateDrl(rules, "com.custom.package", false);
```

## Output Format

```drl
package com.example.aibpmn.rules;

import java.util.*;
import java.time.*;

rule "rule-id"
    salience 50
    // Rule description
when
    eval(true)
then
    System.out.println("Rule fired: rule-id");
end
```

## Rule Types

| Type | Priority | Description |
|------|----------|-------------|
| APPROVAL | 70 | Authorization checks |
| VALIDATION | 65 | Data validation |
| THRESHOLD | 60 | Numeric limits |
| COMPARISON | 55 | Value comparisons |
| CONDITIONAL | 50 | If-then logic |
| CALCULATION | 40 | Math operations |

## Examples

### Example 1: Single Rule

```java
RuleModel rule = new RuleModel();
rule.setId("high-value-check");
rule.setExpression("amount > 1000");
rule.setDescription("High value order");
rule.setRuleType("THRESHOLD");
rule.setPriority(60);
rule.setEnabled(true);

String drl = drlGenerator.generateDrl(rule);
```

**Output**:
```drl
package com.example.aibpmn.rules;

import java.util.*;
import java.time.*;

rule "high-value-check"
    salience 60
    // High value order
when
    eval(true)
then
    System.out.println("Rule fired: high-value-check");
end
```

### Example 2: Multiple Rules

```java
List<RuleModel> rules = Arrays.asList(
    createRule("calc-total", "CALCULATION", 40),
    createRule("validate-data", "VALIDATION", 65),
    createRule("approve-order", "APPROVAL", 70)
);

String drl = drlGenerator.generateDrl(rules);
// Generates 3 rules in order of priority
```

## Priority (Salience)

Higher values execute first:

```
70 → APPROVAL     (execute last)
65 → VALIDATION
60 → THRESHOLD
55 → COMPARISON
50 → CONDITIONAL
40 → CALCULATION  (execute first)
```

## Special Handling

### Disabled Rules

```java
rule.setEnabled(false);
// Rule will NOT appear in generated DRL
```

### Special Characters

```java
rule.setId("rule-with-\"quotes\"");
// Escaped: rule "rule-with-\"quotes\""
```

### Null Expression

```java
rule.setExpression(null);
// Generated: when eval(true)
```

### No Priority

```java
rule.setPriority(null);
// No salience line in DRL
```

## Integration

### With RuleDetectionService

```java
// Detect rules from text
List<DetectedRule> detected = ruleDetector.detectRules(description);
List<RuleModel> rules = detected.stream()
    .map(DetectedRule::getRule)
    .collect(Collectors.toList());

// Generate DRL
String drl = drlGenerator.generateDrl(rules);
```

### With ProcessReasonerService

```java
ReasoningResult result = reasoner.reasonOverDescription(text);
String drl = drlGenerator.generateDrl(result.getRules());
```

### Save to File

```java
String drl = drlGenerator.generateDrl(rules);
Files.writeString(Paths.get("rules.drl"), drl);
```

## Testing

```bash
# Run tests
./gradlew test --tests DrlGeneratorServiceTest

# 22 tests covering:
✅ Single/multiple rule generation
✅ All rule types
✅ Custom packages
✅ Special characters
✅ Disabled rules
✅ Null/empty values
✅ Priority handling
✅ DRL structure
```

## Common Patterns

### 1. Generate and Save

```java
public void generateAndSave(List<RuleModel> rules, String filename) {
    String drl = drlGenerator.generateDrl(rules);
    Files.writeString(Paths.get(filename), drl);
}
```

### 2. Generate by Priority

```java
// Sort by priority first
rules.sort(Comparator.comparing(RuleModel::getPriority).reversed());
String drl = drlGenerator.generateDrl(rules);
```

### 3. Generate Only Enabled

```java
List<RuleModel> enabled = rules.stream()
    .filter(RuleModel::isEnabled)
    .collect(Collectors.toList());

String drl = drlGenerator.generateDrl(enabled);
```

### 4. Multiple Packages

```java
String validationDrl = drlGenerator.generateDrl(
    validationRules,
    "com.example.validation",
    false
);

String businessDrl = drlGenerator.generateDrl(
    businessRules,
    "com.example.business",
    false
);
```

## Workflow

```
RuleModel(s)
    ↓
DrlGeneratorService.generateDrl()
    ↓
DRL String
    ↓
Save to .drl file
    ↓
Deploy to Kogito/Drools
```

## Best Practices

1. **Use meaningful IDs**: `validate-email` not `rule-1`
2. **Set priorities**: Group related rules by priority
3. **Add descriptions**: Help future maintainers
4. **Group by package**: Separate concerns (validation, business, etc.)
5. **Enable/disable**: Control which rules are active

## Limitations

- Simplified WHEN clause (`eval(true)`)
- Basic THEN clause (println only)
- No complex fact patterns
- No rule templates
- No advanced metadata

## Error Handling

```java
try {
    String drl = drlGenerator.generateDrl(rules, "com.example", true);
} catch (IllegalArgumentException e) {
    // No rules provided
} catch (DrlValidationException e) {
    // DRL syntax errors
    e.getErrors().forEach(System.err::println);
}
```

## Files

- **Service**: `DrlGeneratorService.java`
- **Exception**: `DrlValidationException.java`
- **Tests**: `DrlGeneratorServiceTest.java`
- **Docs**: `DRL_GENERATOR.md`

## Related Services

- `RuleDetectionService` - Detects rules from text
- `ProcessReasonerService` - Creates rules from process description
- `BpmnGeneratorService` - Generates BPMN (complementary)

## Quick Reference

```java
// Minimal example
RuleModel rule = new RuleModel();
rule.setId("my-rule");
rule.setRuleType("CONDITIONAL");
rule.setPriority(50);
rule.setEnabled(true);

DrlGeneratorService gen = new DrlGeneratorService();
String drl = gen.generateDrl(rule);
System.out.println(drl);
```

---

For detailed documentation, see [DRL_GENERATOR.md](./DRL_GENERATOR.md)

