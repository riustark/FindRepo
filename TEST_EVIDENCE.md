# Test Evidence Report – GitHub Repository Searcher

This document details the test evidence for the Spring Boot GitHub Repository Searcher project, including automated JUnit 5 / Surefire reports, live HTTP API execution evidence, and PostgreSQL database verification.

---

## 1. Automated Test Execution Summary

- **Total Test Cases**: **43**
- **Passed**: **43 (100%)**
- **Failures**: **0**
- **Errors**: **0**
- **Skipped**: **0**
- **Build Status**: **`BUILD SUCCESS`**

### Test Class Breakdown

| Test Suite / Class | Type | Tests | Status | Execution Time |
| :--- | :--- | :---: | :---: | :---: |
| `GitHubApiClientTest` | Unit / MockRestServiceServer | **5** | **PASSED** | 1.214 s |
| `GitHubControllerTest` | WebMvc / MockMvc | **14** | **PASSED** | 6.742 s |
| `GitHubRepositoryDaoIntegrationTest` | DataJpaTest / H2 DB | **10** | **PASSED** | 2.005 s |
| `GitHubServiceTest` | Unit / Mockito | **13** | **PASSED** | 0.462 s |
| `GitHubSearcherApplicationTests` | Integration / ContextBootstrapper | **1** | **PASSED** | 5.941 s |
| **Total** | | **43** | **ALL PASSED** | **16.364 s** |

---

## 2. Detailed Test Case Catalog

### A. GitHub API Client Tests (`GitHubApiClientTest`)
1. ✅ `searchRepositoriesShouldReturnParsedResponse`: Verifies correct parsing of JSON response from GitHub Search API.
2. ✅ `searchRepositoriesShouldHandleEmptyItems`: Verifies graceful handling when GitHub returns 0 items.
3. ✅ `searchRepositoriesShouldThrowRateLimitExceptionOn403Or429`: Verifies translation of rate limit headers (`Retry-After`, `x-ratelimit-reset`) into `GitHubRateLimitException`.
4. ✅ `searchRepositoriesShouldThrowAuthenticationExceptionOn401`: Verifies mapping of invalid/expired token responses to `GitHubApiException`.
5. ✅ `searchRepositoriesShouldThrowServiceUnavailableOn500`: Verifies handling of upstream GitHub 5xx outages.

### B. Controller & Endpoint Tests (`GitHubControllerTest`)
1. ✅ `statusEndpointShouldReturnUp`: Verifies `/api/github/status` and alias `/api/repositories/status` return HTTP 200 `{"status":"UP"}`.
2. ✅ `searchEndpointShouldReturnFormattedResponseOnSuccess`: Verifies `POST /api/github/search` returns the required response format with `message` and `repositories` array.
3. ✅ `searchEndpointAliasShouldAlsoWork`: Verifies backward compatibility for `POST /api/repositories/search`.
4. ✅ `searchEndpointShouldRejectMissingQuery`: Verifies missing `query` field returns HTTP 400 Bad Request.
5. ✅ `searchEndpointShouldRejectBlankQuery`: Verifies whitespace-only `query` returns HTTP 400 Bad Request.
6. ✅ `searchEndpointShouldTranslateRateLimitTo429`: Verifies rate limit translates to HTTP 429 Too Many Requests with `Retry-After` header.
7. ✅ `searchEndpointShouldTranslateUpstreamErrorTo502`: Verifies upstream authentication/client error translates to HTTP 502 Bad Gateway.
8. ✅ `getStoredRepositoriesShouldReturnRepositoriesArray`: Verifies `GET /api/github/repositories` returns a `repositories` list with pagination metadata.
9. ✅ `getStoredRepositoriesShouldRejectNegativeMinStars`: Verifies `minStars < 0` returns HTTP 400 with a clear error message.
10. ✅ `getStoredRepositoriesShouldRejectInvalidSort`: Verifies unsupported sort parameters return HTTP 400 with valid options (`stars`, `forks`, `updated`).
11. ✅ `getStoredRepositoryByIdShouldReturnRepoWhenFound`: Verifies `GET /api/github/repositories/{id}` returns HTTP 200 with repository details.
12. ✅ `getStoredRepositoryByIdShouldReturn404WhenNotFound`: Verifies looking up a non-existent ID returns HTTP 404 Not Found.
13. ✅ `deleteStoredRepositoryShouldReturnNoContent`: Verifies `DELETE /api/github/repositories/{id}` returns HTTP 204 No Content.
14. ✅ `deleteStoredRepositoryShouldReturn404WhenMissing`: Verifies deleting a non-existent ID returns HTTP 404 Not Found.

