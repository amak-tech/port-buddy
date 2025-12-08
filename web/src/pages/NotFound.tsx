/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

import { Link } from 'react-router-dom'
import {
  ArrowRightIcon,
  HomeIcon,
  MagnifyingGlassIcon
} from '@heroicons/react/24/outline'
import React from 'react'
import Seo from '../components/Seo'

export default function NotFound() {
  return (
    <div className="flex flex-col gap-16 pb-24">
      <Seo
        title="Page Not Found | Port Buddy"
        description="The page you’re looking for doesn’t exist. Head back to the homepage or explore installation instructions."
        keywords="404, page not found, port buddy"
      />

      <section className="relative pt-24 md:pt-36 px-4">
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-indigo-900/20 via-slate-900/0 to-slate-900/0 pointer-events-none" />

        <div className="container relative mx-auto max-w-4xl text-center">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/10 border border-indigo-500/20 text-indigo-300 text-xs font-medium mb-6">
            <span className="relative flex h-2 w-2">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-indigo-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-2 w-2 bg-indigo-500"></span>
            </span>
            404 — Not Found
          </div>

          <h1 className="text-4xl md:text-6xl font-extrabold tracking-tight text-white mb-6 leading-tight">
            Oops, this tunnel seems closed
          </h1>

          <p className="text-lg md:text-xl text-slate-400 mb-10 leading-relaxed max-w-2xl mx-auto">
            The page you’re trying to reach doesn’t exist or has moved. Double‑check the URL, or head back to a safe place.
          </p>

          <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
            <Link
              to="/"
              className="w-full sm:w-auto px-6 py-3 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg font-semibold transition-all flex items-center justify-center gap-2 shadow-lg shadow-indigo-500/20"
            >
              <HomeIcon className="w-5 h-5" />
              Go to Home
            </Link>
            <Link
              to="/install"
              className="w-full sm:w-auto px-6 py-3 bg-slate-800 hover:bg-slate-700 text-white rounded-lg font-semibold transition-all flex items-center justify-center gap-2 border border-slate-700"
            >
              Installation Guide
              <ArrowRightIcon className="w-4 h-4" />
            </Link>
          </div>

          <div className="mt-14 mx-auto max-w-3xl bg-slate-900 rounded-xl border border-slate-800 shadow-2xl overflow-hidden">
            <div className="flex items-center px-4 py-3 bg-slate-800/50 border-b border-slate-800 gap-2">
              <div className="w-3 h-3 rounded-full bg-red-500/20 border border-red-500/50"></div>
              <div className="w-3 h-3 rounded-full bg-yellow-500/20 border border-yellow-500/50"></div>
              <div className="w-3 h-3 rounded-full bg-green-500/20 border border-green-500/50"></div>
              <div className="ml-2 text-xs text-slate-500 font-mono">request — 404</div>
            </div>
            <div className="p-6 font-mono text-sm md:text-base overflow-x-auto text-left">
              <div className="flex items-center gap-2 text-slate-400 mb-3">
                <span className="text-red-400">GET</span>
                <span className="text-slate-300">/unknown</span>
                <span className="text-slate-600">→</span>
                <span className="text-slate-500">404 Not Found</span>
              </div>
              <div className="rounded-lg bg-slate-950/60 border border-slate-800 p-4 text-slate-300">
                <div className="flex items-center gap-2 mb-2">
                  <MagnifyingGlassIcon className="w-4 h-4 text-slate-500" />
                  <span className="text-slate-400">Hints</span>
                </div>
                <ul className="list-disc pl-5 space-y-1 text-slate-400">
                  <li>Check the URL spelling.</li>
                  <li>Use the navigation above to find what you need.</li>
                  <li>Start with our Installation guide to expose a local port.</li>
                </ul>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>
  )
}
