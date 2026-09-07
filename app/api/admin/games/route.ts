import { NextRequest, NextResponse } from 'next/server';
import { supabase, isSupabaseConfigured } from '@/lib/supabase';
import { ManagedGame, GameVersion, DEFAULT_GAMES } from '@/lib/types';

export async function GET() {
  const corsHeaders = {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization',
    'Cache-Control': 'no-cache, no-store, must-revalidate',
  };

  try {
    if (isSupabaseConfigured() && supabase) {
      const { data: dbGames, error: gamesErr } = await supabase
        .from('managed_games')
        .select('*')
        .order('sort_order', { ascending: true });

      if (!gamesErr && dbGames && dbGames.length > 0) {
        const { data: dbVersions } = await supabase
          .from('game_versions')
          .select('*')
          .order('sort_order', { ascending: true });

        const versionsList = dbVersions || [];
        const games: ManagedGame[] = dbGames.map((g: any) => ({
          id: g.id,
          title: g.title,
          package_name: g.package_name,
          lib_name: g.lib_name || 'libbgmi.so',
          icon_type: g.icon_type || g.id,
          is_enabled: g.is_enabled !== undefined ? !!g.is_enabled : true,
          status_text: g.status_text || 'Ready',
          sort_order: g.sort_order || 0,
          created_at: g.created_at,
          updated_at: g.updated_at,
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
              created_at: v.created_at,
              updated_at: v.updated_at,
            })),
        }));

        return NextResponse.json({ success: true, games }, { headers: corsHeaders });
      }
    }

    return NextResponse.json({ success: true, games: DEFAULT_GAMES }, { headers: corsHeaders });
  } catch (err: any) {
    return NextResponse.json(
      { success: false, error: err.message || 'Failed to fetch games' },
      { status: 500, headers: corsHeaders }
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
    const { action } = body;

    if (!isSupabaseConfigured() || !supabase) {
      return NextResponse.json(
        { success: false, error: 'Database not connected' },
        { status: 500, headers: corsHeaders }
      );
    }

    if (action === 'upsert_game') {
      const { game } = body;
      if (!game || !game.id || !game.title || !game.package_name) {
        return NextResponse.json(
          { success: false, error: 'Game ID, title, and package_name are required' },
          { status: 400, headers: corsHeaders }
        );
      }

      const payload = {
        id: game.id.trim().toLowerCase(),
        title: game.title.trim(),
        package_name: game.package_name.trim(),
        lib_name: game.lib_name?.trim() || 'libbgmi.so',
        icon_type: game.icon_type || 'bgmi',
        is_enabled: game.is_enabled !== undefined ? !!game.is_enabled : true,
        status_text: game.status_text?.trim() || 'OBB Ready',
        sort_order: Number(game.sort_order) || 0,
        updated_at: new Date().toISOString(),
      };

      const { data, error } = await supabase
        .from('managed_games')
        .upsert(payload)
        .select()
        .single();

      if (error) {
        return NextResponse.json({ success: false, error: error.message }, { status: 500, headers: corsHeaders });
      }

      return NextResponse.json({ success: true, game: data }, { headers: corsHeaders });
    }

    if (action === 'delete_game') {
      const { id } = body;
      if (!id) {
        return NextResponse.json({ success: false, error: 'Game ID is required' }, { status: 400, headers: corsHeaders });
      }

      const { error } = await supabase.from('managed_games').delete().eq('id', id);
      if (error) {
        return NextResponse.json({ success: false, error: error.message }, { status: 500, headers: corsHeaders });
      }

      return NextResponse.json({ success: true, id }, { headers: corsHeaders });
    }

    if (action === 'upsert_version') {
      const { version } = body;
      if (!version || !version.game_id || !version.version_name || !version.version_code) {
        return NextResponse.json(
          { success: false, error: 'Game ID, version_name, and version_code are required' },
          { status: 400, headers: corsHeaders }
        );
      }

      // If set as default, unset existing defaults for this game
      if (version.is_default) {
        await supabase
          .from('game_versions')
          .update({ is_default: false })
          .eq('game_id', version.game_id);
      }

      const payload: any = {
        game_id: version.game_id,
        version_name: version.version_name.trim(),
        version_code: parseInt(version.version_code, 10),
        obb_name: version.obb_name?.trim() || `main.${version.version_code}.${version.package_name || 'com.pubg.imobile'}.obb`,
        tag: version.tag?.trim().toUpperCase() || 'LATEST',
        status_text: version.status_text?.trim() || 'Ready',
        lib_version: version.lib_version?.trim() || '1.0',
        lib_download_url: version.lib_download_url?.trim() || '',
        is_default: !!version.is_default,
        is_active: version.is_active !== undefined ? !!version.is_active : true,
        sort_order: Number(version.sort_order) || 0,
        updated_at: new Date().toISOString(),
      };

      if (version.id) {
        payload.id = version.id;
      }

      const { data, error } = await supabase
        .from('game_versions')
        .upsert(payload)
        .select()
        .single();

      if (error) {
        return NextResponse.json({ success: false, error: error.message }, { status: 500, headers: corsHeaders });
      }

      return NextResponse.json({ success: true, version: data }, { headers: corsHeaders });
    }

    if (action === 'delete_version') {
      const { id } = body;
      if (!id) {
        return NextResponse.json({ success: false, error: 'Version ID is required' }, { status: 400, headers: corsHeaders });
      }

      const { error } = await supabase.from('game_versions').delete().eq('id', id);
      if (error) {
        return NextResponse.json({ success: false, error: error.message }, { status: 500, headers: corsHeaders });
      }

      return NextResponse.json({ success: true, id }, { headers: corsHeaders });
    }

    return NextResponse.json({ success: false, error: 'Invalid action' }, { status: 400, headers: corsHeaders });
  } catch (err: any) {
    return NextResponse.json(
      { success: false, error: err.message || 'Internal server error' },
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
