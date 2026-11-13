package com.example.githubsearcher.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when communication with the GitHub API fails due to authentication, upstream error, or network issues.
 */
public class GitHubApiException extends RuntimeException {

    private final HttpStatus status;

    public GitHubApiException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

