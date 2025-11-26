import { useEffect, useMemo, useState } from 'react'
import { useAuth } from '../../auth/AuthContext'
import { usePageTitle } from '../../components/PageHeader'
import { apiJson } from '../../lib/api'

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
    <div>
      <p className="text-white/70">Manage your account and personal details.</p>

      <div className="mt-6 max-w-2xl bg-black/20 border border-white/10 rounded-lg p-5">
        <div className="flex items-center gap-4">
          {user?.avatarUrl ? (
            <img src={user.avatarUrl} alt="avatar" className="w-14 h-14 rounded-full border border-white/10" />
          ) : (
            <div className="w-14 h-14 rounded-full bg-white/10 grid place-items-center text-white/60 text-lg">
              {user?.name?.[0] || user?.email?.[0] || '?'}
            </div>
          )}
          <div>
            <div className="font-semibold">{user?.name || 'Unknown User'}</div>
            <div className="text-white/60 text-sm">{user?.email}</div>
          </div>
        </div>

        <div className="mt-6 grid gap-4">
          <div>
            <label className="block text-sm text-white/60">Account name</label>
            <input
              value={accountName}
              onChange={(e) => setAccountName(e.target.value)}
              className="mt-1 w-full bg-black/30 border border-white/10 rounded px-3 py-2"
              placeholder="e.g. Acme Inc"
              disabled={loading}
            />
          </div>
          <div className="grid md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm text-white/60">First name</label>
              <input
                value={firstName}
                onChange={(e) => setFirstName(e.target.value)}
                className="mt-1 w-full bg-black/30 border border-white/10 rounded px-3 py-2"
                placeholder="John"
                disabled={loading}
              />
            </div>
            <div>
              <label className="block text-sm text-white/60">Last name</label>
              <input
                value={lastName}
                onChange={(e) => setLastName(e.target.value)}
                className="mt-1 w-full bg-black/30 border border-white/10 rounded px-3 py-2"
                placeholder="Doe"
                disabled={loading}
              />
            </div>
          </div>
        </div>

        {message && (
          <div className="mt-4 text-sm text-white/80">{message}</div>
        )}

        <div className="mt-6">
          <button className="btn" onClick={() => { void onSave() }} disabled={saving || loading || !isChanged}>
            {saving ? 'Saving…' : isChanged ? 'Save changes' : 'Saved'}
          </button>
        </div>
      </div>
    </div>
  )
}
