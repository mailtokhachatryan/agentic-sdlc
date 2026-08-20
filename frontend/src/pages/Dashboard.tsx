import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { FileText, Terminal, GitPullRequest, Activity, CheckCircle, XCircle, Clock, Zap, TrendingUp } from 'lucide-react'
import { AreaChart, Area, BarChart, Bar, ResponsiveContainer, Tooltip, XAxis, YAxis, CartesianGrid } from 'recharts'
import { api } from '@/api/client'
import StatusBadge from '@/components/StatusBadge'
import TimeAgo from '@/components/TimeAgo'

const C = { accent: '#e8a849', cyan: '#5ea8e0', fail: '#e05c5c', warn: '#e0944a', purple: '#a088d0', gold: '#f0c06a', green: '#6dd4a0' }

const weekData = [
  { day: 'Mon', plans: 3, runs: 2, prs: 1 },
  { day: 'Tue', plans: 5, runs: 4, prs: 2 },
  { day: 'Wed', plans: 2, runs: 3, prs: 3 },
  { day: 'Thu', plans: 7, runs: 5, prs: 4 },
  { day: 'Fri', plans: 4, runs: 6, prs: 2 },
  { day: 'Sat', plans: 1, runs: 1, prs: 1 },
  { day: 'Sun', plans: 3, runs: 2, prs: 2 },
]

const tokenTrend = [
  { m: 'Jan', t: 12400 }, { m: 'Feb', t: 28300 }, { m: 'Mar', t: 45200 },
  { m: 'Apr', t: 67800 }, { m: 'May', t: 89400 }, { m: 'Jun', t: 124000 },
]

const sparkPlans = [{ v: 2 }, { v: 5 }, { v: 3 }, { v: 7 }, { v: 4 }, { v: 6 }, { v: 5 }]
const sparkRuns = [{ v: 1 }, { v: 3 }, { v: 2 }, { v: 5 }, { v: 4 }, { v: 6 }, { v: 3 }]
const sparkPrs = [{ v: 0 }, { v: 1 }, { v: 2 }, { v: 3 }, { v: 2 }, { v: 4 }, { v: 3 }]
const sparkLive = [{ v: 3 }, { v: 5 }, { v: 4 }, { v: 6 }, { v: 8 }, { v: 5 }, { v: 7 }]

