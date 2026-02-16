/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

import { useEffect, useMemo, useState } from 'react'
import { useAuth } from '../../auth/AuthContext'
import { usePageTitle } from '../../components/PageHeader'
import { GlobeAltIcon, PlusIcon, TrashIcon, PencilIcon, CheckIcon, XMarkIcon } from '@heroicons/react/24/outline'
import { apiJson } from '../../lib/api'
import { AlertModal, ConfirmModal } from '../../components/Modal'

interface PortReservation {
  id: string
  publicHost: string
  publicPort: number
  name: string
  createdAt: string
  updatedAt: string
}

export default function Ports() {
  const { user } = useAuth()
  usePageTitle('Port Reservations')

  const [items, setItems] = useState<PortReservation[]>([])
  const [loading, setLoading] = useState(true)
  const [creating, setCreating] = useState(false)
  const [error, setError] = useState('')

  // Edit state
  const [editingId, setEditingId] = useState<string | null>(null)
  const [hosts, setHosts] = useState<string[]>([])
  const [selectedHost, setSelectedHost] = useState<string>('')
  const [portRange, setPortRange] = useState<{ min: number, max: number } | null>(null)
  const [editPort, setEditPort] = useState<string>('')
  const [editName, setEditName] = useState<string>('')

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
      setError('Failed to load port reservations')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  const fetchHosts = async () => {
    const hs = await apiJson<string[]>('/api/ports/hosts')
    setHosts(hs)
    return hs
  }

  const fetchRange = async (host: string) => {
    const r = await apiJson<{ min: number, max: number }>(`/api/ports/hosts/${encodeURIComponent(host)}/range`)
    setPortRange(r)
    return r
  }

  const startEdit = async (res: PortReservation) => {
    setEditingId(res.id)
    setEditName(res.name || '')
    try {
      const hs = await fetchHosts()
      if (hs.length <= 1) {
        // Only port is editable
        setSelectedHost(res.publicHost)
        const r = await fetchRange(res.publicHost)
        setEditPort(String(res.publicPort))
      } else {
        setSelectedHost(res.publicHost)
        await fetchRange(res.publicHost)
        setEditPort(String(res.publicPort))
      }
    } catch (e: any) {
      setAlertState({ isOpen: true, title: 'Error', message: e.message || 'Failed to start editing' })
      setEditingId(null)
    }
  }

  const cancelEdit = () => {
    setEditingId(null)
    setHosts([])
    setPortRange(null)
    setSelectedHost('')
    setEditPort('')
    setEditName('')
  }

  const saveEdit = async (id: string) => {
    try {
      const body: any = {}
      if (hosts.length > 1) body.publicHost = selectedHost
      body.publicPort = Number(editPort)
      body.name = editName
      const updated = await apiJson<PortReservation>(`/api/ports/${id}`, { method: 'PUT', body: JSON.stringify(body) })
      setItems(items.map(i => i.id === id ? updated : i))
      cancelEdit()
    } catch (e: any) {
      let msg = e.message || 'Failed to update reservation'
      if (e.status === 400 && msg.includes('already exists')) {
        msg = 'This name is already taken. Please choose a different name.'
      }
      setAlertState({ isOpen: true, title: 'Update failed', message: msg })
    }
  }

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

  const singleHost = hosts.length <= 1
  const portHint = portRange ? `Allowed range: ${portRange.min} - ${portRange.max}` : ''

  return (
    <div>
      <AlertModal isOpen={alertState.isOpen} title={alertState.title} message={alertState.message} onClose={() => setAlertState({ ...alertState, isOpen: false })} />
      <ConfirmModal isOpen={!!deleteId} title="Delete Port Reservation" message="Are you sure you want to delete this reservation?" onClose={() => setDeleteId(null)} onConfirm={() => void confirmDelete()} />

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
                  {editingId === item.id ? (
                    <div className="flex flex-col gap-2">
                      <input
                        type="text"
                        value={editName}
                        onChange={(e) => setEditName(e.target.value)}
                        className="bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-white w-full"
                        placeholder="Reservation name (optional)"
                      />
                      <div className="flex items-center gap-3">
                        {hosts.length > 1 && (
                          <select value={selectedHost} onChange={async (e) => { setSelectedHost(e.target.value); await fetchRange(e.target.value) }} className="bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-white">
                            {hosts.map(h => <option key={h} value={h}>{h}</option>)}
                          </select>
                        )}
                        <input type="number" inputMode="numeric" value={editPort} onChange={(e) => setEditPort(e.target.value)} className="bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-white w-32" placeholder={portHint} />
                        {portRange && <div className="text-xs text-slate-400">{portHint}</div>}
                      </div>
                    </div>
                  ) : (
                    <>
                      <div className="text-white font-medium">
                        {item.name ? <span className="mr-2">{item.name}</span> : null}
                        <span className={item.name ? "text-slate-400 text-sm font-normal" : ""}>
                          {item.publicHost}:{item.publicPort}
                        </span>
                      </div>
                    </>
                  )}
                  <div className="text-slate-500 text-sm mt-1">Created on {new Date(item.createdAt).toLocaleDateString()}</div>
                </div>
              </div>
              <div className="flex items-center gap-2">
                {editingId === item.id ? (
                  <>
                    <button onClick={() => void saveEdit(item.id)} className="inline-flex items-center gap-2 px-3 py-2 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white">
                      <CheckIcon className="h-5 w-5" /> Save
                    </button>
                    <button onClick={cancelEdit} className="inline-flex items-center gap-2 px-3 py-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200">
                      <XMarkIcon className="h-5 w-5" /> Cancel
                    </button>
                  </>
                ) : (
                  <>
                    <button onClick={() => void startEdit(item)} className="inline-flex items-center gap-2 px-3 py-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200">
                      <PencilIcon className="h-5 w-5" /> Edit
                    </button>
                    <button onClick={() => setDeleteId(item.id)} className="inline-flex items-center gap-2 px-3 py-2 rounded-lg bg-red-500/10 hover:bg-red-500/20 text-red-300 border border-red-500/30">
                      <TrashIcon className="h-5 w-5" /> Delete
                    </button>
                  </>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
