-- Run after the existing backend setup and before deploying account auth.
BEGIN;
CREATE TABLE IF NOT EXISTS public.admin_accounts (
 user_id uuid PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
 is_approved boolean NOT NULL DEFAULT false,
 created_at timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE public.admin_accounts ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.admin_accounts FROM anon, authenticated;
GRANT ALL ON public.admin_accounts TO service_role;
CREATE TABLE IF NOT EXISTS public.management_api_keys (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
 user_id uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
 name varchar(80) NOT NULL, prefix text NOT NULL, key_hash text UNIQUE NOT NULL,
 scopes text[] NOT NULL CHECK (cardinality(scopes) BETWEEN 1 AND 2 AND scopes <@ ARRAY['licenses:read','licenses:write']::text[]),
 created_at timestamptz NOT NULL DEFAULT now(), expires_at timestamptz NOT NULL,
 revoked_at timestamptz, last_used_at timestamptz, request_count bigint NOT NULL DEFAULT 0
);
ALTER TABLE public.management_api_keys ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.management_api_keys FROM anon, authenticated;
GRANT ALL ON public.management_api_keys TO service_role;
CREATE INDEX IF NOT EXISTS management_api_keys_owner ON public.management_api_keys(user_id);
CREATE OR REPLACE FUNCTION public.record_management_api_usage(p_id uuid)
RETURNS void LANGUAGE sql SET search_path = public AS $$
 UPDATE public.management_api_keys SET request_count = request_count + 1, last_used_at = now() WHERE id = p_id;
$$;
REVOKE ALL ON FUNCTION public.record_management_api_usage(uuid) FROM PUBLIC, anon, authenticated;
GRANT EXECUTE ON FUNCTION public.record_management_api_usage(uuid) TO service_role;
-- Remove permissive legacy policies so direct database access cannot bypass auth.
DO $$
DECLARE p record;
BEGIN
 FOR p IN SELECT schemaname, tablename, policyname FROM pg_policies WHERE schemaname='public'
 AND tablename IN ('license_keys','lib_updates','system_config','app_apk_updates','managed_games','game_versions')
 LOOP EXECUTE format('DROP POLICY %I ON %I.%I',p.policyname,p.schemaname,p.tablename); END LOOP;
 FOR p IN SELECT tablename FROM pg_tables WHERE schemaname='public'
 AND tablename IN ('license_keys','lib_updates','system_config','app_apk_updates','managed_games','game_versions')
 LOOP
  EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY',p.tablename);
  EXECUTE format('REVOKE ALL ON public.%I FROM anon,authenticated',p.tablename);
  EXECUTE format('GRANT ALL ON public.%I TO service_role',p.tablename);
  IF p.tablename <> 'license_keys' THEN
   EXECUTE format('GRANT SELECT ON public.%I TO anon,authenticated',p.tablename);
   EXECUTE format('CREATE POLICY app_public_read ON public.%I FOR SELECT TO anon,authenticated USING (true)',p.tablename);
  END IF;
 END LOOP;
END $$;
-- Preserve existing Android client RPC and public config reads; restrict uploads.
DROP POLICY IF EXISTS "Allow authenticated/anon upload to libs storage" ON storage.objects;
DROP POLICY IF EXISTS "Allow anon upload and delete to storage buckets" ON storage.objects;
-- SECURITY DEFINER management RPCs bypass table RLS, so revoke legacy grants too.
DO $$
DECLARE f record;
BEGIN
 FOR f IN SELECT p.oid::regprocedure AS signature FROM pg_proc p
 JOIN pg_namespace n ON n.oid=p.pronamespace
 WHERE n.nspname='public' AND p.proname IN ('reset_key_hwid','extend_key_duration','delete_expired_keys')
 LOOP
  EXECUTE format('REVOKE ALL ON FUNCTION %s FROM PUBLIC,anon,authenticated',f.signature);
  EXECUTE format('GRANT EXECUTE ON FUNCTION %s TO service_role',f.signature);
  EXECUTE format('ALTER FUNCTION %s SET search_path = public',f.signature);
 END LOOP;
END $$;
COMMIT;
-- Approve a signup through trusted SQL Editor access:
-- INSERT INTO public.admin_accounts (user_id,is_approved)
-- SELECT id,true FROM auth.users WHERE lower(email)=lower('YOUR_ADMIN_EMAIL')
-- ON CONFLICT (user_id) DO UPDATE SET is_approved=true;
-- Revoke account access (also denies all its API keys):
-- UPDATE public.admin_accounts SET is_approved=false WHERE user_id='ACTUAL_USER_UUID';
