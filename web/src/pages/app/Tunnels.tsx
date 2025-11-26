import { useEffect, useMemo, useRef, useState } from 'react'
import { apiJson } from '../../lib/api'
import { useAuth } from '../../auth/AuthContext'
import { usePageTitle } from '../../components/PageHeader'

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
  usePageTitle('Tunnels')
  const hasUser = useMemo(() => !!user, [user])
  const [tunnels, setTunnels] = useState<TunnelView[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(false)
  const sentinelRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    if (!hasUser) return
    void loadTunnels(0, false)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hasUser])

  // Infinite scroll using IntersectionObserver
  useEffect(() => {
    if (!hasUser) return
    const el = sentinelRef.current
    if (!el) return
    const rootEl = document.querySelector('[data-scroll-root]') as Element | null
    const observer = new IntersectionObserver((entries) => {
      const entry = entries[0]
      const hasNext = page < totalPages - 1
      if (entry.isIntersecting && hasNext && !loading) {
        void loadTunnels(page + 1, true)
      }
    }, { root: rootEl ?? null, rootMargin: '0px', threshold: 0.1 })
    observer.observe(el)
    return () => observer.disconnect()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hasUser, page, totalPages, loading])

  // Ensure we fill the viewport: if after loading the sentinel is visible and there are more pages, fetch next automatically
  useEffect(() => {
    if (!hasUser || loading) return
    const hasNext = page < totalPages - 1
    if (!hasNext) return
    const el = sentinelRef.current
    if (!el) return
    const rect = el.getBoundingClientRect()
    const rootEl = document.querySelector('[data-scroll-root]') as Element | null
    if (rootEl) {
      const rootRect = rootEl.getBoundingClientRect()
      if (rect.top <= rootRect.bottom) {
        void loadTunnels(page + 1, true)
      }
    } else {
      if (rect.top <= window.innerHeight) {
        void loadTunnels(page + 1, true)
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hasUser, loading, page, totalPages])

  async function loadTunnels(nextPage: number, append: boolean) {
    setLoading(true)
    try {
      const res = await apiJson<{ content: TunnelView[], number: number, totalPages: number }>(`/api/tunnels?page=${nextPage}&size=30`)
      const nextContent = res.content || []
      if (append) {
        setTunnels((prev) => [...prev, ...nextContent])
      } else {
        setTunnels(nextContent)
      }
      setPage(res.number ?? nextPage)
      setTotalPages(res.totalPages ?? 0)
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
    <div className="flex flex-col">
      <p className="text-white/70">Recent activity across your HTTP and TCP tunnels.</p>

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
                      <td className="px-4 py-2 align-top text-white/70">{formatDate(t.lastHeartbeatAt)}</td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Infinite scroll sentinel */}
      <div ref={sentinelRef} className="mt-4 h-8 w-full" />

      {/* Loading indicator for next page */}
      {loading && tunnels.length > 0 ? (
        <div className="text-white/60 text-sm">Loading more...</div>
      ) : null}
    </div>
  )
}
