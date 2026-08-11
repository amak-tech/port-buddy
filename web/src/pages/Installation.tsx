import { useState } from 'react'
import { Link } from 'react-router-dom'
import { CommandLineIcon, ComputerDesktopIcon } from '@heroicons/react/24/outline'
import CodeBlock from '../components/CodeBlock'
import { EXTRA_TUNNEL_NOTE, PLAN_LIST, freeTunnelsLabel, type Plan } from '../config/plans'
import JsonLd from '../lib/seo/JsonLd'
import { breadcrumbListSchema } from '../lib/seo/schemas'

// Breadcrumbs only: the install steps live behind per-OS tabs, so only the active tab is rendered
// and a HowTo would describe steps that are not on the page.
const installSchema = breadcrumbListSchema([
  { name: 'Home', path: '/' },
  { name: 'Installation', path: '/install' }
])

export default function Installation() {
  const [activeTab, setActiveTab] = useState<'macos' | 'linux' | 'windows' | 'docker'>('macos')

  return (
    <div className="min-h-screen flex flex-col">
      <JsonLd data={installSchema} />
      <div className="flex-1 relative pt-12 pb-12 md:pb-20">
        {/* Background gradients */}
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-indigo-900/20 via-slate-900/0 to-slate-900/0 pointer-events-none" />
        
        <div className="container max-w-4xl relative z-10">
          <div className="text-center mb-12">
            <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-indigo-500/10 border border-indigo-500/20 text-indigo-400 mb-6">
              <CommandLineIcon className="w-8 h-8" />
            </div>
            <h1 className="text-4xl font-bold text-white mb-4">Install Port Buddy CLI</h1>
            <p className="text-slate-400 text-lg max-w-2xl mx-auto">
              Get up and running in seconds. Our CLI is a single static binary with zero dependencies.
            </p>
          </div>

          <div className="bg-slate-900/50 border border-slate-800 rounded-2xl overflow-hidden shadow-2xl">
            {/* Tab Navigation */}
            <div className="flex border-b border-slate-800 overflow-x-auto">
              <TabButton 
                isActive={activeTab === 'macos'} 
                onClick={() => setActiveTab('macos')} 
                label="macOS"
              />
              <TabButton 
                isActive={activeTab === 'linux'} 
                onClick={() => setActiveTab('linux')} 
                label="Linux"
              />
              <TabButton 
                isActive={activeTab === 'windows'} 
                onClick={() => setActiveTab('windows')} 
                label="Windows"
              />
              <TabButton 
                isActive={activeTab === 'docker'} 
                onClick={() => setActiveTab('docker')} 
                label="Docker"
              />
            </div>

            {/* Content Area */}
            <div className="p-4 md:p-8 min-h-[400px]">
              {activeTab === 'macos' && (
                <div className="space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-500">
                  <Step 
                    title="Install via Homebrew"
                    description="The easiest way to install on macOS."
                  >
                    <CodeBlock code={`brew install amak-tech/tap/portbuddy`} />
                  </Step>
                  <Step 
                    title="Expose your local node.js app"
                    description="This command will create a secured HTTP tunnel for localhost:3000"
                  >
                    <CodeBlock code="portbuddy 3000" />
                  </Step>
                </div>
              )}

              {activeTab === 'linux' && (
                <div className="space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-500">
                  <Step
                      title="Install via Script"
                      description="The fastest way to install Port Buddy on Linux. Automatically detects your architecture."
                  >
                    <CodeBlock code={`curl -sSL https://portbuddy.dev/install.sh | sudo bash`} />
                  </Step>
                  <Step 
                    title="Install via Homebrew"
                    description="Recommended for Linux users who use Homebrew."
                  >
                    <CodeBlock code={`brew install amak-tech/tap/portbuddy`} />
                  </Step>
                  <Step
                    title="Share access to your local PostgreSQL DB"
                    description="This command will create a TCP tunnel for localhost:5432"
                  >
                    <CodeBlock code="portbuddy tcp 5432" />
                  </Step>
                  <div className="pt-2">
                    <p className="text-slate-400">
                      Need to run in background? <Link to="/docs/run-as-a-service" className="text-indigo-400 hover:text-indigo-300 transition-colors">Run as a Service guide &rarr;</Link>
                    </p>
                  </div>
                </div>
              )}

              {activeTab === 'windows' && (
                <div className="space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-500">
                  <Step 
                    title="Install via PowerShell"
                    description="The fastest way to install Port Buddy on Windows. This script will download the executable and add it to your PATH."
                  >
                    <CodeBlock code={`iwr https://portbuddy.dev/install.ps1 -useb | iex`} />
                  </Step>
                  <Step 
                    title="Direct Download"
                    description="Download the executable manually and add it to your PATH."
                  >
                    <div className="flex flex-col gap-4">
                      <a 
                        href="https://github.com/amak-tech/port-buddy/releases/latest" 
                        className="inline-flex items-center justify-center px-6 py-3 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-medium transition-colors w-full md:w-auto"
                      >
                        Download latest version
                      </a>
                    </div>
                  </Step>
                  <Step 
                    title="Quick Start"
                    description="Link the CLI to your account and expose your first port."
                  >
                    <CodeBlock code="portbuddy init YOUR_API_TOKEN" />
                  </Step>
                  <div className="pt-2">
                    <p className="text-slate-400">
                      Need to run in background? <Link to="/docs/run-as-a-service" className="text-indigo-400 hover:text-indigo-300 transition-colors">Run as a Service guide &rarr;</Link>
                    </p>
                  </div>
                </div>
              )}

              {activeTab === 'docker' && (
                <div className="space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-500">
                  <Step 
                    title="Pull Docker Image"
                    description="Get the official Port Buddy image from Docker Hub."
                  >
                    <CodeBlock code="docker pull portbuddy/portbuddy" />
                  </Step>
                  <Step 
                    title="Authentication with Docker"
                    description="To use Port Buddy in Docker, mount your token file. The CLI expects it at /root/.port-buddy/token."
                  >
                    <div className="space-y-4">
                      <p className="text-slate-400 text-sm">First, authenticate on your host machine:</p>
                      <CodeBlock code="portbuddy init YOUR_API_TOKEN" />
                      <p className="text-slate-400 text-sm">Then, run with the token mounted:</p>
                      <CodeBlock code={`docker run -it --rm \\
  -v ~/.port-buddy/token:/root/.port-buddy/token \\
  --network host \\
  portbuddy/portbuddy 3000`} />
                    </div>
                  </Step>
                  <Step 
                    title="Mounting a Token File Directly"
                    description="If you have your API token in a file, you can mount it directly."
                  >
                    <CodeBlock code={`docker run -it --rm \\
  -v $(pwd)/my_token.txt:/root/.port-buddy/token \\
  --network host \\
  portbuddy/portbuddy 3000`} />
                  </Step>
                </div>
              )}
            </div>
          </div>

          <div className="mt-12 grid md:grid-cols-2 gap-6">
            {PLAN_LIST.map((plan) => (
              <PlanLimitCard key={plan.id} plan={plan} isPro={plan.id === 'pro'} />
            ))}
          </div>

          <div className="mt-12 grid md:grid-cols-3 gap-6">
            <InfoCard 
              title="Auto-Updates" 
              description="The CLI automatically checks for updates and notifies you when a new version is available."
            />
            <InfoCard 
              title="Cross-Platform" 
              description="Native binaries for macOS (Intel/Apple Silicon), Linux (x64/ARM), and Windows."
            />
            <InfoCard 
              title="Zero Config" 
              description="Smart defaults mean you rarely need to touch a config file. It just works."
            />
          </div>

        </div>
      </div>
    </div>
  )
}

