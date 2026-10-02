# UniRoll

[![CI](https://github.com/mzaidaqil/uniroll-web/actions/workflows/ci.yml/badge.svg)](https://github.com/mzaidaqil/uniroll-web/actions/workflows/ci.yml)

A full-stack course enrollment platform: lecturers create and manage subjects, students browse, enroll in and drop them.

UniRoll started as a university group assignment (an Android app with a local Room/SQLite database). This repository is a solo rebuild as a production-style web application: a React frontend, a secured Spring Boot REST API, a PostgreSQL database and automated tests.

**Live demo:** <https://uniroll-frontend.onrender.com> · API docs: <https://uniroll-backend.onrender.com/swagger-ui.html>

> Hosted on free tiers: the API sleeps after 15 minutes without traffic, so the first request after a pause can take up to a minute while it wakes up.

## Features

- **Accounts and roles**: register and log in as a `LECTURER` or `STUDENT`; passwords are hashed with BCrypt and every request is authenticated with a signed JWT.
- **Lecturers** create, update and delete their own subjects and see the class list of each one.
- **Students** search and page through subjects, enroll, drop, and see their timetable with total credit hours.
- **Business rules** enforced on every enrollment:
  - a subject cannot go over its capacity
  - a student cannot exceed 20 credit hours
  - a student cannot enroll in the same subject twice
  - only the lecturer who owns a subject can change it or see its students
- **Interactive API docs** with Swagger UI.

## Tech stack

| Area | Technology |
|---|---|
| Frontend | React 19, TypeScript, Vite, React Router, TanStack Query, Tailwind CSS |
| Backend | Java 21, Spring Boot 4 (Web MVC, Data JPA, Validation, Actuator) |
| Security | Spring Security, OAuth2 Resource Server (JWT, HS256), BCrypt |
| Database | PostgreSQL 18, Flyway migrations, Hibernate |
| API docs | springdoc-openapi (Swagger UI) |
| Testing | JUnit 5, Mockito, `@WebMvcTest`, `@DataJpaTest`, Testcontainers, JaCoCo |
| Build and run | Maven (wrapper included), npm, Docker multi-stage builds, Docker Compose, nginx |
| CI/CD and hosting | GitHub Actions, Render (API container + static site), Neon (serverless PostgreSQL) |

## API

All endpoints except register and login need an `Authorization: Bearer <token>` header.

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Create an account |
| POST | `/api/auth/login` | Public | Get a JWT |
| GET | `/api/users/me` | Logged in | Current user |
| GET | `/api/subjects?search=&page=&size=&mine=` | Logged in | Search subjects (paged, sorted by code); `mine=true` returns only your own |
| GET | `/api/subjects/{id}` | Logged in | One subject with its enrolled count |
| POST | `/api/subjects` | Lecturer | Create a subject |
| PUT | `/api/subjects/{id}` | Owning lecturer | Update a subject |
| DELETE | `/api/subjects/{id}` | Owning lecturer | Delete a subject and its enrollments |
| GET | `/api/subjects/{id}/students` | Owning lecturer | Class list |
| POST | `/api/enrollments` | Student | Enroll in a subject |
| DELETE | `/api/enrollments/{subjectId}` | Student | Drop a subject |
| GET | `/api/enrollments/me` | Student | Timetable and total credit hours |

Errors use the standard [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) problem details format. Validation errors list each invalid field:

```json
{
  "status": 400,
  "title": "Bad Request",
  "detail": "Validation failed",
  "errors": { "creditHours": "must be less than or equal to 6" }
}
```

## Design highlights

- **Layered, package-by-feature structure.** Each feature (`auth`, `user`, `subject`, `enrollment`) has its own controller, service, repository and request/response records. Entities are never returned from the API.
- **Rules enforced twice.** The service layer checks every rule and returns a clear error; the database enforces the same rules with `UNIQUE`, `CHECK` and foreign-key constraints, so bad data can't get in even if the Java code has a bug.
- **Safe under concurrent requests.** Enrolling locks the student row and then the subject row (`SELECT ... FOR UPDATE`, always in that order to avoid deadlocks). A test fires 8 simultaneous enrollments at a 1-seat subject and checks exactly one succeeds; with the locks removed, all 8 get in.
- **Login doesn't leak which emails exist.** Wrong password and unknown email return the same 401, and both run a BCrypt check so their response times match.
- **No N+1 queries.** Subject lists load lecturers with an entity graph and seat counts with a single grouped query per page.
- **Schema under version control.** Flyway owns the schema; Hibernate only validates that the entities match it.
- **Server state handled by TanStack Query.** The frontend caches API data and refetches it automatically after a change (e.g. enrolling updates seat counts and credit hours) instead of hand-written loading logic.

## Project structure

```
frontend/src/
├── api/          typed API client (adds the JWT, turns errors into ApiError)
├── auth/         logged-in user context and role-based route guards
├── components/   layout, form fields, pagination, shared styles
└── pages/        login, register, student and lecturer pages

backend/src/main/java/org/zayed/unirollweb/
├── auth/         register, login, JWT creation
├── user/         User entity, /api/users/me
├── subject/      subject CRUD, search, owner checks
├── enrollment/   enroll, drop, timetable, class list, locking
└── common/       security and JWT config, error handling, OpenAPI config
backend/src/main/resources/db/migration/   Flyway SQL migrations
```

## Deployment

```
Browser ──► Render Static Site (React build on a CDN)
               │  rewrite /api/* (same origin, so no CORS)
               ▼
            Render Web Service (Spring Boot container from backend/Dockerfile)
               │  JDBC over SSL
               ▼
            Neon PostgreSQL (Singapore)
```

- Both services are defined as code in [`render.yaml`](render.yaml) (a Render Blueprint) and redeploy automatically on every push to `main`.
- The database password is entered once in the Render dashboard and the JWT secret is generated by Render; neither is stored in the repository.
- Flyway applies migrations to the production database on startup.
- GitHub Actions ([`ci.yml`](.github/workflows/ci.yml)) runs on every push: backend tests with coverage, frontend lint and build, and both Docker image builds.

## Running with Docker

The quickest way to run everything. Only Docker is needed:

```bash
docker compose up --build
```

| URL | What |
|---|---|
| <http://localhost:3000> | The app (nginx serving React, forwarding `/api` to the backend) |
| <http://localhost:8081/swagger-ui.html> | Swagger UI |

Compose starts PostgreSQL, waits until it accepts connections, then starts the backend (Flyway creates the tables) and the frontend. Stop with `docker compose down`; add `-v` to also delete the database volume.

Both images use multi-stage builds: the backend is compiled with the JDK and Maven but runs on a JRE-only image as a non-root user; the frontend is built with Node and served by nginx, which also handles React Router URLs and long-term caching of hashed assets.

## Running locally (for development)

**Requirements:** Java 21, Node.js 20+, and Docker (for the database and the tests).

1. Start PostgreSQL:

   ```bash
   docker run --name uniroll-db -d -p 5432:5432 \
     -e POSTGRES_DB=uniroll -e POSTGRES_USER=uniroll -e POSTGRES_PASSWORD=uniroll \
     postgres:18-alpine
   ```

2. Start the API (Flyway creates the tables on first run):

   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```

3. Start the frontend in a second terminal:

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

   Open <http://localhost:5173> and register as a student or lecturer. In development, Vite forwards `/api` requests to the backend on port 8080.

4. The API can also be explored in Swagger UI at <http://localhost:8080/swagger-ui.html>: register, log in, click **Authorize** and paste the `accessToken`.

### Configuration

Every setting has a local default, and production overrides it with an environment variable.

| Variable | Default | Purpose |
|---|---|---|
| `PORT` | `8080` | HTTP port |
| `DB_URL` | `jdbc:postgresql://localhost:5432/uniroll` | Database URL |
| `DB_USERNAME` / `DB_PASSWORD` | `uniroll` / `uniroll` | Database credentials |
| `JWT_SECRET` | dev-only value | JWT signing key, at least 32 bytes. **Must be set in production** |
| `JWT_EXPIRY` | `1h` | Token lifetime |

## Tests

```bash
cd backend
./mvnw test
```

Docker must be running: database tests start a throwaway PostgreSQL container with Testcontainers, so they never touch your local data.

| Layer | What it checks | Tests |
|---|---|---|
| Unit (JUnit 5 + Mockito) | Service rules in isolation, e.g. credit-hour limit, lock order, owner checks | 29 |
| Web (`@WebMvcTest`) | Security rules, validation, status codes and JSON | 22 |
| Repository (`@DataJpaTest` + Testcontainers) | Database constraints and queries on real PostgreSQL | 14 |
| Integration (`@SpringBootTest` + Testcontainers) | Full HTTP flows, including concurrent enrollment | 28 |

**93 backend tests**, with **98% line** and **93% branch** coverage measured by JaCoCo (no classes excluded). The HTML report is written to `backend/target/site/jacoco/index.html`.

## Roadmap

- [x] Database schema, entities and repositories
- [x] JWT authentication and role-based access
- [x] Subject and enrollment API with business rules
- [x] Unit, web-layer and integration tests with coverage
- [x] React + TypeScript frontend
- [x] Docker images and Docker Compose
- [x] GitHub Actions CI
- [x] Deployment (Render + Neon)
