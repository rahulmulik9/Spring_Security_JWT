# Spring Security — Step by Step Learning Plan
### Project: Task Management App
### Stack: Spring Boot, Maven, PostgreSQL (Docker)

---

## Teaching Style

Every substep follows this exact format, in this order:

1. **Need** — One or two concrete sentences on what actually breaks, or what gap exists, right now without this piece. Specific, not generic best-practice language.
2. **Goal for this step** — What "done" looks like for just this piece. Explicitly states what is *not* being done yet (deferred to a later substep).
3. *(Stop and confirm before writing code — unless told to proceed straight through.)*
4. **Code changes** — Full files (not diffs) where short enough to stay readable, matching existing naming/style. Numbered if more than one file.
5. **Flow** — Plain-language walkthrough of how the piece moves through the system end to end (what calls what, what gets saved where).
6. **How to test and expected output** — Concrete steps to verify behavior: API calls, expected responses/log lines/DB rows, before/after values. Not "it compiles" — an actual behavioral check.

Main Steps are confirmed before any substep work begins. Substep tables (with a **Need** column) are confirmed before any code is written for that Main Step.

---

## PHASE 1 — No Security Baseline
*Branch: `phase-1-no-security`*

| Main Step | What it achieves |
|---|---|
| 1 — Project skeleton | App boots, Postgres reachable via Docker, package structure set |
| 2 — Task entity + persistence | `Task` table exists, repository can save/fetch |
| 3 — Task CRUD endpoints | Open REST endpoints for create/read/update/delete |
| 4 — Verify open access | Prove anyone can hit every endpoint (the "before" picture) |

### Step 1 — Project skeleton
| Substep | Need |
|---|---|
| 1.1 — Generate Maven project + dependencies | Without this there's no app to run at all |
| 1.2 — Docker Compose for Postgres | Without a running DB, JPA can't connect — app won't boot |
| 1.3 — `application.properties` DB config | App has no idea how to reach the Postgres container yet |
| 1.4 — Confirm app boots cleanly | Catch connection/config errors now, not buried under later features |

### Step 2 — Task entity + persistence
| Substep | Need |
|---|---|
| 2.1 — `Task` entity | No table = nothing to store; this is the core domain object |
| 2.2 — `TaskRepository` (JPA) | Without this, there's no way to talk to the DB from code |
| 2.3 — Confirm table auto-created | If Hibernate DDL is misconfigured, every later step fails silently |

### Step 3 — Task CRUD endpoints
| Substep | Need |
|---|---|
| 3.1 — DTOs for request/response | Exposing the entity directly leaks internal fields and breaks later when `owner` is added |
| 3.2 — `TaskService` (business logic layer) | Without a service layer, controller and persistence logic get tangled — hard to add auth checks later |
| 3.3 — `TaskController` (CRUD endpoints) | This is the actual usable API surface |

### Step 4 — Verify open access
| Substep | Need |
|---|---|
| 4.1 — Postman collection, manual test pass | Need a concrete "yes, this works and is open" baseline before adding any security — otherwise we can't tell what security *changed* |

---

## PHASE 2 — Default Spring Security (Auto-Config)
*Branch: `phase-2-default-security`*

| Main Step | What it achieves |
|---|---|
| 1 — Add security starter | See Spring's zero-config default behavior |
| 2 — Observe and document the change | Understand exactly what auto-config did, before overriding any of it |

### Step 1 — Add security starter
| Substep | Need |
|---|---|
| 1.1 — Add `spring-boot-starter-security` dependency | This alone changes app behavior — need to see it happen |
| 1.2 — Boot app, capture generated password from logs | Without seeing this, the "magic" stays invisible |

### Step 2 — Observe and document the change
| Substep | Need |
|---|---|
| 2.1 — Hit `/tasks` with no credentials → confirm 401/redirect | Confirms the filter chain is active with zero code written |
| 2.2 — Hit `/tasks` with generated password → confirm success | Confirms default `UserDetailsService` + form login works end to end |

---

## PHASE 3 — In-Memory Users + HTTP Basic Auth
*Branch: `phase-3-inmemory-basic-auth`*

| Main Step | What it achieves |
|---|---|
| 1 — Define in-memory users | Replace the throwaway generated password with real (if fake) accounts |
| 2 — Custom `SecurityFilterChain` | Replace form-login default with HTTP Basic, suited for API clients |
| 3 — Verify via Postman | Confirm Basic Auth actually gates access |

