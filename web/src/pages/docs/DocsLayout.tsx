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
import { DOCS_GROUPS, navEntriesInGroup } from '../../config/docs'

export default function DocsLayout() {
  const location = useLocation()
  const current = location.pathname.replace(/\/+$/, '') || '/'

  return (
    <div className="min-h-screen flex flex-col">
      <div className="flex-1 relative pt-12 md:pt-20 pb-20">
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-indigo-900/20 via-slate-900/0 to-slate-900/0 pointer-events-none" />
        
        <div className="container max-w-5xl relative z-10">
          <div className="flex flex-col md:flex-row gap-12">
            {/* Sidebar Navigation */}
            <aside className="md:w-64 flex-shrink-0">
              <nav className="sticky top-24 space-y-8" aria-label="Documentation">
                {DOCS_GROUPS.map((group) => (
                  <div key={group.id}>
                    <h2 className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-4 px-2">{group.label}</h2>
                    <ul className="space-y-1">
                      {navEntriesInGroup(group.id).map((entry) => (
                        <li key={entry.path}>
                          <SidebarLink to={entry.path} label={entry.label} active={current === entry.path} />
                        </li>
                      ))}
                    </ul>
                  </div>
                ))}
              </nav>
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
  return (
    <Link
      to={to}
      aria-current={active ? 'page' : undefined}
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
