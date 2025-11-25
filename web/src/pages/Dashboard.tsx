import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { useEffect, useMemo, useState } from 'react'
import { apiJson, apiRaw } from '../lib/api'

// Use centralized api client which injects Authorization header and omits cookies

export default function Dashboard() {
  const { user } = useAuth()
  const [tokens, setTokens] = useState<Array<{id:string,label:string,createdAt:string,revoked:boolean,lastUsedAt?:string}>>([])
  const [loading, setLoading] = useState(false)
  const [newLabel, setNewLabel] = useState('cli')
  const [justCreatedToken, setJustCreatedToken] = useState<string|null>(null)
  const hasUser = useMemo(()=>!!user, [user])

  // Recent Activity (tunnels)
  type TunnelView = {
    id: string
    tunnelId: string
    type: 'HTTP' | 'TCP'
    status: 'PENDING' | 'CONNECTED' | 'CLOSED'
    local: string | null
    publicEndpoint: string | null
    publicUrl: string | null
    publicHost: string | null
    publicPort: number | null
    subdomain: string | null
    lastHeartbeatAt: string | null
    createdAt: string | null
  }
  const [tunnels, setTunnels] = useState<TunnelView[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loadingTunnels, setLoadingTunnels] = useState(false)

  useEffect(()=>{
    if (!hasUser) return
    void loadTokens()
    void loadTunnels(0)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hasUser])

  useEffect(()=>{
    if (!hasUser) return
    void loadTunnels(page)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page])

  async function loadTokens() {
    setLoading(true)
    try {
      const list = await apiJson('/api/tokens')
      setTokens(list)
    } catch (e) {
      // noop
    } finally {
      setLoading(false)
    }
  }

  async function loadTunnels(nextPage: number) {
    setLoadingTunnels(true)
    try {
      const res = await apiJson<{content: TunnelView[], number: number, size: number, totalElements: number, totalPages: number}>(`/api/tunnels?page=${nextPage}&size=10`)
      setTunnels(res.content || [])
      setPage(res.number || 0)
      setTotalPages(res.totalPages || 0)
    } catch (e) {
      setTunnels([])
      setPage(0)
      setTotalPages(0)
    } finally {
      setLoadingTunnels(false)
    }
  }

  async function createToken() {
    setLoading(true)
    try {
      const resp = await apiJson('/api/tokens', { method: 'POST', body: JSON.stringify({ label: newLabel || 'cli' }) })
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
      await apiRaw(`/api/tokens/${id}`, { method: 'DELETE' })
      await loadTokens()
    } finally {
      setLoading(false)
    }
  }
  function formatDate(iso: string | null | undefined): string {
    if (!iso) return '-'
    const d = new Date(iso)
    if (Number.isNaN(d.getTime())) return '-'
    return d.toLocaleString()
  }
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
          {loadingTunnels ? (
            <div className="text-white/60 text-sm">Loading recent activity...</div>
          ) : tunnels.length === 0 ? (
            <div className="text-white/60 text-sm">No tunnels yet.</div>
          ) : (
            tunnels.map((t) => {
              const canOpen = t.type === 'HTTP' && t.status === 'CONNECTED' && !!t.publicUrl
              const publicText = t.type === 'HTTP' ? (t.publicUrl || '-') : (t.publicEndpoint || '-')
              return (
                <div key={t.id} className="bg-black/30 border border-white/10 rounded p-4 flex flex-col md:flex-row md:items-center md:justify-between gap-2">
                  <div className="flex items-center gap-3">
                    <span className="badge">{t.type}</span>
                    <span className="text-white/70 text-sm">{t.local || '-'}</span>
                    <span className="text-white/40">→</span>
                    {canOpen ? (
                      <a href={t.publicUrl!} target="_blank" rel="noopener noreferrer" className="text-accent text-sm font-mono hover:underline">
                        {publicText}
                      </a>
                    ) : (
                      <span className="text-white/90 text-sm font-mono">{publicText}</span>
                    )}
                  </div>
                  <div className="flex items-center gap-3 text-sm">
                    <span className="badge">{t.status}</span>
                    <span className="text-white/60">Last activity: {formatDate(t.lastHeartbeatAt)}</span>
                  </div>
                </div>
              )
            })
          )}
        </div>

        <div className="mt-4 flex items-center gap-2" aria-label="Pagination for Recent Activity">
          <button className="btn btn-secondary" disabled={page <= 0 || loadingTunnels} onClick={()=> setPage(p => Math.max(0, p - 1))}>Previous</button>
          <div className="text-white/60 text-sm">Page {totalPages === 0 ? 0 : (page + 1)} of {totalPages}</div>
          <button className="btn btn-secondary" disabled={page >= totalPages - 1 || loadingTunnels || totalPages === 0} onClick={()=> setPage(p => Math.min(totalPages - 1, p + 1))}>Next</button>
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
