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

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const DIST = path.join(ROOT, 'dist')
const USE_BROWSER = !process.argv.includes('--no-browser')

const CHECKS = ['table', 'json-ld', 'canonical', 'robots', 'metadata']

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
    : pathname === '/install' || pathname === '/docs'
      ? ['BreadcrumbList']
      : pathname.startsWith('/docs/guides/')
        ? ['BreadcrumbList', 'HowTo']
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

  const seenTitles = new Map()
  const seenDescriptions = new Map()
  const rows = []

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
