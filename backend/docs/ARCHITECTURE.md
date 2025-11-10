# 🏗️ Book Bartering Social Network - Backend Architecture

## 📋 Overview

This document provides a comprehensive overview of the Django backend architecture for the Book Bartering Social Network. The system is designed as a modular, scalable social platform that enables users to discover, share, and exchange books with each other.

## 🎯 System Architecture

### High-Level Architecture

```mermaid
graph TB
    subgraph "Client Layer"
        A[Android App]
        B[Web Frontend]
        C[Admin Interface]
    end
    
    subgraph "API Gateway"
        D[Django REST Framework]
        E[Authentication Layer]
        F[CORS & Security]
    end
    
    subgraph "Application Layer"
        G[Accounts Service]
        H[Books Service]
        I[Social Service]
        J[Barter Service]
        K[Notification Service]
    end
    
    subgraph "Data Layer"
        L[(PostgreSQL w/ SQLite fallback)]
        M[Redis Cache]
        N[File Storage]
    end
    
    subgraph "External Services"
        O[Google OAuth]
        P[Facebook OAuth]
        Q[Email Service]
        R[Push Notifications]
    end
    
    A --> D
    B --> D
    C --> D
    
    D --> E
    D --> F
    
    E --> G
    D --> G
    D --> H
    D --> I
    D --> J
    D --> K
    
    G --> L
    H --> L
    I --> L
    J --> L
    K --> L
    
    G --> M
    H --> M
    I --> M
    
    H --> N
    G --> N
    
    G --> O
    G --> P
    K --> Q
    K --> R
```

## 📁 Project Structure

### Directory Layout

```
backend/
├── core/                    # Django project configuration
│   ├── settings.py         # Main settings file
│   ├── urls.py             # Root URL configuration
│   ├── wsgi.py             # WSGI application
│   ├── asgi.py             # ASGI application (WebSocket support)
│   └── routing.py          # WebSocket routing
├── accounts/               # User management & authentication
├── books/                  # Book catalog & management
├── social/                 # Social features & interactions
├── barter/                 # Barter system & transactions
├── notify/                 # Notification system
├── tests/                  # Integration tests
├── static/                 # Static files
├── logs/                   # Application logs
├── scripts/                # Utility scripts
└── requirements.txt        # Python dependencies
```

### Django Apps Architecture

```mermaid
graph LR
    subgraph "Django Project"
        subgraph "Core Configuration"
            A[core/settings.py]
            B[core/urls.py]
            C[core/wsgi.py]
            D[core/asgi.py]
        end
        
        subgraph "Application Apps"
            E[accounts/]
            F[books/]
            G[social/]
            H[barter/]
            I[notify/]
        end
        
        subgraph "Supporting"
            J[tests/]
            K[static/]
            L[logs/]
        end
    end
    
    A --> E
    A --> F
    A --> G
    A --> H
    A --> I
    
    B --> E
    B --> F
    B --> G
    B --> H
    B --> I
```

## 🗄️ Database Schema

### Entity Relationship Diagram

