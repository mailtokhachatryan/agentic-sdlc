package com.agenticdev.sdlc.planning.domain;

import com.agenticdev.sdlc.jira.JiraClient;
import com.agenticdev.sdlc.jira.JiraClientException;
import com.agenticdev.sdlc.jira.JiraTicket;
import com.agenticdev.sdlc.llm.ChatModelFactory;
import com.agenticdev.sdlc.llm.Provider;
import com.agenticdev.sdlc.llm.config.LlmProperties;
import com.agenticdev.sdlc.observability.LlmCallMetrics;
import com.agenticdev.sdlc.planning.persistence.PlanRecord;
import com.agenticdev.sdlc.planning.persistence.PlanRepository;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class PlanningService {

    private final ChatModelFactory chatModelFactory;
    private final JiraClient jiraClient;
    private final PlanningAgent agent;
    private final PlanRepository repo;
    private final LlmProperties llmProperties;
    private final LlmCallMetrics metrics;

    public PlanningService(ChatModelFactory chatModelFactory,
                           JiraClient jiraClient,
                           PlanningAgent agent,
                           PlanRepository repo,
                           LlmProperties llmProperties,
                           LlmCallMetrics metrics) {
        this.chatModelFactory = chatModelFactory;
        this.jiraClient = jiraClient;
        this.agent = agent;
        this.repo = repo;
        this.llmProperties = llmProperties;
        this.metrics = metrics;
    }

    public PlanRecord createPlan(Provider provider,
                                 String modelOverride,
                                 InputType inputType,
                                 String prompt,
                                 String jiraKey) {

        ChatModel chatModel = chatModelFactory.resolve(provider, modelOverride);
        String model = modelOverride != null ? modelOverride : defaultModel(provider);

        JiraTicket ticket = null;
        String taskText;
        if (inputType == InputType.JIRA) {
            try {
                ticket = jiraClient.fetch(jiraKey);
                taskText = formatTicket(ticket);
            } catch (JiraClientException e) {
                PlanRecord failed = PlanRecord.jiraPending(provider, model,
                        new JiraTicket(jiraKey, "", "", "", ""));
                failed.markFailed("jira_fetch_failed", safeMessage(e), 0L);
                repo.save(failed);
                throw e;
            }
        } else {
            taskText = prompt;
        }

        PlanRecord record = (inputType == InputType.JIRA)
                ? PlanRecord.jiraPending(provider, model, ticket)
                : PlanRecord.promptPending(provider, model, prompt);
        repo.save(record);

        Instant started = Instant.now();
        try {
            PlanResult plan = agent.plan(chatModel, taskText);
            long durationMs = Duration.between(started, Instant.now()).toMillis();
            record.markCompleted(plan, durationMs);
            repo.save(record);
            metrics.recordCall(provider.name(), model, Duration.ofMillis(durationMs));
            metrics.recordPlan(provider.name(), model, inputType.name(), "COMPLETED", Duration.ofMillis(durationMs));
            return record;
        } catch (LlmCallException e) {
            long durationMs = Duration.between(started, Instant.now()).toMillis();
            record.markFailed("llm_call_failed", safeMessage(e), durationMs);
            repo.save(record);
            metrics.recordPlan(provider.name(), model, inputType.name(), "FAILED", Duration.ofMillis(durationMs));
            throw e;
        } catch (RuntimeException e) {
            long durationMs = Duration.between(started, Instant.now()).toMillis();
            record.markFailed("internal_error", safeMessage(e), durationMs);
            repo.save(record);
            metrics.recordPlan(provider.name(), model, inputType.name(), "FAILED", Duration.ofMillis(durationMs));
            throw e;
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

    private String formatTicket(JiraTicket t) {
        return "Jira: " + t.key() + "\n"
                + "Type: " + t.issueType() + " | Status: " + t.status() + "\n"
                + "Summary: " + t.summary() + "\n"
                + "Description:\n" + t.description();
    }

    private String safeMessage(Throwable e) {
        String m = e.getMessage();
        return (m == null || m.isBlank()) ? e.getClass().getSimpleName() : m;
    }
}
