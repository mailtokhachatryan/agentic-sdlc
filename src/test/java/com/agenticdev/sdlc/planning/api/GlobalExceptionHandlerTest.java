package com.agenticdev.sdlc.planning.api;

import com.agenticdev.sdlc.jira.JiraClientException;
import com.agenticdev.sdlc.llm.Provider;
import com.agenticdev.sdlc.llm.ProviderNotConfiguredException;
import com.agenticdev.sdlc.planning.api.dto.ErrorResponse;
import com.agenticdev.sdlc.planning.domain.LlmCallException;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    Tracer tracer = mock(Tracer.class);
    @SuppressWarnings("unchecked")
    ObjectProvider<Tracer> tracerProvider = mock(ObjectProvider.class);
    GlobalExceptionHandler h = new GlobalExceptionHandler(tracerProvider);

    {
        when(tracerProvider.getIfAvailable()).thenReturn(tracer);
    }

    @Test
    void providerNotConfigured_is400() {
        when(tracer.currentSpan()).thenReturn(null);
        ResponseEntity<ErrorResponse> r = h.handleProvider(new ProviderNotConfiguredException(Provider.OPENAI));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getBody().code()).isEqualTo("provider_not_configured");
    }

    @Test
    void jiraFailure_is502() {
        when(tracer.currentSpan()).thenReturn(null);
        ResponseEntity<ErrorResponse> r = h.handleJira(new JiraClientException("boom"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(r.getBody().code()).isEqualTo("jira_fetch_failed");
    }

    @Test
    void llmFailure_is502() {
        when(tracer.currentSpan()).thenReturn(null);
        ResponseEntity<ErrorResponse> r = h.handleLlm(new LlmCallException("x", new RuntimeException()));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(r.getBody().code()).isEqualTo("llm_call_failed");
    }

    @Test
    void genericFailure_is500() {
        when(tracer.currentSpan()).thenReturn(null);
        ResponseEntity<ErrorResponse> r = h.handleGeneric(new RuntimeException("bang"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(r.getBody().code()).isEqualTo("internal_error");
    }
}
