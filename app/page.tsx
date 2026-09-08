"use client";

import React, { useState, useEffect, useMemo } from "react";
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
  Terminal,
  Cpu,
  Lock,
  ArrowRight,
  Sliders,
  LogOut,
  X,
} from "lucide-react";
import { supabase, isSupabaseConfigured } from "@/lib/supabase";
import { LicenseKey, KeyStatus, DURATION_OPTIONS, DurationOption, ManagedGame, GameVersion } from "@/lib/types";

const INITIAL_MOCK_KEYS: LicenseKey[] = [
  {
    id: "1",
    key: "ANOY-PRO-7D-9941",
    duration_label: "7 Days",
    duration_seconds: 604800,
    max_devices: 1,
    hwid_list: ["HWID-REDMI-K20-8921"],
    status: "ACTIVE",
    created_at: new Date(Date.now() - 86400000).toISOString(),
    activated_at: new Date(Date.now() - 86400000).toISOString(),
    expires_at: new Date(Date.now() + 518400000).toISOString(),
    last_login_at: new Date().toISOString(),
    last_ip: "103.21.144.92",
    notes: "Sample loader key",
  },
  {
    id: "2",
    key: "SDK-VIP-30D-8820",
    duration_label: "30 Days",
    duration_seconds: 2592000,
    max_devices: 1,
    hwid_list: [],
    status: "UNUSED",
    created_at: new Date().toISOString(),
    activated_at: null,
    expires_at: null,
    last_login_at: null,
    last_ip: null,
    notes: "BCORE_SDK",
  },
  {
    id: "3",
    key: "ANOY-LIFE-9921",
    duration_label: "Lifetime",
    duration_seconds: 0,
    max_devices: 2,
    hwid_list: ["HWID-SAMSUNG-S23-7721"],
    status: "ACTIVE",
    created_at: new Date(Date.now() - 86400000 * 10).toISOString(),
    activated_at: new Date(Date.now() - 86400000 * 10).toISOString(),
    expires_at: null,
    last_login_at: new Date().toISOString(),
    last_ip: "49.36.12.11",
    notes: "VIP Lifetime Key",
  },
];

