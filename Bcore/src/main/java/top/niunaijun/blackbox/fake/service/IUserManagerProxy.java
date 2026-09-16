package top.niunaijun.blackbox.fake.service;

import android.content.Context;
import android.os.Bundle;
import java.lang.reflect.Method;
import java.util.ArrayList;

import black.android.content.pm.BRUserInfo;
import black.android.os.BRIUserManagerStub;
import black.android.os.BRServiceManager;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;

public class IUserManagerProxy extends BinderInvocationStub {
    public IUserManagerProxy() {
        super(BRServiceManager.get().getService(Context.USER_SERVICE));
    }

    @Override
    protected Object getWho() {
        return BRIUserManagerStub.get().asInterface(BRServiceManager.get().getService(Context.USER_SERVICE));
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(Context.USER_SERVICE);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @ProxyMethod("getApplicationRestrictions")
    public static class GetApplicationRestrictions extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            // Android 16: system-only for other packages (e.g. com.android.vending).
            try {
                if (args != null && args.length > 0 && args[0] instanceof String) {
                    args[0] = BlackBoxCore.getHostPkg();
                }
                Object result = method.invoke(who, args);
                return result != null ? result : new Bundle();
            } catch (SecurityException se) {
                return new Bundle();
            } catch (Throwable th) {
                return new Bundle();
            }
        }
    }

    @ProxyMethod("getApplicationRestrictionsForUser")
    public static class GetApplicationRestrictionsForUser extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                if (args != null && args.length > 0 && args[0] instanceof String) {
                    args[0] = BlackBoxCore.getHostPkg();
                }
                Object result = method.invoke(who, args);
                return result != null ? result : new Bundle();
            } catch (Throwable th) {
                return new Bundle();
            }
        }
    }

    @ProxyMethod("getProfileParent")
    public static class GetProfileParent extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Object sVBoxCore = BRUserInfo.get()._new(BActivityThread.getUserId(), "RIYAZcore", BRUserInfo.get().FLAG_PRIMARY());
            return sVBoxCore;
        }
    }

    @ProxyMethod("getUsers")
    public static class getUsers extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return new ArrayList<>();
        }
    }
}
