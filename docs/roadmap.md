# Roadmap

## Completed

### M1 — Planning Agent
Jira ticket or free-text prompt → structured implementation plan with tasks, files to touch, risks, and open questions. Multi-provider LLM support (OpenAI, Anthropic, Bedrock, Ollama, LM Studio).

### M2 — Coder Agent
Consumes a plan, clones a repository into an ephemeral Docker container, runs an autonomous tool-use loop (read/write files, run commands), produces a unified diff. Budget enforcement on tokens, iterations, time, and container resources.

### M3 — GitHub PR Integration
Takes a coding run's diff, applies it to a fresh clone, pushes a branch, opens a pull request via GitHub API. Supports PAT and GitHub App authentication, CODEOWNERS-based reviewer assignment, labels, merge (squash/rebase/merge), comments, and draft→ready transitions. Spring Retry on all GitHub API calls.

### Frontend Dashboard
React 19 admin panel with glassmorphism design. Dashboard with stat cards, weekly activity chart, token usage chart. Detail views for plans (tasks, risks, files), coding runs (iterations, tokens, GitHub-style diff viewer), and pull requests (labels, reviewers, merge controls, comments). All data from live API.

---

## Planned Enhancements

### M3.1 — Jira Ticket Transition on PR Merge
Automatically transition a Jira ticket (e.g. "In Progress" → "In Review" → "Done") when the associated PR is merged. Requires a Jira write adapter (currently read-only) and a GitHub webhook listener to detect merge events.

**Effort:** 1-2 days
**Dependencies:** GitHub webhook infrastructure (inbound), Jira write API

### M4 — Review Agent
An autonomous agent that responds to PR review comments. When a reviewer requests changes, the agent reads the comments, runs a new coding iteration to address them, pushes fix commits, and replies on the PR thread. Comparable scope to M2.

**Effort:** 3-5 days
**Dependencies:** GitHub webhook infrastructure (inbound), new LLM agent with conversation memory

### M5 — Documentation Agent
Automatically updates Confluence pages when code changes land. Reads the diff and plan, identifies documentation that needs updating, and posts/updates Confluence pages via the REST API.

**Effort:** 2-3 days
**Dependencies:** Confluence REST API adapter (new external system)

### RAG — Retrieval-Augmented Generation for Coding Agent
Preload relevant code context (similar files, related tests, team conventions) via vector search before the coding agent's tool-use loop starts. Improves plan quality and reduces iterations.

**Effort:** 3-5 days
**Components:** Embedding model integration, vector store (Qdrant or PGVector), document ingestion pipeline, context injection into agent system prompt

### CI/CD Pipeline
GitHub Actions workflow: build → test → Docker image → push to ECR. Branch protection rules, automated test reporting, deployment to staging.

**Effort:** 2-4 hours

### AWS Deployment
ECS Fargate + RDS PostgreSQL + ECR. Infrastructure as Code via CloudFormation or CDK. Docker-in-Docker sidecar for the coding sandbox. ADOT collector for traces.

**Effort:** 1-2 days

### API Authentication
API key authentication as a minimum, OAuth2 resource server for production. Rate limiting per API key. Currently the API runs on a trusted-network assumption.

**Effort:** 0.5-1 day

### Evaluation & A/B Testing
Compare plan quality across different models by replaying the same input through different providers. Score structured outputs on completeness, specificity, and accuracy. The data is already persisted — needs a comparison endpoint and scoring heuristics.

**Effort:** 1-2 days

### Streaming Progress Updates
Server-Sent Events (SSE) or WebSocket endpoint for real-time coding run progress. Show which tool the agent is currently calling, iteration count, and partial output in the dashboard.

**Effort:** 1-2 days

### Confluence Integration
Read adapter for pulling specification documents and team conventions as context for the planning and coding agents. Write adapter for M5 (documentation agent).

**Effort:** 1 day (read), 1 day (write)

---

## Architecture Improvements

| Area | Current | Target |
|------|---------|--------|
| Async execution | `@Async` + ThreadPoolTaskExecutor | Outbox pattern with polling worker (crash-safe) |
| Container management | One-off `docker create/exec/rm` | Container pool with warm standby for faster provisioning |
| Observability | Debug exporter | Jaeger/Tempo for traces, Grafana dashboards |
| Database | Single Postgres | Connection pooling (PgBouncer), read replicas for dashboard queries |
| Frontend | Client-side polling (3s) | SSE/WebSocket for real-time updates |
