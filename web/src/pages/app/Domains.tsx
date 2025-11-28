import { useAuth } from '../../auth/AuthContext'
import { usePageTitle } from '../../components/PageHeader'
import { GlobeAltIcon, LockClosedIcon, CheckBadgeIcon } from '@heroicons/react/24/outline'

export default function Domains() {
  const { user } = useAuth()
  usePageTitle('Domains')
  const plan = user?.plan || 'basic'

  return (
    <div className="max-w-5xl">
      <div className="mb-8">
        <h2 className="text-2xl font-bold text-white">Domains</h2>
        <p className="text-slate-400 mt-1">Manage your custom domains and static subdomains.</p>
      </div>

      <div className="grid md:grid-cols-2 gap-6 mb-8">
        <div className="bg-slate-900/50 border border-slate-800 rounded-xl p-6 relative overflow-hidden group hover:border-indigo-500/50 transition-all">
           <div className="absolute top-0 right-0 p-4 opacity-10 group-hover:opacity-20 transition-opacity">
             <GlobeAltIcon className="w-24 h-24 text-indigo-500 rotate-12" />
           </div>
           <div className="relative">
             <div className="text-sm font-medium text-slate-400 mb-1">Current Plan</div>
             <div className="text-2xl font-bold text-white capitalize flex items-center gap-2">
               {plan}
               {plan === 'professional' && <CheckBadgeIcon className="w-6 h-6 text-indigo-400" />}
             </div>
             <div className="mt-4 flex flex-col gap-2">
               <div className="flex items-center gap-2 text-sm text-slate-300">
                 <span className="w-1.5 h-1.5 rounded-full bg-indigo-500"></span>
                 {plan === 'basic' ? '2 Static Subdomains' : plan === 'individual' ? '5 Static Subdomains' : '10 Static Subdomains'}
               </div>
               <div className="flex items-center gap-2 text-sm text-slate-300">
                 <span className="w-1.5 h-1.5 rounded-full bg-indigo-500"></span>
                 {plan === 'basic' ? 'No Custom Domains' : '1 Custom Domain'}
               </div>
             </div>
           </div>
        </div>

        <div className="bg-slate-900/50 border border-slate-800 rounded-xl p-6 relative overflow-hidden group hover:border-indigo-500/50 transition-all">
           <div className="absolute top-0 right-0 p-4 opacity-10 group-hover:opacity-20 transition-opacity">
             <LockClosedIcon className="w-24 h-24 text-indigo-500 -rotate-12" />
           </div>
           <div className="relative">
             <div className="text-sm font-medium text-slate-400 mb-1">App Domain</div>
             <div className="text-2xl font-bold text-white font-mono">portbuddy.dev</div>
             <div className="mt-4 text-sm text-slate-400 leading-relaxed">
               All tunnels are automatically secured with wildcard SSL certificates on our main domain.
             </div>
           </div>
        </div>
      </div>

      <div className="bg-slate-900/50 border border-slate-800 rounded-xl p-12 text-center">
        <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-slate-800/50 text-indigo-400 mb-6 ring-1 ring-white/10">
          <GlobeAltIcon className="w-8 h-8" />
        </div>
        <h3 className="text-xl font-bold text-white mb-3">Domain Management Coming Soon</h3>
        <p className="text-slate-400 max-w-md mx-auto mb-8">
          We're putting the finishing touches on our domain management system. 
          Soon you'll be able to bring your own domains and reserve static subdomains directly from here.
        </p>
        
        <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-indigo-500/10 border border-indigo-500/20 text-indigo-300 text-sm font-medium">
          <span className="relative flex h-2 w-2">
            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-indigo-400 opacity-75"></span>
            <span className="relative inline-flex rounded-full h-2 w-2 bg-indigo-500"></span>
          </span>
          Expected Q4 2025
        </div>
      </div>
    </div>
  )
}
