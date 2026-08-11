import { defineConfig, loadEnv, type Plugin } from 'vite'
import react from '@vitejs/plugin-react'
import Prerender from '@prerenderer/rollup-plugin'
import Renderer from '@prerenderer/renderer-puppeteer'
import path from 'path'
import fs from 'fs'
import {
  DEFAULT_SITE_ORIGIN,
  PRIVATE_PATH_PREFIXES,
  absoluteUrl,
  prerenderRoutes,
  sitemapRoutes
} from './src/config/routes'
import { ANCHOR_REDIRECTS, ANCHOR_REDIRECT_SOURCE, docsPage } from './src/config/docs'

const OUT_DIR = 'dist'

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

type HeadTag = {
  /** The tag exactly as written in the template. */
  html: string
  /** Matches the tag(s) in the rendered page that this one supersedes. */
  removal: RegExp
}

/**
 * Collects the <meta>/<link> tags a page template contributes, each with a pattern matching the
 * tag it replaces in the rendered HTML. Identity is `name`/`property` for meta and `rel` for link,
 * which is what makes "one description per page" and "one canonical per page" hold. Tags without an
 * identity (<meta charset>) are skipped — the base index.html already provides them.
 */
function templateHeadTags(templateHtml: string): HeadTag[] {
  const head = templateHtml.match(/<head>([\s\S]*?)<\/head>/)
  if (!head) {
    return []
  }

  const tags: HeadTag[] = []
  for (const match of head[1].matchAll(/<(meta|link)\b[^>]*>/g)) {
    const tag = match[0]
    const metaKey = tag.match(/\b(?:name|property)="([^"]+)"/)
    const linkKey = tag.match(/\brel="([^"]+)"/)

    if (match[1] === 'meta' && metaKey) {
      tags.push({
        html: tag,
        removal: new RegExp(`[ \\t]*<meta\\b[^>]*\\b(?:name|property)="${escapeRegExp(metaKey[1])}"[^>]*>\\s*`, 'g')
      })
    } else if (match[1] === 'link' && linkKey) {
      tags.push({
        html: tag,
        removal: new RegExp(`[ \\t]*<link\\b[^>]*\\brel="${escapeRegExp(linkKey[1])}"[^>]*>\\s*`, 'g')
      })
    }
  }
  return tags
}

/** Applies a page template's <title> and head tags to the prerendered HTML of that route. */
function mergeTemplateHead(html: string, templateHtml: string): string {
  let out = html

  const templateTitle = templateHtml.match(/<title>[\s\S]*?<\/title>/)
  if (templateTitle) {
    let seen = false
    out = out.replace(/<title>[\s\S]*?<\/title>/g, () => {
      if (seen) {
        return ''
      }
      seen = true
      return templateTitle[0]
    })
    if (!seen) {
      out = out.replace('</head>', `  ${templateTitle[0]}\n</head>`)
    }
  }

  const tags = templateHeadTags(templateHtml)
  for (const tag of tags) {
    out = out.replace(tag.removal, '')
  }

  const insertion = tags.map((tag) => `    ${tag.html}\n`).join('')
  return out.replace('</head>', `${insertion}  </head>`)
}

/** Locates the head template that supplies metadata for a prerendered route. */
function templatePathFor(route: string): string | null {
  if (route.startsWith('/docs/guides/')) {
    const guide = route.substring('/docs/guides/'.length)
    const guidePath = `public/pages/docs/guides/${guide}.html`
    if (fs.existsSync(path.join(__dirname, guidePath))) {
      return guidePath
    }
  }
  if (route === '/' || route === '/index') return 'public/pages/index.html'
  if (route === '/install') return 'public/pages/install.html'
  // /docs and its child pages are generated from the docs manifest by docsTemplate().
  if (route === '/privacy') return 'public/pages/privacy.html'
  if (route === '/terms') return 'public/pages/terms.html'
  if (route === '/contacts') return 'public/pages/contacts.html'
  return null
}

function escapeAttribute(value: string): string {
  return value.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')
}

/**
 * Builds the head template for a documentation page out of its manifest entry, so a page's title,
 * description, canonical and social tags all come from the same record the sidebar and the sitemap
 * read. Nine near-identical HTML files would only be nine chances to let one drift.
 *
 * og:type is `article` to match the guides: every docs page below the index is prose about one
 * topic, not a site landing page.
 */
function docsTemplate(route: string, origin: string): string | null {
  const page = docsPage(route)
  if (!page) {
    return null
  }

  const url = absoluteUrl(origin, page.path)
  const title = escapeAttribute(page.title)
  const description = escapeAttribute(page.description)
  const tags = [
    `<title>${title}</title>`,
    `<meta name="description" content="${description}" />`,
    `<meta property="og:title" content="${title}" />`,
    `<meta property="og:description" content="${description}" />`,
    `<meta property="og:url" content="${url}" />`,
    ...(page.path === '/docs' ? [] : ['<meta property="og:type" content="article" />']),
    `<meta name="twitter:title" content="${title}" />`,
    `<meta name="twitter:description" content="${description}" />`,
    `<link rel="canonical" href="${url}" />`
  ]

  return `<!DOCTYPE html><html lang="en"><head>\n${tags.map((tag) => `    ${tag}`).join('\n')}\n</head><body></body></html>`
}

/**
 * The client-side shim for the anchors of the old single-page docs.
 *
 * A fragment is never sent to the server, so an old anchor link cannot be answered with a 301. This
 * runs inline in <head> — before first paint, so there is no flash of the index, and with no
 * network work of its own, so it does not hold the paint up either.
 *
 * It is injected into /docs and nowhere else, and it checks the pathname again at runtime, so it
 * can never fire on a destination page and loop. An unknown hash is left completely alone.
 */
