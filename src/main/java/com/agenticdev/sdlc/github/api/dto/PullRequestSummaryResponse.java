package com.agenticdev.sdlc.github.api.dto;

import com.agenticdev.sdlc.github.persistence.PullRequestRecord;
import com.agenticdev.sdlc.github.persistence.PullRequestStatus;

import java.time.Instant;
import java.util.UUID;

public record PullRequestSummaryResponse(
        UUID id,
        PullRequestStatus status,
        UUID codingRunId,
        String repoUrl,
        Integer prNumber,
        String prUrl,
        String title,
        Instant createdAt
) {
    public static PullRequestSummaryResponse from(PullRequestRecord r) {
        return new PullRequestSummaryResponse(
                r.getId(), r.getStatus(), r.getCodingRunId(),
                r.getRepoUrl(), r.getPrNumber(), r.getPrUrl(), r.getTitle(), r.getCreatedAt()
        );
    }
}
