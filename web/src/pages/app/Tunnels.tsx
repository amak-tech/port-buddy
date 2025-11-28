import { useEffect, useMemo, useRef, useState } from 'react'
import { apiJson } from '../../lib/api'
import { useAuth } from '../../auth/AuthContext'
import { usePageTitle } from '../../components/PageHeader'
import { ArrowTopRightOnSquareIcon, GlobeAltIcon, ServerIcon } from '@heroicons/react/24/outline'

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

  // Ensure we fill the viewport
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
    <div className="flex flex-col max-w-6xl">
      <div className="mb-8">
        <h2 className="text-2xl font-bold text-white">Active Tunnels</h2>
        <p className="text-slate-400 mt-1">Monitor and manage your HTTP and TCP tunnels.</p>
      </div>

      <div className="mt-2">
        {loading && tunnels.length === 0 ? (
          <div className="text-slate-400 text-sm">Loading tunnels...</div>
        ) : tunnels.length === 0 ? (
          <div className="bg-slate-900/50 border border-slate-800 rounded-xl p-12 text-center">
            <div className="inline-flex items-center justify-center w-12 h-12 rounded-full bg-slate-800 text-slate-400 mb-4">
              <GlobeAltIcon className="w-6 h-6" />
            </div>
            <h3 className="text-lg font-medium text-white mb-2">No tunnels found</h3>
            <p className="text-slate-400 mb-6">Start your first tunnel using the CLI.</p>
            <code className="px-3 py-2 bg-slate-950 border border-slate-800 rounded-lg text-sm text-indigo-400 font-mono">
              port-buddy 8080
            </code>
          </div>
        ) : (
          <div className="overflow-hidden rounded-xl border border-slate-800 bg-slate-900/50 shadow-xl">
            <div className="overflow-x-auto">
              <table className="min-w-full text-left text-sm">
                <thead className="bg-slate-900 text-slate-400 uppercase font-medium tracking-wider">
                  <tr>
                    <th className="px-6 py-4">Type</th>
                    <th className="px-6 py-4">Local Address</th>
                    <th className="px-6 py-4">Public URL</th>
                    <th className="px-6 py-4">Status</th>
                    <th className="px-6 py-4">Last Activity</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800">
                  {tunnels.map((t) => {
                    const canOpen = t.type === 'HTTP' && t.status === 'CONNECTED' && !!t.publicUrl
                    const publicText = t.type === 'HTTP' ? (t.publicUrl || '-') : (t.publicEndpoint || '-')
                    return (
                      <tr key={t.id} className="hover:bg-slate-800/30 transition-colors">
                        <td className="px-6 py-4">
                          <div className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium border ${
                            t.type === 'HTTP' 
                              ? 'bg-indigo-500/10 text-indigo-400 border-indigo-500/20' 
                              : 'bg-cyan-500/10 text-cyan-400 border-cyan-500/20'
                          }`}>
                            {t.type === 'HTTP' ? <GlobeAltIcon className="w-3 h-3" /> : <ServerIcon className="w-3 h-3" />}
                            {t.type}
                          </div>
                        </td>
                        <td className="px-6 py-4 text-slate-300 font-mono text-xs">{t.local || '-'}</td>
                        <td className="px-6 py-4">
                          {canOpen ? (
                            <a href={t.publicUrl!} target="_blank" rel="noopener noreferrer" className="flex items-center gap-1.5 text-indigo-400 hover:text-indigo-300 hover:underline font-mono text-xs break-all group">
                              {publicText}
                              <ArrowTopRightOnSquareIcon className="w-3 h-3 opacity-0 group-hover:opacity-100 transition-opacity" />
                            </a>
                          ) : (
                            <span className="text-slate-400 font-mono text-xs break-all">{publicText}</span>
                          )}
                        </td>
                        <td className="px-6 py-4">
                           <div className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium border ${
                             t.status === 'CONNECTED' 
                               ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20' 
                               : t.status === 'CLOSED'
                               ? 'bg-slate-700/10 text-slate-400 border-slate-700/20'
                               : 'bg-yellow-500/10 text-yellow-400 border-yellow-500/20'
                           }`}>
                             <span className={`w-1.5 h-1.5 rounded-full ${t.status === 'CONNECTED' ? 'bg-emerald-400 animate-pulse' : t.status === 'CLOSED' ? 'bg-slate-400' : 'bg-yellow-400'}`}></span>
                             {t.status}
                           </div>
                        </td>
                        <td className="px-6 py-4 text-slate-500 text-xs">{formatDate(t.lastHeartbeatAt)}</td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>

      {/* Infinite scroll sentinel */}
      <div ref={sentinelRef} className="mt-4 h-8 w-full" />

      {/* Loading indicator for next page */}
      {loading && tunnels.length > 0 ? (
        <div className="text-center py-4 text-slate-500 text-sm">Loading more tunnels...</div>
      ) : null}
    </div>
  )
}
