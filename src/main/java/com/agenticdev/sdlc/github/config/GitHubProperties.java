package com.agenticdev.sdlc.github.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties("app.github")
public record GitHubProperties(
        boolean enabled,
        AuthMode authMode,
        Pat pat,
        App app,
        String branchPrefix,
        List<String> defaultLabels,
        Commit commit,
        Retry retry,
        String prBodyTemplate
) {
    public enum AuthMode { PAT, APP }

    public record Pat(String token) {}

    public record App(
            String appId,
            String installationId,
            String privateKeyPem
    ) {}

    public record Commit(
            String authorName,
            String authorEmail,
            String messageTemplate
    ) {}

    public record Retry(
            int maxAttempts,
            Duration initialBackoff,
            Duration maxBackoff,
            double multiplier
    ) {}
}
