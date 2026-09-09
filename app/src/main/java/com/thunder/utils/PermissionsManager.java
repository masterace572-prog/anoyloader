package com.ryzen.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionsManager {
    private static final String TAG = "PermissionsManager";
    public static final int REQ_STORAGE = 101;
    public static final int REQ_STORAGE_ALL_FILES = 101;
    public static final int REQ_STORAGE_RUNTIME = 104;
    public static final int REQ_NOTIFICATION = 102;
    public static final int REQ_INSTALL_UNKNOWN = 103;

    public enum PermissionStep {
        STORAGE_ALL_FILES,
        STORAGE_RUNTIME,
        NOTIFICATION,
        INSTALL_UNKNOWN,
        ALL_GRANTED
    }

    /**
     * Checks if All Files Access (MANAGE_EXTERNAL_STORAGE) is granted on Android 11+ (API 30+).
     */
    public static boolean hasAllFilesAccess(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        return true;
    }

    /**
     * Checks if standard READ_EXTERNAL_STORAGE and WRITE_EXTERNAL_STORAGE runtime permissions
     * are granted. This is required so games running inside the sandbox don't prompt for storage.
     */
    public static boolean hasRuntimeStoragePermissions(Context context) {
        boolean readGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        boolean writeGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            boolean mediaImages = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
            boolean mediaVideo = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED;
            return readGranted || (mediaImages && mediaVideo) || Environment.isExternalStorageManager();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11-12
            return readGranted && writeGranted;
        } else {
            // Android 10 and below
            return readGranted && writeGranted;
        }
    }

    public static boolean hasStoragePermission(Context context) {
        return hasAllFilesAccess(context) && hasRuntimeStoragePermissions(context);
    }

    public static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    public static boolean hasInstallPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return context.getPackageManager().canRequestPackageInstalls();
        }
        return true;
    }

    public static boolean isAllPermissionsGranted(Context context) {
        return hasAllFilesAccess(context) &&
               hasRuntimeStoragePermissions(context) &&
               hasNotificationPermission(context) &&
               hasInstallPermission(context);
    }

    public static PermissionStep getNextMissingPermission(Context context) {
        if (!hasAllFilesAccess(context)) {
            return PermissionStep.STORAGE_ALL_FILES;
        }
        if (!hasRuntimeStoragePermissions(context)) {
            return PermissionStep.STORAGE_RUNTIME;
        }
        if (!hasNotificationPermission(context)) {
            return PermissionStep.NOTIFICATION;
        }
        if (!hasInstallPermission(context)) {
            return PermissionStep.INSTALL_UNKNOWN;
        }
        return PermissionStep.ALL_GRANTED;
    }

    public static String[] getRequiredRuntimeStoragePermissions() {
        List<String> list = new ArrayList<>();
        list.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        list.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.READ_MEDIA_IMAGES);
            list.add(Manifest.permission.READ_MEDIA_VIDEO);
            list.add(Manifest.permission.READ_MEDIA_AUDIO);
        }
        return list.toArray(new String[0]);
    }

    public static void requestAllFilesAccess(Activity activity) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + activity.getPackageName()));
                activity.startActivityForResult(intent, REQ_STORAGE_ALL_FILES);
            } catch (Exception e) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    activity.startActivityForResult(intent, REQ_STORAGE_ALL_FILES);
                } catch (Exception ignored) {}
            }
        } else {
            requestRuntimeStorage(activity);
        }
    }

    public static void requestRuntimeStorage(Activity activity) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        ActivityCompat.requestPermissions(activity, getRequiredRuntimeStoragePermissions(), REQ_STORAGE_RUNTIME);
    }

    public static void requestNotification(Activity activity) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(activity, new String[]{
                    Manifest.permission.POST_NOTIFICATIONS
            }, REQ_NOTIFICATION);
        }
    }

    public static void requestInstallUnknown(Activity activity) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                intent.setData(Uri.parse("package:" + activity.getPackageName()));
                activity.startActivityForResult(intent, REQ_INSTALL_UNKNOWN);
            } catch (Exception e) {
                try {
                    Intent intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
                    activity.startActivityForResult(intent, REQ_INSTALL_UNKNOWN);
                } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Guides the user 1-by-1 to grant the next missing permission.
     */
    public static void requestNextPermission(Activity activity) {
        if (activity.isFinishing() || activity.isDestroyed()) return;

        PermissionStep next = getNextMissingPermission(activity);

        switch (next) {
            case STORAGE_ALL_FILES:
                requestAllFilesAccess(activity);
                break;

            case STORAGE_RUNTIME:
                requestRuntimeStorage(activity);
                break;

            case NOTIFICATION:
                requestNotification(activity);
                break;

            case INSTALL_UNKNOWN:
                requestInstallUnknown(activity);
                break;

            case ALL_GRANTED:
                // Everything granted
                break;
        }
    }

    private static void showExplanationDialog(Activity activity, String title, String message, Runnable onConfirm) {
        if (activity.isFinishing() || activity.isDestroyed()) return;

        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("GRANT PERMISSION", (dialog, which) -> {
                    dialog.dismiss();
                    onConfirm.run();
                })
                .show();
    }

    /**
     * Displays a clean restart prompt when all initial permissions have just been completed.
     */
    public static void showRestartAppDialog(Activity activity) {
        if (activity.isFinishing() || activity.isDestroyed()) return;

        new AlertDialog.Builder(activity)
                .setTitle("Permissions Completed")
                .setMessage("All required permissions have been granted!\n\nPlease restart the app to continue setup.")
                .setCancelable(false)
                .setPositiveButton("RESTART APP", (dialog, which) -> {
                    dialog.dismiss();
                    restartApp(activity);
                })
                .show();
    }

    public static void restartApp(Activity activity) {
        try {
            Intent intent = activity.getPackageManager().getLaunchIntentForPackage(activity.getPackageName());
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                activity.startActivity(intent);
            }
        } catch (Exception ignored) {}
        activity.finishAffinity();
        System.exit(0);
    }
}
