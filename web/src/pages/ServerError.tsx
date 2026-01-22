/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

import { Link, useSearchParams } from 'react-router-dom'
import { ArrowPathIcon, HomeIcon, WrenchScrewdriverIcon } from '@heroicons/react/24/outline'
import React, { useEffect, useState } from 'react'

export default function ServerError() {
  const [searchParams] = useSearchParams()
  const [retryUrl, setRetryUrl] = useState<string | null>(null)

  // Extract and decode retry param once
  useEffect(() => {
    const raw = searchParams.get('retry')?.trim()
    if (raw) {
      try {
        const decoded = decodeURIComponent(raw)
        setRetryUrl(decoded)
      } catch (_err) {
        setRetryUrl(null)
      }
    }
  // run once on mount
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // Clean retry from address bar without navigation
  useEffect(() => {
    if (searchParams.has('retry')) {
      const params = new URLSearchParams(searchParams as unknown as URLSearchParams)
      params.delete('retry')
      const queryString = params.toString()
      const hash = window.location.hash || ''
      const newUrl = window.location.pathname + (queryString ? `?${queryString}` : '') + hash
      window.history.replaceState({}, document.title, newUrl)
    }
  }, [searchParams])
  return (
    <div className="flex flex-col gap-16 pb-24">
      <section className="relative pt-12 md:pt-36">
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-red-900/15 via-slate-900/0 to-slate-900/0 pointer-events-none" />
<div className="container relative max-w-4xl text-center">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-red-500/10 border border-red-500/20 text-red-300 text-xs font-medium mb-6">
            <span className="relative flex h-2 w-2">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-red-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-2 w-2 bg-red-500"></span>
            </span>
            5xx — Server Error
          </div>

          <h1 className="text-4xl md:text-6xl font-extrabold tracking-tight text-white mb-6 leading-tight">
            Something went wrong on our side
          </h1>

          <p className="text-lg md:text-xl text-slate-400 mb-10 leading-relaxed max-w-2xl mx-auto">
            We are experiencing an internal issue. Please try again in a moment. If the problem persists, head back home or check your connection.
          </p>

          <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
            <button
              onClick={() => {
                if (retryUrl && retryUrl.length > 0) {
                  window.location.assign(retryUrl)
                } else {
                  window.location.reload()
                }
              }}
              className="w-full sm:w-auto px-6 py-3 bg-red-600 hover:bg-red-500 text-white rounded-lg font-semibold transition-all flex items-center justify-center gap-2 shadow-lg shadow-red-500/20"
            >
              <ArrowPathIcon className="w-5 h-5" />
              Try Again
            </button>
            <Link
              to="/"
              className="w-full sm:w-auto px-6 py-3 bg-slate-800 hover:bg-slate-700 text-white rounded-lg font-semibold transition-all flex items-center justify-center gap-2 border border-slate-700"
            >
              <HomeIcon className="w-5 h-5" />
              Go to Home
            </Link>
          </div>

          <div className="mt-14 mx-auto max-w-3xl bg-slate-900 rounded-xl border border-slate-800 shadow-2xl overflow-hidden">
            <div className="flex items-center px-4 py-3 bg-slate-800/50 border-b border-slate-800 gap-2">
              <div className="w-3 h-3 rounded-full bg-red-500/20 border border-red-500/50"></div>
              <div className="w-3 h-3 rounded-full bg-yellow-500/20 border border-yellow-500/50"></div>
              <div className="w-3 h-3 rounded-full bg-green-500/20 border border-green-500/50"></div>
              <div className="ml-2 text-xs text-slate-500 font-mono">response — 5xx</div>
            </div>
            <div className="p-6 font-mono text-sm md:text-base overflow-x-auto text-left">
              <div className="flex items-center gap-2 text-slate-400 mb-3">
                <span className="text-red-400">GET</span>
                <span className="text-slate-300">/some-endpoint</span>
                <span className="text-slate-600">→</span>
                <span className="text-slate-500">500 Internal Server Error</span>
              </div>
              <div className="rounded-lg bg-slate-950/60 border border-slate-800 p-4 text-slate-300">
                <div className="flex items-center gap-2 mb-2">
                  <WrenchScrewdriverIcon className="w-4 h-4 text-slate-500" />
                  <span className="text-slate-400">You can try</span>
                </div>
                <ul className="list-disc pl-5 space-y-1 text-slate-400">
                  <li>Reload the page.</li>
                  <li>Check your internet connection.</li>
                  <li>Return to the homepage and try again.</li>
                </ul>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>
  )
}
