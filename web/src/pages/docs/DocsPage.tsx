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

import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { ChevronRightIcon, ArrowLeftIcon, ArrowRightIcon } from '@heroicons/react/24/outline'
import JsonLd from '../../lib/seo/JsonLd'
import {
  breadcrumbListSchema,
  techArticleSchema,
  type BreadcrumbEntry,
  type JsonLdObject
} from '../../lib/seo/schemas'
import { docsNeighbours, docsPage, type DocsPage as DocsPageDef } from '../../config/docs'

/**
 * The frame every documentation page shares: breadcrumbs, the single <h1>, the lead paragraph, and
 * the prev/next pair at the foot.
 *
 * The visible breadcrumb trail and the BreadcrumbList markup are built from the same array, and the
 * TechArticle description is the very lead paragraph rendered above — neither can describe
 * something the page does not show.
 */

export type DocsPageProps = {
  /** Path of this page in the docs manifest. */
  path: string
  /** Lead paragraph, rendered under the heading and reused as the TechArticle description. */
  lead: ReactNode
  /** Plain-text form of `lead`, for the structured data. Required when `lead` is not a string. */
  leadText?: string
  children: ReactNode
}

function trailFor(page: DocsPageDef) {
  return [
    { name: 'Home', path: '/' },
    { name: 'Documentation', path: '/docs' },
    ...(page.path === '/docs' ? [] : [{ name: page.heading, path: page.path }])
  ]
}

function schemasFor(page: DocsPageDef, leadText: string): JsonLdObject[] {
  const schemas: JsonLdObject[] = [breadcrumbListSchema(trailFor(page))]
  if (page.techArticle) {
    schemas.push(techArticleSchema({
      headline: page.heading,
      description: leadText,
      path: page.path
    }))
  }
  return schemas
}

/**
 * The visible breadcrumb trail. Rendered from the same array as the BreadcrumbList markup, so the
 * two always agree — including on the guides, which build their trail themselves.
 */
export function DocsBreadcrumbs({ trail }: { trail: readonly BreadcrumbEntry[] }) {
  return (
    <nav aria-label="Breadcrumb" className="mb-6">
      <ol className="flex flex-wrap items-center gap-1.5 text-sm text-slate-500">
        {trail.map((entry, index) => {
          const last = index === trail.length - 1
          return (
            <li key={entry.path} className="flex items-center gap-1.5">
              {index > 0 && <ChevronRightIcon className="w-3.5 h-3.5 text-slate-600" aria-hidden="true" />}
              {last
                ? <span className="text-slate-300" aria-current="page">{entry.name}</span>
                : <Link to={entry.path} className="hover:text-indigo-400 transition-colors">{entry.name}</Link>}
            </li>
          )
        })}
      </ol>
    </nav>
  )
}

function PrevNext({ path }: { path: string }) {
  const { previous, next } = docsNeighbours(path)
  if (!previous && !next) {
    return null
  }

  return (
    <nav aria-label="Documentation pages" className="border-t border-slate-800 mt-16 pt-8 flex flex-col sm:flex-row gap-4 justify-between">
      {previous
        ? (
          <Link
            to={previous.path}
            className="group flex-1 rounded-xl border border-slate-800 bg-slate-900/50 p-4 hover:border-indigo-500/40 transition-colors"
            rel="prev"
          >
            <span className="flex items-center gap-1.5 text-xs uppercase tracking-wider text-slate-500 mb-1">
              <ArrowLeftIcon className="w-3.5 h-3.5" aria-hidden="true" />
              Previous
            </span>
            <span className="text-slate-200 group-hover:text-white font-medium">{previous.label}</span>
          </Link>
        )
        : <div className="flex-1" />}
      {next
        ? (
          <Link
            to={next.path}
            className="group flex-1 rounded-xl border border-slate-800 bg-slate-900/50 p-4 hover:border-indigo-500/40 transition-colors sm:text-right"
            rel="next"
          >
            <span className="flex items-center gap-1.5 text-xs uppercase tracking-wider text-slate-500 mb-1 sm:justify-end">
              Next
              <ArrowRightIcon className="w-3.5 h-3.5" aria-hidden="true" />
            </span>
            <span className="text-slate-200 group-hover:text-white font-medium">{next.label}</span>
          </Link>
        )
        : <div className="flex-1" />}
    </nav>
  )
}

export default function DocsPage({ path, lead, leadText, children }: DocsPageProps) {
  const page = docsPage(path)
  if (!page) {
    throw new Error(`No docs manifest entry for ${path}`)
  }

  const description = leadText ?? (typeof lead === 'string' ? lead : '')

  return (
    <>
      <JsonLd data={schemasFor(page, description)} />
      <DocsBreadcrumbs trail={trailFor(page)} />

      <header className="mb-12">
        <h1 className="text-4xl font-bold text-white mb-4">{page.heading}</h1>
        <p className="text-slate-400 text-lg leading-relaxed">{lead}</p>
      </header>

      {children}

      <PrevNext path={path} />
    </>
  )
}
