package com.agenticdev.sdlc.github.api;

import com.agenticdev.sdlc.github.api.dto.CreatePullRequestRequest;
import com.agenticdev.sdlc.github.api.dto.MergePullRequestRequest;
import com.agenticdev.sdlc.github.api.dto.PostCommentRequest;
import com.agenticdev.sdlc.github.api.dto.PullRequestResponse;
import com.agenticdev.sdlc.github.api.dto.PullRequestSummaryResponse;
import com.agenticdev.sdlc.github.domain.PullRequestService;
import com.agenticdev.sdlc.github.persistence.PullRequestRecord;
import com.agenticdev.sdlc.github.persistence.PullRequestRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pull-requests")
public class PullRequestsController {

    private final ObjectProvider<PullRequestService> serviceProvider;
    private final PullRequestRepository repo;

    public PullRequestsController(ObjectProvider<PullRequestService> serviceProvider, PullRequestRepository repo) {
        this.serviceProvider = serviceProvider;
        this.repo = repo;
    }

    private PullRequestService requireService() {
        PullRequestService svc = serviceProvider.getIfAvailable();
        if (svc == null) {
            throw new IllegalStateException("GitHub integration is not enabled (app.github.enabled=false)");
        }
        return svc;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public PullRequestResponse create(@Valid @RequestBody CreatePullRequestRequest req) {
        PullRequestService svc = requireService();
        PullRequestRecord record = svc.createRun(
                req.codingRunId(), req.title(), req.body(),
                Boolean.TRUE.equals(req.draft()), req.labels(), req.webhookUrl());
        svc.executePr(record.getId());
        return PullRequestResponse.from(record);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PullRequestResponse> get(@PathVariable UUID id) {
        return repo.findById(id)
                .map(r -> ResponseEntity.ok(PullRequestResponse.from(r)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public Page<PullRequestSummaryResponse> list(@RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        return repo.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(PullRequestSummaryResponse::from);
    }

    @PostMapping("/{id}/ready")
    public PullRequestResponse markReady(@PathVariable UUID id) {
        return PullRequestResponse.from(requireService().markReady(id));
    }

    @PostMapping("/{id}/merge")
    public PullRequestResponse merge(@PathVariable UUID id,
                                     @Valid @RequestBody MergePullRequestRequest req) {
        return PullRequestResponse.from(requireService().merge(id, req.strategy()));
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public PullRequestResponse comment(@PathVariable UUID id,
                                       @Valid @RequestBody PostCommentRequest req) {
        return PullRequestResponse.from(requireService().postComment(id, req.body()));
    }
}
