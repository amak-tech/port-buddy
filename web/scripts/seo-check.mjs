#!/usr/bin/env node
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
 * Audits the built site (dist/) the way a crawler sees it. Run after `npm run build`:
 *
 *   node scripts/seo-check.mjs            # full audit, renders the private routes in Chromium
 *   node scripts/seo-check.mjs --no-browser
 *
 * Public routes are read from dist/sitemap.xml and private ones from the Disallow rules in
 * dist/robots.txt, so the script has no route list of its own to fall out of date.
 *
 * Exits non-zero on any failure, which is what makes it useful in CI.
 */

import fs from 'node:fs'
import http from 'node:http'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const SELF = fileURLToPath(import.meta.url)
const ROOT = path.resolve(path.dirname(SELF), '..')
const DIST = path.join(ROOT, 'dist')
const REPO = path.resolve(ROOT, '..')
const USE_BROWSER = !process.argv.includes('--no-browser')

const CHECKS = ['content', 'table', 'json-ld', 'canonical', 'robots', 'metadata', 'links']

// --- tiny HTML helpers (the built pages are machine-generated, so patterns are enough) ---

const ENTITIES = { amp: '&', lt: '<', gt: '>', quot: '"', apos: "'", nbsp: ' ', '#39': "'", '#x27': "'" }

function decodeEntities(value) {
  return value.replace(/&(#x?[0-9a-fA-F]+|[a-zA-Z]+);/g, (match, name) => {
    if (ENTITIES[name] !== undefined) return ENTITIES[name]
    if (name.startsWith('#x') || name.startsWith('#X')) return String.fromCodePoint(parseInt(name.slice(2), 16))
    if (name.startsWith('#')) return String.fromCodePoint(parseInt(name.slice(1), 10))
    return match
  })
}

function textOf(html) {
  return decodeEntities(html.replace(/<[^>]*>/g, '')).replace(/\s+/g, ' ').trim()
}

function head(html) {
  const end = html.indexOf('</head>')
  return end === -1 ? html : html.slice(0, end)
}

function metaContents(html, attr, value) {
  const pattern = new RegExp(`<meta\\b[^>]*\\b${attr}="${value}"[^>]*>`, 'g')
  return (html.match(pattern) ?? []).map((tag) => {
    const content = tag.match(/\bcontent="([^"]*)"/)
    return content ? decodeEntities(content[1]) : ''
  })
}

function titles(html) {
  return [...html.matchAll(/<title>([\s\S]*?)<\/title>/g)].map((match) => decodeEntities(match[1].trim()))
}

function canonicals(html) {
  return [...html.matchAll(/<link\b[^>]*\brel="canonical"[^>]*>/g)].map((match) => {
    const href = match[0].match(/\bhref="([^"]*)"/)
    return href ? decodeEntities(href[1]) : ''
  })
}

function jsonLdBlocks(html) {
  return [...html.matchAll(/<script[^>]*type="application\/ld\+json"[^>]*>([\s\S]*?)<\/script>/g)]
    .map((match) => match[1])
}

// --- schema.org validation ---

/** Walks a JSON-LD tree; every node must be typed and carry no empty strings. */
function validateNode(node, trailPath, problems) {
  if (Array.isArray(node)) {
    node.forEach((item, index) => validateNode(item, `${trailPath}[${index}]`, problems))
    return
  }
  if (node === null || typeof node !== 'object') {
    if (typeof node === 'string' && node.trim() === '') problems.push(`${trailPath} is an empty string`)
    return
  }
  const isReference = Object.keys(node).length === 1 && node['@id']
  if (!node['@type'] && !isReference) problems.push(`${trailPath} has no @type`)
  for (const [key, value] of Object.entries(node)) {
    validateNode(value, `${trailPath}.${key}`, problems)
  }
}

const REQUIRED_PROPERTIES = {
  TechArticle: ['headline', 'description', 'url'],
  SoftwareApplication: ['name', 'applicationCategory', 'operatingSystem', 'description', 'offers'],
  Organization: ['name', 'url', 'logo'],
  WebSite: ['name', 'url'],
  FAQPage: ['mainEntity'],
  BreadcrumbList: ['itemListElement'],
  HowTo: ['name', 'step'],
  Offer: ['price', 'priceCurrency'],
  Question: ['name', 'acceptedAnswer'],
  ListItem: ['position', 'name'],
  HowToStep: ['name', 'text'],
  HowToSection: ['name', 'itemListElement']
}

