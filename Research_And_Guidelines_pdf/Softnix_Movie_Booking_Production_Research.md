# Softnix Round 2 Backend Assessment --- Movie Show Booking System

## Production-Ready Research & Agent Implementation Specification

**Purpose:** This document is an implementation contract for an agentic
coding AI and a human developer. It translates the Softnix assessment
guidelines and the assigned Movie Show Booking brief into an
implementable Spring Boot backend.

The supplied assessment requires Java 21, Spring Boot, MySQL or
PostgreSQL, proper JPA relationships, RESTful APIs, Controller → Service
→ Repository layering, centralized exception handling, Bean Validation,
Maven, Spring Data JPA, and a Postman collection.
fileciteturn0file1L10-L30

------------------------------------------------------------------------

## 1. Objective

Build a RESTful movie-show seat booking backend that:

-   creates movie shows and their seats;
-   books exactly one seat per request;
-   guarantees no double-booking under concurrent requests;
-   supports cancellation before show time;
-   refunds the booking amount in full on valid cancellation;
-   immediately releases cancelled seats;
-   exposes available-seat information;
-   validates every business rule server-side;
-   is demonstrable entirely through Postman;
-   includes automated tests, especially concurrency tests.

Recommended database: **PostgreSQL**.

Recommended concurrency strategy:

``` text
@Transactional
+
JPA PESSIMISTIC_WRITE
+
PostgreSQL row locking
+
database uniqueness constraint
```

------------------------------------------------------------------------

## 2. Assessment Constraints

Mandatory:

-   Java 21
-   Spring Boot
-   PostgreSQL or MySQL
-   Spring Data JPA
-   Maven
-   REST APIs
-   Controller → Service → Repository
-   Bean Validation
-   centralized `@ControllerAdvice`
-   Postman collection
-   public GitHub repository
-   conventional commits

The assessment also explicitly allows supporting tables/columns and
welcomes thoughtful edge cases and useful read APIs.
fileciteturn0file1L42-L50

Optional bonus:

-   OpenAPI/Swagger
-   Dockerfile
-   structured SLF4J logging
-   Spring Cache/Redis

Do not add unnecessary infrastructure.

------------------------------------------------------------------------

## 3. Business Rules

### Booking

1.  A seat can be booked only once per show.
2.  Booking fails if the seat is already booked.
3.  Status transitions are:

``` text
CREATED → CONFIRMED
CONFIRMED → CANCELLED
```

No other transition is permitted.

### Cancellation

1.  Cancellation is allowed only before `showTime`.
2.  Cancellation is allowed only when status is `CONFIRMED`.
3.  Valid cancellation refunds exactly `amountPaid`.
4.  Cancelled seats become available immediately.
5.  No refund is given after show time.

------------------------------------------------------------------------

## 4. Required Data Model

Minimum tables:

``` text
shows
seats
bookings
```

### Show

``` text
show_id
movie_name
show_time
total_seats
price_per_seat       -- supporting column
created_at
updated_at
```

### Seat

``` text
seat_id
show_id              -- FK
seat_number
is_booked
created_at
updated_at
```

Constraint:

``` text
UNIQUE(show_id, seat_number)
```

### Booking

``` text
booking_id
show_id              -- FK
seat_id              -- FK
customer_id
status
amount_paid
created_at
cancelled_at
```

Use:

``` java
enum BookingStatus {
    CREATED,
    CONFIRMED,
    CANCELLED
}
```

The assessment requires proper JPA relationships rather than flat
denormalized tables. fileciteturn0file1L14-L18

------------------------------------------------------------------------

## 5. Why Add `pricePerSeat`?

The brief requires `Booking.amountPaid` but does not specify how the
amount is calculated.

Use:

``` text
Show.pricePerSeat
        ↓
Booking.amountPaid
```

At booking time, copy the current show price into `amountPaid`.

Refund:

``` text
refundAmount = booking.amountPaid
```

Never calculate an old booking's refund from a potentially changed show
price.

------------------------------------------------------------------------

## 6. Show and Seat Initialization

The customer should not create seats.

Recommended setup API:

``` http
POST /api/v1/shows
```

Example:

``` json
{
  "movieName": "Avengers: Endgame",
  "showTime": "2026-08-31T18:30:00+05:30",
  "totalSeats": 100,
  "pricePerSeat": 250.00
}
```

Creating a show automatically generates its seats.

For 100 seats, a deterministic layout can be:

``` text
A1 ... A10
B1 ... B10
...
J1 ... J10
```

