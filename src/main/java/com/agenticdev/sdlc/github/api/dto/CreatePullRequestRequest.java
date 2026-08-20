package com.agenticdev.sdlc.github.api.dto;

import com.agenticdev.sdlc.github.domain.MergeStrategy;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreatePullRequestRequest(
        @NotNull UUID codingRunId,
        @Size(max = 512) String title,
        @Size(max = 16384) String body,
        Boolean draft,
        List<@Size(max = 64) String> labels,
        MergeStrategy mergeStrategy,
        Boolean autoMerge,
        @Size(max = 1024) String webhookUrl
) {}
