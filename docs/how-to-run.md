# How to Run

## Prerequisites

- **Docker** (Docker Desktop, Rancher Desktop, or OrbStack)
- **Java 21** (Temurin recommended) — only needed for running tests outside Docker
- **Node.js 22+** — only needed for frontend development outside Docker
- **One LLM provider:** LM Studio (local), OpenAI API key, Anthropic API key, AWS Bedrock credentials, or Ollama

## Quick Start (Docker Compose)

```bash
# 1. Clone and enter the project
git clone <repo-url>
cd spring-llm

# 2. Copy environment config
cp .env.example .env
# Edit .env — enable at least one LLM provider

# 3. Start everything
docker compose up --build
```

**Services started:**

| Service | URL | Purpose |
|---------|-----|---------|
| Frontend | http://localhost:3000 | React dashboard |
| Backend API | http://localhost:8088 | Spring Boot REST API |
| Swagger UI | http://localhost:8088/swagger-ui.html | Interactive API docs |
| Postgres | localhost:5432 | Database (agentic/agentic/agentic) |
| OTel Collector | localhost:4317 (gRPC), 4318 (HTTP) | Traces + metrics |

## LLM Provider Setup

### LM Studio (recommended for local dev)

1. Download and install [LM Studio](https://lmstudio.ai)
2. Load a model (e.g. `qwen2.5-7b-instruct`, `phi-3-mini`)
3. Start the server (Developer → Local Server → Start)
4. Enable "Serve on Local Network" for Docker access

```env
LMSTUDIO_ENABLED=true
LMSTUDIO_BASE_URL=http://192.168.64.1:1234/v1   # Adjust IP for your setup
LMSTUDIO_DEFAULT_MODEL=qwen2.5-7b-instruct
LLM_DEFAULT_PROVIDER=LMSTUDIO
```

> **Note:** For Docker on macOS with Rancher Desktop, use your Mac's LAN IP instead of `host.docker.internal`. Find it with `ifconfig | grep "inet "`.

### OpenAI

```env
OPENAI_ENABLED=true
OPENAI_API_KEY=sk-...
LLM_DEFAULT_PROVIDER=OPENAI
```

### Anthropic

```env
ANTHROPIC_ENABLED=true
ANTHROPIC_API_KEY=sk-ant-...
LLM_DEFAULT_PROVIDER=ANTHROPIC
```

### AWS Bedrock

```env
BEDROCK_ENABLED=true
AWS_REGION=eu-central-1
AWS_ACCESS_KEY_ID=...
AWS_SECRET_ACCESS_KEY=...
LLM_DEFAULT_PROVIDER=BEDROCK
```

### Ollama

```env
OLLAMA_ENABLED=true
OLLAMA_BASE_URL=http://host.docker.internal:11434
LLM_DEFAULT_PROVIDER=OLLAMA
```

## GitHub Integration (M3)

To enable PR creation:

```env
GITHUB_ENABLED=true
GITHUB_AUTH_MODE=pat
GITHUB_TOKEN=ghp_...
```

For GitHub App auth:
```env
GITHUB_AUTH_MODE=app
GITHUB_APP_ID=123456
GITHUB_APP_INSTALLATION_ID=789
GITHUB_APP_PRIVATE_KEY_PEM=/path/to/private-key.pem
```

## Try It

### Create a plan (prompt mode)

```bash
curl -s localhost:8088/api/v1/plans \
  -H 'Content-Type: application/json' \
  -d '{
    "inputType": "PROMPT",
    "prompt": "Add a rate limiter to /search allowing 10 req/min per API key."
  }' | jq
```

### Create a plan (Jira mode)

```bash
curl -s localhost:8088/api/v1/plans \
  -H 'Content-Type: application/json' \
  -d '{
    "inputType": "JIRA",
    "jiraKey": "AS24-1234",
    "provider": "ANTHROPIC"
  }' | jq
```

### Start a coding run

```bash
curl -s localhost:8088/api/v1/coding-runs \
  -H 'Content-Type: application/json' \
  -d '{
    "planId": "<plan-id-from-above>",
    "repoUrl": "https://github.com/org/repo.git",
    "autoOpenPr": true
  }' | jq
```

### Poll for results

```bash
curl -s localhost:8088/api/v1/coding-runs/<run-id> | jq '.status, .testsPassed, .filesChanged'
```

### List everything

```bash
curl -s localhost:8088/api/v1/plans | jq '.content[].status'
curl -s localhost:8088/api/v1/coding-runs | jq '.content[].status'
curl -s localhost:8088/api/v1/pull-requests | jq '.content[].status'
```

## Running Tests

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)

# Unit tests (no Docker required) — 87 tests
./mvnw test -Dtest='!PlanRepositoryTest,!PlansControllerIT,!CodingRunsControllerIT'

# All tests (requires Docker socket for Testcontainers)
./mvnw test
```

## Frontend Development

```bash
cd frontend
npm install
npm run dev    # http://localhost:3000 with hot reload
               # Proxies /api/* to localhost:8080
```

## Configuration Reference

All config is in `src/main/resources/application.yml`, overridable via environment variables. See `.env.example` for the full list.

Key settings:

| Variable | Default | Purpose |
|----------|---------|---------|
| `LLM_DEFAULT_PROVIDER` | `LMSTUDIO` | Default provider when request omits `provider` |
| `LLM_REQUEST_TIMEOUT` | `10m` | HTTP timeout for LLM calls |
| `CODING_MAX_ITERATIONS` | `20` | Max tool-use loop iterations per coding run |
| `CODING_MAX_DURATION` | `15m` | Wall-clock timeout per coding run |
| `CODING_CONTAINER_IMAGE` | `eclipse-temurin:21-jdk` | Docker image for coding sandbox |
| `CODING_CONTAINER_MEMORY` | `2g` | Memory limit for sandbox container |
| `GITHUB_BRANCH_PREFIX` | `agentic/` | Branch naming for auto-created PRs |
