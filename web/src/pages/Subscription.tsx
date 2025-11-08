import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export default function Subscription() {
  const { user } = useAuth()

  const plans: { key: 'basic' | 'individual' | 'professional', name: string, price: string, features: string[] }[] = [
    { key: 'basic', name: 'basic', price: '$5', features: ['HTTP connections only', 'Up to 3Gb traffic per day'] },
    { key: 'individual', name: 'individual', price: '$10', features: ['Everything in basic', 'TCP connections', 'Up to 6Gb traffic per day'] },
    { key: 'professional', name: 'professional', price: '$20', features: ['Everything in individual', 'Up to 20Gb traffic per day'] },
  ]

  return (
    <div className="container py-16">
      <h1 className="text-3xl font-bold">Subscription</h1>
      <p className="text-white/70 mt-2">Choose a plan that fits your needs. Current plan: <span className="badge capitalize">{user?.plan || 'basic'}</span></p>

      <div className="grid md:grid-cols-3 gap-6 mt-8">
        {plans.map((p) => (
          <div key={p.key} className="bg-black/30 border border-white/10 rounded-xl p-6 flex flex-col">
            <div className="flex items-baseline gap-2">
              <div className="badge capitalize">{p.name}</div>
              <div className="text-2xl font-bold">{p.price}<span className="text-white/50 text-base">/mo</span></div>
            </div>
            <ul className="mt-4 text-sm text-white/80 space-y-2 list-disc list-inside">
              {p.features.map((f, i) => <li key={i}>{f}</li>)}
            </ul>
            <button className="btn mt-6" aria-label={`Select ${p.name} plan`}>Select</button>
          </div>
        ))}
      </div>

      <div className="mt-10 text-sm">
        <Link to="/app">← Back to dashboard</Link>
      </div>
    </div>
  )
}
