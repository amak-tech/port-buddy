import { Link, NavLink, Outlet } from 'react-router-dom'
import type { ComponentType, SVGProps } from 'react'
import { useAuth } from '../auth/AuthContext'
import { PageHeaderProvider, usePageHeader } from './PageHeader'
import {
  AcademicCapIcon,
  ArrowsRightLeftIcon,
  Cog8ToothIcon,
  GlobeAltIcon,
  LinkIcon,
  LockClosedIcon,
  WalletIcon,
  UserGroupIcon,
  PowerIcon,
  ShieldCheckIcon,
} from '@heroicons/react/24/outline'

export default function AppLayout() {
  const { user, logout } = useAuth()

  return (
    <div className="min-h-screen flex bg-slate-950">
      {/* Sidebar */}
      <aside className="fixed top-0 left-0 h-screen w-64 border-r border-slate-800 bg-slate-900">
        <div className="h-full flex flex-col">
          {/* Top app title (fixed at top) */}
          <div className="sticky top-0 z-10 border-b border-slate-800 bg-slate-900 px-6 py-5">
            <Link to="/" className="flex items-center gap-3 text-lg font-bold text-white hover:opacity-90 transition-opacity">
              <span className="relative flex h-3 w-3">
                 <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-indigo-400 opacity-75"></span>
                 <span className="relative inline-flex rounded-full h-3 w-3 bg-indigo-500"></span>
              </span>
              Port Buddy
            </Link>
          </div>

          {/* Nav list (scrollable middle) */}
          <nav className="flex-1 overflow-y-auto px-4 py-6 space-y-1">
            <SideLink to="/app" end label="Tunnels" Icon={ArrowsRightLeftIcon} />
            <SideLink to="/app/tokens" label="Access Tokens" Icon={LockClosedIcon} />
            <SideLink to="/app/domains" label="Domains" Icon={GlobeAltIcon} />
            <SideLink to="/app/ports" label="Port Reservations" Icon={LinkIcon} />
            <SideLink to="/app/team" label="Team" Icon={UserGroupIcon} />
            <SideLink to="/app/billing" label="Billing" Icon={WalletIcon} />
            <SideLink to="/app/settings" label="Settings" Icon={Cog8ToothIcon} />
            {user?.roles?.includes('ADMIN') && (
              <SideLink to="/app/admin" label="Admin Panel" Icon={ShieldCheckIcon} />
            )}
          </nav>

          {/* Bottom block (fixed at bottom) */}
          <div className="sticky bottom-0 z-10 border-t border-slate-800 bg-slate-900 px-6 py-5">
            <div className="flex items-center justify-between gap-3 mb-4">
              <a href="/#docs" className="text-slate-400 hover:text-white text-sm inline-flex items-center gap-2 transition-colors">
                <AcademicCapIcon className="h-5 w-5" aria-hidden="true" />
                <span>Documentation</span>
              </a>
            </div>
            <div className="flex items-center justify-between gap-3">
              <div className="flex items-center gap-3 min-w-0">
                {user?.avatarUrl ? (
                  <img src={user.avatarUrl} alt="avatar" className="w-9 h-9 rounded-full border border-slate-700" />
                ) : (
                  <div className="w-9 h-9 rounded-full bg-indigo-600 flex items-center justify-center text-white text-sm font-bold shadow-lg shadow-indigo-500/20">
                    {user?.name?.[0] || user?.email?.[0] || '?'}
                  </div>
                )}
                <div className="truncate">
                  <div className="text-sm font-medium text-white truncate">{user?.name || user?.email || 'Unknown user'}</div>
                  <div className="text-slate-500 text-xs truncate">{user?.email}</div>
                </div>
              </div>
              <button
                type="button"
                aria-label="Logout"
                title="Logout"
                onClick={() => void logout()}
                className="p-2 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-all"
              >
                <PowerIcon className="h-5 w-5" aria-hidden="true" />
              </button>
            </div>
          </div>
        </div>
      </aside>

      {/* Main content */}
      <section className="flex-1 min-w-0 flex flex-col min-h-0 ml-64 bg-slate-950">
        <PageHeaderProvider>
          {/* Page Header (sticky at top) */}
          <div className="sticky top-0 z-10 border-b border-slate-800 bg-slate-950/80 backdrop-blur px-8 py-5">
            <HeaderTitle />
          </div>
          {/* Page body */}
          <div className="px-8 py-8 flex-1 overflow-y-auto" data-scroll-root>
            <Outlet />
          </div>
        </PageHeaderProvider>
      </section>
    </div>
  )
}

type IconType = ComponentType<SVGProps<SVGSVGElement>>

function SideLink({ to, label, end = false, Icon }: { to: string, label: string, end?: boolean, Icon?: IconType }) {
  return (
    <NavLink
      to={to}
      end={end}
      className={({ isActive }) => 
        `flex items-center gap-3 px-3 py-2.5 rounded-lg transition-all duration-200 ${
          isActive 
            ? 'bg-indigo-500/10 text-indigo-400 font-medium shadow-[inset_3px_0_0_0_rgb(99,102,241)]' 
            : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/50'
        }`
      }
    >
      {Icon ? <Icon className={`h-5 w-5 ${end ? '' : ''}`} aria-hidden="true" /> : null}
      <span>{label}</span>
    </NavLink>
  )
}

function HeaderTitle() {
  const { title } = usePageHeader()
  return (
    <div className="text-xl font-bold text-white truncate tracking-tight">{title}</div>
  )
}
