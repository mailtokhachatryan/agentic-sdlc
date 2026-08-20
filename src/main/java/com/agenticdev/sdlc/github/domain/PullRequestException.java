package com.agenticdev.sdlc.github.domain;

public class PullRequestException extends RuntimeException {
    private final String code;

    public PullRequestException(String code, String message) {
        super(message);
        this.code = code;
    }

    public PullRequestException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String code() { return code; }
}
