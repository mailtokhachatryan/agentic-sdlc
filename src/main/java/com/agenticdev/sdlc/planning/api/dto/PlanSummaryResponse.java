package com.agenticdev.sdlc.planning.api.dto;

import com.agenticdev.sdlc.llm.Provider;
import com.agenticdev.sdlc.planning.domain.InputType;
import com.agenticdev.sdlc.planning.persistence.PlanRecord;
import com.agenticdev.sdlc.planning.persistence.PlanStatus;

import java.time.Instant;
import java.util.UUID;

public record PlanSummaryResponse(
        UUID id,
        PlanStatus status,
        Provider provider,
        String model,
        InputType inputType,
        String jiraKey,
        String promptPreview,
        Instant createdAt
) {
    public static PlanSummaryResponse from(PlanRecord r) {
        String preview = r.getPrompt() == null ? null
                : (r.getPrompt().length() > 120 ? r.getPrompt().substring(0, 117) + "..." : r.getPrompt());
        return new PlanSummaryResponse(
                r.getId(), r.getStatus(), r.getProvider(), r.getModel(),
                r.getInputType(), r.getJiraKey(), preview, r.getCreatedAt()
        );
    }
}
