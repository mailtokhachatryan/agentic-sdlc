package com.agenticdev.sdlc.llm.config;

import com.agenticdev.sdlc.llm.Provider;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("app.llm")
public record LlmProperties(
        Duration requestTimeout,
        Provider defaultProvider,
        ProviderConfig openai,
        ProviderConfig anthropic,
        ProviderConfig bedrock,
        ProviderConfig ollama,
        ProviderConfig lmstudio
) {
    public record ProviderConfig(
            boolean enabled,
            String apiKey,
            String defaultModel,
            String baseUrl,
            String region
    ) {}
}
