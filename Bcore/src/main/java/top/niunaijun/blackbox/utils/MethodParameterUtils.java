package top.niunaijun.blackbox.utils;

import android.os.Parcelable;
import android.os.Process;

import java.util.Arrays;
import java.util.HashSet;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;

public class MethodParameterUtils {

    public static <T> T getFirstParam(Object[] args, Class<T> tClass) {
        if (args == null) {
            return null;
        }
        int index = ArrayUtils.indexOfFirst(args, tClass);
        if (index != -1) {
            return (T) args[index];
        }
        return null;
    }

    public static String replaceFirstAppPkg(Object[] args) {
        if (args == null) {
            return null;
        }
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof String) {
                String value = (String) args[i];
                if (BlackBoxCore.get().isInstalled(value, BActivityThread.getUserId())) {
                    args[i] = BlackBoxCore.getHostPkg();
                    return value;
                }
            }
        }
        return null;
    }

    public static void replaceAllAppPkg(Object[] args) {
        if (args == null) {
            return;
        }
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null)
                continue;
            if (args[i] instanceof String) {
                String value = (String) args[i];
                if (BlackBoxCore.get().isInstalled(value, BActivityThread.getUserId())) {
                    args[i] = BlackBoxCore.getHostPkg();
                }
            }
        }
    }

    public static void replaceFirstUid(Object[] args) {
        if (args == null)
            return;
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Integer) {
                int uid = (int) args[i];
                if (uid == BActivityThread.getBUid()) {
                    args[i] = BlackBoxCore.getHostUid();
                }
            }
        }
    }

    public static void replaceLastUid(Object[] args) {
        int index = ArrayUtils.indexOfLast(args, Integer.class);
        if (index != -1) {
            int uid = (int) args[index];
            if (uid == BActivityThread.getBUid()) {
                args[index] = BlackBoxCore.getHostUid();
            }
        }
    }

    public static String replaceLastAppPkg(Object[] args) {
        int index = ArrayUtils.indexOfLast(args, String.class);
        if (index != -1) {
            String pkg = (String) args[index];
            if (BlackBoxCore.get().isInstalled(pkg, BActivityThread.getUserId())) {
                args[index] = BlackBoxCore.getHostPkg();
            }
            return pkg;
        }
        return null;
    }
    
    /**
     * Rewrite the trailing userId argument used by AMS APIs.
     * USER_ALL (-1) / USER_CURRENT (-2) / USER_CURRENT_OR_SELF (-3) must become the host
     * user id, otherwise Android 10+ throws:
     * SecurityException: broadcast ... asks to run as user -1 ... requires INTERACT_ACROSS_USERS.
     * GMS NetworkMonitor on Android 16 triggers this via sendBroadcastAsUser(UserHandle.ALL).
     */
    public static void replaceLastUserId(Object[] args) {
        if (args == null || args.length == 0) {
            return;
        }
        int index = -1;
        for (int i = args.length - 1; i >= 0; i--) {
            if (args[i] instanceof Integer) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            return;
        }
        int userId = (int) args[index];
        // USER_ALL=-1, USER_CURRENT=-2, USER_CURRENT_OR_SELF=-3, USER_NULL=-10000
        // Also rewrite virtual BUser ids that are not the real host user.
        if (userId < 0 || userId == BActivityThread.getUserId()) {
            args[index] = BlackBoxCore.getHostUserId();
        }
    }

    /**
     * Rewrite every Integer user-handle sentinel (-1/-2/-3) in the arg list.
     * Safer for APIs where userId is not strictly the last parameter.
     */
    public static void replaceAllUserIdSentinels(Object[] args) {
        if (args == null) {
            return;
        }
        int host = BlackBoxCore.getHostUserId();
        int bUser = BActivityThread.getUserId();
        for (int i = 0; i < args.length; i++) {
            if (!(args[i] instanceof Integer)) {
                continue;
            }
            int v = (int) args[i];
            if (v == -1 || v == -2 || v == -3 || v == bUser) {
                args[i] = host;
            }
        }
    }
    
    public static String replaceSequenceAppPkg(Object[] args, int sequence) {
        int index = ArrayUtils.indexOf(args, String.class, sequence);
        if (index != -1) {
            String pkg = (String) args[index];
            if (BlackBoxCore.get().isInstalled(pkg, BActivityThread.getUserId())) {
                args[index] = BlackBoxCore.getHostPkg();
            }
            return pkg;
        }
        return null;
    }

    public static int getParamsIndex(Class[] args, Class<?> type) {
        for (int i = 0; i < args.length; i++) {
            Class obj = args[i];
            if (obj.equals(type)) {
                return i;
            }
        }
        return -1;
    }

    public static int getIndex(Object[] args, Class<?> type) {
        return getIndex(args, type, 0);
    }

    public static int getIndex(Object[] args, Class<?> type, int start) {
        for (int i = start; i < args.length; i++) {
            Object obj = args[i];
            if (obj != null && obj.getClass() == type) {
                return i;
            }
            if (type.isInstance(obj)) {
                return i;
            }
        }
        return -1;
    }

    public static Class<?>[] getAllInterface(Class clazz) {
        HashSet<Class<?>> classes = new HashSet<>();
        getAllInterfaces(clazz, classes);
        Class<?>[] result = new Class[classes.size()];
        classes.toArray(result);
        return result;
    }


    public static void getAllInterfaces(Class clazz, HashSet<Class<?>> interfaceCollection) {
        Class<?>[] classes = clazz.getInterfaces();
        if (classes.length != 0) {
            interfaceCollection.addAll(Arrays.asList(classes));
        }
        if (clazz.getSuperclass() != Object.class) {
            getAllInterfaces(clazz.getSuperclass(), interfaceCollection);
        }
    }
    
    public static String getString(Object[] args, int index) {
        if (args == null || index < 0 || index >= args.length) return null;
        Object obj = args[index];
        return obj != null ? obj.toString() : null;
    }
    
    public static int toInt(Object obj){
        if(obj instanceof Long){
            return ((Long) obj).intValue();
        }
        return (int)obj;
    }
    

}
