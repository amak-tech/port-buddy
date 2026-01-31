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

import { Link, Outlet, useLocation } from 'react-router-dom'

export default function DocsLayout() {
  const location = useLocation()

  return (
    <div className="min-h-screen flex flex-col">
      <div className="flex-1 relative pt-12 md:pt-20 pb-20">
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-indigo-900/20 via-slate-900/0 to-slate-900/0 pointer-events-none" />
        
        <div className="container max-w-5xl relative z-10">
          <div className="flex flex-col md:flex-row gap-12">
            {/* Sidebar Navigation */}
            <aside className="md:w-64 flex-shrink-0">
              <div className="sticky top-24 space-y-8">
                <div>
                  <h3 className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-4 px-2">Getting Started</h3>
                  <nav className="space-y-1">
                    <SidebarLink to="/docs#introduction" label="Introduction" active={location.pathname === '/docs' && (!location.hash || location.hash === '#introduction')} />
                    <SidebarLink to="/docs#installation" label="Installation" active={location.hash === '#installation'} />
                    <SidebarLink to="/docs#authentication" label="Authentication" active={location.hash === '#authentication'} />
                  </nav>
                </div>
                <div>
                  <h3 className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-4 px-2">Tunnels</h3>
                  <nav className="space-y-1">
                    <SidebarLink to="/docs#http-tunnels" label="HTTP Tunnels" active={location.hash === '#http-tunnels'} />
                    <SidebarLink to="/docs#tcp-tunnels" label="TCP Tunnels" active={location.hash === '#tcp-tunnels'} />
                    <SidebarLink to="/docs#udp-tunnels" label="UDP Tunnels" active={location.hash === '#udp-tunnels'} />
                  </nav>
                </div>
                <div>
                  <h3 className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-4 px-2">Advanced</h3>
                  <nav className="space-y-1">
                    <SidebarLink to="/docs#custom-domains" label="Custom Domains" active={location.hash === '#custom-domains'} />
                    <SidebarLink to="/docs#private-tunnels" label="Private Tunnels" active={location.hash === '#private-tunnels'} />
                    <SidebarLink to="/docs#pricing-limits" label="Pricing & Limits" active={location.hash === '#pricing-limits'} />
                  </nav>
                </div>
                <div>
                  <h3 className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-4 px-2">How-to Guides</h3>
                  <nav className="space-y-1">
                    <SidebarLink to="/docs/guides/minecraft-server" label="Minecraft Server" active={location.pathname === '/docs/guides/minecraft-server'} />
                  </nav>
                </div>
              </div>
            </aside>

            {/* Main Content */}
            <main className="flex-1 max-w-3xl">
              <Outlet />
            </main>
          </div>
        </div>
      </div>
    </div>
  )
}

function SidebarLink({ to, label, active }: { to: string, label: string, active: boolean }) {
  // If we are on a different page and the link is an anchor on /docs, we need to make sure we go to /docs first.
  // The 'to' prop should be the full path (e.g., "/docs#introduction").
  // However, ScrollToHash in App.tsx handles the scrolling.
  
  return (
    <Link 
      to={to} 
      className={`block px-2 py-1.5 text-sm rounded-lg transition-colors ${
        active 
          ? 'text-white bg-slate-800' 
          : 'text-slate-400 hover:text-white hover:bg-slate-800'
      }`}
    >
      {label}
    </Link>
  )
}
