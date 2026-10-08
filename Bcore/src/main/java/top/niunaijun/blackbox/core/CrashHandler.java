package top.niunaijun.blackbox.core;

import android.os.Process;
import android.util.Log;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;

/** Reports fatal virtual-process failures; an uncaught handler cannot resume a dead thread. */
public class CrashHandler implements Thread.UncaughtExceptionHandler {
    private static final String TAG = "BBCrashHandler";
    private final Thread.UncaughtExceptionHandler mDefaultHandler;

    public static void create() {
        if (!(Thread.getDefaultUncaughtExceptionHandler() instanceof CrashHandler)) {
            new CrashHandler();
        }
    }

    public CrashHandler() {
        Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        mDefaultHandler = previous instanceof CrashHandler
                ? ((CrashHandler) previous).mDefaultHandler : previous;
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable failure) {
        Log.e(TAG, "Fatal virtual process failure; pkg=" + safePkg(), failure);
        try {
            Thread.UncaughtExceptionHandler observer = BlackBoxCore.get().getExceptionHandler();
            if (observer != null && observer != this && observer != mDefaultHandler) {
                observer.uncaughtException(thread, failure);
            }
        } catch (Throwable reportingFailure) {
            Log.e(TAG, "Exception observer failed", reportingFailure);
        } finally {
            // Returning here would leave a dead main/render thread and a zombie process.
            if (mDefaultHandler != null) {
                mDefaultHandler.uncaughtException(thread, failure);
            } else {
                Process.killProcess(Process.myPid());
            }
        }
    }

    /** Compatibility entry points. Fatal failures must be recovered at their source. */
    public static boolean isSurvivableFailure(Thread thread, Throwable failure) { return false; }
    public static boolean isSurvivableBackgroundFailure(Thread thread, Throwable failure) { return false; }

    private static String safePkg() {
        try { return BActivityThread.getAppPackageName(); }
        catch (Throwable ignored) { return "unknown"; }
    }
}
