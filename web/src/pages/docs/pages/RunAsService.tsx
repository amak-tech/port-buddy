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
import CodeBlock from '../../../components/CodeBlock'
import DocsPage from '../DocsPage'

const LEAD = 'You can configure Port Buddy to run as a background service. This ensures that your '
  + 'tunnel starts automatically on system boot and restarts if it fails.'

export default function RunAsService() {
  return (
    <DocsPage path="/docs/run-as-a-service" lead={LEAD}>
      <section className="mb-16">
        <p className="text-slate-400 mb-6">
          We provide helper scripts to set this up easily. You can run these scripts multiple times to set up different tunnels.
        </p>

        <div className="space-y-8">
          <div>
            <h2 className="text-2xl font-bold text-white mb-3">Linux (systemd)</h2>
            <CodeBlock code="curl -sSL https://portbuddy.dev/setup-portbuddy-service.sh | sudo bash -s -- [options] <mode> <port> [host]" />
          </div>

          <div>
            <h2 className="text-2xl font-bold text-white mb-3">Windows (Scheduled Task)</h2>
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

      <section className="mb-16">
        <h2 className="text-2xl font-bold text-white mb-6">Before you start</h2>
        <p className="text-slate-400">
          {'The machine needs the CLI '}
          <Link to="/install" className="text-indigo-400 hover:underline">installed</Link>
          {' and '}
          <Link to="/docs/authentication" className="text-indigo-400 hover:underline">authenticated</Link>
          {', because the service runs the same command you would type by hand. The mode argument '}
          {'is one of '}
          <Link to="/docs/http-tunnels" className="text-indigo-400 hover:underline">http</Link>,{' '}
          <Link to="/docs/tcp-tunnels" className="text-indigo-400 hover:underline">tcp</Link>
          {' or '}
          <Link to="/docs/udp-tunnels" className="text-indigo-400 hover:underline">udp</Link>.
        </p>
      </section>
    </DocsPage>
  )
}
