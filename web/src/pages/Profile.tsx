import { useAuth } from '../auth/AuthContext'

export default function Profile() {
  const { user, refresh } = useAuth()

  return (
    <div className="container py-16">
      <h1 className="text-3xl font-bold">Profile</h1>
      <p className="text-white/70 mt-2">Manage your account details.</p>

      <div className="mt-8 max-w-3xl bg-black/30 border border-white/10 rounded-xl p-6">
        <div className="flex items-center gap-4">
          {user?.avatarUrl ? (
            <img src={user.avatarUrl} alt="avatar" className="w-16 h-16 rounded-full border border-white/10" />
          ) : (
            <div className="w-16 h-16 rounded-full bg-white/10 grid place-items-center text-white/60">{user?.name?.[0] || user?.email?.[0] || '?'}</div>
          )}
          <div>
            <div className="text-lg font-semibold">{user?.name || 'Unknown User'}</div>
            <div className="text-white/60 text-sm">{user?.email}</div>
          </div>
        </div>

        <div className="mt-6 grid md:grid-cols-2 gap-4">
          <div className="bg-black/20 border border-white/10 rounded p-4">
            <div className="text-white/60 text-sm">Plan</div>
            <div className="mt-1 font-semibold capitalize">{user?.plan || 'basic'}</div>
          </div>
          <div className="bg-black/20 border border-white/10 rounded p-4">
            <div className="text-white/60 text-sm">User ID</div>
            <div className="mt-1 font-mono text-sm break-all">{user?.id}</div>
          </div>
        </div>

        <button className="btn mt-6" onClick={() => { void refresh() }}>Refresh</button>
        </div>
      </div>
    )
  }
