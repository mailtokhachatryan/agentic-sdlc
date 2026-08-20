package com.agenticdev.sdlc.coding.persistence;

import com.agenticdev.sdlc.coding.domain.CodingBudget;
import com.agenticdev.sdlc.coding.domain.CodingResult;
import com.agenticdev.sdlc.llm.Provider;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CodingRunRecordTest {

    @Test
    void pending_initializesCreatedAt() {
        CodingRunRecord r = CodingRunRecord.pending(UUID.randomUUID(),
                Provider.LMSTUDIO, "m", "https://repo", "main", null);
        assertThat(r.getStatus()).isEqualTo(CodingRunStatus.PENDING);
        assertThat(r.getCreatedAt()).isNotNull();
        assertThat(r.isWebhookSent()).isFalse();
    }

    @Test
    void markInProgress_changesStatus() {
        CodingRunRecord r = CodingRunRecord.pending(UUID.randomUUID(),
                Provider.LMSTUDIO, "m", "r", "main", null);
        r.markInProgress();
        assertThat(r.getStatus()).isEqualTo(CodingRunStatus.IN_PROGRESS);
    }

    @Test
    void markCompleted_setsCompletedFields() {
        CodingRunRecord r = CodingRunRecord.pending(UUID.randomUUID(),
                Provider.LMSTUDIO, "m", "r", "main", null);
        CodingResult result = new CodingResult("diff", 2, true, 5, 1234,
                List.of("a.txt"), null);
        r.markCompleted(result, 5000L);

        assertThat(r.getStatus()).isEqualTo(CodingRunStatus.COMPLETED);
        assertThat(r.getDiff()).isEqualTo("diff");
        assertThat(r.getFilesChanged()).isEqualTo(2);
        assertThat(r.getTestsPassed()).isTrue();
        assertThat(r.getIterationsUsed()).isEqualTo(5);
        assertThat(r.getTokensUsed()).isEqualTo(1234);
        assertThat(r.getDurationMs()).isEqualTo(5000L);
        assertThat(r.getCompletedAt()).isNotNull();
        assertThat(r.getTimeoutReason()).isNull();
    }

    @Test
    void markCompleted_withExhaustReason_marksTimedOut() {
        CodingRunRecord r = CodingRunRecord.pending(UUID.randomUUID(),
                Provider.LMSTUDIO, "m", "r", "main", null);
        CodingResult result = new CodingResult("", 0, false, 20, 50000,
                List.of(), CodingBudget.ExhaustReason.TIME_LIMIT);
        r.markCompleted(result, 900_000L);

        assertThat(r.getStatus()).isEqualTo(CodingRunStatus.TIMED_OUT);
        assertThat(r.getTimeoutReason()).isEqualTo("TIME_LIMIT");
    }

    @Test
    void markFailed_setsErrorFields() {
        CodingRunRecord r = CodingRunRecord.pending(UUID.randomUUID(),
                Provider.LMSTUDIO, "m", "r", "main", null);
        r.markFailed("repo_clone_failed", "not found", 100L);

        assertThat(r.getStatus()).isEqualTo(CodingRunStatus.FAILED);
        assertThat(r.getErrorCode()).isEqualTo("repo_clone_failed");
        assertThat(r.getErrorMessage()).isEqualTo("not found");
        assertThat(r.getDurationMs()).isEqualTo(100L);
    }

    @Test
    void markWebhookSent_flipsFlag() {
        CodingRunRecord r = CodingRunRecord.pending(UUID.randomUUID(),
                Provider.LMSTUDIO, "m", "r", "main", "https://cb");
        assertThat(r.isWebhookSent()).isFalse();
        r.markWebhookSent();
        assertThat(r.isWebhookSent()).isTrue();
    }
}
