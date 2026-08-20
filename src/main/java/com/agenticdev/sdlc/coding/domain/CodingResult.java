package com.agenticdev.sdlc.coding.domain;

import java.util.List;

public record CodingResult(
        String diff,
        int filesChanged,
        boolean testsPassed,
        int iterationsUsed,
        long tokensUsed,
        List<String> filesModified,
        CodingBudget.ExhaustReason exhaustReason
) {
    public boolean budgetExhausted() {
        return exhaustReason != null;
    }
}
