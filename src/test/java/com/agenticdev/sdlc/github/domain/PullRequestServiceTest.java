package com.agenticdev.sdlc.github.domain;

import com.agenticdev.sdlc.coding.persistence.CodingRunRecord;
import com.agenticdev.sdlc.coding.persistence.CodingRunRepository;
import com.agenticdev.sdlc.github.config.GitHubProperties;
import com.agenticdev.sdlc.github.persistence.PullRequestRecord;
import com.agenticdev.sdlc.github.persistence.PullRequestRepository;
import com.agenticdev.sdlc.github.persistence.PullRequestStatus;
import com.agenticdev.sdlc.github.pipeline.DiffApplier;
import com.agenticdev.sdlc.llm.Provider;
import com.agenticdev.sdlc.planning.domain.PlanResult;
import com.agenticdev.sdlc.planning.persistence.PlanRecord;
import com.agenticdev.sdlc.planning.persistence.PlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PullRequestServiceTest {

    CodingRunRepository codingRunRepo = mock(CodingRunRepository.class);
    PlanRepository planRepo = mock(PlanRepository.class);
    PullRequestRepository prRepo = mock(PullRequestRepository.class);
    DiffApplier diffApplier = mock(DiffApplier.class);
    CodeownersResolver codeownersResolver = mock(CodeownersResolver.class);
    GitHubClient gitHubClient = mock(GitHubClient.class);

    GitHubProperties props = new GitHubProperties(
            true, GitHubProperties.AuthMode.PAT,
            new GitHubProperties.Pat("ghp_x"),
            new GitHubProperties.App(null, null, null),
            "agentic/",
            List.of("agentic-sdlc"),
            new GitHubProperties.Commit("Agentic", "bot@example.com", "feat: {{summary}}"),
            new GitHubProperties.Retry(3, Duration.ofMillis(500), Duration.ofSeconds(5), 2.0),
            "body: {{summary}}"
    );

    PullRequestService service;

    @BeforeEach
    void setUp() {
        service = new PullRequestService(codingRunRepo, planRepo, prRepo,
                diffApplier, codeownersResolver, gitHubClient, props);
        service.validate();
        when(prRepo.save(any(PullRequestRecord.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private CodingRunRecord completedRun(UUID id, UUID planId, String diff) {
        CodingRunRecord r = CodingRunRecord.pending(planId, Provider.LMSTUDIO, "m",
                "https://github.com/org/repo.git", "main", null);
        setField(r, "id", id);
        r.markInProgress();
        r.markCompleted(new com.agenticdev.sdlc.coding.domain.CodingResult(
                diff, 1, true, 3, 100, List.of("a.txt"), null), 1000L);
        return r;
    }

    private PlanRecord completedPlan() {
        PlanRecord p = PlanRecord.promptPending(Provider.LMSTUDIO, "m", "do X");
        p.markCompleted(new PlanResult("sum", "appr", List.of(), List.of(), List.of(), List.of(), "# md"), 100L);
        return p;
    }

    @Test
    void createRun_codingRunNotFound_throws() {
        UUID id = UUID.randomUUID();
        when(codingRunRepo.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createRun(id, null, null, false, null, null))
                .isInstanceOf(CodingRunNotFoundException.class);
    }

    @Test
    void createRun_codingRunNotCompleted_throws() {
        UUID id = UUID.randomUUID();
        CodingRunRecord r = CodingRunRecord.pending(UUID.randomUUID(), Provider.LMSTUDIO,
                "m", "https://x", "main", null);
        when(codingRunRepo.findById(id)).thenReturn(Optional.of(r));
        assertThatThrownBy(() -> service.createRun(id, null, null, false, null, null))
                .isInstanceOf(CodingRunNotCompletedException.class);
    }

    @Test
    void createRun_emptyDiff_throws() {
        UUID id = UUID.randomUUID();
        when(codingRunRepo.findById(id)).thenReturn(Optional.of(completedRun(id, UUID.randomUUID(), "")));
        when(planRepo.findById(any())).thenReturn(Optional.of(completedPlan()));
        assertThatThrownBy(() -> service.createRun(id, null, null, false, null, null))
                .isInstanceOf(EmptyDiffException.class);
    }

    @Test
    void createRun_savesPendingWithDefaults() {
        UUID id = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        when(codingRunRepo.findById(id)).thenReturn(Optional.of(completedRun(id, planId, "diff content")));
        when(planRepo.findById(planId)).thenReturn(Optional.of(completedPlan()));

        PullRequestRecord r = service.createRun(id, null, null, false, null, null);
        assertThat(r.getStatus()).isEqualTo(PullRequestStatus.PENDING);
        assertThat(r.getTitle()).startsWith("feat: ");
        assertThat(r.getLabels()).contains("agentic-sdlc");
    }

    @Test
    void executePr_happyPath() {
        UUID prId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        PullRequestRecord record = PullRequestRecord.pending(runId,
                "https://github.com/org/repo.git", "main",
                "feat: do x", "body", false, List.of("agentic-sdlc"), null);
        setField(record, "id", prId);
        when(prRepo.findById(prId)).thenReturn(Optional.of(record));
        when(codingRunRepo.findById(runId)).thenReturn(Optional.of(completedRun(runId, planId, "diff...")));
        when(planRepo.findById(planId)).thenReturn(Optional.of(completedPlan()));
        when(diffApplier.apply(anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString()))
                .thenReturn(new DiffApplier.Result("abc123",
                        "* @alice\n", List.of("a.txt")));
        when(codeownersResolver.reviewersFor(anyString(), anyList()))
                .thenReturn(List.of("alice"));
        when(gitHubClient.openPullRequest(any()))
                .thenReturn(new GitHubClient.OpenedPr(42, "https://github.com/org/repo/pull/42"));

        service.executePr(prId);

        assertThat(record.getStatus()).isEqualTo(PullRequestStatus.OPEN);
        assertThat(record.getPrNumber()).isEqualTo(42);
        assertThat(record.getReviewers()).containsExactly("alice");
        verify(gitHubClient).applyLabels(eq("org"), eq("repo"), eq(42), anyList());
        verify(gitHubClient).requestReviewers(eq("org"), eq("repo"), eq(42), anyList());
    }

    @Test
    void executePr_pushFailure_marksFailed() {
        UUID prId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        PullRequestRecord record = PullRequestRecord.pending(runId,
                "https://github.com/org/repo.git", "main",
                "title", "body", false, List.of(), null);
        setField(record, "id", prId);
        when(prRepo.findById(prId)).thenReturn(Optional.of(record));
        when(codingRunRepo.findById(runId)).thenReturn(Optional.of(completedRun(runId, planId, "diff")));
        when(planRepo.findById(any())).thenReturn(Optional.of(completedPlan()));
        when(diffApplier.apply(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new PushFailedException("denied"));

        service.executePr(prId);

        assertThat(record.getStatus()).isEqualTo(PullRequestStatus.FAILED);
        assertThat(record.getErrorCode()).isEqualTo("push_failed");
        verify(gitHubClient, never()).openPullRequest(any());
    }

    @Test
    void executePr_githubFailure_marksFailed() {
        UUID prId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        PullRequestRecord record = PullRequestRecord.pending(runId,
                "https://github.com/org/repo.git", "main",
                "title", "body", false, List.of(), null);
        setField(record, "id", prId);
        when(prRepo.findById(prId)).thenReturn(Optional.of(record));
        when(codingRunRepo.findById(runId)).thenReturn(Optional.of(completedRun(runId, planId, "diff")));
        when(planRepo.findById(any())).thenReturn(Optional.of(completedPlan()));
        when(diffApplier.apply(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new DiffApplier.Result("sha", null, List.of()));
        when(codeownersResolver.reviewersFor(any(), any())).thenReturn(List.of());
        when(gitHubClient.openPullRequest(any()))
                .thenThrow(new GitHubUnavailableException("502"));

        service.executePr(prId);

        assertThat(record.getStatus()).isEqualTo(PullRequestStatus.FAILED);
        assertThat(record.getErrorCode()).isEqualTo("github_unavailable");
    }

    @Test
    void executePr_missingRecord_aborts() {
        UUID id = UUID.randomUUID();
        when(prRepo.findById(id)).thenReturn(Optional.empty());
        service.executePr(id);
        verify(diffApplier, never()).apply(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void parseOwnerRepo_handlesHttpsUrl() {
        PullRequestService.OwnerRepo or = PullRequestService.parseOwnerRepo(
                "https://github.com/agenticdev/project.git");
        assertThat(or.owner()).isEqualTo("agenticdev");
        assertThat(or.repo()).isEqualTo("project");
    }

    @Test
    void parseOwnerRepo_handlesSshUrl() {
        PullRequestService.OwnerRepo or = PullRequestService.parseOwnerRepo(
                "git@github.com:agenticdev/project.git");
        assertThat(or.owner()).isEqualTo("agenticdev");
        assertThat(or.repo()).isEqualTo("project");
    }

    // reflection for setting @Id on entity
    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field f = findField(target.getClass(), fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Field findField(Class<?> c, String name) throws NoSuchFieldException {
        while (c != null) {
            try { return c.getDeclaredField(name); } catch (NoSuchFieldException ignore) { c = c.getSuperclass(); }
        }
        throw new NoSuchFieldException(name);
    }
}
