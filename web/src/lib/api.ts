// Centralized API client that attaches Authorization: Bearer <JWT>
// and avoids sending cookies. Works in dev and prod using VITE_API_BASE.

export const API_BASE: string = (() => {
    const env = (import.meta as any).env?.VITE_API_BASE?.toString()
    if (env) return env
    if (window.location.hostname === 'localhost' && window.location.port === '5173') {
        return 'http://localhost:8080'
    }
    return '' // same-origin in production
})()

export function getToken(): string | null {
    try {
        return localStorage.getItem('pb_token')
    } catch {
        return null
    }
}

function redirectToLogin(withFrom: boolean = true): void {
    try {
        localStorage.removeItem('pb_token')
    } catch {
        // ignore
    }

    const { pathname, search, hash } = window.location
    const here = `${pathname}${search}${hash}`
    const onLogin = pathname.startsWith('/login') || pathname.startsWith('/auth/callback')

    // Prevent multi-trigger redirects
    const flag = '__pbRedirectingToLogin'
    if ((window as any)[flag]) return
    ;(window as any)[flag] = true

    // If we are already on a login-related page, do not navigate again to avoid reload loops
    if (onLogin) {
        return
    }

    const to = !withFrom
        ? '/login'
        : `/login?from=${encodeURIComponent(here)}`
    // Use assign to create a fresh navigation (clears any stale protected UI)
    window.location.assign(to)
}

function withAuth(init?: RequestInit, skipToken: boolean = false): RequestInit {
    const token = !skipToken ? getToken() : null
    const headers: Record<string, string> = {
        ...(init?.headers as Record<string, string> | undefined),
    }
    if (token) {
        headers['Authorization'] = `Bearer ${token}`
    }
    return {
        ...init,
        // Explicitly avoid sending cookies for stateless JWT API
        credentials: 'omit',
        headers,
    }
}

export async function apiJson<T = any>(path: string, init?: RequestInit, options?: { skipRedirectOn401?: boolean, skipAuth?: boolean }): Promise<T> {
    const res = await fetch(`${API_BASE}${path}`, withAuth({
        headers: { 'Content-Type': 'application/json', ...(init?.headers || {}) },
        ...init,
    }, options?.skipAuth))
    if (res.status === 401) {
        if (!options?.skipRedirectOn401) {
            redirectToLogin(true)
        }
        // Throw to stop any further processing by callers
        const err: any = new Error('Unauthorized')
        err.status = 401
        throw err
    }
    if (!res.ok) {
        const text = await res.text().catch(() => '')
        const err: any = new Error(text || `HTTP ${res.status}`)
        err.status = res.status
        throw err
    }
    // 204 No Content has no body
    if (res.status === 204) return undefined as unknown as T
    return res.json() as Promise<T>
}

export async function apiRaw(path: string, init?: RequestInit): Promise<Response> {
    const res = await fetch(`${API_BASE}${path}`, withAuth(init))
    if (res.status === 401) {
        redirectToLogin(true)
        // Throw to ensure callers do not proceed under unauthorized state
        const err: any = new Error('Unauthorized')
        err.status = 401
        throw err
    }
    return res
}
