package android.app;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;

public interface IBinderSession extends IInterface {
    abstract class Stub extends Binder implements IBinderSession {
        public static IBinderSession asInterface(IBinder obj) {
            return null;
        }
    }
}
