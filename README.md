# xDS Control Plane

Dynamic xDS Control Plane for Envoy proxy that allows adding new executors and controllers without restarting Envoy.

## Features

- Dynamic configuration updates via xDS API
- HTTP management API for runtime configuration changes
- Support for adding clusters and endpoints dynamically
- Configuration file watching for automatic updates
- **JWT-based authentication and authorization**
- Role-based access control (RBAC)
- Secure token-based API access

## Quick Start

```bash
# Build
mvn clean package

# Run with authentication enabled (default)
mvn exec:java -Dexec.mainClass="com.one211.xds.XdsControlPlaneApplication"

# Run with custom ports
mvn exec:java -Dexec.mainClass="com.one211.xds.XdsControlPlaneApplication" \
    -Dexec.args="-Dgrpc.port=18000 -Dhttp.port=18001"

# Run with authentication disabled (development only)
export DISABLE_AUTH=true
mvn exec:java -Dexec.mainClass="com.one211.xds.XdsControlPlaneApplication"
```

### Quick Authentication Test

```bash
# 1. Get a JWT token
TOKEN=$(curl -s -X POST http://localhost:18001/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.token')

# 2. Use the token to access protected endpoints
curl -X POST http://localhost:18001/api/clusters \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "cluster_name": "test-cluster",
    "endpoints": ["localhost:8080"]
  }'

# 3. Test health endpoint (no auth required)
curl http://localhost:18001/health
```

## HTTP API

| Endpoint | Method | Authentication | Required Permission | Description |
|----------|--------|----------------|-------------------|-------------|
| `/health` | GET | No | - | Health check |
| `/login` | POST | No | - | Authenticate and get JWT token |
| `/reload` | POST | Yes | `reload` | Reload configuration from files |
| `/api/clusters` | POST | Yes | `add-cluster` | Add a new cluster |
| `/api/endpoints` | POST | Yes | `update-endpoints` | Update cluster endpoints |

## Authentication

### Overview

The xDS Control Plane supports JWT-based authentication and role-based authorization for secure API access. All protected endpoints require a valid JWT token in the `Authorization` header and the appropriate role permissions.

### Default Users

| Username | Password | Role |
|----------|----------|------|
| `admin` | `admin123` | ADMIN |
| `operator` | `operator123` | OPERATOR |
| `viewer` | `viewer123` | VIEWER |
| `service-account` | `service-secret` | SERVICE |

> **⚠️ Security Warning:** The default credentials are for development only. Change them in production!

### Role Permissions

| Role | Permissions | Description |
|------|-------------|-------------|
| ADMIN | reload, add-cluster, update-endpoints, delete-cluster, manage-users, view-logs | Full administrative access |
| OPERATOR | reload, add-cluster, update-endpoints, view-logs | Operational access (no user management) |
| VIEWER | view-logs, health-check | Read-only access |
| SERVICE | add-cluster, update-endpoints | Service account for automated tasks |

### Getting a JWT Token

To authenticate and get a JWT token:

```bash
curl -X POST http://localhost:18001/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

**Response:**

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "username": "admin",
  "role": "ADMIN",
  "expiresIn": 86400000
}
```

### Using the JWT Token

Include the JWT token in the `Authorization` header with the `Bearer` prefix:

```bash
curl -X POST http://localhost:18001/api/clusters \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "cluster_name": "my-service",
    "endpoints": ["192.168.1.10:8080", "192.168.1.11:8080"]
  }'
```

### Configuration

#### Configuration File

The xDS Control Plane can be configured via `src/main/resources/application.conf`:

**Server Configuration:**
- gRPC port: `server.grpc.port` (default: 18000)
- HTTP management port: `server.http.port` (default: 18001)

**xDS Configuration:**
- Gateway ports: `xds.https-gateway-port`, `xds.minio-s3-api-port`
- Arrow Flight SQL ports: `xds.flight-ports.*`
- PostgreSQL ports: `xds.postgres-ports.*`
- Service ports: `xds.services.*`
- Timeouts: `xds.timeouts.*` (api-short, api-standard, api-long, tcp-connect, tcp-idle)
- Scheduler: `xds.scheduler.*` (initial-delay-sec, interval-sec)

**Authentication Configuration:**
- JWT secret key, expiration, issuer, audience
- User credentials and roles (ADMIN, OPERATOR, VIEWER, SERVICE)
- Role permissions configuration

#### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `JWT_SECRET_KEY` | Secret key for JWT signing (should be 256 bits) | `your-256-bit-secret-key...` |
| `DEFAULT_ADMIN_PASSWORD` | Password for default admin user | `admin123` |
| `DISABLE_AUTH` | Disable authentication (true/false) | `false` |

