# AuthBaseApp

A small Spring Boot authentication example. Kept the existing Boot 4.1.1 / Java 25 setup and package structure.

## How it is built

`Controller` handles HTTP and wraps results in `ApiResponse<T>`.
`Service` checks passwords and business rules, then calls the repository.
`Repository` reads/writes the JPA `UsersApp` entity.
Request/response records in `DTOs` keep stored passwords out of responses.
`SecurityConfig` handles stateless bearer JWT authentication and CORS.
`GlobalExceptionHandler` turns exceptions into HTTP status codes and the same response format.
This is the only exception file. Services use Spring's `ResponseStatusException` to choose a status and message.
Security and rate-limit filters write that format themselves because they run before controllers.

The review found and fixed: login issuing tokens without checking passwords, a repository query for a missing username property that prevented startup, missing bearer authentication/CORS activation, invalid rate-limit JSON, and a rolling-log filename missing its size counter.

## Run

Requires JDK 25 and MySQL. Create the `first_java` database first.
From this folder, in PowerShell:

```powershell
$env:DB_USER = "root"
$env:DB_PASSWORD = "your-password"
$env:JWT_SECRET = "replace-this-with-a-random-secret-of-at-least-32-bytes"
.\mvnw.cmd spring-boot:run
```

Default: `http://localhost:8080`. Optional environment variables: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `PORT`.
The secret and database credentials in application.properties are local development defaults.

## API

| Method | Path | Body / authentication |
| --- | --- | --- |
| POST | /api/auth/register | name, email, password |
| POST | /api/auth/login | email, password |
| GET | /api/auth/me | Authorization: Bearer TOKEN |

Registration returns 201, login returns 200. Login data contains `token` and `role`.
Registration always assigns USER. Passwords are BCrypt hashes; use 8 or more characters, up to 72 UTF-8 bytes.
Emails are trimmed and lowercased. Duplicate email returns 409; invalid input 400; invalid credentials/token 401.
Tokens expire after one hour. Log out by discarding the token.

All normal API responses share this shape:

```json
{"success":true,"message":"Logged in successfully","data":{"token":"...","role":"USER"}}
```

```json
{"success":false,"message":"Invalid email or password","data":null}
```

Allowed local browser origins are localhost:5500 and 127.0.0.1:5500.
Rate limit: 100 requests per IP with 100 tokens refilled per minute; preflight is exempt. This simple limiter is in memory per application instance.

## Verify

```powershell
.\mvnw.cmd test
```

Tests use a separate in-memory H2 database, so MySQL is not required for tests.
The validation starter provides Jakarta Bean Validation; H2 is test-scoped. Tests cover register/login, password rejection, bearer authentication, duplicate/invalid requests, CORS and rate-limit JSON.

## Validation example for students

Lombok generates getters/setters and the logger. Field validation uses Jakarta annotations on the request DTOs:
`@NotBlank`, `@Email`, and `@Size`.
The controller's `@Valid @RequestBody` triggers validation before the service runs.
The global handler catches `MethodArgumentNotValidException`, uses `@ResponseStatus(BAD_REQUEST)`, and returns one field message with `ApiResponse.error(message)`.
Passwords also have a short `@AssertTrue` check to enforce BCrypt's 72-byte limit for non-ASCII input.
