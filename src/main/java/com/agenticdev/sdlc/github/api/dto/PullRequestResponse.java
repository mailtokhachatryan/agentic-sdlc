package com.agenticdev.sdlc.github.api.dto;

import com.agenticdev.sdlc.github.domain.MergeStrategy;
import com.agenticdev.sdlc.github.persistence.PullRequestRecord;
import com.agenticdev.sdlc.github.persistence.PullRequestStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PullRequestResponse(
        UUID id,
        PullRequestStatus status,
        UUID codingRunId,
        String repoUrl,
        String baseRef,
        String headBranch,
        String headSha,
        Integer prNumber,
        String prUrl,
        String title,
        String body,
        boolean draft,
        List<String> labels,
        List<String> reviewers,
        MergeStrategy mergeStrategy,
        String mergedSha,
        String errorCode,
        String errorMessage,
        Long durationMs,
        String webhookUrl,
        boolean webhookSent,
        Instant createdAt,
        Instant openedAt,
        Instant mergedAt
) {
    public static PullRequestResponse from(PullRequestRecord r) {
        return new PullRequestResponse(
                r.getId(), r.getStatus(), r.getCodingRunId(),
                r.getRepoUrl(), r.getBaseRef(), r.getHeadBranch(), r.getHeadSha(),
                r.getPrNumber(), r.getPrUrl(), r.getTitle(), r.getBody(), r.isDraft(),
                r.getLabels(), r.getReviewers(),
                r.getMergeStrategy(), r.getMergedSha(),
                r.getErrorCode(), r.getErrorMessage(), r.getDurationMs(),
                r.getWebhookUrl(), r.isWebhookSent(),
                r.getCreatedAt(), r.getOpenedAt(), r.getMergedAt()
        );
    }
}
