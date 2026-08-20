package com.agenticdev.sdlc.github.domain;

import java.util.List;

public record PrContext(
        String repoOwner,
        String repoName,
        String baseRef,
        String headBranch,
        String headSha,
        String title,
        String body,
        boolean draft,
        List<String> labels,
        List<String> reviewers
) {}
