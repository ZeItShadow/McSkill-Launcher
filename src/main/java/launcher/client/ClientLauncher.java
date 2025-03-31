package launcher.client;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.WriterConfig;
import launcher.Launcher;
import launcher.LauncherAPI;
import launcher.hasher.DirWatcher;
import launcher.hasher.FileNameMatcher;
import launcher.hasher.HashedDir;
import launcher.helper.*;
import launcher.request.update.LauncherRequest;
import launcher.serialize.HInput;
import launcher.serialize.HOutput;
import launcher.serialize.signed.SignedObjectHolder;
import launcher.serialize.stream.StreamObject;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.security.interfaces.RSAPublicKey;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

public final class ClientLauncher {
    @LauncherAPI
    public static final String SKIN_URL_PROPERTY = "skinURL";
    @LauncherAPI
    public static final String SKIN_DIGEST_PROPERTY = "skinDigest";
    @LauncherAPI
    public static final String CLOAK_URL_PROPERTY = "cloakURL";
    @LauncherAPI
    public static final String CLOAK_DIGEST_PROPERTY = "cloakDigest";
    private static final String[] aux = new String[0];
    private static final String MAGICAL_INTEL_OPTION = "-XX:HeapDumpPath=ThisTricksIntelDriversForPerformance_javaw.exe_minecraft.exe.heapdump";
    private static final Set<PosixFilePermission> BIN_POSIX_PERMISSIONS;
    private static final Path NATIVES_DIR;
    private static final Path RESOURCEPACKS_DIR;
    private static final Pattern UUID_PATTERN;
    private static final AtomicBoolean LAUNCHED;

    private ClientLauncher() {
    }

    @LauncherAPI
    public static boolean isLaunched() {
        return LAUNCHED.get();
    }

    public static String jvmProperty(String string, String string2) {
        return String.format("-D%s=%s", string, string2);
    }

