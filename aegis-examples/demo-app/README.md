# Aegis Demo Application

A comprehensive demonstration application showcasing all authentication mechanisms supported by the Aegis Authentication SDK.

## 🚀 Quick Start

### Prerequisites

- Java 21 or later
- Maven 3.8+
- Internet connection (for downloading dependencies)

### Run the Application

```bash
# From the demo-app directory
mvn spring-boot:run

# Or from the project root
cd Aegis-
mvn clean package
java -jar aegis-examples/demo-app/target/demo-app-1.0.0-SNAPSHOT.jar
```

### Access the UI

Open your browser and navigate to:
```
http://localhost:8080
```

## 📚 Features

This demo application demonstrates:

- ✅ **JWT Authentication** - Token-based authentication with login/logout
- ✅ **API Key Authentication** - Header and query parameter support
- ✅ **Basic Authentication** - HTTP Basic Auth with Base64 encoding
- ✅ **Role-Based Access Control** - USER, ADMIN, MANAGER roles
- ✅ **Permission-Based Access** - Fine-grained permissions
- ✅ **Interactive Web UI** - Beautiful, responsive interface
- ✅ **Real-time Testing** - Test all endpoints directly from the UI

## 🔑 Test Credentials

### Users (for JWT and Basic Auth)

| Username | Password  | Roles          | Description |
|----------|-----------|----------------|-------------|
| demo     | password  | USER           | Regular user |
| alice    | alice123  | USER, MANAGER  | Manager account |
| bob      | bob123    | USER           | Regular user |
| admin    | admin123  | USER, ADMIN    | Administrator |

### API Keys

| API Key                    | User        | Roles          |
|----------------------------|-------------|----------------|
| demo-key-alice-12345       | alice       | USER           |
| demo-key-admin-67890       | admin       | USER, ADMIN    |
| demo-key-service-abc123    | service-bot | SERVICE        |

## 🌐 API Endpoints

### Public Endpoints

- `GET /` - Home page (Web UI)
- `GET /api/public/hello` - Public API endpoint
- `GET /api/health` - Health check
- `POST /api/auth/login` - Login endpoint
- `GET /api/auth/test-credentials` - Get test credentials

### Protected Endpoints

All require authentication:

- `GET /api/me` - Get current user info
- `GET /api/protected/data` - Get protected data
- `GET /api/test/auth-method` - Test authentication method

### Role-Based Endpoints

- `GET /api/user/dashboard` - Requires USER role
- `GET /api/manager/reports` - Requires MANAGER role
- `GET /api/admin/users` - Requires ADMIN role

### Permission-Based Endpoints

- `POST /api/data` - Requires `write:data` permission
- `DELETE /api/data/{id}` - Requires `delete:data` permission

## 🧪 Testing Guide

### 1. JWT Authentication

**Via Web UI:**
1. Go to "JWT Authentication" tab
2. Use quick-fill or enter credentials
3. Click "Login & Get Token"
4. Use "Test Token" to verify

**Via cURL:**
```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","password":"password"}'

# Use the token
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/api/me
```

### 2. API Key Authentication

**Via Web UI:**
1. Go to "API Key" tab
2. Enter or quick-fill an API key
3. Select location (header or query)
4. Click "Test API Key"

**Via cURL (Header):**
```bash
curl -H "X-API-Key: demo-key-alice-12345" \
  http://localhost:8080/api/me
```

**Via cURL (Query Parameter):**
```bash
curl "http://localhost:8080/api/me?api_key=demo-key-alice-12345"
```

### 3. Basic Authentication

**Via Web UI:**
1. Go to "Basic Auth" tab
2. Enter credentials
3. Click "Test Basic Auth"

**Via cURL:**
```bash
curl -u demo:password http://localhost:8080/api/me
```

### 4. Test All Endpoints

**Via Web UI:**
1. Login with JWT or use an API key
2. Go to "Test Endpoints" tab
3. Click on any endpoint card to test

**Via cURL:**
```bash
# Test public endpoint (no auth)
curl http://localhost:8080/api/public/hello

# Test protected endpoint (with JWT)
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/api/protected/data

# Test admin endpoint (requires ADMIN role)
curl -H "Authorization: Bearer ADMIN_TOKEN" \
  http://localhost:8080/api/admin/users
```

## 🎨 UI Features

The web UI provides:

- **Tabbed Interface** - Easy navigation between auth methods
- **Quick Fill Buttons** - One-click credential filling
- **Real-time Responses** - See API responses immediately
- **Color-Coded Results** - Success (green) and error (red) indicators
- **Token Persistence** - Tokens stored in memory for testing
- **Responsive Design** - Works on desktop and mobile
- **cURL Examples** - Copy-paste ready commands

## 🔧 Configuration

