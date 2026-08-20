package com.agenticdev.sdlc.coding.domain;

import java.time.Duration;

public record CodingBudget(
        long maxTokens,
        int maxIterations,
        Duration maxDuration,
        String containerMemory,
        double containerCpu
) {
    public enum ExhaustReason {
        TOKEN_BUDGET,
        ITERATION_LIMIT,
        TIME_LIMIT
    }
}
