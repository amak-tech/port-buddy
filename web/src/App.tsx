import { Link, Navigate, Outlet, Route, Routes, useLocation } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { Bars3Icon, ChevronDownIcon, XMarkIcon } from '@heroicons/react/24/outline'
import Landing from './pages/Landing'
import Installation from './pages/Installation'
import DocsLayout from './pages/docs/DocsLayout'
import DocsOverview from './pages/docs/DocsOverview'
import MinecraftGuide from './pages/docs/guides/MinecraftGuide'
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
import Contacts from './pages/Contacts'
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
  const [communityOpen, setCommunityOpen] = useState(false)
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
              
              <div className="relative group">
                <button 
                  className="flex items-center gap-1 text-slate-400 hover:text-white transition-colors"
                  onMouseEnter={() => setCommunityOpen(true)}
                  onMouseLeave={() => setCommunityOpen(false)}
                >
                  Community
                  <ChevronDownIcon className={`w-3.5 h-3.5 transition-transform duration-200 ${communityOpen ? 'rotate-180' : ''}`} />
                </button>
                
                {communityOpen && (
                  <div 
                    className="absolute left-0 mt-0 w-48 pt-2 z-50"
                    onMouseEnter={() => setCommunityOpen(true)}
                    onMouseLeave={() => setCommunityOpen(false)}
                  >
                    <div className="bg-slate-900 border border-slate-800 rounded-xl shadow-2xl p-1.5 overflow-hidden">
                      <a 
                        href="https://discord.gg/RCT82A6T" 
                        target="_blank" 
                        rel="noopener noreferrer"
                        className="flex items-center gap-3 px-3 py-2 rounded-lg text-slate-300 hover:text-white hover:bg-slate-800 transition-colors"
                      >
                        <svg className="w-4 h-4 fill-current" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                          <path d="M20.317 4.37a19.791 19.791 0 0 0-4.885-1.515.074.074 0 0 0-.079.037c-.21.375-.444.864-.608 1.25a18.27 18.27 0 0 0-5.487 0 12.64 12.64 0 0 0-.617-1.25.077.077 0 0 0-.079-.037A19.736 19.736 0 0 0 3.677 4.37a.07.07 0 0 0-.032.027C.533 9.046-.32 13.58.099 18.057a.082.082 0 0 0 .031.057 19.9 19.9 0 0 0 5.993 3.03.078.078 0 0 0 .084-.028 14.09 14.09 0 0 0 1.226-1.994.076.076 0 0 0-.041-.106 13.107 13.107 0 0 1-1.872-.892.077.077 0 0 1-.008-.128 10.2 10.2 0 0 0 .372-.292.074.074 0 0 1 .077-.01c3.928 1.793 8.18 1.793 12.062 0a.074.074 0 0 1 .078.01c.12.098.246.198.373.292a.077.077 0 0 1-.006.127 12.299 12.299 0 0 1-1.873.892.077.077 0 0 0-.041.107c.36.698.772 1.362 1.225 1.993a.076.076 0 0 0 .084.028 19.839 19.839 0 0 0 6.002-3.03.077.077 0 0 0 .032-.054c.5-5.177-.838-9.674-3.549-13.66a.061.061 0 0 0-.031-.03zM8.02 15.33c-1.183 0-2.157-1.085-2.157-2.419 0-1.333.955-2.419 2.157-2.419 1.21 0 2.176 1.096 2.157 2.42 0 1.333-.956 2.418-2.157 2.418zm7.975 0c-1.183 0-2.157-1.085-2.157-2.419 0-1.333.955-2.419 2.157-2.419 1.21 0 2.176 1.096 2.157 2.42 0 1.333-.946 2.418-2.157 2.418z"/>
                        </svg>
                        Discord
                      </a>
                      <a 
                        href="https://t.me/portbuddy" 
                        target="_blank" 
                        rel="noopener noreferrer"
                        className="flex items-center gap-3 px-3 py-2 rounded-lg text-slate-300 hover:text-white hover:bg-slate-800 transition-colors"
                      >
                        <svg className="w-4 h-4 fill-current" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                          <path d="M11.944 0C5.346 0 0 5.346 0 11.944s5.346 11.944 11.944 11.944 11.944-5.346 11.944-11.944S18.542 0 11.944 0zm5.203 7.847c-.16 1.497-.859 5.635-1.215 7.541-.15.807-.444 1.077-.731 1.104-.623.056-1.097-.413-1.701-.81-.944-.621-1.477-1.008-2.391-1.61-.952-.621-.295-.964.218-1.503.134-.14 2.463-2.259 2.508-2.449.006-.024.01-.115-.042-.162-.052-.047-.13-.031-.186-.019-.08.017-1.353.858-3.819 2.525-.361.248-.688.37-.98.361-.322-.01-.942-.185-1.403-.335-.565-.184-1.013-.281-.974-.593.02-.162.244-.33.673-.505 2.636-1.148 4.393-1.907 5.271-2.277 2.51-.83 3.03-.974 3.37-.98.075-.001.244.018.354.108.093.075.118.177.127.248.009.072.02.213.01.35z"/>
                        </svg>
                        Telegram
                      </a>
                    </div>
                  </div>
                )}
              </div>

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
                    <div className="h-px bg-slate-800 my-1 mx-2"></div>
                    <div className="px-4 py-2 text-xs font-semibold text-slate-500 uppercase tracking-wider">Community</div>
                    <a href="https://discord.gg/RCT82A6T" target="_blank" rel="noopener noreferrer" className="flex items-center gap-3 px-4 py-2.5 rounded-lg text-slate-300 hover:text-white hover:bg-slate-800 transition-colors" onClick={() => setMenuOpen(false)}>
                      <svg className="w-4 h-4 fill-current text-slate-400" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                        <path d="M20.317 4.37a19.791 19.791 0 0 0-4.885-1.515.074.074 0 0 0-.079.037c-.21.375-.444.864-.608 1.25a18.27 18.27 0 0 0-5.487 0 12.64 12.64 0 0 0-.617-1.25.077.077 0 0 0-.079-.037A19.736 19.736 0 0 0 3.677 4.37a.07.07 0 0 0-.032.027C.533 9.046-.32 13.58.099 18.057a.082.082 0 0 0 .031.057 19.9 19.9 0 0 0 5.993 3.03.078.078 0 0 0 .084-.028 14.09 14.09 0 0 0 1.226-1.994.076.076 0 0 0-.041-.106 13.107 13.107 0 0 1-1.872-.892.077.077 0 0 1-.008-.128 10.2 10.2 0 0 0 .372-.292.074.074 0 0 1 .077-.01c3.928 1.793 8.18 1.793 12.062 0a.074.074 0 0 1 .078.01c.12.098.246.198.373.292a.077.077 0 0 1-.006.127 12.299 12.299 0 0 1-1.873.892.077.077 0 0 0-.041.107c.36.698.772 1.362 1.225 1.993a.076.076 0 0 0 .084.028 19.839 19.839 0 0 0 6.002-3.03.077.077 0 0 0 .032-.054c.5-5.177-.838-9.674-3.549-13.66a.061.061 0 0 0-.031-.03zM8.02 15.33c-1.183 0-2.157-1.085-2.157-2.419 0-1.333.955-2.419 2.157-2.419 1.21 0 2.176 1.096 2.157 2.42 0 1.333-.956 2.418-2.157 2.418zm7.975 0c-1.183 0-2.157-1.085-2.157-2.419 0-1.333.955-2.419 2.157-2.419 1.21 0 2.176 1.096 2.157 2.42 0 1.333-.946 2.418-2.157 2.418z"/>
                      </svg>
                      Discord
                    </a>
                    <a href="https://t.me/portbuddy" target="_blank" rel="noopener noreferrer" className="flex items-center gap-3 px-4 py-2.5 rounded-lg text-slate-300 hover:text-white hover:bg-slate-800 transition-colors" onClick={() => setMenuOpen(false)}>
                      <svg className="w-4 h-4 fill-current text-slate-400" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                        <path d="M11.944 0C5.346 0 0 5.346 0 11.944s5.346 11.944 11.944 11.944 11.944-5.346 11.944-11.944S18.542 0 11.944 0zm5.203 7.847c-.16 1.497-.859 5.635-1.215 7.541-.15.807-.444 1.077-.731 1.104-.623.056-1.097-.413-1.701-.81-.944-.621-1.477-1.008-2.391-1.61-.952-.621-.295-.964.218-1.503.134-.14 2.463-2.259 2.508-2.449.006-.024.01-.115-.042-.162-.052-.047-.13-.031-.186-.019-.08.017-1.353.858-3.819 2.525-.361.248-.688.37-.98.361-.322-.01-.942-.185-1.403-.335-.565-.184-1.013-.281-.974-.593.02-.162.244-.33.673-.505 2.636-1.148 4.393-1.907 5.271-2.277 2.51-.83 3.03-.974 3.37-.98.075-.001.244.018.354.108.093.075.118.177.127.248.009.072.02.213.01.35z"/>
                      </svg>
                      Telegram
                    </a>
                    <div className="h-px bg-slate-800 my-1 mx-2"></div>
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
          <Route path="/docs" element={<DocsLayout/>}>
            <Route index element={<DocsOverview/>} />
            <Route path="guides/minecraft-server" element={<MinecraftGuide/>} />
          </Route>
          <Route path="/login" element={<Login/>} />
          <Route path="/register" element={<Register/>} />
          <Route path="/forgot-password" element={<ForgotPassword/>} />
          <Route path="/reset-password" element={<ResetPassword/>} />
          <Route path="/auth/callback" element={<Login/>} />
          <Route path="/accept-invite" element={<AcceptInvite/>} />
          <Route path="/passcode" element={<Passcode/>} />
          <Route path="/terms" element={<Terms/>} />
          <Route path="/privacy" element={<Privacy/>} />
          <Route path="/contacts" element={<Contacts/>} />
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
            <div className="flex items-center gap-4">
              <a 
                href="https://discord.gg/RCT82A6T" 
                target="_blank" 
                rel="noopener noreferrer"
                className="text-slate-400 hover:text-[#5865F2] transition-colors"
                title="Discord"
              >
                <svg className="w-5 h-5 fill-current" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                  <path d="M20.317 4.37a19.791 19.791 0 0 0-4.885-1.515.074.074 0 0 0-.079.037c-.21.375-.444.864-.608 1.25a18.27 18.27 0 0 0-5.487 0 12.64 12.64 0 0 0-.617-1.25.077.077 0 0 0-.079-.037A19.736 19.736 0 0 0 3.677 4.37a.07.07 0 0 0-.032.027C.533 9.046-.32 13.58.099 18.057a.082.082 0 0 0 .031.057 19.9 19.9 0 0 0 5.993 3.03.078.078 0 0 0 .084-.028 14.09 14.09 0 0 0 1.226-1.994.076.076 0 0 0-.041-.106 13.107 13.107 0 0 1-1.872-.892.077.077 0 0 1-.008-.128 10.2 10.2 0 0 0 .372-.292.074.074 0 0 1 .077-.01c3.928 1.793 8.18 1.793 12.062 0a.074.074 0 0 1 .078.01c.12.098.246.198.373.292a.077.077 0 0 1-.006.127 12.299 12.299 0 0 1-1.873.892.077.077 0 0 0-.041.107c.36.698.772 1.362 1.225 1.993a.076.076 0 0 0 .084.028 19.839 19.839 0 0 0 6.002-3.03.077.077 0 0 0 .032-.054c.5-5.177-.838-9.674-3.549-13.66a.061.061 0 0 0-.031-.03zM8.02 15.33c-1.183 0-2.157-1.085-2.157-2.419 0-1.333.955-2.419 2.157-2.419 1.21 0 2.176 1.096 2.157 2.42 0 1.333-.956 2.418-2.157 2.418zm7.975 0c-1.183 0-2.157-1.085-2.157-2.419 0-1.333.955-2.419 2.157-2.419 1.21 0 2.176 1.096 2.157 2.42 0 1.333-.946 2.418-2.157 2.418z"/>
                </svg>
              </a>
              <a 
                href="https://t.me/portbuddy" 
                target="_blank" 
                rel="noopener noreferrer"
                className="text-slate-400 hover:text-[#24A1DE] transition-colors"
                title="Telegram"
              >
                <svg className="w-5 h-5 fill-current" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                  <path d="M11.944 0C5.346 0 0 5.346 0 11.944s5.346 11.944 11.944 11.944 11.944-5.346 11.944-11.944S18.542 0 11.944 0zm5.203 7.847c-.16 1.497-.859 5.635-1.215 7.541-.15.807-.444 1.077-.731 1.104-.623.056-1.097-.413-1.701-.81-.944-.621-1.477-1.008-2.391-1.61-.952-.621-.295-.964.218-1.503.134-.14 2.463-2.259 2.508-2.449.006-.024.01-.115-.042-.162-.052-.047-.13-.031-.186-.019-.08.017-1.353.858-3.819 2.525-.361.248-.688.37-.98.361-.322-.01-.942-.185-1.403-.335-.565-.184-1.013-.281-.974-.593.02-.162.244-.33.673-.505 2.636-1.148 4.393-1.907 5.271-2.277 2.51-.83 3.03-.974 3.37-.98.075-.001.244.018.354.108.093.075.118.177.127.248.009.072.02.213.01.35z"/>
                </svg>
              </a>
            </div>
            <a href="/#use-cases" className="text-slate-400 hover:text-indigo-400 transition-colors">Use Cases</a>
            <Link to="/docs" className="text-slate-400 hover:text-indigo-400 transition-colors">Documentation</Link>
            <Link to="/contacts" className="text-slate-400 hover:text-indigo-400 transition-colors">Contacts</Link>
            <Link to="/terms" className="text-slate-400 hover:text-indigo-400 transition-colors">Terms</Link>
            <Link to="/privacy" className="text-slate-400 hover:text-indigo-400 transition-colors">Privacy</Link>
          </div>
        </div>
      </footer>
      )}
    </div>
  )
}
