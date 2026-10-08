import { test, beforeEach } from "node:test";
import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import { NextRequest } from "next/server";
import { requireAdmin } from "../lib/admin-auth";
import * as credentials from "../app/api/admin/api-keys/route";
import * as licenses from "../app/api/v1/licenses/route";
import * as authRoute from "../app/api/admin/auth/route";

const owner = "11111111-1111-4111-8111-111111111111";
const id = "22222222-2222-4222-8222-222222222222";
let approved: boolean,
  record: any,
  saved: any,
  licenseRows: any[],
  calls: { path: string; init: RequestInit }[];
beforeEach(() => {
  process.env.NEXT_PUBLIC_SUPABASE_URL = "https://mock.supabase.test";
  process.env.SUPABASE_SERVICE_ROLE_KEY = "service-role-for-tests-only";
  approved = true;
  record = {
    id,
    user_id: owner,
    scopes: ["licenses:read"],
    expires_at: "2099-01-01",
    revoked_at: null,
  };
  saved = null;
  licenseRows = [];
  calls = [];
  globalThis.fetch = async (input, init = {}) => {
    const url = new URL(String(input));
    calls.push({ path: url.pathname + url.search, init });
    let data: any = null;
    if (url.pathname === "/auth/v1/user") {
      if (
        new Headers(init.headers).get("authorization") !== "Bearer session-test"
      )
        return Response.json({ message: "invalid" }, { status: 401 });
      data = { id: owner, email: "admin@example.test" };
    } else if (url.pathname.endsWith("/admin_accounts"))
      data = approved ? { user_id: owner } : null;
    else if (url.pathname.endsWith("/management_api_keys")) {
      if (init.method === "POST") {
        saved = JSON.parse(String(init.body));
        const { key_hash, user_id, ...publicFields } = saved;
        data = { ...publicFields, id };
      } else if (init.method === "PATCH") {
        data =
          url.searchParams.get("user_id") === `eq.${owner}` ? { id } : null;
      } else data = url.searchParams.has("key_hash") ? record : [];
    } else if (url.pathname.endsWith("/license_keys")) {
      const licenseId = url.searchParams.get("id")?.replace(/^eq\./, "");
      if (init.method === "POST") {
        const payload = JSON.parse(String(init.body));
        licenseRows = payload.map((row: any, i: number) => ({
          ...row,
          id: i === 0 ? id : `test-${i}`,
        }));
        data = new Headers(init.headers)
          .get("accept")
          ?.includes("vnd.pgrst.object")
          ? licenseRows[0]
          : licenseRows;
      } else if (init.method === "PATCH") {
        const row = licenseRows.find((row) => row.id === licenseId);
        if (row) Object.assign(row, JSON.parse(String(init.body)));
        data = row || null;
      } else if (init.method === "DELETE") {
        data = licenseRows.find((row) => row.id === licenseId) || null;
        licenseRows = licenseRows.filter((row) => row.id !== licenseId);
      } else
        data = licenseId
          ? licenseRows.find((row) => row.id === licenseId) || null
          : licenseRows;
    }
    return Response.json(data);
  };
});
function request(
  token?: string,
  body?: unknown,
  url = "/api/v1/licenses",
  method?: string,
) {
  return new NextRequest(`https://anoy.test${url}`, {
    method: method || (body !== undefined ? "POST" : "GET"),
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      "Content-Type": "application/json",
    },
    ...(body !== undefined ? { body: JSON.stringify(body) } : {}),
  });
}
test("missing server service role fails closed, never falls back to anon", async () => {
  delete process.env.SUPABASE_SERVICE_ROLE_KEY;
  process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY = "anon";
  assert.deepEqual(await requireAdmin(request("session-test")), {
    ok: false,
    status: 503,
    error: "Server database is not configured.",
  });
  assert.equal(calls.length, 0);
});
test("missing and invalid sessions are denied; approved session succeeds", async () => {
  assert.equal((await requireAdmin(request())).ok, false);
  assert.equal((await requireAdmin(request("forged-token"))).ok, false);
  assert.deepEqual(await requireAdmin(request("session-test")), {
    ok: true,
    userId: owner,
  });
});
test("signup alone does not grant administration", async () => {
  approved = false;
  const result = await requireAdmin(request("session-test"));
  assert.equal(result.ok, false);
  if (!result.ok) assert.equal(result.status, 403);
});
test("legacy PIN endpoint is gone", async () => {
  assert.equal((await authRoute.POST()).status, 410);
});
test("API key lookup uses SHA256, not plaintext, and tracks approved usage", async () => {
  assert.equal(
    (await requireAdmin(request("anoy_api_secret"), "licenses:read")).ok,
    true,
  );
  const lookup = calls.find((c) => c.path.includes("key_hash"))!;
  assert.ok(
    lookup.path.includes(
      createHash("sha256").update("anoy_api_secret").digest("hex"),
    ),
  );
  assert.ok(!lookup.path.includes("anoy_api_secret"));
  assert.ok(
    calls.some((c) => c.path.endsWith("/rpc/record_management_api_usage")),
  );
});
test("API keys cannot mint credentials or access system admin routes", async () => {
  const result = await requireAdmin(request("anoy_api_secret"));
  assert.equal(result.ok, false);
  if (!result.ok) assert.equal(result.status, 403);
});
test("read-only credentials cannot write", async () => {
  assert.equal(
    (await licenses.POST(request("anoy_api_secret", { action: "delete", id })))
      .status,
    403,
  );
  assert.ok(!calls.some((c) => c.path.includes("/license_keys")));
});
test("expired and revoked credentials fail", async () => {
  record.expires_at = "2000-01-01";
  assert.equal((await licenses.GET(request("anoy_api_secret"))).status, 401);
  record.expires_at = "2099-01-01";
  record.revoked_at = "2026-01-01";
  assert.equal((await licenses.GET(request("anoy_api_secret"))).status, 401);
});
test("revoked account immediately loses API access", async () => {
  approved = false;
  assert.equal((await licenses.GET(request("anoy_api_secret"))).status, 403);
  assert.ok(!calls.some((c) => c.path.includes("record_management_api_usage")));
});
test("credential creation stores only a hash and returns secret once", async () => {
  const response = await credentials.POST(
    request("session-test", {
      name: "Integration",
      scopes: ["licenses:read"],
      expires_in_days: 30,
    }),
  );
  assert.equal(response.status, 201);
  const body = await response.json();
  assert.match(body.secret, /^anoy_api_[a-f0-9]{64}$/);
  assert.equal(
    saved.key_hash,
    createHash("sha256").update(body.secret).digest("hex"),
  );
  assert.equal(saved.user_id, owner);
  assert.ok(!JSON.stringify(saved).includes(body.secret));
  assert.equal(body.key.key_hash, undefined);
});
test("invalid scopes, expiry, and JSON shape rejected", async () => {
  for (const body of [
    null,
    [],
    { name: "test", scopes: ["admin"], expires_in_days: 2 },
    { name: "test", scopes: ["licenses:read"], expires_in_days: 0 },
  ]) {
    assert.equal(
      (await credentials.POST(request("session-test", body))).status,
      400,
    );
  }
});
test("credential list omits secret/hash columns and is owner scoped", async () => {
  assert.equal((await credentials.GET(request("session-test"))).status, 200);
  const query = new URL(
    "https://test" +
      calls.find((c) => c.path.includes("/management_api_keys"))!.path,
  );
  assert.ok(!query.searchParams.get("select")!.includes("hash"));
  assert.equal(query.searchParams.get("user_id"), `eq.${owner}`);
});
test("revocation constrains mutation to authenticated owner", async () => {
  assert.equal(
    (
      await credentials.DELETE(
        request(
          "session-test",
          undefined,
          `/api/admin/api-keys?id=${id}`,
          "DELETE",
        ),
      )
    ).status,
    200,
  );
  assert.ok(
    calls.some(
      (c) =>
        c.init.method === "PATCH" && c.path.includes(`user_id=eq.${owner}`),
    ),
  );
});
test("license mutation validation blocks malformed IDs and oversized batches", async () => {
  for (const body of [
    null,
    { action: "delete", id: "broken" },
    {
      action: "bulk",
      count: 51,
      duration_label: "Day",
      duration_seconds: 86400,
    },
    { action: "create", duration_label: "Day", duration_seconds: 1e100 },
  ]) {
    assert.equal(
      (await licenses.POST(request("session-test", body))).status,
      400,
    );
  }
});
test("delete returns 404 for a missing license", async () => {
  assert.equal(
    (await licenses.POST(request("session-test", { action: "delete", id })))
      .status,
    404,
  );
});

