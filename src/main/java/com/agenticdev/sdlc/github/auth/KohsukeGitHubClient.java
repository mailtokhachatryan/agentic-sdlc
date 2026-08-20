package com.agenticdev.sdlc.github.auth;

import com.agenticdev.sdlc.github.domain.GitHubClient;
import com.agenticdev.sdlc.github.domain.GitHubUnavailableException;
import com.agenticdev.sdlc.github.domain.MergeStrategy;
import com.agenticdev.sdlc.github.domain.PrContext;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestCommitDetail;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * GitHubClient backed by the kohsuke github-api library. Same adapter is used
 * for both PAT and App auth — only the underlying {@link GitHub} bean differs.
 */
@Component
@ConditionalOnBean(GitHub.class)
public class KohsukeGitHubClient implements GitHubClient {

    private static final Logger log = LoggerFactory.getLogger(KohsukeGitHubClient.class);

    private final GitHub github;

    public KohsukeGitHubClient(GitHub github) {
        this.github = github;
    }

    @Override
    @Retryable(retryFor = IOException.class,
            backoff = @Backoff(delayExpression = "#{${app.github.retry.initial-backoff.toMillis() ?: 500}}",
                    maxDelayExpression = "#{${app.github.retry.max-backoff.toMillis() ?: 5000}}",
                    multiplierExpression = "#{${app.github.retry.multiplier ?: 2.0}}"),
            maxAttemptsExpression = "#{${app.github.retry.max-attempts ?: 3}}")
    public OpenedPr openPullRequest(PrContext ctx) {
        try {
            GHRepository repo = github.getRepository(ctx.repoOwner() + "/" + ctx.repoName());
            GHPullRequest pr = repo.createPullRequest(
                    ctx.title(), ctx.headBranch(), ctx.baseRef(), ctx.body(), true, ctx.draft());
            log.info("Opened PR #{} at {}", pr.getNumber(), pr.getHtmlUrl());
            return new OpenedPr(pr.getNumber(), pr.getHtmlUrl().toString());
        } catch (IOException e) {
            throw new GitHubUnavailableException("openPullRequest failed", e);
        }
    }

    @Override
    @Retryable(retryFor = IOException.class,
            backoff = @Backoff(delay = 500, maxDelay = 5000, multiplier = 2.0),
            maxAttempts = 3)
    public void applyLabels(String repoOwner, String repoName, int prNumber, List<String> labels) {
        if (labels == null || labels.isEmpty()) return;
        try {
            GHRepository repo = github.getRepository(repoOwner + "/" + repoName);
            repo.getPullRequest(prNumber).addLabels(labels.toArray(new String[0]));
        } catch (IOException e) {
            throw new GitHubUnavailableException("applyLabels failed", e);
        }
    }

    @Override
    @Retryable(retryFor = IOException.class,
            backoff = @Backoff(delay = 500, maxDelay = 5000, multiplier = 2.0),
            maxAttempts = 3)
    public void requestReviewers(String repoOwner, String repoName, int prNumber, List<String> reviewers) {
        if (reviewers == null || reviewers.isEmpty()) return;
        try {
            GHRepository repo = github.getRepository(repoOwner + "/" + repoName);
            GHPullRequest pr = repo.getPullRequest(prNumber);
            List<org.kohsuke.github.GHUser> users = new java.util.ArrayList<>();
            for (String r : reviewers) {
                try { users.add(github.getUser(r)); } catch (IOException ignore) { /* teams or missing users */ }
            }
            if (!users.isEmpty()) pr.requestReviewers(users);
        } catch (IOException e) {
            throw new GitHubUnavailableException("requestReviewers failed", e);
        }
    }

    @Override
    @Retryable(retryFor = IOException.class,
            backoff = @Backoff(delay = 500, maxDelay = 5000, multiplier = 2.0),
            maxAttempts = 3)
    public void markReady(String repoOwner, String repoName, int prNumber) {
        try {
            GHRepository repo = github.getRepository(repoOwner + "/" + repoName);
            GHPullRequest pr = repo.getPullRequest(prNumber);
            // Mark-ready is a GraphQL-only operation in the REST API; post a comment
            // so the PR becomes "active" in review flow and log a guidance message.
            // For production-grade draft→ready, use the GraphQL markPullRequestReadyForReview mutation.
            log.warn("markReady is approximated — use GraphQL for full draft→ready semantics. PR #{}", prNumber);
            pr.comment("/ready (requested by Agentic SDLC)");
        } catch (IOException e) {
            throw new GitHubUnavailableException("markReady failed", e);
        }
    }

    @Override
    @Retryable(retryFor = IOException.class,
            backoff = @Backoff(delay = 500, maxDelay = 5000, multiplier = 2.0),
            maxAttempts = 3)
    public String merge(String repoOwner, String repoName, int prNumber, MergeStrategy strategy) {
        try {
            GHRepository repo = github.getRepository(repoOwner + "/" + repoName);
            GHPullRequest pr = repo.getPullRequest(prNumber);
            GHPullRequest.MergeMethod method = switch (strategy) {
                case SQUASH -> GHPullRequest.MergeMethod.SQUASH;
                case REBASE -> GHPullRequest.MergeMethod.REBASE;
                case MERGE -> GHPullRequest.MergeMethod.MERGE;
            };
            pr.merge("Merged by Agentic SDLC", pr.getHead().getSha(), method);
            for (GHPullRequestCommitDetail c : pr.listCommits()) {
                // touch iterable so auth kicks in (no-op); we just want merge_commit_sha next
            }
            return pr.getMergeCommitSha();
        } catch (IOException e) {
            throw new GitHubUnavailableException("merge failed", e);
        }
    }

    @Override
    @Retryable(retryFor = IOException.class,
            backoff = @Backoff(delay = 500, maxDelay = 5000, multiplier = 2.0),
            maxAttempts = 3)
    public void postComment(String repoOwner, String repoName, int prNumber, String body) {
        try {
            GHRepository repo = github.getRepository(repoOwner + "/" + repoName);
            repo.getPullRequest(prNumber).comment(body);
        } catch (IOException e) {
            throw new GitHubUnavailableException("postComment failed", e);
        }
    }
}
