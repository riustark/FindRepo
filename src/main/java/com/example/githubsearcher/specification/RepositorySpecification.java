package com.example.githubsearcher.specification;

import com.example.githubsearcher.entity.GitHubRepository;
import org.springframework.data.jpa.domain.Specification;

/**
 * Specifications for querying and filtering GitHubRepository records dynamically.
 */
public final class RepositorySpecification {

    private RepositorySpecification() {
        // Utility class
    }

    /**
     * Filter by programming language (case-insensitive).
     */
    public static Specification<GitHubRepository> hasLanguage(String language) {
        return (root, query, criteriaBuilder) -> {
            if (language == null || language.isBlank()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(
                    criteriaBuilder.lower(root.get("language")),
                    language.trim().toLowerCase()
            );
        };
    }

    /**
     * Filter by minimum star count.
     */
    public static Specification<GitHubRepository> hasMinStars(Integer minStars) {
        return (root, query, criteriaBuilder) -> {
            if (minStars == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get("stars"), minStars);
        };
    }

    /**
     * Filter by matching keyword in repository name or description.
     */
    public static Specification<GitHubRepository> containsKeyword(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (keyword == null || keyword.isBlank()) {
                return criteriaBuilder.conjunction();
            }
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern)
            );
        };
    }
}
