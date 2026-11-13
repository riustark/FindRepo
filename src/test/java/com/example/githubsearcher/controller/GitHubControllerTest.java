package com.example.githubsearcher.controller;

import com.example.githubsearcher.dto.RepositoryListResponse;
import com.example.githubsearcher.dto.RepositoryResponse;
import com.example.githubsearcher.dto.SearchRequest;
import com.example.githubsearcher.dto.SearchResponse;
import com.example.githubsearcher.exception.GitHubApiException;
import com.example.githubsearcher.exception.GitHubRateLimitException;
import com.example.githubsearcher.exception.ResourceNotFoundException;
import com.example.githubsearcher.service.GitHubService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GitHubController.class)
class GitHubControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GitHubService gitHubService;

    @Test
    void statusEndpointShouldReturnUp() throws Exception {
        mockMvc.perform(get("/api/github/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("github-searcher"));

        // Verify alias
        mockMvc.perform(get("/api/repositories/status"))
                .andExpect(status().isOk());
    }

    @Test
    void searchEndpointShouldReturnFormattedResponseOnSuccess() throws Exception {
        SearchResponse mockResponse = SearchResponse.builder()
                .message("Repositories fetched and saved successfully")
                .repositories(List.of(
                        RepositoryResponse.builder()
                                .id(123456L)
                                .name("spring-boot-example")
                                .owner("user123")
                                .language("Java")
                                .stars(450)
                                .forks(120)
                                .build()
                ))
                .totalCount(1)
                .build();

        when(gitHubService.searchAndSave(any(SearchRequest.class))).thenReturn(mockResponse);

        SearchRequest request = SearchRequest.builder()
                .query("spring boot")
                .language("Java")
                .sort("stars")
                .build();

        mockMvc.perform(post("/api/github/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Repositories fetched and saved successfully"))
                .andExpect(jsonPath("$.repositories[0].id").value(123456))
                .andExpect(jsonPath("$.repositories[0].name").value("spring-boot-example"));
    }

    @Test
    void searchEndpointAliasShouldAlsoWork() throws Exception {
        SearchResponse mockResponse = SearchResponse.builder()
                .message("Repositories fetched and saved successfully")
                .repositories(List.of())
                .build();

        when(gitHubService.searchAndSave(any(SearchRequest.class))).thenReturn(mockResponse);

        SearchRequest request = SearchRequest.builder().query("spring").build();

        mockMvc.perform(post("/api/repositories/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Repositories fetched and saved successfully"));
    }

    @Test
    void searchEndpointShouldRejectMissingQuery() throws Exception {
        String invalidJson = """
                {
                    "language": "Java",
                    "sort": "stars"
                }
                """;

        mockMvc.perform(post("/api/github/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void searchEndpointShouldRejectBlankQuery() throws Exception {
        String invalidJson = """
                {
                    "query": "   ",
                    "language": "Java"
                }
                """;

        mockMvc.perform(post("/api/github/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void searchEndpointShouldTranslateRateLimitTo429() throws Exception {
        when(gitHubService.searchAndSave(any(SearchRequest.class)))
                .thenThrow(new GitHubRateLimitException("Rate limit exceeded", "60"));

        SearchRequest request = SearchRequest.builder().query("popular").build();

        mockMvc.perform(post("/api/github/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(header().string("Retry-After", "60"));
    }

    @Test
    void searchEndpointShouldTranslateUpstreamErrorTo502() throws Exception {
        when(gitHubService.searchAndSave(any(SearchRequest.class)))
                .thenThrow(new GitHubApiException("GitHub auth failed", HttpStatus.BAD_GATEWAY));

        SearchRequest request = SearchRequest.builder().query("test").build();

        mockMvc.perform(post("/api/github/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502));
    }

    @Test
    void getStoredRepositoriesShouldReturnRepositoriesArray() throws Exception {
        RepositoryResponse repo = RepositoryResponse.builder()
                .id(1001L)
                .name("spring-boot")
                .owner("spring-projects")
                .stars(70000)
                .build();

        RepositoryListResponse mockResponse = RepositoryListResponse.builder()
                .repositories(List.of(repo))
                .totalCount(1)
                .page(0)
                .size(20)
                .totalPages(1)
                .build();

        when(gitHubService.getStoredRepositories(any(), any(), any(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/api/github/repositories")
                        .param("language", "Java")
                        .param("minStars", "100")
                        .param("sort", "stars"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repositories[0].name").value("spring-boot"))
                .andExpect(jsonPath("$.totalCount").value(1));
    }

    @Test
    void getStoredRepositoriesShouldRejectNegativeMinStars() throws Exception {
        when(gitHubService.getStoredRepositories(any(), eq(-1), any(), anyString(), anyString(), anyInt(), anyInt()))
                .thenThrow(new IllegalArgumentException("minStars must be greater than or equal to 0"));

        mockMvc.perform(get("/api/github/repositories").param("minStars", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("minStars must be greater than or equal to 0"));
    }

    @Test
    void getStoredRepositoriesShouldRejectInvalidSort() throws Exception {
        when(gitHubService.getStoredRepositories(any(), any(), any(), eq("invalid_field"), anyString(), anyInt(), anyInt()))
                .thenThrow(new IllegalArgumentException("Invalid sort parameter 'invalid_field'. Supported values are: stars, forks, updated"));

        mockMvc.perform(get("/api/github/repositories").param("sort", "invalid_field"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid sort parameter 'invalid_field'. Supported values are: stars, forks, updated"));
    }

    @Test
    void getStoredRepositoryByIdShouldReturnRepoWhenFound() throws Exception {
        RepositoryResponse repo = RepositoryResponse.builder()
                .id(1001L)
                .name("spring-boot")
                .build();

        when(gitHubService.getStoredRepositoryById(1001L)).thenReturn(repo);

        mockMvc.perform(get("/api/github/repositories/1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1001))
                .andExpect(jsonPath("$.name").value("spring-boot"));
    }

    @Test
    void getStoredRepositoryByIdShouldReturn404WhenNotFound() throws Exception {
        when(gitHubService.getStoredRepositoryById(9999L))
                .thenThrow(new ResourceNotFoundException("Repository not found with ID: 9999"));

        mockMvc.perform(get("/api/github/repositories/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deleteStoredRepositoryShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/github/repositories/1001"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteStoredRepositoryShouldReturn404WhenMissing() throws Exception {
        doThrow(new ResourceNotFoundException("Repository not found with ID: 9999"))
                .when(gitHubService).deleteStoredRepository(9999L);

        mockMvc.perform(delete("/api/github/repositories/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
