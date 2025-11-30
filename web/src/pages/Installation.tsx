import { useState } from 'react'
import { 
  CommandLineIcon, 
  ClipboardDocumentIcon, 
  CheckIcon,
  ComputerDesktopIcon
} from '@heroicons/react/24/outline'
import Seo from '../components/Seo'

export default function Installation() {
  const [activeTab, setActiveTab] = useState<'macos' | 'linux' | 'windows'>('macos')

  return (
    <div className="min-h-screen flex flex-col">
      <Seo 
        title="Install Port Buddy - Windows, macOS, Linux | Port Buddy CLI"
        description="Install Port Buddy CLI on your machine. Supports Homebrew (macOS), Scoop (Windows), and Shell script (Linux). Get started with secure localhost tunneling."
        keywords="install port buddy, download port buddy, port buddy cli, homebrew port buddy, scoop port buddy, linux port forwarding"
      />
      <div className="flex-1 relative pt-20 pb-20 px-4">
        {/* Background gradients */}
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-indigo-900/20 via-slate-900/0 to-slate-900/0 pointer-events-none" />
        
        <div className="container mx-auto max-w-4xl relative z-10">
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
            </div>

            {/* Content Area */}
            <div className="p-8 min-h-[400px]">
              {activeTab === 'macos' && (
                <div className="space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-500">
                  <Step 
                    title="Install via Homebrew"
                    description="The easiest way to install on macOS."
                  >
                    <CodeBlock code={`brew tap port-buddy/tap\nbrew install port-buddy`} />
                  </Step>
                  <Step 
                    title="Initialize"
                    description="Link the CLI to your account."
                  >
                    <CodeBlock code="port-buddy init {API_TOKEN}" />
                  </Step>
                </div>
              )}

              {activeTab === 'linux' && (
                <div className="space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-500">
                  <Step 
                    title="Install via Script"
                    description="Download and install the binary to /usr/local/bin."
                  >
                    <CodeBlock code={`curl -L https://github.com/port-buddy/cli/releases/download/v1.0.0/port-buddy-linux-amd64 -o port-buddy\nchmod +x port-buddy\nsudo mv port-buddy /usr/local/bin/`} />
                  </Step>
                  <Step 
                    title="Initialize"
                    description="Link the CLI to your account."
                  >
                    <CodeBlock code="port-buddy init {API_TOKEN}" />
                  </Step>
                </div>
              )}

              {activeTab === 'windows' && (
                <div className="space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-500">
                  <Step 
                    title="Install via Scoop"
                    description="Recommended for Windows users."
                  >
                    <CodeBlock code={`scoop bucket add port-buddy https://github.com/port-buddy/scoop-bucket\nscoop install port-buddy`} />
                  </Step>
                  <Step 
                    title="Initialize (PowerShell)"
                    description="Link the CLI to your account."
                  >
                    <CodeBlock code="port-buddy init {API_TOKEN}" />
                  </Step>
                </div>
              )}
            </div>
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

function CodeBlock({ code }: { code: string }) {
  const [copied, setCopied] = useState(false)

  const copy = () => {
    navigator.clipboard.writeText(code)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  return (
    <div className="relative group">
      <div className="absolute -inset-0.5 bg-gradient-to-r from-indigo-500/20 to-purple-500/20 rounded-lg blur opacity-0 group-hover:opacity-100 transition duration-500"></div>
      <div className="relative bg-slate-950 border border-slate-800 rounded-lg p-4 font-mono text-sm text-slate-300 overflow-x-auto">
        <pre>{code}</pre>
        <button 
          onClick={copy}
          className="absolute top-3 right-3 p-2 rounded-md bg-slate-800/50 text-slate-400 hover:text-white hover:bg-slate-700 transition-all opacity-0 group-hover:opacity-100 focus:opacity-100"
          title="Copy to clipboard"
        >
          {copied ? <CheckIcon className="w-4 h-4 text-green-400" /> : <ClipboardDocumentIcon className="w-4 h-4" />}
        </button>
      </div>
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
