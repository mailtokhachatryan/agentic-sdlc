package com.agenticdev.sdlc.jira;

public class JiraClientException extends RuntimeException {
    public JiraClientException(String message, Throwable cause) { super(message, cause); }
    public JiraClientException(String message) { super(message); }
}
