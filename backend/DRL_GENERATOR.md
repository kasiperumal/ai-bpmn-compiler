# DRL Generator Service

## Overview

The `DrlGeneratorService` converts `RuleModel` objects to Drools Rule Language (DRL) format. It generates executable business rules that can be deployed to the Drools/Kogito runtime.

## Features

### Core Capabilities

1. **Rule Generation**
   - Converts single or multiple RuleModels to DRL
   - Generates valid Drools syntax
   - Supports custom package names
   - Handles rule priorities (salience)

2. **Rule Type Support**
   - CONDITIONAL - If-then logic
   - THRESHOLD - Numeric limits
   - COMPARISON - Value comparisons
   - APPROVAL - Authorization rules
   - VALIDATION - Data validation
   - CALCULATION - Mathematical operations

3. **DRL Features**
   - Package declarations
   - Import statements
   - Rule metadata (descriptions as comments)
   - Salience (priority) configuration
   - Enabled/disabled rule handling

4. **Optional Validation**
   - Drools compiler integration
   - Syntax validation
   - Error reporting

## DRL Structure

### Generated DRL Format

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

## Usage

### Basic Single Rule Generation

```java
@Autowired
private DrlGeneratorService drlGenerator;

public String generateRuleDrl(RuleModel rule) {
    String drl = drlGenerator.generateDrl(rule);
    return drl;
}
```

### Multiple Rules Generation

```java
public String generateAllRules(List<RuleModel> rules) {
    String drl = drlGenerator.generateDrl(rules);
    return drl;
}
```

### Custom Package

```java
public String generateWithCustomPackage(List<RuleModel> rules) {
    String drl = drlGenerator.generateDrl(
        rules, 
        "com.mycompany.rules",
        false  // skip validation
    );
    return drl;
}
```

### With Validation

```java
public String generateAndValidate(List<RuleModel> rules) {
    try {
        String drl = drlGenerator.generateDrl(
            rules,
            "com.example.rules",
            true  // enable validation
        );
        return drl;
    } catch (DrlValidationException e) {
        logger.error("DRL validation failed: {}", e.getErrorSummary());
        throw e;
    }
}
```

## Examples

### Example 1: Conditional Rule

**Input**:
```java
RuleModel rule = new RuleModel();
rule.setId("high-value-order");
rule.setExpression("amount > 1000");
rule.setDescription("High value order check");
rule.setRuleType("CONDITIONAL");
rule.setPriority(50);
rule.setEnabled(true);
```

**Output DRL**:
```drl
package com.example.aibpmn.rules;

import java.util.*;
import java.time.*;

rule "high-value-order"
    salience 50
    // High value order check
when
    eval(true)
then
    System.out.println("Rule fired: high-value-order");
end
```

### Example 2: Threshold Rule

**Input**:
```java
RuleModel rule = new RuleModel();
rule.setId("threshold-check");
rule.setExpression("value >= 5000");
rule.setDescription("Minimum threshold");
rule.setRuleType("THRESHOLD");
rule.setPriority(60);
rule.setEnabled(true);
```

**Output DRL**:
```drl
package com.example.aibpmn.rules;

import java.util.*;
import java.time.*;

rule "threshold-check"
    salience 60
    // Minimum threshold
when
    eval(true)
then
    System.out.println("Rule fired: threshold-check");
end
```

### Example 3: Multiple Rules

**Input**:
```java
List<RuleModel> rules = new ArrayList<>();

RuleModel rule1 = new RuleModel();
rule1.setId("rule-1");
rule1.setExpression("check1");
rule1.setRuleType("CONDITIONAL");
rule1.setPriority(50);
rule1.setEnabled(true);
rules.add(rule1);

RuleModel rule2 = new RuleModel();
rule2.setId("rule-2");
rule2.setExpression("check2");
rule2.setRuleType("THRESHOLD");
rule2.setPriority(60);
rule2.setEnabled(true);
rules.add(rule2);

String drl = drlGenerator.generateDrl(rules);
```

