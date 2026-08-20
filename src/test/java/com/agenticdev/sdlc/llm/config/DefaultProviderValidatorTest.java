package com.agenticdev.sdlc.llm.config;

import com.agenticdev.sdlc.llm.Provider;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class DefaultProviderValidatorTest {

    private LlmProperties props(Provider defaultProvider) {
        return new LlmProperties(
                Duration.ofSeconds(60),
                defaultProvider,
                new LlmProperties.ProviderConfig(false, null, "gpt-4o", null, null),
                new LlmProperties.ProviderConfig(false, null, "claude-sonnet-4-6", null, null),
                new LlmProperties.ProviderConfig(false, null, "m", null, "eu-central-1"),
                new LlmProperties.ProviderConfig(false, null, "llama3.1", null, null),
                new LlmProperties.ProviderConfig(true, "lm-studio", "openai/gpt-oss-20b", "http://localhost:1234/v1", null)
        );
    }

    @Test
    void passesWhenDefaultProviderIsEnabled() {
        DefaultProviderValidator v = new DefaultProviderValidator(
                props(Provider.LMSTUDIO),
                Map.of(Provider.LMSTUDIO, mock(ChatModel.class)));

        assertThatCode(v::verifyDefaultProviderEnabled).doesNotThrowAnyException();
    }

    @Test
    void failsWhenDefaultProviderIsNotEnabled() {
        DefaultProviderValidator v = new DefaultProviderValidator(
                props(Provider.ANTHROPIC),
                Map.of(Provider.LMSTUDIO, mock(ChatModel.class)));

        assertThatThrownBy(v::verifyDefaultProviderEnabled)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ANTHROPIC")
                .hasMessageContaining("not enabled");
    }

    @Test
    void failsWhenDefaultProviderIsNull() {
        DefaultProviderValidator v = new DefaultProviderValidator(
                props(null),
                Map.of(Provider.LMSTUDIO, mock(ChatModel.class)));

        assertThatThrownBy(v::verifyDefaultProviderEnabled)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("default-provider");
    }
}
