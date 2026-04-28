# Banking Backend

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.x-6DB33F?logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-4169E1?logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker)
![Railway](https://img.shields.io/badge/Deployed-Railway-blueviolet?logo=railway)

A production-grade banking backend built to understand how money moves safely — not just how to write CRUD endpoints.

**Live API:** [https://reasonable-reprieve-production.up.railway.app/swagger-ui/index.html](https://banking-application.up.railway.app/swagger-ui/index.html)


---

## Why I built this

Most portfolio projects are todo apps with auth bolted on. I wanted to build something that forced me to think about real problems: what happens when two transfers hit the same account at the same time? What prevents a client retry from charging twice? How do you ensure a debit and credit either both happen or neither does?

This project is my answer to those questions.

---

## Architecture

```
┌─────────────────────────────────────────┐
│           Spring Security Layer          │
│      JWT Filter · @PreAuthorize RBAC     │
└──────────────────┬──────────────────────┘
                   │
     ┌─────────────┼─────────────┐
     ▼             ▼             ▼
┌─────────┐  ┌──────────┐  ┌────────────┐
│  Auth   │  │ Account  │  │Transaction │
│ Module  │  │ Module   │  │  Module    │
└─────────┘  └──────────┘  └────────────┘
                   │
     ┌─────────────▼─────────────┐
     │       PostgreSQL 15        │
     │  Flyway · Optimistic Lock  │
     └───────────────────────────┘
```

---

## Tech Stack

| Layer | Technology | Decision |
|-------|------------|----------|
| Language | Java 21 | LTS, modern records and sealed classes |
| Framework | Spring Boot 4.x | Industry standard for JVM backends |
| Security | Spring Security + JWT | Stateless — no session overhead, horizontally scalable |
| Database | PostgreSQL 15 | ACID guarantees, strong consistency for financial data |
| Migrations | Flyway | Versioned, auditable schema changes — `ddl-auto` is not acceptable in finance |
| ORM | JPA / Hibernate + `@Version` | Optimistic locking for concurrent transfer safety |
| Docs | SpringDoc OpenAPI | Interactive API testing, no Postman required |
| Containers | Docker + Compose | One-command startup — zero environment setup |
| Deploy | Railway | Live demo accessible without cloning |

---

## Features

### Authentication & Authorization
- JWT-based stateless authentication
- BCrypt password hashing — plain text passwords never touch the database
- Three roles with distinct permission boundaries: `CUSTOMER`, `STAFF`, `ADMIN`
- `@PreAuthorize` per endpoint + ownership checks in the service layer

### Account Management
- Create bank accounts with unique account numbers
- `GET /accounts` — Admin/Staff only (full list)
- `GET /accounts/my-accounts` — Customer sees only their own
- `GET /accounts/{id}` — Customers cannot access other users' accounts (403, not 404)
- Freeze accounts — Admin only

### Money Transfer Engine
This is the core of the project. A transfer is not a single database write — it is two coordinated writes that must both succeed or both fail.

- `@Transactional` wraps debit + credit — any exception rolls back both
- `@Version` on `Account` entity — optimistic locking prevents concurrent transfers from producing a negative balance
- Daily transfer limit enforced per account — configurable via environment variable
- Unique reference numbers per transaction leg (`DBT-` / `CDT-`)
- Ownership verification before execution — users cannot transfer from accounts they do not own

### Transaction History
- Paginated history per account, sorted latest-first
- Full pagination metadata — `totalElements`, `totalPages`, `number`, `size`

### API Documentation
- Swagger UI with JWT Bearer authentication built-in
- All endpoints testable from the browser at `/swagger-ui/index.html`

---

## Engineering Decisions

**Why `@Transactional` on transfer?**
A transfer is debit source + credit target. A crash between the two operations would either destroy money or create it from thin air. `@Transactional` guarantees the database treats both writes as one atomic unit.

**Why Optimistic Locking instead of Pessimistic?**
`SELECT FOR UPDATE` serializes every concurrent transfer on the same account — a bottleneck under load. Optimistic Locking allows concurrent reads and detects conflicts only at commit time. The losing transaction receives an `ObjectOptimisticLockingFailureException`. This is the standard approach in high-throughput payment systems.

**Why Flyway instead of `ddl-auto`?**
`ddl-auto=update` in production can silently drop columns. Every schema change in this project is a versioned, named SQL file in version control — reviewable, testable, and reversible. That is the only acceptable approach in any financial system.

**Why DTOs instead of exposing entities?**
JPA entities can expose internal fields (`version`, lazy collections) that have no place in an API response. DTOs give explicit control over the contract — a database refactor does not break the API.

**Why `BigDecimal` for all monetary values?**
`double` arithmetic is not exact. `0.1 + 0.2 = 0.30000000000000004`. In a payment system, a rounding error is not a bug — it is a liability. `BigDecimal` is non-negotiable.

---

## Project Structure

```
src/main/java/com/example/banking/
├── config/          # SecurityConfig, OpenApiConfig
├── controller/      # REST controllers — no business logic
├── service/         # AccountService, AuthService, TransactionService
├── repository/      # JPA repositories
├── domain/          # Account, User, Transaction, Role, AccountStatus
├── dto/             # Request/Response DTOs
├── exception/       # Custom exceptions + GlobalExceptionHandler
└── security/        # JwtUtils, JwtAuthFilter, UserDetailsServiceImpl
```

---

## API Reference

### Auth
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST | `/api/v1/auth/register` | Public | Register new user |
| POST | `/api/v1/auth/login` | Public | Login, returns JWT |

### Accounts
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST | `/api/v1/accounts` | Authenticated | Create account |
| GET | `/api/v1/accounts` | Admin, Staff | List all accounts |
| GET | `/api/v1/accounts/my-accounts` | Customer | List own accounts |
| GET | `/api/v1/accounts/{id}` | Authenticated* | Get account by ID |
| PATCH | `/api/v1/accounts/{id}/freeze` | Admin | Freeze account |

*Customers are 403'd on accounts they do not own.

### Transactions
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST | `/api/v1/transactions/transfer` | Customer | Execute transfer |
| GET | `/api/v1/transactions/history/{accountNumber}` | Authenticated* | Paginated history |

*Customers are 403'd on accounts they do not own.

---

## Database Schema

```
users
├── id (UUID, PK)
├── email (unique)
├── password (BCrypt)
├── role (ADMIN / STAFF / CUSTOMER)
└── created_at

accounts
├── id (UUID, PK)
├── account_number (unique)
├── balance (NUMERIC 19,4)
├── currency
├── status (ACTIVE / FROZEN / CLOSED)
├── version (optimistic lock counter)
├── user_id (FK → users)
└── created_at

transactions
├── id (UUID, PK)
├── account_id (FK → accounts)
├── type (DEBIT / CREDIT)
├── amount (NUMERIC 19,4)
├── balance_after (NUMERIC 19,4)
├── related_account_id
├── description
├── reference_number (unique — DBT-xxx / CDT-xxx)
└── created_at
```

---

## Running Locally

### Docker (recommended)

Requires Docker and Docker Compose — nothing else.

```bash
git clone https://github.com/edamame69/banking-app.git
cd banking-app
docker-compose up --build
```

- App: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

### Without Docker

Requires Java 21+, PostgreSQL 15+, Maven 3.8+.

```bash
# Create database
psql -U postgres -c "CREATE DATABASE bankingdb;"

# Set environment variables
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/bankingdb
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=your_password
export APP_JWT_SECRET=your_secret_key
export APP_JWT_EXPIRATION=86400000
export APP_TRANSFER_DAILY_LIMIT=50000000

# Run (Flyway creates all tables automatically)
./mvnw spring-boot:run
```

---

## What's next

The foundation is solid. The next layer I am building:

- **Chatbot assistant** — natural language interface over the transaction API using Claude
- **Account types** — `SAVINGS` with daily compounding interest via `@Scheduled`
- **Email verification** — SMTP-based OTP on registration
- **Deploy on AWS** — EC2 + RDS, proper production environment

---

## License

Educational use.
