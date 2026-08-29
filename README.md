# Movie Ticket Booking System - Production Backend

A high-performance, concurrency-safe RESTful Movie Ticket Booking backend built with **Java 21/25, Spring Boot 3.3, Spring Data JPA, PostgreSQL / H2, Flyway, and Bean Validation**.

---

## 📌 Features & Business Rules

### 1. Booking Rules
- **Atomic Single Seat Booking**: Exactly one seat is booked per request.
- **Pessimistic Concurrency Control**: `@Lock(LockModeType.PESSIMISTIC_WRITE)` (`SELECT ... FOR UPDATE`) guarantees zero double-booking under concurrent load ($N$ threads $\rightarrow$ 1 success, $N-1$ `409 Conflict`).
- **Database Defense in Depth**: Active booking partial unique index ensures database-level invariant enforcement:
  ```sql
  CREATE UNIQUE INDEX uq_active_booking_show_seat
  ON bookings(show_id, seat_id)
  WHERE status IN ('CREATED', 'CONFIRMED');
  ```
- **Lifecycle Transition**: `CREATED → CONFIRMED`.

### 2. Cancellation & Refund Rules
- **Pre-Show Cancellation**: Cancellation is strictly permitted only before the show's `showTime`.
- **100% Full Refund**: Issues an immediate refund equal to `Booking.amountPaid` (copied from `Show.pricePerSeat` at booking time).
- **Instant Seat Release**: The cancelled seat becomes available immediately (`isBooked = false`).
- **No Duplicate Cancellations**: Concurrently cancelling the same booking results in exactly 1 cancellation and `409 Conflict` for duplicate attempts.

### 3. Seat & Show Management
- **Automatic Deterministic Seat Grid**: Creating a show automatically creates its seats (e.g. `A1..A10`, `B1..B10`, up to `totalSeats`).
- **Real-Time Availability**: `GET /api/v1/shows/{showId}/available-seats` dynamically returns seats where `isBooked == false`.

---

## 🏗️ Architecture & Layering

```text
Controller (REST Endpoints & HTTP Mapping)
       ↓
Service (Business Logic, Transaction Boundaries, Invariants)
       ↓
Repository (Spring Data JPA, Pessimistic Row Locking Queries)
       ↓
Database (PostgreSQL / H2, Foreign Keys, Partial Indexes, Flyway)
```

---

## 🚀 Step-by-Step Running Guide

### Option 1: Run Standalone (Zero Configuration - In-Memory Dev Profile)
The project is configured by default to run instantly without requiring local PostgreSQL:

```powershell
# Windows (PowerShell / Command Prompt)
.\mvnw.cmd spring-boot:run
```

```bash
# Linux / macOS
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`.

---

### Option 2: Run with Docker Compose (Full PostgreSQL Stack)
To run the full production stack with PostgreSQL:

```bash
docker-compose up --build
```

---

### Option 3: Run with Local PostgreSQL
1. Ensure PostgreSQL is running on port `5432` with database `movie_booking_db`.
2. Start the application with active profile `prod`:
```powershell
$env:SPRING_PROFILES_ACTIVE="prod"
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/movie_booking_db"
$env:SPRING_DATASOURCE_USERNAME="postgres"
$env:SPRING_DATASOURCE_PASSWORD="your_password"
.\mvnw.cmd spring-boot:run
```

---

## 🧪 Running Automated Tests & Concurrency Verification

To run all unit tests, slice tests, and multi-threaded concurrency race condition tests:

```powershell
# Windows
.\mvnw.cmd clean test
```

```bash
# Linux / macOS
./mvnw clean test
```

### Key Automated Tests Included:
- `ConcurrentBookingIntegrationTest`: 20 concurrent threads trying to book the exact same seat simultaneously. Verified: exactly 1 succeeds, 19 receive `409 Conflict`, database active booking count = 1.
- `ConcurrentCancellationIntegrationTest`: 10 concurrent threads cancelling the same booking. Verified: exactly 1 succeeds, 9 receive `409 Conflict`, seat is released, booking status is `CANCELLED`.
- `BookingServiceTest` & `ShowServiceTest`: Complete domain unit tests.
- `BookingControllerTest` & `ShowControllerTest`: MockMvc validation and HTTP status code tests.

---

## 📖 API Documentation & Swagger UI

Once started, open your browser to:
- **Interactive Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON Spec**: [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

---

## 📡 REST API Reference

### 1. Show Endpoints

| Method | Endpoint | Description | Status Code |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/shows` | Create a show & auto-generate seats | `201 Created` |
| `GET` | `/api/v1/shows/{showId}` | Get show details & available seats count | `200 OK` |
| `GET` | `/api/v1/shows` | List all shows | `200 OK` |
| `GET` | `/api/v1/shows/{showId}/available-seats` | Get real-time available seats (`isBooked=false`) | `200 OK` |
| `GET` | `/api/v1/shows/{showId}/seats` | Get all seats with booking status | `200 OK` |
| `GET` | `/api/v1/shows/{showId}/bookings` | Get all bookings for a show | `200 OK` |

#### Create Show Request Example:
```http
POST /api/v1/shows
Content-Type: application/json

{
  "movieName": "Avatar: The Way of Water",
  "showTime": "2026-10-31T18:30:00Z",
  "totalSeats": 20,
  "pricePerSeat": 250.00
}
```

---

### 2. Booking Endpoints

| Method | Endpoint | Description | Status Code |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/bookings` | Book a seat (Pessimistic Locking) | `201 Created` / `409 Conflict` |
| `GET` | `/api/v1/bookings/{bookingId}` | Get booking details | `200 OK` / `404 Not Found` |
| `GET` | `/api/v1/bookings?customerId={id}` | Get customer booking history | `200 OK` |
| `POST` | `/api/v1/bookings/{bookingId}/cancel` | Cancel booking & issue 100% refund | `200 OK` / `409 Conflict` |

#### Book Seat Request Example:
```http
POST /api/v1/bookings
Content-Type: application/json

{
  "showId": 1,
  "seatId": 1,
  "customerId": "CUST-1001"
}
```

#### Cancel Booking Request Example:
```http
POST /api/v1/bookings/1/cancel
```

---

## 📬 Postman Collection Setup

1. Open Postman.
2. Click **Import** and select `postman/Movie_Booking_System.postman_collection.json`.
3. The collection is pre-configured with test scripts that automatically update variables (`showId`, `seatId`, `bookingId`).
4. Execute requests in order:
   - **Create Show** $\rightarrow$ Creates show and sets `showId` and `seatId`.
   - **Get Available Seats** $\rightarrow$ Shows all unbooked seats.
   - **Book Seat** $\rightarrow$ Books seat and sets `bookingId`.
   - **Conflict - Double Booking Same Seat** $\rightarrow$ Verifies HTTP `409 SEAT_ALREADY_BOOKED`.
   - **Cancel Booking** $\rightarrow$ Verifies HTTP `200` and full refund amount.
   - **Conflict - Cancel Already Cancelled** $\rightarrow$ Verifies HTTP `409 BOOKING_ALREADY_CANCELLED`.
