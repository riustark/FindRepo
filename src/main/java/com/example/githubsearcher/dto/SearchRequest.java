package com.example.githubsearcher.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO representing repository search and filter criteria.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Criteria for searching GitHub repositories and persisting results")
public class SearchRequest {

    @Schema(description = "Keyword or repository name to search on GitHub", example = "spring-boot", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Search query must not be blank")
    private String query;

    @Schema(description = "Filter repositories by primary programming language", example = "Java")
    private String language;

    @Schema(description = "Filter repositories having at least this many stars", example = "5000")
    private Integer minStars;

    @Schema(description = "Sort field: 'stars', 'forks', or 'updated'", example = "stars", defaultValue = "stars")
    @Builder.Default
    private String sort = "stars";

    @Schema(description = "Sort order: 'desc' or 'asc'", example = "desc", defaultValue = "desc")
    @Builder.Default
    private String order = "desc";

    @Schema(description = "Page number (1-based index)", example = "1", defaultValue = "1")
    @Builder.Default
    private int page = 1;

    @Schema(description = "Number of repositories to fetch per page (max 100)", example = "10", defaultValue = "30")
    @Builder.Default
    private int perPage = 30;
}

