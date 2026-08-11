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

import { useEffect } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import {
  GlobeAltIcon,
  ShieldCheckIcon,
  BoltIcon,
  LockClosedIcon,
  ArrowRightIcon,
  InformationCircleIcon
} from '@heroicons/react/24/outline'
import DocsPage from './DocsPage'
import {
  ANCHOR_REDIRECT_SOURCE,
  DOCS_GROUPS,
  anchorRedirect,
  navEntriesInGroup,
  type DocsNavEntry
} from '../../config/docs'

const LEAD = 'Port Buddy is a tool that allows you to share a port opened on your local host or '
  + 'private network to the public network. It’s perfect for testing webhooks, sharing your work '
  + 'with clients, or exposing any local service securely.'

/**
 * Sends the anchors of the old single-page docs to the pages that replaced them.
 *
 * The prerendered /docs page carries an inline copy of this same map that runs before first paint,
 * so a visitor arriving from an old link never sees the index. This hook is what covers the cases
 * that script cannot: the dev server, and in-app navigation to one of the old anchor links.
 *
 * It only ever runs on /docs, so it cannot bounce a visitor who is already on a destination page,
 * and an unrecognised hash is left alone.
 */
function useAnchorRedirects() {
  const { pathname, hash } = useLocation()
  const navigate = useNavigate()

  useEffect(() => {
    if (pathname !== ANCHOR_REDIRECT_SOURCE || !hash) {
      return
    }
    const target = anchorRedirect(hash)
    if (target && target !== pathname) {
      navigate(target, { replace: true })
    }
  }, [pathname, hash, navigate])
}

export default function DocsOverview() {
  useAnchorRedirects()

  return (
    <DocsPage path="/docs" lead={LEAD}>
      <section className="mb-16">
        <h2 className="text-2xl font-bold text-white mb-6">How it works</h2>
        <p className="text-slate-400 mb-6">
          Port Buddy works as a reverse proxy. When you run the CLI, it establishes a secure connection to our edge servers.
          Any traffic sent to your unique Port Buddy URL is then proxied through this connection to your local machine.
        </p>
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-6">
          <h3 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
            <InformationCircleIcon className="w-5 h-5 text-indigo-400" />
            Key Features
          </h3>
          <ul className="grid sm:grid-cols-2 gap-4">
            <FeatureItem icon={<GlobeAltIcon className="w-5 h-5" />} label="HTTP/HTTPS Tunnels" />
            <FeatureItem icon={<ShieldCheckIcon className="w-5 h-5" />} label="TCP & UDP Support" />
            <FeatureItem icon={<BoltIcon className="w-5 h-5" />} label="WebSocket Support" />
            <FeatureItem icon={<LockClosedIcon className="w-5 h-5" />} label="Custom Domains" />
          </ul>
        </div>
        <p className="text-slate-400 mt-6">
          {'How many tunnels you can run at once, and which modes your account unlocks, depends on '}
          {'your plan — the '}
          <a href="/#pricing" className="text-indigo-400 hover:underline">pricing section</a>
          {' on the homepage has the current plans and limits.'}
        </p>
      </section>

      {DOCS_GROUPS.map((group) => {
        const entries = navEntriesInGroup(group.id).filter((entry) => entry.path !== '/docs')
        if (entries.length === 0) {
          return null
        }
        return (
          <section key={group.id} className="mb-12">
            <h2 className="text-2xl font-bold text-white mb-6">{group.label}</h2>
            <div className="grid sm:grid-cols-2 gap-4">
              {entries.map((entry) => <DocsCard key={entry.path} entry={entry} />)}
            </div>
          </section>
        )
      })}
    </DocsPage>
  )
}

function DocsCard({ entry }: { entry: DocsNavEntry }) {
  return (
    <Link
      to={entry.path}
      className="group block rounded-xl border border-slate-800 bg-slate-900/50 p-5 hover:border-indigo-500/40 transition-colors"
    >
      <span className="flex items-center gap-1.5 text-white font-semibold mb-2">
        {entry.label}
        <ArrowRightIcon className="w-4 h-4 text-slate-600 group-hover:text-indigo-400 group-hover:translate-x-0.5 transition-all" aria-hidden="true" />
      </span>
      <span className="block text-sm text-slate-400">{entry.summary}</span>
    </Link>
  )
}

function FeatureItem({ icon, label }: { icon: React.ReactNode, label: string }) {
  return (
    <li className="flex items-center gap-3 text-sm text-slate-300">
      <div className="text-indigo-400">{icon}</div>
      {label}
    </li>
  )
}
