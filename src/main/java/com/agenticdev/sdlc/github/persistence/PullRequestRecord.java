package com.agenticdev.sdlc.github.persistence;

import com.agenticdev.sdlc.github.domain.MergeStrategy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pull_requests")
public class PullRequestRecord {

    @Id
    private UUID id;

    @Column(name = "coding_run_id", nullable = false)
    private UUID codingRunId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PullRequestStatus status;

    @Column(name = "repo_url", nullable = false, length = 512)
    private String repoUrl;

    @Column(name = "base_ref", nullable = false, length = 256)
    private String baseRef;

    @Column(name = "head_branch", length = 256)
    private String headBranch;

    @Column(name = "head_sha", length = 64)
    private String headSha;

    @Column(name = "pr_number")
    private Integer prNumber;

    @Column(name = "pr_url", length = 1024)
    private String prUrl;

    @Column(length = 512)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(nullable = false)
    private boolean draft;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> labels;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> reviewers;

    @Enumerated(EnumType.STRING)
    @Column(name = "merge_strategy", length = 16)
    private MergeStrategy mergeStrategy;

    @Column(name = "merged_sha", length = 64)
    private String mergedSha;

    @Column(name = "error_code", length = 64)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "webhook_url", length = 1024)
    private String webhookUrl;

    @Column(name = "webhook_sent", nullable = false)
    private boolean webhookSent;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "opened_at")
    private Instant openedAt;

    @Column(name = "merged_at")
    private Instant mergedAt;

    protected PullRequestRecord() { /* JPA */ }

    private PullRequestRecord(UUID codingRunId, String repoUrl, String baseRef,
                              String title, String body, boolean draft,
                              List<String> labels, String webhookUrl) {
        this.id = UUID.randomUUID();
        this.codingRunId = codingRunId;
        this.status = PullRequestStatus.PENDING;
        this.repoUrl = repoUrl;
        this.baseRef = baseRef;
        this.title = title;
        this.body = body;
        this.draft = draft;
        this.labels = labels;
        this.webhookUrl = webhookUrl;
        this.createdAt = Instant.now();
    }

    public static PullRequestRecord pending(UUID codingRunId, String repoUrl, String baseRef,
                                            String title, String body, boolean draft,
                                            List<String> labels, String webhookUrl) {
        return new PullRequestRecord(codingRunId, repoUrl, baseRef, title, body, draft, labels, webhookUrl);
    }

    public void markPushed(String headBranch, String headSha) {
        this.status = PullRequestStatus.PUSHED;
        this.headBranch = headBranch;
        this.headSha = headSha;
    }

    public void markOpen(int prNumber, String prUrl, List<String> reviewers) {
        this.status = draft ? PullRequestStatus.DRAFT : PullRequestStatus.OPEN;
        this.prNumber = prNumber;
        this.prUrl = prUrl;
        this.reviewers = reviewers;
        this.openedAt = Instant.now();
    }

    public void markReady() {
        this.draft = false;
        this.status = PullRequestStatus.OPEN;
    }

    public void markMerged(String mergedSha, MergeStrategy strategy) {
        this.status = PullRequestStatus.MERGED;
        this.mergedSha = mergedSha;
        this.mergeStrategy = strategy;
        this.mergedAt = Instant.now();
    }

    public void markClosed() {
        this.status = PullRequestStatus.CLOSED;
    }

    public void markFailed(String errorCode, String errorMessage, long durationMs) {
        this.status = PullRequestStatus.FAILED;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.durationMs = durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public void markWebhookSent() { this.webhookSent = true; }

    public UUID getId() { return id; }
    public UUID getCodingRunId() { return codingRunId; }
    public PullRequestStatus getStatus() { return status; }
    public String getRepoUrl() { return repoUrl; }
    public String getBaseRef() { return baseRef; }
    public String getHeadBranch() { return headBranch; }
    public String getHeadSha() { return headSha; }
    public Integer getPrNumber() { return prNumber; }
    public String getPrUrl() { return prUrl; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public boolean isDraft() { return draft; }
    public List<String> getLabels() { return labels; }
    public List<String> getReviewers() { return reviewers; }
    public MergeStrategy getMergeStrategy() { return mergeStrategy; }
    public String getMergedSha() { return mergedSha; }
    public String getErrorCode() { return errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public Long getDurationMs() { return durationMs; }
    public String getWebhookUrl() { return webhookUrl; }
    public boolean isWebhookSent() { return webhookSent; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getOpenedAt() { return openedAt; }
    public Instant getMergedAt() { return mergedAt; }
}
