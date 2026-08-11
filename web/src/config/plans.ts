/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

/**
 * The single source of truth for plan names, prices, tunnel counts and feature availability.
 *
 * Every surface that mentions a plan reads from here — the homepage hero, the pricing cards, the
 * comparison table, the FAQ, /install, /docs and the JSON-LD offers. Structured data is generated
 * from the same values as the visible copy so the two cannot drift apart.
 */

export const PRICE_CURRENCY = 'USD'
export const PRICE_CURRENCY_SYMBOL = '$'

/** Price of one additional concurrent tunnel, per month. */
export const EXTRA_TUNNEL_PRICE = 1

/** TCP tunnels are unlocked once an account has at least this many tunnels. */
export const TCP_MIN_TUNNELS = 5

export type PlanId = 'pro' | 'team'

export type Plan = {
  id: PlanId
  /** Plan name as shown to users. */
  name: string
  /** Monthly price in {@link PRICE_CURRENCY}. */
  priceMonthly: number
  /** Concurrent tunnels included at no extra cost. */
  freeTunnels: number
  /** Long-form positioning line used on the pricing cards. */
  tagline: string
  /** Condensed positioning line used where space is tight (/install). */
  shortTagline: string
  /** Everything the plan includes, in display order. */
  features: readonly string[]
  cta: { label: string, to: string }
}

function pluralize(count: number, singular: string): string {
  return `${count} ${singular}${count === 1 ? '' : 's'}`
}

export function priceLabel(plan: Plan): string {
  return `${PRICE_CURRENCY_SYMBOL}${plan.priceMonthly}`
}

/** e.g. "1 free tunnel", "10 free tunnels". */
export function freeTunnelsLabel(plan: Plan): string {
  return `${plan.freeTunnels} free ${plan.freeTunnels === 1 ? 'tunnel' : 'tunnels'}`
}

export const EXTRA_TUNNEL_PRICE_LABEL = `${PRICE_CURRENCY_SYMBOL}${EXTRA_TUNNEL_PRICE}/mo`

export const EXTRA_TUNNEL_NOTE = `${EXTRA_TUNNEL_PRICE_LABEL} for each additional concurrent tunnel.`

/** One sentence stating the TCP requirement, reused wherever it comes up. */
export const TCP_REQUIREMENT = `TCP tunnels require at least ${TCP_MIN_TUNNELS} tunnels`

export const TCP_REQUIREMENT_NOTE = `HTTP and UDP tunnels are available on every plan. `
  + `${TCP_REQUIREMENT} (add extra tunnels on Pro, or use the Team plan).`

const PRO: Plan = {
  id: 'pro',
  name: 'Pro',
  priceMonthly: 0,
  freeTunnels: 1,
  tagline: 'Perfect for hobbyists and individual developers working on side projects.',
  shortTagline: 'Perfect for individual developers.',
  features: [
    '1 free HTTP or UDP tunnel',
    'Unlimited custom domains',
    'Static subdomains',
    `TCP tunnels with ${TCP_MIN_TUNNELS}+ tunnels`,
    `${EXTRA_TUNNEL_PRICE_LABEL} per extra tunnel (${TCP_MIN_TUNNELS}+ pack)`
  ],
  cta: { label: 'Get Started', to: '/login' }
}

const TEAM: Plan = {
  id: 'team',
  name: 'Team',
  priceMonthly: 10,
  freeTunnels: 10,
  tagline: 'For growing teams that need more resources and collaboration features.',
  shortTagline: 'Built for teams and power users.',
  features: [
    '10 free tunnels included',
    'Team member management',
    'Priority email support',
    `${EXTRA_TUNNEL_PRICE_LABEL} per extra tunnel`
  ],
  cta: { label: 'Upgrade to Team', to: '/login' }
}

export const PLANS: Record<PlanId, Plan> = { pro: PRO, team: TEAM }

/** Plans in display order: cheapest first, which is also the order of the pricing cards. */
export const PLAN_LIST: readonly Plan[] = [PRO, TEAM]

/**
 * A feature's availability on a plan: `true`/`false` for a plain yes/no, or a string when the
 * answer needs a qualifier ("5+ tunnels", "Coming soon").
 */
export type FeatureAvailability = boolean | string

export type ComparisonRow = {
  feature: string
  pro: FeatureAvailability
  team: FeatureAvailability
}

export const PLAN_COMPARISON: readonly ComparisonRow[] = [
  { feature: 'HTTP Tunnels', pro: true, team: true },
  { feature: 'UDP Tunnels', pro: true, team: true },
  { feature: 'TCP Tunnels', pro: `${TCP_MIN_TUNNELS}+ tunnels`, team: true },
  { feature: 'SSL for HTTP', pro: true, team: true },
  { feature: 'Static Subdomains', pro: true, team: true },
  { feature: 'Custom Domains', pro: true, team: true },
  { feature: 'Private Tunnels', pro: true, team: true },
  { feature: 'Web Socket Support', pro: true, team: true },
  { feature: 'Free Tunnels', pro: pluralize(PRO.freeTunnels, 'tunnel'), team: pluralize(TEAM.freeTunnels, 'tunnel') },
  { feature: 'Extra Tunnels', pro: `${EXTRA_TUNNEL_PRICE_LABEL} each`, team: `${EXTRA_TUNNEL_PRICE_LABEL} each` },
  { feature: 'Team Members', pro: false, team: true },
  { feature: 'SSO', pro: false, team: 'Coming soon' },
  { feature: 'Support', pro: 'Community', team: 'Priority' }
]

/** Text alternatives for the check/cross icons in the comparison table. */
export const INCLUDED_LABEL = 'Included'
export const NOT_INCLUDED_LABEL = 'Not included'
