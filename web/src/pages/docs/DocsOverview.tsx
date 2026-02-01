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
  BoltIcon,
  LockClosedIcon,
  BookOpenIcon,
  InformationCircleIcon
} from '@heroicons/react/24/outline'
import CodeBlock from '../../components/CodeBlock'

export default function DocsOverview() {
  return (
    <>
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

      <section id="run-as-service" className="mb-16 scroll-mt-24">
        <h2 className="text-2xl font-bold text-white mb-6">Run as Service</h2>
        <p className="text-slate-400 mb-6">
          You can configure Port Buddy to run as a background service. This ensures that your tunnel starts automatically on system boot and restarts if it fails.
        </p>
        <p className="text-slate-400 mb-6">
          We provide helper scripts to set this up easily. You can run these scripts multiple times to set up different tunnels.
        </p>
        
        <div className="space-y-8">
          <div>
            <h3 className="text-lg font-semibold text-white mb-3">Linux (systemd)</h3>
            <CodeBlock code="curl -sSL https://portbuddy.dev/setup-portbuddy-service.sh | sudo bash -s -- [options] <mode> <port> [host]" />
          </div>

          <div>
            <h3 className="text-lg font-semibold text-white mb-3">Windows (Scheduled Task)</h3>
            <p className="text-slate-400 mb-2 text-sm">Run as Administrator:</p>
            <CodeBlock code={`iwr https://portbuddy.dev/setup-portbuddy-service.ps1 -OutFile setup-service.ps1
./setup-service.ps1 [options] <mode> <port> [host]`} />
          </div>
        </div>

        <h3 className="text-lg font-semibold text-white mb-3 mt-6">Options</h3>
        <ul className="list-disc list-inside text-slate-400 mb-4 space-y-1">
            <li><code className="text-indigo-300">--name &lt;name&gt;</code> (Linux) or <code className="text-indigo-300">-Name &lt;name&gt;</code> (Windows) - Custom name for the service.</li>
        </ul>
        
        <h3 className="text-lg font-semibold text-white mb-3 mt-6">Example</h3>
        <p className="text-slate-400 mb-4">
          To expose port 22 (SSH) over TCP and run it as a service:
        </p>
        
        <div className="grid gap-4">
            <div>
                <p className="text-sm text-slate-500 mb-2 font-semibold">Linux:</p>
                <CodeBlock code="curl -sSL https://portbuddy.dev/setup-portbuddy-service.sh | sudo bash -s -- tcp 22" />
            </div>
            <div>
                <p className="text-sm text-slate-500 mb-2 font-semibold">Windows:</p>
                <CodeBlock code="./setup-service.ps1 tcp 22" />
            </div>
        </div>
        
        <p className="text-slate-400 mt-4 mb-4">
          By default, the service name follows the pattern <code className="text-indigo-300">portbuddy-&lt;mode&gt;-&lt;port&gt;</code>.
        </p>

        <h3 className="text-lg font-semibold text-white mb-3 mt-6">Custom Service Name</h3>
        <div className="grid gap-4">
            <div>
                <p className="text-sm text-slate-500 mb-2 font-semibold">Linux:</p>
                <CodeBlock code="curl -sSL https://portbuddy.dev/setup-portbuddy-service.sh | sudo bash -s -- --name my-ssh-service tcp 22" />
            </div>
            <div>
                <p className="text-sm text-slate-500 mb-2 font-semibold">Windows:</p>
                <CodeBlock code="./setup-service.ps1 -Name my-ssh-service tcp 22" />
            </div>
        </div>
        
        <h3 className="text-lg font-semibold text-white mb-3 mt-6">Managing the Service</h3>
        <div className="grid md:grid-cols-2 gap-4">
            <div>
                <p className="text-sm text-slate-500 mb-2 font-semibold">Linux (systemctl):</p>
                <CodeBlock code={`sudo systemctl status portbuddy-tcp-22
sudo systemctl stop portbuddy-tcp-22
sudo systemctl start portbuddy-tcp-22`} />
            </div>
            <div>
                <p className="text-sm text-slate-500 mb-2 font-semibold">Windows (PowerShell):</p>
                <CodeBlock code={`Get-ScheduledTask -TaskName portbuddy-tcp-22
Stop-ScheduledTask -TaskName portbuddy-tcp-22
Start-ScheduledTask -TaskName portbuddy-tcp-22`} />
            </div>
        </div>
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
    </>
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
