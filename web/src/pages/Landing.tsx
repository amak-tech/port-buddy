import { Link } from 'react-router-dom'

export default function Landing() {
  return (
    <div>
      {/* Hero */}
      <section className="container py-20 md:py-28">
        <div className="grid md:grid-cols-2 gap-12 items-center">
          <div>
            <h1 className="text-4xl md:text-6xl font-extrabold leading-tight">
              Expose local ports to the internet in seconds
            </h1>
            <p className="text-white/70 mt-6 text-lg">
              Port Buddy is a simple, developer-friendly alternative to ngrok. Share your local HTTP and TCP services securely with a single command.
            </p>
            <div className="mt-8 flex flex-wrap gap-4">
              <Link to="/install" className="btn">Install CLI</Link>
              <a href="#pricing" className="btn" aria-label="View pricing plans">Pricing</a>
              <Link to="/app" className="btn" aria-label="Open your dashboard">Open App</Link>
            </div>
            <p className="mt-2 text-xs text-white/50">No credit card required to start. Free local testing with rate limits.</p>
          </div>
          <div>
            <div className="bg-black/30 border border-white/10 rounded-xl p-6">
              <div className="text-sm text-white/60">Quick start</div>
              <pre className="mt-3 bg-black/30 border border-white/10 rounded-lg p-4 overflow-auto text-sm" aria-label="CLI usage examples">
{`# HTTP example
$ port-buddy 3000
http://localhost:3000 exposed to: https://random-subdomain.port-buddy.com

# TCP example (e.g., Postgres)
$ port-buddy tcp 127.0.0.1:5432
tcp 127.0.0.1:5432 exposed to: tcp-proxy-3.port-buddy.com:43452`}
              </pre>
              <p className="text-xs text-white/50 mt-3">Supports HTTP, WebSocket, and raw TCP.</p>
            </div>
          </div>
        </div>

        {/* Trust / badges */}
        <div className="mt-12 grid grid-cols-2 md:grid-cols-4 gap-4 text-xs text-white/40">
          <Badge text="OAuth2 + JWT" />
          <Badge text="TLS everywhere" />
          <Badge text="Dockerized server" />
          <Badge text="PostgreSQL-backed" />
        </div>
      </section>

      {/* Features */}
      <section id="features" className="container py-16">
        <h2 className="text-2xl font-bold">Main Functionality</h2>
        <div className="grid md:grid-cols-3 gap-6 mt-6 text-white/80">
          <Feature title="HTTP & WebSocket tunneling" desc="Proxy HTTP(S) and WebSocket traffic to your local web app." />
          <Feature title="TCP proxying" desc="Expose databases and custom TCP services securely to the public internet." />
          <Feature title="Auth & subscriptions" desc="Sign in with Google/GitHub, manage API tokens, track usage." />
          <Feature title="Usage limits" desc="Traffic quotas by plan with clear daily caps to prevent surprises." />
          <Feature title="CLI simplicity" desc="One binary, intuitive commands via PicoCLI; optional GraalVM native build." />
          <Feature title="Observability" desc="Dashboard to view traffic stats and manage subscription." />
        </div>
      </section>

      {/* How it works */}
      <section id="how-it-works" className="container py-16">
        <h2 className="text-2xl font-bold">How it works</h2>
        <ol className="grid md:grid-cols-3 gap-6 mt-6 text-white/80 list-decimal list-inside">
          <li className="bg-black/30 border border-white/10 rounded-xl p-6">Install the CLI and authenticate: <code className="bg-black/50 px-1 rounded">port-buddy init {'{API_TOKEN}'}</code>.</li>
          <li className="bg-black/30 border border-white/10 rounded-xl p-6">Run a tunnel: <code className="bg-black/50 px-1 rounded">port-buddy [http|tcp] [host:]port</code>.</li>
          <li className="bg-black/30 border border-white/10 rounded-xl p-6">Share the generated public URL or TCP endpoint with anyone.</li>
        </ol>
        <p className="text-xs text-white/50 mt-3" id="docs">Full documentation coming soon.</p>
      </section>

      {/* Use cases */}
      <section id="use-cases" className="container py-16">
        <h2 className="text-2xl font-bold">Use Cases</h2>
        <ul className="grid md:grid-cols-2 gap-4 mt-6 list-disc list-inside text-white/80">
          <li>Share your in-progress web app with teammates or clients</li>
          <li>Test webhooks from third-party services on your local machine</li>
          <li>Grant temporary access to a database in your private network</li>
          <li>Demo features without deploying to staging</li>
        </ul>
      </section>

      {/* Security & limits */}
      <section id="security" className="container py-16">
        <h2 className="text-2xl font-bold">Security and limits</h2>
        <div className="grid md:grid-cols-3 gap-6 mt-6 text-white/80">
          <Feature title="OAuth2 sign-in" desc="Login with Google or GitHub; issue API tokens for CLI." />
          <Feature title="JWT secured APIs" desc="Short-lived tokens, server-side validation, and revocation controls." />
          <Feature title="Plan-based caps" desc="Daily traffic quotas per plan; fair usage to keep service reliable." />
        </div>
      </section>

      {/* Pricing */}
      <section id="pricing" className="container py-16">
        <h2 className="text-2xl font-bold">Pricing</h2>
        <div className="grid md:grid-cols-3 gap-6 mt-6">
          <PlanCard name="basic" price="$5" features={[
            'HTTP connections only',
            'Up to 3Gb traffic per day',
          ]} />
          <PlanCard name="individual" price="$10" features={[
            'Everything in basic',
            'TCP connections',
            'Up to 6Gb traffic per day',
          ]} />
          <PlanCard name="professional" price="$20" features={[
            'Everything in individual',
            'Up to 20Gb traffic per day',
          ]} />
        </div>
        <p className="text-xs text-white/50 mt-4">Prices in USD per month.</p>
      </section>

      {/* FAQ */}
      <section id="faq" className="container py-16">
        <h2 className="text-2xl font-bold">Frequently asked questions</h2>
        <div className="mt-6 grid md:grid-cols-2 gap-6">
          <Faq q="Is there a free tier?" a="You can start without a subscription for quick tests with strict rate limits. Upgrade anytime for higher limits and TCP support." />
          <Faq q="Do you support custom domains?" a="Planned. Today we generate secure random subdomains; bring-your-own-domain is on the roadmap." />
          <Faq q="Is traffic encrypted?" a="Yes, TLS is enforced end-to-end for HTTP(S). TCP forwarding is proxied through secure infrastructure." />
          <Faq q="Can I cancel anytime?" a="Yes, manage your subscription in the App dashboard." />
        </div>
      </section>

      {/* Final CTA */}
      <section className="container py-20">
        <div className="bg-black/30 border border-white/10 rounded-xl p-8 text-center">
          <h3 className="text-2xl font-bold">Ready to share your local app?</h3>
          <p className="text-white/70 mt-2">Install the CLI and expose a port in under a minute.</p>
          <div className="mt-6 flex justify-center gap-4">
            <Link to="/install" className="btn">Get Started</Link>
            <a href="#pricing" className="btn">View Pricing</a>
          </div>
        </div>
      </section>
    </div>
  )
}