All start as:

``` text
isBooked = false
```

The exact layout is an implementation decision because the supplied
brief does not specify rows/columns.

------------------------------------------------------------------------

## 7. JPA Relationships

Use proper relationships:

``` text
Show
 ├── @OneToMany → Seat
 └── @OneToMany → Booking

Seat
 ├── @ManyToOne → Show
 └── @OneToMany → Booking

Booking
 ├── @ManyToOne → Show
 └── @ManyToOne → Seat
```

Prefer:

``` java
@ManyToOne(fetch = FetchType.LAZY)
```

Avoid unnecessary eager loading.

Do not expose entities directly through REST responses; use DTOs.

------------------------------------------------------------------------

## 8. The Critical Concurrency Problem

Naive code is unsafe:

``` java
Seat seat = repository.findById(id);

if (!seat.isBooked()) {
    seat.setBooked(true);
    repository.save(seat);
}
```

Two requests can both read:

``` text
isBooked = false
```

before either commits.

Result: double-booking.

The system must guarantee:

``` text
N concurrent requests
       ↓
same show + same seat
       ↓
exactly 1 success
N-1 conflicts
```

------------------------------------------------------------------------

## 9. Concurrency Solution

Use a transaction plus pessimistic row locking.

Spring provides declarative transaction management through
`@Transactional`. citeturn0search0turn0search2

PostgreSQL row locks obtained by `FOR UPDATE` prevent conflicting
transactions from modifying or locking the same row until the
transaction ends. PostgreSQL 18 documents `FOR UPDATE` as part of its
locking clauses. citeturn0search10turn0search13

Repository:

``` java
public interface SeatRepository extends JpaRepository<Seat, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select s
        from Seat s
        where s.seatId = :seatId
          and s.show.id = :showId
    """)
    Optional<Seat> findByIdAndShowIdForUpdate(
        @Param("seatId") Long seatId,
        @Param("showId") Long showId
    );
}
```

Service:

``` java
@Transactional
public BookingResponse bookSeat(BookSeatRequest request) {

    Seat seat = seatRepository
        .findByIdAndShowIdForUpdate(
            request.showId(),
            request.seatId()
        )
        .orElseThrow(SeatNotFoundException::new);

    if (seat.isBooked()) {
        throw new SeatAlreadyBookedException();
    }

    Show show = seat.getShow();

    if (!clock.instant().isBefore(show.getShowTime())) {
        throw new ShowAlreadyStartedException();
    }

    seat.setBooked(true);

    Booking booking = new Booking();
    booking.setShow(show);
    booking.setSeat(seat);
    booking.setCustomerId(request.customerId());
    booking.setAmountPaid(show.getPricePerSeat());
    booking.setStatus(BookingStatus.CONFIRMED);

    return mapper.toResponse(bookingRepository.save(booking));
}
```

The lock must remain active for the entire booking transaction.

------------------------------------------------------------------------

## 10. Database Defense in Depth

Also enforce active-booking uniqueness.

For PostgreSQL:

``` sql
CREATE UNIQUE INDEX uq_active_booking_show_seat
ON bookings(show_id, seat_id)
WHERE status IN ('CREATED', 'CONFIRMED');
```

This prevents two active bookings for the same show/seat even if an
application defect bypasses the normal flow.

Use both:

``` text
application transaction/lock
+
database constraint
```

Do not rely on the database constraint alone for the primary booking
workflow.

------------------------------------------------------------------------

## 11. Booking Flow

``` text
POST /bookings
      ↓
Validate DTO
      ↓
Begin transaction
      ↓
Lock Seat row
      ↓
Verify seat belongs to show
      ↓
Check isBooked
      ↓
Check showTime
      ↓
isBooked = true
      ↓
Create Booking
      ↓
COMMIT
```

Failure:

``` text
ROLLBACK
```

The seat must remain available if the booking transaction fails.

------------------------------------------------------------------------

## 12. Cancellation Flow

Endpoint:

``` http
POST /api/v1/bookings/{bookingId}/cancel
```

Transaction:

``` text
BEGIN
  ↓
Lock Booking row
  ↓
Check booking exists
  ↓
Check status == CONFIRMED
  ↓
Check now < showTime
  ↓
Lock Seat row
  ↓
seat.isBooked = false
  ↓
booking.status = CANCELLED
  ↓
booking.cancelledAt = now
  ↓
refund = booking.amountPaid
  ↓
COMMIT
```

If any validation fails:

``` text
ROLLBACK
```