### Step 1 — Define in-memory users
| Substep | Need |
|---|---|
| 1.1 — `PasswordEncoder` bean (BCrypt) | Storing/comparing raw passwords is wrong even in a toy project — build the right habit now |
| 1.2 — `InMemoryUserDetailsManager` with 2–3 users + roles | Need actual distinguishable identities (e.g. USER vs ADMIN) for Phase 4 |

### Step 2 — Custom SecurityFilterChain
| Substep | Need |
|---|---|
| 2.1 — `SecurityFilterChain` bean, enable HTTP Basic | Form login returns HTML; Postman/API clients need a header-based scheme |
| 2.2 — Disable CSRF (with explanation of why, for stateless APIs) | Default CSRF protection blocks POST/PUT/DELETE from Postman without a token |

### Step 3 — Verify via Postman
| Substep | Need |
|---|---|
| 3.1 — Test each user via Postman Basic Auth | Confirms real credential-based access replaces the generated password |

---

## PHASE 4 — Role-Based Authorization
*Branch: `phase-4-role-based-authz`*

| Main Step | What it achieves |
|---|---|
| 1 — Categorize endpoints by access level | Decide what's public vs USER vs ADMIN before writing rules |
| 2 — Configure `authorizeHttpRequests` | Enforce those rules at the filter-chain level |
| 3 — Verify 401 vs 403 distinctly | Confirm the two failure modes are actually different |

### Step 1 — Categorize endpoints by access level
| Substep | Need |
|---|---|
| 1.1 — Add `GET /tasks/public` sample endpoint | Need at least one genuinely open route to prove rules aren't all-or-nothing |
| 1.2 — Add `GET /admin/tasks` (all tasks) endpoint | Need an ADMIN-only route to demonstrate role separation |

### Step 2 — Configure authorizeHttpRequests
| Substep | Need |
|---|---|
| 2.1 — Matcher rules in `SecurityFilterChain`, correct ordering | Wrong order = more specific rules silently ignored (classic bug) |

### Step 3 — Verify 401 vs 403 distinctly
| Substep | Need |
|---|---|
| 3.1 — Test unauthenticated request → 401 | Confirms "who are you" failure path |
| 3.2 — Test USER hitting `/admin/tasks` → 403 | Confirms "you are known, but not allowed" failure path — the two are easy to conflate |

---

## PHASE 5 — Database-Backed Users
*Branch: `phase-5-db-users`*

| Main Step | What it achieves |
|---|---|
| 1 — `User` entity + repository | Real accounts persisted in Postgres, not hardcoded in Java |
| 2 — Custom `UserDetailsService` | Bridge Spring Security to your own `User` table |
| 3 — Registration endpoint | Users can self-register with hashed passwords |
| 4 — Link `Task` to `User` (ownership) | Enables ownership checks in Phase 6 — nothing works there without this |

### Step 1 — User entity + repository
| Substep | Need |
|---|---|
| 1.1 — `User` entity (username, password hash, role) | In-memory users can't scale or persist — needed for real registration |
| 1.2 — `UserRepository` | No DB access = no way to look up a user at login time |

### Step 2 — Custom UserDetailsService
| Substep | Need |
|---|---|
| 2.1 — Implement `UserDetailsService` using `UserRepository` | Spring Security still thinks in terms of in-memory users otherwise — this is the actual bridge |
| 2.2 — Wire it into `SecurityFilterChain`, remove in-memory users | Old and new user sources can't coexist without confusion about which wins |

### Step 3 — Registration endpoint
| Substep | Need |
|---|---|
| 3.1 — `POST /auth/register` (hash password before save) | Without this, the only way to create a user is manual SQL — not realistic |
| 3.2 — Test registration + login via Postman | Confirms the full loop: register → stored hashed → login succeeds |

### Step 4 — Link Task to User (ownership)
| Substep | Need |
|---|---|
| 4.1 — Add `owner` (`@ManyToOne User`) to `Task` | Right now tasks have no owner — "your tasks" is meaningless without this |
| 4.2 — Auto-assign owner on task creation from logged-in principal | Without this, owner would have to be manually supplied — insecure and wrong |
| 4.3 — Migration/verify existing test data compatible | Existing rows from Phase 1–4 testing will have `null` owner and could break queries |

---

