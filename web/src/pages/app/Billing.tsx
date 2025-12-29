import { Link } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import { usePageTitle } from '../../components/PageHeader'
import { CheckIcon, ArrowLeftIcon } from '@heroicons/react/24/outline'

export default function Billing() {
  usePageTitle('Billing')
  const { user } = useAuth()

  const plans: { key: 'pro' | 'team', name: string, price: string, period?: string, description: string, features: string[] }[] = [
    { 
      key: 'pro', 
      name: 'Pro', 
      price: '$0', 
      description: 'Everything you need for personal exposure.',
      features: [
        'HTTP, TCP, UDP tunnels',
        'SSL for HTTP tunnels',
        'Static subdomains',
        'Custom domains',
        'Private tunnels',
        'Web socket support',
        '1 free tunnel at a time',
        '$1/mo per extra tunnel'
      ] 
    },
    { 
      key: 'team', 
      name: 'Team', 
      price: '$10', 
      period: '/mo',
      description: 'For teams and collaborative projects.',
      features: [
        'Everything in Pro',
        'Team members',
        'SSO (Single Sign-On)',
        'Priority support',
        '10 free tunnels at a time',
        '$1/mo per extra tunnel'
      ] 
    },
  ]

  const currentPlanKey = user?.plan || 'pro'

  return (
    <div className="max-w-6xl mx-auto">
      <div className="mb-8 text-center lg:text-left">
        <h2 className="text-2xl font-bold text-white">Billing & Plans</h2>
        <p className="text-slate-400 mt-1">Choose the plan that fits your needs.</p>
      </div>

      <div className="grid lg:grid-cols-2 gap-8 max-w-4xl mx-auto lg:mx-0">
        {plans.map((p) => {
           const isCurrent = p.key === currentPlanKey
           const isPopular = p.key === 'team'

           return (
            <div 
              key={p.key} 
              className={`relative flex flex-col p-8 rounded-2xl border transition-all duration-300 ${
                isPopular 
                  ? 'bg-slate-900/80 border-indigo-500 shadow-2xl shadow-indigo-500/10 transform lg:-translate-y-4 z-10' 
                  : 'bg-slate-900/40 border-slate-800 hover:border-slate-700'
              }`}
            >
              {isPopular && (
                <div className="absolute top-0 left-1/2 -translate-x-1/2 -translate-y-1/2 px-4 py-1 bg-indigo-600 text-white text-xs font-bold uppercase tracking-wide rounded-full shadow-lg shadow-indigo-500/20">
                  Most Popular
                </div>
              )}

              <div className="mb-6">
                <h3 className={`text-lg font-bold ${isPopular ? 'text-white' : 'text-slate-200'}`}>{p.name}</h3>
                <div className="mt-2 flex items-baseline gap-1">
                  <span className="text-4xl font-bold text-white">{p.price}</span>
                  {p.period && <span className="text-slate-500">{p.period}</span>}
                </div>
                <p className="text-slate-400 text-sm mt-2">{p.description}</p>
              </div>

              <ul className="space-y-4 mb-8 flex-1">
                {p.features.map((f, i) => (
                  <li key={i} className="flex items-start gap-3 text-sm text-slate-300">
                    <CheckIcon className={`w-5 h-5 flex-shrink-0 ${isPopular ? 'text-indigo-400' : 'text-slate-500'}`} />
                    <span>{f}</span>
                  </li>
                ))}
              </ul>

              <button 
                className={`w-full py-3 rounded-lg font-semibold transition-all ${
                  isCurrent
                    ? 'bg-slate-800 text-slate-400 cursor-default border border-slate-700'
                    : isPopular
                    ? 'bg-indigo-600 hover:bg-indigo-500 text-white shadow-lg shadow-indigo-500/25'
                    : 'bg-slate-800 hover:bg-slate-700 text-white border border-slate-700'
                }`}
                disabled={isCurrent}
              >
                {isCurrent ? 'Current Plan' : 'Upgrade'}
              </button>
            </div>
          )
        })}
      </div>

      <div className="mt-12 text-center">
        <p className="text-slate-500 text-sm mb-4">
          Need a custom enterprise plan? <a href="#" className="text-indigo-400 hover:text-indigo-300 hover:underline">Contact us</a>
        </p>
        <Link to="/app" className="inline-flex items-center gap-2 text-slate-400 hover:text-white transition-colors text-sm">
          <ArrowLeftIcon className="w-4 h-4" />
          Back to dashboard
        </Link>
      </div>
    </div>
  )
}
