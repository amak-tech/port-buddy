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

import type { JsonLdObject } from './schemas'

/**
 * Renders a structured-data block. Rendered inside the page body so it ends up in the prerendered
 * HTML of every marketing/docs route without a head manager.
 *
 * Every "<" is escaped to its < form so a value containing a closing script tag cannot
 * terminate the block early. The escape is part of JSON's own syntax, so it still parses as JSON.
 */
export default function JsonLd({ data }: { data: JsonLdObject | JsonLdObject[] }) {
  const json = JSON.stringify(data).replace(/</g, '\\u003c')
  return <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: json }} />
}
