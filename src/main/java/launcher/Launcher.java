package launcher;
import javafx.application.Application;
import launcher.client.ClientLauncher;
import launcher.client.ClientProfile;
import launcher.client.PlayerProfile;
import launcher.client.ServerPinger;
import launcher.hasher.FileNameMatcher;
import launcher.hasher.HashedDir;
import launcher.hasher.HashedEntry;
import launcher.hasher.HashedFile;
import launcher.helper.*;
import launcher.helper.js.JSApplication;
import launcher.request.CustomRequest;
import launcher.request.PingRequest;
import launcher.request.Request;
import launcher.request.RequestException;
import launcher.request.auth.AuthRequest;
import launcher.request.auth.CheckServerRequest;
import launcher.request.auth.JoinServerRequest;
import launcher.request.update.LauncherRequest;
import launcher.request.update.UpdateRequest;
import launcher.request.uuid.BatchProfileByUsernameRequest;
import launcher.request.uuid.ProfileByUUIDRequest;
import launcher.request.uuid.ProfileByUsernameRequest;
import launcher.serialize.HInput;
import launcher.serialize.HOutput;
import launcher.serialize.config.ConfigObject;
import launcher.serialize.config.TextConfigReader;
import launcher.serialize.config.TextConfigWriter;
import launcher.serialize.config.entry.*;
import launcher.serialize.signed.SignedBytesHolder;
import launcher.serialize.signed.SignedObjectHolder;
import launcher.serialize.stream.EnumSerializer;
import launcher.serialize.stream.StreamObject;

