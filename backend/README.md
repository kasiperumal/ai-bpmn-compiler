# AI-BPMN Compiler - Backend

Spring Boot 3.x application with Drools for business rules management.

## Project Details

- **Name**: ai-bpmn-compiler
- **Package**: com.example.aibpmn
- **Java Version**: 17
- **Spring Boot**: 3.2.1
- **Spring Framework**: 6.1.2
- **Drools**: 8.44.0.Final (Jakarta EE compatible)
- **Build Tool**: Gradle (Groovy DSL)
- **Port**: 8080

## Technology Stack

### Core Framework
- Spring Boot 3.2.1 (Spring MVC)
- Spring Framework 6.1.2
- Java 17
- Tomcat 10.1.17 (Jakarta EE)

### Business Rules Engine
- **Drools 8.44.0.Final** - Business Rules Management System (BRMS)
  - `drools-core` - Core rule engine
  - `drools-compiler` - Rule compilation
  - `drools-mvel` - MVEL expression language
  - `drools-decisiontables` - Decision table support
  - Jakarta EE 9+ compatible (`jakarta.*` packages)

## Project Structure

```
backend/
├── build.gradle              # Gradle build configuration
├── settings.gradle           # Project settings
├── gradlew                   # Gradle wrapper (Unix)
├── gradlew.bat              # Gradle wrapper (Windows)
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── example/
        │           └── aibpmn/
        │               └── AiBpmnCompilerApplication.java
        └── resources/
            ├── application.yml
            ├── processes/         # Place BPMN files here
            └── rules/             # Place DRL rule files here
```

## Building the Project

### Build
```bash
./gradlew build
```

### Run
```bash
./gradlew bootRun
```

Or run the JAR directly:
```bash
java -jar build/libs/ai-bpmn-compiler-0.0.1-SNAPSHOT.jar
```

### Clean Build
```bash
./gradlew clean build
```

## Configuration

### application.yml
```yaml
spring:
  application:
    name: ai-bpmn-compiler

server:
  port: 8080

logging:
  level:
    root: INFO
    com.example.aibpmn: DEBUG
    org.drools: DEBUG
    org.kie: DEBUG
```

## Adding BPMN Support

This project currently includes Drools for business rules. To add BPMN workflow support with Spring Boot 3.x, consider:

1. **Camunda 7.x** - Community edition BPMN engine
   - Add `camunda-bpm-spring-boot-starter-webapp` dependency
   - Provides BPMN 2.0, DMN, CMMN support
   - Includes web UI for process modeling

2. **Flowable** - Open source BPMN engine
   - Add `flowable-spring-boot-starter` dependency
   - BPMN 2.0, CMMN, DMN support
   - REST API and UI included

3. **Camunda 8** (Cloud-native)
   - Zeebe workflow engine
   - Cloud-first architecture
   - Event-driven processes

## Adding Business Rules

1. Create a DRL file (e.g., `my-rules.drl`)
2. Place it in `src/main/resources/rules/`

Example DRL:
```drools
package com.example.aibpmn.rules

rule "Example Rule"
    when
        // conditions
    then
        // actions
end
```

## API Endpoints

After implementing services, you can create REST endpoints like:

- `POST /api/processes/{processId}/start` - Start a process
- `POST /api/rules/evaluate` - Evaluate rules
- `GET /api/processes/{instanceId}` - Get process instance

## Features

✅ Spring Boot 3.2.1 (Latest, Jakarta EE)
✅ Drools 8.44.0.Final Business Rules Engine
✅ Spring Web MVC
✅ Java 17
✅ No Security (as requested)
✅ No Database (as requested)
✅ No Observability tools (as requested)

## What Changed from 2.7.x to 3.2.1

### Upgraded ✨
- **Spring Boot**: 2.7.18 → 3.2.1
- **Spring Framework**: 5.3.31 → 6.1.2
- **Tomcat**: 9.0.83 → 10.1.17
- **Drools**: 7.74.1.Final → 8.44.0.Final
- **Jakarta EE**: `javax.*` → `jakarta.*` packages

### Removed 🗑️
- **jBPM dependencies** - Not available in Maven Central for Spring Boot 3.x
- For BPMN support, integrate Camunda or Flowable instead

## Next Steps

1. **Create Services**: Implement services to work with Drools and jBPM
2. **Add REST Controllers**: Create endpoints for process and rule management
3. **Define BPMN Processes**: Create BPMN2 files for your workflows
4. **Write Rules**: Create DRL files for business logic
5. **Add Tests**: Write unit and integration tests

## Example Usage

### Drools Service Example

```java
@Service
public class RulesService {
    
    private KieContainer kieContainer;
    
    @PostConstruct
    public void init() {
        KieServices kieServices = KieServices.Factory.get();
        kieContainer = kieServices.getKieClasspathContainer();
    }
    
    public void executeRules(Object... facts) {
        KieSession kSession = kieContainer.newKieSession();
        
        for (Object fact : facts) {
            kSession.insert(fact);
        }
        
        kSession.fireAllRules();
        kSession.dispose();
    }
}
```

### REST Controller Example

```java
@RestController
@RequestMapping("/api/rules")
public class RulesController {
    
    @Autowired
    private RulesService rulesService;
    
    @PostMapping("/evaluate")
    public ResponseEntity<?> evaluate(@RequestBody MyFactObject fact) {
        rulesService.executeRules(fact);
        return ResponseEntity.ok(fact);
    }
}
```

## Resources

- [Drools Documentation](https://docs.drools.org/)
- [Spring Boot 3.2.1 Reference](https://docs.spring.io/spring-boot/docs/3.2.1/reference/html/)
- [Spring Framework 6.1.x](https://docs.spring.io/spring-framework/reference/6.1/)
- [Jakarta EE 9+](https://jakarta.ee/specifications/)
- [Camunda BPMN](https://docs.camunda.org/)
- [Flowable BPMN](https://www.flowable.com/open-source)

## Troubleshooting

### Port Already in Use
If you see "Port 8080 is already in use", either:
1. Stop the process using port 8080
2. Change the port in `application.yml`:
```yaml
server:
  port: 8081
```

### Build Failures
- Ensure Java 17 is installed: `java -version`
- Clear Gradle cache: `./gradlew clean`
- Delete `.gradle` folder and rebuild

## License

Copyright © 2026


