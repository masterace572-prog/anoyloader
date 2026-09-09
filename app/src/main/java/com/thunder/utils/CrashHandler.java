package com.ryzen.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Looper;
import android.os.Process;
import android.util.Log;

import com.ryzen.CrashActivity;

import java.io.PrintWriter;
import java.io.StringWriter;

public class CrashHandler implements Thread.UncaughtExceptionHandler {
    private static final String TAG = "CrashHandler";
    private final Context context;
    private final Thread.UncaughtExceptionHandler defaultHandler;

    private CrashHandler(Context context, Thread.UncaughtExceptionHandler defaultHandler) {
        this.context = context.getApplicationContext();
        this.defaultHandler = defaultHandler;
    }

    public static void init(Context context) {
        Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        if (!(defaultHandler instanceof CrashHandler)) {
            Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(context, defaultHandler));
            Log.d(TAG, "Global CrashHandler successfully initialized");
        }
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        // GMS Dynamite / BackgroundExecutor ClassCast on worker threads must not kill the game.
        if (isSurvivableBackgroundFailure(thread, throwable)) {
            Log.w(TAG, "Survived non-fatal background failure on " + thread.getName() + ": " + throwable);
            return;
        }

        try {
            Log.e(TAG, "Uncaught exception trapped in thread " + thread.getName(), throwable);

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            String stackTrace = sw.toString();

            StringBuilder report = new StringBuilder();
            report.append("Anoy Loader Crash Report\n");
            report.append("========================\n");
            report.append("Device: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\n");
            report.append("Android: ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
            report.append("Thread: ").append(thread.getName()).append("\n\n");
            report.append("Exception: ").append(throwable.getClass().getName()).append(": ").append(throwable.getMessage()).append("\n\n");
            report.append("Stack Trace:\n").append(stackTrace);

            Intent intent = new Intent(context, CrashActivity.class);
            intent.putExtra("error_message", throwable.getMessage() != null ? throwable.getMessage() : throwable.getClass().getSimpleName());
            intent.putExtra("stack_trace", report.toString());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            context.startActivity(intent);
        } catch (Throwable t) {
            Log.e(TAG, "Error inside CrashHandler", t);
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable);
                return;
            }
        }

        // Clean process termination to avoid system ANR or zombie process
        Process.killProcess(Process.myPid());
        System.exit(10);
    }

    /**
     * Survive GMS Play Services BackgroundExecutor ClassCastException (dual ClassLoader Dynamite)
     * on non-main threads. Matches the crash signature seen on Samsung Android 16 with PUBG Global.
     */
    static boolean isSurvivableBackgroundFailure(Thread thread, Throwable throwable) {
        if (throwable == null) return false;
        try {
            if (Looper.getMainLooper() != null && thread == Looper.getMainLooper().getThread()) {
                return false;
            }
        } catch (Throwable ignored) {
            // If Looper is unavailable, still try signature match below.
        }

        boolean bgThread = false;
        if (thread != null && thread.getName() != null) {
            String n = thread.getName().toLowerCase();
            bgThread = n.contains("bgexecutor")
                    || n.contains("backgroundexecutor")
                    || n.contains("googleapi")
                    || n.contains("dynamite")
                    || n.contains("gms-")
                    || n.contains("play-services")
                    || n.startsWith("android.bg")
                    || n.contains("binder:");
        }

        Throwable cur = throwable;
        int depth = 0;
        boolean classCast = false;
        boolean gmsStack = false;
        boolean bgExecutorMsg = false;
        while (cur != null && depth < 8) {
            if (cur instanceof ClassCastException) classCast = true;
            String msg = cur.getMessage();
            if (msg != null) {
                String m = msg.toLowerCase();
                if (m.contains("cannot cast") || m.contains("classcastexception")) classCast = true;
                if (m.contains("backgroundexecutor")) bgExecutorMsg = true;
            }
            StackTraceElement[] st = cur.getStackTrace();
            if (st != null) {
                int limit = Math.min(st.length, 12);
                for (int i = 0; i < limit; i++) {
                    String cn = st[i].getClassName();
                    String file = st[i].getFileName();
                    if (file != null && file.startsWith("PG")) gmsStack = true;
                    if (cn != null && (cn.startsWith("com.google.android.gms")
                            || cn.startsWith("com.google.android.gsf")
                            || cn.startsWith("com.google.android.play")
                            || cn.contains("BackgroundExecutor")
                            || cn.contains("Dynamite"))) {
                        gmsStack = true;
                    }
                }
            }
            cur = cur.getCause();
            depth++;
        }

        if (classCast && (bgThread || gmsStack || bgExecutorMsg)) return true;
        if (bgExecutorMsg && (bgThread || gmsStack)) return true;
        return false;
    }
}
