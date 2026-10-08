import { PublicNav } from "@/components/PublicNav";
import Link from "next/link";
const create = `curl "$ANOY_URL/api/v1/licenses" \\\n -H "Authorization: Bearer $ANOY_API_KEY" \\\n -H "Content-Type: application/json" \\\n -d '{"action":"create","duration_label":"7 Days","duration_seconds":604800,"max_devices":1}'`;
export default function Page() {
  return (
    <>
      <PublicNav />
      <main className="mx-auto max-w-4xl space-y-9 px-6 py-12">
        <nav className="flex justify-between text-sm">
          <Link href="/">← Anoy home</Link>
          <Link href="/dashboard">Dashboard ↗</Link>
        </nav>
        <header>
          <p className="text-xs uppercase tracking-widest text-muted">
            Developer documentation / v1
          </p>
          <h1 className="mt-4 text-4xl font-semibold">
            License management API
          </h1>
          <p className="mt-5 text-muted">
            Automate your license workflow with scoped, revocable credentials.
            Management API keys are not Android app license keys.
          </p>
        </header>
        <section className="space-y-3">
          <h2 className="text-xl">Get access</h2>
          <p className="text-sm leading-relaxed text-muted">
            Sign up, confirm your email, and request administrator approval.
            Open API access in the dashboard and create a read-only or
            read/write key, with an expiry of 1–365 days. Copy the secret
            immediately: only its hash is stored. All approved admins share the
            existing license catalog.
          </p>
        </section>
        <section className="space-y-3">
          <h2 className="text-xl">Authentication</h2>
          <p className="text-sm leading-relaxed text-muted">
            Set ANOY_URL to your deployed HTTPS site and ANOY_API_KEY to the
            secret. Send Authorization: Bearer on every request. Keep secrets on
            your server, never in browser code or Git. Expiry, revocation,
            scopes, and account approval are checked on each request. API keys
            cannot create more API keys or change system settings.
          </p>
          <pre className="overflow-x-auto rounded-xl border border-line bg-surface p-5 text-xs">
            <code>
              {
                'curl "$ANOY_URL/api/v1/licenses" -H "Authorization: Bearer $ANOY_API_KEY"'
              }
            </code>
          </pre>
        </section>
        <section className="space-y-3">
          <h2 className="text-xl">Create a license</h2>
          <pre className="overflow-x-auto rounded-xl border border-line bg-surface p-5 text-xs leading-7">
            <code>{create}</code>
          </pre>
        </section>
        <section className="space-y-3">
          <h2 className="text-xl">Operations</h2>
          <p className="text-sm text-muted">
            GET /api/v1/licenses requires licenses:read. POST /api/v1/licenses
            requires licenses:write. POST accepts these JSON actions:
          </p>
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr>
                  <th className="p-3">Action</th>
                  <th className="p-3">Parameters</th>
                </tr>
              </thead>
              <tbody>
                {[
                  [
                    "create",
                    "duration_label, duration_seconds (0 = lifetime; max 31536000), max_devices (1–10). Optional: key, notes, activation_timing (ON_FIRST_USE or IMMEDIATE).",
                  ],
                  [
                    "bulk",
                    "Same as create, plus count (1–50). Returns keys and generated license strings.",
                  ],
                  [
                    "extend",
                    "id, seconds (positive integer, max 31536000). Extends from expiry or now and reactivates.",
                  ],
                  ["reset_hwid", "id. Clears bound hardware IDs."],
                  [
                    "toggle_ban",
                    "id. Bans or restores UNUSED, ACTIVE, or EXPIRED status.",
                  ],
                  [
                    "delete",
                    "id. Permanently deletes a license. Use POST action:delete, not HTTP DELETE.",
                  ],
                ].map(([a, p]) => (
                  <tr key={a} className="border-b border-line">
                    <td className="p-3 align-top font-mono">{a}</td>
                    <td className="p-3 text-muted">{p}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <pre className="overflow-x-auto rounded-xl border border-line p-5 text-xs">
            <code>{'{"action":"delete","id":"LICENSE_UUID_FROM_LIST"}'}</code>
          </pre>
        </section>
        <section className="space-y-3">
          <h2 className="text-xl">Responses, usage, and limits</h2>
          <p className="text-sm text-muted">
            List: {"{success:true, live:true, keys:[…]}"} · Create/update:{" "}
            {"{success:true, key:{…}}"} · Delete: {"{success:true, id:…}"} ·
            Errors: {'{success:false, error:"…"}'}.
          </p>
          <p className="text-sm text-muted">
            400 invalid input · 401 invalid/expired/revoked credential · 403
            missing scope/unapproved account · 404 not found · 409 duplicate key
            · 500 database failure · 503 backend unavailable.
          </p>
          <p className="text-sm leading-relaxed text-muted">
            The dashboard shows authenticated request counts and last usage
            (including requests with invalid operation input). No application
            rate limiter or pagination is included yet: configure deployment/WAF
            limits before exposing automation at scale. List is subject to your
            Supabase project's row limit. Retry transient failures with backoff;
            create/bulk are not idempotent, so never blindly retry them. Revoke
            leaked credentials immediately.
          </p>
        </section>
      </main>
    </>
  );
}
