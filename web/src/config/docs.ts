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
 * The documentation manifest: one entry per docs URL, and the single source of truth for the
 * sidebar, the breadcrumbs, the prev/next chain, the index card grid, the per-page <head> metadata
 * and the sitemap entries.
 *
 * Docs used to be one page at /docs with hash anchors. The anchors are still linked from the
 * homepage, the README and outside sites, so {@link ANCHOR_REDIRECTS} maps every one of them onto
 * the route that replaced it. It lives here, next to the routes themselves, so a route rename can
 * not silently orphan an old link.
 *
 * Imported by both the app and `vite.config.ts` (which generates the head templates and the
 * client-side anchor shim from it), so it must stay free of JSX, browser APIs and env access.
 */

export type DocsGroupId = 'getting-started' | 'tunnels' | 'advanced' | 'guides'

export type DocsGroup = {
  id: DocsGroupId
  /** Sidebar heading. */
  label: string
}

/** Sidebar groups, in display order. */
export const DOCS_GROUPS: readonly DocsGroup[] = [
  { id: 'getting-started', label: 'Getting Started' },
  { id: 'tunnels', label: 'Tunnels' },
  { id: 'advanced', label: 'Advanced' },
  { id: 'guides', label: 'How-to Guides' }
]

export type DocsPage = {
  /** Path as served. No trailing slash, matching the site's canonical convention. */
  path: string
  group: DocsGroupId
  /** Sidebar label — short, since the sidebar is narrow. */
  navLabel: string
  /** The page's single <h1>, and the last entry in its breadcrumb trail. */
  heading: string
  /** <title>. Kept under 60 characters and leading with the topic rather than the brand. */
  title: string
  /** Meta description, 140–160 characters, describing this page and nothing else. */
  description: string
  /** One sentence for the card grid on the docs index. */
  summary: string
  /**
   * True where the page is a genuine how-to and earns a TechArticle block. Reference material and
   * the index itself get a BreadcrumbList only — marking up a link list as an article is a lie.
   */
  techArticle: boolean
}

/** The docs index. Listed separately because it is the parent of every other entry. */
export const DOCS_INDEX: DocsPage = {
  path: '/docs',
  group: 'getting-started',
  navLabel: 'Introduction',
  heading: 'Port Buddy Documentation',
  title: 'Documentation — Expose Local Ports | Port Buddy',
  description: 'Port Buddy documentation: expose a local port to the internet over HTTP, TCP or UDP. '
    + 'Set up the CLI, add a custom domain, or run a tunnel as a background service.',
  summary: 'How Port Buddy tunnels traffic from a public URL to a port on your machine.',
  techArticle: false
}