function validateRequired(node, trailPath, problems) {
  if (Array.isArray(node)) {
    node.forEach((item, index) => validateRequired(item, `${trailPath}[${index}]`, problems))
    return
  }
  if (node === null || typeof node !== 'object') return

  const type = node['@type']
  for (const property of REQUIRED_PROPERTIES[type] ?? []) {
    if (node[property] === undefined) problems.push(`${trailPath} (${type}) is missing "${property}"`)
  }
  for (const [key, value] of Object.entries(node)) {
    validateRequired(value, `${trailPath}.${key}`, problems)
  }
}

function collectTypes(node, found = new Set()) {
  if (Array.isArray(node)) {
    node.forEach((item) => collectTypes(item, found))
  } else if (node && typeof node === 'object') {
    if (typeof node['@type'] === 'string') found.add(node['@type'])
    Object.values(node).forEach((value) => collectTypes(value, found))
  }
  return found
}

// --- artefacts under audit ---

function readDist(file) {
  const target = path.join(DIST, file)
  if (!fs.existsSync(target)) throw new Error(`missing build artefact: dist/${file}`)
  return fs.readFileSync(target, 'utf8')
}

function sitemapUrls(xml) {
  return [...xml.matchAll(/<loc>([^<]+)<\/loc>/g)].map((match) => match[1])
}

function robotsDisallows(text) {
  return text.split('\n')
    .map((line) => line.match(/^Disallow:\s*(\S+)/))
    .filter(Boolean)
    .map((match) => match[1])
}

function pathnameOf(url) {
  return new URL(url).pathname
}

/** dist file that the gateway serves for a path: /docs -> dist/docs/index.html. */
function distFileFor(pathname) {
  const clean = pathname.replace(/^\/+|\/+$/g, '')
  return clean === '' ? 'index.html' : path.join(clean, 'index.html')
}

// --- per-route checks ---

function checkComparisonTable(html) {
  const problems = []
  const tables = [...html.matchAll(/<table[\s\S]*?<\/table>/g)].map((match) => match[0])
  if (tables.length === 0) return { applicable: false, problems }

  for (const table of tables) {
    if (!/<caption[\s\S]*?<\/caption>/.test(table)) problems.push('table has no <caption>')
    if (!/<th\b[^>]*\bscope="col"/.test(table)) problems.push('table has no <th scope="col">')
    if (!/<th\b[^>]*\bscope="row"/.test(table)) problems.push('table has no <th scope="row">')

    const rows = [...table.matchAll(/<tr[\s\S]*?<\/tr>/g)].map((match) => match[0])
    rows.forEach((row, rowIndex) => {
      for (const cell of row.matchAll(/<(td|th)\b[^>]*>([\s\S]*?)<\/\1>/g)) {
        if (textOf(cell[2]) === '') {
          problems.push(`row ${rowIndex + 1} has a <${cell[1]}> with no text (icon-only cell)`)
        }
      }
    })
  }
  return { applicable: true, problems }
}

