import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { Plus } from 'lucide-react'
import { api } from '@/api/client'
import StatusBadge from '@/components/StatusBadge'
import TimeAgo from '@/components/TimeAgo'

export default function PlansPage() {
  const { data, isLoading } = useQuery({ queryKey: ['plans'], queryFn: () => api.plans.list(0, 50) })

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title"><span className="accent">{'>'}</span> Plans</h1>
        <Link to="/plans/new" className="btn btn-primary"><Plus size={14} /> New Plan</Link>
      </div>
      {isLoading && <div className="loading"><div className="spinner" /></div>}
      {data?.content.map((p, i) => (
        <Link key={p.id} to={`/plans/${p.id}`} className="card-link">
          <div className={`card fade-in`} style={{ marginBottom: 10, padding: '14px 18px', display: 'flex', alignItems: 'center', gap: 16, animationDelay: `${i * 30}ms` }}>
            <StatusBadge status={p.status} />
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 13, fontWeight: 500 }}>{p.promptPreview || p.jiraKey || '—'}</div>
              <div className="meta-row" style={{ marginTop: 4 }}>
                <span className="mono">{p.provider}</span>
                <span className="mono">{p.model}</span>
                <span className="mono">{p.inputType}</span>
              </div>
            </div>
            <div style={{ fontSize: 11, color: 'var(--text-muted)', whiteSpace: 'nowrap' }}><TimeAgo date={p.createdAt} /></div>
          </div>
        </Link>
      ))}
      {data && !data.content.length && <div className="empty-state">No plans created yet. Start by creating one.</div>}
    </div>
  )
}
