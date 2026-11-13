# GitHub Repository Searcher

A production-grade Spring Boot 3 REST API backend that searches GitHub repositories using GitHub's REST API, stores and updates repository metadata in PostgreSQL without duplicates, and allows users to query, filter, and sort stored repositories with dynamic criteria.

---

## 1. Project Purpose & Features

- **GitHub Search & Ingestion (`POST /api/github/search`)**:
  - Searches repositories on GitHub using custom queries, optional programming language, and sorting.
  - Automatically persists results into PostgreSQL.
  - **Duplicate Prevention**: Maps GitHub's repository ID directly as the database primary key (`id`), automatically updating existing records on subsequent searches instead of duplicating them.
- **Local Database Querying (`GET /api/github/repositories`)**:
  - Reads directly from local PostgreSQL without consuming GitHub API rate limits.
  - Dynamic filtering: filter by programming language (case-insensitive) and minimum star count (`minStars`).
  - Safe sorting: supports `stars`, `forks`, and `updated` (mapped safely to entity properties).
  - Built-in pagination with configurable `page` and `size`.
- **Robust Error Handling**:
  - Translates GitHub rate limits (HTTP 403 / 429) to clean HTTP 429 responses with `Retry-After` headers.
  - Maps invalid requests, blank queries, negative stars, or unsupported sort values to HTTP 400 Bad Request.
  - Handles upstream GitHub outages (HTTP 502 / 503) and 404 resource lookups cleanly without leaking secrets or stack traces.
- **Interactive Documentation & API Testing**:
  - Interactive OpenAPI / Swagger UI on [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html).
  - Pre-configured Postman Collection located in `postman/GitHub-Searcher.postman_collection.json`.

---

## 2. Technology Stack & Prerequisites

- **Java**: 17 LTS (OpenJDK)
- **Framework**: Spring Boot 3.3.5
- **Build Tool**: Apache Maven 3.9.x (Portable wrapper provided: `.\mvn.cmd`)
- **Database**: PostgreSQL 14+ (Local port 5432, Driver: `org.postgresql:postgresql`)
- **ORM / Persistence**: Spring Data JPA & Hibernate
- **HTTP Client**: Spring 6 `RestClient` (with connection and response timeouts)
- **Validation**: Jakarta Bean Validation (`spring-boot-starter-validation`)
- **API Documentation**: Springdoc OpenAPI / Swagger UI 2.6.0
- **Testing**: JUnit 5, Mockito, Spring Boot Test, H2 In-Memory DB (test scope)

---

## 3. Project Architecture & Package Responsibilities

The codebase follows standard Spring Boot layered architecture with constructor injection throughout:

```text
com.example.githubsearcher
│
├── client/
│   └── GitHubApiClient.java             # REST client communicating with GitHub API, handles timeouts and error status mapping
│
├── config/
│   └── RestClientConfig.java            # Configures Spring RestClient bean with timeouts, GitHub headers, and PAT authentication
│
├── controller/
│   └── GitHubController.java            # Exposes required /api/github/* endpoints and legacy /api/repositories/* aliases
│
├── dto/
│   ├── RepositoryListResponse.java      # Response container returning 'repositories' array with pagination metadata
│   ├── RepositoryResponse.java          # Repository DTO returned to API consumers
│   ├── SearchRequest.java               # Incoming search request with validations
│   ├── SearchResponse.java              # Search response container matching assignment schema
│   └── SortField.java                   # Safe enum mapping client sort values (stars, forks, updated) to DB entity properties
│
├── entity/
│   └── GitHubRepository.java            # JPA Entity mapping to PostgreSQL table 'github_repositories' (PK = GitHub ID)
│
├── exception/
│   ├── GitHubApiException.java          # Upstream GitHub communication exception (502 / 503)
│   ├── GitHubRateLimitException.java    # Rate limit exception (429)
│   ├── GlobalExceptionHandler.java      # @RestControllerAdvice centralizing error responses
│   └── ResourceNotFoundException.java   # 404 Not Found exception
│
├── repository/
│   └── GitHubRepositoryDao.java         # Spring Data JPA repository extending JpaSpecificationExecutor
│
├── service/
│   └── GitHubService.java               # Core business logic: query building, GitHub ingestion, upserting, and dynamic filtering
│
├── specification/
│   └── RepositorySpecification.java     # Dynamic criteria specifications (language, minStars, keyword search)
│
└── GitHubSearcherApplication.java       # Spring Boot main application class
```

---

## 4. PostgreSQL Database Setup

### Local Setup
1. Ensure PostgreSQL 17 is running on port `5432`.
2. Connect to PostgreSQL and create the project database:
   ```powershell
   & "C:\Program Files\PostgreSQL\17\bin\psql.exe" -U postgres -h 127.0.0.1 -p 5432 -c "CREATE DATABASE github_searcher;"
   ```
3. Hibernate will automatically manage and create the `github_repositories` table on startup.

---

## 5. Environment Variables & Configuration

Configuration is managed via `src/main/resources/application.properties` and populated via `.env`:

| Variable | Description | Default Value |
| :--- | :--- | :--- |
| `DB_URL` | JDBC URL for PostgreSQL | `jdbc:postgresql://localhost:5432/github_searcher` |
| `DB_USERNAME` | PostgreSQL database user | `postgres` |
| `DB_PASSWORD` | PostgreSQL database password | *(set in .env)* |
| `GITHUB_API_BASE_URL` | Base URL for GitHub REST API | `https://api.github.com` |
| `GITHUB_TOKEN` | GitHub Personal Access Token (PAT) | *(optional, increases rate limits)* |
| `SERVER_PORT` | HTTP port for the Spring Boot server | `8081` *(configured to avoid port 8080 conflicts)* |

See [.env.example](file:///e:/backend-Java/FindRepo/.env.example) for reference.

> **Security Note:** Never commit your `.env` file to version control. The project's `.gitignore` explicitly excludes `.env`.

---

## 6. How to Build & Run

### Run the Application (Automatic .env Loading)
Run the PowerShell launcher script:
```powershell
.\run.ps1
```

### Run All Automated Tests
```powershell
.\mvn.cmd test
```

### Build and Package Executable JAR
```powershell
.\mvn.cmd clean package
```

Once running:
- **Swagger UI**: [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)
- **Health / Status Check**: [http://localhost:8081/api/github/status](http://localhost:8081/api/github/status)
- **OpenAPI JSON Spec**: [http://localhost:8081/api-docs](http://localhost:8081/api-docs)

---

## 7. API Endpoints Reference

### 1. Search GitHub Repositories (Required Assignment Endpoint)
- **Method & Path**: `POST /api/github/search` *(Alias: `POST /api/repositories/search`)*
- **Headers**: `Content-Type: application/json`
- **Request Body**:
  ```json
  {
    "query": "spring boot",
    "language": "Java",
    "sort": "stars"
  }
  ```
- **Requirements**:
  - `query` is required (must not be blank).
  - `language` is optional.
  - `sort` is optional (`stars`, `forks`, or `updated`; default: `stars`).
- **Success Response (HTTP 200)**:
  ```json
  {
    "message": "Repositories fetched and saved successfully",
    "repositories": [
      {
        "id": 6296790,
        "name": "spring-boot",
        "description": "Spring Boot helps you create stand-alone, production-grade Spring based Applications...",
        "owner": "spring-projects",
        "language": "Java",
        "stars": 81910,
        "forks": 41200,
        "lastUpdated": "2026-11-13T12:51:55Z",
        "htmlUrl": "https://github.com/spring-projects/spring-boot",
        "createdAt": "2026-11-13T12:44:02.566",
        "updatedAt": "2026-11-13T13:36:52.124"
      }
    ],
    "totalCount": 81910,
    "page": 1,
    "perPage": 30
  }
  ```

---

### 2. Retrieve Stored Repositories (Required Assignment Endpoint)
- **Method & Path**: `GET /api/github/repositories` *(Alias: `GET /api/repositories`)*
- **Query Parameters**:
  - `language` *(optional)*: Filter by programming language (case-insensitive).
  - `minStars` *(optional)*: Only return repositories with `stars >= minStars`.
  - `sort` *(optional)*: Safe sort property (`stars`, `forks`, or `updated`; default: `stars`).
  - `direction` *(optional)*: `DESC` or `ASC` (default: `DESC`).
  - `page` *(optional)*: Zero-indexed page number (default: `0`).
  - `size` *(optional)*: Page size (default: `20`).
- **Example**:
  `GET /api/github/repositories?language=Java&minStars=100&sort=stars`
- **Success Response (HTTP 200)**:
  ```json
  {
    "repositories": [
      {
        "id": 6296790,
        "name": "spring-boot",
        "description": "Spring Boot helps you create stand-alone, production-grade Spring based Applications...",
        "owner": "spring-projects",
        "language": "Java",
        "stars": 81910,
        "forks": 41200,
        "lastUpdated": "2026-11-13T12:51:55Z",
        "htmlUrl": "https://github.com/spring-projects/spring-boot"
      }
    ],
    "totalCount": 1,
    "page": 0,
    "size": 20,
    "totalPages": 1
  }
  ```

---

### 3. Additional Auxiliary Endpoints
- **Get Stored Repository by ID**: `GET /api/github/repositories/{id}`
- **Delete Stored Repository**: `DELETE /api/github/repositories/{id}` (HTTP 204 No Content)
- **Health Check**: `GET /api/github/status` (`{"status": "UP", "service": "github-searcher"}`)

---

## 8. Postman Collection

An official Postman collection is included in the project under:
📁 **`postman/GitHub-Searcher.postman_collection.json`**

To use:
1. Open Postman.
2. Click **Import** and select `postman/GitHub-Searcher.postman_collection.json`.
3. The collection is pre-configured with `{{baseUrl}} = http://localhost:8081` and contains requests for:
   - Search GitHub repositories with sample body
   - Retrieve all stored repositories
   - Filter by language
   - Filter by minimum stars
   - Combined filters (language + minStars + sort)
   - Negative validation test (blank query -> HTTP 400)
   - Negative validation test (invalid sort -> HTTP 400)
   - Health status check

---

## 9. GitHub API Authentication & Rate Limits

- **Unauthenticated requests** to GitHub's Search API are limited by GitHub to 10 requests per minute per IP.
- **Authenticated requests** using a GitHub Personal Access Token (PAT) enjoy 30 requests per minute.
- Configure your token in `.env` as `GITHUB_TOKEN=your_token`.
- When rate limits are reached, the application returns HTTP 429 Too Many Requests with a `Retry-After` header indicating when the rate limit window resets.