function checkJsonLd(html, pathname) {
  const problems = []
  const blocks = jsonLdBlocks(html)
  const expected = pathname === '/'
    ? ['SoftwareApplication', 'Organization', 'WebSite', 'FAQPage']
    : pathname.startsWith('/docs/guides/')
      ? ['BreadcrumbList', 'HowTo']
      : pathname === '/install' || pathname === '/docs' || pathname.startsWith('/docs/')
        ? ['BreadcrumbList']
        : []

  if (blocks.length === 0) {
    // Legal/contact pages carry no structured data by design; only flag the pages that need it.
    return expected.length === 0
      ? { applicable: false, problems }
      : { applicable: true, problems: [`no JSON-LD on the page (expected ${expected.join(', ')})`] }
  }

  const parsed = []
  blocks.forEach((raw, index) => {
    try {
      parsed.push(JSON.parse(raw))
    } catch (error) {
      problems.push(`block ${index + 1} is not valid JSON: ${error.message}`)
    }
  })

  parsed.forEach((data, index) => {
    validateNode(data, `block ${index + 1}`, problems)
    validateRequired(data, `block ${index + 1}`, problems)
  })

  const types = collectTypes(parsed)
  for (const type of expected) {
    if (!types.has(type)) problems.push(`expected a ${type} block`)
  }

  // Nothing may be marked up that the page does not render.
  const pageText = textOf(html)
  const flat = parsed.flat()
  for (const data of flat) {
    if (data['@type'] === 'FAQPage') {
      for (const entry of data.mainEntity ?? []) {
        if (!pageText.includes(entry.name)) problems.push(`FAQ question not rendered on the page: "${entry.name}"`)
        const answer = entry.acceptedAnswer?.text ?? ''
        if (!pageText.includes(answer)) problems.push(`FAQ answer not rendered on the page: "${answer.slice(0, 60)}…"`)
      }
    }
    if (data['@type'] === 'HowTo') {
      const steps = (data.step ?? []).flatMap((step) => step['@type'] === 'HowToSection' ? step.itemListElement ?? [] : [step])
      for (const step of steps) {
        if (!pageText.includes(step.name)) problems.push(`HowTo step not rendered on the page: "${step.name}"`)
        if (!pageText.includes(step.text)) problems.push(`HowTo step text not rendered: "${step.text.slice(0, 60)}…"`)
      }
    }
    if (data['@type'] === 'TechArticle') {
      // The headline is the page's h1 and the description is its lead paragraph: both have to be
      // text the visitor actually sees.
      if (!pageText.includes(data.headline)) problems.push(`TechArticle headline not rendered: "${data.headline}"`)
      if (!pageText.includes(data.description)) {
        problems.push(`TechArticle description not rendered: "${String(data.description).slice(0, 60)}…"`)
      }
    }
    if (data['@type'] === 'BreadcrumbList') {
      (data.itemListElement ?? []).forEach((item, index) => {
        if (item.position !== index + 1) problems.push(`breadcrumb ${index + 1} has position ${item.position}`)
        if (item.item && !/^https?:\/\//.test(item.item)) problems.push(`breadcrumb ${index + 1} item is not absolute`)
      })
    }
    if (data['@type'] === 'SoftwareApplication') {
      for (const offer of data.offers ?? []) {
        if (!pageText.includes(`$${offer.price}`)) problems.push(`offer price $${offer.price} is not shown on the page`)
        if (offer.priceCurrency !== 'USD') problems.push(`offer ${offer.name} is not priced in USD`)
      }
    }
  }
  return { applicable: true, problems }
}

function checkCanonical(html, expectedUrl) {
  const problems = []
  const found = canonicals(head(html))
  if (found.length === 0) problems.push('no rel=canonical')
  if (found.length > 1) problems.push(`${found.length} rel=canonical tags: ${found.join(', ')}`)
  if (found[0] !== undefined && found[0] !== expectedUrl) {
    problems.push(`canonical "${found[0]}" != sitemap "${expectedUrl}"`)
  }

  const ogUrls = metaContents(head(html), 'property', 'og:url')
  if (ogUrls.length !== 1) problems.push(`${ogUrls.length} og:url tags`)
  else if (ogUrls[0] !== expectedUrl) problems.push(`og:url "${ogUrls[0]}" != canonical "${expectedUrl}"`)

  return { problems }
}

function checkRobotsMeta(html, expected) {
  const found = metaContents(head(html), 'name', 'robots')
  const problems = []
  if (found.length !== 1) problems.push(`${found.length} robots meta tags`)
  else if (found[0].replace(/\s/g, '') !== expected) problems.push(`robots is "${found[0]}", expected "${expected}"`)
  return { problems }
}

function checkMetadata(html) {
  const problems = []
  const pageHead = head(html)
  const pageTitles = titles(pageHead)
  const descriptions = metaContents(pageHead, 'name', 'description')

  if (pageTitles.length !== 1) problems.push(`${pageTitles.length} <title> tags`)
  else if (pageTitles[0] === '') problems.push('empty <title>')
  if (descriptions.length !== 1) problems.push(`${descriptions.length} meta descriptions`)
  else if (descriptions[0] === '') problems.push('empty meta description')

  for (const [property, attr] of [['og:description', 'property'], ['twitter:description', 'name']]) {
    const values = metaContents(pageHead, attr, property)
    if (values.length !== 1) problems.push(`${values.length} ${property} tags`)
    else if (descriptions[0] && values[0] !== descriptions[0]) {
      problems.push(`${property} does not describe this page (differs from meta description)`)
    }
  }
  if (/<meta\b[^>]*\bname="keywords"/.test(pageHead)) problems.push('meta keywords is still present')

  return { title: pageTitles[0], description: descriptions[0], problems }
}

/**
 * The page's own content has to be in the file the crawler is handed, not injected once React
 * boots: exactly one <h1>, and prose under it.
 *
 * The bar here is deliberately low — it asks "did this page render at all", not "is it any good".
 * Whether a page has enough content to stand on its own is a separate, advisory report, because a
 * thin page is an editorial problem and a blank one is a build failure.
 */
const UNRENDERED_WORDS = 40
const THIN_CONTENT_WORDS = 250

function bodyOf(html) {
  const start = html.indexOf('<body')
  return start === -1 ? html : html.slice(start)
}

/** Words of page prose: the chrome every page repeats (nav, sidebar, footer) does not count. */
function contentWords(html) {
  const stripped = bodyOf(html)
    .replace(/<script[\s\S]*?<\/script>/g, '')
    .replace(/<style[\s\S]*?<\/style>/g, '')
    .replace(/<nav\b[\s\S]*?<\/nav>/g, '')
    .replace(/<aside\b[\s\S]*?<\/aside>/g, '')
    .replace(/<footer\b[\s\S]*?<\/footer>/g, '')
  return textOf(stripped).split(/\s+/).filter(Boolean).length
}

function checkContent(html) {
  const problems = []
  const body = bodyOf(html)
  const headings = [...body.matchAll(/<h1\b[^>]*>([\s\S]*?)<\/h1>/g)].map((match) => textOf(match[1]))

  if (headings.length !== 1) problems.push(`${headings.length} <h1> elements, expected exactly 1`)
  else if (headings[0] === '') problems.push('the <h1> is empty')

  const words = contentWords(html)
  if (words < UNRENDERED_WORDS) {
    problems.push(`only ${words} words in the prerendered body (content is missing or client-injected)`)
  }
  return { heading: headings[0], words, problems }
}

// --- internal links ---

const ASSET_EXTENSIONS = /\.(png|jpg|jpeg|svg|ico|webmanifest|xml|txt|js|mjs|css|map|sh|ps1|json)$/i

function internalHrefs(html) {
  return [...html.matchAll(/\bhref="(\/[^"]*)"/g)]
    .map((match) => decodeEntities(match[1]))
    .filter((href) => !ASSET_EXTENSIONS.test(href.split(/[?#]/)[0]))
}

/**
 * A link resolves when it is a page in the sitemap, a static file in dist/, or one of the
 * client-only routes robots.txt disallows (the dashboard and the account flows).
 */
function makeLinkResolver(sitemapPaths, privatePrefixes) {
  return (href) => {
    const pathname = href.split(/[?#]/)[0]
    if (pathname === '' || pathname === '/') return true
    const clean = pathname.replace(/\/+$/, '') || '/'
    if (sitemapPaths.has(clean)) return true
    if (privatePrefixes.some((prefix) => clean === prefix || clean.startsWith(`${prefix}/`))) return true
    return fs.existsSync(path.join(DIST, distFileFor(clean))) || fs.existsSync(path.join(DIST, clean))
  }
}

function checkLinks(html, resolves) {
  const problems = []
  for (const href of new Set(internalHrefs(html))) {
    if (!resolves(href)) problems.push(`broken internal link: ${href}`)
  }
  return { problems }
}

// --- the anchor redirect shim ---

/**
 * Reads the shim's map out of the built /docs page rather than out of the source, so what is
 * asserted is what actually ships.
 */
function anchorRedirectMap(html) {
  const match = html.match(/<script>\(function\(\)\{try\{var m=(\{.*?\});/)
  if (!match) return null
  try {
    return JSON.parse(match[1])
  } catch {
    return null
  }
}

function checkAnchorRedirects(resolves) {
  const problems = []
  const docsIndex = path.join(DIST, 'docs', 'index.html')
  if (!fs.existsSync(docsIndex)) {
    return ['dist/docs/index.html is missing, so the anchor shim could not be checked']
  }

  const html = fs.readFileSync(docsIndex, 'utf8')
  const map = anchorRedirectMap(html)
  if (!map) {
    return ['the /docs anchor redirect shim is missing or unparseable']
  }

  for (const [hash, target] of Object.entries(map)) {
    if (!hash.startsWith('#')) problems.push(`anchor "${hash}" is not a fragment`)
    if (!resolves(target)) problems.push(`anchor ${hash} redirects to ${target}, which does not resolve`)
  }

  // The shim must exist on /docs and nowhere else, or an old link could bounce forever.
  for (const file of distHtmlFiles()) {
    if (file === docsIndex) continue
    if (anchorRedirectMap(fs.readFileSync(file, 'utf8'))) {
      problems.push(`the anchor shim also ships on ${path.relative(DIST, file)} (redirect loop risk)`)
    }
  }
  return problems
}

function distHtmlFiles(dir = DIST, found = []) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const target = path.join(dir, entry.name)
    if (entry.isDirectory()) distHtmlFiles(target, found)
    else if (entry.name.endsWith('.html')) found.push(target)
  }
  return found
}

// --- the rest of the repository ---

const SOURCE_EXTENSIONS = /\.(ts|tsx|js|jsx|mjs|html|md|sh|ps1|java|yml|yaml)$/
const SKIP_DIRECTORIES = new Set(['node_modules', 'dist', 'target', '.git', '.idea', 'pg_data', 'log', 'cert'])

function sourceFiles(dir, found = []) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    if (entry.isDirectory()) {
      if (!SKIP_DIRECTORIES.has(entry.name)) sourceFiles(path.join(dir, entry.name), found)
    } else if (SOURCE_EXTENSIONS.test(entry.name)) {
      found.push(path.join(dir, entry.name))
    }
  }
  return found
}

/**
 * The old anchors are redirects, not destinations: nothing in the repository should still link to
 * one. Written in two halves so this file does not match its own check.
 */
function checkNoDocsAnchors() {
  const needle = '/docs' + '#'
  const problems = []
  for (const file of sourceFiles(REPO)) {
    if (file === SELF) continue
    const contents = fs.readFileSync(file, 'utf8')
    if (!contents.includes(needle)) continue
    contents.split('\n').forEach((line, index) => {
      if (line.includes(needle)) {
        problems.push(`${path.relative(REPO, file)}:${index + 1} still links to a ${needle} anchor`)
      }
    })
  }
  return problems
}

/**
 * The gateway serves the prerendered HTML from an enumerated route, so a docs page that is in the
 * sitemap but not in that list would 404 in production however well it builds.
 */
function checkGatewayRoutes(docsPaths) {
  const config = path.join(REPO, 'gateway', 'src', 'main', 'resources', 'application.yml')
  if (!fs.existsSync(config)) {
    return ['gateway/src/main/resources/application.yml not found; docs routes were not verified']
  }

  const match = fs.readFileSync(config, 'utf8').match(/Path=\/docs\/\{page:([^}]+)\}/)
  if (!match) {
    return ['the gateway has no /docs/{page:…} route; the new docs pages would 404 in production']
  }

  const served = new Set(match[1].split('|').map((name) => `/docs/${name.trim()}`))
  const problems = []
  for (const docsPath of docsPaths) {
    if (!served.has(docsPath)) problems.push(`the gateway does not serve ${docsPath} (add it to static_docs_route)`)
  }
  for (const servedPath of served) {
    if (!docsPaths.includes(servedPath)) problems.push(`the gateway serves ${servedPath}, which is not in the sitemap`)
  }
  return problems
}

// --- private routes: rendered in Chromium, since they are client-side only ---

const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.mjs': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.webmanifest': 'application/manifest+json',
  '.xml': 'application/xml',
  '.txt': 'text/plain; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.ico': 'image/x-icon',
  '.map': 'application/json; charset=utf-8'
}

/** Serves dist/ the way the gateway does: static file, directory index, then SPA fallback. */
function serveDist() {
  const server = http.createServer((request, response) => {
    const pathname = decodeURIComponent(new URL(request.url, 'http://127.0.0.1').pathname)
    let file = path.join(DIST, pathname)
    if (fs.existsSync(file) && fs.statSync(file).isDirectory()) file = path.join(file, 'index.html')
    if (!fs.existsSync(file) || !fs.statSync(file).isFile()) file = path.join(DIST, 'index.html')
    response.writeHead(200, { 'Content-Type': MIME[path.extname(file)] ?? 'application/octet-stream' })
    fs.createReadStream(file).pipe(response)
  })
  return new Promise((resolve) => server.listen(0, '127.0.0.1', () => resolve(server)))
}

async function checkPrivateRoutes(paths) {
  const { default: puppeteer } = await import('puppeteer')
  const server = await serveDist()
  const origin = `http://127.0.0.1:${server.address().port}`
  const browser = await puppeteer.launch({ args: ['--no-sandbox', '--disable-setuid-sandbox'] })
  const results = []

  try {
    for (const pathname of paths) {
      const page = await browser.newPage()
      const problems = []
      try {
        await page.goto(`${origin}${pathname}`, { waitUntil: 'domcontentloaded', timeout: 30000 })
        await page.waitForFunction(
          () => document.querySelector('meta[name="robots"]')?.getAttribute('content')?.includes('noindex'),
          { timeout: 15000 }
        ).catch(() => {})
        const content = await page.evaluate(
          () => document.querySelector('meta[name="robots"]')?.getAttribute('content') ?? null
        )
        if (content === null) problems.push('no robots meta tag after render')
        else if (content.replace(/\s/g, '') !== 'noindex,nofollow') problems.push(`robots is "${content}"`)
      } catch (error) {
        problems.push(`render failed: ${error.message}`)
      } finally {
        await page.close()
      }
      results.push({ route: pathname, problems })
    }
  } finally {
    await browser.close()
    server.close()
  }
  return results
}

// --- report ---

function pad(value, width) {
  return String(value).padEnd(width)
}

function printTable(rows) {
  const routeWidth = Math.max(24, ...rows.map((row) => row.route.length + 2))
  const widths = CHECKS.map((check) => Math.max(check.length, 6) + 2)

  console.log(pad('route', routeWidth) + CHECKS.map((check, index) => pad(check, widths[index])).join(''))
  console.log('-'.repeat(routeWidth + widths.reduce((sum, width) => sum + width, 0)))
  for (const row of rows) {
    const cells = CHECKS.map((check, index) => pad(row.results[check] ?? '-', widths[index]))
    console.log(pad(row.route, routeWidth) + cells.join(''))
  }
}

async function main() {
  const failures = []
  const sitemap = readDist('sitemap.xml')
  const robots = readDist('robots.txt')

  // robots.txt / sitemap.xml sanity.
  const sitemapReference = robots.match(/^Sitemap:\s*(\S+)$/m)
  if (!sitemapReference) failures.push('robots.txt: no Sitemap line')
  else if (!/^https?:\/\//.test(sitemapReference[1])) failures.push('robots.txt: Sitemap is not an absolute URL')

  const urls = sitemapUrls(sitemap)
  if (urls.length === 0) failures.push('sitemap.xml: no <loc> entries')

  const disallowed = robotsDisallows(robots)
  for (const url of urls) {
    const pathname = pathnameOf(url)
    if (disallowed.some((rule) => pathname.startsWith(rule.replace(/\$$/, '')) && rule !== '/')) {
      failures.push(`sitemap.xml lists ${pathname}, which robots.txt disallows`)
    }
  }

  const sitemapPaths = new Set(urls.map((url) => pathnameOf(url).replace(/\/+$/, '') || '/'))
  const privatePrefixes = [...new Set(disallowed.map((rule) => rule.replace(/[$/]+$/, '')))]
    .filter((prefix) => prefix.startsWith('/') && prefix !== '/')
  const resolves = makeLinkResolver(sitemapPaths, privatePrefixes)

  const seenTitles = new Map()
  const seenDescriptions = new Map()
  const rows = []
  const thin = []

  for (const url of urls) {
    const pathname = pathnameOf(url)
    const results = {}
    const routeFailures = []
    let html

    try {
      html = readDist(distFileFor(pathname))
    } catch (error) {
      rows.push({ route: pathname, results: Object.fromEntries(CHECKS.map((check) => [check, 'FAIL'])) })
      failures.push(`${pathname}: ${error.message}`)
      continue
    }

    const content = checkContent(html)
    results.content = content.problems.length === 0 ? 'pass' : 'FAIL'
    routeFailures.push(...content.problems)
    if (content.words < THIN_CONTENT_WORDS) thin.push({ route: pathname, words: content.words })

    const links = checkLinks(html, resolves)
    results.links = links.problems.length === 0 ? 'pass' : 'FAIL'
    routeFailures.push(...links.problems)

    const table = checkComparisonTable(html)
    results.table = table.applicable ? (table.problems.length === 0 ? 'pass' : 'FAIL') : '-'
    routeFailures.push(...table.problems)

    const jsonLd = checkJsonLd(html, pathname)
    results['json-ld'] = jsonLd.applicable ? (jsonLd.problems.length === 0 ? 'pass' : 'FAIL') : '-'
    routeFailures.push(...jsonLd.problems)

    const canonical = checkCanonical(html, url)
    results.canonical = canonical.problems.length === 0 ? 'pass' : 'FAIL'
    routeFailures.push(...canonical.problems)

    const robotsMeta = checkRobotsMeta(html, 'index,follow')
    results.robots = robotsMeta.problems.length === 0 ? 'pass' : 'FAIL'
    routeFailures.push(...robotsMeta.problems)

    const metadata = checkMetadata(html)
    const duplicateTitle = metadata.title && seenTitles.has(metadata.title)
    const duplicateDescription = metadata.description && seenDescriptions.has(metadata.description)
    if (duplicateTitle) metadata.problems.push(`<title> duplicates ${seenTitles.get(metadata.title)}`)
    if (duplicateDescription) metadata.problems.push(`description duplicates ${seenDescriptions.get(metadata.description)}`)
    if (metadata.title) seenTitles.set(metadata.title, pathname)
    if (metadata.description) seenDescriptions.set(metadata.description, pathname)
    results.metadata = metadata.problems.length === 0 ? 'pass' : 'FAIL'
    routeFailures.push(...metadata.problems)

    rows.push({ route: pathname, results })
    failures.push(...routeFailures.map((problem) => `${pathname}: ${problem}`))
  }

  // Sitewide checks: the anchor shim, the links left in the repository, and the gateway's route
  // list. None of them belong to a single page, so they are reported outside the table.
  const docsPaths = [...sitemapPaths].filter((pathname) => pathname.startsWith('/docs/') && !pathname.startsWith('/docs/guides/'))
  const sitewide = [
    ...checkAnchorRedirects(resolves),
    ...checkNoDocsAnchors(),
    ...checkGatewayRoutes(docsPaths)
  ]
  failures.push(...sitewide)

  if (USE_BROWSER) {
    const privatePaths = [...new Set(disallowed
      .filter((rule) => rule.endsWith('$'))
      .map((rule) => rule.slice(0, -1)))]
    for (const result of await checkPrivateRoutes(privatePaths)) {
      rows.push({
        route: result.route,
        results: { robots: result.problems.length === 0 ? 'pass' : 'FAIL' }
      })
      failures.push(...result.problems.map((problem) => `${result.route}: ${problem}`))
    }
  } else {
    console.log('(skipping the rendered noindex check for private routes: --no-browser)\n')
  }

  printTable(rows)

  console.log(`\nrobots.txt: ${robots.split('\n').filter(Boolean).length} directives, sitemap.xml: ${urls.length} URLs`)
  console.log(`sitewide: anchor shim, ${docsPaths.length} docs routes, repo-wide anchor grep — ${sitewide.length === 0 ? 'pass' : `${sitewide.length} problem(s)`}`)

  if (thin.length > 0) {
    // Advisory, not a failure: a short page is an editorial call, and failing the build on one
    // would only invite padding, which ranks no better.
    console.log(`\n${thin.length} page(s) under ${THIN_CONTENT_WORDS} words of prose:`)
    for (const page of thin.sort((a, b) => a.words - b.words)) {
      console.log(`  - ${page.route}: ${page.words} words`)
    }
  }

  if (failures.length > 0) {
    console.error(`\n${failures.length} problem(s):`)
    for (const failure of failures) console.error(`  - ${failure}`)
    process.exitCode = 1
    return
  }
  console.log('\nAll SEO checks passed.')
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
