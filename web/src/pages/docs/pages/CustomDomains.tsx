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
import DocsPage from '../DocsPage'

const LEAD = 'With a Pro or Team plan, you can use your own domain name for your tunnels. '
  + 'We handle the SSL certificate issuance and renewal for you automatically.'

export default function CustomDomains() {
  return (
    <DocsPage path="/docs/custom-domains" lead={LEAD}>
      <section className="mb-16">
        <h2 className="text-2xl font-bold text-white mb-6">Adding a domain</h2>
        <p className="text-slate-400">
          Configure your custom domains in the <Link to="/app/domains" className="text-indigo-400 hover:underline">Domains</Link> section of the dashboard.
        </p>
      </section>

      <section className="mb-16">
        <h2 className="text-2xl font-bold text-white mb-6">Related</h2>
        <ul className="list-disc list-inside text-slate-400 space-y-2">
          <li>
            {'Custom domains apply to '}
            <Link to="/docs/http-tunnels" className="text-indigo-400 hover:underline">HTTP tunnels</Link>
            {'. TCP and UDP tunnels are reached by host and port instead.'}
          </li>
          <li>
            {'A domain can also carry a passcode, so every visitor is challenged before the tunnel '}
            {'is reachable — see '}
            <Link to="/docs/private-tunnels" className="text-indigo-400 hover:underline">private tunnels</Link>.
          </li>
          <li>
            {'To request a fixed portbuddy.dev subdomain instead of your own domain, use the '}
            <code className="text-indigo-300">--domain</code>
            {' option from the '}
            <Link to="/docs/cli-reference" className="text-indigo-400 hover:underline">CLI reference</Link>.
          </li>
        </ul>
      </section>
    </DocsPage>
  )
}
