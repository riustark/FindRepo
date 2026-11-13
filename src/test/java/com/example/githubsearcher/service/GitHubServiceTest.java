package com.example.githubsearcher.service;

import com.example.githubsearcher.client.GitHubApiClient;
import com.example.githubsearcher.dto.RepositoryListResponse;
import com.example.githubsearcher.dto.SearchRequest;
import com.example.githubsearcher.dto.SearchResponse;
import com.example.githubsearcher.entity.GitHubRepository;
import com.example.githubsearcher.exception.ResourceNotFoundException;
import com.example.githubsearcher.repository.GitHubRepositoryDao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitHubServiceTest {

    @Mock
    private GitHubRepositoryDao gitHubRepositoryDao;

    @Mock
    private GitHubApiClient gitHubApiClient;

    @InjectMocks
    private GitHubService gitHubService;

    @Test
    void searchAndSaveShouldBuildQueryWithLanguageAndStars() {
        GitHubApiClient.GitHubItemDto item = new GitHubApiClient.GitHubItemDto(
                12345L,
                "spring-boot-example",
                "Sample description",
                new GitHubApiClient.OwnerDto("spring-user"),
                "Java",
                450,
                120,
                OffsetDateTime.now(),
                "https://github.com/spring-user/spring-boot-example"
        );

        when(gitHubApiClient.searchRepositories(anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(new GitHubApiClient.GitHubSearchApiResponse(1, List.of(item)));

        when(gitHubRepositoryDao.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        SearchRequest request = SearchRequest.builder()
                .query("spring boot")
                .language("Java")
                .minStars(100)
                .sort("stars")
                .build();

        SearchResponse response = gitHubService.searchAndSave(request);

        assertThat(response.getMessage()).isEqualTo("Repositories fetched and saved successfully");
        assertThat(response.getRepositories()).hasSize(1);
        assertThat(response.getRepositories().get(0).getName()).isEqualTo("spring-boot-example");

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(gitHubApiClient).searchRepositories(queryCaptor.capture(), eq("stars"), anyString(), anyInt(), anyInt());
        assertThat(queryCaptor.getValue()).isEqualTo("spring boot language:Java stars:>=100");
    }

    @Test
    void searchAndSaveShouldRejectBlankQuery() {
        SearchRequest request = SearchRequest.builder().query("   ").build();

        assertThatThrownBy(() -> gitHubService.searchAndSave(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Search query must not be blank");
    }

    @Test
    void searchAndSaveShouldRejectNullQuery() {
        SearchRequest request = SearchRequest.builder().query(null).build();

        assertThatThrownBy(() -> gitHubService.searchAndSave(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Search query must not be blank");
    }

    @Test
    void searchAndSaveShouldSupportForksAndUpdatedSort() {
        when(gitHubApiClient.searchRepositories(anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(new GitHubApiClient.GitHubSearchApiResponse(0, Collections.emptyList()));
        when(gitHubRepositoryDao.saveAll(any())).thenReturn(Collections.emptyList());

        SearchRequest forksReq = SearchRequest.builder().query("test").sort("forks").build();
        gitHubService.searchAndSave(forksReq);
        verify(gitHubApiClient).searchRepositories(anyString(), eq("forks"), anyString(), anyInt(), anyInt());

        SearchRequest updatedReq = SearchRequest.builder().query("test").sort("updated").build();
        gitHubService.searchAndSave(updatedReq);
        verify(gitHubApiClient).searchRepositories(anyString(), eq("updated"), anyString(), anyInt(), anyInt());
    }

    @Test
    void searchAndSaveShouldRejectUnsupportedSort() {
        SearchRequest request = SearchRequest.builder().query("test").sort("invalid_sort").build();

        assertThatThrownBy(() -> gitHubService.searchAndSave(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Supported values are: stars, forks, updated");
    }

    @Test
    void searchAndSaveShouldHandleEmptyGitHubResults() {
        when(gitHubApiClient.searchRepositories(anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(new GitHubApiClient.GitHubSearchApiResponse(0, Collections.emptyList()));
        when(gitHubRepositoryDao.saveAll(any())).thenReturn(Collections.emptyList());

        SearchRequest request = SearchRequest.builder().query("nonexistent-repo-123456").build();
        SearchResponse response = gitHubService.searchAndSave(request);

        assertThat(response.getRepositories()).isEmpty();
        assertThat(response.getTotalCount()).isEqualTo(0);
    }

    @Test
    void getStoredRepositoriesShouldRejectNegativeMinStars() {
        assertThatThrownBy(() -> gitHubService.getStoredRepositories("Java", -5, null, "stars", "DESC", 0, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minStars must be greater than or equal to 0");
    }

    @Test
    void getStoredRepositoriesShouldRejectInvalidSort() {
        assertThatThrownBy(() -> gitHubService.getStoredRepositories("Java", 10, null, "unknown_field", "DESC", 0, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Supported values are: stars, forks, updated");
    }

    @Test
    void getStoredRepositoriesShouldReturnFilteredRepositories() {
        GitHubRepository repo = GitHubRepository.builder()
                .id(1001L)
                .name("spring-framework")
                .owner("spring-projects")
                .language("Java")
                .stars(55000)
                .forks(37000)
                .build();

        when(gitHubRepositoryDao.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(repo)));

        RepositoryListResponse response = gitHubService.getStoredRepositories("Java", 1000, null, "stars", "DESC", 0, 20);

        assertThat(response.getRepositories()).hasSize(1);
        assertThat(response.getRepositories().get(0).getName()).isEqualTo("spring-framework");
        assertThat(response.getTotalCount()).isEqualTo(1);
    }

    @Test
    void getStoredRepositoryByIdShouldReturnRepositoryWhenFound() {
        GitHubRepository repo = GitHubRepository.builder().id(1001L).name("spring-framework").build();
        when(gitHubRepositoryDao.findById(1001L)).thenReturn(Optional.of(repo));

        assertThat(gitHubService.getStoredRepositoryById(1001L).getName()).isEqualTo("spring-framework");
    }

    @Test
    void getStoredRepositoryByIdShouldThrowNotFoundWhenMissing() {
        when(gitHubRepositoryDao.findById(9999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gitHubService.getStoredRepositoryById(9999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Repository not found with ID: 9999");
    }

    @Test
    void deleteStoredRepositoryShouldDeleteWhenFound() {
        when(gitHubRepositoryDao.existsById(1001L)).thenReturn(true);

        gitHubService.deleteStoredRepository(1001L);

        verify(gitHubRepositoryDao).deleteById(1001L);
    }

    @Test
    void deleteStoredRepositoryShouldThrowWhenMissing() {
        when(gitHubRepositoryDao.existsById(9999L)).thenReturn(false);

        assertThatThrownBy(() -> gitHubService.deleteStoredRepository(9999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
