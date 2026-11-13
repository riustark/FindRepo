package com.example.githubsearcher.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Configuration for external REST clients, configured with timeouts,
 * the GitHub API base URL, and optional Bearer token authentication.
 */
@Configuration
public class RestClientConfig {

    private final String baseUrl;
    private final String token;

    public RestClientConfig(
            @Value("${github.api.base-url:https://api.github.com}") String baseUrl,
            @Value("${github.api.token:}") String token
    ) {
        this.baseUrl = baseUrl;
        this.token = token;
    }

    @Bean
    public RestClient gitHubRestClient(RestClient.Builder builder) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        RestClient.Builder clientBuilder = builder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28");

        if (token != null && !token.isBlank()) {
            clientBuilder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }

        return clientBuilder.build();
    }
}
