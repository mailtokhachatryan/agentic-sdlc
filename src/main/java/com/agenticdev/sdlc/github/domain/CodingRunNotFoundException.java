package com.agenticdev.sdlc.github.domain;

import java.util.UUID;

public class CodingRunNotFoundException extends PullRequestException {
    public CodingRunNotFoundException(UUID id) {
        super("coding_run_not_found", "No coding run with id " + id);
    }
}
