package com.agenticdev.sdlc.coding.api.dto;

import com.agenticdev.sdlc.coding.persistence.CodingRunRecord;
import com.agenticdev.sdlc.coding.persistence.CodingRunStatus;
import com.agenticdev.sdlc.llm.Provider;

import java.time.Instant;
import java.util.UUID;

public record CodingRunSummaryResponse(
        UUID id,
        CodingRunStatus status,
        UUID planId,
        Provider provider,
        String model,
        Integer filesChanged,
        Boolean testsPassed,
        Long durationMs,
        Instant createdAt
) {
    public static CodingRunSummaryResponse from(CodingRunRecord r) {
        return new CodingRunSummaryResponse(
                r.getId(), r.getStatus(), r.getPlanId(),
                r.getProvider(), r.getModel(),
                r.getFilesChanged(), r.getTestsPassed(), r.getDurationMs(),
                r.getCreatedAt()
        );
    }
}
