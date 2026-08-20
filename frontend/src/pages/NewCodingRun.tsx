import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { api } from '@/api/client'

export default function NewCodingRun() {
  const nav = useNavigate()
  const [params] = useSearchParams()
  const [planId, setPlanId] = useState(params.get('planId') || '')
  const [repoUrl, setRepoUrl] = useState('')
  const [baseRef, setBaseRef] = useState('main')
  const [provider, setProvider] = useState('')
  const [autoOpenPr, setAutoOpenPr] = useState(false)

  const mutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => api.codingRuns.create(body),
    onSuccess: (data) => nav(`/coding-runs/${data.id}`)
  })

  const submit = () => {
    const body: Record<string, unknown> = { planId }
    if (repoUrl) body.repoUrl = repoUrl
    if (baseRef) body.baseRef = baseRef
    if (provider) body.provider = provider
    if (autoOpenPr) body.autoOpenPr = true
    mutation.mutate(body)
  }

  return (
    <div className="fade-in" style={{ maxWidth: 600 }}>
      <h1 className="page-title" style={{ marginBottom: 24 }}><span className="accent">{'>'}</span> New Coding Run</h1>

      <div className="field-group">
        <label className="label">Plan ID</label>
        <input value={planId} onChange={e => setPlanId(e.target.value)} placeholder="UUID of a completed plan" />
      </div>

      <div className="grid grid-2">
        <div className="field-group">
          <label className="label">Repo URL (optional)</label>
          <input value={repoUrl} onChange={e => setRepoUrl(e.target.value)} placeholder="https://github.com/org/repo.git" />
        </div>
        <div className="field-group">
          <label className="label">Base Ref</label>
          <input value={baseRef} onChange={e => setBaseRef(e.target.value)} />
        </div>
      </div>

      <div className="field-group">
        <label className="label">Provider (optional)</label>
        <select value={provider} onChange={e => setProvider(e.target.value)}>
          <option value="">Default</option>
          <option value="LMSTUDIO">LM Studio</option>
          <option value="OPENAI">OpenAI</option>
          <option value="ANTHROPIC">Anthropic</option>
          <option value="BEDROCK">Bedrock</option>
          <option value="OLLAMA">Ollama</option>
        </select>
      </div>

      <div className="field-group">
        <label style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer' }}>
          <input type="checkbox" checked={autoOpenPr} onChange={e => setAutoOpenPr(e.target.checked)} style={{ width: 'auto' }} />
          <span className="label" style={{ marginBottom: 0 }}>Auto-open PR on success</span>
        </label>
      </div>

      <button className="btn btn-primary" onClick={submit} disabled={mutation.isPending}>
        {mutation.isPending ? 'Starting...' : 'Start Coding Run'}
      </button>

      {mutation.isError && (
        <div style={{ marginTop: 12, color: 'var(--status-failed)', fontFamily: 'var(--font-display)', fontSize: 12 }}>
          {(mutation.error as Error).message}
        </div>
      )}
    </div>
  )
}
