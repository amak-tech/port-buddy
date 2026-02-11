/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

import { useEffect, useState } from 'react'
import { usePageTitle } from '../../components/PageHeader'
import { apiJson } from '../../lib/api'
import { Link } from 'react-router-dom'

type SystemStats = { totalUsers: number, activeTunnels: number, totalAccounts: number }
 type DailyStat = { date: string, newUsersCount: number, tunnelsCount: number, paymentEvents: number }

export default function AdminPanel() {
  usePageTitle('Admin Control Center')

  const [stats, setStats] = useState<SystemStats | null>(null)
  const [daily, setDaily] = useState<DailyStat[] | null>(null)

  useEffect(() => {
    void Promise.all([
      apiJson<SystemStats>('/api/admin/stats').catch(() => null),
      apiJson<DailyStat[]>('/api/admin/stats/daily').catch(() => null),
    ]).then(([s, d]) => {
      setStats(s)
      setDaily(d)
    })
  }, [])

  const formatDay = (iso: string): string => {
    const dt = new Date(iso)
    const dd = dt.getDate().toString().padStart(2, '0')
    const mm = (dt.getMonth() + 1).toString().padStart(2, '0')
    const yy = dt.getFullYear().toString().slice(-2)
    return `${dd}.${mm}.${yy}`
  }

  return (
    <div className="flex flex-col max-w-6xl">
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Link to="/app/admin/accounts" className="bg-slate-900 border border-slate-800 rounded-xl p-6 shadow-xl hover:bg-slate-800/50 transition-colors">
          <h3 className="text-lg font-semibold text-white mb-2">Total Accounts</h3>
          <p className="text-3xl font-bold text-amber-400">{stats ? stats.totalAccounts : '---'}</p>
        </Link>

        <Link to="/app/admin/users" className="bg-slate-900 border border-slate-800 rounded-xl p-6 shadow-xl hover:bg-slate-800/50 transition-colors">
          <h3 className="text-lg font-semibold text-white mb-2">Total Users</h3>
          <p className="text-3xl font-bold text-indigo-400">{stats ? stats.totalUsers : '---'}</p>
        </Link>

        <Link to="/app/admin/tunnels" className="bg-slate-900 border border-slate-800 rounded-xl p-6 shadow-xl hover:bg-slate-800/50 transition-colors">
          <h3 className="text-lg font-semibold text-white mb-2">Active Tunnels</h3>
          <p className="text-3xl font-bold text-emerald-400">{stats ? stats.activeTunnels : '---'}</p>
        </Link>
      </div>

      <div className="mt-8 bg-slate-900 border border-slate-800 rounded-xl overflow-hidden shadow-xl">
        <div className="px-6 py-4 border-b border-slate-800 bg-slate-800/50">
          <h3 className="font-semibold text-white">Last 30 days stats</h3>
        </div>
        <div className="p-0 overflow-x-auto">
          <table className="min-w-full divide-y divide-slate-800">
            <thead className="bg-slate-800/40">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">Date</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">New users</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">Tunnels</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">Payment events</th>
              </tr>
            </thead>
            <tbody className="bg-slate-900 divide-y divide-slate-800">
              {daily && daily.length > 0 ? (
                daily.map((r, idx) => (
                  <tr key={idx} className="hover:bg-slate-800/30">
                    <td className="px-6 py-3 whitespace-nowrap text-slate-200">{formatDay(r.date)}</td>
                    <td className="px-6 py-3 whitespace-nowrap text-slate-200">{r.newUsersCount}</td>
                    <td className="px-6 py-3 whitespace-nowrap text-slate-200">{r.tunnelsCount}</td>
                    <td className="px-6 py-3 whitespace-nowrap text-slate-200">{r.paymentEvents}</td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={4} className="px-6 py-12 text-center text-slate-500">{daily === null ? 'Loading...' : 'No data'}</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