/** Child pages, in sidebar and prev/next order. */
export const DOCS_PAGES: readonly DocsPage[] = [
  {
    path: '/docs/authentication',
    group: 'getting-started',
    navLabel: 'Authentication',
    heading: 'Authentication',
    title: 'Authenticate the Port Buddy CLI with an API Token',
    description: 'Link the Port Buddy CLI to your account: generate an API token in the dashboard, '
      + 'run portbuddy init once, and every later tunnel command authenticates itself.',
    summary: 'Generate an API token and link the CLI to your account.',
    techArticle: true
  },
  {
    path: '/docs/http-tunnels',
    group: 'tunnels',
    navLabel: 'HTTP Tunnels',
    heading: 'HTTP Tunnels',
    title: 'HTTP Tunnels — Expose a Local Web Server',
    description: 'Expose a local web app or API to the internet over HTTPS with one command. '
      + 'Get a public portbuddy.dev URL, with WebSocket support and optional static subdomains.',
    summary: 'Share a local web app or API over HTTPS with one command.',
    techArticle: true
  },
  {
    path: '/docs/tcp-tunnels',
    group: 'tunnels',
    navLabel: 'TCP Tunnels',
    heading: 'TCP Tunnels',
    title: 'TCP Tunnels — Expose Postgres, SSH & RDP',
    description: 'Expose a local PostgreSQL, MySQL, SSH or RDP service through a raw TCP tunnel, '
      + 'and see the plan requirement that unlocks TCP mode before you start.',
    summary: 'Reach a local database, SSH or any raw TCP service remotely.',
    techArticle: true
  },
  {
    path: '/docs/udp-tunnels',
    group: 'tunnels',
    navLabel: 'UDP Tunnels',
    heading: 'UDP Tunnels',
    title: 'UDP Tunnels — Expose Game Servers & VoIP',
    description: 'Expose a local UDP service such as a game server, VoIP endpoint or IoT protocol. '
      + 'One command gives your local socket a public host and port.',
    summary: 'Expose game servers, VoIP and other UDP services.',
    techArticle: true
  },
  {
    path: '/docs/run-as-a-service',
    group: 'advanced',
    navLabel: 'Run as a Service',
    heading: 'Run as a Service',
    title: 'Run Port Buddy as a Background Service',
    description: 'Keep a tunnel up after you close the terminal or reboot. Install Port Buddy as a '
      + 'systemd unit on Linux or a Scheduled Task on Windows, then start, stop and inspect it.',
    summary: 'Keep tunnels up across reboots with systemd or Task Scheduler.',
    techArticle: true
  },
  {
    path: '/docs/custom-domains',
    group: 'advanced',
    navLabel: 'Custom Domains',
    heading: 'Custom Domains',
    title: 'Custom Domains — Use Your Own Domain',
    description: 'Serve a tunnel from a domain you own instead of a generated subdomain. '
      + 'Port Buddy issues and renews the SSL certificate for it automatically.',
    summary: 'Serve tunnels from your own domain, with SSL handled for you.',
    techArticle: true
  },
  {
    path: '/docs/private-tunnels',
    group: 'advanced',
    navLabel: 'Private Tunnels',
    heading: 'Private Tunnels',
    title: 'Private Tunnels — Passcode-Protect a URL',
    description: 'Put a passcode in front of an HTTP tunnel so only the people you share it with '
      + 'can load the page. Set one per tunnel from the CLI, or per domain in the dashboard.',
    summary: 'Require a passcode before anyone can load your tunnel.',
    techArticle: true
  },
  {
    path: '/docs/cli-reference',
    group: 'advanced',
    navLabel: 'CLI Reference',
    heading: 'CLI Reference',
    title: 'Port Buddy CLI Reference — Commands & Flags',
    description: 'Every Port Buddy CLI argument: the http, tcp and udp modes, the init command, and '
      + 'the domain, port-reservation, passcode, no-request-log and verbose options.',
    summary: 'Every command, mode and flag the portbuddy binary accepts.',
    techArticle: false
  }
]

export type DocsNavEntry = {
  path: string
  label: string
  group: DocsGroupId
  /** One sentence for the card grid on the docs index. */
  summary: string
  /**
   * True for sidebar entries that leave the docs tree. They are navigable and belong in the
   * grouping, but they are not part of the prev/next chain and own their metadata elsewhere.
   */
  external: boolean
}

/**
 * The how-to guides. They predate the split and render from their own data-driven component, so
 * they appear in the navigation without being {@link DocsPage} entries.
 */
export const DOCS_GUIDES: readonly { path: string, label: string, summary: string }[] = [
  {
    path: '/docs/guides/minecraft-server',
    label: 'Minecraft Server',
    summary: 'Host a Minecraft server for friends, Java or Bedrock edition.'
  },
  {
    path: '/docs/guides/hytale-server',
    label: 'Hytale Server',
    summary: 'Expose a local Hytale server without touching your router.'
  }
]

/**
 * Sidebar order, including the entries that point outside the docs tree.
 *
 * Installation sits in "Getting Started" but links to /install: the install page already covers it
 * in full, and a second page on the same topic would only compete with it.
 */