### C. Database Persistence & Specification Tests (`GitHubRepositoryDaoIntegrationTest`)
1. ✅ `shouldPersistNewRepositories`: Verifies insert of new repository records into the database.
2. ✅ `shouldUpdateExistingRepositoryRatherThanDuplicate`: **(Idempotency test)** Verifies saving an existing GitHub repository ID updates the existing row and does not create duplicate records.
3. ✅ `shouldHandleNullableDescriptionAndLanguage`: Verifies repositories with null descriptions or languages persist without constraint violations.
4. ✅ `shouldReturnEmptyListWhenDatabaseIsEmpty`: Verifies empty database returns an empty page without errors.
5. ✅ `shouldFilterByLanguage`: Verifies case-insensitive filtering by programming language.
6. ✅ `shouldFilterByMinimumStars`: Verifies filtering by minimum star count (`stars >= minStars`).
7. ✅ `shouldFilterByLanguageAndMinimumStarsTogether`: Verifies multi-parameter filtering combines predicates with logical AND.
8. ✅ `shouldSortByStarsDescending`: Verifies database sorting by stars in descending order.
9. ✅ `shouldSortByForksDescending`: Verifies database sorting by forks in descending order.
10. ✅ `shouldSortByLastUpdatedDescending`: Verifies database sorting by last updated timestamp in descending order.

### D. Service Layer Tests (`GitHubServiceTest`)
1. ✅ `searchAndSaveShouldBuildQueryWithLanguageAndStars`: Verifies GitHub query concatenation (`spring boot language:Java stars:>=100`).
2. ✅ `searchAndSaveShouldRejectBlankQuery`: Verifies service validation for blank query strings.
3. ✅ `searchAndSaveShouldRejectNullQuery`: Verifies service validation for null query strings.
4. ✅ `searchAndSaveShouldSupportForksAndUpdatedSort`: Verifies proper propagation of `forks` and `updated` sort parameters to GitHub client.
5. ✅ `searchAndSaveShouldRejectUnsupportedSort`: Verifies rejection of unsupported sort values with descriptive message.
6. ✅ `searchAndSaveShouldHandleEmptyGitHubResults`: Verifies handling when GitHub returns 0 results.
7. ✅ `getStoredRepositoriesShouldRejectNegativeMinStars`: Verifies validation against negative star thresholds.
8. ✅ `getStoredRepositoriesShouldRejectInvalidSort`: Verifies safe sort field resolution against database properties.
9. ✅ `getStoredRepositoriesShouldReturnFilteredRepositories`: Verifies specification query and mapping to DTO list.
10. ✅ `getStoredRepositoryByIdShouldReturnRepositoryWhenFound`: Verifies lookup mapping.
11. ✅ `getStoredRepositoryByIdShouldThrowNotFoundWhenMissing`: Verifies `ResourceNotFoundException` is raised when missing.
12. ✅ `deleteStoredRepositoryShouldDeleteWhenFound`: Verifies deletion invocation.
13. ✅ `deleteStoredRepositoryShouldThrowWhenMissing`: Verifies delete throws `ResourceNotFoundException` when ID does not exist.

### E. Application Context Test (`GitHubSearcherApplicationTests`)
1. ✅ `contextLoads`: Verifies full Spring Boot application context bootstraps cleanly.

---

## 3. Live API Execution Evidence

### A. Live Search & Persistence (`POST /api/github/search`)
- **HTTP Method**: `POST`
- **URL**: `http://localhost:8081/api/github/search`
- **Request Body**:
  ```json
  {
    "query": "spring boot",
    "language": "Java",
    "sort": "stars"
  }
  ```