```mermaid
erDiagram
    User {
        uuid id PK
        string email UK
        string username
        string first_name
        string last_name
        text bio
        string location
        date birth_date
        string profile_picture
        string phone_number
        boolean is_profile_public
        boolean allow_direct_messages
        int reputation_score
        int successful_trades
        datetime created_at
        datetime updated_at
        datetime last_active
    }
    
    Follow {
        uuid id PK
        uuid follower_id FK
        uuid following_id FK
        datetime created_at
    }
    
    UserPreferences {
        uuid id PK
        uuid user_id FK
        boolean email_notifications
        boolean push_notifications
        boolean barter_request_notifications
        boolean message_notifications
        boolean follow_notifications
        boolean show_email
        boolean show_phone
        boolean show_location
        int max_barter_distance
        text preferred_meeting_locations
    }
    
    Book {
        uuid id PK
        string title
        text description
        string isbn_10
        string isbn_13
        date publication_date
        string edition
        int pages
        string language
        string cover_image
        uuid owner_id FK
        string condition
        string availability
        text owner_notes
        boolean is_for_barter
        decimal average_rating
        int review_count
        datetime created_at
        datetime updated_at
    }
    
    Author {
        uuid id PK
        string name
        text biography
        date birth_date
        date death_date
        string nationality
        string website
    }
    
    Publisher {
        uuid id PK
        string name
        text description
        string website
        string country
        datetime founded_date
    }
    
    Genre {
        uuid id PK
        string name
        text description
        string slug
    }
    
    BookReview {
        uuid id PK
        uuid book_id FK
        uuid reviewer_id FK
        int rating
        text review_text
        int helpful_votes
        datetime created_at
        datetime updated_at
    }
    
    Post {
        uuid id PK
        uuid author_id FK
        string post_type
        string title
        text content
        uuid related_book_id FK
        int likes_count
        int comments_count
        boolean is_public
        datetime created_at
        datetime updated_at
    }
    
    Comment {
        uuid id PK
        uuid post_id FK
        uuid author_id FK
        text content
        uuid parent_comment_id FK
        int likes_count
        datetime created_at
        datetime updated_at
    }
    
    Like {
        uuid id PK
        uuid user_id FK
        uuid post_id FK
        datetime created_at
    }
    
    BarterRequest {
        uuid id PK
        uuid requester_id FK
        uuid recipient_id FK
        text message
        string status
        string preferred_meeting_type
        text meeting_location
        datetime preferred_meeting_time
        text additional_notes
        datetime created_at
        datetime updated_at
        datetime expires_at
    }
    
    BarterTransaction {
        uuid id PK
        uuid barter_request_id FK
        uuid requester_id FK
        uuid recipient_id FK
        string status
        datetime meeting_time
        text meeting_location
        text completion_notes
        datetime completed_at
        datetime created_at
        datetime updated_at
    }
    
    Notification {
        uuid id PK
        uuid recipient_id FK
        string notification_type
        string title
        text message
        string priority
        boolean is_read
        datetime read_at
        boolean is_email_sent
        boolean is_push_sent
        datetime created_at
        datetime expires_at
    }
    
    User ||--o{ Follow : "follower"
    User ||--o{ Follow : "following"
    User ||--|| UserPreferences : "has"
    User ||--o{ Book : "owns"
    User ||--o{ BookReview : "writes"
    User ||--o{ Post : "creates"
    User ||--o{ Comment : "writes"
    User ||--o{ Like : "gives"
    User ||--o{ BarterRequest : "requests"
    User ||--o{ BarterRequest : "receives"
    User ||--o{ BarterTransaction : "participates"
    User ||--o{ Notification : "receives"
    
    Book ||--o{ BookReview : "has"
    Book }o--o{ Author : "written_by"
    Book }o--|| Publisher : "published_by"
    Book }o--o{ Genre : "categorized_as"
    Book ||--o{ Post : "featured_in"
    Book }o--o{ BarterRequest : "offered_books"
    Book }o--o{ BarterRequest : "requested_books"
    
    Post ||--o{ Comment : "has"
    Post ||--o{ Like : "receives"
    Comment ||--o{ Comment : "replies_to"
    
    BarterRequest ||--|| BarterTransaction : "becomes"
```

## 🔧 Application Components

### 1. Accounts App (`accounts/`)

**Purpose**: User management, authentication, and social relationships

**Key Models**:
- `User`: Extended Django user model with social features
- `Follow`: User follow relationships
- `UserPreferences`: User settings and notification preferences

**Key Features**:
- JWT-based authentication
- Social OAuth integration (Google, Facebook, Kakao)
- User profiles with privacy settings
- Follow/unfollow system
- Reputation scoring

### 2. Books App (`books/`)

**Purpose**: Book catalog, reviews, and collection management

**Key Models**:
- `Book`: Complete book information with metadata
- `Author`: Author biographical information
- `Publisher`: Publishing house details
- `Genre`: Book categorization
- `BookReview`: User reviews and ratings

**Key Features**:
- ISBN-based book identification
- Advanced search and filtering
- User reviews and ratings
- Book collections and wishlists
- Reading status tracking

### 3. Social App (`social/`)

**Purpose**: Social networking features and user interactions

**Key Models**:
- `Post`: User posts and updates
- `Comment`: Post comments with threading
- `Like`: Post and comment likes
- `BookClub`: Book discussion groups
- `DirectMessage`: Private messaging

**Key Features**:
- Activity feeds
- Post creation and sharing
- Comment system with threading
- Like/unlike functionality
- Direct messaging
- Book clubs and discussions

### 4. Barter App (`barter/`)

**Purpose**: Book exchange system and transaction management

**Key Models**:
- `BarterRequest`: Exchange requests between users
- `BarterCounter`: Counter-offers in negotiations
- `BarterTransaction`: Completed exchanges
- `BarterRating`: Post-exchange ratings

