-- =========================================================================
-- ANOY LOADER - DEFINITIVE MASTER SUPABASE SETUP SCRIPT
-- Paste and Run this entire script in your Supabase SQL Editor:
-- https://supabase.com/dashboard/project/_/sql/new
-- Safe to run on fresh or existing databases (idempotent / non-destructive).
-- =========================================================================

-- Enable pgcrypto for UUID generation
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ==========================================================
-- 1. Table: license_keys
-- ==========================================================
CREATE TABLE IF NOT EXISTS public.license_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key VARCHAR(128) UNIQUE NOT NULL,
    duration_label VARCHAR(64) NOT NULL,
    duration_seconds BIGINT NOT NULL,
    max_devices INT NOT NULL DEFAULT 1,
    hwid_list TEXT[] NOT NULL DEFAULT '{}',
    status VARCHAR(20) NOT NULL DEFAULT 'UNUSED' CHECK (status IN ('UNUSED', 'ACTIVE', 'EXPIRED', 'BANNED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    activated_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    last_login_at TIMESTAMPTZ,
    last_ip VARCHAR(64),
    notes TEXT
);

CREATE INDEX IF NOT EXISTS idx_license_keys_key ON public.license_keys(key);
CREATE INDEX IF NOT EXISTS idx_license_keys_status ON public.license_keys(status);

ALTER TABLE public.license_keys ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Allow public read access to license_keys" ON public.license_keys;
CREATE POLICY "Allow public read access to license_keys" ON public.license_keys FOR SELECT USING (true);

DROP POLICY IF EXISTS "Allow anon insert and update to license_keys" ON public.license_keys;
CREATE POLICY "Allow anon insert and update to license_keys" ON public.license_keys FOR ALL USING (true);

-- Stored Procedure: verify_and_activate_key
CREATE OR REPLACE FUNCTION public.verify_and_activate_key(
    p_key TEXT,
    p_hwid TEXT,
    p_ip TEXT DEFAULT NULL
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_key_record public.license_keys%ROWTYPE;
    v_now TIMESTAMPTZ := now();
    v_device_count INT;
    v_is_lifetime BOOLEAN;
BEGIN
    p_key := trim(p_key);
    p_hwid := trim(p_hwid);

    SELECT * INTO v_key_record
    FROM public.license_keys
    WHERE key = p_key;

    IF NOT FOUND THEN
        RETURN jsonb_build_object(
            'success', false,
            'error', 'Invalid license key'
        );
    END IF;

    IF v_key_record.status = 'BANNED' THEN
        RETURN jsonb_build_object(
            'success', false,
            'error', 'This license key has been banned by administrator'
        );
    END IF;

    IF v_key_record.status = 'EXPIRED' THEN
        RETURN jsonb_build_object(
            'success', false,
            'error', 'This license key has expired'
        );
    END IF;

    v_is_lifetime := (v_key_record.duration_seconds <= 0);
    IF v_key_record.status = 'ACTIVE' AND NOT v_is_lifetime THEN
        IF v_key_record.expires_at IS NOT NULL AND v_key_record.expires_at < v_now THEN
            UPDATE public.license_keys
            SET status = 'EXPIRED'
            WHERE id = v_key_record.id;

            RETURN jsonb_build_object(
                'success', false,
                'error', 'This license key has expired'
            );
        END IF;
    END IF;

    IF v_key_record.status = 'UNUSED' THEN
        IF v_is_lifetime THEN
            v_key_record.expires_at := NULL;
        ELSE
            v_key_record.expires_at := v_now + (v_key_record.duration_seconds || ' seconds')::interval;
        END IF;

        UPDATE public.license_keys
        SET status = 'ACTIVE',
            activated_at = v_now,
            expires_at = v_key_record.expires_at,
            hwid_list = ARRAY[p_hwid],
            last_login_at = v_now,
            last_ip = p_ip
        WHERE id = v_key_record.id;

        RETURN jsonb_build_object(
            'success', true,
            'status', 'ACTIVE',
            'is_lifetime', v_is_lifetime,
            'activated_at', to_char(v_now, 'YYYY-MM-DD HH24:MI:SS'),
            'expires_at', CASE WHEN v_is_lifetime THEN 'LIFETIME' ELSE to_char(v_key_record.expires_at, 'YYYY-MM-DD HH24:MI:SS') END,
            'remaining_seconds', CASE WHEN v_is_lifetime THEN -1 ELSE v_key_record.duration_seconds END,
            'device_count', 1,
            'max_devices', v_key_record.max_devices
        );
    END IF;

    v_device_count := cardinality(v_key_record.hwid_list);

    IF p_hwid = ANY(v_key_record.hwid_list) THEN
        UPDATE public.license_keys
        SET last_login_at = v_now,
            last_ip = p_ip
        WHERE id = v_key_record.id;

        RETURN jsonb_build_object(
            'success', true,
            'status', 'ACTIVE',
            'is_lifetime', v_is_lifetime,
            'activated_at', to_char(v_key_record.activated_at, 'YYYY-MM-DD HH24:MI:SS'),
            'expires_at', CASE WHEN v_is_lifetime THEN 'LIFETIME' ELSE to_char(v_key_record.expires_at, 'YYYY-MM-DD HH24:MI:SS') END,
            'remaining_seconds', CASE WHEN v_is_lifetime THEN -1 ELSE GREATEST(0, EXTRACT(EPOCH FROM (v_key_record.expires_at - v_now))::BIGINT) END,
            'device_count', v_device_count,
            'max_devices', v_key_record.max_devices
        );
    END IF;

    IF v_device_count < v_key_record.max_devices THEN
        UPDATE public.license_keys
        SET hwid_list = array_append(v_key_record.hwid_list, p_hwid),
            last_login_at = v_now,
            last_ip = p_ip
        WHERE id = v_key_record.id;

        RETURN jsonb_build_object(
            'success', true,
            'status', 'ACTIVE',
            'is_lifetime', v_is_lifetime,
            'activated_at', to_char(v_key_record.activated_at, 'YYYY-MM-DD HH24:MI:SS'),
            'expires_at', CASE WHEN v_is_lifetime THEN 'LIFETIME' ELSE to_char(v_key_record.expires_at, 'YYYY-MM-DD HH24:MI:SS') END,
            'remaining_seconds', CASE WHEN v_is_lifetime THEN -1 ELSE GREATEST(0, EXTRACT(EPOCH FROM (v_key_record.expires_at - v_now))::BIGINT) END,
            'device_count', v_device_count + 1,
            'max_devices', v_key_record.max_devices
        );
    ELSE
        RETURN jsonb_build_object(
            'success', false,
            'error', 'Device limit reached (' || v_device_count || '/' || v_key_record.max_devices || '). Contact admin or reset HWID.'
        );
    END IF;
END;
$$;

-- Stored Procedure: reset_key_hwid
CREATE OR REPLACE FUNCTION public.reset_key_hwid(p_key TEXT)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    UPDATE public.license_keys
    SET hwid_list = '{}'
    WHERE key = trim(p_key);

    IF NOT FOUND THEN
        RETURN jsonb_build_object('success', false, 'error', 'Key not found');
    END IF;

    RETURN jsonb_build_object('success', true);
END;
$$;

-- Stored Procedure: extend_key_duration
CREATE OR REPLACE FUNCTION public.extend_key_duration(
    p_key TEXT,
    p_additional_seconds BIGINT
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_rec public.license_keys%ROWTYPE;
    v_new_expiry TIMESTAMPTZ;
BEGIN
    SELECT * INTO v_rec
    FROM public.license_keys
    WHERE key = trim(p_key);

    IF NOT FOUND THEN
        RETURN jsonb_build_object('success', false, 'error', 'Key not found');
    END IF;

    IF v_rec.duration_seconds <= 0 THEN
        RETURN jsonb_build_object('success', false, 'error', 'Key is already lifetime');
    END IF;

    IF v_rec.status = 'ACTIVE' AND v_rec.expires_at IS NOT NULL THEN
        IF v_rec.expires_at < now() THEN
            v_new_expiry := now() + (p_additional_seconds || ' seconds')::interval;
        ELSE
            v_new_expiry := v_rec.expires_at + (p_additional_seconds || ' seconds')::interval;
        END IF;

        UPDATE public.license_keys
        SET expires_at = v_new_expiry,
            status = 'ACTIVE'
        WHERE id = v_rec.id;

        RETURN jsonb_build_object('success', true, 'new_expires_at', to_char(v_new_expiry, 'YYYY-MM-DD HH24:MI:SS'));
    ELSE
        UPDATE public.license_keys
        SET duration_seconds = duration_seconds + p_additional_seconds
        WHERE id = v_rec.id;

        RETURN jsonb_build_object('success', true, 'duration_seconds', v_rec.duration_seconds + p_additional_seconds);
    END IF;
END;
$$;

-- Stored Procedure: delete_expired_keys
CREATE OR REPLACE FUNCTION public.delete_expired_keys()
RETURNS INT
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_deleted_count INT;
BEGIN
    DELETE FROM public.license_keys
    WHERE status = 'EXPIRED'
       OR (status = 'ACTIVE' AND duration_seconds > 0 AND expires_at < now());

    GET DIAGNOSTICS v_deleted_count = ROW_COUNT;
    RETURN v_deleted_count;
END;
$$;


-- ==========================================================
-- 2. Table: managed_games (Dynamic Game Catalog)
-- ==========================================================
CREATE TABLE IF NOT EXISTS public.managed_games (
    id VARCHAR(64) PRIMARY KEY,
    title VARCHAR(128) NOT NULL,
    package_name VARCHAR(128) NOT NULL,
    lib_name VARCHAR(64) NOT NULL DEFAULT 'libbgmi.so',
    icon_type VARCHAR(32) NOT NULL DEFAULT 'bgmi',
    is_enabled BOOLEAN NOT NULL DEFAULT true,
    status_text VARCHAR(64) NOT NULL DEFAULT 'OBB Ready',
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.managed_games ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Allow public read access to managed_games" ON public.managed_games;
CREATE POLICY "Allow public read access to managed_games" ON public.managed_games FOR SELECT USING (true);

DROP POLICY IF EXISTS "Allow insert/update to managed_games" ON public.managed_games;
CREATE POLICY "Allow insert/update to managed_games" ON public.managed_games FOR ALL USING (true);


-- ==========================================================
-- 3. Table: game_versions (Multi-Version Support per Game)
-- ==========================================================
CREATE TABLE IF NOT EXISTS public.game_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    game_id VARCHAR(64) NOT NULL REFERENCES public.managed_games(id) ON DELETE CASCADE,
    version_name VARCHAR(32) NOT NULL,
    version_code INT DEFAULT 0,
    obb_name VARCHAR(128) DEFAULT '',
    tag VARCHAR(32) NOT NULL DEFAULT 'LATEST',
    status_text VARCHAR(64) NOT NULL DEFAULT 'Ready',
    lib_name VARCHAR(64) DEFAULT '',
    lib_version VARCHAR(32) NOT NULL DEFAULT '1.0',
    lib_download_url TEXT NOT NULL DEFAULT '',
    is_default BOOLEAN NOT NULL DEFAULT false,
    is_active BOOLEAN NOT NULL DEFAULT true,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Ensure lib_name column exists if table already existed
ALTER TABLE public.game_versions ADD COLUMN IF NOT EXISTS lib_name TEXT DEFAULT '';

-- Ensure columns are nullable/defaulted for "COMING SOON" support
ALTER TABLE public.game_versions ALTER COLUMN version_code DROP NOT NULL;
ALTER TABLE public.game_versions ALTER COLUMN obb_name DROP NOT NULL;
ALTER TABLE public.game_versions ALTER COLUMN version_code SET DEFAULT 0;
ALTER TABLE public.game_versions ALTER COLUMN obb_name SET DEFAULT '';

CREATE INDEX IF NOT EXISTS idx_game_versions_game_id ON public.game_versions(game_id, is_active, sort_order);

ALTER TABLE public.game_versions ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Allow public read access to game_versions" ON public.game_versions;
CREATE POLICY "Allow public read access to game_versions" ON public.game_versions FOR SELECT USING (true);

DROP POLICY IF EXISTS "Allow insert/update to game_versions" ON public.game_versions;
CREATE POLICY "Allow insert/update to game_versions" ON public.game_versions FOR ALL USING (true);


-- ==========================================================
-- 4. Table: system_config (Maintenance Mode & In-App Announcements)
-- ==========================================================
CREATE TABLE IF NOT EXISTS public.system_config (
    id VARCHAR(32) PRIMARY KEY DEFAULT 'global',
    maintenance_mode BOOLEAN NOT NULL DEFAULT false,
    maintenance_message TEXT NOT NULL DEFAULT 'Server is currently undergoing scheduled maintenance. Please check back soon.',
    maintenance_estimated_end TEXT NOT NULL DEFAULT 'Soon',
    announcement_active BOOLEAN NOT NULL DEFAULT false,
    announcement_title VARCHAR(256) NOT NULL DEFAULT 'Server Announcement',
    announcement_message TEXT NOT NULL DEFAULT '',
    announcement_type VARCHAR(32) NOT NULL DEFAULT 'info',
    announcement_link TEXT,
    bgmi_enabled BOOLEAN NOT NULL DEFAULT true,
    bgmi_status VARCHAR(64) NOT NULL DEFAULT 'OBB Ready',
    pubg_enabled BOOLEAN NOT NULL DEFAULT true,
    pubg_status VARCHAR(64) NOT NULL DEFAULT 'OBB Ready',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.system_config ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Allow public read access to system_config" ON public.system_config;
CREATE POLICY "Allow public read access to system_config" ON public.system_config FOR SELECT USING (true);

DROP POLICY IF EXISTS "Allow update to system_config" ON public.system_config;
CREATE POLICY "Allow update to system_config" ON public.system_config FOR ALL USING (true);


-- ==========================================================
-- 5. Table: lib_updates (OTA Native Lib Updates)
-- ==========================================================
CREATE TABLE IF NOT EXISTS public.lib_updates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version VARCHAR(32) NOT NULL DEFAULT '1.0',
    download_url TEXT NOT NULL,
    storage_path TEXT,
    file_name VARCHAR(128),
    file_size BIGINT,
    changelog TEXT DEFAULT 'Performance and stability improvements.',
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_lib_updates_active ON public.lib_updates(is_active, updated_at DESC);

ALTER TABLE public.lib_updates ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Allow public read access to lib_updates" ON public.lib_updates;
CREATE POLICY "Allow public read access to lib_updates" ON public.lib_updates FOR SELECT USING (true);

DROP POLICY IF EXISTS "Allow insert/update to lib_updates" ON public.lib_updates;
CREATE POLICY "Allow insert/update to lib_updates" ON public.lib_updates FOR ALL USING (true);


-- ==========================================================
-- 6. Table: app_apk_updates (In-App APK Updates Management)
-- ==========================================================
CREATE TABLE IF NOT EXISTS public.app_apk_updates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version_code INT NOT NULL DEFAULT 2,
    version_name VARCHAR(32) NOT NULL DEFAULT '2026.01.01',
    download_url TEXT NOT NULL,
    changelog TEXT NOT NULL DEFAULT 'Performance improvements and bug fixes.',
    is_mandatory BOOLEAN NOT NULL DEFAULT false,
    is_active BOOLEAN NOT NULL DEFAULT true,
    file_size BIGINT DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.app_apk_updates ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Allow public read access to app_apk_updates" ON public.app_apk_updates;
CREATE POLICY "Allow public read access to app_apk_updates" ON public.app_apk_updates FOR SELECT USING (true);

DROP POLICY IF EXISTS "Allow insert/update to app_apk_updates" ON public.app_apk_updates;
CREATE POLICY "Allow insert/update to app_apk_updates" ON public.app_apk_updates FOR ALL USING (true);


-- ==========================================================
-- 7. Supabase Storage: Public Buckets for Libs and APKs
-- ==========================================================
INSERT INTO storage.buckets (id, name, public)
VALUES 
    ('libs', 'libs', true),
    ('loader-files', 'loader-files', true)
ON CONFLICT (id) DO UPDATE SET public = true;

DROP POLICY IF EXISTS "Allow public read access to storage buckets" ON storage.objects;
CREATE POLICY "Allow public read access to storage buckets" ON storage.objects
    FOR SELECT USING (bucket_id IN ('libs', 'loader-files'));

DROP POLICY IF EXISTS "Allow anon upload and delete to storage buckets" ON storage.objects;
CREATE POLICY "Allow anon upload and delete to storage buckets" ON storage.objects
    FOR ALL USING (bucket_id IN ('libs', 'loader-files'));


-- ==========================================================
-- 8. Default Seed Data (Games, Versions, Config & Test Key)
-- ==========================================================

-- Clean Titles: BGMI and PUBG GL
INSERT INTO public.managed_games (id, title, package_name, lib_name, icon_type, is_enabled, status_text, sort_order)
VALUES 
    ('bgmi', 'BGMI', 'com.pubg.imobile', 'libbgmi.so', 'bgmi', true, 'OBB Ready', 0),
    ('pubg_global', 'PUBG GL', 'com.tencent.ig', 'libpubgm.so', 'pubg_global', true, 'OBB Ready', 1)
ON CONFLICT (id) DO UPDATE 
SET title = EXCLUDED.title,
    package_name = EXCLUDED.package_name,
    lib_name = EXCLUDED.lib_name,
    icon_type = EXCLUDED.icon_type;

-- BGMI v4.5.0 (Build 21325)
INSERT INTO public.game_versions (game_id, version_name, version_code, obb_name, tag, status_text, lib_name, lib_version, lib_download_url, is_default, is_active, sort_order)
SELECT 'bgmi', '4.5.0', 21325, 'main.21325.com.pubg.imobile.obb', 'LATEST', 'Ready', 'libbgmi450.so', 'libbgmi450.so', '', true, true, 0
WHERE NOT EXISTS (SELECT 1 FROM public.game_versions WHERE game_id = 'bgmi' AND version_name = '4.5.0');

-- BGMI v4.6.0 (Build 21455)
INSERT INTO public.game_versions (game_id, version_name, version_code, obb_name, tag, status_text, lib_name, lib_version, lib_download_url, is_default, is_active, sort_order)
SELECT 'bgmi', '4.6.0', 21455, 'main.21455.com.pubg.imobile.obb', 'BETA', 'Beta Build', 'libbgmi460.so', 'libbgmi460.so', '', false, true, 1
WHERE NOT EXISTS (SELECT 1 FROM public.game_versions WHERE game_id = 'bgmi' AND version_name = '4.6.0');

-- PUBG GL v3.6.0 (Build 19120)
INSERT INTO public.game_versions (game_id, version_name, version_code, obb_name, tag, status_text, lib_name, lib_version, lib_download_url, is_default, is_active, sort_order)
SELECT 'pubg_global', '3.6.0', 19120, 'main.19120.com.tencent.ig.obb', 'LATEST', 'Ready', 'libpubgm360.so', 'libpubgm360.so', '', true, true, 0
WHERE NOT EXISTS (SELECT 1 FROM public.game_versions WHERE game_id = 'pubg_global' AND version_name = '3.6.0');

-- Backfill existing versions with lib_name if blank
UPDATE public.game_versions
SET lib_name = CASE
    WHEN version_name LIKE '4.5%' THEN 'libbgmi450.so'
    WHEN version_name LIKE '4.6%' THEN 'libbgmi460.so'
    WHEN version_name LIKE '3.6%' THEN 'libpubgm360.so'
    WHEN lib_version LIKE '%.so' THEN lib_version
    ELSE 'libbgmi.so'
END
WHERE lib_name IS NULL OR lib_name = '';

-- System Config Default Row
INSERT INTO public.system_config (id, maintenance_mode, maintenance_message, maintenance_estimated_end, bgmi_enabled, bgmi_status, pubg_enabled, pubg_status)
VALUES ('global', false, 'Server is currently undergoing scheduled maintenance. Please check back soon.', '1 Hour', true, 'OBB Ready', true, 'OBB Ready')
ON CONFLICT (id) DO NOTHING;

-- Default Initial Lib Record
INSERT INTO public.lib_updates (version, download_url, file_name, is_active)
SELECT '1.0', 'https://github.com/AkhilRyzen/Ryzen/releases/download/Ryzen/hb.zip', 'hb.zip', true
WHERE NOT EXISTS (SELECT 1 FROM public.lib_updates);

-- Default Test License Key: TESTTTT (7 Days)
INSERT INTO public.license_keys (key, duration_label, duration_seconds, max_devices, status, notes)
VALUES ('TESTTTT', '7 Days', 604800, 1, 'ACTIVE', 'Pre-configured master test key')
ON CONFLICT (key) DO NOTHING;