function anchorRedirectScript(): string {
  const map = JSON.stringify(ANCHOR_REDIRECTS)
  const source = JSON.stringify(ANCHOR_REDIRECT_SOURCE)
  return '<script>(function(){try{var m=' + map + ';'
    + 'if(location.pathname.replace(/\\/+$/,"")!==' + source + ')return;'
    + 'var t=m[location.hash];'
    + 'if(t&&t!==location.pathname)location.replace(t)}catch(e){}})();</script>'
}

function buildSitemap(origin: string): string {
  const urls = sitemapRoutes().map((route) => {
    const lines = [`    <loc>${absoluteUrl(origin, route.path)}</loc>`]
    // Element order matters: the sitemap schema fixes it as loc, lastmod, changefreq, priority.
    if (route.changefreq) {
      lines.push(`    <changefreq>${route.changefreq}</changefreq>`)
    }
    if (route.priority !== undefined) {
      lines.push(`    <priority>${route.priority.toFixed(1)}</priority>`)
    }
    return `  <url>\n${lines.join('\n')}\n  </url>`
  })

  return '<?xml version="1.0" encoding="UTF-8"?>\n'
    + '<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n'
    + `${urls.join('\n')}\n`
    + '</urlset>\n'
}

function buildRobots(origin: string): string {
  const disallow = PRIVATE_PATH_PREFIXES.flatMap((prefix) => [`Disallow: ${prefix}$`, `Disallow: ${prefix}/`])

  return [
    '# Generated at build time from src/config/routes.ts — do not edit by hand.',
    'User-agent: *',
    'Allow: /',
    ...disallow,
    '# Head templates for the prerenderer, not pages.',
    'Disallow: /pages/',
    '',
    `Sitemap: ${origin}/sitemap.xml`,
    ''
  ].join('\n')
}

/**
 * Generates robots.txt and sitemap.xml from the route manifest so they cannot go stale, and injects
 * the search-console verification tags when their environment variables are set.
 */
function seoAssets(env: Record<string, string>): Plugin {
  const origin = (env.VITE_CANONICAL || '').trim().replace(/\/+$/, '') || DEFAULT_SITE_ORIGIN

  return {
    name: 'portbuddy-seo-assets',
    // Both files are build artefacts, so the dev server generates them on the fly rather than
    // leaving them 404 (they used to be checked-in files under public/).
    configureServer(server) {
      server.middlewares.use((request, response, next) => {
        const url = (request.url || '').split('?')[0]
        if (url === '/robots.txt') {
          response.setHeader('Content-Type', 'text/plain; charset=utf-8')
          response.end(buildRobots(origin))
          return
        }
        if (url === '/sitemap.xml') {
          response.setHeader('Content-Type', 'application/xml')
          response.end(buildSitemap(origin))
          return
        }
        next()
      })
    },
    transformIndexHtml() {
      const google = (env.VITE_GOOGLE_SITE_VERIFICATION || '').trim()
      const bing = (env.VITE_BING_SITE_VERIFICATION || '').trim()
      const tags = []

      if (google) {
        tags.push({ tag: 'meta', attrs: { name: 'google-site-verification', content: google }, injectTo: 'head' as const })
      }
      if (bing) {
        tags.push({ tag: 'meta', attrs: { name: 'msvalidate.01', content: bing }, injectTo: 'head' as const })
      }
      return tags
    },
    closeBundle() {
      const outDir = path.join(__dirname, OUT_DIR)
      if (!fs.existsSync(outDir)) {
        return
      }
      fs.writeFileSync(path.join(outDir, 'sitemap.xml'), buildSitemap(origin), 'utf8')
      fs.writeFileSync(path.join(outDir, 'robots.txt'), buildRobots(origin), 'utf8')
      if (!env.VITE_CANONICAL) {
        this.warn(`VITE_CANONICAL is not set; robots.txt and sitemap.xml fell back to ${origin}`)
      }
    }
  }
}

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd());
  // Resolved exactly as the sitemap resolves it, so generated canonicals match sitemap entries byte
  // for byte even when VITE_CANONICAL carries a trailing slash.
  const origin = (env.VITE_CANONICAL || '').trim().replace(/\/+$/, '') || DEFAULT_SITE_ORIGIN;

  return {
    define: {
      'process.env.NODE_ENV': JSON.stringify('development')
    },
    plugins: [
      react(),
      Prerender({
        routes: prerenderRoutes(),
        renderer: new Renderer({
          renderAfterDocumentEvent: 'render-event',
        }),
        staticDir: path.join(__dirname, OUT_DIR),
        postProcess(renderedRoute) {
          const route = renderedRoute.route;
          renderedRoute.html = renderedRoute.html.replace(
            '<div id="root">',
            `<div id="root" data-prerendered-route="${route}">`
          );

          let template = docsTemplate(route, origin);

          if (!template) {
            const templatePath = templatePathFor(route);
            if (templatePath) {
              template = fs.readFileSync(path.join(__dirname, templatePath), 'utf8');

              // Replace environment variables
              Object.keys(env).forEach((key) => {
                if (key.startsWith('VITE_')) {
                  template = (template as string).replace(new RegExp(`%${key}%`, 'g'), env[key]);
                }
              });
            }
          }

          if (template) {
            renderedRoute.html = mergeTemplateHead(renderedRoute.html, template);
          }

          if (route === ANCHOR_REDIRECT_SOURCE) {
            renderedRoute.html = renderedRoute.html.replace(
              '</head>',
              `  ${anchorRedirectScript()}\n</head>`
            );
          }
          return renderedRoute;
        }
      }),
      seoAssets(env),
    ],
  build: {
    minify: false,
    sourcemap: true,
  },
};
});
