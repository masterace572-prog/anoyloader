import { NextRequest, NextResponse } from 'next/server';
import { supabase, isSupabaseConfigured } from '@/lib/supabase';

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
    'Access-Control-Allow-Headers': 'Content-Type, Authorization',
    'Cache-Control': 'no-cache, no-store, must-revalidate',
  };

  try {
    let resolvedGames: ManagedGame[] = DEFAULT_GAMES;

    if (isSupabaseConfigured() && supabase) {
      try {
        const { data: dbGames, error: gamesErr } = await supabase
          .from('managed_games')
          .select('*')
          .order('sort_order', { ascending: true });

        if (!gamesErr && dbGames && dbGames.length > 0) {
          const { data: dbVersions } = await supabase
            .from('game_versions')
            .select('*')
            .eq('is_active', true)
            .order('sort_order', { ascending: true });

          const versionsList = dbVersions || [];
          resolvedGames = dbGames.map((g: any) => ({
            id: g.id,
            title: g.title,
            package_name: g.package_name,
            lib_name: g.lib_name || 'libbgmi.so',
            icon_type: g.icon_type || g.id,
            is_enabled: g.is_enabled !== undefined ? !!g.is_enabled : true,
            status_text: g.status_text || 'Ready',
            sort_order: g.sort_order || 0,
            versions: versionsList
              .filter((v: any) => v.game_id === g.id)
              .map((v: any) => ({
                id: v.id,
                game_id: v.game_id,
                version_name: v.version_name,
                version_code: v.version_code,
                obb_name: v.obb_name,
                tag: v.tag || 'LATEST',
                status_text: v.status_text || 'Ready',
                lib_version: v.lib_version || '1.0',
                lib_download_url: v.lib_download_url || '',
                is_default: !!v.is_default,
                is_active: !!v.is_active,
                sort_order: v.sort_order || 0,
              })),
          }));
        }
      } catch (e) {
        console.warn('Could not query managed_games from Supabase, using default games:', e);
      }

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
    'Access-Control-Allow-Headers': 'Content-Type, Authorization',
  };

  try {
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

    if (isSupabaseConfigured() && supabase) {
      const { error } = await supabase.from('system_config').upsert(payload);
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
      'Access-Control-Allow-Headers': 'Content-Type, Authorization',
    },
  });
}
