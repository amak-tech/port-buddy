import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'

export type User = {
    id: string
    email: string
    name?: string
    avatarUrl?: string
    plan?: 'basic' | 'individual' | 'professional'
}

type AuthState = {
    user: User | null
    loading: boolean
    loginWithGoogle: () => void
    logout: () => Promise<void>
    refresh: () => Promise<void>
}

const AuthContext = createContext<AuthState | undefined>(undefined)

const API_BASE = ((): string => {
    // 1) Environment (Vite build-time)
    const env = import.meta.env.VITE_API_BASE?.toString()
    if (env) return env
    // 2) Dev fallback: if app runs on 5173 (Vite), talk to Spring Boot on 8080
    if (window.location.hostname === 'localhost' && window.location.port === '5173') {
        return 'http://localhost:8080'
    }
    // 3) Same-origin by default
    return ''
})()
const APP_ORIGIN = window.location.origin
const OAUTH_REDIRECT_URI = `${APP_ORIGIN}/auth/callback`

async function fetchJson(path: string, init?: RequestInit) {
    const res = await fetch(`${API_BASE}${path}`, {
        credentials: 'include',
        headers: { 'Content-Type': 'application/json', ...(init?.headers || {}) },
        ...init,
    })
    if (!res.ok) {
        const text = await res.text().catch(() => '')
        throw new Error(text || `HTTP ${res.status}`)
    }
    return res.json()
}

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
        try {
            // Attach token header if stored (for JWT-based APIs). Server may also use HttpOnly cookies; we always send credentials.
            const token = localStorage.getItem('pb_token')
            const headers: Record<string, string> = {}
            if (token) headers['Authorization'] = `Bearer ${token}`
            const me = await fetchJson('/api/auth/me', { headers })
            setUser(me as User)
        } catch (e) {
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

    const logout = useCallback(async () => {
        try {
            await fetch(`${API_BASE}/api/auth/logout`, { method: 'POST', credentials: 'include' })
        } catch (_) {
            // ignore network errors on logout
        }
        localStorage.removeItem('pb_token')
        setUser(null)
        // Redirect to landing page after logout
        window.location.assign('/')
    }, [])

    const value = useMemo<AuthState>(() => ({ user, loading, loginWithGoogle, logout, refresh }), [user, loading, loginWithGoogle, logout, refresh])

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