    @LauncherAPI
    public static Process launch(Path path, SignedObjectHolder signedObjectHolder, SignedObjectHolder signedObjectHolder2, SignedObjectHolder signedObjectHolder3, SignedObjectHolder signedObjectHolder4, Params params) throws Throwable {
        LogHelper.debug("Writing ClientLauncher params file");
        Path path2 = Files.createTempFile("ClientLauncherParams", ".bin", new FileAttribute[0]);
        try (HOutput object = new HOutput(IOHelper.newOutput(path2));){
            params.write(object);
            signedObjectHolder4.write(object);
            signedObjectHolder.write(object);
            signedObjectHolder2.write(object);
            signedObjectHolder3.write(object);
        }
        LogHelper.debug("Resolving JVM binary");
        Path javaBin = IOHelper.resolveJavaBin(path);
        if (IOHelper.POSIX) {
            Files.setPosixFilePermissions(javaBin, BIN_POSIX_PERMISSIONS);
        }
        LinkedList<String> args = new LinkedList<String>();
        args.add(javaBin.toString());
        args.add(MAGICAL_INTEL_OPTION);
        if (params.ram > 0 && params.ram <= JVMHelper.RAM) {
            args.add("-Xms" + params.ram + 'M');
            args.add("-Xmx" + params.ram + 'M');
        }
        args.add(ClientLauncher.jvmProperty("launcher.debug", Boolean.toString(LogHelper.isDebugEnabled())));
        if (Launcher.Config.ADDRESS_OVERRIDE != null) {
            args.add(ClientLauncher.jvmProperty("launcher.addressOverride", Launcher.Config.ADDRESS_OVERRIDE));
        }
        if (JVMHelper.OS_TYPE == JVMHelper.OS.MUSTDIE && JVMHelper.OS_VERSION.startsWith("10.")) {
            args.add(ClientLauncher.jvmProperty("os.name", "Windows 10"));
            args.add(ClientLauncher.jvmProperty("os.version", "10.0"));
        }
        args.add(ClientLauncher.jvmProperty("java.library.path", params.clientDir.resolve(NATIVES_DIR).toString()));
        for (String string : ((ClientProfile)signedObjectHolder4.object).getJvmArgs()) {
            args.add(string.replace("${cp_separator}", File.pathSeparator));
        }
        String string = ((ClientProfile)signedObjectHolder4.object).getVersion();
        if (ClientProfile.Version.compare(string, "1.13") >= 0 && JVMHelper.OS_TYPE == JVMHelper.OS.MACOSX) {
            Collections.addAll(args, "-XstartOnFirstThread");
        }
        Collections.addAll(args, "-classpath", IOHelper.getCodeSource(ClientLauncher.class).toString(), ClientLauncher.class.getName());
        args.add(path2.toString());
        LogHelper.debug("Commandline: " + args);
        LogHelper.debug("Launching client instance");
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        processBuilder.directory(params.clientDir.toFile());
        processBuilder.inheritIO();
        processBuilder.redirectErrorStream(true);
        processBuilder.redirectOutput(ProcessBuilder.Redirect.PIPE);
        Map<String, String> map = processBuilder.environment();
        map.put("_JAVA_OPTS", "");
        map.put("_JAVA_OPTIONS", "");
        map.put("JAVA_OPTS", "");
        map.put("JAVA_OPTIONS", "");
        return processBuilder.start();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @LauncherAPI
    public static void main(String ... stringArray) throws Throwable {
        URL[] uRLArray;
        SignedObjectHolder<StreamObject> clientHDir;
        SignedObjectHolder<StreamObject> assetHDir;
        SignedObjectHolder<ClientProfile> profile;
        SignedObjectHolder<StreamObject> jvmHDir;
        Params params;
        SecurityHelper.verifyCertificates(ClientLauncher.class);
        JVMHelper.verifySystemProperties(ClientLauncher.class, true);
        LogHelper.printVersion("Client Launcher");
        VerifyHelper.verifyInt(stringArray.length, n -> n >= 1, "Missing args: <paramsFile>");
        Path path = IOHelper.toPath(stringArray[0]);
        LogHelper.debug("Reading ClientLauncher params file");
        RSAPublicKey rSAPublicKey = Launcher.getConfig().publicKey;
        try (HInput input = new HInput(IOHelper.newInput(path));){
            params = new Params((HInput)input);
            profile = new SignedObjectHolder<ClientProfile>((HInput)input, rSAPublicKey, ClientProfile.RO_ADAPTER);
            jvmHDir = new SignedObjectHolder<StreamObject>((HInput)input, rSAPublicKey, HashedDir::new);
            assetHDir = new SignedObjectHolder<StreamObject>((HInput)input, rSAPublicKey, HashedDir::new);
            clientHDir = new SignedObjectHolder<StreamObject>((HInput)input, rSAPublicKey, HashedDir::new);
        }
        finally {
            Files.delete(path);
        }
        LogHelper.debug("Verifying ClientLauncher sign and classpath");
        //SecurityHelper.verifySign(LauncherRequest.BINARY_PATH, Params.aux((Params)params), rSAPublicKey);
        for (URL classpathURL : uRLArray = JVMHelper.getClassPath()) {
            Path file = Paths.get(classpathURL.toURI());
            if (file.startsWith(IOHelper.JVM_DIR) || file.equals(LauncherRequest.BINARY_PATH)) continue;
            throw new SecurityException(String.format("Forbidden classpath entry: '%s'", file));
        }
        boolean bl = !((ClientProfile)profile.object).isUpdateFastCheck();
        LogHelper.debug("Starting JVM and client WatchService");
        FileNameMatcher fileNameMatcher = ((ClientProfile)profile.object).getAssetUpdateMatcher();
        FileNameMatcher fileNameMatcher2 = ((ClientProfile)profile.object).getClientUpdateMatcher();
        try (DirWatcher DirWatcher2 = new DirWatcher(IOHelper.JVM_DIR, (HashedDir)jvmHDir.object, null, bl);
             DirWatcher throwable = new DirWatcher(params.assetDir, (HashedDir)assetHDir.object, fileNameMatcher, bl);
             DirWatcher DirWatcher3 = new DirWatcher(params.clientDir, (HashedDir)clientHDir.object, fileNameMatcher2, bl);){
            CommonHelper.newThread("JVM Directory Watcher", true, DirWatcher2).start();
            CommonHelper.newThread("Asset Directory Watcher", true, throwable).start();
            CommonHelper.newThread("Client Directory Watcher", true, DirWatcher3).start();
            ClientLauncher.launch((ClientProfile)profile.object, params);
        }
    }

    @LauncherAPI
    public static String toHash(UUID uUID) {
        return UUID_PATTERN.matcher(uUID.toString()).replaceAll("");
    }

    @LauncherAPI
    public static void verifyHDir(Path path, HashedDir hashedDir, FileNameMatcher fileNameMatcher, boolean bl) throws IOException {
        HashedDir hashedDir2;
        if (fileNameMatcher != null) {
            fileNameMatcher = fileNameMatcher.verifyOnly();
        }
        if (!hashedDir.diff(hashedDir2 = new HashedDir(path, fileNameMatcher, false, bl), fileNameMatcher).isSame()) {
            throw new SecurityException(String.format("Forbidden modification: '%s'", IOHelper.getFileName(path)));
        }
    }

    private static void addClientArgs(Collection collection, ClientProfile clientProfile, Params params) {
        PlayerProfile playerProfile = params.pp;
        String string = clientProfile.getVersion();
        Collections.addAll(collection, "--username", playerProfile.username);
        if (ClientProfile.Version.compare(string, "1.7.2") >= 0) {
            Collections.addAll(collection, "--uuid", ClientLauncher.toHash(playerProfile.uuid));
            Collections.addAll(collection, "--accessToken", params.accessToken);
            if (ClientProfile.Version.compare(string, "1.7.3") >= 0) {
                if (ClientProfile.Version.compare(string, "1.7.4") >= 0) {
                    Collections.addAll(collection, "--userType", "mojang");
                }
                JsonObject jsonObject = Json.object();
                if (playerProfile.skin != null) {
                    jsonObject.add(SKIN_URL_PROPERTY, Json.array(playerProfile.skin.url));
                    jsonObject.add(SKIN_DIGEST_PROPERTY, Json.array(SecurityHelper.toHex(playerProfile.skin.digest)));
                }
                if (playerProfile.cloak != null) {
                    jsonObject.add(CLOAK_URL_PROPERTY, Json.array(playerProfile.cloak.url));
                    jsonObject.add(CLOAK_DIGEST_PROPERTY, Json.array(SecurityHelper.toHex(playerProfile.cloak.digest)));
                }
                Collections.addAll(collection, "--userProperties", jsonObject.toString(WriterConfig.MINIMAL));
                Collections.addAll(collection, "--assetIndex", clientProfile.getAssetIndex());
            }
        } else {
            Collections.addAll(collection, "--session", params.accessToken);
        }
        Collections.addAll(collection, "--version", clientProfile.getVersion());
        Collections.addAll(collection, "--gameDir", params.clientDir.toString());
        Collections.addAll(collection, "--assetsDir", params.assetDir.toString());
        Collections.addAll(collection, "--resourcePackDir", params.clientDir.resolve(RESOURCEPACKS_DIR).toString());
        if (ClientProfile.Version.compare(string, "1.9.0") >= 0) {
            Collections.addAll(collection, "--versionType", "Launcher v1.7.5.2");
        }
        if (params.autoEnter) {
            Collections.addAll(collection, "--server", clientProfile.getServerAddress());
            Collections.addAll(collection, "--port", Integer.toString(clientProfile.getServerPort()));
        }
        if (params.fullScreen) {
            Collections.addAll(collection, "--fullscreen", Boolean.toString(true));
        }
        if (params.width > 0 && params.height > 0) {
            Collections.addAll(collection, "--width", Integer.toString(params.width));
            Collections.addAll(collection, "--height", Integer.toString(params.height));
        }
    }

    private static void addClientLegacyArgs(Collection collection, ClientProfile clientProfile, Params params) {
        collection.add(params.pp.username);
        collection.add(params.accessToken);
        Collections.addAll(collection, "--version", clientProfile.getVersion());
        Collections.addAll(collection, "--gameDir", params.clientDir.toString());
        Collections.addAll(collection, "--assetsDir", params.assetDir.toString());
    }

    private static void launch(ClientProfile clientProfile, Params params) throws Throwable {
        URL[] uRLArray;
        LinkedList linkedList = new LinkedList();
        if (ClientProfile.Version.compare(clientProfile.getVersion(), "1.6.0") >= 0) {
            ClientLauncher.addClientArgs(linkedList, clientProfile, params);
        } else {
            ClientLauncher.addClientLegacyArgs(linkedList, clientProfile, params);
        }
        Collections.addAll(linkedList, clientProfile.getClientArgs());
        LogHelper.debug("Args: " + linkedList);
        for (URL uRL : uRLArray = ClientLauncher.resolveClassPath(params.clientDir, clientProfile.getClassPath())) {
            JVMHelper.addClassPath(uRL);
        }
        Class<?> clazz = Class.forName(clientProfile.getMainClass());
        MethodHandle methodHandle = JVMHelper.LOOKUP.findStatic(clazz, "main", MethodType.methodType(Void.TYPE, String[].class)).asFixedArity();
        LAUNCHED.set(true);
        JVMHelper.fullGC();
        System.setProperty("minecraft.launcher.brand", "Launcher");
        System.setProperty("minecraft.launcher.version", "1.7.5.2");
        System.setProperty("minecraft.applet.TargetDirectory", params.clientDir.toString());
        methodHandle.invoke(linkedList.toArray(aux));
    }

    private static URL[] resolveClassPath(Path path, String ... stringArray) throws IOException {
        LinkedList<Path> linkedList = new LinkedList<Path>();
        for (String string : stringArray) {
            Path path2 = path.resolve(IOHelper.toPath(string));
            if (IOHelper.isDir(path2)) {
                IOHelper.walk(path2, new ClassPathFileVisitor(linkedList), false);
                continue;
            }
            linkedList.add(path2);
        }
        return (URL[])linkedList.stream().map(IOHelper::toURL).toArray(URL[]::new);
    }

    static {
        LAUNCHED = new AtomicBoolean(false);
        BIN_POSIX_PERMISSIONS = Collections.unmodifiableSet(EnumSet.of(PosixFilePermission.OWNER_READ, new PosixFilePermission[]{PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_EXECUTE, PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_EXECUTE}));
        NATIVES_DIR = IOHelper.toPath("natives");
        RESOURCEPACKS_DIR = IOHelper.toPath("resourcepacks");
        UUID_PATTERN = Pattern.compile("-", 16);
    }

    private static final class ClassPathFileVisitor
            extends SimpleFileVisitor<Path> {
        private final Collection<Path> aux;

        private ClassPathFileVisitor(Collection<Path> collection) {
            this.aux = collection;
        }

        @Override
        public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
            if (IOHelper.hasExtension(path, "jar") || IOHelper.hasExtension(path, "zip")) {
                this.aux.add(path);
            }
            return super.visitFile(path, basicFileAttributes);
        }
    }

