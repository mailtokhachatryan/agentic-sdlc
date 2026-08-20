import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { Plus } from 'lucide-react'
import { api } from '@/api/client'
import StatusBadge from '@/components/StatusBadge'
import TimeAgo from '@/components/TimeAgo'

export default function CodingRunsPage() {
  const { data, isLoading } = useQuery({ queryKey: ['coding-runs'], queryFn: () => api.codingRuns.list(0, 50) })

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title"><span className="accent">{'>'}</span> Coding Runs</h1>
        <Link to="/coding-runs/new" className="btn btn-primary"><Plus size={14} /> New Run</Link>
      </div>
      {isLoading && <div className="loading"><div className="spinner" /></div>}
      {data?.content.map((r, i) => (
        <Link key={r.id} to={`/coding-runs/${r.id}`} className="card-link">
          <div className="card fade-in" style={{ marginBottom: 10, padding: '14px 18px', display: 'flex', alignItems: 'center', gap: 16, animationDelay: `${i * 30}ms` }}>
            <StatusBadge status={r.status} />
            <div style={{ flex: 1 }}>
              <div className="meta-row">
                <span className="mono">{r.provider}</span>
                <span className="mono">{r.filesChanged ?? 0} files</span>
                <span className="mono">{r.durationMs ? `${(r.durationMs / 1000).toFixed(1)}s` : '—'}</span>
                {r.testsPassed === true && <span style={{ color: 'var(--status-completed)', fontSize: 11 }}>tests passed</span>}
                {r.testsPassed === false && <span style={{ color: 'var(--status-failed)', fontSize: 11 }}>tests failed</span>}
              </div>
            </div>
            <div style={{ fontSize: 11, color: 'var(--text-muted)', whiteSpace: 'nowrap' }}><TimeAgo date={r.createdAt} /></div>
          </div>
        </Link>
      ))}
      {data && !data.content.length && <div className="empty-state">No coding runs yet.</div>}
    </div>
  )
}
