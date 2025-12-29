import React from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export default function ProtectedRoute({ children, role }: { children: React.ReactNode, role?: string }) {
    const { user, loading } = useAuth()
    const location = useLocation()

    if (loading) {
        return (
            <div className="min-h-screen flex bg-slate-950">
                {/* Sidebar shell */}
                <aside className="fixed top-0 left-0 h-screen w-64 border-r border-slate-800 bg-slate-900">
                    <div className="h-full flex flex-col">
                        <div className="border-b border-slate-800 px-6 py-5">
                            <div className="flex items-center gap-3 text-lg font-bold text-white/50">
                                <span className="h-3 w-3 rounded-full bg-slate-700"></span>
                                Port Buddy
                            </div>
                        </div>
                    </div>
                </aside>
                {/* Main shell */}
                <section className="flex-1 ml-64 bg-slate-950 flex flex-col">
                    <div className="h-16 border-b border-slate-800"></div>
                    <div className="p-8 flex-1">
                        <div className="max-w-4xl h-64 bg-slate-900/50 rounded-2xl animate-pulse"></div>
                    </div>
                </section>
            </div>
        )
    }
    if (!user) {
        return <Navigate to="/login" replace state={{ from: location }} />
    }
    if (role && !user.roles?.includes(role)) {
        return <Navigate to="/app" replace />
    }
    return <>{children}</>
}
