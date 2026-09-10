package top.niunaijun.blackbox.app;

import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.app.Service;
import android.app.job.JobService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Build;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Looper;
import android.os.RemoteException;
import android.os.StrictMode;
import android.text.TextUtils;
import android.util.Log;
import android.MetaCore.RemoteManager;
import android.webkit.WebView;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import black.android.app.ActivityThreadAppBindDataContext;
import black.android.app.BRActivity;
import black.android.app.BRActivityManagerNative;
import black.android.app.BRActivityThread;
import black.android.app.BRActivityThreadActivityClientRecord;
import black.android.app.BRActivityThreadAppBindData;
import black.android.app.BRActivityThreadNMR1;
import black.android.app.BRActivityThreadQ;
import black.android.app.BRContextImpl;
import black.android.app.BRLoadedApk;
import black.android.app.BRService;
import black.android.content.BRBroadcastReceiver;
import black.android.content.BRContentProviderClient;
import black.android.graphics.BRCompatibility;
import black.android.security.net.config.BRNetworkSecurityConfigProvider;
import black.com.android.internal.content.BRReferrerIntent;
import black.dalvik.system.BRVMRuntime;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.configuration.AppLifecycleCallback;
import top.niunaijun.blackbox.app.dispatcher.AppServiceDispatcher;
import top.niunaijun.blackbox.core.CrashHandler;
import top.niunaijun.blackbox.utils.compat.ApacheHttpLegacyCompat;
import top.niunaijun.blackbox.core.IBActivityThread;
import top.niunaijun.blackbox.core.RCore;
import top.niunaijun.blackbox.core.RNative;
import top.niunaijun.blackbox.core.env.VirtualRuntime;
import top.niunaijun.blackbox.core.system.user.BUserHandle;
import top.niunaijun.blackbox.entity.AppConfig;
import top.niunaijun.blackbox.entity.am.ReceiverData;
import top.niunaijun.blackbox.entity.pm.InstalledModule;
import top.niunaijun.blackbox.fake.delegate.AppInstrumentation;
import top.niunaijun.blackbox.fake.delegate.ContentProviderDelegate;
import top.niunaijun.blackbox.fake.frameworks.BXposedManager;
import top.niunaijun.blackbox.fake.hook.HookManager;
import top.niunaijun.blackbox.fake.service.HCallbackStub;
import top.niunaijun.blackbox.utils.Reflector;
import top.niunaijun.blackbox.utils.Slog;
import top.niunaijun.blackbox.utils.compat.ActivityManagerCompat;
import top.niunaijun.blackbox.utils.compat.BuildCompat;
import top.niunaijun.blackbox.utils.compat.ContextCompat;
import top.niunaijun.blackbox.utils.compat.StrictModeCompat;
import org.lsposed.lsparanoid.Obfuscate;

@Obfuscate
public class BActivityThread extends IBActivityThread.Stub {
    public static final String TAG = "BActivityThread";
    private static final Object mConfigLock = new Object();
    private static volatile BActivityThread sBActivityThread;
    private AppConfig mAppConfig;
    private AppBindData mBoundApplication;
    private Application mInitialApplication;
    private final List<ProviderInfo> mProviders = new ArrayList<>();
    private final Handler mH = BlackBoxCore.get().getHandler();

    public static class AppBindData {
        ApplicationInfo appInfo;
        Object info;
        String processName;
        List<ProviderInfo> providers;
    }

    public static boolean isThreadInit() {
        return sBActivityThread != null;
    }

    public static BActivityThread currentActivityThread() {
        if (sBActivityThread == null) {
            synchronized (BActivityThread.class) {
                if (sBActivityThread == null) {
                    sBActivityThread = new BActivityThread();
                }
            }
        }
        return sBActivityThread;
    }

    public static AppConfig getAppConfig() {
        synchronized (mConfigLock) {
            return currentActivityThread().mAppConfig;
        }
    }

    public static List<ProviderInfo> getProviders() {
        return currentActivityThread().mProviders;
    }

    public static String getAppProcessName() {
        if (getAppConfig() != null) return getAppConfig().processName;
        if (currentActivityThread().mBoundApplication != null) return currentActivityThread().mBoundApplication.processName;
        return null;
    }

    public static String getAppPackageName() {
        if (getAppConfig() != null) return getAppConfig().packageName;
        if (currentActivityThread().mInitialApplication != null) return currentActivityThread().mInitialApplication.getPackageName();
        return null;
    }

    public static Application getApplication() {
        return currentActivityThread().mInitialApplication;
    }

    public static int getAppPid() {
        return getAppConfig() == null ? -1 : getAppConfig().bpid;
    }

    public static int getBUid() {
        return getAppConfig() == null ? BUserHandle.AID_APP_START : getAppConfig().buid;
    }

    public static int getBAppId() {
        return BUserHandle.getAppId(BlackBoxCore.getHostUid());
    }

