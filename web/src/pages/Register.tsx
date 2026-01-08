/*
 * Copyright (c) 2026 AMAK Inc. All rights reserved.
 */

import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { ArrowLeftIcon } from '@heroicons/react/24/outline'
import Seo from '../components/Seo'

export default function Register() {
  const { user, loading, register } = useAuth()
  const navigate = useNavigate()
  const location = useLocation() as any
  
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!loading && user) {
      const params = new URLSearchParams(location?.search || '')
      const fromQuery = params.get('from')
      const fromState = location?.state?.from?.pathname
      
      const to = (fromQuery && typeof fromQuery === 'string') ? fromQuery : (fromState || '/app')
      navigate(to, { replace: true })
    }
  }, [user, loading, navigate, location])

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await register(email, password, name)
    } catch (err: any) {
      setError(err.message || 'Registration failed')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center relative overflow-hidden">
      <Seo 
        title="Register | Port Buddy"
        description="Create your Port Buddy account."
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
            <h1 className="text-2xl font-bold text-white mb-2">Create an account</h1>
            <p className="text-slate-400 text-sm">
              Start sharing your local ports securely.
            </p>
          </div>

          <form onSubmit={handleRegister} className="space-y-4">
            {error && (
              <div className="bg-red-500/10 border border-red-500/50 text-red-400 text-sm p-3 rounded-lg text-center">
                {error}
              </div>
            )}
            <div>
              <label className="block text-xs font-medium text-slate-400 mb-1">Full Name (optional)</label>
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="w-full bg-slate-950/50 border border-slate-800 rounded-lg px-4 py-2.5 text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500/50 transition-all"
                placeholder="John Doe"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-400 mb-1">Email</label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                className="w-full bg-slate-950/50 border border-slate-800 rounded-lg px-4 py-2.5 text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500/50 transition-all"
                placeholder="name@example.com"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-400 mb-1">Password</label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                className="w-full bg-slate-950/50 border border-slate-800 rounded-lg px-4 py-2.5 text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500/50 transition-all"
                placeholder="••••••••"
              />
            </div>
            <button
              type="submit"
              disabled={submitting}
              className="w-full bg-indigo-600 hover:bg-indigo-500 text-white font-medium py-2.5 px-4 rounded-lg transition-all disabled:opacity-50 disabled:cursor-not-allowed shadow-lg shadow-indigo-500/20"
            >
              Sign up
            </button>
          </form>

          <div className="mt-6 text-center">
            <p className="text-sm text-slate-400">
              Already have an account?{' '}
              <Link to="/login" className="text-indigo-400 hover:text-indigo-300 font-medium">
                Log in
              </Link>
            </p>
          </div>

          <div className="mt-8 pt-6 border-t border-slate-800 text-center">
            <p className="text-xs text-slate-500 mb-4">
              By continuing, you agree to our{' '}
              <Link to="/terms" className="text-indigo-400 hover:text-indigo-300 hover:underline">Terms of Service</Link>
              {' '}and{' '}
              <Link to="/privacy" className="text-indigo-400 hover:text-indigo-300 hover:underline">Privacy Policy</Link>.
            </p>
            
            <Link to="/" className="inline-flex items-center gap-2 text-sm text-slate-400 hover:text-white transition-colors group">
              <ArrowLeftIcon className="w-4 h-4 group-hover:-translate-x-1 transition-transform" />
              Back to home
            </Link>
          </div>
        </div>
      </div>
    </div>
  )
}