function Badge({ text }: { text: string }) {
  return (
    <div className="bg-black/20 border border-white/10 rounded px-3 py-2 text-center" aria-label={text}>{text}</div>
  )
}

function Feature({ title, desc }: { title: string, desc: string }) {
  return (
    <div className="bg-black/30 border border-white/10 rounded-xl p-6">
      <div className="text-accent font-semibold">{title}</div>
      <div className="text-white/70 mt-2 text-sm">{desc}</div>
    </div>
  )
}

function PlanCard({ name, price, features }: { name: string, price: string, features: string[] }) {
  return (
    <div className="bg-black/30 border border-white/10 rounded-xl p-6 flex flex-col">
      <div className="flex items-baseline gap-2">
        <div className="badge capitalize">{name}</div>
        <div className="text-2xl font-bold">{price}<span className="text-white/50 text-base">/mo</span></div>
      </div>
      <ul className="mt-4 text-sm text-white/80 space-y-2 list-disc list-inside">
        {features.map((f, i) => <li key={i}>{f}</li>)}
      </ul>
      <Link to="/app" className="btn mt-6">Choose plan</Link>
    </div>
  )
}

function Faq({ q, a }: { q: string, a: string }) {
  return (
    <div className="bg-black/30 border border-white/10 rounded-xl p-6">
      <div className="font-semibold">{q}</div>
      <div className="text-white/70 mt-2 text-sm">{a}</div>
    </div>
  )
}