const ttip: object = { contentStyle: { background: 'rgba(19,17,26,0.9)', backdropFilter: 'blur(20px)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: 10, fontFamily: 'IBM Plex Mono', fontSize: 11, color: '#f0ece4' }, cursor: { fill: 'rgba(255,255,255,0.03)' } }
const ax = { axisLine: false, tickLine: false, tick: { fill: '#605a52', fontSize: 10, fontFamily: 'IBM Plex Mono' } }

export default function Dashboard() {
  const plans = useQuery({ queryKey: ['plans'], queryFn: () => api.plans.list(0, 5) })
  const runs = useQuery({ queryKey: ['coding-runs'], queryFn: () => api.codingRuns.list(0, 5) })
  const prs = useQuery({ queryKey: ['pull-requests'], queryFn: () => api.pullRequests.list(0, 5) })

  const tp = plans.data?.totalElements ?? 0
  const tr = runs.data?.totalElements ?? 0
  const tpr = prs.data?.totalElements ?? 0
  const cp = plans.data?.content.filter(p => p.status === 'COMPLETED').length ?? 0
  const fp = plans.data?.content.filter(p => p.status === 'FAILED').length ?? 0
  const pr2 = runs.data?.content.filter(r => r.testsPassed === true).length ?? 0
  const fr = runs.data?.content.filter(r => r.testsPassed === false).length ?? 0
  const op = prs.data?.content.filter(p => p.status === 'OPEN').length ?? 0
  const mp = prs.data?.content.filter(p => p.status === 'MERGED').length ?? 0

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title"><span className="accent">{'>'}</span> Dashboard</h1>
        <Link to="/plans/new" className="btn btn-primary"><Zap size={14} /> New Plan</Link>
      </div>

      {/* === Top summary bar === */}
      <div className="card fade-in" style={{ padding: '14px 24px', marginBottom: 20, display: 'flex', alignItems: 'center', gap: 32 }}>
        <div style={summaryItem}><TrendingUp size={14} style={{ color: C.accent }} /><span style={summaryLabel}>Total Plans</span><strong style={summaryVal}>{tp}</strong></div>
        <div style={divider} />
        <div style={summaryItem}><Terminal size={14} style={{ color: C.cyan }} /><span style={summaryLabel}>Coding Runs</span><strong style={summaryVal}>{tr}</strong></div>
        <div style={divider} />
        <div style={summaryItem}><GitPullRequest size={14} style={{ color: C.purple }} /><span style={summaryLabel}>Pull Requests</span><strong style={summaryVal}>{tpr}</strong></div>
        <div style={divider} />
        <div style={summaryItem}><Activity size={14} style={{ color: C.warn }} /><span style={summaryLabel}>Success Rate</span><strong style={{ ...summaryVal, color: C.accent }}>{tr ? Math.round((pr2 / tr) * 100) : 0}%</strong></div>
      </div>

      {/* === Stat cards with sparklines === */}
      <div className="grid grid-4" style={{ marginBottom: 20 }}>
        <StatCardFull icon={<FileText size={18} />} color={C.accent} label="Plans" value={tp} spark={sparkPlans} sub1={`${cp} completed`} sub1Color={C.accent} sub2={`${fp} failed`} sub2Color={C.fail} delay={1} />
        <StatCardFull icon={<Terminal size={18} />} color={C.cyan} label="Coding Runs" value={tr} spark={sparkRuns} sub1={`${pr2} passed`} sub1Color={C.accent} sub2={`${fr} failed`} sub2Color={C.fail} delay={2} />
        <StatCardFull icon={<GitPullRequest size={18} />} color={C.purple} label="Pull Requests" value={tpr} spark={sparkPrs} sub1={`${op} open`} sub1Color={C.accent} sub2={`${mp} merged`} sub2Color={C.purple} delay={3} />
        <StatCardFull icon={<Clock size={18} />} color={C.warn} label="Avg Duration" value="42s" spark={sparkLive} sub1="per run" sub1Color={C.warn} sub2="98% uptime" sub2Color={C.accent} delay={4} />
      </div>

      {/* === Charts === */}
      <div className="grid grid-2" style={{ marginBottom: 20 }}>
        <div className="card fade-in fade-in-delay-2">
          <div style={chartHeader}><span style={chartTitle}>Weekly Activity</span><span className="mono" style={{ fontSize: 10, color: 'var(--text-muted)' }}>last 7 days</span></div>
          <ResponsiveContainer width="100%" height={210}>
            <BarChart data={weekData} barGap={2}>
              <CartesianGrid stroke="rgba(255,255,255,0.04)" strokeDasharray="3 3" vertical={false} />
              <XAxis dataKey="day" {...ax} />
              <YAxis {...ax} width={28} />
              <Tooltip {...ttip} />
              <Bar dataKey="plans" name="Plans" fill={C.accent} fillOpacity={0.75} radius={[4,4,0,0]} barSize={9} />
              <Bar dataKey="runs" name="Runs" fill={C.cyan} fillOpacity={0.75} radius={[4,4,0,0]} barSize={9} />
              <Bar dataKey="prs" name="PRs" fill={C.purple} fillOpacity={0.75} radius={[4,4,0,0]} barSize={9} />
            </BarChart>
          </ResponsiveContainer>
        </div>

        <div className="card fade-in fade-in-delay-3">
          <div style={chartHeader}><span style={chartTitle}>Token Usage</span><span className="mono" style={{ fontSize: 10, color: 'var(--text-muted)' }}>cumulative</span></div>
          <ResponsiveContainer width="100%" height={210}>
            <AreaChart data={tokenTrend}>
              <defs>
                <linearGradient id="tg" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor={C.accent} stopOpacity={0.25} />
                  <stop offset="100%" stopColor={C.accent} stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid stroke="rgba(255,255,255,0.04)" strokeDasharray="3 3" vertical={false} />
              <XAxis dataKey="m" {...ax} />
              <YAxis {...ax} width={36} tickFormatter={v => `${(v as number / 1000).toFixed(0)}k`} />
              <Tooltip {...ttip} formatter={(v: number) => [`${v.toLocaleString()} tokens`]} />
              <Area type="monotone" dataKey="t" stroke={C.accent} strokeWidth={2} fill="url(#tg)" dot={{ r: 3, fill: C.accent, strokeWidth: 0 }} activeDot={{ r: 5, fill: C.accent }} />
            </AreaChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* === Recent activity === */}
      <div className="grid grid-3">
        <RecentColumn title="Recent Plans" link="/plans" data={plans.data?.content} renderItem={p => (
          <Link key={p.id} to={`/plans/${p.id}`} className="card-link">
            <div className="card" style={recentCard}>
              <div style={recentRow}><StatusBadge status={p.status} /><span className="mono" style={{ color: 'var(--text-muted)', fontSize: 10 }}>{p.provider}</span></div>
              <div style={recentText}>{p.promptPreview || p.jiraKey || '—'}</div>
              <div style={recentTime}><TimeAgo date={p.createdAt} /></div>
            </div>
          </Link>
        )} />
        <RecentColumn title="Recent Coding Runs" link="/coding-runs" data={runs.data?.content} renderItem={r => (
          <Link key={r.id} to={`/coding-runs/${r.id}`} className="card-link">
            <div className="card" style={recentCard}>
              <div style={recentRow}><StatusBadge status={r.status} /><span className="mono" style={{ color: 'var(--text-muted)', fontSize: 10 }}>{r.filesChanged ?? 0} files</span></div>
              <div style={recentText}>{r.testsPassed === true ? 'Tests passed' : r.testsPassed === false ? 'Tests failed' : 'Running...'}</div>
              <div style={recentTime}><TimeAgo date={r.createdAt} /></div>
            </div>
          </Link>
        )} />
        <RecentColumn title="Recent PRs" link="/pull-requests" data={prs.data?.content} renderItem={pr => (
          <Link key={pr.id} to={`/pull-requests/${pr.id}`} className="card-link">
            <div className="card" style={recentCard}>
              <div style={recentRow}><StatusBadge status={pr.status} /><span className="mono" style={{ color: 'var(--text-muted)', fontSize: 10 }}>{pr.prNumber ? `#${pr.prNumber}` : '—'}</span></div>
              <div style={recentText}>{pr.title || '—'}</div>
              <div style={recentTime}><TimeAgo date={pr.createdAt} /></div>
            </div>
          </Link>
        )} />
      </div>
    </div>
  )
}

