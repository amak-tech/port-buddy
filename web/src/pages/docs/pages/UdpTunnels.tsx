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

const LEAD = 'UDP mode is useful for game servers, VoIP, and other UDP-based protocols.'

/** Well-known UDP ports people most often expose, as a starting point for the command. */
const COMMON_PORTS: readonly { service: string, port: string, command: string }[] = [
  { service: 'Minecraft (Bedrock)', port: '19132', command: 'portbuddy udp 19132' },
  { service: 'Valheim', port: '2456', command: 'portbuddy udp 2456' },
  { service: 'Source engine games', port: '27015', command: 'portbuddy udp 27015' },
  { service: 'SIP / VoIP', port: '5060', command: 'portbuddy udp 5060' }
]

export default function UdpTunnels() {
  return (
    <DocsPage path="/docs/udp-tunnels" lead={LEAD}>
      <section className="mb-16">
        <h2 className="text-2xl font-bold text-white mb-6">Usage</h2>
        <CodeBlock code="portbuddy udp 19132" />
        <p className="text-slate-400 mt-4">
          {'You get back a public host and port, such as '}
          <code className="text-indigo-300">net-proxy-2.portbuddy.dev:41207</code>
          {', and datagrams sent there are forwarded to your local socket. Unlike '}
          <Link to="/docs/tcp-tunnels" className="text-indigo-400 hover:underline">TCP tunnels</Link>
          {', UDP is available on every plan.'}
        </p>
        <p className="text-slate-400 mt-4">
          {'There is no TLS and no hostname routing here — UDP carries nothing like a Host header '}
          {'to route on, so a tunnel is identified purely by its port. Whatever encryption or '}
          {'authentication your protocol has is what protects it.'}
        </p>
      </section>

      <section className="mb-16">
        <h2 className="text-2xl font-bold text-white mb-6">Common ports</h2>
        <div className="overflow-x-auto">
          <table className="w-full text-sm text-left border-collapse">
            <caption className="sr-only">Common UDP services, their default ports, and the command to expose them</caption>
            <thead>
              <tr className="border-b border-slate-800">
                <th scope="col" className="py-3 pr-4 font-semibold text-white">Service</th>
                <th scope="col" className="py-3 pr-4 font-semibold text-white">Default port</th>
                <th scope="col" className="py-3 font-semibold text-white">Command</th>
              </tr>
            </thead>
            <tbody>
              {COMMON_PORTS.map((entry) => (
                <tr key={entry.service} className="border-b border-slate-800/60">
                  <th scope="row" className="py-3 pr-4 text-slate-300 font-normal">{entry.service}</th>
                  <td className="py-3 pr-4 text-slate-400">{entry.port}</td>
                  <td className="py-3 font-mono text-indigo-300">{entry.command}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <p className="text-slate-400 mt-4">
          Check your server's own configuration if you have changed its port — the tunnel exposes
          whatever local port you name, not a fixed one.
        </p>
      </section>

      <section className="mb-16">
        <h2 className="text-2xl font-bold text-white mb-6">Keeping the same address</h2>
        <p className="text-slate-400 mb-4">
          {'As with TCP, the public port comes from a reservation in the range 40000–60000. One is '}
          {'allocated automatically the first time and reused for the same local port afterwards, '}
          {'so the address you gave your players tends to keep working. Pin one explicitly by name, '}
          {'port, or host and port:'}
        </p>
        <CodeBlock code="portbuddy udp 19132 -pr my-game-server" />
        <p className="text-slate-400 mt-4">
          {'Reservations are managed on the '}
          <Link to="/app/ports" className="text-indigo-400 hover:underline">Ports</Link>
          {' page.'}
        </p>
      </section>

      <section className="mb-16">
        <h2 className="text-2xl font-bold text-white mb-6">Testing and idle tunnels</h2>
        <p className="text-slate-400 mb-4">
          UDP gives you no connection to watch, so the quickest check that traffic is flowing is to
          send a datagram yourself and look at your server's logs:
        </p>
        <CodeBlock code="echo ping | nc -u net-proxy-2.portbuddy.dev 41207" />
        <p className="text-slate-400 mt-4">
          A UDP tunnel that sees no traffic at all for five minutes is cleaned up, since there is no
          connection close to detect. A busy game server never notices; a tunnel you opened and
          forgot will quietly go away.
        </p>
      </section>

      <section className="mb-16">
        <h2 className="text-2xl font-bold text-white mb-6">Going further</h2>
        <ul className="list-disc list-inside text-slate-400 space-y-2">
          <li>
            {'Minecraft Bedrock Edition runs on UDP 19132 — the '}
            <Link to="/docs/guides/minecraft-server" className="text-indigo-400 hover:underline">Minecraft guide</Link>
            {' covers both editions.'}
          </li>
          <li>
            {'The '}
            <Link to="/docs/guides/hytale-server" className="text-indigo-400 hover:underline">Hytale guide</Link>
            {' is a second worked example of exposing a game server.'}
          </li>
          <li>
            {'A game server should outlive your terminal session — install it as a '}
            <Link to="/docs/run-as-a-service" className="text-indigo-400 hover:underline">background service</Link>.
          </li>
        </ul>
      </section>
    </DocsPage>
  )
}
