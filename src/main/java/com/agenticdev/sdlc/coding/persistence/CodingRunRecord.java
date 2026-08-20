package com.agenticdev.sdlc.coding.persistence;

import com.agenticdev.sdlc.coding.domain.CodingBudget;
import com.agenticdev.sdlc.coding.domain.CodingResult;
import com.agenticdev.sdlc.llm.Provider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "coding_runs")
public class CodingRunRecord {

    @Id
    private UUID id;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CodingRunStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider;

    @Column(nullable = false, length = 128)
    private String model;

    @Column(name = "repo_url", length = 512)
    private String repoUrl;

    @Column(name = "base_ref", length = 256)
    private String baseRef;

    @Column(columnDefinition = "TEXT")
    private String diff;

    @Column(name = "files_changed")
    private Integer filesChanged;

    @Column(name = "iterations_used")
    private Integer iterationsUsed;

    @Column(name = "tokens_used")
    private Long tokensUsed;

    @Column(name = "tests_passed")
    private Boolean testsPassed;

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

    @Column(name = "timeout_reason", length = 32)
    private String timeoutReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "auto_open_pr", nullable = false)
    private boolean autoOpenPr;

    @Column(name = "pr_title", columnDefinition = "TEXT")
    private String prTitle;

    @Column(name = "pr_body", columnDefinition = "TEXT")
    private String prBody;

    protected CodingRunRecord() { /* JPA */ }

    private CodingRunRecord(UUID planId, Provider provider, String model,
                            String repoUrl, String baseRef, String webhookUrl,
                            boolean autoOpenPr, String prTitle, String prBody) {
        this.id = UUID.randomUUID();
        this.planId = planId;
        this.status = CodingRunStatus.PENDING;
        this.provider = provider;
        this.model = model;
        this.repoUrl = repoUrl;
        this.baseRef = baseRef;
        this.webhookUrl = webhookUrl;
        this.webhookSent = false;
        this.createdAt = Instant.now();
        this.autoOpenPr = autoOpenPr;
        this.prTitle = prTitle;
        this.prBody = prBody;
    }

    public static CodingRunRecord pending(UUID planId, Provider provider, String model,
                                          String repoUrl, String baseRef, String webhookUrl) {
        return new CodingRunRecord(planId, provider, model, repoUrl, baseRef, webhookUrl,
                false, null, null);
    }

    public static CodingRunRecord pending(UUID planId, Provider provider, String model,
                                          String repoUrl, String baseRef, String webhookUrl,
                                          boolean autoOpenPr, String prTitle, String prBody) {
        return new CodingRunRecord(planId, provider, model, repoUrl, baseRef, webhookUrl,
                autoOpenPr, prTitle, prBody);
    }

    public void markInProgress() {
        this.status = CodingRunStatus.IN_PROGRESS;
    }

    public void markCompleted(CodingResult result, long durationMs) {
        this.status = result.budgetExhausted() ? CodingRunStatus.TIMED_OUT : CodingRunStatus.COMPLETED;
        this.diff = result.diff();
        this.filesChanged = result.filesChanged();
        this.iterationsUsed = result.iterationsUsed();
        this.tokensUsed = result.tokensUsed();
        this.testsPassed = result.testsPassed();
        this.durationMs = durationMs;
        this.completedAt = Instant.now();
        if (result.exhaustReason() != null) {
            this.timeoutReason = result.exhaustReason().name();
        }
    }

    public void markFailed(String errorCode, String errorMessage, long durationMs) {
        this.status = CodingRunStatus.FAILED;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.durationMs = durationMs;
        this.completedAt = Instant.now();
    }

    public void markWebhookSent() {
        this.webhookSent = true;
    }

    public UUID getId() { return id; }
    public UUID getPlanId() { return planId; }
    public CodingRunStatus getStatus() { return status; }
    public Provider getProvider() { return provider; }
    public String getModel() { return model; }
    public String getRepoUrl() { return repoUrl; }
    public String getBaseRef() { return baseRef; }
    public String getDiff() { return diff; }
    public Integer getFilesChanged() { return filesChanged; }
    public Integer getIterationsUsed() { return iterationsUsed; }
    public Long getTokensUsed() { return tokensUsed; }
    public Boolean getTestsPassed() { return testsPassed; }
    public String getErrorCode() { return errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public Long getDurationMs() { return durationMs; }
    public String getWebhookUrl() { return webhookUrl; }
    public boolean isWebhookSent() { return webhookSent; }
    public String getTimeoutReason() { return timeoutReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }
    public boolean isAutoOpenPr() { return autoOpenPr; }
    public String getPrTitle() { return prTitle; }
    public String getPrBody() { return prBody; }
}
