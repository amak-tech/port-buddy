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

function withAuth(init?: RequestInit): RequestInit {
    const token = getToken()
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

export async function apiJson<T = any>(path: string, init?: RequestInit): Promise<T> {
    const res = await fetch(`${API_BASE}${path}`, withAuth({
        headers: { 'Content-Type': 'application/json', ...(init?.headers || {}) },
        ...init,
    }))
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
    return fetch(`${API_BASE}${path}`, withAuth(init))
}
