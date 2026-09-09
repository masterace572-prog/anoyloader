package com.ryzen.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
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
}
