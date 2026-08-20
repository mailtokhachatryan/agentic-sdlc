import { useQuery } from '@tanstack/react-query'
import { useParams, Link } from 'react-router-dom'
import { ArrowLeft, GitPullRequest } from 'lucide-react'
import { api } from '@/api/client'
import StatusBadge from '@/components/StatusBadge'
import DiffView from '@/components/DiffView'

export default function CodingRunDetail() {
  const { id } = useParams<{ id: string }>()
  const { data: run, isLoading } = useQuery({ queryKey: ['coding-run', id], queryFn: () => api.codingRuns.get(id!) })

  if (isLoading) return <div className="loading"><div className="spinner" /></div>
  if (!run) return <div className="empty-state">Run not found</div>

  return (
    <div className="fade-in">
      <div className="page-header">
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <Link to="/coding-runs" className="btn btn-sm"><ArrowLeft size={14} /></Link>
          <h1 className="page-title">Coding Run <span className="mono" style={{ color: 'var(--text-muted)' }}>{run.id.slice(0, 8)}</span></h1>
        </div>
        {run.status === 'COMPLETED' && run.diff && (
          <Link to={`/pull-requests/new?codingRunId=${run.id}`} className="btn btn-primary">
            <GitPullRequest size={14} /> Open PR
          </Link>
        )}
      </div>

      <div className="grid grid-4" style={{ marginBottom: 24 }}>
        <div className="kv"><span className="kv-label">Status</span><StatusBadge status={run.status} /></div>
        <div className="kv"><span className="kv-label">Provider</span><span className="kv-value mono">{run.provider}</span></div>
        <div className="kv"><span className="kv-label">Files Changed</span><span className="kv-value mono">{run.filesChanged ?? '—'}</span></div>
        <div className="kv"><span className="kv-label">Duration</span><span className="kv-value mono">{run.durationMs ? `${(run.durationMs / 1000).toFixed(1)}s` : '—'}</span></div>
      </div>

      <div className="grid grid-4" style={{ marginBottom: 24 }}>
        <div className="kv"><span className="kv-label">Iterations</span><span className="kv-value mono">{run.iterationsUsed ?? '—'}</span></div>
        <div className="kv"><span className="kv-label">Tokens</span><span className="kv-value mono">{run.tokensUsed?.toLocaleString() ?? '—'}</span></div>
        <div className="kv"><span className="kv-label">Tests</span><span className="kv-value" style={{ color: run.testsPassed ? 'var(--status-completed)' : 'var(--status-failed)' }}>{run.testsPassed ? 'Passed' : run.testsPassed === false ? 'Failed' : '—'}</span></div>
        <div className="kv"><span className="kv-label">Plan</span><Link to={`/plans/${run.planId}`} className="mono" style={{ fontSize: 12 }}>{run.planId.slice(0, 8)}...</Link></div>
      </div>

      {run.timeoutReason && (
        <div className="card" style={{ marginBottom: 16, borderColor: 'var(--status-timed-out)' }}>
          <span className="label" style={{ color: 'var(--status-timed-out)' }}>Timeout Reason</span>
          <div className="mono" style={{ color: 'var(--status-timed-out)' }}>{run.timeoutReason}</div>
        </div>
      )}

      {run.errorCode && (
        <div className="card" style={{ marginBottom: 16, borderColor: 'var(--status-failed)' }}>
          <span className="label" style={{ color: 'var(--status-failed)' }}>Error</span>
          <div className="mono" style={{ color: 'var(--status-failed)' }}>{run.errorCode}: {run.errorMessage}</div>
        </div>
      )}

      {run.diff && (
        <div style={{ marginTop: 16 }}>
          <span className="label">Diff</span>
          <DiffView diff={run.diff} />
        </div>
      )}
    </div>
  )
}