#### Set Custom JWT Secret

```bash
export JWT_SECRET_KEY="your-very-secure-256-bit-secret-key-here-change-this-in-production"
export DEFAULT_ADMIN_PASSWORD="your-secure-password-here"

mvn exec:java -Dexec.mainClass="com.one211.xds.XdsControlPlaneApplication"
```

#### Disable Authentication (Development Only)

```bash
export DISABLE_AUTH=true

mvn exec:java -Dexec.mainClass="com.one211.xds.XdsControlPlaneApplication"
```

### Token Expiration

- **Default expiration:** 24 hours (86400000 milliseconds)
- Tokens are signed using HMAC-SHA256
- Expired tokens will be rejected with a 401 Unauthorized response

### Adding Custom Users

Currently, users are managed through the `UserStore` class. In production, integrate with:
- Database (PostgreSQL, MySQL, etc.)
- LDAP/Active Directory
- OAuth2 providers (GitHub, Google, etc.)
- Identity providers (Auth0, Keycloak, etc.)

### Security Best Practices

1. **Never commit JWT secrets** to version control
2. **Use environment variables** for sensitive configuration
3. **Rotate secrets** regularly in production
4. **Use HTTPS/TLS** in production environments
5. **Implement rate limiting** on the login endpoint
6. **Use strong passwords** for all users
7. **Monitor authentication logs** for suspicious activity
8. **Consider implementing** refresh tokens for better security

## Ports

| Service | Port |
|---------|------|
| gRPC (xDS API) | 18000 |
| HTTP Management API | 18001 |

## Running Tests

```bash
mvn test
```

Tests automatically start the application, verify it's ready, run integration tests, and stop it.

## Docker

```bash
# Build image
docker build -t one211/xds-control-plane .

# Run with default authentication
docker run -p 18000:18000 -p 18001:18001 one211/xds-control-plane

# Run with custom JWT secret and admin password
docker run -p 18000:18000 -p 18001:18001 \
  -e JWT_SECRET_KEY="your-secure-256-bit-secret-key" \
  -e DEFAULT_ADMIN_PASSWORD="your-secure-password" \
  one211/xds-control-plane

# Run with authentication disabled (development only)
docker run -p 18000:18000 -p 18001:18001 \
  -e DISABLE_AUTH=true \
  one211/xds-control-plane
```

## Dependencies

- Envoy Control Plane Java API v1.0.49
- gRPC v1.79.0
- JJWT (Java JWT) v0.12.5 - JWT token generation and validation
- BouncyCastle v1.78 - Cryptography library
- JUnit 5.10.1
- Mockito 5.11.0

## License

[Add your license here]

## Authentication Examples

### Example 1: Using curl with JWT token

```bash
# Login and save token
export XDS_TOKEN=$(curl -s -X POST http://localhost:18001/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.token')

# Use token in subsequent requests
curl -X POST http://localhost:18001/api/clusters \
  -H "Authorization: Bearer $XDS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "cluster_name": "backend-service",
    "endpoints": ["10.0.0.1:8080", "10.0.0.2:8080"]
  }'
```

### Example 2: Using Python requests

```python
import requests

# Login and get token
response = requests.post('http://localhost:18001/login',
    json={'username': 'admin', 'password': 'admin123'})
token = response.json()['token']

# Use token in protected requests
headers = {'Authorization': f'Bearer {token}'}
response = requests.post('http://localhost:18001/api/clusters',
    headers=headers,
    json={
        'cluster_name': 'frontend-service',
        'endpoints': ['10.0.0.10:8080', '10.0.0.11:8080']
    })
print(response.json())
```

### Example 3: Using JavaScript fetch

```javascript
// Login and get token
async function login() {
  const response = await fetch('http://localhost:18001/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      username: 'admin',
      password: 'admin123'
    })
  });
  return await response.json();
}

// Use token in protected requests
async function addCluster(token) {
  const response = await fetch('http://localhost:18001/api/clusters', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      cluster_name: 'api-service',
      endpoints: ['10.0.0.20:8080', '10.0.0.21:8080']
    })
  });
  return await response.json();
}

// Usage
login().then(data => {
  addCluster(data.token).then(result => console.log(result));
});
```

### Common Error Messages

| Error | Cause | Solution |
|-------|-------|----------|
| `401 Unauthorized` | Missing or invalid token | Provide valid JWT in Authorization header |
| `Invalid JWT signature` | Wrong JWT secret key | Ensure JWT_SECRET_KEY matches |
| `JWT token is expired` | Token expired | Get a new token via /login |
| `Invalid username or password` | Wrong credentials | Check username/password combination |
| `Authentication failed` | General auth failure | Check logs for specific error |
