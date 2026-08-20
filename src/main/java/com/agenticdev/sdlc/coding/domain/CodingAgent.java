package com.agenticdev.sdlc.coding.domain;

import com.agenticdev.sdlc.planning.domain.PlanResult;
import dev.langchain4j.model.chat.ChatModel;

public interface CodingAgent {

    /**
     * Execute the plan using the provided chat model and executor.
     * The executor must already be provisioned. The agent MAY iterate multiple
     * times (read/write/run) subject to the supplied budget. Returns a
     * {@link CodingResult} describing what changed and whether tests passed.
     */
    CodingResult execute(ChatModel chatModel,
                         PlanResult plan,
                         CodeExecutor executor,
                         CodingBudget budget);
}
