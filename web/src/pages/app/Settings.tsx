import { useEffect, useMemo, useState } from 'react'
import { useAuth } from '../../auth/AuthContext'
import { usePageTitle } from '../../components/PageHeader'
import { apiJson } from '../../lib/api'
import { UserCircleIcon, BuildingOfficeIcon } from '@heroicons/react/24/outline'

export default function Settings() {
  const { user, refresh } = useAuth()
  usePageTitle('Settings')
  const [accountName, setAccountName] = useState('')
  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  const [orig, setOrig] = useState<{ accountName: string, firstName: string, lastName: string } | null>(null)

  useEffect(() => {
    let cancelled = false
    async function load() {
      setLoading(true)
      setMessage(null)
      try {
        const details = await apiJson<{ user: { firstName?: string, lastName?: string }, account: { name: string } }>(
          '/api/users/me/details'
        )
        if (cancelled) return
        const first = details.user?.firstName || ''
        const last = details.user?.lastName || ''
        const acc = details.account?.name || ''
        setFirstName(first)
        setLastName(last)
        setAccountName(acc || defaultAccountName(user?.name, user?.email))
        setOrig({ accountName: acc, firstName: first, lastName: last })
      } catch {
        // Fallback to best-effort defaults if backend not available
        const defAcc = defaultAccountName(user?.name, user?.email)
        if (!firstName && !lastName && (user?.name || '')) {
          const parts = (user?.name || '').trim().split(/\s+/)
          if (parts.length > 0) setFirstName(parts[0])
          if (parts.length > 1) setLastName(parts.slice(1).join(' '))
        }
        if (!accountName) setAccountName(defAcc)
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    void load()
    return () => { cancelled = true }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.id])

  function defaultAccountName(name?: string, email?: string): string {
    const base = (name && name.trim()) || (email ? email.split('@')[0] : '')
    return base ? `${base}'s account` : 'My account'
  }

  const isChanged = useMemo(() => {
    if (!orig) return true
    return (
      accountName.trim() !== (orig.accountName || '').trim() ||
      (firstName || '').trim() !== (orig.firstName || '').trim() ||
      (lastName || '').trim() !== (orig.lastName || '').trim()
    )
  }, [orig, accountName, firstName, lastName])

  async function onSave() {
    setSaving(true)
    setMessage(null)
    try {
      // Update account name if changed
      if (!orig || accountName.trim() !== (orig.accountName || '').trim()) {
        await apiJson('/api/users/me/account', {
          method: 'PATCH',
          body: JSON.stringify({ name: accountName.trim() })
        })
      }
      // Update profile if changed
      if (!orig || (firstName || '').trim() !== (orig.firstName || '').trim() || (lastName || '').trim() !== (orig.lastName || '').trim()) {
        await apiJson('/api/users/me/profile', {
          method: 'PATCH',
          body: JSON.stringify({ firstName: firstName.trim(), lastName: lastName.trim() })
        })
      }
      await refresh()
      setOrig({ accountName, firstName, lastName })
      setMessage('Settings saved.')
    } catch {
      setMessage('Failed to save settings. Please try again later.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="max-w-3xl">
      <div className="mb-8">
        <h2 className="text-2xl font-bold text-white">Settings</h2>
        <p className="text-slate-400 mt-1">Manage your account preferences and personal details.</p>
      </div>

      <div className="bg-slate-900/50 border border-slate-800 rounded-xl p-6 md:p-8 relative overflow-hidden">
        {/* Decorative gradient */}
        <div className="absolute top-0 right-0 -mt-4 -mr-4 w-32 h-32 bg-indigo-500/10 blur-3xl rounded-full pointer-events-none"></div>

        <div className="flex items-center gap-5 mb-8">
          {user?.avatarUrl ? (
            <img src={user.avatarUrl} alt="avatar" className="w-20 h-20 rounded-full border-4 border-slate-800 shadow-xl" />
          ) : (
            <div className="w-20 h-20 rounded-full bg-indigo-600 flex items-center justify-center text-white text-2xl font-bold shadow-lg shadow-indigo-500/20">
              {user?.name?.[0] || user?.email?.[0] || '?'}
            </div>
          )}
          <div>
            <div className="text-xl font-bold text-white">{user?.name || 'Unknown User'}</div>
            <div className="text-slate-400">{user?.email}</div>
          </div>
        </div>

        <div className="space-y-6">
          <div>
            <label className="block text-sm font-medium text-slate-300 mb-2">Account Name</label>
            <div className="relative">
              <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                <BuildingOfficeIcon className="h-5 w-5 text-slate-500" />
              </div>
              <input
                value={accountName}
                onChange={(e) => setAccountName(e.target.value)}
                className="block w-full pl-10 bg-slate-950 border border-slate-800 rounded-lg py-2.5 text-slate-200 placeholder-slate-600 focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500 transition-all"
                placeholder="e.g. Acme Inc"
                disabled={loading}
              />
            </div>
          </div>

          <div className="grid md:grid-cols-2 gap-6">
            <div>
              <label className="block text-sm font-medium text-slate-300 mb-2">First Name</label>
              <div className="relative">
                 <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                   <UserCircleIcon className="h-5 w-5 text-slate-500" />
                 </div>
                 <input
                  value={firstName}
                  onChange={(e) => setFirstName(e.target.value)}
                  className="block w-full pl-10 bg-slate-950 border border-slate-800 rounded-lg py-2.5 text-slate-200 placeholder-slate-600 focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500 transition-all"
                  placeholder="John"
                  disabled={loading}
                />
              </div>
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-300 mb-2">Last Name</label>
              <div className="relative">
                 <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                   <UserCircleIcon className="h-5 w-5 text-slate-500" />
                 </div>
                 <input
                  value={lastName}
                  onChange={(e) => setLastName(e.target.value)}
                  className="block w-full pl-10 bg-slate-950 border border-slate-800 rounded-lg py-2.5 text-slate-200 placeholder-slate-600 focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500 transition-all"
                  placeholder="Doe"
                  disabled={loading}
                />
              </div>
            </div>
          </div>
        </div>

        {message && (
           <div className={`mt-6 p-3 rounded-lg text-sm ${message.includes('Failed') ? 'bg-red-500/10 text-red-400 border border-red-500/20' : 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'}`}>
             {message}
           </div>
        )}

        <div className="mt-8 pt-6 border-t border-slate-800 flex justify-end">
          <button 
            className={`px-6 py-2.5 rounded-lg font-medium transition-all ${
              isChanged 
                ? 'bg-indigo-600 hover:bg-indigo-500 text-white shadow-lg shadow-indigo-500/25' 
                : 'bg-slate-800 text-slate-500 cursor-not-allowed'
            }`}
            onClick={() => { void onSave() }} 
            disabled={saving || loading || !isChanged}
          >
            {saving ? 'Saving...' : isChanged ? 'Save Changes' : 'Saved'}
          </button>
        </div>
      </div>
    </div>
  )
}
