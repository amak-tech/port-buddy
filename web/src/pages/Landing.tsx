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
import {
  CommandLineIcon,
  GlobeAltIcon,
  ShieldCheckIcon,
  ServerIcon,
  BoltIcon,
  LockClosedIcon,
  CodeBracketIcon,
  ArrowRightIcon,
  CheckIcon,
  UserIcon,
  CloudIcon,
  ComputerDesktopIcon,
  ChatBubbleLeftRightIcon,
  CpuChipIcon
} from '@heroicons/react/24/outline'
import React, { useState, useEffect } from 'react'
import PlanComparison from '../components/PlanComparison'

// --- Helper Components ---

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

function Step({ number, title, description }: { number: string, title: string, description: string }) {
  return (
    <div className="relative z-10 flex flex-col items-center text-center group">
      <div className="w-16 h-16 rounded-2xl bg-slate-900 border border-slate-700 flex items-center justify-center text-xl font-bold text-white mb-6 shadow-xl group-hover:border-indigo-500/50 group-hover:shadow-[0_0_30px_rgba(99,102,241,0.3)] transition-all duration-300">
        <span className="text-transparent bg-clip-text bg-gradient-to-br from-white to-slate-500">{number}</span>
      </div>
      <h3 className="text-xl font-bold text-white mb-3">{title}</h3>
      <p className="text-slate-400 text-sm max-w-[200px] leading-relaxed">{description}</p>
    </div>
  )
}

function StatCard({ label, value, icon }: { label: string, value: string, icon: React.ReactNode }) {
  return (
    <div className="flex flex-col items-center p-6 rounded-2xl bg-slate-900/50 border border-white/5 backdrop-blur-sm">
      <div className="mb-3 text-slate-500">{icon}</div>
      <div className="text-3xl md:text-4xl font-bold text-white mb-1 tracking-tight">{value}</div>
      <div className="text-sm font-medium text-slate-400 uppercase tracking-widest">{label}</div>
    </div>
  )
}

function TestimonialCard({ quote, author, role }: { quote: string, author: string, role: string }) {
  return (
    <div className="p-8 rounded-2xl bg-gradient-to-b from-white/10 to-white/5 border border-white/10 backdrop-blur-sm flex flex-col h-full">
      <div className="mb-6">
        {[1, 2, 3, 4, 5].map((star) => (
          <span key={star} className="text-yellow-400 text-lg">★</span>
        ))}
      </div>
      <p className="text-slate-300 text-lg mb-6 leading-relaxed flex-grow">"{quote}"</p>
      <div className="flex items-center gap-4 mt-auto">
        <div className="w-10 h-10 rounded-full bg-indigo-500/20 flex items-center justify-center text-indigo-400 font-bold border border-indigo-500/30">
          {author.charAt(0)}
        </div>
        <div>
          <div className="text-white font-bold text-sm">{author}</div>
          <div className="text-slate-500 text-xs uppercase tracking-wider">{role}</div>
        </div>
      </div>
    </div>
  )
}

function FaqItem({ question, answer }: { question: string, answer: string }) {
  const [isOpen, setIsOpen] = useState(false)
  return (
    <div className="border-b border-white/10 last:border-0">
      <button 
        className="w-full flex items-center justify-between py-6 text-left focus:outline-none group"
        onClick={() => setIsOpen(!isOpen)}
      >
        <span className="text-lg font-medium text-slate-200 group-hover:text-white transition-colors">{question}</span>
        <span className={`ml-6 flex-shrink-0 transition-transform duration-300 ${isOpen ? 'rotate-180' : ''}`}>
          <ChevronDownIcon className="w-5 h-5 text-slate-500 group-hover:text-indigo-400" />
        </span>
      </button>
      <div className={`overflow-hidden transition-all duration-300 ease-in-out ${isOpen ? 'max-h-96 opacity-100 pb-6' : 'max-h-0 opacity-0'}`}>
        <p className="text-slate-400 leading-relaxed pr-12">{answer}</p>
      </div>
    </div>
  )
}

function ChevronDownIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
    </svg>
  )
}

