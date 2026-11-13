package com.example.githubsearcher.service;

import com.example.githubsearcher.client.GitHubApiClient;
import com.example.githubsearcher.dto.RepositoryListResponse;
import com.example.githubsearcher.dto.RepositoryResponse;
import com.example.githubsearcher.dto.SearchRequest;
import com.example.githubsearcher.dto.SearchResponse;
import com.example.githubsearcher.dto.SortField;
import com.example.githubsearcher.entity.GitHubRepository;
import com.example.githubsearcher.exception.ResourceNotFoundException;
import com.example.githubsearcher.repository.GitHubRepositoryDao;
import com.example.githubsearcher.specification.RepositorySpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service managing business logic for searching GitHub repositories,
 * persisting them to PostgreSQL, and querying stored repositories with dynamic filters.
 */
@Service
public class GitHubService {

    private final GitHubRepositoryDao gitHubRepositoryDao;
    private final GitHubApiClient gitHubApiClient;

    public GitHubService(GitHubRepositoryDao gitHubRepositoryDao, GitHubApiClient gitHubApiClient) {
        this.gitHubRepositoryDao = gitHubRepositoryDao;
        this.gitHubApiClient = gitHubApiClient;
    }

    /**
     * Searches GitHub via REST API, persists/updates the repositories in PostgreSQL,
     * and returns the saved repository details.
     */
    @Transactional
    public SearchResponse searchAndSave(SearchRequest request) {
        if (request.getQuery() == null || request.getQuery().isBlank()) {
            throw new IllegalArgumentException("Search query must not be blank");
        }

        SortField sortField = SortField.fromString(request.getSort());
        String fullQuery = buildGitHubQuery(request);

        GitHubApiClient.GitHubSearchApiResponse apiResponse = gitHubApiClient.searchRepositories(
                fullQuery,
                sortField.getClientValue(),
                request.getOrder(),
                request.getPage(),
                request.getPerPage()
        );

        List<GitHubRepository> entities = apiResponse.items().stream()
                .map(this::mapDtoToEntity)
                .toList();

        List<GitHubRepository> saved = gitHubRepositoryDao.saveAll(entities);

        List<RepositoryResponse> responseItems = saved.stream()
                .map(this::mapEntityToResponse)
                .toList();

        return SearchResponse.builder()
                .message("Repositories fetched and saved successfully")
                .repositories(responseItems)
                .totalCount(apiResponse.totalCount())
                .page(request.getPage())
                .perPage(request.getPerPage())
                .build();
    }

    /**
     * Retrieves stored repositories from PostgreSQL using dynamic filtering, safe sorting, and pagination.
     */
    @Transactional(readOnly = true)
    public RepositoryListResponse getStoredRepositories(
            String language,
            Integer minStars,
            String search,
            String sortParam,
            String direction,
            int page,
            int size
    ) {
        if (minStars != null && minStars < 0) {
            throw new IllegalArgumentException("minStars must be greater than or equal to 0");
        }

        SortField sortField = SortField.fromString(sortParam);
        Sort.Direction sortDirection = "ASC".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(sortDirection, sortField.getDatabaseProperty()));

        Specification<GitHubRepository> spec = Specification
                .where(RepositorySpecification.hasLanguage(language))
                .and(RepositorySpecification.hasMinStars(minStars))
                .and(RepositorySpecification.containsKeyword(search));

        Page<GitHubRepository> repoPage = gitHubRepositoryDao.findAll(spec, pageable);

        List<RepositoryResponse> mappedList = repoPage.getContent().stream()
                .map(this::mapEntityToResponse)
                .toList();

        return RepositoryListResponse.builder()
                .repositories(mappedList)
                .totalCount(repoPage.getTotalElements())
                .page(repoPage.getNumber())
                .size(repoPage.getSize())
                .totalPages(repoPage.getTotalPages())
                .build();
    }

    /**
     * Retrieves a single repository by its GitHub ID from the local database.
     */
    @Transactional(readOnly = true)
    public RepositoryResponse getStoredRepositoryById(Long id) {
        return gitHubRepositoryDao.findById(id)
                .map(this::mapEntityToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found with ID: " + id));
    }

    /**
     * Deletes a repository record from the local database by its GitHub ID.
     */
    @Transactional
    public void deleteStoredRepository(Long id) {
        if (!gitHubRepositoryDao.existsById(id)) {
            throw new ResourceNotFoundException("Repository not found with ID: " + id);
        }
        gitHubRepositoryDao.deleteById(id);
    }

    private String buildGitHubQuery(SearchRequest request) {
        StringBuilder query = new StringBuilder(request.getQuery().trim());

        if (request.getLanguage() != null && !request.getLanguage().isBlank()) {
            query.append(" language:").append(request.getLanguage().trim());
        }
        if (request.getMinStars() != null && request.getMinStars() > 0) {
            query.append(" stars:>=").append(request.getMinStars());
        }

        return query.toString();
    }

    private GitHubRepository mapDtoToEntity(GitHubApiClient.GitHubItemDto item) {
        return GitHubRepository.builder()
                .id(item.id())
                .name(item.name())
                .description(item.description())
                .owner(item.owner() != null && item.owner().login() != null ? item.owner().login() : "unknown")
                .language(item.language())
                .stars(item.stars() != null ? item.stars() : 0)
                .forks(item.forks() != null ? item.forks() : 0)
                .lastUpdated(item.updatedAt())
                .htmlUrl(item.htmlUrl() != null ? item.htmlUrl() : "")
                .build();
    }

    private RepositoryResponse mapEntityToResponse(GitHubRepository entity) {
        return RepositoryResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .owner(entity.getOwner())
                .language(entity.getLanguage())
                .stars(entity.getStars())
                .forks(entity.getForks())
                .lastUpdated(entity.getLastUpdated())
                .htmlUrl(entity.getHtmlUrl())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
