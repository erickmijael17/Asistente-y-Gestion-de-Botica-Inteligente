# Botica Amanecer — Spring Boot Scaffold Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create Spring Boot base project with Java 22 + PostgreSQL, clean git branches, rename remote, and push to main.

**Architecture:** Layered (Controller → Service → Repository → Entity) with CORS config and global exception handling.

**Tech Stack:** Java 22, Spring Boot 3.x, Maven, Spring Web, Spring Data JPA, PostgreSQL, Lombok, Validation

## Global Constraints

- Java 22 target
- PostgreSQL driver (not MySQL)
- Remote name: `Asistente y Gestion de Botica Inteligente`
- Package: `com.botica.amanecer`
- Only main branch on remote
- Spring Boot 3.x (3.3+ for Java 22 support)

---

### Task 1: Git cleanup — delete remote branch, rename remote

**Files:**
- Modify: git config (via commands)

- [ ] **Step 1: Delete remote branch `proyecto`**

```bash
git push origin --delete proyecto
```

- [ ] **Step 2: Rename remote `origin` → `Asistente y Gestion de Botica Inteligente`**

```bash
git remote rename origin "Asistente y Gestion de Botica Inteligente"
```

- [ ] **Step 3: Verify changes**

```bash
git remote -v
```
Expected output:
```
Asistente y Gestion de Botica Inteligente  https://github.com/erickmijael17/BoticaAmanecer.git (fetch)
Asistente y Gestion de Botica Inteligente  https://github.com/erickmijael17/BoticaAmanecer.git (push)
```

---

### Task 2: Create pom.xml

**Files:**
- Create: `pom.xml`

- [ ] **Step 1: Create pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.5</version>
        <relativePath/>
    </parent>

    <groupId>com.botica</groupId>
    <artifactId>amanecer</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>BoticaAmanecer</name>
    <description>Asistente y Gestion de Botica Inteligente</description>

    <properties>
        <java.version>22</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

---

### Task 3: Create application structure — main class, config, and resources

**Files:**
- Create: `src/main/java/com/botica/amanecer/BoticaAmanecerApplication.java`
- Create: `src/main/resources/application.yml`
- Create: `src/test/java/com/botica/amanecer/BoticaAmanecerApplicationTests.java`

- [ ] **Step 1: Create main application class**

```bash
New-Item -ItemType Directory -Path "src\main\java\com\botica\amanecer" -Force
New-Item -ItemType Directory -Path "src\test\java\com\botica\amanecer" -Force
New-Item -ItemType Directory -Path "src\main\resources" -Force
```

- [ ] **Step 2: Create BoticaAmanecerApplication.java**

```java
package com.botica.amanecer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BoticaAmanecerApplication {

    public static void main(String[] args) {
        SpringApplication.run(BoticaAmanecerApplication.class, args);
    }
}
```

- [ ] **Step 3: Create application.yml**

```yaml
server:
  port: 8080

spring:
  application:
    name: BoticaAmanecer
  datasource:
    url: jdbc:postgresql://localhost:5432/botica_amanecer
    username: postgres
    password: ${DB_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
```

- [ ] **Step 4: Create BoticaAmanecerApplicationTests.java**

```java
package com.botica.amanecer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BoticaAmanecerApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

---

### Task 4: Create configuration layer

**Files:**
- Create: `src/main/java/com/botica/amanecer/config/CorsConfig.java`

- [ ] **Step 1: Create config package**

```bash
New-Item -ItemType Directory -Path "src\main\java\com\botica\amanecer\config" -Force
```

- [ ] **Step 2: Create CorsConfig.java**

```java
package com.botica.amanecer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH")
                        .allowedHeaders("*");
            }
        };
    }
}
```

---

### Task 5: Create API layer — HealthController

**Files:**
- Create: `src/main/java/com/botica/amanecer/controller/HealthController.java`

- [ ] **Step 1: Create controller package**

```bash
New-Item -ItemType Directory -Path "src\main\java\com\botica\amanecer\controller" -Force
```

- [ ] **Step 2: Create HealthController.java**

```java
package com.botica.amanecer.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}
```

---

### Task 6: Create exception layer

**Files:**
- Create: `src/main/java/com/botica/amanecer/exception/GlobalExceptionHandler.java`

- [ ] **Step 1: Create exception package**

```bash
New-Item -ItemType Directory -Path "src\main\java\com\botica\amanecer\exception" -Force
```

- [ ] **Step 2: Create GlobalExceptionHandler.java**

```java
package com.botica.amanecer.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "Not Found",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Internal Server Error",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}
```

- [ ] **Step 3: Create ResourceNotFoundException.java**

```java
package com.botica.amanecer.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
```

---

### Task 7: Create model, service, and repository directories

**Files:**
- Create: directories only (marker files or empty)

- [ ] **Step 1: Create directories**

```bash
New-Item -ItemType Directory -Path "src\main\java\com\botica\amanecer\model\entity" -Force
New-Item -ItemType Directory -Path "src\main\java\com\botica\amanecer\model\dto" -Force
New-Item -ItemType Directory -Path "src\main\java\com\botica\amanecer\service" -Force
New-Item -ItemType Directory -Path "src\main\java\com\botica\amanecer\repository" -Force
```

---

### Task 8: Update .gitignore for Java/Spring Boot

**Files:**
- Modify: `.gitignore`

- [ ] **Step 1: Write .gitignore**

```
# Compiled class file
*.class

# Log file
*.log

# Package files
*.jar
*.war
*.nar
*.ear
*.zip
*.tar.gz
*.rar

# Maven
target/

# IDE
.idea/
*.iml
*.iws
*.ipr
.vscode/
.settings/
.project
.classpath
bin/

# OS
.DS_Store
Thumbs.db

# Environment
.env
application-local.yml
```

---

### Task 9: Verify build

- [ ] **Step 1: Run Maven compile**

```bash
mvn compile -q
```
Expected: BUILD SUCCESS (no output with -q flag)

- [ ] **Step 2: Run tests**

```bash
mvn test -q
```
Expected: Tests run: 1, Failures: 0, Errors: 0

---

### Task 10: Commit and push

- [ ] **Step 1: Stage and commit**

```bash
git add -A
git commit -m "feat: scaffold Spring Boot project with Java 22 + PostgreSQL"
```

- [ ] **Step 2: Push to main**

```bash
git push "Asistente y Gestion de Botica Inteligente" main
```
