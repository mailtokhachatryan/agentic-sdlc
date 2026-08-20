import { useState } from 'react'

interface FileHunk {
  filename: string
  chunks: DiffChunk[]
}

interface DiffChunk {
  header: string
  oldStart: number
  newStart: number
  lines: DiffLine[]
}

interface DiffLine {
  type: 'add' | 'del' | 'ctx' | 'header'
  content: string
  oldNum: number | null
  newNum: number | null
}

function parseDiff(raw: string): FileHunk[] {
  const files: FileHunk[] = []
  let current: FileHunk | null = null
  let chunk: DiffChunk | null = null
  let oldLine = 0
  let newLine = 0

  for (const line of raw.split('\n')) {
    if (line.startsWith('diff --git')) {
      const match = line.match(/b\/(.+)$/)
      current = { filename: match?.[1] ?? 'unknown', chunks: [] }
      files.push(current)
      chunk = null
      continue
    }
    if (line.startsWith('---') || line.startsWith('+++') || line.startsWith('index ') || line.startsWith('new file') || line.startsWith('old mode') || line.startsWith('new mode')) {
      continue
    }
    if (line.startsWith('@@')) {
      const match = line.match(/@@ -(\d+)(?:,\d+)? \+(\d+)(?:,\d+)? @@/)
      oldLine = match ? parseInt(match[1]) : 1
      newLine = match ? parseInt(match[2]) : 1
      chunk = { header: line, oldStart: oldLine, newStart: newLine, lines: [] }
      current?.chunks.push(chunk)
      continue
    }
    if (!chunk || !current) continue
    if (line.startsWith('+')) {
      chunk.lines.push({ type: 'add', content: line.slice(1), oldNum: null, newNum: newLine++ })
    } else if (line.startsWith('-')) {
      chunk.lines.push({ type: 'del', content: line.slice(1), oldNum: oldLine++, newNum: null })
    } else {
      chunk.lines.push({ type: 'ctx', content: line.startsWith(' ') ? line.slice(1) : line, oldNum: oldLine++, newNum: newLine++ })
    }
  }
  return files
}

export default function DiffView({ diff }: { diff: string | null }) {
  const [collapsed, setCollapsed] = useState<Record<string, boolean>>({})

  if (!diff) return <div className="empty-state">No diff available</div>

  const files = parseDiff(diff)
  if (!files.length) return <div className="empty-state">Empty diff</div>

  const toggle = (f: string) => setCollapsed(p => ({ ...p, [f]: !p[f] }))

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      {files.map((file, fi) => (
        <div key={fi} style={fileCard}>
          <div style={fileHeader} onClick={() => toggle(file.filename)}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <span style={expandIcon}>{collapsed[file.filename] ? '+' : '-'}</span>
              <span style={fileName}>{file.filename}</span>
            </div>
            <div style={{ display: 'flex', gap: 8 }}>
              {(() => {
                let adds = 0, dels = 0
                file.chunks.forEach(c => c.lines.forEach(l => { if (l.type === 'add') adds++; if (l.type === 'del') dels++ }))
                return (
                  <>
                    {adds > 0 && <span style={{ ...statBadge, color: '#5cb87a', background: 'rgba(92,184,122,0.1)' }}>+{adds}</span>}
                    {dels > 0 && <span style={{ ...statBadge, color: '#d45454', background: 'rgba(212,84,84,0.1)' }}>-{dels}</span>}
                  </>
                )
              })()}
            </div>
          </div>
          {!collapsed[file.filename] && (
            <div style={codeArea}>
              {file.chunks.map((chunk, ci) => (
                <div key={ci}>
                  <div style={chunkHeader}>{chunk.header}</div>
                  {chunk.lines.map((line, li) => (
                    <div key={li} style={{
                      ...codeLine,
                      background: line.type === 'add' ? 'rgba(92,184,122,0.06)'
                        : line.type === 'del' ? 'rgba(212,84,84,0.06)'
                        : 'transparent',
                      borderLeft: line.type === 'add' ? '3px solid rgba(92,184,122,0.5)'
                        : line.type === 'del' ? '3px solid rgba(212,84,84,0.5)'
                        : '3px solid transparent'
                    }}>
                      <span style={{ ...lineNum, color: line.type === 'del' ? 'rgba(212,84,84,0.4)' : 'var(--text-muted)' }}>
                        {line.oldNum ?? ''}
                      </span>
                      <span style={{ ...lineNum, color: line.type === 'add' ? 'rgba(92,184,122,0.4)' : 'var(--text-muted)' }}>
                        {line.newNum ?? ''}
                      </span>
                      <span style={{
                        ...lineSign,
                        color: line.type === 'add' ? '#5cb87a' : line.type === 'del' ? '#d45454' : 'var(--text-muted)'
                      }}>
                        {line.type === 'add' ? '+' : line.type === 'del' ? '-' : ' '}
                      </span>
                      <span style={{
                        ...lineContent,
                        color: line.type === 'add' ? '#b5f5e0' : line.type === 'del' ? '#fca5a5' : 'var(--text-secondary)'
                      }}>
                        {line.content}
                      </span>
                    </div>
                  ))}
                </div>
              ))}
            </div>
          )}
        </div>
      ))}
    </div>
  )
}

