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

export default function Contacts() {
  return (
    <div className="min-h-screen flex flex-col">
      <div className="flex-1 relative pt-12 md:pt-32 pb-20">
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-indigo-900/10 via-slate-900/0 to-slate-900/0 pointer-events-none" />
        
        <div className="container max-w-3xl relative z-10">
          <h1 className="text-4xl font-bold text-white mb-8">Contact Us</h1>
          
          <div className="prose prose-invert max-w-none text-slate-300 space-y-12">
            <p className="text-lg">
              Have questions, need assistance, or want to report an issue? We're here to help. 
              Reach out to us via the appropriate email address below.
            </p>

            <div className="grid gap-8">
              <section className="bg-slate-900/50 border border-slate-800 rounded-2xl p-6 hover:border-indigo-500/50 transition-colors">
                <h2 className="text-2xl font-semibold text-white mb-3 flex items-center gap-2">
                  Support
                </h2>
                <p className="mb-4">
                  For any technical questions, issues with your account, subscription inquiries, or general help using Port Buddy.
                </p>
                <a 
                  href="mailto:support@portbuddy.dev" 
                  className="text-indigo-400 hover:text-indigo-300 font-medium text-lg"
                >
                  support@portbuddy.dev
                </a>
              </section>

              <section className="bg-slate-900/50 border border-slate-800 rounded-2xl p-6 hover:border-red-500/50 transition-colors">
                <h2 className="text-2xl font-semibold text-white mb-3 flex items-center gap-2">
                  Abuse
                </h2>
                <p className="mb-4">
                  To report any misuse of our service, phishing attempts, or content that violates our terms of service.
                </p>
                <a 
                  href="mailto:abuse@portbuddy.dev" 
                  className="text-indigo-400 hover:text-indigo-300 font-medium text-lg"
                >
                  abuse@portbuddy.dev
                </a>
              </section>
            </div>

            <section>
              <h2 className="text-2xl font-semibold text-white mb-4">Other Ways to Connect</h2>
              <p>
                You can also find us on our community channels:
              </p>
              <ul className="list-disc pl-6 space-y-2">
                <li>
                  <a href="https://discord.gg/RCT82A6T" target="_blank" rel="noopener noreferrer" className="text-indigo-400 hover:underline">Discord</a> - Join our community for real-time help and discussions.
                </li>
                <li>
                  <a href="https://t.me/portbuddy" target="_blank" rel="noopener noreferrer" className="text-indigo-400 hover:underline">Telegram</a> - Follow us for updates and news.
                </li>
                <li>
                  <a href="https://github.com/amak-tech/port-buddy" target="_blank" rel="noopener noreferrer" className="text-indigo-400 hover:underline">GitHub</a> - Report bugs or contribute to the project.
                </li>
              </ul>
            </section>
          </div>
        </div>
      </div>
    </div>
  )
}
