import { useEffect, useState } from 'react'
import { useAuth } from '../../auth/AuthContext'
import { usePageTitle } from '../../components/PageHeader'
import { GlobeAltIcon, PlusIcon, TrashIcon, PencilIcon, CheckIcon, XMarkIcon, LockClosedIcon, LockOpenIcon } from '@heroicons/react/24/outline'
import { apiJson } from '../../lib/api'
import { AlertModal, ConfirmModal, Modal } from '../../components/Modal'

interface Domain {
  id: string
  subdomain: string
  domain: string
  customDomain: string | null
  cnameVerified: boolean
  sslActive: boolean
  passcodeProtected: boolean
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

  // Custom Domain state
  const [customDomainDomainId, setCustomDomainDomainId] = useState<string | null>(null)
  const [customDomainValue, setCustomDomainValue] = useState('')
  const [customDomainSaving, setCustomDomainSaving] = useState(false)
  const [customDomainRemoving, setCustomDomainRemoving] = useState(false)
  const [verifyingCname, setVerifyingCname] = useState(false)

  // Passcode modal state
  const [passcodeDomainId, setPasscodeDomainId] = useState<string | null>(null)
  const [pass1, setPass1] = useState('')
  const [passSaving, setPassSaving] = useState(false)
  const [passRemoving, setPassRemoving] = useState(false)

