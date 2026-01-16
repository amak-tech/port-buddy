/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

import { CheckIcon, XMarkIcon } from '@heroicons/react/24/outline'

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
    <div className="mt-32 mb-16">
      <div className="text-center mb-16">
        <h2 className="text-4xl font-black text-white mb-4 tracking-tight">Detailed <span className="text-gradient">Comparison</span></h2>
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