**Output DRL**:
```drl
package com.example.aibpmn.rules;

import java.util.*;
import java.time.*;

rule "rule-1"
    salience 50
when
    eval(true)
then
    System.out.println("Rule fired: rule-1");
end

rule "rule-2"
    salience 60
when
    eval(true)
then
    System.out.println("Rule fired: rule-2");
end
```

## Rule Priority (Salience)

DRL uses `salience` to define rule priority. Higher values execute first:

| Rule Type | Default Priority | When It Fires |
|-----------|------------------|---------------|
| APPROVAL | 70 | Last (final checks) |
| VALIDATION | 65 | Before approval |
| THRESHOLD | 60 | Enforce limits |
| COMPARISON | 55 | After conditionals |
| CONDITIONAL | 50 | Middle priority |
| CALCULATION | 40 | First (compute values) |

**Example**:
```drl
rule "high-priority"
    salience 70
when
    eval(true)
then
    // Executes before lower priority rules
end

rule "low-priority"
    salience 40
when
    eval(true)
then
    // Executes after higher priority rules
end
```

## Special Handling

### Disabled Rules

Disabled rules are skipped during generation:

```java
RuleModel rule = new RuleModel();
rule.setId("disabled-rule");
rule.setEnabled(false);  // This rule won't be in DRL

String drl = drlGenerator.generateDrl(rule);
// DRL will not contain this rule
```

### Special Characters

Special characters in rule IDs and descriptions are escaped:

```java
RuleModel rule = new RuleModel();
rule.setId("rule-with-\"quotes\"");
rule.setDescription("Description with\nnewlines");

// Generated DRL properly escapes these
```

### Null/Empty Expressions

```java
RuleModel rule = new RuleModel();
rule.setId("no-expression");
rule.setExpression(null);  // or ""

// Generated: when eval(true)
```

### No Priority

```java
RuleModel rule = new RuleModel();
rule.setPriority(null);  // No salience in DRL

// Generated rule won't have salience line
```

## Integration

### With RuleDetectionService

```java
@Autowired
private RuleDetectionService ruleDetector;

@Autowired
private DrlGeneratorService drlGenerator;

public String processTextToRules(String description) {
    // Detect rules from text
    List<DetectedRule> detected = ruleDetector.detectRules(description);
    
    // Extract RuleModels
    List<RuleModel> rules = detected.stream()
        .map(DetectedRule::getRule)
        .collect(Collectors.toList());
    
    // Generate DRL
    String drl = drlGenerator.generateDrl(rules);
    
    return drl;
}
```

### With ProcessReasonerService

```java
@Autowired
private ProcessReasonerService reasoner;

@Autowired
private DrlGeneratorService drlGenerator;

public String generateRulesFromProcess(String description) {
    // Reason over description
    ReasoningResult result = reasoner.reasonOverDescription(description);
    
    // Generate DRL from rules
    String drl = drlGenerator.generateDrl(result.getRules());
    
    return drl;
}
```

### Save DRL to File

```java
public void saveDrlToFile(List<RuleModel> rules, String filePath) {
    String drl = drlGenerator.generateDrl(rules);
    
    try {
        Files.writeString(
            Paths.get(filePath),
            drl,
            StandardCharsets.UTF_8
        );
        logger.info("DRL saved to: {}", filePath);
    } catch (IOException e) {
        logger.error("Failed to save DRL", e);
        throw new RuntimeException("Failed to save DRL", e);
    }
}
```

### Deploy to Kogito

```java
public void deployRulesToKogito(List<RuleModel> rules) {
    // Generate DRL
    String drl = drlGenerator.generateDrl(rules, "com.example.rules", true);
    
    // Save to Kogito resources directory
    Path targetPath = Paths.get(
        "src/main/resources/com/example/rules/generated-rules.drl"
    );
    
    Files.writeString(targetPath, drl);
    
    // Kogito will auto-compile and deploy
}
```

## Testing

### Test Coverage

The `DrlGeneratorServiceTest` provides comprehensive coverage:

1. **Basic Generation** (3 tests)
   - Single rule
   - Multiple rules
   - Custom package

