/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

import { useEffect, useState } from 'react'
import { usePageTitle } from '../../components/PageHeader'
import { ShieldCheckIcon } from '@heroicons/react/24/outline'
import { apiJson } from '../../lib/api'

type SystemStats = { totalUsers: number, activeTunnels: number }

export default function AdminPanel() {
  usePageTitle('Admin Panel')

  const [stats, setStats] = useState<SystemStats | null>(null)

  useEffect(() => {
    void apiJson<SystemStats>('/api/admin/stats')
      .then(setStats)
      .catch(() => setStats(null))
  }, [])

  return (
    <div className="flex flex-col max-w-6xl">
      <div className="mb-8">
        <h2 className="text-2xl font-bold text-white flex items-center gap-2">
          <ShieldCheckIcon className="h-8 w-8 text-indigo-500" />
          Admin Control Center
        </h2>
        <p className="text-slate-400 mt-1">Manage system-wide settings and monitor all user activity.</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 shadow-xl">
          <h3 className="text-lg font-semibold text-white mb-2">Total Users</h3>
          <p className="text-3xl font-bold text-indigo-400">{stats ? stats.totalUsers : '---'}</p>
        </div>
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 shadow-xl">
          <h3 className="text-lg font-semibold text-white mb-2">Active Tunnels</h3>
          <p className="text-3xl font-bold text-emerald-400">{stats ? stats.activeTunnels : '---'}</p>
        </div>
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 shadow-xl">
          <h3 className="text-lg font-semibold text-white mb-2">System Load</h3>
          <p className="text-3xl font-bold text-amber-400">Normal</p>
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