Do not delete cancelled bookings; preserve history.

------------------------------------------------------------------------

## 13. Cancellation Concurrency

If two requests cancel the same booking concurrently:

``` text
Request A → locks booking
Request B → waits
```

A changes:

``` text
CONFIRMED → CANCELLED
seat → available
```

After A commits, B sees:

``` text
status = CANCELLED
```

B returns:

``` text
409 CONFLICT
```

This prevents duplicate state transitions/refunds.

------------------------------------------------------------------------

## 14. Time Handling

Prefer:

``` java
Instant
```

for persisted timestamps, or `OffsetDateTime` if preserving an API
offset is important.

Use an injectable `Clock`:

``` java
@Bean
Clock clock() {
    return Clock.systemUTC();
}
```

Compare:

``` text
now < showTime
```

Cancellation allowed.

``` text
now >= showTime
```

Cancellation rejected.

Never accept the current time from the client.

------------------------------------------------------------------------

## 15. REST API

### Create Show

``` http
POST /api/v1/shows
```

### Get Show

``` http
GET /api/v1/shows/{showId}
```

### Get Availability

``` http
GET /api/v1/shows/{showId}/available-seats
```

### Book Seat

``` http
POST /api/v1/bookings
```

Request:

``` json
{
  "showId": 1,
  "seatId": 10,
  "customerId": "CUST-101"
}
```

### Get Booking

``` http
GET /api/v1/bookings/{bookingId}
```

### Cancel Booking

``` http
POST /api/v1/bookings/{bookingId}/cancel
```

Recommended additional read APIs are appropriate because the assessment
welcomes useful extra read functionality. fileciteturn0file1L47-L50

------------------------------------------------------------------------

## 16. HTTP Status Codes

``` text
201 CREATED
200 OK
400 BAD REQUEST
404 NOT FOUND
409 CONFLICT
500 INTERNAL SERVER ERROR
```

Examples:

``` text
Seat already booked → 409
Booking already cancelled → 409
Cancellation after show time → 409
Unknown booking → 404
Unknown show → 404
Invalid DTO → 400
```

------------------------------------------------------------------------

## 17. Error Contract

All errors should use one structure:

``` json
{
  "timestamp": "2026-08-21T10:00:00Z",
  "status": 409,
  "error": "SEAT_ALREADY_BOOKED",
  "message": "Seat A10 is already booked for show 1",
  "path": "/api/v1/bookings"
}
```

Implement:

``` java
@RestControllerAdvice
public class GlobalExceptionHandler {
}
```

Handle validation, domain exceptions, constraint violations, and
unexpected exceptions.

Never expose stack traces.

------------------------------------------------------------------------

## 18. DTO Validation

Example:

``` java
public record BookSeatRequest(
    @NotNull Long showId,
    @NotNull Long seatId,
    @NotBlank @Size(max = 100) String customerId
) {}
```

Create show:

``` java
public record CreateShowRequest(
    @NotBlank @Size(max = 255) String movieName,
    @NotNull Instant showTime,
    @Min(1) int totalSeats,
    @DecimalMin("0.00") BigDecimal pricePerSeat
) {}
```

The assessment explicitly requires Bean Validation such as `@Valid`,
`@NotNull`, and `@Min`. fileciteturn0file1L23-L25

------------------------------------------------------------------------

## 19. Package Structure

``` text
com.softnix.moviebooking
├── MovieBookingApplication.java
├── controller
│   ├── ShowController.java
│   └── BookingController.java
├── service
│   ├── ShowService.java
│   └── BookingService.java
├── repository
│   ├── ShowRepository.java
│   ├── SeatRepository.java
│   └── BookingRepository.java
├── entity
│   ├── Show.java
│   ├── Seat.java
│   ├── Booking.java
│   └── BookingStatus.java
├── dto
│   ├── request
│   └── response
├── exception
│   ├── GlobalExceptionHandler.java
│   └── domain exceptions
└── config
    ├── ClockConfig.java
    └── OpenApiConfig.java
```

Controllers only map HTTP requests.

Services contain business logic.

Repositories contain persistence operations.

------------------------------------------------------------------------

## 20. Maven Dependencies

Minimum:

``` text
spring-boot-starter-web
spring-boot-starter-data-jpa
spring-boot-starter-validation
postgresql
spring-boot-starter-test
```

Recommended:

``` text
springdoc-openapi-starter-webmvc-ui
flyway-core
```

Optional:

``` text
Testcontainers PostgreSQL
```

