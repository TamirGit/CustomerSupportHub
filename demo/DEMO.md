# Demo & Manual Test Scenarios

End-to-end scenarios you can run against a **running** Customer Support Hub to evaluate it,
test it by hand, and demonstrate it to others. Each scenario states its **purpose**, the
**command**, and the **expected result** (HTTP status + notable response). Both **happy** and
**unhappy** paths are covered for every role and endpoint.

## Two ways to run

1. **One-click (recommended):** open [`customer-support-hub.http`](customer-support-hub.http) in
   IntelliJ IDEA and use **Run all requests in file** (or run them one-by-one). Tokens, usernames
   and resource IDs are captured automatically between requests, and each request carries an
   assertion on its expected status, so a green run = the whole authorization model behaves
   correctly. It is **re-runnable** — each run creates users with a fresh random suffix, so it
   passes even against a non-empty database (no `docker compose down -v` needed).
2. **By hand / for explanation:** the `curl` scenarios below. This document is the narrative
   companion to the `.http` file — same scenarios, with expected responses spelled out, good for
   walking someone through the behavior. Note: these use fixed usernames (`amy`, `carol`, …), so on
   a **second** manual run the create steps return `409` (already exist) — reset with
   `docker compose down -v && docker compose up --build`, or vary the names.

---

## 0. Prerequisites & conventions

Start the service (it seeds the `admin`/`admin` ADMIN on first run):

```bash
docker compose up --build         # app on http://localhost:8080, MySQL alongside
```

