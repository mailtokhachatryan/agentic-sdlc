package com.agenticdev.sdlc.coding.api.dto;

import com.agenticdev.sdlc.coding.persistence.CodingRunRecord;
import com.agenticdev.sdlc.coding.persistence.CodingRunStatus;
import com.agenticdev.sdlc.llm.Provider;

import java.time.Instant;
import java.util.UUID;

public record CodingRunResponse(
        UUID id,
        CodingRunStatus status,
        UUID planId,
        Provider provider,
        String model,
        String repoUrl,
        String baseRef,
        String diff,
        Integer filesChanged,
        Integer iterationsUsed,
        Long tokensUsed,
        Boolean testsPassed,
        String errorCode,
        String errorMessage,
        Long durationMs,
        String webhookUrl,
        Boolean webhookSent,
        String timeoutReason,
        Boolean autoOpenPr,
        String prTitle,
        String prBody,
        Instant createdAt,
        Instant completedAt
) {
    public static CodingRunResponse from(CodingRunRecord r) {
        return new CodingRunResponse(
                r.getId(), r.getStatus(), r.getPlanId(), r.getProvider(), r.getModel(),
                r.getRepoUrl(), r.getBaseRef(), r.getDiff(),
                r.getFilesChanged(), r.getIterationsUsed(), r.getTokensUsed(), r.getTestsPassed(),
                r.getErrorCode(), r.getErrorMessage(), r.getDurationMs(),
                r.getWebhookUrl(), r.isWebhookSent(), r.getTimeoutReason(),
                r.isAutoOpenPr(), r.getPrTitle(), r.getPrBody(),
                r.getCreatedAt(), r.getCompletedAt()
        );
    }
}
