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
        <h2 className="text-2xl font-bold text-white mb-6">Before you start</h2>
        <p className="text-slate-400">
          {'The CLI has to be on your machine first — see the '}
          <Link to="/install" className="text-indigo-400 hover:underline">installation guide</Link>
          {' for Homebrew, PowerShell and direct-download options. The full list of arguments '}
          {'accepted by the binary is in the '}
          <Link to="/docs/cli-reference" className="text-indigo-400 hover:underline">CLI reference</Link>.
        </p>
      </section>
    </DocsPage>
  )
}
