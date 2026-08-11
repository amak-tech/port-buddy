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

import { Link } from 'react-router-dom'
import React, { useState } from 'react'
import {
  ArrowRightIcon,
  BoltIcon,
  CheckIcon,
  CloudIcon,
  CommandLineIcon,
  ComputerDesktopIcon,
  DevicePhoneMobileIcon,
  GlobeAltIcon,
  LockClosedIcon,
  ServerIcon,
  ShareIcon,
  ShieldCheckIcon,
  UserIcon
} from '@heroicons/react/24/outline'
import PlanComparison from '../components/PlanComparison'

// --- Content (single source of truth for copy + structured data) ---

const problems: { icon: React.ReactNode, problem: string, solution: string, command: string }[] = [
  {
    icon: <BoltIcon className="w-6 h-6 text-yellow-400" />,
    problem: "Webhooks can't reach localhost",
    solution: 'Stripe, GitHub, Twilio and Telegram need a public HTTPS endpoint. Point them at your tunnel '
      + 'URL and debug the real payload on your machine — no redeploy between attempts.',
    command: 'portbuddy 3000'
  },
  {
    icon: <ShareIcon className="w-6 h-6 text-indigo-400" />,
    problem: 'Deploying just to show someone',
    solution: 'Skip the staging deploy for a five-minute review. Send a link to whatever is running on '
      + 'your machine right now and keep editing while they look.',
    command: 'portbuddy 5173'
  },
  {
    icon: <DevicePhoneMobileIcon className="w-6 h-6 text-pink-400" />,
    problem: 'It works on your laptop, not on a phone',
    solution: 'Open the same HTTPS URL on any real device or remote browser. Valid certificates, so no '
      + 'self-signed warnings and no broken service workers.',
    command: 'portbuddy 8080'
  },
  {
    icon: <ServerIcon className="w-6 h-6 text-cyan-400" />,
    problem: 'Someone needs your local database or server',
    solution: 'Raw TCP and UDP tunnels forward Postgres, Redis, SSH, RDP and game servers — no VPN, no '
      + 'router config, no firewall tickets.',
    command: 'portbuddy tcp 5432'
  }
]

const steps: { title: string, description: string, command: string }[] = [
  {
    title: 'Install the CLI',
    description: 'One static binary, zero dependencies. Also available for Linux, Windows and Docker.',
    command: 'brew install amak-tech/tap/portbuddy'
  },
  {
    title: 'Link your account',
    description: 'Create an API token in the dashboard and pass it to the CLI once.',
    command: 'portbuddy init YOUR_API_TOKEN'
  },
  {
    title: 'Expose a port',
    description: 'You get a public HTTPS URL immediately. Use tcp or udp for non-HTTP services.',
    command: 'portbuddy 3000'
  }
]

const features: { icon: React.ReactNode, title: string, description: string }[] = [
  {
    icon: <ShieldCheckIcon className="w-6 h-6 text-green-400" />,
    title: 'HTTPS out of the box',
    description: 'Every HTTP tunnel is served over TLS with a valid certificate. Traffic between the edge '
      + 'and your machine runs inside an encrypted tunnel.'
  },
  {
    icon: <CommandLineIcon className="w-6 h-6 text-purple-400" />,
    title: 'Static subdomains',
    description: 'Reserve a subdomain so your URL survives restarts. Webhook endpoints you register once '
      + 'keep working tomorrow.'
  },
  {
    icon: <GlobeAltIcon className="w-6 h-6 text-jb-blue" />,
    title: 'Custom domains',
    description: 'Point tunnel.yourcompany.com at Port Buddy. Certificates are issued and renewed '
      + 'automatically via Let’s Encrypt.'
  },
  {
    icon: <ServerIcon className="w-6 h-6 text-cyan-400" />,
    title: 'TCP and UDP tunnels',
    description: 'Not just HTTP. UDP tunnels are available on every plan; TCP tunnels (databases, SSH, RDP) '
      + 'need 5+ tunnels or the Team plan.'
  },
  {
    icon: <BoltIcon className="w-6 h-6 text-yellow-400" />,
    title: 'WebSockets supported',
    description: 'Long-lived connections pass through untouched, so live reload, chat and multiplayer '
      + 'backends behave like they do locally.'
  },
  {
    icon: <LockClosedIcon className="w-6 h-6 text-red-400" />,
    title: 'Private tunnels',
    description: 'Put basic auth or an IP allowlist in front of a tunnel when a public URL should not '
      + 'actually be public.'
  }
]

