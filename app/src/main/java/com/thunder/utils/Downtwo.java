package com.ryzen.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import net.lingala.zip4j.ZipFile;
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

public class Downtwo {
    private static final String TAG = "Downtwo";

    public static native String Version();
    public static native String Link();

    public interface Callback {
        void onComplete(boolean success, String message);
    }

    public interface ProgressListener {
        void onProgress(int percent);
    }

    public static class LibInfo {
        public String version;
        public String downloadUrl;

        public LibInfo(String version, String downloadUrl) {
            this.version = version;
            this.downloadUrl = downloadUrl;
        }
    }

    private final Context context;
    private final Callback callback;
    private ProgressListener progressListener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private static final String PREF_NAME = "com.ryzen.download";
    private static final String PREF_VERSION_KEY = "version";
    private static final String HOSTOP_ZIP = "DIE.zip";
    private static final String LOADER_DIR_NAME = "loader";

    public Downtwo(Context context, Callback callback) {
        this.context = context;
        this.callback = callback;
    }

    public void setProgressListener(ProgressListener listener) {
        this.progressListener = listener;
    }

    public void startDownload() {
        executor.execute(() -> {
            String result = doInBackground();
            mainHandler.post(() -> onPostExecute(result));
        });
    }

    public void execute(String... params) {
        startDownload();
    }

