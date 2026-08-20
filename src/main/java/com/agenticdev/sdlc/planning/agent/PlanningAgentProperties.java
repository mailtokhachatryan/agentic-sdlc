package com.agenticdev.sdlc.planning.agent;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.planning.agent")
public record PlanningAgentProperties(
        String systemPrompt,
        String userPromptTemplate
) {}
