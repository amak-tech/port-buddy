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
  CheckIcon
} from '@heroicons/react/24/outline'
import React from 'react'
import Seo from '../components/Seo'

export default function Landing() {
  return (
    <div className="flex flex-col gap-24 pb-24">
      <Seo 
        title="Port Buddy - Expose Localhost to the Internet | Ngrok Alternative"
        description="Securely expose your local web server, database, or TCP/UDP service to the internet. The best free ngrok alternative for developers. Supports HTTP, TCP & UDP tunneling."
        keywords="ngrok alternative, localhost tunneling, expose port, port forwarding, reverse proxy, tcp proxy, udp proxy, local development, port buddy"
      />
      {/* Hero Section */}
      <section className="relative pt-20 md:pt-32 px-4">
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-indigo-900/20 via-slate-900/0 to-slate-900/0 pointer-events-none" />
        
        <div className="container relative mx-auto max-w-6xl">
          <div className="text-center max-w-3xl mx-auto">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/10 border border-indigo-500/20 text-indigo-300 text-xs font-medium mb-6">
              <span className="relative flex h-2 w-2">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-indigo-400 opacity-75"></span>
                <span className="relative inline-flex rounded-full h-2 w-2 bg-indigo-500"></span>
              </span>
              v1.0 is now live
            </div>
            
            <h1 className="text-5xl md:text-7xl font-extrabold tracking-tight text-white mb-8 leading-tight">
              Public URLs for <br/>
              <span className="text-transparent bg-clip-text bg-gradient-to-r from-indigo-400 to-cyan-400">Localhost</span>
            </h1>
            
            <p className="text-xl text-slate-400 mb-10 leading-relaxed">
              Expose your local web server, database, or any TCP/UDP service to the internet securely. 
              No firewalls, no DNS configuration, just one command.
            </p>
            
            <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
              <Link 
                to="/install" 
                className="w-full sm:w-auto px-8 py-4 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg font-semibold transition-all flex items-center justify-center gap-2 shadow-lg shadow-indigo-500/20"
              >
                <CommandLineIcon className="w-5 h-5" />
                Install CLI
              </Link>
              <Link 
                to="/login" 
                className="w-full sm:w-auto px-8 py-4 bg-slate-800 hover:bg-slate-700 text-white rounded-lg font-semibold transition-all flex items-center justify-center gap-2 border border-slate-700"
              >
                Get Started for Free
                <ArrowRightIcon className="w-4 h-4" />
              </Link>
            </div>
          </div>

          {/* Terminal Preview */}
          <div className="mt-20 mx-auto max-w-4xl bg-slate-900 rounded-xl border border-slate-800 shadow-2xl overflow-hidden">
            <div className="flex items-center px-4 py-3 bg-slate-800/50 border-b border-slate-800 gap-2">
              <div className="w-3 h-3 rounded-full bg-red-500/20 border border-red-500/50"></div>
              <div className="w-3 h-3 rounded-full bg-yellow-500/20 border border-yellow-500/50"></div>
              <div className="w-3 h-3 rounded-full bg-green-500/20 border border-green-500/50"></div>
              <div className="ml-2 text-xs text-slate-500 font-mono">bash — 80x24</div>
            </div>
            <div className="p-6 font-mono text-sm md:text-base overflow-x-auto">
              <div className="flex items-center gap-2 text-slate-400 mb-4">
                <span className="text-green-400">➜</span>
                <span className="text-cyan-400">~</span>
                <span>port-buddy 3000</span>
              </div>
              
              <div className="space-y-2">
                <div className="flex gap-8 text-slate-300">
                  <span className="text-slate-500">Status</span>
                  <span className="text-green-400 font-bold">Online</span>
                </div>
                <div className="flex gap-8 text-slate-300">
                  <span className="text-slate-500">Account</span>
                  <span>anton@example.com (Team)</span>
                </div>
                <div className="flex gap-8 text-slate-300">
                  <span className="text-slate-500">Region</span>
                  <span>us-east-1 (Virginia)</span>
                </div>
                <div className="flex gap-8 text-slate-300">
                  <span className="text-slate-500">Web</span>
                  <span>http://localhost:4040</span>
                </div>
                
                <div className="border-t border-slate-800 my-4"></div>
                
                <div className="flex gap-8">
                  <span className="text-slate-500">Forwarding</span>
                  <span className="text-white">https://api-dev.portbuddy.dev <span className="text-slate-600">→</span> localhost:3000</span>
                </div>
                <div className="flex gap-8">
                  <span className="text-slate-500">Forwarding</span>
                  <span className="text-white">http://api-dev.portbuddy.dev <span className="text-slate-600">→</span> localhost:3000</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Features Grid */}
      <section id="features" className="container mx-auto px-4">
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
        </div>
      </section>

      {/* How it Works (Infographic style) */}
      <section id="how-it-works" className="container mx-auto px-4 py-12">
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
              description="Run `port-buddy 8080`. We create a secure tunnel to our edge network."
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
      <section id="use-cases" className="container mx-auto px-4">
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
          
          <div className="relative">
            <div className="absolute -inset-4 bg-gradient-to-r from-indigo-500 to-purple-500 opacity-20 blur-3xl rounded-full"></div>
            <div className="relative bg-slate-900 border border-slate-800 rounded-xl p-6 shadow-2xl">
               <div className="flex items-center justify-between border-b border-slate-800 pb-4 mb-4">
                 <div className="text-sm font-medium text-slate-300">Webhook Inspector</div>
                 <div className="flex gap-2">
                   <div className="w-2 h-2 rounded-full bg-red-500"></div>
                   <div className="w-2 h-2 rounded-full bg-yellow-500"></div>
                   <div className="w-2 h-2 rounded-full bg-green-500"></div>
                 </div>
               </div>
               <div className="space-y-3 font-mono text-xs md:text-sm">
                 <div className="bg-slate-800/50 p-3 rounded border-l-2 border-green-500">
                   <div className="flex justify-between text-slate-400 mb-1">
                     <span>POST /webhooks/stripe</span>
                     <span className="text-green-400">200 OK</span>
                   </div>
                   <div className="text-slate-500 truncate">{`{ "id": "evt_1M...", "type": "payment_intent.succeeded" }`}</div>
                 </div>
                 <div className="bg-slate-800/50 p-3 rounded border-l-2 border-green-500">
                   <div className="flex justify-between text-slate-400 mb-1">
                     <span>POST /webhooks/github</span>
                     <span className="text-green-400">200 OK</span>
                   </div>
                   <div className="text-slate-500 truncate">{`{ "action": "opened", "pull_request": { ... } }`}</div>
                 </div>
                 <div className="bg-slate-800/50 p-3 rounded border-l-2 border-red-500">
                   <div className="flex justify-between text-slate-400 mb-1">
                     <span>POST /api/callback</span>
                     <span className="text-red-400">500 Error</span>
                   </div>
                   <div className="text-slate-500 truncate">Error: Invalid signature</div>
                 </div>
               </div>
            </div>
          </div>
        </div>
      </section>

      {/* Pricing */}
      <section id="pricing" className="container mx-auto px-4">
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
      </section>

      {/* Final CTA */}
      <section className="container mx-auto px-4 pt-12">
        <div className="bg-gradient-to-r from-indigo-900/50 to-purple-900/50 border border-indigo-500/30 rounded-2xl p-12 text-center relative overflow-hidden">
          <div className="relative z-10">
            <h2 className="text-3xl font-bold text-white mb-4">Ready to get started?</h2>
            <p className="text-slate-300 mb-8 max-w-xl mx-auto">
              Join thousands of developers who use Port Buddy to accelerate their workflow.
            </p>
            <Link 
              to="/install" 
              className="inline-flex items-center justify-center px-8 py-3 bg-white text-indigo-900 rounded-lg font-bold hover:bg-slate-100 transition-colors"
            >
              Install Now
            </Link>
          </div>
        </div>
      </section>
    </div>
  )
}