const faqs: { question: string, answer: string }[] = [
  {
    question: 'What is Port Buddy?',
    answer: 'Port Buddy is a tunneling service for developers. You run one command against a local port and '
      + 'get a public URL that forwards traffic to your machine, so external services, teammates and '
      + 'devices can reach an app that only runs on localhost.'
  },
  {
    question: 'How is it different from ngrok?',
    answer: 'Port Buddy focuses on a simpler workflow and a lower price: static subdomains, custom domains '
      + 'and private tunnels are available cheaply, and there is a free tier with one tunnel. It is also '
      + 'open source under Apache 2.0, so you can read the code or run your own instance.'
  },
  {
    question: 'Is there a free plan?',
    answer: 'Yes. The Pro plan starts at $0/month with one free HTTP or UDP tunnel and no credit card. '
      + 'Extra tunnels cost $1/month each, and the Team plan is $10/month with 10 tunnels included plus '
      + 'team member management.'
  },
  {
    question: 'Is my traffic secure?',
    answer: 'HTTP tunnels are served over HTTPS with automatic certificates, and the connection between the '
      + 'CLI on your machine and the Port Buddy edge is encrypted. For tunnels that should not be open to '
      + 'everyone you can require basic auth or restrict access to an IP allowlist.'
  },
  {
    question: 'Can I expose a database, SSH or a game server?',
    answer: 'Yes. Use portbuddy tcp 5432 for Postgres, or portbuddy udp 19132 for UDP services such as '
      + 'Minecraft Bedrock. UDP tunnels are included on every plan; TCP tunnels require at least 5 tunnels, '
      + 'which you get by adding extra tunnels on Pro or by using the Team plan.'
  },
  {
    question: 'Can I use my own domain name?',
    answer: 'Yes. Connect a custom domain such as tunnel.yourcompany.com and Port Buddy provisions and '
      + 'renews the SSL certificate for it automatically.'
  },
  {
    question: 'Does the URL stay the same between restarts?',
    answer: 'Reserve a static subdomain and your tunnel keeps the same address every time you start it, '
      + 'which matters when a webhook provider or a mobile build has the URL hardcoded.'
  },
  {
    question: 'Can I self-host Port Buddy?',
    answer: 'Yes. The full stack — CLI, tunnel server, gateway and dashboard — is published on GitHub under '
      + 'the Apache 2.0 license, so you can run it on your own infrastructure instead of the hosted service.'
  }
]

const faqSchema = {
  '@context': 'https://schema.org',
  '@type': 'FAQPage',
  mainEntity: faqs.map(({ question, answer }) => ({
    '@type': 'Question',
    name: question,
    acceptedAnswer: { '@type': 'Answer', text: answer }
  }))
}

// --- Helper Components ---

// Tokens are individually non-breaking, separated by real space text nodes. So a long command wraps at
// its spaces but never inside a token: default wrapping splits it as "brew install amak-" / "tech/..."
// and nowrap silently truncates it, while CSS gaps between flex items would break copy-paste.
function Command({ children }: { children: string }) {
  return (
    <code className="inline-block max-w-full rounded-lg bg-slate-950 border border-white/10 px-3 py-2 font-mono text-xs md:text-sm text-slate-200">
      <span className="text-jb-blue select-none mr-2">$</span>
      {children.split(' ').map((token, index) => (
        <React.Fragment key={index}>
          {index > 0 && ' '}
          <span className="whitespace-nowrap">{token}</span>
        </React.Fragment>
      ))}
    </code>
  )
}

function ProblemCard({ icon, problem, solution, command }: { icon: React.ReactNode, problem: string, solution: string, command: string }) {
  return (
    <div className="flex flex-col gap-4 p-8 rounded-2xl bg-white/[0.03] border border-white/5 hover:border-white/10 transition-colors min-w-0">
      <div className="p-3 bg-slate-900 rounded-lg w-fit border border-white/5">{icon}</div>
      <h3 className="text-xl font-bold text-white leading-snug">{problem}</h3>
      <p className="text-slate-400 text-sm leading-relaxed flex-1">{solution}</p>
      <div><Command>{command}</Command></div>
    </div>
  )
}

function FeatureCard({ icon, title, description }: { icon: React.ReactNode, title: string, description: string }) {
  return (
    <div className="p-8 rounded-2xl bg-white/5 border border-white/5 hover:border-white/10 hover:bg-white/[0.07] transition-all duration-300 group">
      <div className="mb-6 p-3 bg-slate-900 rounded-lg w-fit group-hover:scale-110 transition-transform duration-300 border border-white/5">
        {icon}
      </div>
      <h3 className="text-xl font-bold text-white mb-3 group-hover:text-indigo-300 transition-colors">{title}</h3>
      <p className="text-slate-400 leading-relaxed text-sm">{description}</p>
    </div>
  )
}

