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

import { Fragment } from 'react'
import { BookOpenIcon } from '@heroicons/react/24/outline'
import CodeBlock from '../../../components/CodeBlock'
import JsonLd from '../../../lib/seo/JsonLd'
import { breadcrumbListSchema, howToSchema } from '../../../lib/seo/schemas'
import { DocsBreadcrumbs } from '../DocsPage'

/**
 * Renders a how-to guide from data and derives its HowTo structured data from the very same steps,
 * so the markup cannot describe a step the page does not render.
 *
 * Step text uses two inline markers: `code` and **bold**. Both are stripped for the JSON-LD, which
 * makes the schema text identical to the text content of the rendered paragraph.
 */

export type GuideStep = {
  /** Step heading, rendered with its position prefixed ("1. …"). */
  name: string
  text: string
  /** Command rendered in a code block below the step text. */
  command?: string
  /** Optional "you will see output like this" pair. */
  resultText?: string
  resultOutput?: string
}

export type GuideSection = {
  name: string
  intro?: string
  steps: readonly GuideStep[]
}

export type GuideDef = {
  /** Route of the guide, used for the canonical URLs inside the structured data. */
  path: string
  /** The h1 — also the HowTo name and the last breadcrumb. */
  title: string
  /** Lead paragraph — also the HowTo description. */
  intro: string
  prerequisites: readonly string[]
  sections: readonly GuideSection[]
}

const INLINE_MARKER = /(`[^`]+`|\*\*[^*]+\*\*)/

/** Drops the inline markers, yielding exactly what the browser renders as text. */
export function plainText(markup: string): string {
  return markup.replace(/`([^`]+)`/g, '$1').replace(/\*\*([^*]+)\*\*/g, '$1')
}

function RichText({ markup }: { markup: string }) {
  return (
    <>
      {/* Empty fragments are dropped: an empty text node survives client rendering but not the
          serialize/reparse round trip of prerendering, which would break hydration. */}
      {markup.split(INLINE_MARKER).filter((part) => part !== '').map((part, index) => {
        if (part.startsWith('`') && part.endsWith('`') && part.length > 1) {
          return <code key={index}>{part.slice(1, -1)}</code>
        }
        if (part.startsWith('**') && part.endsWith('**') && part.length > 3) {
          return <strong key={index}>{part.slice(2, -2)}</strong>
        }
        return <Fragment key={index}>{part}</Fragment>
      })}
    </>
  )
}

function guideTrail(guide: GuideDef) {
  return [
    { name: 'Home', path: '/' },
    { name: 'Documentation', path: '/docs' },
    { name: guide.title, path: guide.path }
  ]
}

function guideSchemas(guide: GuideDef) {
  return [
    howToSchema({
      name: guide.title,
      description: plainText(guide.intro),
      path: guide.path,
      sections: guide.sections.map((section) => ({
        name: section.name,
        steps: section.steps.map((step) => ({
          name: step.name,
          text: plainText(step.text),
          ...(step.command ? { directions: [step.command] } : {})
        }))
      }))
    }),
    breadcrumbListSchema(guideTrail(guide))
  ]
}

export default function GuideArticle({ guide }: { guide: GuideDef }) {
  return (
    <>
      <JsonLd data={guideSchemas(guide)} />
      <DocsBreadcrumbs trail={guideTrail(guide)} />

      <header className="mb-12">
        <div className="inline-flex items-center gap-2 text-indigo-400 font-medium mb-4">
          <BookOpenIcon className="w-5 h-5" />
          <span>How-to Guides</span>
        </div>
        <h1 className="text-4xl font-bold text-white mb-4">{guide.title}</h1>
        <p className="text-slate-400 text-lg leading-relaxed">
          <RichText markup={guide.intro} />
        </p>
      </header>

      <section className="mb-16">
        <h2 className="text-2xl font-bold text-white mb-6">Prerequisites</h2>
        <ul className="list-disc list-inside text-slate-400 space-y-2 mb-6">
          {guide.prerequisites.map((item) => (
            <li key={item}><RichText markup={item} /></li>
          ))}
        </ul>
      </section>

      {guide.sections.map((section) => (
        <section key={section.name} className="mb-16">
          <h2 className="text-2xl font-bold text-white mb-6">{section.name}</h2>
          {section.intro && (
            <p className="text-slate-400 mb-4">
              <RichText markup={section.intro} />
            </p>
          )}
          {section.steps.map((step, index) => (
            <Fragment key={step.name}>
              <h3 className="text-lg font-semibold text-white mb-3">{`${index + 1}. ${step.name}`}</h3>
              <p className="text-slate-400 mb-4">
                <RichText markup={step.text} />
              </p>
              {step.command && (
                <div className="mb-4">
                  <CodeBlock code={step.command} />
                </div>
              )}
              {step.resultText && <p className="text-slate-400 mb-4">{step.resultText}</p>}
              {step.resultOutput && (
                <div className="bg-slate-950 border border-slate-800 rounded-lg p-4 font-mono text-sm text-slate-300 mb-6">
                  {step.resultOutput}
                </div>
              )}
            </Fragment>
          ))}
        </section>
      ))}
    </>
  )
}