    public static final class Params
            extends StreamObject {
        @LauncherAPI
        public final Path assetDir;
        @LauncherAPI
        public final Path clientDir;
        @LauncherAPI
        public final PlayerProfile pp;
        @LauncherAPI
        public final String accessToken;
        @LauncherAPI
        public final boolean autoEnter;
        @LauncherAPI
        public final boolean fullScreen;
        @LauncherAPI
        public final int ram;
        @LauncherAPI
        public final int width;
        @LauncherAPI
        public final int height;
        private final byte[] launcherSign;

        @LauncherAPI
        public Params(byte[] byArray, Path path, Path path2, PlayerProfile playerProfile, String string, boolean bl, boolean bl2, int n, int n2, int n3) {
            this.launcherSign = (byte[])byArray.clone();
            this.assetDir = path;
            this.clientDir = path2;
            this.pp = playerProfile;
            this.accessToken = SecurityHelper.verifyToken(string);
            this.autoEnter = bl;
            this.fullScreen = bl2;
            this.ram = n;
            this.width = n2;
            this.height = n3;
        }

        @LauncherAPI
        public Params(HInput hInput) throws IOException {
            this.launcherSign = hInput.readByteArray(-256);
            this.assetDir = IOHelper.toPath(hInput.readString(0));
            this.clientDir = IOHelper.toPath(hInput.readString(0));
            this.pp = new PlayerProfile(hInput);
            int n = hInput.readInt();
            this.accessToken = SecurityHelper.verifyToken(hInput.readASCII(-n));
            this.autoEnter = hInput.readBoolean();
            this.fullScreen = hInput.readBoolean();
            this.ram = hInput.readVarInt();
            this.width = hInput.readVarInt();
            this.height = hInput.readVarInt();
        }

        @Override
        public void write(HOutput hOutput) throws IOException {
            hOutput.writeByteArray(this.launcherSign, -256);
            hOutput.writeString(this.assetDir.toString(), 0);
            hOutput.writeString(this.clientDir.toString(), 0);
            this.pp.write(hOutput);
            hOutput.writeInt(this.accessToken.length());
            hOutput.writeASCII(this.accessToken, -this.accessToken.length());
            hOutput.writeBoolean(this.autoEnter);
            hOutput.writeBoolean(this.fullScreen);
            hOutput.writeVarInt(this.ram);
            hOutput.writeVarInt(this.width);
            hOutput.writeVarInt(this.height);
        }

        static byte[] aux(Params params) {
            return params.launcherSign;
        }
    }
}
