package com.example.githubsearcher.dto;

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
public class SearchRequest {

    @NotBlank(message = "Search query must not be blank")
    private String query;

    private String language;

    private Integer minStars;

    @Builder.Default
    private String sort = "stars";

    @Builder.Default
    private String order = "desc";

    @Builder.Default
    private int page = 1;

    @Builder.Default
    private int perPage = 30;
}

