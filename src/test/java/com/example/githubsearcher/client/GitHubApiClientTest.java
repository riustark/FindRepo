package com.example.githubsearcher.client;

import com.example.githubsearcher.exception.GitHubApiException;
import com.example.githubsearcher.exception.GitHubRateLimitException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GitHubApiClientTest {

    private MockRestServiceServer mockServer;
    private GitHubApiClient gitHubApiClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.github.com");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        gitHubApiClient = new GitHubApiClient(builder.build());
    }

    @Test
    void searchRepositoriesShouldReturnParsedResponse() {
        String json = """
                {
                    "total_count": 1,
                    "items": [
                        {
                            "id": 12345,
                            "name": "sample-repo",
                            "description": "A sample repository",
                            "owner": { "login": "testuser" },
                            "language": "Java",
                            "stargazers_count": 250,
                            "forks_count": 40,
                            "updated_at": "2024-01-01T12:00:00Z",
                            "html_url": "https://github.com/testuser/sample-repo"
                        }
                    ]
                }
                """;

        mockServer.expect(requestTo("https://api.github.com/search/repositories?q=sample&sort=stars&order=desc&page=1&per_page=30"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        GitHubApiClient.GitHubSearchApiResponse response = gitHubApiClient.searchRepositories("sample", "stars", "desc", 1, 30);

        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).id()).isEqualTo(12345L);
        assertThat(response.items().get(0).name()).isEqualTo("sample-repo");
        assertThat(response.items().get(0).owner().login()).isEqualTo("testuser");
    }

    @Test
    void searchRepositoriesShouldHandleEmptyItems() {
        String json = """
                {
                    "total_count": 0,
                    "items": []
                }
                """;

        mockServer.expect(requestTo("https://api.github.com/search/repositories?q=empty&sort=stars&order=desc&page=1&per_page=30"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        GitHubApiClient.GitHubSearchApiResponse response = gitHubApiClient.searchRepositories("empty", "stars", "desc", 1, 30);

        assertThat(response.totalCount()).isEqualTo(0);
        assertThat(response.items()).isEmpty();
    }

    @Test
    void searchRepositoriesShouldThrowRateLimitExceptionOn403Or429() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Retry-After", "60");

        mockServer.expect(requestTo("https://api.github.com/search/repositories?q=ratelimit&sort=stars&order=desc&page=1&per_page=30"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).headers(headers));

        assertThatThrownBy(() -> gitHubApiClient.searchRepositories("ratelimit", "stars", "desc", 1, 30))
                .isInstanceOf(GitHubRateLimitException.class)
                .hasMessageContaining("rate limit exceeded");
    }

    @Test
    void searchRepositoriesShouldThrowAuthenticationExceptionOn401() {
        mockServer.expect(requestTo("https://api.github.com/search/repositories?q=auth&sort=stars&order=desc&page=1&per_page=30"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> gitHubApiClient.searchRepositories("auth", "stars", "desc", 1, 30))
                .isInstanceOf(GitHubApiException.class)
                .hasMessageContaining("authentication failed");
    }

    @Test
    void searchRepositoriesShouldThrowServiceUnavailableOn500() {
        mockServer.expect(requestTo("https://api.github.com/search/repositories?q=servererror&sort=stars&order=desc&page=1&per_page=30"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> gitHubApiClient.searchRepositories("servererror", "stars", "desc", 1, 30))
                .isInstanceOf(GitHubApiException.class)
                .hasMessageContaining("unavailable");
    }
}

