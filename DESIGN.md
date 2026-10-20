# Kala — Design Notes

## 1. How do login and the two roles work?

Users register and log in through the Spring Boot backend. Customer accounts have the `CUSTOMER` role; the studio account has the `ADMIN` role and is created through configured seed setup, not public registration. Passwords are stored using BCrypt hashing. Spring Security uses session-based authentication, CSRF protection, and server-side role checks. Customer-owned records are restricted to their owners; a request for another customer's record is treated as not found. The React UI shows role-appropriate options, but authorization is enforced by the server.

## 2. Which database did you choose, and why?

I used PostgreSQL 17 because this project contains related records (users, paintings, orders, wall bookings, and pottery requests) that benefit from transactions and database constraints. Spring Data JPA handles database access, and Flyway manages the schema migration. The database is run locally; a cloud-hosted database was not necessary for this submission.

## 3. How do you stop two people from taking the same painting or date?

Painting purchase operations use a pessimistic database row lock to check availability and create an order consistently. The database also uses a unique constraint/index to guard against multiple active orders for the same painting. Wall bookings check for already occupied dates; a database-level partial unique index is the final protection for active bookings. Conflicts should return a clear HTTP 409 response. Pottery requests use version-based optimistic locking for conflicting changes. These safeguards are present in the code, although concurrent requests have not been exhaustively tested against a dedicated test database.

## 4. What did you finish, what did you leave out, and what would you do next?

I implemented customer registration/login, the painting catalogue and purchase/cancellation flow, wall booking requests and admin confirmation, and custom pottery requests and admin acceptance. Admin management for orders, paintings, and catalogue information is also included. Customer and admin workflows were manually tested in the running application. I left out real payments, deployment, password reset, email notifications, and image uploads because they were outside the assignment scope. Next I would add broader automated integration and concurrency tests, improve error/loading-state coverage, and test the setup on a completely fresh machine.

## Implementation notes

The backend currently keeps much of its business logic in controllers alongside Spring Data repositories and request/response classes. This keeps the project small for the assignment, though moving the logic into service classes would be useful as it grows. The React frontend uses Vite's `/api` proxy for local development. Seed loading adds missing catalogue records without overwriting existing data.