function TypewriterText() {
  const words = ["Localhost", "Databases", "Webhooks", "APIs", "Game Servers", "Everything"]
  const [index, setIndex] = useState(0)
  const [subIndex, setSubIndex] = useState(0)
  const [reverse, setReverse] = useState(false)
  const [blink, setBlink] = useState(true)

  // Blinking cursor
  useEffect(() => {
    const timeout2 = setTimeout(() => setBlink((prev) => !prev), 500)
    return () => clearTimeout(timeout2)
  }, [blink])

  // Typewriter logic
  useEffect(() => {
    if (subIndex === words[index].length + 1 && !reverse) {
      setTimeout(() => setReverse(true), 1000)
      return
    }

    if (subIndex === 0 && reverse) {
      setReverse(false)
      setIndex((prev) => (prev + 1) % words.length)
      return
    }

    const timeout = setTimeout(() => {
      setSubIndex((prev) => prev + (reverse ? -1 : 1))
    }, reverse ? 75 : 150)

    return () => clearTimeout(timeout)
  }, [subIndex, index, reverse, words])

  return (
    <span>
      {words[index].substring(0, subIndex)}
      <span className={`${blink ? "opacity-100" : "opacity-0"} text-jb-blue ml-1`}>|</span>
    </span>
  )
}

// --- Main Component ---

