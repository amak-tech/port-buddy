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

/**
 * Extra tunnels are bought and released in blocks of this size. The server rejects any change that
 * is not a multiple of it (`app.subscriptions.tunnels.increment`, enforced in `UsersController`),
 * on both plans — so this is the real minimum purchase, not a discount pack.
 */
export const EXTRA_TUNNEL_BLOCK = 5

/**
 * Number of *purchased extra* tunnels that unlocks TCP on the free plan
 * (`app.subscriptions.tcp-min-extra-tunnels`, checked in `TunnelService.isTcpEnabled`).
 *
 * This counts extras only — the tunnel included with the plan does not count toward it. Saying
 * "5 tunnels" is wrong and has cost us conversions: someone who tops up to 5 *total* still gets
 * refused. Always describe this as extra tunnels, or state the total via {@link TCP_TOTAL_TUNNELS}.
 */
export const TCP_MIN_EXTRA_TUNNELS = 5

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

export const EXTRA_TUNNEL_NOTE = `${EXTRA_TUNNEL_PRICE_LABEL} for each additional concurrent tunnel, `
  + `bought in blocks of ${EXTRA_TUNNEL_BLOCK}.`

/** Monthly plan prices. Mirrored by the Stripe prices behind `STRIPE_PRICE_PRO` / `STRIPE_PRICE_TEAM`. */
const PRO_PRICE = 0
const TEAM_PRICE = 10

function money(amount: number): string {
  return `${PRICE_CURRENCY_SYMBOL}${amount}`
}

// --- TCP entitlement ---------------------------------------------------------------------------
//
// There are exactly two ways to get a TCP tunnel, and the cheapest of the two is the number every
// surface must quote. Both are computed here rather than written down, so a price or threshold
// change in one place cannot leave a stale figure somewhere else on the site.

/** Cost of the cheapest TCP-capable configuration on the free plan: the plan plus one block. */
const PRO_TCP_PRICE = PRO_PRICE + TCP_MIN_EXTRA_TUNNELS * EXTRA_TUNNEL_PRICE

/** Total concurrent tunnels that entry-level TCP configuration ends up with (1 included + 5 extra). */
export const TCP_TOTAL_TUNNELS = 1 + TCP_MIN_EXTRA_TUNNELS

/** The one number every TCP surface quotes: the cheapest monthly cost of a TCP tunnel. */
export const TCP_MIN_PRICE = Math.min(PRO_TCP_PRICE, TEAM_PRICE)

export const TCP_MIN_PRICE_LABEL = `${money(TCP_MIN_PRICE)}/month`

/** Feature grid, comparison table, pricing card — anywhere a full sentence does not fit. */
export const TCP_SHORT = `TCP tunnels from ${TCP_MIN_PRICE_LABEL}`

/** Protocols people actually come looking for. Kept concrete: these are the search terms. */
export const TCP_USE_CASES = 'Postgres, MySQL, Redis, SSH, RDP and Minecraft Java'

/** FAQ and /docs/tcp-tunnels. Two sentences: what it costs, what it unlocks, what stays free. */
export const TCP_LONG = `TCP tunnels start at ${TCP_MIN_PRICE_LABEL} — `
  + `${TCP_MIN_EXTRA_TUNNELS} extra tunnels at ${money(EXTRA_TUNNEL_PRICE)}/month each on the free plan, `
  + `or included with Team at ${money(TEAM_PRICE)}/month — and they carry anything that speaks TCP: `
  + `${TCP_USE_CASES}. HTTP and UDP tunnels are free and need no credit card.`

/** The precise rule, for people who want to know exactly what they are buying. */
export const TCP_ENTITLEMENT_DETAIL = `TCP switches on once your account holds `
  + `${TCP_MIN_EXTRA_TUNNELS} extra tunnels — ${TCP_TOTAL_TUNNELS} concurrent tunnels in total, since the `
  + `free plan already includes one — or as soon as you are on Team. Extra tunnels are sold in blocks of `
  + `${EXTRA_TUNNEL_BLOCK} at ${money(EXTRA_TUNNEL_PRICE)}/month each, so ${money(PRO_TCP_PRICE)}/month is the `
  + `smallest TCP purchase on the free plan.`

/** Free-tier clarity. Prominent on the homepage, because it is the first question people have. */
export const FREE_TIER_LINE = 'HTTP and UDP tunnels are free. No credit card.'

/**
 * Transfer policy. There is no metering anywhere in the stack — no byte accounting, no usage
 * columns, no rate limiter in `server`, `net-proxy` or `gateway` — so this is a statement of fact
 * about the implementation, not a marketing promise. If metering is ever added, change it here.
 */
