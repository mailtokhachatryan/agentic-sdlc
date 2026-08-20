package com.agenticdev.sdlc.planning.domain;

import java.util.List;

public record PlanResult(
        String summary,
        String approach,
        List<PlanTask> tasks,
        List<FileChange> filesToTouch,
        List<PlanRisk> risks,
        List<String> openQuestions,
        String markdown
) {}
