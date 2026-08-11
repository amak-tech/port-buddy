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

import { DEFAULT_SITE_ORIGIN, absoluteUrl } from './routes'

/**
 * Site-level identity used by canonical URLs and structured data. The origin comes from the same
 * VITE_CANONICAL build variable the HTML templates interpolate, so runtime and static HTML agree.
 */
const CONFIGURED_ORIGIN = ((import.meta as any).env?.VITE_CANONICAL ?? '').toString().trim()

export const SITE_ORIGIN = CONFIGURED_ORIGIN.replace(/\/+$/, '') || DEFAULT_SITE_ORIGIN

export const SITE_NAME = 'Port Buddy'

export const SITE_LOGO_URL = `${SITE_ORIGIN}/favicon-512x512.png`

export const GITHUB_URL = 'https://github.com/amak-tech/port-buddy'
export const DISCORD_URL = 'https://discord.gg/PaEzzjmvSH'
export const TELEGRAM_URL = 'https://t.me/portbuddy'

/** Absolute, canonical URL for a path — no trailing slash, homepage included. */
export function canonicalUrl(pathname: string): string {
  return absoluteUrl(SITE_ORIGIN, pathname)
}
