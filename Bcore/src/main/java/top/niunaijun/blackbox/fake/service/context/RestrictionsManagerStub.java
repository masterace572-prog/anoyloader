package top.niunaijun.blackbox.fake.service.context;

import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import black.android.content.BRIRestrictionsManagerStub;
import black.android.os.BRServiceManager;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

/**
 * Android 14+/16: non-system callers cannot read application restrictions for
 * other packages (e.g. GMS → {@code com.android.vending}). Always return empty
 * data and never forward restriction-read binder calls to the real service.
 *
 * Must be registered from {@link top.niunaijun.blackbox.fake.hook.HookManager}.
 */
public class RestrictionsManagerStub extends BinderInvocationStub {
    public static final String TAG = "RestrictionsManagerStub";

    public RestrictionsManagerStub() {
        super(BRServiceManager.get().getService(Context.RESTRICTIONS_SERVICE));
    }

    @Override
    protected Object getWho() {
        IBinder binder = BRServiceManager.get().getService(Context.RESTRICTIONS_SERVICE);
        return BRIRestrictionsManagerStub.get().asInterface(binder);
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(Context.RESTRICTIONS_SERVICE);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    /**
     * Full binder dispatch: never let SecurityException escape to GMS worker threads.
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String name = method.getName();
        if ("asBinder".equals(name)) {
            return this;
        }
        // Restriction reads are system-only on API 34+ for other packages.
        if (name != null && name.startsWith("getApplicationRestrictions")) {
            Class<?> rt = method.getReturnType();
            if (List.class.isAssignableFrom(rt)) {
                return new ArrayList<>();
            }
            return new Bundle();
        }
        if ("hasRestrictionsProvider".equals(name)
                || "hasRestrictionsProviderForUser".equals(name)) {
            return false;
        }
        if ("requestPermission".equals(name)
                || "notifyPermissionResponse".equals(name)
                || "setApplicationRestrictions".equals(name)
                || "setApplicationRestrictionsForUser".equals(name)) {
            return null;
        }

        try {
            Object base = getBase();
            if (base == null) {
                return emptyFor(method);
            }
            return method.invoke(base, args);
        } catch (Throwable t) {
            Throwable c = t.getCause() != null ? t.getCause() : t;
            Slog.w(TAG, name + " swallowed: " + c.getMessage());
            return emptyFor(method);
        }
    }

    private static Object emptyFor(Method method) {
        Class<?> rt = method.getReturnType();
        if (rt == void.class || rt == Void.class) return null;
        if (Bundle.class.isAssignableFrom(rt)) return new Bundle();
        if (List.class.isAssignableFrom(rt)) return new ArrayList<>();
        if (rt == boolean.class || rt == Boolean.class) return false;
        if (rt == int.class || rt == Integer.class) return 0;
        if (rt == long.class || rt == Long.class) return 0L;
        return null;
    }

    @ProxyMethod("getApplicationRestrictions")
    public static class GetApplicationRestrictions extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) {
            return new Bundle();
        }
    }

    @ProxyMethod("getApplicationRestrictionsForUser")
    public static class GetApplicationRestrictionsForUser extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) {
            return new Bundle();
        }
    }

    @ProxyMethod("getApplicationRestrictionsPerAdmin")
    public static class GetApplicationRestrictionsPerAdmin extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) {
            Class<?> rt = method.getReturnType();
            if (List.class.isAssignableFrom(rt)) {
                return new ArrayList<>();
            }
            return new Bundle();
        }
    }

    @ProxyMethod("hasRestrictionsProvider")
    public static class HasRestrictionsProvider extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) {
            return false;
        }
    }
}
