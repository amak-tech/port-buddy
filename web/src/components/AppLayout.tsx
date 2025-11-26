import { NavLink, Outlet } from 'react-router-dom'
import type { ComponentType, SVGProps } from 'react'
import { useAuth } from '../auth/AuthContext'
import { PageHeaderProvider, usePageHeader } from './PageHeader'
import {
  AcademicCapIcon,
  ArrowsRightLeftIcon,
  Cog8ToothIcon,
  GlobeAltIcon,
  LockClosedIcon,
  WalletIcon,
} from '@heroicons/react/24/outline'

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
            <SideLink to="/app" end label="Tunnels" Icon={ArrowsRightLeftIcon} />
            <SideLink to="/app/tokens" label="Access Tokens" Icon={LockClosedIcon} />
            <SideLink to="/app/domains" label="Domains" Icon={GlobeAltIcon} />
            <SideLink to="/app/billing" label="Billing" Icon={WalletIcon} />
            <SideLink to="/app/settings" label="Settings" Icon={Cog8ToothIcon} />
          </nav>

          {/* Bottom block (fixed at bottom) */}
          <div className="sticky bottom-0 z-10 border-t border-white/10 bg-black/30 px-4 py-4">
            <div className="flex items-center justify-between gap-3">
              <a href="/#docs" className="text-white/80 hover:text-white text-sm inline-flex items-center gap-2">
                <AcademicCapIcon className="h-5 w-5" aria-hidden="true" />
                <span>Documentation</span>
              </a>
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
        <PageHeaderProvider>
          {/* Page Header (sticky at top) — same height as sidebar header (py-4) */}
          <div className="sticky top-0 z-10 border-b border-white/10 bg-black/30 px-6 py-4">
            <HeaderTitle />
          </div>
          {/* Page body */}
          <div className="px-6 py-8">
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
      className={({ isActive }) => `flex items-center gap-2 px-3 py-2 rounded-md hover:bg-white/5 ${isActive ? 'bg-white/10 text-white' : 'text-white/80'}`}
    >
      {Icon ? <Icon className="h-5 w-5" aria-hidden="true" /> : null}
      <span>{label}</span>
    </NavLink>
  )
}

function HeaderTitle() {
  const { title } = usePageHeader()
  return (
    <div className="text-lg font-semibold truncate">{title}</div>
  )
}