    public static int getCallingBUid() {
        return getAppConfig() == null ? BlackBoxCore.getHostUid() : getAppConfig().callingBUid;
    }

    public static int getUid() {
        return getAppConfig() == null ? -1 : getAppConfig().uid;
    }

    public static int getUserId() {
        return getAppConfig() == null ? 0 : getAppConfig().userId;
    }

    public void initProcess(AppConfig appConfig) {
        synchronized (mConfigLock) {
            if (this.mAppConfig != null && !this.mAppConfig.packageName.equals(appConfig.packageName)) {
                throw new RuntimeException("reject init process: " + appConfig.processName + ", this process is : " + this.mAppConfig.processName);
            }
            this.mAppConfig = appConfig;
            final IBinder iBinder = asBinder();
            try {
                iBinder.linkToDeath(new IBinder.DeathRecipient() {
                        @Override
                        public void binderDied() {
                            synchronized (BActivityThread.mConfigLock) {
                                try {
                                    iBinder.linkToDeath(this, 0);
                                } catch (RemoteException e) {
                                    // ignore
                                }
                                BActivityThread.this.mAppConfig = null;
                            }
                        }
                    }, 0);

            } catch (RemoteException e) {
                Log.e(TAG, "error", e);
            }
        }
    }

    public boolean isInit() {
        return this.mBoundApplication != null;
    }

