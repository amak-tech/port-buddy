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

export default function UdpTunnels() {
  return (
    <DocsPage path="/docs/udp-tunnels" lead={LEAD}>
      <section className="mb-16">
        <h2 className="text-2xl font-bold text-white mb-6">Usage</h2>
        <CodeBlock code="portbuddy udp 19132" />
        <p className="text-slate-400 mt-4">
          {'UDP tunnels are available on every plan, unlike '}
          <Link to="/docs/tcp-tunnels" className="text-indigo-400 hover:underline">TCP tunnels</Link>
          {'. As with TCP, you get a public host and port rather than a URL, and a reserved port '}
          {'keeps that address stable across restarts.'}
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