The application is configured in `application.yml`:

```yaml
aegis:
  jwt:
    enabled: true
    secret-key: demo-secret-key...
    issuer: aegis-demo
    audience: aegis-demo-api

  apikey:
    enabled: true
    header-name: X-API-Key
    allow-header: true
    allow-query-parameter: true

  basic:
    enabled: true

  public-paths:
    - /
    - /login
    - /api/auth/**
    - /api/public/**
```

## 📖 How It Works

### Authentication Flow

1. **JWT:**
   - User submits credentials to `/api/auth/login`
   - Backend validates and generates JWT token
   - Frontend stores token and includes in subsequent requests
   - Backend validates token on each protected request

2. **API Key:**
   - API key sent in header (`X-API-Key`) or query parameter
   - Backend looks up key in store
   - Returns associated user principal
   - Access granted based on user's roles/permissions

3. **Basic Auth:**
   - Credentials sent with each request (Base64 encoded)
   - Backend validates against credential store
   - Access granted if credentials valid

### Architecture

```
┌─────────────┐
│   Browser   │
│   (Web UI)  │
└──────┬──────┘
       │
       ↓ HTTP Requests
┌─────────────────────────────────┐
│   AegisSecurityFilter           │
│   (Extracts Auth Context)       │
└──────┬──────────────────────────┘
       │
       ↓
┌─────────────────────────────────┐
│   AuthenticationManager          │
│   (Routes to Provider)          │
└──────┬──────────────────────────┘
       │
       ↓
┌──────────────────────────────────┐
│  JwtAuthenticationProvider       │
│  ApiKeyAuthenticationProvider    │
│  BasicAuthenticationProvider     │
└──────┬───────────────────────────┘
       │
       ↓
┌──────────────────────────────────┐
│   Spring Security Context        │
│   (UserPrincipal set)           │
└──────┬───────────────────────────┘
       │
       ↓
┌──────────────────────────────────┐
│   REST Controllers               │
│   (Business Logic)              │
└──────────────────────────────────┘
```

## 🎯 Use Cases Demonstrated

1. **Single Sign-On (SSO)** - JWT tokens for web applications
2. **Service-to-Service** - API keys for backend services
3. **Legacy Systems** - Basic Auth for older systems
4. **Role-Based Access** - Different permissions per role
5. **Multi-Tenant** - Same app, different auth methods
6. **Mobile Apps** - JWT tokens for mobile clients
7. **Third-Party Integrations** - API keys for partners

## 🐛 Troubleshooting

### Port Already in Use

If port 8080 is already in use:

```bash
# Change port in application.yml
server:
  port: 8081

# Or use command line
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

### CORS Issues

If testing from a different origin, CORS is configured to allow all origins in development mode.

### Token Expired

JWT tokens expire after 1 hour (default). Login again to get a new token.

### 403 Forbidden

Check that:
1. You're authenticated (token/API key is valid)
2. Your user has the required role
3. The endpoint path is correct

## 📝 Development Tips

### Adding New Users

Edit `DemoCredentialsStore.java`:

```java
storeCredentials("newuser", "password", createNewUser());
```

### Adding New API Keys

Edit `TestDataInitializer.java`:

```java
apiKeyStore.storeApiKey("new-api-key", userPrincipal);
```

### Customizing JWT Expiration

Edit `application.yml`:

```yaml
aegis:
  jwt:
    token-expiration: PT2H  # 2 hours
```

### Adding New Endpoints

Create a new method in `ApiController.java`:

```java
@GetMapping("/api/custom")
@PreAuthorize("hasRole('CUSTOM_ROLE')")
public ResponseEntity<?> customEndpoint(Authentication auth) {
    // Your logic
}
```

## 🚀 Production Considerations

This is a **DEMO APPLICATION**. For production:

- [ ] Use proper password hashing (BCrypt, Argon2)
- [ ] Store API keys in a database, not in memory
- [ ] Use strong JWT secret keys (256+ bits)
- [ ] Enable HTTPS/TLS
- [ ] Implement rate limiting
- [ ] Add proper logging and monitoring
- [ ] Use environment variables for secrets
- [ ] Implement token refresh mechanism
- [ ] Add CSRF protection for session-based auth
- [ ] Use a persistent session store
- [ ] Implement account lockout after failed attempts
- [ ] Add audit logging

## 📚 Further Reading

- [Aegis SDK Documentation](../../README.md)
- [Quick Start Guide](../../QUICKSTART.md)
- [Testing Guide](../../TESTING.md)
- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [JWT.io](https://jwt.io)

## 🤝 Contributing

Found a bug or want to add a feature? Contributions welcome!

## 📄 License

Apache License 2.0

---

**Built with ❤️ using Aegis Authentication SDK**
