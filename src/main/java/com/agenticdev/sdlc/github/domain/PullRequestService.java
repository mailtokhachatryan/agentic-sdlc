package com.agenticdev.sdlc.github.domain;

import com.agenticdev.sdlc.coding.persistence.CodingRunRecord;
import com.agenticdev.sdlc.coding.persistence.CodingRunRepository;
import com.agenticdev.sdlc.coding.persistence.CodingRunStatus;
import com.agenticdev.sdlc.github.config.GitHubProperties;
import com.agenticdev.sdlc.github.persistence.PullRequestRecord;
import com.agenticdev.sdlc.github.persistence.PullRequestRepository;
import com.agenticdev.sdlc.github.pipeline.DiffApplier;
import com.agenticdev.sdlc.planning.domain.PlanResult;
import com.agenticdev.sdlc.planning.persistence.PlanRecord;
import com.agenticdev.sdlc.planning.persistence.PlanRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "app.github", name = "enabled", havingValue = "true")
public class PullRequestService {

    private static final Logger log = LoggerFactory.getLogger(PullRequestService.class);

    private final CodingRunRepository codingRunRepo;
    private final PlanRepository planRepo;
    private final PullRequestRepository prRepo;
    private final DiffApplier diffApplier;
    private final CodeownersResolver codeownersResolver;
    private final GitHubClient gitHubClient;
    private final GitHubProperties props;

    public PullRequestService(CodingRunRepository codingRunRepo,
                              PlanRepository planRepo,
                              PullRequestRepository prRepo,
                              DiffApplier diffApplier,
                              CodeownersResolver codeownersResolver,
                              GitHubClient gitHubClient,
                              GitHubProperties props) {
        this.codingRunRepo = codingRunRepo;
        this.planRepo = planRepo;
        this.prRepo = prRepo;
        this.diffApplier = diffApplier;
        this.codeownersResolver = codeownersResolver;
        this.gitHubClient = gitHubClient;
        this.props = props;
    }

    @PostConstruct
    void validate() {
        if (props.commit() == null || isBlank(props.commit().authorEmail())) {
            throw new IllegalStateException("app.github.commit.author-email is required");
        }
    }

    /** Synchronously validate + persist PENDING record; caller should then invoke {@link #executePr(UUID)} async. */
    public PullRequestRecord createRun(UUID codingRunId, String titleOverride, String bodyOverride,
                                       boolean draft, List<String> extraLabels, String webhookUrl) {
        CodingRunRecord run = codingRunRepo.findById(codingRunId)
                .orElseThrow(() -> new CodingRunNotFoundException(codingRunId));
        if (run.getStatus() != CodingRunStatus.COMPLETED) {
            throw new CodingRunNotCompletedException(codingRunId, run.getStatus().name());
        }
        if (run.getDiff() == null || run.getDiff().isBlank()) {
            throw new EmptyDiffException(codingRunId);
        }

        PlanResult plan = planRepo.findById(run.getPlanId()).map(PlanRecord::getPlan).orElse(null);
        String title = titleOverride != null ? titleOverride : defaultTitle(plan);
        String body = bodyOverride != null ? bodyOverride : renderBody(plan, run);
        List<String> labels = combineLabels(extraLabels);

        PullRequestRecord record = PullRequestRecord.pending(
                codingRunId, run.getRepoUrl(), run.getBaseRef(),
                title, body, draft, labels, webhookUrl);
        return prRepo.save(record);
    }

    @Async("codingTaskExecutor")
    public void executePr(UUID prId) {
        PullRequestRecord record = prRepo.findById(prId).orElse(null);
        if (record == null) {
            log.warn("PullRequestRecord {} not found; aborting async", prId);
            return;
        }
        CodingRunRecord run = codingRunRepo.findById(record.getCodingRunId()).orElse(null);
        if (run == null) {
            record.markFailed("coding_run_not_found", "coding run gone", 0L);
            prRepo.save(record);
            return;
        }

        Instant started = Instant.now();
        try {
            OwnerRepo or = parseOwnerRepo(record.getRepoUrl());
            String branch = branchFor(record.getCodingRunId());
            PlanResult plan = planRepo.findById(run.getPlanId()).map(PlanRecord::getPlan).orElse(null);

            String commitMessage = renderCommitMessage(plan, record.getCodingRunId(), run.getPlanId());

            DiffApplier.Result applied = diffApplier.apply(
                    record.getRepoUrl(), record.getBaseRef(), branch,
                    run.getDiff(), commitMessage,
                    props.commit().authorName(), props.commit().authorEmail(),
                    pushToken());
            record.markPushed(branch, applied.headSha());
            prRepo.save(record);

            List<String> reviewers = codeownersResolver.reviewersFor(
                    applied.codeownersContent(), applied.changedFiles());

            PrContext ctx = new PrContext(
                    or.owner(), or.repo(),
                    record.getBaseRef(), branch, applied.headSha(),
                    record.getTitle(), record.getBody(), record.isDraft(),
                    record.getLabels(), reviewers);
            GitHubClient.OpenedPr opened = gitHubClient.openPullRequest(ctx);

            if (record.getLabels() != null && !record.getLabels().isEmpty()) {
                gitHubClient.applyLabels(or.owner(), or.repo(), opened.number(), record.getLabels());
            }
            if (!reviewers.isEmpty()) {
                gitHubClient.requestReviewers(or.owner(), or.repo(), opened.number(), reviewers);
            }

            record.markOpen(opened.number(), opened.htmlUrl(), reviewers);
            record.setDurationMs(Duration.between(started, Instant.now()).toMillis());
            prRepo.save(record);

        } catch (PullRequestException e) {
            long d = Duration.between(started, Instant.now()).toMillis();
            record.markFailed(e.code(), safeMessage(e), d);
            prRepo.save(record);
        } catch (RuntimeException e) {
            long d = Duration.between(started, Instant.now()).toMillis();
            log.error("Unexpected PR failure for {}", prId, e);
            record.markFailed("internal_error", safeMessage(e), d);
            prRepo.save(record);
        }
    }