------------------------------------------------------------------------

## 21. Database Migration

Use Flyway for a production-quality schema.

Suggested:

``` text
V1__create_shows.sql
V2__create_seats.sql
V3__create_bookings.sql
V4__booking_constraints.sql
```

Do not use:

``` properties
spring.jpa.hibernate.ddl-auto=create
```

for production.

Once migrations exist, use:

``` properties
spring.jpa.hibernate.ddl-auto=validate
```

------------------------------------------------------------------------

## 22. Indexes

Recommended:

``` text
UNIQUE(show_id, seat_number)
INDEX seats(show_id, is_booked)
INDEX bookings(show_id, seat_id)
INDEX bookings(customer_id)
INDEX bookings(status)
```

Availability query:

``` text
show_id = ?
AND is_booked = false
```

must be efficient.

------------------------------------------------------------------------

## 23. Availability

The authoritative state is:

``` text
Seat.isBooked
```

Do not maintain a separate `availableSeats` counter unless there is a
measured need.

Availability should be derived from seat rows to avoid synchronization
bugs.

------------------------------------------------------------------------

## 24. Booking After Show Time

Although the brief explicitly specifies cancellation timing, reject new
bookings after the show starts:

``` text
now >= showTime
→ 409 SHOW_ALREADY_STARTED
```

This is a sensible edge-case policy and should be documented.

------------------------------------------------------------------------

## 25. Idempotency

Not mandatory, but recognize the retry problem:

``` text
Client sends booking
Server succeeds
Network response is lost
Client retries
```

An optional future enhancement is:

``` http
Idempotency-Key: <unique-key>
```

Do not implement this if it makes the assessment unnecessarily complex.

------------------------------------------------------------------------

## 26. Testing Strategy

### Unit tests

Test:

-   successful booking
-   already booked seat
-   nonexistent show
-   nonexistent seat
-   wrong show/seat combination
-   successful cancellation
-   cancellation after show time
-   cancellation of cancelled booking
-   full refund
-   seat release

### Integration tests

Verify:

-   JPA mappings
-   migrations
-   foreign keys
-   unique constraints
-   locking query
-   availability query

### Concurrency test

Submit many concurrent requests for exactly the same:

``` text
showId
seatId
```

Expected:

``` text
success = 1
conflict = N - 1
active booking count = 1
seat.isBooked = true
```

This test is critical because it proves the main requirement.

Use Testcontainers PostgreSQL if practical.

------------------------------------------------------------------------

## 27. Postman Demonstration

Collection:

``` text
Movie Booking API
├── Shows
│   ├── Create Show
│   ├── Get Show
│   └── Get Available Seats
├── Bookings
│   ├── Book Seat
│   ├── Get Booking
│   └── Cancel Booking
└── Negative Tests
    ├── Already Booked Seat
    ├── Invalid Show
    ├── Invalid Seat
    ├── Cancel Cancelled Booking
    └── Cancel After Show Time
```

Use variables:

``` text
baseUrl
showId
seatId
bookingId
```

The assessment explicitly requires a working Postman collection covering
the endpoints. fileciteturn0file1L27-L30

------------------------------------------------------------------------

## 28. Recommended Live Demo

1.  Create show.
2.  Display generated seats.
3.  Book A10.
4.  Try booking A10 again → `409`.
5.  Check availability → A10 absent.
6.  Cancel booking → `200`, full refund.
7.  Check availability → A10 present.
8.  Cancel again → `409`.
9.  Explain concurrent-booking protection.
10. Run concurrency test.

This demonstrates the actual business rules rather than just CRUD.

------------------------------------------------------------------------

## 29. Project Structure

``` text
movie-show-booking/
├── pom.xml
├── mvnw
├── mvnw.cmd
├── README.md
├── .gitignore
├── Dockerfile
├── docker-compose.yml
├── postman/
│   └── Movie-Booking.postman_collection.json
├── src/
│   ├── main/
│   │   ├── java/com/softnix/moviebooking/
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/
│   │           ├── V1__create_shows.sql
│   │           ├── V2__create_seats.sql
│   │           ├── V3__create_bookings.sql
│   │           └── V4__booking_constraints.sql
│   └── test/
└── docs/
    ├── architecture.md
    └── api-flow.md
```

------------------------------------------------------------------------

## 30. Implementation Order for the Agent

### Phase 1

Bootstrap Maven/Spring Boot with Java 21.

### Phase 2

Configure PostgreSQL and Flyway.

### Phase 3

Implement entities and relationships.

