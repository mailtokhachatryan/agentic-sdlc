package com.agenticdev.sdlc.coding.webhook;

import com.agenticdev.sdlc.coding.persistence.CodingRunRecord;
import com.agenticdev.sdlc.coding.persistence.CodingRunStatus;

import java.util.UUID;

public record WebhookPayload(
        UUID codingRunId,
        UUID planId,
        CodingRunStatus status,
        Boolean testsPassed,
        Integer filesChanged,
        String diffUrl,
        Long durationMs
) {
    public static WebhookPayload from(CodingRunRecord r) {
        return new WebhookPayload(
                r.getId(),
                r.getPlanId(),
                r.getStatus(),
                r.getTestsPassed(),
                r.getFilesChanged(),
                "/api/v1/coding-runs/" + r.getId() + "/diff",
                r.getDurationMs()
        );
    }
}
