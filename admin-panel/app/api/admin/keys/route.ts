import { NextRequest, NextResponse } from 'next/server';
import { requireAdmin } from '@/lib/admin-auth';
import { getServerSupabase, isServerSupabaseConfigured } from '@/lib/supabase-server';
import { LicenseKey, KeyStatus } from '@/lib/types';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, POST, PATCH, DELETE, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type, Authorization, x-admin-token',
  'Cache-Control': 'no-store',
};

const KEY_PATTERN = /^[A-Z0-9][A-Z0-9\-_]{2,63}$/;
const ALLOWED_EXTEND = [3600, 21600, 43200, 86400, 259200, 604800, 1296000, 2592000, 5184000, 31536000];

function json(body: unknown, status = 200) {
  return NextResponse.json(body, { status, headers: corsHeaders });
}

function unauthorized(req: NextRequest) {
  const auth = requireAdmin(req);
  if (!auth.ok) return json({ success: false, error: auth.error }, auth.status);
  return null;
}

function db() {
  if (!isServerSupabaseConfigured()) return null;
  return getServerSupabase();
}

function generateSegment(len = 4) {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  let res = '';
  for (let i = 0; i < len; i++) res += chars.charAt(Math.floor(Math.random() * chars.length));
  return res;
}

function createFormattedKey() {
  return `ANOY-${generateSegment()}-${generateSegment()}-${generateSegment()}`;
}

function buildKeyPayload(input: {
  key: string;
  duration_label: string;
  duration_seconds: number;
  max_devices: number;
  notes: string | null;
  activationTiming: 'ON_FIRST_USE' | 'IMMEDIATE';
}) {
  const now = new Date();
  const isTimed = input.duration_seconds > 0;
  const isImmediate = isTimed && input.activationTiming === 'IMMEDIATE';
  const status: KeyStatus = isImmediate ? 'ACTIVE' : 'UNUSED';
  return {
    key: input.key,
    duration_label: input.duration_label,
    duration_seconds: input.duration_seconds,
    max_devices: input.max_devices,
    hwid_list: [] as string[],
    status,
    created_at: now.toISOString(),
    activated_at: isImmediate ? now.toISOString() : null,
    expires_at: isImmediate ? new Date(now.getTime() + input.duration_seconds * 1000).toISOString() : null,
    notes: input.notes,
  };
}

export async function GET(req: NextRequest) {
  const denied = unauthorized(req);
  if (denied) return denied;

  const client = db();
  if (!client) {
    return json({ success: true, live: false, keys: [] as LicenseKey[] });
  }

  const { data, error } = await client
    .from('license_keys')
    .select('*')
    .order('created_at', { ascending: false });

  if (error) return json({ success: false, error: error.message }, 500);
  return json({ success: true, live: true, keys: (data || []) as LicenseKey[] });
}

