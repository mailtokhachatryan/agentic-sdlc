package com.agenticdev.sdlc.planning.api;

import com.agenticdev.sdlc.coding.domain.ContainerException;
import com.agenticdev.sdlc.coding.domain.PlanNotCompletedException;
import com.agenticdev.sdlc.coding.domain.PlanNotFoundException;
import com.agenticdev.sdlc.coding.domain.RepoCloneException;
import com.agenticdev.sdlc.github.domain.BranchExistsException;
import com.agenticdev.sdlc.github.domain.CodingRunNotCompletedException;
import com.agenticdev.sdlc.github.domain.CodingRunNotFoundException;
import com.agenticdev.sdlc.github.domain.EmptyDiffException;
import com.agenticdev.sdlc.github.domain.GitHubUnavailableException;
import com.agenticdev.sdlc.github.domain.PushFailedException;
import com.agenticdev.sdlc.jira.JiraClientException;
import com.agenticdev.sdlc.llm.ProviderNotConfiguredException;
import com.agenticdev.sdlc.planning.api.dto.ErrorResponse;
import com.agenticdev.sdlc.planning.domain.LlmCallException;
import io.micrometer.tracing.Tracer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final ObjectProvider<Tracer> tracerProvider;

    public GlobalExceptionHandler(ObjectProvider<Tracer> tracerProvider) {
        this.tracerProvider = tracerProvider;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("validation failed");
        return ResponseEntity.badRequest().body(body("validation_failed", msg));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(body("validation_failed", ex.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(body("validation_failed", rootMessage(ex)));
    }

    private String rootMessage(Throwable e) {
        Throwable cur = e;
        while (cur.getCause() != null && cur.getCause() != cur) cur = cur.getCause();
        return cur.getMessage() == null ? e.getMessage() : cur.getMessage();
    }

    @ExceptionHandler(ProviderNotConfiguredException.class)
    public ResponseEntity<ErrorResponse> handleProvider(ProviderNotConfiguredException ex) {
        return ResponseEntity.badRequest().body(body("provider_not_configured", ex.getMessage()));
    }

    @ExceptionHandler(JiraClientException.class)
    public ResponseEntity<ErrorResponse> handleJira(JiraClientException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body("jira_fetch_failed", ex.getMessage()));
    }

    @ExceptionHandler(LlmCallException.class)
    public ResponseEntity<ErrorResponse> handleLlm(LlmCallException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body("llm_call_failed", ex.getMessage()));
    }

    @ExceptionHandler(PlanNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePlanNotFound(PlanNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body("plan_not_found", ex.getMessage()));
    }

    @ExceptionHandler(PlanNotCompletedException.class)
    public ResponseEntity<ErrorResponse> handlePlanNotCompleted(PlanNotCompletedException ex) {
        return ResponseEntity.badRequest().body(body("plan_not_completed", ex.getMessage()));
    }

    @ExceptionHandler(RepoCloneException.class)
    public ResponseEntity<ErrorResponse> handleRepoClone(RepoCloneException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body("repo_clone_failed", ex.getMessage()));
    }

    @ExceptionHandler(ContainerException.class)
    public ResponseEntity<ErrorResponse> handleContainer(ContainerException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body("container_error", ex.getMessage()));
    }

    @ExceptionHandler(CodingRunNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCodingRunNotFound(CodingRunNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body("coding_run_not_found", ex.getMessage()));
    }

    @ExceptionHandler(CodingRunNotCompletedException.class)
    public ResponseEntity<ErrorResponse> handleCodingRunNotCompleted(CodingRunNotCompletedException ex) {
        return ResponseEntity.badRequest().body(body("coding_run_not_completed", ex.getMessage()));
    }

    @ExceptionHandler(EmptyDiffException.class)
    public ResponseEntity<ErrorResponse> handleEmptyDiff(EmptyDiffException ex) {
        return ResponseEntity.badRequest().body(body("empty_diff", ex.getMessage()));
    }

    @ExceptionHandler(BranchExistsException.class)
    public ResponseEntity<ErrorResponse> handleBranchExists(BranchExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body("branch_exists", ex.getMessage()));
    }

    @ExceptionHandler(PushFailedException.class)
    public ResponseEntity<ErrorResponse> handlePushFailed(PushFailedException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body("push_failed", ex.getMessage()));
    }

    @ExceptionHandler(GitHubUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleGitHubUnavailable(GitHubUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body("github_unavailable", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        return ResponseEntity.internalServerError().body(body("internal_error", ex.getMessage()));
    }

    private ErrorResponse body(String code, String message) {
        Tracer tracer = tracerProvider.getIfAvailable();
        String traceId = null;
        if (tracer != null && tracer.currentSpan() != null) {
            traceId = tracer.currentSpan().context().traceId();
        }
        return new ErrorResponse(code, message, traceId);
    }
}
