import { NextRequest, NextResponse } from 'next/server';
import {
  isAdminPinConfigured,
  verifyAdminPin,
  issueAdminSessionToken,
  verifyAdminSessionToken,
  extractAdminToken,
} from '@/lib/admin-auth';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type, Authorization, x-admin-token',
  'Cache-Control': 'no-store',
};

/**
 * POST /api/admin/auth
 * Body: { pin: string }
 * Verifies against process.env.ADMIN_PIN (Vercel secret) and returns a signed session token.
 */
export async function POST(req: NextRequest) {
  try {
    if (!isAdminPinConfigured()) {
      return NextResponse.json(
        {
          success: false,
          error:
            'ADMIN_PIN is not set. Add it under Vercel → Project → Settings → Environment Variables, then redeploy.',
        },
        { status: 503, headers: corsHeaders }
      );
    }

    const body = await req.json().catch(() => ({}));
    const pin = typeof body?.pin === 'string' ? body.pin.trim() : '';

    if (!pin) {
      return NextResponse.json(
        { success: false, error: 'PIN is required' },
        { status: 400, headers: corsHeaders }
      );
    }

    if (!verifyAdminPin(pin)) {
      // uniform delay to slow brute force a bit
      await new Promise((r) => setTimeout(r, 400));
      return NextResponse.json(
        { success: false, error: 'Invalid PIN' },
        { status: 401, headers: corsHeaders }
      );
    }

    const token = issueAdminSessionToken();
    if (!token) {
      return NextResponse.json(
        { success: false, error: 'Failed to issue session token' },
        { status: 500, headers: corsHeaders }
      );
    }

    return NextResponse.json(
      {
        success: true,
        token,
        expires_in_hours: 12,
      },
      { headers: corsHeaders }
    );
  } catch (err: any) {
    return NextResponse.json(
      { success: false, error: err?.message || 'Auth failed' },
      { status: 500, headers: corsHeaders }
    );
  }
}

/**
 * GET /api/admin/auth
 * Validates an existing session token (Authorization: Bearer … or x-admin-token).
 * Does NOT reveal whether ADMIN_PIN is set beyond a boolean flag.
 */
export async function GET(req: NextRequest) {
  const configured = isAdminPinConfigured();
  const token = extractAdminToken(req);
  const valid = configured && verifyAdminSessionToken(token);
  return NextResponse.json(
    {
      success: true,
      configured,
      authenticated: valid,
    },
    { headers: corsHeaders }
  );
}

export async function OPTIONS() {
  return new NextResponse(null, { status: 204, headers: corsHeaders });
}
