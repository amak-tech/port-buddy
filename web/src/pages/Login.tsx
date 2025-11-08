import { useEffect } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export default function Login() {
  const { user, loading, loginWithGoogle, refresh } = useAuth()
  const navigate = useNavigate()
  const location = useLocation() as any

  useEffect(() => {
    // If token present in URL, AuthProvider will store it and refresh on mount.
    // After refresh completes and user exists, redirect to intended page or /app.
    if (!loading && user) {
      const to = location?.state?.from?.pathname || '/app'
      navigate(to, { replace: true })
    }
  }, [user, loading, navigate, location])

  return (
    <div className="container py-16">
      <div className="max-w-md mx-auto bg-black/30 border border-white/10 rounded-xl p-8">
        <h1 className="text-2xl font-bold">Login</h1>
        <p className="text-white/70 mt-2 text-sm">Sign in to access your dashboard, manage your profile and subscription.</p>

        <div className="mt-6 flex flex-col gap-3">
          <button className="btn" onClick={() => loginWithGoogle()} aria-label="Sign in with Google">
            <span>Continue with Google</span>
          </button>
          {/* Placeholder for GitHub if added later */}
        </div>

        <div className="text-xs text-white/50 mt-6">
          By continuing, you agree to the
          {' '}<a href="#terms">Terms</a> and <a href="#privacy">Privacy Policy</a>.
        </div>

        <div className="mt-8 text-sm">
          <Link to="/">← Back to home</Link>
        </div>
      </div>
    </div>
  )
}