    public Service createService(ServiceInfo serviceInfo, IBinder token) {
        if (serviceInfo == null || serviceInfo.name == null) {
            return null;
        }
        try {
            if (!isInit()) {
                bindApplication(serviceInfo.packageName, serviceInfo.processName);
            }
        } catch (Throwable bindErr) {
            Log.e(TAG, "bindApplication for service failed: " + serviceInfo.name, bindErr);
            return null;
        }
        try {
            Class<?> clazz = loadServiceClass(serviceInfo);
            if (clazz == null) {
                Slog.w(TAG, "Service class not found (skipped): " + serviceInfo.name
                        + " pkg=" + serviceInfo.packageName);
                return null;
            }
            Service service = (Service) clazz.newInstance();
            Context context = null;
            try {
                context = BlackBoxCore.getContext().createPackageContext(
                        serviceInfo.packageName,
                        Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
            } catch (Throwable ignored) {
            }
            if (context == null) {
                context = BlackBoxCore.getContext();
            }
            if (context == null) {
                Slog.w(TAG, "No context for service " + serviceInfo.name);
                return null;
            }
            BRContextImpl.get(context).setOuterContext(service);
            BRService.get(service).attach(
                    context,
                    BlackBoxCore.mainThread(),
                    serviceInfo.name,
                    token,
                    this.mInitialApplication,
                    BRActivityManagerNative.get().getDefault());
            ContextCompat.fix(context);
            service.onCreate();
            return service;
        } catch (Throwable e) {
            // Never throw: ProxyService.onBind runs on the host main thread.
            // GMS/Play often request services (e.g. CheckinService) that are not
            // present in the current package ClassLoader (vending vs gms).
            Log.e(TAG, "Unable to create service " + serviceInfo.name + " (returning null)", e);
            return null;
        }
    }

    public JobService createJobService(ServiceInfo serviceInfo) {
        if (serviceInfo == null || serviceInfo.name == null) {
            return null;
        }
        try {
            if (!isInit()) {
                bindApplication(serviceInfo.packageName, serviceInfo.processName);
            }
        } catch (Throwable bindErr) {
            Log.e(TAG, "bindApplication for job service failed: " + serviceInfo.name, bindErr);
            return null;
        }
        try {
            Class<?> clazz = loadServiceClass(serviceInfo);
            if (clazz == null || !JobService.class.isAssignableFrom(clazz)) {
                Slog.w(TAG, "JobService class not found (skipped): " + serviceInfo.name);
                return null;
            }
            JobService service = (JobService) clazz.newInstance();
            Context context = null;
            try {
                context = BlackBoxCore.getContext().createPackageContext(
                        serviceInfo.packageName,
                        Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
            } catch (Throwable ignored) {
            }
            if (context == null) {
                context = BlackBoxCore.getContext();
            }
            if (context == null) {
                Slog.w(TAG, "No context for job service " + serviceInfo.name);
                return null;
            }
            BRContextImpl.get(context).setOuterContext(service);
            BRService.get(service).attach(
                    context,
                    BlackBoxCore.mainThread(),
                    serviceInfo.name,
                    getActivityThread(),
                    this.mInitialApplication,
                    BRActivityManagerNative.get().getDefault());
            ContextCompat.fix(context);
            service.onCreate();
            service.onBind(null);
            return service;
        } catch (Throwable e) {
            Log.e(TAG, "Unable to create JobService " + serviceInfo.name + " (returning null)", e);
            return null;
        }
    }

    /**
     * Resolve a Service/JobService class. Prefer the bound app ClassLoader; on
     * ClassNotFoundException try the declared service package (GMS services are
     * often requested while the process is bound as Play Store / game).
     */
    private Class<?> loadServiceClass(ServiceInfo serviceInfo) {
        String name = serviceInfo.name;
        ClassLoader primary = null;
        try {
            if (this.mBoundApplication != null && this.mBoundApplication.info != null) {
                primary = BRLoadedApk.get(this.mBoundApplication.info).getClassLoader();
            }
        } catch (Throwable ignored) {
        }
        if (primary != null) {
            try {
                return primary.loadClass(name);
            } catch (ClassNotFoundException ignored) {
            } catch (Throwable t) {
                Slog.w(TAG, "primary loadClass failed for " + name + ": " + t.getMessage());
            }
        }
        // Cross-package: e.g. com.android.vending process binding GMS CheckinService
        try {
            if (serviceInfo.packageName != null
                    && (getAppPackageName() == null
                    || !serviceInfo.packageName.equals(getAppPackageName()))) {
                Context other = BlackBoxCore.getContext().createPackageContext(
                        serviceInfo.packageName,
                        Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
                if (other != null && other.getClassLoader() != null) {
                    return other.getClassLoader().loadClass(name);
                }
            }
        } catch (ClassNotFoundException ignored) {
        } catch (Throwable t) {
            Slog.w(TAG, "cross-pkg loadClass failed for " + name + ": " + t.getMessage());
        }
        // Last resort: host / current initial application loader
        try {
            if (this.mInitialApplication != null && this.mInitialApplication.getClassLoader() != null) {
                return this.mInitialApplication.getClassLoader().loadClass(name);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public void bindApplication(final String packageName, final String processName) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            final ConditionVariable conditionVariable = new ConditionVariable();
            BlackBoxCore.get().getHandler().post(() -> {
                handleBindApplication(packageName, processName);
                conditionVariable.open();
            });
            conditionVariable.block();
        } else {
            handleBindApplication(packageName, processName);
        }
    }

    public synchronized void handleBindApplication(String packageName, String processName) {
        if (isInit())
            return;
        try {
            CrashHandler.create();
        } catch (Throwable ignored) {
        }
        Binder.clearCallingIdentity();
        PackageInfo packageInfo = BlackBoxCore.getBPackageManager().getPackageInfo(packageName, PackageManager.GET_PROVIDERS, BActivityThread.getUserId());
        if (packageInfo == null || packageInfo.applicationInfo == null) {
            throw new RuntimeException("Unable to get PackageInfo for " + packageName);
        }
        ApplicationInfo applicationInfo = packageInfo.applicationInfo;
        if (packageInfo.providers == null) {
            packageInfo.providers = new ProviderInfo[]{};
        }
        mProviders.addAll(Arrays.asList(packageInfo.providers));
        Object boundApplication = BRActivityThread.get(BlackBoxCore.mainThread()).mBoundApplication();
        Context packageContext = createPackageContext(applicationInfo);
        if (packageContext == null) {
            // Android 16 (esp. Samsung): createPackageContext can fail for virtual
            // packages. Fall back to host context so bind can continue.
            Slog.w(TAG, "createPackageContext failed for " + packageName
                    + "; falling back to host context");
            packageContext = BlackBoxCore.getContext();
        }
        if (packageContext == null) {
            throw new RuntimeException("Unable to create package context for " + packageName);
        }
        Object loadedApk = null;
        try {
            loadedApk = BRContextImpl.get(packageContext).mPackageInfo();
        } catch (Throwable t) {
            Slog.w(TAG, "mPackageInfo missing on context: " + t.getMessage());
        }
        // If we fell back to host context, LoadedApk is the host's — try to obtain
        // a proper LoadedApk for the virtual package via ActivityThread.
        if (loadedApk == null || isHostLoadedApk(loadedApk, packageName)) {
            Object alt = peekLoadedApk(packageName, applicationInfo);
            if (alt != null) {
                loadedApk = alt;
            }
        }
        if (loadedApk == null) {
            throw new RuntimeException("Unable to obtain LoadedApk for " + packageName);
        }
        try {
            BRLoadedApk.get(loadedApk)._set_mSecurityViolation(false);
            // fix applicationInfo
            BRLoadedApk.get(loadedApk)._set_mApplicationInfo(applicationInfo);
        } catch (Throwable t) {
            Slog.w(TAG, "LoadedApk ApplicationInfo fix failed: " + t.getMessage());
        }
        int targetSdkVersion = applicationInfo.targetSdkVersion;
        if (targetSdkVersion < Build.VERSION_CODES.GINGERBREAD) {
            StrictMode.ThreadPolicy newPolicy = new StrictMode.ThreadPolicy.Builder(StrictMode.getThreadPolicy()).permitNetwork().build();
            StrictMode.setThreadPolicy(newPolicy);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (targetSdkVersion < Build.VERSION_CODES.N) {
                StrictModeCompat.disableDeathOnFileUriExposure();
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                WebView.setDataDirectorySuffix(getUserId() + ":" + packageName + ":" + processName);
            } catch (Throwable wv) {
                Slog.w(TAG, "WebView.setDataDirectorySuffix failed: " + wv.getMessage());
            }
        }
        
        VirtualRuntime.setupRuntime(processName, applicationInfo);
        try {
            BRVMRuntime.get(BRVMRuntime.get().getRuntime()).setTargetSdkVersion(applicationInfo.targetSdkVersion);
        } catch (Throwable ignored) {
        }
        if (BuildCompat.isS()) {
            try {
                BRCompatibility.get().setTargetSdkVersion(applicationInfo.targetSdkVersion);
            } catch (Throwable ignored) {
            }
        }
        try {
            RNative.init(Build.VERSION.SDK_INT);
        } catch (Throwable rn) {
            Slog.w(TAG, "RNative.init failed: " + rn.getMessage());
        }
        try {
            RCore.get().enableRedirect(packageContext, packageName);
        } catch (Throwable redir) {
            Slog.w(TAG, "enableRedirect failed: " + redir.getMessage());
        }
        AppBindData bindData = new AppBindData();
        bindData.appInfo = applicationInfo;
        bindData.processName = processName;
        bindData.info = loadedApk;
        bindData.providers = mProviders;
        ActivityThreadAppBindDataContext activityThreadAppBindData = BRActivityThreadAppBindData.get(boundApplication);
        activityThreadAppBindData._set_instrumentationName(new ComponentName(bindData.appInfo.packageName, Instrumentation.class.getName()));
        activityThreadAppBindData._set_appInfo(bindData.appInfo);
        activityThreadAppBindData._set_info(bindData.info);
        activityThreadAppBindData._set_processName(bindData.processName);
        activityThreadAppBindData._set_providers(bindData.providers);
        mBoundApplication = bindData;
        //ssl适配
        try {
            if (BRNetworkSecurityConfigProvider.getRealClass() != null && packageContext != null) {
                Security.removeProvider("AndroidNSSP");
                BRNetworkSecurityConfigProvider.get().install(packageContext);
            }
        } catch (Throwable sslErr) {
            Slog.w(TAG, "NetworkSecurityConfigProvider.install failed: " + sslErr.getMessage());
        }
        Application application = null;
        try {
            // IMSDK Volley HurlStack needs org.apache.http.ProtocolVersion (removed from boot
            // classpath since Android 10). Inject framework legacy jar into the app ClassLoader.
            try {
                ClassLoader appCl = null;
                try {
                    appCl = BRLoadedApk.get(loadedApk).getClassLoader();
                } catch (Throwable ignored) {
                }
                if (appCl == null && packageContext != null) {
                    try {
                        appCl = packageContext.getClassLoader();
                    } catch (Throwable ignored) {
                    }
                }
                ApacheHttpLegacyCompat.ensureLoaded(appCl);
            } catch (Throwable apacheErr) {
                Slog.w(TAG, "Apache HTTP legacy inject failed: " + apacheErr.getMessage());
            }
            onBeforeCreateApplication(packageName, processName, packageContext);

            application = createApplicationRobust(loadedApk, packageContext, applicationInfo, packageName);

            if (application == null) {
                // Last-ditch: empty Application so ActivityThread config changes
                // (ClientTransactionListenerController) do not NPE on null mInitialApplication.
                Slog.w(TAG, "createApplicationRobust returned null for " + packageName
                        + "; using bare Application stub");
                application = new Application();
                try {
                    Method attach = Application.class.getDeclaredMethod("attach", Context.class);
                    attach.setAccessible(true);
                    Context base = packageContext != null ? packageContext : BlackBoxCore.getContext();
                    attach.invoke(application, base);
                } catch (Throwable attachErr) {
                    Slog.w(TAG, "Application.attach failed: " + attachErr.getMessage());
                }
            }

            try {
                if (application.getClassLoader() != null) {
                    ApacheHttpLegacyCompat.ensureLoaded(application.getClassLoader());
                }
            } catch (Throwable ignored) {
            }
            try {
                ContextCompat.fix(application);
            } catch (Throwable ignored) {
            }
            try {
                Object sysCtx = BRActivityThread.get(BlackBoxCore.mainThread()).getSystemContext();
                if (sysCtx instanceof Context) {
                    ContextCompat.fix((Context) sysCtx);
                }
            } catch (Throwable ignored) {
            }
            mInitialApplication = application;
            // Set on ActivityThread BEFORE onCreate so concurrent ConfigurationChange
            // items on Samsung API 36 see a non-null Application context.
            try {
                BRActivityThread.get(BlackBoxCore.mainThread())._set_mInitialApplication(mInitialApplication);
            } catch (Throwable setAppErr) {
                Slog.w(TAG, "set mInitialApplication failed: " + setAppErr.getMessage());
            }
            // Also stash on LoadedApk if empty
            try {
                if (BRLoadedApk.get(loadedApk).mApplication() == null) {
                    try {
                        Field f = loadedApk.getClass().getDeclaredField("mApplication");
                        f.setAccessible(true);
                        f.set(loadedApk, application);
                    } catch (Throwable ignored) {
                    }
                }
            } catch (Throwable ignored) {
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && "com.tencent.mm:recovery".equals(processName)) {
                try {
                    fixWeChatRecovery(mInitialApplication);
                } catch (Throwable ignored) {
                }
            }
            try {
                installProviders(mInitialApplication, bindData.processName, bindData.providers);
            } catch (Throwable provErr) {
                Slog.w(TAG, "installProviders failed: " + provErr.getMessage());
            }
            try {
                new WebView(mInitialApplication).destroy();
            } catch (Throwable e) {
                // ignore
            }
            try {
                fixAiLiaoPhoto(mInitialApplication);
            } catch (Throwable e) {
                // ignore
            }
            try {
                onBeforeApplicationOnCreate(packageName, processName, application);
            } catch (Throwable ignored) {
            }
            try {
                AppInstrumentation.get().callApplicationOnCreate(application);
            } catch (Throwable onCreateErr) {
                Slog.w(TAG, "callApplicationOnCreate failed: " + onCreateErr.getMessage());
                try {
                    application.onCreate();
                } catch (Throwable ignored) {
                }
            }
            try {
                onAfterApplicationOnCreate(packageName, processName, application);
            } catch (Throwable ignored) {
            }
            try {
                HookManager.get().checkEnv(HCallbackStub.class);
            } catch (Throwable ignored) {
            }
            try {
                // GMS may replace the default UEH during init; put ours back.
                CrashHandler.create();
            } catch (Throwable ignored) {
            }
        } catch (Throwable e) {
            e.printStackTrace();
            // Prefer a degraded but alive process over killing the host main thread.
            if (mInitialApplication == null && application != null) {
                mInitialApplication = application;
                try {
                    BRActivityThread.get(BlackBoxCore.mainThread())._set_mInitialApplication(application);
                } catch (Throwable ignored) {
                }
            }
            if (mInitialApplication == null) {
                Application stub = new Application();
                try {
                    Method attach = Application.class.getDeclaredMethod("attach", Context.class);
                    attach.setAccessible(true);
                    Context base = packageContext != null ? packageContext : BlackBoxCore.getContext();
                    if (base != null) {
                        attach.invoke(stub, base);
                    }
                } catch (Throwable ignored) {
                }
                mInitialApplication = stub;
                try {
                    BRActivityThread.get(BlackBoxCore.mainThread())._set_mInitialApplication(stub);
                } catch (Throwable ignored) {
                }
                Slog.w(TAG, "makeApplication recovered with stub Application for " + packageName
                        + " after: " + e.getMessage());
            } else {
                Slog.w(TAG, "makeApplication partial failure for " + packageName + ": " + e.getMessage());
            }
            // Do NOT rethrow — GMS/vending secondary processes must not crash the host.
        }
    }

    /**
     * Try several strategies to instantiate the virtual app Application.
     * Android 16 / Samsung often returns null from LoadedApk.makeApplication when
     * the LoadedApk was obtained via fallback paths.
     */
    private Application createApplicationRobust(Object loadedApk, Context packageContext,
                                                ApplicationInfo applicationInfo, String packageName) {
        // 1) Standard makeApplication
        try {
            Application app = BRLoadedApk.get(loadedApk).makeApplication(false, null);
            if (app != null) return app;
        } catch (Throwable th) {
            Slog.w(TAG, "makeApplication(false,null) failed: " + th.getMessage());
        }
        // 2) Force with instrumentation
        try {
            Instrumentation instr = AppInstrumentation.get();
            Application app = BRLoadedApk.get(loadedApk).makeApplication(false, instr);
            if (app != null) return app;
        } catch (Throwable th) {
            Slog.w(TAG, "makeApplication(false,instr) failed: " + th.getMessage());
        }
        // 3) makeApplication forceDefaultAppClass
        try {
            Application app = BRLoadedApk.get(loadedApk).makeApplication(true, null);
            if (app != null) return app;
        } catch (Throwable th) {
            Slog.w(TAG, "makeApplication(true,null) failed: " + th.getMessage());
        }
        // 4) Instrumentation.newApplication
        try {
            ClassLoader cl = null;
            try {
                cl = BRLoadedApk.get(loadedApk).getClassLoader();
            } catch (Throwable ignored) {
            }
            if (cl == null && packageContext != null) {
                cl = packageContext.getClassLoader();
            }
            if (cl == null) {
                cl = BlackBoxCore.getContext().getClassLoader();
            }
            String appClass = applicationInfo != null ? applicationInfo.className : null;
            if (appClass == null || appClass.isEmpty()) {
                appClass = Application.class.getName();
            }
            Context base = packageContext != null ? packageContext : BlackBoxCore.getContext();
            Instrumentation instr = AppInstrumentation.get();
            Application app = instr.newApplication(cl, appClass, base);
            if (app != null) {
                try {
                    Field f = loadedApk.getClass().getDeclaredField("mApplication");
                    f.setAccessible(true);
                    f.set(loadedApk, app);
                } catch (Throwable ignored) {
                }
                return app;
            }
        } catch (Throwable th) {
            Slog.w(TAG, "Instrumentation.newApplication failed: " + th.getMessage());
        }
        // 5) Manual Class.forName + attach
        try {
            ClassLoader cl = null;
            try {
                cl = BRLoadedApk.get(loadedApk).getClassLoader();
            } catch (Throwable ignored) {
            }
            if (cl == null && packageContext != null) cl = packageContext.getClassLoader();
            if (cl == null) cl = BlackBoxCore.getContext().getClassLoader();
            String appClass = applicationInfo != null ? applicationInfo.className : null;
            Class<?> clazz;
            if (appClass == null || appClass.isEmpty()) {
                clazz = Application.class;
            } else {
                try {
                    clazz = cl.loadClass(appClass);
                } catch (Throwable cnfe) {
                    clazz = Application.class;
                }
            }
            Application app = (Application) clazz.newInstance();
            Method attach = Application.class.getDeclaredMethod("attach", Context.class);
            attach.setAccessible(true);
            Context base = packageContext != null ? packageContext : BlackBoxCore.getContext();
            attach.invoke(app, base);
            try {
                Field f = loadedApk.getClass().getDeclaredField("mApplication");
                f.setAccessible(true);
                f.set(loadedApk, app);
            } catch (Throwable ignored) {
            }
            return app;
        } catch (Throwable th) {
            Slog.w(TAG, "manual Application create failed: " + th.getMessage());
        }
        return null;
    }

    private void fixAiLiaoPhoto(Application application) throws Throwable {
		if (application.getPackageName().equals("com.mosheng")) {
			ClassLoader loader = AppInstrumentation.get().getDelegateAppClassLoader();
			Class fileProviderClass = loader.loadClass("androidx.core.content.FileProvider");
			Method parsePathStrategyMethod = fileProviderClass.getDeclaredMethod("getPathStrategy", Context.class, String.class);
			parsePathStrategyMethod.setAccessible(true);
			Object pathStrategy = parsePathStrategyMethod.invoke(null, application, "com.mosheng.provider");
			Field fieldAuthority = pathStrategy.getClass().getDeclaredField("mAuthority");
			fieldAuthority.setAccessible(true);
            String newAuthority = "files." + BlackBoxCore.getHostPkg();
			fieldAuthority.set(pathStrategy, newAuthority);
		}
	}
    
    private void fixWeChatRecovery(Application app) {
        try {
            Field field = app.getClassLoader().loadClass("com.tencent.recovery.Recovery").getField("context");
            field.setAccessible(true);
            if (field.get(null) != null) {
                return;
            }
            field.set(null, app.getBaseContext());
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static Context createPackageContext(ApplicationInfo info) {
        if (info == null || info.packageName == null) {
            return null;
        }
        final String pkg = info.packageName;
        final int flags = Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY;
        // Attempt 1: standard createPackageContext (goes through our PM hooks)
        try {
            Context c = BlackBoxCore.getContext().createPackageContext(pkg, flags);
            if (c != null) return c;
        } catch (Throwable e) {
            Slog.w(TAG, "createPackageContext attempt1 failed for " + pkg + ": " + e.getMessage());
        }
        // Attempt 2: without INCLUDE_CODE (Android 16 sometimes rejects code context)
        try {
            Context c = BlackBoxCore.getContext().createPackageContext(pkg, Context.CONTEXT_IGNORE_SECURITY);
            if (c != null) return c;
        } catch (Throwable e) {
            Slog.w(TAG, "createPackageContext attempt2 failed for " + pkg + ": " + e.getMessage());
        }
        // Attempt 3: ActivityThread.getPackageInfo / createPackageContextAsUser reflection
        try {
            Object mainThread = BlackBoxCore.mainThread();
            Context c = createPackageContextViaActivityThread(mainThread, info);
            if (c != null) return c;
        } catch (Throwable e) {
            Slog.w(TAG, "createPackageContext attempt3 failed for " + pkg + ": " + e.getMessage());
        }
        Log.e(TAG, "createPackageContext exhausted for " + pkg);
        return null;
    }

    private static boolean isHostLoadedApk(Object loadedApk, String packageName) {
        try {
            ApplicationInfo ai = BRLoadedApk.get(loadedApk).mApplicationInfo();
            if (ai == null || ai.packageName == null) return true;
            return !ai.packageName.equals(packageName);
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Obtain LoadedApk for a virtual package via ActivityThread internals when
     * createPackageContext fell back to the host Context.
     */
    private static Object peekLoadedApk(String packageName, ApplicationInfo applicationInfo) {
        try {
            Object mainThread = BlackBoxCore.mainThread();
            // ActivityThread.getPackageInfoNoCheck(ApplicationInfo, CompatibilityInfo)
            Method[] methods = mainThread.getClass().getDeclaredMethods();
            for (Method m : methods) {
                String n = m.getName();
                if (!"getPackageInfoNoCheck".equals(n) && !"getPackageInfo".equals(n)) {
                    continue;
                }
                Class<?>[] pts = m.getParameterTypes();
                if (pts.length < 1 || !ApplicationInfo.class.isAssignableFrom(pts[0])) {
                    continue;
                }
                m.setAccessible(true);
                Object[] args = new Object[pts.length];
                args[0] = applicationInfo;
                for (int i = 1; i < pts.length; i++) {
                    args[i] = null;
                    if (pts[i] == boolean.class) args[i] = false;
                    if (pts[i] == int.class) args[i] = 0;
                }
                try {
                    Object result = m.invoke(mainThread, args);
                    if (result != null) {
                        return result;
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable t) {
            Slog.w(TAG, "peekLoadedApk failed: " + t.getMessage());
        }
        return null;
    }

    private static Context createPackageContextViaActivityThread(Object mainThread, ApplicationInfo info) {
        try {
            Object loadedApk = peekLoadedApk(info.packageName, info);
            if (loadedApk == null) return null;
            // LoadedApk.makeApplication / getContext? Prefer ContextImpl.createAppContext
            try {
                Class<?> contextImplClz = Class.forName("android.app.ContextImpl");
                Method createAppContext = null;
                for (Method m : contextImplClz.getDeclaredMethods()) {
                    if ("createAppContext".equals(m.getName())) {
                        createAppContext = m;
                        break;
                    }
                }
                if (createAppContext != null) {
                    createAppContext.setAccessible(true);
                    Class<?>[] pts = createAppContext.getParameterTypes();
                    Object[] args = new Object[pts.length];
                    for (int i = 0; i < pts.length; i++) {
                        if (pts[i].getName().contains("ActivityThread")) {
                            args[i] = mainThread;
                        } else if (pts[i].getName().contains("LoadedApk")) {
                            args[i] = loadedApk;
                        } else if (pts[i] == String.class) {
                            args[i] = null;
                        } else if (pts[i] == int.class) {
                            args[i] = 0;
                        } else {
                            args[i] = null;
                        }
                    }
                    Object ctx = createAppContext.invoke(null, args);
                    if (ctx instanceof Context) {
                        return (Context) ctx;
                    }
                }
            } catch (Throwable t) {
                Slog.w(TAG, "ContextImpl.createAppContext failed: " + t.getMessage());
            }
        } catch (Throwable t) {
            Slog.w(TAG, "createPackageContextViaActivityThread: " + t.getMessage());
        }
        return null;
    }
    
    public Object getPackageInfo() {
        return this.mBoundApplication.info;
    }
    
    private void installProviders(Context context, String processName, List<ProviderInfo> provider) {
        long origId = Binder.clearCallingIdentity();
        try {
            for (ProviderInfo providerInfo : provider) {
                try {
                    if (processName.equals(providerInfo.processName) ||
                            providerInfo.processName.equals(context.getPackageName()) || providerInfo.multiprocess) {
                        installProvider(BlackBoxCore.mainThread(), context, providerInfo, null);
                    }
                } catch (Throwable ignored) { }
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
            ContentProviderDelegate.init();
        }
    }

    public static void installProvider(Object mainThread, Context context, ProviderInfo providerInfo, Object holder) throws Throwable {
        Method installProvider = Reflector.findMethodByFirstName(mainThread.getClass(), "installProvider");
        if (installProvider != null) {
            installProvider.setAccessible(true);
            installProvider.invoke(mainThread, context, holder, providerInfo, false, true, true);
        }
    }
    
    public void loadXposed(Context context) {
        String vPackageName = getAppPackageName();
        String vProcessName = getAppProcessName();
        if (!TextUtils.isEmpty(vPackageName) && !TextUtils.isEmpty(vProcessName) && BXposedManager.get().isXPEnable()) {
            assert vPackageName != null;
            assert vProcessName != null;
            boolean isFirstApplication = vPackageName.equals(vProcessName);
            List<InstalledModule> installedModules = BXposedManager.get().getInstalledModules();
            for (InstalledModule installedModule : installedModules) {
                if (!installedModule.enable) {
                    continue;
                }
                try {
                  //  PineXposed.loadModule(new File(installedModule.getApplication().sourceDir));
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
            try {
              //  PineXposed.onPackageLoad(vPackageName, vProcessName, context.getApplicationInfo(), isFirstApplication, context.getClassLoader());
            } catch (Throwable ignored) {
            }
        }
        if (RemoteManager.sHideXposed) {
            RNative.hideXposed();
        }
    }

    @Override
    public IBinder getActivityThread() {
        return BRActivityThread.get(BlackBoxCore.mainThread()).getApplicationThread();
    }

    @Override
    public void bindApplication() {
        if (!isInit()) bindApplication(getAppPackageName(), getAppProcessName());
    }

    @Override
    public void stopService(Intent intent) {
        AppServiceDispatcher.get().stopService(intent);
    }

    @Override
    public void restartJobService(String selfId) {}

    @Override
    public IBinder acquireContentProviderClient(ProviderInfo providerInfo) {
        if (!isInit()) bindApplication(getAppConfig().packageName, getAppConfig().processName);
        for (String auth : providerInfo.authority.split(";")) {
            ContentProviderClient client = BlackBoxCore.getContext().getContentResolver().acquireContentProviderClient(auth);
            IInterface iInterface = BRContentProviderClient.get(client).mContentProvider();
            if (iInterface != null) return iInterface.asBinder();
        }
        return null;
    }

    @Override
    public IBinder peekService(Intent intent) {
        return AppServiceDispatcher.get().peekService(intent);
    }

    @Override
    public void finishActivity(final IBinder token) {
        mH.post(() -> {
            Map<IBinder, Object> activities = BRActivityThread.get(BlackBoxCore.mainThread()).mActivities();
            Object clientRecord = activities.get(token);
            if (clientRecord == null) return;
            Activity activity = getActivityByToken(token);
            while (activity.getParent() != null) activity = activity.getParent();
            int resultCode = BRActivity.get(activity).mResultCode();
            Intent resultData = BRActivity.get(activity).mResultData();
            ActivityManagerCompat.finishActivity(token, resultCode, resultData);
            BRActivity.get(activity)._set_mFinished(true);
        });
    }

    @Override
    public void handleNewIntent(final IBinder token, final Intent intent) {
        mH.post(() -> {
            Intent newIntent = BuildCompat.isLollipop_MR1() ? BRReferrerIntent.get()._new(intent, BlackBoxCore.getHostPkg()) : intent;
            Object mainThread = BlackBoxCore.mainThread();
            if (BRActivityThread.get(mainThread)._check_performNewIntents(null, null) != null) {
                BRActivityThread.get(mainThread).performNewIntents(token, Collections.singletonList(newIntent));
            } else if (BRActivityThreadNMR1.get(mainThread)._check_performNewIntents(null, null, false) != null) {
                BRActivityThreadNMR1.get(mainThread).performNewIntents(token, Collections.singletonList(newIntent), true);
            } else if (BRActivityThreadQ.get(mainThread)._check_handleNewIntent(null, null) != null) {
                BRActivityThreadQ.get(mainThread).handleNewIntent(token, Collections.singletonList(newIntent));
            }
        });
    }

    @Override
    public void scheduleReceiver(final ReceiverData data) {
        if (!isInit()) bindApplication();
        mH.post(() -> {
            try {
                Context baseContext = mInitialApplication.getBaseContext();
                ClassLoader cl = baseContext.getClassLoader();
                data.intent.setExtrasClassLoader(cl);
                BroadcastReceiver receiver = (BroadcastReceiver) cl.loadClass(data.activityInfo.name).newInstance();
                BRBroadcastReceiver.get(receiver).setPendingResult(data.data.build());
                receiver.onReceive(baseContext, data.intent);
                BroadcastReceiver.PendingResult finish = BRBroadcastReceiver.get(receiver).getPendingResult();
                if (finish != null) finish.finish();
                BlackBoxCore.getBActivityManager().finishBroadcast(data.data);
            } catch (Throwable e) {
                Log.e(TAG, "error", e);
                Slog.e(TAG, "Error receiving broadcast " + data.intent);
            }
        });
    }

    public static Activity getActivityByToken(IBinder token) {
        Map<IBinder, Object> map = BRActivityThread.get(BlackBoxCore.mainThread()).mActivities();
        return BRActivityThreadActivityClientRecord.get(map.get(token)).activity();
    }

    private void onBeforeCreateApplication(String packageName, String processName, Context context) {
        for (AppLifecycleCallback cb : BlackBoxCore.get().getAppLifecycleCallbacks()) {
            cb.beforeCreateApplication(packageName, processName, context, getUserId());
        }
    }

    private void onBeforeApplicationOnCreate(String packageName, String processName, Application app) {
        for (AppLifecycleCallback cb : BlackBoxCore.get().getAppLifecycleCallbacks()) {
            cb.beforeApplicationOnCreate(packageName, processName, app, getUserId());
        }
    }

    private void onAfterApplicationOnCreate(String packageName, String processName, Application app) {
        for (AppLifecycleCallback cb : BlackBoxCore.get().getAppLifecycleCallbacks()) {
            cb.afterApplicationOnCreate(packageName, processName, app, getUserId());
        }
    }
}
