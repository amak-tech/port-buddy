import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { API_BASE, apiJson, getToken } from '../lib/api'

export type User = {
    id: string
    email: string
    name?: string
    avatarUrl?: string
    roles?: string[]
    plan?: 'pro' | 'team'
}

type AuthState = {
    user: User | null
    loading: boolean
    loginWithGoogle: () => void
    loginWithEmail: (email: string, pass: string) => Promise<void>
    logout: () => Promise<void>
    refresh: () => Promise<void>
}

const AuthContext = createContext<AuthState | undefined>(undefined)

const APP_ORIGIN = window.location.origin
const OAUTH_REDIRECT_URI = `${APP_ORIGIN}/auth/callback`

function storeTokenFromUrlIfPresent(): string | null {
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

        if (!getToken()) {
            setUser(null)
            setLoading(false)
            return
        }

        try {
            const details = await apiJson<{
                user: { id: string, email: string, firstName?: string, lastName?: string, avatarUrl?: string, roles?: string[] }
                account?: { plan?: string }
            }>('/api/users/me/details', undefined, { skipRedirectOn401: true })

            const firstName = details?.user?.firstName?.trim() || ''
            const lastName = details?.user?.lastName?.trim() || ''
            const name = [firstName, lastName].filter(Boolean).join(' ') || undefined

            // Map server response to SPA User shape
            const mapped: User = {
                id: details.user.id,
                email: details.user.email,
                name,
                avatarUrl: details.user.avatarUrl || undefined,
                roles: details.user.roles,
                // Keep plan optional; server plans may not match current union type
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

    const loginWithEmail = useCallback(async (email: string, pass: string) => {
        const res = await apiJson<{ accessToken: string, tokenType: string }>('/api/auth/login', {
            method: 'POST',
            body: JSON.stringify({ email, password: pass })
        }, { skipAuth: true })
        localStorage.setItem('pb_token', res.accessToken)
        await refresh()
    }, [refresh])

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

    const value = useMemo<AuthState>(() => ({ user, loading, loginWithGoogle, loginWithEmail, logout, refresh }), [user, loading, loginWithGoogle, loginWithEmail, logout, refresh])

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
