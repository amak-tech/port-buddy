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

import { BookOpenIcon } from '@heroicons/react/24/outline'
import CodeBlock from '../../../components/CodeBlock'

export default function HytaleGuide() {
  return (
    <>
      <header className="mb-12">
        <div className="inline-flex items-center gap-2 text-indigo-400 font-medium mb-4">
          <BookOpenIcon className="w-5 h-5" />
          <span>How-to Guides</span>
        </div>
        <h1 className="text-4xl font-bold text-white mb-4">Hosting a Hytale Server</h1>
        <p className="text-slate-400 text-lg leading-relaxed">
          Learn how to expose your local Hytale server to the internet using Port Buddy, allowing your friends to join your adventure without complex network configuration.
        </p>
      </header>

      <section className="mb-16">
        <h2 className="text-2xl font-bold text-white mb-6">Prerequisites</h2>
        <ul className="list-disc list-inside text-slate-400 space-y-2 mb-6">
          <li>A running Hytale Server on your local machine.</li>
          <li>Port Buddy CLI installed and authenticated.</li>
        </ul>
      </section>

      <section className="mb-16">
        <h2 className="text-2xl font-bold text-white mb-6">Exposing the Server</h2>
        <p className="text-slate-400 mb-4">
          Hytale servers use UDP port <strong>5520</strong> by default.
        </p>
        
        <h3 className="text-lg font-semibold text-white mb-3">1. Start your Hytale Server</h3>
        <p className="text-slate-400 mb-4">
          Launch your Hytale server and ensure it is running locally.
        </p>

        <h3 className="text-lg font-semibold text-white mb-3">2. Expose the port</h3>
        <p className="text-slate-400 mb-4">
          Run the following command in your terminal:
        </p>
        <div className="mb-4">
            <CodeBlock code="portbuddy udp 5520" />
        </div>
        
        <p className="text-slate-400 mb-4">
          You will see output similar to this:
        </p>
        <div className="bg-slate-950 border border-slate-800 rounded-lg p-4 font-mono text-sm text-slate-300 mb-6">
          udp localhost:5520 exposed to: net-proxy-2.portbuddy.dev:54321
        </div>

        <h3 className="text-lg font-semibold text-white mb-3">3. Connect</h3>
        <p className="text-slate-400 mb-6">
          Share the address (e.g., <code>net-proxy-2.portbuddy.dev:54321</code>) with your friends. 
          They can use this address to connect to your server from the game client.
        </p>
      </section>
    </>
  )
}