  // Dialog states
  const [alertState, setAlertState] = useState<{ isOpen: boolean, title: string, message: string }>({ 
    isOpen: false, 
    title: '', 
    message: '' 
  })
  const [deleteId, setDeleteId] = useState<string | null>(null)

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
        setAlertState({ 
            isOpen: true, 
            title: 'Error', 
            message: err.message || 'Failed to add domain' 
        })
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
          setAlertState({ 
            isOpen: true, 
            title: 'Error', 
            message: err.message || 'Failed to update domain' 
        })
      }
  }
  
  const handleDeleteClick = (id: string) => {
      setDeleteId(id)
  }

  const handleConfirmDelete = async () => {
      if (!deleteId) return
      try {
          await apiJson(`/api/domains/${deleteId}`, { method: 'DELETE' })
          setDomains(domains.filter(d => d.id !== deleteId))
      } catch (err: any) {
          setAlertState({ 
            isOpen: true, 
            title: 'Error', 
            message: err.message || 'Failed to delete domain' 
        })
      } finally {
          setDeleteId(null)
      }
  }

  const handleCustomDomainClick = (domain: Domain) => {
    setCustomDomainDomainId(domain.id)
    setCustomDomainValue(domain.customDomain || '')
  }

  const handleCustomDomainSave = async () => {
    if (!customDomainDomainId) return
    setCustomDomainSaving(true)
    try {
      const updated = await apiJson<Domain>(`/api/domains/${customDomainDomainId}/custom-domain`, {
        method: 'PUT',
        body: JSON.stringify({ customDomain: customDomainValue })
      })
      setDomains(domains.map(d => d.id === customDomainDomainId ? updated : d))
      setCustomDomainDomainId(null)
    } catch (err: any) {
      setAlertState({
        isOpen: true,
        title: 'Error',
        message: err.message || 'Failed to update custom domain'
      })
    } finally {
      setCustomDomainSaving(false)
    }
  }

  const handleCustomDomainRemove = async () => {
    if (!customDomainDomainId) return
    setCustomDomainRemoving(true)
    try {
      await apiJson(`/api/domains/${customDomainDomainId}/custom-domain`, {
        method: 'DELETE'
      })
      setDomains(domains.map(d => d.id === customDomainDomainId ? { ...d, customDomain: null, cnameVerified: false } : d))
      setCustomDomainDomainId(null)
    } catch (err: any) {
      setAlertState({
        isOpen: true,
        title: 'Error',
        message: err.message || 'Failed to remove custom domain'
      })
    } finally {
      setCustomDomainRemoving(false)
    }
  }

  const handleVerifyCname = async (id: string) => {
    setVerifyingCname(true)
    try {
      const updated = await apiJson<Domain>(`/api/domains/${id}/verify-cname`, { method: 'POST' })
      setDomains(domains.map(d => d.id === id ? updated : d))
      setAlertState({
        isOpen: true,
        title: 'Success',
        message: 'CNAME verified successfully! SSL certificate issuance has been triggered.'
      })
    } catch (err: any) {
      setAlertState({
        isOpen: true,
        title: 'Error',
        message: err.message || 'CNAME verification failed'
      })
    } finally {
      setVerifyingCname(false)
    }
  }

  const openSetPasscode = (id: string) => {
    setPasscodeDomainId(id)
    setPass1('')
  }

  const closePasscodeModal = () => {
    setPasscodeDomainId(null)
    setPass1('')
    setPassSaving(false)
    setPassRemoving(false)
  }

  const savePasscode = async () => {
    if (!passcodeDomainId) return
    if (pass1.length < 4) {
      setAlertState({ isOpen: true, title: 'Invalid passcode', message: 'Passcode must be at least 4 characters long.' })
      return
    }
    setPassSaving(true)
    try {
      const updated = await apiJson<Domain>(`/api/domains/${passcodeDomainId}/passcode`, {
        method: 'PUT',
        body: JSON.stringify({ passcode: pass1 })
      })
      setDomains(domains.map(d => d.id === updated.id ? updated : d))
      closePasscodeModal()
    } catch (err: any) {
      setPassSaving(false)
      setAlertState({ isOpen: true, title: 'Error', message: err.message || 'Failed to set passcode' })
    }
  }

  const removePasscode = async (id: string) => {
    try {
      setPassRemoving(true)
      await apiJson(`/api/domains/${id}/passcode`, { method: 'DELETE' })
      setDomains(domains.map(d => d.id === id ? { ...d, passcodeProtected: false } : d))
      closePasscodeModal()
    } catch (err: any) {
      setPassRemoving(false)
      setAlertState({ isOpen: true, title: 'Error', message: err.message || 'Failed to remove passcode' })
    }
  }

  return (
    <div className="max-w-6xl">
      <AlertModal 
        isOpen={alertState.isOpen} 
        onClose={() => setAlertState({ ...alertState, isOpen: false })}
        title={alertState.title}
        message={alertState.message}
      />

      <ConfirmModal
        isOpen={!!deleteId}
        onClose={() => setDeleteId(null)}
        onConfirm={handleConfirmDelete}
        title="Delete Domain"
        message="Are you sure you want to delete this domain? This action cannot be undone."
        confirmText="Delete"
        isDangerous
      />

      {/* Set/Change Passcode Modal */}
      <Modal
        isOpen={!!passcodeDomainId}
        onClose={closePasscodeModal}
        title={(domains.find(d => d.id === passcodeDomainId)?.passcodeProtected ? 'Change' : 'Set') + ' Domain Passcode'}
      >
        <div className="space-y-4">
          <div>
            <label className="block text-sm text-slate-300 mb-1">Passcode</label>
            <input
              type="password"
              value={pass1}
              onChange={e => setPass1(e.target.value)}
              className="w-full bg-slate-800 border border-slate-700 rounded px-3 py-2 text-white focus:outline-none focus:border-indigo-500"
              placeholder="Enter passcode"
            />
          </div>
          <div className="flex items-center justify-between gap-3 pt-2">
            {/* Left side: Remove Passcode (only if currently protected) */}
            {domains.find(d => d.id === passcodeDomainId)?.passcodeProtected && (
              <button
                onClick={() => passcodeDomainId && removePasscode(passcodeDomainId)}
                disabled={passSaving || passRemoving}
                className="px-4 py-2 text-sm font-medium text-red-400 hover:text-white hover:bg-red-500/10 rounded-lg transition-colors disabled:opacity-50"
              >
                {passRemoving ? 'Removing...' : 'Remove Passcode'}
              </button>
            )}

            {/* Right side: Cancel + Save */}
            <div className="ml-auto flex items-center gap-3">
              <button onClick={closePasscodeModal} className="px-4 py-2 text-sm font-medium text-slate-300 hover:text-white hover:bg-slate-800 rounded-lg transition-colors">
                Cancel
              </button>
              <button
                onClick={savePasscode}
                disabled={passSaving || passRemoving}
                className="px-4 py-2 text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 rounded-lg transition-colors disabled:opacity-50"
              >
                {passSaving ? 'Saving...' : 'Save Passcode'}
              </button>
            </div>
          </div>
        </div>
      </Modal>

      {/* Custom Domain Modal */}
      <Modal
        isOpen={!!customDomainDomainId}
        onClose={() => setCustomDomainDomainId(null)}
        title="Custom Domain"
      >
        <div className="space-y-4">
          <div>
            <p className="text-sm text-slate-400 mb-4">
              Bind your own custom domain (e.g., <code className="text-indigo-400">app.mycompany.com</code>) to your Port Buddy subdomain.
              Ensure you have a CNAME record pointing to your subdomain first.
            </p>
            <label className="block text-sm text-slate-300 mb-1">Custom Domain</label>
            <input
              type="text"
              value={customDomainValue}
              onChange={e => setCustomDomainValue(e.target.value)}
              className="w-full bg-slate-800 border border-slate-700 rounded px-3 py-2 text-white focus:outline-none focus:border-indigo-500"
              placeholder="e.g. app.mycompany.com"
            />
          </div>
          <div className="flex items-center justify-between gap-3 pt-2">
            {domains.find(d => d.id === customDomainDomainId)?.customDomain && (
              <button
                onClick={handleCustomDomainRemove}
                disabled={customDomainSaving || customDomainRemoving}
                className="px-4 py-2 text-sm font-medium text-red-400 hover:text-white hover:bg-red-500/10 rounded-lg transition-colors disabled:opacity-50"
              >
                {customDomainRemoving ? 'Removing...' : 'Remove Custom Domain'}
              </button>
            )}
            <div className="ml-auto flex items-center gap-3">
              <button
                onClick={() => setCustomDomainDomainId(null)}
                className="px-4 py-2 text-sm font-medium text-slate-300 hover:text-white hover:bg-slate-800 rounded-lg transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={handleCustomDomainSave}
                disabled={customDomainSaving || customDomainRemoving}
                className="px-4 py-2 text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 rounded-lg transition-colors disabled:opacity-50"
              >
                {customDomainSaving ? 'Saving...' : 'Save Custom Domain'}
              </button>
            </div>
          </div>
        </div>
      </Modal>

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
          <div className="grid gap-4">
              {[1, 2].map(i => (
                  <div key={i} className="bg-slate-900/50 border border-slate-800 rounded-xl p-6 h-24 animate-pulse"></div>
              ))}
          </div>
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
                              {domain.customDomain && (
                                <div className="mt-3 flex flex-col gap-2">
                                  <div className="flex items-center gap-2">
                                    <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">Custom Domain:</span>
                                    <span className="text-sm text-indigo-400 font-mono">{domain.customDomain}</span>
                                    {domain.cnameVerified ? (
                                      domain.sslActive ? (
                                        <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-green-400/10 text-green-400" title="CNAME is verified and SSL certificate is active.">
                                          <CheckIcon className="w-3 h-3 mr-1" />
                                          Verified & SSL Active
                                        </span>
                                      ) : (
                                        <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-green-400/10 text-green-400" title="CNAME is verified. SSL certificate is being provisioned.">
                                          <CheckIcon className="w-3 h-3 mr-1" />
                                          Verified. SSL Provisioning...
                                        </span>
                                      )
                                    ) : (
                                      <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-yellow-400/10 text-yellow-400">
                                        Pending Verification
                                      </span>
                                    )}
                                  </div>
                                  {!domain.cnameVerified && (
                                    <button
                                      onClick={() => handleVerifyCname(domain.id)}
                                      disabled={verifyingCname}
                                      className="text-xs text-indigo-400 hover:text-indigo-300 flex items-center gap-1 transition-colors disabled:opacity-50"
                                    >
                                      {verifyingCname ? (
                                        <div className="w-3 h-3 border border-indigo-400/30 border-t-indigo-400 rounded-full animate-spin" />
                                      ) : (
                                        <CheckIcon className="w-3 h-3" />
                                      )}
                                      Verify CNAME & Issue SSL
                                    </button>
                                  )}
                                </div>
                              )}
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
                                    onClick={() => handleCustomDomainClick(domain)}
                                    className="p-2 text-slate-400 hover:text-indigo-400 hover:bg-indigo-400/10 rounded-lg transition-colors"
                                    title="Custom Domain"
                                  >
                                    <GlobeAltIcon className="w-5 h-5" />
                                  </button>
                                  <button
                                      onClick={() => handleEditStart(domain)}
                                      className="p-2 text-slate-400 hover:text-indigo-400 hover:bg-indigo-400/10 rounded-lg transition-colors"
                                      title="Edit Subdomain"
                                  >
                                      <PencilIcon className="w-5 h-5" />
                                  </button>
                                  {/* Passcode control icon */}
                                  <button
                                    onClick={() => openSetPasscode(domain.id)}
                                    className={`p-2 rounded-lg transition-colors ${domain.passcodeProtected
                                      ? 'text-green-400 hover:bg-green-400/10'
                                      : 'text-slate-400 hover:text-indigo-400 hover:bg-indigo-400/10'}`}
                                    title={domain.passcodeProtected ? 'Change passcode' : 'Set passcode'}
                                    aria-label={domain.passcodeProtected ? 'Change passcode' : 'Set passcode'}
                                  >
                                    {domain.passcodeProtected ? (
                                      <LockClosedIcon className="w-5 h-5" />
                                    ) : (
                                      <LockOpenIcon className="w-5 h-5" />
                                    )}
                                  </button>
                                  <button
                                      onClick={() => handleDeleteClick(domain.id)}
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
