import Link from "next/link";
import {
  ArrowUpRight,
  KeyRound,
  Code2,
  Layers,
  ShieldCheck,
} from "lucide-react";
export default function Page() {
  return (
    <div className="min-h-screen bg-background">
      <nav className="mx-auto flex max-w-6xl items-center justify-between border-b border-line px-6 py-6">
        <Link href="/" className="text-xl font-medium">
          anoy<span className="text-accent">.</span>
        </Link>
        <div className="flex items-center gap-5 text-sm">
          <Link href="/docs">API docs</Link>
          <Link href="/login">Sign in</Link>
          <Link
            href="/signup"
            className="rounded-lg bg-accent px-4 py-2 text-onAccent"
          >
            Get started
          </Link>
        </div>
      </nav>
      <main className="mx-auto max-w-6xl px-6">
        <section className="grid gap-12 py-20 md:grid-cols-2 md:py-28">
          <div>
            <p className="mb-6 text-xs uppercase tracking-[0.2em] text-muted">
              Anoy Loader / Control workspace
            </p>
            <h1 className="text-5xl font-medium leading-tight sm:text-6xl">
              One workspace.
              <br />
              <span className="text-muted">Complete control.</span>
            </h1>
            <p className="mt-7 max-w-md text-lg leading-relaxed text-muted">
              Manage licenses, publish app updates, and keep your community
              informed. Built for your workflow. Ready for your integrations.
            </p>
            <div className="mt-9 flex flex-wrap gap-3">
              <Link
                href="/signup"
                className="flex items-center gap-2 rounded-lg bg-accent px-5 py-3 text-onAccent"
              >
                Create an account <ArrowUpRight size={17} />
              </Link>
              <Link
                href="/docs"
                className="rounded-lg border border-line px-5 py-3"
              >
                Explore the API
              </Link>
            </div>
            <p className="mt-5 text-xs text-muted">
              Account-based access. Administrator approval required.
            </p>
          </div>
          <div className="self-center rounded-2xl border border-line bg-surface p-7 shadow-xl">
            <div className="mb-7 flex items-center justify-between border-b border-line pb-5">
              <span className="text-sm">Your control workspace</span>
              <span className="rounded-full bg-subtle px-3 py-1 text-xs text-muted">
                Preview
              </span>
            </div>
            {[
              [
                "01",
                "License lifecycle",
                "Create, extend, reset, ban, and delete.",
              ],
              [
                "02",
                "Developer access",
                "Scoped API keys with expiry and revocation.",
              ],
              [
                "03",
                "Release operations",
                "App updates, game catalog, and announcements.",
              ],
            ].map(([n, title, detail]) => (
              <div key={n} className="flex gap-5 py-5">
                <span className="font-mono text-sm text-muted">{n}</span>
                <div>
                  <h2 className="font-medium">{title}</h2>
                  <p className="mt-2 text-sm text-muted">{detail}</p>
                </div>
              </div>
            ))}
            <div className="mt-5 rounded-lg bg-subtle p-4 font-mono text-xs text-muted">
              GET /api/v1/licenses
              <br />
              <span className="mt-2 block text-ink">
                Authorization: Bearer anoy_api_…
              </span>
            </div>
          </div>
        </section>
        <section className="grid gap-6 border-t border-line py-14 sm:grid-cols-2 lg:grid-cols-4">
          {[
            [
              KeyRound,
              "Licenses, simplified",
              "Manage individual and bulk keys from one place.",
            ],
            [
              Code2,
              "Automation first",
              "Use the same license operations in your own tools.",
            ],
            [
              ShieldCheck,
              "Controlled access",
              "Approved accounts, scoped credentials, hashed secrets.",
            ],
            [
              Layers,
              "App operations",
              "Keep updates and system messages organized.",
            ],
          ].map(([Icon, title, detail]: any) => (
            <article key={title} className="space-y-4">
              <Icon className="h-5 w-5 text-muted" />
              <h2 className="font-medium">{title}</h2>
              <p className="text-sm leading-relaxed text-muted">{detail}</p>
            </article>
          ))}
        </section>
      </main>
      <footer className="border-t border-line px-6 py-7 text-center text-xs text-muted">
        Anoy Control ·{" "}
        <Link href="/docs" className="underline">
          Developer documentation
        </Link>
      </footer>
    </div>
  );
}