export const DOCS_NAV: readonly DocsNavEntry[] = [
  {
    path: DOCS_INDEX.path,
    label: DOCS_INDEX.navLabel,
    group: DOCS_INDEX.group,
    summary: DOCS_INDEX.summary,
    external: false
  },
  {
    path: '/install',
    label: 'Installation',
    group: 'getting-started',
    summary: 'Install the CLI on macOS, Linux or Windows.',
    external: true
  },
  ...DOCS_PAGES.map((page): DocsNavEntry => ({
    path: page.path,
    label: page.navLabel,
    group: page.group,
    summary: page.summary,
    external: false
  })),
  ...DOCS_GUIDES.map((guide): DocsNavEntry => ({
    path: guide.path,
    label: guide.label,
    group: 'guides',
    summary: guide.summary,
    external: false
  }))
]

/**
 * The prev/next chain: the docs pages in sidebar order, then the guides. /install is left out —
 * it is a different section of the site, not the next page of the manual.
 */
export const DOCS_SEQUENCE: readonly { path: string, label: string }[] = [
  { path: DOCS_INDEX.path, label: DOCS_INDEX.navLabel },
  ...DOCS_PAGES.map((page) => ({ path: page.path, label: page.navLabel })),
  ...DOCS_GUIDES
]

/** Every page that owns metadata here, index first. */
export const ALL_DOCS_PAGES: readonly DocsPage[] = [DOCS_INDEX, ...DOCS_PAGES]

export function docsPage(path: string): DocsPage | undefined {
  return ALL_DOCS_PAGES.find((page) => page.path === path)
}

export function docsPagesInGroup(group: DocsGroupId): readonly DocsPage[] {
  return DOCS_PAGES.filter((page) => page.group === group)
}

export function navEntriesInGroup(group: DocsGroupId): readonly DocsNavEntry[] {
  return DOCS_NAV.filter((entry) => entry.group === group)
}

export type DocsNeighbours = {
  previous?: { path: string, label: string }
  next?: { path: string, label: string }
}

/** The pages either side of `path` in {@link DOCS_SEQUENCE}. */
export function docsNeighbours(path: string): DocsNeighbours {
  const index = DOCS_SEQUENCE.findIndex((entry) => entry.path === path)
  if (index === -1) {
    return {}
  }
  return {
    previous: index > 0 ? DOCS_SEQUENCE[index - 1] : undefined,
    next: index < DOCS_SEQUENCE.length - 1 ? DOCS_SEQUENCE[index + 1] : undefined
  }
}

/**
 * Where each anchor of the old single-page /docs now lives.
 *
 * A fragment never reaches the server, so none of these can be a 301: they are applied on the
 * client, on /docs only, by the shim generated from this map. Unknown hashes are left alone.
 *
 * Two entries deliberately leave the docs tree. `#installation` was only ever a link to /install,
 * and `#pricing-limits` duplicated the homepage pricing section, which owns the pricing markup.
 */
export const ANCHOR_REDIRECTS: Readonly<Record<string, string>> = {
  '#introduction': '/docs',
  '#installation': '/install',
  '#authentication': '/docs/authentication',
  '#http-tunnels': '/docs/http-tunnels',
  '#tcp-tunnels': '/docs/tcp-tunnels',
  '#udp-tunnels': '/docs/udp-tunnels',
  '#run-as-service': '/docs/run-as-a-service',
  '#custom-domains': '/docs/custom-domains',
  '#private-tunnels': '/docs/private-tunnels',
  '#pricing-limits': '/#pricing'
}

/** The one page the anchor shim runs on. Destination pages must never carry it. */
export const ANCHOR_REDIRECT_SOURCE = DOCS_INDEX.path

/** Resolves an old anchor to its new location, or undefined when the hash is not one of ours. */
export function anchorRedirect(hash: string): string | undefined {
  return ANCHOR_REDIRECTS[hash]
}
