'use client';

import React, { useState, useEffect, useMemo } from 'react';
import {
  Key,
  Shield,
  Plus,
  RefreshCw,
  Trash2,
  Clock,
  Smartphone,
  Copy,
  Check,
  Search,
  Filter,
  Ban,
  Unlock,
  Download,
  AlertCircle,
  Database,
  ExternalLink,
  ChevronDown,
  Layers,
  Sparkles,
  Zap,
  Upload,
  FileArchive,
  CheckCircle2,
  Wrench,
  Megaphone,
} from 'lucide-react';
import { supabase, isSupabaseConfigured } from '@/lib/supabase';
import { LicenseKey, KeyStatus, DURATION_OPTIONS, DurationOption } from '@/lib/types';

// Mock initial data if Supabase is not connected yet
const INITIAL_MOCK_KEYS: LicenseKey[] = [
  {
    id: '1',
    key: 'ANOY-PRO-7D-9941',
    duration_label: '7 Days',
    duration_seconds: 604800,
    max_devices: 1,
    hwid_list: ['HWID-REDMI-K20-8921'],
    status: 'ACTIVE',
    created_at: new Date(Date.now() - 86400000).toISOString(),
    activated_at: new Date(Date.now() - 86400000).toISOString(),
    expires_at: new Date(Date.now() + 518400000).toISOString(),
    last_login_at: new Date().toISOString(),
    last_ip: '103.21.144.92',
    notes: 'Sample active user',
  },
  {
    id: '2',
    key: 'ANOY-TRIAL-1H-8820',
    duration_label: '1 Hour',
    duration_seconds: 3600,
    max_devices: 1,
    hwid_list: [],
    status: 'UNUSED',
    created_at: new Date().toISOString(),
    activated_at: null,
    expires_at: null,
    last_login_at: null,
    last_ip: null,
    notes: 'Unused key - timer starts on first login',
  },
  {
    id: '3',
    key: 'ANOY-LIFE-9921',
    duration_label: 'Lifetime',
    duration_seconds: 0,
    max_devices: 2,
    hwid_list: ['HWID-SAMSUNG-S23-7721'],
    status: 'ACTIVE',
    created_at: new Date(Date.now() - 86400000 * 10).toISOString(),
    activated_at: new Date(Date.now() - 86400000 * 10).toISOString(),
    expires_at: null,
    last_login_at: new Date().toISOString(),
    last_ip: '49.36.12.11',
    notes: 'VIP Lifetime Key (2 Devices)',
  },
];

