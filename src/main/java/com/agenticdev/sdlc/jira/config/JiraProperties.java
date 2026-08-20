package com.agenticdev.sdlc.jira.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("app.jira")
public record JiraProperties(
        boolean enabled,
        String baseUrl,
        String email,
        String apiToken,
        Duration connectTimeout,
        Duration readTimeout
) {}
