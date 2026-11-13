# GitHub Repository Searcher --- Submission Readiness

## Objective

Bring the existing Spring Boot GitHub Repository Searcher project into
full alignment with the supplied backend assignment.

**Important:** The application already has a working implementation,
PostgreSQL integration, GitHub API integration, filtering, exception
handling, and passing tests. Do not recreate the project or replace
working code unnecessarily. First inspect the current codebase, then
make the smallest safe changes required.

## 1. Preserve the existing implementation

Before editing:

1.  Inspect the complete project structure, `pom.xml`, configuration,
    controllers, services, GitHub API client, entity, repositories,
    specifications, exception handling, tests, and README.
2.  Identify the current behavior and existing tests.
3.  Keep the current Java 17, Spring Boot 3.3.5, Maven, PostgreSQL,
    Spring Data JPA, and `RestClient` setup.
4.  Do not remove useful existing functionality such as pagination,
    keyword filtering, health checks, get-by-ID, or delete endpoints.
5.  Do not introduce microservices, unnecessary abstractions, new
    infrastructure, or unrelated dependencies.
6.  Do not overwrite `.env` or expose any credentials in source code,
    logs, documentation, or test output.

Make focused changes and avoid regressions.

## 2. Align the required API endpoints

The assignment requires these exact routes:

### A. Search GitHub repositories

**Required endpoint:** `POST /api/github/search`

**Request body:**

``` json
{
  "query": "spring boot",
  "language": "Java",
  "sort": "stars"
}
```

Requirements:

-   Accept `query`, `language`, and `sort`.
-   `query` must not be null, empty, or blank.
-   `language` is optional.
-   `sort` is optional and must support `stars`, `forks`, and `updated`.
-   Build the GitHub Search API query correctly. For example, combine
    the keyword and language as `spring boot language:Java`.
-   Call `https://api.github.com/search/repositories` through the
    existing GitHub API client.
-   Map the GitHub response's `items` into the application's repository
    model.
-   Persist all returned repositories.
-   If a GitHub repository ID already exists, update that record rather
    than creating a duplicate.
-   Return the saved repository details.

Use a response structure consistent with the assignment:

``` json
{
  "message": "Repositories fetched and saved successfully",
  "repositories": [
    {
      "id": 123456,
      "name": "spring-boot-example",
      "description": "An example repository for Spring Boot",
      "owner": "user123",
      "language": "Java",
      "stars": 450,
      "forks": 120,
      "lastUpdated": "2024-01-01T12:00:00Z"
    }
  ]
}
```

The example values are illustrative. Do not hardcode them.

### B. Retrieve stored repositories

**Required endpoint:** `GET /api/github/repositories`

Support these optional query parameters:

-   `language`: filter by programming language.
-   `minStars`: return repositories whose star count is greater than or
    equal to this value.
-   `sort`: `stars`, `forks`, or `updated`; default to `stars`.

Example:

`GET /api/github/repositories?language=Java&minStars=100&sort=stars`

Requirements:

-   Read from PostgreSQL only. Do not call GitHub from this endpoint.
-   Apply all supplied filters together.
-   If no filters are supplied, return all stored repositories, subject
    to any existing pagination behavior. Preserve pagination if already
    implemented.
-   Use safe, explicit mapping from allowed sort values to database sort
    properties. Never pass arbitrary client input directly as a database
    property.
-   Return a response containing a `repositories` array.

### C. Route compatibility

-   Update the controller mappings so the two required routes work
    exactly as specified.
-   Keep useful existing routes, but do not let them conflict with the
    required routes.
-   If retaining legacy routes is straightforward, support them as
    aliases or keep them as additional endpoints.
-   Update Swagger/OpenAPI descriptions and README examples to show the
    required routes as the primary APIs.
-   Do not change the required HTTP methods.

## 3. Validate request input and sorting

Implement or verify validation for:

-   Missing or blank search query → HTTP 400.
-   Negative `minStars` → HTTP 400.
-   Unsupported sort value → HTTP 400 with a clear message.
-   Optional language may be absent or blank; handle consistently.
-   Use an enum or a small explicit mapping for supported sort values:
    `stars`, `forks`, `updated`.
-   Ensure `updated` maps to the entity's actual last-updated field.

Avoid silently accepting invalid sort values or falling back to an
unintended database field.

## 4. Verify GitHub API behavior and error handling

Review the existing GitHub API client and service. Ensure:

-   GitHub API requests use the configured base URL and optional token
    from environment configuration.
-   The GitHub token is never hardcoded.
-   Query parameters are encoded safely.
-   Connection and response timeouts are configured where practical.
-   GitHub's response is checked before attempting to map or persist
    data.
-   An empty `items` array returns a successful response with an empty
    repository list.
-   GitHub authentication/authorization failures are handled clearly.
-   GitHub rate limiting (HTTP 403 or 429, including rate-limit headers
    where available) is translated into a meaningful API error.
-   GitHub server errors and connectivity failures do not appear as
    misleading successful responses.
-   Error responses use a consistent JSON structure and appropriate HTTP
    status codes.

Suggested status behavior:

  Situation                                                     HTTP status
  ----------------------------------------------------------- -------------
  Successful search, including zero results                             200
  Invalid request or filter                                             400
  GitHub rate limit exceeded                                            429
  GitHub unavailable or upstream failure                         502 or 503
  Requested stored repository not found, if using get-by-ID             404
  Unexpected internal failure                                           500

Do not leak stack traces, credentials, tokens, or internal database
details in API responses.

## 5. Database and persistence verification

Review the existing `GitHubRepository` entity and JPA repository.

Confirm:

-   GitHub's repository ID is the primary key and is stored as a `Long`.
-   Repository fields include ID, name, description, owner login,
    language, stars, forks, and last-updated timestamp.
-   Nullable GitHub fields such as description and language are handled
    safely.
-   Existing repository records are updated on subsequent searches.
-   New repository records are inserted.
-   The persistence operation is transactional where appropriate.
-   Repeated searches do not create duplicate rows.

Use the existing JPA `saveAll` approach if it correctly performs
insert/update based on the entity ID. Do not add native upsert SQL
unless there is a demonstrated correctness or performance need.

## 6. Automated tests

The assignment explicitly prefers TDD, JUnit, and comprehensive test
coverage. Expand the current test suite beyond the existing seven tests.

Use JUnit 5, Mockito, Spring Boot Test, MockMvc, and the existing H2
test dependency where appropriate. Do not make real GitHub network calls
in automated tests.

Add or verify tests for at least these scenarios:

### Search API and service

1.  Valid search request returns repositories.
2.  Search query is missing.
3.  Search query is blank.
4.  Language filter is included in the GitHub query when provided.
5.  Each supported GitHub sort value is mapped correctly.
6.  GitHub returns an empty result list.
7.  GitHub returns an unexpected or malformed response.
8.  GitHub returns an authentication/authorization error.
9.  GitHub rate limit is handled.
10. GitHub server error or connection failure is handled.
11. New repositories are persisted.
12. Existing repository IDs are updated rather than duplicated.
13. Nullable description and language are handled.

### Stored repository retrieval

14. Retrieve all stored repositories without filters.
15. Filter by language.
16. Filter by minimum stars.
17. Apply language and minimum stars together.
18. Sort by stars.
19. Sort by forks.
20. Sort by last-updated date.
21. Default sorting is by stars.
22. Negative `minStars` is rejected.
23. Unsupported sort value is rejected.
24. Empty database returns an empty list.

### Test quality rules

-   Mock the GitHub API boundary in unit tests.
-   Use database-backed integration tests for persistence and dynamic
    filtering.
