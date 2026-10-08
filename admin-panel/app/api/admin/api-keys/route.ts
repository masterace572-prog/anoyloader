import { createHash, randomBytes } from "crypto";
import { NextRequest, NextResponse } from "next/server";
import { requireAdmin } from "@/lib/admin-auth";
import { getServerSupabase } from "@/lib/supabase-server";
export const dynamic = "force-dynamic";
const fields =
  "id,name,prefix,scopes,created_at,expires_at,revoked_at,last_used_at,request_count";
function json(data: unknown, status = 200) {
  return NextResponse.json(data, {
    status,
    headers: { "Cache-Control": "no-store" },
  });
}
export async function GET(req: NextRequest) {
  const auth = await requireAdmin(req);
  if (!auth.ok) return json({ error: auth.error }, auth.status);
  const { data, error } = await getServerSupabase()!
    .from("management_api_keys")
    .select(fields)
    .eq("user_id", auth.userId)
    .order("created_at", { ascending: false });
  return error
    ? json({ error: "Could not load API keys." }, 500)
    : json({ keys: data });
}
export async function POST(req: NextRequest) {
  const auth = await requireAdmin(req);
  if (!auth.ok) return json({ error: auth.error }, auth.status);
  const body = await req.json().catch(() => ({}));
  if (!body || typeof body !== "object" || Array.isArray(body))
    return json({ error: "JSON object required." }, 400);
  const name = typeof body.name === "string" ? body.name.trim() : "";
  const days = Number(body.expires_in_days ?? 30);
  const scopes = body.scopes;
  if (
    !name ||
    name.length > 80 ||
    !Number.isInteger(days) ||
    days < 1 ||
    days > 365 ||
    !Array.isArray(scopes) ||
    !scopes.length ||
    scopes.length > 2 ||
    scopes.some((s) => !["licenses:read", "licenses:write"].includes(s))
  )
    return json(
      {
        error:
          "Provide a name (1–80 characters), valid scopes, and expiry (1–365 days).",
      },
      400,
    );
  const secret = `anoy_api_${randomBytes(32).toString("hex")}`;
  const { data, error } = await getServerSupabase()!
    .from("management_api_keys")
    .insert({
      user_id: auth.userId,
      name,
      scopes: Array.from(new Set(scopes)),
      prefix: secret.slice(0, 17),
      key_hash: createHash("sha256").update(secret).digest("hex"),
      expires_at: new Date(Date.now() + days * 86400000).toISOString(),
    })
    .select(fields)
    .single();
  return error
    ? json({ error: "Could not create API key." }, 500)
    : json({ key: data, secret }, 201);
}
export async function DELETE(req: NextRequest) {
  const auth = await requireAdmin(req);
  if (!auth.ok) return json({ error: auth.error }, auth.status);
  const id = req.nextUrl.searchParams.get("id");
  if (!id || !/^[0-9a-f-]{36}$/i.test(id))
    return json({ error: "Valid API key id required." }, 400);
  const { data, error } = await getServerSupabase()!
    .from("management_api_keys")
    .update({ revoked_at: new Date().toISOString() })
    .eq("id", id)
    .eq("user_id", auth.userId)
    .select("id")
    .maybeSingle();
  return error
    ? json({ error: "Could not revoke API key." }, 500)
    : !data
      ? json({ error: "API key not found." }, 404)
      : json({ success: true });
}
