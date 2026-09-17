# Phase 5 — Database-Backed Users
### Branch: `phase-5-db-users`

This phase replaced the hardcoded in-memory users (Phase 3) with real accounts
stored in Postgres, added self-registration, and linked every `Task` to the
`User` who owns it — the foundation Phase 6's ownership checks depend on.

---

## Step 1 — `User` entity + repository

**What we built:**
- `Role` enum (`USER`, `ADMIN`)
- `User` JPA entity → maps to a `users` table (`id`, `username`, `password`, `role`)
- `UserRepository` with a derived query method `findByUsername(String)`

**Why:** In-memory users can't persist or scale. This gives us a real table
to authenticate against and register into.

```mermaid
erDiagram
    USERS {
        bigint id PK
        varchar username UK
        varchar password
        varchar role
    }
```

---

## Step 2 — Custom `UserDetailsService`

**What we built:**
- `CustomUserDetailsService implements UserDetailsService`, using `UserRepository`
- Removed the old `InMemoryUserDetailsManager` bean from `SecurityConfig`
  (kept the `PasswordEncoder` bean)

**Why:** Spring Security needs to know *how* to look up a user by username.
This class is the bridge between Spring Security's authentication machinery
and our own `users` table.

**Important distinction:** this class only **fetches** the user. It never
compares passwords itself — that's Spring Security's `DaoAuthenticationProvider`,
using the `PasswordEncoder` bean.

````mermaid
User Login using password and username
CustomUserDetailsService  have method  loadUserByUsername which will fetch user details from repo using username.
User user = userRepository.findByUsername(username)

Then this user will be handover to DaoAuthenticationProvider which compare password

````


## Step 3 — Registration endpoint

**What we built:**
- `RegisterRequest` / `RegisterResponse` DTOs
- `AuthService.register(...)` — checks for duplicate username, hashes the
  raw password, **hardcodes `Role.USER`** (never client-supplied), saves
- `AuthController` → `POST /auth/register`
- Added `/auth/register` to the `permitAll()` matcher list (has to be public —
  you can't require login to create your first login)

**Why `Role.USER` is hardcoded:** letting a public endpoint accept an
arbitrary role would let anyone register themselves as `ADMIN`. Role
elevation has to happen some other way (manual DB edit, or an admin-only
endpoint later) — never from the client's own registration request.


---

## Step 4 — Link `Task` to `User` (ownership)

**What we built:**
- `Task.owner` — `@ManyToOne(fetch = LAZY)` to `User`, via `owner_id`
  foreign key, `nullable = false`
- `TaskService.createTask()` now pulls the logged-in user from
  `SecurityContextHolder`, looks up their full `User` entity, and sets
  `owner` — **never** from client input
- `data.sql` updated: users inserted first, tasks reference `owner_id` via
  a subquery on username (since IDs regenerate every boot)

**Why owner is server-derived, not client-supplied:** if the client could
say "create this task and set owner = someone else," ownership would be
meaningless as a security boundary. The server already knows who's
authenticated — that's the only trustworthy source.

```mermaid
UerDiagram
    USERS ||--o{ TASKS : owns
    USERS {
        bigint id PK
        varchar username UK
        varchar password
        varchar role
    }
    TASKS {
        bigint id PK
        varchar title
        varchar description
        boolean completed
        timestamp created_at
        bigint owner_id FK
    }
```


---

## End-to-end summary — Phase 5 in one picture

```mermaid
flowchart TD
    A[Client registers] -->|POST /auth/register| B[AuthController → AuthService]
    B -->|hash password, Role.USER| C[(users table)]

    D[Client logs in via Basic Auth] -->|any request| E[Basic Auth Filter]
    E --> F[CustomUserDetailsService]
    F -->|findByUsername| C
    F --> G[PasswordEncoder.matches]
    G -->|success| H[Authenticated as User X]

    H -->|POST /tasks| I[TaskService.createTask]
    I -->|owner = current user from SecurityContext| J[(tasks table, owner_id FK)]

    H -->|GET /admin/tasks| K{Role check}
    K -->|ROLE_ADMIN| L[200 OK]
    K -->|ROLE_USER| M[403 Forbidden]
```

---

## What's still open / deferred to later phases

- **Ownership is stored but not yet enforced** on update/delete — right now
  any authenticated user can still edit/delete *any* task, regardless of
  `owner`. That enforcement is exactly what **Phase 6**'s `@PreAuthorize`
  rules add.
- Duplicate-username registration currently returns a raw `500` (the
  `IllegalArgumentException` isn't caught by `GlobalExceptionHandler` yet) —
  a known gap, deferred alongside **Phase 7**'s broader error-handling work.
- All 6 seeded tasks are owned by `Ajay` — deliberate, so Phase 6 testing
  has a clear "these are Ajay's, prove others can't touch them" baseline.

---

## Verified test flow (as run in Postman)

1. `POST /auth/register` → new user created, `201`, no password in response
2. New user logs in immediately via Basic Auth → `200` on `GET /tasks/{id}`
3. Duplicate username → rejected (currently `500`, known gap)
4. New user hits `/admin/tasks` → `403` (self-registration can't grant ADMIN)
5. `POST /tasks` as any user → `owner_id` in DB correctly matches whoever
   was authenticated, never client-supplied
