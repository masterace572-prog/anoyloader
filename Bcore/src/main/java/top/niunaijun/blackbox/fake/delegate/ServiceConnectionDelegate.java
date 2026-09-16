package top.niunaijun.blackbox.fake.delegate;

import android.app.IBinderSession;
import android.app.IServiceConnection;
import android.content.ComponentName;
import android.content.Intent;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ServiceConnectionDelegate extends IServiceConnection.Stub {
    private static final Map<IBinder, ServiceConnectionDelegate> sServiceConnectDelegate = new HashMap<>();
    private final IServiceConnection mConn;
    private final ComponentName mComponentName;
    private volatile Method mCachedConnectedMethod;

    private ServiceConnectionDelegate(IServiceConnection mConn, ComponentName targetComponent) {
        this.mConn = mConn;
        this.mComponentName = targetComponent;
    }

    public static ServiceConnectionDelegate getDelegate(IBinder iBinder) {
        return sServiceConnectDelegate.get(iBinder);
    }

    public static IServiceConnection createProxy(IServiceConnection base, Intent intent) {
        final IBinder iBinder = base.asBinder();
        ServiceConnectionDelegate delegate = sServiceConnectDelegate.get(iBinder);
        if (delegate == null) {
            try {
                iBinder.linkToDeath(new IBinder.DeathRecipient() {
                    @Override
                    public void binderDied() {
                        sServiceConnectDelegate.remove(iBinder);
                        iBinder.unlinkToDeath(this, 0);
                    }
                }, 0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            delegate = new ServiceConnectionDelegate(base, intent.getComponent());
            sServiceConnectDelegate.put(iBinder, delegate);
        }
        return delegate;
    }

    // Android <= 7.1 (API <= 25)
    @Override
    public void connected(ComponentName name, IBinder service) throws RemoteException {
        deliverConnected(name, service, null, false);
    }

    // Android 8.0 - 15 (API 26 - 35)
    public void connected(ComponentName name, IBinder service, boolean dead) throws RemoteException {
        deliverConnected(name, service, null, dead);
    }

    // Android 16+ (API 36+) - Fixes AbstractMethodError on Android 16 (Samsung SM-S921B)
    public void connected(ComponentName name, IBinder service, IBinderSession session, boolean dead) throws RemoteException {
        deliverConnected(name, service, session, dead);
    }

    // Additional variant for future Android compatibility
    public void connected(ComponentName name, IBinder service, IBinderSession session) throws RemoteException {
        deliverConnected(name, service, session, false);
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        try {
            return super.onTransact(code, data, reply, flags);
        } catch (AbstractMethodError e) {
            e.printStackTrace();
            // Handle TRANSACTION_connected fallback if any unexpected OS version has an abstract method mismatch
            if (code == (IBinder.FIRST_CALL_TRANSACTION + 0)) {
                try {
                    data.setDataPosition(0);
                    data.enforceInterface(getInterfaceDescriptor());
                    ComponentName name = null;
                    if (data.readInt() != 0) {
                        name = ComponentName.CREATOR.createFromParcel(data);
                    }
                    IBinder service = data.readStrongBinder();
                    deliverConnected(mComponentName != null ? mComponentName : name, service, null, false);
                    if (reply != null) {
                        reply.writeNoException();
                    }
                    return true;
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
            return false;
        } catch (Throwable t) {
            t.printStackTrace();
            return super.onTransact(code, data, reply, flags);
        }
    }

    private void deliverConnected(ComponentName name, IBinder service, Object session, boolean dead) {
        if (mConn == null) {
            return;
        }
        ComponentName target = (mComponentName != null) ? mComponentName : name;

        // 1. Check cached method
        try {
            Method m = mCachedConnectedMethod;
            if (m != null) {
                int count = m.getParameterTypes().length;
                if (count == 4) {
                    m.invoke(mConn, target, service, session, dead);
                    return;
                } else if (count == 3) {
                    m.invoke(mConn, target, service, dead);
                    return;
                } else if (count == 2) {
                    m.invoke(mConn, target, service);
                    return;
                }
            }
        } catch (Throwable t) {
            mCachedConnectedMethod = null;
        }

        // 2. Discover best matching 'connected' method on mConn
        Method method = findConnectedMethod(mConn.getClass());
        if (method != null) {
            try {
                method.setAccessible(true);
                mCachedConnectedMethod = method;
                int count = method.getParameterTypes().length;
                if (count == 4) {
                    method.invoke(mConn, target, service, session, dead);
                    return;
                } else if (count == 3) {
                    method.invoke(mConn, target, service, dead);
                    return;
                } else if (count == 2) {
                    method.invoke(mConn, target, service);
                    return;
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        // 3. Fallback to basic AIDL method
        try {
            mConn.connected(target, service);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static Method findConnectedMethod(Class<?> clazz) {
        Method candidate4 = null;
        Method candidate3 = null;
        Method candidate2 = null;

        for (Method m : clazz.getMethods()) {
            if ("connected".equals(m.getName())) {
                int len = m.getParameterTypes().length;
                if (len == 4) candidate4 = m;
                else if (len == 3) candidate3 = m;
                else if (len == 2) candidate2 = m;
            }
        }
        if (candidate4 == null || candidate3 == null || candidate2 == null) {
            for (Method m : clazz.getDeclaredMethods()) {
                if ("connected".equals(m.getName())) {
                    int len = m.getParameterTypes().length;
                    if (len == 4 && candidate4 == null) candidate4 = m;
                    else if (len == 3 && candidate3 == null) candidate3 = m;
                    else if (len == 2 && candidate2 == null) candidate2 = m;
                }
            }
        }

        if (candidate4 != null) return candidate4;
        if (candidate3 != null) return candidate3;
        return candidate2;
    }
}
