export const dynamic = 'force-dynamic';
export const revalidate = 0;

import { NextRequest, NextResponse } from 'next/server';
import { createClient } from '@supabase/supabase-js';
import { requireAdmin } from '@/lib/admin-auth';
import { getServerSupabase } from '@/lib/supabase-server';

const supabaseUrl = process.env.NEXT_PUBLIC_SUPABASE_URL || '';
const supabaseServiceKey =
  process.env.SUPABASE_SERVICE_ROLE_KEY || process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY || '';

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type, Authorization, x-admin-token',
  'Cache-Control': 'no-store',
};

const DEFAULT_LIB_UPDATE = {
  version: '1.0',
  download_url: '',
  updated_at: new Date().toISOString(),
};

export async function GET() {
  try {
    if (!supabaseUrl || !supabaseServiceKey || supabaseUrl.includes('your-project-id')) {
      return NextResponse.json(
        { success: true, ...DEFAULT_LIB_UPDATE, source: 'local_fallback' },
        { headers: corsHeaders }
      );
    }

    const supabase = createClient(supabaseUrl, supabaseServiceKey);
    const { data, error } = await supabase
      .from('lib_updates')
      .select('version, download_url, updated_at, file_name')
      .eq('is_active', true)
      .order('updated_at', { ascending: false })
      .limit(1)
      .maybeSingle();

    if (error || !data) {
      return NextResponse.json(
        { success: true, ...DEFAULT_LIB_UPDATE, source: 'default' },
        { headers: corsHeaders }
      );
    }

    return NextResponse.json(
      {
        success: true,
        version: data.version,
        download_url: data.download_url,
        updated_at: data.updated_at,
        file_name: data.file_name,
        source: 'supabase',
      },
      { headers: corsHeaders }
    );
  } catch (err: any) {
    return NextResponse.json(
      { success: true, ...DEFAULT_LIB_UPDATE, error: err.message },
      { headers: corsHeaders }
    );
  }
}

export async function POST(req: NextRequest) {
  const auth = requireAdmin(req);
  if (!auth.ok) {
    return NextResponse.json({ success: false, error: auth.error }, { status: auth.status, headers: corsHeaders });
  }

  const body = await req.json().catch(() => ({}));
  const version = String(body.version || '').trim();
  const downloadUrl = String(body.download_url || '').trim();

  if (!version) {
    return NextResponse.json({ success: false, error: 'Version is required.' }, { status: 400, headers: corsHeaders });
  }
  if (!downloadUrl || !/^https?:\/\//i.test(downloadUrl)) {
    return NextResponse.json(
      { success: false, error: 'A valid direct download URL is required.' },
      { status: 400, headers: corsHeaders }
    );
  }

  const db = getServerSupabase();
  if (!db) {
    return NextResponse.json(
      { success: false, error: 'Database is not configured.' },
      { status: 503, headers: corsHeaders }
    );
  }

  await db.from('lib_updates').update({ is_active: false }).eq('is_active', true);

  const payload = {
    version,
    download_url: downloadUrl,
    is_active: true,
    updated_at: new Date().toISOString(),
  };

  const { error } = await db.from('lib_updates').insert([payload]);
  if (error) {
    return NextResponse.json({ success: false, error: error.message }, { status: 500, headers: corsHeaders });
  }

  return NextResponse.json({ success: true, update: payload }, { headers: corsHeaders });
}

export async function OPTIONS() {
  return new NextResponse(null, { status: 204, headers: corsHeaders });
}
