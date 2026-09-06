import { NextRequest, NextResponse } from 'next/server';
import { createClient } from '@supabase/supabase-js';

const supabaseUrl = process.env.NEXT_PUBLIC_SUPABASE_URL || '';
const supabaseServiceKey = process.env.SUPABASE_SERVICE_ROLE_KEY || process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY || '';

export async function POST(req: NextRequest) {
  try {
    const body = await req.json();
    const { key, hwid } = body;
    const clientIp = req.headers.get('x-forwarded-for') || req.headers.get('x-real-ip') || 'unknown';

    if (!key || typeof key !== 'string' || !key.trim()) {
      return NextResponse.json({ success: false, error: 'Key is required' }, { status: 400 });
    }

    const trimmedKey = key.trim();
    if (trimmedKey.startsWith('BCORE-') || trimmedKey.startsWith('SDK-')) {
      return NextResponse.json({
        success: false,
        error: 'This is a Bcore SDK Key. Please use a valid Loader key.',
      }, { status: 400 });
    }

    if (!hwid || typeof hwid !== 'string' || !hwid.trim()) {
      return NextResponse.json({ success: false, error: 'Hardware ID (HWID) is required' }, { status: 400 });
    }

    if (!supabaseUrl || !supabaseServiceKey || supabaseUrl.includes('your-project-id')) {
      return NextResponse.json({
        success: false,
        error: 'Backend database is not configured. Please set Supabase URL and Key in .env.local.'
      }, { status: 500 });
    }

    const supabase = createClient(supabaseUrl, supabaseServiceKey);

    // Call stored procedure
    const { data, error } = await supabase.rpc('verify_and_activate_key', {
      p_key: key.trim(),
      p_hwid: hwid.trim(),
      p_ip: clientIp,
    });

    const corsHeaders = {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'POST, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type, Authorization',
    };

    if (error) {
      console.error('Supabase RPC error:', error);
      return NextResponse.json({ success: false, error: error.message }, { status: 500, headers: corsHeaders });
    }

    return NextResponse.json(data, { headers: corsHeaders });
  } catch (err: any) {
    console.error('Login API error:', err);
    return NextResponse.json(
      { success: false, error: err.message || 'Internal server error' },
      {
        status: 500,
        headers: {
          'Access-Control-Allow-Origin': '*',
          'Access-Control-Allow-Methods': 'POST, OPTIONS',
          'Access-Control-Allow-Headers': 'Content-Type, Authorization',
        },
      }
    );
  }
}

export async function OPTIONS() {
  return new NextResponse(null, {
    status: 204,
    headers: {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'POST, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type, Authorization',
    },
  });
}
