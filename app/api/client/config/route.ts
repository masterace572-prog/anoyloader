import { NextRequest, NextResponse } from 'next/server';
import { supabase, isSupabaseConfigured } from '@/lib/supabase';

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
            updated_at: data.updated_at || new Date().toISOString(),
          },
          { headers: corsHeaders }
        );
      }
    }

    return NextResponse.json(DEFAULT_CONFIG, { headers: corsHeaders });
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
