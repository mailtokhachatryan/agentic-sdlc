package com.agenticdev.sdlc.planning.domain;

import com.agenticdev.sdlc.jira.JiraClient;
import com.agenticdev.sdlc.jira.JiraClientException;
import com.agenticdev.sdlc.jira.JiraTicket;
import com.agenticdev.sdlc.llm.ChatModelFactory;
import com.agenticdev.sdlc.llm.Provider;
import com.agenticdev.sdlc.llm.ProviderNotConfiguredException;
import com.agenticdev.sdlc.llm.config.LlmProperties;
import com.agenticdev.sdlc.observability.LlmCallMetrics;
import com.agenticdev.sdlc.planning.persistence.PlanRecord;
import com.agenticdev.sdlc.planning.persistence.PlanRepository;
import com.agenticdev.sdlc.planning.persistence.PlanStatus;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PlanningServiceTest {

    ChatModelFactory chatModelFactory = mock(ChatModelFactory.class);
    JiraClient jiraClient = mock(JiraClient.class);
    PlanningAgent agent = mock(PlanningAgent.class);
    PlanRepository repo = mock(PlanRepository.class);
    ChatModel chatModel = mock(ChatModel.class);
    LlmCallMetrics metrics = mock(LlmCallMetrics.class);

    LlmProperties props = new LlmProperties(
            Duration.ofSeconds(60),
            Provider.LMSTUDIO,
            new LlmProperties.ProviderConfig(true, "k", "gpt-4o", "https://x", null),
            new LlmProperties.ProviderConfig(true, "k", "claude-sonnet-4-6", null, null),
            new LlmProperties.ProviderConfig(false, null, "m", null, "eu-central-1"),
            new LlmProperties.ProviderConfig(false, null, "llama3.1", "http://o", null),
            new LlmProperties.ProviderConfig(true, "lm-studio", "openai/gpt-oss-20b", "http://localhost:1234/v1", null)
    );

    PlanningService service;

    @BeforeEach
    void setUp() {
        service = new PlanningService(chatModelFactory, jiraClient, agent, repo, props, metrics);
        when(chatModelFactory.resolve(Provider.ANTHROPIC, null)).thenReturn(chatModel);
        when(repo.save(any(PlanRecord.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private PlanResult samplePlan() {
        return new PlanResult("s", "a", List.of(), List.of(), List.of(), List.of(), "md");
    }

    @Test
    void promptMode_callsAgentAndPersistsCompleted() {
        when(agent.plan(chatModel, "do X")).thenReturn(samplePlan());

        PlanRecord result = service.createPlan(Provider.ANTHROPIC, null, InputType.PROMPT, "do X", null);

        ArgumentCaptor<PlanRecord> cap = ArgumentCaptor.forClass(PlanRecord.class);
        verify(repo, times(2)).save(cap.capture());
        assertThat(result.getStatus()).isEqualTo(PlanStatus.COMPLETED);
        assertThat(result.getPlan()).isNotNull();
        assertThat(result.getPrompt()).isEqualTo("do X");
    }

    @Test
    void jiraMode_fetchesTicketAndStoresSnapshot() {
        JiraTicket ticket = new JiraTicket("AS-1", "Sum", "Desc", "Open", "Story");
        when(jiraClient.fetch("AS-1")).thenReturn(ticket);
        when(agent.plan(eq(chatModel), contains("Sum"))).thenReturn(samplePlan());

        PlanRecord result = service.createPlan(Provider.ANTHROPIC, null, InputType.JIRA, null, "AS-1");

        assertThat(result.getStatus()).isEqualTo(PlanStatus.COMPLETED);
        assertThat(result.getJiraKey()).isEqualTo("AS-1");
        assertThat(result.getJiraSnapshot()).isEqualTo(ticket);
    }

    @Test
    void providerNotConfigured_propagates() {
        when(chatModelFactory.resolve(Provider.OPENAI, null))
                .thenThrow(new ProviderNotConfiguredException(Provider.OPENAI));

        assertThatThrownBy(() -> service.createPlan(Provider.OPENAI, null, InputType.PROMPT, "x", null))
                .isInstanceOf(ProviderNotConfiguredException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void jiraFailure_recordsFailedAndRethrows() {
        when(jiraClient.fetch("AS-1")).thenThrow(new JiraClientException("boom"));

        assertThatThrownBy(() -> service.createPlan(Provider.ANTHROPIC, null, InputType.JIRA, null, "AS-1"))
                .isInstanceOf(JiraClientException.class);

        ArgumentCaptor<PlanRecord> cap = ArgumentCaptor.forClass(PlanRecord.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getStatus()).isEqualTo(PlanStatus.FAILED);
        assertThat(cap.getValue().getErrorCode()).isEqualTo("jira_fetch_failed");
    }

    @Test
    void lmStudioDefaultModel_isAppliedWhenModelOverrideMissing() {
        ChatModel lmStudio = mock(ChatModel.class);
        when(chatModelFactory.resolve(Provider.LMSTUDIO, null)).thenReturn(lmStudio);
        when(agent.plan(eq(lmStudio), any())).thenReturn(samplePlan());

        PlanRecord result = service.createPlan(Provider.LMSTUDIO, null, InputType.PROMPT, "do X", null);

        assertThat(result.getProvider()).isEqualTo(Provider.LMSTUDIO);
        assertThat(result.getModel()).isEqualTo("openai/gpt-oss-20b");
    }

    @Test
    void llmFailure_marksRecordFailed() {
        when(agent.plan(any(), any())).thenThrow(new LlmCallException("bad", new RuntimeException()));

        assertThatThrownBy(() -> service.createPlan(Provider.ANTHROPIC, null, InputType.PROMPT, "x", null))
                .isInstanceOf(LlmCallException.class);

        ArgumentCaptor<PlanRecord> cap = ArgumentCaptor.forClass(PlanRecord.class);
        verify(repo, times(2)).save(cap.capture());
        PlanRecord failed = cap.getAllValues().get(1);
        assertThat(failed.getStatus()).isEqualTo(PlanStatus.FAILED);
        assertThat(failed.getErrorCode()).isEqualTo("llm_call_failed");
    }
}