### Phase 4

Implement DTOs and validation.

### Phase 5

Implement repositories and locking queries.

### Phase 6

Implement show creation and seat generation.

### Phase 7

Implement booking transaction.

### Phase 8

Implement cancellation transaction.

### Phase 9

Implement controllers.

### Phase 10

Implement centralized exception handling.

### Phase 11

Implement unit/integration/concurrency tests.

### Phase 12

Create Postman collection.

### Phase 13

Create README and architecture documentation.

### Phase 14

Run full verification.

------------------------------------------------------------------------

## 31. Definition of Done

### Functional

-   [ ] Show creation works.
-   [ ] Seats are automatically generated.
-   [ ] Seats initially have `isBooked=false`.
-   [ ] One seat can be booked.
-   [ ] Already-booked seat returns 409.
-   [ ] Cancellation works before show time.
-   [ ] Cancellation fails after show time.
-   [ ] Cancellation requires CONFIRMED status.
-   [ ] Full refund equals `amountPaid`.
-   [ ] Cancellation releases the seat.
-   [ ] Released seat can be booked again.
-   [ ] Availability returns only available seats.

### Concurrency

-   [ ] Same seat cannot be double-booked.
-   [ ] Booking is transactional.
-   [ ] Seat is pessimistically locked.
-   [ ] Active-booking uniqueness is enforced.
-   [ ] Concurrent cancellation cannot duplicate the state transition.
-   [ ] Concurrency test passes.

### Architecture

-   [ ] Controller → Service → Repository.
-   [ ] No business logic in controllers.
-   [ ] DTOs are used.
-   [ ] Proper JPA relationships.
-   [ ] Central exception handler.
-   [ ] Bean Validation.

### Database

-   [ ] Foreign keys.
-   [ ] Unique seat per show.
-   [ ] Active-booking uniqueness.
-   [ ] Useful indexes.
-   [ ] Migration scripts.

### Submission

-   [ ] Public GitHub repository.
-   [ ] Conventional commits.
-   [ ] Postman collection.
-   [ ] README.
-   [ ] No secrets committed.

The assessment requires a public GitHub repository, conventional commit
messages, and a README with setup/database/Postman/test instructions.
fileciteturn0file1L31-L34 fileciteturn0file1L51-L59

------------------------------------------------------------------------

## 32. Agentic AI Hard Constraints

You are a senior Java 21/Spring Boot backend engineer.

Implement this system exactly according to this specification.

Do NOT:

-   build a console application;
-   replace Spring Boot;
-   replace Spring Data JPA;
-   replace PostgreSQL/MySQL with MongoDB;
-   put business logic in controllers;
-   use in-memory collections as the source of truth;
-   rely only on `isBooked` checking for concurrency;
-   use Java `synchronized` as the primary booking protection;
-   expose JPA entities directly as REST contracts;
-   store plaintext secrets;
-   remove business rules;
-   silently change architecture.

Primary concurrency strategy:

``` text
@Transactional
+
PESSIMISTIC_WRITE
+
PostgreSQL row lock
+
database uniqueness constraint
```

Prefer simple, explicit, explainable code over unnecessary abstractions.

Before changing a requirement, explain the conflict, proposed change,
safety implications, and whether assessment requirements remain
satisfied.

------------------------------------------------------------------------

## 33. Verification Commands

The finished repository must support:

``` bash
mvn clean test
mvn clean install
mvn spring-boot:run
```

The agent must verify all endpoints and the concurrency test before
declaring the implementation complete.

------------------------------------------------------------------------

## 34. Architectural Invariant

The most important invariant is:

> For a given show and seat, at most one active booking (`CREATED` or
> `CONFIRMED`) may exist, and the Seat row must agree with that state.

The implementation must preserve this invariant during:

-   normal booking;
-   concurrent booking;
-   cancellation;
-   concurrent cancellation;
-   retries;
-   validation failures;
-   transaction rollback;
-   application errors.

------------------------------------------------------------------------

## 35. Research Basis

Spring provides declarative transaction management and `@Transactional`
for defining transaction boundaries. citeturn0search0turn0search2

PostgreSQL documents `FOR UPDATE` and related locking clauses for
protecting selected rows against conflicting concurrent operations until
transaction completion. citeturn0search10turn0search13

The Softnix assessment itself mandates Java 21, Spring Boot, JPA,
PostgreSQL/MySQL, REST layering, centralized exception handling, Bean
Validation, Maven and Postman. fileciteturn0file1L10-L30
