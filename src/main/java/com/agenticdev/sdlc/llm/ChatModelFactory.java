package com.agenticdev.sdlc.llm;

import dev.langchain4j.model.chat.ChatModel;

public interface ChatModelFactory {
    ChatModel resolve(Provider provider, String modelOverride);
}
