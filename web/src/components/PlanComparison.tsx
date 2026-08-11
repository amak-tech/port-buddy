/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

import { CheckIcon, XMarkIcon } from '@heroicons/react/24/outline'
import { Link } from 'react-router-dom'
import {
  BANDWIDTH_POLICY,
  INCLUDED_LABEL,
  NGROK_COMPARISON_LINE,
  NOT_INCLUDED_LABEL,
  PLANS,
  PLAN_COMPARISON,
  TCP_ENTITLEMENT_DETAIL,
  TCP_LONG,
  freeTunnelsLabel,
  priceLabel,
  type FeatureAvailability,
  type Plan
} from '../config/plans'

/**
 * A cell in the comparison table. The check/cross icons are decorative, so each one is paired with
 * a visually hidden label — without it the cell is empty to screen readers and to crawlers.
 */
function AvailabilityCell({ value, tone }: { value: FeatureAvailability, tone: 'pro' | 'team' }) {
  if (typeof value !== 'boolean') {
    return tone === 'pro'
      ? <span className="text-xs md:text-sm text-slate-400 font-mono">{value}</span>
      : <span className="text-xs md:text-sm text-jb-blue font-bold font-mono">{value}</span>
  }

  const iconClass = `w-5 h-5 md:w-6 md:h-6 mx-auto ${
    value ? (tone === 'pro' ? 'text-green-500' : 'text-jb-blue') : 'text-slate-700'
  }`

  return (
    <>
      {value ? <CheckIcon className={iconClass} /> : <XMarkIcon className={iconClass} />}
      <span className="sr-only">{value ? INCLUDED_LABEL : NOT_INCLUDED_LABEL}</span>
    </>
  )
}

function PricingCard({ plan, highlighted }: { plan: Plan, highlighted?: boolean }) {
  return (
    <div
      className={highlighted
        ? 'relative p-8 rounded-3xl bg-gradient-to-b from-indigo-500/10 to-purple-500/5 border border-indigo-500/20 flex flex-col shadow-[0_0_50px_-20px_rgba(99,102,241,0.3)]'
        : 'relative p-8 rounded-3xl bg-white/[0.02] border border-white/5 flex flex-col hover:border-white/10 transition-colors'}
    >
      {highlighted && (
        <div className="absolute -top-4 left-1/2 -translate-x-1/2 px-3 py-1 bg-gradient-to-r from-indigo-500 to-purple-500 rounded-full text-[10px] font-bold uppercase tracking-wider text-white shadow-lg">
          Most Popular
        </div>
      )}
      <div className="mb-8">
        <h3 className={`text-lg font-medium mb-2 ${highlighted ? 'text-indigo-300' : 'text-slate-400'}`}>{plan.name}</h3>
        <div className="flex items-baseline gap-1">
          <span className="text-4xl font-bold text-white">{priceLabel(plan)}</span>
          <span className="text-slate-500">/month</span>
        </div>
        <p className={`text-sm mt-4 leading-relaxed ${highlighted ? 'text-indigo-200/60' : 'text-slate-400'}`}>
          {plan.tagline}
        </p>
      </div>
      <ul className="space-y-4 mb-8 flex-1">
        {plan.features.map((feature) => (
          <li key={feature} className="flex gap-3 text-sm text-slate-300">
            <CheckIcon className="w-5 h-5 text-indigo-400 flex-shrink-0" />
            <span>{feature}</span>
          </li>
        ))}
      </ul>
      <Link
        to={plan.cta.to}
        className={highlighted ? 'btn btn-primary w-full justify-center' : 'btn w-full justify-center glass hover:bg-white/10'}
      >
        {plan.cta.label}
      </Link>
    </div>
  )
}

export default function PlanComparison() {
  const { pro, team } = PLANS

  return (
    <div className="mt-12 mb-16">

      {/* Pricing Cards */}
      <div className="grid md:grid-cols-2 gap-8 max-w-4xl mx-auto mb-24">
        <PricingCard plan={pro} />
        <PricingCard plan={team} highlighted />
      </div>

      <div className="text-center mb-16">
        <h2 className="text-4xl font-black text-white mb-4 tracking-tight">Detailed Comparison</h2>
        <p className="text-slate-400 text-lg">Choose the plan that fits your development needs.</p>
      </div>

      <div className="overflow-x-auto glass rounded-3xl border border-white/5 p-4 md:p-8">
        {/* table-fixed + tighter mobile padding: without it the Team column is pushed outside the
            scroll container on phones and reads as clipped text. */}
        <table className="w-full table-fixed text-left border-collapse">
          <caption className="sr-only">
            {`Feature comparison of the ${pro.name} plan (${priceLabel(pro)}/month, ${freeTunnelsLabel(pro)}) `
              + `and the ${team.name} plan (${priceLabel(team)}/month, ${freeTunnelsLabel(team)})`}
          </caption>
          <thead>
            <tr className="border-b border-white/5">
              <th scope="col" className="py-5 md:py-8 px-2 md:px-6 text-slate-500 font-bold uppercase tracking-widest text-[10px] md:text-xs">Feature</th>
              <th scope="col" className="py-5 md:py-8 px-2 md:px-6 text-white font-black text-center w-[30%] md:w-1/4 text-lg md:text-2xl tracking-tighter">{pro.name}</th>
              <th scope="col" className="py-5 md:py-8 px-2 md:px-6 text-jb-blue font-black text-center w-[30%] md:w-1/4 text-lg md:text-2xl tracking-tighter">{team.name}</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-white/[0.02]">
            {PLAN_COMPARISON.map((row) => (
              <tr key={row.feature} className="group hover:bg-white/[0.02] transition-colors">
                <th scope="row" className="py-4 md:py-5 px-2 md:px-6 text-left text-sm md:text-base text-slate-300 font-medium group-hover:text-white transition-colors">{row.feature}</th>
                <td className="py-4 md:py-5 px-2 md:px-6 text-center">
                  <AvailabilityCell value={row.pro} tone="pro" />
                </td>
                <td className="py-4 md:py-5 px-2 md:px-6 text-center">
                  <AvailabilityCell value={row.team} tone="team" />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* The TCP price in full, immediately under the table that quotes it in short form. The
          entitlement rule follows it rather than hiding in a footnote marker: people who want the
          exact rule get it here, and nobody has to decode a threshold to find the price. */}
      <div className="max-w-3xl mx-auto mt-8 space-y-3 text-center text-sm">
        <p className="text-slate-400">{TCP_LONG}</p>
        <p className="text-slate-500">{TCP_ENTITLEMENT_DETAIL}</p>
        <p className="text-slate-400">{BANDWIDTH_POLICY}</p>
        <p className="text-slate-500">{NGROK_COMPARISON_LINE}</p>
      </div>
    </div>
  )
}
