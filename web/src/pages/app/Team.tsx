/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

import { useEffect, useState } from 'react'
import { apiJson } from '../../lib/api'
import { useAuth } from '../../auth/AuthContext'
import { usePageTitle } from '../../components/PageHeader'
import { UserGroupIcon, UserPlusIcon, XMarkIcon } from '@heroicons/react/24/outline'

type Member = {
  id: string
  email: string
  firstName: string | null
  lastName: string | null
  avatarUrl: string | null
  roles: string[]
  joinedAt: string
}

type Invitation = {
  id: string
  email: string
  invitedBy: string
  createdAt: string
  expiresAt: string
}

export default function Team() {
  const { user } = useAuth()
  usePageTitle('Team Management')
  
  const [members, setMembers] = useState<Member[]>([])
  const [invitations, setInvitations] = useState<Invitation[]>([])
  const [loading, setLoading] = useState(true)
  const [email, setEmail] = useState('')
  const [inviting, setInviting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const isTeamPlan = user?.plan === 'TEAM'
  const isAccountAdmin = user?.roles?.includes('ACCOUNT_ADMIN')

  useEffect(() => {
    if (isTeamPlan) {
      void loadData()
    } else {
      setLoading(false)
    }
  }, [isTeamPlan])

  async function loadData() {
    setLoading(true)
    try {
      const [m, i] = await Promise.all([
        apiJson<Member[]>('/api/team/members'),
        apiJson<Invitation[]>('/api/team/invitations')
      ])
      setMembers(m)
      setInvitations(i)
    } catch (err) {
      console.error('Failed to load team data', err)
    } finally {
      setLoading(false)
    }
  }

  async function handleInvite(e: React.FormEvent) {
    e.preventDefault()
    if (!email) return
    
    setInviting(true)
    setError(null)
    try {
      await apiJson('/api/team/invitations', {
        method: 'POST',
        body: JSON.stringify({ email })
      })
      setEmail('')
      void loadData()
    } catch (err: any) {
      setError(err.message || 'Failed to send invitation')
    } finally {
      setInviting(false)
    }
  }

  async function handleCancelInvitation(id: string) {
    if (!window.confirm('Are you sure you want to cancel this invitation?')) return
    
    try {
      await apiJson(`/api/team/invitations/${id}`, { method: 'DELETE' })
      void loadData()
    } catch (err) {
      console.error('Failed to cancel invitation', err)
    }
  }

  if (!isTeamPlan) {
    return (
      <div className="max-w-4xl">
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-8 text-center">
          <UserGroupIcon className="w-12 h-12 text-slate-500 mx-auto mb-4" />
          <h2 className="text-xl font-bold text-white mb-2">Team plan required</h2>
          <p className="text-slate-400 mb-6">
            Team features like inviting members and shared tunnel limits are only available on the Team plan.
          </p>
          <a href="/app/billing" className="inline-flex items-center justify-center px-6 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg font-medium transition-colors">
            Upgrade to Team Plan
          </a>
        </div>
      </div>
    )
  }

  return (
    <div className="max-w-6xl space-y-8">
      {/* Invite Section */}
      {isAccountAdmin && (
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-6">
          <h3 className="text-lg font-bold text-white mb-4 flex items-center gap-2">
            <UserPlusIcon className="w-5 h-5 text-indigo-400" />
            Invite Team Member
          </h3>
          <form onSubmit={handleInvite} className="flex gap-4">
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="colleague@example.com"
              required
              className="flex-1 bg-slate-950 border border-slate-800 rounded-lg px-4 py-2 text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/50"
            />
            <button
              type="submit"
              disabled={inviting}
              className="bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 text-white px-6 py-2 rounded-lg font-medium transition-colors"
            >
              {inviting ? 'Sending...' : 'Send Invite'}
            </button>
          </form>
          {error && <p className="mt-2 text-red-400 text-sm">{error}</p>}
        </div>
      )}

      {/* Members List */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl overflow-hidden">
        <div className="px-6 py-4 border-b border-slate-800 bg-slate-900/50">
          <h3 className="text-lg font-bold text-white">Team Members</h3>
        </div>
        <div className="overflow-x-auto">
          <table className="min-w-full text-left text-sm">
            <thead className="bg-slate-900 text-slate-400 uppercase font-medium tracking-wider">
              <tr>
                <th className="px-6 py-4">User</th>
                <th className="px-6 py-4">Roles</th>
                <th className="px-6 py-4">Joined</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800">
              {loading ? (
                <tr>
                   <td colSpan={3} className="px-6 py-12 text-center text-slate-500">Loading members...</td>
                </tr>
              ) : members.map((m) => (
                <tr key={m.id} className="hover:bg-slate-800/30 transition-colors">
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-3">
                      {m.avatarUrl ? (
                        <img src={m.avatarUrl} alt="" className="w-8 h-8 rounded-full" />
                      ) : (
                        <div className="w-8 h-8 rounded-full bg-slate-800 flex items-center justify-center text-xs font-bold text-slate-400">
                          {m.firstName?.[0] || m.email[0].toUpperCase()}
                        </div>
                      )}
                      <div>
                        <div className="text-white font-medium">
                          {m.firstName} {m.lastName}
                          {m.id === user?.id && <span className="ml-2 text-[10px] bg-slate-800 text-slate-400 px-1.5 py-0.5 rounded uppercase">You</span>}
                        </div>
                        <div className="text-slate-500 text-xs">{m.email}</div>
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-4">
                    <div className="flex flex-wrap gap-1">
                      {m.roles.map(r => (
                        <span key={r} className={`text-[10px] px-1.5 py-0.5 rounded uppercase font-bold ${
                          r === 'ACCOUNT_ADMIN' ? 'bg-indigo-500/10 text-indigo-400 border border-indigo-500/20' : 'bg-slate-800 text-slate-400'
                        }`}>
                          {r.replace('_', ' ')}
                        </span>
                      ))}
                    </div>
                  </td>
                  <td className="px-6 py-4 text-slate-400 text-xs">
                    {new Date(m.joinedAt).toLocaleDateString()}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Pending Invitations */}
      {invitations.length > 0 && (
        <div className="bg-slate-900 border border-slate-800 rounded-xl overflow-hidden">
          <div className="px-6 py-4 border-b border-slate-800 bg-slate-900/50">
            <h3 className="text-lg font-bold text-white">Pending Invitations</h3>
          </div>
          <div className="overflow-x-auto">
            <table className="min-w-full text-left text-sm">
              <thead className="bg-slate-900 text-slate-400 uppercase font-medium tracking-wider">
                <tr>
                  <th className="px-6 py-4">Email</th>
                  <th className="px-6 py-4">Invited By</th>
                  <th className="px-6 py-4">Expires</th>
                  <th className="px-6 py-4"></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800">
                {invitations.map((i) => (
                  <tr key={i.id} className="hover:bg-slate-800/30 transition-colors">
                    <td className="px-6 py-4 text-white font-medium">{i.email}</td>
                    <td className="px-6 py-4 text-slate-400">{i.invitedBy}</td>
                    <td className="px-6 py-4 text-slate-400 text-xs">
                      {new Date(i.expiresAt).toLocaleDateString()}
                    </td>
                    <td className="px-6 py-4 text-right">
                      {isAccountAdmin && (
                        <button
                          onClick={() => handleCancelInvitation(i.id)}
                          className="text-slate-500 hover:text-red-400 p-1 rounded-lg transition-colors"
                          title="Cancel invitation"
                        >
                          <XMarkIcon className="w-5 h-5" />
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  )
}
