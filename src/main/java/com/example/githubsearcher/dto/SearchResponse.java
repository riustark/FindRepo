package com.example.githubsearcher.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * DTO representing search response after persisting repositories.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResponse {

    @Builder.Default
    private String message = "Repositories fetched and saved successfully";

    @JsonProperty("repositories")
    private List<RepositoryResponse> repositories;

    private long totalCount;
    private int page;
    private int perPage;

    // Backward-compatibility alias for items
    public List<RepositoryResponse> getItems() {
        return repositories;
    }

    public void setItems(List<RepositoryResponse> items) {
        this.repositories = items;
    }
}
