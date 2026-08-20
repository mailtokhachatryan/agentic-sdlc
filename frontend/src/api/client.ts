const BASE = '/api/v1'

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...init,
  })
  if (!res.ok) {
    const body = await res.json().catch(() => ({}))
    throw new Error(body.message || body.code || `HTTP ${res.status}`)
  }
  return res.json()
}

export interface Plan {
  id: string; status: string; provider: string; model: string
  inputType: string; prompt: string | null; jiraKey: string | null
  plan: PlanResult | null; createdAt: string; durationMs: number | null
  error: { code: string; message: string } | null
}

export interface PlanResult {
  summary: string; approach: string; markdown: string
  tasks: { title: string; description: string; estimate: string }[]
  filesToTouch: { path: string; change: string; reason: string }[]
  risks: { risk: string; mitigation: string }[]
  openQuestions: string[]
}

export interface PlanSummary {
  id: string; status: string; provider: string; model: string
  inputType: string; jiraKey: string | null; promptPreview: string | null
  createdAt: string
}

export interface CodingRun {
  id: string; status: string; planId: string; provider: string; model: string
  repoUrl: string | null; baseRef: string | null; diff: string | null
  filesChanged: number | null; iterationsUsed: number | null
  tokensUsed: number | null; testsPassed: boolean | null
  errorCode: string | null; errorMessage: string | null
  durationMs: number | null; timeoutReason: string | null
  autoOpenPr: boolean | null
  createdAt: string; completedAt: string | null
}

export interface CodingRunSummary {
  id: string; status: string; planId: string; provider: string; model: string
  filesChanged: number | null; testsPassed: boolean | null
  durationMs: number | null; createdAt: string
}

export interface PullRequest {
  id: string; status: string; codingRunId: string
  repoUrl: string; baseRef: string; headBranch: string | null
  headSha: string | null; prNumber: number | null; prUrl: string | null
  title: string | null; body: string | null; draft: boolean
  labels: string[] | null; reviewers: string[] | null
  mergeStrategy: string | null; mergedSha: string | null
  errorCode: string | null; errorMessage: string | null
  durationMs: number | null; createdAt: string
  openedAt: string | null; mergedAt: string | null
}

export interface PullRequestSummary {
  id: string; status: string; codingRunId: string; repoUrl: string
  prNumber: number | null; prUrl: string | null; title: string | null
  createdAt: string
}

export interface Page<T> {
  content: T[]; totalPages: number; totalElements: number
  number: number; size: number
}

export const api = {
  plans: {
    list: (page = 0, size = 20) =>
      request<Page<PlanSummary>>(`/plans?page=${page}&size=${size}`),
    get: (id: string) => request<Plan>(`/plans/${id}`),
    create: (body: Record<string, unknown>) =>
      request<Plan>('/plans', { method: 'POST', body: JSON.stringify(body) }),
  },
  codingRuns: {
    list: (page = 0, size = 20) =>
      request<Page<CodingRunSummary>>(`/coding-runs?page=${page}&size=${size}`),
    get: (id: string) => request<CodingRun>(`/coding-runs/${id}`),
    getDiff: (id: string) =>
      fetch(`${BASE}/coding-runs/${id}/diff`).then(r => r.text()),
    create: (body: Record<string, unknown>) =>
      request<CodingRun>('/coding-runs', { method: 'POST', body: JSON.stringify(body) }),
  },
  pullRequests: {
    list: (page = 0, size = 20) =>
      request<Page<PullRequestSummary>>(`/pull-requests?page=${page}&size=${size}`),
    get: (id: string) => request<PullRequest>(`/pull-requests/${id}`),
    create: (body: Record<string, unknown>) =>
      request<PullRequest>('/pull-requests', { method: 'POST', body: JSON.stringify(body) }),
    markReady: (id: string) =>
      request<PullRequest>(`/pull-requests/${id}/ready`, { method: 'POST' }),
    merge: (id: string, strategy: string) =>
      request<PullRequest>(`/pull-requests/${id}/merge`, {
        method: 'POST', body: JSON.stringify({ strategy })
      }),
    comment: (id: string, body: string) =>
      request<PullRequest>(`/pull-requests/${id}/comments`, {
        method: 'POST', body: JSON.stringify({ body })
      }),
  }
}
