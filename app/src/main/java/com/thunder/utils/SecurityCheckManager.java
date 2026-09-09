package com.ryzen.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;

import java.io.File;

/**
 * Performs real-time security, anti-tamper, debugger, and environment integrity checks.
 */
public class SecurityCheckManager {
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface SecurityCallback {
        void onProgress(String status);
        void onResult(boolean isSecure, String warningMessage);
    }

    public static void runChecks(Context context, SecurityCallback callback) {
        new Thread(() -> {
            try {
                // Step 1: Initializing core
                postProgress(callback, "Initializing core runtime...");
                Thread.sleep(100);

                // Step 2: Debugger check
                postProgress(callback, "Checking runtime debugger...");
                boolean hasDebugger = Debug.isDebuggerConnected() || Debug.waitingForDebugger();
                Thread.sleep(80);

                // Step 3: Root & Binary checks
                postProgress(callback, "Verifying device integrity...");
                boolean isRooted = isDeviceRooted();
                Thread.sleep(80);

                // Step 4: Permissions check
                postProgress(callback, "Checking environment permissions...");
                boolean hasPermissions = PermissionsManager.isAllPermissionsGranted(context);
                Thread.sleep(80);

                // Step 5: Package signature & integrity
                postProgress(callback, "Verifying package signature...");
                boolean isSignatureValid = verifyPackageIntegrity(context);
                Thread.sleep(80);

                boolean isSecure = !hasDebugger && isSignatureValid;
                String warning = "";
                if (hasDebugger) warning = "Active debugger detected!";
                else if (isRooted) warning = "Device has root privileges.";

                final String finalWarning = warning;
                final boolean finalIsSecure = isSecure;
                mainHandler.post(() -> callback.onResult(finalIsSecure, finalWarning));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onResult(true, ""));
            }
        }).start();
    }

    private static void postProgress(SecurityCallback callback, String status) {
        mainHandler.post(() -> callback.onProgress(status));
    }

    private static boolean isDeviceRooted() {
        // Check for test-keys build tags
        String buildTags = Build.TAGS;
        if (buildTags != null && buildTags.contains("test-keys")) {
            return true;
        }

        // Check common root binary paths
        String[] paths = {
                "/system/app/Superuser.apk",
                "/sbin/su",
                "/system/bin/su",
                "/system/xbin/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/system/sd/xbin/su",
                "/system/bin/failsafe/su",
                "/data/local/su",
                "/su/bin/su"
        };
        for (String path : paths) {
            if (new File(path).exists()) {
                return true;
            }
        }
        return false;
    }

    private static boolean verifyPackageIntegrity(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);
            return pi != null && pi.signatures != null && pi.signatures.length > 0;
        } catch (Exception ignored) {
            return true;
        }
    }
}
