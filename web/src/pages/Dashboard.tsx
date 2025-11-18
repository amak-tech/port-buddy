import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { useEffect, useMemo, useState } from 'react'

// Resolve API base using build-time env first, then dev default, else same-origin
const API_BASE = ((): string => {
  const env = (import.meta as any).env?.VITE_API_BASE?.toString()
  if (env) return env
  if (window.location.hostname === 'localhost' && window.location.port === '5173') return 'http://localhost:8080'
  return ''
})()

async function fetchJson(path: string, init?: RequestInit) {
  const res = await fetch(`${API_BASE}${path}`, {
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', ...(init?.headers || {}) },
    ...init,
  })
  if (!res.ok) throw new Error(await res.text().catch(()=> '') || `HTTP ${res.status}`)
  return res.json()
}

export default function Dashboard() {
  const { user } = useAuth()
  const [tokens, setTokens] = useState<Array<{id:string,label:string,createdAt:string,revoked:boolean,lastUsedAt?:string}>>([])
  const [loading, setLoading] = useState(false)
  const [newLabel, setNewLabel] = useState('cli')
  const [justCreatedToken, setJustCreatedToken] = useState<string|null>(null)
  const hasUser = useMemo(()=>!!user, [user])

  useEffect(()=>{
    if (!hasUser) return
    void loadTokens()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hasUser])

  async function loadTokens() {
    setLoading(true)
    try {
      const list = await fetchJson('/api/tokens')
      setTokens(list)
    } catch (e) {
      // noop
    } finally {
      setLoading(false)
    }
  }

  async function createToken() {
    setLoading(true)
    try {
      const resp = await fetchJson('/api/tokens', { method: 'POST', body: JSON.stringify({ label: newLabel || 'cli' }) })
      setJustCreatedToken(resp.token as string)
      await loadTokens()
    } catch (e) {
      // noop
    } finally {
      setLoading(false)
    }
  }

  async function revokeToken(id: string) {
    setLoading(true)
    try {
      await fetch(`${API_BASE}/api/tokens/${id}`, { method: 'DELETE', credentials: 'include' })
      await loadTokens()
    } finally {
      setLoading(false)
    }
  }
  // Placeholder usage activity; in real app fetch from /api/usage
  const activity = [
    { id: 1, type: 'HTTP', local: 'http://localhost:3000', publicUrl: 'https://r2dx.portbuddy.dev', bytes: 512_000, at: 'Today 14:22' },
    { id: 2, type: 'TCP', local: '127.0.0.1:5432', publicUrl: 'tcp-proxy-3.portbuddy.dev:43452', bytes: 2_048_000, at: 'Yesterday 19:03' },
  ]
  const plan = user?.plan || 'basic'
  const planCapGb = plan === 'professional' ? 20 : plan === 'individual' ? 6 : 3
  const usedGb = 0.8 // placeholder
  const usedPct = Math.min(100, Math.round((usedGb / planCapGb) * 100))

  return (
    <div className="container py-16">
      <h1 className="text-3xl font-bold">Dashboard</h1>
      <p className="text-white/70 mt-2">See your recent activity, traffic usage, and manage your subscription.</p>

      <section className="mt-10 grid md:grid-cols-3 gap-6">
        <div className="bg-black/30 border border-white/10 rounded-xl p-6">
          <div className="text-white/60 text-sm">Subscription</div>
          <div className="mt-2 flex items-center gap-2">
            <span className="badge capitalize">{plan}</span>
            <span className="text-white/50 text-sm">Active</span>
          </div>
          <Link to="/app/subscription" className="btn mt-4">Manage</Link>
        </div>

        <div className="bg-black/30 border border-white/10 rounded-xl p-6">
          <div className="text-white/60 text-sm">Daily Traffic</div>
          <div className="text-2xl font-bold mt-2">{usedGb}<span className="text-base text-white/50"> / {planCapGb} GB</span></div>
          <div className="w-full h-2 bg-white/10 rounded mt-3">
            <div className="h-2 bg-accent rounded" style={{ width: `${usedPct}%` }}></div>
          </div>
        </div>

        <div className="bg-black/30 border border-white/10 rounded-xl p-6">
          <div className="text-white/60 text-sm">Profile</div>
          <div className="mt-2 text-sm">{user?.email}</div>
          <Link to="/app/profile" className="btn mt-4">Edit Profile</Link>
        </div>
      </section>

      <section className="mt-10">
        <h2 className="text-xl font-semibold">Recent Activity</h2>
        <div className="mt-4 grid gap-3">
          {activity.map((a) => (
            <div key={a.id} className="bg-black/30 border border-white/10 rounded p-4 flex flex-col md:flex-row md:items-center md:justify-between gap-2">
              <div className="flex items-center gap-3">
                <span className="badge">{a.type}</span>
                <span className="text-white/70 text-sm">{a.local}</span>
                <span className="text-white/40">→</span>
                <span className="text-white/90 text-sm font-mono">{a.publicUrl}</span>
              </div>
              <div className="text-white/60 text-sm">{(a.bytes/1024).toFixed(0)} KB • {a.at}</div>
            </div>
          ))}
        </div>
      </section>

      <section className="mt-10">
        <h2 className="text-xl font-semibold">API Tokens</h2>
        <p className="text-white/70 text-sm mt-1">Generate API tokens to authenticate the CLI using <span className="font-mono">port-buddy init {'{API_TOKEN}'}</span>.</p>

        <div className="mt-4 flex flex-col md:flex-row gap-3 md:items-end">
          <div className="flex-1">
            <label className="text-sm text-white/60">Label</label>
            <input value={newLabel} onChange={(e)=>setNewLabel(e.target.value)} className="mt-1 w-full bg-black/30 border border-white/10 rounded px-3 py-2" placeholder="e.g. laptop" />
          </div>
          <button className="btn" onClick={()=>{ void createToken() }} disabled={loading}>Generate Token</button>
        </div>

        {justCreatedToken && (
          <div className="mt-4 p-3 bg-emerald-500/10 border border-emerald-500/30 rounded">
            <div className="text-emerald-300 text-sm">New token created. Copy and store it now — it will not be shown again.</div>
            <div className="mt-2 font-mono break-all text-sm">{justCreatedToken}</div>
            <div className="mt-2 flex gap-2">
              <button className="btn" onClick={()=>{ navigator.clipboard.writeText(justCreatedToken).catch(()=>{}); }}>Copy</button>
              <button className="btn btn-secondary" onClick={()=>setJustCreatedToken(null)}>Dismiss</button>
            </div>
          </div>
        )}

        <div className="mt-6">
          {loading ? (
            <div className="text-white/60 text-sm">Loading tokens...</div>
          ) : tokens.length === 0 ? (
            <div className="text-white/60 text-sm">No tokens yet.</div>
          ) : (
            <div className="grid gap-2">
              {tokens.map(t => (
                <div key={t.id} className="flex items-center justify-between bg-black/20 border border-white/10 rounded p-3">
                  <div>
                    <div className="font-mono text-sm">{t.label}</div>
                    <div className="text-white/50 text-xs">Created {new Date(t.createdAt).toLocaleString()} {t.lastUsedAt ? `• Last used ${new Date(t.lastUsedAt).toLocaleString()}` : ''}</div>
                  </div>
                  <div className="flex items-center gap-2">
                    {t.revoked ? (
                      <span className="badge">revoked</span>
                    ) : (
                      <button className="btn btn-secondary" onClick={()=>{ void revokeToken(t.id) }}>Revoke</button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </section>

      <section className="mt-10">
        <h2 className="text-xl font-semibold">Quick Start</h2>
        <pre className="mt-3 bg-black/30 border border-white/10 rounded p-3 text-sm">
{`# Authenticate the CLI
port-buddy init {API_TOKEN}

# Expose an HTTP service
port-buddy 3000

# Expose a TCP service
port-buddy tcp 127.0.0.1:5432`}
        </pre>
      </section>
    </div>
  )
}
