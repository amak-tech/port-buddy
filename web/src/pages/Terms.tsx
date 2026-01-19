import Seo from '../components/Seo'

export default function Terms() {
  return (
    <div className="min-h-screen flex flex-col">
      <Seo 
        title="Terms and Conditions - Port Buddy"
        description="Read the terms and conditions for using Port Buddy services."
        path="/terms"
      />
      <div className="flex-1 relative pt-12 md:pt-32 pb-20">
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-indigo-900/10 via-slate-900/0 to-slate-900/0 pointer-events-none" />
        
        <div className="container max-w-3xl relative z-10">
          <h1 className="text-4xl font-bold text-white mb-8">Terms and Conditions</h1>
          
          <div className="prose prose-invert max-w-none text-slate-300 space-y-6">
            <section>
              <h2 className="text-2xl font-semibold text-white mb-4">1. Acceptance of Terms</h2>
              <p>
                By accessing or using Port Buddy, you agree to be bound by these Terms and Conditions. If you do not agree with any part of these terms, you may not use our services.
              </p>
            </section>

            <section>
              <h2 className="text-2xl font-semibold text-white mb-4">2. Description of Service</h2>
              <p>
                Port Buddy provides a tool that allows you to share a port opened on your local host or private network to the public network. It is a proxy service that facilitates remote access to local development environments.
              </p>
            </section>

            <section>
              <h2 className="text-2xl font-semibold text-white mb-4">3. User Accounts</h2>
              <p>
                To use certain features of Port Buddy, you must register for an account. You are responsible for maintaining the confidentiality of your account credentials and for all activities that occur under your account.
              </p>
            </section>

            <section>
              <h2 className="text-2xl font-semibold text-white mb-4">4. Prohibited Use</h2>
              <p>
                You agree not to use Port Buddy for any illegal or unauthorized purpose. Prohibited activities include, but are not limited to:
              </p>
              <ul className="list-disc pl-6 space-y-2">
                <li>Sharing content that violates any laws or regulations.</li>
                <li>Distributing malware or performing malicious attacks.</li>
                <li>Attempting to circumvent any security features of the service.</li>
                <li>Using the service for high-traffic production workloads beyond your subscription limits.</li>
              </ul>
            </section>

            <section>
              <h2 className="text-2xl font-semibold text-white mb-4">5. Subscription and Billing</h2>
              <p>
                Port Buddy offers both free and paid subscription plans. By subscribing to a paid plan, you agree to pay the specified fees. Fees are non-refundable except as required by law.
              </p>
            </section>

            <section>
              <h2 className="text-2xl font-semibold text-white mb-4">6. Limitation of Liability</h2>
              <p>
                Port Buddy is provided "as is" without any warranties. We shall not be liable for any indirect, incidental, or consequential damages arising out of your use of the service.
              </p>
            </section>

            <section>
              <h2 className="text-2xl font-semibold text-white mb-4">7. Changes to Terms</h2>
              <p>
                We reserve the right to modify these terms at any time. We will notify users of any significant changes by posting the new terms on our website.
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
