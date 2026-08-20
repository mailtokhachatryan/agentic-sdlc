package com.agenticdev.sdlc.llm.config;

import com.agenticdev.sdlc.llm.Provider;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.bedrock.BedrockChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class LlmAutoConfiguration {

    @Bean(name = "openAiChatModel")
    @ConditionalOnProperty(prefix = "app.llm.openai", name = "enabled", havingValue = "true")
    ChatModel openAiChatModel(LlmProperties props) {
        var p = props.openai();
        return OpenAiChatModel.builder()
                .apiKey(p.apiKey())
                .modelName(p.defaultModel())
                .baseUrl(p.baseUrl())
                .timeout(props.requestTimeout())
                .build();
    }

    @Bean(name = "anthropicChatModel")
    @ConditionalOnProperty(prefix = "app.llm.anthropic", name = "enabled", havingValue = "true")
    ChatModel anthropicChatModel(LlmProperties props) {
        var p = props.anthropic();
        return AnthropicChatModel.builder()
                .apiKey(p.apiKey())
                .modelName(p.defaultModel())
                .timeout(props.requestTimeout())
                .build();
    }

    @Bean(name = "bedrockChatModel")
    @ConditionalOnProperty(prefix = "app.llm.bedrock", name = "enabled", havingValue = "true")
    ChatModel bedrockChatModel(LlmProperties props) {
        var p = props.bedrock();
        return BedrockChatModel.builder()
                .modelId(p.defaultModel())
                .region(software.amazon.awssdk.regions.Region.of(p.region()))
                .build();
    }

    @Bean(name = "ollamaChatModel")
    @ConditionalOnProperty(prefix = "app.llm.ollama", name = "enabled", havingValue = "true")
    ChatModel ollamaChatModel(LlmProperties props) {
        var p = props.ollama();
        return OllamaChatModel.builder()
                .baseUrl(p.baseUrl())
                .modelName(p.defaultModel())
                .timeout(props.requestTimeout())
                .build();
    }

    @Bean(name = "lmStudioChatModel")
    @ConditionalOnProperty(prefix = "app.llm.lmstudio", name = "enabled", havingValue = "true")
    ChatModel lmStudioChatModel(LlmProperties props) {
        var p = props.lmstudio();
        String apiKey = (p.apiKey() == null || p.apiKey().isBlank()) ? "lm-studio" : p.apiKey();
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(p.baseUrl())
                .modelName(p.defaultModel())
                .timeout(props.requestTimeout())
                .maxTokens(4096)
                .maxRetries(1)
                .build();
    }

    @Bean
    Map<Provider, ChatModel> chatModelsByProvider(
            @Autowired(required = false)
            @Qualifier("openAiChatModel") ChatModel openAi,
            @Autowired(required = false)
            @Qualifier("anthropicChatModel") ChatModel anthropic,
            @Autowired(required = false)
            @Qualifier("bedrockChatModel") ChatModel bedrock,
            @Autowired(required = false)
            @Qualifier("ollamaChatModel") ChatModel ollama,
            @Autowired(required = false)
            @Qualifier("lmStudioChatModel") ChatModel lmStudio) {
        Map<Provider, ChatModel> m = new HashMap<>();
        if (openAi != null) m.put(Provider.OPENAI, openAi);
        if (anthropic != null) m.put(Provider.ANTHROPIC, anthropic);
        if (bedrock != null) m.put(Provider.BEDROCK, bedrock);
        if (ollama != null) m.put(Provider.OLLAMA, ollama);
        if (lmStudio != null) m.put(Provider.LMSTUDIO, lmStudio);
        return Map.copyOf(m);
    }
}
