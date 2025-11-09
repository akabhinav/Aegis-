# Aegis Quick Start Guide

Get up and running with Aegis in 5 minutes!

## Prerequisites

- Java 21 or later
- Maven 3.8+
- Your favorite IDE

## Step 1: Create a Spring Boot Project

```bash
curl https://start.spring.io/starter.zip \
  -d dependencies=web \
  -d javaVersion=21 \
  -d type=maven-project \
  -o aegis-demo.zip

unzip aegis-demo.zip
cd aegis-demo
```

## Step 2: Add Aegis Dependency

Add to `pom.xml`:

```xml
<dependency>
    <groupId>io.aegis</groupId>
    <artifactId>aegis-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Step 3: Configure Aegis

Create `src/main/resources/application.yml`:

```yaml
server:
  port: 8080

aegis:
  jwt:
    enabled: true
    secret-key: my-super-secret-key-change-in-production
    issuer: aegis-demo
    audience: aegis-api

  public-paths:
    - /api/auth/**
    - /health

logging:
  level:
    io.aegis: DEBUG
```

## Step 4: Create a Login Controller

Create `src/main/java/com/example/demo/AuthController.java`:

```java
package com.example.demo;

import io.aegis.core.user.DefaultUserPrincipal;
import io.aegis.core.user.UserPrincipal;
import io.aegis.jwt.JwtGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private JwtGenerator jwtGenerator;

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody LoginRequest request) {
        // In production, validate credentials against a database
        if ("demo".equals(request.username()) && "password".equals(request.password())) {

            UserPrincipal principal = DefaultUserPrincipal.builder()
                .id("1")
                .username(request.username())
                .email("demo@example.com")
                .addRole("USER")
                .addRole("ADMIN")
                .build();

            String token = jwtGenerator.generateToken(principal);

            return Map.of(
                "token", token,
                "type", "Bearer"
            );
        }

        throw new RuntimeException("Invalid credentials");
    }

    record LoginRequest(String username, String password) {}
}
```

## Step 5: Create a Protected Controller

Create `src/main/java/com/example/demo/ApiController.java`:

```java
package com.example.demo;

import io.aegis.core.user.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    @GetMapping("/me")
    public Map<String, Object> getCurrentUser(Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();

        return Map.of(
            "id", user.getId(),
            "username", user.getUsername(),
            "email", user.getEmail(),
            "roles", user.getRoles()
        );
    }

    @GetMapping("/protected")
    public Map<String, String> protectedEndpoint(Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();

        return Map.of(
            "message", "Hello, " + user.getUsername() + "!",
            "timestamp", String.valueOf(System.currentTimeMillis())
        );
    }
}
```

## Step 6: Run the Application

```bash
mvn spring-boot:run
```

## Step 7: Test It Out

### 1. Login to get a token

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","password":"password"}'
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer"
}
```

### 2. Access protected endpoint with the token

```bash
TOKEN="<your-token-from-step-1>"

curl http://localhost:8080/api/me \
  -H "Authorization: Bearer $TOKEN"
```

Response:
```json
{
  "id": "1",
  "username": "demo",
  "email": "demo@example.com",
  "roles": ["USER", "ADMIN"]
}
```

### 3. Access another protected endpoint

```bash
curl http://localhost:8080/api/protected \
  -H "Authorization: Bearer $TOKEN"
```

Response:
```json
{
  "message": "Hello, demo!",
  "timestamp": "1234567890"
}
```

### 4. Try accessing without a token (should fail)

```bash
curl http://localhost:8080/api/protected
```

Response: `403 Forbidden`

## Next Steps

### Add API Key Authentication

Update `application.yml`:

```yaml
aegis:
  jwt:
    enabled: true
    secret-key: my-super-secret-key

  apikey:
    enabled: true
    header-name: X-API-Key
    allow-header: true
```

Create an API key service:

```java
@Service
public class ApiKeyService {

    @Autowired
    private ApiKeyStore apiKeyStore;

    @PostConstruct
    public void init() {
        // Create a demo API key
        UserPrincipal apiUser = DefaultUserPrincipal.builder()
            .id("api-1")
            .username("api-user")
            .addRole("API")
            .build();

        apiKeyStore.storeApiKey("demo-api-key-123", apiUser);
    }
}
```

Test with API key:

```bash
curl http://localhost:8080/api/protected \
  -H "X-API-Key: demo-api-key-123"
```

### Add Role-Based Access Control

```java
@GetMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public Map<String, String> adminOnly() {
    return Map.of("message", "Admin access granted");
}

@GetMapping("/user")
@PreAuthorize("hasRole('USER')")
public Map<String, String> userAccess() {
    return Map.of("message", "User access granted");
}
```

### Add Custom Claims to JWT

```java
Map<String, Object> customClaims = Map.of(
    "department", "Engineering",
    "level", "Senior"
);

String token = jwtGenerator.generateToken(principal, customClaims);
```

## Production Checklist

- [ ] Use strong secret keys (minimum 256 bits)
- [ ] Store secrets in environment variables or secure vault
- [ ] Enable HTTPS
- [ ] Implement token refresh mechanism
- [ ] Add rate limiting
- [ ] Enable audit logging
- [ ] Set up monitoring and alerting
- [ ] Use a persistent store for API keys
- [ ] Implement password hashing (BCrypt)
- [ ] Add input validation
- [ ] Enable CORS properly
- [ ] Set appropriate token expiration times

## Troubleshooting

### Authentication not working?

1. Check that the JWT secret is set correctly
2. Verify the token hasn't expired
3. Ensure the Authorization header is formatted correctly: `Bearer <token>`
4. Check application logs for detailed error messages

### Getting 403 Forbidden?

1. Verify the endpoint is not in the public paths list
2. Check that authentication is successful
3. Verify user has required roles for the endpoint

### Token generation fails?

1. Ensure JWT module is enabled in configuration
2. Check that secret key is provided
3. Verify JwtGenerator bean is properly autowired

## Examples Repository

For more examples, check out the `aegis-examples` module:
- JWT authentication example
- OAuth 2.0 integration
- API key management
- Multi-provider setup
- Custom authentication provider

## Need Help?

- Read the [full documentation](README.md)
- Check [GitHub Issues](https://github.com/akabhinav/Aegis/issues)
- Ask on Stack Overflow with tag `aegis-auth`

Happy coding with Aegis! 🛡️
