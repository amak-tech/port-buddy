/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

import { FormEvent, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'

export default function Passcode() {
  const [sp] = useSearchParams()
  const [passcode, setPasscode] = useState('')
  const targetDomain = sp.get('target_domain') || ''

  const protocol = useMemo(() => window.location.protocol, [])

  const onSubmit = (e: FormEvent) => {
    e.preventDefault()
    if (!targetDomain || !passcode) return
    const url = `${protocol}//${targetDomain}/?passcode=${encodeURIComponent(passcode)}`
    window.location.href = url
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-950 px-4">
      <div className="w-full max-w-md bg-slate-900/70 backdrop-blur border border-slate-800 rounded-2xl p-6 shadow-xl">
        <h1 className="text-xl font-semibold text-white mb-2">Enter passcode</h1>
        <p className="text-slate-400 text-sm mb-6">
          This tunnel is protected. Enter the passcode to proceed to
          {targetDomain ? ' ' : ''}
          {targetDomain && (
            <span className="text-slate-200 font-mono">{targetDomain}</span>
          )}.
        </p>
        <form onSubmit={onSubmit} className="flex flex-col gap-4">
          <div>
            <label className="block text-sm text-slate-300 mb-1" htmlFor="passcode">Passcode</label>
            <input
              id="passcode"
              type="password"
              value={passcode}
              onChange={(e) => setPasscode(e.target.value)}
              className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500"
              placeholder="Enter passcode"
              autoFocus
            />
          </div>
          <button
            type="submit"
            disabled={!passcode || !targetDomain}
            className="bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed text-white px-4 py-2 rounded-lg transition-colors"
          >
            Continue
          </button>
        </form>
      </div>
    </div>
  )
}