export default function Landing() {
  return (
    <div className="flex flex-col gap-24 md:gap-32 pb-24">
      {/* Hero Section */}
      <section className="relative pt-20 pb-10 overflow-hidden">
        {/* Background Decorative Elements */}
        <div className="absolute top-0 left-1/2 -translate-x-1/2 w-full h-[800px] bg-mesh-gradient opacity-40 pointer-events-none" />
        <div className="absolute top-[10%] left-[10%] w-72 h-72 bg-jb-purple/20 rounded-full blur-[100px] pointer-events-none" />
        <div className="absolute top-[20%] right-[10%] w-96 h-96 bg-jb-blue/20 rounded-full blur-[120px] pointer-events-none" />
        
        <div className="container relative z-10">
          <div className="text-center max-w-5xl mx-auto">


            <h1 className="text-6xl md:text-8xl font-black tracking-tighter text-white mb-8 leading-[1.1]">
              Secure Tunnels for <br/>
              <TypewriterText/>
            </h1>

            <p className="text-xl md:text-2xl text-slate-400 mb-12 leading-relaxed max-w-3xl mx-auto font-light">
              Expose your local server to the internet in seconds. <br/>
              Production-ready security, developer-friendly experience.
            </p>

            <div className="flex flex-col sm:flex-row items-center justify-center gap-6">
              <Link
                to="/install"
                className="btn btn-primary w-full sm:w-auto text-lg py-4 px-10 shadow-[0_0_40px_-10px_rgba(99,102,241,0.5)] hover:shadow-[0_0_60px_-10px_rgba(99,102,241,0.6)]"
              >
                <CommandLineIcon className="w-6 h-6"/>
                Install CLI
              </Link>
              <Link
                to="/login"
                className="btn w-full sm:w-auto text-lg py-4 px-10 glass hover:bg-white/5 border border-white/10"
              >
                Get Started
                <ArrowRightIcon className="w-5 h-5"/>
              </Link>
            </div>

            <div className="mt-12 flex items-center justify-center gap-8 text-sm font-medium">
              <div className="flex items-center gap-2 text-slate-400">
                <CheckIcon className="w-5 h-5 text-green-400"/>
                <span>No credit card required</span>
              </div>
              <div className="flex items-center gap-2 text-slate-400">
                <CheckIcon className="w-5 h-5 text-green-400"/>
                <span>Free tier available</span>
              </div>
            </div>
          </div>

          {/* Terminal Preview */}
          <div className="mt-24 mx-auto max-w-4xl glass rounded-xl border border-white/10 shadow-[0_20px_80px_rgba(0,0,0,0.6)] overflow-hidden transform hover:scale-[1.01] transition-transform duration-500 group">
            <div className="flex items-center justify-between px-6 py-4 bg-[#0F1117] border-b border-white/5">
              <div className="flex gap-2">
                <div className="w-3 h-3 rounded-full bg-red-500/80"></div>
                <div className="w-3 h-3 rounded-full bg-yellow-500/80"></div>
                <div className="w-3 h-3 rounded-full bg-green-500/80"></div>
              </div>
              <div className="text-xs text-slate-500 font-mono tracking-widest uppercase opacity-50 group-hover:opacity-100 transition-opacity">user@machine:~</div>
              <div className="w-10"></div> {/* Spacer for center alignment */}
            </div>
            <div className="p-8 font-mono text-sm md:text-base overflow-x-auto leading-relaxed bg-[#0F1117]/95 backdrop-blur">
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
                  <span className="px-2 py-0.5 bg-green-500/10 text-green-400 rounded text-xs font-bold uppercase tracking-wider border border-green-500/20">Online</span>
                </div>
                <div className="flex gap-10">
                  <span className="text-slate-500 w-24">Forwarding</span>
                  <div className="flex flex-col gap-1">
                    <span className="text-slate-400">Local:  <span className="text-white hover:underline cursor-pointer">http://localhost:3000</span></span>
                    <span className="text-slate-400">Public: <span className="text-jb-pink hover:underline cursor-pointer font-bold">https://app.portbuddy.dev</span></span>
                  </div>
                </div>
                
                <div className="border-t border-white/5 my-6"></div>
                
                <div className="space-y-2 opacity-80">
                  <div className="flex gap-4 text-xs md:text-sm">
                    <span className="text-slate-600">14:32:01</span>
                    <span className="text-green-400 font-bold w-12">200 OK</span>
                    <span className="text-white">GET /api/users</span>
                    <span className="ml-auto text-slate-500">12ms</span>
                  </div>
                  <div className="flex gap-4 text-xs md:text-sm">
                    <span className="text-slate-600">14:32:05</span>
                    <span className="text-green-400 font-bold w-12">201 OK</span>
                    <span className="text-white">POST /api/webhooks/stripe</span>
                    <span className="ml-auto text-slate-500">45ms</span>
                  </div>
                   <div className="flex gap-4 text-xs md:text-sm">
                    <span className="text-slate-600">14:32:12</span>
                    <span className="text-yellow-400 font-bold w-12">401</span>
                    <span className="text-white">GET /admin/settings</span>
                    <span className="ml-auto text-slate-500">8ms</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Stats Section (Trust Builder) */}
      <section className="container">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 md:gap-8">
          <StatCard 
            icon={<GlobeAltIcon className="w-6 h-6" />}
            value="50+"
            label="Regions"
          />
           <StatCard 
            icon={<CpuChipIcon className="w-6 h-6" />}
            value="99.9%"
            label="Uptime"
          />
           <StatCard 
            icon={<UserIcon className="w-6 h-6" />}
            value="10k+"
            label="Users"
          />
           <StatCard 
            icon={<ShieldCheckIcon className="w-6 h-6" />}
            value="AES-256"
            label="Encryption"
          />
        </div>
      </section>

      {/* Features Grid */}
      <section id="features" className="container">
        <div className="text-center mb-16">
          <h2 className="text-3xl md:text-5xl font-black text-white mb-6">
            Everything you need for <br/>
            <span className="text-indigo-400">local development</span>
          </h2>
          <p className="text-slate-400 max-w-2xl mx-auto text-lg">
            Port Buddy is packed with features to help you develop, test, and demo your applications faster without compromising on security.
          </p>
        </div>

        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
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

      {/* Architecture Section */}
      <section id="architecture" className="container">
        <div className="relative glass rounded-3xl border border-white/5 p-8 md:p-20 overflow-hidden">
          {/* Animated Background */}
          <div className="absolute top-0 left-0 w-full h-full bg-jb-blue/5 opacity-30 pointer-events-none" />
          
          <div className="text-center mb-16 relative z-10">
            <h2 className="text-3xl md:text-4xl font-bold text-white mb-4">
              How it works
            </h2>
            <p className="text-slate-400 max-w-2xl mx-auto">
              A high-performance edge network that routes traffic securely to your machine.
            </p>
          </div>

          <div className="relative flex flex-col lg:flex-row items-center justify-between gap-12 lg:gap-8 z-10">
            {/* Public Client */}
            <div className="flex flex-col items-center text-center w-full lg:w-1/4 group">
              <div className="w-24 h-24 rounded-3xl bg-slate-800 flex items-center justify-center text-slate-300 mb-6 border border-white/10 shadow-2xl group-hover:-translate-y-2 transition-transform duration-300">
                <UserIcon className="w-10 h-10" />
              </div>
              <h3 className="text-lg font-bold text-white mb-2">Public Visitor</h3>
              <p className="text-sm text-slate-400 leading-relaxed">Accesses your app via <br/> <span className="text-jb-pink font-mono">*.portbuddy.dev</span></p>
            </div>

            {/* Arrow 1 */}
            <div className="hidden lg:flex flex-col items-center justify-center flex-1 px-4">
               <div className="w-full h-0.5 bg-slate-700 relative overflow-hidden">
                 <div className="absolute top-0 bottom-0 w-20 bg-gradient-to-r from-transparent via-jb-blue to-transparent animate-flow" />
               </div>
               <span className="text-[10px] text-slate-500 mt-4 uppercase tracking-widest font-bold">HTTPS/TCP</span>
            </div>

            {/* Port Buddy Cloud */}
            <div className="flex flex-col items-center text-center w-full lg:w-1/4 p-8 rounded-3xl bg-[#0F1117] border border-jb-blue/30 shadow-[0_0_50px_rgba(51,204,255,0.1)] relative z-20 group">
              <div className="absolute -top-3 -right-3 px-3 py-1 bg-jb-blue text-white text-[10px] font-bold rounded-full uppercase tracking-tighter shadow-lg">Edge Node</div>
              <div className="w-24 h-24 rounded-3xl bg-jb-blue/10 flex items-center justify-center text-jb-blue mb-6 border border-jb-blue/20 shadow-inner group-hover:-translate-y-2 transition-transform duration-300">
                <CloudIcon className="w-12 h-12" />
              </div>
              <h3 className="text-lg font-bold text-white mb-2">Port Buddy Cloud</h3>
              <p className="text-sm text-slate-400 leading-relaxed">Auth, SSL Termination & <br/> Request Routing</p>
            </div>

            {/* Arrow 2 (The Tunnel) */}
            <div className="hidden lg:flex flex-col items-center justify-center flex-1 px-4">
               <div className="w-full h-1 bg-slate-800 relative rounded-full overflow-hidden">
                 <div className="absolute inset-0 bg-gradient-to-r from-jb-blue/20 to-jb-purple/20 animate-pulse" />
                 <div className="absolute top-0 bottom-0 w-20 bg-gradient-to-r from-transparent via-white to-transparent animate-flow" style={{animationDelay: '0.5s'}} />
               </div>
               <div className="flex items-center gap-1 mt-4">
                 <LockClosedIcon className="w-3 h-3 text-green-400" />
                 <span className="text-[10px] text-slate-500 uppercase tracking-widest font-bold">Secure Tunnel</span>
               </div>
            </div>

            {/* Local Environment */}
            <div className="flex flex-col items-center text-center w-full lg:w-1/4 p-8 rounded-3xl bg-white/[0.02] border border-white/10 shadow-xl group">
              <div className="w-24 h-24 rounded-3xl bg-slate-800 flex items-center justify-center text-jb-purple mb-6 border border-white/10 shadow-2xl group-hover:-translate-y-2 transition-transform duration-300">
                <ComputerDesktopIcon className="w-10 h-10" />
              </div>
              <h3 className="text-lg font-bold text-white mb-2">Your Machine</h3>
              <div className="flex flex-col gap-2 mt-2">
                 <div className="px-3 py-1 bg-white/5 rounded text-[10px] font-mono text-jb-purple border border-white/5">Port Buddy CLI</div>
                 <div className="px-3 py-1 bg-white/5 rounded text-[10px] font-mono text-slate-400 border border-white/5">Localhost:3000</div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Testimonials (Trust Builder) */}
      <section className="container">
        <h2 className="text-3xl md:text-4xl font-bold text-center text-white mb-16">
          Loved by developers
        </h2>
        <div className="grid md:grid-cols-3 gap-6">
          <TestimonialCard 
            quote="Finally, a tunneling tool that is simple, fast, and doesn't break. I use it daily for webhook testing."
            author="Sarah Jenkins"
            role="Senior Backend Engineer"
          />
          <TestimonialCard 
            quote="The custom domain feature is a lifesaver. Being able to show clients a consistent URL during demos is huge."
            author="David Chen"
            role="Freelance Developer"
          />
          <TestimonialCard 
            quote="I switched from ngrok because of the pricing, but stayed for the speed. Port Buddy is blazing fast."
            author="Michael Rossi"
            role="CTO @ StartupX"
          />
        </div>
      </section>

      {/* Use Cases */}
      <section id="use-cases" className="container bg-slate-900/50 py-16 rounded-3xl border border-white/5 overflow-hidden">
        <div className="grid lg:grid-cols-2 gap-16 items-center px-4 md:px-12">
          <div>
            <h2 className="text-3xl md:text-4xl font-bold text-white mb-6">
              Built for modern workflows
            </h2>
            <p className="text-slate-400 mb-8 text-lg leading-relaxed">
              From webhooks to client demos, Port Buddy streamlines your development workflow. Stop deploying just to test a small change.
            </p>
            
            <div className="space-y-6">
              <div className="flex gap-4">
                <div className="p-3 rounded-lg bg-indigo-500/10 border border-indigo-500/20 h-fit">
                   <ChatBubbleLeftRightIcon className="w-6 h-6 text-indigo-400" />
                </div>
                <div>
                  <h3 className="text-white font-bold mb-1">Test Webhooks</h3>
                  <p className="text-slate-400 text-sm">Receive webhooks from Stripe, GitHub, or Twilio directly to your localhost.</p>
                </div>
              </div>
              <div className="flex gap-4">
                <div className="p-3 rounded-lg bg-pink-500/10 border border-pink-500/20 h-fit">
                   <GlobeAltIcon className="w-6 h-6 text-pink-400" />
                </div>
                <div>
                  <h3 className="text-white font-bold mb-1">Demo to Clients</h3>
                  <p className="text-slate-400 text-sm">Share your work in progress with clients or colleagues instantly.</p>
                </div>
              </div>
              <div className="flex gap-4">
                <div className="p-3 rounded-lg bg-cyan-500/10 border border-cyan-500/20 h-fit">
                   <CodeBracketIcon className="w-6 h-6 text-cyan-400" />
                </div>
                <div>
                  <h3 className="text-white font-bold mb-1">Mobile Testing</h3>
                  <p className="text-slate-400 text-sm">Test your responsive designs on real mobile devices via public URL.</p>
                </div>
              </div>
            </div>
          </div>
          
          <div className="relative min-w-0">
             <div className="absolute inset-0 bg-gradient-to-br from-indigo-500/20 to-purple-500/20 blur-[60px] rounded-full pointer-events-none" />
             <div className="relative bg-slate-950 border border-slate-800 rounded-xl p-6 shadow-2xl">
               <div className="flex items-center gap-2 mb-4 border-b border-slate-800 pb-4">
                 <div className="w-3 h-3 rounded-full bg-slate-700"></div>
                 <div className="w-3 h-3 rounded-full bg-slate-700"></div>
                 <div className="flex-1 text-center text-xs text-slate-500">webhook-handler.js</div>
               </div>
               <pre className="font-mono text-sm text-slate-300 overflow-x-auto">
                 <code>
{`app.post('/webhook', (req, res) => {
  const event = req.body;
  
  // Handle the event locally
  console.log('Received event:', event.type);
  
  if (event.type === 'payment_succeeded') {
    fulfillOrder(event.data);
  }

  res.json({received: true});
});`}
                 </code>
               </pre>
             </div>
          </div>
        </div>
      </section>

      {/* Pricing */}
      <section id="pricing" className="container">
        <div className="text-center mb-16">
          <h2 className="text-3xl md:text-5xl font-black text-white mb-6">
            Simple, transparent pricing
          </h2>
          <p className="text-slate-400 text-lg">
            Start for free, upgrade as you grow.
          </p>
        </div>
        <PlanComparison />
      </section>

      {/* FAQ */}
      <section className="container max-w-3xl">
        <h2 className="text-3xl font-bold text-center text-white mb-12">Frequently Asked Questions</h2>
        <div className="space-y-2">
          <FaqItem 
            question="Is Port Buddy secure?" 
            answer="Yes. All traffic is encrypted end-to-end. We use industry-standard TLS encryption for tunnels. For private tunnels, you can enforce Basic Auth or IP Allowlisting." 
          />
          <FaqItem 
            question="How does it differ from ngrok?" 
            answer="Port Buddy is designed to be a simpler, more affordable alternative with a focus on developer experience. We offer features like static subdomains and custom domains at a much lower price point." 
          />
          <FaqItem 
            question="Can I use my own domain?" 
            answer="Absolutely. You can connect your own domain name (e.g., tunnel.yourcompany.com) and we will automatically handle SSL certificates for you." 
          />
          <FaqItem 
            question="Do you support TCP tunnels?" 
            answer="Yes, TCP and UDP tunnels are supported. This is perfect for exposing databases, game servers, or RDP/SSH connections." 
          />
        </div>
      </section>

      {/* Final CTA */}
      <section className="container py-12">
        <div className="relative rounded-3xl overflow-hidden p-12 md:p-24 text-center">
          <div className="absolute inset-0 bg-gradient-to-br from-indigo-900/50 to-purple-900/50 z-0" />
          <div className="absolute inset-0 bg-mesh-gradient opacity-30 z-0" />
          
          <div className="relative z-10 max-w-3xl mx-auto">
            <h2 className="text-4xl md:text-5xl font-black text-white mb-8 tracking-tight">
              Ready to code from anywhere?
            </h2>
            <p className="text-xl text-slate-300 mb-10">
              Join thousands of developers who trust Port Buddy for their local development.
            </p>
            <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
              <Link 
                to="/install" 
                className="btn btn-primary text-lg py-4 px-12 w-full sm:w-auto"
              >
                Get Started for Free
              </Link>
              <Link 
                to="/docs" 
                className="btn glass text-lg py-4 px-12 w-full sm:w-auto hover:bg-white/10"
              >
                Read Documentation
              </Link>
            </div>
          </div>
        </div>
      </section>
    </div>
  )
}