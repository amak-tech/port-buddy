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

const LEAD = 'With a Pro or Team plan, you can use your own domain name for your tunnels. '
  + 'We handle the SSL certificate issuance and renewal for you automatically.'

export default function CustomDomains() {
  return (
    <DocsPage path="/docs/custom-domains" lead={LEAD}>
      <section className="mb-16">
        <h2 className="text-2xl font-bold text-white mb-6">Setting one up</h2>
        <p className="text-slate-400 mb-6">
          {'A custom domain is an alias for one of your Port Buddy subdomains, so the subdomain '}
          {'comes first and your domain is pointed at it.'}
        </p>
        <ol className="space-y-4 text-slate-400 list-decimal list-inside mb-6">
          <li>
            {'Open the '}
            <Link to="/app/domains" className="text-indigo-400 hover:underline">Domains</Link>
            {' page and note the subdomain you want to use, for example '}
            <code className="text-indigo-300">otter-4821.portbuddy.dev</code>.
          </li>
          <li>
            {'Use its Custom Domain action to bind the hostname you own, for example '}
            <code className="text-indigo-300">app.mycompany.com</code>.
          </li>
          <li>At your DNS provider, create a CNAME record from your hostname to that subdomain.</li>
          <li>Come back and choose <strong className="text-slate-200">Verify CNAME &amp; Issue SSL</strong>.</li>
        </ol>
        <CodeBlock code={`Type    Name              Value
CNAME   app               otter-4821.portbuddy.dev`} />
        <p className="text-slate-400 mt-4">
          {'Most providers ask for just the label ('}<code className="text-indigo-300">app</code>
          {') rather than the full hostname, and append your zone for you.'}
        </p>
      </section>

      <section className="mb-16">
        <h2 className="text-2xl font-bold text-white mb-6">Verification and SSL</h2>
        <p className="text-slate-400 mb-4">
          Verification resolves your hostname's CNAME and compares it with the subdomain you bound.
          If they do not match, the error names both sides, which usually points straight at a typo
          or a record created in the wrong zone.
        </p>
        <p className="text-slate-400 mb-4">
          DNS changes are not instant. If verification fails right after you add the record, wait
          for your provider's TTL to pass and try again — you can check what the world currently
          sees with:
        </p>
        <CodeBlock code="dig +short CNAME app.mycompany.com" />
        <p className="text-slate-400 mt-4">
          Once the record checks out, a certificate is requested for your domain and renewed for you
          from then on. The dashboard shows the domain as verified while the certificate is being
          provisioned, then as active once it is in place.
        </p>
      </section>

      <section className="mb-16">
        <h2 className="text-2xl font-bold text-white mb-6">Things worth knowing</h2>
        <ul className="list-disc list-inside text-slate-400 space-y-2">
          <li>
            A CNAME cannot live at the apex of a zone, so use a subdomain such as
            <code className="text-indigo-300"> app.mycompany.com</code> rather than
            <code className="text-indigo-300"> mycompany.com</code> itself.
          </li>
          <li>
            If your DNS provider proxies traffic (an orange cloud, for instance), turn that off for
            this record — verification looks for a CNAME and a proxy replaces it with its own
            addresses.
          </li>
          <li>Binding a different hostname clears the verification and the certificate for the old one, so create the new CNAME and verify again.</li>
          <li>Custom domains apply to <Link to="/docs/http-tunnels" className="text-indigo-400 hover:underline">HTTP tunnels</Link>; TCP and UDP tunnels are reached by host and port instead.</li>
        </ul>
      </section>

      <section className="mb-16">
        <h2 className="text-2xl font-bold text-white mb-6">Related</h2>
        <ul className="list-disc list-inside text-slate-400 space-y-2">
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
