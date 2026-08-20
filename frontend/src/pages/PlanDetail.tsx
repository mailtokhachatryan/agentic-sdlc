import { useQuery } from '@tanstack/react-query'
import { useParams, Link } from 'react-router-dom'
import { ArrowLeft, Play } from 'lucide-react'
import { api } from '@/api/client'
import StatusBadge from '@/components/StatusBadge'

export default function PlanDetail() {
  const { id } = useParams<{ id: string }>()
  const { data: plan, isLoading } = useQuery({ queryKey: ['plan', id], queryFn: () => api.plans.get(id!) })

  if (isLoading) return <div className="loading"><div className="spinner" /></div>
  if (!plan) return <div className="empty-state">Plan not found</div>

  return (
    <div className="fade-in">
      <div className="page-header">
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <Link to="/plans" className="btn btn-sm"><ArrowLeft size={14} /></Link>
          <h1 className="page-title">Plan <span className="mono" style={{ color: 'var(--text-muted)' }}>{plan.id.slice(0, 8)}</span></h1>
        </div>
        {plan.status === 'COMPLETED' && (
          <Link to={`/coding-runs/new?planId=${plan.id}`} className="btn btn-primary">
            <Play size={14} /> Start Coding Run
          </Link>
        )}
      </div>

      <div className="grid grid-4" style={{ marginBottom: 24 }}>
        <div className="kv"><span className="kv-label">Status</span><StatusBadge status={plan.status} /></div>
        <div className="kv"><span className="kv-label">Provider</span><span className="kv-value mono">{plan.provider}</span></div>
        <div className="kv"><span className="kv-label">Model</span><span className="kv-value mono">{plan.model}</span></div>
        <div className="kv"><span className="kv-label">Duration</span><span className="kv-value mono">{plan.durationMs ? `${(plan.durationMs / 1000).toFixed(1)}s` : '—'}</span></div>
      </div>

      {plan.prompt && (
        <div className="card" style={{ marginBottom: 16 }}>
          <span className="label">Prompt</span>
          <div style={{ whiteSpace: 'pre-wrap', fontSize: 13 }}>{plan.prompt}</div>
        </div>
      )}

      {plan.jiraKey && (
        <div className="card" style={{ marginBottom: 16 }}>
          <span className="label">Jira Key</span>
          <div className="mono" style={{ fontSize: 14 }}>{plan.jiraKey}</div>
        </div>
      )}

      {plan.plan && (
        <>
          <div className="card" style={{ marginBottom: 16 }}>
            <span className="label">Summary</span>
            <div style={{ fontSize: 13, fontWeight: 500 }}>{plan.plan.summary}</div>
          </div>

          <div className="card" style={{ marginBottom: 16 }}>
            <span className="label">Approach</span>
            <div style={{ fontSize: 13 }}>{plan.plan.approach}</div>
          </div>

          {plan.plan.tasks.length > 0 && (
            <div className="card" style={{ marginBottom: 16 }}>
              <span className="label">Tasks ({plan.plan.tasks.length})</span>
              {plan.plan.tasks.map((t, i) => (
                <div key={i} style={{ display: 'flex', gap: 10, padding: '8px 0', borderBottom: i < plan.plan!.tasks.length - 1 ? '1px solid var(--border)' : 'none' }}>
                  <span className="mono" style={{ color: 'var(--accent)', minWidth: 20 }}>{i + 1}.</span>
                  <div>
                    <div style={{ fontWeight: 500, fontSize: 13 }}>{t.title}</div>
                    <div style={{ fontSize: 12, color: 'var(--text-secondary)', marginTop: 2 }}>{t.description}</div>
                    <span className="status-badge" style={{ marginTop: 4, fontSize: 10, background: 'var(--bg-tertiary)', color: 'var(--text-muted)', border: '1px solid var(--border)' }}>{t.estimate}</span>
                  </div>
                </div>
              ))}
            </div>
          )}

          {plan.plan.filesToTouch.length > 0 && (
            <div className="card" style={{ marginBottom: 16 }}>
              <span className="label">Files to Touch</span>
              {plan.plan.filesToTouch.map((f, i) => (
                <div key={i} style={{ display: 'flex', gap: 10, padding: '6px 0', borderBottom: i < plan.plan!.filesToTouch.length - 1 ? '1px solid var(--border)' : 'none' }}>
                  <span className={`status-badge status-${f.change === 'CREATE' ? 'COMPLETED' : f.change === 'DELETE' ? 'FAILED' : 'IN_PROGRESS'}`} style={{ fontSize: 10 }}>{f.change}</span>
                  <span className="mono" style={{ fontSize: 12 }}>{f.path}</span>
                  <span style={{ fontSize: 11, color: 'var(--text-muted)' }}>{f.reason}</span>
                </div>
              ))}
            </div>
          )}

          {plan.plan.risks.length > 0 && (
            <div className="card" style={{ marginBottom: 16 }}>
              <span className="label">Risks</span>
              {plan.plan.risks.map((r, i) => (
                <div key={i} style={{ padding: '8px 0', borderBottom: i < plan.plan!.risks.length - 1 ? '1px solid var(--border)' : 'none' }}>
                  <div style={{ fontSize: 13, fontWeight: 500, color: 'var(--status-timed-out)' }}>{r.risk}</div>
                  <div style={{ fontSize: 12, color: 'var(--text-secondary)', marginTop: 2 }}>{r.mitigation}</div>
                </div>
              ))}
            </div>
          )}
        </>
      )}

      {plan.error && (
        <div className="card" style={{ borderColor: 'var(--status-failed)' }}>
          <span className="label" style={{ color: 'var(--status-failed)' }}>Error</span>
          <div className="mono" style={{ color: 'var(--status-failed)' }}>{plan.error.code}: {plan.error.message}</div>
        </div>
      )}
    </div>
  )
}