2. **Rule Types** (6 tests)
   - Conditional
   - Threshold
   - Comparison
   - Approval
   - Validation
   - Calculation

3. **Special Cases** (8 tests)
   - Disabled rules
   - Null/empty expressions
   - No priority
   - Zero priority
   - Special characters
   - Complex rules

4. **DRL Structure** (2 tests)
   - Includes imports
   - Includes println

5. **Error Handling** (2 tests)
   - Null rules list
   - Empty rules list

### Running Tests

```bash
# Run DRL generator tests
./gradlew test --tests DrlGeneratorServiceTest

# All 22 tests should pass
```

## Best Practices

1. **Group Rules by Purpose**
   ```java
   List<RuleModel> validationRules = ...;
   List<RuleModel> businessRules = ...;
   
   String validationDrl = drlGenerator.generateDrl(
       validationRules, 
       "com.example.validation"
   );
   
   String businessDrl = drlGenerator.generateDrl(
       businessRules,
       "com.example.business"
   );
   ```

2. **Use Meaningful Rule IDs**
   ```java
   rule.setId("validate-customer-email");  // Good
   rule.setId("rule-1");  // Bad
   ```

3. **Set Appropriate Priorities**
   ```java
   calculationRule.setPriority(40);  // Execute first
   validationRule.setPriority(65);   // Validate after calculations
   approvalRule.setPriority(70);     // Final approval
   ```

4. **Add Descriptions**
   ```java
   rule.setDescription("Validates that email format is correct");
   // Appears as comment in DRL
   ```

5. **Enable/Disable Strategically**
   ```java
   // Temporarily disable a rule
   rule.setEnabled(false);
   
   // It won't be in generated DRL
   ```

## Limitations

1. **Simplified WHEN Clause** - Currently generates `eval(true)` for all rules
2. **Basic THEN Clause** - Only prints rule execution
3. **No Fact Patterns** - Doesn't generate complex fact-based conditions
4. **No Rule Metadata** - Limited to comments for descriptions
5. **No Rule Templates** - Each rule is self-contained

## Future Enhancements

1. **Smart Expression Parsing** - Convert expressions to proper fact patterns
2. **Rich THEN Actions** - Generate actual business logic
3. **Rule Templates** - Support for rule templates
4. **Extended Metadata** - @tags, @author, @version
5. **Rule Groups** - Agenda groups and ruleflow-groups
6. **Advanced Conditions** - Complex boolean expressions
7. **Integration Testing** - Test DRL execution in Drools/Kogito

## Error Handling

### DrlValidationException

Thrown when DRL validation fails:

```java
try {
    String drl = drlGenerator.generateDrl(rules, "com.example", true);
} catch (DrlValidationException e) {
    System.err.println("Validation failed:");
    for (String error : e.getErrors()) {
        System.err.println("  - " + error);
    }
}
```

### Common Errors

| Error | Cause | Fix |
|-------|-------|-----|
| `Rules list cannot be null or empty` | No rules provided | Provide at least one rule |
| `DRL validation failed` | Invalid DRL syntax | Check generated DRL |

## Related Services

- **`RuleDetectionService`** - Detects rules from text (input for this service)
- **`ProcessReasonerService`** - Creates rules during process inference
- **`BpmnGeneratorService`** - Generates BPMN (complementary to DRL)
- **`BpmnValidationService`** - Validates generated artifacts

## Files

- **Service**: `DrlGeneratorService.java`
- **Exception**: `DrlValidationException.java`
- **Tests**: `DrlGeneratorServiceTest.java` (22 test cases)
- **Docs**: `DRL_GENERATOR.md`

## References

- [Drools Documentation](https://docs.jboss.org/drools/release/latest-final/drools-docs/html_single/)
- [Drools Rule Language](https://docs.jboss.org/drools/release/latest-final/drools-docs/html_single/#_droolslanguagereferencechapter)
- [Kogito Rules](https://docs.jboss.org/kogito/release/latest/html_single/#con-kogito-dmn-drl_kogito-developing-decision-services)