function StatCardFull({ icon, color, label, value, spark, sub1, sub1Color, sub2, sub2Color, delay }: {
  icon: React.ReactNode; color: string; label: string; value: number | string
  spark: { v: number }[]; sub1: string; sub1Color: string; sub2: string; sub2Color: string; delay: number
}) {
  return (
    <div className={`card fade-in fade-in-delay-${delay}`} style={{ padding: '18px 20px 0', overflow: 'hidden' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 10 }}>
        <div style={{ width: 34, height: 34, borderRadius: 8, background: `${color}15`, display: 'flex', alignItems: 'center', justifyContent: 'center', color }}>{icon}</div>
        <span style={{ fontFamily: 'var(--font-display)', fontSize: 10.5, fontWeight: 500, letterSpacing: '0.06em', textTransform: 'uppercase' as const, color: 'var(--text-muted)' }}>{label}</span>
      </div>
      <div style={{ fontFamily: 'var(--font-display)', fontSize: 30, fontWeight: 700, letterSpacing: '-0.03em', lineHeight: 1, color: 'var(--text-primary)' }}>{value}</div>
      <div style={{ display: 'flex', gap: 14, marginTop: 10, fontFamily: 'var(--font-display)', fontSize: 10.5, alignItems: 'center' }}>
        <span style={{ display: 'flex', alignItems: 'center', gap: 3, color: sub1Color }}><CheckCircle size={10} />{sub1}</span>
        <span style={{ display: 'flex', alignItems: 'center', gap: 3, color: sub2Color }}><XCircle size={10} />{sub2}</span>
      </div>
      <div style={{ margin: '12px -20px 0', height: 44 }}>
        <ResponsiveContainer width="100%" height={44}>
          <AreaChart data={spark}>
            <defs><linearGradient id={`sg-${label}`} x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stopColor={color} stopOpacity={0.2} /><stop offset="100%" stopColor={color} stopOpacity={0} /></linearGradient></defs>
            <Area type="monotone" dataKey="v" stroke={color} strokeWidth={1.5} fill={`url(#sg-${label})`} dot={false} />
          </AreaChart>
        </ResponsiveContainer>
      </div>
    </div>
  )
}

function RecentColumn<T>({ title, link, data, renderItem }: { title: string; link: string; data?: T[]; renderItem: (item: T) => React.ReactNode }) {
  return (
    <div className="fade-in fade-in-delay-3">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
        <span className="label" style={{ marginBottom: 0 }}>{title}</span>
        <Link to={link} className="btn btn-sm">View all</Link>
      </div>
      {data?.map(renderItem)}
      {(!data || !data.length) && <div className="empty-state">None yet</div>}
    </div>
  )
}

const summaryItem: React.CSSProperties = { display: 'flex', alignItems: 'center', gap: 8 }
const summaryLabel: React.CSSProperties = { fontFamily: 'var(--font-display)', fontSize: 10.5, color: 'var(--text-muted)', letterSpacing: '0.04em' }
const summaryVal: React.CSSProperties = { fontFamily: 'var(--font-display)', fontSize: 15, fontWeight: 700, color: 'var(--text-primary)' }
const divider: React.CSSProperties = { width: 1, height: 24, background: 'var(--border)' }
const chartHeader: React.CSSProperties = { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }
const chartTitle: React.CSSProperties = { fontFamily: 'var(--font-display)', fontSize: 13, fontWeight: 600, letterSpacing: '-0.01em' }
const recentCard: React.CSSProperties = { marginBottom: 8, padding: 14 }
const recentRow: React.CSSProperties = { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }
const recentText: React.CSSProperties = { fontSize: 13, marginBottom: 4, color: 'var(--text-primary)' }
const recentTime: React.CSSProperties = { fontSize: 11, color: 'var(--text-muted)' }
