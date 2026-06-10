# Customer Support Hub

A secure, role-based customer-support backend built with **Spring Boot 3**, **Spring Data JPA over
MySQL**, **Spring Validation**, and **Spring Security (OAuth2 Resource Server + JWT)**.

The service manages three roles and the relationships between them:

| Role | Capabilities |
|------|--------------|
| **ADMIN** | Can do anything in the system. Seeded on startup. |
| **AGENT** | Owns several customers. Creates/lists its own customers; searches its customers' tickets; updates its own profile. |
| **CUSTOMER** | Registered under an agent. Opens and reads its own tickets; queries/updates its own profile. |

---

## Architecture

Standard layered design (package `com.example.supporthub`):

```
domain/       JPA entities (User, Ticket) + enums (Role, TicketStatus)
repository/   Spring Data repositories
dto/          request/response records + ErrorResponse
service/      AuthService, CustomerService, ProfileService, TicketService (authorization/ownership logic)
security/     JwtService (token minting), AppUserDetailsService
config/       SecurityConfig, JwtConfig (HS256 encoder/decoder), JwtProperties, DataSeeder
web/          REST controllers
web/error/    GlobalExceptionHandler, custom exceptions, JSON auth entry-point / access-denied handlers
```

A single `users` table holds all roles; a CUSTOMER row references its owning AGENT via a
self-referencing `agent_id`. Tickets reference their owning customer.

### Authorization model

Two complementary layers:

1. **Coarse role gates** — `@PreAuthorize` on endpoints (e.g. only `AGENT`/`ADMIN` may create a
   customer; only `CUSTOMER` may open a ticket).
2. **Per-record ownership checks** — enforced in the service layer: an AGENT only ever touches its
   own customers and their tickets; a CUSTOMER only its own records; ADMIN bypasses all checks.
   Violations produce `403` (or `404` where revealing existence would leak information).

---

## Security: how authentication works (and a tradeoff note)

The assignment asks to *"use Spring OAuth to authenticate a user via username/password
credentials."* Worth being precise about what that means in 2024+:

- The OAuth2 **Resource Owner Password Credentials grant** was deprecated in OAuth 2.0 and
  **removed in OAuth 2.1**; the current **Spring Authorization Server does not implement it**, and
  the old `spring-security-oauth2` project that did has been **end-of-life since 2022**.

So this service takes the modern, supported interpretation:

- **`POST /api/auth/login`** verifies username/password via Spring Security's
  `AuthenticationManager` (passwords stored **BCrypt**-hashed) and returns a **signed JWT**.
- Every other endpoint is protected by an **OAuth2 Resource Server** that validates the **JWT
  bearer token** on each request and rebuilds the caller's authorities from the token's `roles`
  claim. The app is fully **stateless** (no sessions).
- Tokens are signed with a symmetric **HS256** secret (`JWT_SECRET`), since a single self-contained
  service both issues and validates them.

This keeps the codebase small and avoids hand-rolling a deprecated grant, while still using the
OAuth2/JWT machinery for all request authorization.

---

## Building & running

### Prerequisites
- JDK 21
- Docker + Docker Compose (for the containerised path)
- A MySQL 8 instance (provided by Docker Compose for local dev)

### Option A — Docker Compose (recommended)

Brings up MySQL and the app together:

```bash
docker compose up --build
```

The app is then available at `http://localhost:8080`. A bootstrap admin is seeded
(`admin` / `admin` by default — see configuration below).

### Option B — Local Maven run

Start a MySQL (e.g. `docker compose up mysql`) then:

```bash
./mvnw spring-boot:run
```

### Run the tests

```bash
./mvnw test         # unit + security-aware web slice tests (no DB required)
./mvnw clean verify # full build + tests
```

---

## Configuration

All settings have local-dev defaults and are overridable via environment variables:

| Env var | Default | Purpose |
|---------|---------|---------|
| `DB_URL` | `jdbc:mysql://localhost:3306/support_hub?...` | JDBC URL |
| `DB_USER` / `DB_PASSWORD` | `support` / `support` | MySQL credentials |
| `JWT_SECRET` | dev-only placeholder | HS256 signing secret (**must** be ≥ 32 bytes; rotate in prod) |
| `JWT_TTL_SECONDS` | `3600` | Access-token lifetime |
| `SEED_ADMIN_USERNAME` | `admin` | Bootstrap admin username |
| `SEED_ADMIN_PASSWORD` | `admin` | Bootstrap admin password |

