package com.ryzen;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.util.Log;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.configuration.AppLifecycleCallback;
import top.niunaijun.blackbox.app.configuration.ClientConfiguration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Host application + BlackBox virtualization bootstrap.
 * Injects the correct game-specific native library into sandboxed PUBG/BGMI processes.
 */
public class BoxApplication extends Application {

    static {
        try {
            System.loadLibrary("ryzen");
        } catch (UnsatisfiedLinkError error) {
            Log.e("BoxApplication", "ryzen native library is unavailable", error);
        }
    }

    private static final String TAG = "BoxApplication";

    /** All supported PUBG-family package names that receive native injection. */
    private static final String[] TARGET_PACKAGES = {
            "com.pubg.krmobile",   // Korea
            "com.tencent.ig",      // Global
            "com.rekoo.pubgm",     // Taiwan
            "com.vng.pubgmobile",  // Vietnam
            "com.pubg.imobile"     // BGMI (India)
    };

    private static final Set<String> TARGET_PACKAGE_SET =
            new HashSet<>(Arrays.asList(TARGET_PACKAGES));

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Log.d(TAG, "BoxApplication attachBaseContext started");
        try {
            Log.d(TAG, "Initializing BlackBoxCore...");
            BlackBoxCore.get().doAttachBaseContext(base, new ClientConfiguration() {
                @Override
                public String getHostPackageName() {
                    return base.getPackageName();
                }

                @Override
                public boolean isEnableDaemonService() {
                    return false;
                }

                /**
                 * Enable root-path hiding inside the sandbox.
                 * PUBG Global anti-cheat is much stricter than BGMI and will
                 * terminate shortly after launch if /system/bin/su etc. are visible.
                 */
                @Override
                public boolean setHideRoot() {
                    return true;
                }

                @Override
                public boolean requestInstallPackage(File file) {
                    try {
                        base.getPackageManager().getPackageArchiveInfo(file.getAbsolutePath(), 0);
                    } catch (Throwable ignored) {
                    }
                    return false;
                }
            });

            // Ensure MetaCore flags match ClientConfiguration (used by Xposed hide path).
            try {
                BlackBoxCore.setHideRoot(true);
                BlackBoxCore.setHideXposed(true);
                BlackBoxCore.setEnableDaemonService(false);
            } catch (Throwable t) {
                Log.w(TAG, "Unable to set BlackBox hide flags: " + t.getMessage());
            }

            Log.d(TAG, "BlackBoxCore initialization completed (hideRoot=true, hideXposed=true)");
        } catch (Exception e) {
            Log.e(TAG, "Error in attachBaseContext", e);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        com.ryzen.utils.CrashHandler.init(this);
        try {
            BlackBoxCore.get().doCreate();
            Log.d(TAG, "BlackBoxCore initialized successfully (keyless)");
        } catch (Throwable error) {
            Log.e(TAG, "BlackBox SDK startup failed", error);
            return;
        }

        BlackBoxCore.get().addAppLifecycleCallback(new AppLifecycleCallback() {
            @Override
            public void beforeCreateApplication(String packageName, String processName,
                                                Context context, int userId) {
                // Inject as early as possible (before Application object construction).
                // Critical for UE4/Il2Cpp games that load native code during attachBaseContext.
                tryInjectNativeLib(packageName, processName);
            }

            @Override
            public void beforeApplicationOnCreate(String packageName, String processName,
                                                  Application application, int userId) {
                // Safety net: re-attempt if early injection was skipped (e.g. loader dir not ready).
                tryInjectNativeLib(packageName, processName);
            }

            @Override
            public void afterApplicationOnCreate(String packageName, String processName,
                                                 Application application, int userId) {
                // no-op
            }
        });
    }

    /**
     * Returns true when this virtual process belongs to a supported game package.
     * Matches main process (pkg == process) AND child processes (pkg:xxx).
     */
    private static boolean isTargetGameProcess(String packageName, String processName) {
        if (packageName == null || packageName.isEmpty()) return false;
        if (!TARGET_PACKAGE_SET.contains(packageName)) return false;
        if (processName == null || processName.isEmpty()) {
            return true; // package matched; process name unknown — still inject
        }
        // Main process
        if (packageName.equals(processName)) return true;
        // Child processes: com.tencent.ig:push, com.tencent.ig:VgPlay, etc.
        if (processName.startsWith(packageName + ":")) return true;
        return false;
    }

    private static boolean isBgmiPackage(String packageName) {
        return "com.pubg.imobile".equals(packageName);
    }

    private static String libPrefixFor(String packageName) {
        return isBgmiPackage(packageName) ? "libbgmi" : "libpubgm";
    }

    /**
     * Resolve and System.load() the correct game-version native library.
     * Idempotent: tracks already-loaded path so dual lifecycle callbacks are safe.
     */
    private static final Set<String> sLoadedLibs = new HashSet<>();

    private void tryInjectNativeLib(String packageName, String processName) {
        try {
            if (!isTargetGameProcess(packageName, processName)) {
                return;
            }

            File loaderDir = new File(getFilesDir(), "loader");
            if (!loaderDir.exists()) {
                //noinspection ResultOfMethodCallIgnored
                loaderDir.mkdirs();
            }

            int vCode = 0;
            String vName = "";
            try {
                // Prefer host PackageManager so we see the real installed game version
                // even inside the sandbox process.
                PackageInfo pi = getPackageManager().getPackageInfo(packageName, 0);
                if (pi != null) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        vCode = (int) pi.getLongVersionCode();
                    } else {
                        //noinspection deprecation
                        vCode = pi.versionCode;
                    }
                    vName = pi.versionName != null ? pi.versionName : "";
                }
            } catch (Throwable t) {
                Log.w(TAG, "Unable to inspect host package info for " + packageName + ": " + t.getMessage());
            }

