export type KeyStatus = 'UNUSED' | 'ACTIVE' | 'EXPIRED' | 'BANNED';

export interface LicenseKey {
  id: string;
  key: string;
  duration_label: string;
  duration_seconds: number;
  max_devices: number;
  hwid_list: string[];
  status: KeyStatus;
  created_at: string;
  activated_at: string | null;
  expires_at: string | null;
  last_login_at: string | null;
  last_ip: string | null;
  notes: string | null;
}

export interface DurationOption {
  label: string;
  seconds: number;
  category: 'hours' | 'days' | 'extended';
}

export const DURATION_OPTIONS: DurationOption[] = [
  { label: '1 Hour', seconds: 3600, category: 'hours' },
  { label: '6 Hours', seconds: 21600, category: 'hours' },
  { label: '12 Hours', seconds: 43200, category: 'hours' },
  { label: '1 Day', seconds: 86400, category: 'days' },
  { label: '3 Days', seconds: 259200, category: 'days' },
  { label: '7 Days', seconds: 604800, category: 'days' },
  { label: '15 Days', seconds: 1296000, category: 'days' },
  { label: '30 Days', seconds: 2592000, category: 'days' },
  { label: '60 Days', seconds: 5184000, category: 'days' },
  { label: '1 Year', seconds: 31536000, category: 'extended' },
  { label: 'Lifetime', seconds: 0, category: 'extended' },
];

export interface GameVersion {
  id: string;
  game_id: string;
  version_name: string;
  version_code: number;
  obb_name: string;
  tag: 'LATEST' | 'BETA' | 'TEST' | 'STABLE' | 'COMING SOON' | string;
  status_text: string;
  lib_name?: string;
  lib_version: string;
  lib_download_url?: string;
  is_default: boolean;
  is_active: boolean;
  sort_order: number;
  created_at?: string;
  updated_at?: string;
}

export interface ManagedGame {
  id: string;
  title: string;
  package_name: string;
  lib_name: string;
  icon_type: string;
  is_enabled: boolean;
  status_text: string;
  sort_order: number;
  versions: GameVersion[];
  created_at?: string;
  updated_at?: string;
}

export const DEFAULT_GAMES: ManagedGame[] = [
  {
    id: 'bgmi',
    title: 'BGMI',
    package_name: 'com.pubg.imobile',
    lib_name: 'libbgmi.so',
    icon_type: 'bgmi',
    is_enabled: true,
    status_text: 'OBB Ready',
    sort_order: 0,
    versions: [
      {
        id: 'bgmi_4_5_0',
        game_id: 'bgmi',
        version_name: '4.5.0',
        version_code: 21325,
        obb_name: 'main.21325.com.pubg.imobile.obb',
        tag: 'LATEST',
        status_text: 'Ready',
        lib_version: '1.0',
        lib_download_url: '',
        is_default: true,
        is_active: true,
        sort_order: 0,
      },
      {
        id: 'bgmi_4_6_0',
        game_id: 'bgmi',
        version_name: '4.6.0',
        version_code: 21455,
        obb_name: 'main.21455.com.pubg.imobile.obb',
        tag: 'BETA',
        status_text: 'Beta Build',
        lib_version: '1.1-beta',
        lib_download_url: '',
        is_default: false,
        is_active: true,
        sort_order: 1,
      },
    ],
  },
  {
    id: 'pubg_global',
    title: 'PUBG GL',
    package_name: 'com.tencent.ig',
    lib_name: 'libpubgm.so',
    icon_type: 'pubg_global',
    is_enabled: true,
    status_text: 'OBB Ready',
    sort_order: 1,
    versions: [
      {
        id: 'pubg_3_6_0',
        game_id: 'pubg_global',
        version_name: '3.6.0',
        version_code: 19120,
        obb_name: 'main.19120.com.tencent.ig.obb',
        tag: 'LATEST',
        status_text: 'Ready',
        lib_version: '1.0',
        lib_download_url: '',
        is_default: true,
        is_active: true,
        sort_order: 0,
      },
    ],
  },
];
