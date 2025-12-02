import { useEffect, useState } from 'react'
import { useAuth } from '../../auth/AuthContext'
import { usePageTitle } from '../../components/PageHeader'
import { GlobeAltIcon, PlusIcon, TrashIcon, PencilIcon, CheckIcon, XMarkIcon } from '@heroicons/react/24/outline'
import { apiJson } from '../../lib/api'

interface Domain {
  id: string
  subdomain: string
  domain: string
  createdAt: string
  updatedAt: string
}

export default function Domains() {
  const { user } = useAuth()
  usePageTitle('Domains')
  
  const [domains, setDomains] = useState<Domain[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [editingId, setEditingId] = useState<string | null>(null)
  const [editValue, setEditValue] = useState('')
  const [creating, setCreating] = useState(false)

  const fetchDomains = async () => {
    try {
      const data = await apiJson<Domain[]>('/api/domains')
      setDomains(data)
    } catch (err) {
      setError('Failed to load domains')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchDomains()
  }, [])

  const handleAdd = async () => {
    setCreating(true)
    try {
      const newDomain = await apiJson<Domain>('/api/domains', { method: 'POST' })
      setDomains([...domains, newDomain])
    } catch (err: any) {
        alert(err.message || 'Failed to add domain')
    } finally {
        setCreating(false)
    }
  }
  
  const handleEditStart = (domain: Domain) => {
      setEditingId(domain.id)
      setEditValue(domain.subdomain)
  }
  
  const handleEditCancel = () => {
      setEditingId(null)
      setEditValue('')
  }

  const handleEditSave = async (id: string) => {
      try {
          const updated = await apiJson<Domain>(`/api/domains/${id}`, {
              method: 'PUT',
              body: JSON.stringify({ subdomain: editValue })
          })
          setDomains(domains.map(d => d.id === id ? updated : d))
          setEditingId(null)
      } catch (err: any) {
          alert(err.message || 'Failed to update domain')
      }
  }
  
  const handleDelete = async (id: string) => {
      if (!confirm('Are you sure you want to delete this domain?')) return;
      try {
          await apiJson(`/api/domains/${id}`, { method: 'DELETE' })
          setDomains(domains.filter(d => d.id !== id))
      } catch (err: any) {
          alert(err.message || 'Failed to delete domain')
      }
  }

  return (
    <div className="max-w-5xl">
      <div className="flex items-center justify-between mb-8">
        <div>
            <h2 className="text-2xl font-bold text-white">Domains</h2>
            <p className="text-slate-400 mt-1">Manage your custom domains and static subdomains.</p>
        </div>
        <button 
            onClick={handleAdd}
            disabled={creating}
            className="inline-flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
            {creating ? (
                <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
            ) : (
                <PlusIcon className="w-5 h-5" />
            )}
            Add Domain
        </button>
      </div>

      {loading ? (
          <div className="text-center py-12 text-slate-400">Loading...</div>
      ) : error ? (
          <div className="bg-red-500/10 border border-red-500/20 text-red-400 p-4 rounded-lg mb-6">
              {error}
          </div>
      ) : domains.length === 0 ? (
          <div className="bg-slate-900/50 border border-slate-800 rounded-xl p-12 text-center">
              <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-slate-800/50 text-slate-500 mb-6">
                  <GlobeAltIcon className="w-8 h-8" />
              </div>
              <h3 className="text-xl font-bold text-white mb-3">No domains yet</h3>
              <p className="text-slate-400 max-w-md mx-auto mb-8">
                  Create your first static subdomain to get started.
              </p>
              <button 
                onClick={handleAdd}
                disabled={creating}
                className="inline-flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg font-medium transition-colors disabled:opacity-50"
              >
                <PlusIcon className="w-5 h-5" />
                Add Domain
              </button>
          </div>
      ) : (
          <div className="grid gap-4">
              {domains.map(domain => (
                  <div key={domain.id} className="bg-slate-900/50 border border-slate-800 rounded-xl p-6 flex items-center justify-between group hover:border-indigo-500/30 transition-all">
                      <div className="flex items-center gap-4">
                          <div className="p-3 rounded-lg bg-indigo-500/10 text-indigo-400">
                              <GlobeAltIcon className="w-6 h-6" />
                          </div>
                          <div>
                              {editingId === domain.id ? (
                                  <div className="flex items-center gap-2">
                                      <input
                                          type="text"
                                          value={editValue}
                                          onChange={(e) => setEditValue(e.target.value)}
                                          className="bg-slate-800 border border-slate-700 rounded px-2 py-1 text-white focus:outline-none focus:border-indigo-500"
                                          autoFocus
                                      />
                                      <span className="text-slate-500">.{domain.domain}</span>
                                  </div>
                              ) : (
                                  <div className="text-lg font-medium text-white">
                                      {domain.subdomain}.{domain.domain}
                                  </div>
                              )}
                              <div className="text-sm text-slate-500 mt-1">
                                  Created on {new Date(domain.createdAt).toLocaleDateString()}
                              </div>
                          </div>
                      </div>
                      
                      <div className="flex items-center gap-2">
                          {editingId === domain.id ? (
                              <>
                                  <button
                                      onClick={() => handleEditSave(domain.id)}
                                      className="p-2 text-green-400 hover:bg-green-400/10 rounded-lg transition-colors"
                                      title="Save"
                                  >
                                      <CheckIcon className="w-5 h-5" />
                                  </button>
                                  <button
                                      onClick={handleEditCancel}
                                      className="p-2 text-slate-400 hover:bg-slate-700 rounded-lg transition-colors"
                                      title="Cancel"
                                  >
                                      <XMarkIcon className="w-5 h-5" />
                                  </button>
                              </>
                          ) : (
                              <>
                                  <button
                                      onClick={() => handleEditStart(domain)}
                                      className="p-2 text-slate-400 hover:text-indigo-400 hover:bg-indigo-400/10 rounded-lg transition-colors"
                                      title="Edit"
                                  >
                                      <PencilIcon className="w-5 h-5" />
                                  </button>
                                  <button
                                      onClick={() => handleDelete(domain.id)}
                                      className="p-2 text-slate-400 hover:text-red-400 hover:bg-red-400/10 rounded-lg transition-colors"
                                      title="Delete"
                                  >
                                      <TrashIcon className="w-5 h-5" />
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
