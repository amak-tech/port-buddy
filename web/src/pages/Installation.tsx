export default function Installation() {
  return (
    <div className="container py-16">
      <h1 className="text-3xl font-bold">Installation</h1>
      <p className="text-white/70 mt-2">Install the Port Buddy CLI on your platform.</p>

      <div className="mt-10 space-y-6">
        <section className="bg-black/30 border border-white/10 rounded-xl p-6">
          <h2 className="font-semibold">macOS</h2>
          <pre className="mt-3 bg-black/30 border border-white/10 rounded p-3 text-sm overflow-x-auto max-w-full">
{`# with Homebrew
brew tap port-buddy/tap
brew install port-buddy

# initialize
port-buddy init {API_TOKEN}

# expose a local web app
port-buddy 3000`}
          </pre>
        </section>

        <section className="bg-black/30 border border-white/10 rounded-xl p-6">
          <h2 className="font-semibold">Linux</h2>
          <pre className="mt-3 bg-black/30 border border-white/10 rounded p-3 text-sm overflow-x-auto max-w-full">
{`# download latest
curl -L https://github.com/port-buddy/cli/releases/download/v1.0.0/port-buddy-linux-amd64 -o port-buddy
chmod +x port-buddy
sudo mv port-buddy /usr/local/bin/

# initialize
port-buddy init {API_TOKEN}`}
          </pre>
        </section>

        <section className="bg-black/30 border border-white/10 rounded-xl p-6">
          <h2 className="font-semibold">Windows</h2>
          <pre className="mt-3 bg-black/30 border border-white/10 rounded p-3 text-sm overflow-x-auto max-w-full">
{`# with Scoop
scoop bucket add port-buddy https://github.com/port-buddy/scoop-bucket
scoop install port-buddy

# PowerShell
port-buddy init {API_TOKEN}`}
          </pre>
        </section>
      </div>

      <p className="text-white/60 text-sm mt-6">
        Note: Replace <code>{'{API_TOKEN}'}</code> with a token generated in your account. Default mode is HTTP; use <code>tcp</code> for TCP exposure.
      </p>
    </div>
  )
}
