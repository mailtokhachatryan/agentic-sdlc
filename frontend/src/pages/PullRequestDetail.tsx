import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useParams, Link } from 'react-router-dom'
import { ArrowLeft, ExternalLink, GitMerge, MessageSquare, Eye } from 'lucide-react'
import { useState } from 'react'
import { api } from '@/api/client'
import StatusBadge from '@/components/StatusBadge'

export default function PullRequestDetail() {
  const { id } = useParams<{ id: string }>()
  const qc = useQueryClient()
  const { data: pr, isLoading } = useQuery({ queryKey: ['pull-request', id], queryFn: () => api.pullRequests.get(id!) })
  const [comment, setComment] = useState('')
  const [mergeStrategy, setMergeStrategy] = useState('SQUASH')

  const markReady = useMutation({
    mutationFn: () => api.pullRequests.markReady(id!),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['pull-request', id] })
  })

  const merge = useMutation({
    mutationFn: () => api.pullRequests.merge(id!, mergeStrategy),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['pull-request', id] })
  })

  const postComment = useMutation({
    mutationFn: () => api.pullRequests.comment(id!, comment),
    onSuccess: () => { setComment(''); qc.invalidateQueries({ queryKey: ['pull-request', id] }) }
  })

  if (isLoading) return <div className="loading"><div className="spinner" /></div>
  if (!pr) return <div className="empty-state">PR not found</div>

  return (
    <div className="fade-in">
      <div className="page-header">
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <Link to="/pull-requests" className="btn btn-sm"><ArrowLeft size={14} /></Link>
          <h1 className="page-title">PR <span className="mono" style={{ color: 'var(--text-muted)' }}>{pr.prNumber ? `#${pr.prNumber}` : pr.id.slice(0, 8)}</span></h1>
        </div>
        {pr.prUrl && (
          <a href={pr.prUrl} target="_blank" rel="noreferrer" className="btn">
            <ExternalLink size={14} /> View on GitHub
          </a>
        )}
      </div>

      <div className="grid grid-4" style={{ marginBottom: 24 }}>
        <div className="kv"><span className="kv-label">Status</span><StatusBadge status={pr.status} /></div>
        <div className="kv"><span className="kv-label">Branch</span><span className="kv-value mono">{pr.headBranch ?? '—'}</span></div>
        <div className="kv"><span className="kv-label">Base</span><span className="kv-value mono">{pr.baseRef}</span></div>
        <div className="kv"><span className="kv-label">Duration</span><span className="kv-value mono">{pr.durationMs ? `${(pr.durationMs / 1000).toFixed(1)}s` : '—'}</span></div>
      </div>

      {pr.title && (
        <div className="card" style={{ marginBottom: 16 }}>
          <span className="label">Title</span>
          <div style={{ fontSize: 15, fontWeight: 600 }}>{pr.title}</div>
        </div>
      )}

      {pr.labels && pr.labels.length > 0 && (
        <div style={{ display: 'flex', gap: 6, marginBottom: 16, flexWrap: 'wrap' }}>
          {pr.labels.map(l => (
            <span key={l} className="status-badge" style={{ background: 'var(--bg-tertiary)', color: 'var(--text-secondary)', border: '1px solid var(--border)', fontSize: 10 }}>{l}</span>
          ))}
        </div>
      )}

      {pr.reviewers && pr.reviewers.length > 0 && (
        <div className="card" style={{ marginBottom: 16 }}>
          <span className="label">Reviewers</span>
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
            {pr.reviewers.map(r => <span key={r} className="mono" style={{ fontSize: 12, color: 'var(--accent)' }}>@{r}</span>)}
          </div>
        </div>
      )}

      {pr.errorCode && (
        <div className="card" style={{ marginBottom: 16, borderColor: 'var(--status-failed)' }}>
          <span className="label" style={{ color: 'var(--status-failed)' }}>Error</span>
          <div className="mono" style={{ color: 'var(--status-failed)' }}>{pr.errorCode}: {pr.errorMessage}</div>
        </div>
      )}

      {pr.mergedSha && (
        <div className="card" style={{ marginBottom: 16, borderColor: 'var(--status-merged)' }}>
          <span className="label" style={{ color: 'var(--status-merged)' }}>Merged</span>
          <div className="mono">{pr.mergedSha} via {pr.mergeStrategy}</div>
        </div>
      )}

      {/* Actions */}
      <div style={{ display: 'flex', gap: 8, marginTop: 24, flexWrap: 'wrap' }}>
        {pr.status === 'DRAFT' && (
          <button className="btn" onClick={() => markReady.mutate()} disabled={markReady.isPending}>
            <Eye size={14} /> Mark Ready
          </button>
        )}
        {(pr.status === 'OPEN' || pr.status === 'DRAFT') && (
          <div style={{ display: 'flex', gap: 4, alignItems: 'center' }}>
            <select value={mergeStrategy} onChange={e => setMergeStrategy(e.target.value)} style={{ width: 'auto', padding: '6px 10px', fontSize: 11 }}>
              <option value="SQUASH">Squash</option>
              <option value="MERGE">Merge</option>
              <option value="REBASE">Rebase</option>
            </select>
            <button className="btn btn-primary" onClick={() => merge.mutate()} disabled={merge.isPending}>
              <GitMerge size={14} /> Merge
            </button>
          </div>
        )}
      </div>

      {pr.prNumber && (
        <div style={{ marginTop: 24 }}>
          <span className="label">Post Comment</span>
          <div style={{ display: 'flex', gap: 8 }}>
            <textarea value={comment} onChange={e => setComment(e.target.value)} rows={2} placeholder="Write a comment..." style={{ flex: 1 }} />
            <button className="btn" onClick={() => postComment.mutate()} disabled={!comment.trim() || postComment.isPending} style={{ alignSelf: 'flex-end' }}>
              <MessageSquare size={14} /> Send
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
