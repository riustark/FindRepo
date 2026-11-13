package com.example.githubsearcher.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/**
 * DTO representing details of a GitHub repository returned to clients.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepositoryResponse {

    private Long id;
    private String name;
    private String description;
    private String owner;
    private String language;
    private Integer stars;
    private Integer forks;
    private OffsetDateTime lastUpdated;
    private String htmlUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