function TabButton({ isActive, onClick, label }: { isActive: boolean, onClick: () => void, label: string }) {
  return (
    <button
      onClick={onClick}
      className={`flex-1 px-6 py-4 text-sm font-medium transition-all relative ${
        isActive 
          ? 'text-white bg-slate-800/50' 
          : 'text-slate-400 hover:text-white hover:bg-slate-800/30'
      }`}
    >
      {label}
      {isActive && (
        <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-indigo-500"></div>
      )}
    </button>
  )
}

function Step({ title, description, children }: { title: string, description: string, children: React.ReactNode }) {
  return (
    <div>
      <h3 className="text-lg font-semibold text-white mb-1">{title}</h3>
      <p className="text-slate-400 text-sm mb-4">{description}</p>
      {children}
    </div>
  )
}

function InfoCard({ title, description }: { title: string, description: string }) {
  return (
    <div className="bg-slate-900/30 border border-slate-800 rounded-xl p-6">
      <h4 className="text-white font-medium mb-2 flex items-center gap-2">
        <ComputerDesktopIcon className="w-5 h-5 text-indigo-500" />
        {title}
      </h4>
      <p className="text-slate-400 text-sm leading-relaxed">
        {description}
      </p>
    </div>
  )
}

function PlanLimitCard({ plan, isPro }: { plan: Plan, isPro?: boolean }) {
  return (
    <div className={`relative p-6 rounded-2xl border ${isPro ? 'border-indigo-500/30 bg-indigo-500/5' : 'border-slate-800 bg-slate-900/30'}`}>
      <div className="flex justify-between items-start mb-4">
        <div>
          <h4 className="text-lg font-bold text-white mb-1">{`${plan.name} Plan`}</h4>
          <div className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
            {freeTunnelsLabel(plan)}
          </div>
        </div>
      </div>
      <p className="text-slate-400 text-sm leading-relaxed">
        {`${plan.shortTagline} ${EXTRA_TUNNEL_NOTE}`}
      </p>
    </div>
  )
}
