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

const LEAD = 'To use Port Buddy you must be authenticated. This is what lets us manage your tunnels '
  + 'and respect your subscription limits.'

export default function Authentication() {
  return (
    <DocsPage path="/docs/authentication" lead={LEAD}>
      <section className="mb-16">
        <h2 className="text-2xl font-bold text-white mb-6">Link the CLI to your account</h2>
        <ol className="space-y-4 text-slate-400 list-decimal list-inside mb-6">
          <li>Log in to your account at <Link to="/login" className="text-indigo-400 hover:underline">portbuddy.dev</Link>.</li>
          <li>Go to the <Link to="/app/tokens" className="text-indigo-400 hover:underline">Tokens</Link> page and generate a new API token.</li>
          <li>Run the following command in your terminal:</li>
        </ol>
        <CodeBlock code="portbuddy init {YOUR_API_TOKEN}" />
        <p className="text-slate-400 mt-4">
          {'You only need to do this once per machine. Every later command — '}
          <Link to="/docs/http-tunnels" className="text-indigo-400 hover:underline">HTTP</Link>,{' '}
          <Link to="/docs/tcp-tunnels" className="text-indigo-400 hover:underline">TCP</Link> or{' '}
          <Link to="/docs/udp-tunnels" className="text-indigo-400 hover:underline">UDP</Link>
          {' — authenticates itself with the stored token.'}
        </p>
      </section>

      <section className="mb-16">
        <h2 className="text-2xl font-bold text-white mb-6">Where the token is stored</h2>
        <p className="text-slate-400 mb-4">
          {'The token is written to a file in your home directory. On macOS and Linux the CLI also '}
          {'restricts it to owner read/write, so other users on the machine cannot read it.'}
        </p>
        <CodeBlock code={`~/.port-buddy/token           # macOS, Linux
%USERPROFILE%\\.port-buddy\\token   # Windows`} />
        <p className="text-slate-400 mt-4">
          {'Running '}<code className="text-indigo-300">portbuddy init</code>
          {' again overwrites the file, which is how you switch accounts. Deleting the file logs '}
          {'this machine out.'}
        </p>
      </section>

      <section className="mb-16">
        <h2 className="text-2xl font-bold text-white mb-6">How a tunnel authenticates</h2>
        <p className="text-slate-400 mb-4">
          The API token is long-lived and never leaves your machine except to be exchanged. Each time
          you start a tunnel, the CLI trades it for a short-lived JWT and uses that for the tunnel
          connection itself, so the token is not attached to the traffic your tunnel carries.
        </p>
        <p className="text-slate-400">
          {'This exchange happens before the tunnel opens, which is why an expired or revoked token '}
          {'fails immediately rather than halfway through a session.'}
        </p>
      </section>

      <section className="mb-16">
        <h2 className="text-2xl font-bold text-white mb-6">Revoking access</h2>
        <p className="text-slate-400">
          {'Delete a token on the '}
          <Link to="/app/tokens" className="text-indigo-400 hover:underline">Tokens</Link>
          {' page to cut off every machine holding it. The next command on those machines stops '}
          {'with an authentication error and a reminder to re-run '}
          <code className="text-indigo-300">portbuddy init</code>
          {'. Issue one token per machine if you want to revoke them independently.'}
        </p>
      </section>

      <section className="mb-16">
        <h2 className="text-2xl font-bold text-white mb-6">Docker and CI</h2>
        <p className="text-slate-400 mb-4">
          {'There is no interactive login, so an automated environment only has to put the token '}
          {'file where the CLI looks for it. In the official image that path is '}
          <code className="text-indigo-300">/root/.port-buddy/token</code>
          {' — mount it from the host rather than baking it into an image layer.'}
        </p>
        <CodeBlock code={'docker run -v ~/.port-buddy/token:/root/.port-buddy/token:ro \\\n  amaktech/portbuddy:latest 3000'} />
        <p className="text-slate-400 mt-4">
          {'The same trick works in CI: write the token from a secret to '}
          <code className="text-indigo-300">~/.port-buddy/token</code>
          {' before the first tunnel command, or call '}
          <code className="text-indigo-300">portbuddy init</code>
          {' with the secret as its argument.'}
        </p>
      </section>

      <section className="mb-16">
        <h2 className="text-2xl font-bold text-white mb-6">Before you start</h2>
        <p className="text-slate-400">
          {'The CLI has to be on your machine first — see the '}
          <Link to="/install" className="text-indigo-400 hover:underline">installation guide</Link>
          {' for Homebrew, PowerShell and direct-download options. The server also requires a '}
          {'recent CLI and will refuse an old one with an upgrade notice, so update before '}
          {'debugging an authentication failure further. The full list of arguments accepted by '}
          {'the binary is in the '}
          <Link to="/docs/cli-reference" className="text-indigo-400 hover:underline">CLI reference</Link>.
        </p>
      </section>
    </DocsPage>
  )
}