- **Response Status**: `HTTP/1.1 200 OK`
- **Response Payload (truncated sample)**:
  ```json
  {
    "message": "Repositories fetched and saved successfully",
    "repositories": [
      {
        "id": 127988011,
        "name": "mall",
        "description": "mall项目是一套电商系统，采用了 Spring Boot ...",
        "owner": "macrozheng",
        "language": "Java",
        "stars": 85044,
        "forks": 29824,
        "lastUpdated": "2026-11-13T08:20:28Z",
        "htmlUrl": "https://github.com/macrozheng/mall",
        "createdAt": "2026-11-13T12:44:02.566",
        "updatedAt": "2026-11-13T14:32:09.112"
      },
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
        "updatedAt": "2026-11-13T14:32:09.112"
      }
    ],
    "totalCount": 609765,
    "page": 1,
    "perPage": 30
  }
  ```

### B. Live Repeated Search (Duplicate Prevention Evidence)
- **Action**: Repeated identical search query for `"spring boot"`.
- **Result**: `HTTP 200 OK`. Records were updated in place in PostgreSQL without unique constraint violations or duplicate primary keys. Total records remained consistent.

### C. Live Filtered Retrieval (`GET /api/github/repositories`)
- **HTTP Method**: `GET`
- **URL**: `http://localhost:8081/api/github/repositories?language=Java&minStars=100&sort=stars`
- **Response Status**: `HTTP/1.1 200 OK`
- **Response Payload (truncated sample)**:
  ```json
  {
    "repositories": [
      {
        "id": 127988011,
        "name": "mall",
        "owner": "macrozheng",
        "language": "Java",
        "stars": 85044,
        "forks": 29824,
        "lastUpdated": "2026-11-13T08:20:28Z",
        "htmlUrl": "https://github.com/macrozheng/mall"
      },
      {
        "id": 6296790,
        "name": "spring-boot",
        "owner": "spring-projects",
        "language": "Java",
        "stars": 81910,
        "forks": 41200,
        "lastUpdated": "2026-11-13T12:51:55Z",
        "htmlUrl": "https://github.com/spring-projects/spring-boot"
      }
    ],
    "totalCount": 20,
    "page": 0,
    "size": 20,
    "totalPages": 1
  }
  ```

### D. Live Validation Error Rejections
1. **Blank Search Query**:
   - `POST /api/github/search` with `{ "query": "   " }`
   - Response: `HTTP/1.1 400 Bad Request`
   - Body: `{"status": 400, "error": "Bad Request", "message": "Search query must not be blank"}`
2. **Invalid Sort Field**:
   - `GET /api/github/repositories?sort=invalid_column`
   - Response: `HTTP/1.1 400 Bad Request`
   - Body: `{"status": 400, "error": "Bad Request", "message": "Invalid sort parameter 'invalid_column'. Supported values are: stars, forks, updated"}`
3. **Negative Minimum Stars**:
   - `GET /api/github/repositories?minStars=-10`
   - Response: `HTTP/1.1 400 Bad Request`
   - Body: `{"status": 400, "error": "Bad Request", "message": "minStars must be greater than or equal to 0"}`

---

## 4. PostgreSQL Database Live State Evidence

Executing SQL against database `github_searcher` in PostgreSQL 17:
```sql
SELECT id, owner, name, stars, forks, language, last_updated 
FROM github_repositories 
ORDER BY stars DESC 
LIMIT 5;
```

**Output**:
```text
    id     |      owner      |       name       | stars  | forks |  language  |       last_updated        
-----------+-----------------+------------------+--------+-------+------------+---------------------------
  10270250 | react           | react            | 252565 | 46800 | JavaScript | 2026-11-13 13:23:37+05:30
  70107786 | vercel          | next.js          | 143498 | 26900 | JavaScript | 2026-11-13 13:22:10+05:30
  63537249 | react           | create-react-app | 103232 | 26100 | JavaScript | 2026-11-13 04:16:40+05:30
  23083156 | mui             | material-ui      |  99340 | 31800 | JavaScript | 2026-11-13 12:54:08+05:30
 127988011 | macrozheng      | mall             |  85044 | 29824 | Java       | 2026-11-13 13:50:28+05:30
```

