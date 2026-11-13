# Project Architecture & File Directory Guide

## GitHub Repository Searcher — What Each File & Folder Does

This document explains the purpose, responsibility, and architectural contribution of every file and folder in the **FindRepo (GitHub Repository Searcher)** codebase.

---

## High-Level Architecture Overview

The project is designed following standard Spring Boot enterprise layered architecture:

```
                  ┌────────────────────────────────────────────────┐
                  │                 Client Request                 │
                  │   (Swagger UI / Browser / Postman / Frontend)  │
                  └───────────────────────┬────────────────────────┘
                                          │
                                          ▼
                  ┌────────────────────────────────────────────────┐
                  │               Controller Layer                 │
                  │             (GitHubController.java)            │
                  └───────────────────────┬────────────────────────┘
                                          │
                                          ▼
                  ┌────────────────────────────────────────────────┐
                  │                Service Layer                   │
                  │              (GitHubService.java)              │
                  └──────────────┬──────────────────┬──────────────┘
                                 │                  │
                (Search & Fetch) │                  │ (Store & Query)
                                 ▼                  ▼
      ┌───────────────────────────────────┐  ┌───────────────────────────────────┐
      │         Client / Upstream         │  │       Persistence Layer (JPA)     │
      │        GitHubApiClient.java       │  │ GitHubRepositoryDao & Spec        │
      └─────────────────┬─────────────────┘  └─────────────────┬─────────────────┘
                        │                                      │
                        ▼                                      ▼
           [External GitHub REST API]               [PostgreSQL Database]
```

---

## 1. Root Configuration & Project Files

| File / Folder | Role & Purpose | Contribution to the Project |
|---|---|---|
| `pom.xml` | **Maven Project Object Model** | Defines project dependencies (Spring Boot Web, Data JPA, PostgreSQL, Springdoc OpenAPI, Lombok, H2, JUnit 5), Java version (17), and build plugins. |
| `.gitignore` | **Git Exclusion Rules** | Prevents secrets (`.env`), build artifacts (`target/`), IDE settings (`.idea`, `.vscode`), and local tool caches from being tracked or pushed to GitHub. |
| `.env.example` | **Environment Template** | A safe, non-secret reference showing developers what environment variables are required (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `GITHUB_TOKEN`). |
| `.env` | **Local Secrets (Ignored)** | Stores local database passwords and personal GitHub access tokens. Kept strictly on the developer's machine. |
| `run.ps1` | **Local Startup Script** | PowerShell utility that automatically parses `.env` into the process environment and boots the Spring Boot application using Maven. |
| `mvn.cmd` / `mvnw.cmd` | **Maven Command Wrappers** | Command-line scripts that execute Maven commands using the project's bundled Maven runtime. |
| `.mvn/maven.config` | **Maven JVM Config** | Configures JVM arguments and options for the Maven wrapper. |

---

## 2. Documentation & Deployment Artifacts

| File | Purpose | Contribution to the Project |
|---|---|---|
| `README.md` | **Main Project Documentation** | Contains setup instructions, API specifications, environment variable configuration, cURL examples, and design decisions. |
| `RAILWAY_DEPLOYMENT.md` | **Cloud Deployment Guide** | Step-by-step instructions for provisioning PostgreSQL, configuring reference variables, and deploying the application to Railway. |
| `SWAGGER_CLEANUP.md` | **Documentation Spec** | The specification detailing how Swagger UI should focus on the two primary assignment APIs. |
| `SWAGGER_CLEANUP_REPORT.md` | **Verification Audit Report** | Detailed audit verifying that auxiliary routes were hidden in Swagger with `@Hidden` without altering underlying HTTP routes. |
| `TEST_EVIDENCE.md` | **Test Execution Evidence** | Static log evidence showing that all 43 automated unit and integration tests execute with zero failures. |

---

## 3. Postman Test Suite

* **`postman/GitHub-Searcher.postman_collection.json`**:
  A ready-to-import Postman collection containing pre-configured requests for searching GitHub repositories, retrieving saved repositories with filters, validating edge cases, rate limits, and health checks.

---

## 4. Main Application Code (`src/main/java`)

The source code is organized into modular packages under `com.example.githubsearcher`:

### 4.1 Application Entrypoint
* **`GitHubSearcherApplication.java`**:
  The Spring Boot main class annotated with `@SpringBootApplication`. Boots Tomcat, initializes the Spring application context, component scanning, and Hibernate entity scanning.

---

### 4.2 Configuration Package (`com.example.githubsearcher.config`)

* **`OpenApiConfig.java`**:
  Configures the Springdoc OpenAPI metadata (title, description, version) and sets the server base URL to relative (`/`). This ensures Swagger UI works securely across both local HTTP and cloud HTTPS proxies without mixed-content errors.
* **`RestClientConfig.java`**:
  Initializes Spring 6's modern `RestClient` bean used to communicate with GitHub. Sets connection timeouts (5 seconds), read timeouts (10 seconds), default GitHub API headers (`application/vnd.github+json`), and optionally attaches the Bearer token if configured.
* **`WebConfig.java`**:
  Implements `WebMvcConfigurer` to configure Global Cross-Origin Resource Sharing (CORS). Allows browsers, Swagger UI, and external frontend single-page apps to query the REST endpoints without CORS rejection.

---

### 4.3 Controller Layer (`com.example.githubsearcher.controller`)

* **`GitHubController.java`**:
  The REST API presentation layer annotated with `@RestController` and `@Validated`.
  * **Primary Endpoint 1 (`POST /api/github/search`)**: Accepts the search criteria JSON, delegates to `GitHubService`, and returns persisted repository results.
  * **Primary Endpoint 2 (`GET /api/github/repositories`)**: Retrieves stored repositories from PostgreSQL with optional filtering (`language`, `minStars`, `search`), sorting, and pagination.
  * **Backward Compatibility Aliases**: Keeps legacy routes (`/api/repositories/...`), health checks (`/api/github/status`), and the root redirect (`/` &rarr; `/swagger-ui.html`) active in code while hiding them from Swagger UI via `@Hidden`.

---

### 4.4 Service Layer (`com.example.githubsearcher.service`)

* **`GitHubService.java`**:
  The core business logic engine:
  * **`searchAndSave()`**: Validates search parameters, builds upstream GitHub query strings (combining keywords, language, and star qualifiers), queries GitHub via `GitHubApiClient`, converts API DTOs into JPA entities, upserts them into PostgreSQL via `GitHubRepositoryDao`, and returns `SearchResponse`.
  * **`getStoredRepositories()`**: Reads repositories from PostgreSQL using dynamic JPA specifications (`RepositorySpecification`), applies sort whitelist validations, constructs pagination metadata, and maps results to `RepositoryListResponse`.
  * **`getStoredRepositoryById()` & `deleteStoredRepository()`**: Supporting CRUD operations with entity existence checks.

---

### 4.5 External Client Layer (`com.example.githubsearcher.client`)

* **`GitHubApiClient.java`**:
  Handles outbound HTTP communication with GitHub's Search REST API (`/search/repositories`):
  * Utilizes `RestClient` with structured query parameter building.
  * Translates HTTP 403 / 429 rate limit responses into `GitHubRateLimitException`, extracting `Retry-After` or `x-ratelimit-reset` headers.
  * Translates HTTP 401 authentication failures and 5xx upstream downtime into structured `GitHubApiException`.
  * Maps JSON responses into immutable Java 17 records (`GitHubSearchApiResponse`, `GitHubItemDto`, `OwnerDto`).

---

### 4.6 Persistence & Repository Layer (`com.example.githubsearcher.repository`)

* **`GitHubRepositoryDao.java`**:
  Spring Data JPA repository interface extending:
  * `JpaRepository<GitHubRepository, Long>`: Provides out-of-the-box CRUD and batch operations (`saveAll()`, `findById()`, `deleteById()`).
  * `JpaSpecificationExecutor<GitHubRepository>`: Enables dynamic, criteria-based filtering without writing raw SQL.

---

### 4.7 Specifications (`com.example.githubsearcher.specification`)

* **`RepositorySpecification.java`**:
  Utility class providing reusable, type-safe JPA Criteria queries:
  * `hasLanguage(String)`: Case-insensitive SQL predicate on `LOWER(language) = ?`.
  * `hasMinStars(Integer)`: SQL predicate on `stars >= ?`.
  * `containsKeyword(String)`: Case-insensitive `LIKE %keyword%` search across both repository `name` and `description`.
  * Completely immune to SQL injection because all user values are parameterized.

