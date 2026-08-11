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
          This command exposes your local PostgreSQL database on port 5432.
          You will get an address like <code className="text-indigo-300">net-proxy-3.portbuddy.dev:43452</code>.
        </p>
        <p className="text-slate-400 mt-4">
          {'The public port is assigned when the tunnel opens. To keep the same port across '}
          {'restarts, reserve one and pass it with '}
          <code className="text-indigo-300">--port-reservation</code>
          {' — see the '}
          <Link to="/docs/cli-reference" className="text-indigo-400 hover:underline">CLI reference</Link>.
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
            {' is the better fit: it terminates SSL and gives you a hostname rather than a port.'}
          </li>
        </ul>
      </section>
    </DocsPage>
  )
}
