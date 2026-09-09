-- =========================================================================
-- ANOY LOADER - MIGRATION: ADD LIB_NAME TO GAME_VERSIONS
-- Run this in your Supabase SQL Editor:
-- https://supabase.com/dashboard/project/_/sql/new
-- =========================================================================

-- 1. Add lib_name column to game_versions if not already present
ALTER TABLE public.game_versions 
ADD COLUMN IF NOT EXISTS lib_name TEXT DEFAULT '';

-- 2. Make version_code and obb_name nullable/defaulted for Coming Soon versions
ALTER TABLE public.game_versions ALTER COLUMN version_code DROP NOT NULL;
ALTER TABLE public.game_versions ALTER COLUMN obb_name DROP NOT NULL;
ALTER TABLE public.game_versions ALTER COLUMN version_code SET DEFAULT 0;
ALTER TABLE public.game_versions ALTER COLUMN obb_name SET DEFAULT '';

-- 3. Backfill existing game versions with their assigned lib filenames
UPDATE public.game_versions
SET lib_name = CASE
    WHEN version_name LIKE '4.5%' THEN 'libbgmi450.so'
    WHEN version_name LIKE '4.6%' THEN 'libbgmi460.so'
    WHEN version_name LIKE '3.6%' THEN 'libpubgm360.so'
    WHEN lib_version LIKE '%.so' THEN lib_version
    WHEN game_id = 'pubg_global' THEN 'libpubgm.so'
    ELSE 'libbgmi.so'
END
WHERE lib_name IS NULL OR lib_name = '';

-- 4. Reload PostgREST schema cache so API immediately reflects new column
NOTIFY pgrst, 'reload schema';
