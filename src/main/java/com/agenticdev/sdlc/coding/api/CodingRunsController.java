package com.agenticdev.sdlc.coding.api;

import com.agenticdev.sdlc.coding.api.dto.CodingRunResponse;
import com.agenticdev.sdlc.coding.api.dto.CodingRunSummaryResponse;
import com.agenticdev.sdlc.coding.api.dto.CreateCodingRunRequest;
import com.agenticdev.sdlc.coding.domain.CodingService;
import com.agenticdev.sdlc.coding.persistence.CodingRunRecord;
import com.agenticdev.sdlc.coding.persistence.CodingRunRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
@RequestMapping("/api/v1/coding-runs")
public class CodingRunsController {

    private final CodingService service;
    private final CodingRunRepository repo;

    public CodingRunsController(CodingService service, CodingRunRepository repo) {
        this.service = service;
        this.repo = repo;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CodingRunResponse create(@Valid @RequestBody CreateCodingRunRequest req) {
        CodingRunRecord record = service.createRun(
                req.planId(), req.repoUrl(), req.baseRef(),
                req.provider(), req.model(), req.webhookUrl(),
                Boolean.TRUE.equals(req.autoOpenPr()),
                req.prTitle(), req.prBody());
        service.executeCodingRun(record.getId());
        return CodingRunResponse.from(record);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CodingRunResponse> get(@PathVariable UUID id) {
        return repo.findById(id)
                .map(r -> ResponseEntity.ok(CodingRunResponse.from(r)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/diff")
    public ResponseEntity<String> getDiff(@PathVariable UUID id) {
        return repo.findById(id)
                .map(r -> {
                    String diff = r.getDiff();
                    return ResponseEntity.ok()
                            .contentType(MediaType.TEXT_PLAIN)
                            .body(diff == null ? "" : diff);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public Page<CodingRunSummaryResponse> list(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return repo.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(CodingRunSummaryResponse::from);
    }
}
