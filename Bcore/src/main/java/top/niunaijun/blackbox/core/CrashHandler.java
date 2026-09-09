package top.niunaijun.blackbox.core;

import android.os.Looper;
import android.util.Log;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;

/**
 * Created by Milk on 4/30/21.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * 此处无Bug
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {
    private static final String TAG = "BBCrashHandler";
    private Thread.UncaughtExceptionHandler mDefaultHandler;

    public static void create() {
        new CrashHandler();
    }

    public CrashHandler() {
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        // Avoid stacking identical wrappers if bindApplication is re-entered.
        if (mDefaultHandler instanceof CrashHandler) {
            mDefaultHandler = ((CrashHandler) mDefaultHandler).mDefaultHandler;
        }
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        if (isSurvivableBackgroundFailure(t, e)) {
            Log.w(TAG, "Survived non-fatal background failure on " + t.getName()
                    + " pkg=" + safePkg() + ": " + e);
            return;
        }
        try {
            if (BlackBoxCore.get().getExceptionHandler() != null) {
                BlackBoxCore.get().getExceptionHandler().uncaughtException(t, e);
            }
        } catch (Throwable ignored) {
        }
        if (mDefaultHandler != null) {
            mDefaultHandler.uncaughtException(t, e);
        }
    }

    /**
     * GMS Dynamite / Play Services BackgroundExecutor often throws ClassCastException when the
     * same obfuscated type is loaded by two ClassLoaders inside a virtual environment (especially
     * Android 14–16). Killing the whole game process for a worker-thread failure is worse than
     * dropping that background task.
     *
     * Matches: ClassCastException "Cannot cast aqhy to adzp" on thread "bgExecutor #N".
     */
    static boolean isSurvivableBackgroundFailure(Thread t, Throwable e) {
        if (e == null) return false;
        try {
            if (Looper.getMainLooper() != null && t == Looper.getMainLooper().getThread()) {
                return false;
            }
        } catch (Throwable ignored) {
        }

        boolean bgNamed = false;
        if (t != null && t.getName() != null) {
            String n = t.getName().toLowerCase();
            bgNamed = n.contains("bgexecutor")
                    || n.contains("backgroundexecutor")
                    || n.contains("googleapi")
                    || n.contains("dynamite")
                    || n.contains("gms-")
                    || n.contains("play-services")
                    || n.contains("imsdk")
                    || n.contains("volley")
                    || n.contains("okhttp")
                    || n.contains("network");
        }

        boolean classCast = false;
        boolean gmsOrBgMarker = false;
        boolean bgExecutorMsg = false;
        boolean apacheMissing = false;

        Throwable cur = e;
        int depth = 0;
        while (cur != null && depth < 8) {
            if (cur instanceof ClassCastException) {
                classCast = true;
            }
            String msg = cur.getMessage();
            if (msg != null) {
                String m = msg.toLowerCase();
                if (m.contains("cannot cast")) classCast = true;
                if (m.contains("backgroundexecutor")) bgExecutorMsg = true;
                if (m.contains("org.apache.http") || m.contains("protocolversion")) apacheMissing = true;
            }
            if (cur instanceof NoClassDefFoundError || cur instanceof ClassNotFoundException) {
                if (msg != null && msg.contains("org.apache.http")) apacheMissing = true;
            }
            StackTraceElement[] st = cur.getStackTrace();
            if (st != null) {
                int limit = Math.min(st.length, 16);
                for (int i = 0; i < limit; i++) {
                    String cn = st[i].getClassName();
                    String file = st[i].getFileName();
                    // Play Services ProGuard source file marker
                    if (file != null && (file.equals("PG") || file.startsWith("PG:"))) {
                        gmsOrBgMarker = true;
                    }
                    if (cn != null) {
                        if (cn.contains("BackgroundExecutor")
                                || cn.contains("Dynamite")
                                || cn.startsWith("com.google.android.gms")
                                || cn.startsWith("com.google.android.gsf")
                                || cn.startsWith("com.google.android.play")) {
                            gmsOrBgMarker = true;
                        }
                    }
                }
            }
            cur = cur.getCause();
            depth++;
        }

        // Require ClassCast (or explicit BackgroundExecutor RuntimeException) + GMS/bg signal.
        if (classCast && (bgNamed || gmsOrBgMarker || bgExecutorMsg)) {
            return true;
        }
        if (bgExecutorMsg && (bgNamed || gmsOrBgMarker)) {
            return true;
        }
        // IMSDK Volley missing apache http on a worker thread — drop the request, keep game alive.
        if (apacheMissing && bgNamed) {
            return true;
        }
        return false;
    }

    private static String safePkg() {
        try {
            String p = BActivityThread.getAppPackageName();
            return p != null ? p : "?";
        } catch (Throwable t) {
            return "?";
        }
    }
}
