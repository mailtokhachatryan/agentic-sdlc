package com.agenticdev.sdlc.github.api.dto;

import com.agenticdev.sdlc.github.domain.MergeStrategy;
import jakarta.validation.constraints.NotNull;

public record MergePullRequestRequest(
        @NotNull MergeStrategy strategy
) {}
