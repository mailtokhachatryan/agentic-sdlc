package com.agenticdev.sdlc.jira;

public record JiraTicket(
        String key,
        String summary,
        String description,
        String status,
        String issueType
) {}
