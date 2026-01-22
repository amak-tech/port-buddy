import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { ArrowLeftIcon, ChevronDownIcon, ChevronUpIcon } from '@heroicons/react/24/outline'

export default function Login() {
  const { user, loading, loginWithGoogle, loginWithGithub, loginWithEmail } = useAuth()
  const navigate = useNavigate()
  const location = useLocation() as any
  
  const [showEmail, setShowEmail] = useState(false)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    // After refresh completes and user exists, redirect to intended page or /app.
    if (!loading && user) {
      const params = new URLSearchParams(location?.search || '')
      const fromQuery = params.get('from')
      const fromState = location?.state?.from?.pathname
      const fromStorage = localStorage.getItem('pb_login_from')
      
      const to = (fromQuery && typeof fromQuery === 'string') ? fromQuery : (fromState || fromStorage || '/app')
      
      // Clean up storage
      localStorage.removeItem('pb_login_from')
      
      navigate(to, { replace: true })
    }
  }, [user, loading, navigate, location])

  const handleGoogleLogin = () => {
    // Save current 'from' to localStorage because OAuth redirect will lose React state
    const from = location?.state?.from?.pathname
    if (from) {
      localStorage.setItem('pb_login_from', from)
    }
    loginWithGoogle()
  }
  
  const handleGithubLogin = () => {
    // Save current 'from' to localStorage because OAuth redirect will lose React state
    const from = location?.state?.from?.pathname
    if (from) {
      localStorage.setItem('pb_login_from', from)
    }
    loginWithGithub()
  }

  const handleEmailLogin = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await loginWithEmail(email, password)
    } catch (err: any) {
      setError(err.message || 'Login failed')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center relative overflow-hidden">
      {/* Background gradients */}
      <div className="absolute inset-0 bg-primary-950"></div>
      <div className="absolute inset-0 bg-mesh-gradient opacity-30 pointer-events-none" />
      <div className="absolute top-[20%] left-[20%] w-64 h-64 bg-jb-purple/10 rounded-full blur-[120px] pointer-events-none" />
      <div className="absolute bottom-[20%] right-[20%] w-96 h-96 bg-jb-blue/10 rounded-full blur-[120px] pointer-events-none" />
      
      <div className="w-full max-w-md p-6 relative z-10">
        <div className="glass border border-white/5 rounded-3xl p-10 shadow-[0_20px_50px_rgba(0,0,0,0.5)]">
          <div className="text-center mb-10">
            <Link to="/" className="inline-block mb-8 group">
              <div className="flex items-center justify-center gap-3 text-2xl font-black text-white tracking-tighter uppercase transition-transform group-hover:scale-105">
                <span className="relative flex h-4 w-4">
                   <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-jb-blue opacity-75"></span>
                   <span className="relative inline-flex rounded-full h-4 w-4 bg-jb-blue shadow-[0_0_10px_rgba(51,204,255,0.5)]"></span>
                </span>
                Port Buddy
              </div>
            </Link>
            <h1 className="text-3xl font-black text-white mb-3 tracking-tight">Welcome back</h1>
            <p className="text-slate-400 text-sm font-medium">
              Manage your tunnels and domains.
            </p>
          </div>

          <div className="space-y-5">
            <button 
              onClick={handleGoogleLogin} 
              className="w-full flex items-center justify-center gap-4 bg-white hover:bg-slate-100 text-slate-900 font-bold py-4 px-6 rounded-2xl transition-all transform hover:-translate-y-1 active:scale-[0.98] shadow-lg"
              aria-label="Sign in with Google"
            >
              <svg className="w-6 h-6" viewBox="0 0 24 24">
                <path
                  d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                  fill="#4285F4"
                />
                <path
                  d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                  fill="#34A853"
                />
                <path
                  d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
                  fill="#FBBC05"
                />
                <path
                  d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
                  fill="#EA4335"
                />
              </svg>
              Continue with Google
            </button>

            <button 
              onClick={handleGithubLogin} 
              className="w-full flex items-center justify-center gap-4 bg-slate-800 hover:bg-slate-700 text-white font-bold py-4 px-6 rounded-2xl transition-all transform hover:-translate-y-1 active:scale-[0.98] shadow-lg border border-white/5"
              aria-label="Sign in with GitHub"
            >
              <svg className="w-6 h-6 fill-current" viewBox="0 0 24 24">
                <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.041-1.416-4.041-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z" />
              </svg>
              Continue with GitHub
            </button>

            <div className="relative py-4">
              <div className="absolute inset-0 flex items-center">
                <div className="w-full border-t border-white/5"></div>
              </div>
              <div className="relative flex justify-center text-xs">
                <span className="px-4 bg-primary-950 text-slate-500 font-bold uppercase tracking-widest">Or continue with</span>
              </div>
            </div>

            <button
              onClick={() => setShowEmail(!showEmail)}
              className="w-full flex items-center justify-center gap-2 text-slate-400 hover:text-white transition-colors text-sm font-bold"
            >
              {showEmail ? 'Hide email login' : 'Log in with email'}
              {showEmail ? <ChevronUpIcon className="w-4 h-4" /> : <ChevronDownIcon className="w-4 h-4" />}
            </button>

            {showEmail && (
              <form onSubmit={handleEmailLogin} className="space-y-5 pt-4 animate-in slide-in-from-top-2 fade-in duration-300">
                {error && (
                  <div className="bg-red-500/10 border border-red-500/50 text-red-400 text-sm p-4 rounded-2xl text-center font-medium">
                    {error}
                  </div>
                )}
                <div>
                  <label className="block text-xs font-black text-slate-500 mb-2 uppercase tracking-widest">Email</label>
                  <input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                    className="w-full bg-white/5 border border-white/10 rounded-2xl px-5 py-3.5 text-white focus:outline-none focus:ring-2 focus:ring-jb-blue/50 focus:border-jb-blue/50 transition-all font-mono"
                    placeholder="name@example.com"
                  />
                </div>
                <div>
                  <label className="block text-xs font-black text-slate-500 mb-2 uppercase tracking-widest">Password</label>
                  <input
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                    className="w-full bg-white/5 border border-white/10 rounded-2xl px-5 py-3.5 text-white focus:outline-none focus:ring-2 focus:ring-jb-blue/50 focus:border-jb-blue/50 transition-all font-mono"
                    placeholder="••••••••"
                  />
                </div>
                <button
                  type="submit"
                  disabled={submitting}
                  className="btn btn-primary w-full py-4 rounded-2xl text-lg font-black uppercase tracking-wider"
                >
                  Sign in
                </button>
                <div className="text-center space-y-3">
                  <Link to="/forgot-password"  className="block text-xs text-jb-blue hover:text-jb-blue/80 font-bold uppercase tracking-widest">
                    Forgot password?
                  </Link>
                  <p className="text-sm text-slate-400">
                    Don't have an account?{' '}
                    <Link to="/register" className="text-jb-blue hover:text-jb-blue/80 font-bold transition-colors">
                      Sign up
                    </Link>
                  </p>
                </div>
              </form>
            )}
          </div>

          <div className="mt-10 pt-8 border-t border-white/5 text-center">
            <p className="text-xs text-slate-500 mb-6 leading-relaxed">
              By continuing, you agree to our{' '}
              <Link to="/terms" className="text-slate-300 hover:text-white transition-colors underline decoration-white/10 underline-offset-4">Terms of Service</Link>
              {' '}and{' '}
              <Link to="/privacy" className="text-slate-300 hover:text-white transition-colors underline decoration-white/10 underline-offset-4">Privacy Policy</Link>.
            </p>
            
            <Link to="/" className="inline-flex items-center gap-3 text-sm font-bold text-slate-400 hover:text-white transition-colors group">
              <ArrowLeftIcon className="w-5 h-5 group-hover:-translate-x-1 transition-transform" />
              Back to home
            </Link>
          </div>
        </div>
      </div>
    </div>
  )
}