**Key Features**:
- Barter request workflow
- Negotiation system
- Meeting coordination
- Transaction tracking
- Rating and feedback system

### 5. Notify App (`notify/`)

**Purpose**: Comprehensive notification system

**Key Models**:
- `Notification`: System notifications
- `NotificationPreference`: User notification settings
- `NotificationTemplate`: Customizable templates
- `NotificationBatch`: System announcements

**Key Features**:
- Real-time notifications
- Email notifications
- Push notifications
- Notification preferences
- Batch notifications

## 🔌 API Architecture

### REST API Structure

```mermaid
graph TD
    A[Client Request] --> B[CORS Middleware]
    B --> C[Authentication Middleware]
    C --> D[DRF ViewSet/APIView]
    D --> E[Serializer Validation]
    E --> F[Business Logic]
    F --> G[Database Query]
    G --> H[Response Serialization]
    H --> I[JSON Response]
    
    subgraph "Authentication Flow"
        J[JWT Token] --> K[Token Validation]
        K --> L[User Identification]
        L --> M[Permission Check]
    end
    
    C --> J
```

### API Endpoints Structure

```
/api/v1/
├── auth/
│   ├── signup/              # User registration
│   ├── login/               # User login
│   ├── logout/              # User logout
│   ├── refresh/             # Token refresh
│   ├── social/              # Social authentication
│   ├── forgot/              # Password reset
│   └── profile/             # User profile management
├── books/
│   ├── books/               # Book CRUD operations
│   ├── authors/             # Author management
│   ├── genres/              # Genre listing
│   ├── reviews/             # Book reviews
│   └── search/              # Book search
├── social/
│   ├── posts/               # Social posts
│   ├── comments/            # Post comments
│   ├── likes/               # Like/unlike
│   ├── follow/              # Follow/unfollow
│   ├── feed/                # Activity feed
│   └── messages/            # Direct messages
├── barter/
│   ├── requests/            # Barter requests
│   ├── transactions/        # Barter transactions
│   ├── ratings/             # Exchange ratings
│   └── history/             # Barter history
└── notifications/
    ├── list/                # User notifications
    ├── mark-read/           # Mark as read
    ├── preferences/         # Notification settings
    └── unread-count/        # Unread count
```

## 🔐 Security Architecture

### Authentication & Authorization

```mermaid
graph LR
    A[Client] --> B[JWT Token]
    B --> C[Token Validation]
    C --> D[User Authentication]
    D --> E[Permission Check]
    E --> F[Resource Access]
    
    subgraph "Social Auth"
        G[OAuth Provider]
        H[Access Token]
        I[User Info]
        J[Account Creation/Link]
    end
    
    A --> G
    G --> H
    H --> I
    I --> J
    J --> D
```

### Security Features

- **JWT Authentication**: Stateless token-based authentication
- **Social OAuth**: Google, Facebook, Kakao integration
- **CORS Protection**: Cross-origin request security
- **CSRF Protection**: Cross-site request forgery prevention
- **Rate Limiting**: API request throttling
- **Input Validation**: Comprehensive data validation
- **SQL Injection Protection**: Django ORM protection
- **XSS Protection**: Cross-site scripting prevention

## 📊 Performance & Scalability

### Caching Strategy

```mermaid
graph TD
    A[Client Request] --> B[Django View]
    B --> C{Cache Hit?}
    C -->|Yes| D[Return Cached Data]
    C -->|No| E[Database Query]
    E --> F[Process Data]
    F --> G[Cache Result]
    G --> H[Return Response]
    
    subgraph "Cache Layers"
        I[Redis Cache]
        J[Database Query Cache]
        K[Template Cache]
    end
    
    C --> I
    E --> J
    H --> K
```

### Database Optimization

- **Indexing**: Strategic database indexes for performance
- **Query Optimization**: Efficient ORM queries with select_related/prefetch_related
- **Connection Pooling**: Database connection management
- **Read Replicas**: Separate read/write database instances (production)

## 🚀 Deployment Architecture

### Production Deployment

