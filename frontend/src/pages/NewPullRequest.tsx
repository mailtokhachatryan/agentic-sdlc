import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { api } from '@/api/client'

export default function NewPullRequest() {
  const nav = useNavigate()
  const [params] = useSearchParams()
  const [codingRunId, setCodingRunId] = useState(params.get('codingRunId') || '')
  const [title, setTitle] = useState('')
  const [body, setBody] = useState('')
  const [draft, setDraft] = useState(false)

  const mutation = useMutation({
    mutationFn: (payload: Record<string, unknown>) => api.pullRequests.create(payload),
    onSuccess: (data) => nav(`/pull-requests/${data.id}`)
  })

  const submit = () => {
    const payload: Record<string, unknown> = { codingRunId }
    if (title) payload.title = title
    if (body) payload.body = body
    if (draft) payload.draft = true
    mutation.mutate(payload)
  }

  return (
    <div className="fade-in" style={{ maxWidth: 600 }}>
      <h1 className="page-title" style={{ marginBottom: 24 }}><span className="accent">{'>'}</span> New Pull Request</h1>

      <div className="field-group">
        <label className="label">Coding Run ID</label>
        <input value={codingRunId} onChange={e => setCodingRunId(e.target.value)} placeholder="UUID of a completed coding run" />
      </div>

      <div className="field-group">
        <label className="label">Title (optional)</label>
        <input value={title} onChange={e => setTitle(e.target.value)} placeholder="feat: add /health endpoint" />
      </div>

      <div className="field-group">
        <label className="label">Body (optional)</label>
        <textarea value={body} onChange={e => setBody(e.target.value)} rows={4} placeholder="PR description..." />
      </div>

      <div className="field-group">
        <label style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer' }}>
          <input type="checkbox" checked={draft} onChange={e => setDraft(e.target.checked)} style={{ width: 'auto' }} />
          <span className="label" style={{ marginBottom: 0 }}>Open as draft</span>
        </label>
      </div>

      <button className="btn btn-primary" onClick={submit} disabled={mutation.isPending}>
        {mutation.isPending ? 'Creating...' : 'Open Pull Request'}
      </button>

      {mutation.isError && (
        <div style={{ marginTop: 12, color: 'var(--status-failed)', fontFamily: 'var(--font-display)', fontSize: 12 }}>
          {(mutation.error as Error).message}
        </div>
      )}
    </div>
  )
}
