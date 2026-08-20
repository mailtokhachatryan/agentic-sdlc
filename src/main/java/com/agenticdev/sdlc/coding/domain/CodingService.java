package com.agenticdev.sdlc.coding.domain;

import com.agenticdev.sdlc.coding.agent.CodingAgentProperties;
import com.agenticdev.sdlc.coding.persistence.CodingRunRecord;
import com.agenticdev.sdlc.coding.persistence.CodingRunRepository;
import com.agenticdev.sdlc.coding.webhook.WebhookClient;
import com.agenticdev.sdlc.llm.ChatModelFactory;
import com.agenticdev.sdlc.llm.Provider;
import com.agenticdev.sdlc.llm.config.LlmProperties;
import com.agenticdev.sdlc.planning.persistence.PlanRecord;
import com.agenticdev.sdlc.planning.persistence.PlanRepository;
import com.agenticdev.sdlc.planning.persistence.PlanStatus;
import dev.langchain4j.model.chat.ChatModel;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Orchestrates the coding-run lifecycle. The POST handler creates the PENDING
 * record synchronously (so the caller gets a 202 with an id) and then hands
 * off to {@link #executeCodingRun(UUID)}, which runs asynchronously on the
 * coding task executor.
 */
@Service
public class CodingService {

    private static final Logger log = LoggerFactory.getLogger(CodingService.class);

    private final PlanRepository planRepo;
    private final CodingRunRepository runRepo;
    private final ChatModelFactory chatModelFactory;
    private final ObjectProvider<CodeExecutor> executorProvider;
    private final CodingAgent agent;
    private final WebhookClient webhookClient;
    private final LlmProperties llmProperties;
    private final CodingAgentProperties codingProperties;
    private final ObjectProvider<PullRequestAutoOpener> autoOpenerProvider;

    public CodingService(PlanRepository planRepo,
                         CodingRunRepository runRepo,
                         ChatModelFactory chatModelFactory,
                         ObjectProvider<CodeExecutor> executorProvider,
                         CodingAgent agent,
                         WebhookClient webhookClient,
                         LlmProperties llmProperties,
                         CodingAgentProperties codingProperties,
                         ObjectProvider<PullRequestAutoOpener> autoOpenerProvider) {
        this.planRepo = planRepo;
        this.runRepo = runRepo;
        this.chatModelFactory = chatModelFactory;
        this.executorProvider = executorProvider;
        this.agent = agent;
        this.webhookClient = webhookClient;
        this.llmProperties = llmProperties;
        this.codingProperties = codingProperties;
        this.autoOpenerProvider = autoOpenerProvider;
    }

    @PostConstruct
    void validateConfig() {
        if (codingProperties.budget() == null
                || codingProperties.budget().maxDuration() == null
                || codingProperties.container() == null
                || codingProperties.agent() == null) {
            throw new IllegalStateException(
                    "app.coding.* properties incomplete — see application.yml");
        }
    }

    /**
     * Synchronously validates and persists a PENDING record. Returns immediately;
     * the caller is responsible for invoking {@link #executeCodingRun(UUID)} to
     * do the real work asynchronously.
     */
    public CodingRunRecord createRun(UUID planId,
                                     String repoUrl,
                                     String baseRef,
                                     Provider providerOrNull,
                                     String modelOverride,
                                     String webhookUrl) {
        return createRun(planId, repoUrl, baseRef, providerOrNull, modelOverride, webhookUrl,
                false, null, null);
    }

    public CodingRunRecord createRun(UUID planId,
                                     String repoUrl,
                                     String baseRef,
                                     Provider providerOrNull,
                                     String modelOverride,
                                     String webhookUrl,
                                     boolean autoOpenPr,
                                     String prTitle,
                                     String prBody) {
        PlanRecord plan = planRepo.findById(planId)
                .orElseThrow(() -> new PlanNotFoundException(planId));
        if (plan.getStatus() != PlanStatus.COMPLETED) {
            throw new PlanNotCompletedException(planId, plan.getStatus().name());
        }

        Provider provider = providerOrNull != null ? providerOrNull : llmProperties.defaultProvider();
        // Resolve eagerly so that 400 comes back on the POST, not in the async path.
        chatModelFactory.resolve(provider, modelOverride);
        String model = modelOverride != null ? modelOverride : defaultModel(provider);

        String resolvedRepo = (repoUrl == null || repoUrl.isBlank())
                ? codingProperties.defaultRepoUrl() : repoUrl;
        String resolvedRef = (baseRef == null || baseRef.isBlank())
                ? codingProperties.defaultBaseRef() : baseRef;

        CodingRunRecord record = CodingRunRecord.pending(
                planId, provider, model, resolvedRepo, resolvedRef, webhookUrl,
                autoOpenPr, prTitle, prBody);
        return runRepo.save(record);
    }

    @Async("codingTaskExecutor")
    public void executeCodingRun(UUID runId) {
        CodingRunRecord record = runRepo.findById(runId).orElse(null);
        if (record == null) {
            log.warn("Coding run {} not found; async job aborted", runId);
            return;
        }
        PlanRecord plan = planRepo.findById(record.getPlanId()).orElse(null);
        if (plan == null) {
            record.markFailed("plan_not_found", "Plan " + record.getPlanId() + " not found", 0L);
            runRepo.save(record);
            fireWebhook(record);
            return;
        }

        record.markInProgress();
        runRepo.save(record);

        ChatModel chatModel = chatModelFactory.resolve(record.getProvider(), record.getModel());
        CodingBudget budget = new CodingBudget(
                codingProperties.budget().maxTokens(),
                codingProperties.budget().maxIterations(),
                codingProperties.budget().maxDuration(),
                codingProperties.container().memory(),
                codingProperties.container().cpu()
        );

        CodeExecutor executor = executorProvider.getObject();
        Instant started = Instant.now();
        try {
            executor.provision(record.getRepoUrl(), record.getBaseRef(), budget);
            CodingResult result = agent.execute(chatModel, plan.getPlan(), executor, budget);
            long durationMs = Duration.between(started, Instant.now()).toMillis();
            record.markCompleted(result, durationMs);
            runRepo.save(record);
        } catch (CodingRunException e) {
            long durationMs = Duration.between(started, Instant.now()).toMillis();
            record.markFailed(e.code(), safeMessage(e), durationMs);
            runRepo.save(record);
        } catch (RuntimeException e) {
            long durationMs = Duration.between(started, Instant.now()).toMillis();
            log.error("Unexpected coding-run failure for {}", runId, e);
            record.markFailed("internal_error", safeMessage(e), durationMs);
            runRepo.save(record);
        } finally {
            safeDestroy(executor);
            fireWebhook(record);
            maybeAutoOpenPr(record);
        }
    }

    private void maybeAutoOpenPr(CodingRunRecord record) {
        PullRequestAutoOpener opener = autoOpenerProvider.getIfAvailable();
        if (opener == null || !record.isAutoOpenPr()) return;
        try {
            opener.openFor(record);
        } catch (RuntimeException e) {
            log.warn("Auto-open PR failed for run {}: {}", record.getId(), e.getMessage());
        }
    }

    private void fireWebhook(CodingRunRecord record) {
        if (record.getWebhookUrl() == null || record.getWebhookUrl().isBlank()) return;
        boolean sent = webhookClient.notify(record.getWebhookUrl(), record);
        if (sent) {
            record.markWebhookSent();
            runRepo.save(record);
        }
    }

    private void safeDestroy(CodeExecutor executor) {
        try {
            executor.destroy();
        } catch (RuntimeException e) {
            log.warn("Container destroy failed: {}", e.getMessage());
        }
    }

    private String defaultModel(Provider provider) {
        return switch (provider) {
            case OPENAI -> llmProperties.openai().defaultModel();
            case ANTHROPIC -> llmProperties.anthropic().defaultModel();
            case BEDROCK -> llmProperties.bedrock().defaultModel();
            case OLLAMA -> llmProperties.ollama().defaultModel();
            case LMSTUDIO -> llmProperties.lmstudio().defaultModel();
        };
    }

    private String safeMessage(Throwable e) {
        String m = e.getMessage();
        return (m == null || m.isBlank()) ? e.getClass().getSimpleName() : m;
    }
}