test("write-scoped key performs the complete license lifecycle", async () => {
  record.scopes = ["licenses:read", "licenses:write"];
  const created = await licenses.POST(
    request("anoy_api_secret", {
      action: "create",
      duration_label: "7 Days",
      duration_seconds: 604800,
      max_devices: 1,
    }),
  );
  assert.equal(created.status, 200);
  const { key } = await created.json();
  assert.match(key.key, /^ANOY-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}$/);
  assert.equal(key.status, "UNUSED");
  assert.equal(key.expires_at, null);
  assert.equal(
    (await (await licenses.GET(request("anoy_api_secret"))).json()).keys.length,
    1,
  );
  const ban = await (
    await licenses.POST(
      request("anoy_api_secret", { action: "toggle_ban", id }),
    )
  ).json();
  assert.equal(ban.key.status, "BANNED");
  const unban = await (
    await licenses.POST(
      request("anoy_api_secret", { action: "toggle_ban", id }),
    )
  ).json();
  assert.equal(unban.key.status, "UNUSED");
  const extended = await (
    await licenses.POST(
      request("anoy_api_secret", { action: "extend", id, seconds: 86400 }),
    )
  ).json();
  assert.equal(extended.key.status, "ACTIVE");
  assert.ok(extended.key.activated_at);
  assert.ok(new Date(extended.key.expires_at).getTime() > Date.now());
  licenseRows[0].hwid_list = ["device"];
  const reset = await (
    await licenses.POST(
      request("anoy_api_secret", { action: "reset_hwid", id }),
    )
  ).json();
  assert.deepEqual(reset.key.hwid_list, []);
  assert.equal(
    (await licenses.POST(request("anoy_api_secret", { action: "delete", id })))
      .status,
    200,
  );
  assert.equal(licenseRows.length, 0);
});
test("bulk creates unique immediate licenses, lifetime creation remains unexpired", async () => {
  const bulk = await (
    await licenses.POST(
      request("session-test", {
        action: "bulk",
        count: 50,
        duration_label: "Day",
        duration_seconds: 86400,
        activation_timing: "IMMEDIATE",
      }),
    )
  ).json();
  assert.equal(bulk.keys.length, 50);
  assert.equal(new Set(bulk.generated).size, 50);
  assert.ok(
    bulk.keys.every((key: any) => key.status === "ACTIVE" && key.expires_at),
  );
  const lifetime = await (
    await licenses.POST(
      request("session-test", {
        action: "create",
        duration_label: "Lifetime",
        duration_seconds: 0,
      }),
    )
  ).json();
  assert.equal(lifetime.key.expires_at, null);
  assert.equal(
    (
      await licenses.POST(
        request("session-test", { action: "extend", id, seconds: 86400 }),
      )
    ).status,
    400,
  );
});
