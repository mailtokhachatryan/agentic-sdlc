# Architecture

## Overview

Agentic SDLC is a Spring Boot 4 / Java 21 platform that autonomously executes software development tasks end-to-end: from reading a Jira ticket to planning, coding in a sandboxed container, and opening a GitHub pull request.

```
┌──────────────────────────────────────────────────────────────────────────┐
│                        React Dashboard (:3000)                           │
│   Plans • Coding Runs • Pull Requests • Charts • GitHub-style Diff       │
└──────────────────────┬───────────────────────────────────────────────────┘
                       │ /api/v1/*  (nginx reverse proxy)
                       ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                    Spring Boot 4 API (:8080)                             │
│                                                                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐                  │
│  │  planning/   │  │  coding/    │  │  github/          │                │
│  │             │  │             │  │                   │                  │
│  │ PlansCtrl   │  │ CodingCtrl  │  │ PullRequestsCtrl │                  │
│  │ PlanService │  │ CodingServ  │  │ PullRequestServ  │                  │
│  │ PlanAgent   │  │ CodingAgent │  │ GitHubClient     │                  │
│  │ (AiServices)│  │ (@Tool loop)│  │ DiffApplier      │                  │
│  └──────┬──────┘  └──────┬──────┘  └───────┬──────────┘                  │
│         │                │                  │                             │
│  ┌──────┴──────────────┬─┴──────────────────┴─────────┐                  │
│  │              Shared Infrastructure                  │                  │
│  │  llm/ (ChatModelFactory, 5 providers)               │                  │
│  │  jira/ (JiraClient, RestClient)                     │                  │
│  │  observability/ (OTel spans, Micrometer metrics)    │                  │
│  └─────────────────────────────────────────────────────┘                  │
└──────┬────────────────────┬────────────────────┬─────────────────────────┘
       │                    │                    │
       ▼                    ▼                    ▼
  ┌─────────┐     ┌──────────────┐      ┌──────────────┐
  │Postgres │     │ Docker Engine│      │  GitHub API   │
  │ (Flyway │     │ (sandboxed   │      │  (PAT / App)  │
  │  4 migr)│     │  containers) │      │  + Retry      │
  └─────────┘     └──────────────┘      └──────────────┘
       │
       ▼
  ┌──────────────┐
  │ OTel Collector│
  │ (traces +     │
  │  metrics)     │
  └──────────────┘
```

## Feature Slices

The codebase follows **vertical slice architecture** — each feature is a self-contained package:

| Package | Milestone | Purpose |
|---------|-----------|---------|
| `planning/` | M1 | Jira ticket or prompt → structured implementation plan via LLM |
| `coding/` | M2 | Plan → code changes via agentic tool-use loop in Docker sandbox |
| `github/` | M3 | Diff → branch + commit + PR on GitHub with CODEOWNERS reviewers |
| `llm/` | shared | Multi-provider ChatModel factory (OpenAI, Anthropic, Bedrock, Ollama, LM Studio) |
| `jira/` | shared | Jira REST client for ticket fetching |
| `observability/` | shared | Micrometer metrics + OpenTelemetry tracing |

## Key Design Decisions

### Ports & Adapters at Every Boundary

Every external system interaction is behind an interface:

- `PlanningAgent` → `LangChain4jPlanningAgent` (LLM structured output)
- `CodingAgent` → `LangChain4jCodingAgent` (LLM tool-use loop)
- `CodeExecutor` → `DockerCodeExecutor` (sandboxed container)
- `GitHubClient` → `KohsukeGitHubClient` (PAT or App auth)
- `DiffApplier` → `DockerDiffApplier` (ephemeral git container)
- `JiraClient` → `RestJiraClient`
- `ChatModelFactory` → `DefaultChatModelFactory`

Tests mock the interface; implementations can be swapped without touching services.

### Per-Request Provider Selection

The caller picks the LLM provider in the request body. `ChatModelFactory` holds a `Map<Provider, ChatModel>` assembled at startup by `LlmAutoConfiguration`. Five providers supported: OpenAI, Anthropic, AWS Bedrock, Ollama, LM Studio.

### Sandboxed Code Execution (M2)

Every coding run spins up an **ephemeral Docker container** with:
- `--network none` (disconnected after clone)
- Configurable memory + CPU limits
- Disposable filesystem
- Tool calls executed via `docker exec`

The LLM has four tools: `readFile`, `writeFile`, `listFiles`, `runCommand`. Budget enforcement (tokens, iterations, time) checked after every tool call.

### Async with Webhook (M2, M3)

Long-running operations return `202 Accepted` immediately. A bounded `ThreadPoolTaskExecutor` runs the work. Callers either poll `GET /{id}` or provide a `webhookUrl` for push notification on completion.

### PENDING Row First

Every request writes a `PENDING` database row before starting work. If the server crashes mid-run, the row remains auditable. Terminal state (`COMPLETED`/`FAILED`/`TIMED_OUT`) written in a separate transaction after the work finishes.

## Data Flow: Jira → PR

```
1. POST /api/v1/plans { inputType: "JIRA", jiraKey: "AS24-1234" }
   → JiraClient.fetch() → LLM structured output → PlanResult → DB row

2. POST /api/v1/coding-runs { planId: "...", autoOpenPr: true }
   → 202 Accepted
   → async: Docker create → git clone → LLM tool loop → git diff → DB row
   → on COMPLETED + testsPassed: auto-triggers step 3

3. PullRequestAutoOpener.openFor(run)
   → Docker create → git apply → git push → GitHub API: create PR
   → CODEOWNERS parse → request reviewers → apply labels → DB row
```

## Database Schema

Four Flyway migrations:

| Migration | Tables |
|-----------|--------|
| V1 | `plans` (JSONB plan_json, jira_snapshot) |
| V2 | `coding_runs` (FK to plans, diff TEXT, budget tracking) |
| V3 | `pull_requests` (FK to coding_runs, JSONB labels/reviewers) + extends coding_runs with auto_open_pr |
| V4 | Demo seed data (5 plans, 5 runs, 4 PRs covering all statuses) |

## Observability

Every operation emits OpenTelemetry spans + Micrometer metrics:

```
POST /api/v1/plans → planning.create → jira.fetch → llm.plan
POST /api/v1/coding-runs → coding.run → coding.provision → coding.iterate[N] → coding.extract-diff
POST /api/v1/pull-requests → pr.create → pr.apply-diff → pr.open → pr.apply-labels
```

All error responses carry `{ code, message, traceId }` — one click from user report to trace.

## Testing Strategy

| Tier | Count | What |
|------|-------|------|
| Unit | 87 | Services, records, validators, controllers (@WebMvcTest), budget enforcement |
| Integration | 3 (deferred) | Full HTTP + Testcontainers Postgres + WireMock webhooks |
| Container | deferred | DockerCodeExecutor lifecycle against real Docker daemon |
