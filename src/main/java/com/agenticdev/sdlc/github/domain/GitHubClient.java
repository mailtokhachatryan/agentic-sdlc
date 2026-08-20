package com.agenticdev.sdlc.github.domain;

/**
 * Port for GitHub REST operations used by the PR service.
 * Implementations handle auth (PAT or App installation token).
 */
public interface GitHubClient {

    /** Open a pull request. Returns the PR number + URL. */
    OpenedPr openPullRequest(PrContext ctx);

    void applyLabels(String repoOwner, String repoName, int prNumber, java.util.List<String> labels);

    void requestReviewers(String repoOwner, String repoName, int prNumber, java.util.List<String> reviewers);

    void markReady(String repoOwner, String repoName, int prNumber);

    /** Returns merge commit SHA. */
    String merge(String repoOwner, String repoName, int prNumber, MergeStrategy strategy);

    void postComment(String repoOwner, String repoName, int prNumber, String body);

    record OpenedPr(int number, String htmlUrl) {}
}
