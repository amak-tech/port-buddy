import { Link } from 'react-router-dom'
import {
  CommandLineIcon,
  GlobeAltIcon,
  ShieldCheckIcon,
  ServerIcon,
  BoltIcon,
  LockClosedIcon,
  CodeBracketIcon,
  RocketLaunchIcon,
  ShareIcon,
  CircleStackIcon,
  ArrowRightIcon,
  CheckIcon,
  HeartIcon
} from '@heroicons/react/24/outline'
import React from 'react'
import Seo from '../components/Seo'
import PlanComparison from '../components/PlanComparison'

export default function Landing() {
  const softwareSchema = {
    "@context": "https://schema.org",
    "@type": "SoftwareApplication",
    "name": "Port Buddy",
    "operatingSystem": "Windows, macOS, Linux",
    "applicationCategory": "DeveloperApplication",
    "description": "Securely expose your local web server, database, or TCP/UDP service to the internet.",
    "offers": {
      "@type": "Offer",
      "price": "0",
      "priceCurrency": "USD"
    }
  };

  return (
    <div className="flex flex-col gap-16 md:gap-24 pb-16 md:pb-24">
      <Seo 
        title="Port Buddy - Expose Localhost to the Internet | Ngrok Alternative"
        description="Securely expose your local web server, database, or TCP/UDP service to the internet. The best free ngrok alternative for developers. Supports HTTP, TCP & UDP tunneling."
        keywords="ngrok alternative, localhost tunneling, expose port, port forwarding, reverse proxy, tcp proxy, udp proxy, local development, port buddy"
        schema={softwareSchema}
        canonical="https://portbuddy.dev/"
        url="https://portbuddy.dev/"
      />
      {/* Hero Section */}
      <section className="relative pt-20 overflow-hidden">
        {/* Background Decorative Elements */}
        <div className="absolute top-0 left-1/2 -translate-x-1/2 w-full h-[600px] bg-mesh-gradient opacity-60 pointer-events-none" />
        <div className="absolute top-[10%] left-[10%] w-64 h-64 bg-jb-purple/10 rounded-full blur-[120px] pointer-events-none" />
        <div className="absolute top-[20%] right-[10%] w-96 h-96 bg-jb-blue/10 rounded-full blur-[120px] pointer-events-none" />
        
        <div className="container relative">
          <div className="text-center max-w-4xl mx-auto">
            <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full glass border border-white/10 text-slate-300 text-xs font-medium mb-8">
              <span className="relative flex h-2 w-2">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-jb-blue opacity-75"></span>
                <span className="relative inline-flex rounded-full h-2 w-2 bg-jb-blue"></span>
              </span>
              <span className="tracking-wide uppercase">Version 1.0 is now live</span>
            </div>
            
            <h1 className="text-6xl md:text-8xl font-black tracking-tighter text-white mb-8 leading-[1.1]">
              Secure Tunnels for <br/>
              <span className="text-gradient">Localhost</span>
            </h1>
            
            <p className="text-xl md:text-2xl text-slate-400 mb-12 leading-relaxed max-w-2xl mx-auto">
              Share your local web server, database, or TCP/UDP service with the world in seconds. 
              Built for developers who value <span className="text-white font-semibold">speed</span> and <span className="text-white font-semibold">security</span>.
            </p>
            
            <div className="flex flex-col sm:flex-row items-center justify-center gap-6">
              <Link 
                to="/install" 
                className="btn btn-primary w-full sm:w-auto text-lg py-4 px-10"
              >
                <CommandLineIcon className="w-6 h-6" />
                Install CLI
              </Link>
              <Link 
                to="/login" 
                className="btn w-full sm:w-auto text-lg py-4 px-10 glass hover:bg-white/5"
              >
                Get Started
                <ArrowRightIcon className="w-5 h-5" />
              </Link>
            </div>
            
            <div className="mt-12 flex items-center justify-center gap-8">
              <a 
                href="https://github.com/amak-tech/port-buddy" 
                target="_blank" 
                rel="noopener noreferrer"
                className="flex items-center gap-3 text-slate-400 hover:text-white transition-all group"
              >
                <svg className="w-6 h-6 fill-current" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                  <path d="M12 .297c-6.63 0-12 5.373-12 12 0 5.303 3.438 9.8 8.205 11.385.6.113.82-.258.82-.577 0-.285-.01-1.04-.015-2.04-3.338.724-4.042-1.61-4.042-1.61C4.422 18.07 3.633 17.7 3.633 17.7c-1.087-.744.084-.729.084-.729 1.205.084 1.838 1.236 1.838 1.236 1.07 1.835 2.809 1.305 3.495.998.108-.776.417-1.305.76-1.605-2.665-.3-5.466-1.332-5.466-5.93 0-1.31.465-2.38 1.235-3.22-.135-.303-.54-1.523.105-3.176 0 0 1.005-.322 3.3 1.23.96-.267 1.98-.399 3-.405 1.02.006 2.04.138 3 .405 2.28-1.552 3.285-1.23 3.285-1.23.645 1.653.24 2.873.12 3.176.765.84 1.23 1.91 1.23 3.22 0 4.61-2.805 5.625-5.475 5.92.42.36.81 1.096.81 2.22 0 1.606-.015 2.896-.015 3.286 0 .315.21.69.825.57C20.565 22.092 24 17.592 24 12.297c0-6.627-5.373-12-12-12"/>
                </svg>
                <span className="text-sm font-semibold tracking-wide">GitHub</span>
                <span className="text-[10px] glass px-2 py-0.5 rounded border border-white/5 group-hover:border-white/20 transition-colors">amak-tech/port-buddy</span>
              </a>
              <div className="w-px h-6 bg-white/10"></div>
              <div className="flex items-center gap-3 text-slate-400">
                <HeartIcon className="w-6 h-6 text-jb-pink" />
                <span className="text-sm font-semibold tracking-wide uppercase">Open Source</span>
              </div>
            </div>
          </div>

          {/* Terminal Preview */}
          <div className="mt-20 mx-auto max-w-4xl glass rounded-2xl border border-white/10 shadow-[0_20px_50px_rgba(0,0,0,0.5)] overflow-hidden transform hover:scale-[1.01] transition-transform duration-500">
            <div className="flex items-center px-6 py-4 bg-white/5 border-b border-white/10 gap-3">
              <div className="flex gap-2">
                <div className="w-3.5 h-3.5 rounded-full bg-red-500/40"></div>
                <div className="w-3.5 h-3.5 rounded-full bg-yellow-500/40"></div>
                <div className="w-3.5 h-3.5 rounded-full bg-green-500/40"></div>
              </div>
              <div className="ml-4 text-xs text-slate-500 font-mono tracking-widest uppercase">Port Buddy Terminal</div>
            </div>
            <div className="p-8 font-mono text-sm md:text-base overflow-x-auto leading-relaxed">
              <div className="flex items-center gap-3 text-slate-400 mb-6">
                <span className="text-jb-blue font-bold">➜</span>
                <span className="text-jb-purple font-bold">~</span>
                <span className="text-white">portbuddy 3000</span>
              </div>
              
              <div className="space-y-3">
                <div className="flex gap-10">
                  <span className="text-slate-500 w-24">Port Buddy</span>
                  <span className="text-jb-blue font-bold">HTTP mode</span>
                </div>
                <div className="flex gap-10">
                  <span className="text-slate-500 w-24">Status</span>
                  <span className="px-2 py-0.5 bg-green-500/20 text-green-400 rounded text-xs font-bold uppercase tracking-wider">Online</span>
                </div>
                <div className="flex gap-10">
                  <span className="text-slate-500 w-24">Forwarding</span>
                  <div className="flex flex-col gap-1">
                    <span className="text-slate-300">Local:  <span className="text-white underline decoration-jb-blue/40">http://localhost:3000</span></span>
                    <span className="text-slate-300">Public: <span className="text-white underline decoration-jb-pink/40">https://abc123.portbuddy.dev</span></span>
                  </div>
                </div>
                
                <div className="border-t border-white/5 my-6"></div>
                
                <div className="text-slate-500 mb-3 uppercase text-xs font-bold tracking-widest">Live Traffic</div>
                <div className="flex gap-6 items-center">
                  <span className="text-jb-blue font-bold">GET</span>
                  <span className="text-slate-400">/api/user/profile</span>
                  <span className="ml-auto text-green-400 font-bold">200 OK</span>
                </div>
                <div className="flex gap-6 items-center">
                  <span className="text-jb-pink font-bold">POST</span>
                  <span className="text-slate-400">/api/webhooks/stripe</span>
                  <span className="ml-auto text-green-400 font-bold">200 OK</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Features Grid */}
      <section id="features" className="container">
        <div className="text-center mb-16">
          <h2 className="text-3xl md:text-4xl font-bold text-white mb-4">
            Everything you need for 
            <span className="text-indigo-400"> local development</span>
          </h2>
          <p className="text-slate-400 max-w-2xl mx-auto">
            Port Buddy is packed with features to help you develop, test, and demo your applications faster.
          </p>
        </div>

        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-8">
          <FeatureCard 
            icon={<GlobeAltIcon className="w-6 h-6 text-indigo-400" />}
            title="Custom Domains"
            description="Bring your own domain name. We automatically provision and manage SSL certificates for you."
          />
          <FeatureCard 
            icon={<ServerIcon className="w-6 h-6 text-cyan-400" />}
            title="TCP & UDP Tunnels"
            description="Expose any TCP or UDP service. Databases, SSH, RDP, game servers, IoT protocols, and more."
          />
          <FeatureCard 
            icon={<ShieldCheckIcon className="w-6 h-6 text-green-400" />}
            title="Secure by Default"
            description="Automatic HTTPS for all HTTP tunnels. End-to-end encryption keeps your data safe."
          />
          <FeatureCard 
            icon={<BoltIcon className="w-6 h-6 text-yellow-400" />}
            title="WebSockets Support"
            description="Full support for WebSockets. Perfect for real-time applications, chat apps, and game servers."
          />
          <FeatureCard 
            icon={<CommandLineIcon className="w-6 h-6 text-purple-400" />}
            title="Static Subdomains"
            description="Reserve your own subdomains on our platform. Keep your URLs consistent across restarts."
          />
          <FeatureCard 
            icon={<LockClosedIcon className="w-6 h-6 text-red-400" />}
            title="Private Tunnels"
            description="Protect your tunnels with basic auth or IP allowlisting. Control who can access your local apps."
          />
          <FeatureCard 
            icon={<CodeBracketIcon className="w-6 h-6 text-indigo-400" />}
            title="100% Open Source"
            description="Port Buddy is open source and community-driven. Check out our code on GitHub and contribute!"
          />
        </div>
      </section>

      {/* How it Works (Infographic style) */}
      <section id="how-it-works" className="container py-12">
        <div className="bg-slate-900/50 border border-slate-800 rounded-2xl p-8 md:p-12">
          <h2 className="text-3xl font-bold text-center text-white mb-16">How it works</h2>
          
          <div className="grid md:grid-cols-3 gap-8 relative">
            {/* Connecting lines for desktop */}
            <div className="hidden md:block absolute top-12 left-[20%] right-[20%] h-0.5 bg-gradient-to-r from-indigo-500/0 via-indigo-500/50 to-indigo-500/0 border-t border-dashed border-slate-600 z-0"></div>

            <Step 
              number="01"
              title="Install CLI"
              description="Download the single binary for your OS. No dependencies required."
            />
            <Step 
              number="02"
              title="Connect"
              description="Run `portbuddy 8080`. We create a secure tunnel to our edge network."
            />
            <Step 
              number="03"
              title="Share"
              description="Get a public URL instantly. Anyone can now access your local service."
            />
          </div>

          <div className="mt-16 pt-8 border-t border-slate-800 flex flex-wrap justify-center gap-8 md:gap-16 opacity-50 grayscale hover:grayscale-0 transition-all duration-500">
            <div className="flex items-center gap-2 text-slate-300">
              <CommandLineIcon className="w-6 h-6" />
              <span>macOS</span>
            </div>
            <div className="flex items-center gap-2 text-slate-300">
              <CommandLineIcon className="w-6 h-6" />
              <span>Linux</span>
            </div>
            <div className="flex items-center gap-2 text-slate-300">
              <CommandLineIcon className="w-6 h-6" />
              <span>Windows</span>
            </div>
            <div className="flex items-center gap-2 text-slate-300">
              <CommandLineIcon className="w-6 h-6" />
              <span>Docker</span>
            </div>
          </div>
        </div>
      </section>

      {/* Use Cases */}
      <section id="use-cases" className="container">
        <div className="grid lg:grid-cols-2 gap-12 items-center">
          <div>
            <h2 className="text-3xl md:text-4xl font-bold text-white mb-6">
              Built for Developers
            </h2>
            <p className="text-slate-400 mb-8 text-lg">
              From webhooks to demos, Port Buddy streamlines your development workflow.
            </p>
            
            <div className="space-y-6">
              <UseCaseItem 
                icon={<CodeBracketIcon className="w-6 h-6" />}
                title="Test Webhooks"
                description="Debug payment gateways (Stripe, PayPal) or SMS webhooks (Twilio) locally without deploying."
              />
              <UseCaseItem 
                icon={<ShareIcon className="w-6 h-6" />}
                title="Share Progress"
                description="Show off your work to clients or colleagues instantly. No staging server needed."
              />
              <UseCaseItem 
                icon={<RocketLaunchIcon className="w-6 h-6" />}
                title="Test Chatbots"
                description="Develop Slack, Discord, or Telegram bots on your local machine with a public HTTPS URL."
              />
              <UseCaseItem 
                icon={<CircleStackIcon className="w-6 h-6" />}
                title="Expose Databases"
                description="Securely access your local database from the cloud or allow remote team access."
              />
            </div>
          </div>
          
          <div className="relative overflow-hidden lg:overflow-visible">
            <div className="absolute -inset-4 bg-gradient-to-r from-indigo-500 to-purple-500 opacity-20 blur-3xl rounded-full"></div>
            <div className="relative bg-slate-900 border border-slate-800 rounded-xl p-6 shadow-2xl overflow-hidden">
               <div className="flex items-center justify-between border-b border-slate-800 pb-4 mb-4">
                 <div className="text-sm font-medium text-slate-300">Webhook Inspector</div>
                 <div className="flex gap-2">
                   <div className="w-2 h-2 rounded-full bg-red-500"></div>
                   <div className="w-2 h-2 rounded-full bg-yellow-500"></div>
                   <div className="w-2 h-2 rounded-full bg-green-500"></div>
                 </div>
               </div>
               <div className="space-y-3 font-mono text-xs md:text-sm">
                 <div className="bg-slate-800/50 p-3 rounded border-l-2 border-green-500 overflow-hidden">
                   <div className="flex justify-between text-slate-400 mb-1 gap-4">
                     <span className="truncate">POST /webhooks/stripe</span>
                     <span className="text-green-400 shrink-0">200 OK</span>
                   </div>
                   <div className="text-slate-500 truncate">{`{ "id": "evt_1M...", "type": "payment_intent.succeeded" }`}</div>
                 </div>
                 <div className="bg-slate-800/50 p-3 rounded border-l-2 border-green-500 overflow-hidden">
                   <div className="flex justify-between text-slate-400 mb-1 gap-4">
                     <span className="truncate">POST /webhooks/github</span>
                     <span className="text-green-400 shrink-0">200 OK</span>
                   </div>
                   <div className="text-slate-500 truncate">{`{ "action": "opened", "pull_request": { ... } }`}</div>
                 </div>
                 <div className="bg-slate-800/50 p-3 rounded border-l-2 border-red-500 overflow-hidden">
                   <div className="flex justify-between text-slate-400 mb-1 gap-4">
                     <span className="truncate">POST /api/callback</span>
                     <span className="text-red-400 shrink-0">500 Error</span>
                   </div>
                   <div className="text-slate-500 truncate">Error: Invalid signature</div>
                 </div>
               </div>
            </div>
          </div>
        </div>
      </section>

      {/* Pricing */}
      <section id="pricing" className="container">
        <h2 className="text-3xl md:text-4xl font-bold text-white text-center mb-16">Simple Pricing</h2>
        <div className="grid md:grid-cols-2 gap-8 max-w-4xl mx-auto">
          <PriceCard 
            name="Pro"
            price="$0"
            description="Everything you need for personal exposure."
            features={[
              'HTTP, TCP, UDP tunnels',
              'SSL for HTTP tunnels',
              'Static subdomains',
              'Custom domains',
              'Private tunnels',
              'Web socket support',
              '1 free tunnel at a time',
              '$1/mo per extra tunnel'
            ]}
            cta="Start for Free"
            ctaLink="/install"
          />
          <PriceCard 
            name="Team"
            price="$10"
            period="/mo"
            description="For teams and collaborative projects."
            features={[
              'Everything in Pro',
              'Team members',
              'SSO (Coming soon)',
              'Priority support',
              '10 free tunnels at a time',
              '$1/mo per extra tunnel'
            ]}
            highlight
            cta="Get Started"
            ctaLink="/app/billing"
          />
        </div>

        <PlanComparison />
      </section>

      {/* CTA Section */}
      <section className="container pt-12">
        <div className="relative overflow-hidden rounded-3xl glass p-12 md:p-20 text-center border border-white/10">
          <div className="absolute top-0 left-0 w-full h-full bg-mesh-gradient opacity-20 pointer-events-none" />
          <div className="relative z-10">
            <h2 className="text-4xl md:text-6xl font-black text-white mb-6 tracking-tighter">Ready to <span className="text-gradient">Share?</span></h2>
            <p className="text-xl text-slate-400 mb-10 max-w-2xl mx-auto leading-relaxed">
              Join thousands of developers using Port Buddy to expose their local services securely and efficiently.
            </p>
            <Link 
              to="/login" 
              className="btn btn-primary text-xl py-5 px-12 rounded-2xl transition-transform hover:scale-105"
            >
              Get Started for Free
              <ArrowRightIcon className="w-5 h-5" />
            </Link>
          </div>
        </div>
      </section>
    </div>
  )
}

