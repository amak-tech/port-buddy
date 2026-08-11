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

const LEAD = 'Every argument the portbuddy binary accepts, with the modes each option applies to.'

type Option = {
  flags: string
  argument?: string
  applies: string
  description: string
}

/** Mirrors `portbuddy --help`; keep in step with the CLI's argument parsing. */
const OPTIONS: readonly Option[] = [
  {
    flags: '-d, --domain',
    argument: '<domain>',
    applies: 'HTTP',
    description: 'Request a specific subdomain, e.g. my-app, instead of a generated one.'
  },
  {
    flags: '-pr, --port-reservation',
    argument: '<host:port>',
    applies: 'TCP, UDP',
    description: 'Use a port reservation you own, so the public address stays the same across restarts.'
  },
  {
    flags: '-pc, --passcode',
    argument: '<passcode>',
    applies: 'HTTP',
    description: 'Protect the tunnel with a passcode for as long as it is open.'
  },
  {
    flags: '-n, --no-request-log',
    applies: 'All',
    description: 'Disable the live request log in the terminal.'
  },
  {
    flags: '-v, --verbose',
    applies: 'All',
    description: 'Verbose logging, useful when a tunnel will not connect.'
  },
  {
    flags: '-h, --help',
    applies: 'All',
    description: 'Print the usage message and exit.'
  },
  {
    flags: '-V, --version',
    applies: 'All',
    description: 'Print the CLI version and exit.'
  }
]

export default function CliReference() {
  return (
    <DocsPage path="/docs/cli-reference" lead={LEAD}>
      <section className="mb-16">
        <h2 className="text-2xl font-bold text-white mb-6">Synopsis</h2>
        <CodeBlock code="portbuddy [options] [mode] [host:][port]" />
        <p className="text-slate-400 mt-4">
          {'Options may appear before or after the positional arguments. Long options also accept '}
          {'an equals sign, as in '}<code className="text-indigo-300">--domain=my-app</code>.
        </p>
      </section>

      <section className="mb-16">
        <h2 className="text-2xl font-bold text-white mb-6">Modes</h2>
        <p className="text-slate-400 mb-6">
          {'The mode is optional and defaults to '}<code className="text-indigo-300">http</code>
          {', so '}<code className="text-indigo-300">portbuddy 3000</code>
          {' and '}<code className="text-indigo-300">portbuddy http 3000</code>
          {' are the same command.'}
        </p>
        <ul className="list-disc list-inside text-slate-400 space-y-2">
          <li>
            <code className="text-indigo-300">http</code>
            {' — a public HTTPS URL for a local web app. See '}
            <Link to="/docs/http-tunnels" className="text-indigo-400 hover:underline">HTTP tunnels</Link>.
          </li>
          <li>
            <code className="text-indigo-300">tcp</code>
            {' — a public host and port for a raw TCP service. See '}
            <Link to="/docs/tcp-tunnels" className="text-indigo-400 hover:underline">TCP tunnels</Link>.
          </li>
          <li>
            <code className="text-indigo-300">udp</code>
            {' — the same, for UDP. See '}
            <Link to="/docs/udp-tunnels" className="text-indigo-400 hover:underline">UDP tunnels</Link>.
          </li>
        </ul>
      </section>

      <section className="mb-16">
        <h2 className="text-2xl font-bold text-white mb-6">Target</h2>
        <p className="text-slate-400 mb-6">
          The target says what to expose. A bare port is the common case; the host defaults to
          <code className="text-indigo-300"> localhost</code> and the scheme to
          <code className="text-indigo-300"> http</code>.
        </p>
        <CodeBlock code={`portbuddy 3000                    # localhost:3000 over http
portbuddy 192.168.1.10:8080       # another machine on your network
portbuddy https://localhost:8443  # local server that already speaks TLS`} />
        <p className="text-slate-400 mt-4">
          Only <code className="text-indigo-300">http</code> and <code className="text-indigo-300">https</code> schemes
          are accepted, and the port must be in the range 1–65535.
        </p>
      </section>

      <section className="mb-16">
        <h2 className="text-2xl font-bold text-white mb-6">Options</h2>
        <div className="overflow-x-auto">
          <table className="w-full text-sm text-left border-collapse">
            <caption className="sr-only">Port Buddy CLI options, the modes they apply to, and what they do</caption>
            <thead>
              <tr className="border-b border-slate-800">
                <th scope="col" className="py-3 pr-4 font-semibold text-white">Option</th>
                <th scope="col" className="py-3 pr-4 font-semibold text-white">Modes</th>
                <th scope="col" className="py-3 font-semibold text-white">Description</th>
              </tr>
            </thead>
            <tbody>
              {OPTIONS.map((option) => (
                <tr key={option.flags} className="border-b border-slate-800/60 align-top">
                  <th scope="row" className="py-3 pr-4 font-mono text-indigo-300 font-normal whitespace-nowrap">
                    {option.argument ? `${option.flags} ${option.argument}` : option.flags}
                  </th>
                  <td className="py-3 pr-4 text-slate-400 whitespace-nowrap">{option.applies}</td>
                  <td className="py-3 text-slate-400">{option.description}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <p className="text-slate-400 mt-4">
          {'An option that does not apply to the mode you chose is ignored: '}
          <code className="text-indigo-300">--domain</code>
          {' and '}<code className="text-indigo-300">--passcode</code>
          {' are only sent for HTTP tunnels, and '}
          <code className="text-indigo-300">--port-reservation</code>
          {' only for TCP and UDP.'}
        </p>
      </section>

      <section className="mb-16">
        <h2 className="text-2xl font-bold text-white mb-6">Commands</h2>
        <p className="text-slate-400 mb-4">
          {'There is one subcommand. It stores an API token on this machine so later tunnel '}
          {'commands authenticate themselves — see '}
          <Link to="/docs/authentication" className="text-indigo-400 hover:underline">authentication</Link>.
        </p>
        <CodeBlock code="portbuddy init <apiToken>" />
      </section>

      <section className="mb-16">
        <h2 className="text-2xl font-bold text-white mb-6">Exit codes</h2>
        <ul className="list-disc list-inside text-slate-400 space-y-2">
          <li><code className="text-indigo-300">0</code> — the command completed, or the tunnel was closed normally.</li>
          <li><code className="text-indigo-300">1</code> — the tunnel could not be established: authentication, quota or network failure.</li>
          <li><code className="text-indigo-300">2</code> — the arguments were wrong; the usage message is printed.</li>
        </ul>
      </section>

      <section className="mb-16">
        <h2 className="text-2xl font-bold text-white mb-6">Examples</h2>
        <CodeBlock code={`portbuddy init YOUR_API_TOKEN            # once per machine
portbuddy 3000                           # expose a local web app
portbuddy --domain=my-app 8080           # ask for a fixed subdomain
portbuddy -pc my-secret 3000             # require a passcode
portbuddy tcp 5432                       # expose PostgreSQL
portbuddy udp 19132                      # expose a game server`} />
      </section>
    </DocsPage>
  )
}
