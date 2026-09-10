import { createClient, SupabaseClient } from '@supabase/supabase-js';

/**
 * Server-only Supabase clients.
 * Prefer the service-role key for privileged admin API routes so the secret
 * never ships in the browser bundle.
 */

function url(): string {
  return (process.env.NEXT_PUBLIC_SUPABASE_URL || process.env.SUPABASE_URL || '').trim();
}

function anonKey(): string {
  return (process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY || process.env.SUPABASE_ANON_KEY || '').trim();
}

function serviceKey(): string {
  return (process.env.SUPABASE_SERVICE_ROLE_KEY || '').trim();
}

export function isServerSupabaseConfigured(): boolean {
  const u = url();
  const key = serviceKey() || anonKey();
  return Boolean(u) && Boolean(key) && !u.includes('your-project-id');
}

/** Prefer service role; fall back to anon if service role is missing. */
export function getServerSupabase(): SupabaseClient | null {
  const u = url();
  if (!u || u.includes('your-project-id')) return null;
  const key = serviceKey() || anonKey();
  if (!key || key.includes('your-anon-key') || key.includes('your-service-role')) return null;
  return createClient(u, key, {
    auth: { persistSession: false, autoRefreshToken: false },
  });
}

export function hasServiceRoleKey(): boolean {
  const k = serviceKey();
  return Boolean(k) && !k.includes('your-service-role');
}
