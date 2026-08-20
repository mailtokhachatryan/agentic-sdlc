package com.agenticdev.sdlc.coding.agent;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("app.coding")
public record CodingAgentProperties(
        Budget budget,
        Container container,
        String defaultRepoUrl,
        String defaultBaseRef,
        Agent agent,
        Webhook webhook
) {
    public record Budget(
            long maxTokens,
            int maxIterations,
            Duration maxDuration
    ) {}

    public record Container(
            String image,
            String memory,
            double cpu,
            boolean networkDisabled
    ) {}

    public record Agent(
            String systemPrompt,
            String testCommand
    ) {}

    public record Webhook(
            Duration connectTimeout,
            Duration readTimeout
    ) {}
}