```mermaid
graph TB
    subgraph "Load Balancer"
        A[AWS ALB]
    end
    
    subgraph "Application Servers"
        B[Django Instance 1]
        C[Django Instance 2]
        D[Django Instance N]
    end
    
    subgraph "Database Layer"
        E[(PostgreSQL Primary)]
        F[(PostgreSQL Replica)]
    end
    
    subgraph "Cache Layer"
        G[Redis Cluster]
    end
    
    subgraph "Storage"
        H[AWS S3]
        I[CloudFront CDN]
    end
    
    subgraph "Monitoring"
        J[CloudWatch]
        K[Application Logs]
    end
    
    A --> B
    A --> C
    A --> D
    
    B --> E
    C --> E
    D --> E
    
    B --> F
    C --> F
    D --> F
    
    B --> G
    C --> G
    D --> G
    
    B --> H
    C --> H
    D --> H
    
    H --> I
    
    B --> J
    C --> J
    D --> J
    
    B --> K
    C --> K
    D --> K
```

## 🧪 Testing Architecture

### Test Structure

```
backend/tests/
├── test_api_integration.py    # API endpoint testing
├── test_auth_debug.py         # Authentication testing
├── test_server.py             # Server functionality testing
└── debug_auth.py              # Authentication diagnostics

Individual App Tests:
├── accounts/tests.py          # User management tests
├── books/tests.py             # Book system tests
├── social/tests.py            # Social features tests
├── barter/tests.py            # Barter system tests
└── notify/tests.py            # Notification tests
```

### Testing Strategy

- **Unit Tests**: Individual component testing
- **Integration Tests**: API endpoint testing
- **Authentication Tests**: Security and auth flow testing
- **Performance Tests**: Load and stress testing
- **End-to-End Tests**: Complete user workflow testing

## 🔄 Data Flow Architecture

### User Authentication Flow

```mermaid
sequenceDiagram
    participant C as Client
    participant A as Auth API
    participant DB as Database
    participant R as Redis
    participant O as OAuth Provider

    Note over C,O: Registration Flow
    C->>A: POST /auth/signup/
    A->>DB: Create User
    DB-->>A: User Created
    A-->>C: Success Response

    Note over C,O: Login Flow
    C->>A: POST /auth/login/
    A->>DB: Validate Credentials
    DB-->>A: User Validated
    A->>R: Cache User Session
    A-->>C: JWT Tokens

    Note over C,O: Social Auth Flow
    C->>O: OAuth Request
    O-->>C: Access Token
    C->>A: POST /auth/social/
    A->>O: Validate Token
    O-->>A: User Info
    A->>DB: Create/Update User
    A-->>C: JWT Tokens
```

### Barter Request Flow

```mermaid
sequenceDiagram
    participant U1 as User 1 (Requester)
    participant API as Barter API
    participant DB as Database
    participant N as Notification Service
    participant U2 as User 2 (Recipient)

    U1->>API: Create Barter Request
    API->>DB: Save Request
    API->>N: Send Notification
    N->>U2: Notify New Request

    U2->>API: Accept/Reject Request
    API->>DB: Update Request Status
    API->>N: Send Status Notification
    N->>U1: Notify Status Change

    Note over U1,U2: If Accepted
    U1->>API: Confirm Meeting
    U2->>API: Confirm Meeting
    API->>DB: Create Transaction
    API->>N: Send Meeting Confirmation

    Note over U1,U2: After Exchange
    U1->>API: Complete Transaction
    U2->>API: Rate Exchange
    API->>DB: Update Transaction
    API->>DB: Update User Reputation
```

## 🛠️ Technology Stack

### Backend Technologies

| Component | Technology | Purpose |
|-----------|------------|---------|
| **Framework** | Django 5.2+ | Web application framework |
| **API** | Django REST Framework | RESTful API development |
| **Authentication** | JWT + OAuth2 | Token-based authentication |
| **Database** | PostgreSQL (default) / SQLite fallback | Data persistence |
| **Cache** | Redis | Session storage and caching |
| **Task Queue** | Celery | Asynchronous task processing |
| **WebSocket** | Django Channels | Real-time communication |
| **File Storage** | Local (dev) / AWS S3 (prod) | Media file storage |
| **Email** | SMTP / AWS SES | Email notifications |
| **Monitoring** | Django Debug Toolbar | Development debugging |

### Third-Party Integrations

| Service | Purpose | Implementation |
|---------|---------|----------------|
| **Google OAuth** | Social authentication | django-allauth |
| **Facebook OAuth** | Social authentication | django-allauth |
| **Kakao OAuth** | Social authentication | django-allauth |
| **AWS S3** | File storage | django-storages |
| **CloudFront** | CDN | AWS integration |
| **SES** | Email delivery | django-ses |
| **CloudWatch** | Monitoring | AWS SDK |