export const BANDWIDTH_POLICY = 'Transfer is unmetered on every plan: no bandwidth caps, no overage charges.'

/** Short form of {@link BANDWIDTH_POLICY} for the comparison table. */
export const BANDWIDTH_SHORT = 'Unmetered'

/**
 * A neutral price comparison, kept as data so the figure and its provenance travel together.
 *
 * TODO(2026-11-12): re-verify against {@link NGROK_COMPARISON.sourceUrl} and bump `verifiedOn`.
 * This needs re-checking every quarter — a stale competitor price is the kind of claim that draws
 * complaints, and the rendered date qualifier is only honest if someone actually re-reads the page.
 */
export const NGROK_COMPARISON = {
  /** Cheapest ngrok plan that includes a reserved (static) TCP address. */
  planName: 'Hobbyist',
  /** Monthly price on annual billing. */
  priceAnnualMonthly: 8,
  /** Monthly price month-to-month. */
  priceMonthly: 10,
  sourceUrl: 'https://ngrok.com/pricing',
  /** Date the figures above were last read off the source page. */
  verifiedOn: '2026-08-12',
  /** Rendered qualifier, so the claim is never read as evergreen. */
  verifiedLabel: 'August 2026'
} as const

/**
 * Scoped deliberately to *reserved* TCP addresses: ngrok's free plan does offer randomly assigned
 * TCP addresses after card verification, so an unqualified "ngrok charges for TCP" would be wrong.
 */
export const NGROK_COMPARISON_LINE = `For comparison, ngrok's cheapest plan with a reserved TCP address is `
  + `${NGROK_COMPARISON.planName} at ${money(NGROK_COMPARISON.priceAnnualMonthly)}/month billed annually `
  + `(${money(NGROK_COMPARISON.priceMonthly)}/month month-to-month). ngrok pricing as of `
  + `${NGROK_COMPARISON.verifiedLabel}.`

const PRO: Plan = {
  id: 'pro',
  name: 'Pro',
  priceMonthly: PRO_PRICE,
  freeTunnels: 1,
  tagline: 'Perfect for hobbyists and individual developers working on side projects.',
  shortTagline: 'Perfect for individual developers.',
  features: [
    '1 free HTTP or UDP tunnel, no credit card',
    'Unlimited custom domains',
    'Static subdomains',
    BANDWIDTH_SHORT + ' transfer',
    TCP_SHORT,
    `${EXTRA_TUNNEL_PRICE_LABEL} per extra tunnel, in blocks of ${EXTRA_TUNNEL_BLOCK}`
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
    'TCP tunnels included',
    BANDWIDTH_SHORT + ' transfer',
    'Team member management',
    'Priority email support',
    `${EXTRA_TUNNEL_PRICE_LABEL} per extra tunnel, in blocks of ${EXTRA_TUNNEL_BLOCK}`
  ],
  cta: { label: 'Upgrade to Team', to: '/login' }
}

export const PLANS: Record<PlanId, Plan> = { pro: PRO, team: TEAM }

/** Plans in display order: cheapest first, which is also the order of the pricing cards. */
export const PLAN_LIST: readonly Plan[] = [PRO, TEAM]

/**
 * A feature's availability on a plan: `true`/`false` for a plain yes/no, or a string when the
 * answer needs a qualifier ("From $5/mo", "Coming soon"). A qualifier that states a threshold
 * without the price beside it is the bug this module exists to prevent — quote the money.
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
  { feature: 'TCP Tunnels', pro: `From ${money(TCP_MIN_PRICE)}/mo`, team: true },
  { feature: 'SSL for HTTP', pro: true, team: true },
  { feature: 'Static Subdomains', pro: true, team: true },
  { feature: 'Custom Domains', pro: true, team: true },
  { feature: 'Private Tunnels', pro: true, team: true },
  { feature: 'Web Socket Support', pro: true, team: true },
  { feature: 'Free Tunnels', pro: pluralize(PRO.freeTunnels, 'tunnel'), team: pluralize(TEAM.freeTunnels, 'tunnel') },
  {
    feature: 'Extra Tunnels',
    pro: `${EXTRA_TUNNEL_PRICE_LABEL} each`,
    team: `${EXTRA_TUNNEL_PRICE_LABEL} each`
  },
  { feature: 'Transfer', pro: BANDWIDTH_SHORT, team: BANDWIDTH_SHORT },
  { feature: 'Team Members', pro: false, team: true },
  { feature: 'SSO', pro: false, team: 'Coming soon' },
  { feature: 'Support', pro: 'Community', team: 'Priority' }
]

/** Text alternatives for the check/cross icons in the comparison table. */
export const INCLUDED_LABEL = 'Included'
export const NOT_INCLUDED_LABEL = 'Not included'
