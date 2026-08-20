import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { Plus } from 'lucide-react'
import { api } from '@/api/client'
import StatusBadge from '@/components/StatusBadge'
import TimeAgo from '@/components/TimeAgo'

export default function PullRequestsPage() {
  const { data, isLoading } = useQuery({ queryKey: ['pull-requests'], queryFn: () => api.pullRequests.list(0, 50) })

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title"><span className="accent">{'>'}</span> Pull Requests</h1>
        <Link to="/pull-requests/new" className="btn btn-primary"><Plus size={14} /> New PR</Link>
      </div>
      {isLoading && <div className="loading"><div className="spinner" /></div>}
      {data?.content.map((pr, i) => (
        <Link key={pr.id} to={`/pull-requests/${pr.id}`} className="card-link">
          <div className="card fade-in" style={{ marginBottom: 10, padding: '14px 18px', display: 'flex', alignItems: 'center', gap: 16, animationDelay: `${i * 30}ms` }}>
            <StatusBadge status={pr.status} />
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 13, fontWeight: 500 }}>{pr.title || '—'}</div>
              <div className="meta-row" style={{ marginTop: 4 }}>
                {pr.prNumber && <span className="mono">#{pr.prNumber}</span>}
                <span className="mono" style={{ fontSize: 11 }}>{pr.repoUrl?.split('/').slice(-2).join('/')}</span>
              </div>
            </div>
            <div style={{ fontSize: 11, color: 'var(--text-muted)', whiteSpace: 'nowrap' }}><TimeAgo date={pr.createdAt} /></div>
          </div>
        </Link>
      ))}
      {data && !data.content.length && <div className="empty-state">No pull requests yet.</div>}
    </div>
  )
}
