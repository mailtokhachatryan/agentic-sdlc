import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { api } from '@/api/client'

export default function NewPlan() {
  const nav = useNavigate()
  const [inputType, setInputType] = useState<'PROMPT' | 'JIRA'>('PROMPT')
  const [prompt, setPrompt] = useState('')
  const [jiraKey, setJiraKey] = useState('')
  const [provider, setProvider] = useState('')
  const [model, setModel] = useState('')

  const mutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => api.plans.create(body),
    onSuccess: (data) => nav(`/plans/${data.id}`)
  })

  const submit = () => {
    const body: Record<string, unknown> = { inputType }
    if (inputType === 'PROMPT') body.prompt = prompt
    else body.jiraKey = jiraKey
    if (provider) body.provider = provider
    if (model) body.model = model
    mutation.mutate(body)
  }

  return (
    <div className="fade-in" style={{ maxWidth: 600 }}>
      <h1 className="page-title" style={{ marginBottom: 24 }}><span className="accent">{'>'}</span> New Plan</h1>

      <div className="field-group">
        <label className="label">Input Type</label>
        <div style={{ display: 'flex', gap: 8 }}>
          <button className={`btn ${inputType === 'PROMPT' ? 'btn-primary' : ''}`} onClick={() => setInputType('PROMPT')}>Prompt</button>
          <button className={`btn ${inputType === 'JIRA' ? 'btn-primary' : ''}`} onClick={() => setInputType('JIRA')}>Jira</button>
        </div>
      </div>

      {inputType === 'PROMPT' ? (
        <div className="field-group">
          <label className="label">Prompt</label>
          <textarea value={prompt} onChange={e => setPrompt(e.target.value)} rows={5} placeholder="Describe the engineering task..." />
        </div>
      ) : (
        <div className="field-group">
          <label className="label">Jira Key</label>
          <input value={jiraKey} onChange={e => setJiraKey(e.target.value)} placeholder="AS24-1234" />
        </div>
      )}

      <div className="grid grid-2">
        <div className="field-group">
          <label className="label">Provider (optional)</label>
          <select value={provider} onChange={e => setProvider(e.target.value)}>
            <option value="">Default</option>
            <option value="LMSTUDIO">LM Studio</option>
            <option value="OPENAI">OpenAI</option>
            <option value="ANTHROPIC">Anthropic</option>
            <option value="BEDROCK">Bedrock</option>
            <option value="OLLAMA">Ollama</option>
          </select>
        </div>
        <div className="field-group">
          <label className="label">Model (optional)</label>
          <input value={model} onChange={e => setModel(e.target.value)} placeholder="e.g. gpt-4o" />
        </div>
      </div>

      <button className="btn btn-primary" onClick={submit} disabled={mutation.isPending} style={{ marginTop: 8 }}>
        {mutation.isPending ? 'Creating...' : 'Create Plan'}
      </button>

      {mutation.isError && (
        <div style={{ marginTop: 12, color: 'var(--status-failed)', fontFamily: 'var(--font-display)', fontSize: 12 }}>
          {(mutation.error as Error).message}
        </div>
      )}
    </div>
  )
}
