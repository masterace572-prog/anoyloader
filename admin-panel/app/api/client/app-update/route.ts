import { NextRequest, NextResponse } from 'next/server';
import { supabase, isSupabaseConfigured } from '@/lib/supabase';
import { requireAdmin } from '@/lib/admin-auth';
import { getServerSupabase } from '@/lib/supabase-server';

export interface ApkUpdateResponse {
  success: boolean;
  update_available: boolean;
  version_code: number;
  version_name: string;
  download_url: string;
  changelog: string;
  is_mandatory: boolean;
  file_size?: number;
  updated_at?: string;
  error?: string;
}

const DEFAULT_UPDATE: ApkUpdateResponse = {
  success: true,
  update_available: false,
  version_code: 2,
  version_name: '2026.01.01',
  download_url: '',
  changelog: 'Performance improvements and security updates.',
  is_mandatory: false,
};

export async function GET(req: NextRequest) {
  const corsHeaders = {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization',
    'Cache-Control': 'no-cache, no-store, must-revalidate',
  };

  try {
    const { searchParams } = new URL(req.url);
    const clientVersionCode = parseInt(searchParams.get('version_code') || '0', 10);

    if (isSupabaseConfigured() && supabase) {
      const { data, error } = await supabase
        .from('app_apk_updates')
        .select('*')
        .eq('is_active', true)
        .order('version_code', { ascending: false })
        .limit(1)
        .maybeSingle();

      if (!error && data) {
        const updateAvailable = clientVersionCode > 0 ? data.version_code > clientVersionCode : true;
        return NextResponse.json(
          {
            success: true,
            update_available: updateAvailable,
            version_code: data.version_code,
            version_name: data.version_name,
            download_url: data.download_url,
            changelog: data.changelog || DEFAULT_UPDATE.changelog,
            is_mandatory: !!data.is_mandatory,
            file_size: data.file_size || 0,
            updated_at: data.updated_at,
          },
          { headers: corsHeaders }
        );
      }
    }

    return NextResponse.json(DEFAULT_UPDATE, { headers: corsHeaders });
  } catch (err: any) {
    return NextResponse.json(
      { ...DEFAULT_UPDATE, error: err.message || 'Failed to fetch APK update' },
      { headers: corsHeaders }
    );
  }
}

export async function POST(req: NextRequest) {
  const corsHeaders = {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization, x-admin-token',
  };

  try {
    const auth = requireAdmin(req);
    if (!auth.ok) {
      return NextResponse.json(
        { success: false, error: auth.error },
        { status: auth.status, headers: corsHeaders }
      );
    }

    const body = await req.json();

    const payload = {
      version_code: parseInt(body.version_code, 10) || 3,
      version_name: body.version_name || '2026.02.01',
      download_url: body.download_url || '',
      changelog: body.changelog || 'Performance improvements and bug fixes.',
      is_mandatory: !!body.is_mandatory,
      is_active: true,
      updated_at: new Date().toISOString(),
    };

    if (!payload.download_url) {
      return NextResponse.json(
        { success: false, error: 'APK download URL is required' },
        { status: 400, headers: corsHeaders }
      );
    }

    const db = getServerSupabase() || (isSupabaseConfigured() ? supabase : null);
    if (db) {
      // Deactivate older updates
      await db.from('app_apk_updates').update({ is_active: false }).eq('is_active', true);

      // Insert active record
      const { error } = await db.from('app_apk_updates').insert(payload);
      if (error) {
        return NextResponse.json({ success: false, error: error.message }, { status: 500, headers: corsHeaders });
      }
    }

    return NextResponse.json({ success: true, update: payload }, { headers: corsHeaders });
  } catch (err: any) {
    return NextResponse.json(
      { success: false, error: err.message || 'Failed to save APK update' },
      { status: 500, headers: corsHeaders }
    );
  }
}

export async function OPTIONS() {
  return new NextResponse(null, {
    status: 204,
    headers: {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type, Authorization',
    },
  });
}
