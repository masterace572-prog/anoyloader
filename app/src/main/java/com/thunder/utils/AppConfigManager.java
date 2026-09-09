package com.ryzen.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles remote system configuration including Maintenance Mode and In-App Announcements.
 * Features instant local cache response (0ms startup latency) with fast edge CDN background refresh.
 */
public class AppConfigManager {
    private static final String TAG = "AppConfigManager";
    private static final String PREF_CONFIG = "system_config_cache_pref";
    private static final String KEY_CACHED_JSON = "cached_config_json";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static volatile SystemConfig sCachedConfig = null;

    public static class SystemConfig {
        public boolean maintenanceMode = false;
        public String maintenanceMessage = "Server is currently undergoing scheduled maintenance. Please check back soon.";
        public String maintenanceEstimatedEnd = "Soon";

        public boolean announcementActive = false;
        public String announcementTitle = "";
        public String announcementMessage = "";
        public String announcementType = "info";
        public String announcementLink = "";

        public boolean bgmiEnabled = true;
        public String bgmiStatus = "OBB Ready";
        public boolean pubgEnabled = true;
        public String pubgStatus = "OBB Ready";
        public java.util.List<com.ryzen.model.ManagedGame> games = new java.util.ArrayList<>();
    }

    public interface ConfigCallback {
        void onConfigLoaded(SystemConfig config);
    }

    public static void fetchConfig(Context context, ConfigCallback callback) {
        // 1. Immediately deliver cached config if available for 0ms UI loading
        Context appContext = context != null ? context.getApplicationContext() : null;
        if (sCachedConfig == null && appContext != null) {
            sCachedConfig = loadFromCache(appContext);
        }

        if (sCachedConfig != null && callback != null) {
            callback.onConfigLoaded(sCachedConfig);
        }

        // 2. Perform fast background refresh
        executor.execute(() -> {
            SystemConfig freshConfig = new SystemConfig();
            boolean fetched = false;

            // Priority 1: Supabase Direct REST API (~450ms from edge CDN)
            if (SupabaseConfig.isConfigured()) {
                try {
                    String endpoint = SupabaseConfig.SUPABASE_URL.replaceAll("/+$", "") +
                            "/rest/v1/system_config?select=*&id=eq.global&limit=1";

                    URL url = new URL(endpoint);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY);
                    conn.setRequestProperty("Authorization", "Bearer " + SupabaseConfig.SUPABASE_ANON_KEY);
                    conn.setRequestProperty("Connection", "Keep-Alive");
                    conn.setConnectTimeout(3500);
                    conn.setReadTimeout(3500);

                    if (conn.getResponseCode() == 200) {
                        try (InputStream is = conn.getInputStream();
                             Scanner scanner = new Scanner(is, "UTF-8")) {
                            StringBuilder sb = new StringBuilder();
                            while (scanner.hasNextLine()) sb.append(scanner.nextLine());

                            JSONArray arr = new JSONArray(sb.toString());
                            if (arr.length() > 0) {
                                JSONObject obj = arr.getJSONObject(0);
                                parseConfigObject(obj, freshConfig);
                                fetched = true;
                                if (appContext != null) {
                                    saveToCache(appContext, obj.toString());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Direct Supabase config fetch failed: " + e.getMessage());
                }
            }

            // Priority 2: Fallback to Vercel API
            if (!fetched && SupabaseConfig.VERCEL_LOGIN_API_URL != null && !SupabaseConfig.VERCEL_LOGIN_API_URL.isEmpty()) {
                try {
                    String vercelConfigUrl = SupabaseConfig.VERCEL_LOGIN_API_URL.replace("/api/client/login", "/api/client/config");
                    URL url = new URL(vercelConfigUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Connection", "Keep-Alive");
                    conn.setConnectTimeout(4000);
                    conn.setReadTimeout(4000);

                    if (conn.getResponseCode() == 200) {
                        try (InputStream is = conn.getInputStream();
                             Scanner scanner = new Scanner(is, "UTF-8")) {
                            StringBuilder sb = new StringBuilder();
                            while (scanner.hasNextLine()) sb.append(scanner.nextLine());
                            JSONObject obj = new JSONObject(sb.toString());
                            parseConfigObject(obj, freshConfig);
                            fetched = true;
                            if (appContext != null) {
                                saveToCache(appContext, obj.toString());
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Vercel config fetch failed: " + e.getMessage());
                }
            }

            if (freshConfig.games.isEmpty()) {
                freshConfig.games.addAll(com.ryzen.model.ManagedGame.Companion.getDEFAULT_GAMES());
            }

            if (fetched) {
                sCachedConfig = freshConfig;
                if (callback != null) {
                    mainHandler.post(() -> callback.onConfigLoaded(freshConfig));
                }
            } else if (sCachedConfig == null) {
                sCachedConfig = freshConfig;
                if (callback != null) {
                    mainHandler.post(() -> callback.onConfigLoaded(freshConfig));
                }
            }
        });
    }

    private static SystemConfig loadFromCache(Context context) {
        try {
            SharedPreferences sp = context.getSharedPreferences(PREF_CONFIG, Context.MODE_PRIVATE);
            String raw = sp.getString(KEY_CACHED_JSON, null);
            if (raw != null && !raw.isEmpty()) {
                JSONObject obj = new JSONObject(raw);
                SystemConfig cfg = new SystemConfig();
                parseConfigObject(obj, cfg);
                return cfg;
            }
        } catch (Throwable t) {
            Log.w(TAG, "Failed loading config from cache: " + t.getMessage());
        }
        return null;
    }

    private static void saveToCache(Context context, String rawJson) {
        try {
            SharedPreferences sp = context.getSharedPreferences(PREF_CONFIG, Context.MODE_PRIVATE);
            sp.edit().putString(KEY_CACHED_JSON, rawJson).apply();
        } catch (Throwable t) {
            Log.w(TAG, "Failed saving config to cache: " + t.getMessage());
        }
    }

    private static void parseConfigObject(JSONObject obj, SystemConfig config) {
        config.maintenanceMode = obj.optBoolean("maintenance_mode", false);
        config.maintenanceMessage = obj.optString("maintenance_message", config.maintenanceMessage);
        config.maintenanceEstimatedEnd = obj.optString("maintenance_estimated_end", config.maintenanceEstimatedEnd);

        config.announcementActive = obj.optBoolean("announcement_active", false);
        config.announcementTitle = obj.optString("announcement_title", "");
        config.announcementMessage = obj.optString("announcement_message", "");
        config.announcementType = obj.optString("announcement_type", "info");
        config.announcementLink = obj.optString("announcement_link", "");

        config.bgmiEnabled = obj.optBoolean("bgmi_enabled", true);
        config.bgmiStatus = obj.optString("bgmi_status", config.bgmiStatus);
        config.pubgEnabled = obj.optBoolean("pubg_enabled", true);
        config.pubgStatus = obj.optString("pubg_status", config.pubgStatus);

        JSONArray gamesArray = obj.optJSONArray("games");
        if (gamesArray != null && gamesArray.length() > 0) {
            config.games.clear();
            for (int i = 0; i < gamesArray.length(); i++) {
                JSONObject gObj = gamesArray.optJSONObject(i);
                if (gObj != null) {
                    config.games.add(com.ryzen.model.ManagedGame.Companion.fromJson(gObj));
                }
            }
        }
        if (config.games.isEmpty()) {
            config.games.addAll(com.ryzen.model.ManagedGame.Companion.getDEFAULT_GAMES());
        }
    }
}
