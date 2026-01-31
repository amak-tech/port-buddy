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

export default function MinecraftGuide() {
  return (
    <>
      <header className="mb-12">
        <div className="inline-flex items-center gap-2 text-indigo-400 font-medium mb-4">
          <BookOpenIcon className="w-5 h-5" />
          <span>How-to Guides</span>
        </div>
        <h1 className="text-4xl font-bold text-white mb-4">Hosting a Minecraft Server</h1>
        <p className="text-slate-400 text-lg leading-relaxed">
          Learn how to expose your local Minecraft server to the internet using Port Buddy, allowing your friends to join without port forwarding or configuring your router.
        </p>
      </header>

      <section className="mb-16">
        <h2 className="text-2xl font-bold text-white mb-6">Prerequisites</h2>
        <ul className="list-disc list-inside text-slate-400 space-y-2 mb-6">
          <li>A running Minecraft Server (Java or Bedrock Edition) on your local machine.</li>
          <li>Port Buddy CLI installed and authenticated.</li>
        </ul>
      </section>

      <section className="mb-16">
        <h2 className="text-2xl font-bold text-white mb-6">Java Edition</h2>
        <p className="text-slate-400 mb-4">
          Minecraft Java Edition uses TCP port <strong>25565</strong> by default.
        </p>
        
        <h3 className="text-lg font-semibold text-white mb-3">1. Start your Minecraft Server</h3>
        <p className="text-slate-400 mb-4">
          Ensure your server is running and accessible locally (usually at <code>localhost:25565</code>).
        </p>

        <h3 className="text-lg font-semibold text-white mb-3">2. Expose the port</h3>
        <p className="text-slate-400 mb-4">
          Run the following command in your terminal:
        </p>
        <div className="mb-4">
            <CodeBlock code="portbuddy tcp 25565" />
        </div>
        
        <p className="text-slate-400 mb-4">
          You will see output similar to this:
        </p>
        <div className="bg-slate-950 border border-slate-800 rounded-lg p-4 font-mono text-sm text-slate-300 mb-6">
          tcp localhost:25565 exposed to: net-proxy-1.portbuddy.dev:42123
        </div>

        <h3 className="text-lg font-semibold text-white mb-3">3. Connect</h3>
        <p className="text-slate-400 mb-6">
          Share the address (e.g., <code>net-proxy-1.portbuddy.dev:42123</code>) with your friends. 
          They can enter this address in the Multiplayer menu under "Direct Connection" or "Add Server".
        </p>
      </section>

      <section className="mb-16">
        <h2 className="text-2xl font-bold text-white mb-6">Bedrock Edition</h2>
        <p className="text-slate-400 mb-4">
          Minecraft Bedrock Edition uses UDP port <strong>19132</strong> by default.
        </p>
        
        <h3 className="text-lg font-semibold text-white mb-3">1. Start your Bedrock Server</h3>
        <p className="text-slate-400 mb-4">
          Ensure your server is running locally.
        </p>

        <h3 className="text-lg font-semibold text-white mb-3">2. Expose the port</h3>
        <p className="text-slate-400 mb-4">
            Run the following command:
        </p>
        <div className="mb-4">
            <CodeBlock code="portbuddy udp 19132" />
        </div>
        
        <h3 className="text-lg font-semibold text-white mb-3">3. Connect</h3>
        <p className="text-slate-400 mb-6">
          Share the generated address and port with your friends. They can add it to their server list.
        </p>
      </section>
    </>
  )
}
