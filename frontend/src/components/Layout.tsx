import { NavLink } from 'react-router-dom'
import { LayoutDashboard, FileText, Terminal, GitPullRequest } from 'lucide-react'
import type { ReactNode } from 'react'

const NAV = [
  { to: '/', icon: LayoutDashboard, label: 'Dashboard' },
  { to: '/plans', icon: FileText, label: 'Plans' },
  { to: '/coding-runs', icon: Terminal, label: 'Coding Runs' },
  { to: '/pull-requests', icon: GitPullRequest, label: 'Pull Requests' },
]

export default function Layout({ children }: { children: ReactNode }) {
  return (
    <div style={{ display: 'flex', height: '100vh', overflow: 'hidden' }}>
      <aside style={sidebar}>
        <div style={logoArea}>
          <div style={logoMark}>
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
              <path d="M8 1L14.9 5.5V14.5L8 10L1.1 14.5V5.5L8 1Z" fill="#e8a849" fillOpacity="0.9"/>
              <path d="M8 6L11.46 8.5V13.5L8 11L4.54 13.5V8.5L8 6Z" fill="#09090b"/>
            </svg>
          </div>
          <div>
            <div style={{ fontFamily: 'var(--font-serif)', fontWeight: 700, fontSize: 15, letterSpacing: '-0.01em', color: 'var(--text-primary)' }}>
              Agentic <span style={{ color: 'var(--gold-300)' }}>SDLC</span>
            </div>
            <div style={{ fontFamily: 'var(--font-mono)', fontSize: 9, color: 'var(--text-muted)', letterSpacing: '0.14em', textTransform: 'uppercase' as const }}>
              Black Vault
            </div>
          </div>
        </div>

        <nav style={{ flex: 1, padding: '12px 10px' }}>
          {NAV.map(({ to, icon: Icon, label }) => (
            <NavLink
              key={to}
              to={to}
              end={to === '/'}
              style={({ isActive }) => ({
                ...navItem,
                background: isActive ? 'rgba(212,165,100,0.06)' : 'transparent',
                color: isActive ? '#e8a849' : '#918a7e',
                borderLeft: isActive ? '2px solid #c4913a' : '2px solid transparent',
              })}
            >
              <Icon size={15} strokeWidth={1.5} />
              {label}
            </NavLink>
          ))}
        </nav>

        <div style={{ padding: '0 20px', margin: '0 0 12px' }}>
          <div style={{ height: 1, background: 'linear-gradient(90deg, transparent, rgba(255,255,255,0.06), transparent)', opacity: 0.3 }} />
        </div>

        <div style={sidebarFooter}>
          <div style={{ fontFamily: "'IBM Plex Mono', monospace", fontSize: 9, color: '#5c5750', letterSpacing: '0.06em' }}>
            v0.1.0 &middot; Spring Boot 4
          </div>
          <div style={{ fontFamily: "'IBM Plex Mono', monospace", fontSize: 9, color: '#5c5750', letterSpacing: '0.06em' }}>
            LangChain4j &middot; React 19
          </div>
        </div>
      </aside>

      <main style={mainArea}>
        {children}
      </main>
    </div>
  )
}

const sidebar: React.CSSProperties = {
  width: 220,
  minWidth: 220,
  background: 'rgba(255, 255, 255, 0.02)',
  backdropFilter: 'blur(24px) saturate(1.4)',
  borderRight: '1px solid rgba(255,255,255,0.06)',
  display: 'flex',
  flexDirection: 'column',
}

const logoArea: React.CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  gap: 10,
  padding: '22px 16px 20px',
  borderBottom: '1px solid rgba(255,255,255,0.06)',
}

const logoMark: React.CSSProperties = {
  width: 32,
  height: 32,
  borderRadius: 8,
  background: 'rgba(232,168,73,0.1)',
  border: '1px solid rgba(232,168,73,0.2)',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
}

const navItem: React.CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  gap: 10,
  padding: '9px 14px',
  borderRadius: 6,
  fontFamily: "'IBM Plex Mono', monospace",
  fontSize: 11.5,
  fontWeight: 400,
  letterSpacing: '0.03em',
  transition: 'all 0.15s',
  marginBottom: 2,
}

const sidebarFooter: React.CSSProperties = {
  padding: '10px 16px 16px',
}

const mainArea: React.CSSProperties = {
  flex: 1,
  overflow: 'auto',
  padding: '28px 36px',
  background: 'transparent',
}
