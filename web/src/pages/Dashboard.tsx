import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export default function Dashboard() {
  const { user } = useAuth()
  // Placeholder usage activity; in real app fetch from /api/usage
  const activity = [
    { id: 1, type: 'HTTP', local: 'http://localhost:3000', publicUrl: 'https://r2dx.port-buddy.com', bytes: 512_000, at: 'Today 14:22' },
    { id: 2, type: 'TCP', local: '127.0.0.1:5432', publicUrl: 'tcp-proxy-3.port-buddy.com:43452', bytes: 2_048_000, at: 'Yesterday 19:03' },
  ]
  const plan = user?.plan || 'basic'
  const planCapGb = plan === 'professional' ? 20 : plan === 'individual' ? 6 : 3
  const usedGb = 0.8 // placeholder
  const usedPct = Math.min(100, Math.round((usedGb / planCapGb) * 100))

  return (
    <div className="container py-16">
      <h1 className="text-3xl font-bold">Dashboard</h1>
      <p className="text-white/70 mt-2">See your recent activity, traffic usage, and manage your subscription.</p>

      <section className="mt-10 grid md:grid-cols-3 gap-6">
        <div className="bg-black/30 border border-white/10 rounded-xl p-6">
          <div className="text-white/60 text-sm">Subscription</div>
          <div className="mt-2 flex items-center gap-2">
            <span className="badge capitalize">{plan}</span>
            <span className="text-white/50 text-sm">Active</span>
          </div>
          <Link to="/app/subscription" className="btn mt-4">Manage</Link>
        </div>

        <div className="bg-black/30 border border-white/10 rounded-xl p-6">
          <div className="text-white/60 text-sm">Daily Traffic</div>
          <div className="text-2xl font-bold mt-2">{usedGb}<span className="text-base text-white/50"> / {planCapGb} GB</span></div>
          <div className="w-full h-2 bg-white/10 rounded mt-3">
            <div className="h-2 bg-accent rounded" style={{ width: `${usedPct}%` }}></div>
          </div>
        </div>

        <div className="bg-black/30 border border-white/10 rounded-xl p-6">
          <div className="text-white/60 text-sm">Profile</div>
          <div className="mt-2 text-sm">{user?.email}</div>
          <Link to="/app/profile" className="btn mt-4">Edit Profile</Link>
        </div>
      </section>

      <section className="mt-10">
        <h2 className="text-xl font-semibold">Recent Activity</h2>
        <div className="mt-4 grid gap-3">
          {activity.map((a) => (
            <div key={a.id} className="bg-black/30 border border-white/10 rounded p-4 flex flex-col md:flex-row md:items-center md:justify-between gap-2">
              <div className="flex items-center gap-3">
                <span className="badge">{a.type}</span>
                <span className="text-white/70 text-sm">{a.local}</span>
                <span className="text-white/40">→</span>
                <span className="text-white/90 text-sm font-mono">{a.publicUrl}</span>
              </div>
              <div className="text-white/60 text-sm">{(a.bytes/1024).toFixed(0)} KB • {a.at}</div>
            </div>
          ))}
        </div>
      </section>

      <section className="mt-10">
        <h2 className="text-xl font-semibold">Quick Start</h2>
        <pre className="mt-3 bg-black/30 border border-white/10 rounded p-3 text-sm">
{`# Authenticate the CLI
port-buddy init {API_TOKEN}

# Expose an HTTP service
port-buddy 3000

# Expose a TCP service
port-buddy tcp 127.0.0.1:5432`}
        </pre>
      </section>
    </div>
  )
}
