import Link from "next/link";
import {
  ArrowRight,
  Check,
  Code2,
  KeyRound,
  Layers3,
  ShieldCheck,
  Sparkles,
  Smartphone,
  BookOpen,
} from "lucide-react";
import { PublicNav } from "@/components/PublicNav";
export default function Page() {
  return (
    <div className="min-h-screen">
      <PublicNav />
      <main>
        <section className="mx-auto grid max-w-6xl gap-14 px-5 py-16 sm:px-8 sm:py-24 lg:grid-cols-[1.1fr_1fr] lg:items-center">
          <div>
            <div className="mb-7 inline-flex items-center gap-2 rounded-full border border-accent/25 bg-accent/10 px-3 py-2 text-xs font-medium text-accent">
              <Sparkles size={14} />
              Meet your new control workspace
            </div>
            <h1 className="text-5xl font-semibold leading-[1.08] tracking-tight sm:text-6xl">
              Everything in order.
              <br />
              <span className="text-accent">You in control.</span>
            </h1>
            <p className="mt-6 max-w-lg text-base leading-relaxed text-muted sm:text-lg">
              Licenses, releases, and integrations—together in one simple
              workspace. Spend less time managing access and more time moving
              forward.
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Link
                href="/signup"
                className="inline-flex items-center gap-3 rounded-xl bg-accent px-6 py-3.5 text-sm font-semibold text-onAccent shadow-sm hover:brightness-110"
              >
                Create your account <ArrowRight size={17} />
              </Link>
              <Link
                href="/docs"
                className="inline-flex items-center gap-2 rounded-xl border border-line bg-surface px-5 py-3.5 text-sm font-medium hover:border-accent"
              >
                <BookOpen size={17} />
                Explore the API
              </Link>
            </div>
            <div className="mt-6 flex items-center gap-2 text-xs text-muted">
              <ShieldCheck size={15} className="text-accent" />
              Account-based access. Approved administrators only.
            </div>
          </div>
          <div className="relative">
            <div
              aria-hidden="true"
              className="hero-grid absolute -inset-3 sm:-inset-6 -z-0 rounded-3xl opacity-25"
            />
            <div className="relative overflow-hidden rounded-3xl border border-line bg-surface shadow-2xl">
              <div className="flex items-center justify-between border-b border-line px-6 py-5">
                <div className="flex items-center gap-2.5 text-sm font-semibold">
                  <Layers3 size={19} className="text-accent" />
                  Anoy workspace
                </div>
                <span className="rounded-full border border-line px-2.5 py-1 text-[10px] text-muted">
                  Product preview
                </span>
              </div>
              <div className="space-y-6 p-6">
                <div>
                  <p className="text-xs text-muted">
                    A clearer view of your workflow
                  </p>
                  <h2 className="mt-2 text-xl font-semibold">
                    Ready for what’s next.
                  </h2>
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <div className="rounded-2xl border border-line bg-background p-4">
                    <KeyRound size={19} className="mb-4 text-accent" />
                    <p className="text-sm font-semibold">License lifecycle</p>
                    <p className="mt-1.5 text-xs text-muted">
                      Create → manage → renew
                    </p>
                  </div>
                  <div className="rounded-2xl border border-line bg-background p-4">
                    <Code2 size={19} className="mb-4 text-success" />
                    <p className="text-sm font-semibold">Developer access</p>
                    <p className="mt-1.5 text-xs text-muted">
                      Scoped, revocable keys
                    </p>
                  </div>
                </div>
                <div className="space-y-3">
                  {[
                    "One place for all your licenses",
                    "Clear controls for every release",
                    "API tools that fit your workflow",
                  ].map((text) => (
                    <div key={text} className="flex items-center gap-3 text-sm">
                      <span className="flex h-5 w-5 items-center justify-center rounded-full bg-success/10 text-success">
                        <Check size={12} />
                      </span>
                      {text}
                    </div>
                  ))}
                </div>
                <div className="overflow-hidden rounded-xl border border-line bg-background">
                  <div className="flex items-center gap-2 border-b border-line px-4 py-3 text-[10px] font-medium uppercase tracking-widest text-muted">
                    <Code2 size={13} />
                    Built for integrations
                  </div>
                  <div className="p-4 font-mono text-xs leading-7">
                    <span className="text-success">GET</span> /api/v1/licenses
                    <br />
                    <span className="text-muted">Authorization:</span> Bearer
                    anoy_api_…
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>
        <section className="border-y border-line bg-surface/50">
          <div className="mx-auto max-w-6xl px-5 py-14 sm:px-8">
            <p className="text-xs font-semibold uppercase tracking-[0.18em] text-accent">
              Built around your workflow
            </p>
            <h2 className="mt-3 text-3xl font-semibold">
              Less complexity. More clarity.
            </h2>
            <div className="mt-9 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
              {[
                [
                  KeyRound,
                  "License management",
                  "Create individual or bulk keys. Set durations, manage devices, and renew access.",
                ],
                [
                  Code2,
                  "Your tools, connected",
                  "Automate license operations with API credentials you can scope and revoke.",
                ],
                [
                  Smartphone,
                  "App operations",
                  "Publish app and library updates from one organized dashboard.",
                ],
                [
                  ShieldCheck,
                  "Intentional access",
                  "Approve administrators and keep management separate from app license login.",
                ],
              ].map(([Icon, title, detail]: any) => (
                <article
                  key={title}
                  className="rounded-2xl border border-line bg-surface p-6"
                >
                  <span className="mb-5 flex h-11 w-11 items-center justify-center rounded-xl bg-accent/10 text-accent">
                    <Icon size={21} />
                  </span>
                  <h3 className="text-base font-semibold">{title}</h3>
                  <p className="mt-3 text-sm leading-relaxed text-muted">
                    {detail}
                  </p>
                </article>
              ))}
            </div>
          </div>
        </section>
        <section className="mx-auto max-w-6xl px-5 py-16 sm:px-8">
          <div className="grid gap-10 lg:grid-cols-[1fr_1.6fr]">
            <div>
              <p className="text-xs font-semibold uppercase tracking-widest text-accent">
                Getting started
              </p>
              <h2 className="mt-3 text-3xl font-semibold">
                A simple path
                <br />
                to your workspace.
              </h2>
              <Link
                href="/signup"
                className="mt-6 inline-flex items-center gap-2 text-sm font-semibold text-accent"
              >
                Get started <ArrowRight size={16} />
              </Link>
            </div>
            <ol className="grid gap-6 sm:grid-cols-3">
              {[
                [
                  "01",
                  "Create an account",
                  "Register with your email and a strong password.",
                ],
                [
                  "02",
                  "Confirm & get approved",
                  "Confirm your email and ask an admin to grant access.",
                ],
                [
                  "03",
                  "Make it yours",
                  "Manage licenses or create API keys for your integrations.",
                ],
              ].map(([n, title, text]) => (
                <li key={n}>
                  <span className="flex h-10 w-10 items-center justify-center rounded-xl border border-accent/25 bg-accent/10 font-mono text-sm text-accent">
                    {n}
                  </span>
                  <h3 className="mt-4 text-sm font-semibold">{title}</h3>
                  <p className="mt-2 text-sm leading-relaxed text-muted">
                    {text}
                  </p>
                </li>
              ))}
            </ol>
          </div>
        </section>
      </main>
      <footer className="border-t border-line">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-4 px-5 py-7 text-xs text-muted sm:px-8">
          <span>Anoy Control · Keep everything moving.</span>
          <div className="flex gap-5">
            <Link href="/docs" className="hover:text-ink">
              Documentation
            </Link>
            <Link href="/login" className="hover:text-ink">
              Sign in
            </Link>
          </div>
        </div>
      </footer>
    </div>
  );
}
