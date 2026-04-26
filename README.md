# 🏦 Banking Application

A production-grade banking backend built with **Java Spring Boot**, featuring JWT authentication, role-based access control, ACID-compliant money transfers, and Docker support.

---

## 🚀 Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Java 21 |
| Framework | Spring Boot 4.x |
| Security | Spring Security + JWT |
| Database | PostgreSQL 15 |
| Migration | Flyway |
| ORM | JPA / Hibernate |
| Build Tool | Maven |
| API Documentation | Swagger UI (SpringDoc OpenAPI) |
| Containerization | Docker + Docker Compose |

---

## ✨ Features

### Authentication & Authorization
- User registration and login with **JWT (JSON Web Token)**
- **BCrypt** password hashing
- **Role-based access control** — `ADMIN`, `STAFF`, `CUSTOMER`
- Stateless authentication (no sessions)

### Account Management
- Create bank accounts
- View own accounts (Customer) or all accounts (Admin/Staff)
- Freeze/unfreeze accounts (Admin only)
- Each user can have multiple accounts

### Money Transfer
- Internal money transfer between accounts
- **ACID-compliant** transactions — atomic debit + credit
- Automatic rollback on failure
- **Optimistic Locking** — prevents race conditions on concurrent transfers
- Unique reference number per transaction (`DBT-` / `CDT-` prefix)
- Balance validation before transfer
- Ownership verification — only transfer from your own account

### Transaction History
- Paginated transaction history per account
- Sorted by latest first
- Returns full metadata (total pages, total elements, current page)

### API Documentation
- Interactive **Swagger UI** at `/swagger-ui/index.html`
- JWT authentication support in Swagger
- Test all endpoints directly from browser — no Postman needed

### DevOps
- **Dockerized** application with multi-stage build
- **Docker Compose** for one-command startup (app + database)
- Environment variable configuration for easy deployment

---

## 📁 Project Structure

```
src/main/java/com/example/banking/
├── config/          # Spring Security + OpenAPI configuration
├── controller/      # REST controllers (thin layer)
├── service/         # Business logic
├── repository/      # JPA repositories
├── domain/          # JPA entities
├── dto/             # Request/Response DTOs
├── exception/       # Custom exceptions + GlobalExceptionHandler
└── security/        # JWT filter, UserDetailsService
```

---

## 🔐 API Endpoints

### Auth
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST | `/api/v1/auth/register` | Public | Register new user |
| POST | `/api/v1/auth/login` | Public | Login and get JWT token |

### Accounts
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST | `/api/v1/accounts` | All roles | Create new account |
| GET | `/api/v1/accounts` | Admin, Staff | Get all accounts |
| GET | `/api/v1/accounts/my-accounts` | Customer | Get own accounts |
| GET | `/api/v1/accounts/{id}` | All roles* | Get account by ID |
| PATCH | `/api/v1/accounts/{id}/freeze` | Admin only | Freeze account |

> *Customer can only access their own accounts

### Transactions
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST | `/api/v1/transactions/transfer` | Customer | Transfer money |
| GET | `/api/v1/transactions/history/{accountNumber}` | All roles* | Get transaction history |

> *Customer can only view their own account history

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
├── balance (NUMERIC 19,4)
├── currency
├── status (ACTIVE / FROZEN / CLOSED)
├── version (Optimistic Locking)
├── user_id (FK → users)
└── created_at

transactions
├── id (UUID, PK)
├── account_id (FK → accounts)
├── type (DEBIT / CREDIT)
├── amount
├── balance_after
├── related_account_id
├── description
├── reference_number (unique)
└── created_at
```

---

## ⚙️ Getting Started

### 🐳 Run with Docker (Recommended)

**Prerequisites:** Docker + Docker Compose only — no Java or PostgreSQL installation needed!

```bash
git clone https://github.com/edamame69/banking-app.git
cd banking-app
docker-compose up --build
```

That's it! After startup:
- **App:** `http://localhost:8080`
- **Swagger UI:** `http://localhost:8080/swagger-ui/index.html`

---

### 🔧 Run Locally (Manual Setup)

**Prerequisites:**
- Java 21+
- PostgreSQL 15+
- Maven 3.8+

**1. Clone the repository:**
```bash
git clone https://github.com/edamame69/banking-app.git
cd banking-app
```

**2. Create PostgreSQL database:**
```sql
CREATE DATABASE bankingdb;
```

**3. Configure `application.yaml`:**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/bankingdb
    username: your_username
    password: your_password
app:
  jwt:
    secret: your_jwt_secret_key
    expiration: 86400000
```

**4. Run the application:**
```bash
./mvnw spring-boot:run
```

Flyway will automatically create all tables on startup.

---

## 📖 API Documentation

Access **Swagger UI** at:
```
http://localhost:8080/swagger-ui/index.html
```

**To authenticate in Swagger:**
1. Call `POST /api/v1/auth/login` to get JWT token
2. Click **Authorize 🔒** button (top right)
3. Enter: `Bearer <your_token>`
4. All subsequent requests will include the token automatically

**Quick start:**
```bash
# Register a user
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@banking.com","password":"123456","role":"CUSTOMER"}'

# Login and get token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@banking.com","password":"123456"}'
```

---

## 🔒 Security Design

- **JWT** — stateless, no server-side sessions
- **BCrypt** — passwords never stored in plain text
- **Role-based** — endpoints protected by `@PreAuthorize`
- **Ownership check** — customers cannot access other users' data
- **DTO pattern** — entities never exposed directly to clients
- **`BigDecimal`** — used for all monetary values (no floating point errors)
- **Optimistic Locking** — prevents concurrent transaction conflicts

---

## 🏗️ Key Design Decisions

### Why `@Transactional` on transfer?
Money transfer involves two operations: debit source + credit target. If the server crashes between them, `@Transactional` ensures both operations are rolled back — preventing money loss.

### Why Flyway instead of `ddl-auto`?
Flyway provides versioned, auditable database migrations. In production banking systems, database changes must be tracked, reviewable, and reversible.

### Why DTOs?
Entities are decoupled from the API contract. Database schema changes don't break the API, and sensitive internal fields are never accidentally exposed.

### Why Optimistic Locking?
Concurrent transfer requests could cause race conditions — two requests reading the same balance simultaneously, both passing validation, resulting in negative balance. `@Version` on the Account entity ensures only one transaction succeeds when conflicts occur.

### Why Docker?
Eliminates "works on my machine" problems. Anyone can clone the repo and run the entire stack (app + database) with a single command — no manual setup required.

---

## 📝 License

This project is for educational purposes.