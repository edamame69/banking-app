# 🏦 Banking Application

A production-grade banking backend built with **Java Spring Boot**, featuring JWT authentication, role-based access control, and ACID-compliant money transfers.

---

## 🚀 Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Java 21 |
| Framework | Spring Boot 4.x |
| Security | Spring Security + JWT |
| Database | PostgreSQL |
| Migration | Flyway |
| ORM | JPA / Hibernate |
| Build Tool | Maven |

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
- Unique reference number per transaction (`DBT-` / `CDT-` prefix)
- Balance validation before transfer
- Ownership verification — only transfer from your own account

### Transaction History
- Paginated transaction history per account
- Sorted by latest first
- Returns full metadata (total pages, total elements, current page)

---

## 📁 Project Structure

```
src/main/java/com/example/banking/
├── config/          # Spring Security configuration
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

### Prerequisites
- Java 21+
- PostgreSQL 15+
- Maven 3.8+

### Setup

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

**5. Test the API:**

Register a user:
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@banking.com","password":"123456","role":"CUSTOMER"}'
```

Login and get token:
```bash
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

---

## 🏗️ Key Design Decisions

### Why `@Transactional` on transfer?
Money transfer involves two operations: debit source + credit target. If the server crashes between them, `@Transactional` ensures both operations are rolled back — preventing money loss.

### Why Flyway instead of `ddl-auto`?
Flyway provides versioned, auditable database migrations. In production banking systems, database changes must be tracked, reviewable, and reversible.

### Why DTOs?
Entities are decoupled from the API contract. Database schema changes don't break the API, and sensitive internal fields are never accidentally exposed.

---

## 📝 License

This project is for educational purposes.