export default function AdminDashboard() {
  const [keys, setKeys] = useState<LicenseKey[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<'ALL' | KeyStatus>('ALL');
  const [copiedKey, setCopiedKey] = useState<string | null>(null);

  // Modals state
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showExtendModal, setShowExtendModal] = useState<LicenseKey | null>(null);
  const [showBulkSuccessModal, setShowBulkSuccessModal] = useState<string[] | null>(null);
  const [showConfigModal, setShowConfigModal] = useState(false);
  const [showLibModal, setShowLibModal] = useState(false);
  const [showSystemModal, setShowSystemModal] = useState(false);

  // System Settings State (Maintenance Mode & Announcements)
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
  const [showApkModal, setShowApkModal] = useState(false);
  const [apkActiveVerName, setApkActiveVerName] = useState('2026.01.01');
  const [apkActiveVerCode, setApkActiveVerCode] = useState(2);
  const [apkActiveUrl, setApkActiveUrl] = useState('https://example.com/anoy_loader.apk');
  const [apkActiveChangelog, setApkActiveChangelog] = useState('Initial release with Supabase integration & AMOLED UI.');
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
  const [libStoragePath, setLibStoragePath] = useState<string | null>(null);
  const [libUpdatedAt, setLibUpdatedAt] = useState<string>(new Date().toISOString());
  const [libUploadMode, setLibUploadMode] = useState<'upload' | 'url'>('upload');
  const [libFile, setLibFile] = useState<File | null>(null);
  const [libNewVersion, setLibNewVersion] = useState('');
  const [libDirectUrl, setLibDirectUrl] = useState('');
  const [isUploadingLib, setIsUploadingLib] = useState(false);
  const [libSuccessMsg, setLibSuccessMsg] = useState<string | null>(null);

  // Form State for Key Creation
  const [createMode, setCreateMode] = useState<'single' | 'bulk'>('single');
  const [customKeyName, setCustomKeyName] = useState('');
  const [bulkCount, setBulkCount] = useState<number>(5);
  const [selectedDuration, setSelectedDuration] = useState<DurationOption>(DURATION_OPTIONS[5]); // Default: 7 Days
  const [maxDevices, setMaxDevices] = useState<number>(1);
  const [keyNotes, setKeyNotes] = useState('');
  const [extendingSeconds, setExtendingSeconds] = useState<number>(86400); // Default: 1 day extension

  // Dynamic Supabase status
  const [isLiveDatabase, setIsLiveDatabase] = useState(false);

  // Load keys and active lib info
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

        // Fetch active lib update
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
          setLibStoragePath(libData.storage_path);
          setLibUpdatedAt(libData.updated_at);
        }

        // Fetch system config (Maintenance & Announcement)
        const { data: sysData } = await supabase
          .from('system_config')
          .select('*')
          .eq('id', 'global')
          .maybeSingle();

        if (sysData) {
          setMaintenanceMode(!!sysData.maintenance_mode);
          setMaintenanceMessage(sysData.maintenance_message || 'Server is currently undergoing scheduled maintenance. Please check back soon.');
          setMaintenanceEstimatedEnd(sysData.maintenance_estimated_end || 'Soon');
          setAnnouncementActive(!!sysData.announcement_active);
          setAnnouncementTitle(sysData.announcement_title || '');
          setAnnouncementMessage(sysData.announcement_message || '');
          setAnnouncementType(sysData.announcement_type || 'info');
          setAnnouncementLink(sysData.announcement_link || '');
        }

        // Fetch active APK update
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
          setApkActiveChangelog(apkData.changelog);
          setApkActiveMandatory(!!apkData.is_mandatory);
        }

        setLoading(false);
        return;
      } catch (e) {
        console.error('Supabase fetch failed, falling back to local storage', e);
      }
    }

    // Local storage fallback for instant demo/test
    setIsLiveDatabase(false);
    const stored = localStorage.getItem('viper_license_keys');
    if (stored) {
      try {
        setKeys(JSON.parse(stored));
      } catch {
        setKeys(INITIAL_MOCK_KEYS);
      }
    } else {
      setKeys(INITIAL_MOCK_KEYS);
      localStorage.setItem('viper_license_keys', JSON.stringify(INITIAL_MOCK_KEYS));
    }

    const storedLib = localStorage.getItem('viper_lib_update');
    if (storedLib) {
      try {
        const parsedLib = JSON.parse(storedLib);
        setLibActiveVersion(parsedLib.version);
        setLibDownloadUrl(parsedLib.download_url);
        setLibUpdatedAt(parsedLib.updated_at);
      } catch (ignored) {}
    }

    const storedSys = localStorage.getItem('viper_system_config');
    if (storedSys) {
      try {
        const p = JSON.parse(storedSys);
        setMaintenanceMode(!!p.maintenance_mode);
        setMaintenanceMessage(p.maintenance_message || 'Server is currently undergoing scheduled maintenance. Please check back soon.');
        setMaintenanceEstimatedEnd(p.maintenance_estimated_end || 'Soon');
        setAnnouncementActive(!!p.announcement_active);
        setAnnouncementTitle(p.announcement_title || '');
        setAnnouncementMessage(p.announcement_message || '');
        setAnnouncementType(p.announcement_type || 'info');
        setAnnouncementLink(p.announcement_link || '');
      } catch (ignored) {}
    }

    const storedApk = localStorage.getItem('viper_apk_update');
    if (storedApk) {
      try {
        const p = JSON.parse(storedApk);
        setApkActiveVerName(p.version_name);
        setApkActiveVerCode(p.version_code);
        setApkActiveUrl(p.download_url);
        setApkActiveChangelog(p.changelog);
        setApkActiveMandatory(!!p.is_mandatory);
      } catch (ignored) {}
    }

    setLoading(false);
  };

  useEffect(() => {
    fetchKeys();
  }, []);

  // Save to local storage if running in mock mode
  const syncKeys = (updated: LicenseKey[]) => {
    setKeys(updated);
    if (!isLiveDatabase) {
      localStorage.setItem('viper_license_keys', JSON.stringify(updated));
    }
  };

  // KPI Calculations
  const stats = useMemo(() => {
    const total = keys.length;
    const active = keys.filter((k) => k.status === 'ACTIVE').length;
    const unused = keys.filter((k) => k.status === 'UNUSED').length;
    const expired = keys.filter((k) => k.status === 'EXPIRED').length;
    const banned = keys.filter((k) => k.status === 'BANNED').length;
    return { total, active, unused, expired, banned };
  }, [keys]);

  // Filtering
  const filteredKeys = useMemo(() => {
    return keys.filter((k) => {
      const matchesSearch =
        k.key.toLowerCase().includes(searchQuery.toLowerCase()) ||
        (k.notes && k.notes.toLowerCase().includes(searchQuery.toLowerCase())) ||
        k.hwid_list.some((h) => h.toLowerCase().includes(searchQuery.toLowerCase()));

      const matchesStatus = statusFilter === 'ALL' || k.status === statusFilter;
      return matchesSearch && matchesStatus;
    });
  }, [keys, searchQuery, statusFilter]);

  // Generate random key string
  const generateRandomKey = (prefix = 'ANOY') => {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    const segment = (len: number) =>
      Array.from({ length: len }, () => chars[Math.floor(Math.random() * chars.length)]).join('');
    return `${prefix}-${segment(4)}-${segment(4)}-${segment(4)}`;
  };

  // Handle Create Keys (Single & Bulk)
  const handleCreateKeys = async (e: React.FormEvent) => {
    e.preventDefault();
    const newKeyEntries: Partial<LicenseKey>[] = [];

    const finalDevices = Math.max(1, Number(maxDevices) || 1);
    const finalBulk = Math.max(1, Number(bulkCount) || 1);

    if (createMode === 'single') {
      const finalKey = customKeyName.trim() ? customKeyName.trim().toUpperCase() : generateRandomKey();
      newKeyEntries.push({
        key: finalKey,
        duration_label: selectedDuration.label,
        duration_seconds: selectedDuration.seconds,
        max_devices: finalDevices,
        hwid_list: [],
        status: 'UNUSED',
        notes: keyNotes.trim() || null,
        created_at: new Date().toISOString(),
        activated_at: null,
        expires_at: null,
      });
    } else {
      for (let i = 0; i < finalBulk; i++) {
        newKeyEntries.push({
          key: generateRandomKey(),
          duration_label: selectedDuration.label,
          duration_seconds: selectedDuration.seconds,
          max_devices: finalDevices,
          hwid_list: [],
          status: 'UNUSED',
          notes: keyNotes.trim() || `Bulk batch #${i + 1}`,
          created_at: new Date().toISOString(),
          activated_at: null,
          expires_at: null,
        });
      }
    }

    if (isLiveDatabase && supabase) {
      const { data, error } = await supabase.from('license_keys').insert(newKeyEntries).select();
      if (error) {
        alert('Failed to insert keys in Supabase: ' + error.message);
        return;
      }
      if (data) {
        setKeys((prev) => [...(data as LicenseKey[]), ...prev]);
        if (createMode === 'bulk') {
          setShowBulkSuccessModal(data.map((k) => k.key));
        }
      }
    } else {
      const localGenerated: LicenseKey[] = newKeyEntries.map((entry, idx) => ({
        ...entry,
        id: 'local_' + Date.now() + '_' + idx,
        hwid_list: [],
        status: 'UNUSED',
        last_login_at: null,
        last_ip: null,
      })) as LicenseKey[];

      const nextList = [...localGenerated, ...keys];
      syncKeys(nextList);
      if (createMode === 'bulk') {
        setShowBulkSuccessModal(localGenerated.map((k) => k.key));
      }
    }

    setShowCreateModal(false);
    setCustomKeyName('');
    setKeyNotes('');
  };

  // Reset HWID
  const handleResetHwid = async (keyItem: LicenseKey) => {
    if (!confirm(`Reset HWID for key ${keyItem.key}? The user will be able to bind a new device on their next login.`)) {
      return;
    }

    if (isLiveDatabase && supabase) {
      const { error } = await supabase.rpc('reset_key_hwid', { p_key: keyItem.key });
      if (error) {
        alert('Error resetting HWID: ' + error.message);
        return;
      }
    }

    const updated = keys.map((k) => (k.id === keyItem.id ? { ...k, hwid_list: [] } : k));
    syncKeys(updated);
  };

  // Toggle Ban
  const handleToggleBan = async (keyItem: LicenseKey) => {
    const newStatus: KeyStatus = keyItem.status === 'BANNED' ? (keyItem.activated_at ? 'ACTIVE' : 'UNUSED') : 'BANNED';

    if (isLiveDatabase && supabase) {
      const { error } = await supabase.from('license_keys').update({ status: newStatus }).eq('id', keyItem.id);
      if (error) {
        alert('Error updating key status: ' + error.message);
        return;
      }
    }

    const updated = keys.map((k) => (k.id === keyItem.id ? { ...k, status: newStatus } : k));
    syncKeys(updated);
  };

  // Extend Key
  const handleExtendKey = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!showExtendModal) return;

    if (isLiveDatabase && supabase) {
      const { error } = await supabase.rpc('extend_key_duration', {
        p_key: showExtendModal.key,
        p_additional_seconds: extendingSeconds,
      });
      if (error) {
        alert('Error extending key: ' + error.message);
        return;
      }
      fetchKeys();
    } else {
      const updated = keys.map((k) => {
        if (k.id === showExtendModal.id) {
          if (k.expires_at) {
            const currentExp = new Date(k.expires_at).getTime();
            const baseTime = currentExp < Date.now() ? Date.now() : currentExp;
            const newExpiry = new Date(baseTime + extendingSeconds * 1000).toISOString();
            return { ...k, expires_at: newExpiry, status: 'ACTIVE' as KeyStatus };
          } else {
            return { ...k, duration_seconds: k.duration_seconds + extendingSeconds };
          }
        }
        return k;
      });
      syncKeys(updated);
    }

    setShowExtendModal(null);
  };

  // Delete Single Key
  const handleDeleteKey = async (keyItem: LicenseKey) => {
    if (!confirm(`Are you sure you want to permanently delete key: ${keyItem.key}?`)) {
      return;
    }

    if (isLiveDatabase && supabase) {
      const { error } = await supabase.from('license_keys').delete().eq('id', keyItem.id);
      if (error) {
        alert('Error deleting key: ' + error.message);
        return;
      }
    }

    const updated = keys.filter((k) => k.id !== keyItem.id);
    syncKeys(updated);
  };

  // Delete All Expired Keys
  const handleDeleteExpired = async () => {
    if (!confirm('Are you sure you want to delete all EXPIRED keys from the database?')) {
      return;
    }

    if (isLiveDatabase && supabase) {
      const { data, error } = await supabase.rpc('delete_expired_keys');
      if (error) {
        alert('Error deleting expired keys: ' + error.message);
        return;
      }
      alert(`Successfully deleted ${data} expired keys.`);
      fetchKeys();
    } else {
      const updated = keys.filter((k) => k.status !== 'EXPIRED');
      const removedCount = keys.length - updated.length;
      syncKeys(updated);
      alert(`Deleted ${removedCount} expired keys.`);
    }
  };

  // Push Lib Update (Upload ZIP or Direct URL)
  const handlePushLibUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    const cleanVer = libNewVersion.trim();
    if (!cleanVer) {
      alert('Please enter a version string (e.g. 2.0)');
      return;
    }

    setIsUploadingLib(true);
    setLibSuccessMsg(null);

    try {
      if (libUploadMode === 'upload') {
        if (!libFile) {
          alert('Please select a .zip file to upload');
          setIsUploadingLib(false);
          return;
        }

        if (isLiveDatabase && supabase) {
          // 1. Delete old zip from Supabase Storage if present
          if (libStoragePath) {
            try {
              await supabase.storage.from('libs').remove([libStoragePath]);
            } catch (delErr) {
              console.warn('Failed to delete old zip from storage', delErr);
            }
          }

          // 2. Upload new zip
          const newStoragePath = `lib_${Date.now()}_${libFile.name.replace(/[^a-zA-Z0-9._-]/g, '_')}`;
          const { error: uploadError } = await supabase.storage
            .from('libs')
            .upload(newStoragePath, libFile, {
              upsert: true,
              contentType: 'application/zip',
            });

          if (uploadError) {
            alert('Supabase storage upload failed: ' + uploadError.message);
            setIsUploadingLib(false);
            return;
          }

          // 3. Get Public URL
          const { data: publicData } = supabase.storage.from('libs').getPublicUrl(newStoragePath);
          const newPublicUrl = publicData.publicUrl;

          // 4. Deactivate previous lib update records & insert active one
          await supabase.from('lib_updates').update({ is_active: false }).eq('is_active', true);
          const { error: insertError } = await supabase.from('lib_updates').insert({
            version: cleanVer,
            download_url: newPublicUrl,
            storage_path: newStoragePath,
            file_name: libFile.name,
            file_size: libFile.size,
            is_active: true,
            updated_at: new Date().toISOString(),
          });

          if (insertError) {
            alert('Failed to save lib update record: ' + insertError.message);
            setIsUploadingLib(false);
            return;
          }

          setLibActiveVersion(cleanVer);
          setLibDownloadUrl(newPublicUrl);
          setLibStoragePath(newStoragePath);
          setLibUpdatedAt(new Date().toISOString());
        } else {
          // Demo / Local fallback
          const mockUrl = 'https://example.com/libs/' + libFile.name;
          setLibActiveVersion(cleanVer);
          setLibDownloadUrl(mockUrl);
          setLibUpdatedAt(new Date().toISOString());
          localStorage.setItem('viper_lib_update', JSON.stringify({
            version: cleanVer,
            download_url: mockUrl,
            updated_at: new Date().toISOString(),
          }));
        }

        setLibSuccessMsg(`Library v${cleanVer} uploaded & pushed successfully! Old storage archive removed.`);
      } else {
        // Direct URL mode
        const cleanUrl = libDirectUrl.trim();
        if (!cleanUrl) {
          alert('Please enter a valid download URL');
          setIsUploadingLib(false);
          return;
        }

        if (isLiveDatabase && supabase) {
          await supabase.from('lib_updates').update({ is_active: false }).eq('is_active', true);
          const { error: insertError } = await supabase.from('lib_updates').insert({
            version: cleanVer,
            download_url: cleanUrl,
            storage_path: null,
            file_name: 'direct_url.zip',
            is_active: true,
            updated_at: new Date().toISOString(),
          });

          if (insertError) {
            alert('Failed to save lib update record: ' + insertError.message);
            setIsUploadingLib(false);
            return;
          }

          setLibActiveVersion(cleanVer);
          setLibDownloadUrl(cleanUrl);
          setLibStoragePath(null);
          setLibUpdatedAt(new Date().toISOString());
        } else {
          setLibActiveVersion(cleanVer);
          setLibDownloadUrl(cleanUrl);
          setLibUpdatedAt(new Date().toISOString());
          localStorage.setItem('viper_lib_update', JSON.stringify({
            version: cleanVer,
            download_url: cleanUrl,
            updated_at: new Date().toISOString(),
          }));
        }

        setLibSuccessMsg(`Library v${cleanVer} direct URL pushed successfully!`);
      }
    } catch (err: any) {
      alert('Error pushing update: ' + (err.message || err));
    } finally {
      setIsUploadingLib(false);
      setLibFile(null);
      setLibNewVersion('');
      setLibDirectUrl('');
    }
  };

  // Save System Config (Maintenance Mode & Announcements)
  const handleSaveSystemConfig = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSavingSystemConfig(true);
    setSystemConfigSuccessMsg(null);

    const payload = {
      id: 'global',
      maintenance_mode: maintenanceMode,
      maintenance_message: maintenanceMessage.trim(),
      maintenance_estimated_end: maintenanceEstimatedEnd.trim(),
      announcement_active: announcementActive,
      announcement_title: announcementTitle.trim(),
      announcement_message: announcementMessage.trim(),
      announcement_type: announcementType,
      announcement_link: announcementLink.trim(),
      updated_at: new Date().toISOString(),
    };

    try {
      if (isLiveDatabase && supabase) {
        const { error } = await supabase.from('system_config').upsert(payload);
        if (error) {
          alert('Failed to save system settings to Supabase: ' + error.message);
          setIsSavingSystemConfig(false);
          return;
        }
      }

      localStorage.setItem('viper_system_config', JSON.stringify(payload));
      setSystemConfigSuccessMsg(
        maintenanceMode
          ? 'Maintenance Mode is now ACTIVE! App users will be blocked with your notice.'
          : 'System configuration updated and pushed to app successfully!'
      );
    } catch (err: any) {
      alert('Error saving system config: ' + (err.message || err));
    } finally {
      setIsSavingSystemConfig(false);
    }
  };

  // Push APK In-App Update
  const handleSaveApkUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    const cleanUrl = apkNewUrl.trim();
    const cleanVer = apkNewVerName.trim();
    const cleanCode = parseInt(apkNewVerCode.trim(), 10);

    if (!cleanVer || isNaN(cleanCode) || !cleanUrl) {
      alert('Please enter Version Name, numeric Version Code, and APK Download URL');
      return;
    }

    setIsSavingApkUpdate(true);
    setApkUpdateSuccessMsg(null);

    const payload = {
      version_name: cleanVer,
      version_code: cleanCode,
      download_url: cleanUrl,
      changelog: apkNewChangelog.trim() || 'Performance improvements and bug fixes.',
      is_mandatory: apkNewMandatory,
      is_active: true,
      updated_at: new Date().toISOString(),
    };

    try {
      if (isLiveDatabase && supabase) {
        // Deactivate previous active updates
        await supabase.from('app_apk_updates').update({ is_active: false }).eq('is_active', true);
        const { error } = await supabase.from('app_apk_updates').insert(payload);
        if (error) {
          alert('Failed to save APK update to Supabase: ' + error.message);
          setIsSavingApkUpdate(false);
          return;
        }
      }

      setApkActiveVerName(cleanVer);
      setApkActiveVerCode(cleanCode);
      setApkActiveUrl(cleanUrl);
      setApkActiveChangelog(payload.changelog);
      setApkActiveMandatory(apkNewMandatory);

      localStorage.setItem('viper_apk_update', JSON.stringify(payload));
      setApkUpdateSuccessMsg(`APK update v${cleanVer} (Code: ${cleanCode}) published successfully!`);
    } catch (err: any) {
      alert('Error publishing APK update: ' + (err.message || err));
    } finally {
      setIsSavingApkUpdate(false);
      setApkNewVerName('');
      setApkNewVerCode('');
      setApkNewUrl('');
      setApkNewChangelog('');
    }
  };

  // Copy helper
  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
    setCopiedKey(text);
    setTimeout(() => setCopiedKey(null), 2000);
  };

  // Download Bulk TXT
  const downloadBulkTxt = (keysList: string[]) => {
    const element = document.createElement('a');
    const file = new Blob([keysList.join('\n')], { type: 'text/plain' });
    element.href = URL.createObjectURL(file);
    element.download = `anoy_keys_${new Date().toISOString().slice(0, 10)}.txt`;
    document.body.appendChild(element);
    element.click();
    document.body.removeChild(element);
  };

  return (
    <div className="min-h-screen bg-background text-textPrimary pb-16">
      {/* Top Premium Navigation Bar */}
      <header className="sticky top-0 z-30 border-b border-surfaceBorder bg-surface/80 backdrop-blur-xl">
        <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-3.5 sm:px-6">
          <div className="flex items-center space-x-3.5">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/10 border border-primary/20 text-primary shadow-sm">
              <Shield className="h-5 w-5" />
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <h1 className="text-base font-bold tracking-tight text-textPrimary sm:text-lg">
                  ANOY<span className="text-primary font-semibold">PANEL</span>
                </h1>
                <span className="rounded-md bg-surfaceBorder px-2 py-0.5 text-[10px] font-semibold tracking-wider text-textSecondary uppercase">
                  ADMIN
                </span>
              </div>
              <p className="text-[11px] text-textMuted hidden sm:block">License & Device Control Suite</p>
            </div>
          </div>

          <div className="flex items-center space-x-2 sm:space-x-3">
            {/* Database indicator */}
            <button
              onClick={() => setShowConfigModal(true)}
              className={`flex items-center space-x-1.5 rounded-xl px-3 py-1.5 text-xs font-medium border transition-all ${
                isLiveDatabase
                  ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20 hover:bg-emerald-500/15'
                  : 'bg-amber-500/10 text-amber-400 border-amber-500/30 animate-pulse'
              }`}
            >
              <span className="h-1.5 w-1.5 rounded-full bg-emerald-400 animate-pulse" />
              <span className="hidden sm:inline">{isLiveDatabase ? 'Backend Connected' : 'Demo Mode (Click to setup)'}</span>
              <span className="sm:hidden">{isLiveDatabase ? 'Live' : 'Setup'}</span>
            </button>

            {/* Refresh */}
            <button
              onClick={fetchKeys}
              className="rounded-xl border border-surfaceBorder bg-surface p-2 text-textSecondary hover:bg-surfaceHover hover:text-textPrimary transition-colors"
              title="Refresh Keys"
            >
              <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
            </button>

            {/* Manage Lib Updates Button */}
            <button
              onClick={() => {
                setShowLibModal(true);
                setLibSuccessMsg(null);
              }}
              className="flex items-center space-x-1.5 rounded-xl border border-surfaceBorder bg-surface px-3 py-1.5 text-xs font-medium text-textSecondary hover:text-textPrimary hover:bg-surfaceHover transition-all sm:px-3.5 sm:py-2"
            >
              <Upload className="h-3.5 w-3.5 text-primary" />
              <span className="hidden md:inline">Lib Updates</span>
              <span className="md:hidden">Libs</span>
            </button>

            {/* APK In-App Updates Button */}
            <button
              onClick={() => {
                setShowApkModal(true);
                setApkUpdateSuccessMsg(null);
              }}
              className="flex items-center space-x-1.5 rounded-xl border border-surfaceBorder bg-surface px-3 py-1.5 text-xs font-medium text-textSecondary hover:text-textPrimary hover:bg-surfaceHover transition-all sm:px-3.5 sm:py-2"
            >
              <Smartphone className="h-3.5 w-3.5 text-primary" />
              <span className="hidden md:inline">APK Releases</span>
              <span className="md:hidden">APK</span>
            </button>

            {/* System Control Button */}
            <button
              onClick={() => {
                setShowSystemModal(true);
                setSystemConfigSuccessMsg(null);
              }}
              className={`flex items-center space-x-1.5 rounded-xl border px-3 py-1.5 text-xs font-medium transition-all sm:px-3.5 sm:py-2 ${
                maintenanceMode
                  ? 'bg-rose-500/10 text-rose-400 border-rose-500/30'
                  : 'border-surfaceBorder bg-surface text-textSecondary hover:text-textPrimary hover:bg-surfaceHover'
              }`}
            >
              {maintenanceMode ? <Wrench className="h-3.5 w-3.5 text-rose-400" /> : <Megaphone className="h-3.5 w-3.5 text-amber-400" />}
              <span className="hidden md:inline">{maintenanceMode ? 'Maintenance (Active)' : 'Notices & Maintenance'}</span>
              <span className="md:hidden">{maintenanceMode ? 'Maint' : 'Notice'}</span>
            </button>

            {/* Create Key Button */}
            <button
              onClick={() => setShowCreateModal(true)}
              className="flex items-center space-x-1.5 rounded-xl bg-primary hover:bg-primaryHover px-3.5 py-1.5 text-xs font-semibold text-white shadow-sm shadow-blue-500/20 transition-all active:scale-[0.98] sm:px-4 sm:py-2 sm:text-sm"
            >
              <Plus className="h-4 w-4" />
              <span>Create Keys</span>
            </button>
          </div>
        </div>
      </header>

      {/* Main Content Area */}
      <main className="mx-auto max-w-7xl px-4 pt-6 sm:px-6">
        {/* Maintenance Alert Banner */}
        {maintenanceMode && (
          <div className="mb-5 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 rounded-2xl border border-rose-500/30 bg-rose-500/10 p-4 text-xs text-rose-200">
            <div className="flex items-center space-x-3">
              <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-rose-500/20 text-rose-400 shrink-0">
                <Wrench className="h-4 w-4" />
              </div>
              <div>
                <span className="font-semibold text-rose-300">Maintenance Mode Active:</span>{' '}
                <span>{maintenanceMessage}</span>
                <span className="ml-2 text-rose-400/80">(Est. completion: {maintenanceEstimatedEnd})</span>
              </div>
            </div>
            <button
              onClick={() => {
                setShowSystemModal(true);
                setSystemTab('maintenance');
              }}
              className="rounded-xl bg-rose-500/20 hover:bg-rose-500/30 border border-rose-500/30 px-3.5 py-1.5 text-xs font-semibold text-rose-200 shrink-0 transition-colors"
            >
              Configure
            </button>
          </div>
        )}

        {/* KPI Stat Cards Grid */}
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-5 sm:gap-4">
          <div className="rounded-2xl border border-surfaceBorder bg-surface p-4 sm:p-5 shadow-xs hover:border-slate-700 transition-colors">
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-textSecondary">Total Keys</span>
              <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-primary/10 text-primary">
                <Key className="h-4 w-4" />
              </div>
            </div>
            <div className="mt-3 text-2xl font-bold tracking-tight text-textPrimary">{stats.total}</div>
          </div>

          <div className="rounded-2xl border border-surfaceBorder bg-surface p-4 sm:p-5 shadow-xs hover:border-slate-700 transition-colors">
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-textSecondary">Active</span>
              <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-emerald-500/10 text-emerald-400">
                <Zap className="h-4 w-4" />
              </div>
            </div>
            <div className="mt-3 text-2xl font-bold tracking-tight text-emerald-400">{stats.active}</div>
          </div>

          <div className="rounded-2xl border border-surfaceBorder bg-surface p-4 sm:p-5 shadow-xs hover:border-slate-700 transition-colors">
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-textSecondary">Unused</span>
              <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-blue-500/10 text-blue-400">
                <Clock className="h-4 w-4" />
              </div>
            </div>
            <div className="mt-3 text-2xl font-bold tracking-tight text-blue-400">{stats.unused}</div>
          </div>

          <div className="rounded-2xl border border-surfaceBorder bg-surface p-4 sm:p-5 shadow-xs hover:border-slate-700 transition-colors">
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-textSecondary">Expired</span>
              <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-amber-500/10 text-amber-400">
                <AlertCircle className="h-4 w-4" />
              </div>
            </div>
            <div className="mt-3 text-2xl font-bold tracking-tight text-amber-400">{stats.expired}</div>
          </div>

          <div className="col-span-2 rounded-2xl border border-surfaceBorder bg-surface p-4 sm:p-5 shadow-xs hover:border-slate-700 transition-colors sm:col-span-1">
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-textSecondary">Banned</span>
              <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-rose-500/10 text-rose-400">
                <Ban className="h-4 w-4" />
              </div>
            </div>
            <div className="mt-3 text-2xl font-bold tracking-tight text-rose-400">{stats.banned}</div>
          </div>
        </div>

        {/* Filter and Action Bar */}
        <div className="mt-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          {/* Search bar */}
          <div className="relative flex-1">
            <Search className="absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-textMuted" />
            <input
              type="text"
              placeholder="Search key, HWID, notes..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full rounded-xl border border-surfaceBorder bg-surface py-2.5 pl-10 pr-4 text-xs sm:text-sm text-textPrimary placeholder:text-textMuted focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary/40 transition-colors"
            />
          </div>

          {/* Status Filter segmented pills */}
          <div className="flex items-center space-x-2 overflow-x-auto pb-1 sm:pb-0">
            <div className="flex items-center space-x-1 rounded-xl border border-surfaceBorder bg-surface p-1">
              {(['ALL', 'ACTIVE', 'UNUSED', 'EXPIRED', 'BANNED'] as const).map((st) => (
                <button
                  key={st}
                  onClick={() => setStatusFilter(st)}
                  className={`rounded-lg px-3 py-1.5 text-xs font-semibold transition-all ${
                    statusFilter === st
                      ? 'bg-primary text-white shadow-xs'
                      : 'text-textSecondary hover:text-textPrimary hover:bg-surfaceHover'
                  }`}
                >
                  {st}
                </button>
              ))}
            </div>

            {/* Clean expired button */}
            <button
              onClick={handleDeleteExpired}
              className="flex items-center space-x-1.5 rounded-xl border border-amber-500/20 bg-amber-500/10 px-3 py-2 text-xs font-semibold text-amber-400 hover:bg-amber-500/15 transition-all"
              title="Delete all expired keys"
            >
              <Trash2 className="h-3.5 w-3.5" />
              <span className="hidden md:inline">Clean Expired</span>
            </button>
          </div>
        </div>

        {/* Keys Table / Mobile Cards */}
        <div className="mt-5 rounded-2xl border border-surfaceBorder bg-surface shadow-xs overflow-hidden">
          {filteredKeys.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-20 text-center text-textMuted">
              <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-surfaceHover border border-surfaceBorder text-textMuted mb-3">
                <Key className="h-6 w-6 opacity-40" />
              </div>
              <p className="text-sm font-semibold text-textSecondary">No license keys found</p>
              <p className="text-xs text-textMuted mt-1">Create a new license or adjust your search filter.</p>
            </div>
          ) : (
            <>
              {/* Desktop Table View */}
              <div className="hidden lg:block overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className="border-b border-surfaceBorder bg-surfaceHover/50 text-textMuted uppercase tracking-wider text-[11px] font-semibold">
                    <tr>
                      <th className="py-3.5 pl-5 pr-2">License Key</th>
                      <th className="px-3 py-3.5">Status</th>
                      <th className="px-3 py-3.5">Duration</th>
                      <th className="px-3 py-3.5">Devices / HWID</th>
                      <th className="px-3 py-3.5">Expires At</th>
                      <th className="px-3 py-3.5">Notes</th>
                      <th className="py-3.5 pl-2 pr-5 text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-surfaceBorder/60">
                    {filteredKeys.map((item) => (
                      <tr key={item.id} className="hover:bg-surfaceHover/30 transition-colors">
                        <td className="py-3.5 pl-5 pr-2 font-mono font-medium text-textPrimary">
                          <div className="flex items-center space-x-2">
                            <span className="tracking-wide">{item.key}</span>
                            <button
                              onClick={() => copyToClipboard(item.key)}
                              className="text-textMuted hover:text-primary transition-colors p-1 rounded-md hover:bg-surfaceHover"
                              title="Copy Key"
                            >
                              {copiedKey === item.key ? (
                                <Check className="h-3.5 w-3.5 text-emerald-400" />
                              ) : (
                                <Copy className="h-3.5 w-3.5" />
                              )}
                            </button>
                          </div>
                        </td>

                        <td className="px-3 py-3.5">
                          <span
                            className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-[10px] font-semibold tracking-wide uppercase border ${
                              item.status === 'ACTIVE'
                                ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                                : item.status === 'UNUSED'
                                ? 'bg-blue-500/10 text-blue-400 border-blue-500/20'
                                : item.status === 'EXPIRED'
                                ? 'bg-slate-800 text-slate-400 border-slate-700/50'
                                : 'bg-rose-500/10 text-rose-400 border-rose-500/20'
                            }`}
                          >
                            {item.status}
                          </span>
                        </td>

                        <td className="px-3 py-3.5 font-medium text-textSecondary">{item.duration_label}</td>

                        <td className="px-3 py-3.5">
                          <div className="flex items-center space-x-2">
                            <span className="font-medium text-textPrimary">
                              {item.hwid_list.length} / {item.max_devices}
                            </span>
                            {item.hwid_list.length > 0 && (
                              <button
                                onClick={() => handleResetHwid(item)}
                                className="rounded-lg px-2 py-0.5 text-[10px] font-semibold text-amber-400 hover:bg-amber-400/10 border border-amber-400/30 transition-colors"
                                title="Reset HWID"
                              >
                                Reset
                              </button>
                            )}
                          </div>
                        </td>

                        <td className="px-3 py-3.5 text-textMuted">
                          {item.duration_seconds <= 0 ? (
                            <span className="font-semibold text-indigo-400">LIFETIME</span>
                          ) : item.status === 'UNUSED' ? (
                            <span className="italic text-textMuted">Timer starts on first login</span>
                          ) : item.expires_at ? (
                            new Date(item.expires_at).toLocaleString()
                          ) : (
                            '—'
                          )}
                        </td>

                        <td className="px-3 py-3.5 text-textMuted max-w-[150px] truncate">{item.notes || '—'}</td>

                        <td className="py-3.5 pl-2 pr-5 text-right">
                          <div className="flex items-center justify-end space-x-1">
                            <button
                              onClick={() => setShowExtendModal(item)}
                              className="rounded-lg p-1.5 text-textMuted hover:bg-surfaceBorder/80 hover:text-primary transition-colors"
                              title="Extend Time"
                            >
                              <Clock className="h-4 w-4" />
                            </button>

                            <button
                              onClick={() => handleToggleBan(item)}
                              className={`rounded-lg p-1.5 hover:bg-surfaceBorder/80 transition-colors ${
                                item.status === 'BANNED'
                                  ? 'text-rose-400 hover:text-emerald-400'
                                  : 'text-textMuted hover:text-rose-400'
                              }`}
                              title={item.status === 'BANNED' ? 'Unban Key' : 'Ban Key'}
                            >
                              {item.status === 'BANNED' ? (
                                <Unlock className="h-4 w-4" />
                              ) : (
                                <Ban className="h-4 w-4" />
                              )}
                            </button>

                            <button
                              onClick={() => handleDeleteKey(item)}
                              className="rounded-lg p-1.5 text-textMuted hover:bg-surfaceBorder/80 hover:text-rose-400 transition-colors"
                              title="Delete Key"
                            >
                              <Trash2 className="h-4 w-4" />
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {/* Mobile Card View */}
              <div className="block lg:hidden divide-y divide-surfaceBorder/60">
                {filteredKeys.map((item) => (
                  <div key={item.id} className="p-4 space-y-3">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center space-x-2">
                        <span className="font-mono font-medium text-sm text-textPrimary">{item.key}</span>
                        <button
                          onClick={() => copyToClipboard(item.key)}
                          className="text-textMuted hover:text-primary p-1 rounded-md"
                        >
                          {copiedKey === item.key ? (
                            <Check className="h-4 w-4 text-emerald-400" />
                          ) : (
                            <Copy className="h-4 w-4" />
                          )}
                        </button>
                      </div>

                      <span
                        className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-[10px] font-semibold uppercase border ${
                          item.status === 'ACTIVE'
                            ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                            : item.status === 'UNUSED'
                            ? 'bg-blue-500/10 text-blue-400 border-blue-500/20'
                            : item.status === 'EXPIRED'
                            ? 'bg-slate-800 text-slate-400 border-slate-700/50'
                            : 'bg-rose-500/10 text-rose-400 border-rose-500/20'
                        }`}
                      >
                        {item.status}
                      </span>
                    </div>

                    <div className="grid grid-cols-2 gap-2 text-xs text-textMuted">
                      <div>
                        <span className="block text-[10px] uppercase tracking-wider font-medium text-textMuted">Duration</span>
                        <span className="font-medium text-textSecondary">{item.duration_label}</span>
                      </div>

                      <div>
                        <span className="block text-[10px] uppercase tracking-wider font-medium text-textMuted">Devices</span>
                        <span className="font-medium text-textSecondary">
                          {item.hwid_list.length} / {item.max_devices} bound
                        </span>
                      </div>

                      <div className="col-span-2">
                        <span className="block text-[10px] uppercase tracking-wider font-medium text-textMuted">Expires</span>
                        <span className="font-medium text-textSecondary">
                          {item.duration_seconds <= 0 ? (
                            <span className="text-indigo-400 font-semibold">LIFETIME</span>
                          ) : item.status === 'UNUSED' ? (
                            <span className="italic">Timer starts on first login</span>
                          ) : item.expires_at ? (
                            new Date(item.expires_at).toLocaleString()
                          ) : (
                            '—'
                          )}
                        </span>
                      </div>
                    </div>

                    {/* Mobile Actions */}
                    <div className="flex items-center justify-end space-x-2 pt-2.5 border-t border-surfaceBorder/40">
                      {item.hwid_list.length > 0 && (
                        <button
                          onClick={() => handleResetHwid(item)}
                          className="rounded-lg border border-amber-400/30 bg-amber-400/10 px-2.5 py-1.5 text-xs font-semibold text-amber-400"
                        >
                          Reset HWID
                        </button>
                      )}

                      <button
                        onClick={() => setShowExtendModal(item)}
                        className="rounded-lg border border-surfaceBorder bg-surfaceHover px-2.5 py-1.5 text-xs font-semibold text-textSecondary hover:text-primary"
                      >
                        + Extend
                      </button>

                      <button
                        onClick={() => handleToggleBan(item)}
                        className="rounded-lg border border-surfaceBorder bg-surfaceHover p-1.5 text-textMuted"
                      >
                        {item.status === 'BANNED' ? <Unlock className="h-4 w-4" /> : <Ban className="h-4 w-4" />}
                      </button>

                      <button
                        onClick={() => handleDeleteKey(item)}
                        className="rounded-lg border border-surfaceBorder bg-surfaceHover p-1.5 text-rose-400"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </>
          )}
        </div>
      </main>

      {/* CREATE KEY MODAL */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 p-4 backdrop-blur-md">
          <div className="w-full max-w-lg rounded-2xl border border-surfaceBorder bg-surface p-6 shadow-2xl animate-in fade-in zoom-in-95 duration-150">
            <div className="flex items-center justify-between border-b border-surfaceBorder pb-4">
              <div className="flex items-center space-x-2.5">
                <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-primary/10 text-primary">
                  <Plus className="h-4 w-4" />
                </div>
                <h3 className="text-base font-bold text-textPrimary">Generate License Keys</h3>
              </div>
              <button
                onClick={() => setShowCreateModal(false)}
                className="rounded-lg p-1 text-textMuted hover:text-textPrimary hover:bg-surfaceHover transition-colors"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleCreateKeys} className="mt-5 space-y-4">
              {/* Single vs Bulk Tab */}
              <div className="grid grid-cols-2 gap-1 rounded-xl bg-background p-1 border border-surfaceBorder">
                <button
                  type="button"
                  onClick={() => setCreateMode('single')}
                  className={`rounded-lg py-2 text-xs font-semibold transition-all ${
                    createMode === 'single'
                      ? 'bg-primary text-white shadow-xs'
                      : 'text-textSecondary hover:text-textPrimary'
                  }`}
                >
                  Single Key
                </button>
                <button
                  type="button"
                  onClick={() => setCreateMode('bulk')}
                  className={`rounded-lg py-2 text-xs font-semibold transition-all ${
                    createMode === 'bulk'
                      ? 'bg-primary text-white shadow-xs'
                      : 'text-textSecondary hover:text-textPrimary'
                  }`}
                >
                  Bulk Generator
                </button>
              </div>

              {/* Single Mode Custom Name */}
              {createMode === 'single' ? (
                <div>
                  <label className="block text-xs font-medium text-textSecondary">
                    Custom Key Name <span className="text-textMuted font-normal">(Optional, auto-generated if empty)</span>
                  </label>
                  <input
                    type="text"
                    placeholder="e.g. ANOY-PREMIUM-USER-01"
                    value={customKeyName}
                    onChange={(e) => setCustomKeyName(e.target.value)}
                    className="mt-1.5 w-full rounded-xl border border-surfaceBorder bg-background px-3.5 py-2.5 font-mono text-xs sm:text-sm text-textPrimary uppercase placeholder:text-textMuted focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary/40 transition-colors"
                  />
                </div>
              ) : (
                <div>
                  <label className="block text-xs font-medium text-textSecondary">
                    Number of Keys to Generate
                  </label>
                  <input
                    type="number"
                    min={1}
                    max={1000}
                    value={bulkCount || ''}
                    onChange={(e) => setBulkCount(e.target.value === '' ? 0 : Math.max(1, parseInt(e.target.value) || 1))}
                    placeholder="Enter quantity (e.g. 10, 25, 50, 100)"
                    className="mt-1.5 w-full rounded-xl border border-surfaceBorder bg-background px-3.5 py-2.5 font-mono text-xs sm:text-sm text-textPrimary placeholder:text-textMuted focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary/40 transition-colors"
                  />
                  <p className="mt-1 text-[11px] text-textMuted">
                    Type any number of keys you want to generate.
                  </p>
                </div>
              )}

              {/* Duration Selector */}
              <div>
                <label className="block text-xs font-medium text-textSecondary mb-1.5">
                  Select Key Duration
                </label>
                <div className="grid grid-cols-3 sm:grid-cols-4 gap-2">
                  {DURATION_OPTIONS.map((opt) => (
                    <button
                      key={opt.label}
                      type="button"
                      onClick={() => setSelectedDuration(opt)}
                      className={`rounded-xl p-2 text-center border text-xs font-semibold transition-all ${
                        selectedDuration.label === opt.label
                          ? 'border-primary bg-primary text-white shadow-xs'
                          : 'border-surfaceBorder bg-background text-textSecondary hover:bg-surfaceHover hover:text-textPrimary'
                      }`}
                    >
                      {opt.label}
                    </button>
                  ))}
                </div>
              </div>

              {/* Device Limit */}
              <div>
                <label className="block text-xs font-medium text-textSecondary mb-1.5">
                  Device Limit (Max Devices per Key)
                </label>
                <input
                  type="number"
                  min={1}
                  max={100}
                  value={maxDevices || ''}
                  onChange={(e) => setMaxDevices(e.target.value === '' ? 0 : Math.max(1, parseInt(e.target.value) || 1))}
                  placeholder="Enter device limit (e.g. 1, 2, 5)"
                  className="mt-1.5 w-full rounded-xl border border-surfaceBorder bg-background px-3.5 py-2.5 font-mono text-xs sm:text-sm text-textPrimary placeholder:text-textMuted focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary/40 transition-colors"
                />
                <p className="mt-1 text-[11px] text-textMuted">
                  Type the max number of unique devices that can bind to each key.
                </p>
              </div>

              {/* Notes */}
              <div>
                <label className="block text-xs font-medium text-textSecondary">
                  Admin Notes / Customer Tag <span className="text-textMuted font-normal">(Optional)</span>
                </label>
                <input
                  type="text"
                  placeholder="e.g. VIP Customer / Order #104"
                  value={keyNotes}
                  onChange={(e) => setKeyNotes(e.target.value)}
                  className="mt-1.5 w-full rounded-xl border border-surfaceBorder bg-background px-3.5 py-2.5 text-xs sm:text-sm text-textPrimary placeholder:text-textMuted focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary/40 transition-colors"
                />
              </div>

              <div className="pt-2">
                <button
                  type="submit"
                  className="w-full rounded-xl bg-primary hover:bg-primaryHover py-3 text-xs sm:text-sm font-semibold text-white shadow-sm shadow-blue-500/20 transition-all active:scale-[0.98]"
                >
                  {createMode === 'single' ? 'Create Single License Key' : `Generate ${bulkCount} License Keys`}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* EXTEND TIME MODAL */}
      {showExtendModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 p-4 backdrop-blur-md">
          <div className="w-full max-w-md rounded-2xl border border-surfaceBorder bg-surface p-6 shadow-2xl">
            <div className="flex items-center justify-between border-b border-surfaceBorder pb-4">
              <div className="flex items-center space-x-2.5">
                <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-primary/10 text-primary">
                  <Clock className="h-4 w-4" />
                </div>
                <h3 className="text-base font-bold text-textPrimary">Extend Key Duration</h3>
              </div>
              <button
                onClick={() => setShowExtendModal(null)}
                className="rounded-lg p-1 text-textMuted hover:text-textPrimary hover:bg-surfaceHover transition-colors"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleExtendKey} className="mt-4 space-y-4">
              <div className="rounded-xl bg-background p-3.5 border border-surfaceBorder">
                <span className="text-[11px] uppercase font-semibold text-textMuted">Target Key:</span>
                <p className="font-mono font-medium text-sm text-textPrimary mt-0.5">{showExtendModal.key}</p>
                <p className="text-xs text-textSecondary mt-1">
                  Current: {showExtendModal.duration_label}{' '}
                  {showExtendModal.expires_at ? `(Expires: ${new Date(showExtendModal.expires_at).toLocaleDateString()})` : ''}
                </p>
              </div>

              <div>
                <label className="block text-xs font-medium text-textSecondary mb-2">
                  Select Duration to Add:
                </label>
                <div className="grid grid-cols-3 gap-2">
                  {[
                    { label: '+1 Day', sec: 86400 },
                    { label: '+3 Days', sec: 259200 },
                    { label: '+7 Days', sec: 604800 },
                    { label: '+15 Days', sec: 1296000 },
                    { label: '+30 Days', sec: 2592000 },
                    { label: '+60 Days', sec: 5184000 },
                  ].map((ext) => (
                    <button
                      key={ext.sec}
                      type="button"
                      onClick={() => setExtendingSeconds(ext.sec)}
                      className={`rounded-xl py-2 text-xs font-semibold border transition-all ${
                        extendingSeconds === ext.sec
                          ? 'border-primary bg-primary text-white shadow-xs'
                          : 'border-surfaceBorder bg-background text-textSecondary hover:bg-surfaceHover hover:text-textPrimary'
                      }`}
                    >
                      {ext.label}
                    </button>
                  ))}
                </div>
              </div>

              <div className="pt-2">
                <button
                  type="submit"
                  className="w-full rounded-xl bg-primary hover:bg-primaryHover py-2.5 text-xs sm:text-sm font-semibold text-white shadow-sm shadow-blue-500/20 transition-all active:scale-[0.98]"
                >
                  Add Time to License Key
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* BULK SUCCESS / COPY MODAL */}
      {showBulkSuccessModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 p-4 backdrop-blur-md">
          <div className="w-full max-w-lg rounded-2xl border border-surfaceBorder bg-surface p-6 shadow-2xl">
            <div className="flex items-center justify-between border-b border-surfaceBorder pb-4">
              <div className="flex items-center space-x-2.5">
                <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-emerald-500/10 text-emerald-400">
                  <Sparkles className="h-4 w-4" />
                </div>
                <h3 className="text-base font-bold text-textPrimary">
                  {showBulkSuccessModal.length} Keys Generated Successfully!
                </h3>
              </div>
              <button
                onClick={() => setShowBulkSuccessModal(null)}
                className="rounded-lg p-1 text-textMuted hover:text-textPrimary hover:bg-surfaceHover transition-colors"
              >
                ✕
              </button>
            </div>

            <div className="mt-4">
              <textarea
                readOnly
                rows={8}
                value={showBulkSuccessModal.join('\n')}
                className="w-full rounded-xl border border-surfaceBorder bg-background p-3.5 font-mono text-xs text-textPrimary focus:outline-none"
              />
            </div>

            <div className="mt-4 flex items-center space-x-3">
              <button
                onClick={() => {
                  copyToClipboard(showBulkSuccessModal.join('\n'));
                  alert('All keys copied to clipboard!');
                }}
                className="flex-1 flex items-center justify-center space-x-2 rounded-xl bg-primary hover:bg-primaryHover py-2.5 text-xs font-semibold text-white shadow-sm shadow-blue-500/20 transition-all"
              >
                <Copy className="h-4 w-4" />
                <span>Copy All Keys</span>
              </button>

              <button
                onClick={() => downloadBulkTxt(showBulkSuccessModal)}
                className="flex items-center space-x-2 rounded-xl border border-surfaceBorder bg-surfaceHover px-4 py-2.5 text-xs font-semibold text-textPrimary hover:bg-surfaceBorder transition-colors"
              >
                <Download className="h-4 w-4" />
                <span>Download .txt</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* MANAGE LIB UPDATES MODAL */}
      {showLibModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 p-4 backdrop-blur-md">
          <div className="w-full max-w-lg rounded-2xl border border-surfaceBorder bg-surface p-6 shadow-2xl animate-in fade-in zoom-in-95 duration-150 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b border-surfaceBorder pb-4">
              <div className="flex items-center space-x-2.5">
                <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-primary/10 text-primary">
                  <Upload className="h-4 w-4" />
                </div>
                <h3 className="text-base font-bold text-textPrimary">Manage Library Updates</h3>
              </div>
              <button
                onClick={() => setShowLibModal(false)}
                className="rounded-lg p-1 text-textMuted hover:text-textPrimary hover:bg-surfaceHover transition-colors"
              >
                ✕
              </button>
            </div>

            {/* Current Active Lib Banner */}
            <div className="mt-4 rounded-xl border border-surfaceBorder bg-background p-4 space-y-2.5">
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-2">
                  <span className="text-xs font-medium text-textSecondary">Active Lib Version:</span>
                  <span className="rounded-full bg-blue-500/10 px-2.5 py-0.5 text-xs font-semibold text-blue-400 border border-blue-500/20">
                    v{libActiveVersion}
                  </span>
                </div>
                <span className="text-[11px] text-textMuted">
                  Updated: {new Date(libUpdatedAt).toLocaleDateString()}
                </span>
              </div>

              <div className="flex items-center justify-between rounded-xl bg-surface p-2.5 border border-surfaceBorder text-xs">
                <span className="truncate font-mono text-[11px] text-textSecondary max-w-[280px]">
                  {libDownloadUrl}
                </span>
                <div className="flex items-center space-x-1.5 ml-2">
                  <button
                    onClick={() => copyToClipboard(libDownloadUrl)}
                    className="p-1 text-textMuted hover:text-primary transition-colors"
                    title="Copy URL"
                  >
                    <Copy className="h-3.5 w-3.5" />
                  </button>
                  <a
                    href={libDownloadUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="p-1 text-textMuted hover:text-primary transition-colors"
                    title="Test Download Link"
                  >
                    <ExternalLink className="h-3.5 w-3.5" />
                  </a>
                </div>
              </div>
            </div>

            {/* Success Alert Banner */}
            {libSuccessMsg && (
              <div className="mt-3 flex items-center space-x-2 rounded-xl bg-emerald-500/10 border border-emerald-500/20 p-3 text-xs text-emerald-400">
                <CheckCircle2 className="h-4 w-4 shrink-0" />
                <span>{libSuccessMsg}</span>
              </div>
            )}

            {/* Mode Switcher */}
            <div className="mt-4 grid grid-cols-2 gap-1 rounded-xl bg-background p-1 border border-surfaceBorder">
              <button
                type="button"
                onClick={() => setLibUploadMode('upload')}
                className={`rounded-lg py-2 text-xs font-semibold transition-all ${
                  libUploadMode === 'upload'
                    ? 'bg-primary text-white shadow-xs'
                    : 'text-textSecondary hover:text-textPrimary'
                }`}
              >
                1. Upload ZIP File
              </button>
              <button
                type="button"
                onClick={() => setLibUploadMode('url')}
                className={`rounded-lg py-2 text-xs font-semibold transition-all ${
                  libUploadMode === 'url'
                    ? 'bg-primary text-white shadow-xs'
                    : 'text-textSecondary hover:text-textPrimary'
                }`}
              >
                2. Direct ZIP URL
              </button>
            </div>

            <form onSubmit={handlePushLibUpdate} className="mt-4 space-y-4">
              {libUploadMode === 'upload' ? (
                <>
                  <div>
                    <label className="block text-xs font-medium text-textSecondary">
                      Select Library ZIP File (.zip)
                    </label>
                    <input
                      type="file"
                      accept=".zip,application/zip"
                      onChange={(e) => {
                        if (e.target.files && e.target.files[0]) {
                          setLibFile(e.target.files[0]);
                        }
                      }}
                      className="mt-1.5 w-full rounded-xl border border-surfaceBorder bg-background p-2 text-xs text-textPrimary file:mr-3 file:rounded-lg file:border-0 file:bg-surfaceHover file:px-3 file:py-1 file:text-xs file:font-semibold file:text-textPrimary hover:file:bg-surfaceBorder"
                    />
                    <p className="mt-1 text-[11px] text-textMuted">
                      Must contain <code className="text-primary font-mono">libbgmi.so</code> inside.
                    </p>
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-textSecondary">
                      New Version Code (e.g. 2.0, 2.1)
                    </label>
                    <input
                      type="text"
                      placeholder="e.g. 2.0"
                      value={libNewVersion}
                      onChange={(e) => setLibNewVersion(e.target.value)}
                      className="mt-1.5 w-full rounded-xl border border-surfaceBorder bg-background px-3.5 py-2.5 font-mono text-xs sm:text-sm text-textPrimary placeholder:text-textMuted focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary/40 transition-colors"
                    />
                  </div>

                  <div className="rounded-xl bg-background p-3 border border-surfaceBorder text-[11px] text-textMuted">
                    💡 <span className="font-medium text-textSecondary">Smart Storage Cleanup:</span> When uploading a new zip, old archives in Supabase Storage are automatically pruned to keep your cloud storage lean and free.
                  </div>

                  <button
                    type="submit"
                    disabled={isUploadingLib || !libFile || !libNewVersion}
                    className="w-full rounded-xl bg-primary hover:bg-primaryHover py-3 text-xs sm:text-sm font-semibold text-white shadow-sm shadow-blue-500/20 transition-all disabled:opacity-40 disabled:cursor-not-allowed flex items-center justify-center space-x-2 active:scale-[0.98]"
                  >
                    {isUploadingLib ? (
                      <>
                        <RefreshCw className="h-4 w-4 animate-spin" />
                        <span>Uploading & Pushing Update...</span>
                      </>
                    ) : (
                      <>
                        <Upload className="h-4 w-4" />
                        <span>Upload ZIP & Push Release</span>
                      </>
                    )}
                  </button>
                </>
              ) : (
                <>
                  <div>
                    <label className="block text-xs font-medium text-textSecondary">
                      Direct Download URL for ZIP
                    </label>
                    <input
                      type="url"
                      placeholder="https://github.com/.../release/download/v2/lib.zip"
                      value={libDirectUrl}
                      onChange={(e) => setLibDirectUrl(e.target.value)}
                      className="mt-1.5 w-full rounded-xl border border-surfaceBorder bg-background px-3.5 py-2.5 font-mono text-xs sm:text-sm text-textPrimary placeholder:text-textMuted focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary/40 transition-colors"
                    />
                    <p className="mt-1 text-[11px] text-textMuted">
                      Direct link to the zip containing <code className="text-primary font-mono">libbgmi.so</code>.
                    </p>
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-textSecondary">
                      New Version Code (e.g. 2.0, 2.1)
                    </label>
                    <input
                      type="text"
                      placeholder="e.g. 2.0"
                      value={libNewVersion}
                      onChange={(e) => setLibNewVersion(e.target.value)}
                      className="mt-1.5 w-full rounded-xl border border-surfaceBorder bg-background px-3.5 py-2.5 font-mono text-xs sm:text-sm text-textPrimary placeholder:text-textMuted focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary/40 transition-colors"
                    />
                  </div>

                  <button
                    type="submit"
                    disabled={isUploadingLib || !libDirectUrl || !libNewVersion}
                    className="w-full rounded-xl bg-primary hover:bg-primaryHover py-3 text-xs sm:text-sm font-semibold text-white shadow-sm shadow-blue-500/20 transition-all disabled:opacity-40 disabled:cursor-not-allowed flex items-center justify-center space-x-2 active:scale-[0.98]"
                  >
                    {isUploadingLib ? (
                      <>
                        <RefreshCw className="h-4 w-4 animate-spin" />
                        <span>Pushing Update...</span>
                      </>
                    ) : (
                      <>
                        <Zap className="h-4 w-4" />
                        <span>Push Direct URL Update</span>
                      </>
                    )}
                  </button>
                </>
              )}
            </form>
          </div>
        </div>
      )}

      {/* SYSTEM CONTROL & ANNOUNCEMENTS MODAL */}
      {showSystemModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 p-4 backdrop-blur-md">
          <div className="w-full max-w-lg rounded-2xl border border-surfaceBorder bg-surface p-6 shadow-2xl animate-in fade-in zoom-in-95 duration-150 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b border-surfaceBorder pb-4">
              <div className="flex items-center space-x-2.5">
                <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-amber-500/10 border border-amber-500/20 text-amber-400">
                  <Wrench className="h-4 w-4" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-textPrimary">System & Broadcast Notices</h3>
                  <p className="text-[11px] text-textMuted">Control maintenance mode and push broadcast announcements</p>
                </div>
              </div>
              <button
                onClick={() => setShowSystemModal(false)}
                className="rounded-lg p-1 text-textMuted hover:text-textPrimary hover:bg-surfaceHover transition-colors"
              >
                ✕
              </button>
            </div>

            {/* Success notification */}
            {systemConfigSuccessMsg && (
              <div className="mt-3 flex items-center space-x-2 rounded-xl bg-emerald-500/10 border border-emerald-500/20 p-3 text-xs text-emerald-400">
                <CheckCircle2 className="h-4 w-4 shrink-0" />
                <span>{systemConfigSuccessMsg}</span>
              </div>
            )}

            {/* Tab switchers */}
            <div className="mt-4 grid grid-cols-2 gap-1 rounded-xl bg-background p-1 border border-surfaceBorder">
              <button
                type="button"
                onClick={() => {
                  setSystemTab('maintenance');
                  setSystemConfigSuccessMsg(null);
                }}
                className={`flex items-center justify-center space-x-2 rounded-lg py-2 text-xs font-semibold transition-all ${
                  systemTab === 'maintenance'
                    ? 'bg-rose-500 text-white shadow-xs'
                    : 'text-textSecondary hover:text-textPrimary'
                }`}
              >
                <Wrench className="h-3.5 w-3.5" />
                <span>Maintenance</span>
                {maintenanceMode && <span className="h-1.5 w-1.5 rounded-full bg-white animate-ping" />}
              </button>
              <button
                type="button"
                onClick={() => {
                  setSystemTab('announcement');
                  setSystemConfigSuccessMsg(null);
                }}
                className={`flex items-center justify-center space-x-2 rounded-lg py-2 text-xs font-semibold transition-all ${
                  systemTab === 'announcement'
                    ? 'bg-amber-500 text-white shadow-xs'
                    : 'text-textSecondary hover:text-textPrimary'
                }`}
              >
                <Megaphone className="h-3.5 w-3.5" />
                <span>Announcement</span>
                {announcementActive && <span className="h-1.5 w-1.5 rounded-full bg-white" />}
              </button>
            </div>

            <form onSubmit={handleSaveSystemConfig} className="mt-5 space-y-4">
              {systemTab === 'maintenance' ? (
                <>
                  {/* Maintenance Toggle */}
                  <div className="flex items-center justify-between rounded-xl border border-surfaceBorder bg-background p-4">
                    <div>
                      <div className="text-xs font-semibold text-textPrimary flex items-center space-x-2">
                        <span>Maintenance Status</span>
                        <span className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${
                          maintenanceMode ? 'bg-rose-500/20 text-rose-400 border border-rose-500/30' : 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                        }`}>
                          {maintenanceMode ? 'Active (App Locked)' : 'Disabled (Normal)'}
                        </span>
                      </div>
                      <p className="mt-1 text-[11px] text-textMuted">
                        When enabled, client applications display maintenance screen.
                      </p>
                    </div>

                    <button
                      type="button"
                      onClick={() => setMaintenanceMode(!maintenanceMode)}
                      className={`relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none ${
                        maintenanceMode ? 'bg-rose-500' : 'bg-surfaceBorder'
                      }`}
                    >
                      <span
                        className={`inline-block h-5 w-5 transform rounded-full bg-white shadow-md transition duration-200 ease-in-out ${
                          maintenanceMode ? 'translate-x-5' : 'translate-x-0'
                        }`}
                      />
                    </button>
                  </div>

                  {/* Maintenance Message */}
                  <div>
                    <label className="block text-xs font-medium text-textSecondary">
                      Maintenance Notice Message
                    </label>
                    <textarea
                      rows={3}
                      value={maintenanceMessage}
                      onChange={(e) => setMaintenanceMessage(e.target.value)}
                      placeholder="Server is undergoing scheduled maintenance. Please check back later."
                      className="mt-1.5 w-full rounded-xl border border-surfaceBorder bg-background px-3.5 py-2.5 text-xs text-textPrimary placeholder:text-textMuted focus:border-rose-500 focus:outline-none focus:ring-1 focus:ring-rose-500/40 leading-relaxed transition-colors"
                    />
                  </div>

                  {/* Estimated End Time */}
                  <div>
                    <label className="block text-xs font-medium text-textSecondary">
                      Estimated Completion Time
                    </label>
                    <input
                      type="text"
                      value={maintenanceEstimatedEnd}
                      onChange={(e) => setMaintenanceEstimatedEnd(e.target.value)}
                      placeholder="e.g. 1 Hour, 2:00 PM UTC, Tomorrow Morning"
                      className="mt-1.5 w-full rounded-xl border border-surfaceBorder bg-background px-3.5 py-2.5 font-mono text-xs text-textPrimary placeholder:text-textMuted focus:border-rose-500 focus:outline-none focus:ring-1 focus:ring-rose-500/40 transition-colors"
                    />
                    <p className="mt-1 text-[11px] text-textMuted">
                      Shown to users so they know when the server is expected back online.
                    </p>
                  </div>

                  <button
                    type="submit"
                    disabled={isSavingSystemConfig}
                    className="w-full rounded-xl bg-rose-500 hover:bg-rose-600 py-3 text-xs sm:text-sm font-semibold text-white shadow-sm shadow-rose-500/20 transition-all disabled:opacity-40 flex items-center justify-center space-x-2 active:scale-[0.98]"
                  >
                    {isSavingSystemConfig ? (
                      <>
                        <RefreshCw className="h-4 w-4 animate-spin" />
                        <span>Saving Settings...</span>
                      </>
                    ) : (
                      <>
                        <Wrench className="h-4 w-4" />
                        <span>Update Maintenance Status</span>
                      </>
                    )}
                  </button>
                </>
              ) : (
                <>
                  {/* Announcement Toggle */}
                  <div className="flex items-center justify-between rounded-xl border border-surfaceBorder bg-background p-4">
                    <div>
                      <div className="text-xs font-semibold text-textPrimary flex items-center space-x-2">
                        <span>Broadcast Status</span>
                        <span className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${
                          announcementActive ? 'bg-amber-500/20 text-amber-400 border border-amber-500/30' : 'bg-surfaceBorder text-textMuted'
                        }`}>
                          {announcementActive ? 'Broadcasting' : 'Disabled'}
                        </span>
                      </div>
                      <p className="mt-1 text-[11px] text-textMuted">
                        Displays an in-app notice dialog when users launch the app.
                      </p>
                    </div>

                    <button
                      type="button"
                      onClick={() => setAnnouncementActive(!announcementActive)}
                      className={`relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none ${
                        announcementActive ? 'bg-amber-500' : 'bg-surfaceBorder'
                      }`}
                    >
                      <span
                        className={`inline-block h-5 w-5 transform rounded-full bg-white shadow-md transition duration-200 ease-in-out ${
                          announcementActive ? 'translate-x-5' : 'translate-x-0'
                        }`}
                      />
                    </button>
                  </div>

                  {/* Announcement Title */}
                  <div>
                    <label className="block text-xs font-medium text-textSecondary">
                      Announcement Title
                    </label>
                    <input
                      type="text"
                      value={announcementTitle}
                      onChange={(e) => setAnnouncementTitle(e.target.value)}
                      placeholder="e.g. Major Update Released!"
                      className="mt-1.5 w-full rounded-xl border border-surfaceBorder bg-background px-3.5 py-2.5 text-xs text-textPrimary placeholder:text-textMuted focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500/40 font-semibold transition-colors"
                    />
                  </div>

                  {/* Announcement Message */}
                  <div>
                    <label className="block text-xs font-medium text-textSecondary">
                      Announcement Details
                    </label>
                    <textarea
                      rows={3}
                      value={announcementMessage}
                      onChange={(e) => setAnnouncementMessage(e.target.value)}
                      placeholder="Enter update notes or instructions..."
                      className="mt-1.5 w-full rounded-xl border border-surfaceBorder bg-background px-3.5 py-2.5 text-xs text-textPrimary placeholder:text-textMuted focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500/40 leading-relaxed transition-colors"
                    />
                  </div>

                  {/* Type / Priority */}
                  <div>
                    <label className="block text-xs font-medium text-textSecondary mb-1.5">
                      Notice Style
                    </label>
                    <div className="grid grid-cols-3 gap-2">
                      <button
                        type="button"
                        onClick={() => setAnnouncementType('info')}
                        className={`rounded-xl border p-2 text-xs font-semibold transition-all ${
                          announcementType === 'info'
                            ? 'border-blue-500 bg-blue-500 text-white shadow-xs'
                            : 'border-surfaceBorder bg-background text-textSecondary hover:text-textPrimary hover:bg-surfaceHover'
                        }`}
                      >
                        Info (Blue)
                      </button>
                      <button
                        type="button"
                        onClick={() => setAnnouncementType('warning')}
                        className={`rounded-xl border p-2 text-xs font-semibold transition-all ${
                          announcementType === 'warning'
                            ? 'border-amber-500 bg-amber-500 text-white shadow-xs'
                            : 'border-surfaceBorder bg-background text-textSecondary hover:text-textPrimary hover:bg-surfaceHover'
                        }`}
                      >
                        Warning (Amber)
                      </button>
                      <button
                        type="button"
                        onClick={() => setAnnouncementType('critical')}
                        className={`rounded-xl border p-2 text-xs font-semibold transition-all ${
                          announcementType === 'critical'
                            ? 'border-rose-500 bg-rose-500 text-white shadow-xs'
                            : 'border-surfaceBorder bg-background text-textSecondary hover:text-textPrimary hover:bg-surfaceHover'
                        }`}
                      >
                        Alert (Red)
                      </button>
                    </div>
                  </div>

                  {/* Optional Link */}
                  <div>
                    <label className="block text-xs font-medium text-textSecondary">
                      Optional Action Link (Telegram, Discord, Website)
                    </label>
                    <input
                      type="url"
                      value={announcementLink}
                      onChange={(e) => setAnnouncementLink(e.target.value)}
                      placeholder="https://t.me/your_official_channel"
                      className="mt-1.5 w-full rounded-xl border border-surfaceBorder bg-background px-3.5 py-2.5 font-mono text-xs text-textPrimary placeholder:text-textMuted focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500/40 transition-colors"
                    />
                  </div>

                  <button
                    type="submit"
                    disabled={isSavingSystemConfig}
                    className="w-full rounded-xl bg-amber-500 hover:bg-amber-600 py-3 text-xs sm:text-sm font-semibold text-white shadow-sm shadow-amber-500/20 transition-all disabled:opacity-40 flex items-center justify-center space-x-2 active:scale-[0.98]"
                  >
                    {isSavingSystemConfig ? (
                      <>
                        <RefreshCw className="h-4 w-4 animate-spin" />
                        <span>Saving Announcement...</span>
                      </>
                    ) : (
                      <>
                        <Megaphone className="h-4 w-4" />
                        <span>Push Announcement to App</span>
                      </>
                    )}
                  </button>
                </>
              )}
            </form>
          </div>
        </div>
      )}

      {/* APK IN-APP UPDATES MODAL */}
      {showApkModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 p-4 backdrop-blur-md">
          <div className="w-full max-w-lg rounded-2xl border border-surfaceBorder bg-surface p-6 shadow-2xl animate-in fade-in zoom-in-95 duration-150 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b border-surfaceBorder pb-4">
              <div className="flex items-center space-x-2.5">
                <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-primary/10 border border-primary/20 text-primary">
                  <Smartphone className="h-4 w-4" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-textPrimary">In-App APK Releases</h3>
                  <p className="text-[11px] text-textMuted">Publish APK updates with changelog and direct in-app downloader</p>
                </div>
              </div>
              <button
                onClick={() => setShowApkModal(false)}
                className="rounded-lg p-1 text-textMuted hover:text-textPrimary hover:bg-surfaceHover transition-colors"
              >
                ✕
              </button>
            </div>

            {/* Success notification */}
            {apkUpdateSuccessMsg && (
              <div className="mt-3 flex items-center space-x-2 rounded-xl bg-emerald-500/10 border border-emerald-500/20 p-3 text-xs text-emerald-400">
                <CheckCircle2 className="h-4 w-4 shrink-0" />
                <span>{apkUpdateSuccessMsg}</span>
              </div>
            )}

            {/* Current Active APK Info Card */}
            <div className="mt-4 rounded-xl border border-surfaceBorder bg-background p-4 space-y-2.5">
              <div className="flex items-center justify-between">
                <span className="text-xs font-medium text-textSecondary">Active Release</span>
                <span className="flex items-center space-x-1.5 rounded-full bg-blue-500/10 px-2.5 py-0.5 text-[10px] font-semibold text-blue-400 border border-blue-500/20">
                  <span className="h-1.5 w-1.5 rounded-full bg-blue-400 animate-pulse" />
                  <span>v{apkActiveVerName} (Code {apkActiveVerCode})</span>
                </span>
              </div>

              <div className="flex items-center justify-between rounded-xl bg-surface px-3 py-2 border border-surfaceBorder">
                <span className="font-mono text-[11px] text-textSecondary truncate max-w-[280px] sm:max-w-[340px]">
                  {apkActiveUrl || 'No URL configured'}
                </span>
                {apkActiveUrl && (
                  <button
                    onClick={() => copyToClipboard(apkActiveUrl)}
                    className="ml-2 text-textMuted hover:text-primary transition-colors p-1 rounded-md"
                    title="Copy APK URL"
                  >
                    {copiedKey === apkActiveUrl ? <Check className="h-3.5 w-3.5 text-emerald-400" /> : <Copy className="h-3.5 w-3.5" />}
                  </button>
                )}
              </div>

              {apkActiveChangelog && (
                <div className="text-[11px] text-textMuted">
                  <span className="font-medium text-textSecondary">Changelog: </span>
                  <span className="italic">{apkActiveChangelog}</span>
                </div>
              )}
            </div>

            {/* Form to Push New APK */}
            <form onSubmit={handleSaveApkUpdate} className="mt-5 space-y-4">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-medium text-textSecondary">
                    Version Name
                  </label>
                  <input
                    type="text"
                    required
                    value={apkNewVerName}
                    onChange={(e) => setApkNewVerName(e.target.value)}
                    placeholder="e.g. 2026.02.01"
                    className="mt-1.5 w-full rounded-xl border border-surfaceBorder bg-background px-3.5 py-2.5 font-mono text-xs sm:text-sm text-textPrimary placeholder:text-textMuted focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary/40 transition-colors"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-textSecondary">
                    Version Code (Number)
                  </label>
                  <input
                    type="number"
                    required
                    min={apkActiveVerCode + 1}
                    value={apkNewVerCode}
                    onChange={(e) => setApkNewVerCode(e.target.value)}
                    placeholder={`e.g. ${apkActiveVerCode + 1}`}
                    className="mt-1.5 w-full rounded-xl border border-surfaceBorder bg-background px-3.5 py-2.5 font-mono text-xs sm:text-sm text-textPrimary placeholder:text-textMuted focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary/40 transition-colors"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium text-textSecondary">
                  Direct APK Download URL
                </label>
                <input
                  type="url"
                  required
                  value={apkNewUrl}
                  onChange={(e) => setApkNewUrl(e.target.value)}
                  placeholder="https://.../Anoy_Loader_v2.apk"
                  className="mt-1.5 w-full rounded-xl border border-surfaceBorder bg-background px-3.5 py-2.5 font-mono text-xs sm:text-sm text-textPrimary placeholder:text-textMuted focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary/40 transition-colors"
                />
                <p className="mt-1 text-[11px] text-textMuted">
                  The app will download this .apk directly in-app with a live progress bar and launch package installer.
                </p>
              </div>

              <div>
                <label className="block text-xs font-medium text-textSecondary">
                  What's New / Changelog
                </label>
                <textarea
                  rows={3}
                  value={apkNewChangelog}
                  onChange={(e) => setApkNewChangelog(e.target.value)}
                  placeholder="• Performance optimizations&#10;• New VIP features&#10;• Faster connection"
                  className="mt-1.5 w-full rounded-xl border border-surfaceBorder bg-background px-3.5 py-2.5 text-xs text-textPrimary placeholder:text-textMuted focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary/40 leading-relaxed transition-colors"
                />
              </div>

              <div className="flex items-center space-x-2.5 rounded-xl border border-surfaceBorder bg-background p-3.5">
                <input
                  type="checkbox"
                  id="apkMandatory"
                  checked={apkNewMandatory}
                  onChange={(e) => setApkNewMandatory(e.target.checked)}
                  className="h-4 w-4 rounded border-surfaceBorder text-primary focus:ring-primary/30 bg-surface"
                />
                <label htmlFor="apkMandatory" className="text-xs text-textSecondary cursor-pointer select-none">
                  <strong className="text-textPrimary">Mandatory Update</strong> (Users must update before they can use the app)
                </label>
              </div>

              <button
                type="submit"
                disabled={isSavingApkUpdate}
                className="w-full rounded-xl bg-primary hover:bg-primaryHover py-3 text-xs sm:text-sm font-semibold text-white shadow-sm shadow-blue-500/20 transition-all disabled:opacity-40 flex items-center justify-center space-x-2 active:scale-[0.98]"
              >
                {isSavingApkUpdate ? (
                  <>
                    <RefreshCw className="h-4 w-4 animate-spin" />
                    <span>Publishing APK Release...</span>
                  </>
                ) : (
                  <>
                    <Smartphone className="h-4 w-4" />
                    <span>Push APK Update to App</span>
                  </>
                )}
              </button>
            </form>
          </div>
        </div>
      )}

      {/* CONFIG MODAL */}
      {showConfigModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4 backdrop-blur-sm">
          <div className="w-full max-w-md rounded-2xl border border-surfaceBorder bg-surface p-6 shadow-2xl">
            <div className="flex items-center justify-between border-b border-surfaceBorder pb-4">
              <div className="flex items-center space-x-2">
                <Database className="h-5 w-5 text-primary" />
                <h3 className="text-base font-bold text-textPrimary">DATABASE CONFIGURATION</h3>
              </div>
              <button
                onClick={() => setShowConfigModal(false)}
                className="text-textMuted hover:text-textPrimary"
              >
                ✕
              </button>
            </div>

            <div className="mt-4 space-y-3 text-xs text-textSecondary">
              <p>
                To connect this Admin Panel with your live <strong>Supabase</strong> instance:
              </p>
              <ol className="list-decimal pl-4 space-y-1 text-textMuted">
                <li>Create a free project at supabase.com</li>
                <li>Run the provided <code className="text-primary">supabase/schema.sql</code> in your Supabase SQL Editor</li>
                <li>Add your Project URL and Anon Key to <code className="text-primary">admin-panel/.env.local</code></li>
                <li>Deploy to Vercel with the same environment variables!</li>
              </ol>

              <div className="rounded-xl bg-background p-3 border border-surfaceBorder font-mono text-[11px] text-textMuted space-y-1">
                <div>NEXT_PUBLIC_SUPABASE_URL=...</div>
                <div>NEXT_PUBLIC_SUPABASE_ANON_KEY=...</div>
              </div>

              <div className="pt-2">
                <button
                  onClick={() => setShowConfigModal(false)}
                  className="w-full rounded-xl bg-primary hover:bg-primaryHover py-2.5 text-xs font-semibold text-white shadow-xs transition-colors"
                >
                  Close
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
