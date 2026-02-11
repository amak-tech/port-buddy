import { Link, NavLink, Outlet } from 'react-router-dom'
import { ComponentType, SVGProps, useEffect, useState } from 'react'
import { useAuth } from '../auth/AuthContext'
import { PageHeaderProvider, usePageHeader } from './PageHeader'
import { apiJson } from '../lib/api'
import {
  AcademicCapIcon,
  ArrowsRightLeftIcon,
  ChevronUpDownIcon,
  Cog8ToothIcon,
  GlobeAltIcon,
  LinkIcon,
  LockClosedIcon,
  WalletIcon,
  UserGroupIcon,
  PowerIcon,
  ShieldCheckIcon,
  XMarkIcon,
  Bars3Icon,
} from '@heroicons/react/24/outline'

type UserAccount = {
  accountId: string
  accountName: string
  plan: string
  roles: string[]
  lastUsedAt: string
}

export default function AppLayout() {
  const { user, logout, switchAccount } = useAuth()
  const [accounts, setAccounts] = useState<UserAccount[]>([])
  const [showAccountSwitcher, setShowAccountSwitcher] = useState(false)
  const [isSidebarOpen, setIsSidebarOpen] = useState(false)

  useEffect(() => {
    if (user) {
      void apiJson<UserAccount[]>('/api/users/me/accounts').then(setAccounts)
    }
  }, [user])

  const otherAccounts = accounts.filter(a => a.accountId !== user?.accountId)

  return (
    <div className="min-h-screen w-full flex bg-primary-950">
      {/* Mobile Sidebar Overlay */}
      {isSidebarOpen && (
        <div 
          className="fixed inset-0 z-40 bg-primary-950/60 backdrop-blur-sm lg:hidden"
          onClick={() => setIsSidebarOpen(false)}
        />
      )}

      {/* Sidebar */}
      <aside className={`fixed top-0 left-0 h-screen w-64 border-r border-white/5 glass z-[60] transition-transform duration-300 lg:translate-x-0 ${
        isSidebarOpen ? 'translate-x-0' : '-translate-x-full'
      }`}>
        <div className="h-full flex flex-col">
          {/* Top app title (fixed at top) */}
          <div className="sticky top-0 z-10 border-b border-white/5 px-6 py-6 flex items-center justify-between">
            <Link to="/" className="flex items-center gap-3 text-xl font-black text-white hover:opacity-90 transition-opacity tracking-tighter">
              <span className="relative flex h-3 w-3">
                 <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-jb-blue opacity-75"></span>
                 <span className="relative inline-flex rounded-full h-3 w-3 bg-jb-blue"></span>
              </span>
              Port Buddy
            </Link>
            <button 
              className="lg:hidden p-2 -mr-2 text-slate-400 hover:text-white"
              onClick={() => setIsSidebarOpen(false)}
            >
              <XMarkIcon className="h-6 w-6" />
            </button>
          </div>

          {/* Account Switcher */}
          <div className="px-4 py-4 border-b border-white/5 relative">
            <button
              disabled={otherAccounts.length === 0}
              onClick={() => setShowAccountSwitcher(!showAccountSwitcher)}
              className={`w-full flex items-center justify-between gap-2 px-3 py-2.5 rounded-xl glass text-white transition-all border border-white/5 ${
                otherAccounts.length > 0 ? 'hover:bg-white/5 cursor-pointer' : 'cursor-default'
              }`}
            >
              <div className="flex flex-col items-start min-w-0">
                <span className="text-[10px] font-bold text-slate-500 uppercase tracking-widest">Account</span>
                <span className="text-sm font-bold truncate w-full text-left">
                  {user?.accountName || 'Select Account'}
                </span>
              </div>
              {otherAccounts.length > 0 && <ChevronUpDownIcon className="h-5 w-5 text-slate-400 shrink-0" />}
            </button>

            {showAccountSwitcher && (
              <div className="absolute top-full left-4 right-4 mt-1 z-50 bg-slate-800 border border-slate-700 rounded-lg shadow-xl overflow-hidden py-1">
                {otherAccounts.map(account => (
                  <button
                    key={account.accountId}
                    onClick={() => {
                      void switchAccount(account.accountId)
                      setShowAccountSwitcher(false)
                    }}
                    className="w-full text-left px-3 py-2 text-sm text-slate-300 hover:bg-slate-700 hover:text-white transition-colors"
                  >
                    {account.accountName}
                  </button>
                ))}
              </div>
            )}
          </div>

          {/* Nav list (scrollable middle) */}
          <nav className="flex-1 overflow-y-auto px-4 py-6 space-y-1">
            <SideLink to="/app" end label="Tunnels" Icon={ArrowsRightLeftIcon} onClick={() => setIsSidebarOpen(false)} />
            <SideLink to="/app/tokens" label="Access Tokens" Icon={LockClosedIcon} onClick={() => setIsSidebarOpen(false)} />
            <SideLink to="/app/domains" label="Domains" Icon={GlobeAltIcon} onClick={() => setIsSidebarOpen(false)} />
            <SideLink to="/app/ports" label="Port Reservations" Icon={LinkIcon} onClick={() => setIsSidebarOpen(false)} />
            <SideLink to="/app/team" label="Team" Icon={UserGroupIcon} onClick={() => setIsSidebarOpen(false)} />
            {(user?.roles?.includes('ACCOUNT_ADMIN')) && (
              <SideLink to="/app/billing" label="Billing" Icon={WalletIcon} onClick={() => setIsSidebarOpen(false)} />
            )}
            {user?.roles?.includes('ACCOUNT_ADMIN') && (
              <SideLink to="/app/settings" label="Settings" Icon={Cog8ToothIcon} onClick={() => setIsSidebarOpen(false)} />
            )}
            {user?.roles?.includes('ADMIN') && (
              <SideLink to="/app/admin" label="Admin Panel" Icon={ShieldCheckIcon} onClick={() => setIsSidebarOpen(false)} />
            )}
          </nav>

          {/* Bottom block (fixed at bottom) */}
          <div className="sticky bottom-0 z-10 border-t border-white/5 bg-primary-950 px-6 py-6">
            <div className="flex items-center justify-between gap-3 mb-6">
              <Link to="/docs" className="text-slate-400 hover:text-white text-sm inline-flex items-center gap-2 transition-colors">
                <AcademicCapIcon className="h-5 w-5" aria-hidden="true" />
                <span className="font-bold uppercase tracking-widest text-[10px]">Documentation</span>
              </Link>
            </div>
            <div className="flex items-center justify-between gap-3">
              <Link to="/app/profile" className="flex items-center gap-3 min-w-0 hover:opacity-80 transition-opacity" onClick={() => setIsSidebarOpen(false)}>
                {user?.avatarUrl ? (
                  <img src={user.avatarUrl} alt="avatar" className="w-10 h-10 rounded-xl border border-white/10" />
                ) : (
                  <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-jb-blue to-jb-purple flex items-center justify-center text-white text-sm font-black shadow-lg shadow-jb-blue/20">
                    {user?.name?.[0] || user?.email?.[0] || '?'}
                  </div>
                )}
                <div className="truncate">
                  <div className="text-sm font-bold text-white truncate">{user?.name || user?.email || 'Unknown user'}</div>
                  <div className="text-slate-500 text-[10px] font-mono truncate">{user?.email}</div>
                </div>
              </Link>
              <button
                type="button"
                aria-label="Logout"
                title="Logout"
                onClick={() => void logout()}
                className="p-2.5 rounded-xl text-slate-400 hover:text-white hover:bg-white/5 transition-all"
              >
                <PowerIcon className="h-5 w-5" aria-hidden="true" />
              </button>
            </div>
          </div>
        </div>
      </aside>

      {/* Main content */}
      <section className="flex-1 w-full min-w-0 flex flex-col min-h-0 lg:ml-64 bg-primary-950">
        <PageHeaderProvider>
          {/* Page Header (sticky at top) */}
          <div className="sticky top-0 z-10 border-b border-white/5 bg-primary-950/80 backdrop-blur px-6 lg:px-10 py-6 flex items-center gap-6">
            <button
              className="lg:hidden p-2 -ml-2 text-slate-400 hover:text-white transition-colors"
              onClick={() => setIsSidebarOpen(true)}
              aria-label="Open sidebar"
            >
              <Bars3Icon className="h-6 w-6" />
            </button>
            <HeaderTitle />
          </div>
          {/* Page body */}
          <div className="px-6 lg:px-10 py-10 flex-1 overflow-y-auto" data-scroll-root>
            {user?.blocked && (
              <div className="mb-8 rounded-xl bg-red-500/10 border border-red-500/20 p-6 flex items-start gap-4">
                <div className="p-3 rounded-lg bg-red-500/10 text-red-400">
                   <LockClosedIcon className="h-6 w-6" />
                </div>
                <div>
                  <h3 className="text-lg font-bold text-red-200">Account Blocked</h3>
                  <p className="mt-1 text-slate-300">
                    Your account is currently blocked. Access to services is restricted. 
                    Please contact support for assistance.
                  </p>
                </div>
              </div>
            )}
            <Outlet />
          </div>
        </PageHeaderProvider>
      </section>
    </div>
  )
}

type IconType = ComponentType<SVGProps<SVGSVGElement>>

function SideLink({ to, label, end = false, Icon, onClick }: { to: string, label: string, end?: boolean, Icon?: IconType, onClick?: () => void }) {
  return (
    <NavLink
      to={to}
      end={end}
      onClick={onClick}
      className={({ isActive }) => 
        `flex items-center gap-3 px-3 py-3 rounded-xl transition-all duration-200 ${
          isActive 
            ? 'bg-jb-blue/10 text-jb-blue font-bold shadow-[inset_4px_0_0_0_#33ccff]' 
            : 'text-slate-400 hover:text-slate-200 hover:bg-white/5'
        }`
      }
    >
      {Icon ? <Icon className="h-5 w-5" aria-hidden="true" /> : null}
      <span className="text-sm tracking-tight">{label}</span>
    </NavLink>
  )
}

function HeaderTitle() {
  const { title } = usePageHeader()
  return (
    <div className="text-2xl font-black text-white truncate tracking-tighter uppercase">{title}</div>
  )
}