## PHASE 6 — Method-Level Security (Ownership Checks)
*Branch: `phase-6-method-security`*

| Main Step | What it achieves |
|---|---|
| 1 — Enable method security | Turns on `@PreAuthorize` support |
| 2 — Admin-only method rule | ADMIN-only service methods enforced at the service layer, not just URL |
| 3 — Ownership rule on update/delete | USER can only touch their own tasks |
| 4 — Verify cross-user access is blocked | Prove the rule actually stops User A from touching User B's data |

### Step 1 — Enable method security
| Substep | Need |
|---|---|
| 1.1 — `@EnableMethodSecurity` on config class | `@PreAuthorize` annotations are silently ignored without this |

### Step 2 — Admin-only method rule
| Substep | Need |
|---|---|
| 2.1 — `@PreAuthorize("hasRole('ADMIN')")` on "get all tasks" service method | URL-based rule from Phase 4 doesn't protect the method if called internally from elsewhere later |

### Step 3 — Ownership rule on update/delete
| Substep | Need |
|---|---|
| 3.1 — `@PreAuthorize` expression checking task owner vs `authentication.name` | Without this, any logged-in USER can edit/delete *any* task — Phase 4's role check alone doesn't stop this |

### Step 4 — Verify cross-user access is blocked
| Substep | Need |
|---|---|
| 4.1 — Test: User A tries deleting User B's task → expect 403 | This is the actual security guarantee of the whole phase — must be proven, not assumed |
| 4.2 — Test: ADMIN deletes any user's task → expect success | Confirms the override path also works, not just the block path |

---

## PHASE 7 — Custom Security Responses
*Branch: `phase-7-custom-error-handling`*

| Main Step | What it achieves |
|---|---|
| 1 — Custom 401 handler | Replace default login-page/HTML response with JSON |
| 2 — Custom 403 handler | Same for access-denied cases |
| 3 — Verify consistent error shape | Confirm both match a single agreed JSON structure |

### Step 1 — Custom 401 handler
| Substep | Need |
|---|---|
| 1.1 — Implement `AuthenticationEntryPoint` | Right now, unauthenticated API calls get an HTML login page — unusable for a REST client |
| 1.2 — Wire into `SecurityFilterChain` | Without registering it, Spring keeps using the default entry point |

### Step 2 — Custom 403 handler
| Substep | Need |
|---|---|
| 2.1 — Implement `AccessDeniedHandler` | Right now, a 403 might come back as a blank page or inconsistent body |
| 2.2 — Wire into `SecurityFilterChain` | Same reason as 1.2 — must be explicitly registered |

### Step 3 — Verify consistent error shape
| Substep | Need |
|---|---|
| 3.1 — Test both paths, confirm identical JSON shape (`code`, `message`, `timestamp`, `path`) | An API with two different error formats for 401 vs 403 is a real bug class in production APIs |

---

## PHASE 8 — Why Sessions Don't Scale (Conceptual Bridge)
*Branch: `phase-8-stateless-motivation`*

| Main Step | What it achieves |
|---|---|
| 1 — Switch to stateless session policy | See what changes when Spring stops issuing session cookies |
| 2 — Confirm Basic Auth still works, understand why it's still not good enough | Sets up the actual motivation for JWT |

### Step 1 — Switch to stateless session policy
| Substep | Need |
|---|---|
| 1.1 — `sessionManagement(STATELESS)` in filter chain | Without this, Spring silently uses session cookies, hiding a real scalability problem |
| 1.2 — Confirm no `JSESSIONID` issued in response headers | Need to actually observe the change, not just trust the config did something |

### Step 2 — Confirm Basic Auth still works, understand why it's still not enough
| Substep | Need |
|---|---|
| 2.1 — Retest Basic Auth flows from Phase 3 — confirm still functional | Proves stateless mode doesn't break existing auth (Basic sends creds every request already) |
| 2.2 — Discussion/notes: why sending raw password every request is still bad | This is the actual conceptual bridge — without articulating why, JWT in Phase 9 feels arbitrary |

---

## PHASE 9 — JWT Authentication
*Branch: `phase-9-jwt-auth`*

| Main Step | What it achieves |
|---|---|
| 1 — JWT utility (generate/validate tokens) | Core token logic, independent of Spring Security wiring |
| 2 — Login endpoint issues JWT | Replaces Basic Auth as the credential-exchange mechanism |
| 3 — JWT filter validates incoming requests | Replaces Basic Auth as the per-request auth mechanism |
| 4 — Remove Basic Auth, verify JWT-only flow | Confirm the cutover is complete and clean |

