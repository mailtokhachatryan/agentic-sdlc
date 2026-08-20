package com.agenticdev.sdlc.planning.persistence;

import com.agenticdev.sdlc.jira.JiraTicket;
import com.agenticdev.sdlc.llm.Provider;
import com.agenticdev.sdlc.planning.domain.InputType;
import com.agenticdev.sdlc.planning.domain.PlanResult;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "plans")
public class PlanRecord {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider;

    @Column(nullable = false, length = 128)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(name = "input_type", nullable = false)
    private InputType inputType;

    @Column(columnDefinition = "TEXT")
    private String prompt;

    @Column(name = "jira_key", length = 64)
    private String jiraKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "jira_snapshot", columnDefinition = "jsonb")
    private JiraTicket jiraSnapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "plan_json", columnDefinition = "jsonb")
    private PlanResult plan;

    @Column(name = "error_code", length = 64)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PlanRecord() { /* JPA */ }

    private PlanRecord(Provider provider, String model, InputType inputType,
                       String prompt, String jiraKey, JiraTicket jiraSnapshot) {
        this.id = UUID.randomUUID();
        this.status = PlanStatus.PENDING;
        this.provider = provider;
        this.model = model;
        this.inputType = inputType;
        this.prompt = prompt;
        this.jiraKey = jiraKey;
        this.jiraSnapshot = jiraSnapshot;
        this.createdAt = Instant.now();
    }

    public static PlanRecord promptPending(Provider provider, String model, String prompt) {
        return new PlanRecord(provider, model, InputType.PROMPT, prompt, null, null);
    }

    public static PlanRecord jiraPending(Provider provider, String model, JiraTicket ticket) {
        return new PlanRecord(provider, model, InputType.JIRA, null, ticket.key(), ticket);
    }

    public void markCompleted(PlanResult plan, long durationMs) {
        this.status = PlanStatus.COMPLETED;
        this.plan = plan;
        this.durationMs = durationMs;
        this.errorCode = null;
        this.errorMessage = null;
    }

    public void markFailed(String code, String message, long durationMs) {
        this.status = PlanStatus.FAILED;
        this.errorCode = code;
        this.errorMessage = message;
        this.durationMs = durationMs;
    }

    public UUID getId() { return id; }
    public PlanStatus getStatus() { return status; }
    public Provider getProvider() { return provider; }
    public String getModel() { return model; }
    public InputType getInputType() { return inputType; }
    public String getPrompt() { return prompt; }
    public String getJiraKey() { return jiraKey; }
    public JiraTicket getJiraSnapshot() { return jiraSnapshot; }
    public PlanResult getPlan() { return plan; }
    public String getErrorCode() { return errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public Long getDurationMs() { return durationMs; }
    public Instant getCreatedAt() { return createdAt; }
}
