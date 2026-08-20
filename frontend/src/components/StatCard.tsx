import type { ReactNode } from 'react'

export default function StatCard({ label, value, icon, delay = 0 }: {
  label: string; value: ReactNode; icon: ReactNode; delay?: number
}) {
  return (
    <div className={`card fade-in fade-in-delay-${delay}`} style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
      <div style={{
        width: 42, height: 42, borderRadius: 'var(--radius-md)',
        background: 'var(--accent-dim)', display: 'flex',
        alignItems: 'center', justifyContent: 'center', color: 'var(--accent)',
        flexShrink: 0,
      }}>
        {icon}
      </div>
      <div>
        <div className="kv-label">{label}</div>
        <div style={{ fontFamily: 'var(--font-display)', fontSize: 22, fontWeight: 700, color: 'var(--text-primary)', letterSpacing: '-0.02em' }}>
          {value}
        </div>
      </div>
    </div>
  )
}
