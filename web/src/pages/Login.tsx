import { useEffect } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { ArrowLeftIcon } from '@heroicons/react/24/outline'

export default function Login() {
  const { user, loading, loginWithGoogle } = useAuth()
  const navigate = useNavigate()
  const location = useLocation() as any

  useEffect(() => {
    // After refresh completes and user exists, redirect to intended page or /app.
    if (!loading && user) {
      const params = new URLSearchParams(location?.search || '')
      const fromQuery = params.get('from')
      const fromState = location?.state?.from?.pathname
      const to = (fromQuery && typeof fromQuery === 'string') ? fromQuery : (fromState || '/app')
      navigate(to, { replace: true })
    }
  }, [user, loading, navigate, location])

  return (
    <div className="min-h-screen flex items-center justify-center relative overflow-hidden">
      {/* Background gradients */}
      <div className="absolute inset-0 bg-slate-950"></div>
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_center,_var(--tw-gradient-stops))] from-indigo-900/20 via-slate-900/0 to-slate-900/0 pointer-events-none" />
      
      <div className="w-full max-w-md p-6 relative z-10">
        <div className="bg-slate-900/50 border border-slate-800 rounded-2xl p-8 shadow-2xl backdrop-blur-sm">
          <div className="text-center mb-8">
            <Link to="/" className="inline-block mb-6">
              <div className="flex items-center justify-center gap-2 text-xl font-bold text-white">
                <span className="w-3 h-3 rounded-full bg-indigo-500 inline-block shadow-[0_0_10px_rgba(99,102,241,0.5)]"></span>
                Port Buddy
              </div>
            </Link>
            <h1 className="text-2xl font-bold text-white mb-2">Welcome back</h1>
            <p className="text-slate-400 text-sm">
              Sign in to manage your tunnels and domains.
            </p>
          </div>

          <div className="space-y-4">
            <button 
              onClick={() => loginWithGoogle()} 
              className="w-full flex items-center justify-center gap-3 bg-white hover:bg-slate-100 text-slate-900 font-medium py-3 px-4 rounded-lg transition-all transform hover:scale-[1.02] active:scale-[0.98]"
              aria-label="Sign in with Google"
            >
              <svg className="w-5 h-5" viewBox="0 0 24 24">
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
          </div>

          <div className="mt-8 pt-6 border-t border-slate-800 text-center">
            <p className="text-xs text-slate-500 mb-4">
              By continuing, you agree to our{' '}
              <a href="#terms" className="text-indigo-400 hover:text-indigo-300 hover:underline">Terms of Service</a>
              {' '}and{' '}
              <a href="#privacy" className="text-indigo-400 hover:text-indigo-300 hover:underline">Privacy Policy</a>.
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