These scenarios use `bash` + `curl` + [`jq`](https://jqlang.github.io/jq/). Run them in
**Git Bash** or **WSL**. (PowerShell users: see the note at the very bottom.)

The cast we build up as we go:

| User | Role | Created by | Notes |
|------|------|------------|-------|
| `admin` | ADMIN | seeded | password `admin` |
| `amy` | AGENT | admin | password `agent123` |
| `bob` | AGENT | admin | password `agent123` |
| `carol` | CUSTOMER | agent **amy** | password `cust123` |
| `dave` | CUSTOMER | agent **bob** | password `cust123` |

A helper to log in and print a token:

```bash
login() { curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"$1\",\"password\":\"$2\"}"; }

token() { login "$1" "$2" | jq -r .accessToken; }
```

> Throughout, `-i` (or `-s -o /dev/null -w '%{http_code}\n'`) is handy to see the status code.
> Add `| jq` to pretty-print JSON bodies.

---

## 1. Authentication (`/api/auth/login`)

### 1.1 ✅ Login with valid credentials → **200**
```bash
login admin admin | jq
# { "accessToken": "eyJ...", "tokenType": "Bearer", "expiresInSeconds": 3600 }
ADMIN=$(token admin admin)
```

### 1.2 ❌ Wrong password → **401**
```bash
curl -i -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"wrong"}'
# 401  { "status":401, "error":"Unauthorized", "message":"Invalid username or password", ... }
```

### 1.3 ❌ Unknown user → **401**
```bash
curl -s -o /dev/null -w '%{http_code}\n' -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' -d '{"username":"ghost","password":"whatever"}'
# 401  (same generic message — we don't reveal whether the username exists)
```

### 1.4 ❌ Missing/blank fields → **400** (validation)
```bash
curl -i -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' -d '{"username":"admin"}'
# 400  fieldErrors: ["password: password is required"]
```

### 1.5 ❌ Call a protected endpoint with **no token** → **401**
```bash
curl -i http://localhost:8080/api/users/me
# 401  message: "Authentication required: a valid bearer token must be supplied"
```

### 1.6 ❌ Call with a **garbage/invalid token** → **401**
```bash
curl -i http://localhost:8080/api/users/me -H "Authorization: Bearer not-a-real-jwt"
# 401
```

---

## 2. Admin provisions agents (`POST /api/admin/users`, ADMIN-only)

### 2.1 ✅ Admin creates agent `amy` → **201**
```bash
curl -i -X POST http://localhost:8080/api/admin/users \
  -H "Authorization: Bearer $ADMIN" -H 'Content-Type: application/json' \
  -d '{"username":"amy","password":"agent123","fullName":"Amy Agent","email":"amy@example.com","role":"AGENT"}'
# 201  { "id":2, "username":"amy", "role":"AGENT", "agentId":null, "createdAt":"...", "updatedAt":"..." }
```

### 2.2 ✅ Admin creates agent `bob` → **201**
```bash
curl -s -X POST http://localhost:8080/api/admin/users \
  -H "Authorization: Bearer $ADMIN" -H 'Content-Type: application/json' \
  -d '{"username":"bob","password":"agent123","fullName":"Bob Agent","email":"bob@example.com","role":"AGENT"}' | jq
AMY=$(token amy agent123)
BOB=$(token bob agent123)
```

### 2.3 ❌ A non-admin (agent) tries to provision a user → **403**
```bash
curl -i -X POST http://localhost:8080/api/admin/users \
  -H "Authorization: Bearer $AMY" -H 'Content-Type: application/json' \
  -d '{"username":"mallory","password":"agent123","fullName":"Mallory","email":"m@example.com","role":"AGENT"}'
# 403  message: "Access denied: you do not have permission to perform this action"
```

### 2.4 ❌ Duplicate username → **409**
```bash
curl -i -X POST http://localhost:8080/api/admin/users \
  -H "Authorization: Bearer $ADMIN" -H 'Content-Type: application/json' \
  -d '{"username":"amy","password":"agent123","fullName":"Amy 2","email":"amy2@example.com","role":"AGENT"}'
# 409  message: "Username 'amy' is already taken"
```

### 2.5 ❌ Invalid body — bad email, short password, missing role → **400**
```bash
curl -i -X POST http://localhost:8080/api/admin/users \
  -H "Authorization: Bearer $ADMIN" -H 'Content-Type: application/json' \
  -d '{"username":"x","password":"123","fullName":"X","email":"not-an-email"}'
# 400  fieldErrors include: username size, password size, email invalid, role required
```

### 2.6 ❌ Create a CUSTOMER without `agentId` → **400**
```bash
curl -i -X POST http://localhost:8080/api/admin/users \
  -H "Authorization: Bearer $ADMIN" -H 'Content-Type: application/json' \
  -d '{"username":"orphan","password":"cust123","fullName":"Orphan","email":"o@example.com","role":"CUSTOMER"}'
# 400  message: "agentId is required when creating a CUSTOMER"
```

### 2.7 ❌ Provide `agentId` when creating an AGENT → **400**
```bash
curl -i -X POST http://localhost:8080/api/admin/users \
  -H "Authorization: Bearer $ADMIN" -H 'Content-Type: application/json' \
  -d '{"username":"weird","password":"agent123","fullName":"Weird","email":"w@example.com","role":"AGENT","agentId":2}'
# 400  message: "agentId is only valid when creating a CUSTOMER"
```

### 2.8 ❌ Create a CUSTOMER under a non-existent agent → **404**
```bash
curl -i -X POST http://localhost:8080/api/admin/users \
  -H "Authorization: Bearer $ADMIN" -H 'Content-Type: application/json' \
  -d '{"username":"nope","password":"cust123","fullName":"Nope","email":"n@example.com","role":"CUSTOMER","agentId":9999}'
# 404  message: "Agent 9999 not found"
```

### 2.9 ❌ Create a CUSTOMER whose `agentId` points to a non-agent (the admin) → **400**
```bash
curl -i -X POST http://localhost:8080/api/admin/users \
  -H "Authorization: Bearer $ADMIN" -H 'Content-Type: application/json' \
  -d '{"username":"bad","password":"cust123","fullName":"Bad","email":"b@example.com","role":"CUSTOMER","agentId":1}'
# 400  message: "User 1 is not an agent"
```

---

## 3. Customer management (`/api/customers`)

### 3.1 ✅ Agent `amy` creates customer `carol` (registered under amy) → **201**
```bash
curl -s -X POST http://localhost:8080/api/customers \
  -H "Authorization: Bearer $AMY" -H 'Content-Type: application/json' \
  -d '{"username":"carol","password":"cust123","fullName":"Carol Customer","email":"carol@example.com"}' | jq
# 201  agentId == amy's id (2)
CAROL_ID=$(curl -s http://localhost:8080/api/customers -H "Authorization: Bearer $AMY" | jq '.[0].id')
```

### 3.2 ✅ Agent `bob` creates customer `dave` → **201**
```bash
curl -s -X POST http://localhost:8080/api/customers \
  -H "Authorization: Bearer $BOB" -H 'Content-Type: application/json' \
  -d '{"username":"dave","password":"cust123","fullName":"Dave Customer","email":"dave@example.com"}' | jq
CAROL=$(token carol cust123)
DAVE=$(token dave cust123)
```

### 3.3 ✅ Agent `amy` lists customers → sees **only carol** → **200**
```bash
curl -s http://localhost:8080/api/customers -H "Authorization: Bearer $AMY" | jq '.[].username'
# "carol"   (NOT dave)
```

### 3.4 ✅ Admin lists customers → sees **all** customers → **200**
```bash
curl -s http://localhost:8080/api/customers -H "Authorization: Bearer $ADMIN" | jq '.[].username'
# "carol", "dave"
```

### 3.5 ❌ A CUSTOMER tries to create a customer → **403**
```bash
curl -i -X POST http://localhost:8080/api/customers \
  -H "Authorization: Bearer $CAROL" -H 'Content-Type: application/json' \
  -d '{"username":"eve","password":"cust123","fullName":"Eve","email":"eve@example.com"}'
# 403
```

### 3.6 ❌ A CUSTOMER tries to list customers → **403**
```bash
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8080/api/customers -H "Authorization: Bearer $CAROL"
# 403
```

### 3.7 ❌ Duplicate customer username → **409**
```bash
curl -i -X POST http://localhost:8080/api/customers \
  -H "Authorization: Bearer $AMY" -H 'Content-Type: application/json' \
  -d '{"username":"carol","password":"cust123","fullName":"Carol Again","email":"c2@example.com"}'
# 409  message: "Username 'carol' is already taken"
```

### 3.7b ❌ Duplicate customer email → **409** (email is unique)
```bash
curl -i -X POST http://localhost:8080/api/customers \
  -H "Authorization: Bearer $AMY" -H 'Content-Type: application/json' \
  -d '{"username":"carol-twin","password":"cust123","fullName":"Carol Twin","email":"carol@example.com"}'
# 409  message: "Email 'carol@example.com' is already registered"
```

### 3.8 ✅ Agent `amy` reads her own customer by id → **200**
```bash
curl -s http://localhost:8080/api/customers/$CAROL_ID -H "Authorization: Bearer $AMY" | jq
```

### 3.9 ❌ Agent `bob` reads amy's customer `carol` → **403** (ownership)
```bash
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8080/api/customers/$CAROL_ID -H "Authorization: Bearer $BOB"
# 403  (carol is not bob's customer)
```

### 3.10 ✅ Customer `carol` reads her own record by id → **200**
```bash
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8080/api/customers/$CAROL_ID -H "Authorization: Bearer $CAROL"
# 200
```

### 3.11 ❌ Read a non-existent customer id → **404**
```bash
curl -i http://localhost:8080/api/customers/999999 -H "Authorization: Bearer $ADMIN"
# 404  message: "Customer 999999 not found"
```

---

## 4. Self-service profile (`/api/users/me`)

### 4.1 ✅ `carol` reads her profile → **200**
```bash
curl -s http://localhost:8080/api/users/me -H "Authorization: Bearer $CAROL" | jq
# { "username":"carol", "role":"CUSTOMER", "agentId":2, ... }
```

### 4.2 ✅ `carol` updates name + email → **200**
```bash
curl -s -X PUT http://localhost:8080/api/users/me \
  -H "Authorization: Bearer $CAROL" -H 'Content-Type: application/json' \
  -d '{"fullName":"Carol C. Customer","email":"carol.new@example.com"}' | jq
```

### 4.3 ✅ `carol` changes her password, then re-logs in with it → **200**
```bash
curl -s -o /dev/null -w '%{http_code}\n' -X PUT http://localhost:8080/api/users/me \
  -H "Authorization: Bearer $CAROL" -H 'Content-Type: application/json' \
  -d '{"password":"newpass123"}'
# 200
token carol newpass123        # prints a fresh JWT → new password works
CAROL=$(token carol newpass123)
```

### 4.4 ✅ Agent `amy` updates her own profile → **200**
```bash
curl -s -X PUT http://localhost:8080/api/users/me \
  -H "Authorization: Bearer $AMY" -H 'Content-Type: application/json' \
  -d '{"fullName":"Amy A. Agent"}' | jq '.fullName'
```

### 4.5 ❌ Update with an invalid email → **400**
```bash
curl -i -X PUT http://localhost:8080/api/users/me \
  -H "Authorization: Bearer $CAROL" -H 'Content-Type: application/json' \
  -d '{"email":"nope"}'
# 400  fieldErrors: ["email: email must be a valid address"]
```

### 4.6 🔒 A customer **cannot escalate role**: extra/role fields are ignored
```bash
# 'role' is not part of the update DTO; sending it has no effect (Jackson ignores unknowns by default).
curl -s -X PUT http://localhost:8080/api/users/me \
  -H "Authorization: Bearer $CAROL" -H 'Content-Type: application/json' \
  -d '{"fullName":"Still A Customer","role":"ADMIN"}' | jq '.role'
# "CUSTOMER"   (unchanged — no privilege escalation)
```

---

## 5. Tickets (`/api/tickets`)

### 5.1 ✅ Customer `carol` opens a ticket → **201**
```bash
curl -s -X POST http://localhost:8080/api/tickets \
  -H "Authorization: Bearer $CAROL" -H 'Content-Type: application/json' \
  -d '{"subject":"Cannot log in","description":"I get a 500 error on login"}' | jq
# 201  { "status":"OPEN", "ownerUsername":"carol", ... }
TICKET_ID=$(curl -s http://localhost:8080/api/tickets -H "Authorization: Bearer $CAROL" | jq '.[0].id')
```

### 5.2 ✅ `carol` lists her tickets → sees her own → **200**
```bash
curl -s http://localhost:8080/api/tickets -H "Authorization: Bearer $CAROL" | jq '.[].subject'
```

### 5.3 ✅ `carol` reads her ticket by id → **200**
```bash
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8080/api/tickets/$TICKET_ID -H "Authorization: Bearer $CAROL"
# 200
```

### 5.4 ❌ An AGENT tries to open a ticket → **403** (only customers open tickets)
```bash
curl -i -X POST http://localhost:8080/api/tickets \
  -H "Authorization: Bearer $AMY" -H 'Content-Type: application/json' \
  -d '{"subject":"Agent ticket","description":"should not be allowed"}'
# 403  message: "Only customers can open tickets"
```

### 5.5 ✅ Owning agent `amy` sees her customers' tickets → **200**
```bash
curl -s http://localhost:8080/api/tickets -H "Authorization: Bearer $AMY" | jq '.[].ownerUsername'
# "carol"
```

### 5.6 ❌ Other agent `bob` does **not** see carol's ticket → empty list → **200**
```bash
curl -s http://localhost:8080/api/tickets -H "Authorization: Bearer $BOB" | jq
# []   (bob only sees his own customers' tickets; dave has none yet)
```

### 5.7 ❌ `bob` reads carol's ticket by id → **403** (ownership)
```bash
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8080/api/tickets/$TICKET_ID -H "Authorization: Bearer $BOB"
# 403
```

### 5.8 ✅ Admin lists tickets → sees **all** → **200**
```bash
curl -s http://localhost:8080/api/tickets -H "Authorization: Bearer $ADMIN" | jq 'length'
```

### 5.9 ✅ Agent filters by status → **200**
```bash
curl -s "http://localhost:8080/api/tickets?status=OPEN"   -H "Authorization: Bearer $AMY" | jq 'length'  # >= 1
curl -s "http://localhost:8080/api/tickets?status=CLOSED" -H "Authorization: Bearer $AMY" | jq 'length'  # 0
```

### 5.10 ❌ Open a ticket with a missing subject → **400**
```bash
curl -i -X POST http://localhost:8080/api/tickets \
  -H "Authorization: Bearer $CAROL" -H 'Content-Type: application/json' \
  -d '{"description":"no subject here"}'
# 400  fieldErrors: ["subject: subject is required"]
```

### 5.11 ❌ Read a non-existent ticket id → **404**
```bash
curl -i http://localhost:8080/api/tickets/999999 -H "Authorization: Bearer $ADMIN"
# 404  message: "Ticket 999999 not found"
```

### 5.12 ❌ Invalid `status` filter value → **400**
```bash
curl -i "http://localhost:8080/api/tickets?status=BOGUS" -H "Authorization: Bearer $AMY"
# 400  message: "Invalid value 'BOGUS' for parameter 'status'"
```

### 5.13 ❌ Malformed JSON body → **400**
```bash
curl -i -X POST http://localhost:8080/api/tickets \
  -H "Authorization: Bearer $CAROL" -H 'Content-Type: application/json' \
  -d '{ this is not json'
# 400  message: "Malformed or unreadable request body"
```

### 5.14 ✅ ADMIN opens a ticket on behalf of a customer → **201** ("admin can do anything")
```bash
# A ticket is owned by a CUSTOMER, so admin names the customer (path id) rather than owning it.
curl -i -X POST http://localhost:8080/api/admin/customers/$CAROL_ID/tickets \
  -H "Authorization: Bearer $ADMIN" -H 'Content-Type: application/json' \
  -d '{"subject":"Raised by admin","description":"Opened on behalf of the customer"}'
# 201  ownerUsername == carol  (the ticket is owned by the customer, not the admin)

# The customer-only endpoint stays closed to admin — admin uses the /api/admin path above:
curl -s -o /dev/null -w '%{http_code}\n' -X POST http://localhost:8080/api/tickets \
  -H "Authorization: Bearer $ADMIN" -H 'Content-Type: application/json' \
  -d '{"subject":"x","description":"y"}'
# 403
```

---

## 6. Error status codes (correct status per error type)

The API returns the precise status for each error type rather than a blanket 500.

### 6.1 ❌ Unsupported HTTP method → **405**
```bash
curl -i -X DELETE http://localhost:8080/api/users/me -H "Authorization: Bearer $CAROL"
# 405  (only GET/PUT are mapped on /api/users/me)
```

### 6.2 ❌ Unsupported media type → **415**
```bash
curl -i -X POST http://localhost:8080/api/tickets \
  -H "Authorization: Bearer $CAROL" -H 'Content-Type: text/plain' --data 'hello'
# 415
```

### 6.3 ❌ Unknown path (authenticated) → **404**
```bash
curl -i http://localhost:8080/api/does-not-exist -H "Authorization: Bearer $ADMIN"
# 404
```

---

## 7. Authorization matrix (quick reference)

What each role should get on the key endpoints — useful as a checklist while demoing:

| Endpoint | ADMIN | AGENT | CUSTOMER | anonymous |
|----------|:-----:|:-----:|:--------:|:---------:|
| `POST /api/auth/login` | 200 | 200 | 200 | 200 |
| `POST /api/admin/users` | 201 | **403** | **403** | **401** |
| `POST /api/admin/customers/{id}/tickets` | 201 | **403** | **403** | **401** |
| `POST /api/customers` | 201 | 201 | **403** | **401** |
| `GET /api/customers` | 200 (all) | 200 (own) | **403** | **401** |
| `GET /api/customers/{id}` | 200 | 200 own / **403** other | 200 self / **403** other | **401** |
| `GET/PUT /api/users/me` | 200 | 200 | 200 | **401** |
| `POST /api/tickets` | **403** | **403** | 201 | **401** |
| `GET /api/tickets` | 200 (all) | 200 (own customers') | 200 (own) | **401** |
| `GET /api/tickets/{id}` | 200 | 200 own / **403** other | 200 own / **403** other | **401** |

(ADMIN gets 403 on `POST /api/tickets` because tickets are owned by a CUSTOMER — admins aren't
customers; admin opens tickets via `POST /api/admin/customers/{id}/tickets` instead.)

---

## 8. PowerShell note (Windows)

PowerShell aliases `curl` to `Invoke-WebRequest`, which parses flags differently. Either:

- Use **`curl.exe`** (the real curl) with the exact commands above, or
- Use native cmdlets, e.g.:

```powershell
$admin = (Invoke-RestMethod -Method Post http://localhost:8080/api/auth/login `
  -ContentType 'application/json' -Body '{"username":"admin","password":"admin"}').accessToken

Invoke-RestMethod http://localhost:8080/api/customers -Headers @{ Authorization = "Bearer $admin" }

# For expected-error cases, capture the status code:
try { Invoke-WebRequest http://localhost:8080/api/users/me } catch { $_.Exception.Response.StatusCode.value__ }
```

The simplest cross-platform path is to run the `bash` scenarios in **Git Bash** or **WSL**.