---

### 4.8 Entity Model Layer (`com.example.githubsearcher.entity`)

* **`GitHubRepository.java`**:
  JPA Entity mapped to the `github_repositories` table in PostgreSQL:
  * Uses the GitHub repository ID (`@Id Long id`) as the primary key to ensure automatic upserts (preventing duplicate rows).
  * Stores repository name, description (TEXT), owner, language, stars, forks, last updated timestamp, and HTML URL.
  * Uses Hibernate's `@CreationTimestamp` and `@UpdateTimestamp` for automatic audit tracking.

---

### 4.9 Data Transfer Objects (DTOs) (`com.example.githubsearcher.dto`)

* **`SearchRequest.java`**:
  Input model for `POST /api/github/search`. Contains `@NotBlank` validation for `query`, optional `language`, `minStars`, and pagination/sorting fields. Enriched with Swagger `@Schema` annotations.
* **`SearchResponse.java`**:
  Output model returned after searching and persisting. Contains the list of saved repositories, status message, and pagination counters.
* **`RepositoryResponse.java`**:
  Clean client representation of a single repository record, shielding the database entity from exposure.
* **`RepositoryListResponse.java`**:
  Output model for `GET /api/github/repositories`. Wraps the repository list with pagination metadata (`totalCount`, `page`, `size`, `totalPages`).
* **`SortField.java`**:
  Enum defining supported sort fields (`STARS`, `FORKS`, `UPDATED`). Maps client query strings safely to database column properties and rejects unsupported parameters.

---

### 4.10 Exception Handling (`com.example.githubsearcher.exception`)

* **`GlobalExceptionHandler.java`**:
  Central `@RestControllerAdvice` converting application exceptions into consistent JSON error responses (`timestamp`, `status`, `error`, `message`):
  * `MethodArgumentNotValidException` &rarr; `400 Bad Request` with field validation errors.
  * `IllegalArgumentException` &rarr; `400 Bad Request`.
  * `ResourceNotFoundException` &rarr; `404 Not Found`.
  * `GitHubRateLimitException` &rarr; `429 Too Many Requests` with `Retry-After` header.
  * `GitHubApiException` &rarr; `502 Bad Gateway` or `503 Service Unavailable`.
  * `NoResourceFoundException` &rarr; `404 Not Found` for unmapped static resources.
* **`ResourceNotFoundException.java`**: Custom unchecked exception for missing entities.
* **`GitHubRateLimitException.java`**: Custom exception holding rate limit reset windows.
* **`GitHubApiException.java`**: Custom exception for upstream GitHub communication failures.

---

## 5. Main Resources (`src/main/resources`)

* **`application.properties`**:
  Central configuration file for Spring Boot:
  * Configures application port with cloud fallback (`${PORT:${SERVER_PORT:8081}}`).
  * Configures reverse proxy SSL header forwarding (`server.forward-headers-strategy=framework`).
  * Configures PostgreSQL connection parameters supporting both `SPRING_DATASOURCE_*` and `DB_*` environment variables.
  * Configures Hibernate dialect (`PostgreSQLDialect`), DDL auto update, and OpenAPI paths (`/api-docs`, `/swagger-ui.html`).

---

## 6. Test Suite (`src/test`)

### 6.1 Test Resources
* **`src/test/resources/application.properties`**:
  Configures an in-memory H2 database (`jdbc:h2:mem:testdb;MODE=PostgreSQL`) allowing all tests to execute in complete isolation without requiring a live PostgreSQL instance.

### 6.2 Test Classes
* **`GitHubSearcherApplicationTests.java`**:
  Verifies that the Spring Boot application context loads successfully.
* **`GitHubApiClientTest.java`**:
  Tests external GitHub API integration using MockRestServiceServer, validating JSON deserialization, query construction, and rate limit error mapping.
* **`GitHubControllerTest.java`**:
  Spring MVC Web layer tests using `MockMvc`: tests validation, status endpoints, POST search, GET repositories, and error response formatting.
* **`GitHubServiceTest.java`**:
  Unit tests for business logic, mapping routines, upsert behavior, and sorting validation using Mockito.
* **`GitHubRepositoryDaoIntegrationTest.java`**:
  Integration tests for the Data Access Object against H2 in PostgreSQL compatibility mode, verifying JPA queries and dynamic specifications.
