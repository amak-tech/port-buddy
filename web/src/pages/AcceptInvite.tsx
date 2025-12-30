/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { apiJson } from '../lib/api'
import { useAuth } from '../auth/AuthContext'
import { CheckCircleIcon, XCircleIcon } from '@heroicons/react/24/outline'

export default function AcceptInvite() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')
  const { user, authenticated, refresh } = useAuth()
  const navigate = useNavigate()
  
  const [status, setStatus] = useState<'loading' | 'success' | 'error' | 'unauthenticated'>('loading')
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!token) {
      setStatus('error')
      setError('Missing invitation token.')
      return
    }

    if (!authenticated) {
      setStatus('unauthenticated')
      return
    }

    void handleAccept()
  }, [token, authenticated])

  async function handleAccept() {
    setStatus('loading')
    try {
      await apiJson(`/api/team/accept?token=${token}`, { method: 'POST' })
      setStatus('success')
      await refresh()
      setTimeout(() => navigate('/app/team'), 3000)
    } catch (err: any) {
      setStatus('error')
      setError(err.message || 'Failed to accept invitation.')
    }
  }

  if (status === 'unauthenticated') {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-950 px-4">
        <div className="max-w-md w-full bg-slate-900 border border-slate-800 rounded-2xl p-8 text-center">
          <h2 className="text-2xl font-bold text-white mb-4">Login required</h2>
          <p className="text-slate-400 mb-8">
            You must be logged in to accept a team invitation.
          </p>
          <button
            onClick={() => navigate(`/login?redirect=${encodeURIComponent(window.location.pathname + window.location.search)}`)}
            className="w-full py-3 bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl font-bold transition-all"
          >
            Login to Accept
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-950 px-4">
      <div className="max-w-md w-full bg-slate-900 border border-slate-800 rounded-2xl p-8 text-center shadow-2xl">
        {status === 'loading' && (
          <div className="flex flex-col items-center">
            <div className="w-12 h-12 border-4 border-indigo-500/30 border-t-indigo-500 rounded-full animate-spin mb-4"></div>
            <h2 className="text-xl font-bold text-white">Accepting invitation...</h2>
          </div>
        )}

        {status === 'success' && (
          <div className="flex flex-col items-center">
            <CheckCircleIcon className="w-16 h-16 text-emerald-500 mb-4" />
            <h2 className="text-2xl font-bold text-white mb-2">Welcome to the team!</h2>
            <p className="text-slate-400">
              You've successfully joined the team. Redirecting you to the dashboard...
            </p>
          </div>
        )}

        {status === 'error' && (
          <div className="flex flex-col items-center">
            <XCircleIcon className="w-16 h-16 text-red-500 mb-4" />
            <h2 className="text-2xl font-bold text-white mb-2">Invitation failed</h2>
            <p className="text-red-400 mb-8">{error}</p>
            <button
              onClick={() => navigate('/app')}
              className="w-full py-3 bg-slate-800 hover:bg-slate-700 text-white rounded-xl font-bold transition-all"
            >
              Go to Dashboard
            </button>
          </div>
        )}
      </div>
    </div>
  )
}
