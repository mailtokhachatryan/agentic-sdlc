package com.agenticdev.sdlc.planning.agent;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class PlanningAgentPropertiesTest {

    @Configuration
    @EnableConfigurationProperties(PlanningAgentProperties.class)
    static class Config {}

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                    PropertyPlaceholderAutoConfiguration.class))
            .withUserConfiguration(Config.class);

    @Test
    void bindsFromProperties() {
        runner
                .withPropertyValues(
                        "app.planning.agent.system-prompt=SYS",
                        "app.planning.agent.user-prompt-template=Task:\n{{task}}")
                .run(ctx -> {
                    PlanningAgentProperties p = ctx.getBean(PlanningAgentProperties.class);
                    assertThat(p.systemPrompt()).isEqualTo("SYS");
                    assertThat(p.userPromptTemplate()).contains("{{task}}");
                });
    }
}