-   Ensure tests are deterministic and do not depend on the developer's
    local PostgreSQL data.
-   Assert HTTP status codes and response JSON fields for controller
    tests.
-   Test meaningful behavior, not merely that methods execute.
-   Keep test names descriptive.
-   Fix flaky tests and avoid disabling tests just to obtain a passing
    build.

Aim for broad coverage of the core requirements; do not chase an
arbitrary coverage percentage at the expense of meaningful tests.

## 7. API documentation and Postman verification

Update Swagger/OpenAPI documentation so both required endpoints have:

-   Clear summaries and descriptions.
-   Request body schema for search.
-   Optional query parameter descriptions for retrieval.
-   Supported sort values and defaults.
-   Representative success and error responses.

Create or update a Postman collection in the project (for example,
`postman/GitHub-Searcher.postman_collection.json`) containing:

1.  `POST /api/github/search` with the assignment's sample request.
2.  `GET /api/github/repositories` with no filters.
3.  Retrieval filtered by language.
4.  Retrieval filtered by minimum stars.
5.  Retrieval with language + minimum stars + sort.
6.  Invalid search request.
7.  Invalid sort value.

Use `http://localhost:8081` as the local base URL because this project
uses port 8081. Do not include credentials or tokens in the collection.

## 8. README and project hygiene

Update `README.md` to include:

-   Project purpose and features.
-   Technology stack and prerequisites.
-   Architecture and package responsibilities.
-   PostgreSQL database creation/configuration.
-   Environment variables and `.env.example`.
-   How to start the application using the existing `run.ps1`.
-   Maven build and test commands.
-   Swagger UI URL: `http://localhost:8081/swagger-ui.html` (verify the
    actual configured Swagger path).
-   Required endpoint documentation with example requests and responses.
-   Postman collection usage.
-   Notes about GitHub API authentication and rate limits.

Verify `.gitignore` excludes at least:

-   `.env`
-   `.m2/`
-   `target/`
-   IDE-specific files and local logs where appropriate.

Do not delete the developer's local `.env`. Do not print its contents.
If `.env` is tracked by Git, report that clearly and recommend removing
it from version control safely.

## 9. Final verification

After implementation:

1.  Run the Maven clean test build using the project's Maven wrapper or
    existing Maven setup.
2.  Run `mvn clean test`.
3.  Run `mvn clean package` if the test build succeeds.
4.  Start the Spring Boot application using the existing startup script.
5.  Verify the application starts on port 8081.
6.  Verify PostgreSQL connectivity if the local database is available.
7.  Verify the two required endpoints through HTTP/Postman or MockMvc.
8.  Confirm the search endpoint saves records and a repeated search
    updates existing records.
9.  Confirm the retrieval endpoint reads from PostgreSQL and applies
    filters/sorting.
10. Confirm Swagger UI exposes the required routes.

Do not claim a live API or database verification succeeded unless it was
actually executed. If an external dependency is unavailable, report the
exact verification that could not be completed and why.

## 10. Final report

When finished, provide a concise report containing:

-   Files created or modified.
-   Exact required endpoints and example request formats.
-   Tests added and total tests passing.
-   Maven build result.
-   Whether application startup was verified.
-   Whether PostgreSQL connectivity was verified.
-   Whether the Postman collection and README were updated.
-   Any remaining issues or manual steps.

## Non-negotiable constraints

-   Work in the existing project directory.
-   Preserve working functionality.
-   Make only changes relevant to assignment readiness.
-   Do not rewrite the application from scratch.
-   Do not add a frontend.
-   Do not hardcode secrets.
-   Do not fabricate test results.
-   Do not stop after editing: run the available tests and build, then
    report the actual results.

**Definition of done:** The existing application supports the exact two
required assignment endpoints, persists and updates GitHub repositories
without duplicates, retrieves stored records with the specified optional
filters and sorting, has meaningful automated tests, and includes clear
setup/API documentation.
