package com.agenticdev.sdlc.planning.persistence;

import com.agenticdev.sdlc.jira.JiraTicket;
import com.agenticdev.sdlc.llm.Provider;
import com.agenticdev.sdlc.planning.domain.InputType;
import com.agenticdev.sdlc.planning.domain.PlanResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlanRecordTest {

    @Test
    void promptRecordStartsPending() {
        PlanRecord r = PlanRecord.promptPending(Provider.ANTHROPIC, "claude-sonnet-4-6", "do the thing");

        assertThat(r.getStatus()).isEqualTo(PlanStatus.PENDING);
        assertThat(r.getPrompt()).isEqualTo("do the thing");
        assertThat(r.getJiraKey()).isNull();
        assertThat(r.getInputType()).isEqualTo(InputType.PROMPT);
    }

    @Test
    void jiraRecordStoresSnapshot() {
        JiraTicket t = new JiraTicket("AS-1", "Summary", "Description", "Open", "Story");
        PlanRecord r = PlanRecord.jiraPending(Provider.OPENAI, "gpt-4o", t);

        assertThat(r.getJiraKey()).isEqualTo("AS-1");
        assertThat(r.getJiraSnapshot()).isSameAs(t);
        assertThat(r.getInputType()).isEqualTo(InputType.JIRA);
    }

    @Test
    void markCompletedStoresPlanAndStatus() {
        PlanRecord r = PlanRecord.promptPending(Provider.ANTHROPIC, "m", "x");
        PlanResult plan = new PlanResult("s", "a", List.of(), List.of(), List.of(), List.of(), "md");

        r.markCompleted(plan, 1234L);

        assertThat(r.getStatus()).isEqualTo(PlanStatus.COMPLETED);
        assertThat(r.getPlan()).isSameAs(plan);
        assertThat(r.getDurationMs()).isEqualTo(1234L);
        assertThat(r.getErrorCode()).isNull();
    }

    @Test
    void markFailedStoresErrorAndStatus() {
        PlanRecord r = PlanRecord.promptPending(Provider.ANTHROPIC, "m", "x");

        r.markFailed("llm_call_failed", "boom", 42L);

        assertThat(r.getStatus()).isEqualTo(PlanStatus.FAILED);
        assertThat(r.getErrorCode()).isEqualTo("llm_call_failed");
        assertThat(r.getErrorMessage()).isEqualTo("boom");
        assertThat(r.getDurationMs()).isEqualTo(42L);
    }
}
