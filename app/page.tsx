'use client';

import React, { useState, useEffect, useMemo } from 'react';
import {
  Key,
  Plus,
  Trash2,
  RefreshCw,
  Copy,
  Check,
  Clock,
  User,
  LogOut,
  Ban,
  Unlock,
  FileArchive,
  Download,
  Sliders,
  X,
  AlertTriangle,
  AlertCircle,
  Info,
  ArrowUpRight,
  Zap,
} from 'lucide-react';
import { supabase, isSupabaseConfigured } from '../lib/supabase';
import { LicenseKey, KeyStatus, DurationOption, DURATION_OPTIONS } from '../lib/types';

const INITIAL_MOCK_KEYS: LicenseKey[] = [
  {
    id: '1',
    key: 'ANOY-VIP-30D-9941',
    duration_label: '30 Days',
    duration_seconds: 2592000,
    max_devices: 1,
    hwid_list: ['d41d8cd98f00b204e9800998ecf8427e'],
    status: 'ACTIVE',
    created_at: new Date(Date.now() - 86400000 * 3).toISOString(),
    activated_at: new Date(Date.now() - 86400000 * 3).toISOString(),
    expires_at: new Date(Date.now() + 86400000 * 27).toISOString(),
    last_login_at: new Date().toISOString(),
    last_ip: '103.212.144.52',
    notes: 'Premium VIP User',
  },
  {
    id: '2',
    key: 'ANOY-VIP-30D-8820',
    duration_label: '30 Days',
    duration_seconds: 2592000,
    max_devices: 1,
    hwid_list: [],
    status: 'UNUSED',
    created_at: new Date(Date.now() - 86400000 * 1).toISOString(),
    activated_at: null,
    expires_at: null,
    last_login_at: null,
    last_ip: null,
    notes: 'Sample 30-day key',
  },
  {
    id: '3',
    key: 'ANOY-TRIAL-1D-4412',
    duration_label: '1 Day',
    duration_seconds: 86400,
    max_devices: 1,
    hwid_list: ['e99a18c428cb38d5f260853678922e03'],
    status: 'EXPIRED',
    created_at: new Date(Date.now() - 86400000 * 5).toISOString(),
    activated_at: new Date(Date.now() - 86400000 * 5).toISOString(),
    expires_at: new Date(Date.now() - 86400000 * 4).toISOString(),
    last_login_at: new Date(Date.now() - 86400000 * 4).toISOString(),
    last_ip: '157.34.12.98',
    notes: 'Expired 1-day trial',
  },
  {
    id: '4',
    key: 'ANOY-LIFE-PERM-0001',
    duration_label: 'Lifetime',
    duration_seconds: 0,
    max_devices: 2,
    hwid_list: ['8b1a9953c4611296a827abf8c47804d7'],
    status: 'ACTIVE',
    created_at: new Date(Date.now() - 86400000 * 10).toISOString(),
    activated_at: new Date(Date.now() - 86400000 * 10).toISOString(),
    expires_at: null,
    last_login_at: new Date().toISOString(),
    last_ip: '49.36.12.11',
    notes: 'VIP Lifetime Key',
  },
];

