package com.ryzen.utils;

import android.content.Context;
import android.os.Build;
import android.os.Process;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

/** Persist a bounded private report, then let Android terminate the failed process. */
public class CrashHandler implements Thread.UncaughtExceptionHandler {
    private static final String TAG = "CrashHandler";
    private final File reportDirectory;
    private final String appVersion;
    private final Thread.UncaughtExceptionHandler defaultHandler;
    private final AtomicBoolean handling = new AtomicBoolean();

    private CrashHandler(Context context, Thread.UncaughtExceptionHandler previous) {
        // Resolve before a crash; avoid binder/Activity launches from a dying process.
        reportDirectory = new File(context.getFilesDir(), "crash-reports");
        reportDirectory.mkdirs();
        String version;
        try {
            version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (Exception unavailable) {
            version = "unknown";
        }
        appVersion = version;
        defaultHandler = previous;
    }

    public static void init(Context context) {
        Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        if (!(previous instanceof CrashHandler)) {
            Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(context, previous));
        }
    }

    @Override
    public void uncaughtException(Thread thread, Throwable failure) {
        try {
            if (handling.compareAndSet(false, true)) {
                Log.e(TAG, "Fatal exception on " + thread.getName(), failure);
                StringWriter trace = new StringWriter();
                failure.printStackTrace(new PrintWriter(trace));
                String report = com.ryzen.BrandConfig.crashReportHeader() + "\n"
                        + "Time: " + new java.util.Date() + "\n"
                        + "App: " + appVersion + "\n"
                        + "Device: " + Build.MANUFACTURER + " " + Build.MODEL + "\n"
                        + "Android: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")\n"
                        + "PID: " + Process.myPid() + " Thread: " + thread.getName() + "\n\n" + trace;
                byte[] bytes = report.getBytes(StandardCharsets.UTF_8);
                // Keep at most 5 reports, and never pass a large stack through Binder extras.
                File[] old = reportDirectory.listFiles((dir, name) -> name.endsWith(".txt"));
                if (old != null && old.length >= 5) {
                    java.util.Arrays.sort(old, java.util.Comparator.comparingLong(File::lastModified));
                    for (int i = 0; i <= old.length - 5; i++) old[i].delete();
                }
                File destination = new File(reportDirectory, System.currentTimeMillis() + "-" + Process.myPid() + ".txt");
                try (FileOutputStream out = new FileOutputStream(destination)) {
                    out.write(bytes, 0, Math.min(bytes.length, 128 * 1024));
                }
            }
        } catch (Throwable reportingFailure) {
            Log.e(TAG, "Unable to save crash report", reportingFailure);
        } finally {
            if (defaultHandler != null) defaultHandler.uncaughtException(thread, failure);
            else Process.killProcess(Process.myPid());
        }
    }
}
