package com.ryzen.utils;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.ryzen.R;

import org.lsposed.lsparanoid.Obfuscate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.entity.pm.InstallResult;
import top.niunaijun.blackbox.core.env.BEnvironment;



import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Obfuscate
public class AppManager {
    private final Context ctx;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Action codes
    public static final int INSTALL_APP = 1;
    public static final int COPY_OBB = 3;

    public interface CopyCallback {
        void onCopyCompleted(boolean success);
    }

    public AppManager(Context ctx) {
        this.ctx = ctx;
    }

    public void appManager(String pkg, int method) {
        Dialog[] progressDialogHolder = new Dialog[1];
        ProgressBar[] progressBarHolder = new ProgressBar[1];
        TextView[] progressTextHolder = new TextView[1];

        if (method == COPY_OBB && ctx instanceof Activity && !((Activity) ctx).isFinishing() && !((Activity) ctx).isDestroyed()) {
            Dialog progressDialog = new Dialog(ctx);
            progressDialog.setCancelable(false);
            progressDialogHolder[0] = progressDialog;

            progressBarHolder[0] = progressDialog.findViewById(R.id.progressBar);
            progressTextHolder[0] = progressDialog.findViewById(R.id.progressText);

            if (progressBarHolder[0] != null) {
                progressBarHolder[0].setMax(100);
                progressBarHolder[0].setProgress(0);
            }
            if (progressTextHolder[0] != null) {
                progressTextHolder[0].setText("0/100");
            }
            try {
                progressDialog.show();
            } catch (Exception ignored) {}
        }

        executor.execute(() -> {
            String[] errorMsg = new String[]{""};
            boolean success = false;
            if (method == COPY_OBB) {
                success = copyObbFolderInternal(pkg, errorMsg, percent -> {
                    mainHandler.post(() -> {
                        if (progressBarHolder[0] != null) progressBarHolder[0].setProgress(percent);
                        if (progressTextHolder[0] != null) progressTextHolder[0].setText(percent + "/100");
                    });
                });
            } else if (method == INSTALL_APP) {
                success = installAppInternal(pkg, errorMsg);
            }

            final boolean finalSuccess = success;
            final String finalError = errorMsg[0];

            mainHandler.post(() -> {
                if (progressDialogHolder[0] != null && progressDialogHolder[0].isShowing()) {
                    try {
                        progressDialogHolder[0].dismiss();
                    } catch (Exception ignored) {}
                }
                showResultDialog(method, finalSuccess, finalError);
            });
        });
    }

    private interface ProgressCallback {
        void onProgress(int percent);
    }

    private boolean copyObbFolderInternal(String packageName, String[] outError, ProgressCallback progress) {
        File sourceDir = new File(android.os.Environment.getExternalStorageDirectory(), "Android/obb/" + packageName);
        if (!sourceDir.exists()) {
            sourceDir = new File("/storage/emulated/0/Android/obb/", packageName);
        }
        File destDir = BEnvironment.getExternalObbDir(packageName);

        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            outError[0] = "OBB not found!";
            return false;
        }

        if (!destDir.exists() && !destDir.mkdirs()) {
            outError[0] = "Destination directory creation failed!";
            return false;
        }

        File[] files = sourceDir.listFiles();
        if (files == null || files.length == 0) {
            outError[0] = "No files found to copy!";
            return false;
        }

        long totalBytes = 0, copiedBytes = 0;
        for (File file : files) {
            totalBytes += file.length();
        }

        try {
            byte[] buffer = new byte[8192];
            for (File file : files) {
                InputStream inputStream = new FileInputStream(file);
                FileOutputStream outputStream = new FileOutputStream(new File(destDir, file.getName()));

                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    copiedBytes += bytesRead;
                    if (totalBytes > 0 && progress != null) {
                        progress.onProgress((int) ((copiedBytes * 100) / totalBytes));
                    }
                }

                inputStream.close();
                outputStream.close();
            }
            return true;
        } catch (IOException e) {
            outError[0] = "Error copying files: " + e.getMessage();
            return false;
        }
    }

    private boolean installAppInternal(String pkg, String[] outError) {
        try {
            PackageInfo packageInfo = ctx.getPackageManager().getPackageInfo(pkg, 0);
            String apkPath = packageInfo.applicationInfo.sourceDir;
            InstallResult result = BlackBoxCore.get().installPackageAsUser(apkPath, 0);
            if (!result.success) {
                outError[0] = "Installation failed: " + result.msg;
                return false;
            }
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            outError[0] = "Package not found: " + e.getMessage();
            return false;
        } catch (Exception e) {
            outError[0] = "Installation error: " + e.getMessage();
            return false;
        }
    }

    private void showResultDialog(int method, boolean success, String errorMessage) {
        if (ctx instanceof Activity && (((Activity) ctx).isFinishing() || ((Activity) ctx).isDestroyed())) {
            return;
        }
        try {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(ctx);
            builder.setTitle(success ? "Success" : "Error")
                    .setMessage(success ?
                            (method == INSTALL_APP ? "App installed successfully." : "OBB copied successfully.") :
                            errorMessage)
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();
        } catch (Exception ignored) {}
    }

    public boolean checkObbContainer(String pkg) {
        File destDir = BEnvironment.getExternalObbDir(pkg);
        return destDir.exists() && destDir.isDirectory() && destDir.list() != null && destDir.list().length > 0;
    }

    public void copyObbFolderAsync(final String packageName, final CopyCallback callback) {
        if (checkObbContainer(packageName)) {
            if (callback != null) {
                callback.onCopyCompleted(true);
            }
            return;
        }

        executor.execute(() -> {
            String[] err = new String[]{""};
            boolean result = copyObbFolderInternal(packageName, err, null);
            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onCopyCompleted(result);
                }
            });
        });
    }
}