            String cleanVer = vName.replace(".", "").trim();
            String expectedPrefix = libPrefixFor(packageName);

            // Read active library configuration written by loader (MAct.prepareActiveLibForGame)
            File activeConfigFile = new File(loaderDir, "active_lib_for_" + packageName + ".txt");
            String assignedLibName = "";
            if (activeConfigFile.exists() && activeConfigFile.isFile()) {
                try (BufferedReader br = new BufferedReader(new FileReader(activeConfigFile))) {
                    assignedLibName = br.readLine();
                    if (assignedLibName != null) assignedLibName = assignedLibName.trim();
                    else assignedLibName = "";
                } catch (Throwable t) {
                    Log.w(TAG, "Failed reading active_lib config: " + t.getMessage());
                }
            }

            List<File> candidates = new ArrayList<>();

            // 1. Explicitly assigned library from config (highest priority)
            if (assignedLibName != null && !assignedLibName.isEmpty()) {
                if (assignedLibName.toLowerCase(Locale.US).startsWith(expectedPrefix)) {
                    candidates.add(new File(loaderDir, assignedLibName));
                } else {
                    Log.w(TAG, "Assigned lib " + assignedLibName
                            + " does not match expected prefix " + expectedPrefix + " — skipped");
                }
            }

            // 2. Active symlink/copy prepared by MAct
            candidates.add(new File(loaderDir, expectedPrefix + "_active.so"));

            // 3. Version-specific names
            if (vCode > 0) {
                candidates.add(new File(loaderDir, expectedPrefix + "_" + vCode + ".so"));
            }
            if (!cleanVer.isEmpty()) {
                candidates.add(new File(loaderDir, expectedPrefix + cleanVer + ".so"));
                candidates.add(new File(loaderDir, expectedPrefix + "_v" + cleanVer + ".so"));
            }
            if (!vName.isEmpty()) {
                candidates.add(new File(loaderDir, expectedPrefix + "_" + vName + ".so"));
            }

            // 4. Generic real library last (libbgmi.so / libpubgm.so)
            candidates.add(new File(loaderDir, expectedPrefix + ".so"));

            // For non-BGMI international builds also accept alternate naming some packs use
            if (!isBgmiPackage(packageName)) {
                candidates.add(new File(loaderDir, "libpubg.so"));
                candidates.add(new File(loaderDir, "libUE4.so")); // never preferred; only if nothing else
            }

            File selectedLib = null;
            for (File candidate : candidates) {
                if (candidate == null || !candidate.exists() || !candidate.isFile() || candidate.length() < 100) {
                    continue;
                }
                String candName = candidate.getName().toLowerCase(Locale.US);

                // Forbid cross-game library injection (BGMI lib into PUBG GL and vice-versa)
                boolean prefixOk = candName.startsWith(expectedPrefix)
                        || (!isBgmiPackage(packageName) && (candName.startsWith("libpubg") || candName.equals("libue4.so")));
                if (!prefixOk) {
                    continue;
                }

                // Forbid loading 450 library on 460 version or vice-versa
                if (!cleanVer.isEmpty() && candName.contains("450") && cleanVer.startsWith("460")) {
                    Log.e(TAG, "STRICT VERSION REJECTION: Refusing 450 lib (" + candName
                            + ") for running " + vName);
                    continue;
                }
                if (!cleanVer.isEmpty() && candName.contains("460") && cleanVer.startsWith("450")) {
                    Log.e(TAG, "STRICT VERSION REJECTION: Refusing 460 lib (" + candName
                            + ") for running " + vName);
                    continue;
                }

                // Never load a BGMI-named file into a Global process (and reverse)
                if (!isBgmiPackage(packageName) && candName.startsWith("libbgmi")) {
                    continue;
                }
                if (isBgmiPackage(packageName) && candName.startsWith("libpubgm")) {
                    continue;
                }

                selectedLib = candidate;
                break;
            }

            if (selectedLib == null) {
                Log.e(TAG, "CRITICAL: No matching native lib for " + packageName
                        + " process=" + processName + " [v" + vName + " (" + vCode + ")]. "
                        + "Expected prefix=" + expectedPrefix);
                return;
            }

            String loadKey = packageName + "|" + selectedLib.getAbsolutePath();
            synchronized (sLoadedLibs) {
                if (sLoadedLibs.contains(loadKey)) {
                    Log.d(TAG, "Lib already loaded in this process: " + selectedLib.getName());
                    return;
                }
            }

            try {
                // Ensure executable bit (some extract paths strip it)
                //noinspection ResultOfMethodCallIgnored
                selectedLib.setReadable(true, false);
                //noinspection ResultOfMethodCallIgnored
                selectedLib.setExecutable(true, false);
            } catch (Throwable ignored) {
            }

            try {
                System.load(selectedLib.getAbsolutePath());
                synchronized (sLoadedLibs) {
                    sLoadedLibs.add(loadKey);
                }
                Log.i(TAG, "STRICT LIB INJECTION: loaded " + selectedLib.getName()
                        + " (" + selectedLib.length() + " bytes) into "
                        + packageName + "/" + processName
                        + " [v" + vName + " (" + vCode + ")]");
            } catch (UnsatisfiedLinkError linkError) {
                Log.e(TAG, "UnsatisfiedLinkError loading " + selectedLib.getAbsolutePath()
                        + ": " + linkError.getMessage(), linkError);
            }
        } catch (Throwable e) {
            Log.e(TAG, "Error in tryInjectNativeLib", e);
        }
    }
}
