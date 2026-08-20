package com.agenticdev.sdlc.github.domain;

public class PushFailedException extends PullRequestException {
    public PushFailedException(String message) {
        super("push_failed", message);
    }
    public PushFailedException(String message, Throwable cause) {
        super("push_failed", message, cause);
    }
}
