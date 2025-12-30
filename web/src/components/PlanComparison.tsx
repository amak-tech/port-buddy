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
    <div className="mt-24 mb-16">
      <div className="text-center mb-12">
        <h2 className="text-3xl font-bold text-white mb-4">Detailed Plan Comparison</h2>
        <p className="text-slate-400">Everything you need to know about our plans.</p>
      </div>
      
      <div className="overflow-x-auto">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="border-b border-slate-800">
              <th className="py-6 px-4 text-slate-400 font-medium">Feature</th>
              <th className="py-6 px-4 text-white font-bold text-center w-1/4">Pro</th>
              <th className="py-6 px-4 text-indigo-400 font-bold text-center w-1/4">Team</th>
            </tr>
          </thead>
          <tbody>
            {comparisonData.map((item, idx) => (
              <tr key={idx} className="border-b border-slate-800/50 hover:bg-slate-900/30 transition-colors">
                <td className="py-4 px-4 text-slate-300">{item.feature}</td>
                <td className="py-4 px-4 text-center">
                  {typeof item.pro === 'boolean' ? (
                    item.pro ? <CheckIcon className="w-5 h-5 text-green-500 mx-auto" /> : <XMarkIcon className="w-5 h-5 text-slate-600 mx-auto" />
                  ) : (
                    <span className="text-sm text-slate-400">{item.pro}</span>
                  )}
                </td>
                <td className="py-4 px-4 text-center">
                  {typeof item.team === 'boolean' ? (
                    item.team ? <CheckIcon className="w-5 h-5 text-indigo-500 mx-auto" /> : <XMarkIcon className="w-5 h-5 text-slate-600 mx-auto" />
                  ) : (
                    <span className="text-sm text-indigo-400 font-medium">{item.team}</span>
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
