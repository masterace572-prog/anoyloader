import { NextResponse } from 'next/server';
import { createClient } from '@supabase/supabase-js';

const supabaseUrl = process.env.NEXT_PUBLIC_SUPABASE_URL || '';
const supabaseServiceKey = process.env.SUPABASE_SERVICE_ROLE_KEY || process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY || '';

// Fallback default in case database is empty or unconfigured
const DEFAULT_LIB_UPDATE = {
  version: '1.0',
  download_url: 'https://github.com/AkhilRyzen/Ryzen/releases/download/Ryzen/hb.zip',
  updated_at: new Date().toISOString(),
};

export async function GET() {
  try {
    if (!supabaseUrl || !supabaseServiceKey || supabaseUrl.includes('your-project-id')) {
      return NextResponse.json({
        success: true,
        ...DEFAULT_LIB_UPDATE,
        source: 'local_fallback',
      });
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
      return NextResponse.json({
        success: true,
        ...DEFAULT_LIB_UPDATE,
        source: 'default',
      });
    }

    return NextResponse.json({
      success: true,
      version: data.version,
      download_url: data.download_url,
      updated_at: data.updated_at,
      file_name: data.file_name,
      source: 'supabase',
    });
  } catch (err: any) {
    return NextResponse.json({
      success: true,
      ...DEFAULT_LIB_UPDATE,
      error: err.message,
    });
  }
}
