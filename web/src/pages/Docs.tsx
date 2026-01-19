/*
 * Copyright (c) 2026 AMAK Inc. All rights reserved.
 */

import { Link } from 'react-router-dom'
import Seo from '../components/Seo'
import {
  CommandLineIcon,
  GlobeAltIcon,
  ShieldCheckIcon,
  BoltIcon,
  LockClosedIcon,
  BookOpenIcon,
  InformationCircleIcon
} from '@heroicons/react/24/outline'

export default function Docs() {
  return (
    <div className="min-h-screen flex flex-col">
      <Seo 
        title="Documentation - Port Buddy"
        description="Learn how to use Port Buddy to expose your local services to the internet. Documentation for HTTP, TCP, and UDP tunnels."
        keywords="port buddy docs, port buddy documentation, how to use port buddy, port buddy tutorial"
        path="/docs"
      />
      
      <div className="flex-1 relative pt-12 md:pt-20 pb-20">
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-indigo-900/20 via-slate-900/0 to-slate-900/0 pointer-events-none" />
        
        <div className="container max-w-5xl relative z-10">
          <div className="flex flex-col md:flex-row gap-12">
            {/* Sidebar Navigation */}
            <aside className="md:w-64 flex-shrink-0">
              <div className="sticky top-24 space-y-8">
                <div>
                  <h3 className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-4 px-2">Getting Started</h3>
                  <nav className="space-y-1">
                    <SidebarLink href="#introduction" label="Introduction" />
                    <SidebarLink href="#installation" label="Installation" />
                    <SidebarLink href="#authentication" label="Authentication" />
                  </nav>
                </div>
                <div>
                  <h3 className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-4 px-2">Tunnels</h3>
                  <nav className="space-y-1">
                    <SidebarLink href="#http-tunnels" label="HTTP Tunnels" />
                    <SidebarLink href="#tcp-tunnels" label="TCP Tunnels" />
                    <SidebarLink href="#udp-tunnels" label="UDP Tunnels" />
                  </nav>
                </div>
                <div>
                  <h3 className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-4 px-2">Advanced</h3>
                  <nav className="space-y-1">
                    <SidebarLink href="#custom-domains" label="Custom Domains" />
                    <SidebarLink href="#private-tunnels" label="Private Tunnels" />
                    <SidebarLink href="#pricing-limits" label="Pricing & Limits" />
                  </nav>
                </div>
              </div>
            </aside>

            {/* Main Content */}
            <main className="flex-1 max-w-3xl">
              <header className="mb-12">
                <div className="inline-flex items-center gap-2 text-indigo-400 font-medium mb-4">
                  <BookOpenIcon className="w-5 h-5" />
                  <span>Documentation</span>
                </div>
                <h1 className="text-4xl font-bold text-white mb-4">Introduction</h1>
                <p className="text-slate-400 text-lg leading-relaxed">
                  Port Buddy is a tool that allows you to share a port opened on your local host or private network to the public network. 
                  It's perfect for testing webhooks, sharing your work with clients, or exposing any local service securely.
                </p>
              </header>

              <section id="introduction" className="mb-16 scroll-mt-24">
                <h2 className="text-2xl font-bold text-white mb-6">How it works</h2>
                <p className="text-slate-400 mb-6">
                  Port Buddy works as a reverse proxy. When you run the CLI, it establishes a secure connection to our edge servers. 
                  Any traffic sent to your unique Port Buddy URL is then proxied through this connection to your local machine.
                </p>
                <div className="bg-slate-900 border border-slate-800 rounded-xl p-6">
                  <h3 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
                    <InformationCircleIcon className="w-5 h-5 text-indigo-400" />
                    Key Features
                  </h3>
                  <ul className="grid sm:grid-cols-2 gap-4">
                    <FeatureItem icon={<GlobeAltIcon className="w-5 h-5" />} label="HTTP/HTTPS Tunnels" />
                    <FeatureItem icon={<ShieldCheckIcon className="w-5 h-5" />} label="TCP & UDP Support" />
                    <FeatureItem icon={<BoltIcon className="w-5 h-5" />} label="WebSocket Support" />
                    <FeatureItem icon={<LockClosedIcon className="w-5 h-5" />} label="Custom Domains" />
                  </ul>
                </div>
              </section>

              <section id="installation" className="mb-16 scroll-mt-24">
                <h2 className="text-2xl font-bold text-white mb-6">Installation</h2>
                <p className="text-slate-400 mb-6">
                  Before you can start using Port Buddy, you need to install the CLI on your machine. 
                  We provide binaries for macOS, Linux, and Windows.
                </p>
                <Link 
                  to="/install" 
                  className="inline-flex items-center gap-2 text-indigo-400 hover:text-indigo-300 font-medium"
                >
                  View Installation Guide
                  <CommandLineIcon className="w-4 h-4" />
                </Link>
              </section>

              <section id="authentication" className="mb-16 scroll-mt-24">
                <h2 className="text-2xl font-bold text-white mb-6">Authentication</h2>
                <p className="text-slate-400 mb-6">
                  To use Port Buddy, you must be authenticated. This allows us to manage your tunnels and respect your subscription limits.
                </p>
                <ol className="space-y-4 text-slate-400 list-decimal list-inside mb-6">
                  <li>Log in to your account at <Link to="/login" className="text-indigo-400 hover:underline">portbuddy.dev</Link>.</li>
                  <li>Go to the <Link to="/app/tokens" className="text-indigo-400 hover:underline">Tokens</Link> page and generate a new API token.</li>
                  <li>Run the following command in your terminal:</li>
                </ol>
                <CodeBlock code="portbuddy init {YOUR_API_TOKEN}" />
              </section>

              <section id="http-tunnels" className="mb-16 scroll-mt-24">
                <h2 className="text-2xl font-bold text-white mb-6">HTTP Tunnels</h2>
                <p className="text-slate-400 mb-6">
                  HTTP is the default mode for Port Buddy. It's used for web applications and APIs.
                </p>
                <h3 className="text-lg font-semibold text-white mb-3">Usage</h3>
                <CodeBlock code="portbuddy 3000" />
                <p className="text-slate-400 mt-4">
                  This command will expose your local web server running on port 3000. 
                  You will receive a public URL like <code className="text-indigo-300">https://abc123.portbuddy.dev</code>.
                </p>
              </section>

              <section id="tcp-tunnels" className="mb-16 scroll-mt-24">
                <h2 className="text-2xl font-bold text-white mb-6">TCP Tunnels</h2>
                <p className="text-slate-400 mb-6">
                  TCP mode allows you to expose any TCP-based service, such as databases or SSH.
                </p>
                <h3 className="text-lg font-semibold text-white mb-3">Usage</h3>
                <CodeBlock code="portbuddy tcp 5432" />
                <p className="text-slate-400 mt-4">
                  This command exposes your local PostgreSQL database on port 5432. 
                  You will get an address like <code className="text-indigo-300">net-proxy-3.portbuddy.dev:43452</code>.
                </p>
              </section>

              <section id="udp-tunnels" className="mb-16 scroll-mt-24">
                <h2 className="text-2xl font-bold text-white mb-6">UDP Tunnels</h2>
                <p className="text-slate-400 mb-6">
                  UDP mode is useful for game servers, VoIP, and other UDP-based protocols.
                </p>
                <h3 className="text-lg font-semibold text-white mb-3">Usage</h3>
                <CodeBlock code="portbuddy udp 19132" />
              </section>

              <section id="custom-domains" className="mb-16 scroll-mt-24">
                <h2 className="text-2xl font-bold text-white mb-6">Custom Domains</h2>
                <p className="text-slate-400 mb-6">
                  With a Pro or Team plan, you can use your own domain name for your tunnels. 
                  We handle the SSL certificate issuance and renewal for you automatically.
                </p>
                <p className="text-slate-400">
                  Configure your custom domains in the <Link to="/app/domains" className="text-indigo-400 hover:underline">Domains</Link> section of the dashboard.
                </p>
              </section>

              <section id="pricing-limits" className="mb-16 scroll-mt-24">
                <h2 className="text-2xl font-bold text-white mb-6">Pricing & Limits</h2>
                <p className="text-slate-400 mb-6">
                  Port Buddy offers two simple plans to suit your needs.
                </p>
                <div className="grid sm:grid-cols-2 gap-6">
                  <div className="bg-slate-900/50 border border-slate-800 rounded-xl p-6">
                    <h3 className="text-xl font-bold text-white mb-2">Pro</h3>
                    <p className="text-indigo-400 font-bold mb-4">$0 / mo</p>
                    <ul className="text-sm text-slate-400 space-y-2">
                      <li>• 1 free tunnel at a time</li>
                      <li>• Custom domains</li>
                      <li>• Static subdomains</li>
                      <li>• $1/mo per extra tunnel</li>
                    </ul>
                  </div>
                  <div className="bg-slate-900/50 border border-indigo-500/30 rounded-xl p-6 shadow-lg shadow-indigo-500/10">
                    <h3 className="text-xl font-bold text-white mb-2">Team</h3>
                    <p className="text-indigo-400 font-bold mb-4">$10 / mo</p>
                    <ul className="text-sm text-slate-400 space-y-2">
                      <li>• 10 free tunnels at a time</li>
                      <li>• Team members</li>
                      <li>• Priority support</li>
                      <li>• $1/mo per extra tunnel</li>
                    </ul>
                  </div>
                </div>
              </section>
            </main>
          </div>
        </div>
      </div>
    </div>
  )
}

function SidebarLink({ href, label }: { href: string, label: string }) {
  return (
    <a 
      href={href} 
      className="block px-2 py-1.5 text-sm text-slate-400 hover:text-white hover:bg-slate-800 rounded-lg transition-colors"
    >
      {label}
    </a>
  )
}

function FeatureItem({ icon, label }: { icon: React.ReactNode, label: string }) {
  return (
    <li className="flex items-center gap-3 text-sm text-slate-300">
      <div className="text-indigo-400">{icon}</div>
      {label}
    </li>
  )
}

function CodeBlock({ code }: { code: string }) {
  return (
    <div className="bg-slate-950 border border-slate-800 rounded-lg p-4 font-mono text-sm text-slate-300">
      <pre>{code}</pre>
    </div>
  )
}
