# Keza Backend

AI-powered equity crowdfunding platform for East Africa.

Keza enables retail investors to participate in startup and SME equity offerings, with integrated KYC verification, M-Pesa payments, AI-assisted due diligence, and a secondary marketplace for share trading.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
| Database | PostgreSQL 17 + pgvector |
| Cache | Redis 7 |
| Messaging | RabbitMQ 3.13 |
| Object Storage | MinIO (S3-compatible) |
| AI | Spring AI + Anthropic Claude |
| Build | Maven, JaCoCo |
| Containerization | Docker, Kubernetes |
| CI/CD | GitHub Actions |

## Architecture

Multi-module Maven project following hexagonal (ports & adapters) architecture:

```
keza-backend/
  keza-common/           Shared base entities, DTOs, exceptions, enums, utilities
  keza-infrastructure/   Security (JWT), audit, Redis, RabbitMQ, S3 configs
  keza-user/             Auth, registration, profiles, KYC document management
  keza-campaign/         Campaign CRUD, 6-step creation wizard, state machine
  keza-investment/       Investment processing, cooling-off period, portfolio
  keza-payment/          M-Pesa STK Push, card/bank payment routing
  keza-ai/               Chatbot, fraud detection, risk scoring
  keza-marketplace/      Secondary market listings, share trading
  keza-notification/     Email (SendGrid), SMS (Africa's Talking), push
  keza-admin/            User management, due diligence, analytics dashboard
  keza-app/              Spring Boot main app, Flyway migrations, assembly
```

Each domain module follows this internal structure:

```
com.keza.<module>/
  domain/model/             Entities, value objects
  domain/port/in/           Use case interfaces
  domain/port/out/          Repository & service interfaces
  domain/service/           Domain services
  domain/event/             Domain events
  application/usecase/      Use case implementations
  application/dto/          Request/response DTOs
  adapter/in/web/           REST controllers
  adapter/in/messaging/     RabbitMQ listeners
  adapter/out/persistence/  JPA repositories
  adapter/out/external/     HTTP clients, S3, payment gateways
```

## Prerequisites

- Java 21+
- Maven 3.9+
- Docker & Docker Compose

## Getting Started

### 1. Start Infrastructure

```bash
docker compose -f docker/docker-compose.yml up -d
```

This starts PostgreSQL 17 (with pgvector), Redis 7, RabbitMQ 3.13, and MinIO.

### 2. Build

```bash
mvn clean install -DskipTests
```

### 3. Run

```bash
mvn spring-boot:run -pl keza-app -Dspring-boot.run.profiles=dev
```

The app starts on **http://localhost:8080**.

### 4. Verify

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

### API Documentation

Swagger UI is available at **http://localhost:8080/swagger-ui.html** when running with the `dev` profile.

## Key Features

### Authentication & Authorization
- JWT-based auth with access tokens (15 min) and refresh tokens (7 days)
- BCrypt password hashing (cost factor 12)
- Account lockout after 5 failed login attempts
- Token blacklisting via Redis on logout
- Refresh token rotation

### KYC Verification
- Document upload to S3 (JPEG, PNG, PDF, max 10MB)
- State machine: PENDING -> SUBMITTED -> IN_REVIEW -> APPROVED/REJECTED
- Presigned URLs for secure document viewing
- AI document validation stub (replaceable with real OCR/LLM pipeline)
- Auto-approve when all required documents pass review

### Campaign Management
- 6-step creation wizard (Company Info, Offering Details, Pitch, Financials, Documents, Review)
- State machine: DRAFT -> REVIEW -> LIVE -> FUNDED/CLOSED
- Full-text search with industry, type, status, and keyword filters
- Optimistic locking for concurrent safety
- Scheduled auto-close (expired) and auto-fund (target reached)

### Investment Processing
- Investment validation (KYC status, campaign status, amount limits, CMA rules)
- 48-hour cooling-off period with cancellation support
- Share calculation based on campaign share price
- Optimistic locking on campaign raised amount
- Portfolio aggregation with sector distribution

### Payments
- **M-Pesa**: Daraja API STK Push with OAuth token caching, Resilience4j circuit breaker
- **Card**: Flutterwave/Paystack integration stub
- **Bank Transfer**: KCB escrow stub
- Strategy pattern routing by payment method
- Idempotent callback processing via Redis

### AI Services
- Chatbot with multilingual support (English, Swahili, French)
- Fraud detection: velocity checks, amount anomaly detection
- Campaign risk scoring: weighted 6-dimension analysis (1-10 scale)
- All AI services conditional (`keza.ai.enabled`) with stub fallbacks

### Secondary Marketplace
- Share listing with 12-month holding period enforcement
- Company consent requirement
- Escrow-based settlement with 2% seller fee
- Listing expiration and auto-cancellation

### Notifications
- Email via SendGrid, SMS via Africa's Talking
- RabbitMQ event-driven (user registered, KYC status, investment, payment)
- Per-user notification preferences with critical bypass
- Thymeleaf email templates

### Admin
- User management with filters (KYC status, user type, search)
- 144-point due diligence checklist across 5 categories
- Weighted risk report generation with recommendations
- Platform analytics dashboard with caching

## Testing

Run all unit tests (600 tests):

```bash
mvn test
```

Run integration tests (requires Docker):

```bash
mvn verify -pl keza-app
```

Tests use JUnit 5, Mockito, AssertJ, MockMvc, and Testcontainers.

## Configuration

### Profiles

| Profile | Purpose |
|---------|---------|
| `dev` | Local development with Docker Compose services |
| `test` | Automated tests with Testcontainers |
| `prod` | Production with environment variable configuration |

### Key Environment Variables (Production)

| Variable | Description |
|----------|-------------|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | Database password |
| `REDIS_HOST` | Redis hostname |
| `REDIS_PASSWORD` | Redis password |
| `RABBITMQ_HOST` | RabbitMQ hostname |
| `RABBITMQ_USERNAME` | RabbitMQ username |
| `RABBITMQ_PASSWORD` | RabbitMQ password |
| `JWT_SECRET` | Base64-encoded JWT signing key (min 256 bits) |
| `MPESA_CONSUMER_KEY` | Safaricom Daraja API consumer key |
| `MPESA_CONSUMER_SECRET` | Safaricom Daraja API consumer secret |
| `MPESA_PASSKEY` | M-Pesa STK Push passkey |
| `SENDGRID_API_KEY` | SendGrid email API key |

## Docker

Build the image:

```bash
mvn clean package -DskipTests
docker build -t keza/keza-backend .
```

The Dockerfile uses a multi-stage build with `eclipse-temurin:21-jre-alpine`, runs as a non-root user, and enables ZGC.

## Deployment

Kubernetes manifests are in `k8s/deployment.yml` including:
- Deployment with liveness/readiness probes
- Service (ClusterIP)
- HorizontalPodAutoscaler (2-10 replicas, 70% CPU target)
- ConfigMap and Secret templates

## Database Migrations

Flyway migrations are in `keza-app/src/main/resources/db/migration/`:

| Migration | Description |
|-----------|-------------|
| V1 | Audit logs table |
| V2 | Users, roles, user_roles with seed data |
| V3 | pgvector extension and AI schema |
| V4 | KYC documents with JSONB extracted data |
| V5 | Campaigns, media, updates with GIN full-text index |
| V7 | Investments and transactions |
| V8 | Notifications and preferences |
| V9 | Due diligence checks and reports |
| V10 | AI chat sessions and messages |
| V11 | Fraud alerts |
| V12 | Marketplace listings and transactions |

## License

Proprietary. All rights reserved.
