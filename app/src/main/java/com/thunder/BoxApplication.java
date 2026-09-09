package com.ryzen;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.util.Log;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.configuration.AppLifecycleCallback;
import top.niunaijun.blackbox.app.configuration.ClientConfiguration;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import com.ryzen.utils.Prefs;


public class BoxApplication extends Application {

static {
        try {
            System.loadLibrary("ryzen");
        } catch (UnsatisfiedLinkError error) {
            Log.e("BoxApplication", "ryzen native library is unavailable", error);
        }
    }

    private static final String TAG = "BoxApplication";
    private final String[] process_names = {
            "com.pubg.krmobile",   // KOREA - 1
            "com.tencent.ig",      // GLOBAL - 2
            "com.rekoo.pubgm",     // TAIWAN - 3
            "com.vng.pubgmobile",  // VIETNAM - 4
            "com.pubg.imobile"     // BGMI - 5
    };

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Log.d(TAG, "BoxApplication attachBaseContext started");
        Prefs prefs = new Prefs(base);
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

               @Override
                public boolean requestInstallPackage(File file){
                    PackageInfo packageInfo = base.getPackageManager().getPackageArchiveInfo(file.getAbsolutePath(),0);
                    return false;
                }
            });
            // Hide-root and hide-Xposed APIs are not present in the bundled BlackBoxCore version.
            Log.d(TAG, "BlackBoxCore initialization completed");
            Log.d(TAG, "App name check completed");
        } catch (Exception e) {
            Log.e(TAG, "Error in attachBaseContext", e);
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        com.ryzen.utils.CrashHandler.init(this);
        try {
            BlackBoxCore.get().doCreate();
            // BCore operates keyless without license requirement
            Log.d(TAG, "BlackBoxCore initialized successfully (keyless)");
        } catch (Throwable error) {
            Log.e(TAG, "BlackBox SDK startup failed", error);
            return;
        }
        BlackBoxCore.get().addAppLifecycleCallback(new AppLifecycleCallback() {
            @Override
            public void beforeCreateApplication(String packageName, String processName, Context context, int userId) {
                // Handle before application creation
            }

            @Override
            public void beforeApplicationOnCreate(String packageName, String processName, Application application, int userId) {
                try {
                    for (String pkg : process_names) {
                        if (pkg.equals(packageName) && pkg.equals(processName)) {
                            File loaderDir = new File(getFilesDir(), "loader");
                            if (!loaderDir.exists()) loaderDir.mkdirs();

                            int vCode = 0;
                            String vName = "";
                            try {
                                PackageInfo pi = application.getPackageManager().getPackageInfo(packageName, 0);
                                if (pi != null) {
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                                        vCode = (int) pi.getLongVersionCode();
                                    } else {
                                        vCode = pi.versionCode;
                                    }
                                    vName = pi.versionName != null ? pi.versionName : "";
                                }
                            } catch (Throwable t) {
                                Log.w(TAG, "Unable to inspect package info for " + packageName + ": " + t.getMessage());
                            }

                            String cleanVer = vName.replace(".", "");

                            // Read active library configuration written by loader
                            File activeConfigFile = new File(loaderDir, "active_lib_for_" + packageName + ".txt");
                            String assignedLibName = "";
                            if (activeConfigFile.exists() && activeConfigFile.isFile()) {
                                try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(activeConfigFile))) {
                                    assignedLibName = br.readLine();
                                    if (assignedLibName != null) assignedLibName = assignedLibName.trim();
                                } catch (Throwable t) {
                                    Log.w(TAG, "Failed reading active_lib config: " + t.getMessage());
                                }
                            }

                            // Candidate library resolution strictly bound to running package & version
                            List<File> candidates = new ArrayList<>();
                            String expectedPrefix = pkg.equals("com.pubg.imobile") ? "libbgmi" : "libpubgm";

                            // 1. Explicitly assigned library from config
                            if (assignedLibName != null && !assignedLibName.isEmpty()) {
                                if (assignedLibName.startsWith(expectedPrefix)) {
                                    candidates.add(new File(loaderDir, assignedLibName));
                                } else {
                                    Log.w(TAG, "Assigned lib " + assignedLibName + " does not match expected prefix " + expectedPrefix);
                                }
                            }

                            // 2. Strict candidates prioritizing real lib files
                            if (pkg.equals("com.pubg.imobile")) {
                                candidates.add(new File(loaderDir, "libbgmi.so"));
                                candidates.add(new File(loaderDir, "libbgmi_active.so"));
                                if (vCode > 0) {
                                    candidates.add(new File(loaderDir, "libbgmi_" + vCode + ".so"));
                                }
                                if (!cleanVer.isEmpty()) {
                                    candidates.add(new File(loaderDir, "libbgmi" + cleanVer + ".so"));
                                    candidates.add(new File(loaderDir, "libbgmi_v" + cleanVer + ".so"));
                                }
                                if (!vName.isEmpty()) {
                                    candidates.add(new File(loaderDir, "libbgmi_" + vName + ".so"));
                                }
                            } else if (pkg.equals("com.tencent.ig")) {
                                candidates.add(new File(loaderDir, "libpubgm.so"));
                                candidates.add(new File(loaderDir, "libpubgm_active.so"));
                                if (vCode > 0) {
                                    candidates.add(new File(loaderDir, "libpubgm_" + vCode + ".so"));
                                }
                                if (!cleanVer.isEmpty()) {
                                    candidates.add(new File(loaderDir, "libpubgm" + cleanVer + ".so"));
                                    candidates.add(new File(loaderDir, "libpubgm_v" + cleanVer + ".so"));
                                }
                                if (!vName.isEmpty()) {
                                    candidates.add(new File(loaderDir, "libpubgm_" + vName + ".so"));
                                }
                            }

                            // Strict validation: check existence and forbid version mismatches
                            File selectedLib = null;
                            for (File candidate : candidates) {
                                if (candidate != null && candidate.exists() && candidate.isFile() && candidate.length() >= 100) {
                                    String candName = candidate.getName().toLowerCase();
                                    // Security: Forbid cross-game library injection
                                    if (!candName.startsWith(expectedPrefix)) {
                                        continue;
                                    }
                                    // Security: Forbid loading 450 library on 460 version or vice-versa
                                    if (!cleanVer.isEmpty() && candName.contains("450") && cleanVer.equals("460")) {
                                        Log.e(TAG, "STRICT VERSION REJECTION: Refusing to load 450 lib (" + candName + ") for running 4.6.0 version!");
                                        continue;
                                    }
                                    if (!cleanVer.isEmpty() && candName.contains("460") && cleanVer.equals("450")) {
                                        Log.e(TAG, "STRICT VERSION REJECTION: Refusing to load 460 lib (" + candName + ") for running 4.5.0 version!");
                                        continue;
                                    }
                                    selectedLib = candidate;
                                    break;
                                }
                            }

                            if (selectedLib != null) {
                                try {
                                    System.load(selectedLib.getAbsolutePath());
                                    Log.i(TAG, "STRICT LIB INJECTION: Successfully injected " + selectedLib.getName() + " (" + selectedLib.length() + " bytes) for " + packageName + " [v" + vName + " (" + vCode + ")]");
                                } catch (UnsatisfiedLinkError linkError) {
                                    Log.e(TAG, "UnsatisfiedLinkError loading " + selectedLib.getAbsolutePath() + ": " + linkError.getMessage(), linkError);
                                }
                            } else {
                                Log.e(TAG, "CRITICAL: Strict library validation prevented loading mismatched library for " + packageName + " [v" + vName + " (" + vCode + ")]. Required versioned library missing!");
                            }

                            break;
                        }
                    }
                } catch (Throwable e) {
                    Log.e(TAG, "Error in beforeApplicationOnCreate", e);
                }
            }

            @Override
            public void afterApplicationOnCreate(String packageName, String processName, Application application, int userId) {
                // Handle after application onCreate
            }
        });
    }
}