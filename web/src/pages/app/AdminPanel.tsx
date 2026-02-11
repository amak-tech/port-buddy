/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

import { useEffect, useState } from 'react'
import { usePageTitle } from '../../components/PageHeader'
import { apiJson } from '../../lib/api'
import { Link } from 'react-router-dom'

type SystemStats = { totalUsers: number, activeTunnels: number, totalAccounts: number }

export default function AdminPanel() {
  usePageTitle('Admin Control Center')

  const [stats, setStats] = useState<SystemStats | null>(null)

  useEffect(() => {
    void apiJson<SystemStats>('/api/admin/stats')
      .then(setStats)
      .catch(() => setStats(null))
  }, [])

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

        <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 shadow-xl">
          <h3 className="text-lg font-semibold text-white mb-2">Active Tunnels</h3>
          <p className="text-3xl font-bold text-emerald-400">{stats ? stats.activeTunnels : '---'}</p>
        </div>
      </div>

      <div className="mt-8 bg-slate-900 border border-slate-800 rounded-xl overflow-hidden shadow-xl">
        <div className="px-6 py-4 border-b border-slate-800 bg-slate-800/50">
          <h3 className="font-semibold text-white">System Logs</h3>
        </div>
        <div className="p-6">
          <p className="text-slate-500 text-center py-12">Detailed administrative tools coming soon...</p>
        </div>
      </div>
    </div>
  )
}