## 📈 Monitoring & Observability

### Application Monitoring

```mermaid
graph TD
    subgraph "Application Layer"
        A[Django Application]
        B[API Endpoints]
        C[Database Queries]
        D[Cache Operations]
    end

    subgraph "Monitoring Tools"
        E[Django Debug Toolbar]
        F[Application Logs]
        G[Performance Metrics]
        H[Error Tracking]
    end

    subgraph "Infrastructure Monitoring"
        I[AWS CloudWatch]
        J[Database Metrics]
        K[Server Metrics]
        L[Network Metrics]
    end

    A --> E
    B --> F
    C --> G
    D --> H

    E --> I
    F --> I
    G --> J
    H --> K
```

### Logging Strategy

```python
# Logging Configuration
LOGGING = {
    'version': 1,
    'disable_existing_loggers': False,
    'formatters': {
        'verbose': {
            'format': '{levelname} {asctime} {module} {process:d} {thread:d} {message}',
            'style': '{',
        },
    },
    'handlers': {
        'file': {
            'level': 'INFO',
            'class': 'logging.FileHandler',
            'filename': 'logs/django.log',
            'formatter': 'verbose',
        },
        'console': {
            'level': 'DEBUG',
            'class': 'logging.StreamHandler',
            'formatter': 'verbose',
        },
    },
    'loggers': {
        'django': {
            'handlers': ['file', 'console'],
            'level': 'INFO',
            'propagate': True,
        },
        'accounts': {
            'handlers': ['file', 'console'],
            'level': 'DEBUG',
            'propagate': True,
        },
        'barter': {
            'handlers': ['file', 'console'],
            'level': 'DEBUG',
            'propagate': True,
        },
    },
}
```

## 🔧 Configuration Management

### Environment-Based Configuration

```python
# Development Settings
DEBUG = True
DATABASE_URL = 'sqlite:///db.sqlite3'
ALLOWED_HOSTS = ['localhost', '127.0.0.1', '10.0.2.2']
CORS_ALLOW_ALL_ORIGINS = True

# Production Settings
DEBUG = False
DATABASE_URL = 'postgresql://user:pass@host:port/db'
ALLOWED_HOSTS = ['api.bookbarter.com']
CORS_ALLOWED_ORIGINS = ['https://bookbarter.com']

# Security Settings
SECURE_SSL_REDIRECT = True
SECURE_HSTS_SECONDS = 31536000
SECURE_HSTS_INCLUDE_SUBDOMAINS = True
SECURE_HSTS_PRELOAD = True
```

### Feature Flags

```python
# Feature toggles for gradual rollout
FEATURE_FLAGS = {
    'SOCIAL_AUTH_ENABLED': True,
    'REAL_TIME_NOTIFICATIONS': True,
    'ADVANCED_SEARCH': True,
    'BOOK_RECOMMENDATIONS': False,  # Coming soon
    'VIDEO_CALLS': False,           # Future feature
}
```

## 🚀 Future Architecture Considerations

### Microservices Migration Path

```mermaid
graph TB
    subgraph "Current Monolith"
        A[Django Monolith]
        B[Single Database]
        C[Shared Cache]
    end

    subgraph "Future Microservices"
        D[User Service]
        E[Book Service]
        F[Social Service]
        G[Barter Service]
        H[Notification Service]
    end

    subgraph "Supporting Infrastructure"
        I[API Gateway]
        J[Service Discovery]
        K[Message Queue]
        L[Distributed Cache]
    end

    A --> D
    A --> E
    A --> F
    A --> G
    A --> H

    D --> I
    E --> I
    F --> I
    G --> I
    H --> I

    I --> J
    I --> K
    I --> L
```

### Scalability Roadmap

1. **Phase 1**: Optimize current monolith
   - Database query optimization
   - Caching implementation
   - CDN integration

2. **Phase 2**: Extract notification service
   - Separate notification microservice
   - Message queue implementation
   - Real-time WebSocket service

3. **Phase 3**: Extract user service
   - Authentication microservice
   - User profile service
   - Social graph service

4. **Phase 4**: Full microservices architecture
   - Complete service decomposition
   - API gateway implementation
   - Service mesh deployment

---

**Last Updated**: September 29, 2025
**Architecture Version**: 1.0
**Django Version**: 5.2+
**Status**: Production Ready
