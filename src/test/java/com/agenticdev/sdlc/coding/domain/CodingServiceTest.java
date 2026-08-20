package com.agenticdev.sdlc.coding.domain;

import com.agenticdev.sdlc.coding.agent.CodingAgentProperties;
import com.agenticdev.sdlc.coding.persistence.CodingRunRecord;
import com.agenticdev.sdlc.coding.persistence.CodingRunRepository;
import com.agenticdev.sdlc.coding.persistence.CodingRunStatus;
import com.agenticdev.sdlc.coding.webhook.WebhookClient;
import com.agenticdev.sdlc.llm.ChatModelFactory;
import com.agenticdev.sdlc.llm.Provider;
import com.agenticdev.sdlc.llm.config.LlmProperties;
import com.agenticdev.sdlc.planning.domain.InputType;
import com.agenticdev.sdlc.planning.domain.PlanResult;
import com.agenticdev.sdlc.planning.persistence.PlanRecord;
import com.agenticdev.sdlc.planning.persistence.PlanRepository;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CodingServiceTest {

    PlanRepository planRepo = mock(PlanRepository.class);
    CodingRunRepository runRepo = mock(CodingRunRepository.class);
    ChatModelFactory chatModelFactory = mock(ChatModelFactory.class);
    CodeExecutor executor = mock(CodeExecutor.class);
    @SuppressWarnings("unchecked")
    ObjectProvider<CodeExecutor> executorProvider = mock(ObjectProvider.class);
    CodingAgent agent = mock(CodingAgent.class);
    WebhookClient webhookClient = mock(WebhookClient.class);
    ChatModel chatModel = mock(ChatModel.class);

    LlmProperties llmProps = new LlmProperties(
            Duration.ofSeconds(60),
            Provider.LMSTUDIO,
            new LlmProperties.ProviderConfig(true, "k", "gpt-4o", "https://x", null),
            new LlmProperties.ProviderConfig(true, "k", "claude-sonnet-4-6", null, null),
            new LlmProperties.ProviderConfig(false, null, "m", null, "eu-central-1"),
            new LlmProperties.ProviderConfig(false, null, "llama3.1", "http://o", null),
            new LlmProperties.ProviderConfig(true, "lm-studio", "openai/gpt-oss-20b", "http://localhost:1234/v1", null)
    );

    CodingAgentProperties codingProps = new CodingAgentProperties(
            new CodingAgentProperties.Budget(100_000, 10, Duration.ofMinutes(5)),
            new CodingAgentProperties.Container("eclipse-temurin:21-jdk", "1g", 1.0, true),
            "https://github.com/default/repo.git",
            "main",
            new CodingAgentProperties.Agent("SYS", "./mvnw test"),
            new CodingAgentProperties.Webhook(Duration.ofSeconds(2), Duration.ofSeconds(5))
    );

    CodingService service;

    @SuppressWarnings("unchecked")
    org.springframework.beans.factory.ObjectProvider<com.agenticdev.sdlc.coding.domain.PullRequestAutoOpener>
            autoOpenerProvider = mock(org.springframework.beans.factory.ObjectProvider.class);

    @BeforeEach
    void setUp() {
        service = new CodingService(planRepo, runRepo, chatModelFactory,
                executorProvider, agent, webhookClient, llmProps, codingProps, autoOpenerProvider);
        service.validateConfig();
        when(executorProvider.getObject()).thenReturn(executor);
        when(autoOpenerProvider.getIfAvailable()).thenReturn(null);
        when(runRepo.save(any(CodingRunRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        when(chatModelFactory.resolve(any(), any())).thenReturn(chatModel);
    }

    private PlanRecord completedPlan(UUID id) {
        PlanRecord p = PlanRecord.promptPending(Provider.LMSTUDIO, "openai/gpt-oss-20b", "do X");
        p.markCompleted(samplePlan(), 100L);
        return p;
    }

    private PlanResult samplePlan() {
        return new PlanResult("s", "a", List.of(), List.of(), List.of(), List.of(), "md");
    }

    // --- createRun ---

    @Test
    void createRun_planMissing_throws() {
        UUID planId = UUID.randomUUID();
        when(planRepo.findById(planId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createRun(planId, null, null, null, null, null))
                .isInstanceOf(PlanNotFoundException.class);
        verify(runRepo, never()).save(any());
    }

    @Test
    void createRun_planPending_throws() {
        UUID planId = UUID.randomUUID();
        PlanRecord plan = PlanRecord.promptPending(Provider.LMSTUDIO, "m", "do X");
        when(planRepo.findById(planId)).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> service.createRun(planId, null, null, null, null, null))
                .isInstanceOf(PlanNotCompletedException.class);
        verify(runRepo, never()).save(any());
    }

    @Test
    void createRun_savesPendingWithDefaults() {
        UUID planId = UUID.randomUUID();
        when(planRepo.findById(planId)).thenReturn(Optional.of(completedPlan(planId)));

        CodingRunRecord result = service.createRun(planId, null, null, null, null, null);

        assertThat(result.getStatus()).isEqualTo(CodingRunStatus.PENDING);
        assertThat(result.getProvider()).isEqualTo(Provider.LMSTUDIO);
        assertThat(result.getRepoUrl()).isEqualTo("https://github.com/default/repo.git");
        assertThat(result.getBaseRef()).isEqualTo("main");
    }

    @Test
    void createRun_respectsRequestOverrides() {
        UUID planId = UUID.randomUUID();
        when(planRepo.findById(planId)).thenReturn(Optional.of(completedPlan(planId)));

        CodingRunRecord result = service.createRun(planId,
                "https://github.com/other/repo.git", "develop",
                Provider.ANTHROPIC, "claude-opus-4-6", "https://hooks/cb");

        assertThat(result.getProvider()).isEqualTo(Provider.ANTHROPIC);
        assertThat(result.getModel()).isEqualTo("claude-opus-4-6");
        assertThat(result.getRepoUrl()).isEqualTo("https://github.com/other/repo.git");
        assertThat(result.getBaseRef()).isEqualTo("develop");
        assertThat(result.getWebhookUrl()).isEqualTo("https://hooks/cb");
    }

    // --- executeCodingRun ---

    @Test
    void executeCodingRun_happyPath_completes() {
        UUID runId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        CodingRunRecord run = CodingRunRecord.pending(planId, Provider.LMSTUDIO, "m",
                "https://repo", "main", "https://hooks/cb");
        when(runRepo.findById(runId)).thenReturn(Optional.of(run));
        when(planRepo.findById(planId)).thenReturn(Optional.of(completedPlan(planId)));
        CodingResult result = new CodingResult("diff...", 2, true, 5, 1234, List.of("a.txt"), null);
        when(agent.execute(any(), any(), any(), any())).thenReturn(result);
        when(webhookClient.notify(any(), any())).thenReturn(true);

        service.executeCodingRun(runId);

        assertThat(run.getStatus()).isEqualTo(CodingRunStatus.COMPLETED);
        assertThat(run.getDiff()).isEqualTo("diff...");
        assertThat(run.isWebhookSent()).isTrue();
        verify(executor).destroy();
    }

    @Test
    void executeCodingRun_budgetExhausted_marksTimedOut() {
        UUID runId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        CodingRunRecord run = CodingRunRecord.pending(planId, Provider.LMSTUDIO, "m",
                "https://repo", "main", null);
        when(runRepo.findById(runId)).thenReturn(Optional.of(run));
        when(planRepo.findById(planId)).thenReturn(Optional.of(completedPlan(planId)));
        CodingResult result = new CodingResult("", 0, false, 10, 100_000,
                List.of(), CodingBudget.ExhaustReason.ITERATION_LIMIT);
        when(agent.execute(any(), any(), any(), any())).thenReturn(result);

        service.executeCodingRun(runId);

        assertThat(run.getStatus()).isEqualTo(CodingRunStatus.TIMED_OUT);
        assertThat(run.getTimeoutReason()).isEqualTo("ITERATION_LIMIT");
        verify(executor).destroy();
    }

    @Test
    void executeCodingRun_cloneFailure_marksFailed() {
        UUID runId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        CodingRunRecord run = CodingRunRecord.pending(planId, Provider.LMSTUDIO, "m",
                "https://bad-repo", "main", null);
        when(runRepo.findById(runId)).thenReturn(Optional.of(run));
        when(planRepo.findById(planId)).thenReturn(Optional.of(completedPlan(planId)));
        org.mockito.Mockito.doThrow(new RepoCloneException("https://bad-repo", "not found"))
                .when(executor).provision(any(), any(), any());

        service.executeCodingRun(runId);

        assertThat(run.getStatus()).isEqualTo(CodingRunStatus.FAILED);
        assertThat(run.getErrorCode()).isEqualTo("repo_clone_failed");
        verify(executor).destroy();
    }

    @Test
    void executeCodingRun_containerCleanupAlwaysRuns() {
        UUID runId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        CodingRunRecord run = CodingRunRecord.pending(planId, Provider.LMSTUDIO, "m",
                "https://repo", "main", null);
        when(runRepo.findById(runId)).thenReturn(Optional.of(run));
        when(planRepo.findById(planId)).thenReturn(Optional.of(completedPlan(planId)));
        org.mockito.Mockito.doThrow(new RuntimeException("boom"))
                .when(agent).execute(any(), any(), any(), any());

        service.executeCodingRun(runId);

        assertThat(run.getStatus()).isEqualTo(CodingRunStatus.FAILED);
        assertThat(run.getErrorCode()).isEqualTo("internal_error");
        verify(executor).destroy();
    }

    @Test
    void executeCodingRun_webhookFailureDoesNotFailRun() {
        UUID runId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        CodingRunRecord run = CodingRunRecord.pending(planId, Provider.LMSTUDIO, "m",
                "https://repo", "main", "https://hooks/cb");
        when(runRepo.findById(runId)).thenReturn(Optional.of(run));
        when(planRepo.findById(planId)).thenReturn(Optional.of(completedPlan(planId)));
        when(agent.execute(any(), any(), any(), any()))
                .thenReturn(new CodingResult("d", 1, true, 3, 100, List.of("x"), null));
        when(webhookClient.notify(any(), any())).thenReturn(false);

        service.executeCodingRun(runId);

        assertThat(run.getStatus()).isEqualTo(CodingRunStatus.COMPLETED);
        assertThat(run.isWebhookSent()).isFalse();
    }

    @Test
    void executeCodingRun_missingRun_silentlyAborts() {
        UUID runId = UUID.randomUUID();
        when(runRepo.findById(runId)).thenReturn(Optional.empty());

        service.executeCodingRun(runId);

        verify(executorProvider, never()).getObject();
        verify(agent, never()).execute(any(), any(), any(), any());
    }
}
