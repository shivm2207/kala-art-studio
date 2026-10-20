# Kala — Art Studio Website

Kala is a full-stack art studio application with two roles: **customer** and **studio admin**. Customers can buy one-of-a-kind paintings, book a wall-painting date, and request custom pottery. Studio admins manage paintings, bookings, pottery requests, and orders. Real payments are not included.

## Stack

- **Frontend:** React 19, Vite 8, JavaScript, CSS
- **Backend:** Java 21, Spring Boot 3.5.16, Spring Security, Spring Data JPA, Maven wrapper
- **Database:** PostgreSQL 17, Flyway migrations
- **Authentication:** Session-based login, BCrypt password hashing, and Spring Security role checks

## Features

**Customer:** Register/login, browse paintings, buy available paintings, cancel purchases, request and cancel eligible wall bookings, request/edit/cancel eligible pottery requests, and view their own records.

**Studio admin:** View orders, add/edit available paintings, view and confirm/decline wall bookings, accept/decline pottery requests, and manage studio catalogue information.

The server enforces role permissions and record ownership. A painting can have only one active purchase; a wall date can have only one active booking.

## Requirements

Install Java 21, PostgreSQL 17, Node.js compatible with Vite 8, and npm. Maven does not need to be installed separately because the backend includes a Maven wrapper. Commands below are for Windows Command Prompt.

## Local setup

1. Start PostgreSQL. Create a database named `kala` if it does not already exist. **Do not drop an existing database.** You can create it through pgAdmin or PostgreSQL CLI.
2. Open a Command Prompt in `backend` and set environment variables for this terminal session:

   ```bat
   cd backend
   set "DB_URL=jdbc:postgresql://localhost:5432/kala"
   set "DB_USERNAME=postgres"
   set /p "DB_PASSWORD=PostgreSQL password: "
   ```

   The `set /p` prompt displays input as you type, so enter it privately. Never commit or share credentials.
3. For a **fresh installation only**, set `KALA_ADMIN_EMAIL` and `KALA_ADMIN_PASSWORD` before the first startup if you want the seed loader to create a studio admin. Use a dedicated admin email (not an existing customer email). Do not include real passwords in Git:

   ```bat
   set "KALA_ADMIN_EMAIL=studio@example.com"
   set /p "KALA_ADMIN_PASSWORD=New admin password: "
   ```

   On startup, Flyway applies the database migration and the seed loader loads missing catalogue records from `backend/src/main/resources/seed/seed.json`. Existing records are preserved; the loader does not automatically promote customer accounts.
4. Start the backend:

   ```bat
   mvnw.cmd spring-boot:run
   ```

   The backend runs at `http://localhost:8080`.
5. In a **second** Command Prompt, start the frontend:

   ```bat
   cd frontend
   npm ci
   npm run dev
   ```

   Open the local URL displayed by Vite (normally `http://localhost:5173`). Vite proxies requests under `/api` to the backend. Keep both terminals running.

## Testing

From `backend` you can compile without executing tests:

```bat
mvnw.cmd clean compile
```

For database-backed tests, first create a **separate** PostgreSQL database named `kala_test` and configure the `test` profile to use it. Do not point automated tests at the main `kala` database. Then execute:

```bat
mvnw.cmd test -Dspring.profiles.active=test
```

Test suite status: a previous test attempt failed while the PostgreSQL password was not configured; a complete passing automated test suite has **not** been verified. The customer and admin workflows described below were subsequently checked manually in the running application.

## Manually checked flows

- Register/login as a customer and view the paintings catalogue.
- Buy an available painting, then cancel the order and see the painting become available again.
- Submit a valid wall-painting booking.
- Submit a valid pottery request.
- Log in as admin, view customer orders, confirm a booking, accept pottery, and add/edit an available painting.

These are user-performed manual checks, not automated test results or evidence of exhaustive concurrency testing.

## Project structure

```text
Kala/
  backend/      Spring Boot API, database migration, seed JSON, Maven wrapper
  frontend/     React/Vite UI and painting images
  README.md     Setup and usage instructions
  DESIGN.md     Architecture decisions and implementation scope
  .env.example  Environment-variable placeholders
  .gitignore    Files excluded from Git
```

## Notes / limitations

- Buying is order creation; no payment gateway is integrated.
- No deployment, password-reset, email verification, or image-upload feature.
- The UI is designed for the assignment rather than production use.
- Not all security, validation, and concurrent-request scenarios have been proven by automated integration tests.
- Do not commit `.env` files or real credentials.
