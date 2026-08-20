package com.agenticdev.sdlc.planning.api.dto;

import com.agenticdev.sdlc.llm.Provider;
import com.agenticdev.sdlc.planning.domain.InputType;
import com.agenticdev.sdlc.planning.domain.PlanResult;
import com.agenticdev.sdlc.planning.persistence.PlanRecord;
import com.agenticdev.sdlc.planning.persistence.PlanStatus;

import java.time.Instant;
import java.util.UUID;

public record PlanResponse(
        UUID id,
        PlanStatus status,
        Provider provider,
        String model,
        InputType inputType,
        String prompt,
        String jiraKey,
        PlanResult plan,
        Instant createdAt,
        Long durationMs,
        ErrorDetail error
) {
    public record ErrorDetail(String code, String message) {}

    public static PlanResponse from(PlanRecord r) {
        ErrorDetail err = r.getErrorCode() == null ? null
                : new ErrorDetail(r.getErrorCode(), r.getErrorMessage());
        return new PlanResponse(
                r.getId(), r.getStatus(), r.getProvider(), r.getModel(),
                r.getInputType(), r.getPrompt(), r.getJiraKey(),
                r.getPlan(), r.getCreatedAt(), r.getDurationMs(), err
        );
    }
}
