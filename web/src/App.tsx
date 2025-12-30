import { Link, Navigate, Outlet, Route, Routes, useLocation } from 'react-router-dom'
import { useEffect, useState } from 'react'
import Landing from './pages/Landing'
import Installation from './pages/Installation'
import Login from './pages/Login'
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
import Ports from './pages/app/Ports'
import AdminPanel from './pages/app/AdminPanel'
import NotFound from './pages/NotFound'
import ServerError from './pages/ServerError'
import Passcode from './pages/Passcode'

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
  }, [startLoading, stopLoading])

  const isApp = location.pathname.startsWith('/app')
  const showHeader = !isApp && !['/login', '/forgot-password', '/reset-password'].includes(location.pathname)

  return (
    <div className="min-h-full flex flex-col bg-slate-950 text-slate-200">
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
          <nav className="flex items-center gap-8 text-sm font-medium">
            <Link to="/install" className="text-slate-400 hover:text-white transition-colors" aria-label="Installation instructions">Installation</Link>
            <Link to="/#pricing" className="text-slate-400 hover:text-white transition-colors" aria-label="View pricing">Pricing</Link>
            {!user ? (
              // Only Login button when not authenticated
              <Link 
                to="/login" 
                className="bg-indigo-600 hover:bg-indigo-500 text-white px-5 py-2 rounded-lg transition-all shadow-lg shadow-indigo-500/20"
              >
                Login
              </Link>
            ) : (
              // Authenticated: show hamburger menu
              <div className="relative">
                <button
                  className="w-10 h-10 inline-flex items-center justify-center rounded-lg border border-slate-700 text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
                  aria-label="Open menu"
                  aria-expanded={menuOpen}
                  onClick={() => setMenuOpen((v) => !v)}
                >
                  <span className="sr-only">Menu</span>
                  <span aria-hidden="true" className="flex flex-col items-center justify-center gap-1.5">
                    <span className="block w-5 h-0.5 bg-current"></span>
                    <span className="block w-5 h-0.5 bg-current"></span>
                    <span className="block w-5 h-0.5 bg-current"></span>
                  </span>
                </button>
                {menuOpen && (
                  <div className="absolute right-0 mt-3 w-56 rounded-xl border border-slate-800 bg-slate-900 shadow-2xl p-2 z-50">
                    <Link to="/app" className="block px-4 py-2.5 rounded-lg text-slate-300 hover:text-white hover:bg-slate-800 transition-colors" onClick={() => setMenuOpen(false)}>Dashboard</Link>
                    <Link to="/app/settings" className="block px-4 py-2.5 rounded-lg text-slate-300 hover:text-white hover:bg-slate-800 transition-colors" onClick={() => setMenuOpen(false)}>Settings</Link>
                    <Link to="/app/billing" className="block px-4 py-2.5 rounded-lg text-slate-300 hover:text-white hover:bg-slate-800 transition-colors" onClick={() => setMenuOpen(false)}>Billing</Link>
                    <div className="h-px bg-slate-800 my-2 mx-2"></div>
                    <button className="block w-full text-left px-4 py-2.5 rounded-lg text-red-400 hover:text-red-300 hover:bg-red-500/10 transition-colors" onClick={() => { setMenuOpen(false); void logout() }}>Logout</button>
                  </div>
                )}
              </div>
            )}
          </nav>
        </div>
      </header>
      )}

      <main className="flex-1">
        <Routes>
          <Route path="/" element={<Landing/>} />
          <Route path="/install" element={<Installation/>} />
          <Route path="/login" element={<Login/>} />
          <Route path="/forgot-password" element={<ForgotPassword/>} />
          <Route path="/reset-password" element={<ResetPassword/>} />
          <Route path="/auth/callback" element={<Login/>} />
          <Route path="/passcode" element={<Passcode/>} />
          {/* App area with sidebar layout */}
          <Route path="/app" element={<ProtectedRoute><AppLayout/></ProtectedRoute>}>
            <Route index element={<Tunnels/>} />
            <Route path="tokens" element={<Tokens/>} />
            <Route path="domains" element={<Domains/>} />
            <Route path="ports" element={<Ports/>} />
            <Route path="billing" element={<Billing/>} />
            <Route path="billing/success" element={<BillingSuccess/>} />
            <Route path="billing/cancel" element={<BillingCancel/>} />
            <Route path="settings" element={<Settings/>} />
            <Route path="admin" element={<ProtectedRoute role="ADMIN"><AdminPanel/></ProtectedRoute>} />
            {/* Unknown app routes redirect to dashboard */}
            <Route path="*" element={<Navigate to="/app" replace />} />
          </Route>
          {/* Backward-compat for old links */}
          <Route path="/app/profile" element={<Navigate to="/app/settings" replace />} />
          <Route path="/app/subscription" element={<Navigate to="/app/billing" replace />} />
          {/* Global 404 */}
          <Route path="/500" element={<ServerError/>} />
          <Route path="*" element={<NotFound/>} />
        </Routes>
        <ScrollToHash />
        <Outlet />
      </main>

      {showHeader && (
      <footer className="border-t border-slate-800 py-12 bg-slate-900 mt-auto">
        <div className="container flex flex-col md:flex-row items-center justify-between gap-6">
          <div className="flex items-center gap-2 text-slate-400 text-sm">
             <span>© {new Date().getFullYear()} Port Buddy. All rights reserved.</span>
          </div>
          <div className="flex gap-8 text-sm font-medium">
            <a href="/#pricing" className="text-slate-400 hover:text-indigo-400 transition-colors">Pricing</a>
            <a href="/#use-cases" className="text-slate-400 hover:text-indigo-400 transition-colors">Use Cases</a>
            <a href="/#docs" className="text-slate-400 hover:text-indigo-400 transition-colors">Documentation</a>
          </div>
        </div>
      </footer>
      )}
    </div>
  )
}
