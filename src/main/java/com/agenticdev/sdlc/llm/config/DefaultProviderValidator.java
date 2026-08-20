package com.agenticdev.sdlc.llm.config;

import com.agenticdev.sdlc.llm.Provider;
import dev.langchain4j.model.chat.ChatModel;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DefaultProviderValidator {

    private final LlmProperties props;
    private final Map<Provider, ChatModel> modelsByProvider;

    public DefaultProviderValidator(LlmProperties props, Map<Provider, ChatModel> modelsByProvider) {
        this.props = props;
        this.modelsByProvider = modelsByProvider;
    }

    @PostConstruct
    void verifyDefaultProviderEnabled() {
        Provider defaultProvider = props.defaultProvider();
        if (defaultProvider == null) {
            throw new IllegalStateException(
                    "app.llm.default-provider is not configured");
        }
        if (!modelsByProvider.containsKey(defaultProvider)) {
            throw new IllegalStateException(
                    "Default provider " + defaultProvider
                            + " is configured as app.llm.default-provider but is not enabled. "
                            + "Enable app.llm." + defaultProvider.name().toLowerCase() + ".enabled=true "
                            + "or change app.llm.default-provider to one of: " + modelsByProvider.keySet());
        }
    }
}