    public PullRequestRecord markReady(UUID prId) {
        PullRequestRecord r = prRepo.findById(prId).orElseThrow();
        OwnerRepo or = parseOwnerRepo(r.getRepoUrl());
        gitHubClient.markReady(or.owner(), or.repo(), r.getPrNumber());
        r.markReady();
        return prRepo.save(r);
    }

    public PullRequestRecord merge(UUID prId, MergeStrategy strategy) {
        PullRequestRecord r = prRepo.findById(prId).orElseThrow();
        OwnerRepo or = parseOwnerRepo(r.getRepoUrl());
        String sha = gitHubClient.merge(or.owner(), or.repo(), r.getPrNumber(), strategy);
        r.markMerged(sha, strategy);
        return prRepo.save(r);
    }

    public PullRequestRecord postComment(UUID prId, String body) {
        PullRequestRecord r = prRepo.findById(prId).orElseThrow();
        OwnerRepo or = parseOwnerRepo(r.getRepoUrl());
        gitHubClient.postComment(or.owner(), or.repo(), r.getPrNumber(), body);
        return r;
    }

    // --- helpers ---

    private String pushToken() {
        if (props.pat() != null && !isBlank(props.pat().token())) return props.pat().token();
        // For App-auth pushes you'd mint an installation token here; PAT fallback is fine for M3.
        return "";
    }

    private String defaultTitle(PlanResult plan) {
        String summary = plan != null ? plan.summary() : "agentic change";
        String trimmed = summary.length() > 72 ? summary.substring(0, 69) + "..." : summary;
        return "feat: " + trimmed;
    }

    private String renderBody(PlanResult plan, CodingRunRecord run) {
        String template = props.prBodyTemplate();
        if (template == null) template = "Generated by Agentic SDLC\n\nCoding run: {{codingRunId}}";
        return template
                .replace("{{summary}}", plan != null ? plan.summary() : "")
                .replace("{{planMarkdown}}", plan != null ? nullSafe(plan.markdown()) : "")
                .replace("{{planId}}", run.getPlanId() != null ? run.getPlanId().toString() : "")
                .replace("{{codingRunId}}", run.getId().toString())
                .replace("{{filesChanged}}", run.getFilesChanged() != null ? run.getFilesChanged().toString() : "0")
                .replace("{{testsPassed}}", String.valueOf(Boolean.TRUE.equals(run.getTestsPassed())));
    }

    private String renderCommitMessage(PlanResult plan, UUID codingRunId, UUID planId) {
        String template = props.commit().messageTemplate();
        if (template == null) template = "feat: {{summary}}\n\nCoding run: {{codingRunId}}";
        return template
                .replace("{{summary}}", plan != null ? plan.summary() : "agentic change")
                .replace("{{planId}}", planId != null ? planId.toString() : "")
                .replace("{{codingRunId}}", codingRunId.toString())
                .replace("{{authorName}}", nullSafe(props.commit().authorName()))
                .replace("{{authorEmail}}", nullSafe(props.commit().authorEmail()));
    }

    private String branchFor(UUID codingRunId) {
        String prefix = props.branchPrefix() == null ? "agentic/" : props.branchPrefix();
        return prefix + codingRunId;
    }

    private List<String> combineLabels(List<String> extra) {
        List<String> out = new ArrayList<>();
        if (props.defaultLabels() != null) out.addAll(props.defaultLabels());
        if (extra != null) for (String l : extra) if (!out.contains(l)) out.add(l);
        return out;
    }

    static OwnerRepo parseOwnerRepo(String repoUrl) {
        String u = repoUrl;
        if (u.endsWith(".git")) u = u.substring(0, u.length() - 4);
        if (u.startsWith("git@")) {
            int colon = u.indexOf(':');
            if (colon > 0) u = "https://" + u.substring(4, colon) + "/" + u.substring(colon + 1);
        }
        int slash = u.lastIndexOf('/');
        int prevSlash = u.lastIndexOf('/', slash - 1);
        if (prevSlash < 0 || slash < 0) {
            throw new IllegalArgumentException("Cannot parse owner/repo from " + repoUrl);
        }
        return new OwnerRepo(u.substring(prevSlash + 1, slash), u.substring(slash + 1));
    }

    private static String nullSafe(String s) { return s == null ? "" : s; }
    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static String safeMessage(Throwable e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    record OwnerRepo(String owner, String repo) {}
}
