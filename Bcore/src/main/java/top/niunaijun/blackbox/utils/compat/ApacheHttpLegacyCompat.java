package top.niunaijun.blackbox.utils.compat;

import android.util.Log;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import dalvik.system.DexFile;
import top.niunaijun.blackbox.utils.Reflector;

/**
 * PUBG Global IMSDK (Volley HurlStack) still references {@code org.apache.http.ProtocolVersion}.
 * On Android 10+ that package is no longer on the boot classpath; on Android 14–16 virtual
 * ClassLoaders often never pick up {@code org.apache.http.legacy} via sharedLibraryFiles alone.
 * Inject the framework jar into the app ClassLoader pathList.
 */
public final class ApacheHttpLegacyCompat {
    private static final String TAG = "ApacheHttpLegacy";

    private static final String[] CANDIDATE_JARS = new String[]{
            "/system/framework/org.apache.http.legacy.jar",
            "/system/framework/org.apache.http.legacy.boot.jar",
            "/system_ext/framework/org.apache.http.legacy.jar",
            "/apex/com.android.runtime/javalib/org.apache.http.legacy.jar",
            "/apex/com.android.art/javalib/org.apache.http.legacy.jar",
    };

    private ApacheHttpLegacyCompat() {
    }

    public static String resolveJarPath() {
        for (String path : CANDIDATE_JARS) {
            File f = new File(path);
            if (f.isFile() && f.canRead() && f.length() > 0) {
                return path;
            }
        }
        return null;
    }

    /** Paths to advertise on ApplicationInfo.sharedLibraryFiles. */
    public static String[] resolveAllJarPaths() {
        List<String> found = new ArrayList<>();
        for (String path : CANDIDATE_JARS) {
            File f = new File(path);
            if (f.isFile() && f.canRead() && f.length() > 0) {
                found.add(path);
            }
        }
        if (found.isEmpty()) {
            found.add("/system/framework/org.apache.http.legacy.jar");
            found.add("/system/framework/org.apache.http.legacy.boot.jar");
        }
        return found.toArray(new String[0]);
    }

