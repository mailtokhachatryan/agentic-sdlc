package com.agenticdev.sdlc.coding.api;

import com.agenticdev.sdlc.coding.domain.CodingService;
import com.agenticdev.sdlc.coding.persistence.CodingRunRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CodingRunsController.class)
class CodingRunsControllerValidationTest {

    @Autowired MockMvc mvc;
    @MockitoBean CodingService service;
    @MockitoBean CodingRunRepository repo;

    @Test
    void missingPlanId_is400() throws Exception {
        mvc.perform(post("/api/v1/coding-runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"repoUrl":"https://github.com/o/r.git"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformedRepoUrl_is400() throws Exception {
        mvc.perform(post("/api/v1/coding-runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"planId":"018f1f9a-3c42-7e4a-9f1d-0123456789ab","repoUrl":"not-a-url"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformedBaseRef_is400() throws Exception {
        mvc.perform(post("/api/v1/coding-runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"planId":"018f1f9a-3c42-7e4a-9f1d-0123456789ab","baseRef":"bad ref!"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownProvider_is400() throws Exception {
        mvc.perform(post("/api/v1/coding-runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"planId":"018f1f9a-3c42-7e4a-9f1d-0123456789ab","provider":"NOPE"}
                                """))
                .andExpect(status().isBadRequest());
    }
}
