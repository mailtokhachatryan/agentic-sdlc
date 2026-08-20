package com.agenticdev.sdlc.planning.api;

import com.agenticdev.sdlc.llm.Provider;
import com.agenticdev.sdlc.llm.config.LlmProperties;
import com.agenticdev.sdlc.planning.domain.InputType;
import com.agenticdev.sdlc.planning.domain.PlanResult;
import com.agenticdev.sdlc.planning.domain.PlanningService;
import com.agenticdev.sdlc.planning.persistence.PlanRecord;
import com.agenticdev.sdlc.planning.persistence.PlanRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlansController.class)
class PlansControllerValidationTest {

    @Autowired MockMvc mvc;

    @MockitoBean PlanningService service;
    @MockitoBean PlanRepository repo;
    @MockitoBean LlmProperties llmProperties;

    @Test
    void missingInputType_is400() throws Exception {
        mvc.perform(post("/api/v1/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"prompt":"x","provider":"ANTHROPIC"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void promptModeWithoutPrompt_is400() throws Exception {
        mvc.perform(post("/api/v1/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"inputType":"PROMPT","provider":"ANTHROPIC"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void promptModeWithJiraKey_is400() throws Exception {
        mvc.perform(post("/api/v1/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"inputType":"PROMPT","prompt":"x","jiraKey":"AS-1","provider":"ANTHROPIC"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void jiraModeWithoutJiraKey_is400() throws Exception {
        mvc.perform(post("/api/v1/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"inputType":"JIRA","provider":"ANTHROPIC"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void jiraModeWithPrompt_is400() throws Exception {
        mvc.perform(post("/api/v1/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"inputType":"JIRA","jiraKey":"AS-1","prompt":"x","provider":"ANTHROPIC"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformedJiraKey_is400() throws Exception {
        mvc.perform(post("/api/v1/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"inputType":"JIRA","jiraKey":"bad-key","provider":"ANTHROPIC"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void promptTooLong_is400() throws Exception {
        String longPrompt = "x".repeat(8001);
        mvc.perform(post("/api/v1/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"inputType":"PROMPT","prompt":"%s","provider":"ANTHROPIC"}
                                """.formatted(longPrompt)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validPromptRequest_is200() throws Exception {
        PlanRecord record = PlanRecord.promptPending(Provider.ANTHROPIC, "claude-sonnet-4-6", "do X");
        record.markCompleted(new PlanResult("s", "a", List.of(), List.of(), List.of(), List.of(), "md"), 100L);
        when(llmProperties.defaultProvider()).thenReturn(Provider.ANTHROPIC);
        when(service.createPlan(eq(Provider.ANTHROPIC), any(), eq(InputType.PROMPT), eq("do X"), any()))
                .thenReturn(record);

        mvc.perform(post("/api/v1/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"inputType":"PROMPT","prompt":"do X","provider":"ANTHROPIC"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }
}