> The seeded admin is **configuration-driven**, not hard-coded. Set `SEED_ADMIN_USERNAME` /
> `SEED_ADMIN_PASSWORD` to control it; it is only created if it does not already exist.

---

## API reference

All request bodies are validated; errors return a JSON `ErrorResponse`
(`timestamp, status, error, message, path, fieldErrors`) with an appropriate status
(`400`, `401`, `403`, `404`, `409`).

| Method | Path | Role | Description |
|--------|------|------|-------------|
| `POST` | `/api/auth/login` | public | Exchange credentials for a JWT |
| `POST` | `/api/admin/agents` | ADMIN | Create an AGENT (the only way to add agents) |
| `POST` | `/api/admin/customers/{customerId}/tickets` | ADMIN | Open a ticket on behalf of a customer (a ticket is owned by a CUSTOMER, so admin names the customer) |
| `POST` | `/api/admin/agents/{agentId}/customers` | ADMIN | Create a customer under the named agent (admin owns no customers, so it names the agent) |
| `GET`  | `/api/users/me` | any | Get own profile |
| `PUT`  | `/api/users/me` | any | Update own profile (fullName / email / password) |
| `POST` | `/api/customers` | AGENT | Create a customer under the calling agent |
| `GET`  | `/api/customers` | AGENT, ADMIN | List customers (AGENT → own; ADMIN → all) |
| `GET`  | `/api/customers/{id}` | AGENT(own) / ADMIN | Get a customer |
| `POST` | `/api/tickets` | CUSTOMER | Open a ticket |
| `GET`  | `/api/tickets` | any | List tickets (CUSTOMER → own; AGENT → their customers', optional `?status=`; ADMIN → all) |
| `GET`  | `/api/tickets/{id}` | owner / owning-agent / ADMIN | Get a ticket |

> For a full set of runnable **happy- and unhappy-path scenarios** covering every endpoint and
> role, see the **[`demo/`](demo/)** folder: a one-click IntelliJ
> **[`.http` file](demo/customer-support-hub.http)** and a narrative **[DEMO.md](demo/DEMO.md)**
> (with expected status codes and an authorization matrix).

### Example walkthrough (curl)

```bash
# 1. Log in as the seeded admin → capture the token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin"}' | jq -r .accessToken)

# 2. As admin, create an AGENT, then log in as that agent to get an AGENT token.
curl -X POST http://localhost:8080/api/admin/agents \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"username":"amy","password":"agent123","fullName":"Amy Agent","email":"amy@example.com"}'

AGENT_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"amy","password":"agent123"}' | jq -r .accessToken)

# 3. As an AGENT, create a customer (registered under that agent)
curl -X POST http://localhost:8080/api/customers \
  -H "Authorization: Bearer $AGENT_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"passw0rd","fullName":"Alice","email":"alice@example.com"}'

# 4. As that CUSTOMER, open a ticket
curl -X POST http://localhost:8080/api/tickets \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"subject":"Cannot log in","description":"I get a 500 error on login"}'

# 5. The owning AGENT can list its customers' tickets
curl -H "Authorization: Bearer $AGENT_TOKEN" http://localhost:8080/api/tickets
```

---

## Testing

- `CustomerServiceTest`, `TicketServiceTest`, `AdminUserServiceTest` — Mockito unit tests of the
  authorization/ownership logic (customer registered under calling agent, duplicate username → 409,
  agents only see their own customers/tickets, non-customers cannot open tickets, CUSTOMER requires
  an owning agent).
- `SecurityWebTest`, `AdminUserSecurityTest` — **security-aware** `@WebMvcTest` slices asserting
  `401` without a token, `403` for the wrong role (CUSTOMER on the AGENT-only customer endpoint;
  CUSTOMER/AGENT on the ADMIN-only user endpoint), and authorized access for the correct role.

---

## Notes & tradeoffs

- **MySQL is the only database engine** (no H2), per the assignment. Tests run against mocks / a web
  slice and therefore need no database.
- **Stateless JWT** keeps the service horizontally scalable; the tradeoff is no server-side token
  revocation (mitigated by short token TTLs).
- `ddl-auto=update` is used for convenience in this exercise; a production system would use
  versioned migrations (Flyway/Liquibase).
