-- ==========================================================
-- VIPER LOADER - SUPABASE DATABASE SCHEMA
-- Execute this script in your Supabase SQL Editor.
-- ==========================================================

-- Enable pgcrypto for UUID generation if needed
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. Table: license_keys
CREATE TABLE IF NOT EXISTS public.license_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key VARCHAR(128) UNIQUE NOT NULL,
    duration_label VARCHAR(64) NOT NULL, -- e.g. "1 Hour", "7 Days", "30 Days", "Lifetime"
    duration_seconds BIGINT NOT NULL,    -- Duration in seconds (0 = Lifetime)
    max_devices INT NOT NULL DEFAULT 1,  -- Allowed concurrent/bound devices
    hwid_list TEXT[] NOT NULL DEFAULT '{}', -- Registered device HWIDs
    status VARCHAR(20) NOT NULL DEFAULT 'UNUSED' CHECK (status IN ('UNUSED', 'ACTIVE', 'EXPIRED', 'BANNED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    activated_at TIMESTAMPTZ,            -- NULL until first client login!
    expires_at TIMESTAMPTZ,              -- NULL until activated, computed upon first login
    last_login_at TIMESTAMPTZ,
    last_ip VARCHAR(64),
    notes TEXT
);

-- Index for high-speed key lookup
CREATE INDEX IF NOT EXISTS idx_license_keys_key ON public.license_keys(key);
CREATE INDEX IF NOT EXISTS idx_license_keys_status ON public.license_keys(status);

-- Enable Row Level Security (RLS)
ALTER TABLE public.license_keys ENABLE ROW LEVEL SECURITY;

-- Allow anon read & write via defined functions, or allow anon access with public key
DROP POLICY IF EXISTS "Allow public read access via key" ON public.license_keys;
CREATE POLICY "Allow public read access via key" ON public.license_keys
    FOR SELECT USING (true);

DROP POLICY IF EXISTS "Allow anon insert and update via service role or api" ON public.license_keys;
CREATE POLICY "Allow anon insert and update via service role or api" ON public.license_keys
    FOR ALL USING (true);

-- 2. Stored Procedure: verify_and_activate_key
-- Atomic, race-condition-proof client login and activation.
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
    -- Trim inputs
    p_key := trim(p_key);
    p_hwid := trim(p_hwid);

    -- 1. Find key
    SELECT * INTO v_key_record
    FROM public.license_keys
    WHERE key = p_key;

    IF NOT FOUND THEN
        RETURN jsonb_build_object(
            'success', false,
            'error', 'Invalid license key'
        );
    END IF;

    -- 2. Check if banned or expired
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

    -- 3. Check if active key has expired
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

    -- 4. First-time Activation (if UNUSED)
    IF v_key_record.status = 'UNUSED' THEN
        -- Calculate expiration strictly on first use
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

    -- 5. Already Active: verify HWID and device limits
    v_device_count := cardinality(v_key_record.hwid_list);

    -- If this device is already registered
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

    -- If device is new, check if device limit allows adding it
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
        -- Device limit reached
        RETURN jsonb_build_object(
            'success', false,
            'error', 'Device limit reached (' || v_device_count || '/' || v_key_record.max_devices || '). Contact admin or reset HWID.'
        );
    END IF;
END;
$$;

-- 3. Stored Procedure: reset_key_hwid
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

-- 4. Stored Procedure: extend_key_duration
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
        -- If already expired, extend from now
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
        -- If unused, simply add to duration_seconds
        UPDATE public.license_keys
        SET duration_seconds = duration_seconds + p_additional_seconds
        WHERE id = v_rec.id;

        RETURN jsonb_build_object('success', true, 'duration_seconds', v_rec.duration_seconds + p_additional_seconds);
    END IF;
END;
$$;

-- 5. Stored Procedure: delete_expired_keys
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
-- 6. Table: lib_updates (Dynamic Library ZIP & Version Management)
-- ==========================================================
CREATE TABLE IF NOT EXISTS public.lib_updates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version VARCHAR(32) NOT NULL,            -- e.g. "1.0", "2.1"
    download_url TEXT NOT NULL,              -- Supabase Storage URL or custom direct URL
    storage_path TEXT,                       -- Path in Supabase bucket if uploaded (e.g. "libs/lib_123.zip")
    file_name VARCHAR(128),
    file_size BIGINT,
    changelog TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Index for fetching active lib version
CREATE INDEX IF NOT EXISTS idx_lib_updates_active ON public.lib_updates(is_active, updated_at DESC);

-- Enable RLS
ALTER TABLE public.lib_updates ENABLE ROW LEVEL SECURITY;

-- Allow public read access to lib updates
DROP POLICY IF EXISTS "Allow public read access to lib_updates" ON public.lib_updates;
CREATE POLICY "Allow public read access to lib_updates" ON public.lib_updates
    FOR SELECT USING (true);

-- Allow anon/service role to insert or update lib updates
DROP POLICY IF EXISTS "Allow insert/update to lib_updates" ON public.lib_updates;
CREATE POLICY "Allow insert/update to lib_updates" ON public.lib_updates
    FOR ALL USING (true);

-- Insert initial default record if empty
INSERT INTO public.lib_updates (version, download_url, file_name, is_active)
SELECT '1.0', 'https://github.com/AkhilRyzen/Ryzen/releases/download/Ryzen/hb.zip', 'hb.zip', true
WHERE NOT EXISTS (SELECT 1 FROM public.lib_updates);

-- ==========================================================
-- 7. Supabase Storage Bucket Setup Instructions:
-- Create a public bucket named "libs" in Supabase Storage.
-- SQL to create bucket if running in SQL editor with storage extension:
-- ==========================================================
INSERT INTO storage.buckets (id, name, public)
VALUES ('libs', 'libs', true)
ON CONFLICT (id) DO NOTHING;

DROP POLICY IF EXISTS "Allow public read access to libs storage" ON storage.objects;
CREATE POLICY "Allow public read access to libs storage" ON storage.objects
    FOR SELECT USING (bucket_id = 'libs');

DROP POLICY IF EXISTS "Allow authenticated/anon upload to libs storage" ON storage.objects;
CREATE POLICY "Allow authenticated/anon upload to libs storage" ON storage.objects
    FOR ALL USING (bucket_id = 'libs');

-- ==========================================================
-- 8. Table: system_config (Maintenance Mode & In-App Announcements)
-- ==========================================================
CREATE TABLE IF NOT EXISTS public.system_config (
    id VARCHAR(32) PRIMARY KEY DEFAULT 'global',
    maintenance_mode BOOLEAN NOT NULL DEFAULT false,
    maintenance_message TEXT NOT NULL DEFAULT 'Server is currently undergoing scheduled maintenance. Please check back soon.',
    maintenance_estimated_end TEXT NOT NULL DEFAULT 'Soon',
    announcement_active BOOLEAN NOT NULL DEFAULT false,
    announcement_title VARCHAR(256) NOT NULL DEFAULT 'Server Announcement',
    announcement_message TEXT NOT NULL DEFAULT '',
    announcement_type VARCHAR(32) NOT NULL DEFAULT 'info', -- 'info' | 'warning' | 'critical'
    announcement_link TEXT,
    bgmi_enabled BOOLEAN NOT NULL DEFAULT true,
    bgmi_status VARCHAR(64) NOT NULL DEFAULT 'OBB Ready',
    pubg_enabled BOOLEAN NOT NULL DEFAULT true,
    pubg_status VARCHAR(64) NOT NULL DEFAULT 'OBB Ready',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Enable RLS
ALTER TABLE public.system_config ENABLE ROW LEVEL SECURITY;

-- Allow public read access to system config
DROP POLICY IF EXISTS "Allow public read access to system_config" ON public.system_config;
CREATE POLICY "Allow public read access to system_config" ON public.system_config
    FOR SELECT USING (true);

-- Allow admin/anon to update system config
DROP POLICY IF EXISTS "Allow update to system_config" ON public.system_config;
CREATE POLICY "Allow update to system_config" ON public.system_config
    FOR ALL USING (true);

-- Insert default global row if not exists
INSERT INTO public.system_config (id, maintenance_mode, maintenance_message, maintenance_estimated_end)
VALUES ('global', false, 'Server is currently undergoing scheduled maintenance. Please check back soon.', '1 Hour')
ON CONFLICT (id) DO NOTHING;

-- ==========================================================
-- 9. Table: app_apk_updates (In-App APK Updates Management)
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
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Enable RLS
ALTER TABLE public.app_apk_updates ENABLE ROW LEVEL SECURITY;

-- Allow public read access to app_apk_updates
DROP POLICY IF EXISTS "Allow public read access to app_apk_updates" ON public.app_apk_updates;
CREATE POLICY "Allow public read access to app_apk_updates" ON public.app_apk_updates
    FOR SELECT USING (true);

-- Allow admin/anon to update app_apk_updates
DROP POLICY IF EXISTS "Allow insert/update to app_apk_updates" ON public.app_apk_updates;
CREATE POLICY "Allow insert/update to app_apk_updates" ON public.app_apk_updates
    FOR ALL USING (true);

-- Insert initial record if empty
INSERT INTO public.app_apk_updates (version_code, version_name, download_url, changelog, is_mandatory, is_active)
SELECT 2, '2026.01.01', 'https://example.com/anoy_loader_update.apk', 'Initial release with Supabase integration and AMOLED UI.', false, true
WHERE NOT EXISTS (SELECT 1 FROM public.app_apk_updates);

-- ==========================================================
-- 10. Table: managed_games (Dynamic Game Catalog Management)
-- ==========================================================
CREATE TABLE IF NOT EXISTS public.managed_games (
    id VARCHAR(64) PRIMARY KEY,              -- e.g. "bgmi", "pubg_global"
    title VARCHAR(128) NOT NULL,            -- e.g. "BGMI (BATTLEGROUNDS)"
    package_name VARCHAR(128) NOT NULL,      -- e.g. "com.pubg.imobile"
    lib_name VARCHAR(64) NOT NULL DEFAULT 'libbgmi.so',
    icon_type VARCHAR(32) NOT NULL DEFAULT 'bgmi', -- 'bgmi' | 'pubg_global' | 'custom'
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
-- 11. Table: game_versions (Multi-Version Support per Game)
-- ==========================================================
CREATE TABLE IF NOT EXISTS public.game_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    game_id VARCHAR(64) NOT NULL REFERENCES public.managed_games(id) ON DELETE CASCADE,
    version_name VARCHAR(32) NOT NULL,       -- e.g. "4.5.0", "4.6.0"
    version_code INT NOT NULL,               -- e.g. 21325, 21455
    obb_name VARCHAR(128) NOT NULL,          -- e.g. "main.21325.com.pubg.imobile.obb"
    tag VARCHAR(32) NOT NULL DEFAULT 'LATEST', -- 'LATEST' | 'BETA' | 'TEST' | 'STABLE'
    status_text VARCHAR(64) NOT NULL DEFAULT 'Ready',
    lib_version VARCHAR(32) NOT NULL DEFAULT '1.0',
    lib_download_url TEXT NOT NULL DEFAULT '',
    is_default BOOLEAN NOT NULL DEFAULT false,
    is_active BOOLEAN NOT NULL DEFAULT true,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_game_versions_game_id ON public.game_versions(game_id, is_active, sort_order);

ALTER TABLE public.game_versions ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow public read access to game_versions" ON public.game_versions;
CREATE POLICY "Allow public read access to game_versions" ON public.game_versions FOR SELECT USING (true);
DROP POLICY IF EXISTS "Allow insert/update to game_versions" ON public.game_versions;
CREATE POLICY "Allow insert/update to game_versions" ON public.game_versions FOR ALL USING (true);

-- Seed initial games
INSERT INTO public.managed_games (id, title, package_name, lib_name, icon_type, is_enabled, status_text, sort_order)
VALUES 
    ('bgmi', 'BGMI (BATTLEGROUNDS)', 'com.pubg.imobile', 'libbgmi.so', 'bgmi', true, 'OBB Ready', 0),
    ('pubg_global', 'PUBG MOBILE (GLOBAL)', 'com.tencent.ig', 'libpubgm.so', 'pubg_global', true, 'OBB Ready', 1)
ON CONFLICT (id) DO NOTHING;

-- Seed initial versions
INSERT INTO public.game_versions (game_id, version_name, version_code, obb_name, tag, status_text, lib_version, lib_download_url, is_default, is_active, sort_order)
SELECT 'bgmi', '4.5.0', 21325, 'main.21325.com.pubg.imobile.obb', 'LATEST', 'Ready', '1.0', '', true, true, 0
WHERE NOT EXISTS (SELECT 1 FROM public.game_versions WHERE game_id = 'bgmi' AND version_code = 21325);

INSERT INTO public.game_versions (game_id, version_name, version_code, obb_name, tag, status_text, lib_version, lib_download_url, is_default, is_active, sort_order)
SELECT 'bgmi', '4.6.0', 21455, 'main.21455.com.pubg.imobile.obb', 'BETA', 'Beta Build', '1.1-beta', '', false, true, 1
WHERE NOT EXISTS (SELECT 1 FROM public.game_versions WHERE game_id = 'bgmi' AND version_code = 21455);

INSERT INTO public.game_versions (game_id, version_name, version_code, obb_name, tag, status_text, lib_version, lib_download_url, is_default, is_active, sort_order)
SELECT 'pubg_global', '3.6.0', 19120, 'main.19120.com.tencent.ig.obb', 'LATEST', 'Ready', '1.0', '', true, true, 0
WHERE NOT EXISTS (SELECT 1 FROM public.game_versions WHERE game_id = 'pubg_global' AND version_code = 19120);


