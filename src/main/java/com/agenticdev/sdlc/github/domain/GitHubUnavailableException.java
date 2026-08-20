package com.agenticdev.sdlc.github.domain;

public class GitHubUnavailableException extends PullRequestException {
    public GitHubUnavailableException(String message, Throwable cause) {
        super("github_unavailable", message, cause);
    }
    public GitHubUnavailableException(String message) {
        super("github_unavailable", message);
    }
}
