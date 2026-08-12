/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

import { useEffect, useRef, useState } from 'react'
import { usePageTitle } from '../../components/PageHeader'
import { GlobeAltIcon, MapPinIcon, PlusIcon, TrashIcon, PencilIcon } from '@heroicons/react/24/outline'
import { apiJson } from '../../lib/api'
import { AlertModal, ConfirmModal, Modal } from '../../components/Modal'

interface PortReservation {
  id: string
  publicHost: string
  region: string | null
  publicPort: number
  name: string | null
  createdAt: string
  updatedAt: string
}

interface ProxyHost {
  host: string
  region: string | null
  portMin: number
  portMax: number
}

interface PortAvailability {
  available: boolean
  reason: string | null
}

export default function Ports() {
  usePageTitle('Port Reservations')

  const [items, setItems] = useState<PortReservation[]>([])
  const [loading, setLoading] = useState(true)
  const [creating, setCreating] = useState(false)

  const [editing, setEditing] = useState<PortReservation | null>(null)

  // Dialogs
  const [alertState, setAlertState] = useState<{ isOpen: boolean, title: string, message: string }>({
    isOpen: false, title: '', message: ''
  })
  const [deleteId, setDeleteId] = useState<string | null>(null)

  const load = async () => {
    try {
      const data = await apiJson<PortReservation[]>('/api/ports')
      setItems(data)
    } catch (e) {
      setAlertState({ isOpen: true, title: 'Error', message: 'Failed to load port reservations' })
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  const addReservation = async () => {
    setCreating(true)
    try {
      const created = await apiJson<PortReservation>('/api/ports', { method: 'POST' })
      setItems([...items, created])
    } catch (e: any) {
      setAlertState({ isOpen: true, title: 'Error', message: e.message || 'Failed to create reservation' })
    } finally {
      setCreating(false)
    }
  }

  const confirmDelete = async () => {
    if (!deleteId) return
    try {
      await apiJson(`/api/ports/${deleteId}`, { method: 'DELETE' })
      setItems(items.filter(i => i.id !== deleteId))
      setDeleteId(null)
    } catch (e: any) {
      const msg = (e.status === 409)
        ? 'Cannot delete this reservation because there are active TCP tunnels using it. Close them first and try again.'
        : (e.message || 'Failed to delete reservation')
      setAlertState({ isOpen: true, title: 'Delete failed', message: msg })
    }
  }

  return (
    <div>
      <AlertModal isOpen={alertState.isOpen} title={alertState.title} message={alertState.message} onClose={() => setAlertState({ ...alertState, isOpen: false })} />
      <ConfirmModal isOpen={!!deleteId} title="Delete Port Reservation" message="Are you sure you want to delete this reservation?" onClose={() => setDeleteId(null)} onConfirm={() => void confirmDelete()} />

      <EditReservationModal
        reservation={editing}
        onClose={() => setEditing(null)}
        onSaved={(updated) => {
          setItems(items.map(i => i.id === updated.id ? updated : i))
          setEditing(null)
        }}
      />

      <div className="flex items-center justify-between mb-6">
        <div>
          <h2 className="text-2xl font-bold text-white">Port Reservations</h2>
          <p className="text-slate-400 mt-1">Reserve public TCP ports on available proxy hosts.</p>
        </div>
        <button
          onClick={() => void addReservation()}
          disabled={creating || loading}
          className="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white transition-all shadow-lg shadow-indigo-500/20 disabled:opacity-60"
        >
          <PlusIcon className="h-5 w-5" /> Add Reservation
        </button>
      </div>

      {loading ? (
        <div className="space-y-4">
          {[1, 2].map(i => (
            <div key={i} className="bg-slate-900/50 border border-slate-800 rounded-xl p-6 h-20 animate-pulse"></div>
          ))}
        </div>
      ) : items.length === 0 ? (
        <div className="border border-slate-800 rounded-xl p-10 text-center bg-slate-900/40">
          <div className="mx-auto w-12 h-12 rounded-xl bg-indigo-500/10 border border-indigo-500/30 flex items-center justify-center mb-4">
            <GlobeAltIcon className="h-6 w-6 text-indigo-400" />
          </div>
          <h3 className="text-xl font-bold text-white mb-1">No reservations yet</h3>
          <p className="text-slate-400 mb-6">Create your first TCP port reservation to get started.</p>
          <button onClick={() => void addReservation()} className="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white transition-all shadow-lg shadow-indigo-500/20">
            <PlusIcon className="h-5 w-5" /> Add Reservation
          </button>
        </div>
      ) : (
        <div className="space-y-4">
          {items.map(item => (
            <div key={item.id} className="bg-slate-900/50 border border-slate-800 rounded-xl p-6 flex items-center justify-between gap-6">
              <div className="flex items-center gap-4">
                <div className="w-10 h-10 rounded-lg bg-indigo-500/10 border border-indigo-500/30 flex items-center justify-center">
                  <GlobeAltIcon className="h-5 w-5 text-indigo-400" />
                </div>
                <div>
                  <div className="text-white font-medium">
                    {item.name ? <span className="mr-2">{item.name}</span> : null}
                    <span className={item.name ? "text-slate-400 text-sm font-normal" : ""}>
                      {item.publicHost}:{item.publicPort}
                    </span>
                  </div>
                  <div className="flex items-center gap-3 mt-1">
                    <span className="inline-flex items-center gap-1 text-xs text-slate-300 bg-slate-800/70 border border-slate-700 rounded-full px-2 py-0.5">
                      <MapPinIcon className="h-3.5 w-3.5 text-indigo-400" />
                      {item.region || 'Region unknown'}
                    </span>
                    <span className="text-slate-500 text-sm">Created on {new Date(item.createdAt).toLocaleDateString()}</span>
                  </div>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <button onClick={() => setEditing(item)} className="inline-flex items-center gap-2 px-3 py-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200">
                  <PencilIcon className="h-5 w-5" /> Edit
                </button>
                <button onClick={() => setDeleteId(item.id)} className="inline-flex items-center gap-2 px-3 py-2 rounded-lg bg-red-500/10 hover:bg-red-500/20 text-red-300 border border-red-500/30">
                  <TrashIcon className="h-5 w-5" /> Delete
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

interface EditReservationModalProps {
  reservation: PortReservation | null
  onClose: () => void
  onSaved: (updated: PortReservation) => void
}

function proxyLabel(proxy: ProxyHost): string {
  return proxy.region ? `${proxy.region} — ${proxy.host}` : proxy.host
}

function EditReservationModal({ reservation, onClose, onSaved }: EditReservationModalProps) {
  const [proxies, setProxies] = useState<ProxyHost[]>([])
  const [loadingProxies, setLoadingProxies] = useState(false)
  const [host, setHost] = useState('')
  const [port, setPort] = useState('')
  const [name, setName] = useState('')
  const [availability, setAvailability] = useState<PortAvailability | null>(null)
  const [checking, setChecking] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [suggestingPort, setSuggestingPort] = useState(false)
  const hostRequest = useRef(0)

  const selected = proxies.find(p => p.host === host) || null

  // Load the proxy list (host + region + its own port range) whenever the dialog opens
  useEffect(() => {
    if (!reservation) return
    setHost(reservation.publicHost)
    setPort(String(reservation.publicPort))
    setName(reservation.name || '')
    setAvailability(null)
    setError('')
    // Discard any port suggestion still in flight from a previous edit
    hostRequest.current++
    setSuggestingPort(false)
    setLoadingProxies(true)
    let cancelled = false
    ;(async () => {
      try {
        const list = await apiJson<ProxyHost[]>('/api/ports/hosts')
        if (cancelled) return
        // The proxy currently holding the reservation may be offline — keep it selectable
        if (!list.some(p => p.host === reservation.publicHost)) {
          const range = await apiJson<{ min: number, max: number }>(
            `/api/ports/hosts/${encodeURIComponent(reservation.publicHost)}/range`)
          if (cancelled) return
          list.unshift({
            host: reservation.publicHost,
            region: reservation.region,
            portMin: range.min,
            portMax: range.max,
          })
        }
        setProxies(list)
      } catch (e: any) {
        if (!cancelled) setError(e.message || 'Failed to load proxy hosts')
      } finally {
        if (!cancelled) setLoadingProxies(false)
      }
    })()
    return () => { cancelled = true }
  }, [reservation])

  // Switching proxy proposes the lowest port still free there — the current one may be taken or out of range
  const selectHost = async (next: string) => {
    if (!reservation || next === host) return
    setHost(next)
    setError('')
    const request = ++hostRequest.current
    if (next === reservation.publicHost) {
      setPort(String(reservation.publicPort))
      return
    }
    setSuggestingPort(true)
    try {
      const res = await apiJson<{ port: number | null }>(
        `/api/ports/hosts/${encodeURIComponent(next)}/next-free-port`)
      if (request !== hostRequest.current) return
      if (res.port === null) {
        setPort('')
        setError(`No free ports left on ${next}. Please pick another proxy.`)
      } else {
        setPort(String(res.port))
      }
    } catch (e: any) {
      if (request === hostRequest.current) setError(e.message || 'Failed to find a free port on this proxy')
    } finally {
      if (request === hostRequest.current) setSuggestingPort(false)
    }
  }

  // Re-check availability whenever the selected proxy or the port changes
  useEffect(() => {
    if (!reservation || !selected) return
    const parsed = Number(port)
    if (!port.trim()) {
      setAvailability(null)
      setChecking(false)
      return
    }
    if (!Number.isInteger(parsed)) {
      setAvailability({ available: false, reason: 'Enter a whole port number.' })
      setChecking(false)
      return
    }
    if (parsed < selected.portMin || parsed > selected.portMax) {
      setAvailability({
        available: false,
        reason: `Port must be between ${selected.portMin} and ${selected.portMax} on ${selected.host}.`,
      })
      setChecking(false)
      return
    }
    if (parsed === reservation.publicPort && selected.host === reservation.publicHost) {
      setAvailability({ available: true, reason: null })
      setChecking(false)
      return
    }

    setChecking(true)
    let cancelled = false
    const timer = setTimeout(() => {
      apiJson<PortAvailability>(
        `/api/ports/hosts/${encodeURIComponent(selected.host)}/availability`
        + `?port=${parsed}&excludeId=${encodeURIComponent(reservation.id)}`)
        .then(res => { if (!cancelled) setAvailability(res) })
        .catch(() => { if (!cancelled) setAvailability(null) })
        .finally(() => { if (!cancelled) setChecking(false) })
    }, 400)
    return () => { cancelled = true; clearTimeout(timer) }
  }, [reservation, selected, port])

  const save = async () => {
    if (!reservation) return
    setSaving(true)
    setError('')
    try {
      // A blank name clears it server-side; trimming keeps stray whitespace out of names
      const body: any = { publicPort: Number(port), name: name.trim() }
      // Only send the host when it actually changed, so an offline proxy is not rejected as unknown
      if (host !== reservation.publicHost) body.publicHost = host
      const updated = await apiJson<PortReservation>(`/api/ports/${reservation.id}`, {
        method: 'PUT',
        body: JSON.stringify(body),
      })
      onSaved(updated)
    } catch (e: any) {
      let msg = e.message || 'Failed to update reservation'
      if (e.status === 400 && msg.includes('already exists')) {
        msg = 'This name is already taken. Please choose a different name.'
      }
      setError(msg)
    } finally {
      setSaving(false)
    }
  }

  const portInvalid = availability !== null && !availability.available
  const canSave = !!reservation && !!selected && !saving && !checking && !suggestingPort
    && !portInvalid && !!port.trim()

  return (
    <Modal isOpen={!!reservation} onClose={onClose} title="Edit Port Reservation">
      <div className="space-y-4">
        <div>
          <label className="block text-sm text-slate-300 mb-1">Name</label>
          <input
            type="text"
            value={name}
            onChange={e => setName(e.target.value)}
            className="w-full bg-slate-800 border border-slate-700 rounded px-3 py-2 text-white focus:outline-none focus:border-indigo-500"
            placeholder="Reservation name (optional)"
          />
          <p className="text-xs text-slate-500 mt-1">Use it instead of the port: <code className="text-indigo-400">portbuddy tcp 5432 --port-reservation {name || 'my-db'}</code></p>
        </div>

        <div>
          <label className="block text-sm text-slate-300 mb-1">Proxy region</label>
          <select
            value={host}
            onChange={e => void selectHost(e.target.value)}
            disabled={loadingProxies || proxies.length <= 1}
            className="w-full bg-slate-800 border border-slate-700 rounded px-3 py-2 text-white focus:outline-none focus:border-indigo-500 disabled:opacity-70"
          >
            {loadingProxies && <option value={host}>Loading…</option>}
            {proxies.map(p => <option key={p.host} value={p.host}>{proxyLabel(p)}</option>)}
          </select>
          {selected && (
            <p className="flex items-center gap-1 text-xs text-slate-400 mt-1">
              <MapPinIcon className="h-3.5 w-3.5 text-indigo-400" />
              {selected.region || 'Region unknown'} · {selected.host}
            </p>
          )}
          {proxies.length === 1 && !loadingProxies && (
            <p className="text-xs text-slate-500 mt-1">Only one proxy is available right now.</p>
          )}
        </div>

        <div>
          <label className="block text-sm text-slate-300 mb-1">Public port</label>
          <input
            type="number"
            inputMode="numeric"
            value={port}
            min={selected?.portMin}
            max={selected?.portMax}
            disabled={suggestingPort}
            onChange={e => setPort(e.target.value)}
            className={`w-full bg-slate-800 border rounded px-3 py-2 text-white focus:outline-none ${
              portInvalid ? 'border-red-500/70 focus:border-red-500' : 'border-slate-700 focus:border-indigo-500'
            }`}
            placeholder={selected ? String(selected.portMin) : ''}
          />
          <div className="mt-1 min-h-[1.25rem] text-xs">
            {suggestingPort ? (
              <span className="text-slate-500">Finding a free port on {host}…</span>
            ) : checking ? (
              <span className="text-slate-500">Checking availability…</span>
            ) : portInvalid ? (
              <span className="text-red-400">{availability?.reason}</span>
            ) : availability?.available ? (
              <span className="text-emerald-400">Port {port} is available on {selected?.host}.</span>
            ) : selected ? (
              <span className="text-slate-500">Allowed range on {selected.host}: {selected.portMin} – {selected.portMax}</span>
            ) : null}
          </div>
        </div>

        {error && <div className="text-sm text-red-400 bg-red-500/10 border border-red-500/30 rounded px-3 py-2">{error}</div>}

        <div className="flex items-center justify-end gap-3 pt-2">
          <button
            onClick={onClose}
            className="px-4 py-2 text-sm font-medium text-slate-300 hover:text-white hover:bg-slate-800 rounded-lg transition-colors"
          >
            Cancel
          </button>
          <button
            onClick={() => void save()}
            disabled={!canSave}
            className="px-4 py-2 text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 rounded-lg transition-colors disabled:opacity-50"
          >
            {saving ? 'Saving...' : 'Save Changes'}
          </button>
        </div>
      </div>
    </Modal>
  )
}
