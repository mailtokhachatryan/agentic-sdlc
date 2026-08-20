package com.agenticdev.sdlc.github.persistence;

import com.agenticdev.sdlc.github.domain.MergeStrategy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PullRequestRecordTest {

    @Test
    void pending_initializesFields() {
        PullRequestRecord r = PullRequestRecord.pending(UUID.randomUUID(),
                "https://x", "main", "t", "b", false,
                List.of("agentic-sdlc"), null);
        assertThat(r.getStatus()).isEqualTo(PullRequestStatus.PENDING);
        assertThat(r.getLabels()).containsExactly("agentic-sdlc");
        assertThat(r.isWebhookSent()).isFalse();
    }

    @Test
    void markPushed_setsSha() {
        PullRequestRecord r = PullRequestRecord.pending(UUID.randomUUID(),
                "https://x", "main", "t", "b", false, List.of(), null);
        r.markPushed("agentic/abc", "sha123");
        assertThat(r.getStatus()).isEqualTo(PullRequestStatus.PUSHED);
        assertThat(r.getHeadBranch()).isEqualTo("agentic/abc");
        assertThat(r.getHeadSha()).isEqualTo("sha123");
    }

    @Test
    void markOpen_nonDraft_movesToOpen() {
        PullRequestRecord r = PullRequestRecord.pending(UUID.randomUUID(),
                "https://x", "main", "t", "b", false, List.of(), null);
        r.markOpen(1, "https://url", List.of("alice"));
        assertThat(r.getStatus()).isEqualTo(PullRequestStatus.OPEN);
        assertThat(r.getPrNumber()).isEqualTo(1);
        assertThat(r.getReviewers()).containsExactly("alice");
    }

    @Test
    void markOpen_draft_movesToDraft() {
        PullRequestRecord r = PullRequestRecord.pending(UUID.randomUUID(),
                "https://x", "main", "t", "b", true, List.of(), null);
        r.markOpen(1, "https://url", List.of());
        assertThat(r.getStatus()).isEqualTo(PullRequestStatus.DRAFT);
    }

    @Test
    void markReady_flipsDraft() {
        PullRequestRecord r = PullRequestRecord.pending(UUID.randomUUID(),
                "https://x", "main", "t", "b", true, List.of(), null);
        r.markOpen(1, "url", List.of());
        r.markReady();
        assertThat(r.isDraft()).isFalse();
        assertThat(r.getStatus()).isEqualTo(PullRequestStatus.OPEN);
    }

    @Test
    void markMerged_setsShaAndStrategy() {
        PullRequestRecord r = PullRequestRecord.pending(UUID.randomUUID(),
                "https://x", "main", "t", "b", false, List.of(), null);
        r.markMerged("abc", MergeStrategy.SQUASH);
        assertThat(r.getStatus()).isEqualTo(PullRequestStatus.MERGED);
        assertThat(r.getMergedSha()).isEqualTo("abc");
        assertThat(r.getMergeStrategy()).isEqualTo(MergeStrategy.SQUASH);
    }

    @Test
    void markFailed_setsError() {
        PullRequestRecord r = PullRequestRecord.pending(UUID.randomUUID(),
                "https://x", "main", "t", "b", false, List.of(), null);
        r.markFailed("push_failed", "denied", 100L);
        assertThat(r.getStatus()).isEqualTo(PullRequestStatus.FAILED);
        assertThat(r.getErrorCode()).isEqualTo("push_failed");
        assertThat(r.getDurationMs()).isEqualTo(100L);
    }
}
