package com.agenticdev.sdlc.planning.api.dto;

import com.agenticdev.sdlc.llm.Provider;
import com.agenticdev.sdlc.planning.domain.InputType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreatePlanRequest(
        @NotNull InputType inputType,
        @Size(min = 1, max = 8000) String prompt,
        @Pattern(regexp = "^[A-Z][A-Z0-9]+-\\d+$") String jiraKey,
        Provider provider,
        String model
) {
    public void validateConsistency() {
        if (inputType == InputType.PROMPT) {
            if (prompt == null || prompt.isBlank())
                throw new IllegalArgumentException("prompt is required when inputType=PROMPT");
            if (jiraKey != null)
                throw new IllegalArgumentException("jiraKey must be absent when inputType=PROMPT");
        } else {
            if (jiraKey == null || jiraKey.isBlank())
                throw new IllegalArgumentException("jiraKey is required when inputType=JIRA");
            if (prompt != null)
                throw new IllegalArgumentException("prompt must be absent when inputType=JIRA");
        }
    }
}
