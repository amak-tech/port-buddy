import { useEffect, useMemo, useState } from 'react'
import { apiJson, apiRaw } from '../../lib/api'
import { useAuth } from '../../auth/AuthContext'
import { usePageTitle } from '../../components/PageHeader'

type TokenItem = { id: string, label: string, createdAt: string, revoked: boolean, lastUsedAt?: string }

export default function Tokens() {
  const { user } = useAuth()
  usePageTitle('Access Tokens')
  const hasUser = useMemo(() => !!user, [user])
  const [tokens, setTokens] = useState<TokenItem[]>([])
  const [loading, setLoading] = useState(false)
  const [newLabel, setNewLabel] = useState('cli')
  const [justCreatedToken, setJustCreatedToken] = useState<string | null>(null)

  useEffect(() => {
    if (!hasUser) return
    void loadTokens()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hasUser])

  async function loadTokens() {
    setLoading(true)
    try {
      const list = await apiJson<TokenItem[]>('/api/tokens')
      setTokens(list)
    } catch {
      setTokens([])
    } finally {
      setLoading(false)
    }
  }

  async function createToken() {
    setLoading(true)
    try {
      const resp = await apiJson<{ token: string }>('/api/tokens', { method: 'POST', body: JSON.stringify({ label: newLabel || 'cli' }) })
      setJustCreatedToken(resp.token as string)
      await loadTokens()
    } catch {
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

  return (
    <div>
      <p className="text-white/70">Generate API tokens to authenticate the CLI using <span className="font-mono">port-buddy init {'{API_TOKEN}'}</span>.</p>

      <div className="mt-6 flex flex-col md:flex-row gap-3 md:items-end">
        <div className="flex-1">
          <label className="text-sm text-white/60">Label</label>
          <input value={newLabel} onChange={(e) => setNewLabel(e.target.value)} className="mt-1 w-full bg-black/30 border border-white/10 rounded px-3 py-2" placeholder="e.g. laptop" />
        </div>
        <button className="btn" onClick={() => { void createToken() }} disabled={loading}>Generate Token</button>
      </div>

      {justCreatedToken && (
        <div className="mt-4 p-3 bg-emerald-500/10 border border-emerald-500/30 rounded">
          <div className="text-emerald-300 text-sm">New token created. Copy and store it now — it will not be shown again.</div>
          <div className="mt-2 font-mono break-all text-sm">{justCreatedToken}</div>
          <div className="mt-2 flex gap-2">
            <button className="btn" onClick={() => { navigator.clipboard.writeText(justCreatedToken).catch(() => { }) }}>Copy</button>
            <button className="btn btn-secondary" onClick={() => setJustCreatedToken(null)}>Dismiss</button>
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
                    <button className="btn btn-secondary" onClick={() => { void revokeToken(t.id) }}>Revoke</button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