import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.NoSuchFileException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class Launcher {
    @LauncherAPI
    public static final String VERSION = "1.7.5.2";
    @LauncherAPI
    public static final String BUILD = Launcher.readBuildNumber();
    @LauncherAPI
    public static final int PROTOCOL_MAGIC = 1917264919;
    @LauncherAPI
    public static final String RUNTIME_DIR = "runtime";
    @LauncherAPI
    public static final String CONFIG_FILE = "config.bin";
    @LauncherAPI
    public static final String INIT_SCRIPT_FILE = "init.js";
    private static final AtomicReference config = new AtomicReference();
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final ScriptEngine scriptEngine = CommonHelper.newScriptEngine();

    private Launcher() {
        this.setScriptBindings();
    }

    @LauncherAPI
    public static void addLauncherClassBindings(ScriptEngine scriptEngine, Map map) {
        Launcher.addClassBinding(scriptEngine, map, "Launcher", Launcher.class);
        Launcher.addClassBinding(scriptEngine, map, "Config", Config.class);
        Launcher.addClassBinding(scriptEngine, map, "PlayerProfile", PlayerProfile.class);
        Launcher.addClassBinding(scriptEngine, map, "PlayerProfileTexture", PlayerProfile.Texture.class);
        Launcher.addClassBinding(scriptEngine, map, "ClientProfile", ClientProfile.class);
        Launcher.addClassBinding(scriptEngine, map, "ClientProfileVersion", ClientProfile.Version.class);
        Launcher.addClassBinding(scriptEngine, map, "ClientLauncher", ClientLauncher.class);
        Launcher.addClassBinding(scriptEngine, map, "ClientLauncherParams", ClientLauncher.Params.class);
        Launcher.addClassBinding(scriptEngine, map, "ServerPinger", ServerPinger.class);
        Launcher.addClassBinding(scriptEngine, map, "Request", Request.class);
        Launcher.addClassBinding(scriptEngine, map, "RequestType", Request.Type.class);
        Launcher.addClassBinding(scriptEngine, map, "RequestException", RequestException.class);
        Launcher.addClassBinding(scriptEngine, map, "CustomRequest", CustomRequest.class);
        Launcher.addClassBinding(scriptEngine, map, "PingRequest", PingRequest.class);
        Launcher.addClassBinding(scriptEngine, map, "AuthRequest", AuthRequest.class);
        Launcher.addClassBinding(scriptEngine, map, "JoinServerRequest", JoinServerRequest.class);
        Launcher.addClassBinding(scriptEngine, map, "CheckServerRequest", CheckServerRequest.class);
        Launcher.addClassBinding(scriptEngine, map, "UpdateRequest", UpdateRequest.class);
        Launcher.addClassBinding(scriptEngine, map, "LauncherRequest", LauncherRequest.class);
        Launcher.addClassBinding(scriptEngine, map, "ProfileByUsernameRequest", ProfileByUsernameRequest.class);
        Launcher.addClassBinding(scriptEngine, map, "ProfileByUUIDRequest", ProfileByUUIDRequest.class);
        Launcher.addClassBinding(scriptEngine, map, "BatchProfileByUsernameRequest", BatchProfileByUsernameRequest.class);
        Launcher.addClassBinding(scriptEngine, map, "FileNameMatcher", FileNameMatcher.class);
        Launcher.addClassBinding(scriptEngine, map, "HashedDir", HashedDir.class);
        Launcher.addClassBinding(scriptEngine, map, "HashedFile", HashedFile.class);
        Launcher.addClassBinding(scriptEngine, map, "HashedEntryType", HashedEntry.Type.class);
        Launcher.addClassBinding(scriptEngine, map, "HInput", HInput.class);
        Launcher.addClassBinding(scriptEngine, map, "HOutput", HOutput.class);
        Launcher.addClassBinding(scriptEngine, map, "StreamObject", StreamObject.class);
        Launcher.addClassBinding(scriptEngine, map, "StreamObjectAdapter", StreamObject.Adapter.class);
        Launcher.addClassBinding(scriptEngine, map, "SignedBytesHolder", SignedBytesHolder.class);
        Launcher.addClassBinding(scriptEngine, map, "SignedObjectHolder", SignedObjectHolder.class);
        Launcher.addClassBinding(scriptEngine, map, "EnumSerializer", EnumSerializer.class);
        Launcher.addClassBinding(scriptEngine, map, "ConfigObject", ConfigObject.class);
        Launcher.addClassBinding(scriptEngine, map, "ConfigObjectAdapter", ConfigObject.Adapter.class);
        Launcher.addClassBinding(scriptEngine, map, "BlockConfigEntry", BlockConfigEntry.class);
        Launcher.addClassBinding(scriptEngine, map, "BooleanConfigEntry", BooleanConfigEntry.class);
        Launcher.addClassBinding(scriptEngine, map, "IntegerConfigEntry", IntegerConfigEntry.class);
        Launcher.addClassBinding(scriptEngine, map, "ListConfigEntry", ListConfigEntry.class);
        Launcher.addClassBinding(scriptEngine, map, "StringConfigEntry", StringConfigEntry.class);
        Launcher.addClassBinding(scriptEngine, map, "ConfigEntryType", ConfigEntry.Type.class);
        Launcher.addClassBinding(scriptEngine, map, "TextConfigReader", TextConfigReader.class);
        Launcher.addClassBinding(scriptEngine, map, "TextConfigWriter", TextConfigWriter.class);
        Launcher.addClassBinding(scriptEngine, map, "CommonHelper", CommonHelper.class);
        Launcher.addClassBinding(scriptEngine, map, "IOHelper", IOHelper.class);
        Launcher.addClassBinding(scriptEngine, map, "JVMHelper", JVMHelper.class);
        Launcher.addClassBinding(scriptEngine, map, "JVMHelperOS", JVMHelper.OS.class);
        Launcher.addClassBinding(scriptEngine, map, "LogHelper", LogHelper.class);
        Launcher.addClassBinding(scriptEngine, map, "LogHelperOutput", LogHelper.Output.class);
        Launcher.addClassBinding(scriptEngine, map, "SecurityHelper", SecurityHelper.class);
        Launcher.addClassBinding(scriptEngine, map, "DigestAlgorithm", SecurityHelper.DigestAlgorithm.class);
        Launcher.addClassBinding(scriptEngine, map, "VerifyHelper", VerifyHelper.class);
        try {
            Launcher.addClassBinding(scriptEngine, map, "Application", Application.class);
            Launcher.addClassBinding(scriptEngine, map, "JSApplication", JSApplication.class);
        }
        catch (Throwable throwable) {
            LogHelper.error("JavaFX API isn't available");
        }
    }
    @LauncherAPI
    public static void addClassBinding(ScriptEngine scriptEngine, Map map, String string, Class clazz) {
        LogHelper.info("addClassBinding: '%s'", string);
        map.put(string + "Class", clazz);
        try {
            scriptEngine.eval("var " + string + " = " + string + "Class.static;");
        }
        catch (ScriptException scriptException) {
            throw new AssertionError((Object)scriptException);
        }
    }

    @LauncherAPI
    public static Config getConfig() {
        Config config = (Config) Launcher.config.get();
        if (config == null) {
            try (HInput hInput = new HInput(IOHelper.newInput(IOHelper.getResourceURL(CONFIG_FILE)));){
                config = new Config(hInput);
            }
            catch (IOException | InvalidKeySpecException exception) {
                throw new SecurityException(exception);
            }
            Launcher.config.set(config);
        }
        return config;
    }

    @LauncherAPI
    public static URL getResourceURL(String string) throws IOException {
        Config config = Launcher.getConfig();
        byte[] byArray = config.runtime.get(string);
        if (byArray == null) {
            throw new NoSuchFileException(string);
        }
        URL uRL = IOHelper.getResourceURL("runtime/" + string);
        if (!Arrays.equals(byArray, SecurityHelper.digest(SecurityHelper.DigestAlgorithm.MD5, uRL))) {
            throw new NoSuchFileException(string);
        }
        return uRL;
    }
    @LauncherAPI
    public static String getVersion() {
        return VERSION;
    }

    public static void main(String ... stringArray) {
        SecurityHelper.verifyCertificates(Launcher.class);
        JVMHelper.verifySystemProperties(Launcher.class, true);
        LogHelper.printVersion("Launcher");
        try {
            new Launcher().start(stringArray);
        }
        catch (Throwable throwable) {
            LogHelper.error(throwable);
        }
    }
    private static String readBuildNumber() {
        try {
            return IOHelper.request(IOHelper.getResourceURL("buildnumber"));
        } catch (IOException ignored) {
            return "dev"; // Maybe dev env?
        }
    }
    @LauncherAPI
    public Object loadScript(URL uRL) throws IOException, ScriptException {
        LogHelper.debug("Loading script: '%s'", uRL);
        try (BufferedReader bufferedReader = IOHelper.newReader(uRL);){
            Object object3;
            Object object;
            Object object2 = object = this.scriptEngine.eval(bufferedReader);
            Object object4 = object3 = object;
            return object4;
        }
    }
    @LauncherAPI
    public void start(String ... stringArray) throws Throwable {
        Objects.requireNonNull(stringArray, "args");
        if (this.started.getAndSet(true)) {
            throw new IllegalStateException("Launcher has been already started");
        }
        this.loadScript(Launcher.getResourceURL(INIT_SCRIPT_FILE));
        ((Invocable)((Object)this.scriptEngine)).invokeFunction("start", new Object[]{stringArray});
    }
    private void setScriptBindings() {
        Bindings bindings = this.scriptEngine.getBindings(100);
        bindings.put("launcher", (Object)this);
        Launcher.addLauncherClassBindings(this.scriptEngine, bindings);
    }

    public static final class Config extends StreamObject {
        @LauncherAPI
        public static final String ADDRESS_OVERRIDE_PROPERTY = "launcher.addressOverride";
        @LauncherAPI
        public static final String ADDRESS_OVERRIDE = System.getProperty("launcher.addressOverride", null);
        @LauncherAPI
        public final InetSocketAddress address;
        @LauncherAPI
        public final InetSocketAddress alterAddress;
        @LauncherAPI
        public final RSAPublicKey publicKey;
        @LauncherAPI
        public final Map<String, byte[]> runtime;

        @LauncherAPI
        public Config(String string, String string2, int n, RSAPublicKey rSAPublicKey, Map map) {
            this.address = InetSocketAddress.createUnresolved(string, n);
            this.alterAddress = InetSocketAddress.createUnresolved(string2, n);
            this.publicKey = Objects.requireNonNull(rSAPublicKey, "publicKey");
            this.runtime = Collections.unmodifiableMap(new HashMap(map));
        }

        @LauncherAPI
        public Config(HInput hInput) throws IOException, InvalidKeySpecException {
            String string = hInput.readASCII(255);
            int n = hInput.readLength(65535);
            this.address = InetSocketAddress.createUnresolved(ADDRESS_OVERRIDE == null ? string : ADDRESS_OVERRIDE, n);
            String string2 = hInput.readASCII(255);
            this.alterAddress = InetSocketAddress.createUnresolved(ADDRESS_OVERRIDE == null ? string2 : ADDRESS_OVERRIDE, n);
            this.publicKey = SecurityHelper.toPublicRSAKey(hInput.readByteArray(2048));
            int n2 = hInput.readLength(0);
            HashMap hashMap = new HashMap(n2);
            for (int i = 0; i < n2; ++i) {
                String string3 = hInput.readString(255);
                VerifyHelper.putIfAbsent(hashMap, string3, hInput.readByteArray(2048), String.format("Duplicate runtime resource: '%s'", string3));
            }
            this.runtime = Collections.unmodifiableMap(hashMap);
            if (ADDRESS_OVERRIDE != null) {
                LogHelper.warning("Address override is enabled: '%s'", ADDRESS_OVERRIDE);
            }
        }

        @Override
        public void write(HOutput hOutput) throws IOException {
            hOutput.writeASCII(this.address.getHostString(), 255);
            hOutput.writeLength(this.address.getPort(), 65535);
            hOutput.writeASCII(this.alterAddress.getHostString(), 255);
            hOutput.writeByteArray(this.publicKey.getEncoded(), 2048);
            Set<Map.Entry<String, byte[]>> set = this.runtime.entrySet();
            hOutput.writeLength(set.size(), 0);
            for (Map.Entry<String, byte[]> entry : this.runtime.entrySet()) {
                hOutput.writeString(entry.getKey(), 255);
                hOutput.writeByteArray(entry.getValue(), 2048);
            }
        }
    }
}
