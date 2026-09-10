import { createHmac, timingSafeEqual } from 'crypto';
import { NextRequest } from 'next/server';

/**
 * Server-only admin auth helpers.
 * Secrets MUST come from Vercel / process.env — never ship them to the client.
 *
 * Required env (Vercel Project → Settings → Environment Variables):
 *   ADMIN_PIN                 – dashboard login PIN
 *   ADMIN_SESSION_SECRET      – optional HMAC secret (falls back to ADMIN_PIN)
 *   SUPABASE_SERVICE_ROLE_KEY – server-side DB writes
 *   NEXT_PUBLIC_SUPABASE_URL
 *   NEXT_PUBLIC_SUPABASE_ANON_KEY
 */

const SESSION_TTL_MS = 12 * 60 * 60 * 1000; // 12 hours
const TOKEN_PREFIX = 'anoy-admin';

function getAdminPin(): string {
  const pin = (process.env.ADMIN_PIN || '').trim();
  return pin;
}

function getSessionSecret(): string {
  const explicit = (process.env.ADMIN_SESSION_SECRET || '').trim();
  if (explicit) return explicit;
  const pin = getAdminPin();
  if (pin) return `anoy-session:${pin}`;
  return '';
}

export function isAdminPinConfigured(): boolean {
  return getAdminPin().length > 0;
}

/** Constant-time PIN compare against process.env.ADMIN_PIN */
export function verifyAdminPin(entered: string): boolean {
  const expected = getAdminPin();
  if (!expected) return false;
  const a = Buffer.from(String(entered ?? ''), 'utf8');
  const b = Buffer.from(expected, 'utf8');
  if (a.length !== b.length) {
    // still run a dummy compare to reduce timing signal on length
    timingSafeEqual(Buffer.alloc(32), Buffer.alloc(32));
    return false;
  }
  return timingSafeEqual(a, b);
}

function sign(payload: string, secret: string): string {
  return createHmac('sha256', secret).update(payload).digest('hex');
}

/** Issue a signed session token after successful PIN login */
export function issueAdminSessionToken(): string | null {
  const secret = getSessionSecret();
  if (!secret) return null;
  const exp = Date.now() + SESSION_TTL_MS;
  const payload = `${TOKEN_PREFIX}:${exp}`;
  const sig = sign(payload, secret);
  return Buffer.from(`${payload}.${sig}`, 'utf8').toString('base64url');
}

export function verifyAdminSessionToken(token: string | null | undefined): boolean {
  if (!token) return false;
  const secret = getSessionSecret();
  if (!secret) return false;
  try {
    const raw = Buffer.from(token, 'base64url').toString('utf8');
    const lastDot = raw.lastIndexOf('.');
    if (lastDot <= 0) return false;
    const payload = raw.slice(0, lastDot);
    const sig = raw.slice(lastDot + 1);
    const expected = sign(payload, secret);
    const a = Buffer.from(sig, 'utf8');
    const b = Buffer.from(expected, 'utf8');
    if (a.length !== b.length || !timingSafeEqual(a, b)) return false;
    const parts = payload.split(':');
    if (parts.length !== 2 || parts[0] !== TOKEN_PREFIX) return false;
    const exp = Number(parts[1]);
    if (!Number.isFinite(exp) || Date.now() > exp) return false;
    return true;
  } catch {
    return false;
  }
}

/** Read Bearer token or x-admin-token header */
export function extractAdminToken(req: NextRequest): string | null {
  const auth = req.headers.get('authorization') || '';
  if (auth.toLowerCase().startsWith('bearer ')) {
    return auth.slice(7).trim() || null;
  }
  const header = req.headers.get('x-admin-token');
  return header?.trim() || null;
}

export function requireAdmin(req: NextRequest): { ok: true } | { ok: false; status: number; error: string } {
  if (!isAdminPinConfigured()) {
    return {
      ok: false,
      status: 503,
      error: 'ADMIN_PIN is not configured on the server. Set it in Vercel Environment Variables.',
    };
  }
  const token = extractAdminToken(req);
  if (!verifyAdminSessionToken(token)) {
    return { ok: false, status: 401, error: 'Unauthorized. Sign in with the admin PIN.' };
  }
  return { ok: true };
}
