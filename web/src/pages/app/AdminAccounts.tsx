/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { EllipsisHorizontalIcon, LockClosedIcon, LockOpenIcon, CheckCircleIcon, XCircleIcon } from '@heroicons/react/24/outline'
import { apiJson } from '../../lib/api'
import { usePageTitle } from '../../components/PageHeader'

export type AdminAccountRow = {
  accountId: string
  name: string
  plan: string
  extraTunnels: number
  activeTunnels: number
  blocked: boolean
  createdAt: string
}

export default function AdminAccounts() {
  usePageTitle('Admin • Accounts')

  const [rows, setRows] = useState<AdminAccountRow[] | null>(null)
  const [openMenuId, setOpenMenuId] = useState<string | null>(null)
  const [search, setSearch] = useState<string>('')

  const refresh = (s?: string) => {
    const qs = s && s.trim().length > 0 ? `?search=${encodeURIComponent(s.trim())}` : ''
    void apiJson<AdminAccountRow[]>(`/api/admin/accounts${qs}`)
      .then(setRows)
      .catch(() => setRows([]))
  }

  useEffect(() => {
    refresh()
    const onDocClick = (e: MouseEvent) => {
      const target = e.target as HTMLElement
      if (!target.closest('[data-menu-root]')) setOpenMenuId(null)
    }
    document.addEventListener('click', onDocClick)
    return () => document.removeEventListener('click', onDocClick)
  }, [])

  useEffect(() => {
    const h = setTimeout(() => refresh(search), 300)
    return () => clearTimeout(h)
  }, [search])

  const onToggleBlock = async (row: AdminAccountRow) => {
    const path = row.blocked
      ? `/api/admin/accounts/${row.accountId}/unblock`
      : `/api/admin/accounts/${row.accountId}/block`
    try {
      await apiJson<void>(path, { method: 'POST' })
      refresh()
    } finally {
      setOpenMenuId(null)
    }
  }

  const data = useMemo(() => rows ?? [], [rows])

  return (
    <div className="flex flex-col max-w-6xl">
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-xl font-semibold text-white">Accounts</h2>
        <Link to="/app/admin" className="text-sm text-slate-400 hover:text-white">Back to Admin</Link>
      </div>

      <div className="overflow-hidden rounded-xl border border-slate-800 bg-slate-900 shadow-xl">
        <div className="px-6 py-4 border-b border-slate-800 bg-slate-800/50 flex items-center justify-between gap-4">
          <div className="font-semibold text-white">Accounts ({data.length})</div>
          <div className="ml-auto">
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search by name or ID..."
              className="w-72 rounded-lg bg-slate-800 border border-slate-700 px-3 py-2 text-sm text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
            />
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="min-w-full text-sm">
            <thead className="bg-slate-800/50">
              <tr>
                <th className="px-6 py-3 text-left font-semibold text-slate-300">Account Name</th>
                <th className="px-6 py-3 text-left font-semibold text-slate-300">Plan</th>
                <th className="px-6 py-3 text-left font-semibold text-slate-300">Extra tunnels</th>
                <th className="px-6 py-3 text-left font-semibold text-slate-300">Active tunnels</th>
                <th className="px-6 py-3 text-left font-semibold text-slate-300">Blocked</th>
                <th className="px-6 py-3 text-left font-semibold text-slate-300">Created at</th>
                <th className="px-6 py-3 text-right font-semibold text-slate-300">Actions</th>
              </tr>
            </thead>
            <tbody>
              {data.length === 0 && (
                <tr>
                  <td colSpan={7} className="px-6 py-10 text-center text-slate-500">No accounts</td>
                </tr>
              )}
              {data.map((r) => (
                <tr key={r.accountId} className="border-t border-slate-800 hover:bg-slate-800/30">
                  <td className="px-6 py-3 text-white">{r.name}</td>
                  <td className="px-6 py-3 text-slate-300">{r.plan}</td>
                  <td className="px-6 py-3 text-slate-300">{r.extraTunnels}</td>
                  <td className="px-6 py-3 text-slate-300">{r.activeTunnels}</td>
                  <td className="px-6 py-3">
                    {r.blocked ? (
                      <div className="inline-flex items-center gap-1 text-red-400"><XCircleIcon className="w-4 h-4"/> <span>Yes</span></div>
                    ) : (
                      <div className="inline-flex items-center gap-1 text-emerald-400"><CheckCircleIcon className="w-4 h-4"/> <span>No</span></div>
                    )}
                  </td>
                  <td className="px-6 py-3 text-slate-400">{new Date(r.createdAt).toLocaleString()}</td>
                  <td className="px-6 py-3 text-right">
                    <div className="relative inline-block text-left" data-menu-root>
                      <button
                        className="inline-flex items-center justify-center w-8 h-8 rounded-lg border border-slate-700 text-slate-400 hover:text-white hover:bg-slate-800"
                        onClick={(e) => { e.stopPropagation(); setOpenMenuId((v) => v === r.accountId ? null : r.accountId) }}
                        aria-label="Actions"
                      >
                        <EllipsisHorizontalIcon className="w-5 h-5" />
                      </button>
                      {openMenuId === r.accountId && (
                        <div className="absolute right-0 mt-2 w-56 rounded-xl border border-slate-800 bg-slate-900 shadow-2xl p-1 z-20">
                          <button
                            className="w-full flex items-center gap-2 px-3 py-2 rounded-lg text-left text-slate-300 hover:text-white hover:bg-slate-800"
                            onClick={() => onToggleBlock(r)}
                          >
                            {r.blocked ? (
                              <><LockOpenIcon className="w-4 h-4"/> Unblock account</>
                            ) : (
                              <><LockClosedIcon className="w-4 h-4"/> Block account</>
                            )}
                          </button>
                        </div>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