export default function AdminDashboard() {
  // Authentication PIN
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [enteredPin, setEnteredPin] = useState("");
  const [pinError, setPinError] = useState(false);

  // Core Data
  const [keys, setKeys] = useState<LicenseKey[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState<"ALL" | KeyStatus>("ALL");
  const [targetTypeFilter, setTargetTypeFilter] = useState<"ALL" | "LOADER" | "BCORE_SDK">("ALL");
  const [copiedKey, setCopiedKey] = useState<string | null>(null);

  // Modals
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showExtendModal, setShowExtendModal] = useState<LicenseKey | null>(null);
  const [showBulkSuccessModal, setShowBulkSuccessModal] = useState<string[] | null>(null);
  const [showLibModal, setShowLibModal] = useState(false);
  const [showApkModal, setShowApkModal] = useState(false);
  const [showSystemModal, setShowSystemModal] = useState(false);
  const [showBcoreInfoModal, setShowBcoreInfoModal] = useState(false);

  // System Settings State
  const [systemTab, setSystemTab] = useState<"maintenance" | "announcement" | "games">("maintenance");
  const [bgmiEnabled, setBgmiEnabled] = useState(true);
  const [bgmiStatus, setBgmiStatus] = useState("OBB Ready");
  const [pubgEnabled, setPubgEnabled] = useState(true);
  const [pubgStatus, setPubgStatus] = useState("OBB Ready");
  const [maintenanceMode, setMaintenanceMode] = useState(false);
  const [maintenanceMessage, setMaintenanceMessage] = useState(
    "Server is currently undergoing scheduled maintenance. Please check back soon."
  );
  const [maintenanceEstimatedEnd, setMaintenanceEstimatedEnd] = useState("1 Hour");
  const [announcementActive, setAnnouncementActive] = useState(false);
  const [announcementTitle, setAnnouncementTitle] = useState("Server Notice");
  const [announcementMessage, setAnnouncementMessage] = useState("");
  const [announcementType, setAnnouncementType] = useState<"info" | "warning" | "critical">("info");
  const [announcementLink, setAnnouncementLink] = useState("");
  const [isSavingSystemConfig, setIsSavingSystemConfig] = useState(false);
  const [systemConfigSuccessMsg, setSystemConfigSuccessMsg] = useState<string | null>(null);

  // Advanced Game & Multi-Version Management State
  const [managedGames, setManagedGames] = useState<ManagedGame[]>([]);
  const [loadingGames, setLoadingGames] = useState(false);
  const [showGamesModal, setShowGamesModal] = useState(false);
  const [gamesSuccessMsg, setGamesSuccessMsg] = useState<string | null>(null);

  // Add Game Form State
  const [showAddGameForm, setShowAddGameForm] = useState(false);
  const [newGameId, setNewGameId] = useState("");
  const [newGameTitle, setNewGameTitle] = useState("");
  const [newGamePackage, setNewGamePackage] = useState("");
  const [newGameLib, setNewGameLib] = useState("libbgmi.so");
  const [newGameIcon, setNewGameIcon] = useState("bgmi");
  const [newGameStatus, setNewGameStatus] = useState("OBB Ready");
  const [newGameEnabled, setNewGameEnabled] = useState(true);

  // Add Version Form State
  const [showAddVersionForm, setShowAddVersionForm] = useState(false);
  const [targetGameForVersion, setTargetGameForVersion] = useState<ManagedGame | null>(null);
  const [newVerName, setNewVerName] = useState("");
  const [newVerCode, setNewVerCode] = useState("");
  const [newVerObb, setNewVerObb] = useState("");
  const [newVerTag, setNewVerTag] = useState<"LATEST" | "BETA" | "TEST" | "STABLE" | "COMING SOON">("LATEST");
  const [newVerStatus, setNewVerStatus] = useState("Ready");
  const [newVerLibVersion, setNewVerLibVersion] = useState("1.0");
  const [newVerLibUrl, setNewVerLibUrl] = useState("");
  const [newVerIsDefault, setNewVerIsDefault] = useState(false);
  const [isSavingGameData, setIsSavingGameData] = useState(false);

  // APK In-App Update State
  const [apkActiveVerName, setApkActiveVerName] = useState("2026.01.01");
  const [apkActiveVerCode, setApkActiveVerCode] = useState(2);
  const [apkActiveUrl, setApkActiveUrl] = useState("https://example.com/anoy_loader.apk");
  const [apkActiveChangelog, setApkActiveChangelog] = useState("Initial release with Supabase integration.");
  const [apkActiveMandatory, setApkActiveMandatory] = useState(false);
  const [apkNewVerName, setApkNewVerName] = useState("");
  const [apkNewVerCode, setApkNewVerCode] = useState("");
  const [apkNewUrl, setApkNewUrl] = useState("");
  const [apkNewChangelog, setApkNewChangelog] = useState("");
  const [apkNewMandatory, setApkNewMandatory] = useState(false);
  const [isSavingApkUpdate, setIsSavingApkUpdate] = useState(false);
  const [apkUpdateSuccessMsg, setApkUpdateSuccessMsg] = useState<string | null>(null);

  // Lib Update State
  const [libActiveVersion, setLibActiveVersion] = useState("1.0");
  const [libDownloadUrl, setLibDownloadUrl] = useState("https://github.com/AkhilRyzen/Ryzen/releases/download/Ryzen/hb.zip");
  const [libStoragePath, setLibStoragePath] = useState<string | null>(null);
  const [libUpdatedAt, setLibUpdatedAt] = useState<string>(new Date().toISOString());
  const [libUploadMode, setLibUploadMode] = useState<"upload" | "url">("upload");
  const [libFile, setLibFile] = useState<File | null>(null);
  const [libNewVersion, setLibNewVersion] = useState("");
  const [libDirectUrl, setLibDirectUrl] = useState("");
  const [isUploadingLib, setIsUploadingLib] = useState(false);
  const [libSuccessMsg, setLibSuccessMsg] = useState<string | null>(null);

  // Form State for Key Creation
  const [createKeyType, setCreateKeyType] = useState<"LOADER" | "BCORE_SDK">("LOADER");
  const [createMode, setCreateMode] = useState<"single" | "bulk">("single");
  const [customKeyName, setCustomKeyName] = useState("");
  const [bulkCount, setBulkCount] = useState<number>(5);
  const [selectedDuration, setSelectedDuration] = useState<DurationOption>(DURATION_OPTIONS[5]); // 7 Days
  const [maxDevices, setMaxDevices] = useState<number>(1);
  const [keyNotes, setKeyNotes] = useState("");
  const [extendingSeconds, setExtendingSeconds] = useState<number>(86400);
  const [activationTiming, setActivationTiming] = useState<"ON_FIRST_USE" | "IMMEDIATE">("ON_FIRST_USE");

  // Supabase live indicator
  const [isLiveDatabase, setIsLiveDatabase] = useState(false);

  // PIN verification on load
  useEffect(() => {
    const savedPin = sessionStorage.getItem("admin_session_auth");
    if (savedPin === "valid") {
      setIsAuthenticated(true);
    }
  }, []);

  const handlePinSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (enteredPin === "1234") {
      sessionStorage.setItem("admin_session_auth", "valid");
      setIsAuthenticated(true);
      setPinError(false);
    } else {
      setPinError(true);
    }
  };

  const handleLogout = () => {
    sessionStorage.removeItem("admin_session_auth");
    setIsAuthenticated(false);
    setEnteredPin("");
  };

  // Fetch keys and active info
  const fetchKeys = async () => {
    setLoading(true);
    if (isSupabaseConfigured() && supabase) {
      try {
        const { data, error } = await supabase
          .from("license_keys")
          .select("*")
          .order("created_at", { ascending: false });

        if (!error && data) {
          setKeys(data as LicenseKey[]);
          setIsLiveDatabase(true);
        }

        // Fetch lib update
        const { data: libData } = await supabase
          .from("lib_updates")
          .select("*")
          .eq("is_active", true)
          .order("updated_at", { ascending: false })
          .limit(1)
          .maybeSingle();

        if (libData) {
          setLibActiveVersion(libData.version);
          setLibDownloadUrl(libData.download_url);
          setLibStoragePath(libData.storage_path);
          setLibUpdatedAt(libData.updated_at);
        }

        // Fetch system config
        const { data: sysData } = await supabase
          .from("system_config")
          .select("*")
          .eq("id", "global")
          .maybeSingle();

        if (sysData) {
          setMaintenanceMode(!!sysData.maintenance_mode);
          setMaintenanceMessage(sysData.maintenance_message || "Server is currently undergoing scheduled maintenance.");
          setMaintenanceEstimatedEnd(sysData.maintenance_estimated_end || "Soon");
          setAnnouncementActive(!!sysData.announcement_active);
          setAnnouncementTitle(sysData.announcement_title || "");
          setAnnouncementMessage(sysData.announcement_message || "");
          setAnnouncementType(sysData.announcement_type || "info");
          setAnnouncementLink(sysData.announcement_link || "");
          setBgmiEnabled(sysData.bgmi_enabled !== undefined ? !!sysData.bgmi_enabled : true);
          setBgmiStatus(sysData.bgmi_status || "OBB Ready");
          setPubgEnabled(sysData.pubg_enabled !== undefined ? !!sysData.pubg_enabled : true);
          setPubgStatus(sysData.pubg_status || "OBB Ready");
        }

        // Fetch APK update
        const { data: apkData } = await supabase
          .from("app_apk_updates")
          .select("*")
          .eq("is_active", true)
          .order("version_code", { ascending: false })
          .limit(1)
          .maybeSingle();

        if (apkData) {
          setApkActiveVerName(apkData.version_name);
          setApkActiveVerCode(apkData.version_code);
          setApkActiveUrl(apkData.download_url);
          setApkActiveChangelog(apkData.changelog || "");
          setApkActiveMandatory(!!apkData.is_mandatory);
        }

        setLoading(false);
        return;
      } catch (e) {
        console.error("Supabase fetch error", e);
      }
    }

    // Local fallback
    setIsLiveDatabase(false);
    const stored = localStorage.getItem("anoy_keys");
    if (stored) {
      try {
        setKeys(JSON.parse(stored));
      } catch {
        setKeys(INITIAL_MOCK_KEYS);
      }
    } else {
      setKeys(INITIAL_MOCK_KEYS);
      localStorage.setItem("anoy_keys", JSON.stringify(INITIAL_MOCK_KEYS));
    }
    setLoading(false);
  };

  // Fetch Managed Games & Multi-Versions
  const fetchManagedGames = async () => {
    setLoadingGames(true);
    try {
      const res = await fetch("/api/admin/games");
      const json = await res.json();
      if (json.success && json.games) {
        setManagedGames(json.games);
      }
    } catch (err) {
      console.error("Failed to fetch managed games", err);
    } finally {
      setLoadingGames(false);
    }
  };

  const handleSaveGame = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newGameId.trim() || !newGameTitle.trim() || !newGamePackage.trim()) {
      alert("Game ID, Title, and Package Name are required");
      return;
    }
    setIsSavingGameData(true);
    try {
      const res = await fetch("/api/admin/games", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          action: "upsert_game",
          game: {
            id: newGameId.trim().toLowerCase(),
            title: newGameTitle.trim(),
            package_name: newGamePackage.trim(),
            lib_name: newGameLib.trim() || "libbgmi.so",
            icon_type: newGameIcon.trim() || "bgmi",
            is_enabled: newGameEnabled,
            status_text: newGameStatus.trim() || "OBB Ready",
          },
        }),
      });
      const json = await res.json();
      if (!json.success) throw new Error(json.error);

      setGamesSuccessMsg(`Game ${newGameTitle} saved successfully!`);
      setShowAddGameForm(false);
      setNewGameId("");
      setNewGameTitle("");
      setNewGamePackage("");
      await fetchManagedGames();
      setTimeout(() => setGamesSuccessMsg(null), 2500);
    } catch (err: any) {
      alert(`Error saving game: ${err.message || err}`);
    } finally {
      setIsSavingGameData(false);
    }
  };

  const handleToggleGame = async (game: ManagedGame) => {
    try {
      const res = await fetch("/api/admin/games", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          action: "upsert_game",
          game: {
            ...game,
            is_enabled: !game.is_enabled,
          },
        }),
      });
      const json = await res.json();
      if (!json.success) throw new Error(json.error);
      await fetchManagedGames();
    } catch (err: any) {
      alert(`Error toggling game: ${err.message || err}`);
    }
  };

  const handleUpdateGameStatus = async (game: ManagedGame, newStatus: string) => {
    try {
      const res = await fetch("/api/admin/games", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          action: "upsert_game",
          game: {
            ...game,
            status_text: newStatus,
          },
        }),
      });
      const json = await res.json();
      if (!json.success) throw new Error(json.error);
      await fetchManagedGames();
    } catch (err: any) {
      alert(`Error updating game status: ${err.message || err}`);
    }
  };

  const handleDeleteGame = async (id: string, title: string) => {
    if (!confirm(`Are you sure you want to delete ${title} (${id})?\n\nThis will also remove this game from the loader app immediately!`)) {
      return;
    }
    try {
      const res = await fetch("/api/admin/games", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ action: "delete_game", id }),
      });
      const json = await res.json();
      if (!json.success) throw new Error(json.error);

      setGamesSuccessMsg(`Game ${title} deleted. Removed from loader.`);
      await fetchManagedGames();
      setTimeout(() => setGamesSuccessMsg(null), 2500);
    } catch (err: any) {
      alert(`Error deleting game: ${err.message || err}`);
    }
  };

  const handleSaveVersion = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!targetGameForVersion) return;
    if (!newVerName.trim()) {
      alert("Version Name is required");
      return;
    }
    if (newVerTag !== "COMING SOON" && !newVerCode.trim()) {
      alert("Version Code is required for playable versions");
      return;
    }
    setIsSavingGameData(true);
    try {
      const vCode = newVerCode.trim() ? parseInt(newVerCode.trim(), 10) : 0;
      const defaultObb = vCode > 0 ? `main.${vCode}.${targetGameForVersion.package_name}.obb` : "";
      const res = await fetch("/api/admin/games", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          action: "upsert_version",
          version: {
            game_id: targetGameForVersion.id,
            version_name: newVerName.trim(),
            version_code: vCode,
            obb_name: newVerObb.trim() || defaultObb,
            tag: newVerTag,
            status_text: newVerStatus.trim() || (newVerTag === "COMING SOON" ? "Coming Soon" : "Ready"),
            lib_version: newVerLibVersion.trim() || "1.0",
            lib_download_url: newVerLibUrl.trim(),
            is_default: newVerIsDefault,
            is_active: true,
          },
        }),
      });
      const json = await res.json();
      if (!json.success) throw new Error(json.error);

      setGamesSuccessMsg(`Version v${newVerName} added for ${targetGameForVersion.title}!`);
      setShowAddVersionForm(false);
      setNewVerName("");
      setNewVerCode("");
      setNewVerObb("");
      setNewVerLibUrl("");
      await fetchManagedGames();
      setTimeout(() => setGamesSuccessMsg(null), 2500);
    } catch (err: any) {
      alert(`Error saving version: ${err.message || err}`);
    } finally {
      setIsSavingGameData(false);
    }
  };

  const handleDeleteVersion = async (id: string, verName: string) => {
    if (!confirm(`Are you sure you want to delete version v${verName}?`)) {
      return;
    }
    try {
      const res = await fetch("/api/admin/games", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ action: "delete_version", id }),
      });
      const json = await res.json();
      if (!json.success) throw new Error(json.error);

      setGamesSuccessMsg(`Version v${verName} deleted.`);
      await fetchManagedGames();
      setTimeout(() => setGamesSuccessMsg(null), 2500);
    } catch (err: any) {
      alert(`Error deleting version: ${err.message || err}`);
    }
  };

  useEffect(() => {
    if (isAuthenticated) {
      fetchKeys();
      fetchManagedGames();
    }
  }, [isAuthenticated]);

  const syncKeys = (updated: LicenseKey[]) => {
    setKeys(updated);
    if (!isLiveDatabase) {
      localStorage.setItem("anoy_keys", JSON.stringify(updated));
    }
  };

  // Helper to test if a key is a Bcore SDK key
  const isBcoreKey = (k: LicenseKey) => {
    return (
      (k.notes && k.notes.includes("BCORE_SDK")) ||
      k.key.startsWith("SDK-") ||
      k.key.startsWith("BCORE")
    );
  };

  // Filtered keys
  const filteredKeys = useMemo(() => {
    return keys.filter((k) => {
      const matchesSearch =
        k.key.toLowerCase().includes(searchQuery.toLowerCase()) ||
        (k.notes && k.notes.toLowerCase().includes(searchQuery.toLowerCase())) ||
        (k.last_ip && k.last_ip.includes(searchQuery)) ||
        k.hwid_list.some((h) => h.toLowerCase().includes(searchQuery.toLowerCase()));

      const matchesStatus = statusFilter === "ALL" || k.status === statusFilter;

      let matchesTarget = true;
      if (targetTypeFilter === "LOADER") {
        matchesTarget = !isBcoreKey(k);
      } else if (targetTypeFilter === "BCORE_SDK") {
        matchesTarget = isBcoreKey(k);
      }

      return matchesSearch && matchesStatus && matchesTarget;
    });
  }, [keys, searchQuery, statusFilter, targetTypeFilter]);

  // Statistics
  const stats = useMemo(() => {
    const total = keys.length;
    const active = keys.filter((k) => k.status === "ACTIVE").length;
    const unused = keys.filter((k) => k.status === "UNUSED").length;
    const expired = keys.filter((k) => k.status === "EXPIRED").length;
    const banned = keys.filter((k) => k.status === "BANNED").length;
    const bcoreSdkCount = keys.filter(isBcoreKey).length;
    const loaderCount = total - bcoreSdkCount;
    return { total, active, unused, expired, banned, bcoreSdkCount, loaderCount };
  }, [keys]);

  // Copy helper
  const handleCopy = (text: string) => {
    navigator.clipboard.writeText(text);
    setCopiedKey(text);
    setTimeout(() => setCopiedKey(null), 2000);
  };

  // Key generator helper
  const generateRandomSegment = (len = 4) => {
    const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    let res = "";
    for (let i = 0; i < len; i++) {
      res += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return res;
  };

  const createFormattedKey = (type: "LOADER" | "BCORE_SDK") => {
    if (type === "BCORE_SDK") {
      return `BCORE-SDK-${generateRandomSegment()}-${generateRandomSegment()}`;
    }
    return `ANOY-${generateRandomSegment()}-${generateRandomSegment()}-${generateRandomSegment()}`;
  };

  // Create Key action
  const handleCreateKey = async (e: React.FormEvent) => {
    e.preventDefault();

    const notesToSave = createKeyType === "BCORE_SDK"
      ? (keyNotes ? `BCORE_SDK - ${keyNotes}` : "BCORE_SDK")
      : keyNotes;

    const isTimed = selectedDuration.seconds > 0;
    const isImmediate = isTimed && activationTiming === "IMMEDIATE";
    const now = new Date();
    const expiresAt = isImmediate
      ? new Date(now.getTime() + selectedDuration.seconds * 1000).toISOString()
      : null;
    const keyStatus: KeyStatus = isImmediate ? "ACTIVE" : "UNUSED";
    const activatedAt = isImmediate ? now.toISOString() : null;

    if (createMode === "single") {
      const generatedKey = customKeyName.trim()
        ? customKeyName.trim().toUpperCase()
        : createFormattedKey(createKeyType);

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
        notes: notesToSave || null,
      };

      if (isLiveDatabase && supabase) {
        const { data, error } = await supabase
          .from("license_keys")
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
          notes: notesToSave || null,
        };
        syncKeys([localKey, ...keys]);
      }
    } else {
      // Bulk generation
      const count = Math.min(Math.max(bulkCount, 1), 50);
      const newItems: Partial<LicenseKey>[] = [];
      const keysList: string[] = [];

      for (let i = 0; i < count; i++) {
        const kStr = createFormattedKey(createKeyType);
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
          notes: notesToSave || null,
        });
      }

      if (isLiveDatabase && supabase) {
        const { data, error } = await supabase
          .from("license_keys")
          .insert(newItems)
          .select();

        if (!error && data) {
          syncKeys([...(data as LicenseKey[]), ...keys]);
          setShowBulkSuccessModal(keysList);
        }
      } else {
        const localItems: LicenseKey[] = newItems.map((item, idx) => ({
          id: String(Date.now() + idx),
          key: item.key!,
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
          notes: notesToSave || null,
        }));
        syncKeys([...localItems, ...keys]);
        setShowBulkSuccessModal(keysList);
      }
    }

    setShowCreateModal(false);
    setCustomKeyName("");
    setKeyNotes("");
    setActivationTiming("ON_FIRST_USE");
  };

  // Reset HWID
  const handleResetHwid = async (targetKey: LicenseKey) => {
    if (!confirm(`Reset HWID device bindings for key ${targetKey.key}?`)) return;

    if (isLiveDatabase && supabase) {
      const { error } = await supabase
        .from("license_keys")
        .update({ hwid_list: [] })
        .eq("id", targetKey.id);

      if (!error) {
        syncKeys(keys.map((k) => (k.id === targetKey.id ? { ...k, hwid_list: [] } : k)));
      }
    } else {
      syncKeys(keys.map((k) => (k.id === targetKey.id ? { ...k, hwid_list: [] } : k)));
    }
  };

  // Toggle Ban
  const handleToggleBan = async (targetKey: LicenseKey) => {
    const nextStatus: KeyStatus = targetKey.status === "BANNED" ? "ACTIVE" : "BANNED";

    if (isLiveDatabase && supabase) {
      const { error } = await supabase
        .from("license_keys")
        .update({ status: nextStatus })
        .eq("id", targetKey.id);

      if (!error) {
        syncKeys(keys.map((k) => (k.id === targetKey.id ? { ...k, status: nextStatus } : k)));
      }
    } else {
      syncKeys(keys.map((k) => (k.id === targetKey.id ? { ...k, status: nextStatus } : k)));
    }
  };

  // Delete Key
  const handleDeleteKey = async (targetKey: LicenseKey) => {
    if (!confirm(`Are you sure you want to permanently delete key ${targetKey.key}?`)) return;

    if (isLiveDatabase && supabase) {
      const { error } = await supabase
        .from("license_keys")
        .delete()
        .eq("id", targetKey.id);

      if (!error) {
        syncKeys(keys.filter((k) => k.id !== targetKey.id));
      }
    } else {
      syncKeys(keys.filter((k) => k.id !== targetKey.id));
    }
  };

  // Extend Key
  const handleExtendKey = async () => {
    if (!showExtendModal) return;

    const targetKey = showExtendModal;
    const now = Date.now();
    const currentExpiry = targetKey.expires_at ? new Date(targetKey.expires_at).getTime() : now;
    // If key has already expired (past date), extend from NOW rather than adding to past timestamp
    const baseTime = currentExpiry > now ? currentExpiry : now;
    const newExpiresAt = new Date(baseTime + extendingSeconds * 1000).toISOString();

    if (isLiveDatabase && supabase) {
      const { error } = await supabase
        .from("license_keys")
        .update({ expires_at: newExpiresAt, status: "ACTIVE" })
        .eq("id", targetKey.id);

      if (!error) {
        syncKeys(
          keys.map((k) =>
            k.id === targetKey.id ? { ...k, expires_at: newExpiresAt, status: "ACTIVE" } : k
          )
        );
      }
    } else {
      syncKeys(
        keys.map((k) =>
          k.id === targetKey.id ? { ...k, expires_at: newExpiresAt, status: "ACTIVE" } : k
        )
      );
    }

    setShowExtendModal(null);
  };

  // Save Lib Update
  const handleSaveLibUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsUploadingLib(true);
    setLibSuccessMsg(null);

    let finalUrl = libDirectUrl.trim();

    try {
      if (libUploadMode === "upload" && libFile) {
        if (!supabase) throw new Error("Supabase storage client not connected.");

        const filePath = `libs/${Date.now()}_${libFile.name}`;
        let uploadRes = await supabase.storage.from("libs").upload(filePath, libFile);
        let targetBucket = "libs";
        if (uploadRes.error) {
          uploadRes = await supabase.storage.from("loader-files").upload(filePath, libFile);
          targetBucket = "loader-files";
        }
        if (uploadRes.error) throw uploadRes.error;

        const { data: publicUrlData } = supabase.storage
          .from(targetBucket)
          .getPublicUrl(filePath);

        finalUrl = publicUrlData.publicUrl;
      }

      if (!finalUrl) {
        alert("Please provide a valid download URL or select a library file.");
        setIsUploadingLib(false);
        return;
      }

      const versionToSave = libNewVersion.trim() || libActiveVersion;

      if (isLiveDatabase && supabase) {
        // Set all existing to inactive
        await supabase
          .from("lib_updates")
          .update({ is_active: false })
          .neq("id", "00000000-0000-0000-0000-000000000000");

        // Insert new active
        const { error: insertError } = await supabase
          .from("lib_updates")
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
      setLibSuccessMsg("Native library updated successfully!");

      setTimeout(() => {
        setShowLibModal(false);
        setLibSuccessMsg(null);
        setLibNewVersion("");
        setLibDirectUrl("");
        setLibFile(null);
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
          .from("app_apk_updates")
          .update({ is_active: false })
          .neq("id", "00000000-0000-0000-0000-000000000000");

        const { error } = await supabase
          .from("app_apk_updates")
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
      setApkUpdateSuccessMsg("In-app APK update published successfully!");

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
          .from("system_config")
          .upsert({
            id: "global",
            maintenance_mode: maintenanceMode,
            maintenance_message: maintenanceMessage,
            maintenance_estimated_end: maintenanceEstimatedEnd,
            announcement_active: announcementActive,
            announcement_title: announcementTitle,
            announcement_message: announcementMessage,
            announcement_type: announcementType,
            announcement_link: announcementLink,
            bgmi_enabled: bgmiEnabled,
            bgmi_status: bgmiStatus,
            pubg_enabled: pubgEnabled,
            pubg_status: pubgStatus,
            updated_at: new Date().toISOString(),
          });

        if (error) throw error;
      }

      setSystemConfigSuccessMsg("System configuration updated successfully!");
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
    if (!isoStr) return "Lifetime";
    try {
      const d = new Date(isoStr);
      return d.toLocaleDateString("en-US", {
        month: "short",
        day: "numeric",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
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
        <span className={isExpired ? "text-red-400 font-medium" : "text-neutral-200"}>
          {formatDate(k.expires_at)}
        </span>
      );
    }

    if (k.duration_seconds === 0 || k.duration_label === "Lifetime") {
      return (
        <span className="text-emerald-400 font-medium">
          Lifetime
        </span>
      );
    }

    if (k.status === "UNUSED") {
      return (
        <span className="text-amber-400/90 text-xs font-mono" title="Timer countdown begins upon first device connection">
          Pending first use ({k.duration_label})
        </span>
      );
    }

    return <span className="text-neutral-400">{k.duration_label} (Active)</span>;
  };

  // -------------------------------------------------------------
  // PIN LOGIN VIEW (If not authorized)
  // -------------------------------------------------------------
  if (!isAuthenticated) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[#080808] px-4 text-white">
        <div className="w-full max-w-sm rounded-xl border border-[#222222] bg-[#121212] p-6 shadow-2xl">
          <div className="mb-6 text-center">
            <div className="mx-auto mb-3 flex h-10 w-10 items-center justify-center rounded-lg border border-[#2A2A2A] bg-[#1A1A1A]">
              <Lock className="h-5 w-5 text-white" />
            </div>
            <h1 className="text-base font-semibold text-white tracking-wide">
              ADMIN ACCESS
            </h1>
            <p className="mt-1 text-xs text-neutral-400">
              Enter your authorization PIN to enter control dashboard
            </p>
          </div>

          <form onSubmit={handlePinSubmit} className="space-y-4">
            <div>
              <label className="block text-[11px] font-medium tracking-wider text-neutral-400 uppercase">
                SECURITY PIN
              </label>
              <input
                type="password"
                maxLength={8}
                value={enteredPin}
                onChange={(e) => {
                  setEnteredPin(e.target.value);
                  setPinError(false);
                }}
                className="mt-1.5 w-full rounded-lg border border-[#262626] bg-[#0A0A0A] px-3.5 py-2.5 text-center font-mono text-base tracking-widest text-white focus:border-white focus:outline-none focus:ring-1 focus:ring-white transition-colors"
                autoFocus
              />
              {pinError && (
                <p className="mt-1.5 text-center text-xs text-red-400">
                  Invalid security PIN. Default is 1234
                </p>
              )}
            </div>

            <button
              type="submit"
              className="w-full rounded-lg bg-white py-2.5 text-xs font-semibold text-black hover:bg-neutral-200 transition-colors"
            >
              AUTHENTICATE
            </button>
          </form>
        </div>
      </div>
    );
  }

  // -------------------------------------------------------------
  // MAIN DASHBOARD VIEW
  // -------------------------------------------------------------
  return (
    <div className="min-h-screen bg-[#080808] text-white">
      {/* TOP HEADER */}
      <header className="sticky top-0 z-30 border-b border-[#202020] bg-[#080808]/95 backdrop-blur-md px-3.5 py-2.5 sm:px-8 sm:py-3.5">
        <div className="mx-auto flex max-w-7xl flex-col gap-2.5 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2.5">
              <div className="flex h-8 w-8 sm:h-9 sm:w-9 items-center justify-center rounded-lg border border-[#2A2A2A] bg-[#141414]">
                <Shield className="h-4 w-4 text-white" />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <span className="font-semibold tracking-wide text-xs sm:text-base">
                    ANOY CONTROL
                  </span>
                  <span className="text-[9px] sm:text-[10px] uppercase font-mono tracking-wider text-neutral-400 border border-[#2B2B2B] px-1.5 py-0.5 rounded">
                    {isLiveDatabase ? "LIVE" : "LOCAL"}
                  </span>
                </div>
                <p className="hidden sm:block text-[11px] text-neutral-400">
                  License management, Bcore SDK authorization, and remote OTA dispatch
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
                <RefreshCw className={`h-3 w-3 ${loading ? "animate-spin" : ""}`} />
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
              onClick={() => setShowBcoreInfoModal(true)}
              className="flex shrink-0 items-center gap-1.5 rounded-lg border border-[#262626] bg-[#141414] px-2.5 py-1.5 text-[11px] sm:text-xs font-medium text-white hover:bg-[#1E1E1E] transition-colors"
            >
              <Cpu className="h-3.5 w-3.5 text-white" />
              <span>BCORE SDK</span>
            </button>

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
              onClick={() => {
                fetchManagedGames();
                setShowGamesModal(true);
              }}
              className="flex shrink-0 items-center gap-1.5 rounded-lg border border-[#262626] bg-[#141414] px-2.5 py-1.5 text-[11px] sm:text-xs font-medium text-white hover:bg-[#1E1E1E] transition-colors"
            >
              <Layers className="h-3.5 w-3.5 text-emerald-400" />
              <span>GAMES ({managedGames.length > 0 ? managedGames.length : "2"})</span>
            </button>

            <button
              onClick={() => setShowSystemModal(true)}
              className="flex shrink-0 items-center gap-1.5 rounded-lg border border-[#262626] bg-[#141414] px-2.5 py-1.5 text-[11px] sm:text-xs font-medium text-white hover:bg-[#1E1E1E] transition-colors"
            >
              <Sliders className="h-3.5 w-3.5 text-white" />
              <span>SYSTEM</span>
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
            <div className="mt-1.5 sm:mt-2 flex items-baseline gap-1.5">
              <span className="font-mono text-xl sm:text-2xl font-bold text-white">{stats.total}</span>
              <span className="text-[9px] sm:text-[10px] text-neutral-400 font-mono">
                ({stats.loaderCount} L / {stats.bcoreSdkCount} SDK)
              </span>
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
              <AlertCircle className="h-3.5 w-3.5 text-neutral-400" />
            </div>
            <div className="mt-1.5 sm:mt-2 font-mono text-xl sm:text-2xl font-bold text-white">
              {stats.expired}
            </div>
          </div>

          <div className="col-span-2 rounded-xl border border-[#222222] bg-[#121212] p-3 sm:p-4 sm:col-span-1">
            <div className="flex items-center justify-between">
              <span className="text-[10px] sm:text-[11px] font-medium tracking-wider text-neutral-400 uppercase">
                BANNED
              </span>
              <Ban className="h-3.5 w-3.5 text-neutral-400" />
            </div>
            <div className="mt-1.5 sm:mt-2 font-mono text-xl sm:text-2xl font-bold text-white">
              {stats.banned}
            </div>
          </div>
        </div>

        {/* TOOLBAR: SEARCH, FILTERS & DEDICATED GENERATE BUTTONS */}
        <div className="flex flex-col gap-2.5 rounded-xl border border-[#222222] bg-[#121212] p-3 sm:p-3.5">
          {/* Top row: Search + Refresh + Generate Buttons */}
          <div className="flex items-center gap-2">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-neutral-500" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full rounded-lg border border-[#262626] bg-[#0A0A0A] py-2 pl-9 pr-3 text-xs text-white focus:border-white focus:outline-none focus:ring-1 focus:ring-white transition-colors"
                aria-label="Search keys"
              />
            </div>

            <button
              onClick={fetchKeys}
              title="Refresh Keys"
              className="hidden sm:flex h-9 w-9 items-center justify-center rounded-lg border border-[#262626] bg-[#161616] text-neutral-300 hover:text-white hover:bg-[#202020] transition-colors"
            >
              <RefreshCw className={`h-3.5 w-3.5 ${loading ? "animate-spin" : ""}`} />
            </button>

            {/* LOADER KEY GENERATOR */}
            <button
              onClick={() => {
                setCreateKeyType("LOADER");
                setShowCreateModal(true);
              }}
              className="flex items-center justify-center gap-1.5 rounded-lg bg-white px-3 py-2 text-xs font-semibold text-black hover:bg-neutral-200 transition-colors shrink-0"
            >
              <Plus className="h-3.5 w-3.5" />
              <span className="hidden sm:inline">LOADER KEY</span>
              <span className="sm:hidden">KEY</span>
            </button>

            {/* DEDICATED BCORE SDK KEY GENERATOR */}
            <button
              onClick={() => {
                setCreateKeyType("BCORE_SDK");
                setShowCreateModal(true);
              }}
              className="flex items-center justify-center gap-1.5 rounded-lg border border-white/30 bg-[#1A1A1A] px-3 py-2 text-xs font-semibold text-white hover:bg-[#262626] transition-colors shrink-0"
            >
              <Cpu className="h-3.5 w-3.5 text-white" />
              <span className="hidden sm:inline">SDK KEY</span>
              <span className="sm:hidden">SDK</span>
            </button>
          </div>

          {/* Bottom row: Target Type Filter & Status Filter */}
          <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between border-t border-[#1C1C1C] pt-2">
            {/* Target Type Filter */}
            <div className="flex rounded-lg border border-[#262626] bg-[#0A0A0A] p-0.5 shrink-0">
              <button
                onClick={() => setTargetTypeFilter("ALL")}
                className={`flex-1 sm:flex-none rounded-md px-3 py-1 text-[11px] font-medium transition-colors ${
                  targetTypeFilter === "ALL" ? "bg-white text-black font-semibold" : "text-neutral-400 hover:text-white"
                }`}
              >
                ALL
              </button>
              <button
                onClick={() => setTargetTypeFilter("LOADER")}
                className={`flex-1 sm:flex-none rounded-md px-3 py-1 text-[11px] font-medium transition-colors ${
                  targetTypeFilter === "LOADER" ? "bg-white text-black font-semibold" : "text-neutral-400 hover:text-white"
                }`}
              >
                LOADER
              </button>
              <button
                onClick={() => setTargetTypeFilter("BCORE_SDK")}
                className={`flex-1 sm:flex-none rounded-md px-3 py-1 text-[11px] font-medium transition-colors ${
                  targetTypeFilter === "BCORE_SDK" ? "bg-white text-black font-semibold" : "text-neutral-400 hover:text-white"
                }`}
              >
                BCORE SDK
              </button>
            </div>

            {/* Status Filter (horizontal swipeable pills on mobile) */}
            <div className="flex items-center gap-1 overflow-x-auto no-scrollbar pb-0.5 sm:pb-0">
              {(["ALL", "ACTIVE", "UNUSED", "EXPIRED", "BANNED"] as const).map((st) => (
                <button
                  key={st}
                  onClick={() => setStatusFilter(st)}
                  className={`shrink-0 rounded-md px-2.5 py-1 text-[11px] font-medium transition-colors ${
                    statusFilter === st
                      ? "border border-white bg-white text-black font-semibold"
                      : "border border-[#262626] bg-[#141414] text-neutral-400 hover:text-white"
                  }`}
                >
                  {st}
                </button>
              ))}
            </div>
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
              filteredKeys.map((k) => {
                const isSdk = isBcoreKey(k);
                return (
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
                        <span className="text-neutral-500 uppercase">Target: </span>
                        <span className="text-white font-medium">{isSdk ? "Bcore SDK" : "Loader"}</span>
                      </div>
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
                      <div>
                        <span className="text-neutral-500 uppercase">Expires: </span>
                        <span className="text-white truncate">{renderKeyExpiry(k)}</span>
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
                        title={k.status === "BANNED" ? "Unban Key" : "Ban Key"}
                        className="rounded-md border border-[#2B2B2B] bg-[#181818] px-3 py-2 text-neutral-300 hover:text-white transition-colors"
                      >
                        {k.status === "BANNED" ? <Unlock className="h-3.5 w-3.5 text-white" /> : <Ban className="h-3.5 w-3.5" />}
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
                );
              })
            )}
          </div>

          {/* Desktop Table View (Hidden on mobile) */}
          <div className="hidden md:block overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-[#202020] bg-[#0E0E0E] text-[11px] font-medium uppercase tracking-wider text-neutral-400">
                  <th className="py-3.5 pl-6 pr-4">KEY</th>
                  <th className="px-4 py-3.5">TARGET</th>
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
                    <td colSpan={8} className="py-12 text-center text-neutral-400">
                      Loading licenses...
                    </td>
                  </tr>
                ) : filteredKeys.length === 0 ? (
                  <tr>
                    <td colSpan={8} className="py-12 text-center text-neutral-400">
                      No matching keys found.
                    </td>
                  </tr>
                ) : (
                  filteredKeys.map((k) => {
                    const isSdk = isBcoreKey(k);
                    return (
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

                        <td className="px-4 py-3.5">
                          <span className="font-mono text-[10px] tracking-wider uppercase border border-[#2B2B2B] px-1.5 py-0.5 rounded text-white">
                            {isSdk ? "BCORE SDK" : "LOADER"}
                          </span>
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
                          {k.last_ip || "—"}
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
                              {k.status === "BANNED" ? (
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
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </div>
      </main>

      {/* ============================================================= */}
      {/* MODAL: CREATE KEY (LOADER OR BCORE SDK) */}
      {/* ============================================================= */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-3 sm:p-4">
          <div className="w-full max-w-lg rounded-xl border border-[#242424] bg-[#121212] p-4 sm:p-6 shadow-2xl space-y-4 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b border-[#202020] pb-3">
              <div>
                <h3 className="font-semibold text-white tracking-wide text-sm sm:text-base">
                  GENERATE AUTHORIZATION KEY
                </h3>
                <p className="text-xs text-neutral-400">
                  Select target module and license parameters
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
              {/* Target Type Selection */}
              <div>
                <label className="block text-[11px] font-medium tracking-wider text-neutral-400 uppercase">
                  KEY TARGET MODULE
                </label>
                <div className="mt-1.5 grid grid-cols-2 gap-2">
                  <button
                    type="button"
                    onClick={() => setCreateKeyType("LOADER")}
                    className={`rounded-lg border p-3 text-left transition-colors ${
                      createKeyType === "LOADER"
                        ? "border-white bg-[#1C1C1C] text-white"
                        : "border-[#262626] bg-[#0A0A0A] text-neutral-400 hover:text-white"
                    }`}
                  >
                    <div className="font-semibold text-xs text-white">LOADER KEY</div>
                    <div className="text-[10px] text-neutral-400 mt-0.5">
                      Client login for Android game loader app
                    </div>
                  </button>

                  <button
                    type="button"
                    onClick={() => setCreateKeyType("BCORE_SDK")}
                    className={`rounded-lg border p-3 text-left transition-colors ${
                      createKeyType === "BCORE_SDK"
                        ? "border-white bg-[#1C1C1C] text-white"
                        : "border-[#262626] bg-[#0A0A0A] text-neutral-400 hover:text-white"
                    }`}
                  >
                    <div className="font-semibold text-xs text-white">BCORE SDK KEY</div>
                    <div className="text-[10px] text-neutral-400 mt-0.5">
                      Virtualization engine activation (isolated)
                    </div>
                  </button>
                </div>
              </div>

              {/* Mode Selection */}
              <div>
                <label className="block text-[11px] font-medium tracking-wider text-neutral-400 uppercase">
                  GENERATION MODE
                </label>
                <div className="mt-1.5 grid grid-cols-2 gap-2">
                  <button
                    type="button"
                    onClick={() => setCreateMode("single")}
                    className={`rounded-lg py-2 font-medium transition-colors ${
                      createMode === "single"
                        ? "bg-white text-black font-semibold"
                        : "border border-[#262626] bg-[#141414] text-neutral-300"
                    }`}
                  >
                    SINGLE KEY
                  </button>
                  <button
                    type="button"
                    onClick={() => setCreateMode("bulk")}
                    className={`rounded-lg py-2 font-medium transition-colors ${
                      createMode === "bulk"
                        ? "bg-white text-black font-semibold"
                        : "border border-[#262626] bg-[#141414] text-neutral-300"
                    }`}
                  >
                    BULK BATCH
                  </button>
                </div>
              </div>

              {createMode === "single" ? (
                <div>
                  <label className="block text-[11px] font-medium tracking-wider text-neutral-400 uppercase">
                    CUSTOM KEY NAME (OPTIONAL)
                  </label>
                  <input
                    type="text"
                    value={customKeyName}
                    onChange={(e) => setCustomKeyName(e.target.value)}
                    className="mt-1.5 w-full rounded-lg border border-[#262626] bg-[#0A0A0A] px-3 py-2 text-xs font-mono text-white focus:border-white focus:outline-none focus:ring-1 focus:ring-white transition-colors"
                  />
                </div>
              ) : (
                <div>
                  <label className="block text-[11px] font-medium tracking-wider text-neutral-400 uppercase">
                    BATCH QUANTITY (MAX 50)
                  </label>
                  <input
                    type="number"
                    min={1}
                    max={50}
                    value={bulkCount}
                    onChange={(e) => setBulkCount(parseInt(e.target.value) || 1)}
                    className="mt-1.5 w-full rounded-lg border border-[#262626] bg-[#0A0A0A] px-3 py-2 text-xs font-mono text-white focus:border-white focus:outline-none focus:ring-1 focus:ring-white transition-colors"
                  />
                </div>
              )}

              {/* Duration Selection */}
              <div>
                <label className="block text-[11px] font-medium tracking-wider text-neutral-400 uppercase">
                  DURATION VALIDITY
                </label>
                <div className="mt-1.5 grid grid-cols-3 gap-1.5 sm:grid-cols-4">
                  {DURATION_OPTIONS.map((d) => (
                    <button
                      key={d.label}
                      type="button"
                      onClick={() => setSelectedDuration(d)}
                      className={`rounded-md py-1.5 px-2 text-center text-[11px] font-medium transition-colors ${
                        selectedDuration.label === d.label
                          ? "bg-white text-black font-semibold"
                          : "border border-[#262626] bg-[#0A0A0A] text-neutral-400 hover:text-white"
                      }`}
                    >
                      {d.label}
                    </button>
                  ))}
                </div>
              </div>

              {/* Activation Timing for Timed Keys */}
              {selectedDuration.seconds > 0 && (
                <div>
                  <label className="block text-[11px] font-medium tracking-wider text-neutral-400 uppercase">
                    ACTIVATION TIMING
                  </label>
                  <div className="mt-1.5 grid grid-cols-2 gap-1.5">
                    <button
                      type="button"
                      onClick={() => setActivationTiming("ON_FIRST_USE")}
                      className={`rounded-md py-2 px-2 text-center text-[11px] font-medium transition-colors ${
                        activationTiming === "ON_FIRST_USE"
                          ? "bg-white text-black font-semibold"
                          : "border border-[#262626] bg-[#0A0A0A] text-neutral-400 hover:text-white"
                      }`}
                    >
                      On First Launch
                    </button>
                    <button
                      type="button"
                      onClick={() => setActivationTiming("IMMEDIATE")}
                      className={`rounded-md py-2 px-2 text-center text-[11px] font-medium transition-colors ${
                        activationTiming === "IMMEDIATE"
                          ? "bg-white text-black font-semibold"
                          : "border border-[#262626] bg-[#0A0A0A] text-neutral-400 hover:text-white"
                      }`}
                    >
                      Immediate (Now)
                    </button>
                  </div>
                  <p className="mt-1 text-[10px] text-neutral-500 font-mono">
                    {activationTiming === "ON_FIRST_USE"
                      ? "Timer begins when app/Bcore first connects from a device"
                      : "Timer starts immediately upon creation"}
                  </p>
                </div>
              )}

              {/* Device Limit */}
              <div>
                <label className="block text-[11px] font-medium tracking-wider text-neutral-400 uppercase">
                  MAX CONCURRENT DEVICES
                </label>
                <input
                  type="number"
                  min={1}
                  max={100}
                  value={maxDevices}
                  onChange={(e) => setMaxDevices(parseInt(e.target.value) || 1)}
                  className="mt-1.5 w-full rounded-lg border border-[#262626] bg-[#0A0A0A] px-3 py-2 text-xs font-mono text-white focus:border-white focus:outline-none focus:ring-1 focus:ring-white transition-colors"
                />
              </div>

              {/* Notes */}
              <div>
                <label className="block text-[11px] font-medium tracking-wider text-neutral-400 uppercase">
                  NOTES / REFERENCE
                </label>
                <input
                  type="text"
                  value={keyNotes}
                  onChange={(e) => setKeyNotes(e.target.value)}
                  className="mt-1.5 w-full rounded-lg border border-[#262626] bg-[#0A0A0A] px-3 py-2 text-xs text-white focus:border-white focus:outline-none focus:ring-1 focus:ring-white transition-colors"
                />
              </div>

              {/* Sticky bottom buttons */}
              <div className="sticky bottom-0 bg-[#121212] flex gap-2 pt-3 pb-1 border-t border-[#202020]">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="flex-1 rounded-lg border border-[#2B2B2B] bg-[#161616] py-2.5 text-xs font-semibold text-white hover:bg-[#202020] transition-colors"
                >
                  CANCEL
                </button>
                <button
                  type="submit"
                  className="flex-1 rounded-lg bg-white py-2.5 text-xs font-semibold text-black hover:bg-neutral-200 transition-colors"
                >
                  CREATE
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ============================================================= */}
      {/* MODAL: EXTEND KEY */}
      {/* ============================================================= */}
      {showExtendModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-3 sm:p-4">
          <div className="w-full max-w-sm rounded-xl border border-[#242424] bg-[#121212] p-4 sm:p-6 shadow-2xl space-y-4 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b border-[#202020] pb-3">
              <h3 className="font-semibold text-white tracking-wide text-sm">
                EXTEND VALIDITY
              </h3>
              <button
                onClick={() => setShowExtendModal(null)}
                className="text-neutral-400 hover:text-white p-1"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            <div className="font-mono text-xs text-white">
              Target: <span className="font-bold">{showExtendModal.key}</span>
            </div>

            <div className="space-y-2">
              <label className="block text-[11px] font-medium tracking-wider text-neutral-400 uppercase">
                ADD EXTENSION TIME
              </label>
              <div className="grid grid-cols-3 gap-1.5">
                {[
                  { label: "+1 Day", sec: 86400 },
                  { label: "+3 Days", sec: 259200 },
                  { label: "+7 Days", sec: 604800 },
                  { label: "+15 Days", sec: 1296000 },
                  { label: "+30 Days", sec: 2592000 },
                  { label: "+60 Days", sec: 5184000 },
                ].map((item) => (
                  <button
                    key={item.label}
                    type="button"
                    onClick={() => setExtendingSeconds(item.sec)}
                    className={`rounded-md py-1.5 text-center text-[11px] font-medium transition-colors ${
                      extendingSeconds === item.sec
                        ? "bg-white text-black font-semibold"
                        : "border border-[#262626] bg-[#0A0A0A] text-neutral-400 hover:text-white"
                    }`}
                  >
                    {item.label}
                  </button>
                ))}
              </div>
            </div>

            <div className="sticky bottom-0 bg-[#121212] flex gap-2 pt-3 pb-1 border-t border-[#202020]">
              <button
                onClick={() => setShowExtendModal(null)}
                className="flex-1 rounded-lg border border-[#2B2B2B] bg-[#161616] py-2 text-xs font-semibold text-white hover:bg-[#202020]"
              >
                CANCEL
              </button>
              <button
                onClick={handleExtendKey}
                className="flex-1 rounded-lg bg-white py-2 text-xs font-semibold text-black hover:bg-neutral-200"
              >
                APPLY EXTENSION
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ============================================================= */}
      {/* MODAL: BULK KEYS CREATED */}
      {/* ============================================================= */}
      {showBulkSuccessModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-3 sm:p-4">
          <div className="w-full max-w-md rounded-xl border border-[#242424] bg-[#121212] p-4 sm:p-6 shadow-2xl space-y-4 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b border-[#202020] pb-3">
              <h3 className="font-semibold text-white tracking-wide text-sm">
                BATCH GENERATED ({showBulkSuccessModal.length})
              </h3>
              <button
                onClick={() => setShowBulkSuccessModal(null)}
                className="text-neutral-400 hover:text-white p-1"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            <textarea
              readOnly
              rows={8}
              value={showBulkSuccessModal.join("\n")}
              className="w-full rounded-lg border border-[#262626] bg-[#0A0A0A] p-3 font-mono text-xs text-white focus:outline-none"
            />

            <div className="sticky bottom-0 bg-[#121212] flex gap-2 pt-2 pb-1">
              <button
                onClick={() => handleCopy(showBulkSuccessModal.join("\n"))}
                className="flex-1 rounded-lg bg-white py-2 text-xs font-semibold text-black hover:bg-neutral-200"
              >
                COPY ALL KEYS
              </button>
              <button
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
      {/* MODAL: BCORE SDK INFO & QUICK GENERATION */}
      {/* ============================================================= */}
      {showBcoreInfoModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-3 sm:p-4">
          <div className="w-full max-w-lg rounded-xl border border-[#242424] bg-[#121212] p-4 sm:p-6 shadow-2xl space-y-4 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b border-[#202020] pb-3">
              <div className="flex items-center gap-2">
                <Cpu className="h-4 w-4 text-white" />
                <h3 className="font-semibold text-white tracking-wide text-sm">
                  BCORE SDK CONFIGURATION
                </h3>
              </div>
              <button
                onClick={() => setShowBcoreInfoModal(false)}
                className="text-neutral-400 hover:text-white p-1"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            <div className="space-y-3 text-xs text-neutral-300">
              <div className="rounded-lg border border-[#262626] bg-[#0A0A0A] p-3 space-y-1.5">
                <div className="text-[11px] font-semibold text-white uppercase tracking-wider">
                  SDK CONNECT ENDPOINT
                </div>
                <div className="flex items-center justify-between font-mono text-neutral-400 text-[11px] break-all">
                  <span className="truncate pr-2">https://anoyloader.vercel.app/api/connect</span>
                  <button
                    onClick={() => handleCopy("https://anoyloader.vercel.app/api/connect")}
                    className="text-white hover:text-neutral-300 p-1 shrink-0"
                  >
                    <Copy className="h-3 w-3" />
                  </button>
                </div>
              </div>

              <div className="space-y-1">
                <div className="font-semibold text-white">SDK Protection Rules:</div>
                <ul className="list-disc pl-4 space-y-1 text-neutral-400 text-[11px]">
                  <li>Bcore will only authorize keys with prefix <code className="text-white">SDK-</code> or notes tagged <code className="text-white">BCORE_SDK</code>.</li>
                  <li>Loader login keys (<code className="text-white">ANOY-...</code>) are strictly rejected by the Bcore connect endpoint.</li>
                  <li>Bcore SDK keys cannot be used to log in to the loader dashboard.</li>
                  <li>Device ID is locked upon first activation up to the configured limit.</li>
                </ul>
              </div>
            </div>

            <div className="sticky bottom-0 bg-[#121212] pt-2 pb-1 space-y-2 border-t border-[#202020]">
              <button
                onClick={() => {
                  setShowBcoreInfoModal(false);
                  setCreateKeyType("BCORE_SDK");
                  setShowCreateModal(true);
                }}
                className="w-full rounded-lg bg-white py-2.5 text-xs font-semibold text-black hover:bg-neutral-200 transition-colors"
              >
                GENERATE BCORE SDK KEY NOW
              </button>
              <button
                onClick={() => setShowBcoreInfoModal(false)}
                className="w-full rounded-lg border border-[#2B2B2B] bg-[#161616] py-2 text-xs font-semibold text-neutral-300 hover:text-white"
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
              <h3 className="font-semibold text-white tracking-wide text-sm">
                NATIVE LIBRARY UPDATES (LIBBGMI.SO)
              </h3>
              <button
                onClick={() => setShowLibModal(false)}
                className="text-neutral-400 hover:text-white p-1"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            <div className="rounded-lg border border-[#262626] bg-[#0A0A0A] p-3 text-xs space-y-1">
              <div className="flex justify-between">
                <span className="text-neutral-400">ACTIVE VERSION:</span>
                <span className="font-mono font-bold text-white">v{libActiveVersion}</span>
              </div>
              <div className="flex justify-between truncate">
                <span className="text-neutral-400">URL:</span>
                <span className="font-mono text-neutral-300 truncate max-w-xs">{libDownloadUrl}</span>
              </div>
            </div>

            <form onSubmit={handleSaveLibUpdate} className="space-y-3 text-xs">
              <div className="grid grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={() => setLibUploadMode("upload")}
                  className={`rounded-lg py-2 font-medium transition-colors ${
                    libUploadMode === "upload"
                      ? "bg-white text-black font-semibold"
                      : "border border-[#262626] bg-[#141414] text-neutral-300"
                  }`}
                >
                  UPLOAD ZIP / SO
                </button>
                <button
                  type="button"
                  onClick={() => setLibUploadMode("url")}
                  className={`rounded-lg py-2 font-medium transition-colors ${
                    libUploadMode === "url"
                      ? "bg-white text-black font-semibold"
                      : "border border-[#262626] bg-[#141414] text-neutral-300"
                  }`}
                >
                  DIRECT URL
                </button>
              </div>

              <div>
                <label className="block text-[11px] font-medium tracking-wider text-neutral-400 uppercase">
                  NEW VERSION NUMBER
                </label>
                <input
                  type="text"
                  value={libNewVersion}
                  onChange={(e) => setLibNewVersion(e.target.value)}
                  className="mt-1.5 w-full rounded-lg border border-[#262626] bg-[#0A0A0A] px-3 py-2 text-xs font-mono text-white focus:border-white focus:outline-none focus:ring-1 focus:ring-white transition-colors"
                />
              </div>

              {libUploadMode === "upload" ? (
                <div>
                  <label className="block text-[11px] font-medium tracking-wider text-neutral-400 uppercase">
                    SELECT FILE (.ZIP OR .SO)
                  </label>
                  <input
                    type="file"
                    accept=".zip,.so"
                    onChange={(e) => setLibFile(e.target.files ? e.target.files[0] : null)}
                    className="mt-1.5 w-full rounded-lg border border-[#262626] bg-[#0A0A0A] px-3 py-2 text-xs text-neutral-300 file:mr-3 file:rounded-md file:border-0 file:bg-white file:px-2.5 file:py-1 file:text-xs file:font-semibold file:text-black hover:file:bg-neutral-200"
                  />
                </div>
              ) : (
                <div>
                  <label className="block text-[11px] font-medium tracking-wider text-neutral-400 uppercase">
                    DIRECT DOWNLOAD URL
                  </label>
                  <input
                    type="text"
                    value={libDirectUrl}
                    onChange={(e) => setLibDirectUrl(e.target.value)}
                    className="mt-1.5 w-full rounded-lg border border-[#262626] bg-[#0A0A0A] px-3 py-2 text-xs font-mono text-white focus:border-white focus:outline-none focus:ring-1 focus:ring-white transition-colors"
                  />
                </div>
              )}

              {libSuccessMsg && (
                <div className="rounded-lg border border-emerald-900/50 bg-emerald-950/20 p-2 text-center text-xs text-emerald-400">
                  {libSuccessMsg}
                </div>
              )}

              <div className="sticky bottom-0 bg-[#121212] flex gap-2 pt-3 pb-1 border-t border-[#202020]">
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
                  {isUploadingLib ? "DEPLOYING..." : "DEPLOY UPDATE"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ============================================================= */}
      {/* MODAL: APK IN-APP UPDATE */}
      {/* ============================================================= */}
      {showApkModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-3 sm:p-4">
          <div className="w-full max-w-lg rounded-xl border border-[#242424] bg-[#121212] p-4 sm:p-6 shadow-2xl space-y-4 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b border-[#202020] pb-3">
              <h3 className="font-semibold text-white tracking-wide text-sm">
                IN-APP APK UPDATER
              </h3>
              <button
                onClick={() => setShowApkModal(false)}
                className="text-neutral-400 hover:text-white p-1"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            <div className="rounded-lg border border-[#262626] bg-[#0A0A0A] p-3 text-xs space-y-1">
              <div className="flex justify-between">
                <span className="text-neutral-400">CURRENT VERSION:</span>
                <span className="font-mono font-bold text-white">v{apkActiveVerName} ({apkActiveVerCode})</span>
              </div>
              <div className="flex justify-between truncate">
                <span className="text-neutral-400">DOWNLOAD URL:</span>
                <span className="font-mono text-neutral-300 truncate max-w-xs">{apkActiveUrl}</span>
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
                    value={apkNewVerName}
                    onChange={(e) => setApkNewVerName(e.target.value)}
                    className="mt-1.5 w-full rounded-lg border border-[#262626] bg-[#0A0A0A] px-3 py-2 text-xs font-mono text-white focus:border-white focus:outline-none focus:ring-1 focus:ring-white transition-colors"
                  />
                </div>
                <div>
                  <label className="block text-[11px] font-medium tracking-wider text-neutral-400 uppercase">
                    VERSION CODE
                  </label>
                  <input
                    type="number"
                    value={apkNewVerCode}
                    onChange={(e) => setApkNewVerCode(e.target.value)}
                    className="mt-1.5 w-full rounded-lg border border-[#262626] bg-[#0A0A0A] px-3 py-2 text-xs font-mono text-white focus:border-white focus:outline-none focus:ring-1 focus:ring-white transition-colors"
                  />
                </div>
              </div>

              <div>
                <label className="block text-[11px] font-medium tracking-wider text-neutral-400 uppercase">
                  APK DOWNLOAD URL
                </label>
                <input
                  type="text"
                  value={apkNewUrl}
                  onChange={(e) => setApkNewUrl(e.target.value)}
                  className="mt-1.5 w-full rounded-lg border border-[#262626] bg-[#0A0A0A] px-3 py-2 text-xs font-mono text-white focus:border-white focus:outline-none focus:ring-1 focus:ring-white transition-colors"
                />
              </div>

              <div>
                <label className="block text-[11px] font-medium tracking-wider text-neutral-400 uppercase">
                  RELEASE CHANGELOG
                </label>
                <textarea
                  rows={3}
                  value={apkNewChangelog}
                  onChange={(e) => setApkNewChangelog(e.target.value)}
                  className="mt-1.5 w-full rounded-lg border border-[#262626] bg-[#0A0A0A] px-3 py-2 text-xs text-white focus:border-white focus:outline-none focus:ring-1 focus:ring-white transition-colors"
                />
              </div>

              <div className="flex items-center gap-2 pt-1">
                <input
                  type="checkbox"
                  id="mandatoryCheck"
                  checked={apkNewMandatory}
                  onChange={(e) => setApkNewMandatory(e.target.checked)}
                  className="rounded border-[#262626] bg-[#0A0A0A] text-white"
                />
                <label htmlFor="mandatoryCheck" className="text-xs text-neutral-300">
                  Force mandatory update (blocks previous versions)
                </label>
              </div>

              {apkUpdateSuccessMsg && (
                <div className="rounded-lg border border-emerald-900/50 bg-emerald-950/20 p-2 text-center text-xs text-emerald-400">
                  {apkUpdateSuccessMsg}
                </div>
              )}

              <div className="sticky bottom-0 bg-[#121212] flex gap-2 pt-3 pb-1 border-t border-[#202020]">
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
                  {isSavingApkUpdate ? "PUBLISHING..." : "PUBLISH UPDATE"}
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
              <h3 className="font-semibold text-white tracking-wide text-sm">
                REMOTE SYSTEM CONTROL
              </h3>
              <button
                onClick={() => setShowSystemModal(false)}
                className="text-neutral-400 hover:text-white p-1"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            <div className="flex rounded-lg border border-[#262626] bg-[#0A0A0A] p-1 text-xs">
              <button
                type="button"
                onClick={() => setSystemTab("maintenance")}
                className={`flex-1 rounded-md py-1.5 font-medium transition-colors ${
                  systemTab === "maintenance"
                    ? "bg-white text-black font-semibold"
                    : "text-neutral-400 hover:text-white"
                }`}
              >
                MAINTENANCE
              </button>
              <button
                type="button"
                onClick={() => setSystemTab("announcement")}
                className={`flex-1 rounded-md py-1.5 font-medium transition-colors ${
                  systemTab === "announcement"
                    ? "bg-white text-black font-semibold"
                    : "text-neutral-400 hover:text-white"
                }`}
              >
                ANNOUNCEMENT
              </button>
              <button
                type="button"
                onClick={() => setSystemTab("games")}
                className={`flex-1 rounded-md py-1.5 font-medium transition-colors ${
                  systemTab === "games"
                    ? "bg-white text-black font-semibold"
                    : "text-neutral-400 hover:text-white"
                }`}
              >
                GAMES CONTROL
              </button>
            </div>

            <form onSubmit={handleSaveSystemConfig} className="space-y-4 text-xs">
              {systemTab === "maintenance" && (
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

              {systemTab === "announcement" && (
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

              {systemTab === "games" && (
                <div className="space-y-4">
                  {/* BGMI Card */}
                  <div className="rounded-lg border border-[#262626] bg-[#0A0A0A] p-3 space-y-2">
                    <div className="flex items-center justify-between">
                      <div>
                        <div className="font-semibold text-white">BGMI (Battlegrounds Mobile India)</div>
                        <div className="text-[11px] text-neutral-400 font-mono">com.pubg.imobile &bull; libbgmi.so</div>
                      </div>
                      <input
                        type="checkbox"
                        checked={bgmiEnabled}
                        onChange={(e) => setBgmiEnabled(e.target.checked)}
                        className="h-4 w-4 rounded border-[#262626] bg-[#121212] text-white"
                      />
                    </div>
                    <div>
                      <label className="block text-[11px] font-medium tracking-wider text-neutral-400 uppercase">
                        STATUS TEXT
                      </label>
                      <input
                        type="text"
                        value={bgmiStatus}
                        onChange={(e) => setBgmiStatus(e.target.value)}
                        placeholder="e.g. OBB Ready or Unavailable"
                        className="mt-1 w-full rounded-lg border border-[#262626] bg-[#121212] px-3 py-1.5 text-xs text-white focus:border-white focus:outline-none"
                      />
                    </div>
                  </div>

                  {/* PUBG Global Card */}
                  <div className="rounded-lg border border-[#262626] bg-[#0A0A0A] p-3 space-y-2">
                    <div className="flex items-center justify-between">
                      <div>
                        <div className="font-semibold text-white">PUBG Mobile (Global)</div>
                        <div className="text-[11px] text-neutral-400 font-mono">com.tencent.ig &bull; libpubgm.so</div>
                      </div>
                      <input
                        type="checkbox"
                        checked={pubgEnabled}
                        onChange={(e) => setPubgEnabled(e.target.checked)}
                        className="h-4 w-4 rounded border-[#262626] bg-[#121212] text-white"
                      />
                    </div>
                    <div>
                      <label className="block text-[11px] font-medium tracking-wider text-neutral-400 uppercase">
                        STATUS TEXT
                      </label>
                      <input
                        type="text"
                        value={pubgStatus}
                        onChange={(e) => setPubgStatus(e.target.value)}
                        placeholder="e.g. OBB Ready or Unavailable"
                        className="mt-1 w-full rounded-lg border border-[#262626] bg-[#121212] px-3 py-1.5 text-xs text-white focus:border-white focus:outline-none"
                      />
                    </div>
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
                  {isSavingSystemConfig ? "SAVING..." : "SAVE CONFIG"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
      {/* ============================================================= */}
      {/* MODAL: ADVANCED GAME & MULTI-VERSION MANAGER */}
      {/* ============================================================= */}
      {showGamesModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/85 backdrop-blur-sm p-3 sm:p-4">
          <div className="w-full max-w-3xl rounded-xl border border-[#242424] bg-[#121212] p-4 sm:p-6 shadow-2xl space-y-4 max-h-[90vh] overflow-y-auto">
            {/* Modal Header */}
            <div className="flex items-center justify-between border-b border-[#202020] pb-3">
              <div className="flex items-center gap-2">
                <Layers className="h-5 w-5 text-emerald-400" />
                <div>
                  <h3 className="font-semibold text-white tracking-wide text-sm">
                    GAME & MULTI-VERSION MANAGER
                  </h3>
                  <p className="text-[11px] text-neutral-400">
                    Remotely add/delete games and publish multi-version OBBs (e.g. 4.5.0 and 4.6.0)
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <button
                  onClick={() => {
                    setShowAddGameForm(!showAddGameForm);
                    setShowAddVersionForm(false);
                  }}
                  className="flex items-center gap-1.5 rounded-lg bg-white px-2.5 py-1.5 text-xs font-semibold text-black hover:bg-neutral-200 transition-colors"
                >
                  <Plus className="h-3.5 w-3.5" />
                  <span>ADD GAME</span>
                </button>
                <button
                  onClick={() => setShowGamesModal(false)}
                  className="text-neutral-400 hover:text-white p-1 rounded-lg border border-[#262626] bg-[#161616]"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>
            </div>

            {/* Success message banner */}
            {gamesSuccessMsg && (
              <div className="rounded-lg border border-emerald-900/50 bg-emerald-950/20 p-2.5 text-center text-xs text-emerald-400 font-medium">
                {gamesSuccessMsg}
              </div>
            )}

            {/* Form: Add New Game */}
            {showAddGameForm && (
              <form onSubmit={handleSaveGame} className="rounded-lg border border-[#2B2B2B] bg-[#0E0E0E] p-4 space-y-3">
                <div className="flex items-center justify-between border-b border-[#202020] pb-2">
                  <div className="font-semibold text-white text-xs flex items-center gap-1.5">
                    <Plus className="h-3.5 w-3.5 text-emerald-400" />
                    CREATE NEW MANAGED GAME
                  </div>
                  <button
                    type="button"
                    onClick={() => setShowAddGameForm(false)}
                    className="text-neutral-400 hover:text-white text-xs"
                  >
                    Cancel
                  </button>
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
                  <div>
                    <label className="block text-[11px] font-medium text-neutral-400 uppercase">Game ID (Slug)</label>
                    <input
                      type="text"
                      required
                      placeholder="e.g. bgmi or pubg_kr"
                      value={newGameId}
                      onChange={(e) => setNewGameId(e.target.value)}
                      className="mt-1 w-full rounded-lg border border-[#262626] bg-[#141414] px-3 py-1.5 font-mono text-white text-xs focus:border-white focus:outline-none"
                    />
                  </div>
                  <div>
                    <label className="block text-[11px] font-medium text-neutral-400 uppercase">Display Title</label>
                    <input
                      type="text"
                      required
                      placeholder="e.g. BGMI (Battlegrounds Mobile India)"
                      value={newGameTitle}
                      onChange={(e) => setNewGameTitle(e.target.value)}
                      className="mt-1 w-full rounded-lg border border-[#262626] bg-[#141414] px-3 py-1.5 text-white text-xs focus:border-white focus:outline-none"
                    />
                  </div>
                  <div>
                    <label className="block text-[11px] font-medium text-neutral-400 uppercase">Package Name (Host APK)</label>
                    <input
                      type="text"
                      required
                      placeholder="e.g. com.pubg.imobile"
                      value={newGamePackage}
                      onChange={(e) => setNewGamePackage(e.target.value)}
                      className="mt-1 w-full rounded-lg border border-[#262626] bg-[#141414] px-3 py-1.5 font-mono text-white text-xs focus:border-white focus:outline-none"
                    />
                  </div>
                  <div>
                    <label className="block text-[11px] font-medium text-neutral-400 uppercase">Native Lib Name</label>
                    <input
                      type="text"
                      placeholder="e.g. libbgmi.so"
                      value={newGameLib}
                      onChange={(e) => setNewGameLib(e.target.value)}
                      className="mt-1 w-full rounded-lg border border-[#262626] bg-[#141414] px-3 py-1.5 font-mono text-white text-xs focus:border-white focus:outline-none"
                    />
                  </div>
                  <div>
                    <label className="block text-[11px] font-medium text-neutral-400 uppercase">Icon Type</label>
                    <select
                      value={newGameIcon}
                      onChange={(e) => setNewGameIcon(e.target.value)}
                      className="mt-1 w-full rounded-lg border border-[#262626] bg-[#141414] px-3 py-1.5 text-white text-xs focus:border-white focus:outline-none"
                    >
                      <option value="bgmi">BGMI (India)</option>
                      <option value="pubg_global">PUBG Mobile (Global)</option>
                    </select>
                  </div>
                  <div>
                    <label className="block text-[11px] font-medium text-neutral-400 uppercase">Status Text</label>
                    <div className="flex gap-1.5 mt-1">
                      <input
                        type="text"
                        placeholder="e.g. OBB Ready or Coming Soon"
                        value={newGameStatus}
                        onChange={(e) => setNewGameStatus(e.target.value)}
                        className="w-full rounded-lg border border-[#262626] bg-[#141414] px-3 py-1.5 text-white text-xs focus:border-white focus:outline-none"
                      />
                      <button
                        type="button"
                        onClick={() => setNewGameStatus("Coming Soon")}
                        className="shrink-0 rounded bg-purple-950/60 border border-purple-800/60 px-2 py-1 text-[10px] font-semibold text-purple-300 hover:bg-purple-900/60"
                      >
                        Coming Soon
                      </button>
                      <button
                        type="button"
                        onClick={() => setNewGameStatus("OBB Ready")}
                        className="shrink-0 rounded bg-emerald-950/60 border border-emerald-800/60 px-2 py-1 text-[10px] font-semibold text-emerald-300 hover:bg-emerald-900/60"
                      >
                        OBB Ready
                      </button>
                    </div>
                  </div>
                </div>
                <div className="flex items-center justify-between pt-2">
                  <label className="flex items-center gap-2 text-xs text-neutral-300">
                    <input
                      type="checkbox"
                      checked={newGameEnabled}
                      onChange={(e) => setNewGameEnabled(e.target.checked)}
                      className="h-4 w-4 rounded border-[#262626] bg-[#141414]"
                    />
                    Enable this game in loader
                  </label>
                  <button
                    type="submit"
                    disabled={isSavingGameData}
                    className="rounded-lg bg-emerald-500 px-3 py-1.5 text-xs font-semibold text-black hover:bg-emerald-400 disabled:opacity-50"
                  >
                    {isSavingGameData ? "SAVING..." : "CREATE GAME"}
                  </button>
                </div>
              </form>
            )}

            {/* Form: Add Version to Game */}
            {showAddVersionForm && targetGameForVersion && (
              <form onSubmit={handleSaveVersion} className="rounded-lg border border-[#2B2B2B] bg-[#0E0E0E] p-4 space-y-3">
                <div className="flex items-center justify-between border-b border-[#202020] pb-2">
                  <div className="font-semibold text-white text-xs flex items-center gap-1.5">
                    <Plus className="h-3.5 w-3.5 text-cyan-400" />
                    ADD VERSION FOR {targetGameForVersion.title}
                  </div>
                  <button
                    type="button"
                    onClick={() => setShowAddVersionForm(false)}
                    className="text-neutral-400 hover:text-white text-xs"
                  >
                    Cancel
                  </button>
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
                  <div>
                    <label className="block text-[11px] font-medium text-neutral-400 uppercase">Version Name</label>
                    <input
                      type="text"
                      required
                      placeholder="e.g. 4.5.0 or 4.6.0"
                      value={newVerName}
                      onChange={(e) => setNewVerName(e.target.value)}
                      className="mt-1 w-full rounded-lg border border-[#262626] bg-[#141414] px-3 py-1.5 text-white text-xs focus:border-white focus:outline-none"
                    />
                  </div>
                  <div>
                    <label className="block text-[11px] font-medium text-neutral-400 uppercase">
                      Version Code {newVerTag === "COMING SOON" ? "(Optional for Coming Soon)" : "(Android versionCode)"}
                    </label>
                    <input
                      type="number"
                      required={newVerTag !== "COMING SOON"}
                      placeholder={newVerTag === "COMING SOON" ? "Optional for Coming Soon" : "e.g. 21325 for 4.5.0 or 21455 for 4.6.0"}
                      value={newVerCode}
                      onChange={(e) => {
                        setNewVerCode(e.target.value);
                        if (!newVerObb && e.target.value) {
                          setNewVerObb(`main.${e.target.value}.${targetGameForVersion.package_name}.obb`);
                        }
                      }}
                      className="mt-1 w-full rounded-lg border border-[#262626] bg-[#141414] px-3 py-1.5 font-mono text-white text-xs focus:border-white focus:outline-none"
                    />
                  </div>
                  <div>
                    <label className="block text-[11px] font-medium text-neutral-400 uppercase">
                      OBB File Name {newVerTag === "COMING SOON" ? "(Optional)" : ""}
                    </label>
                    <input
                      type="text"
                      placeholder={newVerTag === "COMING SOON" ? "Optional for Coming Soon" : `e.g. main.${newVerCode || "21325"}.${targetGameForVersion.package_name}.obb`}
                      value={newVerObb}
                      onChange={(e) => setNewVerObb(e.target.value)}
                      className="mt-1 w-full rounded-lg border border-[#262626] bg-[#141414] px-3 py-1.5 font-mono text-white text-xs focus:border-white focus:outline-none"
                    />
                  </div>
                  <div>
                    <label className="block text-[11px] font-medium text-neutral-400 uppercase">Tag / Status Badge</label>
                    <select
                      value={newVerTag}
                      onChange={(e) => setNewVerTag(e.target.value as any)}
                      className="mt-1 w-full rounded-lg border border-[#262626] bg-[#141414] px-3 py-1.5 text-white text-xs focus:border-white focus:outline-none"
                    >
                      <option value="LATEST">LATEST (Official Current)</option>
                      <option value="BETA">BETA (Upcoming Build e.g. 4.6.0)</option>
                      <option value="COMING SOON">COMING SOON (Unreleased Version)</option>
                      <option value="TEST">TEST (Internal Testing)</option>
                      <option value="STABLE">STABLE (Legacy Stable)</option>
                    </select>
                  </div>
                  <div>
                    <label className="block text-[11px] font-medium text-neutral-400 uppercase">Lib Version</label>
                    <input
                      type="text"
                      placeholder="e.g. 1.0 or 1.1-beta"
                      value={newVerLibVersion}
                      onChange={(e) => setNewVerLibVersion(e.target.value)}
                      className="mt-1 w-full rounded-lg border border-[#262626] bg-[#141414] px-3 py-1.5 font-mono text-white text-xs focus:border-white focus:outline-none"
                    />
                  </div>
                  <div>
                    <label className="block text-[11px] font-medium text-neutral-400 uppercase">Lib Download URL (Optional)</label>
                    <input
                      type="text"
                      placeholder="e.g. https://.../libbgmi_4.6.0.so"
                      value={newVerLibUrl}
                      onChange={(e) => setNewVerLibUrl(e.target.value)}
                      className="mt-1 w-full rounded-lg border border-[#262626] bg-[#141414] px-3 py-1.5 font-mono text-white text-xs focus:border-white focus:outline-none"
                    />
                  </div>
                </div>
                <div className="flex items-center justify-between pt-2">
                  <label className="flex items-center gap-2 text-xs text-neutral-300">
                    <input
                      type="checkbox"
                      checked={newVerIsDefault}
                      onChange={(e) => setNewVerIsDefault(e.target.checked)}
                      className="h-4 w-4 rounded border-[#262626] bg-[#141414]"
                    />
                    Set as default selected version
                  </label>
                  <button
                    type="submit"
                    disabled={isSavingGameData}
                    className="rounded-lg bg-cyan-500 px-3 py-1.5 text-xs font-semibold text-black hover:bg-cyan-400 disabled:opacity-50"
                  >
                    {isSavingGameData ? "SAVING..." : "SAVE VERSION"}
                  </button>
                </div>
              </form>
            )}

            {/* Managed Games List */}
            <div className="space-y-4">
              {loadingGames ? (
                <div className="rounded-lg border border-[#262626] bg-[#0A0A0A] p-6 text-center text-xs text-neutral-400">
                  Loading games and versions...
                </div>
              ) : managedGames.length === 0 ? (
                <div className="rounded-lg border border-[#262626] bg-[#0A0A0A] p-6 text-center text-xs text-neutral-400">
                  No games configured. Click &quot;+ ADD GAME&quot; above to create one.
                </div>
              ) : (
                managedGames.map((game) => (
                  <div key={game.id} className="rounded-xl border border-[#262626] bg-[#0E0E0E] p-4 space-y-3">
                    {/* Game Header */}
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-[#202020] pb-3">
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="font-semibold text-white text-sm">{game.title}</span>
                          <span
                            className={`text-[10px] uppercase font-mono px-1.5 py-0.5 rounded font-semibold ${
                              game.is_enabled ? "bg-emerald-950/50 text-emerald-400 border border-emerald-800/40" : "bg-red-950/50 text-red-400 border border-red-800/40"
                            }`}
                          >
                            {game.is_enabled ? "ACTIVE" : "DISABLED"}
                          </span>
                          {game.status_text && game.status_text.toLowerCase().includes("soon") && (
                            <span className="text-[10px] uppercase font-mono px-1.5 py-0.5 rounded font-semibold bg-purple-950/50 text-purple-400 border border-purple-800/40">
                              COMING SOON
                            </span>
                          )}
                        </div>
                        <div className="text-[11px] text-neutral-400 font-mono mt-0.5">
                          Pkg: {game.package_name} &bull; Lib: {game.lib_name} &bull; Status: {game.status_text}
                        </div>
                      </div>

                      <div className="flex items-center gap-2">
                        <button
                          type="button"
                          onClick={() => {
                            const isSoon = game.status_text.toLowerCase().includes("soon");
                            handleUpdateGameStatus(game, isSoon ? "OBB Ready" : "Coming Soon");
                          }}
                          className="rounded-md border border-[#2B2B2B] bg-[#161616] px-2 py-1 text-[11px] font-medium text-purple-300 hover:text-white hover:bg-purple-950/40"
                          title="Toggle Coming Soon status in loader"
                        >
                          {game.status_text.toLowerCase().includes("soon") ? "Mark Ready" : "Set Coming Soon"}
                        </button>
                        <button
                          type="button"
                          onClick={() => {
                            setTargetGameForVersion(game);
                            setNewVerName("");
                            setNewVerCode("");
                            setNewVerObb("");
                            setShowAddVersionForm(true);
                            setShowAddGameForm(false);
                          }}
                          className="flex items-center gap-1 rounded-md border border-[#2B2B2B] bg-[#161616] px-2 py-1 text-[11px] font-medium text-white hover:bg-[#222222]"
                        >
                          <Plus className="h-3 w-3 text-cyan-400" />
                          Add Version
                        </button>
                        <button
                          type="button"
                          onClick={() => handleToggleGame(game)}
                          className="rounded-md border border-[#2B2B2B] bg-[#161616] px-2 py-1 text-[11px] font-medium text-neutral-300 hover:text-white hover:bg-[#222222]"
                        >
                          {game.is_enabled ? "Disable" : "Enable"}
                        </button>
                        <button
                          type="button"
                          onClick={() => handleDeleteGame(game.id, game.title)}
                          title="Delete game from server and loader"
                          className="rounded-md border border-[#2B2B2B] bg-[#161616] p-1 text-neutral-400 hover:text-red-400 hover:bg-red-950/20"
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                        </button>
                      </div>
                    </div>

                    {/* Versions Section */}
                    <div className="space-y-2">
                      <div className="text-[11px] font-medium tracking-wider text-neutral-400 uppercase">
                        VERSIONS ({game.versions ? game.versions.length : 0})
                      </div>

                      {!game.versions || game.versions.length === 0 ? (
                        <div className="rounded-lg border border-dashed border-[#262626] bg-[#121212] p-3 text-center text-[11px] text-neutral-500">
                          No versions added yet. Click &quot;Add Version&quot; to configure v4.5.0, v4.6.0, etc.
                        </div>
                      ) : (
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                          {game.versions.map((ver) => {
                            const tagColor =
                              ver.tag === "LATEST"
                                ? "bg-emerald-950/50 text-emerald-400 border-emerald-800/40"
                                : ver.tag === "BETA"
                                ? "bg-amber-950/50 text-amber-400 border-amber-800/40"
                                : ver.tag === "TEST"
                                ? "bg-cyan-950/50 text-cyan-400 border-cyan-800/40"
                                : "bg-neutral-800 text-neutral-300 border-neutral-700";

                            return (
                              <div
                                key={ver.id}
                                className="rounded-lg border border-[#222222] bg-[#141414] p-2.5 flex items-center justify-between"
                              >
                                <div className="space-y-1">
                                  <div className="flex items-center gap-1.5">
                                    <span className="font-semibold text-white text-xs">v{ver.version_name}</span>
                                    <span className={`text-[9px] uppercase font-mono px-1 py-0.2 rounded border ${tagColor}`}>
                                      {ver.tag}
                                    </span>
                                    {ver.is_default && (
                                      <span className="text-[9px] uppercase font-mono px-1 py-0.2 rounded border bg-purple-950/50 text-purple-400 border-purple-800/40">
                                        DEFAULT
                                      </span>
                                    )}
                                  </div>
                                  <div className="text-[10px] text-neutral-400 font-mono">
                                    Code: {ver.version_code} &bull; Lib: v{ver.lib_version}
                                  </div>
                                  <div className="text-[10px] text-neutral-500 font-mono truncate max-w-[200px]">
                                    {ver.obb_name}
                                  </div>
                                </div>

                                <button
                                  type="button"
                                  onClick={() => handleDeleteVersion(ver.id, ver.version_name)}
                                  title="Delete this version"
                                  className="text-neutral-500 hover:text-red-400 p-1.5 rounded"
                                >
                                  <Trash2 className="h-3 w-3" />
                                </button>
                              </div>
                            );
                          })}
                        </div>
                      )}
                    </div>
                  </div>
                ))
              )}
            </div>

            {/* Footer */}
            <div className="flex justify-end pt-3 border-t border-[#202020]">
              <button
                type="button"
                onClick={() => setShowGamesModal(false)}
                className="rounded-lg bg-white px-4 py-1.5 text-xs font-semibold text-black hover:bg-neutral-200"
              >
                CLOSE
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
