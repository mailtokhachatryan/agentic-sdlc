package com.agenticdev.sdlc.github.domain;

import java.util.UUID;

public class EmptyDiffException extends PullRequestException {
    public EmptyDiffException(UUID codingRunId) {
        super("empty_diff", "Coding run " + codingRunId + " has an empty diff");
    }
}
