package launcher.helper;

import com.sun.management.OperatingSystemMXBean;
import launcher.LauncherAPI;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.management.ManagementFactory;

import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.net.URL;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Locale;

public class JVMHelper {
    @LauncherAPI
    public static final RuntimeMXBean RUNTIME_MXBEAN = ManagementFactory.getRuntimeMXBean();
    @LauncherAPI
    public static final OperatingSystemMXBean OPERATING_SYSTEM_MXBEAN = (OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();
    @LauncherAPI
    public static final OS OS_TYPE = OS.byName(OPERATING_SYSTEM_MXBEAN.getName());
    @LauncherAPI
    public static final int OS_BITS = JVMHelper.getCorrectOSArch();
    @LauncherAPI
    public static final Platform ARCH_TYPE = JVMHelper.getArchPlatform(System.getProperty("os.arch"));
    @LauncherAPI
    public static final int RAM = JVMHelper.getRAMAmount();
    @LauncherAPI
    public static final String OS_VERSION = OPERATING_SYSTEM_MXBEAN.getVersion();
    @LauncherAPI
    public static final int JVM_BITS = Integer.parseInt(System.getProperty("sun.arch.data.model"));
    @LauncherAPI
    public static final Unsafe UNSAFE;
    @LauncherAPI
    public static final MethodHandles.Lookup LOOKUP;
    @LauncherAPI
    public static final Runtime RUNTIME;
    @LauncherAPI
    public static final ClassLoader LOADER;
    @LauncherAPI
    public static final String JAVA_DIR;
    @LauncherAPI
    public static final String JAVA_DIR_NO_ARM;
    public static final String JAVA_LIBRARY_PATH = "java.library.path";
    private static final Object UCP;
    private static final MethodHandle MH_UCP_ADDURL_METHOD;
    private static final MethodHandle MH_UCP_GETURLS_METHOD;
    private static final MethodHandle MH_UCP_GETRESOURCE_METHOD;
    private static final MethodHandle MH_RESOURCE_GETCERTS_METHOD;
    private static final String unknown_platform = "-unknown";

    static {
        RUNTIME = Runtime.getRuntime();
        LOADER = ClassLoader.getSystemClassLoader();
        JAVA_DIR = JVMHelper.getPlatform(true);
        JAVA_DIR_NO_ARM = JVMHelper.getPlatform(false);
        try {
            MethodHandles.publicLookup();
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            UNSAFE = (Unsafe)field.get(null);

            Field implLookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            LOOKUP = (MethodHandles.Lookup)UNSAFE.getObject(UNSAFE.staticFieldBase(implLookupField), UNSAFE.staticFieldOffset(implLookupField));

            Class<?> ucpClass = JVMHelper.firstClass("jdk.internal.loader.URLClassPath", "sun.misc.URLClassPath");
            Class<?> loaderClass = JVMHelper.firstClass("jdk.internal.loader.ClassLoaders$AppClassLoader", "java.net.URLClassLoader");
            Class<?> resourceClass = JVMHelper.firstClass("jdk.internal.loader.Resource", "sun.misc.Resource");
            UCP = LOOKUP.findGetter(loaderClass, "ucp", ucpClass).invoke(LOADER);
            MH_UCP_ADDURL_METHOD = LOOKUP.findVirtual(ucpClass, "addURL", MethodType.methodType(Void.TYPE, URL.class));
            MH_UCP_GETURLS_METHOD = LOOKUP.findVirtual(ucpClass, "getURLs", MethodType.methodType(URL[].class));
            MH_UCP_GETRESOURCE_METHOD = LOOKUP.findVirtual(ucpClass, "getResource", MethodType.methodType(resourceClass, String.class));
            MH_RESOURCE_GETCERTS_METHOD = LOOKUP.findVirtual(resourceClass, "getCertificates", MethodType.methodType(Certificate[].class));
        }
        catch (Throwable throwable) {
            throw new InternalError(throwable);
        }
    }
    private JVMHelper() {
    }

    @LauncherAPI
    public static void addClassPath(URL uRL) {
        try {
            MH_UCP_ADDURL_METHOD.invoke(UCP, uRL);
        }
        catch (Throwable throwable) {
            throw new InternalError(throwable);
        }
    }

    @LauncherAPI
    public static void fullGC() {
        RUNTIME.gc();
        RUNTIME.runFinalization();
        LogHelper.debug("Used heap: %d MiB", RUNTIME.totalMemory() - RUNTIME.freeMemory() >> 20);
    }

    @LauncherAPI
    public static Certificate[] getCertificates(String string) {
        try {
            Object object = MH_UCP_GETRESOURCE_METHOD.invoke(UCP, string);
            return object == null ? null : (Certificate[]) MH_RESOURCE_GETCERTS_METHOD.invoke(object);
        }
        catch (Throwable throwable) {
            throw new InternalError(throwable);
        }
    }

    @LauncherAPI
    public static URL[] getClassPath() {
        try {
            return (URL[]) MH_UCP_GETURLS_METHOD.invoke(UCP);
        }
        catch (Throwable throwable) {
            throw new InternalError(throwable);
        }
    }

    @LauncherAPI
    public static void halt0(int n) {
        LogHelper.debug("Trying to halt JVM");
        try {
            LOOKUP.findStatic(Class.forName("java.lang.Shutdown"), "halt0", MethodType.methodType(Void.TYPE, Integer.TYPE)).invokeExact(n);
        }
        catch (Throwable throwable) {
            throw new InternalError(throwable);
        }
    }

    @LauncherAPI
    public static boolean isJVMMatchesSystemArch() {
        return JVM_BITS == OS_BITS;
    }

    @LauncherAPI
    public static void verifySystemProperties(Class clazz, boolean bl) {
        Locale.setDefault(Locale.US);
        LogHelper.debug("Verifying class loader");
        if (bl && !clazz.getClassLoader().equals(LOADER)) {
            throw new SecurityException("ClassLoader should be system");
        }
        LogHelper.debug("Verifying JVM architecture");
        if (!JVMHelper.isJVMMatchesSystemArch()) {
            LogHelper.warning("Java and OS architecture mismatch");
            LogHelper.warning("It's recommended to download %d-bit JRE", OS_BITS);
        }
    }

    private static int getCorrectOSArch() {
        if (OS_TYPE == OS.MUSTDIE) {
            return System.getenv("ProgramFiles(x86)") == null ? 32 : 64;
        }
        return System.getProperty("os.arch").contains("64") ? 64 : 32;
    }

    private static Platform getArchPlatform(String string) {
        if (string.equals("amd64") || string.equals("x86-64") || string.equals("x86_64")) {
            return Platform.x86_64;
        }
        if (string.equals("i386") || string.equals("i686") || string.equals("x86")) {
            return Platform.x86;
        }
        if (string.startsWith("armv8") || string.startsWith("aarch64")) {
            return Platform.arm64;
        }
        if (string.startsWith("arm") || string.startsWith("aarch32")) {
            return Platform.arm32;
        }
        throw new InternalError(String.format("Unsupported arch '%s'", string));
    }

    private static int getRAMAmount() {
        int n = (int)(OPERATING_SYSTEM_MXBEAN.getTotalPhysicalMemorySize() >> 20);
        return Math.min(n, OS_BITS == 32 ? 1536 : 65534);
    }

    public static Class firstClass(String ... stringArray) throws ClassNotFoundException {
        for (String string : stringArray) {
            try {
                return Class.forName(string, false, LOADER);
            }
            catch (ClassNotFoundException classNotFoundException) {
            }
        }
        throw new ClassNotFoundException(Arrays.toString(stringArray));
    }

    private static String getPlatform(boolean bl) {
        switch (OS_TYPE) {
            case MUSTDIE: {
                return JVMHelper.getWindows(bl);
            }
            case LINUX: {
                return JVMHelper.getLinux(bl);
            }
            case MACOSX: {
                return JVMHelper.getMacos(bl);
            }
        }
        return unknown_platform;
    }

    private static String getWindows(boolean bl) {
        if (OS_BITS == 32) {
            return "-win32";
        }
        if (OS_BITS == 64) {
            return "-win64";
        }
        return unknown_platform;
    }

    private static String getLinux(boolean bl) {
        if (OS_BITS == 32) {
            return "-linux32";
        }
        if (OS_BITS == 64) {
            return "-linux64";
        }
        return unknown_platform;
    }

    private static String getMacos(boolean bl) {
        if (OS_BITS == 64) {
            return bl && ARCH_TYPE == Platform.arm64 ? "-macosx-arm" : "-macosx";
        }
        return unknown_platform;
    }

    public enum Platform {
        x86("X86", 0, "x86"),
        x86_64("X86_64", 1, "x86-64"),
        arm64("ARM64", 2, "arm64"),
        arm32("ARM32", 3, "arm32");

        public final String platform;

        private Platform(String name, int ordinal, String platform) {
            this.platform = platform;
        }
    }

    @LauncherAPI
    public enum OS {
        MUSTDIE("mustdie"), LINUX("linux"), MACOSX("macosx");
        public final String name;

        OS(String name) {
            this.name = name;
        }

        public static OS byName(String name) {
            if (name.startsWith("Windows")) {
                return MUSTDIE;
            }
            if (name.startsWith("Linux")) {
                return LINUX;
            }
            if (name.startsWith("Mac OS X")) {
                return MACOSX;
            }
            throw new RuntimeException(String.format("This shit is not yet supported: '%s'", name));
        }
    }
}
