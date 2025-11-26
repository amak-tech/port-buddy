import { useAuth } from '../../auth/AuthContext'
import { usePageTitle } from '../../components/PageHeader'

export default function Domains() {
  const { user } = useAuth()
  usePageTitle('Domains')
  const plan = user?.plan || 'basic'

  return (
    <div>
      <p className="text-white/70">Manage your static subdomains under the app domain.</p>

      <div className="mt-4 p-4 rounded-lg border border-white/10 bg-black/20">
        <div className="text-white/80">This feature is coming soon.</div>
        <ul className="text-white/60 text-sm list-disc list-inside mt-2">
          <li>Hobby plan: 2 static subdomains</li>
          <li>Developer plan: 10 static subdomains and 1 custom domain</li>
        </ul>
      </div>

      <div className="mt-6 grid md:grid-cols-2 gap-4">
        <div className="p-4 rounded-lg border border-white/10 bg-black/20">
          <div className="text-white/60 text-sm">Your plan</div>
          <div className="mt-1"><span className="badge capitalize">{plan}</span></div>
        </div>
        <div className="p-4 rounded-lg border border-white/10 bg-black/20">
          <div className="text-white/60 text-sm">App domain</div>
          <div className="mt-1 font-mono">auto-assigned per environment</div>
        </div>
      </div>
    </div>
  )
}
