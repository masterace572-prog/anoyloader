import { NextRequest, NextResponse } from "next/server";
import { requireAdmin } from "@/lib/admin-auth";
import { isServerSupabaseConfigured } from "@/lib/supabase-server";
export const dynamic = "force-dynamic";
export async function GET(req: NextRequest) {
  const result = await requireAdmin(req);
  return NextResponse.json(
    {
      authenticated: result.ok,
      configured: isServerSupabaseConfigured(),
      error: result.ok ? null : result.error,
      status: result.ok ? 200 : result.status,
    },
    { headers: { "Cache-Control": "no-store" } },
  );
}
// Password verification, email confirmation and refresh are handled by Supabase Auth.
export async function POST() {
  return NextResponse.json(
    { error: "PIN login has been removed. Use account login." },
    { status: 410 },
  );
}
