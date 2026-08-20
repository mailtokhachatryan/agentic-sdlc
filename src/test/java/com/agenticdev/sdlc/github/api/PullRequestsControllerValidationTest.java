package com.agenticdev.sdlc.github.api;

import com.agenticdev.sdlc.github.domain.PullRequestService;
import com.agenticdev.sdlc.github.persistence.PullRequestRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PullRequestsController.class)
class PullRequestsControllerValidationTest {

    @Autowired MockMvc mvc;
    @MockitoBean PullRequestService service;
    @MockitoBean PullRequestRepository repo;

    @Test
    void missingCodingRunId_is400() throws Exception {
        mvc.perform(post("/api/v1/pull-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"feat: x"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidUuid_is400() throws Exception {
        mvc.perform(post("/api/v1/pull-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codingRunId":"not-a-uuid"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mergeWithoutStrategy_is400() throws Exception {
        String id = "018f1f9a-3c42-7e4a-9f1d-0123456789ab";
        mvc.perform(post("/api/v1/pull-requests/" + id + "/merge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void commentEmptyBody_is400() throws Exception {
        String id = "018f1f9a-3c42-7e4a-9f1d-0123456789ab";
        mvc.perform(post("/api/v1/pull-requests/" + id + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":""}
                                """))
                .andExpect(status().isBadRequest());
    }
}
