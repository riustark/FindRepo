package com.example.githubsearcher.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Response container for retrieved stored repositories, returning a 'repositories' array
 * alongside pagination metadata.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepositoryListResponse {

    @JsonProperty("repositories")
    private List<RepositoryResponse> repositories;

    private long totalCount;
    private int page;
    private int size;
    private int totalPages;
}