export default function AdminDashboard() {
  // Authentication PIN
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [enteredPin, setEnteredPin] = useState('');
  const [pinError, setPinError] = useState(false);

  // Core Data
  const [keys, setKeys] = useState<LicenseKey[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<'ALL' | KeyStatus>('ALL');
  const [copiedKey, setCopiedKey] = useState<string | null>(null);

  // Modals
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showExtendModal, setShowExtendModal] = useState<LicenseKey | null>(null);
  const [showBulkSuccessModal, setShowBulkSuccessModal] = useState<string[] | null>(null);
  const [showLibModal, setShowLibModal] = useState(false);
  const [showApkModal, setShowApkModal] = useState(false);
  const [showSystemModal, setShowSystemModal] = useState(false);

  // System Settings State (Maintenance & Announcements)
  const [systemTab, setSystemTab] = useState<'maintenance' | 'announcement'>('maintenance');
  const [maintenanceMode, setMaintenanceMode] = useState(false);
  const [maintenanceMessage, setMaintenanceMessage] = useState(
    'Server is currently undergoing scheduled maintenance. Please check back soon.'
  );
  const [maintenanceEstimatedEnd, setMaintenanceEstimatedEnd] = useState('1 Hour');
  const [announcementActive, setAnnouncementActive] = useState(false);
  const [announcementTitle, setAnnouncementTitle] = useState('Server Notice');
  const [announcementMessage, setAnnouncementMessage] = useState('');
  const [announcementType, setAnnouncementType] = useState<'info' | 'warning' | 'critical'>('info');
  const [announcementLink, setAnnouncementLink] = useState('');
  const [isSavingSystemConfig, setIsSavingSystemConfig] = useState(false);
  const [systemConfigSuccessMsg, setSystemConfigSuccessMsg] = useState<string | null>(null);

  // APK In-App Update State
  const [apkActiveVerName, setApkActiveVerName] = useState('2026.01.01');
  const [apkActiveVerCode, setApkActiveVerCode] = useState(2);
  const [apkActiveUrl, setApkActiveUrl] = useState('https://example.com/anoy_loader.apk');
  const [apkActiveChangelog, setApkActiveChangelog] = useState('Initial release with Supabase integration.');
  const [apkActiveMandatory, setApkActiveMandatory] = useState(false);
  const [apkNewVerName, setApkNewVerName] = useState('');
  const [apkNewVerCode, setApkNewVerCode] = useState('');
  const [apkNewUrl, setApkNewUrl] = useState('');
  const [apkNewChangelog, setApkNewChangelog] = useState('');
  const [apkNewMandatory, setApkNewMandatory] = useState(false);
  const [isSavingApkUpdate, setIsSavingApkUpdate] = useState(false);
  const [apkUpdateSuccessMsg, setApkUpdateSuccessMsg] = useState<string | null>(null);

  // Lib Update State
  const [libActiveVersion, setLibActiveVersion] = useState('1.0');
  const [libDownloadUrl, setLibDownloadUrl] = useState('https://github.com/AkhilRyzen/Ryzen/releases/download/Ryzen/hb.zip');
  const [libUpdatedAt, setLibUpdatedAt] = useState<string>(new Date().toISOString());
  const [libNewVersion, setLibNewVersion] = useState('');
  const [libDirectUrl, setLibDirectUrl] = useState('');
  const [isUploadingLib, setIsUploadingLib] = useState(false);
  const [libSuccessMsg, setLibSuccessMsg] = useState<string | null>(null);

  // Form State for Key Creation
  const [createMode, setCreateMode] = useState<'single' | 'bulk'>('single');
  const [customKeyName, setCustomKeyName] = useState('');
  const [bulkCount, setBulkCount] = useState<number>(5);
  const [selectedDuration, setSelectedDuration] = useState<DurationOption>(DURATION_OPTIONS[5]); // 7 Days
  const [maxDevices, setMaxDevices] = useState<number>(1);
  const [keyNotes, setKeyNotes] = useState('');
  const [extendingSeconds, setExtendingSeconds] = useState<number>(86400);
  const [activationTiming, setActivationTiming] = useState<'ON_FIRST_USE' | 'IMMEDIATE'>('ON_FIRST_USE');

  // Supabase live indicator
  const [isLiveDatabase, setIsLiveDatabase] = useState(false);

  // PIN verification on load
  useEffect(() => {
    const savedPin = sessionStorage.getItem('admin_session_auth');
    if (savedPin === 'valid') {
      setIsAuthenticated(true);
    }
  }, []);

  const handlePinSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (enteredPin === '1234') {
      sessionStorage.setItem('admin_session_auth', 'valid');
      setIsAuthenticated(true);
      setPinError(false);
    } else {
      setPinError(true);
    }
  };

  const handleLogout = () => {
    sessionStorage.removeItem('admin_session_auth');
    setIsAuthenticated(false);
    setEnteredPin('');
  };

  // Fetch keys and active info
  const fetchKeys = async () => {
    setLoading(true);
    if (isSupabaseConfigured() && supabase) {
      try {
        const { data, error } = await supabase
          .from('license_keys')
          .select('*')
          .order('created_at', { ascending: false });

        if (!error && data) {
          setKeys(data as LicenseKey[]);
          setIsLiveDatabase(true);
        }

        // Fetch lib update
        const { data: libData } = await supabase
          .from('lib_updates')
          .select('*')
          .eq('is_active', true)
          .order('updated_at', { ascending: false })
          .limit(1)
          .maybeSingle();

        if (libData) {
          setLibActiveVersion(libData.version);
          setLibDownloadUrl(libData.download_url);
          setLibUpdatedAt(libData.updated_at);
        }

        // Fetch system config
        const { data: sysData } = await supabase
          .from('system_config')
          .select('*')
          .eq('id', 'global')
          .maybeSingle();

        if (sysData) {
          setMaintenanceMode(!!sysData.maintenance_mode);
          setMaintenanceMessage(sysData.maintenance_message || 'Server is currently undergoing scheduled maintenance.');
          setMaintenanceEstimatedEnd(sysData.maintenance_estimated_end || 'Soon');
          setAnnouncementActive(!!sysData.announcement_active);
          setAnnouncementTitle(sysData.announcement_title || '');
          setAnnouncementMessage(sysData.announcement_message || '');
          setAnnouncementType(sysData.announcement_type || 'info');
          setAnnouncementLink(sysData.announcement_link || '');
        }

        // Fetch APK update
        const { data: apkData } = await supabase
          .from('app_apk_updates')
          .select('*')
          .eq('is_active', true)
          .order('version_code', { ascending: false })
          .limit(1)
          .maybeSingle();

        if (apkData) {
          setApkActiveVerName(apkData.version_name);
          setApkActiveVerCode(apkData.version_code);
          setApkActiveUrl(apkData.download_url);
          setApkActiveChangelog(apkData.changelog || '');
          setApkActiveMandatory(!!apkData.is_mandatory);
        }

        setLoading(false);
        return;
      } catch (e) {
        console.error('Supabase fetch error', e);
      }
    }

    // Local fallback
    setIsLiveDatabase(false);
    const stored = localStorage.getItem('anoy_keys');
    if (stored) {
      try {
        setKeys(JSON.parse(stored));
      } catch {
        setKeys(INITIAL_MOCK_KEYS);
      }
    } else {
      setKeys(INITIAL_MOCK_KEYS);
      localStorage.setItem('anoy_keys', JSON.stringify(INITIAL_MOCK_KEYS));
    }
    setLoading(false);
  };

  useEffect(() => {
    if (isAuthenticated) {
      fetchKeys();
    }
  }, [isAuthenticated]);

  const syncKeys = (updated: LicenseKey[]) => {
    setKeys(updated);
    if (!isLiveDatabase) {
      localStorage.setItem('anoy_keys', JSON.stringify(updated));
    }
  };

  // Filtered keys
  const filteredKeys = useMemo(() => {
    return keys.filter((k) => {
      const matchesSearch =
        k.key.toLowerCase().includes(searchQuery.toLowerCase()) ||
        (k.notes && k.notes.toLowerCase().includes(searchQuery.toLowerCase())) ||
        (k.last_ip && k.last_ip.includes(searchQuery)) ||
        k.hwid_list.some((h) => h.toLowerCase().includes(searchQuery.toLowerCase()));

      const matchesStatus = statusFilter === 'ALL' || k.status === statusFilter;
      return matchesSearch && matchesStatus;
    });
  }, [keys, searchQuery, statusFilter]);

  // Statistics
  const stats = useMemo(() => {
    const total = keys.length;
    const active = keys.filter((k) => k.status === 'ACTIVE').length;
    const unused = keys.filter((k) => k.status === 'UNUSED').length;
    const expired = keys.filter((k) => k.status === 'EXPIRED').length;
    const banned = keys.filter((k) => k.status === 'BANNED').length;
    return { total, active, unused, expired, banned };
  }, [keys]);

  // Copy helper
  const handleCopy = (text: string) => {
    navigator.clipboard.writeText(text);
    setCopiedKey(text);
    setTimeout(() => setCopiedKey(null), 2000);
  };

  // Key generator helper
  const generateRandomSegment = (len = 4) => {
    const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
    let res = '';
    for (let i = 0; i < len; i++) {
      res += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return res;
  };

  const createFormattedKey = () => {
    return `ANOY-${generateRandomSegment()}-${generateRandomSegment()}-${generateRandomSegment()}`;
  };

  // Create Key action
  const handleCreateKey = async (e: React.FormEvent) => {
    e.preventDefault();

    const isTimed = selectedDuration.seconds > 0;
    const isImmediate = isTimed && activationTiming === 'IMMEDIATE';
    const now = new Date();
    const expiresAt = isImmediate
      ? new Date(now.getTime() + selectedDuration.seconds * 1000).toISOString()
      : null;
    const keyStatus: KeyStatus = isImmediate ? 'ACTIVE' : 'UNUSED';
    const activatedAt = isImmediate ? now.toISOString() : null;

    if (createMode === 'single') {
      const generatedKey = customKeyName.trim()
        ? customKeyName.trim().toUpperCase()
        : createFormattedKey();

      const newKeyObj: Partial<LicenseKey> = {
        key: generatedKey,
        duration_label: selectedDuration.label,
        duration_seconds: selectedDuration.seconds,
        max_devices: maxDevices,
        hwid_list: [],
        status: keyStatus,
        created_at: now.toISOString(),
        activated_at: activatedAt,
        expires_at: expiresAt,
        notes: keyNotes.trim() || null,
      };

      if (isLiveDatabase && supabase) {
        const { data, error } = await supabase
          .from('license_keys')
          .insert([newKeyObj])
          .select()
          .single();

        if (!error && data) {
          syncKeys([data as LicenseKey, ...keys]);
        }
      } else {
        const localKey: LicenseKey = {
          id: String(Date.now()),
          key: generatedKey,
          duration_label: selectedDuration.label,
          duration_seconds: selectedDuration.seconds,
          max_devices: maxDevices,
          hwid_list: [],
          status: keyStatus,
          created_at: now.toISOString(),
          activated_at: activatedAt,
          expires_at: expiresAt,
          last_login_at: null,
          last_ip: null,
          notes: keyNotes.trim() || null,
        };
        syncKeys([localKey, ...keys]);
      }
    } else {
      // Bulk generation
      const count = Math.min(Math.max(bulkCount, 1), 50);
      const newItems: Partial<LicenseKey>[] = [];
      const keysList: string[] = [];

      for (let i = 0; i < count; i++) {
        const kStr = createFormattedKey();
        keysList.push(kStr);
        newItems.push({
          key: kStr,
          duration_label: selectedDuration.label,
          duration_seconds: selectedDuration.seconds,
          max_devices: maxDevices,
          hwid_list: [],
          status: keyStatus,
          created_at: now.toISOString(),
          activated_at: activatedAt,
          expires_at: expiresAt,
          notes: keyNotes.trim() || null,
        });
      }

      if (isLiveDatabase && supabase) {
        const { data, error } = await supabase
          .from('license_keys')
          .insert(newItems)
          .select();

        if (!error && data) {
          syncKeys([...(data as LicenseKey[]), ...keys]);
          setShowBulkSuccessModal(keysList);
        }
      } else {
        const localList: LicenseKey[] = newItems.map((item, idx) => ({
          ...(item as LicenseKey),
          id: String(Date.now() + idx),
          hwid_list: [],
          last_login_at: null,
          last_ip: null,
        }));
        syncKeys([...localList, ...keys]);
        setShowBulkSuccessModal(keysList);
      }
    }

    setShowCreateModal(false);
    setCustomKeyName('');
    setKeyNotes('');
  };

  const handleResetHwid = async (targetKey: LicenseKey) => {
    if (!confirm(`Reset HWID device bindings for key ${targetKey.key}?`)) return;

    if (isLiveDatabase && supabase) {
      await supabase
        .from('license_keys')
        .update({ hwid_list: [] })
        .eq('id', targetKey.id);
      syncKeys(keys.map((k) => (k.id === targetKey.id ? { ...k, hwid_list: [] } : k)));
    } else {
      syncKeys(keys.map((k) => (k.id === targetKey.id ? { ...k, hwid_list: [] } : k)));
    }
  };

  const handleToggleBan = async (targetKey: LicenseKey) => {
    const nextStatus: KeyStatus = targetKey.status === 'BANNED' ? 'ACTIVE' : 'BANNED';
    if (isLiveDatabase && supabase) {
      await supabase
        .from('license_keys')
        .update({ status: nextStatus })
        .eq('id', targetKey.id);
      syncKeys(keys.map((k) => (k.id === targetKey.id ? { ...k, status: nextStatus } : k)));
    } else {
      syncKeys(keys.map((k) => (k.id === targetKey.id ? { ...k, status: nextStatus } : k)));
    }
  };

  const handleDeleteKey = async (targetKey: LicenseKey) => {
    if (!confirm(`Are you sure you want to permanently delete key ${targetKey.key}?`)) return;

    if (isLiveDatabase && supabase) {
      await supabase
        .from('license_keys')
        .delete()
        .eq('id', targetKey.id);
      syncKeys(keys.filter((k) => k.id !== targetKey.id));
    } else {
      syncKeys(keys.filter((k) => k.id !== targetKey.id));
    }
  };

  const handleExtendKey = async () => {
    if (!showExtendModal) return;
    const targetKey = showExtendModal;
    const now = Date.now();
    const currentExpiry = targetKey.expires_at ? new Date(targetKey.expires_at).getTime() : now;
    const baseTime = Math.max(now, currentExpiry);
    const newExpiresAt = new Date(baseTime + extendingSeconds * 1000).toISOString();

    if (isLiveDatabase && supabase) {
      await supabase
        .from('license_keys')
        .update({ expires_at: newExpiresAt, status: 'ACTIVE' })
        .eq('id', targetKey.id);

      syncKeys(
        keys.map((k) =>
          k.id === targetKey.id ? { ...k, expires_at: newExpiresAt, status: 'ACTIVE' } : k
        )
      );
    } else {
      syncKeys(
        keys.map((k) =>
          k.id === targetKey.id ? { ...k, expires_at: newExpiresAt, status: 'ACTIVE' } : k
        )
      );
    }

    setShowExtendModal(null);
  };

  // Save Lib Update (Direct ZIP Package URL)
  const handleSaveLibUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsUploadingLib(true);
    setLibSuccessMsg(null);

    const finalUrl = libDirectUrl.trim();

    try {
      if (!finalUrl) {
        alert('Please provide a valid direct ZIP download URL.');
        setIsUploadingLib(false);
        return;
      }

      const versionToSave = libNewVersion.trim() || libActiveVersion;

      if (isLiveDatabase && supabase) {
        await supabase
          .from('lib_updates')
          .update({ is_active: false })
          .neq('id', '00000000-0000-0000-0000-000000000000');

        const { error: insertError } = await supabase
          .from('lib_updates')
          .insert([
            {
              version: versionToSave,
              download_url: finalUrl,
              is_active: true,
              updated_at: new Date().toISOString(),
            },
          ]);

        if (insertError) throw insertError;
      }

      setLibActiveVersion(versionToSave);
      setLibDownloadUrl(finalUrl);
      setLibUpdatedAt(new Date().toISOString());
      setLibSuccessMsg('Native library updated successfully!');

      setTimeout(() => {
        setShowLibModal(false);
        setLibSuccessMsg(null);
        setLibNewVersion('');
        setLibDirectUrl('');
      }, 1500);
    } catch (err: any) {
      alert(`Error updating lib: ${err.message || err}`);
    } finally {
      setIsUploadingLib(false);
    }
  };

  // Save APK In-App Update
  const handleSaveApkUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSavingApkUpdate(true);
    setApkUpdateSuccessMsg(null);

    const vName = apkNewVerName.trim() || apkActiveVerName;
    const vCode = parseInt(apkNewVerCode) || apkActiveVerCode;
    const url = apkNewUrl.trim() || apkActiveUrl;
    const changelog = apkNewChangelog.trim() || apkActiveChangelog;

    try {
      if (isLiveDatabase && supabase) {
        await supabase
          .from('app_apk_updates')
          .update({ is_active: false })
          .neq('id', '00000000-0000-0000-0000-000000000000');

        const { error } = await supabase
          .from('app_apk_updates')
          .insert([
            {
              version_name: vName,
              version_code: vCode,
              download_url: url,
              changelog: changelog,
              is_mandatory: apkNewMandatory,
              is_active: true,
              updated_at: new Date().toISOString(),
            },
          ]);

        if (error) throw error;
      }

      setApkActiveVerName(vName);
      setApkActiveVerCode(vCode);
      setApkActiveUrl(url);
      setApkActiveChangelog(changelog);
      setApkActiveMandatory(apkNewMandatory);
      setApkUpdateSuccessMsg('In-app APK update published successfully!');

      setTimeout(() => {
        setShowApkModal(false);
        setApkUpdateSuccessMsg(null);
      }, 1500);
    } catch (err: any) {
      alert(`Failed to save APK update: ${err.message || err}`);
    } finally {
      setIsSavingApkUpdate(false);
    }
  };

  // Save System Config
  const handleSaveSystemConfig = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSavingSystemConfig(true);
    setSystemConfigSuccessMsg(null);

    try {
      if (isLiveDatabase && supabase) {
        const { error } = await supabase
          .from('system_config')
          .upsert({
            id: 'global',
            maintenance_mode: maintenanceMode,
            maintenance_message: maintenanceMessage,
            maintenance_estimated_end: maintenanceEstimatedEnd,
            announcement_active: announcementActive,
            announcement_title: announcementTitle,
            announcement_message: announcementMessage,
            announcement_type: announcementType,
            announcement_link: announcementLink,
            updated_at: new Date().toISOString(),
          });

        if (error) throw error;
      }

      setSystemConfigSuccessMsg('System configuration updated successfully!');
      setTimeout(() => {
        setShowSystemModal(false);
        setSystemConfigSuccessMsg(null);
      }, 1500);
    } catch (err: any) {
      alert(`Failed to update system config: ${err.message || err}`);
    } finally {
      setIsSavingSystemConfig(false);
    }
  };

  const formatDate = (isoStr: string | null) => {
    if (!isoStr) return 'Lifetime';
    try {
      const d = new Date(isoStr);
      return d.toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return isoStr;
    }
  };

  const renderKeyExpiry = (k: LicenseKey) => {
    if (k.expires_at) {
      const expDate = new Date(k.expires_at);
      const isExpired = expDate.getTime() <= Date.now();
      return (
        <span className={isExpired ? 'text-red-400 font-medium' : 'text-neutral-200'}>
          {formatDate(k.expires_at)}
        </span>
      );
    }

    if (k.duration_seconds === 0 || k.duration_label === 'Lifetime') {
      return <span className="text-emerald-400 font-medium">Lifetime</span>;
    }

    if (k.status === 'UNUSED') {
      return (
        <span className="text-amber-400/90 text-xs font-mono" title="Timer countdown begins upon first device connection">
          Pending first use ({k.duration_label})
        </span>
      );
    }

    return <span className="text-neutral-500">—</span>;
  };

  // PIN PROTECTION SCREEN
  if (!isAuthenticated) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[#0D0D0D] p-4 font-sans antialiased text-white selection:bg-white selection:text-black">
        <div className="w-full max-w-sm rounded-xl border border-[#242424] bg-[#141414] p-6 sm:p-8 shadow-2xl">
          <div className="flex flex-col items-center text-center">
            <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-white text-black mb-4">
              <Key className="h-6 w-6" />
            </div>
            <h1 className="text-lg font-bold tracking-tight text-white">
              ANOY LOADER CONTROL
            </h1>
            <p className="mt-1 text-xs text-neutral-400">
              Enter Administrator PIN to manage licenses
            </p>
          </div>

          <form onSubmit={handlePinSubmit} className="mt-6 space-y-4">
            <div>
              <input
                type="password"
                maxLength={8}
                value={enteredPin}
                onChange={(e) => setEnteredPin(e.target.value)}
                placeholder="Enter PIN (Default: 1234)"
                className="w-full rounded-lg border border-[#2B2B2B] bg-[#0A0A0A] px-3.5 py-2.5 text-center font-mono text-sm tracking-widest text-white placeholder-neutral-600 focus:border-white focus:outline-none focus:ring-1 focus:ring-white transition-all"
                autoFocus
              />
              {pinError && (
                <p className="mt-2 text-center text-xs text-red-400 font-medium">
                  Invalid PIN code. Please try again.
                </p>
              )}
            </div>

            <button
              type="submit"
              className="w-full rounded-lg bg-white py-2.5 text-xs font-semibold uppercase tracking-wider text-black hover:bg-neutral-200 transition-colors"
            >
              Authorize Access
            </button>
          </form>

          <div className="mt-6 text-center text-[10px] text-neutral-400 font-mono">
            SECURE MANAGEMENT CONSOLE &bull; 2026
          </div>
        </div>
      </div>
    );
  }

  // MAIN ADMIN DASHBOARD
  return (
    <div className="min-h-screen bg-[#0D0D0D] font-sans antialiased text-white selection:bg-white selection:text-black pb-12">
      {/* HEADER BAR */}
      <header className="sticky top-0 z-30 border-b border-[#1F1F1F] bg-[#111111]/90 backdrop-blur-md px-3.5 py-3 sm:px-8">
        <div className="mx-auto flex max-w-7xl flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2.5">
              <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-white text-black font-bold text-xs">
                A
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <span className="font-semibold tracking-wide text-xs sm:text-base">
                    ANOY CONTROL
                  </span>
                  <span className="text-[9px] sm:text-[10px] uppercase font-mono tracking-wider text-neutral-400 border border-[#2B2B2B] px-1.5 py-0.5 rounded">
                    {isLiveDatabase ? 'LIVE' : 'LOCAL'}
                  </span>
                </div>
                <p className="hidden sm:block text-[11px] text-neutral-400">
                  License management, maintenance mode, and OTA dispatch
                </p>
              </div>
            </div>

            {/* Mobile quick actions: Refresh & Logout */}
            <div className="flex items-center gap-1.5 sm:hidden">
              <button
                onClick={fetchKeys}
                title="Refresh Keys"
                className="flex h-7 w-7 items-center justify-center rounded-md border border-[#262626] bg-[#141414] text-neutral-300 hover:text-white"
              >
                <RefreshCw className={`h-3 w-3 ${loading ? 'animate-spin' : ''}`} />
              </button>
              <button
                onClick={handleLogout}
                title="Logout"
                className="flex h-7 w-7 items-center justify-center rounded-md border border-[#262626] bg-[#141414] text-neutral-400 hover:text-white"
              >
                <LogOut className="h-3 w-3" />
              </button>
            </div>
          </div>

          {/* Action Tabs: Horizontally scrollable on mobile, flex on desktop */}
          <div className="flex items-center gap-1.5 overflow-x-auto no-scrollbar py-0.5 sm:py-0">
            <button
              onClick={() => setShowLibModal(true)}
              className="flex shrink-0 items-center gap-1.5 rounded-lg border border-[#262626] bg-[#141414] px-2.5 py-1.5 text-[11px] sm:text-xs font-medium text-white hover:bg-[#1E1E1E] transition-colors"
            >
              <FileArchive className="h-3.5 w-3.5 text-white" />
              <span>LIB (v{libActiveVersion})</span>
            </button>

            <button
              onClick={() => setShowApkModal(true)}
              className="flex shrink-0 items-center gap-1.5 rounded-lg border border-[#262626] bg-[#141414] px-2.5 py-1.5 text-[11px] sm:text-xs font-medium text-white hover:bg-[#1E1E1E] transition-colors"
            >
              <Download className="h-3.5 w-3.5 text-white" />
              <span>APK (v{apkActiveVerName})</span>
            </button>

            <button
              onClick={() => setShowSystemModal(true)}
              className="flex shrink-0 items-center gap-1.5 rounded-lg border border-[#262626] bg-[#141414] px-2.5 py-1.5 text-[11px] sm:text-xs font-medium text-white hover:bg-[#1E1E1E] transition-colors"
            >
              <Sliders className="h-3.5 w-3.5 text-white" />
              <span>MAINTENANCE</span>
            </button>

            <button
              onClick={handleLogout}
              title="Logout"
              className="hidden sm:flex h-8 w-8 items-center justify-center rounded-lg border border-[#262626] bg-[#141414] text-neutral-400 hover:text-white hover:bg-[#1E1E1E] transition-colors"
            >
              <LogOut className="h-3.5 w-3.5" />
            </button>
          </div>
        </div>
      </header>

      {/* DASHBOARD CONTENT */}
      <main className="mx-auto max-w-7xl px-3.5 py-4 sm:px-8 sm:py-6 space-y-4 sm:space-y-6">
        {/* KPI METRICS */}
        <div className="grid grid-cols-2 gap-2 sm:gap-3 sm:grid-cols-3 lg:grid-cols-5">
          <div className="rounded-xl border border-[#222222] bg-[#121212] p-3 sm:p-4">
            <div className="flex items-center justify-between">
              <span className="text-[10px] sm:text-[11px] font-medium tracking-wider text-neutral-400 uppercase">
                TOTAL KEYS
              </span>
              <Key className="h-3.5 w-3.5 text-neutral-400" />
            </div>
            <div className="mt-1.5 sm:mt-2 font-mono text-xl sm:text-2xl font-bold text-white">
              {stats.total}
            </div>
          </div>

          <div className="rounded-xl border border-[#222222] bg-[#121212] p-3 sm:p-4">
            <div className="flex items-center justify-between">
              <span className="text-[10px] sm:text-[11px] font-medium tracking-wider text-neutral-400 uppercase">
                ACTIVE
              </span>
              <Zap className="h-3.5 w-3.5 text-white" />
            </div>
            <div className="mt-1.5 sm:mt-2 font-mono text-xl sm:text-2xl font-bold text-white">
              {stats.active}
            </div>
          </div>

          <div className="rounded-xl border border-[#222222] bg-[#121212] p-3 sm:p-4">
            <div className="flex items-center justify-between">
              <span className="text-[10px] sm:text-[11px] font-medium tracking-wider text-neutral-400 uppercase">
                UNUSED
              </span>
              <Clock className="h-3.5 w-3.5 text-neutral-400" />
            </div>
            <div className="mt-1.5 sm:mt-2 font-mono text-xl sm:text-2xl font-bold text-white">
              {stats.unused}
            </div>
          </div>

          <div className="rounded-xl border border-[#222222] bg-[#121212] p-3 sm:p-4">
            <div className="flex items-center justify-between">
              <span className="text-[10px] sm:text-[11px] font-medium tracking-wider text-neutral-400 uppercase">
                EXPIRED
              </span>
              <AlertTriangle className="h-3.5 w-3.5 text-neutral-400" />
            </div>
            <div className="mt-1.5 sm:mt-2 font-mono text-xl sm:text-2xl font-bold text-white">
              {stats.expired}
            </div>
          </div>

          <div className="rounded-xl border border-[#222222] bg-[#121212] p-3 sm:p-4 col-span-2 sm:col-span-1">
            <div className="flex items-center justify-between">
              <span className="text-[10px] sm:text-[11px] font-medium tracking-wider text-neutral-400 uppercase">
                BANNED
              </span>
              <Ban className="h-3.5 w-3.5 text-red-400" />
            </div>
            <div className="mt-1.5 sm:mt-2 font-mono text-xl sm:text-2xl font-bold text-white">
              {stats.banned}
            </div>
          </div>
        </div>

        {/* CONTROLS TOOLBAR */}
        <div className="rounded-xl border border-[#222222] bg-[#121212] p-3 sm:p-4 space-y-3">
          <div className="flex flex-col gap-2.5 sm:flex-row sm:items-center sm:justify-between">
            {/* Search Input */}
            <div className="relative flex-1">
              <input
                type="text"
                placeholder="Search key, HWID, IP, or notes..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full rounded-lg border border-[#262626] bg-[#0A0A0A] px-3 py-2 text-xs text-white placeholder-neutral-500 focus:border-white focus:outline-none focus:ring-1 focus:ring-white transition-colors"
              />
            </div>

            {/* Refresh Button */}
            <button
              onClick={fetchKeys}
              title="Refresh Keys"
              className="hidden sm:flex items-center justify-center gap-1.5 rounded-lg border border-[#262626] bg-[#141414] px-3 py-2 text-xs font-medium text-neutral-300 hover:text-white hover:bg-[#1E1E1E] transition-colors shrink-0"
            >
              <RefreshCw className={`h-3.5 w-3.5 ${loading ? 'animate-spin' : ''}`} />
              <span>REFRESH</span>
            </button>

            {/* GENERATE KEY BUTTON */}
            <button
              onClick={() => setShowCreateModal(true)}
              className="flex items-center justify-center gap-1.5 rounded-lg bg-white px-3.5 py-2 text-xs font-semibold text-black hover:bg-neutral-200 transition-colors shrink-0"
            >
              <Plus className="h-3.5 w-3.5" />
              <span>GENERATE KEY</span>
            </button>
          </div>

          {/* Status Filter */}
          <div className="flex items-center gap-1 overflow-x-auto no-scrollbar pt-1 border-t border-[#1C1C1C]">
            {(['ALL', 'ACTIVE', 'UNUSED', 'EXPIRED', 'BANNED'] as const).map((st) => (
              <button
                key={st}
                onClick={() => setStatusFilter(st)}
                className={`shrink-0 rounded-md px-3 py-1 text-[11px] font-medium transition-colors ${
                  statusFilter === st
                    ? 'border border-white bg-white text-black font-semibold'
                    : 'border border-[#262626] bg-[#141414] text-neutral-400 hover:text-white'
                }`}
              >
                {st}
              </button>
            ))}
          </div>
        </div>

        {/* KEYS LIST / TABLE */}
        <div className="rounded-xl border border-[#222222] bg-[#121212] overflow-hidden">
          {/* Mobile Card List (Visible on small screens) */}
          <div className="block divide-y divide-[#1F1F1F] md:hidden">
            {loading ? (
              <div className="p-8 text-center text-xs text-neutral-400">Loading licenses...</div>
            ) : filteredKeys.length === 0 ? (
              <div className="p-8 text-center text-xs text-neutral-400">No matching keys found.</div>
            ) : (
              filteredKeys.map((k) => (
                <div key={k.id} className="p-3.5 space-y-2.5">
                  <div className="flex items-center justify-between gap-2">
                    <div className="flex items-center gap-1.5 min-w-0">
                      <span className="font-mono text-xs font-semibold tracking-wide text-white select-all break-all">
                        {k.key}
                      </span>
                      <button
                        onClick={() => handleCopy(k.key)}
                        className="text-neutral-400 hover:text-white p-1 shrink-0"
                        title="Copy Key"
                      >
                        {copiedKey === k.key ? <Check className="h-3.5 w-3.5 text-white" /> : <Copy className="h-3.5 w-3.5" />}
                      </button>
                    </div>
                    <span className="text-[9px] font-mono border border-[#333333] px-1.5 py-0.5 rounded text-white uppercase shrink-0">
                      {k.status}
                    </span>
                  </div>

                  <div className="grid grid-cols-2 gap-1.5 text-[11px] text-neutral-400 bg-[#0A0A0A] p-2.5 rounded-lg border border-[#1A1A1A]">
                    <div>
                      <span className="text-neutral-500 uppercase">Duration: </span>
                      <span className="text-white">{k.duration_label}</span>
                    </div>
                    <div>
                      <span className="text-neutral-500 uppercase">Devices: </span>
                      <span className="text-white font-mono">
                        {k.hwid_list.length} / {k.max_devices}
                      </span>
                    </div>
                    <div className="col-span-2">
                      <span className="text-neutral-500 uppercase">Expires: </span>
                      <span className="text-white">{renderKeyExpiry(k)}</span>
                    </div>
                  </div>

                  {k.notes && (
                    <p className="text-[10px] text-neutral-500 truncate font-mono">
                      {k.notes}
                    </p>
                  )}

                  <div className="flex items-center gap-1.5 pt-1 border-t border-[#1C1C1C]">
                    <button
                      onClick={() => setShowExtendModal(k)}
                      className="flex-1 rounded-md border border-[#2B2B2B] bg-[#181818] py-2 text-[11px] font-semibold text-white hover:bg-[#222222] transition-colors"
                    >
                      EXTEND
                    </button>
                    <button
                      onClick={() => handleResetHwid(k)}
                      className="flex-1 rounded-md border border-[#2B2B2B] bg-[#181818] py-2 text-[11px] font-semibold text-white hover:bg-[#222222] transition-colors"
                    >
                      RESET HWID
                    </button>
                    <button
                      onClick={() => handleToggleBan(k)}
                      title={k.status === 'BANNED' ? 'Unban Key' : 'Ban Key'}
                      className="rounded-md border border-[#2B2B2B] bg-[#181818] px-3 py-2 text-neutral-300 hover:text-white transition-colors"
                    >
                      {k.status === 'BANNED' ? <Unlock className="h-3.5 w-3.5 text-white" /> : <Ban className="h-3.5 w-3.5" />}
                    </button>
                    <button
                      onClick={() => handleDeleteKey(k)}
                      title="Delete Key"
                      className="rounded-md bg-[#DC2626] px-3 py-2 text-white hover:bg-red-700 transition-colors"
                    >
                      <Trash2 className="h-3.5 w-3.5" />
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>

          {/* Desktop Table View (Hidden on mobile) */}
          <div className="hidden md:block overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-[#202020] bg-[#0E0E0E] text-[11px] font-medium uppercase tracking-wider text-neutral-400">
                  <th className="py-3.5 pl-6 pr-4">KEY</th>
                  <th className="px-4 py-3.5">DURATION</th>
                  <th className="px-4 py-3.5">DEVICES</th>
                  <th className="px-4 py-3.5">STATUS</th>
                  <th className="px-4 py-3.5">EXPIRES</th>
                  <th className="px-4 py-3.5">LAST IP</th>
                  <th className="py-3.5 pl-4 pr-6 text-right">ACTIONS</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#1F1F1F] text-xs">
                {loading ? (
                  <tr>
                    <td colSpan={7} className="py-12 text-center text-neutral-400">
                      Loading licenses...
                    </td>
                  </tr>
                ) : filteredKeys.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="py-12 text-center text-neutral-400">
                      No matching keys found.
                    </td>
                  </tr>
                ) : (
                  filteredKeys.map((k) => (
                    <tr key={k.id} className="hover:bg-[#161616] transition-colors">
                      <td className="py-3.5 pl-6 pr-4">
                        <div className="flex items-center gap-2">
                          <span className="font-mono font-semibold tracking-wide text-white">
                            {k.key}
                          </span>
                          <button
                            onClick={() => handleCopy(k.key)}
                            className="text-neutral-500 hover:text-white p-0.5"
                          >
                            {copiedKey === k.key ? (
                              <Check className="h-3.5 w-3.5 text-white" />
                            ) : (
                              <Copy className="h-3.5 w-3.5" />
                            )}
                          </button>
                        </div>
                        {k.notes && (
                          <div className="mt-0.5 text-[10px] text-neutral-400 truncate max-w-xs font-mono">
                            {k.notes}
                          </div>
                        )}
                      </td>

                      <td className="px-4 py-3.5 text-neutral-300 font-medium">
                        {k.duration_label}
                      </td>

                      <td className="px-4 py-3.5 font-mono text-neutral-300">
                        {k.hwid_list.length} / {k.max_devices}
                      </td>

                      <td className="px-4 py-3.5">
                        <span className="text-[10px] font-mono border border-[#333333] px-2 py-0.5 rounded text-white uppercase">
                          {k.status}
                        </span>
                      </td>

                      <td className="px-4 py-3.5 text-neutral-300">
                        {renderKeyExpiry(k)}
                      </td>

                      <td className="px-4 py-3.5 font-mono text-[11px] text-neutral-400">
                        {k.last_ip || '—'}
                      </td>

                      <td className="py-3.5 pl-4 pr-6 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          <button
                            onClick={() => setShowExtendModal(k)}
                            className="rounded-md border border-[#2B2B2B] bg-[#181818] px-2.5 py-1 text-[11px] font-medium text-white hover:bg-[#222222]"
                          >
                            EXTEND
                          </button>
                          <button
                            onClick={() => handleResetHwid(k)}
                            className="rounded-md border border-[#2B2B2B] bg-[#181818] px-2.5 py-1 text-[11px] font-medium text-white hover:bg-[#222222]"
                          >
                            RESET HWID
                          </button>
                          <button
                            onClick={() => handleToggleBan(k)}
                            className="rounded-md border border-[#2B2B2B] bg-[#181818] p-1.5 text-neutral-400 hover:text-white"
                          >
                            {k.status === 'BANNED' ? (
                              <Unlock className="h-3.5 w-3.5 text-white" />
                            ) : (
                              <Ban className="h-3.5 w-3.5" />
                            )}
                          </button>
                          <button
                            onClick={() => handleDeleteKey(k)}
                            className="rounded-md bg-[#DC2626] p-1.5 text-white hover:bg-red-700"
                          >
                            <Trash2 className="h-3.5 w-3.5" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </main>

      {/* ============================================================= */}
      {/* MODAL: CREATE KEY */}
      {/* ============================================================= */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-3 sm:p-4">
          <div className="w-full max-w-lg rounded-xl border border-[#242424] bg-[#121212] p-4 sm:p-6 shadow-2xl space-y-4 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b border-[#202020] pb-3">
              <div>
                <h3 className="font-semibold text-white tracking-wide text-sm sm:text-base">
                  GENERATE LICENSE KEY
                </h3>
                <p className="text-xs text-neutral-400">
                  Configure license parameters for client access
                </p>
              </div>
              <button
                onClick={() => setShowCreateModal(false)}
                className="text-neutral-400 hover:text-white p-1"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            <form onSubmit={handleCreateKey} className="space-y-4 text-xs">
              {/* Mode Selection */}
              <div>
                <label className="block text-[11px] font-medium tracking-wider text-neutral-400 uppercase">
                  GENERATION MODE
                </label>
                <div className="mt-1.5 grid grid-cols-2 gap-2">
                  <button
                    type="button"
                    onClick={() => setCreateMode('single')}
                    className={`rounded-lg py-2 font-medium transition-colors ${
                      createMode === 'single'
                        ? 'bg-white text-black font-semibold'
                        : 'border border-[#262626] bg-[#141414] text-neutral-300'
                    }`}
                  >
                    SINGLE KEY
                  </button>
                  <button
                    type="button"
                    onClick={() => setCreateMode('bulk')}
                    className={`rounded-lg py-2 font-medium transition-colors ${
                      createMode === 'bulk'
                        ? 'bg-white text-black font-semibold'
                        : 'border border-[#262626] bg-[#141414] text-neutral-300'
                    }`}
                  >
                    BULK BATCH
                  </button>
                </div>
              </div>

              {createMode === 'single' ? (
                <div>
                  <label className="block text-[11px] font-medium tracking-wider text-neutral-400 uppercase">
                    CUSTOM KEY NAME (OPTIONAL)
                  </label>
                  <input
                    type="text"
                    value={customKeyName}
                    onChange={(e) => setCustomKeyName(e.target.value)}
                    className="mt-1.5 w-full rounded-lg border border-[#262626] bg-[#0A0A0A] px-3 py-2 text-xs font-mono text-white focus:border-white focus:outline-none focus:ring-1 focus:ring-white transition-colors"
                    placeholder="Leave empty for auto-generated ANOY key"
                  />
                </div>
              ) : (
                <div>
                  <label className="block text-[11px] font-medium tracking-wider text-neutral-400 uppercase">
                    NUMBER OF KEYS TO GENERATE (MAX 50)
                  </label>
                  <input
                    type="number"
                    min="1"
                    max="50"
                    value={bulkCount}
                    onChange={(e) => setBulkCount(parseInt(e.target.value) || 1)}
                    className="mt-1.5 w-full rounded-lg border border-[#262626] bg-[#0A0A0A] px-3 py-2 text-xs font-mono text-white focus:border-white focus:outline-none focus:ring-1 focus:ring-white transition-colors"
                  />
                </div>
              )}

              {/* Duration Selection */}
              <div>
                <label className="block text-[11px] font-medium tracking-wider text-neutral-400 uppercase">
                  LICENSE DURATION
                </label>
                <div className="mt-1.5 grid grid-cols-3 gap-1.5 sm:grid-cols-4">
                  {DURATION_OPTIONS.map((opt) => (
                    <button
                      key={opt.label}
                      type="button"
                      onClick={() => setSelectedDuration(opt)}
                      className={`rounded-lg border px-2 py-2 text-center text-xs transition-colors ${
                        selectedDuration.label === opt.label
                          ? 'border-white bg-white text-black font-semibold'
                          : 'border-[#262626] bg-[#0A0A0A] text-neutral-300 hover:border-neutral-700'
                      }`}
                    >
                      {opt.label}
                    </button>
                  ))}
                </div>
              </div>

              {/* Activation Timing (only for timed keys) */}
              {selectedDuration.seconds > 0 && (
                <div>
                  <label className="block text-[11px] font-medium tracking-wider text-neutral-400 uppercase">
                    EXPIRATION COUNTDOWN TIMING
                  </label>
                  <div className="mt-1.5 grid grid-cols-2 gap-2">
                    <button
                      type="button"
                      onClick={() => setActivationTiming('ON_FIRST_USE')}
                      className={`rounded-lg border p-2.5 text-left transition-colors ${
                        activationTiming === 'ON_FIRST_USE'
                          ? 'border-white bg-[#1C1C1C] text-white'
                          : 'border-[#262626] bg-[#0A0A0A] text-neutral-400 hover:text-white'
                      }`}
                    >
                      <div className="font-semibold text-xs text-white">ON FIRST LOGIN</div>
                      <div className="text-[10px] text-neutral-400 mt-0.5">
                        Timer starts when user first logs in
                      </div>
                    </button>
                    <button
                      type="button"
                      onClick={() => setActivationTiming('IMMEDIATE')}
                      className={`rounded-lg border p-2.5 text-left transition-colors ${
                        activationTiming === 'IMMEDIATE'
                          ? 'border-white bg-[#1C1C1C] text-white'
                          : 'border-[#262626] bg-[#0A0A0A] text-neutral-400 hover:text-white'
                      }`}
                    >
                      <div className="font-semibold text-xs text-white">IMMEDIATE</div>
                      <div className="text-[10px] text-neutral-400 mt-0.5">
                        Timer starts immediately right now
                      </div>
                    </button>
                  </div>
                </div>
              )}

              {/* Max Devices */}
              <div>
                <label className="block text-[11px] font-medium tracking-wider text-neutral-400 uppercase">
                  MAX CONCURRENT DEVICES (HWID LIMIT)
                </label>
                <input
                  type="number"
                  min="1"
                  max="10"
                  value={maxDevices}
                  onChange={(e) => setMaxDevices(parseInt(e.target.value) || 1)}
                  className="mt-1.5 w-full rounded-lg border border-[#262626] bg-[#0A0A0A] px-3 py-2 text-xs font-mono text-white focus:border-white focus:outline-none focus:ring-1 focus:ring-white transition-colors"
                />
              </div>

              {/* Notes */}
              <div>
                <label className="block text-[11px] font-medium tracking-wider text-neutral-400 uppercase">
                  INTERNAL NOTES / CUSTOMER (OPTIONAL)
                </label>
                <input
                  type="text"
                  placeholder="e.g. VIP Customer, Telegram handle..."
                  value={keyNotes}
                  onChange={(e) => setKeyNotes(e.target.value)}
                  className="mt-1.5 w-full rounded-lg border border-[#262626] bg-[#0A0A0A] px-3 py-2 text-xs text-white focus:border-white focus:outline-none focus:ring-1 focus:ring-white transition-colors"
                />
              </div>

              <div className="flex gap-2 pt-2 border-t border-[#202020]">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="flex-1 rounded-lg border border-[#2B2B2B] bg-[#161616] py-2 text-xs font-semibold text-white hover:bg-[#202020] transition-colors"
                >
                  CANCEL
                </button>
                <button
                  type="submit"
                  className="flex-1 rounded-lg bg-white py-2 text-xs font-semibold text-black hover:bg-neutral-200 transition-colors"
                >
                  {createMode === 'single' ? 'GENERATE KEY' : `GENERATE ${bulkCount} KEYS`}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ============================================================= */}
      {/* MODAL: EXTEND KEY DURATION */}
      {/* ============================================================= */}
      {showExtendModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-3 sm:p-4">
          <div className="w-full max-w-sm rounded-xl border border-[#242424] bg-[#121212] p-4 sm:p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between border-b border-[#202020] pb-3">
              <div>
                <h3 className="font-semibold text-white tracking-wide text-sm">
                  EXTEND KEY DURATION
                </h3>
                <p className="text-xs text-neutral-400 font-mono">
                  Target: <span className="font-bold">{showExtendModal.key}</span>
                </p>
              </div>
              <button
                onClick={() => setShowExtendModal(null)}
                className="text-neutral-400 hover:text-white p-1"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            <div className="space-y-2">
              <label className="block text-[11px] font-medium tracking-wider text-neutral-400 uppercase">
                ADD EXTENSION TIME
              </label>
              <div className="grid grid-cols-2 gap-2">
                {[
                  { label: '+1 Day', secs: 86400 },
                  { label: '+3 Days', secs: 86400 * 3 },
                  { label: '+7 Days', secs: 86400 * 7 },
                  { label: '+30 Days', secs: 86400 * 30 },
                ].map((opt) => (
                  <button
                    key={opt.label}
                    type="button"
                    onClick={() => setExtendingSeconds(opt.secs)}
                    className={`rounded-lg border py-2 text-xs font-medium transition-colors ${
                      extendingSeconds === opt.secs
                        ? 'border-white bg-white text-black font-semibold'
                        : 'border-[#262626] bg-[#0A0A0A] text-neutral-300'
                    }`}
                  >
                    {opt.label}
                  </button>
                ))}
              </div>
            </div>

            <div className="flex gap-2 pt-2 border-t border-[#202020]">
              <button
                type="button"
                onClick={() => setShowExtendModal(null)}
                className="flex-1 rounded-lg border border-[#2B2B2B] bg-[#161616] py-2 text-xs font-semibold text-white hover:bg-[#202020]"
              >
                CANCEL
              </button>
              <button
                type="button"
                onClick={handleExtendKey}
                className="flex-1 rounded-lg bg-white py-2 text-xs font-semibold text-black hover:bg-neutral-200"
              >
                CONFIRM EXTENSION
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ============================================================= */}
      {/* MODAL: BULK GENERATION SUCCESS */}
      {/* ============================================================= */}
      {showBulkSuccessModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-3 sm:p-4">
          <div className="w-full max-w-md rounded-xl border border-[#242424] bg-[#121212] p-4 sm:p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between border-b border-[#202020] pb-3">
              <div>
                <h3 className="font-semibold text-white tracking-wide text-sm">
                  BULK KEYS GENERATED ({showBulkSuccessModal.length})
                </h3>
                <p className="text-xs text-neutral-400">
                  Copy and save these keys securely
                </p>
              </div>
              <button
                onClick={() => setShowBulkSuccessModal(null)}
                className="text-neutral-400 hover:text-white p-1"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            <div className="rounded-lg border border-[#262626] bg-[#0A0A0A] p-3 max-h-60 overflow-y-auto font-mono text-xs text-neutral-300 space-y-1 select-all">
              {showBulkSuccessModal.map((k) => (
                <div key={k}>{k}</div>
              ))}
            </div>

            <div className="flex gap-2 pt-2 border-t border-[#202020]">
              <button
                type="button"
                onClick={() => handleCopy(showBulkSuccessModal.join('\n'))}
                className="flex-1 rounded-lg bg-white py-2 text-xs font-semibold text-black hover:bg-neutral-200"
              >
                COPY ALL KEYS
              </button>
              <button
                type="button"
                onClick={() => setShowBulkSuccessModal(null)}
                className="flex-1 rounded-lg border border-[#2B2B2B] bg-[#161616] py-2 text-xs font-semibold text-white hover:bg-[#202020]"
              >
                CLOSE
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ============================================================= */}
      {/* MODAL: LIB UPDATES */}
      {/* ============================================================= */}
      {showLibModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-3 sm:p-4">
          <div className="w-full max-w-lg rounded-xl border border-[#242424] bg-[#121212] p-4 sm:p-6 shadow-2xl space-y-4 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b border-[#202020] pb-3">
              <div className="flex items-center gap-2">
                <FileArchive className="h-4 w-4 text-white" />
                <h3 className="font-semibold text-white tracking-wide text-sm">
                  CORE LIB UPDATE DISPATCH
                </h3>
              </div>
              <button
                onClick={() => setShowLibModal(false)}
                className="text-neutral-400 hover:text-white p-1"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            {/* Current Active Info */}
            <div className="rounded-lg border border-[#262626] bg-[#0A0A0A] p-3 space-y-2 text-xs">
              <div className="flex items-center justify-between">
                <span className="text-neutral-400">Current Active Version:</span>
                <span className="font-mono font-semibold text-white">v{libActiveVersion}</span>
              </div>
              <div className="space-y-1">
                <span className="text-neutral-400">Download ZIP URL:</span>
                <p className="font-mono text-[11px] text-neutral-300 break-all bg-[#141414] p-2 rounded border border-[#222]">
                  {libDownloadUrl}
                </p>
              </div>
              <div className="flex items-center justify-between text-[11px] text-neutral-400">
                <span>Last Updated:</span>
                <span className="font-mono">{new Date(libUpdatedAt).toLocaleString()}</span>
              </div>
            </div>

            {/* Direct URL Form */}
            <form onSubmit={handleSaveLibUpdate} className="space-y-3 text-xs">
              <div>
                <label className="block text-[11px] font-medium tracking-wider text-neutral-400 uppercase">
                  NEW PACKAGE VERSION
                </label>
                <input
                  type="text"
                  placeholder={`Current: ${libActiveVersion} (e.g. 1.1 or 2.0)`}
                  value={libNewVersion}
                  onChange={(e) => setLibNewVersion(e.target.value)}
                  className="mt-1 w-full rounded-lg border border-[#262626] bg-[#0A0A0A] px-3 py-2 text-xs font-mono text-white focus:border-white focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-[11px] font-medium tracking-wider text-neutral-400 uppercase">
                  DIRECT ZIP DOWNLOAD URL
                </label>
                <input
                  type="url"
                  placeholder="https://.../hb.zip (direct public download URL)"
                  value={libDirectUrl}
                  onChange={(e) => setLibDirectUrl(e.target.value)}
                  required
                  className="mt-1 w-full rounded-lg border border-[#262626] bg-[#0A0A0A] px-3 py-2 text-xs font-mono text-white focus:border-white focus:outline-none"
                />
                <p className="mt-1 text-[10px] text-neutral-400">
                  The loader app automatically downloads and unzips libraries (libbgmi.so & libpubgm.so) into sandbox memory.
                </p>
              </div>

              {libSuccessMsg && (
                <div className="rounded-lg border border-emerald-900/50 bg-emerald-950/20 p-2 text-center text-xs text-emerald-400">
                  {libSuccessMsg}
                </div>
              )}

              <div className="sticky bottom-0 bg-[#121212] flex gap-2 pt-2 pb-1 border-t border-[#202020]">
                <button
                  type="button"
                  onClick={() => setShowLibModal(false)}
                  className="flex-1 rounded-lg border border-[#2B2B2B] bg-[#161616] py-2 text-xs font-semibold text-white hover:bg-[#202020]"
                >
                  CANCEL
                </button>
                <button
                  type="submit"
                  disabled={isUploadingLib}
                  className="flex-1 rounded-lg bg-white py-2 text-xs font-semibold text-black hover:bg-neutral-200 disabled:opacity-50"
                >
                  {isUploadingLib ? 'PUBLISHING...' : 'PUBLISH UPDATE'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ============================================================= */}
      {/* MODAL: IN-APP APK UPDATES */}
      {/* ============================================================= */}
      {showApkModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-3 sm:p-4">
          <div className="w-full max-w-lg rounded-xl border border-[#242424] bg-[#121212] p-4 sm:p-6 shadow-2xl space-y-4 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b border-[#202020] pb-3">
              <div className="flex items-center gap-2">
                <Download className="h-4 w-4 text-white" />
                <h3 className="font-semibold text-white tracking-wide text-sm">
                  IN-APP APK UPDATE MANAGER
                </h3>
              </div>
              <button
                onClick={() => setShowApkModal(false)}
                className="text-neutral-400 hover:text-white p-1"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            {/* Current Active APK Info */}
            <div className="rounded-lg border border-[#262626] bg-[#0A0A0A] p-3 space-y-1.5 text-xs">
              <div className="flex items-center justify-between">
                <span className="text-neutral-400">Current Version:</span>
                <span className="font-mono font-semibold text-white">
                  v{apkActiveVerName} (Code: {apkActiveVerCode})
                </span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-neutral-400">Type:</span>
                <span className={`font-mono text-[11px] ${apkActiveMandatory ? 'text-red-400' : 'text-emerald-400'}`}>
                  {apkActiveMandatory ? 'Mandatory' : 'Optional'}
                </span>
              </div>
              <div className="space-y-0.5">
                <span className="text-neutral-400">Download URL:</span>
                <p className="font-mono text-[11px] text-neutral-300 break-all truncate">
                  {apkActiveUrl}
                </p>
              </div>
            </div>

            <form onSubmit={handleSaveApkUpdate} className="space-y-3 text-xs">
              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="block text-[11px] font-medium tracking-wider text-neutral-400 uppercase">
                    VERSION NAME
                  </label>
                  <input
                    type="text"
                    placeholder="e.g. 2026.02.01"
                    value={apkNewVerName}
                    onChange={(e) => setApkNewVerName(e.target.value)}
                    className="mt-1 w-full rounded-lg border border-[#262626] bg-[#0A0A0A] px-3 py-2 text-xs font-mono text-white focus:border-white focus:outline-none"
                  />
                </div>
                <div>
                  <label className="block text-[11px] font-medium tracking-wider text-neutral-400 uppercase">
                    VERSION CODE
                  </label>
                  <input
                    type="number"
                    placeholder={`> ${apkActiveVerCode}`}
                    value={apkNewVerCode}
                    onChange={(e) => setApkNewVerCode(e.target.value)}
                    className="mt-1 w-full rounded-lg border border-[#262626] bg-[#0A0A0A] px-3 py-2 text-xs font-mono text-white focus:border-white focus:outline-none"
                  />
                </div>
              </div>

              <div>
                <label className="block text-[11px] font-medium tracking-wider text-neutral-400 uppercase">
                  DIRECT APK DOWNLOAD URL
                </label>
                <input
                  type="url"
                  placeholder="https://.../anoy_loader.apk"
                  value={apkNewUrl}
                  onChange={(e) => setApkNewUrl(e.target.value)}
                  className="mt-1 w-full rounded-lg border border-[#262626] bg-[#0A0A0A] px-3 py-2 text-xs font-mono text-white focus:border-white focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-[11px] font-medium tracking-wider text-neutral-400 uppercase">
                  UPDATE CHANGELOG
                </label>
                <textarea
                  rows={3}
                  placeholder="Describe new features or fixes..."
                  value={apkNewChangelog}
                  onChange={(e) => setApkNewChangelog(e.target.value)}
                  className="mt-1 w-full rounded-lg border border-[#262626] bg-[#0A0A0A] px-3 py-2 text-xs text-white focus:border-white focus:outline-none"
                />
              </div>

              <div className="flex items-center justify-between rounded-lg border border-[#262626] bg-[#0A0A0A] p-2.5">
                <div>
                  <div className="font-semibold text-white">FORCE MANDATORY UPDATE</div>
                  <div className="text-[10px] text-neutral-400">
                    Blocks older loader versions from entering until updated
                  </div>
                </div>
                <input
                  type="checkbox"
                  checked={apkNewMandatory}
                  onChange={(e) => setApkNewMandatory(e.target.checked)}
                  className="h-4 w-4 rounded border-[#262626] bg-[#121212] text-white"
                />
              </div>

              {apkUpdateSuccessMsg && (
                <div className="rounded-lg border border-emerald-900/50 bg-emerald-950/20 p-2 text-center text-xs text-emerald-400">
                  {apkUpdateSuccessMsg}
                </div>
              )}

              <div className="sticky bottom-0 bg-[#121212] flex gap-2 pt-2 pb-1 border-t border-[#202020]">
                <button
                  type="button"
                  onClick={() => setShowApkModal(false)}
                  className="flex-1 rounded-lg border border-[#2B2B2B] bg-[#161616] py-2 text-xs font-semibold text-white hover:bg-[#202020]"
                >
                  CANCEL
                </button>
                <button
                  type="submit"
                  disabled={isSavingApkUpdate}
                  className="flex-1 rounded-lg bg-white py-2 text-xs font-semibold text-black hover:bg-neutral-200 disabled:opacity-50"
                >
                  {isSavingApkUpdate ? 'PUBLISHING...' : 'PUBLISH APK UPDATE'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ============================================================= */}
      {/* MODAL: SYSTEM SETTINGS (MAINTENANCE & ANNOUNCEMENTS) */}
      {/* ============================================================= */}
      {showSystemModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-3 sm:p-4">
          <div className="w-full max-w-lg rounded-xl border border-[#242424] bg-[#121212] p-4 sm:p-6 shadow-2xl space-y-4 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b border-[#202020] pb-3">
              <div className="flex items-center gap-2">
                <Sliders className="h-4 w-4 text-white" />
                <h3 className="font-semibold text-white tracking-wide text-sm">
                  MAINTENANCE & ANNOUNCEMENTS
                </h3>
              </div>
              <button
                onClick={() => setShowSystemModal(false)}
                className="text-neutral-400 hover:text-white p-1"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            {/* Tabs */}
            <div className="flex rounded-lg border border-[#262626] bg-[#0A0A0A] p-1 text-xs">
              <button
                type="button"
                onClick={() => setSystemTab('maintenance')}
                className={`flex-1 rounded-md py-1.5 font-medium transition-colors ${
                  systemTab === 'maintenance'
                    ? 'bg-white text-black font-semibold'
                    : 'text-neutral-400 hover:text-white'
                }`}
              >
                MAINTENANCE
              </button>
              <button
                type="button"
                onClick={() => setSystemTab('announcement')}
                className={`flex-1 rounded-md py-1.5 font-medium transition-colors ${
                  systemTab === 'announcement'
                    ? 'bg-white text-black font-semibold'
                    : 'text-neutral-400 hover:text-white'
                }`}
              >
                ANNOUNCEMENT
              </button>
            </div>

            <form onSubmit={handleSaveSystemConfig} className="space-y-4 text-xs">
              {systemTab === 'maintenance' && (
                <div className="space-y-3">
                  <div className="flex items-center justify-between rounded-lg border border-[#262626] bg-[#0A0A0A] p-3">
                    <div>
                      <div className="font-semibold text-white">MAINTENANCE GATE</div>
                      <div className="text-[11px] text-neutral-400">
                        When active, clients receive maintenance notice
                      </div>
                    </div>
                    <input
                      type="checkbox"
                      checked={maintenanceMode}
                      onChange={(e) => setMaintenanceMode(e.target.checked)}
                      className="h-4 w-4 rounded border-[#262626] bg-[#121212] text-white"
                    />
                  </div>

                  <div>
                    <label className="block text-[11px] font-medium tracking-wider text-neutral-400 uppercase">
                      MAINTENANCE NOTICE MESSAGE
                    </label>
                    <textarea
                      rows={3}
                      value={maintenanceMessage}
                      onChange={(e) => setMaintenanceMessage(e.target.value)}
                      className="mt-1.5 w-full rounded-lg border border-[#262626] bg-[#0A0A0A] px-3 py-2 text-xs text-white focus:border-white focus:outline-none focus:ring-1 focus:ring-white transition-colors"
                    />
                  </div>

                  <div>
                    <label className="block text-[11px] font-medium tracking-wider text-neutral-400 uppercase">
                      ESTIMATED COMPLETION
                    </label>
                    <input
                      type="text"
                      value={maintenanceEstimatedEnd}
                      onChange={(e) => setMaintenanceEstimatedEnd(e.target.value)}
                      className="mt-1.5 w-full rounded-lg border border-[#262626] bg-[#0A0A0A] px-3 py-2 text-xs font-mono text-white focus:border-white focus:outline-none focus:ring-1 focus:ring-white transition-colors"
                    />
                  </div>
                </div>
              )}

              {systemTab === 'announcement' && (
                <div className="space-y-3">
                  <div className="flex items-center justify-between rounded-lg border border-[#262626] bg-[#0A0A0A] p-3">
                    <div>
                      <div className="font-semibold text-white">ACTIVE BANNER</div>
                      <div className="text-[11px] text-neutral-400">
                        Broadcasts modal announcement on app launch
                      </div>
                    </div>
                    <input
                      type="checkbox"
                      checked={announcementActive}
                      onChange={(e) => setAnnouncementActive(e.target.checked)}
                      className="h-4 w-4 rounded border-[#262626] bg-[#121212] text-white"
                    />
                  </div>

                  <div>
                    <label className="block text-[11px] font-medium tracking-wider text-neutral-400 uppercase">
                      ANNOUNCEMENT TITLE
                    </label>
                    <input
                      type="text"
                      value={announcementTitle}
                      onChange={(e) => setAnnouncementTitle(e.target.value)}
                      className="mt-1.5 w-full rounded-lg border border-[#262626] bg-[#0A0A0A] px-3 py-2 text-xs text-white focus:border-white focus:outline-none focus:ring-1 focus:ring-white transition-colors"
                    />
                  </div>

                  <div>
                    <label className="block text-[11px] font-medium tracking-wider text-neutral-400 uppercase">
                      ANNOUNCEMENT MESSAGE
                    </label>
                    <textarea
                      rows={3}
                      value={announcementMessage}
                      onChange={(e) => setAnnouncementMessage(e.target.value)}
                      className="mt-1.5 w-full rounded-lg border border-[#262626] bg-[#0A0A0A] px-3 py-2 text-xs text-white focus:border-white focus:outline-none focus:ring-1 focus:ring-white transition-colors"
                    />
                  </div>

                  <div>
                    <label className="block text-[11px] font-medium tracking-wider text-neutral-400 uppercase">
                      REDIRECT LINK (TELEGRAM / WEBSITE)
                    </label>
                    <input
                      type="text"
                      value={announcementLink}
                      onChange={(e) => setAnnouncementLink(e.target.value)}
                      className="mt-1.5 w-full rounded-lg border border-[#262626] bg-[#0A0A0A] px-3 py-2 text-xs font-mono text-white focus:border-white focus:outline-none focus:ring-1 focus:ring-white transition-colors"
                    />
                  </div>
                </div>
              )}

              {systemConfigSuccessMsg && (
                <div className="rounded-lg border border-emerald-900/50 bg-emerald-950/20 p-2 text-center text-xs text-emerald-400">
                  {systemConfigSuccessMsg}
                </div>
              )}

              <div className="sticky bottom-0 bg-[#121212] flex gap-2 pt-3 pb-1 border-t border-[#202020]">
                <button
                  type="button"
                  onClick={() => setShowSystemModal(false)}
                  className="flex-1 rounded-lg border border-[#2B2B2B] bg-[#161616] py-2 text-xs font-semibold text-white hover:bg-[#202020]"
                >
                  CANCEL
                </button>
                <button
                  type="submit"
                  disabled={isSavingSystemConfig}
                  className="flex-1 rounded-lg bg-white py-2 text-xs font-semibold text-black hover:bg-neutral-200 disabled:opacity-50"
                >
                  {isSavingSystemConfig ? 'SAVING...' : 'SAVE CONFIG'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
