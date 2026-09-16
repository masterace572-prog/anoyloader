package top.niunaijun.blackbox.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;
import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.core.env.BEnvironment;
import top.niunaijun.blackbox.utils.FileUtils;
import top.niunaijun.blackbox.utils.TrieTree;

@SuppressLint("SdCardPath")
public class RCore {
    public static final String TAG = "RCore";

    private static final RCore sRCore = new RCore();
    private static final TrieTree mTrieTree = new TrieTree();
    private static final TrieTree sBlackTree = new TrieTree();
    private final Map<String, String> mRedirectMap = new LinkedHashMap<>();

    public static RCore get() {
        return sRCore;
    }

    // /data/data/com.google/  ----->  /data/data/com.virtual/data/com.google/
    public void addRedirect(String origPath, String redirectPath) {
        if (TextUtils.isEmpty(origPath) || TextUtils.isEmpty(redirectPath) || mRedirectMap.get(origPath) != null)
            return;
        //Add the key to TrieTree
        mTrieTree.add(origPath);
        mRedirectMap.put(origPath, redirectPath);
        File redirectFile = new File(redirectPath);
        if (!redirectFile.exists()) {
            FileUtils.mkdirs(redirectPath);
        }
        RNative.addIORule(origPath, redirectPath);
    }

    public void addBlackRedirect(String path) {
        if (TextUtils.isEmpty(path))
            return;
        sBlackTree.add(path);
    }

    public String redirectPath(String path) {
        if (TextUtils.isEmpty(path))
            return path;
        if (path.contains("/SdCard/")) {
            return path;
        }
        String search = sBlackTree.search(path);
        if (!TextUtils.isEmpty(search))
            return search;

        //Search the key from TrieTree
        String key = mTrieTree.search(path);
        if (!TextUtils.isEmpty(key))
            path = path.replace(key, Objects.requireNonNull(mRedirectMap.get(key)));

        return path;
    }

    public File redirectPath(File path) {
        if (path == null)
            return null;
        String pathStr = path.getAbsolutePath();
        return new File(redirectPath(pathStr));
    }

    public String redirectPath(String path, Map<String, String> rule) {
        if (TextUtils.isEmpty(path))
            return path;

        //Search the key from TrieTree
        String key = mTrieTree.search(path);
        if (!TextUtils.isEmpty(key))
            path = path.replace(key, Objects.requireNonNull(rule.get(key)));

        return path;
    }

    public File redirectPath(File path, Map<String, String> rule) {
        if (path == null)
            return null;
        String pathStr = path.getAbsolutePath();
        return new File(redirectPath(pathStr, rule));
    }

    // 由于正常情况Application已完成重定向，以下重定向是怕代码写死。
    public void enableRedirect(Context context) {
        enableRedirect(context, null);
    }

