/*
 * Copyright (c) 2026 AMAK Inc. All rights reserved.
 */

import Seo from '../components/Seo'

export default function Privacy() {
  return (
    <div className="min-h-screen flex flex-col">
      <Seo 
        title="Privacy Policy - Port Buddy"
        description="Learn about how Port Buddy collects, uses, and protects your personal data."
      />
      <div className="flex-1 relative pt-32 pb-20 px-4">
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-indigo-900/10 via-slate-900/0 to-slate-900/0 pointer-events-none" />
        
        <div className="container mx-auto max-w-3xl relative z-10">
          <h1 className="text-4xl font-bold text-white mb-8">Privacy Policy</h1>
          
          <div className="prose prose-invert max-w-none text-slate-300 space-y-6">
            <section>
              <h2 className="text-2xl font-semibold text-white mb-4">1. Information We Collect</h2>
              <p>
                We collect information you provide directly to us when you create an account, such as your name, email address, and authentication details from Google or GitHub.
              </p>
              <p>
                We also collect technical data related to your use of our service, including IP addresses, browser type, and usage statistics of the tunnels you create.
              </p>
            </section>

            <section>
              <h2 className="text-2xl font-semibold text-white mb-4">2. How We Use Your Information</h2>
              <p>
                We use the information we collect to:
              </p>
              <ul className="list-disc pl-6 space-y-2">
                <li>Provide, maintain, and improve our services.</li>
                <li>Process transactions and manage your subscription.</li>
                <li>Communicate with you about updates, security alerts, and support.</li>
                <li>Monitor and analyze trends, usage, and activities.</li>
              </ul>
            </section>

            <section>
              <h2 className="text-2xl font-semibold text-white mb-4">3. Data Sharing and Disclosure</h2>
              <p>
                We do not share your personal information with third parties except as described in this policy:
              </p>
              <ul className="list-disc pl-6 space-y-2">
                <li>With your consent or at your direction.</li>
                <li>With vendors and service providers who perform services for us (e.g., payment processing).</li>
                <li>To comply with legal obligations.</li>
                <li>To protect the rights and safety of Port Buddy and our users.</li>
              </ul>
            </section>

            <section>
              <h2 className="text-2xl font-semibold text-white mb-4">4. Data Security</h2>
              <p>
                We take reasonable measures to protect your personal information from loss, theft, misuse, and unauthorized access. However, no internet transmission is 100% secure.
              </p>
            </section>

            <section>
              <h2 className="text-2xl font-semibold text-white mb-4">5. Your Choices</h2>
              <p>
                You can access, update, or delete your account information at any time through your account settings. You may also contact us to request data deletion.
              </p>
            </section>

            <section>
              <h2 className="text-2xl font-semibold text-white mb-4">6. Cookies</h2>
              <p>
                We use cookies and similar technologies to enhance your experience and collect usage data. You can manage cookie preferences through your browser settings.
              </p>
            </section>

            <footer className="pt-8 border-t border-slate-800 text-sm text-slate-500">
              Last updated: January 8, 2026
            </footer>
          </div>
        </div>
      </div>
    </div>
  )
}