function FeatureCard({ icon, title, description }: { icon: React.ReactNode, title: string, description: string }) {
  return (
    <div className="group glass p-8 rounded-2xl border border-white/5 hover:border-white/10 hover:bg-white/[0.07] transition-all duration-300">
      <div className="w-14 h-14 rounded-xl bg-jb-blue/10 flex items-center justify-center text-jb-blue mb-6 group-hover:scale-110 group-hover:bg-jb-blue/20 transition-all duration-300">
        <div className="w-8 h-8">
          {icon}
        </div>
      </div>
      <h3 className="text-xl font-bold text-white mb-3 group-hover:text-jb-blue transition-colors">{title}</h3>
      <p className="text-slate-400 leading-relaxed group-hover:text-slate-300 transition-colors">
        {description}
      </p>
    </div>
  )
}

function Step({ number, title, description }: { number: string, title: string, description: string }) {
  return (
    <div className="relative z-10 flex flex-col items-center text-center group">
      <div className="w-16 h-16 rounded-2xl glass text-white font-black text-2xl flex items-center justify-center mb-6 border border-white/10 group-hover:border-jb-blue/50 transition-all duration-500 transform group-hover:-rotate-6">
        <span className="text-gradient">{number}</span>
      </div>
      <h3 className="text-xl font-bold text-white mb-3 group-hover:text-jb-blue transition-colors">{title}</h3>
      <p className="text-slate-400 leading-relaxed group-hover:text-slate-300 transition-colors">
        {description}
      </p>
    </div>
  )
}

