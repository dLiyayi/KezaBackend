# Keza Backend Architecture

## System Architecture

```mermaid
flowchart TB
    Client["Web / Mobile Client"]
    LB["Load Balancer / Nginx"]

    Client --> LB
    LB --> API

    subgraph API["Keza Backend — Spring Boot 3.5 / Java 21"]
        Auth["Auth & Security<br/>JWT + OAuth2"]
        User["User Module<br/>Registration, Profiles"]
        KYC["KYC Module<br/>Document Verification"]
        Campaign["Campaign Module<br/>6-Step Wizard, State Machine"]
        Investment["Investment Module<br/>Portfolio, Cooling-Off"]
        Payment["Payment Module<br/>M-Pesa, Card, Bank"]
        AI["AI Module<br/>Chatbot, Fraud, Risk"]
        Market["Marketplace Module<br/>Secondary Trading"]
        Notif["Notification Module<br/>Email, SMS, Push"]
        Admin["Admin Module<br/>Dashboard, Due Diligence"]
    end

    API --> PG["PostgreSQL 17<br/>+ pgvector"]
    API --> Redis["Redis 7<br/>Cache + Sessions + Rate Limiting"]
    API --> RMQ["RabbitMQ 3.13<br/>Event Bus + DLQ"]
    API --> S3["MinIO / S3<br/>Object Storage"]

    Payment --> MPesa["Safaricom Daraja API<br/>M-Pesa STK Push"]
    Payment --> Flutter["Flutterwave / Paystack<br/>Card Payments"]
    Payment --> KCB["KCB<br/>Bank Transfer + Escrow"]
    Notif --> SendGrid["SendGrid<br/>Email Delivery"]
    Notif --> AT["Africa's Talking<br/>SMS Gateway"]
    AI --> Claude["Anthropic Claude<br/>LLM / Embeddings"]
```

## Module Architecture (Hexagonal)

```mermaid
flowchart LR
    subgraph Adapters_In["Adapters — Inbound"]
        REST["REST Controllers<br/>adapter/in/web"]
        MQ_In["RabbitMQ Listeners<br/>adapter/in/messaging"]
    end

    subgraph Application["Application Layer"]
        UC["Use Case Implementations<br/>application/usecase"]
        DTO["DTOs<br/>application/dto"]
    end

    subgraph Domain["Domain Core"]
        Model["Entities & Value Objects<br/>domain/model"]
        PortIn["Port In — Use Case Interfaces<br/>domain/port/in"]
        PortOut["Port Out — Repository Interfaces<br/>domain/port/out"]
        Svc["Domain Services<br/>domain/service"]
        Evt["Domain Events<br/>domain/event"]
    end

    subgraph Adapters_Out["Adapters — Outbound"]
        JPA["JPA Repositories<br/>adapter/out/persistence"]
        Ext["External Clients<br/>adapter/out/external"]
    end

    REST --> PortIn
    MQ_In --> PortIn
    PortIn --> UC
    UC --> Svc
    UC --> PortOut
    PortOut --> JPA
    PortOut --> Ext
```

## Module Dependency Graph

```mermaid
flowchart TB
    app["keza-app<br/>Main Application"]
    common["keza-common<br/>Shared Entities, DTOs, Enums"]
    infra["keza-infrastructure<br/>Security, Audit, Config"]
    user["keza-user<br/>Auth, Profiles, KYC"]
    campaign["keza-campaign<br/>Campaigns, Wizard"]
    investment["keza-investment<br/>Investments, Portfolio"]
    payment["keza-payment<br/>M-Pesa, Card, Bank"]
    ai["keza-ai<br/>Chatbot, Fraud, Risk"]
    market["keza-marketplace<br/>Secondary Trading"]
    notif["keza-notification<br/>Email, SMS, Push"]
    admin["keza-admin<br/>User Mgmt, DD, Analytics"]

    app --> user
    app --> campaign
    app --> investment
    app --> payment
    app --> ai
    app --> market
    app --> notif
    app --> admin

    user --> infra
    campaign --> infra
    investment --> infra
    payment --> infra
    ai --> infra
    market --> infra
    notif --> infra
    admin --> infra

    infra --> common
    user --> common
    campaign --> common
    investment --> common
    payment --> common
    ai --> common
    market --> common
    notif --> common
    admin --> common
```