function FaqItem({ question, answer }: { question: string, answer: string }) {
  const [isOpen, setIsOpen] = useState(false)
  return (
    <div className="border-b border-white/10 last:border-0">
      <h3>
        <button
          className="w-full flex items-center justify-between py-6 text-left focus:outline-none group"
          aria-expanded={isOpen}
          onClick={() => setIsOpen(!isOpen)}
        >
          <span className="text-lg font-medium text-slate-200 group-hover:text-white transition-colors">{question}</span>
          <span className={`ml-6 flex-shrink-0 transition-transform duration-300 ${isOpen ? 'rotate-180' : ''}`}>
            <ChevronDownIcon className="w-5 h-5 text-slate-500 group-hover:text-indigo-400" />
          </span>
        </button>
      </h3>
      <div className={`overflow-hidden transition-all duration-300 ease-in-out ${isOpen ? 'max-h-96 opacity-100 pb-6' : 'max-h-0 opacity-0'}`}>
        <p className="text-slate-400 leading-relaxed pr-4 md:pr-12">{answer}</p>
      </div>
    </div>
  )
}

function ChevronDownIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor" aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
    </svg>
  )
}

// --- Main Component ---

export default function Landing() {
  return (
    <div className="flex flex-col gap-24 md:gap-32 pb-24">
      {/* Hero */}
      <section className="relative pt-20 md:pt-28 overflow-hidden">
        <div className="absolute top-0 left-1/2 -translate-x-1/2 w-full h-[600px] bg-mesh-gradient opacity-40 pointer-events-none" />
        <div className="absolute top-[10%] left-1/2 -translate-x-1/2 w-[600px] h-80 bg-jb-blue/10 rounded-full blur-[120px] pointer-events-none" />

        <div className="container relative z-10">
          <div className="max-w-4xl mx-auto text-center">
            <p className="badge inline-block mb-8">Open source · HTTP · TCP · UDP</p>

            <h1 className="text-4xl md:text-6xl font-black tracking-tighter text-white mb-8 leading-[1.05]">
              Expose localhost to the internet in one command
            </h1>

            <p className="text-lg md:text-xl text-slate-400 mb-10 leading-relaxed font-light max-w-2xl mx-auto">
              Port Buddy gives any port on your machine a public HTTPS URL. Test webhooks, demo work in
              progress, debug on a real phone, or share a local database — without deploying, opening
              firewall ports or setting up a VPN.
            </p>

            <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-center gap-4 mb-10">
              <Link
                to="/install"
                className="btn btn-primary justify-center text-base py-3.5 px-8 shadow-[0_0_40px_-10px_rgba(99,102,241,0.5)]"
              >
                <CommandLineIcon className="w-5 h-5" />
                Install the CLI
              </Link>
              <Link
                to="/register"
                className="btn justify-center text-base py-3.5 px-8 glass hover:bg-white/5 border border-white/10"
              >
                Start for free
                <ArrowRightIcon className="w-4 h-4" />
              </Link>
            </div>

            <ul className="flex flex-wrap justify-center gap-x-6 gap-y-3 text-sm text-slate-400">
              <li className="flex items-center gap-2">
                <CheckIcon className="w-4 h-4 text-green-400" /> Free tier, no credit card
              </li>
              <li className="flex items-center gap-2">
                <CheckIcon className="w-4 h-4 text-green-400" /> macOS, Linux, Windows, Docker
              </li>
              <li className="flex items-center gap-2">
                <CheckIcon className="w-4 h-4 text-green-400" /> Single static binary
              </li>
            </ul>
          </div>
        </div>
      </section>

      {/* Problems / Use cases */}
      <section id="use-cases" className="container" aria-labelledby="use-cases-heading">
        <div className="text-center mb-16">
          <h2 id="use-cases-heading" className="text-3xl md:text-5xl font-black text-white mb-6">
            Your machine is invisible <span className="text-indigo-400">to everything else</span>
          </h2>
          <p className="text-slate-400 text-lg leading-relaxed max-w-3xl mx-auto">
            Localhost sits behind NAT and a firewall, so anything outside your laptop — a payment provider, a
            teammate, your phone — simply cannot reach it. These are the four problems developers hit most,
            and the command that solves each one.
          </p>
        </div>

        <div className="grid md:grid-cols-2 gap-6">
          {problems.map((problem) => (
            <ProblemCard key={problem.problem} {...problem} />
          ))}
        </div>

        {/* Keep text and links on one line: splitting them across lines (or with {' '}) creates adjacent
            text nodes that the HTML parser merges, which breaks hydration of the prerendered page. */}
        <p className="text-slate-500 text-sm mt-8">
          Running a game server? See the guides for a <Link to="/docs/guides/minecraft-server" className="text-indigo-400 hover:text-indigo-300">public Minecraft server</Link> or a <Link to="/docs/guides/hytale-server" className="text-indigo-400 hover:text-indigo-300">Hytale server</Link>.
        </p>
      </section>

      {/* Quickstart */}
      <section id="quickstart" className="container" aria-labelledby="quickstart-heading">
        <div className="text-center mb-16">
          <h2 id="quickstart-heading" className="text-3xl md:text-5xl font-black text-white mb-6">
            Public URL in three commands
          </h2>
          <p className="text-slate-400 text-lg">
            No account setup wizard, no config file, no agent running in the background.
          </p>
        </div>

        {/* min-w-0 on the cards: grid items default to min-width:auto, which would let the non-wrapping
            command widen the card past the viewport on narrow phones instead of scrolling in its pill. */}
        <ol className="grid lg:grid-cols-3 gap-6 list-none">
          {steps.map((step, index) => (
            <li key={step.title} className="p-8 rounded-2xl bg-white/[0.03] border border-white/5 flex flex-col gap-4 min-w-0">
              <div className="w-10 h-10 rounded-xl bg-slate-900 border border-slate-700 flex items-center justify-center text-sm font-bold text-white">
                {index + 1}
              </div>
              <h3 className="text-lg font-bold text-white">{step.title}</h3>
              <p className="text-slate-400 text-sm leading-relaxed flex-1">{step.description}</p>
              <div><Command>{step.command}</Command></div>
            </li>
          ))}
        </ol>

        <div className="text-center mt-10">
          <Link to="/docs" className="text-indigo-400 hover:text-indigo-300 text-sm font-medium">
            Read the Port Buddy documentation &rarr;
          </Link>
        </div>
      </section>

      {/* Features */}
      <section id="features" className="container" aria-labelledby="features-heading">
        <div className="text-center mb-16">
          <h2 id="features-heading" className="text-3xl md:text-5xl font-black text-white mb-6">
            What you get with every tunnel
          </h2>
          <p className="text-slate-400 max-w-2xl mx-auto text-lg">
            The things you would otherwise build yourself: TLS, stable URLs, access control and support for
            protocols that are not HTTP.
          </p>
        </div>

        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
          {features.map((feature) => (
            <FeatureCard key={feature.title} {...feature} />
          ))}
        </div>
      </section>

      {/* How it works */}
      <section id="architecture" className="container" aria-labelledby="architecture-heading">
        <div className="relative glass rounded-3xl border border-white/5 p-8 md:p-20 overflow-hidden">
          <div className="absolute top-0 left-0 w-full h-full bg-jb-blue/5 opacity-30 pointer-events-none" />

          <div className="text-center mb-16 relative z-10">
            <h2 id="architecture-heading" className="text-3xl md:text-4xl font-bold text-white mb-4">
              How the tunnel works
            </h2>
            <p className="text-slate-400 max-w-2xl mx-auto">
              The CLI opens one outbound connection to the Port Buddy edge. Incoming traffic travels back down
              that connection, so nothing has to be open on your network.
            </p>
          </div>

          <div className="relative flex flex-col lg:flex-row items-center justify-between gap-12 lg:gap-8 z-10">
            <div className="flex flex-col items-center text-center w-full lg:w-1/4 group">
              <div className="w-24 h-24 rounded-3xl bg-slate-800 flex items-center justify-center text-slate-300 mb-6 border border-white/10 shadow-2xl group-hover:-translate-y-2 transition-transform duration-300">
                <UserIcon className="w-10 h-10" />
              </div>
              <h3 className="text-lg font-bold text-white mb-2">Visitor or webhook</h3>
              <p className="text-sm text-slate-400 leading-relaxed">
                Requests your public URL <br /> <span className="text-jb-pink font-mono">*.portbuddy.dev</span>
              </p>
            </div>

            <div className="hidden lg:flex flex-col items-center justify-center flex-1 px-4">
              <div className="w-full h-0.5 bg-slate-700 relative overflow-hidden">
                <div className="absolute top-0 bottom-0 w-20 bg-gradient-to-r from-transparent via-jb-blue to-transparent animate-flow" />
              </div>
              <span className="text-[10px] text-slate-500 mt-4 uppercase tracking-widest font-bold">HTTPS / TCP / UDP</span>
            </div>

            <div className="flex flex-col items-center text-center w-full lg:w-1/4 p-8 rounded-3xl bg-[#0F1117] border border-jb-blue/30 shadow-[0_0_50px_rgba(51,204,255,0.1)] relative z-20 group">
              <div className="absolute -top-3 -right-3 px-3 py-1 bg-jb-blue text-white text-[10px] font-bold rounded-full uppercase tracking-tighter shadow-lg">Edge</div>
              <div className="w-24 h-24 rounded-3xl bg-jb-blue/10 flex items-center justify-center text-jb-blue mb-6 border border-jb-blue/20 shadow-inner group-hover:-translate-y-2 transition-transform duration-300">
                <CloudIcon className="w-12 h-12" />
              </div>
              <h3 className="text-lg font-bold text-white mb-2">Port Buddy edge</h3>
              <p className="text-sm text-slate-400 leading-relaxed">TLS termination, access rules and routing to the right tunnel</p>
            </div>

            <div className="hidden lg:flex flex-col items-center justify-center flex-1 px-4">
              <div className="w-full h-1 bg-slate-800 relative rounded-full overflow-hidden">
                <div className="absolute inset-0 bg-gradient-to-r from-jb-blue/20 to-jb-purple/20" />
                <div className="absolute top-0 bottom-0 w-20 bg-gradient-to-r from-transparent via-white to-transparent animate-flow [animation-delay:0.5s]" />
              </div>
              <div className="flex items-center gap-1 mt-4">
                <LockClosedIcon className="w-3 h-3 text-green-400" />
                <span className="text-[10px] text-slate-500 uppercase tracking-widest font-bold">Encrypted tunnel</span>
              </div>
            </div>

            <div className="flex flex-col items-center text-center w-full lg:w-1/4 p-8 rounded-3xl bg-white/[0.02] border border-white/10 shadow-xl group">
              <div className="w-24 h-24 rounded-3xl bg-slate-800 flex items-center justify-center text-jb-purple mb-6 border border-white/10 shadow-2xl group-hover:-translate-y-2 transition-transform duration-300">
                <ComputerDesktopIcon className="w-10 h-10" />
              </div>
              <h3 className="text-lg font-bold text-white mb-2">Your machine</h3>
              <div className="flex flex-col gap-2 mt-2">
                <div className="px-3 py-1 bg-white/5 rounded text-[10px] font-mono text-jb-purple border border-white/5">Port Buddy CLI</div>
                <div className="px-3 py-1 bg-white/5 rounded text-[10px] font-mono text-slate-400 border border-white/5">localhost:3000</div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Pricing */}
      <section id="pricing" className="container" aria-labelledby="pricing-heading">
        <div className="text-center mb-16">
          <h2 id="pricing-heading" className="text-3xl md:text-5xl font-black text-white mb-6">
            Simple, transparent pricing
          </h2>
          <p className="text-slate-400 text-lg">
            Start free with one tunnel. Add tunnels for $1/month when you need them.
          </p>
        </div>
        <PlanComparison />
      </section>

      {/* FAQ */}
      <section id="faq" className="container max-w-3xl" aria-labelledby="faq-heading">
        <h2 id="faq-heading" className="text-3xl font-bold text-center text-white mb-12">
          Frequently asked questions
        </h2>
        <div className="space-y-2">
          {faqs.map((faq) => (
            <FaqItem key={faq.question} {...faq} />
          ))}
        </div>
        <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(faqSchema) }} />
      </section>

      {/* Final CTA */}
      <section className="container" aria-labelledby="cta-heading">
        <div className="relative rounded-3xl overflow-hidden p-12 md:p-20 text-center">
          <div className="absolute inset-0 bg-gradient-to-br from-indigo-900/50 to-purple-900/50 z-0" />
          <div className="absolute inset-0 bg-mesh-gradient opacity-30 z-0" />

          <div className="relative z-10 max-w-3xl mx-auto">
            <h2 id="cta-heading" className="text-3xl md:text-5xl font-black text-white mb-6 tracking-tight">
              Give your localhost a public URL
            </h2>
            <p className="text-lg md:text-xl text-slate-300 mb-10">
              Install the CLI, run one command, and share the link. The free tier needs no credit card.
            </p>
            <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
              <Link to="/install" className="btn btn-primary text-lg py-4 px-12 w-full sm:w-auto justify-center">
                Install the CLI
              </Link>
              <Link to="/docs" className="btn glass text-lg py-4 px-12 w-full sm:w-auto justify-center hover:bg-white/10">
                Read the docs
              </Link>
            </div>
          </div>
        </div>
      </section>
    </div>
  )
}