export async function POST(req: NextRequest) {
  const denied = unauthorized(req);
  if (denied) return denied;

  const client = db();
  if (!client) return json({ success: false, error: 'Database is not configured.' }, 503);

  const body = await req.json().catch(() => ({}));
  const action = String(body?.action || 'create');

  if (action === 'create' || action === 'bulk') {
    const durationLabel = String(body.duration_label || '').trim();
    const durationSeconds = Number(body.duration_seconds);
    const maxDevices = Math.min(Math.max(parseInt(String(body.max_devices ?? 1), 10) || 1, 1), 10);
    const notes = typeof body.notes === 'string' && body.notes.trim() ? body.notes.trim() : null;
    const activationTiming: 'ON_FIRST_USE' | 'IMMEDIATE' =
      body.activation_timing === 'IMMEDIATE' ? 'IMMEDIATE' : 'ON_FIRST_USE';

    if (!durationLabel || !Number.isFinite(durationSeconds) || durationSeconds < 0) {
      return json({ success: false, error: 'Duration is required.' }, 400);
    }

    if (action === 'create') {
      const custom = typeof body.key === 'string' ? body.key.trim().toUpperCase() : '';
      const generated = custom || createFormattedKey();
      if (!KEY_PATTERN.test(generated)) {
        return json(
          { success: false, error: 'Key must be 3–64 characters: letters, numbers, hyphen, underscore.' },
          400
        );
      }

      const payload = buildKeyPayload({
        key: generated,
        duration_label: durationLabel,
        duration_seconds: durationSeconds,
        max_devices: maxDevices,
        notes,
        activationTiming,
      });

      const { data, error } = await client.from('license_keys').insert([payload]).select().single();
      if (error) {
        if (String(error.message).toLowerCase().includes('duplicate')) {
          return json({ success: false, error: 'That key already exists.' }, 409);
        }
        return json({ success: false, error: error.message }, 500);
      }
      return json({ success: true, key: data as LicenseKey });
    }

    const count = Math.min(Math.max(parseInt(String(body.count ?? 1), 10) || 1, 1), 50);
    const items = Array.from({ length: count }, () =>
      buildKeyPayload({
        key: createFormattedKey(),
        duration_label: durationLabel,
        duration_seconds: durationSeconds,
        max_devices: maxDevices,
        notes,
        activationTiming,
      })
    );

    const { data, error } = await client.from('license_keys').insert(items).select();
    if (error) return json({ success: false, error: error.message }, 500);
    return json({
      success: true,
      keys: (data || []) as LicenseKey[],
      generated: items.map((i) => i.key),
    });
  }

  if (action === 'extend') {
    const id = String(body.id || '').trim();
    const seconds = Number(body.seconds);
    if (!id) return json({ success: false, error: 'Key id is required.' }, 400);
    if (!Number.isFinite(seconds) || seconds <= 0) {
      return json({ success: false, error: 'Extension time is required.' }, 400);
    }
    if (!ALLOWED_EXTEND.includes(seconds) && seconds > 31536000) {
      return json({ success: false, error: 'Extension is too large.' }, 400);
    }

    const { data: existing, error: fetchErr } = await client
      .from('license_keys')
      .select('*')
      .eq('id', id)
      .maybeSingle();
    if (fetchErr) return json({ success: false, error: fetchErr.message }, 500);
    if (!existing) return json({ success: false, error: 'Key not found.' }, 404);

    const now = Date.now();
    const currentExpiry = existing.expires_at ? new Date(existing.expires_at).getTime() : now;
    const baseTime = Math.max(now, currentExpiry);
    const newExpiresAt = new Date(baseTime + seconds * 1000).toISOString();

    const { data, error } = await client
      .from('license_keys')
      .update({ expires_at: newExpiresAt, status: 'ACTIVE' })
      .eq('id', id)
      .select()
      .single();
    if (error) return json({ success: false, error: error.message }, 500);
    return json({ success: true, key: data as LicenseKey });
  }

  if (action === 'reset_hwid') {
    const id = String(body.id || '').trim();
    if (!id) return json({ success: false, error: 'Key id is required.' }, 400);
    const { data, error } = await client
      .from('license_keys')
      .update({ hwid_list: [] })
      .eq('id', id)
      .select()
      .single();
    if (error) return json({ success: false, error: error.message }, 500);
    return json({ success: true, key: data as LicenseKey });
  }

  if (action === 'toggle_ban') {
    const id = String(body.id || '').trim();
    if (!id) return json({ success: false, error: 'Key id is required.' }, 400);
    const { data: existing, error: fetchErr } = await client
      .from('license_keys')
      .select('*')
      .eq('id', id)
      .maybeSingle();
    if (fetchErr) return json({ success: false, error: fetchErr.message }, 500);
    if (!existing) return json({ success: false, error: 'Key not found.' }, 404);

    let nextStatus: KeyStatus = 'BANNED';
    if (existing.status === 'BANNED') {
      const expired = existing.expires_at && new Date(existing.expires_at).getTime() <= Date.now();
      if (expired) nextStatus = 'EXPIRED';
      else if (!existing.activated_at) nextStatus = 'UNUSED';
      else nextStatus = 'ACTIVE';
    }

    const { data, error } = await client
      .from('license_keys')
      .update({ status: nextStatus })
      .eq('id', id)
      .select()
      .single();
    if (error) return json({ success: false, error: error.message }, 500);
    return json({ success: true, key: data as LicenseKey });
  }

  if (action === 'delete') {
    const id = String(body.id || '').trim();
    if (!id) return json({ success: false, error: 'Key id is required.' }, 400);
    const { error } = await client.from('license_keys').delete().eq('id', id);
    if (error) return json({ success: false, error: error.message }, 500);
    return json({ success: true, id });
  }

  return json({ success: false, error: 'Invalid action.' }, 400);
}

export async function OPTIONS() {
  return new NextResponse(null, { status: 204, headers: corsHeaders });
}
