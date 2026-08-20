package com.agenticdev.sdlc.planning.agent;

import com.agenticdev.sdlc.planning.domain.LlmCallException;
import com.agenticdev.sdlc.planning.domain.PlanResult;
import com.agenticdev.sdlc.planning.domain.PlanningAgent;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.V;
import org.springframework.stereotype.Component;

@Component
public class LangChain4jPlanningAgent implements PlanningAgent {

    interface AgentProxy {
        PlanResult plan(@V("task") String taskText);
    }

    private final PlanningAgentProperties props;

    public LangChain4jPlanningAgent(PlanningAgentProperties props) {
        this.props = props;
    }

    @Override
    public PlanResult plan(ChatModel chatModel, String taskText) {
        try {
            AgentProxy proxy = AiServices.builder(AgentProxy.class)
                    .chatModel(chatModel)
                    .systemMessageProvider(memoryId -> props.systemPrompt())
                    .userMessageProvider(memoryId -> props.userPromptTemplate())
                    .build();
            return proxy.plan(taskText);
        } catch (RuntimeException e) {
            throw new LlmCallException("LLM planning call failed", e);
        }
    }
}
