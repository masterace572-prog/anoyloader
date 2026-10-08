import { createClient, SupabaseClient } from "@supabase/supabase-js";

/**
 * Server-only Supabase clients.
 * Prefer the service-role key for privileged admin API routes so the secret
 * never ships in the browser bundle.
 */

function url(): string {
  return (
    process.env.NEXT_PUBLIC_SUPABASE_URL ||
    process.env.SUPABASE_URL ||
    ""
  ).trim();
}

function serviceKey(): string {
  return (process.env.SUPABASE_SERVICE_ROLE_KEY || "").trim();
}

export function isServerSupabaseConfigured(): boolean {
  const u = url();
  const key = serviceKey();
  return (
    Boolean(u) &&
    Boolean(key) &&
    !u.includes("your-project-id") &&
    !key.includes("your-service-role")
  );
}

/** Privileged routes fail closed without a server-only service role key. */
export function getServerSupabase(): SupabaseClient | null {
  const u = url();
  if (!u || u.includes("your-project-id")) return null;
  const key = serviceKey();
  if (
    !key ||
    key.includes("your-anon-key") ||
    key.includes("your-service-role")
  )
    return null;
  return createClient(u, key, {
    auth: { persistSession: false, autoRefreshToken: false },
  });
}

export function hasServiceRoleKey(): boolean {
  const k = serviceKey();
  return Boolean(k) && !k.includes("your-service-role");
}
