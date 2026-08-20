package com.agenticdev.sdlc.integration;

import com.agenticdev.sdlc.coding.api.dto.CodingRunResponse;
import com.agenticdev.sdlc.coding.api.dto.CreateCodingRunRequest;
import com.agenticdev.sdlc.coding.domain.CodeExecutor;
import com.agenticdev.sdlc.coding.domain.CodingAgent;
import com.agenticdev.sdlc.coding.domain.CodingResult;
import com.agenticdev.sdlc.coding.persistence.CodingRunStatus;
import com.agenticdev.sdlc.jira.JiraClient;
import com.agenticdev.sdlc.llm.Provider;
import com.agenticdev.sdlc.planning.api.dto.CreatePlanRequest;
import com.agenticdev.sdlc.planning.api.dto.PlanResponse;
import com.agenticdev.sdlc.planning.domain.InputType;
import com.agenticdev.sdlc.planning.domain.PlanResult;
import com.agenticdev.sdlc.planning.domain.PlanningAgent;
import com.github.tomakehurst.wiremock.WireMockServer;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
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
        "app.planning.agent.user-prompt-template=Task: {{task}}",
        "app.coding.budget.max-tokens=100000",
        "app.coding.budget.max-iterations=10",
        "app.coding.budget.max-duration=1m",
        "app.coding.container.image=eclipse-temurin:21-jdk",
        "app.coding.container.memory=1g",
        "app.coding.container.cpu=1.0",
        "app.coding.container.network-disabled=true",
        "app.coding.default-repo-url=https://github.com/default/repo.git",
        "app.coding.default-base-ref=main",
        "app.coding.agent.system-prompt=coding sys",
        "app.coding.agent.test-command=./mvnw test",
        "app.coding.webhook.connect-timeout=2s",
        "app.coding.webhook.read-timeout=5s"
})
class CodingRunsControllerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    static final WireMockServer wireMock = new WireMockServer(wireMockConfig().dynamicPort());

    @BeforeAll
    static void startWireMock() {
        wireMock.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @Autowired TestRestTemplate http;

    @TestConfiguration
    static class StubConfig {

        @Bean @Primary
        Map<Provider, ChatModel> stubChatModels() {
            return Map.of(Provider.ANTHROPIC, mock(ChatModel.class));
        }

        @Bean @Primary
        PlanningAgent stubPlanningAgent() {
            PlanningAgent a = mock(PlanningAgent.class);
            PlanResult canned = new PlanResult("sum", "appr",
                    List.of(), List.of(), List.of(), List.of(), "# md");
            when(a.plan(any(), anyString())).thenReturn(canned);
            return a;
        }

        @Bean @Primary
        JiraClient stubJira() {
            return mock(JiraClient.class);
        }

        @Bean @Primary
        CodingAgent stubCodingAgent() {
            CodingAgent a = mock(CodingAgent.class);
            when(a.execute(any(), any(), any(), any()))
                    .thenReturn(new CodingResult(
                            "diff --git a/README.md b/README.md\n+ hello\n",
                            1, true, 3, 1234, List.of("README.md"), null));
            return a;
        }

        @Bean @Primary
        @Scope("prototype")
        CodeExecutor stubCodeExecutor() {
            return mock(CodeExecutor.class);
        }
    }

    @Test
    void codingRunLifecycle_completesAndFiresWebhook() {
        // 1. Create a plan to reference.
        CreatePlanRequest planReq = new CreatePlanRequest(
                InputType.PROMPT, "Add hello to README", null, Provider.ANTHROPIC, null);
        ResponseEntity<PlanResponse> planResp = http.postForEntity("/api/v1/plans", planReq, PlanResponse.class);
        assertThat(planResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        UUID planId = planResp.getBody().id();

        // 2. WireMock stub for webhook delivery.
        String webhookPath = "/webhooks/coding-done";
        wireMock.stubFor(post(urlEqualTo(webhookPath))
                .willReturn(aResponse().withStatus(200)));
        String webhookUrl = "http://localhost:" + wireMock.port() + webhookPath;

        // 3. Start coding run.
        CreateCodingRunRequest runReq = new CreateCodingRunRequest(
                planId, "https://github.com/org/repo.git", "main",
                Provider.ANTHROPIC, null, webhookUrl,
                false, null, null);
        ResponseEntity<CodingRunResponse> runResp = http.postForEntity(
                "/api/v1/coding-runs", runReq, CodingRunResponse.class);
        assertThat(runResp.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(runResp.getBody().status()).isEqualTo(CodingRunStatus.PENDING);
        UUID runId = runResp.getBody().id();

        // 4. Poll until COMPLETED.
        AtomicReference<CodingRunResponse> finalState = new AtomicReference<>();
        await().atMost(10, TimeUnit.SECONDS).pollInterval(200, TimeUnit.MILLISECONDS).untilAsserted(() -> {
            ResponseEntity<CodingRunResponse> r = http.getForEntity(
                    "/api/v1/coding-runs/" + runId, CodingRunResponse.class);
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(r.getBody().status()).isEqualTo(CodingRunStatus.COMPLETED);
            finalState.set(r.getBody());
        });

        assertThat(finalState.get().diff()).contains("hello");
        assertThat(finalState.get().filesChanged()).isEqualTo(1);
        assertThat(finalState.get().testsPassed()).isTrue();

        // 5. Verify webhook fired.
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                wireMock.verify(postRequestedFor(urlEqualTo(webhookPath))));
    }

    @Test
    void missingPlan_returns404() {
        CreateCodingRunRequest runReq = new CreateCodingRunRequest(
                UUID.randomUUID(), "https://github.com/org/repo.git", "main",
                Provider.ANTHROPIC, null, null,
                false, null, null);
        ResponseEntity<String> r = http.postForEntity("/api/v1/coding-runs", runReq, String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(r.getBody()).contains("plan_not_found");
    }

    @Test
    void getDiff_returnsPlainText() {
        CreatePlanRequest planReq = new CreatePlanRequest(
                InputType.PROMPT, "do X", null, Provider.ANTHROPIC, null);
        PlanResponse plan = http.postForObject("/api/v1/plans", planReq, PlanResponse.class);
        CreateCodingRunRequest runReq = new CreateCodingRunRequest(
                plan.id(), "https://github.com/org/repo.git", "main",
                Provider.ANTHROPIC, null, null,
                false, null, null);
        CodingRunResponse run = http.postForObject("/api/v1/coding-runs", runReq, CodingRunResponse.class);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            ResponseEntity<CodingRunResponse> r = http.getForEntity(
                    "/api/v1/coding-runs/" + run.id(), CodingRunResponse.class);
            assertThat(r.getBody().status()).isIn(CodingRunStatus.COMPLETED, CodingRunStatus.FAILED);
        });

        ResponseEntity<String> diffResp = http.getForEntity(
                "/api/v1/coding-runs/" + run.id() + "/diff", String.class);
        assertThat(diffResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
