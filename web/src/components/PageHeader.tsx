import { createContext, PropsWithChildren, useContext, useEffect, useMemo, useState } from 'react'

type PageHeaderContextValue = {
    title: string
    setTitle: (title: string) => void
}

const PageHeaderContext = createContext<PageHeaderContextValue | undefined>(undefined)

export function PageHeaderProvider({ children }: PropsWithChildren) {
    const [title, setTitle] = useState<string>('')
    const value = useMemo(() => ({ title, setTitle }), [title])
    return (
        <PageHeaderContext.Provider value={value}>
            {children}
        </PageHeaderContext.Provider>
    )
}

export function usePageHeader() {
    const ctx = useContext(PageHeaderContext)
    if (!ctx) throw new Error('usePageHeader must be used within PageHeaderProvider')
    return ctx
}

export function usePageTitle(title: string) {
    const { setTitle } = usePageHeader()
    useEffect(() => {
        setTitle(title)
    }, [setTitle, title])
}
