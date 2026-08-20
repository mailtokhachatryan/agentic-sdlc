package com.agenticdev.sdlc.jira;

public interface JiraClient {
    JiraTicket fetch(String jiraKey);
}
