/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

import { Link } from 'react-router-dom'
import { CheckCircleIcon } from '@heroicons/react/24/outline'
import { usePageTitle } from '../../components/PageHeader'

export default function BillingSuccess() {
  usePageTitle('Payment Successful')

  return (
    <div className="max-w-2xl mx-auto pt-12 px-4 text-center">
      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-8 md:p-12 shadow-xl">
        <div className="w-20 h-20 bg-green-500/10 rounded-full flex items-center justify-center mx-auto mb-8">
          <CheckCircleIcon className="w-12 h-12 text-green-500" />
        </div>
        
        <h1 className="text-3xl font-bold text-white mb-4">Payment Successful!</h1>
        <p className="text-slate-400 text-lg mb-10">
          Thank you for your purchase. Your subscription has been updated successfully.
          It might take a few moments for the changes to reflect in your account.
        </p>

        <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
          <Link
            to="/app"
            className="w-full sm:w-auto px-8 py-3 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg font-semibold transition-all"
          >
            Return to Dashboard
          </Link>
          <Link
            to="/app/billing"
            className="w-full sm:w-auto px-8 py-3 bg-slate-800 hover:bg-slate-700 text-white rounded-lg font-semibold transition-all border border-slate-700"
          >
            View Billing
          </Link>
        </div>
      </div>
    </div>
  )
}
