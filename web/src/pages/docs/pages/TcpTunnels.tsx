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
import { TCP_MIN_TUNNELS } from '../../../config/plans'

const LEAD = 'TCP mode allows you to expose any TCP-based service, such as databases or SSH. '
  + `TCP tunnels are a paid capability: your account needs at least ${TCP_MIN_TUNNELS} tunnels `
  + '(add extra tunnels on the Pro plan, or use the Team plan).'

export default function TcpTunnels() {
  return (
    <DocsPage path="/docs/tcp-tunnels" lead={LEAD}>
      <section className="mb-16">
        <h2 className="text-2xl font-bold text-white mb-6">Usage</h2>
        <CodeBlock code="portbuddy tcp 5432" />
        <p className="text-slate-400 mt-4">
          {'This command exposes your local PostgreSQL database on port 5432. You will get an '}
          {'address like '}
          <code className="text-indigo-300">net-proxy-3.portbuddy.dev:43452</code>
          {' — a host and a port, not a URL. There is no TLS and no hostname routing at this '}
          {'layer: whatever your local service speaks is what arrives, byte for byte.'}
        </p>
      </section>

      <section className="mb-16">
        <h2 className="text-2xl font-bold text-white mb-6">Connecting to it</h2>
        <p className="text-slate-400 mb-4">
          Point any ordinary client at the host and port you were given:
        </p>
        <CodeBlock code={`psql -h net-proxy-3.portbuddy.dev -p 43452 -U postgres
ssh -p 43452 user@net-proxy-3.portbuddy.dev
mysql -h net-proxy-3.portbuddy.dev -P 43452 -u root -p`} />
        <p className="text-slate-400 mt-4">
          {'The service on the other end is unchanged, so its own authentication still applies — '}
          {'and now applies to the whole internet. Give a database you expose this way a real '}
          {'password, and prefer key-based authentication for SSH.'}
        </p>
      </section>

      <section className="mb-16">
        <h2 className="text-2xl font-bold text-white mb-6">Keeping the same address</h2>
        <p className="text-slate-400 mb-4">
          {'The public port comes from a reservation, taken from the range 40000–60000. You do not '}
          {'have to create one first: the first TCP tunnel for a local port allocates a reservation '}
          {'and later tunnels for that same local host and port reuse it, so the address you hand '}
          {'out generally survives a restart.'}
        </p>
        <p className="text-slate-400 mb-4">
          {'Manage them on the '}
          <Link to="/app/ports" className="text-indigo-400 hover:underline">Ports</Link>
          {' page, where a reservation can also be given a name. To pin a tunnel to a specific one, '}
          {'pass the name, the port, or the full host and port:'}
        </p>
        <CodeBlock code={`portbuddy tcp 5432 -pr staging-db
portbuddy tcp 5432 -pr 43452
portbuddy tcp 5432 -pr net-proxy-3.portbuddy.dev:43452`} />
        <p className="text-slate-400 mt-4">
          {'A reservation already in use by a running tunnel is refused rather than shared, so two '}
          {'services never end up behind one address.'}
        </p>
      </section>

      <section className="mb-16">
        <h2 className="text-2xl font-bold text-white mb-6">Going further</h2>
        <ul className="list-disc list-inside text-slate-400 space-y-2">
          <li>
            {'Keep a database or SSH tunnel up across reboots by installing it as a '}
            <Link to="/docs/run-as-a-service" className="text-indigo-400 hover:underline">background service</Link>.
          </li>
          <li>
            {'Hosting a game server on TCP? The '}
            <Link to="/docs/guides/minecraft-server" className="text-indigo-400 hover:underline">Minecraft guide</Link>
            {' walks through it end to end.'}
          </li>
          <li>
            {'For web services, an '}
            <Link to="/docs/http-tunnels" className="text-indigo-400 hover:underline">HTTP tunnel</Link>
            {' is the better fit: it terminates SSL, gives you a hostname rather than a port, and '}
            {'can be '}
            <Link to="/docs/private-tunnels" className="text-indigo-400 hover:underline">passcode-protected</Link>.
          </li>
        </ul>
      </section>
    </DocsPage>
  )
}
