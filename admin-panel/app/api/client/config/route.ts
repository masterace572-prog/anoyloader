export const dynamic = 'force-dynamic';
export const revalidate = 0;

import { NextRequest, NextResponse } from 'next/server';
import { supabase, isSupabaseConfigured } from '@/lib/supabase';
import { requireAdmin } from '@/lib/admin-auth';
import { getServerSupabase } from '@/lib/supabase-server';

import { ManagedGame, GameVersion, DEFAULT_GAMES } from '@/lib/types';

// System Config Response Model
export interface SystemConfigResponse {
  success: boolean;
  maintenance_mode: boolean;
  maintenance_message: string;
  maintenance_estimated_end: string;
  announcement_active: boolean;
  announcement_title: string;
  announcement_message: string;
  announcement_type: 'info' | 'warning' | 'critical';
  announcement_link?: string;
  bgmi_enabled: boolean;
  bgmi_status: string;
  pubg_enabled: boolean;
  pubg_status: string;
  games: ManagedGame[];
  updated_at?: string;
  error?: string;
}

const DEFAULT_CONFIG: SystemConfigResponse = {
  success: true,
  maintenance_mode: false,
  maintenance_message: 'Server is currently undergoing scheduled maintenance. Please check back soon.',
  maintenance_estimated_end: 'Soon',
  announcement_active: false,
  announcement_title: 'Server Announcement',
  announcement_message: '',
  announcement_type: 'info',
  announcement_link: '',
  bgmi_enabled: true,
  bgmi_status: 'OBB Ready',
  pubg_enabled: true,
  pubg_status: 'OBB Ready',
  games: DEFAULT_GAMES,
  updated_at: new Date().toISOString(),
};

export async function GET() {
  const corsHeaders = {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization, x-admin-token',
    'Cache-Control': 'no-cache, no-store, must-revalidate',
  };

  try {
    const resolvedGames: ManagedGame[] = DEFAULT_GAMES;

    if (isSupabaseConfigured() && supabase) {
      const { data, error } = await supabase
        .from('system_config')
        .select('*')
        .eq('id', 'global')
        .maybeSingle();

      if (!error && data) {
        return NextResponse.json(
          {
            success: true,
            maintenance_mode: !!data.maintenance_mode,
            maintenance_message: data.maintenance_message || DEFAULT_CONFIG.maintenance_message,
            maintenance_estimated_end: data.maintenance_estimated_end || DEFAULT_CONFIG.maintenance_estimated_end,
            announcement_active: !!data.announcement_active,
            announcement_title: data.announcement_title || '',
            announcement_message: data.announcement_message || '',
            announcement_type: data.announcement_type || 'info',
            announcement_link: data.announcement_link || '',
            bgmi_enabled: data.bgmi_enabled !== undefined ? !!data.bgmi_enabled : true,
            bgmi_status: data.bgmi_status || 'OBB Ready',
            pubg_enabled: data.pubg_enabled !== undefined ? !!data.pubg_enabled : true,
            pubg_status: data.pubg_status || 'OBB Ready',
            games: resolvedGames,
            updated_at: data.updated_at || new Date().toISOString(),
          },
          { headers: corsHeaders }
        );
      }
    }

    return NextResponse.json({ ...DEFAULT_CONFIG, games: resolvedGames }, { headers: corsHeaders });
  } catch (error: any) {
    return NextResponse.json(
      {
        ...DEFAULT_CONFIG,
        error: error.message || 'Failed to fetch system config',
      },
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
      id: 'global',
      maintenance_mode: !!body.maintenance_mode,
      maintenance_message: body.maintenance_message || DEFAULT_CONFIG.maintenance_message,
      maintenance_estimated_end: body.maintenance_estimated_end || DEFAULT_CONFIG.maintenance_estimated_end,
      announcement_active: !!body.announcement_active,
      announcement_title: body.announcement_title || '',
      announcement_message: body.announcement_message || '',
      announcement_type: body.announcement_type || 'info',
      announcement_link: body.announcement_link || '',
      bgmi_enabled: body.bgmi_enabled !== undefined ? !!body.bgmi_enabled : true,
      bgmi_status: body.bgmi_status || 'OBB Ready',
      pubg_enabled: body.pubg_enabled !== undefined ? !!body.pubg_enabled : true,
      pubg_status: body.pubg_status || 'OBB Ready',
      updated_at: new Date().toISOString(),
    };

    const db = getServerSupabase() || (isSupabaseConfigured() ? supabase : null);
    if (db) {
      const { error } = await db.from('system_config').upsert(payload);
      if (error) {
        return NextResponse.json({ success: false, error: error.message }, { status: 500, headers: corsHeaders });
      }
    }

    return NextResponse.json({ success: true, config: payload }, { headers: corsHeaders });
  } catch (error: any) {
    return NextResponse.json(
      { success: false, error: error.message || 'Failed to save system config' },
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
      'Access-Control-Allow-Headers': 'Content-Type, Authorization, x-admin-token',
    },
  });
}