    public static boolean isAvailable(ClassLoader loader) {
        if (loader == null) return false;
        try {
            Class.forName("org.apache.http.ProtocolVersion", false, loader);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Ensure {@code org.apache.http.*} is loadable from {@code classLoader}.
     * Safe to call multiple times; no-ops if already resolvable.
     */
    public static void ensureLoaded(ClassLoader classLoader) {
        if (classLoader == null) return;
        if (isAvailable(classLoader)) {
            return;
        }
        String jar = resolveJarPath();
        if (jar == null) {
            Log.w(TAG, "org.apache.http.legacy jar not found on device");
            return;
        }
        try {
            if (injectIntoPathList(classLoader, jar)) {
                Log.i(TAG, "Injected apache http legacy: " + jar);
                return;
            }
        } catch (Throwable t) {
            Log.w(TAG, "pathList inject error: " + t.getMessage());
        }
        // Fallback: PathClassLoader.addDexPath (hidden API, still present on many OEMs)
        try {
            Method addDexPath = findMethod(classLoader.getClass(), "addDexPath", String.class);
            if (addDexPath != null) {
                addDexPath.invoke(classLoader, jar);
                Log.i(TAG, "addDexPath apache http legacy: " + jar);
            }
        } catch (Throwable t) {
            Log.w(TAG, "addDexPath fallback failed: " + t.getMessage());
        }
        if (!isAvailable(classLoader)) {
            Log.e(TAG, "org.apache.http.ProtocolVersion still unresolved after inject");
        }
    }

    private static boolean injectIntoPathList(ClassLoader classLoader, String jarPath) throws Exception {
        Object pathList = Reflector.on("dalvik.system.BaseDexClassLoader")
                .field("pathList")
                .get(classLoader);
        if (pathList == null) return false;

        Object[] oldElements = Reflector.with(pathList).field("dexElements").get();
        if (oldElements == null) oldElements = new Object[0];

        for (Object el : oldElements) {
            try {
                Object dexFile = Reflector.with(el).field("dexFile").get();
                if (dexFile instanceof DexFile) {
                    String name = ((DexFile) dexFile).getName();
                    if (name != null && name.contains("org.apache.http.legacy")) {
                        return true;
                    }
                }
            } catch (Throwable ignored) {
            }
            try {
                Object pathObj = Reflector.with(el).field("path").get();
                if (pathObj instanceof File
                        && ((File) pathObj).getAbsolutePath().contains("org.apache.http.legacy")) {
                    return true;
                }
            } catch (Throwable ignored) {
            }
        }

        Object newElement = makeDexElement(jarPath, pathList);
        if (newElement == null) return false;

        Class<?> elementClass = oldElements.getClass().getComponentType();
        if (elementClass == null) {
            elementClass = newElement.getClass();
        }
        Object[] combined = (Object[]) Array.newInstance(elementClass, oldElements.length + 1);
        // Put legacy jar first so it wins over any stubs
        combined[0] = newElement;
        System.arraycopy(oldElements, 0, combined, 1, oldElements.length);
        Reflector.with(pathList).field("dexElements").set(combined);
        return isAvailable(classLoader);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object makeDexElement(String jarPath, Object pathList) {
        File jarFile = new File(jarPath);

        // DexPathList.makePathElements(List, File, List) — API 26+
        try {
            Method m = findMethod(pathList.getClass(), "makePathElements",
                    List.class, File.class, List.class);
            if (m != null) {
                List files = new ArrayList();
                files.add(jarFile);
                Object elements = m.invoke(pathList, files, null, new ArrayList());
                if (elements != null && Array.getLength(elements) > 0) {
                    return Array.get(elements, 0);
                }
            }
        } catch (Throwable ignored) {
        }

        // DexPathList.makeDexElements(ArrayList, File, ArrayList, ClassLoader)
        try {
            Method m = findMethod(pathList.getClass(), "makeDexElements",
                    ArrayList.class, File.class, ArrayList.class, ClassLoader.class);
            if (m != null) {
                ArrayList files = new ArrayList();
                files.add(jarFile);
                Object elements = m.invoke(pathList, files, null, new ArrayList(), null);
                if (elements != null && Array.getLength(elements) > 0) {
                    return Array.get(elements, 0);
                }
            }
        } catch (Throwable ignored) {
        }

        // DexPathList.makeDexElements(ArrayList, File, ArrayList)
        try {
            Method m = findMethod(pathList.getClass(), "makeDexElements",
                    ArrayList.class, File.class, ArrayList.class);
            if (m != null) {
                ArrayList files = new ArrayList();
                files.add(jarFile);
                Object elements = m.invoke(pathList, files, null, new ArrayList());
                if (elements != null && Array.getLength(elements) > 0) {
                    return Array.get(elements, 0);
                }
            }
        } catch (Throwable ignored) {
        }

        // Element constructors
        try {
            Class<?> elementClass = Class.forName("dalvik.system.DexPathList$Element");
            DexFile dex = null;
            try {
                dex = new DexFile(jarPath);
            } catch (Throwable ignored) {
            }
            for (Constructor<?> ctor : elementClass.getDeclaredConstructors()) {
                ctor.setAccessible(true);
                Class<?>[] pts = ctor.getParameterTypes();
                try {
                    if (pts.length == 4
                            && File.class.isAssignableFrom(pts[0])
                            && pts[1] == boolean.class
                            && (pts[2] == File.class || pts[2] == null)
                            && DexFile.class.isAssignableFrom(pts[3])) {
                        return ctor.newInstance(jarFile, Boolean.FALSE, null, dex);
                    }
                    if (pts.length == 3
                            && File.class.isAssignableFrom(pts[0])
                            && pts[1] == boolean.class) {
                        return ctor.newInstance(jarFile, Boolean.FALSE, null);
                    }
                } catch (Throwable ignoredCtor) {
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private static Method findMethod(Class<?> clazz, String name, Class<?>... params) {
        Class<?> c = clazz;
        while (c != null) {
            try {
                Method m = c.getDeclaredMethod(name, params);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) {
                c = c.getSuperclass();
            }
        }
        // Match by name + arity as last resort (OEM signature drift)
        c = clazz;
        while (c != null) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getName().equals(name) && m.getParameterTypes().length == params.length) {
                    m.setAccessible(true);
                    return m;
                }
            }
            c = c.getSuperclass();
        }
        return null;
    }
}
