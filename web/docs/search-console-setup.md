# Search Console setup (Google + Bing)

Everything in this file that can be automated already is. What is left is the part that only you can
do, because it happens inside the Google and Bing web consoles.

## How verification is wired up

**Meta tag method (recommended).** Two build-time variables, both empty by default; when a variable
is empty the tag is not emitted at all:

| Variable | Emitted tag |
| --- | --- |
| `VITE_GOOGLE_SITE_VERIFICATION` | `<meta name="google-site-verification" content="…">` |
| `VITE_BING_SITE_VERIFICATION` | `<meta name="msvalidate.01" content="…">` |

They are injected into `<head>` by the `portbuddy-seo-assets` plugin in `web/vite.config.ts`, so they
appear on every page — the SPA shell and all prerendered routes. Set them the same way the other
`VITE_*` variables are set for a release build (`.github/workflows/ci.yml`, the
`build-backend` job's "Set environment variables" step):

```yaml
echo "VITE_GOOGLE_SITE_VERIFICATION=<token from GSC>" >> $GITHUB_ENV
echo "VITE_BING_SITE_VERIFICATION=<token from Bing>"  >> $GITHUB_ENV
```

Verify after deploying: `curl -s https://portbuddy.dev | grep -i verification`.

**File upload method (works too).** Anything in `web/public/` is copied to `web/dist/` and served from
the site root: the gateway exposes `web/dist/` as static content (`spring.web.resources.static-locations:
file:./web/dist/`) and, in the `prod` profile, no gateway route claims unknown root paths
(`spa-backend-enabled: false`, and the SPA fallback only matches an explicit list of paths). So:

1. Drop the file Google/Bing gives you into `web/public/` — e.g. `google1a2b3c4d.html` or
   `BingSiteAuth.xml`.
2. Rebuild and deploy the web image.
3. It is then served at `https://portbuddy.dev/google1a2b3c4d.html` with `Content-Type: text/html`
   (`.xml` files are served as XML).

Same mechanism as `robots.txt` and `sitemap.xml`, which are generated into `dist/` at build time from
`src/config/routes.ts`.

## Manual checklist — Google Search Console

- [ ] Open <https://search.google.com/search-console> and add a property.
      Prefer the **Domain** property (`portbuddy.dev`) — it needs one DNS TXT record and covers
      http/https and every subdomain. If you would rather not touch DNS, add a **URL prefix**
      property for `https://portbuddy.dev` and use the HTML-tag method below.
- [ ] For a Domain property: add the TXT record Google shows to the `portbuddy.dev` DNS zone, then
      press **Verify**.
- [ ] For a URL-prefix property: choose **HTML tag**, copy the `content` value only, set
      `VITE_GOOGLE_SITE_VERIFICATION` to it, deploy, then press **Verify**.
- [ ] Submit the sitemap: **Sitemaps** → enter `sitemap.xml` → **Submit**. Expect 8 discovered URLs.
- [ ] **URL inspection** on `https://portbuddy.dev`: confirm "URL is on Google" (or request
      indexing), and that the rendered HTML shows the comparison table content.
- [ ] Confirm the canonical Google picked matches ours: URL inspection → *Page indexing* →
      "User-declared canonical" should read `https://portbuddy.dev`.
- [ ] Check **Page indexing** for `Excluded by 'noindex'` and confirm the only entries are
      `/login`, `/register`, `/app/*` and the other account paths.
- [ ] Set the international targeting / preferred domain only if you actually need it (not required).
- [ ] Optional but useful: **Settings → Users and permissions**, add a second owner so verification
      does not depend on one account.

## Manual checklist — Bing Webmaster Tools

- [ ] Open <https://www.bing.com/webmasters> and add `https://portbuddy.dev`.
      The fastest route is **Import from Google Search Console** (do the GSC steps first) — it copies
      the property and the sitemap and skips verification entirely.
- [ ] If importing is not an option: choose **Meta tag**, copy the `content` value into
      `VITE_BING_SITE_VERIFICATION`, deploy, then press **Verify**.
- [ ] Submit the sitemap: **Sitemaps** → `https://portbuddy.dev/sitemap.xml`.
- [ ] Run **URL Inspection** on `https://portbuddy.dev` and check the "Live URL" tab renders the
      pricing table.
- [ ] **Robots.txt Tester**: confirm Bing reads the generated file and that the account paths are
      blocked.
- [ ] Turn on **IndexNow** if you want pushes on deploy (optional; needs an API key in the repo).

## After both are verified

- [ ] Re-run the structured-data checks in Google's [Rich Results
      Test](https://search.google.com/test/rich-results) for `/`, `/docs`,
      `/docs/guides/minecraft-server`, `/docs/guides/hytale-server` and `/install`.
      `npm run seo:check` validates the same blocks offline, but only Google can tell you which rich
      results it is willing to show.
- [ ] Watch **Enhancements → FAQ** in GSC for a week; if Google reports FAQ items it cannot see,
      compare the accordion text with `faqs` in `src/pages/Landing.tsx` (both come from that array,
      so a mismatch would mean a rendering bug, not a copy bug).
