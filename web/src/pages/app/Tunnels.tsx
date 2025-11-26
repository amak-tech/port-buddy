import { useEffect, useMemo, useState } from 'react'
import { apiJson } from '../../lib/api'
import { useAuth } from '../../auth/AuthContext'

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

export default function Tunnels() {
  const { user } = useAuth()
  const hasUser = useMemo(() => !!user, [user])
  const [tunnels, setTunnels] = useState<TunnelView[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!hasUser) return
    void loadTunnels(0)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hasUser])

  useEffect(() => {
    if (!hasUser) return
    void loadTunnels(page)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page])

  async function loadTunnels(nextPage: number) {
    setLoading(true)
    try {
      const res = await apiJson<{ content: TunnelView[], number: number, totalPages: number }>(`/api/tunnels?page=${nextPage}&size=10`)
      setTunnels(res.content || [])
      setPage(res.number || 0)
      setTotalPages(res.totalPages || 0)
    } catch {
      setTunnels([])
      setPage(0)
      setTotalPages(0)
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

  return (
    <div>
      <h1 className="text-2xl font-bold">Tunnels</h1>
      <p className="text-white/70 mt-1">Recent activity across your HTTP and TCP tunnels.</p>

      <div className="mt-6">
        {loading ? (
          <div className="text-white/60 text-sm">Loading tunnels...</div>
        ) : tunnels.length === 0 ? (
          <div className="text-white/60 text-sm">No tunnels yet.</div>
        ) : (
          <div className="overflow-x-auto rounded-lg border border-white/10">
            <table className="min-w-full text-sm">
              <thead className="bg-black/40 text-white/70">
                <tr>
                  <th className="text-left font-medium px-4 py-2">Type</th>
                  <th className="text-left font-medium px-4 py-2">Local</th>
                  <th className="text-left font-medium px-4 py-2">Public</th>
                  <th className="text-left font-medium px-4 py-2">Status</th>
                  <th className="text-left font-medium px-4 py-2">Created</th>
                  <th className="text-left font-medium px-4 py-2">Last Activity</th>
                </tr>
              </thead>
              <tbody>
                {tunnels.map((t) => {
                  const canOpen = t.type === 'HTTP' && t.status === 'CONNECTED' && !!t.publicUrl
                  const publicText = t.type === 'HTTP' ? (t.publicUrl || '-') : (t.publicEndpoint || '-')
                  return (
                    <tr key={t.id} className="odd:bg-black/20 even:bg-black/10">
                      <td className="px-4 py-2 align-top"><span className="badge">{t.type}</span></td>
                      <td className="px-4 py-2 align-top text-white/80">{t.local || '-'}</td>
                      <td className="px-4 py-2 align-top">
                        {canOpen ? (
                          <a href={t.publicUrl!} target="_blank" rel="noopener noreferrer" className="text-accent font-mono hover:underline break-all">
                            {publicText}
                          </a>
                        ) : (
                          <span className="text-white/90 font-mono break-all">{publicText}</span>
                        )}
                      </td>
                      <td className="px-4 py-2 align-top"><span className="badge">{t.status}</span></td>
                      <td className="px-4 py-2 align-top text-white/70">{formatDate(t.createdAt)}</td>
                      <td className="px-4 py-2 align-top text-white/70">{formatDate(t.lastHeartbeatAt)}</td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {(() => {
        const hasPrev = page > 0
        const hasNext = page < totalPages - 1
        const isDisabledPrev = !hasPrev || loading
        const isDisabledNext = !hasNext || loading || totalPages === 0
        return (
          <div className="mt-4 flex items-center gap-2" aria-label="Pagination for Tunnels">
            <button
              className={`btn btn-secondary px-2 py-1 text-xs ${isDisabledPrev ? 'opacity-50' : ''}`}
              disabled={isDisabledPrev}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
            >
              Prev
            </button>
            <div className="text-white/60 text-sm">Page {totalPages === 0 ? 0 : page + 1} of {totalPages}</div>
            <button
              className={`btn btn-secondary px-2 py-1 text-xs ${isDisabledNext ? 'opacity-50' : ''}`}
              disabled={isDisabledNext}
              onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
            >
              Next
            </button>
          </div>
        )
      })()}
    </div>
  )
}
