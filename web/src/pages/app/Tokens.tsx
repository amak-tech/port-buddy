import { useEffect, useMemo, useState } from 'react'
import { apiJson, apiRaw } from '../../lib/api'
import { useAuth } from '../../auth/AuthContext'
import { usePageTitle } from '../../components/PageHeader'
import { ConfirmModal } from '../../components/Modal'
import { 
  KeyIcon, 
  TrashIcon, 
  ClipboardDocumentIcon, 
  CheckIcon,
  PlusIcon 
} from '@heroicons/react/24/outline'

type TokenItem = { id: string, label: string, createdAt: string, revoked: boolean, lastUsedAt?: string }

export default function Tokens() {
  const { user } = useAuth()
  usePageTitle('Access Tokens')
  const hasUser = useMemo(() => !!user, [user])
  const [tokens, setTokens] = useState<TokenItem[]>([])
  const [loading, setLoading] = useState(false)
  const [newLabel, setNewLabel] = useState('cli')
  const [justCreatedToken, setJustCreatedToken] = useState<string | null>(null)
  
  // Dialog state
  const [revokeId, setRevokeId] = useState<string | null>(null)

  useEffect(() => {
    if (!hasUser) return
    void loadTokens()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hasUser])

  async function loadTokens() {
    setLoading(true)
    try {
      const list = await apiJson<TokenItem[]>('/api/tokens')
      setTokens(list)
    } catch {
      setTokens([])
    } finally {
      setLoading(false)
    }
  }

  async function createToken() {
    setLoading(true)
    try {
      const resp = await apiJson<{ token: string }>('/api/tokens', { method: 'POST', body: JSON.stringify({ label: newLabel || 'cli' }) })
      setJustCreatedToken(resp.token as string)
      await loadTokens()
    } catch {
      // noop
    } finally {
      setLoading(false)
    }
  }

  async function revokeToken(id: string) {
    setLoading(true)
    try {
      await apiRaw(`/api/tokens/${id}`, { method: 'DELETE' })
      await loadTokens()
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-4xl">
      <ConfirmModal
        isOpen={!!revokeId}
        onClose={() => setRevokeId(null)}
        onConfirm={() => {
            if (revokeId) void revokeToken(revokeId)
        }}
        title="Revoke Token"
        message="Are you sure you want to revoke this token? This action cannot be undone and any applications using this token will lose access."
        confirmText="Revoke"
        isDangerous
      />

      <div className="mb-8">
        <h2 className="text-2xl font-bold text-white">Access Tokens</h2>
        <p className="text-slate-400 mt-1">Generate and manage API tokens to authenticate the CLI.</p>
      </div>

      <div className="bg-slate-900/50 border border-slate-800 rounded-xl p-6 mb-8 shadow-lg">
        <h3 className="text-lg font-medium text-white mb-4">Generate New Token</h3>
        <div className="flex flex-col md:flex-row gap-4 items-start md:items-end">
          <div className="flex-1 w-full">
            <label className="block text-sm font-medium text-slate-300 mb-2">Token Label</label>
            <input 
              value={newLabel} 
              onChange={(e) => setNewLabel(e.target.value)} 
              className="block w-full bg-slate-950 border border-slate-800 rounded-lg py-2.5 px-4 text-slate-200 placeholder-slate-600 focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500 transition-all" 
              placeholder="e.g. MacBook Pro" 
            />
          </div>
          <button 
            className="w-full md:w-auto px-6 py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg font-medium transition-all flex items-center justify-center gap-2 shadow-lg shadow-indigo-500/20 disabled:opacity-50 disabled:cursor-not-allowed"
            onClick={() => { void createToken() }} 
            disabled={loading}
          >
            <PlusIcon className="w-5 h-5" />
            Generate
          </button>
        </div>

        {justCreatedToken && (
          <div className="mt-6 p-4 bg-emerald-500/10 border border-emerald-500/20 rounded-lg animate-in fade-in slide-in-from-top-2">
            <div className="flex items-start justify-between gap-4">
              <div>
                <div className="text-emerald-400 font-medium mb-1">Token generated successfully</div>
                <div className="text-emerald-400/70 text-sm mb-3">Make sure to copy your token now. You won't be able to see it again!</div>
              </div>
              <button className="text-emerald-400 hover:text-emerald-300" onClick={() => setJustCreatedToken(null)}>
                <span className="sr-only">Dismiss</span>
                <svg className="w-5 h-5" viewBox="0 0 20 20" fill="currentColor"><path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd" /></svg>
              </button>
            </div>
            
            <div className="relative group">
              <div className="bg-slate-950 border border-slate-800 rounded-lg p-3 font-mono text-sm text-emerald-300 break-all pr-12">
                {justCreatedToken}
              </div>
              <CopyButton text={justCreatedToken} />
            </div>
            
            <div className="mt-4 p-3 bg-slate-900/50 rounded border border-slate-800 text-slate-400 text-xs font-mono">
               port-buddy init {justCreatedToken}
            </div>
          </div>
        )}
      </div>

      <div className="space-y-4">
        {loading && tokens.length === 0 ? (
          <div className="text-slate-500 text-sm">Loading tokens...</div>
        ) : tokens.length === 0 ? (
          <div className="bg-slate-900/50 border border-slate-800 rounded-xl p-12 text-center">
            <div className="inline-flex items-center justify-center w-12 h-12 rounded-full bg-slate-800 text-slate-400 mb-4">
              <KeyIcon className="w-6 h-6" />
            </div>
            <h3 className="text-lg font-medium text-white mb-2">No tokens yet</h3>
            <p className="text-slate-400">Generate your first token to start using the CLI.</p>
          </div>
        ) : (
          <div className="grid gap-4">
            {tokens.map(t => (
              <div key={t.id} className="group bg-slate-900/50 border border-slate-800 rounded-xl p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4 hover:border-slate-700 transition-all">
                <div className="flex items-start gap-4">
                  <div className={`p-2.5 rounded-lg ${t.revoked ? 'bg-red-500/10 text-red-400' : 'bg-indigo-500/10 text-indigo-400'}`}>
                    <KeyIcon className="w-6 h-6" />
                  </div>
                  <div>
                    <div className="flex items-center gap-3 mb-1">
                      <div className="font-semibold text-white text-lg">{t.label}</div>
                      {t.revoked && <span className="px-2 py-0.5 rounded text-xs font-medium bg-red-500/10 text-red-400 border border-red-500/20">Revoked</span>}
                    </div>
                    <div className="text-slate-500 text-sm flex flex-col sm:flex-row sm:items-center gap-1 sm:gap-3">
                      <span>Created {new Date(t.createdAt).toLocaleDateString()}</span>
                      {t.lastUsedAt && (
                        <>
                          <span className="hidden sm:inline">•</span>
                          <span>Last used {new Date(t.lastUsedAt).toLocaleDateString()}</span>
                        </>
                      )}
                    </div>
                  </div>
                </div>
                
                {!t.revoked && (
                  <button 
                    className="self-end sm:self-center px-4 py-2 text-sm font-medium text-red-400 hover:text-red-300 hover:bg-red-500/10 rounded-lg border border-transparent hover:border-red-500/20 transition-all flex items-center gap-2 opacity-100 sm:opacity-0 sm:group-hover:opacity-100 focus:opacity-100"
                    onClick={() => setRevokeId(t.id)}
                  >
                    <TrashIcon className="w-4 h-4" />
                    Revoke
                  </button>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

function CopyButton({ text }: { text: string }) {
  const [copied, setCopied] = useState(false)
  const copy = () => {
    navigator.clipboard.writeText(text)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }
  return (
    <button 
      onClick={copy}
      className="absolute top-2 right-2 p-1.5 rounded-md bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-white transition-all"
      title="Copy to clipboard"
    >
      {copied ? <CheckIcon className="w-4 h-4 text-emerald-400" /> : <ClipboardDocumentIcon className="w-4 h-4" />}
    </button>
  )
}
