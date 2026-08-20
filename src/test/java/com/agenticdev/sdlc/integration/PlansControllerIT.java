package com.agenticdev.sdlc.integration;

import com.agenticdev.sdlc.jira.JiraClient;
import com.agenticdev.sdlc.jira.JiraTicket;
import com.agenticdev.sdlc.llm.Provider;
import com.agenticdev.sdlc.planning.api.dto.CreatePlanRequest;
import com.agenticdev.sdlc.planning.api.dto.PlanResponse;
import com.agenticdev.sdlc.planning.domain.InputType;
import com.agenticdev.sdlc.planning.domain.PlanResult;
import com.agenticdev.sdlc.planning.domain.PlanningAgent;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestPropertySource(properties = {
        "app.llm.default-provider=ANTHROPIC",
        "app.llm.anthropic.enabled=true",
        "app.llm.anthropic.api-key=test",
        "app.llm.openai.enabled=false",
        "app.llm.bedrock.enabled=false",
        "app.llm.ollama.enabled=false",
        "app.llm.lmstudio.enabled=false",
        "app.jira.enabled=false",
        "app.planning.agent.system-prompt=test system",
        "app.planning.agent.user-prompt-template=Task: {{task}}"
})
class PlansControllerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired TestRestTemplate http;

    @TestConfiguration
    static class StubConfig {
        @Bean @Primary
        Map<Provider, ChatModel> stubChatModels() {
            return Map.of(Provider.ANTHROPIC, mock(ChatModel.class));
        }

        @Bean @Primary
        PlanningAgent stubAgent() {
            PlanningAgent a = mock(PlanningAgent.class);
            PlanResult canned = new PlanResult(
                    "sum", "appr",
                    List.of(), List.of(), List.of(), List.of(), "# md");
            when(a.plan(any(), anyString())).thenReturn(canned);
            return a;
        }

        @Bean @Primary
        JiraClient stubJira() {
            JiraClient j = mock(JiraClient.class);
            when(j.fetch("AS-1"))
                    .thenReturn(new JiraTicket("AS-1", "Sum", "Desc", "Open", "Story"));
            return j;
        }
    }

    @Test
    void postPrompt_returns200WithPlan() {
        CreatePlanRequest req = new CreatePlanRequest(
                InputType.PROMPT, "do X", null, Provider.ANTHROPIC, null);
        ResponseEntity<PlanResponse> r = http.postForEntity("/api/v1/plans", req, PlanResponse.class);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody().status().name()).isEqualTo("COMPLETED");
        assertThat(r.getBody().plan().summary()).isEqualTo("sum");
    }

    @Test
    void postJira_returns200AndPersistsSnapshot() {
        CreatePlanRequest req = new CreatePlanRequest(
                InputType.JIRA, null, "AS-1", Provider.ANTHROPIC, null);
        ResponseEntity<PlanResponse> r = http.postForEntity("/api/v1/plans", req, PlanResponse.class);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody().jiraKey()).isEqualTo("AS-1");
    }

    @Test
    void getById_returnsStoredPlan() {
        CreatePlanRequest req = new CreatePlanRequest(
                InputType.PROMPT, "do Y", null, Provider.ANTHROPIC, null);
        PlanResponse created = http.postForObject("/api/v1/plans", req, PlanResponse.class);

        ResponseEntity<PlanResponse> fetched = http.getForEntity(
                "/api/v1/plans/" + created.id(), PlanResponse.class);

        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fetched.getBody().id()).isEqualTo(created.id());
    }

    @Test
    void postWithDisabledProvider_is400() {
        CreatePlanRequest req = new CreatePlanRequest(
                InputType.PROMPT, "do Z", null, Provider.OPENAI, null);
        ResponseEntity<String> r = http.postForEntity("/api/v1/plans", req, String.class);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getBody()).contains("provider_not_configured");
    }

    @Test
    void postWithoutProvider_usesDefault() {
        CreatePlanRequest req = new CreatePlanRequest(
                InputType.PROMPT, "do W", null, null, null);
        ResponseEntity<PlanResponse> r = http.postForEntity("/api/v1/plans", req, PlanResponse.class);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody().provider()).isEqualTo(Provider.ANTHROPIC);
    }
}
