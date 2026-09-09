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
            report.append(com.ryzen.BrandConfig.crashReportHeader()).append("\n");
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

        boolean main = false;
        try {
            main = Looper.getMainLooper() != null && thread == Looper.getMainLooper().getThread();
        } catch (Throwable ignored) {
        }

        String tname = thread != null && thread.getName() != null ? thread.getName().toLowerCase() : "";
        boolean gameCritical = main
                || tname.contains("unity")
                || tname.contains("ue4")
                || tname.contains("unreal")
                || tname.contains("renderthread")
                || tname.contains("glthread")
                || tname.contains("choreographer")
                || tname.contains("game thread")
                || tname.contains("gamethread");

        boolean bgNamed = tname.contains("bgexecutor")
                || tname.contains("backgroundexecutor")
                || tname.contains("blockingexecutor")
                || tname.contains("googleapi")
                || tname.contains("dynamite")
                || tname.contains("gms-")
                || tname.contains("play-services")
                || tname.contains("firebase")
                || tname.startsWith("android.bg")
                || tname.contains("binder:")
                || tname.contains("imsdk")
                || tname.contains("volley")
                || tname.contains("okhttp")
                || tname.contains("network")
                || tname.contains("pool-")
                || tname.contains("executor")
                || tname.contains("async")
                || tname.contains("chromium")
                || tname.contains("measurement")
                || tname.contains("checkin")
                || tname.contains("vending")
                || tname.contains("workmanager")
                || tname.contains("defaultdispatcher")
                || tname.contains("queued-work");

        boolean classCast = false, npe = false, security = false, gms = false, blackbox = false;
        boolean apache = false, restrictions = false, config = false, clientTx = false;
        boolean resourcesNpe = false, remote = false, runtime = false;

        Throwable cur = throwable;
        int depth = 0;
        while (cur != null && depth < 10) {
            if (cur instanceof ClassCastException) classCast = true;
            if (cur instanceof NullPointerException) npe = true;
            if (cur instanceof SecurityException) security = true;
            if (cur instanceof RuntimeException) runtime = true;
            String cnSimple = cur.getClass().getName();
            if (cnSimple.contains("RemoteException") || cnSimple.contains("DeadSystem")
                    || cnSimple.contains("DeadObject") || cnSimple.contains("TransactionTooLarge")) {
                remote = true;
            }
            if (cur instanceof NoClassDefFoundError || cur instanceof ClassNotFoundException) {
                String msg0 = cur.getMessage();
                if (msg0 != null && msg0.contains("org.apache.http")) apache = true;
            }
            String msg = cur.getMessage();
            if (msg != null) {
                String m = msg.toLowerCase();
                if (m.contains("cannot cast")) classCast = true;
                if (m.contains("org.apache.http") || m.contains("protocolversion")) apache = true;
                if (m.contains("application restrictions") || m.contains("only system may")) {
                    restrictions = true;
                    security = true;
                }
                if (m.contains("getresources()") || m.contains("getpackagename()")) resourcesNpe = true;
                if (m.contains("unable to bind to service") || m.contains("unable to makeapplication")) {
                    blackbox = true;
                }
                if (m.contains("backgroundexecutor") || m.contains("blockingexecutor")) gms = true;
            }
            StackTraceElement[] st = cur.getStackTrace();
            if (st != null) {
                int limit = Math.min(st.length, 24);
                for (int i = 0; i < limit; i++) {
                    String cn = st[i].getClassName();
                    String file = st[i].getFileName();
                    if (file != null && file.startsWith("PG")) gms = true;
                    if (cn != null) {
                        if (cn.startsWith("com.google.android.gms")
                                || cn.startsWith("com.google.android.gsf")
                                || cn.startsWith("com.google.android.play")
                                || cn.startsWith("com.android.vending")
                                || cn.contains("BackgroundExecutor")
                                || cn.contains("BlockingExecutor")
                                || cn.contains("Dynamite")
                                || cn.contains("Firebase")) {
                            gms = true;
                        }
                        if (cn.startsWith("top.niunaijun.blackbox")
                                || cn.startsWith("com.ogcheats")
                                || cn.startsWith("com.ryzen")) {
                            blackbox = true;
                        }
                        if (cn.contains("ConfigurationChange") || cn.contains("ConfigurationController")) {
                            config = true;
                        }
                        if (cn.contains("ClientTransaction") || cn.contains("TransactionExecutor")) {
                            clientTx = true;
                        }
                        if (cn.contains("RestrictionsManager")) restrictions = true;
                    }
                }
            }
            cur = cur.getCause();
            depth++;
        }

        if (!gameCritical) {
            if (bgNamed || gms || blackbox || security || restrictions || apache || classCast || remote) {
                return true;
            }
            if (bgNamed && (runtime || npe)) return true;
            return false;
        }

        // Main/game critical: only known false fatals
        if (restrictions || (security && gms)) return true;
        if (npe && (config || clientTx || resourcesNpe)) return true;
        if (npe && blackbox && gms) return true;
        if (runtime && blackbox && (msgHas(throwable, "Unable to bind to service")
                || msgHas(throwable, "Unable to makeApplication"))) {
            return true;
        }
        return false;
    }

    private static boolean msgHas(Throwable e, String needle) {
        Throwable cur = e;
        int d = 0;
        while (cur != null && d < 8) {
            if (cur.getMessage() != null && cur.getMessage().contains(needle)) return true;
            cur = cur.getCause();
            d++;
        }
        return false;
    }
}
