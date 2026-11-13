package com.example.githubsearcher.client;

import com.example.githubsearcher.exception.GitHubApiException;
import com.example.githubsearcher.exception.GitHubRateLimitException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Client for communicating with the GitHub REST API using Spring's RestClient.
 */
@Component
public class GitHubApiClient {

    private final RestClient restClient;

    public GitHubApiClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Searches GitHub repositories using the GitHub Search API.
     */
    public GitHubSearchApiResponse searchRepositories(String query, String sort, String order, int page, int perPage) {
        try {
            GitHubSearchApiResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search/repositories")
                            .queryParam("q", query)
                            .queryParam("sort", sort != null && !sort.isBlank() ? sort : "stars")
                            .queryParam("order", order != null && !order.isBlank() ? order : "desc")
                            .queryParam("page", page > 0 ? page : 1)
                            .queryParam("per_page", perPage > 0 ? Math.min(perPage, 100) : 30)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, clientResponse) -> {
                        int statusCode = clientResponse.getStatusCode().value();
                        if (statusCode == 403 || statusCode == 429) {
                            String retryAfter = clientResponse.getHeaders().getFirst("Retry-After");
                            if (retryAfter == null) {
                                retryAfter = clientResponse.getHeaders().getFirst("x-ratelimit-reset");
                            }
                            throw new GitHubRateLimitException(
                                    "GitHub API rate limit exceeded. Please wait before retrying or verify your personal access token.",
                                    retryAfter
                            );
                        }
                        if (statusCode == 401) {
                            throw new GitHubApiException(
                                    "GitHub API authentication failed. Please verify your configured token.",
                                    HttpStatus.BAD_GATEWAY
                            );
                        }
                        throw new GitHubApiException(
                                "GitHub API request failed with client error (HTTP " + statusCode + ").",
                                HttpStatus.BAD_GATEWAY
                        );
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, clientResponse) -> {
                        throw new GitHubApiException(
                                "GitHub API is currently unavailable (HTTP " + clientResponse.getStatusCode().value() + ").",
                                HttpStatus.SERVICE_UNAVAILABLE
                        );
                    })
                    .body(GitHubSearchApiResponse.class);

            if (response == null || response.items() == null) {
                return new GitHubSearchApiResponse(0, Collections.emptyList());
            }

            return response;

        } catch (ResourceAccessException ex) {
            throw new GitHubApiException(
                    "Failed to connect to GitHub API: " + ex.getMessage(),
                    HttpStatus.SERVICE_UNAVAILABLE
            );
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GitHubSearchApiResponse(
            @JsonProperty("total_count") long totalCount,
            @JsonProperty("items") List<GitHubItemDto> items
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GitHubItemDto(
            @JsonProperty("id") Long id,
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("owner") OwnerDto owner,
            @JsonProperty("language") String language,
            @JsonProperty("stargazers_count") Integer stars,
            @JsonProperty("forks_count") Integer forks,
            @JsonProperty("updated_at") OffsetDateTime updatedAt,
            @JsonProperty("html_url") String htmlUrl
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OwnerDto(
            @JsonProperty("login") String login
    ) {}
}
