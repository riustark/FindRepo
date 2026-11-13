# Antigravity Project Setup Prompt – GitHub Repository Searcher

Act as a Senior Java Spring Boot Developer. Set up a clean, modular Spring Boot backend project for the following assignment.

## 1. Project Objective

Build a REST API application that searches GitHub repositories using the GitHub REST API, stores repository information in PostgreSQL, and allows users to retrieve stored repositories using filters and sorting.

For this phase, focus ONLY on project initialization, dependencies, configuration, and basic application structure. Do not implement the complete business logic yet.

## 2. Technology Stack

Use the following technologies:

- Java 17
- Spring Boot 3.x
- Maven
- Spring Web
- Spring Data JPA
- PostgreSQL
- Hibernate
- Jakarta Validation
- Lombok (only where it genuinely reduces boilerplate)
- JUnit 5
- Mockito
- Spring Boot Test
- Springdoc OpenAPI

Use RestClient for future GitHub API integration.

## 3. Project Structure

Create the following package structure:

```text
com.example.githubsearcher
│
├── config/
│   └── RestClientConfig.java
│
├── controller/
│   └── GitHubController.java
│
├── service/
│   └── GitHubService.java
│
├── client/
│   └── GitHubApiClient.java
│
├── repository/
│   └── GitHubRepositoryDao.java
│
├── entity/
│   └── GitHubRepository.java
│
├── dto/
│   ├── SearchRequest.java
│   ├── SearchResponse.java
│   └── RepositoryResponse.java
│
├── specification/
│   └── RepositorySpecification.java
│
├── exception/
│   └── GlobalExceptionHandler.java
│
└── GitHubSearcherApplication.java
```

Also create:

```text
src/
├── main/
│   ├── java/
│   └── resources/
│       └── application.properties
│
└── test/
    └── java/
```

## 4. Initial Configuration

Configure `application.properties` with:

- Application name: github-searcher
- PostgreSQL database connection properties
- Hibernate DDL auto: update (development only)
- Show SQL enabled
- Server port: 8080
- GitHub API base URL: https://api.github.com
- GitHub token through an environment variable (optional)

Do not hardcode database credentials or access tokens.

Provide an `.env.example` file documenting the expected environment variables.

## 5. Database Entity

Create the initial GitHubRepository entity with these fields:

- id: Long (GitHub repository ID, primary key)
- name: String
- description: String
- owner: String
- language: String
- stars: Integer
- forks: Integer
- lastUpdated: OffsetDateTime
- htmlUrl: String
- createdAt: LocalDateTime
- updatedAt: LocalDateTime

Use appropriate JPA annotations and sensible nullable constraints.

The GitHub repository ID must be the primary key to prevent duplicate repository records.

## 6. Initial Classes

Create minimal class definitions for the other packages.

Requirements:

- GitHubController: REST controller placeholder.
- GitHubService: Service layer placeholder.
- GitHubApiClient: External API client placeholder.
- GitHubRepositoryDao: Extend JpaRepository and JpaSpecificationExecutor.
- RepositorySpecification: Placeholder for dynamic filtering.
- RestClientConfig: Configure a reusable Spring RestClient bean.
- GlobalExceptionHandler: Placeholder using @RestControllerAdvice.

Do not add unnecessary methods or complicated abstractions at this stage.

## 7. Code Quality Rules

Follow these rules strictly:

1. Use clean, readable Java code.
2. Follow standard Spring Boot layered architecture.
3. Keep classes small and focused.
4. Avoid unnecessary interfaces and design patterns.
5. Avoid overengineering.
6. Use constructor injection instead of field injection.
7. Use meaningful class and variable names.
8. Keep business logic out of controllers.
9. Do not create unnecessary packages or files.
10. Ensure the application compiles successfully.

## 8. Documentation

Create a README.md containing:

- Project overview.
- Technology stack.
- Project architecture.
- Prerequisites.
- PostgreSQL database setup instructions.
- Environment variable configuration.
- Maven build and run commands.
- Current implementation status.

## 9. Verification

After generating the project:

1. Run `mvn clean compile`.
2. Run `mvn test`.
3. Resolve all compilation errors.
4. Verify that the Spring Boot application starts.
5. Verify PostgreSQL connectivity if a local database is configured.
6. Report any missing external configuration separately.

## 10. Important Instructions

- Do not implement GitHub search functionality yet.
- Do not implement database filtering yet.
- Do not create a frontend.
- Do not use Docker unless necessary.
- Do not generate excessive boilerplate.
- Do not modify unrelated files.

**Expected outcome:** A working, compilable Spring Boot project with PostgreSQL configuration, the required package structure, an initial entity, and foundational classes ready for incremental implementation.