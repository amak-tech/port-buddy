import React from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export default function ProtectedRoute({ children }: { children: React.ReactNode }) {
    const { user, loading } = useAuth()
    const location = useLocation()

    if (loading) {
        return (
            <div className="container py-16">
                <div className="text-white/70">Loading...</div>
            </div>
        )
    }
    if (!user) {
        return <Navigate to="/login" replace state={{ from: location }} />
    }
    return <>{children}</>
}
