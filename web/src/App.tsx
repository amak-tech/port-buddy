import { Link, Navigate, Outlet, Route, Routes, useLocation } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { Bars3Icon, XMarkIcon } from '@heroicons/react/24/outline'
import Landing from './pages/Landing'
import Installation from './pages/Installation'
import Docs from './pages/Docs'
import AcceptInvite from './pages/AcceptInvite'
import Login from './pages/Login'
import Register from './pages/Register'
import ForgotPassword from './pages/ForgotPassword'
import ResetPassword from './pages/ResetPassword'
import Billing from './pages/app/Billing'
import BillingSuccess from './pages/app/BillingSuccess'
import BillingCancel from './pages/app/BillingCancel'
import ProtectedRoute from './components/ProtectedRoute'
import { useAuth } from './auth/AuthContext'
import AppLayout from './components/AppLayout'
import { useLoading } from './components/LoadingContext'
import ProgressBar from './components/ProgressBar'
import { setLoadingCallbacks } from './lib/api'
import Tunnels from './pages/app/Tunnels'
import Tokens from './pages/app/Tokens'
import Domains from './pages/app/Domains'
import Settings from './pages/app/Settings'
import Team from './pages/app/Team'
import Ports from './pages/app/Ports'
import Profile from './pages/app/Profile'
import AdminPanel from './pages/app/AdminPanel'
import Terms from './pages/Terms'
import Privacy from './pages/Privacy'
import NotFound from './pages/NotFound'
import ServerError from './pages/ServerError'
import Passcode from './pages/Passcode'

function ScrollToTop() {
  const { pathname } = useLocation()
  useEffect(() => {
    window.scrollTo(0, 0)
  }, [pathname])
  return null
}

function ScrollToHash() {
  const location = useLocation()
  useEffect(() => {
    if (!location.hash) {
      return
    }
    const id = location.hash.replace('#', '')
    const scroll = () => {
      const el = document.getElementById(id)
      if (el) {
        el.scrollIntoView({ behavior: 'smooth', block: 'start' })
      }
    }
    // Try immediately and once more on next tick to ensure target is mounted
    scroll()
    const t = setTimeout(scroll, 0)
    return () => clearTimeout(t)
  }, [location.hash])
  return null
}

