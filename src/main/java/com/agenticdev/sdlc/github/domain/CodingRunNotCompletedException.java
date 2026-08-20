package com.agenticdev.sdlc.github.domain;

import java.util.UUID;

public class CodingRunNotCompletedException extends PullRequestException {
    public CodingRunNotCompletedException(UUID id, String status) {
        super("coding_run_not_completed",
                "Coding run " + id + " has status " + status + "; only COMPLETED runs can open a PR");
    }
}