function FeatureCard({ icon, title, description }: { icon: React.ReactNode, title: string, description: string }) {
  return (
    <div className="bg-slate-900/50 border border-slate-800 hover:border-slate-600 p-6 rounded-xl transition-all duration-300 hover:bg-slate-800/50 group">
      <div className="mb-4 p-3 bg-slate-800 rounded-lg inline-block group-hover:scale-110 transition-transform duration-300">
        {icon}
      </div>
      <h3 className="text-xl font-semibold text-white mb-2">{title}</h3>
      <p className="text-slate-400 leading-relaxed">{description}</p>
    </div>
  )
}

function Step({ number, title, description }: { number: string, title: string, description: string }) {
  return (
    <div className="relative z-10 flex flex-col items-center text-center">
      <div className="w-12 h-12 rounded-full bg-indigo-600 text-white font-bold text-xl flex items-center justify-center mb-4 shadow-lg shadow-indigo-500/30 border-4 border-slate-900">
        {number}
      </div>
      <h3 className="text-xl font-bold text-white mb-2">{title}</h3>
      <p className="text-slate-400 text-sm">{description}</p>
    </div>
  )
}

function UseCaseItem({ icon, title, description }: { icon: React.ReactNode, title: string, description: string }) {
  return (
    <div className="flex gap-4">
      <div className="flex-shrink-0 w-12 h-12 bg-slate-800 rounded-lg flex items-center justify-center text-indigo-400">
        {icon}
      </div>
      <div>
        <h3 className="text-lg font-semibold text-white mb-1">{title}</h3>
        <p className="text-slate-400 text-sm">{description}</p>
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
