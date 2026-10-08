import { test } from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { PGlite } from "@electric-sql/pglite";

test("account migration closes direct table and definer-RPC bypasses, preserves app reads", async () => {
  const db = new PGlite();
  try {
    await db.exec(`
      CREATE ROLE anon; CREATE ROLE authenticated; CREATE ROLE service_role BYPASSRLS;
      CREATE SCHEMA auth; CREATE SCHEMA storage;
      CREATE TABLE auth.users(id uuid PRIMARY KEY, email text);
      CREATE TABLE storage.objects(id uuid, bucket_id text);
      ALTER TABLE storage.objects ENABLE ROW LEVEL SECURITY;
      CREATE POLICY "Allow anon upload and delete to storage buckets" ON storage.objects FOR ALL USING (true);
      CREATE TABLE public.license_keys(id uuid PRIMARY KEY, key text);
      CREATE TABLE public.system_config(id text PRIMARY KEY, maintenance_mode boolean);
      GRANT USAGE ON SCHEMA public TO anon,authenticated,service_role;
      GRANT ALL ON public.license_keys, public.system_config TO anon,authenticated;
      ALTER TABLE public.license_keys ENABLE ROW LEVEL SECURITY;
      ALTER TABLE public.system_config ENABLE ROW LEVEL SECURITY;
      CREATE POLICY old_open ON public.license_keys FOR ALL USING (true);
      CREATE POLICY old_open ON public.system_config FOR ALL USING (true);
      CREATE FUNCTION public.reset_key_hwid(text) RETURNS boolean LANGUAGE sql SECURITY DEFINER AS 'SELECT true';
      CREATE FUNCTION public.extend_key_duration(text,integer) RETURNS boolean LANGUAGE sql SECURITY DEFINER AS 'SELECT true';
      CREATE FUNCTION public.delete_expired_keys() RETURNS boolean LANGUAGE sql SECURITY DEFINER AS 'SELECT true';
      CREATE FUNCTION public.verify_and_activate_key(text,text,text DEFAULT NULL) RETURNS boolean LANGUAGE sql SECURITY DEFINER AS 'SELECT true';
      INSERT INTO public.system_config VALUES ('global',false);
      INSERT INTO auth.users VALUES ('11111111-1111-4111-8111-111111111111','test@example.test');
    `);
    const migration = readFileSync(
      new URL("../../supabase/migration_account_auth.sql", import.meta.url),
      "utf8",
    );
    await db.exec(migration);
    await db.exec(migration); // repeatable upgrade, not a destructive reset
    for (const role of ["anon", "authenticated"]) {
      await db.exec(`SET ROLE ${role}`);
      assert.equal(
        (await db.query("SELECT * FROM public.system_config")).rows.length,
        1,
      );
      assert.equal(
        (await db.query("SELECT public.verify_and_activate_key('a','b') AS ok"))
          .rows[0] && true,
        true,
      );
      for (const query of [
        "SELECT * FROM public.license_keys",
        "DELETE FROM public.license_keys",
        "SELECT * FROM public.management_api_keys",
        "INSERT INTO public.admin_accounts(user_id,is_approved) VALUES ('11111111-1111-4111-8111-111111111111',true)",
        "UPDATE public.system_config SET maintenance_mode=true",
        "SELECT public.reset_key_hwid('a')",
        "SELECT public.extend_key_duration('a',1)",
        "SELECT public.delete_expired_keys()",
        "SELECT public.record_management_api_usage('22222222-2222-4222-8222-222222222222')",
      ])
        await assert.rejects(db.query(query), /permission denied/, query);
      await db.exec("RESET ROLE");
    }
    await db.exec(`SET ROLE service_role;
      INSERT INTO public.admin_accounts(user_id,is_approved) VALUES ('11111111-1111-4111-8111-111111111111',true);
      INSERT INTO public.management_api_keys(id,user_id,name,prefix,key_hash,scopes,expires_at)
      VALUES ('22222222-2222-4222-8222-222222222222','11111111-1111-4111-8111-111111111111','test','prefix','hash',ARRAY['licenses:read'],now()+interval '1 day');
      SELECT public.record_management_api_usage('22222222-2222-4222-8222-222222222222');
    `);
    const usage = await db.query<{
      request_count: number;
      last_used_at: unknown;
    }>("SELECT request_count,last_used_at FROM public.management_api_keys");
    assert.equal(Number(usage.rows[0].request_count), 1);
    assert.ok(usage.rows[0].last_used_at);
    await db.exec("RESET ROLE");
    const policies = await db.query(
      "SELECT policyname FROM pg_policies WHERE schemaname='storage' AND cmd='ALL'",
    );
    assert.equal(policies.rows.length, 0);
  } finally {
    await db.close();
  }
});
