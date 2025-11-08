import { Link, Outlet, Route, Routes } from 'react-router-dom'
import { useState } from 'react'
import Landing from './pages/Landing'
import Installation from './pages/Installation'
import Dashboard from './pages/Dashboard'
import Login from './pages/Login'
import Profile from './pages/Profile'
import Subscription from './pages/Subscription'
import ProtectedRoute from './components/ProtectedRoute'
import { useAuth } from './auth/AuthContext'

export default function App() {
  const { user, logout } = useAuth()
  const [menuOpen, setMenuOpen] = useState(false)
  return (
    <div className="min-h-full flex flex-col">
      <header className="border-b border-white/10 bg-primary/60 backdrop-blur sticky top-0 z-10">
        <div className="container flex items-center justify-between py-3 relative">
          <Link to="/" className="flex items-center gap-2 text-xl font-bold">
            <span className="w-2.5 h-2.5 rounded-full bg-accent inline-block"></span>
            Port Buddy
          </Link>
          <nav className="flex items-center gap-6 text-sm">
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
                    <Link to="/app" className="block px-3 py-2 rounded hover:bg-white/5" onClick={() => setMenuOpen(false)}>Dashboard</Link>
                    <Link to="/app/profile" className="block px-3 py-2 rounded hover:bg-white/5" onClick={() => setMenuOpen(false)}>Profile</Link>
                    <Link to="/app/subscription" className="block px-3 py-2 rounded hover:bg-white/5" onClick={() => setMenuOpen(false)}>Subscription</Link>
                    <button className="block w-full text-left px-3 py-2 rounded hover:bg-white/5" onClick={() => { setMenuOpen(false); void logout() }}>Logout</button>
                  </div>
                )}
              </div>
            )}
          </nav>
        </div>
      </header>

      <main className="flex-1">
        <Routes>
          <Route path="/" element={<Landing/>} />
          <Route path="/install" element={<Installation/>} />
          <Route path="/login" element={<Login/>} />
          <Route path="/auth/callback" element={<Login/>} />

          <Route path="/app" element={<ProtectedRoute><Dashboard/></ProtectedRoute>} />
          <Route path="/app/profile" element={<ProtectedRoute><Profile/></ProtectedRoute>} />
          <Route path="/app/subscription" element={<ProtectedRoute><Subscription/></ProtectedRoute>} />
        </Routes>
        <Outlet />
      </main>

      <footer className="border-t border-white/10 py-8 mt-16">
        <div className="container text-sm text-white/60 flex items-center justify-between">
          <span>© {new Date().getFullYear()} Port Buddy</span>
          <div className="flex gap-4">
            <a href="#pricing">Pricing</a>
            <a href="#use-cases">Use Cases</a>
            <a href="#docs">Docs</a>
          </div>
        </div>
      </footer>
    </div>
  )
}
