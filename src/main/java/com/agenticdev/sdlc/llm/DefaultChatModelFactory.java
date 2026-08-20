package com.agenticdev.sdlc.llm;

import dev.langchain4j.model.chat.ChatModel;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DefaultChatModelFactory implements ChatModelFactory {

    private final Map<Provider, ChatModel> modelsByProvider;

    public DefaultChatModelFactory(Map<Provider, ChatModel> modelsByProvider) {
        this.modelsByProvider = modelsByProvider;
    }

    @Override
    public ChatModel resolve(Provider provider, String modelOverride) {
        ChatModel model = modelsByProvider.get(provider);
        if (model == null) {
            throw new ProviderNotConfiguredException(provider);
        }
        return model;
    }
}