function UseCaseItem({ icon, title, description }: { icon: React.ReactNode, title: string, description: string }) {
  return (
    <div className="flex gap-6 p-6 rounded-2xl transition-all duration-300 hover:bg-white/[0.03] group">
      <div className="flex-shrink-0 w-14 h-14 bg-white/5 rounded-xl flex items-center justify-center text-jb-blue group-hover:bg-jb-blue group-hover:text-white transition-all duration-300">
        <div className="w-7 h-7">
          {icon}
        </div>
      </div>
      <div>
        <h3 className="text-xl font-bold text-white mb-2 group-hover:text-jb-blue transition-colors">{title}</h3>
        <p className="text-slate-400 leading-relaxed group-hover:text-slate-300 transition-colors">{description}</p>
      </div>
    </div>
  )
}

function PriceCard({ 
  name, price, period, description, features, highlight = false, cta, ctaLink 
}: { 
  name: string, price: string, period?: string, description: string, features: string[], highlight?: boolean, cta: string, ctaLink: string 
}) {
  return (
    <div className={`rounded-2xl p-8 flex flex-col border ${highlight ? 'bg-slate-800/80 border-indigo-500 shadow-2xl shadow-indigo-500/10' : 'bg-slate-900/50 border-slate-800'}`}>
      <div className="mb-6">
        <h3 className="text-lg font-medium text-indigo-400 mb-2">{name}</h3>
        <div className="flex items-baseline gap-1">
          <span className="text-4xl font-bold text-white">{price}</span>
          {period && <span className="text-slate-500">{period}</span>}
        </div>
        <p className="text-slate-400 mt-2 text-sm">{description}</p>
      </div>
      
      <ul className="space-y-4 mb-8 flex-1">
        {features.map((feature, i) => (
          <li key={i} className="flex items-start gap-3 text-sm text-slate-300">
            <CheckIcon className="w-5 h-5 text-indigo-500 flex-shrink-0" />
            <span>{feature}</span>
          </li>
        ))}
      </ul>
      
      <Link 
        to={ctaLink} 
        className={`w-full py-3 rounded-lg font-semibold text-center transition-all ${
          highlight 
            ? 'bg-indigo-600 hover:bg-indigo-500 text-white shadow-lg shadow-indigo-500/25' 
            : 'bg-slate-700 hover:bg-slate-600 text-white'
        }`}
      >
        {cta}
      </Link>
    </div>
  )
}
