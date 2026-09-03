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
