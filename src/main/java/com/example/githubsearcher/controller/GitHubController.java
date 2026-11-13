package com.example.githubsearcher.controller;

import com.example.githubsearcher.dto.RepositoryListResponse;
import com.example.githubsearcher.dto.RepositoryResponse;
import com.example.githubsearcher.dto.SearchRequest;
import com.example.githubsearcher.dto.SearchResponse;
import com.example.githubsearcher.service.GitHubService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

/**
 * REST controller exposing primary assignment endpoints under /api/github,
 * while maintaining backward-compatible aliases under /api/repositories.
 */
@Tag(name = "GitHub Repositories", description = "Core APIs: Search GitHub repositories, persist in PostgreSQL, and retrieve stored records with filters and sorting")
@Validated
@RestController
public class GitHubController {

    private final GitHubService gitHubService;

    public GitHubController(GitHubService gitHubService) {
        this.gitHubService = gitHubService;
    }

    @Hidden
    @GetMapping("/")
    public ResponseEntity<Void> root() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/swagger-ui.html"))
                .build();
    }

    @Hidden
    @GetMapping({"/api/github/status", "/api/repositories/status"})
    public Map<String, String> status() {
        return Map.of("status", "UP", "service", "github-searcher");
    }

    @Operation(
            summary = "1. Search GitHub repositories and persist results to PostgreSQL",
            description = "Searches GitHub using the configured token, maps repository details, upserts existing records to prevent duplicates, and returns saved repositories."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Repositories fetched from GitHub and successfully saved to PostgreSQL",
                    content = @Content(schema = @Schema(implementation = SearchResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload (e.g. blank query)"),
            @ApiResponse(responseCode = "429", description = "GitHub API rate limit exceeded")
    })
    @PostMapping("/api/github/search")
    public ResponseEntity<SearchResponse> searchGitHubRepositories(
            @Valid @RequestBody SearchRequest request
    ) {
        SearchResponse response = gitHubService.searchAndSave(request);
        return ResponseEntity.ok(response);
    }

    @Hidden
    @PostMapping("/api/repositories/search")
    public ResponseEntity<SearchResponse> searchGitHubRepositoriesAlias(
            @Valid @RequestBody SearchRequest request
    ) {
        return searchGitHubRepositories(request);
    }

    @Hidden
    @GetMapping({"/api/github/search", "/api/repositories/search"})
    public ResponseEntity<SearchResponse> searchGitHubRepositoriesGet(
            @RequestParam @NotBlank(message = "Search query must not be blank") String query,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) Integer minStars,
            @RequestParam(defaultValue = "stars") String sort,
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int perPage
    ) {
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .language(language)
                .minStars(minStars)
                .sort(sort)
                .order(order)
                .page(page)
                .perPage(perPage)
                .build();

        return ResponseEntity.ok(gitHubService.searchAndSave(request));
    }

    @Operation(
            summary = "2. Retrieve stored repositories from PostgreSQL",
            description = "Reads from PostgreSQL with optional filters for language, minimum stars, keyword search, safe sorting (stars, forks, updated), and pagination."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stored repositories retrieved successfully",
                    content = @Content(schema = @Schema(implementation = RepositoryListResponse.class)))
    })
    @GetMapping("/api/github/repositories")
    public ResponseEntity<RepositoryListResponse> getStoredRepositories(
            @Parameter(description = "Filter by programming language (e.g. Java, Python, TypeScript)")
            @RequestParam(required = false) String language,

            @Parameter(description = "Filter repositories having at least this many stars")
            @RequestParam(required = false) Integer minStars,

            @Parameter(description = "Optional keyword to search in name or description")
            @RequestParam(required = false) String search,

            @Parameter(description = "Sort property: 'stars', 'forks', or 'updated' (default: 'stars')")
            @RequestParam(defaultValue = "stars") String sort,

            @Parameter(description = "Sort direction: 'DESC' or 'ASC' (default: 'DESC')")
            @RequestParam(defaultValue = "DESC") String direction,

            @Parameter(description = "Zero-indexed page number (default: 0)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size (default: 20)")
            @RequestParam(defaultValue = "20") int size
    ) {
        RepositoryListResponse response = gitHubService.getStoredRepositories(
                language,
                minStars,
                search,
                sort,
                direction,
                page,
                size
        );
        return ResponseEntity.ok(response);
    }

    @Hidden
    @GetMapping("/api/repositories")
    public ResponseEntity<RepositoryListResponse> getStoredRepositoriesAlias(
            @RequestParam(required = false) String language,
            @RequestParam(required = false) Integer minStars,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "stars") String sort,
            @RequestParam(defaultValue = "DESC") String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return getStoredRepositories(language, minStars, search, sort, direction, page, size);
    }

    @Hidden
    @GetMapping({"/api/github/repositories/{id}", "/api/repositories/{id}"})
    public ResponseEntity<RepositoryResponse> getStoredRepositoryById(@PathVariable Long id) {
        return ResponseEntity.ok(gitHubService.getStoredRepositoryById(id));
    }

    @Hidden
    @DeleteMapping({"/api/github/repositories/{id}", "/api/repositories/{id}"})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStoredRepository(@PathVariable Long id) {
        gitHubService.deleteStoredRepository(id);
    }
}
