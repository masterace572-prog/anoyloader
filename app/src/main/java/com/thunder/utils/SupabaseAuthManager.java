package com.ryzen.utils;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionPool;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Handles ultra-fast and resilient license authentication.
 * Prioritizes direct Supabase Edge RPC (~500ms) with automatic Vercel API fallback.
 */
public class SupabaseAuthManager {
    private static final String TAG = "SupabaseAuth";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES))
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .writeTimeout(4, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface AuthCallback {
        void onSuccess(AuthResult result);
        void onFailure(String errorMessage);
    }

    public static class AuthResult {
        public boolean success;
        public String status;
        public boolean isLifetime;
        public String activatedAt;
        public String expiresAt;
        public long remainingSeconds;
        public int deviceCount;
        public int maxDevices;

        public AuthResult() {}
    }

    /**
     * Computes a unique, tamper-resistant Hardware ID for the device.
     */
    public static String getHWID(Context context) {
        String androidId = "";
        try {
            androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        } catch (Exception ignored) {}

        if (androidId == null || androidId.isEmpty()) {
            androidId = "VIPER_DEVICE_" + Build.SERIAL;
        }

        String raw = androidId + "#" + Build.MANUFACTURER + "#" + Build.MODEL + "#" + Build.HARDWARE;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder("HWID-");
            for (byte b : digest) {
                sb.append(String.format("%02X", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "HWID-" + Math.abs(raw.hashCode());
        }
    }

    /**
     * Authenticates and activates a license key via Supabase Edge RPC with Vercel fallback.
     */
    public static void authenticate(Context context, String key, AuthCallback callback) {
        final String finalKey = (key != null) ? key.trim() : "";
        final String hwid = getHWID(context);

        executor.execute(() -> {
            if (!SupabaseConfig.isConfigured()) {
                if (finalKey.equalsIgnoreCase("TEST") ||
                    finalKey.equalsIgnoreCase("ADMIN") ||
                    finalKey.equalsIgnoreCase("DEV") ||
                    finalKey.startsWith("TEST-")) {

                    AuthResult mockResult = new AuthResult();
                    mockResult.success = true;
                    mockResult.status = "ACTIVE";
                    mockResult.isLifetime = true;
                    mockResult.expiresAt = "LIFETIME (DEMO)";
                    mockResult.remainingSeconds = -1;
                    mockResult.deviceCount = 1;
                    mockResult.maxDevices = 1;

                    mainHandler.post(() -> callback.onSuccess(mockResult));
                    return;
                }

                mainHandler.post(() -> callback.onFailure(
                        "License key verification failed. Please verify your key and try again."
                ));
                return;
            }

            // --- Priority 1: Direct Supabase RPC (~500ms Edge CDN) ---
            try {
                String directRpcUrl = SupabaseConfig.SUPABASE_URL.replaceAll("/+$", "") + "/rest/v1/rpc/verify_and_activate_key";
                JSONObject directBody = new JSONObject();
                directBody.put("p_key", finalKey);
                directBody.put("p_hwid", hwid);

                Request directRequest = new Request.Builder()
                        .url(directRpcUrl)
                        .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                        .addHeader("Authorization", "Bearer " + SupabaseConfig.SUPABASE_ANON_KEY)
                        .addHeader("Content-Type", "application/json")
                        .post(RequestBody.create(directBody.toString(), JSON_MEDIA_TYPE))
                        .build();

                Response directResponse = httpClient.newCall(directRequest).execute();
                String directResponseString = (directResponse.body() != null) ? directResponse.body().string() : "";

                if (directResponse.isSuccessful() && !directResponseString.isEmpty()) {
                    JSONObject resultJson = new JSONObject(directResponseString);
                    boolean success = resultJson.optBoolean("success", false);

                    if (success) {
                        AuthResult authResult = parseAuthResult(resultJson);
                        mainHandler.post(() -> callback.onSuccess(authResult));
                        return;
                    } else {
                        // Key explicitly rejected by database logic
                        final String errorMsg = resultJson.optString("error", "Invalid or rejected license key");
                        mainHandler.post(() -> callback.onFailure(errorMsg));
                        return;
                    }
                }
            } catch (Exception directEx) {
                Log.w(TAG, "Direct Supabase RPC attempt failed, trying Vercel fallback: " + directEx.getMessage());
            }

            // --- Priority 2: Vercel Proxy API Fallback ---
            if (SupabaseConfig.VERCEL_LOGIN_API_URL != null && !SupabaseConfig.VERCEL_LOGIN_API_URL.isEmpty()) {
                try {
                    String requestUrl = SupabaseConfig.VERCEL_LOGIN_API_URL;
                    JSONObject jsonBody = new JSONObject();
                    jsonBody.put("key", finalKey);
                    jsonBody.put("hwid", hwid);

                    Request request = new Request.Builder()
                            .url(requestUrl)
                            .post(RequestBody.create(jsonBody.toString(), JSON_MEDIA_TYPE))
                            .build();

                    Response response = httpClient.newCall(request).execute();
                    String responseString = (response.body() != null) ? response.body().string() : "";

                    if (!response.isSuccessful()) {
                        String errorMsg = "Authentication failed (HTTP " + response.code() + ")";
                        try {
                            JSONObject errorJson = new JSONObject(responseString);
                            if (errorJson.has("message")) errorMsg = errorJson.getString("message");
                            else if (errorJson.has("error")) errorMsg = errorJson.getString("error");
                        } catch (Exception ignored) {}

                        final String msg = errorMsg;
                        mainHandler.post(() -> callback.onFailure(msg));
                        return;
                    }

                    JSONObject resultJson = new JSONObject(responseString);
                    boolean success = resultJson.optBoolean("success", false);

                    if (!success) {
                        final String errorMsg = resultJson.optString("error", "Invalid or rejected key");
                        mainHandler.post(() -> callback.onFailure(errorMsg));
                        return;
                    }

                    AuthResult authResult = parseAuthResult(resultJson);
                    mainHandler.post(() -> callback.onSuccess(authResult));
                    return;
                } catch (Exception e) {
                    Log.e(TAG, "Vercel fallback auth exception", e);
                }
            }

            mainHandler.post(() -> callback.onFailure("Network error: Unable to connect to server. Please check internet."));
        });
    }

    private static AuthResult parseAuthResult(JSONObject resultJson) {
        AuthResult authResult = new AuthResult();
        authResult.success = true;
        authResult.status = resultJson.optString("status", "ACTIVE");
        authResult.isLifetime = resultJson.optBoolean("is_lifetime", false);
        authResult.activatedAt = resultJson.optString("activated_at", "");
        authResult.expiresAt = resultJson.optString("expires_at", "ACTIVE");
        authResult.remainingSeconds = resultJson.optLong("remaining_seconds", 0);
        authResult.deviceCount = resultJson.optInt("device_count", 1);
        authResult.maxDevices = resultJson.optInt("max_devices", 1);
        return authResult;
    }
}
