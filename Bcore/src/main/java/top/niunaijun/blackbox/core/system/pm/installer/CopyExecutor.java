package top.niunaijun.blackbox.core.system.pm.installer;


import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.core.env.BEnvironment;
import top.niunaijun.blackbox.core.system.pm.BPackageSettings;
import top.niunaijun.blackbox.entity.pm.InstallOption;
import top.niunaijun.blackbox.utils.FileUtils;
import top.niunaijun.blackbox.utils.NativeUtils;

/**
 * Created by Milk on 4/24/21.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * 此处无Bug
 * 拷贝文件相关
 *
 * Extended to pull native .so libraries from ALL split APKs (base + config.arm64_v8a + …).
 * Play Store builds of PUBG Global ship native code only inside ABI splits — copying
 * solely from base.apk leaves the sandbox without libUE4 / libgcloud / etc. and the
 * game crashes a few seconds after launch.
 */
public class CopyExecutor implements Executor {
    private static final String TAG = "CopyExecutor";

    @Override
    public int exec(BPackageSettings ps, InstallOption option, int userId) {
        try {
            File libDir = BEnvironment.getAppLibDir(ps.pkg.packageName);
            File[] splitSources = collectApkSources(ps);
            if (splitSources.length > 0) {
                NativeUtils.copyNativeLibFromSplits(splitSources, libDir);
            } else if (!option.isFlag(InstallOption.FLAG_SYSTEM)) {
                NativeUtils.copyNativeLib(new File(ps.pkg.baseCodePath), libDir);
            } else {
                // FLAG_SYSTEM: still try host ApplicationInfo splits so arm64 libs are present
                NativeUtils.copyNativeLibFromSplits(splitSources, libDir);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Do not hard-fail install if native copy partially fails — base APK may still run.
            Log.w(TAG, "Native lib copy warning for " + ps.pkg.packageName + ": " + e.getMessage());
        }
        if (option.isFlag(InstallOption.FLAG_STORAGE)) {
            // 外部安装
            File origFile = new File(ps.pkg.baseCodePath);
            File newFile = BEnvironment.getBaseApkDir(ps.pkg.packageName);
            try {
                if (option.isFlag(InstallOption.FLAG_URI_FILE)) {
                    boolean b = FileUtils.renameTo(origFile, newFile);
                    if (!b) {
                        FileUtils.copyFile(origFile, newFile);
                    }
                } else {
                    FileUtils.copyFile(origFile, newFile);
                }
                // update baseCodePath
                ps.pkg.baseCodePath = newFile.getAbsolutePath();
            } catch (IOException e) {
                e.printStackTrace();
                return -1;
            }
        } else if (option.isFlag(InstallOption.FLAG_SYSTEM)) {
            // 系统安装 — keep pointing at the host base APK (and splits via ApplicationInfo)
        }
        return 0;
    }

    /**
     * Collect base + split APK file paths for the package being installed.
     * Prefers live host ApplicationInfo (has splitSourceDirs) then falls back to baseCodePath.
     */
    private static File[] collectApkSources(BPackageSettings ps) {
        List<File> files = new ArrayList<>();
        try {
            ApplicationInfo ai = null;
            // Prefer the ApplicationInfo already attached during FLAG_SYSTEM install
            if (ps.pkg.applicationInfo != null) {
                ai = ps.pkg.applicationInfo;
            }
            if (ai == null || ai.sourceDir == null) {
                try {
                    ai = BlackBoxCore.getPackageManager()
                            .getApplicationInfo(ps.pkg.packageName, 0);
                } catch (PackageManager.NameNotFoundException ignored) {
                }
            }
            if (ai != null) {
                if (ai.sourceDir != null) {
                    files.add(new File(ai.sourceDir));
                }
                if (ai.splitSourceDirs != null) {
                    for (String split : ai.splitSourceDirs) {
                        if (split != null) files.add(new File(split));
                    }
                }
                if (ai.publicSourceDir != null
                        && (ai.sourceDir == null || !ai.publicSourceDir.equals(ai.sourceDir))) {
                    files.add(new File(ai.publicSourceDir));
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "collectApkSources host lookup failed: " + t.getMessage());
        }
        if (files.isEmpty() && ps.pkg.baseCodePath != null) {
            files.add(new File(ps.pkg.baseCodePath));
        }
        return files.toArray(new File[0]);
    }
}