    /**
     * IO redirect setup. {@code context} may be null on Android 16 when
     * {@code createPackageContext} fails for a virtual package; pass
     * {@code fallbackPackageName} from ApplicationInfo in that case.
     */
    public void enableRedirect(Context context, String fallbackPackageName) {
        Map<String, String> rule = new LinkedHashMap<>();
        Set<String> blackRule = new HashSet<>();
        String packageName = null;
        if (context != null) {
            try {
                packageName = context.getPackageName();
            } catch (Throwable ignored) {
            }
        }
        if (TextUtils.isEmpty(packageName)) {
            packageName = fallbackPackageName;
        }
        if (TextUtils.isEmpty(packageName)) {
            try {
                packageName = BActivityThread.getAppPackageName();
            } catch (Throwable ignored) {
            }
        }
        if (TextUtils.isEmpty(packageName)) {
            Log.w(TAG, "enableRedirect skipped: no package name (context=" + context + ")");
            try {
                RNative.enableIO();
            } catch (Throwable ignored) {
            }
            return;
        }

        try {
            ApplicationInfo packageInfo = BlackBoxCore.getBPackageManager().getApplicationInfo(packageName, PackageManager.GET_META_DATA, BActivityThread.getUserId());
            if (packageInfo == null) {
                Log.w(TAG, "enableRedirect: no ApplicationInfo for " + packageName);
                RNative.enableIO();
                return;
            }
            int systemUserId = BlackBoxCore.getHostUserId();
            rule.put(String.format("/data/data/%s/lib", packageName), packageInfo.nativeLibraryDir);
            rule.put(String.format("/data/user/%d/%s/lib", systemUserId, packageName), packageInfo.nativeLibraryDir);

            rule.put(String.format("/data/data/%s", packageName), packageInfo.dataDir);
            rule.put(String.format("/data/user/%d/%s", systemUserId, packageName), packageInfo.dataDir);

            // Ensure ART profile directories exist and redirect both current and reference profiles
            File profilesRoot = new File(BEnvironment.getVBoxRoot(), "profiles");
            FileUtils.mkdirs(profilesRoot.getAbsolutePath());
            // broad redirect as a safety net
            rule.put("/data/misc/profiles", profilesRoot.getAbsolutePath());

            File profilesCurDir = new File(profilesRoot, String.format("cur/%d/%s", BActivityThread.getUserId(), packageName));
            File profilesRefDir = new File(profilesRoot, String.format("ref/%d/%s", BActivityThread.getUserId(), packageName));
            FileUtils.mkdirs(profilesCurDir.getAbsolutePath());
            FileUtils.mkdirs(profilesRefDir.getAbsolutePath());
            rule.put(String.format("/data/misc/profiles/cur/%d/%s", BActivityThread.getUserId(), packageName), profilesCurDir.getAbsolutePath());
            rule.put(String.format("/data/misc/profiles/ref/%d/%s", BActivityThread.getUserId(), packageName), profilesRefDir.getAbsolutePath());

            boolean hasExt = false;
            try {
                hasExt = BlackBoxCore.getContext().getExternalCacheDir() != null;
            } catch (Throwable ignored) {
            }
            if (!hasExt && context != null) {
                try {
                    hasExt = context.getExternalCacheDir() != null;
                } catch (Throwable ignored) {
                }
            }
            if (hasExt) {
                File external = BEnvironment.getExternalStorageDirectory();
               // File external = BEnvironment.getExternalUserDir(BActivityThread.getUserId());

                // sdcard
                rule.put("/sdcard", external.getAbsolutePath());
                rule.put(String.format("/storage/emulated/%d", systemUserId), external.getAbsolutePath());

                blackRule.add("/sdcard/Pictures");
                blackRule.add(String.format("/storage/emulated/%d/Pictures", systemUserId));
            }
            // Prefer ClientConfiguration.setHideRoot(); also honor MetaCore.RemoteManager.sHideRoot
            // which defaults to true. PUBG Global anti-cheat kills the process if su paths are visible.
            boolean shouldHideRoot = false;
            try {
                shouldHideRoot = BlackBoxCore.get().setHideRoot();
            } catch (Throwable ignored) {
            }
            if (!shouldHideRoot) {
                try {
                    shouldHideRoot = android.MetaCore.RemoteManager.sHideRoot;
                } catch (Throwable ignored) {
                }
            }
            if (shouldHideRoot) {
                hideRoot(rule);
            }
            proc(rule);
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (String key : rule.keySet()) {
            get().addRedirect(key, rule.get(key));
        }
        for (String s : blackRule) {
            get().addBlackRedirect(s);
        }
        RNative.enableIO();
    }

    private void hideRoot(Map<String, String> rule) {
        // Classic Magisk / SuperSU paths
        rule.put("/system/app/Superuser.apk", "/system/app/Superuser.apk-fake");
        rule.put("/sbin/su", "/sbin/su-fake");
        rule.put("/system/bin/su", "/system/bin/su-fake");
        rule.put("/system/xbin/su", "/system/xbin/su-fake");
        rule.put("/data/local/xbin/su", "/data/local/xbin/su-fake");
        rule.put("/data/local/bin/su", "/data/local/bin/su-fake");
        rule.put("/system/sd/xbin/su", "/system/sd/xbin/su-fake");
        rule.put("/system/bin/failsafe/su", "/system/bin/failsafe/su-fake");
        rule.put("/data/local/su", "/data/local/su-fake");
        rule.put("/su/bin/su", "/su/bin/su-fake");
        // Magisk / KernelSU / APatch common paths (PUBG Global scans these aggressively)
        rule.put("/sbin/.magisk", "/sbin/.magisk-fake");
        rule.put("/data/adb/magisk", "/data/adb/magisk-fake");
        rule.put("/data/adb/ksu", "/data/adb/ksu-fake");
        rule.put("/data/adb/modules", "/data/adb/modules-fake");
        rule.put("/data/adb/ap", "/data/adb/ap-fake");
        rule.put("/debug_ramdisk", "/debug_ramdisk-fake");
        rule.put("/system/bin/magisk", "/system/bin/magisk-fake");
        rule.put("/system/xbin/magisk", "/system/xbin/magisk-fake");
        rule.put("/system/app/SuperSU", "/system/app/SuperSU-fake");
        rule.put("/system/xbin/daemonsu", "/system/xbin/daemonsu-fake");
        rule.put("/system/etc/init.d/99SuperSUDaemon", "/system/etc/init.d/99SuperSUDaemon-fake");
        rule.put("/dev/com.koushikdutta.superuser.daemon", "/dev/com.koushikdutta.superuser.daemon-fake");
        rule.put("/system/xbin/busybox", "/system/xbin/busybox-fake");
        rule.put("/system/bin/busybox", "/system/bin/busybox-fake");
    }

    private void proc(Map<String, String> rule) {
        int appPid = BActivityThread.getAppPid();
        int pid = Process.myPid();
        String selfProc = "/proc/self/";
        String proc = "/proc/" + pid + "/";

        String cmdline = new File(BEnvironment.getProcDir(appPid), "cmdline").getAbsolutePath();
        rule.put(proc + "cmdline", cmdline);
        rule.put(selfProc + "cmdline", cmdline);
    }
}
