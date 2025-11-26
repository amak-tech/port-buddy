import { useEffect, useState } from 'react'
import { useAuth } from '../../auth/AuthContext'
import { usePageTitle } from '../../components/PageHeader'

export default function Settings() {
  const { user, refresh } = useAuth()
  usePageTitle('Settings')
  const [accountName, setAccountName] = useState('')
  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState<string | null>(null)

  useEffect(() => {
    // Pre-fill from best-effort name split
    const name = user?.name || ''
    if (name && !firstName && !lastName) {
      const parts = name.trim().split(/\s+/)
      if (parts.length > 0) setFirstName(parts[0])
      if (parts.length > 1) setLastName(parts.slice(1).join(' '))
    }
    if (!accountName && name) {
      setAccountName(`${name}'s account`)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user])

  async function onSave() {
    setSaving(true)
    setMessage(null)
    try {
      // TODO: Implement backend endpoints to persist account and user profile updates.
      // For now, we just simulate success and refresh user profile.
      await new Promise((r) => setTimeout(r, 500))
      await refresh()
      setMessage('Settings saved (placeholder). Coming soon: real profile and account updates.')
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
              />
            </div>
            <div>
              <label className="block text-sm text-white/60">Last name</label>
              <input
                value={lastName}
                onChange={(e) => setLastName(e.target.value)}
                className="mt-1 w-full bg-black/30 border border-white/10 rounded px-3 py-2"
                placeholder="Doe"
              />
            </div>
          </div>
        </div>

        {message && (
          <div className="mt-4 text-sm text-white/80">{message}</div>
        )}

        <div className="mt-6">
          <button className="btn" onClick={() => { void onSave() }} disabled={saving}>
            {saving ? 'Saving…' : 'Save changes'}
          </button>
        </div>
      </div>
    </div>
  )
}
