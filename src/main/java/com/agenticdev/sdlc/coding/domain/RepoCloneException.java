package com.agenticdev.sdlc.coding.domain;

public class RepoCloneException extends CodingRunException {

    public RepoCloneException(String repoUrl, Throwable cause) {
        super("repo_clone_failed", "Failed to clone repository: " + repoUrl, cause);
    }

    public RepoCloneException(String repoUrl, String message) {
        super("repo_clone_failed", "Failed to clone repository " + repoUrl + ": " + message);
    }
}
