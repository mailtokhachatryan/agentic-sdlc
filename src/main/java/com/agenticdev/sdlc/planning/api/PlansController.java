package com.agenticdev.sdlc.planning.api;

import com.agenticdev.sdlc.llm.Provider;
import com.agenticdev.sdlc.llm.config.LlmProperties;
import com.agenticdev.sdlc.planning.api.dto.CreatePlanRequest;
import com.agenticdev.sdlc.planning.api.dto.PlanResponse;
import com.agenticdev.sdlc.planning.api.dto.PlanSummaryResponse;
import com.agenticdev.sdlc.planning.domain.PlanningService;
import com.agenticdev.sdlc.planning.persistence.PlanRecord;
import com.agenticdev.sdlc.planning.persistence.PlanRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/plans")
public class PlansController {

    private final PlanningService service;
    private final PlanRepository repo;
    private final LlmProperties llmProperties;

    public PlansController(PlanningService service, PlanRepository repo, LlmProperties llmProperties) {
        this.service = service;
        this.repo = repo;
        this.llmProperties = llmProperties;
    }

    @PostMapping
    public PlanResponse create(@Valid @RequestBody CreatePlanRequest req) {
        req.validateConsistency();
        Provider provider = req.provider() != null ? req.provider() : llmProperties.defaultProvider();
        PlanRecord record = service.createPlan(
                provider, req.model(), req.inputType(), req.prompt(), req.jiraKey());
        return PlanResponse.from(record);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlanResponse> get(@PathVariable UUID id) {
        return repo.findById(id)
                .map(r -> ResponseEntity.ok(PlanResponse.from(r)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public Page<PlanSummaryResponse> list(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        return repo.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(PlanSummaryResponse::from);
    }
}
