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

const LEAD = 'HTTP is the default mode for Port Buddy. It is used for web applications and APIs.'

export default function HttpTunnels() {
  return (
    <DocsPage path="/docs/http-tunnels" lead={LEAD}>
      <section className="mb-16">
        <h2 className="text-2xl font-bold text-white mb-6">Usage</h2>
        <CodeBlock code="portbuddy 3000" />
        <p className="text-slate-400 mt-4">
          {'This command exposes your local web server running on port 3000 and prints the public '}
          {'URL it was given, such as '}
          <code className="text-indigo-300">https://otter-4821.portbuddy.dev</code>
          {'. Because HTTP is the default, the mode can be left out; pass one explicitly when you '}
          {'need raw '}
          <Link to="/docs/tcp-tunnels" className="text-indigo-400 hover:underline">TCP</Link>
          {' or '}
          <Link to="/docs/udp-tunnels" className="text-indigo-400 hover:underline">UDP</Link>
          {' instead.'}
        </p>
        <p className="text-slate-400 mt-4">
          {'The tunnel stays open until you stop the CLI. Traffic reaches your machine over HTTPS '}
          {'even though your local server speaks plain HTTP — the certificate is ours and is '}
          {'terminated at the edge.'}
        </p>
      </section>

      <section className="mb-16">
        <h2 className="text-2xl font-bold text-white mb-6">Choosing what to expose</h2>
        <p className="text-slate-400 mb-4">
          A bare port means <code className="text-indigo-300">localhost</code> on that port. You can
          also name a different host on your network, or point at a local server that already speaks
          TLS.
        </p>
        <CodeBlock code={`portbuddy 3000                    # localhost:3000
portbuddy 192.168.1.10:8080       # another machine on your LAN
portbuddy https://localhost:8443  # local server with its own certificate`} />
      </section>

      <section className="mb-16">
        <h2 className="text-2xl font-bold text-white mb-6">Picking the subdomain</h2>
        <p className="text-slate-400 mb-4">
          {'Every account has one or more subdomains, managed on the '}
          <Link to="/app/domains" className="text-indigo-400 hover:underline">Domains</Link>
          {' page. Ask for a specific one with '}
          <code className="text-indigo-300">--domain</code>
          {', giving either the label or the full hostname:'}
        </p>
        <CodeBlock code={`portbuddy --domain=my-app 8080
portbuddy --domain=my-app.portbuddy.dev 8080`} />
        <p className="text-slate-400 mt-4">
          {'The subdomain has to already belong to your account — this requests one, it does not '}
          {'claim a new one — and it must not be in use by another running tunnel.'}
        </p>
        <p className="text-slate-400 mt-4">
          {'Leave the option out and Port Buddy picks for you. If you have used a subdomain for '}
          {'this same local host and port before, you get that one back, so restarting a tunnel '}
          {'usually keeps the URL your colleagues already have. If several subdomains are free and '}
          {'none of them matches, the CLI stops and lists the candidates rather than guessing.'}
        </p>
      </section>

      <section className="mb-16">
        <h2 className="text-2xl font-bold text-white mb-6">What the tunnel carries</h2>
        <ul className="list-disc list-inside text-slate-400 space-y-2">
          <li>WebSocket connections upgrade through the same tunnel, so live-reload, chat and realtime apps work unchanged.</li>
          <li>Requests are capped at 10 MB, which is worth knowing before you test a large file upload.</li>
          <li>Each request is printed in your terminal as it arrives. Pass <code className="text-indigo-300">-n</code> to turn that log off.</li>
          <li>The CLI heartbeats while connected; if the process dies, the tunnel is closed for you within seconds rather than lingering.</li>
        </ul>
      </section>

      <section className="mb-16">
        <h2 className="text-2xl font-bold text-white mb-6">Going further</h2>
        <ul className="list-disc list-inside text-slate-400 space-y-2">
          <li>
            {'Swap the generated subdomain for a domain you own — see '}
            <Link to="/docs/custom-domains" className="text-indigo-400 hover:underline">custom domains</Link>.
          </li>
          <li>
            {'Keep the URL to yourself by putting a passcode in front of it — see '}
            <Link to="/docs/private-tunnels" className="text-indigo-400 hover:underline">private tunnels</Link>.
          </li>
          <li>
            {'Keep a tunnel up after you close the terminal by installing it as a '}
            <Link to="/docs/run-as-a-service" className="text-indigo-400 hover:underline">background service</Link>.
          </li>
        </ul>
      </section>
    </DocsPage>
  )
}
