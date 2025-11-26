import { Link, Navigate, Outlet, Route, Routes, useLocation } from 'react-router-dom'
import { useEffect, useState } from 'react'
import Landing from './pages/Landing'
import Installation from './pages/Installation'
import Login from './pages/Login'
import Subscription from './pages/Subscription'
import ProtectedRoute from './components/ProtectedRoute'
import { useAuth } from './auth/AuthContext'
import AppLayout from './components/AppLayout'
import Tunnels from './pages/app/Tunnels'
import Tokens from './pages/app/Tokens'
import Domains from './pages/app/Domains'
import Settings from './pages/app/Settings'

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
  const [menuOpen, setMenuOpen] = useState(false)
  const location = useLocation()
  const isApp = location.pathname.startsWith('/app')
  return (
    <div className="min-h-full flex flex-col">
      {!isApp && (
      <header className="border-b border-white/10 bg-primary/60 backdrop-blur sticky top-0 z-10">
        <div className="container flex items-center justify-between py-3 relative">
          <Link to="/" className="flex items-center gap-2 text-xl font-bold">
            <span className="w-2.5 h-2.5 rounded-full bg-accent inline-block"></span>
            Port Buddy
          </Link>
          <nav className="flex items-center gap-6 text-sm">
            <Link to="/install" className="hover:text-white" aria-label="Installation instructions">Installation</Link>
            <Link to="/#pricing" className="hover:text-white" aria-label="View pricing">Pricing</Link>
            {!user ? (
              // Only Login button when not authenticated
              <Link to="/login" className="btn">Login</Link>
            ) : (
              // Authenticated: show hamburger menu
              <div className="relative">
                <button
                  className="w-10 h-10 inline-flex items-center justify-center rounded-md border border-white/10 hover:bg-white/5"
                  aria-label="Open menu"
                  aria-expanded={menuOpen}
                  onClick={() => setMenuOpen((v) => !v)}
                >
                  <span className="sr-only">Menu</span>
                  <span aria-hidden="true" className="flex flex-col items-center justify-center gap-1.5">
                    <span className="block w-6 h-0.5 bg-white"></span>
                    <span className="block w-6 h-0.5 bg-white"></span>
                    <span className="block w-6 h-0.5 bg-white"></span>
                  </span>
                </button>
                {menuOpen && (
                  <div className="absolute right-0 mt-2 w-56 rounded-md border border-white/10 bg-primary/90 backdrop-blur shadow-lg p-2 z-20">
                    <Link to="/app" className="block px-3 py-2 rounded hover:bg-white/5" onClick={() => setMenuOpen(false)}>Open App</Link>
                    <Link to="/app/settings" className="block px-3 py-2 rounded hover:bg-white/5" onClick={() => setMenuOpen(false)}>Settings</Link>
                    <Link to="/app/billing" className="block px-3 py-2 rounded hover:bg-white/5" onClick={() => setMenuOpen(false)}>Billing</Link>
                    <button className="block w-full text-left px-3 py-2 rounded hover:bg-white/5" onClick={() => { setMenuOpen(false); void logout() }}>Logout</button>
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
          <Route path="/auth/callback" element={<Login/>} />
          {/* App area with sidebar layout */}
          <Route path="/app" element={<ProtectedRoute><AppLayout/></ProtectedRoute>}>
            <Route index element={<Tunnels/>} />
            <Route path="tokens" element={<Tokens/>} />
            <Route path="domains" element={<Domains/>} />
            <Route path="billing" element={<Subscription/>} />
            <Route path="settings" element={<Settings/>} />
          </Route>
          {/* Backward-compat for old links */}
          <Route path="/app/profile" element={<Navigate to="/app/settings" replace />} />
          <Route path="/app/subscription" element={<Navigate to="/app/billing" replace />} />
        </Routes>
        <ScrollToHash />
        <Outlet />
      </main>

      {!isApp && (
      <footer className="border-t border-white/10 py-8 mt-16">
        <div className="container text-sm text-white/60 flex items-center justify-between">
          <span>© {new Date().getFullYear()} Port Buddy</span>
          <div className="flex gap-4">
            <a href="/#pricing">Pricing</a>
            <a href="/#use-cases">Use Cases</a>
            <a href="/#docs">Docs</a>
          </div>
        </div>
      </footer>
      )}
    </div>
  )
}
