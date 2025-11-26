import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export default function AppLayout() {
  const { user } = useAuth()

  return (
    <div className="min-h-screen flex bg-primary">
      {/* Sidebar */}
      <aside className="w-64 shrink-0 border-r border-white/10 bg-black/20 backdrop-blur">
        <div className="h-full flex flex-col">
          {/* Top app title (fixed at top) */}
          <div className="sticky top-0 z-10 border-b border-white/10 bg-black/30 px-4 py-4">
            <div className="flex items-center gap-2 text-lg font-bold">
              <span className="w-2.5 h-2.5 rounded-full bg-accent inline-block"></span>
              Port Buddy
            </div>
          </div>

          {/* Nav list (scrollable middle) */}
          <nav className="flex-1 overflow-y-auto px-2 py-3 text-sm">
            <SideLink to="/app" end label="Tunnels" />
            <SideLink to="/app/tokens" label="Access Tokens" />
            <SideLink to="/app/domains" label="Domains" />
            <SideLink to="/app/billing" label="Billing" />
            <SideLink to="/app/settings" label="Settings" />
          </nav>

          {/* Bottom block (fixed at bottom) */}
          <div className="sticky bottom-0 z-10 border-t border-white/10 bg-black/30 px-4 py-4">
            <div className="flex items-center justify-between gap-3">
              <a href="/#docs" className="text-white/80 hover:text-white text-sm">Documentation</a>
            </div>
            <div className="mt-3 flex items-center gap-3">
              {user?.avatarUrl ? (
                <img src={user.avatarUrl} alt="avatar" className="w-8 h-8 rounded-full border border-white/10" />
              ) : (
                <div className="w-8 h-8 rounded-full bg-white/10 grid place-items-center text-white/60 text-sm">
                  {user?.name?.[0] || user?.email?.[0] || '?'}
                </div>
              )}
              <div className="truncate">
                <div className="text-sm font-medium truncate">{user?.name || user?.email || 'Unknown user'}</div>
                <div className="text-white/50 text-xs truncate">{user?.email}</div>
              </div>
            </div>
          </div>
        </div>
      </aside>

      {/* Main content */}
      <section className="flex-1 min-w-0">
        <div className="px-6 py-8">
          <Outlet />
        </div>
      </section>
    </div>
  )
}

function SideLink({ to, label, end = false }: { to: string, label: string, end?: boolean }) {
  return (
    <NavLink
      to={to}
      end={end}
      className={({ isActive }) => `flex items-center gap-2 px-3 py-2 rounded-md hover:bg-white/5 ${isActive ? 'bg-white/10 text-white' : 'text-white/80'}`}
    >
      <span>{label}</span>
    </NavLink>
  )
}
