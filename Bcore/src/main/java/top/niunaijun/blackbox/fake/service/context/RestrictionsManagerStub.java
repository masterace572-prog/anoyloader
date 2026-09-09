package top.niunaijun.blackbox.fake.service.context;

import android.content.Context;
import android.os.Bundle;

import java.lang.reflect.Method;

import black.android.content.BRIRestrictionsManagerStub;
import black.android.os.BRServiceManager;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

/**
 * Android 14+ / 16: non-system callers may not read application restrictions for
 * other packages (e.g. GMS querying {@code com.android.vending}). Always return
 * an empty Bundle so Play / GMS worker threads do not crash the sandbox.
 */
public class RestrictionsManagerStub extends BinderInvocationStub {
    public static final String TAG = "RestrictionsManagerStub";

    public RestrictionsManagerStub() {
        super(BRServiceManager.get().getService(Context.RESTRICTIONS_SERVICE));
    }

    @Override
    protected Object getWho() {
        return BRIRestrictionsManagerStub.get().asInterface(
                BRServiceManager.get().getService(Context.RESTRICTIONS_SERVICE));
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(Context.RESTRICTIONS_SERVICE);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    private static Bundle emptyRestrictions() {
        return new Bundle();
    }

    private static Object safeInvokeOrEmpty(Object who, Method method, Object[] args) {
        try {
            if (args != null && args.length > 0 && args[0] instanceof String) {
                args[0] = BlackBoxCore.getHostPkg();
            }
            Object result = method.invoke(who, args);
            return result != null ? result : emptyRestrictions();
        } catch (SecurityException se) {
            Slog.w(TAG, method.getName() + " SecurityException swallowed: " + se.getMessage());
            return emptyRestrictions();
        } catch (Throwable t) {
            Throwable c = t.getCause();
            if (t instanceof SecurityException || c instanceof SecurityException) {
                Slog.w(TAG, method.getName() + " SecurityException (wrapped) swallowed: "
                        + (c != null ? c.getMessage() : t.getMessage()));
                return emptyRestrictions();
            }
            // Unknown failure — still safer empty than crash GMS threads
            Slog.w(TAG, method.getName() + " failed, returning empty: " + t.getMessage());
            return emptyRestrictions();
        }
    }

    @ProxyMethod("getApplicationRestrictions")
    public static class GetApplicationRestrictions extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return safeInvokeOrEmpty(who, method, args);
        }
    }

    @ProxyMethod("getApplicationRestrictionsForUser")
    public static class GetApplicationRestrictionsForUser extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return safeInvokeOrEmpty(who, method, args);
        }
    }

    @ProxyMethod("getApplicationRestrictionsPerAdmin")
    public static class GetApplicationRestrictionsPerAdmin extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            // List return type on some APIs — prefer empty list via invoke; on failure empty Bundle is wrong.
            try {
                if (args != null && args.length > 0 && args[0] instanceof String) {
                    args[0] = BlackBoxCore.getHostPkg();
                }
                Object result = method.invoke(who, args);
                if (result != null) return result;
                Class<?> rt = method.getReturnType();
                if (java.util.List.class.isAssignableFrom(rt)) {
                    return new java.util.ArrayList<>();
                }
                if (Bundle.class.isAssignableFrom(rt)) {
                    return emptyRestrictions();
                }
                return null;
            } catch (Throwable t) {
                Slog.w(TAG, "getApplicationRestrictionsPerAdmin swallowed: " + t.getMessage());
                Class<?> rt = method.getReturnType();
                if (java.util.List.class.isAssignableFrom(rt)) {
                    return new java.util.ArrayList<>();
                }
                if (Bundle.class.isAssignableFrom(rt)) {
                    return emptyRestrictions();
                }
                return null;
            }
        }
    }
}
