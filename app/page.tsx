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
    key: 'VIPER-PRO-7D-9941',
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
    key: 'VIPER-TRIAL-1H-8820',
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
    key: 'VIPER-LIFE-9921',
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
  const generateRandomKey = (prefix = 'VIPER') => {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    const segment = (len: number) =>
      Array.from({ length: len }, () => chars[Math.floor(Math.random() * chars.length)]).join('');
    return `${prefix}-${segment(4)}-${segment(4)}-${segment(4)}`;
  };

  // Handle Create Keys (Single & Bulk)
  const handleCreateKeys = async (e: React.FormEvent) => {
    e.preventDefault();
    const newKeyEntries: Partial<LicenseKey>[] = [];

    if (createMode === 'single') {
      const finalKey = customKeyName.trim() ? customKeyName.trim().toUpperCase() : generateRandomKey();
      newKeyEntries.push({
        key: finalKey,
        duration_label: selectedDuration.label,
        duration_seconds: selectedDuration.seconds,
        max_devices: maxDevices,
        hwid_list: [],
        status: 'UNUSED',
        notes: keyNotes.trim() || null,
        created_at: new Date().toISOString(),
        activated_at: null,
        expires_at: null,
      });
    } else {
      for (let i = 0; i < bulkCount; i++) {
        newKeyEntries.push({
          key: generateRandomKey(),
          duration_label: selectedDuration.label,
          duration_seconds: selectedDuration.seconds,
          max_devices: maxDevices,
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
    element.download = `viper_keys_${new Date().toISOString().slice(0, 10)}.txt`;
    document.body.appendChild(element);
    element.click();
    document.body.removeChild(element);
  };

  return (
    <div className="min-h-screen bg-background text-textPrimary pb-12">
      {/* Top Cyber Navigation Bar */}
      <header className="sticky top-0 z-30 border-b border-surfaceBorder bg-surface/90 backdrop-blur-md">
        <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-3 sm:px-6">
          <div className="flex items-center space-x-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-tr from-primary to-accent p-0.5 shadow-lg shadow-primary/20">
              <div className="flex h-full w-full items-center justify-center rounded-[10px] bg-surface">
                <Shield className="h-5 w-5 text-primary" />
              </div>
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <h1 className="text-lg font-black tracking-wider text-textPrimary sm:text-xl">
                  VIPER<span className="text-primary">PANEL</span>
                </h1>
                <span className="rounded-full bg-primary/10 px-2 py-0.5 text-[10px] font-bold tracking-widest text-primary border border-primary/20">
                  ADMIN
                </span>
              </div>
              <p className="text-[11px] text-textMuted sm:text-xs">License Key & Device Management</p>
            </div>
          </div>

          <div className="flex items-center space-x-2 sm:space-x-3">
            {/* Database indicator */}
            <button
              onClick={() => setShowConfigModal(true)}
              className={`flex items-center space-x-1.5 rounded-lg px-2.5 py-1.5 text-xs font-semibold border transition-all ${
                isLiveDatabase
                  ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30'
                  : 'bg-amber-500/10 text-amber-400 border-amber-500/30 animate-pulse'
              }`}
            >
              <Database className="h-3.5 w-3.5" />
              <span className="hidden sm:inline">{isLiveDatabase ? 'Supabase Connected' : 'Demo Mode (Click to setup)'}</span>
              <span className="sm:hidden">{isLiveDatabase ? 'Live' : 'Setup'}</span>
            </button>

            {/* Refresh */}
            <button
              onClick={fetchKeys}
              className="rounded-lg border border-surfaceBorder bg-surface p-2 text-textSecondary hover:bg-surfaceHover hover:text-textPrimary"
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
              className="flex items-center space-x-1.5 rounded-lg border border-secondary/40 bg-secondary/10 px-3 py-2 text-xs font-bold text-secondary shadow-lg shadow-secondary/10 transition-all hover:bg-secondary/20 hover:scale-[1.02] active:scale-[0.98] sm:px-4 sm:text-sm"
            >
              <Upload className="h-4 w-4" />
              <span className="hidden md:inline">MANAGE LIB UPDATES</span>
              <span className="md:hidden">LIBS</span>
            </button>

            {/* APK In-App Updates Button */}
            <button
              onClick={() => {
                setShowApkModal(true);
                setApkUpdateSuccessMsg(null);
              }}
              className="flex items-center space-x-1.5 rounded-lg border border-cyan-500/40 bg-cyan-500/10 px-3 py-2 text-xs font-bold text-cyan-400 shadow-lg shadow-cyan-500/10 transition-all hover:bg-cyan-500/20 hover:scale-[1.02] active:scale-[0.98] sm:px-4 sm:text-sm"
            >
              <Smartphone className="h-4 w-4" />
              <span className="hidden md:inline">APK UPDATES</span>
              <span className="md:hidden">APK</span>
            </button>

            {/* System Control Button */}
            <button
              onClick={() => {
                setShowSystemModal(true);
                setSystemConfigSuccessMsg(null);
              }}
              className={`flex items-center space-x-1.5 rounded-lg border px-3 py-2 text-xs font-bold transition-all hover:scale-[1.02] active:scale-[0.98] sm:px-4 sm:text-sm ${
                maintenanceMode
                  ? 'bg-rose-500/10 text-rose-400 border-rose-500/40 shadow-lg shadow-rose-500/20 animate-pulse'
                  : 'border-surfaceBorder bg-surface text-textSecondary hover:bg-surfaceHover hover:text-textPrimary'
              }`}
            >
              {maintenanceMode ? <Wrench className="h-4 w-4 text-rose-400" /> : <Megaphone className="h-4 w-4 text-amber-400" />}
              <span className="hidden md:inline">{maintenanceMode ? 'MAINTENANCE (ACTIVE)' : 'SYSTEM & NOTICE'}</span>
              <span className="md:hidden">{maintenanceMode ? 'MAINT' : 'SYS'}</span>
            </button>

            {/* Create Key Button */}
            <button
              onClick={() => setShowCreateModal(true)}
              className="flex items-center space-x-2 rounded-lg bg-primary px-3 py-2 text-xs font-bold text-black shadow-lg shadow-primary/20 transition-all hover:bg-primaryHover hover:scale-[1.02] active:scale-[0.98] sm:px-4 sm:text-sm"
            >
              <Plus className="h-4 w-4 stroke-[3]" />
              <span>CREATE KEYS</span>
            </button>
          </div>
        </div>
      </header>

      {/* Main Content Area */}
      <main className="mx-auto max-w-7xl px-4 pt-6 sm:px-6">
        {/* Maintenance Alert Banner */}
        {maintenanceMode && (
          <div className="mb-4 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 rounded-xl border border-rose-500/40 bg-rose-500/10 p-3.5 text-xs text-rose-300 backdrop-blur-md">
            <div className="flex items-center space-x-2.5">
              <Wrench className="h-5 w-5 shrink-0 text-rose-400 animate-bounce" />
              <div>
                <span className="font-extrabold uppercase tracking-wide text-rose-400">MAINTENANCE MODE ACTIVE:</span>{' '}
                <span>{maintenanceMessage}</span>
                <span className="ml-2 font-mono text-[11px] text-rose-400/80">(Estimated completion: {maintenanceEstimatedEnd})</span>
              </div>
            </div>
            <button
              onClick={() => {
                setShowSystemModal(true);
                setSystemTab('maintenance');
              }}
              className="rounded-lg bg-rose-500/20 px-3 py-1.5 text-xs font-bold text-rose-300 hover:bg-rose-500/30 border border-rose-500/40 shrink-0"
            >
              CONFIGURE
            </button>
          </div>
        )}
        {/* KPI Stat Cards Grid */}
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-5 sm:gap-4">
          <div className="rounded-xl border border-surfaceBorder bg-surface p-4 shadow-sm">
            <div className="flex items-center justify-between text-textMuted">
              <span className="text-xs font-bold uppercase tracking-wider">Total Keys</span>
              <Key className="h-4 w-4 text-primary" />
            </div>
            <div className="mt-2 text-2xl font-extrabold text-textPrimary">{stats.total}</div>
          </div>

          <div className="rounded-xl border border-surfaceBorder bg-surface p-4 shadow-sm">
            <div className="flex items-center justify-between text-textMuted">
              <span className="text-xs font-bold uppercase tracking-wider">Active</span>
              <Zap className="h-4 w-4 text-emerald-400" />
            </div>
            <div className="mt-2 text-2xl font-extrabold text-emerald-400">{stats.active}</div>
          </div>

          <div className="rounded-xl border border-surfaceBorder bg-surface p-4 shadow-sm">
            <div className="flex items-center justify-between text-textMuted">
              <span className="text-xs font-bold uppercase tracking-wider">Unused</span>
              <Clock className="h-4 w-4 text-secondary" />
            </div>
            <div className="mt-2 text-2xl font-extrabold text-secondary">{stats.unused}</div>
          </div>

          <div className="rounded-xl border border-surfaceBorder bg-surface p-4 shadow-sm">
            <div className="flex items-center justify-between text-textMuted">
              <span className="text-xs font-bold uppercase tracking-wider">Expired</span>
              <AlertCircle className="h-4 w-4 text-amber-400" />
            </div>
            <div className="mt-2 text-2xl font-extrabold text-amber-400">{stats.expired}</div>
          </div>

          <div className="col-span-2 rounded-xl border border-surfaceBorder bg-surface p-4 shadow-sm sm:col-span-1">
            <div className="flex items-center justify-between text-textMuted">
              <span className="text-xs font-bold uppercase tracking-wider">Banned</span>
              <Ban className="h-4 w-4 text-rose-400" />
            </div>
            <div className="mt-2 text-2xl font-extrabold text-rose-400">{stats.banned}</div>
          </div>
        </div>

        {/* Filter and Action Bar */}
        <div className="mt-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          {/* Search bar */}
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-textMuted" />
            <input
              type="text"
              placeholder="Search by key, HWID, or notes..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full rounded-xl border border-surfaceBorder bg-surface py-2.5 pl-9 pr-4 text-sm text-textPrimary placeholder:text-textMuted focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
            />
          </div>

          {/* Status Filter buttons */}
          <div className="flex items-center space-x-1.5 overflow-x-auto pb-1 sm:pb-0">
            {(['ALL', 'ACTIVE', 'UNUSED', 'EXPIRED', 'BANNED'] as const).map((st) => (
              <button
                key={st}
                onClick={() => setStatusFilter(st)}
                className={`rounded-lg px-3 py-2 text-xs font-bold transition-all ${
                  statusFilter === st
                    ? 'bg-surfaceBorder text-primary border border-primary/40'
                    : 'bg-surface text-textMuted hover:bg-surfaceHover hover:text-textSecondary'
                }`}
              >
                {st}
              </button>
            ))}

            {/* Clean expired button */}
            <button
              onClick={handleDeleteExpired}
              className="flex items-center space-x-1.5 rounded-lg border border-amber-500/30 bg-amber-500/10 px-3 py-2 text-xs font-bold text-amber-400 hover:bg-amber-500/20"
              title="Delete all expired keys"
            >
              <Trash2 className="h-3.5 w-3.5" />
              <span className="hidden md:inline">Clean Expired</span>
            </button>
          </div>
        </div>

        {/* Keys Table / Mobile Cards */}
        <div className="mt-4 rounded-xl border border-surfaceBorder bg-surface shadow-md overflow-hidden">
          {filteredKeys.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-16 text-center text-textMuted">
              <Key className="h-10 w-10 opacity-30 mb-2" />
              <p className="text-base font-semibold">No license keys found</p>
              <p className="text-xs mt-1">Create a new key or adjust your filter.</p>
            </div>
          ) : (
            <>
              {/* Desktop Table View */}
              <div className="hidden lg:block overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className="border-b border-surfaceBorder bg-surfaceHover/50 text-textMuted uppercase tracking-wider font-semibold">
                    <tr>
                      <th className="py-3.5 pl-4 pr-2">License Key</th>
                      <th className="px-3 py-3.5">Status</th>
                      <th className="px-3 py-3.5">Duration</th>
                      <th className="px-3 py-3.5">Devices / HWID</th>
                      <th className="px-3 py-3.5">Expires At</th>
                      <th className="px-3 py-3.5">Notes</th>
                      <th className="py-3.5 pl-2 pr-4 text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-surfaceBorder/60">
                    {filteredKeys.map((item) => (
                      <tr key={item.id} className="hover:bg-surfaceHover/40 transition-colors">
                        <td className="py-3 pl-4 pr-2 font-mono font-bold text-textPrimary">
                          <div className="flex items-center space-x-2">
                            <span>{item.key}</span>
                            <button
                              onClick={() => copyToClipboard(item.key)}
                              className="text-textMuted hover:text-primary transition-colors"
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

                        <td className="px-3 py-3">
                          <span
                            className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-[10px] font-extrabold tracking-wider uppercase border ${
                              item.status === 'ACTIVE'
                                ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30'
                                : item.status === 'UNUSED'
                                ? 'bg-secondary/10 text-secondary border-secondary/30'
                                : item.status === 'EXPIRED'
                                ? 'bg-amber-500/10 text-amber-400 border-amber-500/30'
                                : 'bg-rose-500/10 text-rose-400 border-rose-500/30'
                            }`}
                          >
                            {item.status}
                          </span>
                        </td>

                        <td className="px-3 py-3 font-medium text-textSecondary">{item.duration_label}</td>

                        <td className="px-3 py-3">
                          <div className="flex items-center space-x-2">
                            <span className="font-semibold text-textPrimary">
                              {item.hwid_list.length} / {item.max_devices}
                            </span>
                            {item.hwid_list.length > 0 && (
                              <button
                                onClick={() => handleResetHwid(item)}
                                className="rounded px-1.5 py-0.5 text-[10px] font-bold text-amber-400 hover:bg-amber-400/10 border border-amber-400/30"
                                title="Reset HWID"
                              >
                                Reset
                              </button>
                            )}
                          </div>
                        </td>

                        <td className="px-3 py-3 text-textMuted">
                          {item.duration_seconds <= 0 ? (
                            <span className="font-bold text-accent">LIFETIME</span>
                          ) : item.status === 'UNUSED' ? (
                            <span className="italic text-textMuted">Starts on first login</span>
                          ) : item.expires_at ? (
                            new Date(item.expires_at).toLocaleString()
                          ) : (
                            '—'
                          )}
                        </td>

                        <td className="px-3 py-3 text-textMuted max-w-[150px] truncate">{item.notes || '—'}</td>

                        <td className="py-3 pl-2 pr-4 text-right">
                          <div className="flex items-center justify-end space-x-1.5">
                            <button
                              onClick={() => setShowExtendModal(item)}
                              className="rounded-lg p-1.5 text-textMuted hover:bg-surfaceBorder hover:text-secondary"
                              title="Extend Time"
                            >
                              <Clock className="h-4 w-4" />
                            </button>

                            <button
                              onClick={() => handleToggleBan(item)}
                              className={`rounded-lg p-1.5 hover:bg-surfaceBorder ${
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
                              className="rounded-lg p-1.5 text-textMuted hover:bg-surfaceBorder hover:text-rose-400"
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
                        <span className="font-mono font-bold text-sm text-textPrimary">{item.key}</span>
                        <button
                          onClick={() => copyToClipboard(item.key)}
                          className="text-textMuted hover:text-primary"
                        >
                          {copiedKey === item.key ? (
                            <Check className="h-4 w-4 text-emerald-400" />
                          ) : (
                            <Copy className="h-4 w-4" />
                          )}
                        </button>
                      </div>

                      <span
                        className={`inline-flex items-center rounded-full px-2 py-0.5 text-[9px] font-extrabold uppercase border ${
                          item.status === 'ACTIVE'
                            ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30'
                            : item.status === 'UNUSED'
                            ? 'bg-secondary/10 text-secondary border-secondary/30'
                            : item.status === 'EXPIRED'
                            ? 'bg-amber-500/10 text-amber-400 border-amber-500/30'
                            : 'bg-rose-500/10 text-rose-400 border-rose-500/30'
                        }`}
                      >
                        {item.status}
                      </span>
                    </div>

                    <div className="grid grid-cols-2 gap-2 text-xs text-textMuted">
                      <div>
                        <span className="block text-[10px] uppercase tracking-wider font-bold">Duration</span>
                        <span className="font-medium text-textSecondary">{item.duration_label}</span>
                      </div>

                      <div>
                        <span className="block text-[10px] uppercase tracking-wider font-bold">Devices</span>
                        <span className="font-medium text-textSecondary">
                          {item.hwid_list.length} / {item.max_devices} bound
                        </span>
                      </div>

                      <div className="col-span-2">
                        <span className="block text-[10px] uppercase tracking-wider font-bold">Expires</span>
                        <span className="font-medium text-textSecondary">
                          {item.duration_seconds <= 0 ? (
                            <span className="text-accent font-bold">LIFETIME</span>
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
                    <div className="flex items-center justify-end space-x-2 pt-2 border-t border-surfaceBorder/40">
                      {item.hwid_list.length > 0 && (
                        <button
                          onClick={() => handleResetHwid(item)}
                          className="rounded-lg border border-amber-400/30 bg-amber-400/10 px-2.5 py-1.5 text-xs font-bold text-amber-400"
                        >
                          Reset HWID
                        </button>
                      )}

                      <button
                        onClick={() => setShowExtendModal(item)}
                        className="rounded-lg border border-surfaceBorder bg-surfaceHover px-2.5 py-1.5 text-xs font-bold text-secondary"
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
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4 backdrop-blur-sm">
          <div className="w-full max-w-lg rounded-2xl border border-surfaceBorder bg-surface p-6 shadow-2xl animate-in fade-in zoom-in-95 duration-150">
            <div className="flex items-center justify-between border-b border-surfaceBorder pb-4">
              <div className="flex items-center space-x-2">
                <Plus className="h-5 w-5 text-primary" />
                <h3 className="text-lg font-black text-textPrimary">GENERATE LICENSE KEYS</h3>
              </div>
              <button
                onClick={() => setShowCreateModal(false)}
                className="text-textMuted hover:text-textPrimary"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleCreateKeys} className="mt-4 space-y-4">
              {/* Single vs Bulk Tab */}
              <div className="grid grid-cols-2 gap-2 rounded-xl bg-background p-1 border border-surfaceBorder">
                <button
                  type="button"
                  onClick={() => setCreateMode('single')}
                  className={`rounded-lg py-2 text-xs font-bold uppercase transition-all ${
                    createMode === 'single'
                      ? 'bg-primary text-black shadow-md'
                      : 'text-textMuted hover:text-textSecondary'
                  }`}
                >
                  Single Key
                </button>
                <button
                  type="button"
                  onClick={() => setCreateMode('bulk')}
                  className={`rounded-lg py-2 text-xs font-bold uppercase transition-all ${
                    createMode === 'bulk'
                      ? 'bg-primary text-black shadow-md'
                      : 'text-textMuted hover:text-textSecondary'
                  }`}
                >
                  Bulk Generator
                </button>
              </div>

              {/* Single Mode Custom Name */}
              {createMode === 'single' ? (
                <div>
                  <label className="block text-xs font-bold uppercase tracking-wider text-textMuted">
                    Custom Key Name <span className="font-normal text-[11px]">(Optional, auto-generated if empty)</span>
                  </label>
                  <input
                    type="text"
                    placeholder="e.g. VIPER-PRO-PLAYER-01"
                    value={customKeyName}
                    onChange={(e) => setCustomKeyName(e.target.value)}
                    className="mt-1.5 w-full rounded-xl border border-surfaceBorder bg-background px-3.5 py-2.5 font-mono text-sm text-textPrimary uppercase placeholder:text-textMuted focus:border-primary focus:outline-none"
                  />
                </div>
              ) : (
                <div>
                  <label className="block text-xs font-bold uppercase tracking-wider text-textMuted">
                    Number of Keys to Generate
                  </label>
                  <div className="mt-1.5 grid grid-cols-5 gap-2">
                    {[5, 10, 25, 50, 100].map((count) => (
                      <button
                        key={count}
                        type="button"
                        onClick={() => setBulkCount(count)}
                        className={`rounded-lg py-2 text-xs font-bold border transition-all ${
                          bulkCount === count
                            ? 'bg-primary/10 border-primary text-primary'
                            : 'border-surfaceBorder bg-background text-textMuted hover:text-textSecondary'
                        }`}
                      >
                        {count}
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {/* Duration Selector */}
              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-textMuted mb-2">
                  Select Key Duration
                </label>
                <div className="grid grid-cols-3 sm:grid-cols-4 gap-2">
                  {DURATION_OPTIONS.map((opt) => (
                    <button
                      key={opt.label}
                      type="button"
                      onClick={() => setSelectedDuration(opt)}
                      className={`rounded-lg p-2 text-center border text-xs font-bold transition-all ${
                        selectedDuration.label === opt.label
                          ? 'border-primary bg-primary/10 text-primary shadow-sm'
                          : 'border-surfaceBorder bg-background text-textSecondary hover:bg-surfaceHover'
                      }`}
                    >
                      {opt.label}
                    </button>
                  ))}
                </div>
              </div>

              {/* Device Limit */}
              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-textMuted">
                  Device Limit (Concurrent / Bound Devices)
                </label>
                <div className="mt-1.5 flex items-center space-x-2">
                  {[1, 2, 3, 5, 10].map((dev) => (
                    <button
                      key={dev}
                      type="button"
                      onClick={() => setMaxDevices(dev)}
                      className={`flex-1 rounded-lg py-2 text-xs font-bold border ${
                        maxDevices === dev
                          ? 'border-primary bg-primary/10 text-primary'
                          : 'border-surfaceBorder bg-background text-textMuted'
                      }`}
                    >
                      {dev} {dev === 1 ? 'Device' : 'Devices'}
                    </button>
                  ))}
                </div>
              </div>

              {/* Notes */}
              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-textMuted">
                  Admin Notes / Customer Tag (Optional)
                </label>
                <input
                  type="text"
                  placeholder="e.g. Reseller Alex / Order #104"
                  value={keyNotes}
                  onChange={(e) => setKeyNotes(e.target.value)}
                  className="mt-1.5 w-full rounded-xl border border-surfaceBorder bg-background px-3.5 py-2 text-xs text-textPrimary placeholder:text-textMuted focus:border-primary focus:outline-none"
                />
              </div>

              <div className="pt-2">
                <button
                  type="submit"
                  className="w-full rounded-xl bg-primary py-3 text-sm font-bold text-black shadow-lg shadow-primary/20 hover:bg-primaryHover transition-all active:scale-[0.98]"
                >
                  {createMode === 'single' ? 'GENERATE SINGLE KEY' : `GENERATE ${bulkCount} BULK KEYS`}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* EXTEND TIME MODAL */}
      {showExtendModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4 backdrop-blur-sm">
          <div className="w-full max-w-md rounded-2xl border border-surfaceBorder bg-surface p-6 shadow-2xl">
            <div className="flex items-center justify-between border-b border-surfaceBorder pb-4">
              <div className="flex items-center space-x-2">
                <Clock className="h-5 w-5 text-secondary" />
                <h3 className="text-base font-bold text-textPrimary">EXTEND KEY DURATION</h3>
              </div>
              <button
                onClick={() => setShowExtendModal(null)}
                className="text-textMuted hover:text-textPrimary"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleExtendKey} className="mt-4 space-y-4">
              <div className="rounded-xl bg-background p-3 border border-surfaceBorder">
                <span className="text-[11px] uppercase font-bold text-textMuted">Target Key:</span>
                <p className="font-mono font-bold text-sm text-textPrimary">{showExtendModal.key}</p>
                <p className="text-xs text-secondary mt-1">
                  Current: {showExtendModal.duration_label}{' '}
                  {showExtendModal.expires_at ? `(Expires: ${new Date(showExtendModal.expires_at).toLocaleDateString()})` : ''}
                </p>
              </div>

              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-textMuted mb-2">
                  Select Time to Add:
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
                      className={`rounded-lg py-2 text-xs font-bold border ${
                        extendingSeconds === ext.sec
                          ? 'border-secondary bg-secondary/10 text-secondary'
                          : 'border-surfaceBorder bg-background text-textMuted hover:text-textSecondary'
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
                  className="w-full rounded-xl bg-secondary py-2.5 text-sm font-bold text-black hover:bg-secondary/90 transition-all"
                >
                  ADD TIME TO KEY
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* BULK SUCCESS / COPY MODAL */}
      {showBulkSuccessModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4 backdrop-blur-sm">
          <div className="w-full max-w-lg rounded-2xl border border-surfaceBorder bg-surface p-6 shadow-2xl">
            <div className="flex items-center justify-between border-b border-surfaceBorder pb-4">
              <div className="flex items-center space-x-2">
                <Sparkles className="h-5 w-5 text-primary" />
                <h3 className="text-base font-bold text-textPrimary">
                  {showBulkSuccessModal.length} Keys Generated Successfully!
                </h3>
              </div>
              <button
                onClick={() => setShowBulkSuccessModal(null)}
                className="text-textMuted hover:text-textPrimary"
              >
                ✕
              </button>
            </div>

            <div className="mt-4">
              <textarea
                readOnly
                rows={8}
                value={showBulkSuccessModal.join('\n')}
                className="w-full rounded-xl border border-surfaceBorder bg-background p-3 font-mono text-xs text-textPrimary focus:outline-none"
              />
            </div>

            <div className="mt-4 flex items-center space-x-3">
              <button
                onClick={() => {
                  copyToClipboard(showBulkSuccessModal.join('\n'));
                  alert('All keys copied to clipboard!');
                }}
                className="flex-1 flex items-center justify-center space-x-2 rounded-xl bg-primary py-2.5 text-xs font-bold text-black hover:bg-primaryHover"
              >
                <Copy className="h-4 w-4" />
                <span>COPY ALL KEYS</span>
              </button>

              <button
                onClick={() => downloadBulkTxt(showBulkSuccessModal)}
                className="flex items-center space-x-2 rounded-xl border border-surfaceBorder bg-surfaceHover px-4 py-2.5 text-xs font-bold text-textPrimary hover:bg-surfaceBorder"
              >
                <Download className="h-4 w-4" />
                <span>DOWNLOAD .TXT</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* MANAGE LIB UPDATES MODAL */}
      {showLibModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4 backdrop-blur-sm">
          <div className="w-full max-w-lg rounded-2xl border border-surfaceBorder bg-surface p-6 shadow-2xl animate-in fade-in zoom-in-95 duration-150 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b border-surfaceBorder pb-4">
              <div className="flex items-center space-x-2">
                <Upload className="h-5 w-5 text-secondary" />
                <h3 className="text-lg font-black text-textPrimary">MANAGE LIB UPDATES</h3>
              </div>
              <button
                onClick={() => setShowLibModal(false)}
                className="text-textMuted hover:text-textPrimary"
              >
                ✕
              </button>
            </div>

            {/* Current Active Lib Banner */}
            <div className="mt-4 rounded-xl border border-secondary/30 bg-secondary/5 p-4 space-y-2">
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-2">
                  <span className="text-xs font-bold uppercase tracking-wider text-textMuted">Active Lib Version:</span>
                  <span className="rounded-full bg-secondary/20 px-2.5 py-0.5 text-xs font-extrabold text-secondary border border-secondary/40">
                    v{libActiveVersion}
                  </span>
                </div>
                <span className="text-[10px] text-textMuted">
                  Updated: {new Date(libUpdatedAt).toLocaleDateString()}
                </span>
              </div>

              <div className="flex items-center justify-between rounded-lg bg-background/80 p-2 border border-surfaceBorder text-xs">
                <span className="truncate font-mono text-[11px] text-textSecondary max-w-[280px]">
                  {libDownloadUrl}
                </span>
                <div className="flex items-center space-x-1.5 ml-2">
                  <button
                    onClick={() => copyToClipboard(libDownloadUrl)}
                    className="p-1 text-textMuted hover:text-secondary transition-colors"
                    title="Copy URL"
                  >
                    <Copy className="h-3.5 w-3.5" />
                  </button>
                  <a
                    href={libDownloadUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="p-1 text-textMuted hover:text-secondary transition-colors"
                    title="Test Download Link"
                  >
                    <ExternalLink className="h-3.5 w-3.5" />
                  </a>
                </div>
              </div>
            </div>

            {/* Success Alert Banner */}
            {libSuccessMsg && (
              <div className="mt-3 flex items-center space-x-2 rounded-xl bg-emerald-500/10 border border-emerald-500/30 p-3 text-xs text-emerald-400">
                <CheckCircle2 className="h-4 w-4 shrink-0" />
                <span>{libSuccessMsg}</span>
              </div>
            )}

            {/* Mode Switcher */}
            <div className="mt-4 grid grid-cols-2 gap-2 rounded-xl bg-background p-1 border border-surfaceBorder">
              <button
                type="button"
                onClick={() => setLibUploadMode('upload')}
                className={`rounded-lg py-2 text-xs font-bold uppercase transition-all ${
                  libUploadMode === 'upload'
                    ? 'bg-secondary text-black shadow-md'
                    : 'text-textMuted hover:text-textSecondary'
                }`}
              >
                1. Upload ZIP File
              </button>
              <button
                type="button"
                onClick={() => setLibUploadMode('url')}
                className={`rounded-lg py-2 text-xs font-bold uppercase transition-all ${
                  libUploadMode === 'url'
                    ? 'bg-secondary text-black shadow-md'
                    : 'text-textMuted hover:text-textSecondary'
                }`}
              >
                2. Direct ZIP URL
              </button>
            </div>

            <form onSubmit={handlePushLibUpdate} className="mt-4 space-y-4">
              {libUploadMode === 'upload' ? (
                <>
                  <div>
                    <label className="block text-xs font-bold uppercase tracking-wider text-textMuted">
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
                      className="mt-1.5 w-full rounded-xl border border-surfaceBorder bg-background p-2.5 text-xs text-textPrimary file:mr-3 file:rounded-lg file:border-0 file:bg-surfaceHover file:px-3 file:py-1 file:text-xs file:font-bold file:text-secondary hover:file:bg-surfaceBorder"
                    />
                    <p className="mt-1 text-[11px] text-textMuted">
                      Must contain <code className="text-secondary">libbgmi.so</code> inside.
                    </p>
                  </div>

                  <div>
                    <label className="block text-xs font-bold uppercase tracking-wider text-textMuted">
                      New Version Code (e.g. 2.0, 2.1)
                    </label>
                    <input
                      type="text"
                      placeholder="e.g. 2.0"
                      value={libNewVersion}
                      onChange={(e) => setLibNewVersion(e.target.value)}
                      className="mt-1.5 w-full rounded-xl border border-surfaceBorder bg-background px-3.5 py-2.5 font-mono text-sm text-textPrimary placeholder:text-textMuted focus:border-secondary focus:outline-none"
                    />
                  </div>

                  <div className="rounded-xl bg-background/60 p-3 border border-surfaceBorder text-[11px] text-textMuted">
                    💡 <span className="font-semibold text-textSecondary">Smart Storage Cleanup:</span> When you upload a new zip, any previously uploaded zip in Supabase Storage is automatically deleted to keep your storage usage free and clean.
                  </div>

                  <button
                    type="submit"
                    disabled={isUploadingLib || !libFile || !libNewVersion}
                    className="w-full rounded-xl bg-secondary py-3 text-sm font-bold text-black shadow-lg shadow-secondary/20 hover:bg-secondary/90 transition-all disabled:opacity-40 disabled:cursor-not-allowed flex items-center justify-center space-x-2"
                  >
                    {isUploadingLib ? (
                      <>
                        <RefreshCw className="h-4 w-4 animate-spin" />
                        <span>UPLOADING & PUSHING UPDATE...</span>
                      </>
                    ) : (
                      <>
                        <Upload className="h-4 w-4" />
                        <span>UPLOAD ZIP & PUSH UPDATE</span>
                      </>
                    )}
                  </button>
                </>
              ) : (
                <>
                  <div>
                    <label className="block text-xs font-bold uppercase tracking-wider text-textMuted">
                      Direct Download URL for ZIP
                    </label>
                    <input
                      type="url"
                      placeholder="https://github.com/.../release/download/v2/lib.zip"
                      value={libDirectUrl}
                      onChange={(e) => setLibDirectUrl(e.target.value)}
                      className="mt-1.5 w-full rounded-xl border border-surfaceBorder bg-background px-3.5 py-2.5 font-mono text-xs text-textPrimary placeholder:text-textMuted focus:border-secondary focus:outline-none"
                    />
                    <p className="mt-1 text-[11px] text-textMuted">
                      Direct downloadable link to the zip containing <code className="text-secondary">libbgmi.so</code>.
                    </p>
                  </div>

                  <div>
                    <label className="block text-xs font-bold uppercase tracking-wider text-textMuted">
                      New Version Code (e.g. 2.0, 2.1)
                    </label>
                    <input
                      type="text"
                      placeholder="e.g. 2.0"
                      value={libNewVersion}
                      onChange={(e) => setLibNewVersion(e.target.value)}
                      className="mt-1.5 w-full rounded-xl border border-surfaceBorder bg-background px-3.5 py-2.5 font-mono text-sm text-textPrimary placeholder:text-textMuted focus:border-secondary focus:outline-none"
                    />
                  </div>

                  <button
                    type="submit"
                    disabled={isUploadingLib || !libDirectUrl || !libNewVersion}
                    className="w-full rounded-xl bg-secondary py-3 text-sm font-bold text-black shadow-lg shadow-secondary/20 hover:bg-secondary/90 transition-all disabled:opacity-40 disabled:cursor-not-allowed flex items-center justify-center space-x-2"
                  >
                    {isUploadingLib ? (
                      <>
                        <RefreshCw className="h-4 w-4 animate-spin" />
                        <span>PUSHING UPDATE...</span>
                      </>
                    ) : (
                      <>
                        <Zap className="h-4 w-4" />
                        <span>PUSH DIRECT URL UPDATE</span>
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
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4 backdrop-blur-sm">
          <div className="w-full max-w-lg rounded-2xl border border-surfaceBorder bg-surface p-6 shadow-2xl animate-in fade-in zoom-in-95 duration-150 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b border-surfaceBorder pb-4">
              <div className="flex items-center space-x-2">
                <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-amber-500/10 border border-amber-500/20">
                  <Wrench className="h-4 w-4 text-amber-400" />
                </div>
                <div>
                  <h3 className="text-base font-black text-textPrimary">SYSTEM & ANNOUNCEMENTS</h3>
                  <p className="text-[11px] text-textMuted">Control server maintenance and push broadcast notices</p>
                </div>
              </div>
              <button
                onClick={() => setShowSystemModal(false)}
                className="text-textMuted hover:text-textPrimary text-sm font-bold"
              >
                ✕
              </button>
            </div>

            {/* Success notification */}
            {systemConfigSuccessMsg && (
              <div className="mt-3 flex items-center space-x-2 rounded-xl bg-emerald-500/10 border border-emerald-500/30 p-3 text-xs text-emerald-400">
                <CheckCircle2 className="h-4 w-4 shrink-0" />
                <span>{systemConfigSuccessMsg}</span>
              </div>
            )}

            {/* Tab switchers */}
            <div className="mt-4 grid grid-cols-2 gap-2 rounded-xl bg-background p-1 border border-surfaceBorder">
              <button
                type="button"
                onClick={() => {
                  setSystemTab('maintenance');
                  setSystemConfigSuccessMsg(null);
                }}
                className={`flex items-center justify-center space-x-2 rounded-lg py-2 text-xs font-bold uppercase transition-all ${
                  systemTab === 'maintenance'
                    ? 'bg-rose-500 text-white shadow-md'
                    : 'text-textMuted hover:text-textSecondary'
                }`}
              >
                <Wrench className="h-3.5 w-3.5" />
                <span>1. Maintenance</span>
                {maintenanceMode && <span className="h-2 w-2 rounded-full bg-white animate-ping" />}
              </button>
              <button
                type="button"
                onClick={() => {
                  setSystemTab('announcement');
                  setSystemConfigSuccessMsg(null);
                }}
                className={`flex items-center justify-center space-x-2 rounded-lg py-2 text-xs font-bold uppercase transition-all ${
                  systemTab === 'announcement'
                    ? 'bg-amber-500 text-black shadow-md'
                    : 'text-textMuted hover:text-textSecondary'
                }`}
              >
                <Megaphone className="h-3.5 w-3.5" />
                <span>2. Announcement</span>
                {announcementActive && <span className="h-2 w-2 rounded-full bg-black" />}
              </button>
            </div>

            <form onSubmit={handleSaveSystemConfig} className="mt-5 space-y-4">
              {systemTab === 'maintenance' ? (
                <>
                  {/* Maintenance Toggle */}
                  <div className="flex items-center justify-between rounded-xl border border-surfaceBorder bg-background p-3.5">
                    <div>
                      <div className="text-xs font-extrabold uppercase tracking-wide text-textPrimary flex items-center space-x-2">
                        <span>MAINTENANCE MODE</span>
                        <span className={`rounded-full px-2 py-0.5 text-[10px] font-bold ${
                          maintenanceMode ? 'bg-rose-500/20 text-rose-400 border border-rose-500/30' : 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                        }`}>
                          {maintenanceMode ? 'ACTIVE (APP LOCKED)' : 'OFF (NORMAL)'}
                        </span>
                      </div>
                      <p className="mt-1 text-[11px] text-textMuted">
                        When active, app users cannot use or log into the app.
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
                        className={`inline-block h-5 w-5 transform rounded-full bg-white shadow-lg transition duration-200 ease-in-out ${
                          maintenanceMode ? 'translate-x-5' : 'translate-x-0'
                        }`}
                      />
                    </button>
                  </div>

                  {/* Maintenance Message */}
                  <div>
                    <label className="block text-xs font-bold uppercase tracking-wider text-textMuted">
                      Maintenance Notice Message
                    </label>
                    <textarea
                      rows={3}
                      value={maintenanceMessage}
                      onChange={(e) => setMaintenanceMessage(e.target.value)}
                      placeholder="Server is undergoing maintenance. Please check back later."
                      className="mt-1.5 w-full rounded-xl border border-surfaceBorder bg-background px-3.5 py-2.5 text-xs text-textPrimary placeholder:text-textMuted focus:border-rose-500 focus:outline-none leading-relaxed"
                    />
                  </div>

                  {/* Estimated End Time */}
                  <div>
                    <label className="block text-xs font-bold uppercase tracking-wider text-textMuted">
                      Estimated Completion Time
                    </label>
                    <input
                      type="text"
                      value={maintenanceEstimatedEnd}
                      onChange={(e) => setMaintenanceEstimatedEnd(e.target.value)}
                      placeholder="e.g. 1 Hour, 2:00 PM UTC, Tomorrow Morning"
                      className="mt-1.5 w-full rounded-xl border border-surfaceBorder bg-background px-3.5 py-2.5 font-mono text-xs text-textPrimary placeholder:text-textMuted focus:border-rose-500 focus:outline-none"
                    />
                    <p className="mt-1 text-[10px] text-textMuted">
                      This will be shown to users so they know when to expect the server back online.
                    </p>
                  </div>

                  <button
                    type="submit"
                    disabled={isSavingSystemConfig}
                    className="w-full rounded-xl bg-rose-500 py-3 text-xs font-bold uppercase tracking-wider text-white shadow-lg shadow-rose-500/20 hover:bg-rose-600 transition-all disabled:opacity-40 flex items-center justify-center space-x-2"
                  >
                    {isSavingSystemConfig ? (
                      <>
                        <RefreshCw className="h-4 w-4 animate-spin" />
                        <span>SAVING SETTINGS...</span>
                      </>
                    ) : (
                      <>
                        <Wrench className="h-4 w-4" />
                        <span>PUSH MAINTENANCE STATUS</span>
                      </>
                    )}
                  </button>
                </>
              ) : (
                <>
                  {/* Announcement Toggle */}
                  <div className="flex items-center justify-between rounded-xl border border-surfaceBorder bg-background p-3.5">
                    <div>
                      <div className="text-xs font-extrabold uppercase tracking-wide text-textPrimary flex items-center space-x-2">
                        <span>BROADCAST ANNOUNCEMENT</span>
                        <span className={`rounded-full px-2 py-0.5 text-[10px] font-bold ${
                          announcementActive ? 'bg-amber-500/20 text-amber-400 border border-amber-500/30' : 'bg-surfaceBorder text-textMuted'
                        }`}>
                          {announcementActive ? 'BROADCASTING' : 'OFF'}
                        </span>
                      </div>
                      <p className="mt-1 text-[11px] text-textMuted">
                        Displays an in-app popup notice when users launch the app.
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
                        className={`inline-block h-5 w-5 transform rounded-full bg-black shadow-lg transition duration-200 ease-in-out ${
                          announcementActive ? 'translate-x-5' : 'translate-x-0'
                        }`}
                      />
                    </button>
                  </div>

                  {/* Announcement Title */}
                  <div>
                    <label className="block text-xs font-bold uppercase tracking-wider text-textMuted">
                      Announcement Title
                    </label>
                    <input
                      type="text"
                      value={announcementTitle}
                      onChange={(e) => setAnnouncementTitle(e.target.value)}
                      placeholder="e.g. Season Update 3.5 Released!"
                      className="mt-1.5 w-full rounded-xl border border-surfaceBorder bg-background px-3.5 py-2.5 text-xs text-textPrimary placeholder:text-textMuted focus:border-amber-500 focus:outline-none font-semibold"
                    />
                  </div>

                  {/* Announcement Message */}
                  <div>
                    <label className="block text-xs font-bold uppercase tracking-wider text-textMuted">
                      Announcement Message / Details
                    </label>
                    <textarea
                      rows={3}
                      value={announcementMessage}
                      onChange={(e) => setAnnouncementMessage(e.target.value)}
                      placeholder="Enter update changelog, community news, or instructions..."
                      className="mt-1.5 w-full rounded-xl border border-surfaceBorder bg-background px-3.5 py-2.5 text-xs text-textPrimary placeholder:text-textMuted focus:border-amber-500 focus:outline-none leading-relaxed"
                    />
                  </div>

                  {/* Type / Priority */}
                  <div>
                    <label className="block text-xs font-bold uppercase tracking-wider text-textMuted">
                      Notice Style / Priority
                    </label>
                    <div className="mt-1.5 grid grid-cols-3 gap-2">
                      <button
                        type="button"
                        onClick={() => setAnnouncementType('info')}
                        className={`rounded-xl border p-2 text-xs font-bold transition-all ${
                          announcementType === 'info'
                            ? 'border-cyan-500 bg-cyan-500/10 text-cyan-400'
                            : 'border-surfaceBorder bg-background text-textMuted hover:text-textSecondary'
                        }`}
                      >
                        INFO (Blue)
                      </button>
                      <button
                        type="button"
                        onClick={() => setAnnouncementType('warning')}
                        className={`rounded-xl border p-2 text-xs font-bold transition-all ${
                          announcementType === 'warning'
                            ? 'border-amber-500 bg-amber-500/10 text-amber-400'
                            : 'border-surfaceBorder bg-background text-textMuted hover:text-textSecondary'
                        }`}
                      >
                        WARNING (Yellow)
                      </button>
                      <button
                        type="button"
                        onClick={() => setAnnouncementType('critical')}
                        className={`rounded-xl border p-2 text-xs font-bold transition-all ${
                          announcementType === 'critical'
                            ? 'border-rose-500 bg-rose-500/10 text-rose-400'
                            : 'border-surfaceBorder bg-background text-textMuted hover:text-textSecondary'
                        }`}
                      >
                        ALERT (Red)
                      </button>
                    </div>
                  </div>

                  {/* Optional Link */}
                  <div>
                    <label className="block text-xs font-bold uppercase tracking-wider text-textMuted">
                      Optional Action Link (Telegram, Discord, Website)
                    </label>
                    <input
                      type="url"
                      value={announcementLink}
                      onChange={(e) => setAnnouncementLink(e.target.value)}
                      placeholder="https://t.me/your_official_channel"
                      className="mt-1.5 w-full rounded-xl border border-surfaceBorder bg-background px-3.5 py-2.5 font-mono text-xs text-textPrimary placeholder:text-textMuted focus:border-amber-500 focus:outline-none"
                    />
                  </div>

                  <button
                    type="submit"
                    disabled={isSavingSystemConfig}
                    className="w-full rounded-xl bg-amber-500 py-3 text-xs font-bold uppercase tracking-wider text-black shadow-lg shadow-amber-500/20 hover:bg-amber-400 transition-all disabled:opacity-40 flex items-center justify-center space-x-2"
                  >
                    {isSavingSystemConfig ? (
                      <>
                        <RefreshCw className="h-4 w-4 animate-spin" />
                        <span>SAVING ANNOUNCEMENT...</span>
                      </>
                    ) : (
                      <>
                        <Megaphone className="h-4 w-4" />
                        <span>PUSH ANNOUNCEMENT TO APP</span>
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
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4 backdrop-blur-sm">
          <div className="w-full max-w-lg rounded-2xl border border-surfaceBorder bg-surface p-6 shadow-2xl animate-in fade-in zoom-in-95 duration-150 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b border-surfaceBorder pb-4">
              <div className="flex items-center space-x-2">
                <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-cyan-500/10 border border-cyan-500/20">
                  <Smartphone className="h-4 w-4 text-cyan-400" />
                </div>
                <div>
                  <h3 className="text-base font-black text-textPrimary">IN-APP APK UPDATES</h3>
                  <p className="text-[11px] text-textMuted">Push APK releases with changelogs and in-app installer</p>
                </div>
              </div>
              <button
                onClick={() => setShowApkModal(false)}
                className="text-textMuted hover:text-textPrimary text-sm font-bold"
              >
                ✕
              </button>
            </div>

            {/* Success notification */}
            {apkUpdateSuccessMsg && (
              <div className="mt-3 flex items-center space-x-2 rounded-xl bg-emerald-500/10 border border-emerald-500/30 p-3 text-xs text-emerald-400">
                <CheckCircle2 className="h-4 w-4 shrink-0" />
                <span>{apkUpdateSuccessMsg}</span>
              </div>
            )}

            {/* Current Active APK Info Card */}
            <div className="mt-4 rounded-xl border border-surfaceBorder bg-background p-4 space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-[11px] font-bold uppercase tracking-wider text-textMuted">Currently Active Release</span>
                <span className="flex items-center space-x-1.5 rounded-full bg-cyan-500/10 px-2 py-0.5 text-[10px] font-extrabold text-cyan-400 border border-cyan-500/30">
                  <span className="h-1.5 w-1.5 rounded-full bg-cyan-400 animate-pulse" />
                  <span>v{apkActiveVerName} (Code {apkActiveVerCode})</span>
                </span>
              </div>

              <div className="flex items-center justify-between rounded-lg bg-surface px-3 py-2 border border-surfaceBorder/60">
                <span className="font-mono text-[11px] text-textSecondary truncate max-w-[280px] sm:max-w-[340px]">
                  {apkActiveUrl || 'No URL configured'}
                </span>
                {apkActiveUrl && (
                  <button
                    onClick={() => copyToClipboard(apkActiveUrl)}
                    className="ml-2 text-textMuted hover:text-textPrimary"
                    title="Copy APK URL"
                  >
                    {copiedKey === apkActiveUrl ? <Check className="h-3.5 w-3.5 text-emerald-400" /> : <Copy className="h-3.5 w-3.5" />}
                  </button>
                )}
              </div>

              {apkActiveChangelog && (
                <div className="text-[11px] text-textMuted">
                  <span className="font-semibold text-textSecondary">Changelog: </span>
                  <span className="italic">{apkActiveChangelog}</span>
                </div>
              )}
            </div>

            {/* Form to Push New APK */}
            <form onSubmit={handleSaveApkUpdate} className="mt-5 space-y-4">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold uppercase tracking-wider text-textMuted">
                    Version Name
                  </label>
                  <input
                    type="text"
                    required
                    value={apkNewVerName}
                    onChange={(e) => setApkNewVerName(e.target.value)}
                    placeholder="e.g. 2026.02.01"
                    className="mt-1.5 w-full rounded-xl border border-surfaceBorder bg-background px-3.5 py-2.5 font-mono text-xs text-textPrimary placeholder:text-textMuted focus:border-cyan-500 focus:outline-none"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold uppercase tracking-wider text-textMuted">
                    Version Code (Number)
                  </label>
                  <input
                    type="number"
                    required
                    min={apkActiveVerCode + 1}
                    value={apkNewVerCode}
                    onChange={(e) => setApkNewVerCode(e.target.value)}
                    placeholder={`e.g. ${apkActiveVerCode + 1}`}
                    className="mt-1.5 w-full rounded-xl border border-surfaceBorder bg-background px-3.5 py-2.5 font-mono text-xs text-textPrimary placeholder:text-textMuted focus:border-cyan-500 focus:outline-none"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-textMuted">
                  Direct APK Download URL
                </label>
                <input
                  type="url"
                  required
                  value={apkNewUrl}
                  onChange={(e) => setApkNewUrl(e.target.value)}
                  placeholder="https://.../Anoy_Loader_v2.apk"
                  className="mt-1.5 w-full rounded-xl border border-surfaceBorder bg-background px-3.5 py-2.5 font-mono text-xs text-textPrimary placeholder:text-textMuted focus:border-cyan-500 focus:outline-none"
                />
                <p className="mt-1 text-[10px] text-textMuted">
                  The app will download this .apk directly in-app with a live progress bar and trigger installation.
                </p>
              </div>

              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-textMuted">
                  What's New / Changelog
                </label>
                <textarea
                  rows={3}
                  value={apkNewChangelog}
                  onChange={(e) => setApkNewChangelog(e.target.value)}
                  placeholder="• Fixed login connection issue&#10;• Added new VIP features&#10;• Faster injection speed"
                  className="mt-1.5 w-full rounded-xl border border-surfaceBorder bg-background px-3.5 py-2.5 text-xs text-textPrimary placeholder:text-textMuted focus:border-cyan-500 focus:outline-none leading-relaxed"
                />
              </div>

              <div className="flex items-center space-x-2.5 rounded-xl border border-surfaceBorder bg-background p-3">
                <input
                  type="checkbox"
                  id="apkMandatory"
                  checked={apkNewMandatory}
                  onChange={(e) => setApkNewMandatory(e.target.checked)}
                  className="h-4 w-4 rounded border-surfaceBorder text-cyan-500 focus:ring-cyan-500 bg-surface"
                />
                <label htmlFor="apkMandatory" className="text-xs text-textSecondary cursor-pointer select-none">
                  <strong className="text-textPrimary">Mandatory Update</strong> (Users must update before they can use the app)
                </label>
              </div>

              <button
                type="submit"
                disabled={isSavingApkUpdate}
                className="w-full rounded-xl bg-cyan-500 py-3 text-xs font-bold uppercase tracking-wider text-black shadow-lg shadow-cyan-500/20 hover:bg-cyan-400 transition-all disabled:opacity-40 flex items-center justify-center space-x-2"
              >
                {isSavingApkUpdate ? (
                  <>
                    <RefreshCw className="h-4 w-4 animate-spin" />
                    <span>PUBLISHING APK UPDATE...</span>
                  </>
                ) : (
                  <>
                    <Smartphone className="h-4 w-4" />
                    <span>PUSH APK UPDATE TO APP</span>
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
                  className="w-full rounded-xl bg-primary py-2 text-xs font-bold text-black"
                >
                  GOT IT
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
