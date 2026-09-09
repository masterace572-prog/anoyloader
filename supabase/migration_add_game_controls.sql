-- ==========================================================
-- Migration: Add Dynamic Game Availability Controls
-- Run this in your Supabase SQL Editor:
-- https://supabase.com/dashboard/project/wawjxbwahndpjtmonobs/sql
-- ==========================================================

ALTER TABLE public.system_config 
ADD COLUMN IF NOT EXISTS bgmi_enabled BOOLEAN NOT NULL DEFAULT true,
ADD COLUMN IF NOT EXISTS bgmi_status VARCHAR(64) NOT NULL DEFAULT 'OBB Ready',
ADD COLUMN IF NOT EXISTS pubg_enabled BOOLEAN NOT NULL DEFAULT true,
ADD COLUMN IF NOT EXISTS pubg_status VARCHAR(64) NOT NULL DEFAULT 'OBB Ready';

-- Ensure the 'global' config row has default values populated
UPDATE public.system_config
SET 
  bgmi_enabled = COALESCE(bgmi_enabled, true),
  bgmi_status = COALESCE(bgmi_status, 'OBB Ready'),
  pubg_enabled = COALESCE(pubg_enabled, true),
  pubg_status = COALESCE(pubg_status, 'OBB Ready')
WHERE id = 'global';

-- Reload PostgREST schema cache
NOTIFY pgrst, 'reload schema';
