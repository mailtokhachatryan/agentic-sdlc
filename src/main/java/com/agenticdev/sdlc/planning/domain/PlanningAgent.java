package com.agenticdev.sdlc.planning.domain;

import dev.langchain4j.model.chat.ChatModel;

public interface PlanningAgent {
    PlanResult plan(ChatModel chatModel, String taskText);
}
