# Swagger Cleanup -- GitHub Repository Searcher

## Objective

Make the Swagger UI clearly present the **two APIs required by the
backend assignment**, without changing existing application behaviour or
breaking any existing functionality.

This is a documentation-only cleanup. The existing codebase must remain
backward-compatible.

## Required APIs to feature in Swagger

Keep these operations visible and clearly documented:

  ----------------------------------------------------------------------------
  Method                  Endpoint                     Purpose
  ----------------------- ---------------------------- -----------------------
  `POST`                  `/api/github/search`         Search GitHub
                                                       repositories and
                                                       persist the results in
                                                       PostgreSQL

  `GET`                   `/api/github/repositories`   Retrieve stored
                                                       repositories with
                                                       optional language,
                                                       minimum-stars, and
                                                       sorting filters
  ----------------------------------------------------------------------------

Confirm that both operations have the correct request/response schemas,
descriptions, parameter documentation, and example values.

## Optional APIs currently visible

The current Swagger UI also shows additional operations, including:

-   `GET /api/github/search`
-   `GET /api/repositories/search`
-   `POST /api/repositories/search`
-   `GET /api/repositories`
-   `GET /api/repositories/status`
-   `GET /api/github/status`
-   `GET /api/repositories/{id}`
-   `DELETE /api/repositories/{id}`
-   `GET /api/github/repositories/{id}`
-   `DELETE /api/github/repositories/{id}`
-   `GET /`

These may be useful extensions or compatibility routes. **Do not assume
they are safe to delete.**

## Strict safety rules

1.  **Do not delete, rename, rewrite, or change the URL or HTTP method
    of any existing endpoint.**
2.  Do not change controller logic, service logic, repository/database
    logic, entity mappings, DTOs, validation, exception handling, or
    GitHub API integration.
3.  Do not change request or response formats, status codes, filtering,
    sorting, persistence, or upsert behaviour.
4.  Do not remove legacy aliases. Other clients or tests may depend on
    them.
5.  Do not change database schema, application properties, environment
    variables, dependencies, or deployment configuration unless
    absolutely necessary for a Swagger-only change.
6.  Do not regenerate or restructure the project.
7.  Never expose secrets or edit `.env` values.
8.  Preserve all existing tests and Postman collection requests.

## Recommended implementation approach

First inspect the existing Springdoc/OpenAPI configuration and
controller annotations.

-   Prefer a **Swagger/OpenAPI documentation-only visibility change**.
-   If the project already has a suitable OpenAPI configuration, use it
    to hide optional operations from the default Swagger presentation
    while keeping the two required operations visible.
-   If no suitable configuration exists, use the smallest possible
    documentation-only change, such as Springdoc's `@Hidden` annotation
    on optional controller methods. This annotation must only affect API
    documentation; it must not remove or disable the actual route.
-   Do not add a new dependency if the existing Springdoc dependency
    already supports the required approach.
-   Do not hide an entire controller if it also contains either required
    endpoint.
-   If safely hiding an operation is unclear, leave it visible and
    report that rather than risking a behaviour change.

The preferred result is a focused Swagger UI with the two required
operations easy to find. Existing optional routes should continue to
work even if they are no longer displayed in Swagger.

## Step-by-step tasks for Antigravity

### Step 1 -- Inspect before editing

1.  Inspect all controller mappings and the current OpenAPI/Springdoc
    configuration.
2.  Identify exactly which controller methods implement:
    -   `POST /api/github/search`
    -   `GET /api/github/repositories`
3.  Identify duplicate aliases, status endpoints, ID-based operations,
    and the root redirect.
4.  Check whether any controller method maps more than one route or
    contains a required operation alongside optional operations.
5.  Record the files that would need a documentation-only change.

Do not edit anything during this inspection.

### Step 2 -- Make the smallest safe change

1.  Keep the two required operations visible in Swagger.
2.  Hide optional operations from Swagger only, using the safest
    supported approach in this project.
3.  Preserve every existing HTTP route and controller method.
4.  Add or improve concise Swagger summaries and descriptions for the
    two required operations.
5.  Document the search request fields (`query`, `language`, `sort`) and
    the retrieval filters (`language`, `minStars`, `sort`), including
    defaults and accepted sort values.
6.  Ensure the Swagger response schemas reflect the actual DTOs. Do not
    invent or alter response fields.

### Step 3 -- Verify no application behaviour changed

Run the existing test suite:

``` powershell
.\mvn.cmd clean test
```

Then build the application:

``` powershell
.\mvn.cmd clean package
```

If the project uses a different Maven wrapper or command, use the
existing project command instead.

Verify all of the following:

-   All existing tests still pass.
-   `POST /api/github/search` still works.
-   `GET /api/github/repositories` still works.
-   Existing optional routes still respond as they did before the
    change.
-   Swagger UI displays the two required operations clearly.
-   The OpenAPI JSON contains the two required operations.
-   No secrets appear in Swagger examples or generated documentation.

Do not claim a live API or deployment check unless it was actually
performed.

### Step 4 -- Review the change

Inspect the final Git diff.

The diff should contain only minimal Swagger/OpenAPI documentation
changes. If business logic, database code, route mappings, DTOs, or
application configuration changed unexpectedly, revert those unrelated
edits.

Create a short report named `SWAGGER_CLEANUP_REPORT.md` containing:

-   Files changed.
-   Which optional operations were hidden from Swagger.
-   Confirmation that their actual routes remain available.
-   Test and build results with actual counts.
-   Any optional operations intentionally left visible and why.

## Acceptance criteria

The task is complete only when:

-   [ ] `POST /api/github/search` is visible in Swagger.
-   [ ] `GET /api/github/repositories` is visible in Swagger.
-   [ ] Both operations accurately describe their real request and
    response contracts.
-   [ ] No existing route has been deleted, renamed, or behaviourally
    modified.
-   [ ] Optional routes remain functional even if hidden from Swagger.
-   [ ] Existing tests pass.
-   [ ] The Maven package build succeeds.
-   [ ] The final Git diff contains only the intended
    documentation-related changes.
-   [ ] `SWAGGER_CLEANUP_REPORT.md` records what was actually changed
    and verified.

## Final instruction

**Do not refactor or simplify the application itself.** The goal is only
to make the Swagger documentation easier for an evaluator to understand.
Preserve the current implementation, compatibility routes, tests, and
deployed behaviour.
