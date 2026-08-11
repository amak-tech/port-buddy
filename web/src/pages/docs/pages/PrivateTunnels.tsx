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

const LEAD = 'A tunnel URL is public as soon as it exists. Protect one with a passcode and every '
  + 'visitor has to enter it before they reach your local service.'

export default function PrivateTunnels() {
  return (
    <DocsPage path="/docs/private-tunnels" lead={LEAD}>
      <section className="mb-16">
        <h2 className="text-2xl font-bold text-white mb-6">Protect a single tunnel</h2>
        <p className="text-slate-400 mb-6">
          {'Pass a passcode when you start the tunnel. It applies to that tunnel only and is '}
          {'discarded when the tunnel closes, which makes it a good fit for a one-off demo link.'}
        </p>
        <CodeBlock code="portbuddy --passcode my-secret 3000" />
        <p className="text-slate-400 mt-4">
          {'The short form is '}<code className="text-indigo-300">-pc</code>
          {'. Passcodes apply to '}
          <Link to="/docs/http-tunnels" className="text-indigo-400 hover:underline">HTTP tunnels</Link>
          {' only — '}
          <Link to="/docs/tcp-tunnels" className="text-indigo-400 hover:underline">TCP</Link>
          {' and '}
          <Link to="/docs/udp-tunnels" className="text-indigo-400 hover:underline">UDP</Link>
          {' tunnels carry no HTTP layer to challenge the visitor on.'}
        </p>
      </section>

      <section className="mb-16">
        <h2 className="text-2xl font-bold text-white mb-6">Protect a domain</h2>
        <p className="text-slate-400 mb-6">
          {'A passcode set on a domain persists across restarts and applies to every tunnel served '}
          {'from it. Set one in the '}
          <Link to="/app/domains" className="text-indigo-400 hover:underline">Domains</Link>
          {' section of the dashboard, and remove it there when the tunnel should go public. '}
          {'Domain passcodes must be at least four characters long.'}
        </p>
        <p className="text-slate-400">
          {'Domain passcodes work with your own '}
          <Link to="/docs/custom-domains" className="text-indigo-400 hover:underline">custom domains</Link>
          {' as well as generated subdomains.'}
        </p>
      </section>

      <section className="mb-16">
        <h2 className="text-2xl font-bold text-white mb-6">What visitors see</h2>
        <p className="text-slate-400 mb-6">
          A request to a protected tunnel is redirected to a passcode page naming the domain being
          opened. Once the visitor enters the correct passcode they are forwarded to the tunnel,
          and a cookie keeps them signed in to that tunnel for 12 hours.
        </p>
        <p className="text-slate-400 mb-4">
          Scripts and API clients can skip the page by sending the passcode themselves, either as a
          query parameter or as a request header:
        </p>
        <CodeBlock code={`curl "https://abc123.portbuddy.dev/?passcode=my-secret"
curl -H "X-API-Key: my-secret" https://abc123.portbuddy.dev/`} />
      </section>
    </DocsPage>
  )
}