export default function App() {
  const { user, logout } = useAuth()
  const { startLoading, stopLoading } = useLoading()
  const [menuOpen, setMenuOpen] = useState(false)
  const location = useLocation()
  
  useEffect(() => {
    setLoadingCallbacks(startLoading, stopLoading)
    // Signal to pre-renderer that the page is ready
    const timer = setTimeout(() => {
      document.dispatchEvent(new Event('render-event'))
    }, 100);
    return () => clearTimeout(timer);
  }, [startLoading, stopLoading, location.pathname])

  const isApp = location.pathname.startsWith('/app')
  const showHeader = !isApp && !['/login', '/register', '/forgot-password', '/reset-password'].includes(location.pathname)

  // const [currentYear, setCurrentYear] = useState<number | null>(null);
  //
  // useEffect(() => {
  //   setCurrentYear(new Date().getFullYear());
  // }, []);

  return (
    <div className="min-h-full w-full flex flex-col bg-slate-950 text-slate-200">
      <ProgressBar />
      {showHeader && (
      <header className="border-b border-slate-800 bg-slate-900/80 backdrop-blur fixed w-full top-0 z-50">
        <div className="container flex items-center justify-between py-4 relative">
          <Link to="/" className="flex items-center gap-3 text-lg font-bold text-white hover:opacity-90 transition-opacity">
            <span className="relative flex h-3 w-3">
               <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-indigo-400 opacity-75"></span>
               <span className="relative inline-flex rounded-full h-3 w-3 bg-indigo-500"></span>
            </span>
            Port Buddy
          </Link>
          <nav className="flex items-center gap-4 lg:gap-8 text-sm font-medium">
            <div className="hidden lg:flex items-center gap-8">
              <a 
                href="https://github.com/amak-tech/port-buddy" 
                target="_blank" 
                rel="noopener noreferrer"
                className="text-slate-400 hover:text-white transition-colors flex items-center gap-1.5"
              >
                <svg className="w-4 h-4 fill-current" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                  <path d="M12 .297c-6.63 0-12 5.373-12 12 0 5.303 3.438 9.8 8.205 11.385.6.113.82-.258.82-.577 0-.285-.01-1.04-.015-2.04-3.338.724-4.042-1.61-4.042-1.61C4.422 18.07 3.633 17.7 3.633 17.7c-1.087-.744.084-.729.084-.729 1.205.084 1.838 1.236 1.838 1.236 1.07 1.835 2.809 1.305 3.495.998.108-.776.417-1.305.76-1.605-2.665-.3-5.466-1.332-5.466-5.93 0-1.31.465-2.38 1.235-3.22-.135-.303-.54-1.523.105-3.176 0 0 1.005-.322 3.3 1.23.96-.267 1.98-.399 3-.405 1.02.006 2.04.138 3 .405 2.28-1.552 3.285-1.23 3.285-1.23.645 1.653.24 2.873.12 3.176.765.84 1.23 1.91 1.23 3.22 0 4.61-2.805 5.625-5.475 5.92.42.36.81 1.096.81 2.22 0 1.606-.015 2.896-.015 3.286 0 .315.21.69.825.57C20.565 22.092 24 17.592 24 12.297c0-6.627-5.373-12-12-12"/>
                </svg>
                GitHub
              </a>
              <Link to="/install" className="text-slate-400 hover:text-white transition-colors" aria-label="Installation instructions">Installation</Link>
              <Link to="/docs" className="text-slate-400 hover:text-white transition-colors" aria-label="Documentation">Docs</Link>
              <Link to="/#pricing" className="text-slate-400 hover:text-white transition-colors" aria-label="View pricing">Pricing</Link>
            </div>
            
            <div className="relative">
              <button
                className="lg:hidden w-10 h-10 inline-flex items-center justify-center rounded-lg border border-slate-700 text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
                aria-label="Open menu"
                aria-expanded={menuOpen}
                onClick={() => setMenuOpen((v) => !v)}
              >
                <span className="sr-only">Menu</span>
                {menuOpen ? <XMarkIcon className="h-6 w-6" /> : <Bars3Icon className="h-6 w-6" />}
              </button>

              {!user && (
                <Link 
                  to="/login" 
                  className="hidden lg:inline-block bg-indigo-600 hover:bg-indigo-500 text-white px-5 py-2 rounded-lg transition-all shadow-lg shadow-indigo-500/20"
                >
                  Login
                </Link>
              )}

              {user && (
                <button
                  className="hidden lg:inline-flex w-10 h-10 items-center justify-center rounded-lg border border-slate-700 text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
                  aria-label="Open menu"
                  aria-expanded={menuOpen}
                  onClick={() => setMenuOpen((v) => !v)}
                >
                  <span className="sr-only">Menu</span>
                  {menuOpen ? <XMarkIcon className="h-6 w-6" /> : <Bars3Icon className="h-6 w-6" />}
                </button>
              )}

              {menuOpen && (
                <div className="absolute right-0 mt-3 w-64 rounded-xl border border-slate-800 bg-slate-900 shadow-2xl p-2 z-50">
                  <div className="lg:hidden">
                    <Link to="/install" className="block px-4 py-2.5 rounded-lg text-slate-300 hover:text-white hover:bg-slate-800 transition-colors" onClick={() => setMenuOpen(false)}>Installation</Link>
                    <Link to="/docs" className="block px-4 py-2.5 rounded-lg text-slate-300 hover:text-white hover:bg-slate-800 transition-colors" onClick={() => setMenuOpen(false)}>Documentation</Link>
                    <Link to="/#pricing" className="block px-4 py-2.5 rounded-lg text-slate-300 hover:text-white hover:bg-slate-800 transition-colors" onClick={() => setMenuOpen(false)}>Pricing</Link>
                    <div className="h-px bg-slate-800 my-2 mx-2"></div>
                  </div>
                  
                  {user ? (
                    <>
                      <Link to="/app" className="block px-4 py-2.5 rounded-lg text-slate-300 hover:text-white hover:bg-slate-800 transition-colors" onClick={() => setMenuOpen(false)}>Dashboard</Link>
                      <Link to="/app/settings" className="block px-4 py-2.5 rounded-lg text-slate-300 hover:text-white hover:bg-slate-800 transition-colors" onClick={() => setMenuOpen(false)}>Settings</Link>
                      <Link to="/app/billing" className="block px-4 py-2.5 rounded-lg text-slate-300 hover:text-white hover:bg-slate-800 transition-colors" onClick={() => setMenuOpen(false)}>Billing</Link>
                      <div className="h-px bg-slate-800 my-2 mx-2"></div>
                      <button className="block w-full text-left px-4 py-2.5 rounded-lg text-red-400 hover:text-red-300 hover:bg-red-500/10 transition-colors" onClick={() => { setMenuOpen(false); void logout() }}>Logout</button>
                    </>
                  ) : (
                    <Link to="/login" className="block px-4 py-2.5 rounded-lg text-indigo-400 font-medium hover:bg-indigo-500/10 transition-colors" onClick={() => setMenuOpen(false)}>Login</Link>
                  )}
                </div>
              )}
            </div>
          </nav>
        </div>
      </header>
      )}

      <main className={`flex-1 w-full ${showHeader ? 'pt-[73px]' : ''}`}>
        <Routes>
          <Route path="/" element={<Landing/>} />
          <Route path="/index" element={<Navigate to="/" replace />} />
          <Route path="/install" element={<Installation/>} />
          <Route path="/docs" element={<Docs/>} />
          <Route path="/login" element={<Login/>} />
          <Route path="/register" element={<Register/>} />
          <Route path="/forgot-password" element={<ForgotPassword/>} />
          <Route path="/reset-password" element={<ResetPassword/>} />
          <Route path="/auth/callback" element={<Login/>} />
          <Route path="/accept-invite" element={<AcceptInvite/>} />
          <Route path="/passcode" element={<Passcode/>} />
          <Route path="/terms" element={<Terms/>} />
          <Route path="/privacy" element={<Privacy/>} />
          {/* App area with sidebar layout */}
          <Route path="/app" element={<ProtectedRoute><AppLayout/></ProtectedRoute>}>
            <Route index element={<Tunnels/>} />
            <Route path="tokens" element={<Tokens/>} />
            <Route path="domains" element={<Domains/>} />
            <Route path="ports" element={<Ports/>} />
            <Route path="team" element={<Team/>} />
            <Route path="billing" element={<ProtectedRoute role="ACCOUNT_ADMIN"><Billing/></ProtectedRoute>} />
            <Route path="billing/success" element={<BillingSuccess/>} />
            <Route path="billing/cancel" element={<BillingCancel/>} />
            <Route path="settings" element={<ProtectedRoute role="ACCOUNT_ADMIN"><Settings/></ProtectedRoute>} />
            <Route path="profile" element={<Profile/>} />
            <Route path="admin" element={<ProtectedRoute role="ADMIN"><AdminPanel/></ProtectedRoute>} />
            {/* Unknown app routes redirect to dashboard */}
            <Route path="*" element={<Navigate to="/app" replace />} />
          </Route>
          {/* Backward-compat for old links */}
          <Route path="/app/subscription" element={<Navigate to="/app/billing" replace />} />
          {/* Global 404 */}
          <Route path="/500" element={<ServerError/>} />
          <Route path="*" element={<NotFound/>} />
        </Routes>
        <ScrollToTop />
        <ScrollToHash />
        <Outlet />
      </main>

      {showHeader && (
      <footer className="border-t border-slate-800 py-12 bg-slate-900 mt-auto">
        <div className="container flex flex-col md:flex-row items-center justify-between gap-6">
          <div className="flex items-center gap-2 text-slate-400 text-sm">
             <span>© 2026 Port Buddy. All rights reserved.</span>
          </div>
          <div className="flex flex-wrap justify-center gap-x-8 gap-y-4 text-sm font-medium">
            <a 
              href="https://github.com/amak-tech/port-buddy" 
              target="_blank" 
              rel="noopener noreferrer"
              className="text-slate-400 hover:text-indigo-400 transition-colors flex items-center gap-1.5"
            >
              <svg className="w-4 h-4 fill-current" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                <path d="M12 .297c-6.63 0-12 5.373-12 12 0 5.303 3.438 9.8 8.205 11.385.6.113.82-.258.82-.577 0-.285-.01-1.04-.015-2.04-3.338.724-4.042-1.61-4.042-1.61C4.422 18.07 3.633 17.7 3.633 17.7c-1.087-.744.084-.729.084-.729 1.205.084 1.838 1.236 1.838 1.236 1.07 1.835 2.809 1.305 3.495.998.108-.776.417-1.305.76-1.605-2.665-.3-5.466-1.332-5.466-5.93 0-1.31.465-2.38 1.235-3.22-.135-.303-.54-1.523.105-3.176 0 0 1.005-.322 3.3 1.23.96-.267 1.98-.399 3-.405 1.02.006 2.04.138 3 .405 2.28-1.552 3.285-1.23 3.285-1.23.645 1.653.24 2.873.12 3.176.765.84 1.23 1.91 1.23 3.22 0 4.61-2.805 5.625-5.475 5.92.42.36.81 1.096.81 2.22 0 1.606-.015 2.896-.015 3.286 0 .315.21.69.825.57C20.565 22.092 24 17.592 24 12.297c0-6.627-5.373-12-12-12"/>
              </svg>
              GitHub
            </a>
            <a href="/#pricing" className="text-slate-400 hover:text-indigo-400 transition-colors">Pricing</a>
            <a href="/#use-cases" className="text-slate-400 hover:text-indigo-400 transition-colors">Use Cases</a>
            <Link to="/docs" className="text-slate-400 hover:text-indigo-400 transition-colors">Documentation</Link>
            <Link to="/terms" className="text-slate-400 hover:text-indigo-400 transition-colors">Terms</Link>
            <Link to="/privacy" className="text-slate-400 hover:text-indigo-400 transition-colors">Privacy</Link>
          </div>
        </div>
      </footer>
      )}
    </div>
  )
}
