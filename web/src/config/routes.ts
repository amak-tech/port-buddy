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
 * The route manifest: the single source of truth for which routes exist, which are prerendered,
 * and which are public enough to be indexed.
 *
 * This module is imported by both the app and `vite.config.ts` (which derives the prerender list,
 * sitemap.xml and robots.txt from it), so it must stay free of JSX, browser APIs and env access.
 */

/** Used when VITE_CANONICAL is not set (local builds, dev server). */
export const DEFAULT_SITE_ORIGIN = 'https://portbuddy.dev'

export type ChangeFreq = 'always' | 'hourly' | 'daily' | 'weekly' | 'monthly' | 'yearly' | 'never'

export type SiteRoute = {
  /** Path as served. No trailing slash — that is the site's canonical convention. */
  path: string
  /** Prerendered to static HTML at build time. */
  prerender: boolean
  /** Listed in sitemap.xml. Sitemap membership and indexability are deliberately the same flag. */
  sitemap: boolean
  changefreq?: ChangeFreq
  priority?: number
}

export const SITE_ROUTES: readonly SiteRoute[] = [
  // '/index' is a legacy alias that redirects to '/'; it is not prerendered (it would only rewrite
  // dist/index.html) and normalizePath() folds it onto '/' so it never gets its own canonical.
  { path: '/', prerender: true, sitemap: true, changefreq: 'daily', priority: 1.0 },
  { path: '/install', prerender: true, sitemap: true, changefreq: 'monthly', priority: 0.8 },
  { path: '/docs', prerender: true, sitemap: true, changefreq: 'monthly', priority: 0.8 },
  { path: '/docs/guides/minecraft-server', prerender: true, sitemap: true, changefreq: 'monthly', priority: 0.8 },
  { path: '/docs/guides/hytale-server', prerender: true, sitemap: true, changefreq: 'monthly', priority: 0.8 },
  { path: '/contacts', prerender: true, sitemap: true, changefreq: 'monthly', priority: 0.3 },
  { path: '/terms', prerender: true, sitemap: true, changefreq: 'monthly', priority: 0.3 },
  { path: '/privacy', prerender: true, sitemap: true, changefreq: 'monthly', priority: 0.3 }
]

/**
 * Path prefixes that must never be indexed: the authenticated dashboard plus the account flows
 * that lead into it. Used for both the `noindex,nofollow` meta tag and robots.txt.
 */
export const PRIVATE_PATH_PREFIXES: readonly string[] = [
  '/app',
  '/login',
  '/register',
  '/forgot-password',
  '/reset-password',
  '/auth',
  '/accept-invite',
  '/passcode'
]

/** Routes handed to the prerenderer. */
export function prerenderRoutes(): string[] {
  return SITE_ROUTES.filter((route) => route.prerender).map((route) => route.path)
}

/** Routes listed in sitemap.xml. */
export function sitemapRoutes(): SiteRoute[] {
  return SITE_ROUTES.filter((route) => route.sitemap)
}

/** Collapses trailing slashes and the legacy `/index` alias onto the canonical path. */
export function normalizePath(pathname: string): string {
  const stripped = pathname.replace(/\/+$/, '')
  return stripped === '' || stripped === '/index' ? '/' : stripped
}

/**
 * Builds the absolute URL for a path. Every canonical, og:url and sitemap entry goes through this
 * function so they match byte for byte: no trailing slash, not even on the homepage.
 */
export function absoluteUrl(origin: string, pathname: string): string {
  const base = origin.replace(/\/+$/, '')
  const path = normalizePath(pathname)
  return path === '/' ? base : base + path
}

/** True for paths that belong in the index (i.e. the ones listed in the sitemap). */
export function isIndexablePath(pathname: string): boolean {
  const path = normalizePath(pathname)
  return SITE_ROUTES.some((route) => route.sitemap && route.path === path)
}

/** True for the authenticated area and the account flows around it. */
export function isPrivatePath(pathname: string): boolean {
  const path = normalizePath(pathname)
  return PRIVATE_PATH_PREFIXES.some((prefix) => path === prefix || path.startsWith(`${prefix}/`))
}
