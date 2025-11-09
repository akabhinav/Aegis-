# Aegis Authentication SDK

[![Java 21](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.java.net/projects/jdk/21/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

A production-grade, enterprise-ready authentication SDK for Java 21 applications with seamless Spring Boot integration.

## Features

### Multiple Authentication Mechanisms
- **JWT (JSON Web Tokens)** - Stateless token-based authentication
- **OAuth 2.0 / OpenID Connect** - Industry-standard federated authentication
- **API Keys** - Simple and efficient API authentication
- **Basic Authentication** - HTTP Basic Auth support
- **mTLS** - Mutual TLS certificate authentication
- **SAML 2.0** - Enterprise SSO integration
- **Session-Based** - Traditional cookie/session authentication
- **Custom** - Extensible for proprietary mechanisms

### Developer Experience
- **Zero Boilerplate** - Smart defaults with optional customization
- **Spring Boot Auto-Configuration** - Just add the dependency and configure
- **Type Safety** - Leverages Java 21 features (Records, Sealed Interfaces, Pattern Matching)
- **Pluggable Architecture** - Mix and match authentication mechanisms
- **Production-Ready** - Built for enterprise use cases

## Quick Start

### Maven Dependency

```xml
<dependency>
    <groupId>io.aegis</groupId>
    <artifactId>aegis-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Minimal Configuration

Add to your `application.yml`:

```yaml
aegis:
  jwt:
    enabled: true
    secret-key: your-secret-key-here
    issuer: your-app
    audience: your-audience

  public-paths:
    - /public/**
    - /health
    - /api/auth/login
```

That's it! Your Spring Boot application now has JWT authentication enabled.

## Architecture

### Core Components

```
aegis-core              - Core abstractions and interfaces
aegis-jwt               - JWT authentication
aegis-oauth2            - OAuth 2.0 / OIDC
aegis-apikey            - API Key authentication
aegis-basic             - Basic authentication
aegis-mtls              - Mutual TLS
aegis-saml              - SAML 2.0
aegis-session           - Session-based auth
aegis-spring-boot-starter - Spring Boot auto-configuration
```

### Authentication Flow

```
HTTP Request
    ↓
AegisSecurityFilter (extracts context)
    ↓
AuthenticationManager (routes to appropriate provider)
    ↓
AuthenticationProvider (validates credentials)
    ↓
UserPrincipal (authenticated user)
    ↓
Spring Security Context
```

## Usage Examples

### 1. JWT Authentication

#### Configuration

```yaml
aegis:
  jwt:
    enabled: true
    secret-key: ${JWT_SECRET_KEY}
    issuer: my-application
    audience: my-api
```

#### Generating Tokens

```java
@Service
public class AuthService {

    @Autowired
    private JwtGenerator jwtGenerator;

    public String login(String username, String password) {
        // Validate credentials...

        UserPrincipal principal = DefaultUserPrincipal.builder()
            .id(user.getId())
            .username(username)
            .email(user.getEmail())
            .addRole("USER")
            .build();

        return jwtGenerator.generateToken(principal);
    }
}
```

#### Authenticating Requests

```bash
curl -H "Authorization: Bearer <token>" http://localhost:8080/api/protected
```

### 2. API Key Authentication

#### Configuration

```yaml
aegis:
  apikey:
    enabled: true
    header-name: X-API-Key
    allow-header: true
    allow-query-parameter: false
```

#### Registering API Keys

```java
@Service
public class ApiKeyService {

    @Autowired
    private ApiKeyStore apiKeyStore;

    public void createApiKey(String key, UserPrincipal user) {
        apiKeyStore.storeApiKey(key, user);
    }
}
```

#### Using API Keys

```bash
curl -H "X-API-Key: your-api-key" http://localhost:8080/api/data
```

### 3. OAuth 2.0 / OpenID Connect

#### Configuration

```yaml
aegis:
  oauth2:
    enabled: true
    client-id: ${OAUTH_CLIENT_ID}
    client-secret: ${OAUTH_CLIENT_SECRET}
```

#### Usage

```bash
curl -H "Authorization: Bearer <oauth-access-token>" \
     http://localhost:8080/api/user
```

### 4. Basic Authentication

#### Configuration

```yaml
aegis:
  basic:
    enabled: true
```

#### Custom Credentials Store

```java
@Component
public class DatabaseCredentialsStore implements CredentialsStore {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public Optional<UserPrincipal> validateCredentials(
            String username, String password) {

        return userRepository.findByUsername(username)
            .filter(user -> passwordEncoder.matches(password, user.getPassword()))
            .map(this::toPrincipal);
    }

    private UserPrincipal toPrincipal(User user) {
        return DefaultUserPrincipal.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .roles(user.getRoles())
            .build();
    }
}
```

### 5. Multiple Authentication Mechanisms

Aegis supports multiple authentication mechanisms simultaneously:

```yaml
aegis:
  jwt:
    enabled: true
    secret-key: ${JWT_SECRET}

  apikey:
    enabled: true
    header-name: X-API-Key

  basic:
    enabled: true

  public-paths:
    - /public/**
    - /health
```

Requests will be authenticated using the first matching provider:
1. JWT (if Bearer token present)
2. API Key (if X-API-Key header present)
3. Basic Auth (if Basic auth header present)

## Advanced Usage

### Custom Authentication Provider

```java
@Component
public class CustomAuthProvider implements AuthenticationProvider<CustomContext> {

    @Override
    public AuthenticationResult authenticate(CustomContext context) {
        // Your custom authentication logic
        Object credentials = context.getCredentials();

        // Validate credentials...

        UserPrincipal principal = // ... create principal

        return AuthenticationResult.success(principal);
    }

    @Override
    public boolean supports(AuthenticationContext context) {
        return context instanceof CustomContext;
    }

    @Override
    public int getPriority() {
        return 50; // Lower priority than built-in providers
    }
}
```

### Accessing Current User

```java
@RestController
public class UserController {

    @GetMapping("/api/me")
    public UserPrincipal getCurrentUser(Authentication authentication) {
        return (UserPrincipal) authentication.getPrincipal();
    }
}
```

### Role-Based Access Control

```java
@RestController
public class AdminController {

    @GetMapping("/api/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminEndpoint() {
        return "Admin access granted";
    }
}
```

### Programmatic Authentication

```java
@Service
public class CustomService {

    @Autowired
    private AuthenticationManager authenticationManager;

    public void authenticateCustomRequest(String token) {
        JwtContext context = new JwtContext(token);
        AuthenticationResult result = authenticationManager.authenticate(context);

        if (result.isSuccess()) {
            UserPrincipal user = result.getPrincipal().get();
            // Use authenticated user...
        }
    }
}
```

## Configuration Reference

### Complete Configuration Example

```yaml
aegis:
  # JWT Configuration
  jwt:
    enabled: true
    secret-key: ${JWT_SECRET_KEY}
    issuer: my-application
    audience: my-api

  # OAuth 2.0 Configuration
  oauth2:
    enabled: true
    client-id: ${OAUTH_CLIENT_ID}
    client-secret: ${OAUTH_CLIENT_SECRET}

  # API Key Configuration
  apikey:
    enabled: true
    header-name: X-API-Key
    allow-header: true
    allow-query-parameter: false

  # Basic Authentication
  basic:
    enabled: true

  # Public paths (no authentication required)
  public-paths:
    - /public/**
    - /health
    - /actuator/health
    - /api/auth/login
    - /api/auth/register
```

## Java 21 Features

Aegis leverages modern Java 21 features for better developer experience:

### Records for Immutable Data

```java
public record JwtContext(
    String token,
    String source,
    Map<String, Object> attributes
) implements AuthenticationContext { }
```

### Sealed Interfaces for Type Safety

```java
public sealed interface AuthenticationContext permits
    JwtContext,
    OAuth2Context,
    ApiKeyContext,
    BasicAuthContext,
    MtlsContext,
    SamlContext,
    SessionContext,
    CustomContext { }
```

### Pattern Matching

```java
AuthenticationResult result = authenticationManager.authenticate(context);

String message = switch (result) {
    case AuthenticationResult.Success(var principal, _, _) ->
        "Welcome, " + principal.getUsername();
    case AuthenticationResult.Failure(var reason, _, _, _) ->
        "Authentication failed: " + reason;
};
```

## Testing

### Unit Testing with Mock Providers

```java
@Test
void testAuthentication() {
    // Create mock credentials store
    CredentialsStore store = mock(CredentialsStore.class);
    when(store.validateCredentials("user", "pass"))
        .thenReturn(Optional.of(createTestPrincipal()));

    // Create provider
    BasicAuthenticationProvider provider = new BasicAuthenticationProvider(store);

    // Test authentication
    BasicAuthContext context = new BasicAuthContext("user", "pass");
    AuthenticationResult result = provider.authenticate(context);

    assertTrue(result.isSuccess());
}
```

### Integration Testing

```java
@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testJwtAuthentication() throws Exception {
        String token = generateTestToken();

        mockMvc.perform(get("/api/protected")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }
}
```

## Performance

- **Zero-Copy Operations** - Efficient token parsing
- **Caching** - Optional caching for token validation
- **Virtual Threads** - Ready for Java 21 virtual threads
- **Non-Blocking** - Designed for reactive applications

## Security Best Practices

1. **Secrets Management** - Never hardcode secrets, use environment variables
2. **HTTPS Only** - Always use HTTPS in production
3. **Token Rotation** - Implement token refresh mechanisms
4. **Rate Limiting** - Add rate limiting for authentication endpoints
5. **Audit Logging** - Log all authentication attempts

## Migration Guide

### From Spring Security OAuth

```java
// Before (Spring Security OAuth)
@EnableAuthorizationServer
public class AuthServerConfig extends AuthorizationServerConfigurerAdapter {
    // Lots of boilerplate...
}

// After (Aegis)
// Just add configuration in application.yml
aegis:
  jwt:
    enabled: true
    secret-key: ${JWT_SECRET}
```

## Contributing

Contributions are welcome! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for details.

## License

Apache License 2.0 - see [LICENSE](LICENSE) for details.

## Support

- GitHub Issues: [Report a bug](https://github.com/akabhinav/Aegis/issues)
- Documentation: [Full Documentation](https://docs.aegis.io)
- Stack Overflow: Tag `aegis-auth`

## Roadmap

- [ ] WebAuthn / FIDO2 support
- [ ] Passwordless authentication
- [ ] Multi-factor authentication (MFA)
- [ ] Admin UI for key management
- [ ] Metrics and monitoring integration
- [ ] Kotlin DSL support

## Acknowledgments

Built with:
- Nimbus JOSE + JWT
- Spring Security
- OpenSAML
- Bouncy Castle

---

Made with ❤️ for the Java community
