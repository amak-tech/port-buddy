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

import { useEffect, useRef, useState } from 'react'
import { ClipboardDocumentIcon, CheckIcon } from '@heroicons/react/24/outline'

/**
 * A command the reader is meant to run, with a copy button.
 *
 * Used by /install, the documentation pages and the how-to guides, so a snippet looks and behaves
 * the same wherever it appears. The button is icon-only and labelled for screen readers rather than
 * with visible text, which keeps it out of the way of the code itself.
 */
export default function CodeBlock({ code }: { code: string }) {
  const [copied, setCopied] = useState(false)
  const timer = useRef<ReturnType<typeof setTimeout>>()

  // Clearing on unmount keeps the timeout from calling setState on a component the reader has
  // already navigated away from.
  useEffect(() => () => clearTimeout(timer.current), [])

  const copy = () => {
    void navigator.clipboard.writeText(code)
    setCopied(true)
    clearTimeout(timer.current)
    timer.current = setTimeout(() => setCopied(false), 2000)
  }

  return (
    <div className="relative group">
      <div className="absolute -inset-0.5 bg-gradient-to-r from-indigo-500/20 to-purple-500/20 rounded-lg blur opacity-0 group-hover:opacity-100 transition duration-500"></div>
      <div className="relative bg-slate-950 border border-slate-800 rounded-lg p-4 font-mono text-sm text-slate-300 overflow-x-auto">
        <pre>{code}</pre>
        <button
          type="button"
          onClick={copy}
          className="absolute top-3 right-3 p-2 rounded-md bg-slate-800/50 text-slate-400 hover:text-white hover:bg-slate-700 transition-all opacity-0 group-hover:opacity-100 focus:opacity-100"
          title="Copy to clipboard"
          aria-label={copied ? 'Copied to clipboard' : 'Copy to clipboard'}
        >
          {copied ? <CheckIcon className="w-4 h-4 text-green-400" /> : <ClipboardDocumentIcon className="w-4 h-4" />}
        </button>
      </div>
    </div>
  )
}
