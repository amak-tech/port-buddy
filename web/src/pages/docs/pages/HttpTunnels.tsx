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
          This command will expose your local web server running on port 3000.
          You will receive a public URL like <code className="text-indigo-300">https://abc123.portbuddy.dev</code>.
        </p>
        <p className="text-slate-400 mt-4">
          {'Because HTTP is the default, the mode can be left out. Pass a mode explicitly when you '}
          {'need raw '}
          <Link to="/docs/tcp-tunnels" className="text-indigo-400 hover:underline">TCP</Link>
          {' or '}
          <Link to="/docs/udp-tunnels" className="text-indigo-400 hover:underline">UDP</Link>
          {' instead.'}
        </p>
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
            {'Request a fixed subdomain with the '}
            <code className="text-indigo-300">--domain</code>
            {' option, documented in the '}
            <Link to="/docs/cli-reference" className="text-indigo-400 hover:underline">CLI reference</Link>.
          </li>
        </ul>
      </section>
    </DocsPage>
  )
}
