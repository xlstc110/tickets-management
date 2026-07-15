# OAuth2 Server JWT Payload Claims Reference

## Overview
The JWT tokens returned by this OAuth2 authorization server come in two types:
1. **Access Token** - Used for accessing protected resources
2. **ID Token** (OIDC) - Used for user identity verification

---

## 1️⃣ Standard JWT Claims (Present in Both Token Types)

### Base Claims

| Claim | Field Name | Possible Values/Description | Example |
|-------|--------|-----------|------|
| **Issuer** | `iss` | Address of the Authorization Server | `http://localhost:8080` |
| **Subject** | `sub` | User's UUID (stable identifier) | `550e8400-e29b-41d4-a716-446655440000` |
| **Audience** | `aud` | OAuth2 Client ID | `["client"]` |
| **Issued At** | `iat` | Unix timestamp when token was issued | `1689234567` |
| **Expiration** | `exp` | Unix timestamp when token expires | `1689238167` |
| **JWT ID** | `jti` | Unique Token ID (UUID) | `a1b2c3d4-e5f6-47a8-9b0c-1d2e3f4a5b6c` |
| **Not Before** | `nbf` | Unix timestamp when token becomes valid (Access Token only) | `1689234567` |

---

## 2️⃣ Access Token Exclusive Claims

### Scopes and Permissions Information

| Claim | Field Name | Possible Values/Description | Example |
|-------|--------|-----------|------|
| **Scopes** | `scope` | Authorized permission scopes (array or space-separated string) | `["openid","profile","email"]` |
| **Authorities** | `authorities` | User's permission list, comma-separated | `"ADMIN,user:read,user:write,ticket:read,ticket:write"` |

### Possible Scope Values
- `openid` - OpenID Connect foundation
- `profile` - User profile information
- `email` - User email address

### Possible Authorities Values
Composed of the following parts (comma-separated):
- **Role**: User role (e.g., `ADMIN`, `USER`, `MANAGER`)
- **Permissions**: Fine-grained permissions (e.g., `user:read`, `user:write`, `ticket:read`, `ticket:write`)

Actual values come from `role` and `authorities` fields in the `User` table.

---

## 3️⃣ ID Token Exclusive Claims (OIDC)

### OpenID Connect Specific Claims

| Claim | Field Name | Possible Values/Description | Example |
|-------|--------|-----------|------|
| **Authorized Party** | `azp` | The authorized party (Client ID) | `client` |
| **Nonce** | `nonce` | One-time value passed from the authorization request (optional) | `abc123def456` |
| **Session ID** | `sid` | User session ID | `session-uuid-12345` |
| **Authentication Time** | `auth_time` | Unix timestamp when user completed authentication | `1689234500` |

### Explanation
- `azp`: Determined by the registered client's `clientId`
- `nonce`: Only present if the `nonce` parameter was included in the authorization request
- `sid` and `auth_time`: Only present in the authorization code flow; preserved when refreshing tokens
- When re-issuing an ID Token using a Refresh Token, `sid` and `auth_time` are copied from the old token to the new one

---

## 4️⃣ Token Lifecycle (TTL)

### Configured Expiration Times

| Token Type | TTL | Remarks |
|----------|-----|------|
| **Access Token** | 1 hour | Used for API access; short TTL for enhanced security |
| **ID Token** | 30 minutes | Identity verification token; short TTL |
| **Refresh Token** | 30 days | Used to obtain new Access Tokens and ID Tokens |

### Calculating Expiration Time
```
exp = iat + TTL (in seconds)
Example: iat=1689234567, TTL=3600s => exp=1689238167
```

---

## 5️⃣ Complete JWT Payload Examples

### Access Token Payload
```json
{
  "iss": "http://localhost:8080",
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "aud": ["client"],
  "iat": 1689234567,
  "exp": 1689238167,
  "nbf": 1689234567,
  "jti": "a1b2c3d4-e5f6-47a8-9b0c-1d2e3f4a5b6c",
  "scope": ["openid", "profile", "email"],
  "authorities": "ADMIN,user:read,user:write,ticket:read,ticket:write"
}
```

### ID Token Payload
```json
{
  "iss": "http://localhost:8080",
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "aud": ["client"],
  "iat": 1689234567,
  "exp": 1689235367,
  "jti": "x9y8z7w6-v5u4-t3s2-r1q0-p9o8n7m6l5k4",
  "azp": "client",
  "nonce": "abc123def456",
  "sid": "session-uuid-12345",
  "auth_time": 1689234500
}
```

---

## 6️⃣ JWT Header Information

### JWS Header Structure
```json
{
  "alg": "RS256",
  "typ": "JWT",
  "kid": "key-id"
}
```

### Explanation
- **alg**: Signature algorithm (RS256 = RSA with SHA-256)
- **typ**: Token type (fixed as JWT)
- **kid**: Key ID used to identify the signing key

---

## 7️⃣ Key Technical Details

### Subject (Subject) Selection
- Uses the user's **stable UUID** (`userUuid`) instead of the username
- Advantage: Token remains valid even if the username changes
- Source: `User.userUuid` field

### Authorities Construction
```
authorities = user.getRole() + "," + user.getAuthorities()
Example: "ADMIN,user:read,user:write"
```
- Role: User's primary role
- Authorities: Fine-grained permission list

### Token Generation Flow
1. Generate standard claims (iss, sub, aud, iat, exp, etc.)
2. Add token-type-specific claims based on token type
3. Customize claims via `OAuth2TokenCustomizer` (adds authorities)
4. Sign using RS256 algorithm (performed by JwtEncoder)

---

## 8️⃣ User Information Sources

### Retrieved from User Table
| Field | Usage in JWT |
|------|-----------|
| `userUuid` | `sub` (Subject) |
| `role` | Prefix of `authorities` |
| `authorities` | Permissions portion of `authorities` |

### Authentication Process
```
1. User logs in (email + password)
2. UserAuthenticationProvider validates the user
3. Load User from database, extract role and authorities
4. Create Authentication object containing user permissions
5. During JWT generation, extract authorities from Authentication and add to token
```

---

## 9️⃣ Important Considerations

### Security Considerations
- ✅ Short Access Token TTL (1 hour) reduces breach risk
- ✅ RS256 asymmetric signing ensures integrity
- ✅ Subject uses UUID instead of username for stability
- ⚠️ Refresh Token TTL is longer (30 days), requires secure storage

### Claims Modification
- Access Token's `authorities` claim can be customized via `OAuth2TokenCustomizer`
- Standard claims are managed by the Spring Authorization Server framework
- ID Token's claims are constrained by the OIDC specification

### Token Validation
- Use public key to verify RS256 signature
- Check if `exp` timestamp has passed (expiration)
- Verify `aud` contains the current application's client ID
- Verify `iss` is from a trusted authorization server
