# 🏦 Banking Application

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.x-6DB33F?logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-4169E1?logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker)
![License](https://img.shields.io/badge/License-Educational-lightgrey)

A **production-grade banking backend** built with Java Spring Boot — designed with real-world fintech engineering principles: ACID-compliant transfers, fraud detection, idempotency, optimistic locking, immutable audit trails, and OTP-based transaction confirmation.

> This project demonstrates not just coding ability, but **understanding of banking business logic** — how money moves safely, how risk is managed, and how compliance requirements shape system design.

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                        API Gateway Layer                     │
│         Spring Security · JWT Filter · Rate Limiting         │
└────────────────────────────┬────────────────────────────────┘
                             │
        ┌────────────────────┼────────────────────┐
        ▼                    ▼                    ▼
┌──────────────┐   ┌──────────────────┐   ┌──────────────────┐
│  Auth Module │   │  Account Module  │   │ Transaction Module│
│  JWT · RBAC  │   │  Lifecycle FSM   │   │  ACID · OTP · Fraud│
└──────────────┘   └──────────────────┘   └──────────────────┘
        │                    │                    │
        └────────────────────┼────────────────────┘
                             ▼
              ┌──────────────────────────┐
              │    PostgreSQL 15         │
              │  Flyway migrations       │
              │  Optimistic Locking      │
              └──────────────────────────┘
```

---

## 🚀 Tech Stack

| Layer | Technology | Why |
|-------|------------|-----|
| Language | Java 21 | LTS, virtual threads ready |
| Framework | Spring Boot 4.x | Industry standard for banking backends |
| Security | Spring Security + JWT | Stateless auth, no session management overhead |
| Database | PostgreSQL 15 | ACID compliance, strong consistency |
| Migration | Flyway | Versioned, auditable schema changes |
| ORM | JPA / Hibernate | With `@Version` for optimistic locking |
| Build | Maven | Dependency management, reproducible builds |
| Docs | Swagger UI (SpringDoc OpenAPI) | Interactive API testing without Postman |
| Containerization | Docker + Docker Compose | One-command startup, no environment setup |

---

## ✨ Features

### 🔐 Authentication & Authorization
- Registration and login with **JWT (JSON Web Token)**
- **BCrypt** password hashing — passwords never stored in plain text
- **Role-based access control** — `ADMIN`, `STAFF`, `CUSTOMER` with distinct permission boundaries
- Stateless authentication — no server-side sessions, horizontally scalable

### 🏦 Account Management
- Create bank accounts with unique account numbers
- Account type support: `CHECKING` / `SAVINGS`
- View own accounts (Customer) or all accounts (Admin/Staff)
- **Account Lifecycle State Machine**: `ACTIVE → DORMANT → CLOSED`
    - Accounts inactive for 365+ days automatically transition to `DORMANT`
    - `CLOSED` accounts cannot be reopened — regulatory compliance requirement
- Freeze/unfreeze accounts (Admin only)
- Each user can hold multiple accounts

### 💸 Money Transfer — Core Engine
- Internal money transfer between accounts
- **ACID-compliant** — atomic debit + credit, automatic rollback on failure
- **Optimistic Locking** (`@Version`) — prevents race conditions on concurrent transfers
- **Idempotency Key** (`X-Idempotency-Key` header) — retry-safe API, prevents double charge
- **OTP Confirmation** for high-value transfers above configurable threshold
    - Transfer request returns `pendingTransactionId`
    - OTP (mocked via log + DB) expires in 5 minutes
    - Confirm via `POST /transfer/confirm`
- **Daily transfer limit** enforcement per account
- Unique reference number per leg (`DBT-` debit / `CDT-` credit)
- Balance and ownership validation before execution

### 🚨 Fraud Detection
- Rule-based fraud engine — no ML required, mirrors real bank logic:
    - **Velocity check**: ≥5 transactions within 10 minutes → flagged
    - **Amount anomaly**: transfer > 5× average of last 30 days → flagged
    - **New account risk**: transfer to account created < 24 hours ago → flagged
- Flagged transactions enter `PENDING_REVIEW` status — funds held, not lost
- Admin review endpoint to approve or reject flagged transactions
- All fraud events recorded in the immutable audit log

### 📋 Audit Log (Immutable)
- Every significant action is recorded: login, transfer, freeze, OTP request, fraud flag, admin review
- **Append-only** — no UPDATE or DELETE on `audit_logs` table by design
- Mirrors compliance requirements (MAS TRM, SBV regulations require full audit trail)
- Queryable by Admin/Staff: filter by user, action type, date range

### 📅 Scheduled / Recurring Transfers
- Customer can create recurring transfers: `DAILY` / `WEEKLY` / `MONTHLY`
- Spring `@Scheduled` polls due jobs every minute
- **Failure handling**: insufficient funds → status `FAILED`, logged to audit
- After 3 consecutive failures → status `SUSPENDED` (prevents indefinite retries)
- Edge cases handled: frozen account, closed account, daily limit breach

### 📊 Customer Intelligence (Staff/Admin)
- **Customer Risk Profile**: rule-based scoring `LOW / MEDIUM / HIGH`
    - Factors: fraud flag count, transaction volume, account age, failed login attempts
    - Endpoint: `GET /api/v1/staff/customers/{id}/risk-profile`
- **Customer Activity Timeline**: unified event feed aggregating audit log + transactions
    - Single endpoint replaces jumping between multiple data sources
- **Staff Notes**: internal annotations on customer profiles — visible only to Staff/Admin
    - Supports `is_flagged` marker for customers under investigation

### 💰 Interest Calculation (Savings Accounts)
- Daily compounding interest on `SAVINGS` accounts
- Batch job runs end-of-day via `@Scheduled`
- Interest credited as `INTEREST_CREDIT` transaction type — fully traceable
- Rate configurable per account

### 📄 Account Statement Export
- Export paginated transaction history as **PDF** or **CSV**
- Filter by date range: `?from=2024-01-01&to=2024-03-31&format=pdf`
- Customer can export their own statements; Staff can export any account

### ⚡ Rate Limiting
- Per-user, per-endpoint rate limiting via **Bucket4j**
- `CUSTOMER`: 10 transfer requests/minute
- `STAFF`: 50 requests/minute
- Returns `429 Too Many Requests` with `Retry-After` header

### 🔍 Transaction History
- Paginated history per account, sorted latest-first
- Returns full pagination metadata (total pages, total elements, current page)

### 📖 API Documentation
- Interactive **Swagger UI** at `/swagger-ui/index.html`
- JWT Bearer authentication built-in
- All endpoints testable directly from browser

### 🐳 DevOps
- **Dockerized** with multi-stage build (smaller final image)
- **Docker Compose** — one-command startup for app + database
- Environment variable configuration for secrets

---

## 📁 Project Structure

```
src/main/java/com/example/banking/
├── config/           # Spring Security, OpenAPI, rate limiter config
├── controller/       # REST controllers — thin layer, no business logic
├── service/          # Business logic — transfer engine, fraud rules, interest calc
│   ├── TransferService.java       # ACID transfer + idempotency + OTP + fraud check
│   ├── FraudDetectionService.java # Rule engine: velocity, anomaly, new-account risk
│   ├── AuditService.java          # Append-only audit event recording
│   ├── ScheduledTransferService.java  # Recurring transfer execution + failure handling
│   └── InterestCalculationService.java # Daily compounding for savings accounts
├── repository/       # JPA repositories
├── domain/           # JPA entities
│   ├── Account.java           # With @Version for optimistic locking
│   ├── Transaction.java       # DEBIT / CREDIT / INTEREST_CREDIT / FRAUD_HOLD
│   ├── AuditLog.java          # Append-only, no @Modifying updates allowed
│   ├── ScheduledTransfer.java # Recurring job with failure counter
│   └── IdempotencyKey.java    # Replay protection, TTL 24h
├── dto/              # Request/Response DTOs — entities never exposed directly
├── exception/        # Custom exceptions + GlobalExceptionHandler
├── security/         # JWT filter, UserDetailsService
└── scheduler/        # @Scheduled jobs: interest calc, dormant detection, recurring tx
```

---

## 🔐 API Endpoints

### Auth
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST | `/api/v1/auth/register` | Public | Register new user |
| POST | `/api/v1/auth/login` | Public | Login, receive JWT token |

### Accounts
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST | `/api/v1/accounts` | All roles | Create new account |
| GET | `/api/v1/accounts` | Admin, Staff | Get all accounts |
| GET | `/api/v1/accounts/my-accounts` | Customer | Get own accounts |
| GET | `/api/v1/accounts/{id}` | All roles* | Get account by ID |
| PATCH | `/api/v1/accounts/{id}/freeze` | Admin | Freeze / unfreeze account |
| GET | `/api/v1/accounts/{id}/statement` | All roles* | Export PDF or CSV statement |

> *Customer can only access their own accounts

### Transactions
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST | `/api/v1/transactions/transfer` | Customer | Initiate transfer (returns OTP flow if high-value) |
| POST | `/api/v1/transactions/transfer/confirm` | Customer | Confirm high-value transfer with OTP |
| GET | `/api/v1/transactions/history/{accountNumber}` | All roles* | Paginated transaction history |

### Scheduled Transfers
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST | `/api/v1/scheduled-transfers` | Customer | Create recurring transfer |
| GET | `/api/v1/scheduled-transfers` | Customer | List own scheduled transfers |
| DELETE | `/api/v1/scheduled-transfers/{id}` | Customer | Cancel a scheduled transfer |

### Admin / Staff
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| GET | `/api/v1/admin/audit-logs` | Admin, Staff | Query audit log with filters |
| GET | `/api/v1/admin/transactions/flagged` | Admin, Staff | View fraud-flagged transactions |
| PATCH | `/api/v1/admin/transactions/{id}/review` | Admin | Approve or reject flagged transaction |
| GET | `/api/v1/staff/customers/{id}/risk-profile` | Admin, Staff | Customer risk score + factors |
| GET | `/api/v1/staff/customers/{id}/timeline` | Admin, Staff | Unified customer activity timeline |
| POST | `/api/v1/staff/customers/{id}/notes` | Admin, Staff | Add internal note on customer |
| GET | `/api/v1/staff/customers/{id}/notes` | Admin, Staff | Get staff notes for customer |

---

## 🗄️ Database Schema

```
users
├── id (UUID, PK)
├── email (unique)
├── password (BCrypt hashed)
├── role (ADMIN / STAFF / CUSTOMER)
└── created_at

accounts
├── id (UUID, PK)
├── account_number (unique)
├── account_type (CHECKING / SAVINGS)
├── balance (NUMERIC 19,4)
├── currency
├── status (ACTIVE / DORMANT / FROZEN / CLOSED)
├── interest_rate (NUMERIC 5,4) — applies to SAVINGS only
├── version (Optimistic Locking — @Version)
├── user_id (FK → users)
└── created_at

transactions
├── id (UUID, PK)
├── account_id (FK → accounts)
├── type (DEBIT / CREDIT / INTEREST_CREDIT / FRAUD_HOLD)
├── status (COMPLETED / PENDING_REVIEW / FAILED / REJECTED)
├── amount (NUMERIC 19,4)
├── balance_after (NUMERIC 19,4)
├── related_account_id (FK → accounts, nullable)
├── description
├── reference_number (unique — DBT-xxx / CDT-xxx)
├── idempotency_key (FK → idempotency_keys, nullable)
└── created_at

idempotency_keys
├── id (UUID, PK)
├── key_value (unique)
├── response_payload (JSONB — cached result)
├── user_id (FK → users)
└── expires_at

pending_otps
├── id (UUID, PK)
├── pending_transaction_id (UUID)
├── otp_code (6-digit, hashed)
├── user_id (FK → users)
├── amount
├── from_account_id / to_account_id
└── expires_at

scheduled_transfers
├── id (UUID, PK)
├── from_account_id / to_account_id (FK → accounts)
├── amount (NUMERIC 19,4)
├── frequency (DAILY / WEEKLY / MONTHLY)
├── status (ACTIVE / SUSPENDED / CANCELLED)
├── failure_count (max 3 before SUSPENDED)
├── next_run_at
└── created_at

audit_logs                       ← APPEND-ONLY, no UPDATE/DELETE
├── id (UUID, PK)
├── actor_id (FK → users)
├── actor_role
├── action (LOGIN / TRANSFER / FREEZE / OTP_REQUEST / FRAUD_FLAG / ADMIN_REVIEW ...)
├── target_type (USER / ACCOUNT / TRANSACTION)
├── target_id
├── metadata (JSONB — action-specific details)
└── created_at

customer_notes
├── id (UUID, PK)
├── staff_id (FK → users)
├── customer_id (FK → users)
├── content
├── is_flagged (boolean)
└── created_at
```

---

## ⚙️ Getting Started

### 🐳 Run with Docker (Recommended)

**Prerequisites:** Docker + Docker Compose only.

```bash
git clone https://github.com/edamame69/banking-app.git
cd banking-app
docker-compose up --build
```

- **App:** `http://localhost:8080`
- **Swagger UI:** `http://localhost:8080/swagger-ui/index.html`

### 🔧 Run Locally

**Prerequisites:** Java 21+, PostgreSQL 15+, Maven 3.8+

```bash
# 1. Clone
git clone https://github.com/edamame69/banking-app.git
cd banking-app

# 2. Create database
psql -U postgres -c "CREATE DATABASE bankingdb;"

# 3. Configure application.yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/bankingdb
    username: your_username
    password: your_password
app:
  jwt:
    secret: your_jwt_secret_key
    expiration: 86400000
  transfer:
    otp-threshold: 10000000   # OTP required above 10M VND
    daily-limit: 50000000    # 50M VND daily limit

# 4. Run (Flyway auto-creates all tables)
./mvnw spring-boot:run
```

### Quick Start with curl

```bash
# Register
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"customer@bank.com","password":"secure123","role":"CUSTOMER"}'

# Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"customer@bank.com","password":"secure123"}' | jq -r '.token')

# Create account
curl -X POST http://localhost:8080/api/v1/accounts \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"currency":"VND","accountType":"CHECKING"}'

# Transfer (with idempotency key)
curl -X POST http://localhost:8080/api/v1/transactions/transfer \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Idempotency-Key: $(uuidgen)" \
  -H "Content-Type: application/json" \
  -d '{"fromAccountId":"...","toAccountId":"...","amount":500000,"description":"Rent"}'
```

---

## 🛡️ Security Design

| Concern | Approach |
|---------|----------|
| Authentication | JWT — stateless, no server-side sessions |
| Password storage | BCrypt — one-way hash, never plain text |
| Authorization | `@PreAuthorize` per endpoint + ownership checks in service layer |
| Monetary precision | `BigDecimal` throughout — no floating-point errors |
| Concurrent transfers | Optimistic Locking (`@Version`) — one request wins, others retry |
| Replay attacks | Idempotency Key — same request replayed returns cached result |
| High-value transfers | OTP confirmation — 5-minute TTL, hashed storage |
| Brute force | Rate Limiting via Bucket4j — 429 + Retry-After |
| Audit trail | Append-only `audit_logs` — regulators can reconstruct any event |
| Data exposure | DTO pattern — JPA entities never serialized to API responses |

---

## 🏗️ Key Engineering Decisions

### Why `@Transactional` on transfer?
Money transfer is two operations: debit source + credit target. A crash between them would create money out of thin air or destroy it. `@Transactional` guarantees both succeed or both roll back — the fundamental invariant of any payment system.

### Why Optimistic Locking instead of Pessimistic?
Pessimistic locking (`SELECT FOR UPDATE`) serializes all transfers on the same account, creating a bottleneck under load. Optimistic Locking with `@Version` allows concurrent reads, detecting conflicts only at commit time — the losing transaction gets a `ObjectOptimisticLockingFailureException` and retries. This is how high-throughput payment systems work in practice.

### Why Idempotency Key?
Networks fail. Clients retry. Without idempotency, a retry after a timeout could charge a customer twice. The `X-Idempotency-Key` header (a UUID the client generates once per intent) lets the server detect retries and return the original result — the same pattern used by Stripe and PayPal.

### Why a Rule-Based Fraud Engine (not ML)?
ML models require training data, infrastructure, and monitoring pipelines — all out of scope for a backend service. Real banks run rule-based systems as the first layer of defense regardless. Velocity checks and amount anomaly detection catch the majority of fraud cases with zero training cost and fully auditable, explainable decisions.

### Why Append-Only Audit Log?
Regulators require that no one — not even DBAs — can erase the record of what happened. An append-only table (enforced at the application layer by never issuing UPDATE/DELETE) mirrors this requirement. The `metadata JSONB` column stores action-specific context without requiring schema changes per event type.

### Why Scheduled Transfer Failure Handling?
A naive implementation retries forever — burning money on fees and creating confusing customer experiences. The 3-strike rule (3 consecutive failures → `SUSPENDED`) mirrors how real banks handle standing orders. The failure reason is logged to the audit trail so customers can see exactly why their recurring transfer stopped.

### Why Flyway instead of `ddl-auto`?
`ddl-auto=update` in production is a liability — it can silently drop columns. Flyway gives versioned, peer-reviewed, reversible migrations. Every schema change is a named SQL file in version control — the same artifact that gets reviewed, tested in staging, and deployed to production. This is the only acceptable approach in any financial system.

### Why DTOs instead of exposing entities?
JPA entities can contain lazy-loaded collections that trigger N+1 queries when serialized. They can expose internal fields (like `version` for optimistic locking) that clients have no business seeing. DTOs give explicit control over the API contract — a database refactor doesn't break the API.

---

## 📝 License

This project is for educational purposes.
