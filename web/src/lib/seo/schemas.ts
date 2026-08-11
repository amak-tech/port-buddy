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

import { PLAN_LIST, PRICE_CURRENCY, type Plan } from '../../config/plans'
import {
  DISCORD_URL,
  GITHUB_URL,
  SITE_LOGO_URL,
  SITE_NAME,
  SITE_ORIGIN,
  TELEGRAM_URL,
  canonicalUrl
} from '../../config/site'

/**
 * Builders for the schema.org blocks. Everything they emit is generated from the same data the
 * pages render, which is the rule for structured data: never describe something the visitor cannot
 * see on that page.
 */

export type JsonLdObject = Record<string, unknown>

const ORGANIZATION_ID = `${SITE_ORIGIN}/#organization`
const WEBSITE_ID = `${SITE_ORIGIN}/#website`
const SOFTWARE_ID = `${SITE_ORIGIN}/#software`

function planOffer(plan: Plan): JsonLdObject {
  const price = String(plan.priceMonthly)
  return {
    '@type': 'Offer',
    name: plan.name,
    price,
    priceCurrency: PRICE_CURRENCY,
    url: `${canonicalUrl('/')}/#pricing`,
    availability: 'https://schema.org/InStock',
    priceSpecification: {
      '@type': 'UnitPriceSpecification',
      price,
      priceCurrency: PRICE_CURRENCY,
      unitCode: 'MON',
      unitText: 'MONTH'
    }
  }
}

/** The CLI/service itself. `description` must be text the page actually renders. */
export function softwareApplicationSchema(description: string): JsonLdObject {
  return {
    '@context': 'https://schema.org',
    '@type': 'SoftwareApplication',
    '@id': SOFTWARE_ID,
    name: SITE_NAME,
    applicationCategory: 'DeveloperApplication',
    operatingSystem: 'macOS, Linux, Windows',
    description,
    url: canonicalUrl('/'),
    offers: PLAN_LIST.map(planOffer),
    publisher: { '@id': ORGANIZATION_ID }
  }
}

export function organizationSchema(): JsonLdObject {
  return {
    '@context': 'https://schema.org',
    '@type': 'Organization',
    '@id': ORGANIZATION_ID,
    name: SITE_NAME,
    url: SITE_ORIGIN,
    logo: SITE_LOGO_URL,
    sameAs: [GITHUB_URL, DISCORD_URL, TELEGRAM_URL]
  }
}

export function webSiteSchema(): JsonLdObject {
  return {
    '@context': 'https://schema.org',
    '@type': 'WebSite',
    '@id': WEBSITE_ID,
    name: SITE_NAME,
    url: SITE_ORIGIN,
    inLanguage: 'en',
    publisher: { '@id': ORGANIZATION_ID }
  }
}

export type FaqEntry = { question: string, answer: string }

/** Built from the same array the accordion renders, so question/answer text matches exactly. */
export function faqPageSchema(faqs: readonly FaqEntry[]): JsonLdObject {
  return {
    '@context': 'https://schema.org',
    '@type': 'FAQPage',
    mainEntity: faqs.map(({ question, answer }) => ({
      '@type': 'Question',
      name: question,
      acceptedAnswer: { '@type': 'Answer', text: answer }
    }))
  }
}

export type BreadcrumbEntry = { name: string, path: string }

export function breadcrumbListSchema(entries: readonly BreadcrumbEntry[]): JsonLdObject {
  return {
    '@context': 'https://schema.org',
    '@type': 'BreadcrumbList',
    itemListElement: entries.map((entry, index) => ({
      '@type': 'ListItem',
      position: index + 1,
      name: entry.name,
      item: canonicalUrl(entry.path)
    }))
  }
}

export type HowToStepInput = {
  name: string
  text: string
  /** Commands or other literal instructions rendered as part of the step. */
  directions?: readonly string[]
}

export type HowToSectionInput = {
  name: string
  steps: readonly HowToStepInput[]
}

function howToStep(step: HowToStepInput): JsonLdObject {
  const directions = step.directions ?? []
  return {
    '@type': 'HowToStep',
    name: step.name,
    text: step.text,
    ...(directions.length > 0
      ? { itemListElement: directions.map((text) => ({ '@type': 'HowToDirection', text })) }
      : {})
  }
}

/**
 * A single section is emitted as a flat list of steps; several sections (e.g. Java and Bedrock
 * editions of the same guide) become HowToSections so the markup mirrors the page's headings.
 */
export function howToSchema(input: {
  name: string
  description: string
  path: string
  sections: readonly HowToSectionInput[]
}): JsonLdObject {
  const { name, description, path, sections } = input
  const step = sections.length === 1
    ? sections[0].steps.map(howToStep)
    : sections.map((section) => ({
      '@type': 'HowToSection',
      name: section.name,
      itemListElement: section.steps.map(howToStep)
    }))

  return {
    '@context': 'https://schema.org',
    '@type': 'HowTo',
    name,
    description,
    url: canonicalUrl(path),
    step
  }
}
