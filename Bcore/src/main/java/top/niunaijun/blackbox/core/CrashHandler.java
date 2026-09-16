package top.niunaijun.blackbox.core;

import android.os.Looper;
import android.util.Log;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;

/**
 * Virtual-process uncaught handler. Mid-session PUBG/GMS worker failures must not
 * tear down the whole game process on Android 14–16.
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
        // Also keep main Looper from dying on swallowed exceptions after return.
        try {
            installMainLooperGuard();
        } catch (Throwable ignored) {
        }
    }

    private static void installMainLooperGuard() {
        // No-op placeholder: default UEH already covers main. Kept for future
        // MessageQueue.mNextBarrierToken hooks if needed.
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        if (isSurvivableFailure(t, e)) {
            Log.w(TAG, "Survived non-fatal failure on " + (t != null ? t.getName() : "?")
                    + " pkg=" + safePkg() + ": " + e);
            // Keep the thread alive when possible (worker threads die anyway after return,
            // but we must not kill the process).
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
     * Survive sandbox/GMS/Play noise. Never swallow crashes on Unity/UE4/GL/main
     * unless the stack is a known Android-16 system/GMS false fatal.
     */
    public static boolean isSurvivableFailure(Thread t, Throwable e) {
        if (e == null) return false;

        boolean main = false;
        try {
            main = Looper.getMainLooper() != null && t == Looper.getMainLooper().getThread();
        } catch (Throwable ignored) {
        }

        boolean gameCritical = main || isGameCriticalThread(t);
        FailureSignature sig = analyze(e);

        // Worker / binder / pool / GMS executor: broad survival.
        if (!gameCritical) {
            if (sig.gmsOrPlay || sig.blackbox || sig.security || sig.classCast
                    || sig.apacheMissing || sig.restrictions || sig.npe
                    || sig.classNotFound || sig.remoteOrDead || sig.inflater
                    || isBgNamed(t)) {
                // Any failure on a clearly background/named pool thread with sandbox markers.
                if (isBgNamed(t) || sig.gmsOrPlay || sig.blackbox || sig.security
                        || sig.restrictions || sig.apacheMissing || sig.classCast
                        || sig.remoteOrDead) {
                    return true;
                }
            }
            // Pool threads with RuntimeException wrapping the above
            if (isBgNamed(t) && (sig.runtime || sig.npe || sig.illegalState)) {
                return true;
            }
            return false;
        }

        // Main / Unity / GL: only known-safe system false fatals.
        if (sig.restrictions || sig.security && sig.gmsOrPlay) {
            return true;
        }
        // ConfigurationChange NPE on null Application context (Samsung API 36)
        if (sig.npe && (sig.configChange || sig.clientTransaction || sig.getResourcesNpe)) {
            return true;
        }
        // makeApplication / bindApplication residual NPEs on main during GMS process spin-up
        if (sig.npe && sig.blackbox && (sig.gmsOrPlay || msgContains(e, "makeApplication")
                || msgContains(e, "enableRedirect") || msgContains(e, "createPackageContext"))) {
            return true;
        }
        // ProxyService bind failures already fixed; still survive if they bubble
        if (sig.runtime && msgContains(e, "Unable to bind to service") && sig.blackbox) {
            return true;
        }
        if (sig.runtime && msgContains(e, "Unable to makeApplication") && sig.blackbox) {
            return true;
        }
        return false;
    }

    /** @deprecated use {@link #isSurvivableFailure} */
    public static boolean isSurvivableBackgroundFailure(Thread t, Throwable e) {
        return isSurvivableFailure(t, e);
    }

    private static boolean isGameCriticalThread(Thread t) {
        if (t == null || t.getName() == null) return false;
        String n = t.getName().toLowerCase();
        return n.contains("unity")
                || n.contains("ue4")
                || n.contains("unreal")
                || n.contains("game thread")
                || n.contains("gamethread")
                || n.contains("renderthread")
                || n.contains("glthread")
                || n.contains("choreographer")
                || n.equals("main");
    }

    private static boolean isBgNamed(Thread t) {
        if (t == null || t.getName() == null) return false;
        String n = t.getName().toLowerCase();
        return n.contains("bgexecutor")
                || n.contains("backgroundexecutor")
                || n.contains("blockingexecutor")
                || n.contains("googleapi")
                || n.contains("dynamite")
                || n.contains("gms-")
                || n.contains("gms ")
                || n.contains("play-services")
                || n.contains("firebase")
                || n.startsWith("android.bg")
                || n.contains("binder:")
                || n.contains("binder_")
                || n.contains("imsdk")
                || n.contains("volley")
                || n.contains("okhttp")
                || n.contains("network")
                || n.contains("pool-")
                || n.contains("async")
                || n.contains("executor")
                || n.contains("threadpool")
                || n.contains("scheduled")
                || n.contains("chromium")
                || n.contains("process reaper")
                || n.contains("finalizer")
                || n.contains("referencequeue")
                || n.contains("queued-work")
                || n.contains("wm.")
                || n.contains("hwui")
                || n.startsWith("defaultdispatcher")
                || n.contains("kotlinx.coroutines")
                || n.contains("arch_disk")
                || n.contains("leakcanary")
                || n.contains("fileobserver")
                || n.contains("connectivity")
                || n.contains("job.worker")
                || n.contains("workmanager")
                || n.contains("measurement")
                || n.contains("scion")
                || n.contains("phenotype")
                || n.contains("checkin")
                || n.contains("vending");
    }

    private static boolean msgContains(Throwable e, String needle) {
        Throwable cur = e;
        int d = 0;
        while (cur != null && d < 8) {
            String m = cur.getMessage();
            if (m != null && m.contains(needle)) return true;
            cur = cur.getCause();
            d++;
        }
        return false;
    }

    private static final class FailureSignature {
        boolean classCast;
        boolean npe;
        boolean security;
        boolean runtime;
        boolean illegalState;
        boolean classNotFound;
        boolean apacheMissing;
        boolean restrictions;
        boolean gmsOrPlay;
        boolean blackbox;
        boolean configChange;
        boolean clientTransaction;
        boolean getResourcesNpe;
        boolean remoteOrDead;
        boolean inflater;
    }

    private static FailureSignature analyze(Throwable e) {
        FailureSignature s = new FailureSignature();
        Throwable cur = e;
        int depth = 0;
        while (cur != null && depth < 10) {
            if (cur instanceof ClassCastException) s.classCast = true;
            if (cur instanceof NullPointerException) s.npe = true;
            if (cur instanceof SecurityException) s.security = true;
            if (cur instanceof RuntimeException) s.runtime = true;
            if (cur instanceof IllegalStateException || cur instanceof IllegalArgumentException) {
                s.illegalState = true;
            }
            if (cur instanceof NoClassDefFoundError || cur instanceof ClassNotFoundException) {
                s.classNotFound = true;
            }
            // DeadSystemRuntimeException / RemoteException
            String cnSimple = cur.getClass().getName();
            if (cnSimple.contains("RemoteException")
                    || cnSimple.contains("DeadSystem")
                    || cnSimple.contains("DeadObject")
                    || cnSimple.contains("TransactionTooLarge")) {
                s.remoteOrDead = true;
            }

            String msg = cur.getMessage();
            if (msg != null) {
                String m = msg.toLowerCase();
                if (m.contains("cannot cast")) s.classCast = true;
                if (m.contains("backgroundexecutor") || m.contains("blockingexecutor")) {
                    s.gmsOrPlay = true;
                }
                if (m.contains("org.apache.http") || m.contains("protocolversion")) {
                    s.apacheMissing = true;
                }
                if (m.contains("application restrictions")
                        || m.contains("only system may")
                        || m.contains("getapplicationrestrictions")) {
                    s.restrictions = true;
                    s.security = true;
                }
                if (m.contains("getresources()") || m.contains("getpackagename()")) {
                    s.getResourcesNpe = true;
                }
                if (m.contains("interact_across_users") || m.contains("across users")) {
                    s.security = true;
                }
            }

            StackTraceElement[] st = cur.getStackTrace();
            if (st != null) {
                int limit = Math.min(st.length, 24);
                for (int i = 0; i < limit; i++) {
                    String cn = st[i].getClassName();
                    String file = st[i].getFileName();
                    String method = st[i].getMethodName();
                    if (file != null && (file.equals("PG") || file.startsWith("PG"))) {
                        s.gmsOrPlay = true;
                    }
                    if (cn != null) {
                        if (cn.startsWith("com.google.android.gms")
                                || cn.startsWith("com.google.android.gsf")
                                || cn.startsWith("com.google.android.play")
                                || cn.startsWith("com.android.vending")
                                || cn.contains("BackgroundExecutor")
                                || cn.contains("BlockingExecutor")
                                || cn.contains("Dynamite")
                                || cn.contains("Firebase")
                                || cn.contains("phenotype")
                                || cn.contains("measurement")) {
                            s.gmsOrPlay = true;
                        }
                        if (cn.startsWith("top.niunaijun.blackbox")
                                || cn.startsWith("com.ogcheats")
                                || cn.startsWith("com.ryzen")) {
                            s.blackbox = true;
                        }
                        if (cn.contains("ConfigurationChange")
                                || cn.contains("ConfigurationController")
                                || (method != null && method.contains("Configuration"))) {
                            s.configChange = true;
                        }
                        if (cn.contains("ClientTransaction")
                                || cn.contains("TransactionExecutor")) {
                            s.clientTransaction = true;
                        }
                        if (cn.contains("RestrictionsManager")
                                || cn.contains("IRestrictionsManager")) {
                            s.restrictions = true;
                        }
                        if (cn.contains("RemoteException") || cn.contains("Binder")) {
                            // weak signal
                        }
                        if (cn.contains("InflateException") || cn.contains("LayoutInflater")) {
                            s.inflater = true;
                        }
                    }
                }
            }
            cur = cur.getCause();
            depth++;
        }
        return s;
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
