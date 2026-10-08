import { NextRequest } from "next/server";
import { createHash } from "crypto";
import { getServerSupabase } from "./supabase-server";

export type AuthResult =
  { ok: true; userId: string } | { ok: false; status: number; error: string };
export function extractAdminToken(req: NextRequest): string | null {
  const auth = req.headers.get("authorization") || "";
  return auth.toLowerCase().startsWith("bearer ") ? auth.slice(7).trim() : null;
}

/** Always checks approval in the database, including on every API-key request. */
export async function requireAdmin(
  req: NextRequest,
  scope?: "licenses:read" | "licenses:write",
): Promise<AuthResult> {
  const client = getServerSupabase();
  if (!client)
    return {
      ok: false,
      status: 503,
      error: "Server database is not configured.",
    };
  const token = extractAdminToken(req);
  if (!token) return { ok: false, status: 401, error: "Sign in to continue." };
  let userId: string;
  let apiKeyId: string | undefined;
  if (token.startsWith("anoy_api_")) {
    // Management API keys cannot mint more keys or alter system settings.
    if (!scope)
      return {
        ok: false,
        status: 403,
        error: "An account session is required.",
      };
    const hash = createHash("sha256").update(token).digest("hex");
    const { data: key, error } = await client
      .from("management_api_keys")
      .select("id,user_id,scopes,expires_at,revoked_at")
      .eq("key_hash", hash)
      .maybeSingle();
    if (error)
      return {
        ok: false,
        status: 503,
        error: "API authentication is unavailable.",
      };
    if (
      !key ||
      key.revoked_at ||
      new Date(key.expires_at).getTime() <= Date.now()
    )
      return {
        ok: false,
        status: 401,
        error: "Invalid, expired, or revoked API key.",
      };
    if (!key.scopes.includes(scope))
      return { ok: false, status: 403, error: `Missing permission: ${scope}` };
    userId = key.user_id;
    apiKeyId = key.id;
  } else {
    const { data, error } = await client.auth.getUser(token);
    if (error || !data.user)
      return {
        ok: false,
        status: 401,
        error: "Session expired. Sign in again.",
      };
    userId = data.user.id;
  }
  const { data: admin, error } = await client
    .from("admin_accounts")
    .select("user_id")
    .eq("user_id", userId)
    .eq("is_approved", true)
    .maybeSingle();
  if (error)
    return {
      ok: false,
      status: 503,
      error: "Admin approval lookup is unavailable. Run the account migration.",
    };
  if (!admin)
    return {
      ok: false,
      status: 403,
      error: "Your account is awaiting administrator approval.",
    };
  if (apiKeyId) {
    const { error: usageError } = await client.rpc(
      "record_management_api_usage",
      { p_id: apiKeyId },
    );
    if (usageError)
      return {
        ok: false,
        status: 503,
        error: "API usage tracking is unavailable.",
      };
  }
  return { ok: true, userId };
}
