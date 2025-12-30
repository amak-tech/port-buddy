import { useState, useEffect } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import { usePageTitle } from '../../components/PageHeader'
import { CheckIcon, ArrowLeftIcon, PlusIcon, MinusIcon } from '@heroicons/react/24/outline'
import { apiJson } from '../../lib/api'
import PlanComparison from '../../components/PlanComparison'

export default function Billing() {
  usePageTitle('Billing')
  const { user, refresh } = useAuth()
  const [searchParams, setSearchParams] = useSearchParams()
  const [updating, setUpdating] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)
  const [pendingExtra, setPendingExtra] = useState<number | null>(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (searchParams.get('success')) {
      setSuccess(true)
      refresh()
      // Remove query param
      const newParams = new URLSearchParams(searchParams)
      newParams.delete('success')
      setSearchParams(newParams, { replace: true })
    }
    if (searchParams.get('canceled')) {
      setError('Payment was canceled.')
      const newParams = new URLSearchParams(searchParams)
      newParams.delete('canceled')
      setSearchParams(newParams, { replace: true })
    }
  }, [searchParams, refresh, setSearchParams])

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
        'SSO (Coming soon)',
        'Priority support',
        '10 free tunnels at a time',
        '$1/mo per extra tunnel'
      ] 
    },
  ]

  const currentPlanKey = user?.plan || 'pro'
  const extraTunnels = user?.extraTunnels || 0
  const baseTunnels = user?.baseTunnels || 1
  const activeTunnels = user?.activeTunnels || 0
  const subscriptionStatus = user?.subscriptionStatus
  const effectiveExtra = pendingExtra !== null ? pendingExtra : extraTunnels

  const planPrice = currentPlanKey === 'team' ? 10 : 0
  const extraCost = effectiveExtra * 1
  const totalMonthly = planPrice + extraCost
  const increment = currentPlanKey === 'team' ? 5 : 1

  const getLimitForPlan = (planKey: string) => {
    return planKey === 'team' ? 10 : 1;
  };

  const handleUpdate = async () => {
    if (pendingExtra === null || pendingExtra === extraTunnels) return

    const newLimit = baseTunnels + pendingExtra;
    if (pendingExtra < extraTunnels && activeTunnels > newLimit) {
      if (!window.confirm(`Reducing extra tunnels will lower your total limit to ${newLimit}. You currently have ${activeTunnels} active tunnels. Excess tunnels will be automatically closed. Do you want to proceed?`)) {
        return;
      }
    }

    // If user has no subscription and is adding extra tunnels, we must use checkout session
    if (pendingExtra > 0 && !user?.subscriptionStatus) {
      setError(null)
      setLoading(true)
      try {
        // First update the local extra tunnels count so it's included in checkout
        await apiJson('/api/users/me/account/tunnels', {
          method: 'PATCH',
          body: JSON.stringify({ extraTunnels: pendingExtra })
        })
        
        // Then redirect to checkout
        const { url } = await apiJson('/api/payments/create-checkout-session', {
          method: 'POST',
          body: JSON.stringify({ plan: currentPlanKey.toUpperCase() })
        })
        window.location.href = url
        return
      } catch (e: any) {
        setError(e.message || 'Failed to initiate checkout')
        setLoading(false)
        return
      }
    }

    setError(null)
    setUpdating(true)
    setLoading(true)
    try {
      await apiJson('/api/users/me/account/tunnels', {
        method: 'PATCH',
        body: JSON.stringify({ extraTunnels: pendingExtra })
      })
      await refresh()
      setPendingExtra(null)
      setSuccess(true)
    } catch (e: any) {
      setError(e.message || 'Failed to update tunnels')
    } finally {
      setUpdating(false)
      setLoading(false)
    }
  }

  const changePending = (newExtra: number) => {
    if (newExtra < 0) return
    setPendingExtra(newExtra)
  }

  const handleUpgrade = async (planKey: string) => {
    const isDowngrade = currentPlanKey === 'team' && planKey === 'pro';
    const newLimit = getLimitForPlan(planKey) + extraTunnels;
    
    if (isDowngrade && activeTunnels > newLimit) {
      if (!window.confirm(`Downgrading to PRO will reduce your tunnel limit to ${newLimit}. You currently have ${activeTunnels} active tunnels. Excess tunnels will be automatically closed. Do you want to proceed?`)) {
        return;
      }
    }

    setError(null)
    setLoading(true)
    try {
      const { url } = await apiJson('/api/payments/create-checkout-session', {
        method: 'POST',
        body: JSON.stringify({ plan: planKey.toUpperCase() })
      })
      window.location.href = url
    } catch (e: any) {
      setError(e.message || 'Failed to initiate checkout')
    } finally {
      setLoading(false)
    }
  }

  const handleManageBilling = async () => {
    setError(null)
    setLoading(true)
    try {
      const { url } = await apiJson('/api/payments/create-portal-session', {
        method: 'POST'
      })
      window.location.href = url
    } catch (e: any) {
      setError(e.message || 'Failed to initiate portal session')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-6xl mx-auto">
      <div className="mb-8 flex flex-col md:flex-row md:items-end justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-white">Billing & Plans</h2>
          <p className="text-slate-400 mt-1">Choose the plan that fits your needs.</p>
        </div>
        {subscriptionStatus && (
          <div className="flex items-center gap-2 bg-slate-900 border border-slate-800 px-4 py-2 rounded-xl">
            <span className="text-sm text-slate-400">Subscription Status:</span>
            <span className={`text-sm font-bold uppercase tracking-wider ${
              subscriptionStatus === 'active' ? 'text-green-400' : 
              subscriptionStatus === 'past_due' ? 'text-yellow-400' : 'text-red-400'
            }`}>
              {subscriptionStatus.replace('_', ' ')}
            </span>
          </div>
        )}
      </div>

      {success && (
        <div className="mb-8 p-4 bg-green-900/30 border border-green-500/50 rounded-xl text-green-400 text-center">
          Success! Your subscription has been updated.
        </div>
      )}

      <div className="grid lg:grid-cols-2 gap-8 max-w-4xl mx-auto lg:mx-0">
        {plans.map((p) => {
           const isCurrent = p.key === currentPlanKey
           const isPopular = p.key === 'team'

           return (
            <div 
              key={p.key} 
              className={`relative flex flex-col p-8 rounded-2xl border transition-all duration-300 ${
                isPopular 
                  ? 'bg-slate-900/80 border-indigo-500 shadow-2xl shadow-indigo-500/10' 
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

              {isCurrent && (
                <div className="mb-8 p-4 rounded-xl bg-slate-800/50 border border-slate-700">
                  <div className="flex justify-between items-center mb-4 border-b border-slate-700/50 pb-4">
                    <div>
                      <div className="text-sm font-medium text-slate-300">Total Tunnels</div>
                      <div className="text-2xl font-bold text-white">{baseTunnels + effectiveExtra}</div>
                    </div>
                    <div className="text-right">
                      <div className="text-xs text-slate-500 uppercase tracking-wider">Extra Tunnels</div>
                      <div className="text-lg font-semibold text-indigo-400">+{effectiveExtra} (${extraCost}/mo)</div>
                    </div>
                  </div>

                  <div className="flex justify-between items-center mb-6">
                    <div className="text-sm text-slate-400">Total Monthly</div>
                    <div className="text-xl font-bold text-white">${totalMonthly}<span className="text-sm font-normal text-slate-500">/mo</span></div>
                  </div>
                  
                  <div className="flex items-center gap-4 mb-4">
                    <button
                      onClick={() => changePending(effectiveExtra - increment)}
                      disabled={updating || effectiveExtra <= 0}
                      className="p-2 rounded-lg bg-slate-700 hover:bg-slate-600 text-white disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                    >
                      <MinusIcon className="w-5 h-5" />
                    </button>
                    <div className="flex-1 text-center font-mono text-white">
                      {effectiveExtra < extraTunnels ? `Remove ${increment}` : `Add ${increment}`}
                    </div>
                    <button
                      onClick={() => changePending(effectiveExtra + increment)}
                      disabled={updating}
                      className="p-2 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                    >
                      <PlusIcon className="w-5 h-5" />
                    </button>
                  </div>

                  {pendingExtra !== null && pendingExtra !== extraTunnels && (
                    <div className="flex gap-2">
                      <button
                        onClick={handleUpdate}
                        disabled={updating}
                        className="flex-1 py-2 bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-semibold rounded-lg shadow-lg shadow-indigo-500/20 transition-all disabled:opacity-50"
                      >
                        Update my subscription
                      </button>
                      <button
                        onClick={() => setPendingExtra(null)}
                        disabled={updating}
                        className="px-4 py-2 bg-slate-700 hover:bg-slate-600 text-white text-sm font-semibold rounded-lg transition-all disabled:opacity-50"
                      >
                        Cancel
                      </button>
                    </div>
                  )}
                  {error && <p className="mt-2 text-xs text-red-400">{error}</p>}
                </div>
              )}

              <button 
                onClick={() => handleUpgrade(p.key)}
                className={`w-full py-3 rounded-lg font-semibold transition-all ${
                  isCurrent
                    ? 'bg-slate-800 text-slate-400 cursor-default border border-slate-700'
                    : isPopular
                    ? 'bg-indigo-600 hover:bg-indigo-500 text-white shadow-lg shadow-indigo-500/25'
                    : 'bg-slate-800 hover:bg-slate-700 text-white border border-slate-700'
                } disabled:opacity-50`}
                disabled={isCurrent || loading}
              >
                {isCurrent ? 'Current Plan' : (currentPlanKey === 'team' && p.key === 'pro' ? 'Downgrade' : 'Upgrade')}
              </button>
            </div>
          )
        })}
      </div>

      <div className="mt-8 flex flex-col items-center">
        {user?.stripeCustomerId && (
          <button
            onClick={handleManageBilling}
            disabled={loading}
            className="px-6 py-2 bg-slate-800 hover:bg-slate-700 text-white text-sm font-semibold rounded-lg border border-slate-700 transition-all disabled:opacity-50"
          >
            Manage Billing & Subscriptions
          </button>
        )}
        {error && <p className="mt-4 text-sm text-red-400">{error}</p>}
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

      <PlanComparison />
    </div>
  )
}
