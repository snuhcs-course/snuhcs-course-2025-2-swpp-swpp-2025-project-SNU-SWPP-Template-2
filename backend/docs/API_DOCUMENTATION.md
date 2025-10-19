# 📚 Book Bartering Social Network - API Documentation

## 🚀 Overview

This document provides comprehensive API documentation for the Book Bartering Social Network backend. The API is built with Django REST Framework and provides authentication, user management, and social features.

**Base URL**: `http://10.0.2.2:8000/` (Android Emulator)
**Base URL**: `http://127.0.0.1:8000/` (Local Development)

> **📖 URL Conventions**: This project uses semantic, direct paths (e.g., `/auth/`, `/library/`) instead of `/api/v1/` prefixes.
> For detailed URL conventions and guidelines, see [`API_CONVENTIONS.md`](./API_CONVENTIONS.md).

## 📋 Table of Contents

- [Authentication Endpoints](#authentication-endpoints)
- [Library Endpoints](#library-endpoints)
- [Frontend Integration](#frontend-integration)
- [Testing](#testing)
- [URL Conventions](#url-conventions)

## 🔐 Authentication Endpoints

All authentication endpoints are prefixed with `/auth/`

### 1. User Registration

**Endpoint**: `POST /auth/signup/`

**Request Body**:
```json
{
    "username": "string",
    "email": "string",
    "password": "string"
}
```

**Response (Success - 201)**:
```json
{
    "ok": true,
    "message": "Registration successful",
    "user": {
        "id": 1,
        "username": "testuser",
        "email": "test@example.com",
        "first_name": "",
        "last_name": "",
        "bio": "",
        "location": "",
        "birth_date": null,
        "profile_picture": null,
        "phone_number": "",
        "is_profile_public": true,
        "allow_direct_messages": true,
        "reputation_score": 0,
        "successful_trades": 0,
        "follower_count": 0,
        "following_count": 0,
        "books_count": 0,
        "created_at": "2025-09-29T13:42:25.380555Z",
        "last_active": "2025-09-29T13:42:25.380583Z"
    }
}
```

**Response (Error - 400)**:
```json
{
    "ok": false,
    "message": "Registration failed",
    "errors": {
        "username": ["Username already exists"],
        "email": ["Email already exists"]
    }
}
```

### 2. User Login

**Endpoint**: `POST /auth/login/`

**Request Body**:
```json
{
    "username": "testuser",  // Can be username OR email
    "password": "testpass123"
}
```

**Response (Success - 200)**:
```json
{
    "ok": true,
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refresh": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
        "id": 1,
        "username": "testuser",
        "email": "test@example.com",
        // ... other user fields
    },
    "message": "Login successful"
}
```

**Response (Error - 400)**:
```json
{
    "ok": false,
    "message": "Invalid credentials"
}
```

### 3. Token Refresh

**Endpoint**: `POST /auth/token/refresh/`

**Request Body**:
```json
{
    "refresh": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response (Success - 200)**:
```json
{
    "access": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### 4. Password Reset (Start)

**Endpoint**: `POST /auth/forgot/start/`

**Request Body**:
```json
{
    "email": "test@example.com"
}
```

**Response (Success - 200)**:
```json
{
    "requestId": "uuid-string",
    "code": "123456",  // Only in DEBUG mode
    "message": "Reset code sent to email"
}
```

### 5. Password Reset (Verify)

**Endpoint**: `POST /auth/forgot/verify/`

**Request Body**:
```json
{
    "request_id": "uuid-string",
    "code": "123456"
}
```

**Response (Success - 200)**:
```json
{
    "ok": true,
    "message": "Code verified successfully"
}
```

### 6. Password Reset (Confirm)

**Endpoint**: `POST /auth/forgot/reset/`

**Request Body**:
```json
{
    "password": "newpassword123"
}
```

**Response (Success - 200)**:
```json
{
    "ok": true,
    "message": "Password reset successful"
}
```

### 7. Social Authentication

**Endpoint**: `POST /auth/social/`

**Request Body**:
```json
{
    "provider": "google",  // "google", "facebook", "kakao"
    "access_token": "provider-access-token"
}
```

**Response (Success - 200)**:
```json
{
    "ok": true,
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refresh": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
        // User object
    },
    "message": "Social authentication successful"
}
```

## 👤 User Profile Endpoints

### 8. Get User Profile

**Endpoint**: `GET /auth/profile/`  
**Authentication**: Required (JWT Token)

**Headers**:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response (Success - 200)**:
```json
{
    "id": 1,
    "username": "testuser",
    "email": "test@example.com",
    // ... full user profile
}
```

### 9. Update User Profile

**Endpoint**: `PUT /auth/profile/update/`  
**Authentication**: Required (JWT Token)

**Request Body**:
```json
{
    "first_name": "John",
    "last_name": "Doe",
    "bio": "Book lover and trader",
    "location": "Seoul, Korea",
    "is_profile_public": true
}
```

**Response (Success - 200)**:
```json
{
    "ok": true,
    "user": {
        // Updated user object
    },
    "message": "Profile updated successfully"
}
```

## 🔧 Frontend Integration

### Android/Kotlin Integration

1. **Base URL Configuration**:
```kotlin
// In RetrofitClient.kt
private const val BASE_URL = "http://10.0.2.2:8000/"
```

2. **API Interface**:
```kotlin
interface AuthApi {
    @POST("auth/login/")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @POST("auth/signup/")
    suspend fun signUp(@Body body: SignUpRequest): Response<SignUpResponse>

    @POST("auth/social/")
    suspend fun socialAuth(@Body body: SocialAuthRequest): Response<LoginResponse>

    @POST("auth/forgot/start/")
    suspend fun forgotPassword(@Body body: ForgotPasswordRequest): Response<ForgotPasswordResponse>
}
```

3. **Data Classes**:
```kotlin
data class LoginRequest(
    val username: String,  // Can be username or email
    val password: String
)

data class LoginResponse(
    val ok: Boolean,
    val token: String?,
    val refresh: String?,
    val user: User?,
    val message: String?
)

data class SignUpRequest(
    val username: String,
    val password: String,
    val email: String
)

data class SignUpResponse(
    val ok: Boolean,
    val message: String?,
    val user: User?
)
```

### 🔑 JWT Token Usage

1. **Store tokens securely**:
```kotlin
// Store in SharedPreferences or encrypted storage
val accessToken = response.body()?.token
val refreshToken = response.body()?.refresh
```

2. **Add to API requests**:
```kotlin
// Add Authorization header
@GET("api/protected-endpoint/")
suspend fun getProtectedData(
    @Header("Authorization") token: String
): Response<Data>

// Usage
val response = api.getProtectedData("Bearer $accessToken")
```

3. **Handle token refresh**:
```kotlin
// When access token expires (401 response)
val refreshResponse = api.refreshToken(RefreshRequest(refreshToken))
if (refreshResponse.isSuccessful) {
    val newAccessToken = refreshResponse.body()?.access
    // Retry original request with new token
}
```

## 🧪 Testing

Run the test script to verify all endpoints:

```bash
cd backend
python test_api.py
```

## 🌐 URL Conventions

### Important: MVP Approach - No `/api/v1/` Prefix

**For this MVP, we use semantic, direct paths** (e.g., `/auth/`, `/library/`) **instead of `/api/v1/` prefixes.**

> **📌 Note on Best Practices:**
> Using `/api/v1/` is generally considered a **better approach** for production APIs as it:
> - Provides clear API versioning from the start
> - Makes future breaking changes easier to manage
> - Follows industry-standard REST API conventions
> - Allows running multiple API versions simultaneously
>
> **However, for our MVP (Minimum Viable Product):**
> - We prioritize **simplicity and speed of development**
> - Shorter URLs are easier to work with during rapid iteration
> - We can migrate to `/api/v1/` in future iterations if needed
> - Current approach is sufficient for our initial release

#### Current URL Structure (MVP)

| Resource Group | Prefix | Purpose |
|---------------|--------|---------|
| Authentication | `/auth/` | User authentication and profile management |
| Library | `/library/` | User's library (books, reviews) |
| Social | `/social/` | Social features (future) |
| Barter | `/barter/` | Barter/trade features (future) |

#### Why We Chose This for MVP

1. **Simplicity**: Shorter, cleaner URLs for faster development
2. **Frontend Compatibility**: Frontend expects direct paths
3. **Consistency**: All endpoints follow the same pattern
4. **MVP Focus**: Prioritize features over infrastructure
5. **Future Migration Path**: Can add versioning later when needed

#### Adding New Endpoints

When adding new endpoints:
1. Check `backend/core/urls.py` for existing patterns
2. Use semantic resource groups (`/auth/`, `/library/`, etc.)
3. Follow RESTful principles (use HTTP methods, not verbs in URLs)
4. **Do NOT use `/api/v1/` prefix**
5. Document in this file and `API_CONVENTIONS.md`

For detailed guidelines, see [`API_CONVENTIONS.md`](./API_CONVENTIONS.md).

## 🚀 Production Deployment

1. Set environment variables in `.env`
2. Configure social auth credentials
3. Set up proper SSL certificates
4. Use production database (PostgreSQL)
5. Configure Redis for caching

## 📞 Support

For issues or questions, check the Django logs or contact the development team.

---

**Last Updated**: October 19, 2025
**API Version**: 1.0
**Django Version**: 5.2+
