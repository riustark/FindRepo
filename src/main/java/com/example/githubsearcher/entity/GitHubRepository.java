package com.example.githubsearcher.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/**
 * Entity representing a GitHub repository.
 * The GitHub repository ID is used as the primary key to prevent duplicates.
 */
@Entity
@Table(name = "github_repositories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GitHubRepository {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "owner", nullable = false)
    private String owner;

    @Column(name = "language")
    private String language;

    @Column(name = "stars", nullable = false)
    private Integer stars;

    @Column(name = "forks", nullable = false)
    private Integer forks;

    @Column(name = "last_updated")
    private OffsetDateTime lastUpdated;

    @Column(name = "html_url", nullable = false)
    private String htmlUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

