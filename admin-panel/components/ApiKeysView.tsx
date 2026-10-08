"use client";
import { useEffect, useState } from "react";
import Link from "next/link";
import { api } from "@/lib/api";
import { Button, Field, Input } from "./ui";
type Key = {
  id: string;
  name: string;
  prefix: string;
  scopes: string[];
  expires_at: string;
  revoked_at: string | null;
  request_count: number;
  last_used_at: string | null;
};
export function ApiKeysView() {
  const [keys, setKeys] = useState<Key[]>([]);
  const [name, setName] = useState("");
  const [days, setDays] = useState(30);
  const [write, setWrite] = useState(false);
  const [secret, setSecret] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  async function load() {
    try {
      setKeys((await api<{ keys: Key[] }>("/api/admin/api-keys")).keys);
    } catch (e: any) {
      setError(e.message);
    }
  }
  useEffect(() => {
    void load();
  }, []);
  return (
    <div className="space-y-7">
      <header>
        <h1 className="text-2xl">Developer access</h1>
        <p className="mt-2 text-sm text-muted">
          Create scoped credentials for license automation.{" "}
          <Link href="/docs" className="underline">
            API documentation ↗
          </Link>
        </p>
      </header>
      <form
        className="space-y-4 rounded-xl border border-line bg-surface p-6"
        onSubmit={async (e) => {
          e.preventDefault();
          setBusy(true);
          setError("");
          setSecret("");
          try {
            const data = await api<{ secret: string }>("/api/admin/api-keys", {
              method: "POST",
              body: JSON.stringify({
                name,
                expires_in_days: days,
                scopes: write
                  ? ["licenses:read", "licenses:write"]
                  : ["licenses:read"],
              }),
            });
            setSecret(data.secret);
            setName("");
            await load();
          } catch (e: any) {
            setError(e.message);
          } finally {
            setBusy(false);
          }
        }}
      >
        <h2 className="font-medium">New API key</h2>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Name">
            <Input
              required
              maxLength={80}
              placeholder="e.g. Billing integration"
              value={name}
              onChange={(e) => setName(e.target.value)}
            />
          </Field>
          <Field label="Expires in (days)">
            <Input
              required
              type="number"
              min={1}
              max={365}
              value={days}
              onChange={(e) => setDays(Number(e.target.value))}
            />
          </Field>
        </div>
        <label className="flex items-center gap-2 text-sm text-muted">
          <input
            type="checkbox"
            checked={write}
            onChange={(e) => setWrite(e.target.checked)}
          />
          Allow license writes (create, extend, ban, reset, delete)
        </label>
        <Button type="submit" loading={busy}>
          Create API key
        </Button>
      </form>
      {error && (
        <p role="alert" className="text-sm text-danger">
          {error}
        </p>
      )}
      {secret && (
        <div className="space-y-3 rounded-xl border border-accent p-5">
          <h2>Copy your secret now</h2>
          <p className="text-sm text-muted">
            It will never be displayed again. Treat it like a password.
          </p>
          <code className="block break-all rounded bg-subtle p-3 text-sm">
            {secret}
          </code>
          <Button
            variant="secondary"
            onClick={async () => {
              try {
                await navigator.clipboard.writeText(secret);
              } catch {
                setError(
                  "Clipboard unavailable. Select and copy the secret manually.",
                );
              }
            }}
          >
            Copy secret
          </Button>
          <Button variant="ghost" onClick={() => setSecret("")}>
            Dismiss
          </Button>
        </div>
      )}
      <div className="space-y-3">
        {keys.length === 0 && (
          <p className="text-sm text-muted">No API keys yet.</p>
        )}
        {keys.map((key) => (
          <article
            key={key.id}
            className="flex flex-wrap items-center justify-between gap-4 rounded-xl border border-line p-5"
          >
            <div>
              <h3>
                {key.name}{" "}
                {key.revoked_at ? (
                  <span className="text-xs text-danger">Revoked</span>
                ) : new Date(key.expires_at).getTime() <= Date.now() ? (
                  <span className="text-xs text-muted">Expired</span>
                ) : null}
              </h3>
              <p className="mt-2 font-mono text-xs text-muted">
                {key.prefix}… · {key.scopes.join(", ")}
              </p>
              <p className="mt-2 text-xs text-muted">
                Expires {new Date(key.expires_at).toLocaleDateString()} ·{" "}
                {key.request_count} authenticated requests · Last used:{" "}
                {key.last_used_at
                  ? new Date(key.last_used_at).toLocaleString()
                  : "Never"}
              </p>
            </div>
            {!key.revoked_at && (
              <Button
                variant="danger"
                disabled={busy}
                onClick={async () => {
                  if (
                    !window.confirm(
                      `Revoke ${key.name}? Integrations using it will stop working.`,
                    )
                  )
                    return;
                  setBusy(true);
                  try {
                    await api(`/api/admin/api-keys?id=${key.id}`, {
                      method: "DELETE",
                    });
                    await load();
                  } catch (e: any) {
                    setError(e.message);
                  } finally {
                    setBusy(false);
                  }
                }}
              >
                Revoke
              </Button>
            )}
          </article>
        ))}
      </div>
    </div>
  );
}
