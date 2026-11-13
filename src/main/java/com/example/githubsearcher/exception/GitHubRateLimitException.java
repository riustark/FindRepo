package com.example.githubsearcher.exception;

/**
 * Thrown when the GitHub API responds with HTTP 403 or 429 indicating rate limit exhaustion.
 */
public class GitHubRateLimitException extends RuntimeException {

    private final String retryAfter;

    public GitHubRateLimitException(String message, String retryAfter) {
        super(message);
        this.retryAfter = retryAfter;
    }

    public String getRetryAfter() {
        return retryAfter;
    }
}

