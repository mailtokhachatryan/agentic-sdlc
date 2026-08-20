package com.agenticdev.sdlc.github.domain;

public class BranchExistsException extends PullRequestException {
    public BranchExistsException(String branch) {
        super("branch_exists", "Branch already exists: " + branch);
    }
}