    private String doInBackground() {
        try {
            LibInfo libInfo = fetchActiveLibInfo(context);
            if (libInfo == null || libInfo.downloadUrl == null || libInfo.downloadUrl.isEmpty()) {
                return "Failed to fetch active lib update information";
            }

            File loaderDirectory = new File(context.getFilesDir(), LOADER_DIR_NAME);
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String localVersion = prefs.getString(PREF_VERSION_KEY, "0.0");

            // Check if already up-to-date and at least one valid native library file exists
            boolean hasAnyLib = false;
            if (loaderDirectory.exists() && loaderDirectory.isDirectory()) {
                File[] soFiles = loaderDirectory.listFiles((dir, name) -> name.toLowerCase().endsWith(".so"));
                if (soFiles != null) {
                    for (File f : soFiles) {
                        if (f.isFile() && f.length() >= 100) {
                            hasAnyLib = true;
                            break;
                        }
                    }
                }
            }
            if (localVersion.equals(libInfo.version) && hasAnyLib) {
                Log.d(TAG, "Libraries are already up-to-date (v" + localVersion + ")");
                return "No Update Available";
            }

            return downloadAndExtract(libInfo.downloadUrl, libInfo.version);
        } catch (Exception e) {
            Log.e(TAG, "doInBackground error", e);
            return "Setup error: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
    }

    private void onPostExecute(String result) {
        boolean success = (result == null) || "No Update Available".equals(result);

        try {
            if (callback != null) {
                callback.onComplete(success, result);
            }
        } catch (Exception e) {
            Log.e(TAG, "Callback threw exception", e);
        }
    }

    /**
     * Dynamically fetches the active library version & download URL from Supabase or fallback endpoint.
     */
    public static LibInfo fetchActiveLibInfo(Context context) {
        // 1. Try Supabase REST API if configured
        if (SupabaseConfig.isConfigured()) {
            try {
                String endpoint = SupabaseConfig.SUPABASE_URL.replaceAll("/+$", "") +
                        "/rest/v1/lib_updates?select=version,download_url&is_active=eq.true&order=updated_at.desc&limit=1";

                URL url = new URL(endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + SupabaseConfig.SUPABASE_ANON_KEY);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                if (conn.getResponseCode() == 200) {
                    try (InputStream is = conn.getInputStream();
                         Scanner scanner = new Scanner(is, "UTF-8")) {
                        StringBuilder sb = new StringBuilder();
                        while (scanner.hasNextLine()) sb.append(scanner.nextLine());

                        JSONArray arr = new JSONArray(sb.toString());
                        if (arr.length() > 0) {
                            JSONObject obj = arr.getJSONObject(0);
                            String ver = obj.optString("version", "1.0");
                            String dl = obj.optString("download_url", "");
                            if (!dl.isEmpty()) {
                                Log.d(TAG, "Fetched active lib from Supabase: v" + ver);
                                return new LibInfo(ver, dl);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Supabase lib fetch failed, using fallback", e);
            }
        }

        // 2. Try Vercel API if configured
        if (SupabaseConfig.VERCEL_LOGIN_API_URL != null && !SupabaseConfig.VERCEL_LOGIN_API_URL.isEmpty()) {
            try {
                String vercelApi = SupabaseConfig.VERCEL_LOGIN_API_URL.replace("/api/client/login", "/api/client/lib-update");
                URL url = new URL(vercelApi);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                if (conn.getResponseCode() == 200) {
                    try (InputStream is = conn.getInputStream();
                         Scanner scanner = new Scanner(is, "UTF-8")) {
                        StringBuilder sb = new StringBuilder();
                        while (scanner.hasNextLine()) sb.append(scanner.nextLine());
                        JSONObject obj = new JSONObject(sb.toString());
                        if (obj.optBoolean("success", false)) {
                            return new LibInfo(obj.optString("version", "1.0"), obj.optString("download_url", ""));
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        // 3. Fallback: native URLs from main.cpp
        try {
            String nativeLink = Link();
            if (nativeLink != null && !nativeLink.trim().isEmpty()) {
                String nativeVer = getNativeVersion();
                return new LibInfo(nativeVer != null ? nativeVer : "1.0", nativeLink);
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static String getNativeVersion() {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(Version());
            connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestMethod("GET");

            if (connection.getResponseCode() == 200) {
                try (InputStream inputStream = connection.getInputStream();
                     Scanner scanner = new Scanner(inputStream, "UTF-8")) {
                    if (scanner.hasNextLine()) {
                        return scanner.nextLine().trim();
                    }
                }
            }
        } catch (Exception ignored) {}
        finally {
            if (connection != null) connection.disconnect();
        }
        return null;
    }

    public static HttpURLConnection openConnectionWithRedirects(String urlString, int connectTimeout, int readTimeout) throws Exception {
        URL currentUrl = new URL(urlString);
        int redirects = 0;
        final int MAX_REDIRECTS = 7;

        while (redirects < MAX_REDIRECTS) {
            HttpURLConnection connection = (HttpURLConnection) currentUrl.openConnection();
            connection.setConnectTimeout(connectTimeout);
            connection.setReadTimeout(readTimeout);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) Anoy-Loader");
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
                Log.d(TAG, "Following redirect (" + redirects + ") to: " + currentUrl);
                continue;
            }

            return connection;
        }
        throw new Exception("Too many redirects: " + MAX_REDIRECTS);
    }

    private String downloadAndExtract(String urlString, String targetVersion) {
        HttpURLConnection connection = null;
        File pathOutput = null;
        try {
            Log.d(TAG, "Connecting to download active library: " + urlString);
            connection = openConnectionWithRedirects(urlString, 25000, 90000);

            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                return "Download failed: Server returned HTTP " + responseCode;
            }

            int totalSize = connection.getContentLength();
            int downloaded = 0;
            File pathBase = context.getFilesDir();
            if (!pathBase.exists() && !pathBase.mkdirs()) {
                return "Download failed: Unable to create app storage directory";
            }

            File loaderDirectory = new File(pathBase, LOADER_DIR_NAME);
            if (!loaderDirectory.exists() && !loaderDirectory.mkdirs()) {
                return "Download failed: Unable to create loader directory";
            }

            File targetBgmi = new File(loaderDirectory, "libbgmi.so");
            File targetPubg = new File(loaderDirectory, "libpubgm.so");

            pathOutput = new File(pathBase, "download_lib_temp.bin");
            if (pathOutput.exists()) pathOutput.delete();

            try (InputStream input = connection.getInputStream();
                 FileOutputStream output = new FileOutputStream(pathOutput)) {
                byte[] data = new byte[8192];
                int count;
                while ((count = input.read(data)) != -1) {
                    output.write(data, 0, count);
                    downloaded += count;
                    if (totalSize > 0 && progressListener != null) {
                        final int percent = Math.min(99, (int) ((downloaded * 100L) / totalSize));
                        mainHandler.post(() -> {
                            if (progressListener != null) progressListener.onProgress(percent);
                        });
                    }
                }
                output.flush();
            }

            if (!pathOutput.exists() || pathOutput.length() == 0) {
                return "Download failed: Archive file is empty";
            }

            // Check file format: Raw ELF library vs ZIP archive
            boolean isElf = false;
            boolean isZip = false;
            try (java.io.FileInputStream fis = new java.io.FileInputStream(pathOutput)) {
                byte[] header = new byte[4];
                int read = fis.read(header);
                if (read >= 4) {
                    if (header[0] == 0x7F && header[1] == 'E' && header[2] == 'L' && header[3] == 'F') {
                        isElf = true;
                    } else if (header[0] == 0x50 && header[1] == 0x4B) {
                        isZip = true;
                    }
                }
            }

            if (isElf) {
                Log.d(TAG, "Downloaded file is a raw ELF library. Deploying to both: " + targetBgmi.getName() + " & " + targetPubg.getName());
                copyFile(pathOutput, targetBgmi);
                copyFile(pathOutput, targetPubg);
                pathOutput.delete();
            } else if (isZip) {
                Log.d(TAG, "Downloaded file is a ZIP archive. Extracting to: " + loaderDirectory.getAbsolutePath());
                ZipFile zipFile = new ZipFile(pathOutput);
                if (zipFile.isEncrypted()) {
                    zipFile.setPassword("0000".toCharArray());
                }
                zipFile.extractAll(loaderDirectory.getAbsolutePath());
                pathOutput.delete();

                // Flatten all .so files from nested subdirectories into loaderDirectory
                flattenSoFiles(loaderDirectory, loaderDirectory);

                // If libbgmi.so was in a subfolder inside the zip, move it to loaderDirectory
                if (!targetBgmi.exists() || targetBgmi.length() == 0) {
                    File nestedLib = findFileRecursive(loaderDirectory, "libbgmi.so");
                    if (nestedLib != null && !nestedLib.equals(targetBgmi)) {
                        copyFile(nestedLib, targetBgmi);
                    }
                }

                // If libpubgm.so was in a subfolder inside the zip, move it to loaderDirectory
                if (!targetPubg.exists() || targetPubg.length() == 0) {
                    File nestedPubg = findFileRecursive(loaderDirectory, "libpubgm.so");
                    if (nestedPubg != null && !nestedPubg.equals(targetPubg)) {
                        copyFile(nestedPubg, targetPubg);
                    }
                }
            } else {
                Log.w(TAG, "Unrecognized magic bytes. Assuming direct library file.");
                copyFile(pathOutput, targetBgmi);
                copyFile(pathOutput, targetPubg);
                pathOutput.delete();
            }

            // Verify and set executable permissions for all .so files extracted
            File[] allSoFiles = loaderDirectory.listFiles((dir, name) -> name.toLowerCase().endsWith(".so"));
            boolean hasValidLib = false;
            int validSoCount = 0;
            if (allSoFiles != null) {
                for (File soFile : allSoFiles) {
                    if (soFile.isFile() && soFile.length() >= 100) {
                        hasValidLib = true;
                        validSoCount++;
                        try {
                            Runtime.getRuntime().exec(new String[]{"chmod", "777", soFile.getAbsolutePath()}).waitFor();
                        } catch (Exception ignored) {}
                    }
                }
            }

            if (!hasValidLib) {
                return "Verification failed: no valid native libraries (*.so) found inside downloaded update";
            }

            setPermissions(loaderDirectory);

            if (progressListener != null) {
                mainHandler.post(() -> {
                    if (progressListener != null) progressListener.onProgress(100);
                });
            }

            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(PREF_VERSION_KEY, targetVersion).apply();
            boolean hasBgmi = targetBgmi.isFile() && targetBgmi.length() >= 100;
            boolean hasPubg = targetPubg.isFile() && targetPubg.length() >= 100;
            Log.d(TAG, "Successfully updated libraries to version: " + targetVersion +
                    " (Valid libs: " + validSoCount + ", BGMI: " + (hasBgmi ? targetBgmi.length() + "B" : "none") +
                    ", PUBG: " + (hasPubg ? targetPubg.length() + "B" : "none") + ")");
            return null; // success
        } catch (Exception e) {
            Log.e(TAG, "Download error", e);
            return "Download error: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        } finally {
            if (connection != null) connection.disconnect();
            if (pathOutput != null && pathOutput.exists()) pathOutput.delete();
        }
    }

    private static void copyFile(File source, File destination) throws Exception {
        try (InputStream in = new java.io.FileInputStream(source);
             FileOutputStream out = new FileOutputStream(destination)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            out.flush();
        }
    }

    private static void flattenSoFiles(File currentDir, File targetDir) {
        if (currentDir == null || !currentDir.exists() || !currentDir.isDirectory()) return;
        File[] files = currentDir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                flattenSoFiles(f, targetDir);
            } else if (f.isFile() && f.getName().toLowerCase().endsWith(".so")) {
                File dest = new File(targetDir, f.getName());
                if (!f.getAbsolutePath().equals(dest.getAbsolutePath())) {
                    try {
                        copyFile(f, dest);
                        Log.i(TAG, "Flattened nested library: " + f.getName() + " -> " + dest.getAbsolutePath());
                    } catch (Exception e) {
                        Log.w(TAG, "Failed flattening library: " + f.getName(), e);
                    }
                }
            }
        }
    }

    private static File findFileRecursive(File directory, String fileName) {
        if (directory == null || !directory.exists()) return null;
        File[] files = directory.listFiles();
        if (files == null) return null;
        for (File file : files) {
            if (file.isDirectory()) {
                File found = findFileRecursive(file, fileName);
                if (found != null) return found;
            } else if (file.getName().equalsIgnoreCase(fileName)) {
                return file;
            }
        }
        return null;
    }

    private void setPermissions(File directory) {
        if (directory == null) return;
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    setPermissions(file);
                }
            }
        } else {
            directory.setReadable(true, false);
            directory.setWritable(true, false);
            directory.setExecutable(true, false);
        }
    }
}