const fileCard: React.CSSProperties = {
  border: '1px solid var(--border)',
  borderRadius: 'var(--radius-md)',
  overflow: 'hidden',
  background: 'rgba(6,9,15,0.6)',
  backdropFilter: 'blur(8px)',
}

const fileHeader: React.CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'space-between',
  padding: '10px 16px',
  background: 'rgba(16,24,32,0.8)',
  borderBottom: '1px solid var(--border)',
  cursor: 'pointer',
  userSelect: 'none',
}

const expandIcon: React.CSSProperties = {
  fontFamily: 'var(--font-display)',
  fontSize: 12,
  color: 'var(--text-muted)',
  width: 16,
  textAlign: 'center',
}

const fileName: React.CSSProperties = {
  fontFamily: 'var(--font-display)',
  fontSize: 12,
  fontWeight: 600,
  color: 'var(--text-primary)',
  letterSpacing: '0.01em',
}

const statBadge: React.CSSProperties = {
  fontFamily: 'var(--font-display)',
  fontSize: 11,
  fontWeight: 600,
  padding: '2px 8px',
  borderRadius: 100,
  letterSpacing: '0.02em',
}

const codeArea: React.CSSProperties = {
  fontFamily: 'var(--font-display)',
  fontSize: 12,
  lineHeight: 1.7,
  overflowX: 'auto',
}

const chunkHeader: React.CSSProperties = {
  padding: '6px 16px 6px 52px',
  background: 'rgba(0,201,255,0.04)',
  color: 'var(--accent-secondary)',
  fontSize: 11,
  fontFamily: 'var(--font-display)',
  borderTop: '1px solid var(--border)',
  borderBottom: '1px solid var(--border)',
}

const codeLine: React.CSSProperties = {
  display: 'flex',
  alignItems: 'stretch',
  minHeight: 22,
  padding: '0 12px 0 0',
}

const lineNum: React.CSSProperties = {
  width: 44,
  minWidth: 44,
  textAlign: 'right',
  padding: '0 8px',
  fontSize: 11,
  fontFamily: 'var(--font-display)',
  userSelect: 'none',
  flexShrink: 0,
}

const lineSign: React.CSSProperties = {
  width: 16,
  minWidth: 16,
  textAlign: 'center',
  fontFamily: 'var(--font-display)',
  fontWeight: 700,
  flexShrink: 0,
}

const lineContent: React.CSSProperties = {
  flex: 1,
  whiteSpace: 'pre',
  fontFamily: 'var(--font-display)',
  tabSize: 4,
}
