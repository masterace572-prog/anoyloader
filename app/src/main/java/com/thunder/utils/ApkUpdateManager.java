package com.ryzen.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles APK updates: fetching update metadata, in-app progress downloading, and package installation.
 */
public class ApkUpdateManager {
    private static final String TAG = "ApkUpdateManager";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static class AppUpdateInfo {
        public boolean updateAvailable = false;
        public int serverVersionCode = 0;
        public String serverVersionName = "";
        public String downloadUrl = "";
        public String changelog = "";
        public boolean isMandatory = false;
    }

    public interface UpdateCheckCallback {
        void onCheckFinished(AppUpdateInfo info);
    }

    public interface DownloadProgressCallback {
        void onProgress(int percent, long currentBytes, long totalBytes);
        void onCompleted(File apkFile);
        void onError(String error);
    }

    @SuppressWarnings("deprecation")
    public static int getLocalVersionCode(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pInfo = pm.getPackageInfo(context.getPackageName(), 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return (int) pInfo.getLongVersionCode();
            } else {
                return pInfo.versionCode;
            }
        } catch (Exception e) {
            return 2;
        }
    }

    public static String getLocalVersionName(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pInfo = pm.getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionName != null ? pInfo.versionName : "2026.01.01";
        } catch (Exception e) {
            return "2026.01.01";
        }
    }

    public static void checkForUpdate(Context context, UpdateCheckCallback callback) {
        executor.execute(() -> {
            AppUpdateInfo info = new AppUpdateInfo();
            int localVersionCode = getLocalVersionCode(context);

            // 1. Try Supabase REST
            if (SupabaseConfig.isConfigured()) {
                try {
                    String endpoint = SupabaseConfig.SUPABASE_URL.replaceAll("/+$", "") +
                            "/rest/v1/app_apk_updates?select=*&is_active=eq.true&order=version_code.desc&limit=1";

                    URL url = new URL(endpoint);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY);
                    conn.setRequestProperty("Authorization", "Bearer " + SupabaseConfig.SUPABASE_ANON_KEY);
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(8000);

                    if (conn.getResponseCode() == 200) {
                        try (InputStream is = conn.getInputStream();
                             Scanner scanner = new Scanner(is, "UTF-8")) {
                            StringBuilder sb = new StringBuilder();
                            while (scanner.hasNextLine()) sb.append(scanner.nextLine());
                            JSONArray arr = new JSONArray(sb.toString());
                            if (arr.length() > 0) {
                                JSONObject obj = arr.getJSONObject(0);
                                parseUpdateObject(obj, info, localVersionCode);
                                mainHandler.post(() -> callback.onCheckFinished(info));
                                return;
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Supabase update check failed, checking Vercel fallback", e);
                }
            }

            // 2. Try Vercel API
            if (SupabaseConfig.VERCEL_LOGIN_API_URL != null && !SupabaseConfig.VERCEL_LOGIN_API_URL.isEmpty()) {
                try {
                    String vercelUpdateUrl = SupabaseConfig.VERCEL_LOGIN_API_URL.replace("/api/client/login", "/api/client/app-update")
                            + "?version_code=" + localVersionCode;

                    URL url = new URL(vercelUpdateUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(8000);

                    if (conn.getResponseCode() == 200) {
                        try (InputStream is = conn.getInputStream();
                             Scanner scanner = new Scanner(is, "UTF-8")) {
                            StringBuilder sb = new StringBuilder();
                            while (scanner.hasNextLine()) sb.append(scanner.nextLine());
                            JSONObject obj = new JSONObject(sb.toString());
                            parseUpdateObject(obj, info, localVersionCode);
                            mainHandler.post(() -> callback.onCheckFinished(info));
                            return;
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Vercel update check failed", e);
                }
            }

            mainHandler.post(() -> callback.onCheckFinished(info));
        });
    }

    private static void parseUpdateObject(JSONObject obj, AppUpdateInfo info, int localVersionCode) {
        info.serverVersionCode = obj.optInt("version_code", 0);
        info.serverVersionName = obj.optString("version_name", "");
        info.downloadUrl = obj.optString("download_url", "");
        info.changelog = obj.optString("changelog", "Performance improvements and bug fixes.");
        info.isMandatory = obj.optBoolean("is_mandatory", false);

        if (info.serverVersionCode > localVersionCode && !info.downloadUrl.isEmpty()) {
            info.updateAvailable = true;
        }
    }

    private static HttpURLConnection openConnectionWithRedirects(String urlString, int connectTimeout, int readTimeout) throws Exception {
        URL currentUrl = new URL(urlString);
        int redirects = 0;
        final int MAX_REDIRECTS = 7;

        while (redirects < MAX_REDIRECTS) {
            HttpURLConnection connection = (HttpURLConnection) currentUrl.openConnection();
            connection.setConnectTimeout(connectTimeout);
            connection.setReadTimeout(readTimeout);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) Anoy-Loader-Updater");
            connection.setRequestProperty("Accept-Encoding", "identity");

            int status = connection.getResponseCode();
            if (status == HttpURLConnection.HTTP_MOVED_TEMP
                    || status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == HttpURLConnection.HTTP_SEE_OTHER
                    || status == 307
                    || status == 308) {
                String newUrl = connection.getHeaderField("Location");
                connection.disconnect();
                if (newUrl == null || newUrl.isEmpty()) {
                    throw new Exception("Redirect status " + status + " received without Location header");
                }
                currentUrl = new URL(currentUrl, newUrl);
                redirects++;
                Log.d(TAG, "APK download redirect (" + redirects + ") to: " + currentUrl);
                continue;
            }

            return connection;
        }
        throw new Exception("Too many redirects: " + MAX_REDIRECTS);
    }

    public static void downloadApk(Context context, String apkUrl, DownloadProgressCallback callback) {
        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                conn = openConnectionWithRedirects(apkUrl, 15000, 60000);

                int responseCode = conn.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    mainHandler.post(() -> callback.onError("Server returned HTTP " + responseCode));
                    return;
                }

                int fileLength = conn.getContentLength();
                File downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                if (downloadDir == null) {
                    downloadDir = context.getCacheDir();
                }
                if (!downloadDir.exists()) downloadDir.mkdirs();

                File outputFile = new File(downloadDir, "anoy_loader_update.apk");
                if (outputFile.exists()) outputFile.delete();

                try (InputStream input = conn.getInputStream();
                     FileOutputStream output = new FileOutputStream(outputFile)) {

                    byte[] data = new byte[8192];
                    long total = 0;
                    int count;
                    int lastPercent = -1;

                    while ((count = input.read(data)) != -1) {
                        total += count;
                        output.write(data, 0, count);

                        if (fileLength > 0) {
                            int percent = (int) ((total * 100) / fileLength);
                            if (percent != lastPercent) {
                                lastPercent = percent;
                                final long fTotal = total;
                                final int fPercent = percent;
                                mainHandler.post(() -> callback.onProgress(fPercent, fTotal, fileLength));
                            }
                        }
                    }
                    output.flush();
                }

                mainHandler.post(() -> callback.onCompleted(outputFile));
            } catch (Exception e) {
                Log.e(TAG, "APK download failed", e);
                mainHandler.post(() -> callback.onError("Download error: " + (e.getMessage() != null ? e.getMessage() : "Connection failed")));
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    @SuppressWarnings("deprecation")
    public static void installApk(Activity activity, File apkFile) {
        try {
            if (apkFile == null || !apkFile.exists()) {
                Log.e(TAG, "Install failed: APK file does not exist");
                return;
            }

            Uri apkUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                apkUri = FileProvider.getUriForFile(
                        activity,
                        activity.getPackageName() + ".fileprovider",
                        apkFile
                );
            } else {
                apkUri = Uri.fromFile(apkFile);
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch package installer", e);
        }
    }
}