## Investment Flow

```mermaid
sequenceDiagram
    participant Investor
    participant API as Keza API
    participant Val as InvestmentValidator
    participant Pay as PaymentService
    participant MPesa as M-Pesa (Daraja)
    participant DB as PostgreSQL
    participant MQ as RabbitMQ
    participant Notif as NotificationService

    Investor->>API: POST /investments
    API->>Val: Validate (KYC, limits, campaign status)
    Val-->>API: Validation passed
    API->>DB: Create Investment (PENDING)
    API->>Pay: Initiate Payment
    Pay->>MPesa: STK Push Request
    MPesa-->>Investor: USSD Prompt on Phone
    Investor->>MPesa: Enter M-Pesa PIN
    MPesa->>Pay: Callback (success)
    Pay->>DB: Transaction COMPLETED
    Pay->>DB: Investment CONFIRMED
    Pay->>DB: Update Campaign raised_amount
    Pay->>MQ: Publish InvestmentConfirmedEvent
    MQ->>Notif: Consume event
    Notif->>Investor: Email + SMS confirmation

    Note over API,DB: 48-hour cooling-off period begins
```

## Campaign State Machine

```mermaid
stateDiagram-v2
    [*] --> DRAFT: Issuer creates campaign
    DRAFT --> REVIEW: Submit for review
    REVIEW --> LIVE: Admin approves
    REVIEW --> DRAFT: Admin requests changes
    LIVE --> FUNDED: Target amount reached
    LIVE --> CLOSED: Deadline expired
    FUNDED --> CLOSED: Post-funding close
    DRAFT --> CANCELLED: Issuer cancels
    REVIEW --> CANCELLED: Admin rejects
    LIVE --> CANCELLED: Admin/Issuer cancels
```

## KYC State Machine

```mermaid
stateDiagram-v2
    [*] --> PENDING: User registers
    PENDING --> SUBMITTED: Documents uploaded
    SUBMITTED --> IN_REVIEW: Admin picks up
    IN_REVIEW --> APPROVED: All docs verified
    IN_REVIEW --> REJECTED: Docs failed verification
    REJECTED --> SUBMITTED: User re-uploads
```

## Data Model (Core Entities)

```mermaid
erDiagram
    USERS ||--o{ USER_ROLES : has
    USERS ||--o{ KYC_DOCUMENTS : uploads
    USERS ||--o{ INVESTMENTS : makes
    USERS ||--o{ NOTIFICATIONS : receives
    USERS ||--o{ MARKETPLACE_LISTINGS : creates

    CAMPAIGNS ||--o{ INVESTMENTS : receives
    CAMPAIGNS ||--o{ CAMPAIGN_MEDIA : has
    CAMPAIGNS ||--o{ CAMPAIGN_UPDATES : has
    CAMPAIGNS ||--o{ DUE_DILIGENCE_CHECKS : undergoes

    INVESTMENTS ||--o{ TRANSACTIONS : has
    INVESTMENTS ||--o{ MARKETPLACE_LISTINGS : listed_as

    MARKETPLACE_LISTINGS ||--o{ MARKETPLACE_TRANSACTIONS : traded_in

    USERS {
        uuid id PK
        string email UK
        string phone UK
        string password_hash
        enum kyc_status
        boolean active
    }

    CAMPAIGNS {
        uuid id PK
        uuid issuer_id FK
        string title
        decimal target_amount
        decimal raised_amount
        enum status
        timestamp deadline
    }

    INVESTMENTS {
        uuid id PK
        uuid investor_id FK
        uuid campaign_id FK
        decimal amount
        integer shares
        enum status
        timestamp cooling_off_expires
    }

    TRANSACTIONS {
        uuid id PK
        uuid investment_id FK
        decimal amount
        enum payment_method
        enum status
        jsonb provider_metadata
    }
```
