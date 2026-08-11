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
        <p className="text-slate-400 mb-6">
          {'Both scripts install the same command you would type by hand, so the machine needs the '}
          {'CLI '}
          <Link to="/install" className="text-indigo-400 hover:underline">installed</Link>
          {' and '}
          <Link to="/docs/authentication" className="text-indigo-400 hover:underline">authenticated</Link>
          {' first. The service runs under your account rather than a service identity, precisely '}
          {'so it can find the token you saved. The mode is one of '}
          <Link to="/docs/http-tunnels" className="text-indigo-400 hover:underline">http</Link>,{' '}
          <Link to="/docs/tcp-tunnels" className="text-indigo-400 hover:underline">tcp</Link>
          {' or '}
          <Link to="/docs/udp-tunnels" className="text-indigo-400 hover:underline">udp</Link>
          {', and the optional third argument exposes a host other than localhost.'}
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
        <h2 className="text-2xl font-bold text-white mb-6">What the scripts install</h2>
        <p className="text-slate-400 mb-4">
          {'On Linux the script writes a systemd unit to '}
          <code className="text-indigo-300">/etc/systemd/system/</code>
          {', enables it so it starts at boot, and starts it straight away. The unit waits for the '}
          {'network, runs as the user who invoked sudo, and restarts five seconds after any '}
          {'failure. Request logging is switched off, since nobody is watching the terminal.'}
        </p>
        <p className="text-slate-400">
          {'On Windows it registers a Scheduled Task that triggers at startup and runs as SYSTEM, '}
          {'with your user profile pinned so the CLI still finds your token. A failed task is '}
          {'retried three times at one-minute intervals, and it has no run-time limit.'}
        </p>
      </section>

      <section className="mb-16">
        <h2 className="text-2xl font-bold text-white mb-6">Logs</h2>
        <p className="text-slate-400 mb-4">
          The service writes to the platform's own log, so there is no Port Buddy log file to find:
        </p>
        <div className="grid md:grid-cols-2 gap-4">
          <div>
            <p className="text-sm text-slate-500 mb-2 font-semibold">Linux:</p>
            <CodeBlock code="journalctl -u portbuddy-tcp-22 -f" />
          </div>
          <div>
            <p className="text-sm text-slate-500 mb-2 font-semibold">Windows:</p>
            <CodeBlock code="Get-ScheduledTaskInfo -TaskName portbuddy-tcp-22" />
          </div>
        </div>
        <p className="text-slate-400 mt-4">
          The public address a tunnel was given is printed when it starts, so the log is also where
          you look it up after a reboot.
        </p>
      </section>

      <section className="mb-16">
        <h2 className="text-2xl font-bold text-white mb-6">Updating and removing</h2>
        <p className="text-slate-400 mb-4">
          {'The unit points at the binary that was on your PATH when you installed it, so upgrading '}
          {'the CLI in place needs nothing more than a restart. To change the port or mode, run the '}
          {'setup script again with the new arguments and the same name.'}
        </p>
        <div className="grid md:grid-cols-2 gap-4">
          <div>
            <p className="text-sm text-slate-500 mb-2 font-semibold">Linux:</p>
            <CodeBlock code={`sudo systemctl disable --now portbuddy-tcp-22
sudo rm /etc/systemd/system/portbuddy-tcp-22.service
sudo systemctl daemon-reload`} />
          </div>
          <div>
            <p className="text-sm text-slate-500 mb-2 font-semibold">Windows:</p>
            <CodeBlock code="Unregister-ScheduledTask -TaskName portbuddy-tcp-22" />
          </div>
        </div>
      </section>

      <section className="mb-16">
        <h2 className="text-2xl font-bold text-white mb-6">Running several at once</h2>
        <p className="text-slate-400">
          {'Each run of the script installs one service, named after the mode and port unless you '}
          {'pass a name, so running it again for a different port simply adds a second service. '}
          {'Two tunnels of the same mode and port on one machine need distinct names — and bear in '}
          {'mind that each running tunnel counts against your plan’s concurrent tunnel limit.'}
        </p>
      </section>
    </DocsPage>
  )
}