### Step 1 — JWT utility
| Substep | Need |
|---|---|
| 1.1 — Add JWT library dependency (jjwt) | No token generation/parsing is possible without a library |
| 1.2 — `JwtUtil` — generate token (username + role claims, expiry) | Central place to create tokens consistently |
| 1.3 — `JwtUtil` — validate/parse token | Same reasoning, for the reverse direction |

### Step 2 — Login endpoint issues JWT
| Substep | Need |
|---|---|
| 2.1 — `POST /auth/login` — authenticate via `AuthenticationManager`, return JWT | Currently there's no way to exchange credentials for a token — Basic Auth requires sending password every request, which JWT is meant to avoid |

### Step 3 — JWT filter validates incoming requests
| Substep | Need |
|---|---|
| 3.1 — `JwtAuthenticationFilter extends OncePerRequestFilter` | Without this, incoming `Authorization: Bearer` headers are never read or validated |
| 3.2 — Set `SecurityContextHolder` from valid token | Without this, even a valid token wouldn't actually authenticate the request — downstream `@PreAuthorize` checks would fail |
| 3.3 — Register filter in `SecurityFilterChain` (before `UsernamePasswordAuthenticationFilter`) | Filter order matters — wrong placement means it never runs, or runs too late |

### Step 4 — Remove Basic Auth, verify JWT-only flow
| Substep | Need |
|---|---|
| 4.1 — Remove `httpBasic()` config | Leaving both active is confusing and doesn't match real-world APIs, which pick one |
| 4.2 — Full Postman flow: login → copy token → call `/tasks` with Bearer header | Proves the entire JWT loop works end to end, not just in isolation |
| 4.3 — Test expired/invalid token → expect 401 | This is the actual failure mode JWT introduces — must be explicitly proven, not assumed |

---

## PHASE 10 — Refresh Tokens + Full JWT Lifecycle
*Branch: `phase-10-jwt-refresh`*

| Main Step | What it achieves |
|---|---|
| 1 — Add refresh token generation | Separate short-lived access token from long-lived refresh token |
| 2 — Refresh endpoint | Client can get a new access token without re-entering credentials |
| 3 — Basic logout/invalidation | Some way to kill a token before its natural expiry |
| 4 — Full lifecycle test | Confirm the entire login→expire→refresh→continue flow works |

### Step 1 — Add refresh token generation
| Substep | Need |
|---|---|
| 1.1 — Shorten access token expiry (e.g. 15 min) | A long-lived access token defeats the purpose of having a refresh token at all |
| 1.2 — Generate separate refresh token (7 day expiry, different claim/type) | Without a distinct token type, nothing stops a refresh token being used as an access token |

### Step 2 — Refresh endpoint
| Substep | Need |
|---|---|
| 2.1 — `POST /auth/refresh` — validate refresh token, issue new access token | Without this, an expired access token means forced re-login — bad UX and the whole reason refresh tokens exist |
| 2.2 — Reject expired/invalid refresh tokens → 401 | Otherwise refresh becomes a way to bypass expiry entirely |

### Step 3 — Basic logout/invalidation
| Substep | Need |
|---|---|
| 3.1 — In-memory blacklist/store for revoked tokens | Without any invalidation mechanism, "logout" is fake — token stays valid until natural expiry |
| 3.2 — `POST /auth/logout` — add current token to blacklist | Need an actual user-facing way to trigger revocation |
| 3.3 — Filter checks blacklist before accepting token | Revocation is meaningless unless it's actually checked on every request |

### Step 4 — Full lifecycle test
| Substep | Need |
|---|---|
| 4.1 — Full Postman flow: register → login → use access token → wait for expiry → refresh → use new token | This is the complete real-world behavior this phase promises — must be proven end to end |
| 4.2 — Test: logout, then reuse old token → expect 401 | Confirms revocation actually works, not just that the endpoint returns 200 |

---

## Notes

- Each phase gets its own git branch, named as shown under each phase header.
- Main Steps are confirmed before substep breakdown begins; substeps are confirmed before code is written.
- Substeps are never batched — one substep, one full Need → Goal → Code → Flow → Test cycle, unless explicitly told to proceed straight through multiple.
