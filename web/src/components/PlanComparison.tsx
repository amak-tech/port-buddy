/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

import { CheckIcon, XMarkIcon } from '@heroicons/react/24/outline'
import { Link } from 'react-router-dom'

const comparisonData = [
  { feature: 'HTTP Tunnels', pro: true, team: true },
  { feature: 'TCP Tunnels', pro: true, team: true },
  { feature: 'UDP Tunnels', pro: true, team: true },
  { feature: 'SSL for HTTP', pro: true, team: true },
  { feature: 'Static Subdomains', pro: true, team: true },
  { feature: 'Custom Domains', pro: true, team: true },
  { feature: 'Private Tunnels', pro: true, team: true },
  { feature: 'Web Socket Support', pro: true, team: true },
  { feature: 'Free Tunnels', pro: '1 tunnel', team: '10 tunnels' },
  { feature: 'Extra Tunnels', pro: '$1/mo each', team: '$1/mo each' },
  { feature: 'Team Members', pro: false, team: true },
  { feature: 'SSO', pro: false, team: 'Coming soon' },
  { feature: 'Support', pro: 'Community', team: 'Priority' },
]

export default function PlanComparison() {
  return (
    <div className="mt-12 mb-16">
      
      {/* Pricing Cards */}
      <div className="grid md:grid-cols-2 gap-8 max-w-4xl mx-auto mb-24">
        {/* Pro Plan */}
        <div className="relative p-8 rounded-3xl bg-white/[0.02] border border-white/5 flex flex-col hover:border-white/10 transition-colors">
          <div className="mb-8">
            <h3 className="text-lg font-medium text-slate-400 mb-2">Pro</h3>
            <div className="flex items-baseline gap-1">
              <span className="text-4xl font-bold text-white">$0</span>
              <span className="text-slate-500">/month</span>
            </div>
            <p className="text-slate-400 text-sm mt-4 leading-relaxed">
              Perfect for hobbyists and individual developers working on side projects.
            </p>
          </div>
          <div className="space-y-4 mb-8 flex-1">
            <div className="flex gap-3 text-sm text-slate-300">
              <CheckIcon className="w-5 h-5 text-indigo-400 flex-shrink-0" />
              <span>1 free tunnel included</span>
            </div>
            <div className="flex gap-3 text-sm text-slate-300">
              <CheckIcon className="w-5 h-5 text-indigo-400 flex-shrink-0" />
              <span>Unlimited custom domains</span>
            </div>
            <div className="flex gap-3 text-sm text-slate-300">
              <CheckIcon className="w-5 h-5 text-indigo-400 flex-shrink-0" />
              <span>HTTP, TCP & UDP support</span>
            </div>
             <div className="flex gap-3 text-sm text-slate-300">
              <CheckIcon className="w-5 h-5 text-indigo-400 flex-shrink-0" />
              <span>$1/mo per extra tunnel</span>
            </div>
          </div>
          <Link to="/login" className="btn w-full justify-center glass hover:bg-white/10">
            Get Started
          </Link>
        </div>

        {/* Team Plan */}
        <div className="relative p-8 rounded-3xl bg-gradient-to-b from-indigo-500/10 to-purple-500/5 border border-indigo-500/20 flex flex-col shadow-[0_0_50px_-20px_rgba(99,102,241,0.3)]">
          <div className="absolute -top-4 left-1/2 -translate-x-1/2 px-3 py-1 bg-gradient-to-r from-indigo-500 to-purple-500 rounded-full text-[10px] font-bold uppercase tracking-wider text-white shadow-lg">
            Most Popular
          </div>
          <div className="mb-8">
            <h3 className="text-lg font-medium text-indigo-300 mb-2">Team</h3>
            <div className="flex items-baseline gap-1">
              <span className="text-4xl font-bold text-white">$10</span>
              <span className="text-slate-500">/month</span>
            </div>
             <p className="text-indigo-200/60 text-sm mt-4 leading-relaxed">
              For growing teams that need more resources and collaboration features.
            </p>
          </div>
          <div className="space-y-4 mb-8 flex-1">
             <div className="flex gap-3 text-sm text-slate-300">
              <CheckIcon className="w-5 h-5 text-indigo-400 flex-shrink-0" />
              <span><strong className="text-white">10 free tunnels</strong> included</span>
            </div>
            <div className="flex gap-3 text-sm text-slate-300">
              <CheckIcon className="w-5 h-5 text-indigo-400 flex-shrink-0" />
              <span>Team member management</span>
            </div>
            <div className="flex gap-3 text-sm text-slate-300">
              <CheckIcon className="w-5 h-5 text-indigo-400 flex-shrink-0" />
              <span>Priority email support</span>
            </div>
             <div className="flex gap-3 text-sm text-slate-300">
              <CheckIcon className="w-5 h-5 text-indigo-400 flex-shrink-0" />
              <span>$1/mo per extra tunnel</span>
            </div>
          </div>
          <Link to="/login" className="btn btn-primary w-full justify-center">
            Upgrade to Team
          </Link>
        </div>
      </div>

      <div className="text-center mb-16">
        <h2 className="text-4xl font-black text-white mb-4 tracking-tight">Detailed Comparison</h2>
        <p className="text-slate-400 text-lg">Choose the plan that fits your development needs.</p>
      </div>
      
      <div className="overflow-x-auto glass rounded-3xl border border-white/5 p-4 md:p-8">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="border-b border-white/5">
              <th className="py-8 px-6 text-slate-500 font-bold uppercase tracking-widest text-xs">Feature</th>
              <th className="py-8 px-6 text-white font-black text-center w-1/4 text-2xl tracking-tighter">Pro</th>
              <th className="py-8 px-6 text-jb-blue font-black text-center w-1/4 text-2xl tracking-tighter">Team</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-white/[0.02]">
            {comparisonData.map((item, idx) => (
              <tr key={idx} className="group hover:bg-white/[0.02] transition-colors">
                <td className="py-5 px-6 text-slate-300 font-medium group-hover:text-white transition-colors">{item.feature}</td>
                <td className="py-5 px-6 text-center">
                  {typeof item.pro === 'boolean' ? (
                    item.pro ? <CheckIcon className="w-6 h-6 text-green-500 mx-auto" /> : <XMarkIcon className="w-6 h-6 text-slate-700 mx-auto" />
                  ) : (
                    <span className="text-sm text-slate-400 font-mono">{item.pro}</span>
                  )}
                </td>
                <td className="py-5 px-6 text-center">
                  {typeof item.team === 'boolean' ? (
                    item.team ? <CheckIcon className="w-6 h-6 text-jb-blue mx-auto" /> : <XMarkIcon className="w-6 h-6 text-slate-700 mx-auto" />
                  ) : (
                    <span className="text-sm text-jb-blue font-bold font-mono">{item.team}</span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
