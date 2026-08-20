package com.agenticdev.sdlc.llm;

import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class DefaultChatModelFactoryTest {

    @Test
    void resolvesConfiguredProvider() {
        ChatModel anthropic = mock(ChatModel.class);
        DefaultChatModelFactory factory = new DefaultChatModelFactory(Map.of(Provider.ANTHROPIC, anthropic));

        assertThat(factory.resolve(Provider.ANTHROPIC, null)).isSameAs(anthropic);
    }

    @Test
    void resolvesLmStudioProvider() {
        ChatModel lmStudio = mock(ChatModel.class);
        DefaultChatModelFactory factory = new DefaultChatModelFactory(Map.of(Provider.LMSTUDIO, lmStudio));

        assertThat(factory.resolve(Provider.LMSTUDIO, null)).isSameAs(lmStudio);
    }

    @Test
    void throwsWhenProviderNotConfigured() {
        DefaultChatModelFactory factory = new DefaultChatModelFactory(Map.of());

        assertThatThrownBy(() -> factory.resolve(Provider.OPENAI, null))
                .isInstanceOf(ProviderNotConfiguredException.class)
                .hasMessageContaining("OPENAI");
    }
}
