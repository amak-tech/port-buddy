import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { jwtDecode } from 'jwt-decode'
import { API_BASE, apiJson, getToken } from '../lib/api'

export type User = {
    id: string
    accountId: string
    email: string
    name?: string
    avatarUrl?: string
    roles?: string[]
    plan?: 'pro' | 'team'
    accountName?: string
    extraTunnels?: number
    baseTunnels?: number
    activeTunnels?: number
    subscriptionStatus?: string
    stripeCustomerId?: string
}

const ACCOUNT_NAME_CLAIM = 'aname'
const ACCOUNT_ID_CLAIM = 'aid'

type AuthState = {
    user: User | null
    loading: boolean
    loginWithGoogle: () => void
    loginWithGithub: () => void
    loginWithEmail: (email: string, pass: string) => Promise<void>
    register: (email: string, pass: string, name?: string) => Promise<void>
    logout: () => Promise<void>
    refresh: () => Promise<void>
    switchAccount: (accountId: string) => Promise<void>
}

const AuthContext = createContext<AuthState | undefined>(undefined)

const APP_ORIGIN = window.location.origin
const OAUTH_REDIRECT_URI = `${APP_ORIGIN}/auth/callback`

function storeTokenFromUrlIfPresent(): string | null {
    // Only capture token if we are on the auth callback path or similar
    // to avoid stealing tokens from other pages (like /accept-invite?token=...)
    if (!window.location.pathname.startsWith('/auth/callback') && !window.location.pathname.startsWith('/login')) {
        return localStorage.getItem('pb_token')
    }

    // Support either hash or query param token from backend callback, e.g. /auth/callback#token=... or ?token=...
    const hash = new URLSearchParams(window.location.hash.replace(/^#/, ''))
    const query = new URLSearchParams(window.location.search)
    const token = hash.get('token') || query.get('token')
    if (token) {
        localStorage.setItem('pb_token', token)
        // Clean URL
        const url = new URL(window.location.href)
        url.hash = ''
        url.searchParams.delete('token')
        window.history.replaceState({}, '', url.toString())
        return token
    }
    return localStorage.getItem('pb_token')
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
    const [user, setUser] = useState<User | null>(null)
    const [loading, setLoading] = useState(true)

    const refresh = useCallback(async () => {
        setLoading(true)

        const token = getToken()
        if (!token) {
            setUser(null)
            setLoading(false)
            return
        }

        const decoded: any = jwtDecode(token)
        const currentUserId = decoded.uid
        const currentAccountName = decoded[ACCOUNT_NAME_CLAIM]
        const currentAccountId = decoded[ACCOUNT_ID_CLAIM]

        try {
            const details = await apiJson<{
                user: { id: string, email: string, firstName?: string, lastName?: string, avatarUrl?: string, roles?: string[] }
                account?: { name?: string, plan?: string, extraTunnels?: number, baseTunnels?: number, activeTunnels?: number, subscriptionStatus?: string, stripeCustomerId?: string }
            }>('/api/users/me/details', undefined, { skipRedirectOn401: true })

            const firstName = details?.user?.firstName?.trim() || ''
            const lastName = details?.user?.lastName?.trim() || ''
            const name = [firstName, lastName].filter(Boolean).join(' ') || undefined

            // Map server response to SPA User shape
            const mapped: User = {
                id: details.user.id,
                accountId: currentAccountId,
                email: details.user.email,
                name,
                avatarUrl: details.user.avatarUrl || undefined,
                roles: details.user.roles,
                plan: details.account?.plan?.toLowerCase() as any,
                accountName: details.account?.name || currentAccountName,
                extraTunnels: details.account?.extraTunnels,
                baseTunnels: details.account?.baseTunnels,
                activeTunnels: details.account?.activeTunnels,
                subscriptionStatus: details.account?.subscriptionStatus,
                stripeCustomerId: details.account?.stripeCustomerId,
            }
            setUser(mapped)
        } catch (e: any) {
            if (e.status === 401) {
                try {
                    localStorage.removeItem('pb_token')
                } catch (_) {
                    // ignore
                }
            }
            setUser(null)
        } finally {
            setLoading(false)
        }
    }, [])

    useEffect(() => {
        // On first load, capture token if backend sent it in URL and then fetch profile
        storeTokenFromUrlIfPresent()
        void refresh()
    }, [refresh])

    const loginWithGoogle = useCallback(() => {
        const redirect = encodeURIComponent(OAUTH_REDIRECT_URI)
        // Typical Spring Security OAuth2 endpoint
        const url = `${API_BASE}/oauth2/authorization/google?redirect_uri=${redirect}`
        window.location.href = url
    }, [])
    
    const loginWithGithub = useCallback(() => {
        const redirect = encodeURIComponent(OAUTH_REDIRECT_URI)
        // Typical Spring Security OAuth2 endpoint
        const url = `${API_BASE}/oauth2/authorization/github?redirect_uri=${redirect}`
        window.location.href = url
    }, [])

    const loginWithEmail = useCallback(async (email: string, pass: string) => {
        const res = await apiJson<{ accessToken: string, tokenType: string }>('/api/auth/login', {
            method: 'POST',
            body: JSON.stringify({ email, password: pass })
        }, { skipAuth: true })
        localStorage.setItem('pb_token', res.accessToken)
        await refresh()
    }, [refresh])

    const register = useCallback(async (email: string, pass: string, name?: string) => {
        const res = await apiJson<{ success: boolean, message?: string }>('/api/auth/register', {
            method: 'POST',
            body: JSON.stringify({ email, password: pass, name })
        }, { skipAuth: true })
        
        if (!res.success) {
            throw new Error(res.message || 'Registration failed')
        }
        
        await loginWithEmail(email, pass)
    }, [loginWithEmail])

    const logout = useCallback(async () => {
        // Stateless logout: just drop the JWT from localStorage client-side
        try {
            localStorage.removeItem('pb_token')
        } catch (_) {
            // ignore storage errors
        }
        setUser(null)
        // Redirect to landing page after logout
        window.location.assign('/')
    }, [])

    const switchAccount = useCallback(async (accountId: string) => {
        try {
            const res = await apiJson<{ token: string }>(`/api/users/me/accounts/${accountId}/switch`, {
                method: 'POST'
            })
            localStorage.setItem('pb_token', res.token)
            await refresh()
        } catch (e) {
            console.error('Failed to switch account', e)
            throw e
        }
    }, [refresh])

    const value = useMemo<AuthState>(() => ({ 
        user, 
        loading, 
        loginWithGoogle, 
        loginWithGithub,
        loginWithEmail, 
        register,
        logout, 
        refresh,
        switchAccount
    }), [user, loading, loginWithGoogle, loginWithGithub, loginWithEmail, register, logout, refresh, switchAccount])

    return (
        <AuthContext.Provider value={value}>
            {children}
        </AuthContext.Provider>
    )
}

export function useAuth(): AuthState {
    const ctx = useContext(AuthContext)
    if (!ctx) throw new Error('useAuth must be used within AuthProvider')
    return ctx
}
