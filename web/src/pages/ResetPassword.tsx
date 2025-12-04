/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { ArrowLeftIcon } from '@heroicons/react/24/outline'
import Seo from '../components/Seo'
import { apiJson } from '../lib/api'

export default function ResetPassword() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')
  const navigate = useNavigate()

  const [password, setPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [validToken, setValidToken] = useState<boolean | null>(null) // null = loading, true = valid, false = invalid

  useEffect(() => {
    if (!token) {
      setValidToken(false)
      return
    }

    apiJson(`/api/auth/password-reset/validate?token=${token}`, undefined, { skipAuth: true })
      .then(() => setValidToken(true))
      .catch(() => setValidToken(false))
  }, [token])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)

    if (password.length < 8) {
        setError('Password must be at least 8 characters long')
        return
    }

    setSubmitting(true)
    try {
      await apiJson('/api/auth/password-reset/confirm', {
        method: 'POST',
        body: JSON.stringify({ token, newPassword: password })
      }, { skipAuth: true })
      navigate('/login', { state: { message: 'Password reset successfully. Please login.' } })
    } catch (err: any) {
      setError(err.message || 'Failed to reset password')
    } finally {
      setSubmitting(false)
    }
  }

  if (validToken === null) {
      return <div className="min-h-screen flex items-center justify-center bg-slate-950 text-white">Loading...</div>
  }

  if (!validToken) {
      return (
        <div className="min-h-screen flex items-center justify-center relative overflow-hidden">
          <Seo title="Reset Password | Port Buddy" />
          <div className="absolute inset-0 bg-slate-950"></div>
          <div className="w-full max-w-md p-6 relative z-10">
            <div className="bg-slate-900/50 border border-slate-800 rounded-2xl p-8 shadow-2xl backdrop-blur-sm text-center">
               <h1 className="text-2xl font-bold text-red-500 mb-4">Invalid or Expired Token</h1>
               <p className="text-slate-400 mb-6">This password reset link is invalid or has expired.</p>
               <Link to="/forgot-password" className="block w-full bg-indigo-600 hover:bg-indigo-500 text-white font-medium py-2.5 px-4 rounded-lg transition-all">
                 Request new link
               </Link>
            </div>
          </div>
        </div>
      )
  }

  return (
    <div className="min-h-screen flex items-center justify-center relative overflow-hidden">
      <Seo 
        title="Set New Password | Port Buddy"
        description="Set a new password for your Port Buddy account."
      />
      {/* Background gradients */}
      <div className="absolute inset-0 bg-slate-950"></div>
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_center,_var(--tw-gradient-stops))] from-indigo-900/20 via-slate-900/0 to-slate-900/0 pointer-events-none" />
      
      <div className="w-full max-w-md p-6 relative z-10">
        <div className="bg-slate-900/50 border border-slate-800 rounded-2xl p-8 shadow-2xl backdrop-blur-sm">
          <div className="text-center mb-8">
            <Link to="/" className="inline-block mb-6">
              <div className="flex items-center justify-center gap-2 text-xl font-bold text-white">
                <span className="relative flex h-3 w-3">
                   <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-indigo-400 opacity-75"></span>
                   <span className="relative inline-flex rounded-full h-3 w-3 bg-indigo-500"></span>
                </span>
                Port Buddy
              </div>
            </Link>
            <h1 className="text-2xl font-bold text-white mb-2">Set New Password</h1>
            <p className="text-slate-400 text-sm">
              Create a secure password for your account.
            </p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4 animate-in slide-in-from-top-2 fade-in duration-200">
            {error && (
                <div className="bg-red-500/10 border border-red-500/50 text-red-400 text-sm p-3 rounded-lg text-center">
                    {error}
                </div>
            )}
            <div>
                <label className="block text-xs font-medium text-slate-400 mb-1">New Password</label>
                <input
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                    minLength={8}
                    className="w-full bg-slate-950/50 border border-slate-800 rounded-lg px-4 py-2.5 text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500/50 transition-all"
                    placeholder="At least 8 characters"
                />
                {password.length > 0 && password.length < 8 && (
                     <p className="text-xs text-yellow-500 mt-1">Password is too short (min 8 chars)</p>
                )}
            </div>
            <button
                type="submit"
                disabled={submitting}
                className="w-full bg-indigo-600 hover:bg-indigo-500 text-white font-medium py-2.5 px-4 rounded-lg transition-all disabled:opacity-50 disabled:cursor-not-allowed shadow-lg shadow-indigo-500/20"
            >
                {submitting ? 'Resetting...' : 'Set New Password'}
            </button>
          </form>

          <div className="mt-8 pt-6 border-t border-slate-800 text-center">
             <Link to="/login" className="inline-flex items-center gap-2 text-sm text-slate-400 hover:text-white transition-colors group">
              <ArrowLeftIcon className="w-4 h-4 group-hover:-translate-x-1 transition-transform" />
              Back to Login
            </Link>
          </div>
        </div>
      </div>
    </div>
  )
}
